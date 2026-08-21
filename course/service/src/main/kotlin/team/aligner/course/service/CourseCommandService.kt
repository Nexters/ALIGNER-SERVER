package team.aligner.course.service

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.transaction.annotation.Transactional
import team.aligner.course.infrastructure.CauseLookupPort
import team.aligner.course.infrastructure.CourseRepository
import team.aligner.course.infrastructure.CourseTemplateRepository
import team.aligner.course.infrastructure.ExerciseCatalogPort
import team.aligner.course.infrastructure.MemberBodyPort
import team.aligner.course.infrastructure.StampRepository
import team.aligner.course.infrastructure.TargetPoseCatalogPort
import team.aligner.course.model.Course
import team.aligner.course.model.CourseIdentity
import team.aligner.course.model.CourseStatus
import team.aligner.course.model.Stamp
import team.aligner.course.model.exception.CourseNotFoundException
import team.aligner.course.model.exception.CourseTemplateNotFoundException
import team.aligner.course.model.exception.ScreeningRequiredException
import java.time.Instant

interface CourseCommandService {
    fun recommend(
        memberId: Long,
        command: RecommendCourseCommand,
    ): CourseIdentity

    fun completeStep(
        memberId: Long,
        courseId: Long,
        stepOrder: Int,
        performedExercises: List<PerformedExercise>,
    ): CourseProgressResult
}

/**
 * `@Transactional` 은 **클래스에** 붙인다. kotlin-spring(allopen)이 클래스에 붙은 어노테이션만
 * 보고 open 을 매기기 때문이다. 메서드에만 붙이면 클래스가 final 로 남고 CGLIB 프록시 생성이
 * 실패해 기동이 죽는다 (member·screening 에 같은 주석이 있다).
 */
