plugins {
    id("aligner.boot-mvc")
}

dependencies {
    // 컨트롤러 밖으로 나가는 타입이 없다. Bean 타입은 ScreeningController 자기 자신이다
    // (docs/architecture.md §8).
    implementation(project(":screening:service"))
    // service 의 api(model) 로 전이돼 오지만 애그리거트와 View 를 직접 쓰므로 명시 선언한다.
    implementation(project(":screening:model"))
    // 회원 식별자를 AlignerPrincipal 에서 꺼낸다 (docs/architecture.md §9).
    implementation(project(":support-web"))

    // repository-jdbc 를 여기에 넣지 않는다. api → repository-jdbc 직접 참조는 §3 위반이고
    // 컨벤션 플러그인이 못 막는 자리다 (§8).
}
