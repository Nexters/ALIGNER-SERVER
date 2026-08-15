package team.aligner.training.infrastructure

import team.aligner.training.model.Session
import team.aligner.training.model.SessionIdentity

/**
 * 쓰기 out-port. 애그리거트 단위로만 오간다 (docs/architecture.md §4).
 */
interface SessionRepository {
    fun save(session: Session): Session

    fun findByIdentity(sessionIdentity: SessionIdentity): Session?
}
