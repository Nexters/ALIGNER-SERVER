package team.aligner.support.web.auth

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

/**
 * 카카오 인증 서버와 사용자 API 를 차례로 호출한다.
 *
 * 1. `POST kauth.kakao.com/oauth/token` — 인가 코드를 액세스 토큰으로 바꾼다
 * 2. `GET kapi.kakao.com/v2/user/me` — 그 토큰으로 사용자를 읽는다
 *
 * **1 단계를 서버가 하는 이유**는 REST API 키와 client secret 때문이다. 웹은 번들이 공개되므로
 * 프론트에 두면 그대로 노출된다. 카카오 문서도 토큰 발급 주체를 서비스 서버로 못박는다.
 *
 * 교환 응답의 `refresh_token` 은 **버린다.** MVP 에 리프레시 토큰을 두지 않기로 했고, 저장하면
 * 보관소가 생기면서 SecurityConfig 의 STATELESS 전제가 흔들린다.
 *
 * 응답을 DTO 가 아니라 Map 으로 읽는다. 필요한 값이 몇 개뿐이고, 카카오가 필드를 추가해도
 * 역직렬화가 깨지지 않는다.
 */
internal class RestClientKakaoUserClient(
    private val properties: AuthProperties,
    private val restClient: RestClient,
) : KakaoUserClient {
    override fun fetchUserByAuthorizationCode(authorizationCode: String): KakaoUser {
        val kakaoAccessToken = exchangeCodeForAccessToken(authorizationCode)
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

    /**
     * `redirect_uri` 를 우리 설정에서 싣는다. 프론트가 `authorize()` 에 넘긴 값과 **완전히 일치**해야
     * 카카오가 받아준다. 프론트가 보낸 값을 그대로 되쓰지 않는 것은, 그러면 등록되지 않은 URI 로도
     * 교환을 시도하게 되어 검증 지점이 카카오 콘솔 바깥으로 새기 때문이다.
     */
    private fun exchangeCodeForAccessToken(authorizationCode: String): String {
        val form =
            LinkedMultiValueMap<String, String>().apply {
                add(GRANT_TYPE_PARAM, AUTHORIZATION_CODE_GRANT)
                add(CLIENT_ID_PARAM, properties.kakao.clientId)
                add(REDIRECT_URI_PARAM, properties.kakao.redirectUri)
                add(CODE_PARAM, authorizationCode)
                add(CLIENT_SECRET_PARAM, properties.kakao.clientSecret)
            }

        val body =
            callKakao {
                restClient
                    .post()
                    .uri(properties.kakao.tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    // 400 은 파라미터 문제, 401 은 앱키 또는 인가 코드 문제다. 어느 쪽이든 프론트는
                    // authorize() 부터 다시 태워야 하므로 같은 코드로 접는다.
                    .onStatus({ it.value() == BAD_REQUEST_STATUS || it.value() == UNAUTHORIZED_STATUS }) { _, _ ->
                        throw AuthenticationFailedException(AuthErrorCode.KAKAO_AUTH_CODE_INVALID)
                    }.body(object : ParameterizedTypeReference<Map<String, Any?>>() {})
            }

        // 200 인데 access_token 이 없으면 카카오 응답 형태가 우리 가정과 다른 것이다.
        // 인가 코드 탓으로 돌리면 프론트가 멀쩡한 코드로 재로그인을 반복한다.
        return body[ACCESS_TOKEN_FIELD] as? String
            ?: throw AuthenticationFailedException(AuthErrorCode.KAKAO_UNAVAILABLE)
    }

    private fun requestUserInfo(kakaoAccessToken: String): Map<String, Any?> =
        callKakao {
            restClient
                .get()
                .uri(properties.kakao.userInfoUri)
                .header(HttpHeaders.AUTHORIZATION, "$BEARER_PREFIX$kakaoAccessToken")
                .retrieve()
                .onStatus({ it.value() == UNAUTHORIZED_STATUS }) { _, _ ->
                    throw AuthenticationFailedException(AuthErrorCode.KAKAO_TOKEN_INVALID)
                }.body(object : ParameterizedTypeReference<Map<String, Any?>>() {})
        }

    /**
     * 타임아웃·연결 실패·5xx 를 전부 KAKAO_UNAVAILABLE 로 접는다. 카카오가 죽으면 로그인이 안 된다.
     * onStatus 가 던진 AuthenticationFailedException 은 그대로 통과시켜야 401 이 502 로 바뀌지 않는다.
     */
    private fun callKakao(request: () -> Map<String, Any?>?): Map<String, Any?> =
        try {
            request() ?: throw AuthenticationFailedException(AuthErrorCode.KAKAO_UNAVAILABLE)
        } catch (exception: AuthenticationFailedException) {
            throw exception
        } catch (exception: RuntimeException) {
            throw AuthenticationFailedException(AuthErrorCode.KAKAO_UNAVAILABLE, exception)
        }

    private companion object {
        const val BEARER_PREFIX = "Bearer "
        const val BAD_REQUEST_STATUS = 400
        const val UNAUTHORIZED_STATUS = 401

        const val GRANT_TYPE_PARAM = "grant_type"
        const val AUTHORIZATION_CODE_GRANT = "authorization_code"
        const val CLIENT_ID_PARAM = "client_id"
        const val REDIRECT_URI_PARAM = "redirect_uri"
        const val CODE_PARAM = "code"
        const val CLIENT_SECRET_PARAM = "client_secret"
        const val ACCESS_TOKEN_FIELD = "access_token"

        const val KAKAO_ID_FIELD = "id"
        const val KAKAO_ACCOUNT_FIELD = "kakao_account"
        const val PROFILE_FIELD = "profile"
        const val NICKNAME_FIELD = "nickname"
        const val PROFILE_IMAGE_URL_FIELD = "profile_image_url"
    }
}
