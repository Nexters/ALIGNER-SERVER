plugins {
    id("aligner.kotlin-boot")
}

dependencies {
    // 서비스 시그니처가 Course 와 View 를 반환한다 (docs/architecture.md §8).
    api(project(":course:model"))
    // 계약 구현체를 Bean 으로 노출하므로 계약 타입이 공개 시그니처에 등장한다.
    api(project(":course:contract"))
    // port 는 internal 구현체의 생성자 인자일 뿐이라 밖으로 나가지 않는다 (§8).
    implementation(project(":course:infrastructure"))

    // 다른 도메인의 contract 를 여기서 의존하지 않는다. 연결은 adapter-* 가 port 를 구현해
    // 이룬다 (docs/architecture.md §7).
}
