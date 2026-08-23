package team.aligner.mock

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import team.aligner.screening.api.dto.BodyPartResponse
import team.aligner.screening.api.dto.ScreeningCauseResponse
import team.aligner.screening.api.dto.ScreeningResultResponse
import team.aligner.screening.api.dto.SubmitScreeningRequest
import team.aligner.screening.model.ScreeningResult

/**
 * 진단은 항상 같은 결과를 낸다 — **등·골반이 약한 회원**이다.
 *
 * 와이어프레임의 "홍길동님은 등, 골반 근육이 약한 것으로 분석돼요" 화면에 대응한다.
 * 강화 부위 선택 화면에서 고를 수 있는 부위가 이 결과에 들어 있어야 코스 처방이 이어진다.
 */
@RestController
@RequestMapping("/screening")
internal class MockScreeningController {
    @GetMapping("/body-parts")
    fun getBodyParts(): List<BodyPartResponse> =
        MockFixtures.BODY_PARTS.map { (code, name) -> BodyPartResponse(bodyPartCode = code, name = name) }

    /**
     * 제출한 자세와 무관하게 같은 원인을 돌려준다. 저장하지 않는다.
     *
     * 다만 **검증은 실제와 같이 태운다.** `toAnswers()` 를 지나 애그리거트 규칙까지 확인하므로
     * 빈 응답·중복 자세·체감별 4 개 초과가 실제 서버와 같은 400 이 된다.
     */
    @PostMapping("/results")
    fun submit(
        @RequestBody request: SubmitScreeningRequest,
    ): ScreeningResultResponse {
        ScreeningResult.submit(memberId = MockFixtures.MEMBER_ID, answers = request.toAnswers())
        return result()
    }

    @GetMapping("/results/latest")
    fun getLatestResult(): ScreeningResultResponse = result()

    private fun result() =
        ScreeningResultResponse(
            resultId = 10L,
            causes =
                MockFixtures.CAUSES.map {
                    ScreeningCauseResponse(
                        causeCode = it.code,
                        name = it.name,
                        bodyPartCode = it.bodyPartCode,
                        description = it.description,
                        rank = it.rank,
                        score = it.score,
                    )
                },
            createdAt = MockFixtures.NOW,
        )
}
