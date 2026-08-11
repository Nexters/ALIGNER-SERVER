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
     */
    @PostMapping("/results")
    fun submit(
        @RequestBody request: SubmitScreeningRequest,
    ): ScreeningResultResponse = result()

    @GetMapping("/results/latest")
    fun getLatestResult(): ScreeningResultResponse = result()

    private fun result() =
        ScreeningResultResponse(
            resultId = 10L,
            causes =
                listOf(
                    ScreeningCauseResponse(
                        causeCode = "WEAK_BACK",
                        name = "등 근육 약화",
                        bodyPartCode = "BACK",
                        description = "등과 골반 근육이 약한 것으로 분석돼요",
                        rank = 1,
                        score = 8,
                    ),
                    ScreeningCauseResponse(
                        causeCode = "WEAK_PELVIS",
                        name = "골반 불안정",
                        bodyPartCode = "PELVIS",
                        description = "골반을 잡아주는 근육이 약해요",
                        rank = 2,
                        score = 5,
                    ),
                ),
            createdAt = MockFixtures.NOW,
        )
}
