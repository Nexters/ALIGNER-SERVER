package team.aligner.api.datasource

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 읽기/쓰기 분리 데이터소스 속성 바인딩.
 *
 * spring.datasource:
 *   url: ...
 *   username: ...
 *   password: ...
 *   primary:
 *     url: ...
 *     maximum-pool-size: 10
 *   readonly:
 *     url: ...
 *     maximum-pool-size: 20
 */
@ConfigurationProperties(prefix = "spring.datasource")
data class RoutingDataSourceProperties(
    var url: String? = null,
    var username: String? = null,
    var password: String? = null,
    var driverClassName: String? = null,
    var primary: PoolProperties = PoolProperties(poolName = "HikariPool-Primary", maximumPoolSize = 10),
    var readonly: PoolProperties = PoolProperties(poolName = "HikariPool-Replica", maximumPoolSize = 20),
) {
    data class PoolProperties(
        var url: String? = null,
        var username: String? = null,
        var password: String? = null,
        var driverClassName: String? = null,
        var maximumPoolSize: Int = 10,
        var minimumIdle: Int? = null,
        var initializationFailTimeout: Long? = null,
        var poolName: String? = null,
    )
}
