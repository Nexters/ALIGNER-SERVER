package team.aligner.catalog.api

/**
 * 공통 에러 응답 스키마 참조. 실제 컴포넌트는 support-web 의 OpenApiConfig 가 등록한다.
 *
 * catalog:api 는 support-web 을 의존하지 않으므로(§9 — 이 도메인은 회원 식별자를 쓰지 않는다)
 * 상수를 공유할 수 없다. 어노테이션 인자는 상수여야 해서 문자열을 여기 한 번만 적는다.
 */
internal const val ERROR_SCHEMA_REF = "#/components/schemas/ApiErrorResponse"
