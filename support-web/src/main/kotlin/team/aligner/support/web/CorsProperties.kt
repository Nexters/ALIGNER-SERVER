package team.aligner.support.web

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 교차 출처 허용 범위. **여기 값을 늘리는 것은 보안 경계 변경이다** (PublicPaths 와 같은 성격).
 *
 * 오리진을 코드에 박지 않는다. 로컬은 프론트 개발 서버 포트, 배포는 배포 도메인이라 환경마다
 * 다르고, 프로파일을 새로 만들지 않기로 했으므로(PublicPaths) 환경변수 하나로 받는다.
 */
@ConfigurationProperties(prefix = "aligner.web.cors")
data class CorsProperties(
    /**
     * 허용할 오리진 목록. 스킴·호스트·포트가 **완전히 일치**해야 한다 —
     * `http://localhost:5173` 과 `http://127.0.0.1:5173` 은 브라우저에게 다른 오리진이다.
     *
     * 비어 있으면 교차 출처 요청이 전부 막힌다. 설정을 빠뜨렸을 때 열리는 쪽이 아니라
     * 닫히는 쪽으로 실패해야 해서 그대로 둔다.
     */
    val allowedOrigins: List<String>,
    /**
     * 브라우저가 preflight 결과를 캐시하는 시간. 0 이면 요청마다 `OPTIONS` 가 한 번 더 붙는다.
     */
    val maxAgeSeconds: Long,
) {
    init {
        // `*` 는 allowCredentials 가 false 라 문법상 통과한다. 그래서 조용히 전체 공개가 되는데,
        // 설정 실수와 의도를 구분할 방법이 없어진다. 기동 시점에 막는다.
        require(allowedOrigins.none { it == "*" }) {
            "aligner.web.cors.allowed-origins 에 * 를 쓸 수 없다. 허용할 오리진을 하나씩 적는다"
        }
    }
}
