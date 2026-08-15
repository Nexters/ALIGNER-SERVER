/**
 * repository-jdbc 용. port 구현(CrudRepository / JdbcClient)이 사는 유일한 계층이다.
 *
 * PostgreSQL 드라이버는 `runtimeOnly` 다. 구현 코드가 드라이버 타입을 직접 import 하면
 * 컴파일이 깨져야 정상이다.
 */

plugins {
    id("aligner.kotlin-boot")
}

dependencies {
    "implementation"(alignerLibs.lib("spring-boot-starter-data-jdbc"))
    "runtimeOnly"(alignerLibs.lib("postgresql"))

    // 통합 테스트는 TestContainers 로 PostgreSQL 을 띄우고
    // 그 도메인 schema 모듈의 changelog 로 테이블을 만든다 (docs/architecture.md §3).
    "integrationTestImplementation"(alignerLibs.lib("spring-boot-starter-test"))
    "integrationTestImplementation"(alignerLibs.lib("spring-boot-testcontainers"))
    // changelog 를 실제로 돌리는 자동설정이 boot-application 에만 있어 통합 테스트에서는
    // 테이블이 만들어지지 않는다. TestContainers 를 여기 둔 것과 같은 이유로 이 계층의 성질이다.
    "integrationTestImplementation"(alignerLibs.lib("spring-boot-starter-liquibase"))
    "integrationTestImplementation"(alignerLibs.lib("testcontainers-postgresql"))
    "integrationTestImplementation"(alignerLibs.lib("testcontainers-junit-jupiter"))
    "integrationTestRuntimeOnly"(alignerLibs.lib("postgresql"))
}
