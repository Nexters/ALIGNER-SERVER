plugins {
    id("aligner.kotlin-lib")
}

dependencies {
    // port 시그니처가 ExerciseIdentity·TargetPoseIdentity 와 View 를 그대로 노출한다
    // (docs/architecture.md §8).
    api(project(":catalog:model"))
}
