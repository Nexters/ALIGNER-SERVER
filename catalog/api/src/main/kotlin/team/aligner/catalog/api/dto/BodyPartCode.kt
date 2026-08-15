package team.aligner.catalog.api.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 부위. 값 집합의 정본은 `screening.body_part` 테이블이고 이 enum 은 그 어휘를 API 경계에서
 * 못박은 사본이다.
 *
 * **도메인마다 따로 정의한다.** 도메인 간 직접 참조가 금지고(docs/architecture.md §7),
 * `support-core`·`support-web` 에는 도메인 모델을 둘 수 없다(§9). 그래서 catalog·course·
 * member·screening 의 `api` 모듈이 값이 같은 enum 을 각자 갖는다. **네 정의가 완전히 같아야
 * OpenAPI 스키마가 하나로 합쳐지므로, 값을 바꿀 때는 네 곳을 함께 바꾼다.**
 *
 * `service` 이하는 `String` 을 그대로 쓴다. 변환은 `api` 경계에서만 한다 — 어휘 소유자는
 * 여전히 screening 이고, 여기서 막는 것은 "프론트가 못 볼 값이 들어오는 것" 뿐이다.
 */
@Schema(description = "부위 코드", example = "BACK")
enum class BodyPartCode {
    /** 등 */
    BACK,

    /** 복부 */
    ABDOMEN,

    /** 골반 */
    PELVIS,
    ;

    companion object {
        /**
         * 저장된 값을 응답 타입으로 되돌린다.
         *
         * 세 값 밖의 코드가 DB 에 있으면 데이터 문제라 조용히 넘기지 않는다. null 로 떨어뜨리면
         * 화면에서 부위가 사라지는데 프론트도 서버도 원인을 짚을 단서가 없다.
         */
        fun from(code: String): BodyPartCode =
            entries.firstOrNull { it.name == code }
                ?: throw IllegalStateException("알 수 없는 부위 코드: $code")
    }
}
