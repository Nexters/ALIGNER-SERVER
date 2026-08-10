plugins {
    id("aligner.kotlin-lib")
}

dependencies {
    // TrainingErrorCode 가 ErrorCode 를 구현하고 예외가 BaseException 을 상속한다 (§8).
    api(project(":support-core"))
}
