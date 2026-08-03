package team.aligner.support.web.auth

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

/**
 * 카카오 사용자 조회 API 를 호출한다.
 *
 * 응답을 DTO 가 아니라 Map 으로 읽는다. 필요한 값이 세 개뿐이고, 카카오가 필드를 추가해도
 * 역직렬화가 깨지지 않는다.
 *
 * ⚠️ 엔드포인트와 응답 필드 경로는 카카오 개발자 문서로 확인이 필요하다 (이슈 #5, PR #6 미검증 항목).
 * 값이 다르면 아래 companion object 의 상수만 고치면 된다.
 */
internal class RestClientKakaoUserClient(
    private val properties: AuthProperties,
) : KakaoUserClient {
    private val restClient: RestClient =
        RestClient
            .builder()
            .requestFactory(
                SimpleClientHttpRequestFactory().apply {
                    setConnectTimeout(Duration.ofMillis(properties.kakao.connectTimeoutMillis))
                    setReadTimeout(Duration.ofMillis(properties.kakao.readTimeoutMillis))
                },
            ).build()

    override fun fetchUser(kakaoAccessToken: String): KakaoUser {
        val body = requestUserInfo(kakaoAccessToken)

        // id 는 카카오 회원번호다. 숫자로 오지만 자릿수가 커질 수 있어 문자열로 보관한다.
        //
        // 200 을 받았는데 id 가 없으면 토큰이 아니라 우리 필드 경로가 틀린 것이다. 401 로 내보내면
        // 클라이언트가 "토큰이 만료됐나" 하고 원인을 반대로 짚는다.
        val kakaoId =
            body[KAKAO_ID_FIELD]?.toString()
                ?: throw AuthenticationFailedException(AuthErrorCode.KAKAO_UNAVAILABLE)

        val profile =
            (body[KAKAO_ACCOUNT_FIELD] as? Map<*, *>)
                ?.get(PROFILE_FIELD) as? Map<*, *>

        return KakaoUser(
            kakaoId = kakaoId,
            nickname = profile?.get(NICKNAME_FIELD) as? String,
            profileImageUrl = profile?.get(PROFILE_IMAGE_URL_FIELD) as? String,
        )
    }

    private fun requestUserInfo(kakaoAccessToken: String): Map<String, Any?> =
        try {
            restClient
                .get()
                .uri(properties.kakao.userInfoUri)
                .header(HttpHeaders.AUTHORIZATION, "$BEARER_PREFIX$kakaoAccessToken")
                .retrieve()
                .onStatus({ it.value() == UNAUTHORIZED_STATUS }) { _, _ ->
                    throw AuthenticationFailedException(AuthErrorCode.KAKAO_TOKEN_INVALID)
                }.body(object : ParameterizedTypeReference<Map<String, Any?>>() {})
                ?: throw AuthenticationFailedException(AuthErrorCode.KAKAO_UNAVAILABLE)
        } catch (exception: AuthenticationFailedException) {
            throw exception
        } catch (exception: RuntimeException) {
            // 타임아웃·연결 실패·5xx 를 전부 여기로 접는다. 카카오가 죽으면 로그인이 안 된다.
            throw AuthenticationFailedException(AuthErrorCode.KAKAO_UNAVAILABLE, exception)
        }

    private companion object {
        const val BEARER_PREFIX = "Bearer "
        const val UNAUTHORIZED_STATUS = 401
        const val KAKAO_ID_FIELD = "id"
        const val KAKAO_ACCOUNT_FIELD = "kakao_account"
        const val PROFILE_FIELD = "profile"
        const val NICKNAME_FIELD = "nickname"
        const val PROFILE_IMAGE_URL_FIELD = "profile_image_url"
    }
}
