plugins {
    id("aligner.boot-mvc")
}

dependencies {
    // GlobalExceptionHandler 의 공개 시그니처에 BaseException 이 등장하므로 api 다
    // (docs/architecture.md §8 "언제 api 를 쓰는가").
    api(project(":support-core"))
}
