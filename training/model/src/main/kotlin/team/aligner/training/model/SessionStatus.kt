package team.aligner.training.model

/**
 * 세션 진행 상태.
 *
 * 감수 데이터가 아니라 닫힌 구조 어휘이므로 코드에 둔다. DDL 의 `ck_session_status` 가 같은
 * 값 집합을 강제한다.
 *
 * 중단(ABANDONED)을 두지 않는다. 화면에 중단 동작이 없고, 그만둔 세션은 IN_PROGRESS 로
 * 남는다. 필요해지면 그때 값을 늘린다 (docs/architecture.md §3 "미리 만들지 않는다").
 */
enum class SessionStatus {
    IN_PROGRESS,
    COMPLETED,
}
