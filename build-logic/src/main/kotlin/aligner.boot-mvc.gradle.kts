/**
 * api · support-web 용. 웹·보안 타입이 들어오는 유일한 계층이다.
 *
 * service 이하는 이 플러그인을 쓰지 않는다. Security 타입이 service 시그니처에
 * 등장하면 안 되기 때문이다 (docs/architecture.md §9).
 */

plugins {
    id("aligner.kotlin-boot")
}

dependencies {
    "implementation"(alignerLibs.lib("spring-boot-starter-webmvc"))
    "implementation"(alignerLibs.lib("spring-boot-starter-security"))
    "implementation"(alignerLibs.lib("springdoc-openapi-webmvc"))

    // starter-webmvc 는 이 둘을 끌고 오지 않는다. 없으면 data class 요청 바디
    // 역직렬화가 실패한다 (기본 생성자가 없어 Jackson 이 파라미터 이름을 못 읽는다).
    "implementation"(alignerLibs.lib("jackson-module-kotlin"))
    "implementation"(alignerLibs.lib("kotlin-reflect"))
}
