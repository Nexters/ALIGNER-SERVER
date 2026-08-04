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
            highlightAssetKey = rs.getString("highlight_asset_key"),
            role = MuscleRole.valueOf(rs.getString("role")),
            displayOrder = rs.getInt("display_order"),
        )
}
