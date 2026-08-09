package team.aligner.course.service

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

        courseRepository.findByMemberIdAndTargetPoseId(memberId, targetPose.targetPoseId)?.let { existing ->
            return checkNotNull(existing.identity) { "저장된 코스에 식별자가 없다" }
        }

        val template =
            courseTemplateRepository.findByTargetPoseId(targetPose.targetPoseId)
                ?: throw CourseTemplateNotFoundException()

        val saved =
            courseRepository.save(
                Course.prescribe(memberId = memberId, template = template, causeCode = causeCode),
            )
        return checkNotNull(saved.identity) { "저장된 코스에 식별자가 없다" }
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
        val course =
            courseRepository
                .findByIdentity(CourseIdentity.of(courseId))
                // 남의 코스와 없는 코스를 같은 404 로 돌려준다. 구분해서 알려주면 존재 여부가
                // 새어나간다 (screening 의 findByIdAndMemberId 와 같은 판단).
                ?.takeIf { it.memberId == memberId }
                ?: throw CourseNotFoundException()

        val alreadyCompleted = course.status == CourseStatus.COMPLETED

        val now = Instant.now()
        val completed = courseRepository.save(course.completeStep(stepOrder = stepOrder, at = now))

        val stampAcquired = completed.status == CourseStatus.COMPLETED && !alreadyCompleted
        if (stampAcquired) {
            stampRepository.saveIfAbsent(
                Stamp.acquire(
                    memberId = memberId,
                    targetPoseId = completed.targetPoseId,
                    courseId = courseId,
                    at = now,
                ),
            )
        }

        return CourseProgressResult(
            courseId = courseId,
            completedStepCount = completed.completedStepCount,
            totalStepCount = completed.totalStepCount,
            courseCompleted = completed.status == CourseStatus.COMPLETED,
            stampAcquired = stampAcquired,
        )
    }

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
