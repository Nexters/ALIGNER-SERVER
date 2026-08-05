package team.aligner.screening.repository.jdbc

import org.springframework.data.repository.CrudRepository

/**
 * Spring Data JDBC 리포지토리. 이 인터페이스는 이 모듈 밖으로 나가지 않는다 —
 * service 가 CrudRepository 를 직접 보면 §3 위반이다.
 */
internal interface ScreeningResultJdbcRepository : CrudRepository<ScreeningResultEntity, Long>
