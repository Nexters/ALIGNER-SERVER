package team.aligner.course.infrastructure

/**
 * 목표 자세를 읽는 out-port. `course/adapter-catalog` 가 구현한다.
 *
 * 추천에서는 **존재 검증**에, 조회에서는 화면에 그릴 이름·이미지 키에 쓴다.
 * 도메인 간 FK 가 없으므로 존재 확인은 port 로 한다 (docs/domains.md §6).
 */
interface TargetPoseCatalogPort {
    /**
     * 추천의 첫 걸음. 회원이 고른 (강화 부위, 난이도)가 곧 (부위, 레벨)이다
     * (docs/domains.md §4-4).
     */
    fun findByBodyPartCodeAndLevel(
        bodyPartCode: String,
        level: Int,
    ): TargetPoseCatalogEntry?

    /** 목록 화면이 자세 이름·이미지를 붙일 때 쓴다. 자세 수만큼 부르지 않는다. */
    fun findAllByIds(targetPoseIds: List<Long>): List<TargetPoseCatalogEntry>

    /**
     * 자세 전체. 「자세 도전 현황」이 **회원이 시작한 코스가 아니라 서비스가 제공하는 핀포즈
     * 전체**를 펼치므로 목록의 출발점이 여기다. 회원 코스는 그 위에 얹는다.
     */
    fun findAll(): List<TargetPoseCatalogEntry>
}

data class TargetPoseCatalogEntry(
    val targetPoseId: Long,
    val name: String,
    val imageAssetKey: String?,
    /** 영상 포스터 프레임. imageAssetKey 는 프론트 정적 자산의 키이고 이쪽은 URL 이다. */
    val thumbnailUrl: String?,
    val bodyPartCode: String,
    val level: Int,
)
