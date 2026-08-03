plugins {
    id("aligner.boot-mvc")
}

dependencies {
    // GlobalExceptionHandler 의 공개 시그니처에 BaseException 이 등장하므로 api 다
    // (docs/architecture.md §8 "언제 api 를 쓰는가").
    api(project(":support-core"))

    // 자체 JWT 발급·검증. boot-mvc 의 starter-security 에는 인코더·디코더가 없다.
    // 플러그인이 아니라 여기서 선언한다 — JWT 를 쓰지 않는 도메인 api 까지 nimbus 가 딸려가면 안 된다.
    implementation(libs.spring.security.oauth2.jose)
}
