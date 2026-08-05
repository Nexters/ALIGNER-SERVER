package team.aligner.catalog.repository.jdbc

import java.sql.ResultSet

/**
 * nullable 정수 컬럼을 읽는다.
 *
 * `getObject(column) as Int?` 를 쓰지 않는 이유는 unchecked cast 이기 때문이다. JDBC 스펙상
 * SMALLINT·INTEGER 는 getObject 가 java.lang.Integer 를 돌려주므로 지금은 맞지만, 컬럼을
 * BIGINT 로 넓히는 순간 Long 이 와서 **컴파일은 통과한 채 런타임에 ClassCastException** 이
 * 된다. default_set_count 처럼 감수 데이터에 따라 범위가 바뀔 수 있는 컬럼이 있어 실제 위험이다.
 *
 * getInt 는 모든 숫자 타입에 대해 스펙이 보장하고 컬럼 타입이 넓어져도 깨지지 않는다.
 * SQL NULL 은 0 으로 오므로 wasNull 로 걸러야 한다.
 */
internal fun ResultSet.getIntOrNull(column: String): Int? = getInt(column).takeIf { !wasNull() }
