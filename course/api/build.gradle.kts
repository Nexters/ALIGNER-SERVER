plugins {
    id("aligner.boot-mvc")
}

dependencies {
    // 컨트롤러 밖으로 나가는 타입이 없다. Bean 타입은 컨트롤러 자기 자신이다 (§8).
    implementation(project(":course:service"))
    // service 의 api(model) 로 전이돼 오지만 애그리거트와 View 를 직접 쓰므로 명시 선언한다.
    implementation(project(":course:model"))
    // 회원 식별자를 AlignerPrincipal 에서 꺼낸다 (docs/architecture.md §9).
    implementation(project(":support-web"))
}
