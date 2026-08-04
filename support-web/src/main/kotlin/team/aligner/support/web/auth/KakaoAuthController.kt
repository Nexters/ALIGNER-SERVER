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
 * 클라이언트가 카카오 SDK 로 받은 액세스 토큰을 넘기면, 서버가 카카오에 확인한 뒤 회원을
 * 찾거나 만들고 자체 JWT 를 발급한다. 서버가 인가 코드 리다이렉트를 받는 방식은 쓰지 않는다.
 *
 * **AuthMemberPort 를 요구하는 유일한 Bean 이다.** member:adapter-auth 가 조립에서 빠지면
 * 여기서 기동이 실패해야 정상이다 (docs/architecture.md §9).
 *
 * 문서에서 이 엔드포인트만 Bearer 자물쇠가 없다. OpenApiConfig 가 PublicPaths.LOGIN 을 보고
 * 전역 SecurityRequirement 를 비운다.
 */
@Tag(name = "인증", description = "카카오 액세스 토큰을 자체 JWT 로 교환한다")
@RestController
class KakaoAuthController(
    private val kakaoUserClient: KakaoUserClient,
    private val authMemberPort: AuthMemberPort,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    @Operation(
        summary = "카카오 로그인",
        description =
            "카카오 SDK 로 받은 액세스 토큰을 넘기면 서버가 카카오에 확인한 뒤 회원을 찾거나 만들고 자체 JWT 를 발급한다. " +
                "**토큰 없이 호출하는 유일한 엔드포인트다.** 응답의 accessToken 을 Authorize 에 넣고 나머지 API 를 호출한다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "JWT 발급 성공. 신규 회원이면 이 요청에서 가입까지 끝난다"),
            ApiResponse(
                responseCode = "401",
                description = "`KAKAO_TOKEN_INVALID` — 카카오 액세스 토큰이 유효하지 않습니다",
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
        val kakaoUser = kakaoUserClient.fetchUser(request.kakaoAccessToken)

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
    @field:Schema(description = "카카오 SDK 가 발급한 액세스 토큰", example = "1IuFtM3...ZBHl0A", requiredMode = Schema.RequiredMode.REQUIRED)
    val kakaoAccessToken: String,
)

@Schema(description = "자체 JWT 발급 결과")
data class KakaoLoginResponse(
    @field:Schema(description = "이후 요청의 Authorization: Bearer 에 그대로 넣는 값", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.sig")
    val accessToken: String,
    @field:Schema(description = "만료까지 남은 초. 리프레시 토큰이 없어 만료되면 카카오 로그인부터 다시 한다", example = "1209600")
    val expiresIn: Long,
)

/**
 * 공통 에러 응답 스키마 참조. 실제 컴포넌트는 OpenApiConfig 가 등록한다.
 * 어노테이션 인자는 상수여야 해서 문자열로 둔다.
 */
private const val ERROR_SCHEMA_REF = "#/components/schemas/ApiErrorResponse"
