package team.aligner.screening.service

import org.springframework.transaction.annotation.Transactional
import team.aligner.screening.infrastructure.BodyPartRepository
import team.aligner.screening.infrastructure.CauseRuleRepository
import team.aligner.screening.infrastructure.ScreeningResultRepository
import team.aligner.screening.model.ScreeningResult
import team.aligner.screening.model.ScreeningResultIdentity
import team.aligner.screening.model.exception.BodyPartNotFoundException

interface ScreeningCommandService {
    fun submit(
        memberId: Long,
        command: SubmitScreeningCommand,
    ): ScreeningResultIdentity
}

/**
 * `@Transactional` 은 **클래스에** 붙인다. kotlin-spring(allopen)이 클래스에 붙은 어노테이션만
 * 보고 open 을 매기기 때문이다. 메서드에만 붙이면 클래스가 final 로 남고 CGLIB 프록시 생성이
 * 실패해 기동이 죽는다 (member 에 같은 주석이 있다).
 */
@Transactional
internal class ScreeningCommandServiceImpl(
    private val screeningResultRepository: ScreeningResultRepository,
    private val bodyPartRepository: BodyPartRepository,
    private val causeRuleRepository: CauseRuleRepository,
) : ScreeningCommandService {
    /**
     * 제출 → 판별 → 저장을 한 트랜잭션에서 끝낸다. 온보딩이 "선택 → 결과 화면" 한 걸음이라
     * 두 번 호출할 이유가 없고, 응답만 저장되고 원인이 비는 중간 상태를 만들지 않는다.
     *
     * 식별자만 돌려준다. 결과 화면이 필요로 하는 원인 이름·설명은 마스터 seed 와의 조인이라
     * Query 쪽 모델이다. 여기서 애그리거트를 그대로 반환하면 api 가 두 모델을 섞어 쓰게 된다.
     */
    override fun submit(
        memberId: Long,
        command: SubmitScreeningCommand,
    ): ScreeningResultIdentity {
        // FK 로도 막히지만 그때는 500 이다. 회원이 없는 부위를 보낸 것은 404 여야 한다.
        if (!bodyPartRepository.existsByCode(command.perceivedBodyPartCode)) {
            throw BodyPartNotFoundException()
        }

        val submitted =
            ScreeningResult.submit(
                memberId = memberId,
                perceivedBodyPartCode = command.perceivedBodyPartCode,
                answers = command.answers,
            )

        val rules = causeRuleRepository.findAllByTargetPoseIds(submitted.answers.map { it.targetPoseId })
        val determined = submitted.determineCauses(rules)

        val saved = screeningResultRepository.save(determined)
        return checkNotNull(saved.identity) { "저장 직후 애그리거트에 식별자가 없다" }
    }
}
