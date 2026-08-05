package team.aligner.catalog.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import team.aligner.catalog.api.dto.TargetPoseDetailResponse
import team.aligner.catalog.api.dto.TargetPoseSummaryResponse
import team.aligner.catalog.model.TargetPoseIdentity
import team.aligner.catalog.service.TargetPoseQueryService

/**
 * 온보딩 자세 그리드와 자세 상세.
 *
 * 그리드를 클라이언트가 이 API 로 직접 그린다. 덕분에 screening 이 catalog 를 의존하지
 * 않는다 (docs/domains.md §4-2).
 *
 * bodyPartCode 는 screening 소유 어휘를 값으로 받는다. catalog 가 값 집합을 검증하지 않고,
 * 모르는 코드면 빈 목록이 나간다.
 */
@RestController
@RequestMapping("/catalog/target-poses")
class TargetPoseController(
    private val targetPoseQueryService: TargetPoseQueryService,
) {
    @GetMapping
    fun getTargetPoses(
        @RequestParam bodyPartCode: String,
    ): List<TargetPoseSummaryResponse> =
        targetPoseQueryService
            .getAllByBodyPartCode(bodyPartCode)
            .map(TargetPoseSummaryResponse::from)

    @GetMapping("/{targetPoseId}")
    fun getTargetPose(
        @PathVariable targetPoseId: Long,
    ): TargetPoseDetailResponse =
        TargetPoseDetailResponse.from(
            targetPoseQueryService.getDetail(TargetPoseIdentity.of(targetPoseId)),
        )
}
