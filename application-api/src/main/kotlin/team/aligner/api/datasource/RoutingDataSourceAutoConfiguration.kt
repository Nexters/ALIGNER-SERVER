package team.aligner.api.datasource

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
import team.aligner.api.datasource.ReplicationRoutingDataSource.Companion.DATASOURCE_PRIMARY
import team.aligner.api.datasource.ReplicationRoutingDataSource.Companion.DATASOURCE_READONLY
import javax.sql.DataSource

/**
 * 읽기/쓰기 복제 분기(RoutingDataSource) 자동 설정.
 *
 * spring.datasource.readonly.url 설정이 존재할 때 활성화되며,
 * LazyConnectionDataSourceProxy 를 @Primary DataSource 로 등록하여
 * 실제 첫 쿼리 실행 시점까지 커넥션 획득을 지연시키고 올바른 노드로 라우팅한다.
 */
@AutoConfiguration(beforeName = ["org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"])
@ConditionalOnClass(HikariDataSource::class)
@ConditionalOnProperty(name = ["spring.datasource.readonly.url"])
@EnableConfigurationProperties(RoutingDataSourceProperties::class)
class RoutingDataSourceAutoConfiguration {
    @Bean
    fun primaryDataSource(properties: RoutingDataSourceProperties): DataSource =
        createHikariDataSource(
            poolProps = properties.primary,
            fallbackProps = properties,
            defaultPoolName = "HikariPool-Primary",
        )

    @Bean
    fun readonlyDataSource(properties: RoutingDataSourceProperties): DataSource =
        createHikariDataSource(
            poolProps = properties.readonly,
            fallbackProps = properties,
            defaultPoolName = "HikariPool-Replica",
        )

    @Bean(name = ["routingDataSource"])
    fun routingDataSource(
        @Qualifier("primaryDataSource") primaryDataSource: DataSource,
        @Qualifier("readonlyDataSource") readonlyDataSource: DataSource,
    ): DataSource {
        val routingDataSource = ReplicationRoutingDataSource()
        val targetDataSources =
            mapOf<Any, Any>(
                DATASOURCE_PRIMARY to primaryDataSource,
                DATASOURCE_READONLY to readonlyDataSource,
            )
        routingDataSource.setTargetDataSources(targetDataSources)
        routingDataSource.setDefaultTargetDataSource(primaryDataSource)
        return routingDataSource
    }

    @Bean
    @Primary
    fun dataSource(
        @Qualifier("routingDataSource") routingDataSource: DataSource,
    ): DataSource = LazyConnectionDataSourceProxy(routingDataSource)

    private fun createHikariDataSource(
        poolProps: RoutingDataSourceProperties.PoolProperties,
        fallbackProps: RoutingDataSourceProperties,
        defaultPoolName: String,
    ): HikariDataSource {
        val config =
            HikariConfig().apply {
                jdbcUrl = poolProps.url ?: fallbackProps.url
                username = poolProps.username ?: fallbackProps.username
                password = poolProps.password ?: fallbackProps.password
                (poolProps.driverClassName ?: fallbackProps.driverClassName)?.let { driverClassName = it }
                maximumPoolSize = poolProps.maximumPoolSize
                poolProps.minimumIdle?.let { minimumIdle = it }
                poolProps.initializationFailTimeout?.let { initializationFailTimeout = it }
                poolName = poolProps.poolName ?: defaultPoolName
            }
        return HikariDataSource(config)
    }
}