@Transactional
internal class CourseCommandServiceImpl(
    private val courseRepository: CourseRepository,
    private val courseTemplateRepository: CourseTemplateRepository,
    private val stampRepository: StampRepository,
    private val exerciseCatalogPort: ExerciseCatalogPort,
    private val memberBodyPort: MemberBodyPort,
    private val causeLookupPort: CauseLookupPort,
    private val targetPoseCatalogPort: TargetPoseCatalogPort,
) : CourseCommandService {
    /**
     * (강화 부위, 난이도)로 코스를 추천한다.
     *
     * **난이도가 곧 자세 레벨이다.** 회원이 고른 값이 `catalog.target_pose` 의 (부위, 레벨)이고
     * 그 자세의 템플릿으로 코스를 만든다. 하나의 핀포즈가 곧 하나의 코스다
     * (docs/domains.md §4-4).
     *
     * **원인을 클라이언트가 보내지 않는다.** 서버가 최신 진단을 읽어 회원이 고른 부위가 실제
     * 분석 결과에 있는지 검증한다. 요청 본문으로 받으면 원인 위조가 가능하다 (§2).
     *
     * **멱등하다.** 같은 자세의 코스가 이미 있으면 새로 만들지 않고 그것을 돌려준다 —
     * `(member_id, target_pose_id)` 유니크가 DB 에서도 같은 것을 막는다.
     *
     * **완주한 코스는 여기서 다시 열린다.** 완주할 때마다 도장이 하나 붙고 4 개를 채워야 자세
     * 완성이라, 도전 현황에서 같은 자세를 다시 누른 회원이 두 번째 도전을 시작할 경로가
     * 필요하다 ([restartIfCompleted]).
     */
    override fun recommend(
        memberId: Long,
        command: RecommendCourseCommand,
    ): CourseIdentity {
        val causeCode = findCauseCode(memberId, command.bodyPartCode)

        // 부위·레벨 → 자세 해석은 catalog 의 일이다. 여기서 SQL 을 짜면 도메인 간 조인이
        // 생긴다 (docs/domains.md §6).
        val targetPose =
            targetPoseCatalogPort.findByBodyPartCodeAndLevel(command.bodyPartCode, command.level)
                ?: throw CourseTemplateNotFoundException()

        findExistingCourse(memberId, targetPose.targetPoseId)?.let { return restartIfCompleted(memberId, it) }

        val template =
            courseTemplateRepository.findByTargetPoseId(targetPose.targetPoseId)
                ?: throw CourseTemplateNotFoundException()

        return try {
            val saved =
                courseRepository.save(
                    Course.recommend(memberId = memberId, template = template, causeCode = causeCode),
                )
            checkNotNull(saved.identity) { "저장된 코스에 식별자가 없다" }
        } catch (e: DataIntegrityViolationException) {
            // 조회와 저장 사이에 다른 요청이 같은 코스를 만들었다. 유니크 제약이 막아준
            // 것이므로 실패가 아니라 **멱등 응답**이어야 한다 — 다시 읽어 그 코스를 돌려준다.
            //
            // 조회만으로 막으려 하면 이 틈이 남는다. 제약을 최종 방어선으로 두고 여기서
            // 흡수하는 것이 순서다.
            findExistingCourse(memberId, targetPose.targetPoseId)?.let { restartIfCompleted(memberId, it) } ?: throw e
        }
    }

    /**
     * 완주한 코스를 다시 연다. 스텝이 처음 상태로 돌아가고 회차가 하나 오른다.
     *
     * **진행 중인 코스는 건드리지 않는다.** 추천 재호출이 진행도를 지우면 안 된다 — 홈에서
     * 돌아온 회원이 도전 현황에서 같은 자세를 다시 눌러도 하던 자리에서 이어가야 한다.
     *
     * **도장을 다 채웠으면 열지 않는다.** 4 개가 상한이라 다시 열어도 더 붙을 도장이 없다.
     * 완성한 자세는 그 상태로 남는다.
     *
     * 코스 식별자는 어느 쪽이든 같다. 재도전은 새 코스가 아니라 **같은 코스의 다음 회차**다.
     */
    private fun restartIfCompleted(
        memberId: Long,
        course: Course,
    ): CourseIdentity {
        val identity = checkNotNull(course.identity) { "저장된 코스에 식별자가 없다" }
        if (course.status != CourseStatus.COMPLETED) {
            return identity
        }
        if (stampRepository.countAcquired(memberId, course.targetPoseId) >= Stamp.REQUIRED_COUNT) {
            return identity
        }

        return try {
            courseRepository.save(course.restart())
            identity
        } catch (_: OptimisticLockingFailureException) {
            // 충돌이 곧 "다른 쪽이 이미 다시 열었다" 는 아니다. **완료 push 재시도가 같은
            // 코스를 다시 저장해 버전만 올린 경우**에도 여기로 온다 — 그때 성공으로 삼으면
            // 회원이 다시 눌렀는데 코스는 완주 상태 그대로다.
            //
            // 그래서 최신 상태를 다시 읽어 판단한다. 이미 열려 있으면 그대로 두고, 아직
            // 완주 상태면 새 버전으로 다시 연다. 두 번째도 충돌하면 그대로 올린다 —
            // 계속 미루기보다 클라이언트가 재시도하는 편이 낫다 (saveCompletedStep 과 같은 형태다).
            restartOnce(memberId, identity)
        }
    }

    private fun restartOnce(
        memberId: Long,
        identity: CourseIdentity,
    ): CourseIdentity {
        val current = loadOwned(memberId, identity.value)
        if (current.status != CourseStatus.COMPLETED ||
            stampRepository.countAcquired(memberId, current.targetPoseId) >= Stamp.REQUIRED_COUNT
        ) {
            return identity
        }
        courseRepository.save(current.restart())
        return identity
    }

    /**
     * 스텝 완료를 반영한다. 마지막 스텝이었으면 **이번 회차의 도장**이 붙는다.
     *
     * 도장 부여가 `training` 이 아니라 여기 있는 것이 이 도메인 분할의 요점이다 —
     * 기록은 training, **판단은 course** 다 (docs/domains.md §2).
     *
     * **멱등하다.** 이미 완료된 코스에 재시도가 들어와도 도장이 두 번 붙지 않는다.
     *
     * 완료 리포트가 그리는 "파이어로그 N / 4회" 와 헤더의 `골반 난이도 상 · 낙타자세` 를 여기서
     * 함께 실어 보낸다. 도장 수도 자세 정보도 course 가 이미 들고 있는 값이라, training 이
     * 따로 조회하면 같은 값을 다른 시점에 읽게 된다.
     */
    override fun completeStep(
        memberId: Long,
        courseId: Long,
        stepOrder: Int,
        performedExercises: List<PerformedExercise>,
    ): CourseProgressResult {
        val completed = saveCompletedStep(memberId, courseId, stepOrder)
        val courseCompleted = completed.status == CourseStatus.COMPLETED

        // **도장 획득 여부를 서비스가 짐작하지 않는다.** "방금 코스가 완료됐나" 로 판단하면
        // 두 요청이 동시에 마지막 스텝을 밀어넣을 때 둘 다 획득으로 볼 수 있다.
        // 저장이 실제로 새 행을 넣었는지가 유일한 근거다.
        //
        // 회차 상한을 여기서도 본다. 재도전을 열어주는 쪽(restartIfCompleted)이 이미 막지만,
        // 상한을 넘긴 회차가 어떤 경로로든 생기면 5 번째 도장이 붙어서는 안 된다.
        val stampAcquired =
            courseCompleted &&
                completed.attemptNo <= Stamp.REQUIRED_COUNT &&
                stampRepository.saveIfAbsent(
                    Stamp.acquire(
                        memberId = memberId,
                        targetPoseId = completed.targetPoseId,
                        courseId = courseId,
                        attemptNo = completed.attemptNo,
                        at = completed.completedAt ?: Instant.now(),
                    ),
                )

        val acquiredStampCount = stampRepository.countAcquired(memberId, completed.targetPoseId)
        val targetPose = targetPoseCatalogPort.findAllByIds(listOf(completed.targetPoseId)).firstOrNull()

        return CourseProgressResult(
            courseId = courseId,
            completedStepCount = completed.completedStepCount,
            totalStepCount = completed.totalStepCount,
            courseCompleted = courseCompleted,
            stampAcquired = stampAcquired,
            estimatedKcal = estimateKcal(memberId, completed, performedExercises),
            targetPoseId = completed.targetPoseId,
            // catalog 에 자세가 없어도 리포트를 실패시키지 않는다. 도메인 간 FK 가 없어
            // course seed 가 catalog 보다 앞서갈 수 있다 (CourseQueryServiceImpl 과 같은 판단).
            targetPoseExerciseId = targetPose?.exerciseId,
            targetPoseName = targetPose?.name ?: "",
            bodyPartCode = targetPose?.bodyPartCode,
            level = targetPose?.level,
            acquiredStampCount = acquiredStampCount,
            targetPoseCompleted = acquiredStampCount >= Stamp.REQUIRED_COUNT,
        )
    }

    /**
     * 이번 세션의 소모 칼로리. **수행 기록이 없으면 계산하지 않는다.**
     *
     * `training` 은 실측 시간만 넘기고 계산은 여기서 한다 — MET(catalog)과 몸무게(member)를
     * 이미 읽고 있는 쪽이 course 다 (docs/domains.md §4-3).
     *
     * 코스 개요의 예상 칼로리와 달리 **기본 수행 시간으로 메우지 않는다.** 리포트는 실제로
     * 얼마나 움직였는지를 말하는 화면이라, 모르는 값을 예상치로 채우면 다른 뜻이 된다.
     */
    private fun estimateKcal(
        memberId: Long,
        course: Course,
        performedExercises: List<PerformedExercise>,
    ): Int? {
        if (performedExercises.isEmpty()) {
            return null
        }

        val exerciseIdByStepExerciseId =
            course.steps
                .flatMap { it.exercises }
                .mapNotNull { row -> row.identity?.let { it to row.exerciseId } }
                .toMap()

        val performed =
            performedExercises.mapNotNull { performedExercise ->
                exerciseIdByStepExerciseId[performedExercise.courseStepExerciseId]?.let {
                    it to performedExercise.performedDurationSeconds
                }
            }
        if (performed.isEmpty()) {
            return null
        }

        val metValues =
            exerciseCatalogPort
                .findAllByIds(performed.map { it.first }.distinct())
                .associate { it.exerciseId to it.metValue }
        val weightKg = memberBodyPort.findWeightKg(memberId)

        return CalorieCalculator.sum(
            performed.map { (exerciseId, seconds) ->
                CalorieCalculator.calculate(metValues[exerciseId], weightKg, seconds)
            },
        )
    }

    /**
     * 스텝 완료를 저장한다. 낙관적 락 충돌이면 **다시 읽어 한 번 재시도**한다.
     *
     * 애그리거트를 통째로 저장하므로 두 세션 완료가 동시에 들어오면 나중 저장이 앞선 완료를
     * 덮는다. `version` 이 그것을 실패로 바꾸고, 여기서 최신 상태로 다시 적용한다.
     *
     * 완료는 멱등하므로 재시도가 안전하다. 두 번째도 충돌하면 그대로 올린다 — 계속 미루기보다
     * 호출부(training)가 재시도하는 편이 낫다.
     */
    private fun saveCompletedStep(
        memberId: Long,
        courseId: Long,
        stepOrder: Int,
    ): Course =
        try {
            courseRepository.save(loadOwned(memberId, courseId).completeStep(stepOrder, Instant.now()))
        } catch (_: OptimisticLockingFailureException) {
            courseRepository.save(loadOwned(memberId, courseId).completeStep(stepOrder, Instant.now()))
        }

    /**
     * 남의 코스와 없는 코스를 같은 404 로 돌려준다. 구분해서 알려주면 존재 여부가 새어나간다
     * (screening 의 findByIdAndMemberId 와 같은 판단).
     */
    private fun loadOwned(
        memberId: Long,
        courseId: Long,
    ): Course =
        courseRepository
            .findByIdentity(CourseIdentity.of(courseId))
            ?.takeIf { it.memberId == memberId }
            ?: throw CourseNotFoundException()

    private fun findExistingCourse(
        memberId: Long,
        targetPoseId: Long,
    ): Course? = courseRepository.findByMemberIdAndTargetPoseId(memberId, targetPoseId)

    /**
     * 진단에서 이 부위의 원인을 찾아 스냅샷으로 남긴다. **없어도 막지 않는다.**
     *
     * 코스는 추천이지 처방이 아니다. 온보딩에서 한 번 제안할 뿐이고, 그 뒤 「자세 도전 현황」이
     * 핀포즈 전체를 펼쳐두면 회원은 진단에 없던 부위도 눌러 시작한다. 여기서 거절하면 화면에
     * 보이는 자세가 시작되지 않는다.
     *
     * 진단을 **한 번도 하지 않은** 회원은 여전히 막는다. 그건 부위 선택의 문제가 아니라 온보딩을
     * 건너뛴 것이고, 409 라 화면이 온보딩으로 되돌린다 — 400 으로 내리면 요청이 잘못된 것처럼
     * 보이는데 실제로는 순서를 건너뛴 것이다.
     */
    private fun findCauseCode(
        memberId: Long,
        bodyPartCode: String,
    ): String? {
        val causes = causeLookupPort.findLatestCauses(memberId)
        if (causes.isEmpty()) {
            throw ScreeningRequiredException()
        }
        return causes.find { it.bodyPartCode == bodyPartCode }?.causeCode
    }
}

