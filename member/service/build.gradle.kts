plugins {
    id("aligner.kotlin-boot")
}

dependencies {
    // 공개 서비스 인터페이스가 Member·MemberIdentity·MemberProfileView 를 반환한다.
    api(project(":member:model"))
    // MemberAuthContract 구현체를 Bean 으로 노출하므로 계약 타입이 공개 시그니처에 등장한다.
    api(project(":member:contract"))
    // port 는 internal 구현체의 생성자 인자일 뿐이라 밖으로 나가지 않는다
    // (docs/architecture.md §8).
    implementation(project(":member:infrastructure"))
}
