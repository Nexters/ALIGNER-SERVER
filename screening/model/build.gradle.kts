plugins {
    id("aligner.kotlin-lib")
}

dependencies {
    // ScreeningErrorCode 가 ErrorCode 를 구현하고 예외가 BaseException 을 상속한다.
    // 둘 다 공개 시그니처에 등장하므로 api 다 (docs/architecture.md §8).
    api(project(":support-core"))
}
