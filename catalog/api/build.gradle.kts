plugins {
    id("aligner.boot-mvc")
}

dependencies {
    // 컨트롤러 밖으로 나가는 타입이 없다. Bean 타입은 컨트롤러 자기 자신이다
    // (docs/architecture.md §8).
    implementation(project(":catalog:service"))
    // service 의 api(model) 로 전이돼 오지만 Identity 와 View 를 직접 쓰므로 명시 선언한다.
    implementation(project(":catalog:model"))

    // support-web 을 넣지 않는다. catalog 엔드포인트는 회원 식별자를 쓰지 않아
    // AlignerPrincipal 이 등장하지 않는다. 개인화가 생기면 그때 추가한다.

    // repository-jdbc 를 여기에 넣지 않는다. api → repository-jdbc 직접 참조는 §3 위반이고
    // 컨벤션 플러그인이 못 막는 자리다 (§8).
}
