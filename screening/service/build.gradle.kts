plugins {
    id("aligner.kotlin-boot")
}

dependencies {
    // 서비스 시그니처가 ScreeningResult 와 View 를 반환한다 (docs/architecture.md §8).
    api(project(":screening:model"))
    // 계약 구현체를 Bean 으로 노출하므로 계약 타입이 공개 시그니처에 등장한다.
    api(project(":screening:contract"))
    // port 는 internal 구현체의 생성자 인자일 뿐이라 밖으로 나가지 않는다
    // (docs/architecture.md §8).
    implementation(project(":screening:infrastructure"))

    // repository-jdbc 를 넣지 않는다. service → CrudRepository·JdbcClient 직접 참조는
    // §3 위반이다. 저장은 infrastructure 의 port 로만 한다.
}
