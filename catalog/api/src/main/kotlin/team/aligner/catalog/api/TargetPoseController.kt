package team.aligner.catalog.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "카탈로그 — 목표 자세", description = "자세 그리드와 자세 상세")
@RestController
@RequestMapping("/catalog/target-poses")
class TargetPoseController(
    private val targetPoseQueryService: TargetPoseQueryService,
) {
    @Operation(
        summary = "목표 자세 목록",
        description =
            "요약 목록이다. 근육을 싣지 않는다 — 그리드는 이름과 썸네일만 그린다. " +
                "**bodyPartCode 를 생략하면 전체 핀포즈가 나간다.** 온보딩 그리드가 부위를 먼저 묻지 않고 " +
                "전체를 펼쳐 보여주므로 그쪽이 기본이다. " +
                "bodyPartCode 는 screening 이 소유한 어휘이고 catalog 는 값 집합을 검증하지 않는다. " +
                "모르는 코드를 주면 404 가 아니라 빈 배열이 나간다.",
    )
    @ApiResponse(responseCode = "200", description = "조회 성공. 자세가 없으면 빈 배열이다")
    @GetMapping
    fun getTargetPoses(
        @Parameter(description = "부위 코드. 생략하면 전체다. screening 소유 어휘다", example = "NECK", required = false)
        @RequestParam(required = false) bodyPartCode: String?,
    ): List<TargetPoseSummaryResponse> =
        targetPoseQueryService
            .getAll(bodyPartCode)
            .map(TargetPoseSummaryResponse::from)

    @Operation(
        summary = "목표 자세 상세 조회",
        description = "자세 하나와 그 자세가 쓰는 근육을 함께 내린다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "`TARGET_POSE_NOT_FOUND` — 목표 자세를 찾을 수 없습니다",
                content = [Content(mediaType = "application/json", schema = Schema(ref = ERROR_SCHEMA_REF))],
            ),
        ],
    )
    @GetMapping("/{targetPoseId}")
    fun getTargetPose(
        @Parameter(description = "목표 자세 식별자", example = "1")
        @PathVariable targetPoseId: Long,
    ): TargetPoseDetailResponse =
        TargetPoseDetailResponse.from(
            targetPoseQueryService.getDetail(TargetPoseIdentity.of(targetPoseId)),
        )
}
