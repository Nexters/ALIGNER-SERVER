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
}
