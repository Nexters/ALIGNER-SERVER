package team.aligner.catalog.repository.jdbc

import org.springframework.jdbc.core.RowMapper
import team.aligner.catalog.model.MuscleRole
import team.aligner.catalog.model.view.MuscleView
import java.sql.ResultSet

/**
 * 자세와 운동이 같은 모양으로 근육을 읽으므로 매핑을 한 곳에 둔다.
 *
 * role 문자열 → MuscleRole 변환은 이 어댑터에서만 일어난다. DB 의 CHECK 제약이 값 집합을
 * 이미 막고 있어 valueOf 가 실패하면 스키마와 코드가 어긋난 것이다.
 *
 * `description` 은 `exercise_muscle` 에만 있는 컬럼이다. 자세 쪽 조회는 문구를 적재하지 않아
 * SQL 에서 NULL 을 같은 이름으로 내보낸다 — 매퍼를 둘로 나누는 것보다 싸다.
 */
internal object MuscleViewRowMapper : RowMapper<MuscleView> {
    override fun mapRow(
        rs: ResultSet,
        rowNum: Int,
    ): MuscleView =
        MuscleView(
            muscleCode = rs.getString("muscle_code"),
            name = rs.getString("name"),
            bodyPartCode = rs.getString("body_part_code"),
            frontHighlightAssetKey = rs.getString("front_highlight_asset_key"),
            backHighlightAssetKey = rs.getString("back_highlight_asset_key"),
            role = MuscleRole.valueOf(rs.getString("role")),
            displayOrder = rs.getInt("display_order"),
            description = rs.getString("description"),
        )
}
