plugins {
    id("aligner.kotlin-lib")
}

dependencies {
    // port 시그니처가 Member·MemberIdentity·MemberProfileView 를 그대로 노출한다
    // (docs/architecture.md §8).
    api(project(":member:model"))
}
