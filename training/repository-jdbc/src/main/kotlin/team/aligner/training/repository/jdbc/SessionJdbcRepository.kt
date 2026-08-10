package team.aligner.training.repository.jdbc

import org.springframework.data.repository.CrudRepository

/**
 * @EnableJdbcRepositories 의 basePackageClasses 기준점이다.
 */
internal interface SessionJdbcRepository : CrudRepository<SessionEntity, Long>
