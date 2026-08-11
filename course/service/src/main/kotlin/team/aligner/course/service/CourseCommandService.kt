package team.aligner.course.service

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.transaction.annotation.Transactional
import team.aligner.course.infrastructure.CauseLookupPort
import team.aligner.course.infrastructure.CourseRepository
import team.aligner.course.infrastructure.CourseTemplateRepository
import team.aligner.course.infrastructure.StampRepository
import team.aligner.course.infrastructure.TargetPoseCatalogPort
import team.aligner.course.model.Course
import team.aligner.course.model.CourseIdentity
import team.aligner.course.model.CourseStatus
import team.aligner.course.model.Stamp
import team.aligner.course.model.exception.BodyPartNotInScreeningException
import team.aligner.course.model.exception.CourseNotFoundException
import team.aligner.course.model.exception.CourseTemplateNotFoundException
import team.aligner.course.model.exception.ScreeningRequiredException
import java.time.Instant

interface CourseCommandService {
    fun prescribe(
        memberId: Long,
        command: PrescribeCourseCommand,
    ): CourseIdentity

    fun completeStep(
        memberId: Long,
        courseId: Long,
        stepOrder: Int,
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
    private val causeLookupPort: CauseLookupPort,
    private val targetPoseCatalogPort: TargetPoseCatalogPort,
) : CourseCommandService {
    /**
     * (강화 부위, 난이도)로 코스를 처방한다.
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
     */
    override fun prescribe(
        memberId: Long,
        command: PrescribeCourseCommand,
    ): CourseIdentity {
        val causeCode = verifyBodyPart(memberId, command.bodyPartCode)

        // 부위·레벨 → 자세 해석은 catalog 의 일이다. 여기서 SQL 을 짜면 도메인 간 조인이
        // 생긴다 (docs/domains.md §6).
        val targetPose =
            targetPoseCatalogPort.findByBodyPartCodeAndLevel(command.bodyPartCode, command.level)
                ?: throw CourseTemplateNotFoundException()

        findExistingCourse(memberId, targetPose.targetPoseId)?.let { return it }

        val template =
            courseTemplateRepository.findByTargetPoseId(targetPose.targetPoseId)
                ?: throw CourseTemplateNotFoundException()

        return try {
            val saved =
                courseRepository.save(
                    Course.prescribe(memberId = memberId, template = template, causeCode = causeCode),
                )
            checkNotNull(saved.identity) { "저장된 코스에 식별자가 없다" }
        } catch (e: DataIntegrityViolationException) {
            // 조회와 저장 사이에 다른 요청이 같은 코스를 만들었다. 유니크 제약이 막아준
            // 것이므로 실패가 아니라 **멱등 응답**이어야 한다 — 다시 읽어 그 코스를 돌려준다.
            //
            // 조회만으로 막으려 하면 이 틈이 남는다. 제약을 최종 방어선으로 두고 여기서
            // 흡수하는 것이 순서다.
            findExistingCourse(memberId, targetPose.targetPoseId) ?: throw e
        }
    }

    /**
     * 스텝 완료를 반영한다. 마지막 스텝이었으면 도장이 붙는다.
     *
     * 도장 부여가 `training` 이 아니라 여기 있는 것이 이 도메인 분할의 요점이다 —
     * 기록은 training, **판단은 course** 다 (docs/domains.md §2).
     *
     * **멱등하다.** 이미 완료된 코스에 재시도가 들어와도 도장이 두 번 붙지 않는다.
     */
    override fun completeStep(
        memberId: Long,
        courseId: Long,
        stepOrder: Int,
    ): CourseProgressResult {
        val completed = saveCompletedStep(memberId, courseId, stepOrder)

        // **도장 획득 여부를 서비스가 짐작하지 않는다.** "방금 코스가 완료됐나" 로 판단하면
        // 두 요청이 동시에 마지막 스텝을 밀어넣을 때 둘 다 획득으로 볼 수 있다.
        // 저장이 실제로 새 행을 넣었는지가 유일한 근거다.
        val stampAcquired =
            completed.status == CourseStatus.COMPLETED &&
                stampRepository.saveIfAbsent(
                    Stamp.acquire(
                        memberId = memberId,
                        targetPoseId = completed.targetPoseId,
                        courseId = courseId,
                        at = completed.completedAt ?: Instant.now(),
                    ),
                )

        return CourseProgressResult(
            courseId = courseId,
            completedStepCount = completed.completedStepCount,
            totalStepCount = completed.totalStepCount,
            courseCompleted = completed.status == CourseStatus.COMPLETED,
            stampAcquired = stampAcquired,
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
    ): CourseIdentity? =
        courseRepository
            .findByMemberIdAndTargetPoseId(memberId, targetPoseId)
            ?.let { checkNotNull(it.identity) { "저장된 코스에 식별자가 없다" } }

    /**
     * 회원이 고른 부위가 자기 진단 결과에 있는지 확인하고, 그 부위의 원인 코드를 돌려준다.
     *
     * 진단한 적이 없으면 409 다. 화면은 이때 온보딩으로 보낸다 — 400 으로 내리면 요청이
     * 잘못된 것처럼 보이는데 실제로는 순서를 건너뛴 것이다.
     */
    private fun verifyBodyPart(
        memberId: Long,
        bodyPartCode: String,
    ): String {
        val causes = causeLookupPort.findLatestCauses(memberId)
        if (causes.isEmpty()) {
            throw ScreeningRequiredException()
        }
        return causes
            .find { it.bodyPartCode == bodyPartCode }
            ?.causeCode
            ?: throw BodyPartNotInScreeningException()
    }
}

/**
 * 처방 입력.
 *
 * **자세 식별자를 받지 않는다.** 부위와 난이도만 받고 자세는 서버가 catalog 에서 찾는다 —
 * 클라이언트가 자세를 지정하면 고르지 않은 난이도의 코스를 받아갈 수 있다.
 *
 * `memberId` 를 명령에 담지 않는다. api 가 `AlignerPrincipal` 에서 꺼내 **파라미터로** 넘긴다 —
 * 명령에 섞으면 클라이언트가 보낸 본문으로 남의 회원 식별자를 넣을 여지가 생긴다
 * (docs/architecture.md §9).
 */
data class PrescribeCourseCommand(
    val bodyPartCode: String,
    val level: Int,
)

data class CourseProgressResult(
    val courseId: Long,
    val completedStepCount: Int,
    val totalStepCount: Int,
    val courseCompleted: Boolean,
    val stampAcquired: Boolean,
)
