/**
 * service · adapter-* · adapter-auth 용.
 *
 * Spring Boot **Gradle 플러그인은 적용하지 않는다.** 라이브러리 모듈에 적용하면 bootJar 를
 * 다시 꺼야 하고, 의존성 버전 관리는 BOM(platform)으로 충분하다. 결과적으로 이 계층은
 * 평범한 jar 만 만든다 — 이슈 #3 의 "bootJar 비활성 + jar 활성"과 같은 상태다.
 */

plugins {
    id("aligner.kotlin-lib")
    id("org.jetbrains.kotlin.plugin.spring")
}

dependencies {
    "implementation"(platform(alignerLibs.lib("spring-boot-dependencies")))
    "testImplementation"(platform(alignerLibs.lib("spring-boot-dependencies")))
    "integrationTestImplementation"(platform(alignerLibs.lib("spring-boot-dependencies")))

    "implementation"(alignerLibs.lib("spring-boot-autoconfigure"))
    "implementation"(alignerLibs.lib("spring-tx"))
}
