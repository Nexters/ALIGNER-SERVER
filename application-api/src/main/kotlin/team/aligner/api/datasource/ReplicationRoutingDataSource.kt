package team.aligner.api.datasource

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * 트랜잭션의 readOnly 속성에 따라 적절한 데이터소스로 라우팅한다.
 *
 * - @Transactional(readOnly = true) -> DATASOURCE_READONLY ("readonly")
 * - @Transactional(readOnly = false) 또는 트랜잭션 없음 -> DATASOURCE_PRIMARY ("primary")
 */
class ReplicationRoutingDataSource : AbstractRoutingDataSource() {
    companion object {
        const val DATASOURCE_PRIMARY = "primary"
        const val DATASOURCE_READONLY = "readonly"
    }

    override fun determineCurrentLookupKey(): Any {
        val isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly()
        return if (isReadOnly) DATASOURCE_READONLY else DATASOURCE_PRIMARY
    }
}
