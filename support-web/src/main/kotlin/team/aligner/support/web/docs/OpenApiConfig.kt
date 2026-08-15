package team.aligner.support.web.docs

import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import team.aligner.support.core.CommonErrorCode
import team.aligner.support.core.ErrorCode
import team.aligner.support.web.ApiErrorResponse
import team.aligner.support.web.PublicPaths

/**
 * OpenAPI 문서의 메타데이터·인증 스킴·공통 에러 응답.
 *
 * 문서 설정을 `support-web` 에 두는 이유는 SecurityConfig 와 같다. 인증 방식과 에러 포맷은
 * 어느 한 도메인의 것이 아니면서 모든 도메인 `api` 가 똑같이 필요로 한다
 * (docs/architecture.md §9). `application-api` 에 두면 도메인 `api` 가 조립 모듈을 역참조하는
 * 모양이 된다.
 *
 * ComponentScan 이 없으므로 여기 Bean 도 명시 등록이고, FQCN 이 AutoConfiguration.imports 에
 * 있어야 한다 (§5).
 *
 * `@ConditionalOnProperty` 는 springdoc 의 SpringDocConfiguration 과 같은 조건이다.
 * 문서를 끈 환경에서 소비자 없는 Bean 이 뜨지 않게 맞춰둔다.
 */
@AutoConfiguration
@ConditionalOnProperty(name = ["springdoc.api-docs.enabled"], matchIfMissing = true)
class OpenApiConfig {
    /**
     * 전역 SecurityRequirement 를 걸어 모든 엔드포인트에 Bearer 자물쇠를 표시한다.
     * SecurityConfig 의 기본값이 `anyRequest().authenticated()` 이므로 문서도 같은 기본값을 쓴다.
     * 예외인 로그인 경로만 아래 커스터마이저가 비운다.
     */
    @Bean
    fun alignerOpenApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Aligner API")
                    .version("v1")
                    .description(
                        """
                        코치 없이 요가하는 사용자에게 원인 부위를 짚어 보강 코스와 목표 자세를 추천하는 서버 API.

                        **인증 절차**
                        1. 클라이언트가 카카오 SDK 로 액세스 토큰을 받는다.
                        2. `POST /auth/kakao` 에 그 토큰을 넘기면 서버가 확인하고 자체 JWT 를 발급한다.
                        3. 응답의 `accessToken` 을 오른쪽 위 **Authorize** 에 넣으면 이후 요청에
                           `Authorization: Bearer <JWT>` 가 붙는다.

                        리프레시 토큰이 없다. 만료되면 1번부터 다시 한다.
                        """.trimIndent(),
                    ),
            ).components(
                Components().addSecuritySchemes(
                    BEARER_SCHEME,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("POST /auth/kakao 응답의 accessToken 을 그대로 넣는다. `Bearer ` 는 Swagger UI 가 붙인다."),
                ),
            ).addSecurityItem(SecurityRequirement().addList(BEARER_SCHEME))

    /**
     * 모든 엔드포인트가 똑같이 낼 수 있는 실패만 여기서 한 번에 붙인다 — 인증 실패와 서버 오류다.
     * 둘 다 SecurityConfig 와 GlobalExceptionHandler 가 도메인과 무관하게 만들어내므로
     * 컨트롤러마다 `@ApiResponse` 를 반복해 적을 이유가 없다.
     *
     * 도메인이 정하는 실패(404·400 같은 것)는 반대다. 그건 해당 컨트롤러에 `@ApiResponse` 로
     * 적는다. 여기에 경로별 분기를 넣는 순간 `support-web` 이 도메인을 알게 된다 (§9).
     */
    @Bean
    fun commonErrorResponseCustomizer(): OpenApiCustomizer =
        OpenApiCustomizer { openApi ->
            val errorSchema = ModelConverters.getInstance().readAllAsResolvedSchema(ApiErrorResponse::class.java).schema
            // ModelConverters 를 직접 부르면 springdoc 의 후처리를 타지 않아 type 이 비어 나온다.
            // 다른 스키마는 전부 "object" 로 나가므로 여기만 다르면 클라이언트 코드 생성기가 걸린다.
            // 3.0 은 type, 3.1 은 types 를 직렬화하므로 둘 다 채운다.
            errorSchema.type = "object"
            errorSchema.addType("object")
            openApi.components.addSchemas(ERROR_SCHEMA_NAME, errorSchema)

            openApi.paths.orEmpty().forEach { (path, pathItem) ->
                pathItem.readOperations().forEach { operation ->
                    if (path == PublicPaths.LOGIN) {
                        // 전역 요구사항을 이 엔드포인트에서만 비운다. 토큰을 받으러 오는 경로라
                        // 토큰을 요구할 수 없다 — SecurityConfig 의 permitAll 과 같은 판단이다.
                        operation.security = emptyList()
                    } else {
                        operation.addErrorResponse(CommonErrorCode.UNAUTHORIZED)
                    }
                    operation.addErrorResponse(CommonErrorCode.INTERNAL_ERROR)
                }
            }
        }

    private fun Operation.addErrorResponse(errorCode: ErrorCode) {
        val existing = responses ?: ApiResponses().also { responses = it }
        existing.addApiResponse(
            errorCode.status.toString(),
            ApiResponse()
                .description("`${errorCode.code}` — ${errorCode.message}")
                .content(
                    Content().addMediaType(
                        org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                        MediaType().schema(Schema<Any>().`$ref`(ERROR_SCHEMA_REF)),
                    ),
                ),
        )
    }

    companion object {
        private const val BEARER_SCHEME = "bearerAuth"

        /**
         * 도메인 `api` 모듈의 `@ApiResponse` 도 이 이름을 `$ref` 로 가리킨다.
         * `catalog:api` 는 `support-web` 을 의존하지 않아 상수를 공유할 수 없으므로,
         * 그쪽에는 같은 문자열이 리터럴로 적혀 있다.
         */
        private const val ERROR_SCHEMA_NAME = "ApiErrorResponse"
        private const val ERROR_SCHEMA_REF = "#/components/schemas/$ERROR_SCHEMA_NAME"
    }
}
