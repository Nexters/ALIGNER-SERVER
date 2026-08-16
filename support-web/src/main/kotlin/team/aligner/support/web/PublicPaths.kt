package team.aligner.support.web

/**
 * 인증 없이 열리는 경로. **여기에 값을 추가하는 것은 보안 경계 변경이다.**
 *
 * SecurityConfig 의 permitAll 과 OpenApiConfig 의 문서 표기가 같은 값을 봐야 해서 한곳에 둔다.
 * 두 곳에 따로 적으면 "문서에는 자물쇠가 없는데 실제로는 401" 같은 어긋남이 조용히 생긴다.
 *
 * 문서 경로를 여는 근거는 다음과 같다.
 * - 스펙을 못 읽으면 프론트가 붙을 수 없다. 브라우저 주소창은 Authorization 헤더를 붙이지
 *   못하므로 Swagger UI 경로를 닫으면 화면을 아예 열 수 없다.
 * - 대신 스위치를 하나 둔다. SPRINGDOC_ENABLED=false 면 springdoc 자동설정이 통째로 빠져
 *   핸들러 자체가 없어지고, 열려 있는 경로는 404 만 돌려준다 (application.yml).
 * - 프로파일을 새로 만들지 않는다. MVP 는 단일 배포 서버라 분기할 대상이 없고, 끄고 켜는 판단은
 *   환경변수 하나로 충분하다. 운영에서 닫아야 하면 K8s 가 그 값만 준다.
 * - 헬스체크(/actuator)는 Kubelet 의 생존/준비 판정을 위해 GET 만 허용하며, 외부 접근은
 *   Traefik Gateway API 라우팅 경계에서 원천 차단된다.
 */
internal object PublicPaths {
    /** 토큰을 받으러 오는 경로라 토큰을 요구할 수 없다. */
    const val LOGIN = "/auth/kakao"

    /** OpenAPI 문서(JSON). Swagger UI 가 읽는 swagger-config 도 이 하위 경로다. */
    val API_DOCS = arrayOf("/v3/api-docs", "/v3/api-docs/**")

    /** Swagger UI 진입점과 정적 리소스. */
    val SWAGGER_UI = arrayOf("/swagger-ui.html", "/swagger-ui/**")

    /** 쿠버네티스 헬스체크 프로브 엔드포인트 (/actuator/health/liveness, /actuator/health/readiness). */
    val ACTUATOR = arrayOf("/actuator", "/actuator/**")
}
