package team.aligner.api.datasource

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
import org.springframework.transaction.support.TransactionSynchronizationManager
import javax.sql.DataSource

class ReplicationRoutingDataSourceTest {
    private val routingDataSource = ReplicationRoutingDataSource()

    @AfterEach
    fun tearDown() {
        TransactionSynchronizationManager.clear()
    }

    @Test
    fun `트랜잭션이 없으면 primary 로 라우팅된다`() {
        val testMethod = ReplicationRoutingDataSource::class.java.getDeclaredMethod("determineCurrentLookupKey")
        testMethod.isAccessible = true

        val key = testMethod.invoke(routingDataSource)
        key shouldBe ReplicationRoutingDataSource.DATASOURCE_PRIMARY
    }

    @Test
    fun `readOnly = true 트랜잭션에서는 readonly 로 라우팅된다`() {
        TransactionSynchronizationManager.initSynchronization()
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true)

        val testMethod = ReplicationRoutingDataSource::class.java.getDeclaredMethod("determineCurrentLookupKey")
        testMethod.isAccessible = true

        val key = testMethod.invoke(routingDataSource)
        key shouldBe ReplicationRoutingDataSource.DATASOURCE_READONLY
    }

    @Test
    fun `readOnly = false 트랜잭션에서는 primary 로 라우팅된다`() {
        TransactionSynchronizationManager.initSynchronization()
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false)

        val testMethod = ReplicationRoutingDataSource::class.java.getDeclaredMethod("determineCurrentLookupKey")
        testMethod.isAccessible = true

        val key = testMethod.invoke(routingDataSource)
        key shouldBe ReplicationRoutingDataSource.DATASOURCE_PRIMARY
    }

    @Test
    fun `readonly_url 설정이 있을 때 AutoConfiguration 이 DataSource 를 LazyConnectionDataSourceProxy 로 등록한다`() {
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RoutingDataSourceAutoConfiguration::class.java))
            .withPropertyValues(
                "spring.datasource.primary.url=jdbc:postgresql://primary-db:5432/aligner",
                "spring.datasource.primary.username=aligner",
                "spring.datasource.primary.password=secret",
                "spring.datasource.primary.initialization-fail-timeout=-1",
                "spring.datasource.readonly.url=jdbc:postgresql://readonly-db:5432/aligner",
                "spring.datasource.readonly.username=aligner",
                "spring.datasource.readonly.password=secret",
                "spring.datasource.readonly.initialization-fail-timeout=-1",
            ).run { context ->
                val primaryBean = context.getBean(DataSource::class.java)
                primaryBean.shouldBeInstanceOf<LazyConnectionDataSourceProxy>()

                val routingBean = context.getBean("routingDataSource")
                routingBean.shouldBeInstanceOf<ReplicationRoutingDataSource>()
            }
    }
}
