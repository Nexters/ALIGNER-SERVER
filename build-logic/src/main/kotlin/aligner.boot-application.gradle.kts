import org.springframework.boot.gradle.tasks.bundling.BootJar

/**
 * application-api 용. **실행 가능한 boot jar 를 만드는 유일한 모듈이다.**
 *
 * 단일 배포 단위 결정의 결과로 이 플러그인을 쓰는 모듈은 프로젝트 전체에 하나뿐이다
 * (docs/architecture.md §1, §3).
 */

plugins {
    id("aligner.boot-mvc")
    id("org.springframework.boot")
}

dependencies {
    // Boot 4 는 auto-configuration 을 기술별 모듈로 쪼갰다. spring-boot-autoconfigure 에는
    // DataSource·Liquibase 자동설정이 없으므로 starter 를 명시해야 한다.
    // 이게 없으면 application.yml 의 spring.datasource / spring.liquibase 가 아무 효과 없이
    // 무시되고, 기동은 성공하는데 마이그레이션만 안 도는 상태가 된다.
    "implementation"(alignerLibs.lib("spring-boot-starter-jdbc"))
    "implementation"(alignerLibs.lib("spring-boot-starter-liquibase"))
    "runtimeOnly"(alignerLibs.lib("postgresql"))
}

tasks.named<BootJar>("bootJar") {
    enabled = true
}

// 조립 모듈은 라이브러리로 소비되지 않는다. 평범한 jar 는 만들지 않는다.
tasks.named<Jar>("jar") {
    enabled = false
}
