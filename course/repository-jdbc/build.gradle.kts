plugins {
    id("aligner.repository-jdbc")
}

dependencies {
    // port 구현이 애그리거트와 View 를 주고받는다.
    api(project(":course:model"))
    // 구현체는 전부 internal 이라 밖으로 나가지 않는다 (docs/architecture.md §8).
    implementation(project(":course:infrastructure"))

    // 통합 테스트가 TestContainers PostgreSQL 에 이 도메인 changelog 를 돌려 테이블을 만든다.
    // main 소스셋에서 참조하면 위반이다 (docs/architecture.md §3).
    testImplementation(project(":course:schema"))
}