/**
 * 추천 입력.
 *
 * **자세 식별자를 받지 않는다.** 부위와 난이도만 받고 자세는 서버가 catalog 에서 찾는다 —
 * 클라이언트가 자세를 지정하면 고르지 않은 난이도의 코스를 받아갈 수 있다.
 *
 * `memberId` 를 명령에 담지 않는다. api 가 `AlignerPrincipal` 에서 꺼내 **파라미터로** 넘긴다 —
 * 명령에 섞으면 클라이언트가 보낸 본문으로 남의 회원 식별자를 넣을 여지가 생긴다
 * (docs/architecture.md §9).
 */
data class RecommendCourseCommand(
    val bodyPartCode: String,
    val level: Int,
)

data class CourseProgressResult(
    val courseId: Long,
    val completedStepCount: Int,
    val totalStepCount: Int,
    val courseCompleted: Boolean,
    /** 이 호출로 이번 회차의 도장이 새로 붙었는지. 재시도에서는 false 다. */
    val stampAcquired: Boolean,
    /** 이번 세션의 소모 칼로리. 계산이 성립하지 않으면 null 이다. */
    val estimatedKcal: Int?,
    val targetPoseId: Long,
    val targetPoseExerciseId: Long?,
    /** catalog 에 자세가 없으면 빈 문자열이다. 리포트를 실패시키지 않는다. */
    val targetPoseName: String,
    /** 리포트 헤더의 `골반 난이도 상`. catalog 에 자세가 없으면 null 이다. */
    val bodyPartCode: String?,
    val level: Int?,
    /** 이 자세에 지금까지 붙은 도장 수. 리포트의 "파이어로그 N / 4회" 의 N 이다. */
    val acquiredStampCount: Int,
    /** 도장을 [Stamp.REQUIRED_COUNT] 개 채웠는지. 자세 완성 축하 화면의 신호다. */
    val targetPoseCompleted: Boolean,
) {
    /** 리포트의 분모. 화면이 세그먼트 개수를 하드코딩하지 않게 서버가 함께 내린다. */
    val requiredStampCount: Int get() = Stamp.REQUIRED_COUNT
}

/** `training` 이 넘기는 실측 수행 시간. 계산은 course 가 한다. */
data class PerformedExercise(
    val courseStepExerciseId: Long,
    val performedDurationSeconds: Int?,
)
