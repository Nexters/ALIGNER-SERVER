plugins {
    id("aligner.boot-mvc")
}

dependencies {
    // 컨트롤러 밖으로 나가는 타입이 없다. Bean 타입은 MemberController 자기 자신이다
    // (docs/architecture.md §8).
    implementation(project(":member:service"))
    // service 의 api(model) 로 전이돼 오지만 MemberIdentity·MemberProfileView 를 직접 쓰므로
    // 명시 선언한다. 상위 모듈의 api/implementation 선택이 바뀌어도 깨지지 않는다.
    implementation(project(":member:model"))
    implementation(project(":support-web"))

    // repository-jdbc 를 여기에 넣지 않는다. api → repository-jdbc 직접 참조는 §3 위반이고
    // 컨벤션 플러그인이 못 막는 자리다 (§8).
}
