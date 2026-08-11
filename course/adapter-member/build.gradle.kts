plugins {
    id("aligner.kotlin-boot")
}

dependencies {
    // 구현할 port 도, 연결할 contract 도 internal 구현체 안에서만 쓰인다
    // (docs/architecture.md §8).
    implementation(project(":course:infrastructure"))
    implementation(project(":member:contract"))
}
