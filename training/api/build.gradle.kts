plugins {
    id("aligner.boot-mvc")
}

dependencies {
    implementation(project(":training:service"))
    implementation(project(":training:model"))
    // 회원 식별자를 AlignerPrincipal 에서 꺼낸다 (docs/architecture.md §9).
    implementation(project(":support-web"))
}
