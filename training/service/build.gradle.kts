plugins {
    id("aligner.kotlin-boot")
}

dependencies {
    // 서비스 시그니처가 Session 과 View 를 반환한다 (docs/architecture.md §8).
    api(project(":training:model"))
    implementation(project(":training:infrastructure"))

    // contract 가 없다. training 을 읽는 도메인이 없다 (docs/domains.md §3).
}
