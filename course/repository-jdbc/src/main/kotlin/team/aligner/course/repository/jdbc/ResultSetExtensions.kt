package team.aligner.course.repository.jdbc

import java.sql.ResultSet

/**
 * ResultSet.getInt 는 NULL 을 0 으로 돌려준다. override 컬럼에서 0 과 "미지정" 이 같은 값이
 * 되면 catalog 기본값으로 메우는 분기가 깨지므로 wasNull 로 구분한다
 * (catalog 의 같은 이름 확장과 같은 이유).
 */
internal fun ResultSet.getIntOrNull(column: String): Int? = getInt(column).takeIf { !wasNull() }
