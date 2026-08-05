package team.aligner.support.web.auth

/**
 * 테스트용 카카오 설정. 실제 값이 아니며 어떤 요청도 카카오로 나가지 않는다
 * (MockRestServiceServer 가 가로챈다).
 *
 * 여기 한곳에 둔 것은 AuthProperties.Kakao 에 필드가 늘 때 테스트를 한 군데만 고치기 위해서다.
 */
internal fun kakaoProperties(
    tokenUri: String = TEST_TOKEN_URI,
    userInfoUri: String = TEST_USER_INFO_URI,
    clientId: String = TEST_CLIENT_ID,
    clientSecret: String = TEST_CLIENT_SECRET,
    redirectUri: String = TEST_REDIRECT_URI,
) = AuthProperties.Kakao(
    tokenUri = tokenUri,
    userInfoUri = userInfoUri,
    clientId = clientId,
    clientSecret = clientSecret,
    redirectUri = redirectUri,
    connectTimeoutMillis = 2000,
    readTimeoutMillis = 3000,
)

internal const val TEST_TOKEN_URI = "https://kauth.kakao.com/oauth/token"
internal const val TEST_USER_INFO_URI = "https://kapi.kakao.com/v2/user/me"
internal const val TEST_CLIENT_ID = "dummy-rest-api-key"
internal const val TEST_CLIENT_SECRET = "dummy-client-secret"
internal const val TEST_REDIRECT_URI = "http://localhost:5173/oauth/kakao"
