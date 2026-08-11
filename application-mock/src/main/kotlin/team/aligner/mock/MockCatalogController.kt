package team.aligner.mock

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import team.aligner.catalog.api.dto.ExerciseDetailResponse
import team.aligner.catalog.api.dto.MuscleResponse
import team.aligner.catalog.api.dto.TargetPoseDetailResponse
import team.aligner.catalog.api.dto.TargetPoseSummaryResponse
import team.aligner.catalog.api.dto.VoiceCueResponse
import java.math.BigDecimal

/**
 * 콘텐츠 마스터. 온보딩 그리드와 운동 가이드가 읽는다.
 *
 * **없는 식별자에는 실제 서버와 같은 404 를 낸다.** 프론트가 오류 경로도 만들 수 있어야 한다.
 */
@RestController
internal class MockCatalogController {
    @GetMapping("/catalog/target-poses")
    fun getTargetPoses(
        @RequestParam(required = false) bodyPartCode: String?,
    ): List<TargetPoseSummaryResponse> =
        MockFixtures.TARGET_POSES
            .filter { bodyPartCode == null || it.bodyPartCode == bodyPartCode }
            .map {
                TargetPoseSummaryResponse(
                    targetPoseId = it.id,
                    name = it.name,
                    imageAssetKey = it.assetKey,
                    bodyPartCode = it.bodyPartCode,
                    level = it.level,
                )
            }

    @GetMapping("/catalog/target-poses/{targetPoseId}")
    fun getTargetPose(
        @PathVariable targetPoseId: Long,
    ): TargetPoseDetailResponse {
        val pose =
            MockFixtures.TARGET_POSES.find { it.id == targetPoseId }
                ?: throw MockNotFoundException("TARGET_POSE_NOT_FOUND", "목표 자세를 찾을 수 없습니다")
        return TargetPoseDetailResponse(
            targetPoseId = pose.id,
            name = pose.name,
            imageAssetKey = pose.assetKey,
            bodyPartCode = pose.bodyPartCode,
            level = pose.level,
            muscles = muscles(),
        )
    }

    /**
     * 재생 URL 과 썸네일이 없다. YMove 연동(`catalog/adapter-ymove`)이 아직 없어서이고,
     * **실제 서버도 지금은 같다.**
     */
    @GetMapping("/catalog/exercises/{exerciseId}")
    fun getExercise(
        @PathVariable exerciseId: Long,
    ): ExerciseDetailResponse {
        val exercise =
            MockFixtures.EXERCISES.find { it.id == exerciseId }
                ?: throw MockNotFoundException("EXERCISE_NOT_FOUND", "운동을 찾을 수 없습니다")
        return ExerciseDetailResponse(
            exerciseId = exercise.id,
            name = exercise.name,
            defaultSetCount = exercise.setCount,
            defaultRepCount = null,
            defaultDurationSeconds = exercise.durationSeconds,
            metValue = BigDecimal("3.00"),
            difficulty = "하",
            category = exercise.category,
            cautionNote = "목을 뒤로 완전히 젖히지 마세요. 허리에 날카로운 통증이 오면 즉시 중단하세요.",
            muscles = muscles(),
            voiceCues =
                listOf(
                    VoiceCueResponse(1, null, null, "무릎을 골반 너비로 벌리고 손은 어깨 아래에 둡니다"),
                    VoiceCueResponse(2, 35, 75, "명치를 천장을 향해 높게 끌어올리세요"),
                ),
        )
    }

    private fun muscles() =
        MockFixtures.MUSCLES.map {
            MuscleResponse(
                muscleCode = it.code,
                name = it.name,
                bodyPartCode = it.bodyPartCode,
                frontHighlightAssetKey = it.frontAssetKey,
                backHighlightAssetKey = it.backAssetKey,
                role = it.role,
                displayOrder = it.displayOrder,
            )
        }
}
