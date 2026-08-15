plugins {
    id("aligner.kotlin-lib")
}

dependencies {
    // port 시그니처가 Session 애그리거트를 그대로 노출한다 (docs/architecture.md §8).
    api(project(":training:model"))
}
