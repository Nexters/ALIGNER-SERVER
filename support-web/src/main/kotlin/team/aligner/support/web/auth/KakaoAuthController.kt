package team.aligner.support.web.auth

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import team.aligner.support.web.AuthMemberPort
import team.aligner.support.web.KakaoLoginCommand

/**
 * 카카오 로그인 진입점. 이 컨트롤러만 인증 없이 열려 있다 (SecurityConfig).
 *
 * 프론트가 `Kakao.Auth.authorize()` 로 받은 **인가 코드**를 넘기면, 서버가 카카오 인증 서버에서
 * 액세스 토큰으로 교환하고 사용자를 확인한 뒤 회원을 찾거나 만들고 자체 JWT 를 발급한다.
 *
 * **리다이렉트는 서버가 받지 않는다.** 카카오가 프론트 라우트로 인가 코드를 넘기고, 프론트가 그
 * 값을 이 엔드포인트로 전달한다. 서버가 리다이렉트를 받으면 JWT 를 URL·쿠키로 돌려줘야 해서
 * STATELESS 전제와 CORS 설계가 따라온다.
 *
 * 웹에서 클라이언트가 카카오 액세스 토큰을 직접 받는 경로는 **없다.** JavaScript SDK v2 가 보안
 * 권고에 따라 클라이언트 측 토큰 발급 함수를 전부 제거했고, 카카오 문서도 토큰 발급 주체를
 * 서비스 서버로 못박는다 (이슈 #12).
 *
 * **AuthMemberPort 를 요구하는 유일한 Bean 이다.** member:adapter-auth 가 조립에서 빠지면
 * 여기서 기동이 실패해야 정상이다 (docs/architecture.md §9).
 *
 * 문서에서 이 엔드포인트만 Bearer 자물쇠가 없다. OpenApiConfig 가 PublicPaths.LOGIN 을 보고
 * 전역 SecurityRequirement 를 비운다.
 */
@Tag(name = "인증", description = "카카오 인가 코드를 자체 JWT 로 교환한다")
@RestController
class KakaoAuthController(
    private val kakaoUserClient: KakaoUserClient,
    private val authMemberPort: AuthMemberPort,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    @Operation(
        summary = "카카오 로그인",
        description =
            "`Kakao.Auth.authorize()` 로 받은 인가 코드를 넘기면 서버가 액세스 토큰으로 교환하고 사용자를 확인한 뒤 " +
                "회원을 찾거나 만들고 자체 JWT 를 발급한다. **토큰 없이 호출하는 유일한 엔드포인트다.** " +
                "응답의 accessToken 을 Authorize 에 넣고 나머지 API 를 호출한다. " +
                "인가 코드는 1 회용이라 같은 값으로 재시도하면 401 이다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "JWT 발급 성공. 신규 회원이면 이 요청에서 가입까지 끝난다"),
            ApiResponse(
                responseCode = "401",
                description =
                    "`KAKAO_AUTH_CODE_INVALID` — 인가 코드가 무효·만료·재사용됐다. authorize() 부터 다시 태운다 / " +
                        "`KAKAO_TOKEN_INVALID` — 교환한 액세스 토큰이 사용자 조회에서 거부됐다",
                content = [Content(mediaType = "application/json", schema = Schema(ref = ERROR_SCHEMA_REF))],
            ),
            ApiResponse(
                responseCode = "502",
                description = "`KAKAO_UNAVAILABLE` — 카카오 인증 서버에 연결하지 못했습니다",
                content = [Content(mediaType = "application/json", schema = Schema(ref = ERROR_SCHEMA_REF))],
            ),
        ],
    )
    @PostMapping("/auth/kakao")
    fun login(
        @RequestBody request: KakaoLoginRequest,
    ): KakaoLoginResponse {
        val kakaoUser = kakaoUserClient.fetchUserByAuthorizationCode(request.authorizationCode)

        val member =
            authMemberPort.findOrRegisterByKakao(
                KakaoLoginCommand(
                    kakaoId = kakaoUser.kakaoId,
                    nickname = kakaoUser.nickname,
                    profileImageUrl = kakaoUser.profileImageUrl,
                ),
            )

        val issued = jwtTokenProvider.issue(member.memberId)
        return KakaoLoginResponse(
            accessToken = issued.accessToken,
            expiresIn = issued.expiresIn,
        )
    }
}

@Schema(description = "카카오 로그인 요청")
data class KakaoLoginRequest(
    @field:Schema(
        description = "카카오가 리다이렉트 URI 로 넘긴 인가 코드(`code` 쿼리 파라미터). 1 회용이다",
        example = "0X1yZ...q7Rk",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val authorizationCode: String,
)

@Schema(description = "자체 JWT 발급 결과")
data class KakaoLoginResponse(
    @field:Schema(description = "이후 요청의 Authorization: Bearer 에 그대로 넣는 값", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.sig")
    val accessToken: String,
    @field:Schema(
        description = "만료까지 남은 초. 리프레시 토큰이 없어 만료되면 authorize() 부터 다시 태운다",
        example = "1209600",
    )
    val expiresIn: Long,
)

/**
 * 공통 에러 응답 스키마 참조. 실제 컴포넌트는 OpenApiConfig 가 등록한다.
 * 어노테이션 인자는 상수여야 해서 문자열로 둔다.
 */
private const val ERROR_SCHEMA_REF = "#/components/schemas/ApiErrorResponse"
