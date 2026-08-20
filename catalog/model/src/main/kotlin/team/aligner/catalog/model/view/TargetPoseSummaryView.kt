package team.aligner.catalog.model.view

/**
 * 온보딩 자세 그리드에 쓰는 목표 자세 요약.
 *
 * 자세 그리드는 클라이언트가 catalog API 로 직접 그린다 (docs/domains.md §4-2).
 * 덕분에 screening 이 catalog 를 의존하지 않는다.
 *
 * 근육을 싣지 않는다. 그리드는 이름과 썸네일만 그린다.
 *
 * imageAssetKey 는 URL 이 아니라 안정된 키다. 파일은 프론트가 정적으로 갖는다 (§4-3).
 */
data class TargetPoseSummaryView(
    val targetPoseId: Long,
    val name: String,
    val imageAssetKey: String?,
    /**
     * 영상 포스터 프레임. [imageAssetKey] 와 자리가 다르다 — 그림은 프론트 정적 자산의 키이고
     * 이쪽은 YMove 자산의 URL 이다. 재생 URL 과 달리 만료가 없어 저장돼 있다.
     */
    val thumbnailUrl: String?,
    /**
     * 같은 자세의 catalog.exercise 행 식별자. **targetPoseId 와 다른 값이다** — 핀포즈는
     * 코스 스텝으로 재생되면서 동시에 코스의 목표라 두 테이블에 각각 행을 갖는다
     * (ddl/002-create-target-pose.sql). 영상·음성 큐·MET 은 exercise 쪽에만 있으므로
     * 재생이 필요한 화면은 이 값을 쓴다.
     *
     * 두 행은 ymove_slug 로 잇는다. slug 가 없거나 짝이 없으면 null 이다.
     */
    val exerciseId: Long?,
    val bodyPartCode: String,
    val level: Int,
)
