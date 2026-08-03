plugins {
    id("aligner.kotlin-boot")
}

dependencies {
    // 구현할 AuthMemberPort 도, 연결할 MemberAuthContract 도 internal 구현체 안에서만 쓰인다
    // (docs/architecture.md §8).
    //
    // 이 모듈은 boot-mvc 가 아니라 kotlin-boot 다. support-web 은 api(support-core) 만
    // 공개하고 웹·보안 라이브러리는 implementation 이라 전이되지 않는다. AuthMemberPort 가
    // Spring 타입을 쓰지 않아 이 조합이 성립한다 — §9 설계가 의도한 결과다.
    implementation(project(":support-web"))
    implementation(project(":member:contract"))
}
