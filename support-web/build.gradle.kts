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

    // 카카오 인증 서버·사용자 API 호출. Boot 4 는 RestClient 자동설정을 별도 모듈로 뺐고
    // boot-mvc 의 starter-webmvc 에는 없다 — 없으면 RestClient.Builder Bean 을 못 찾아 기동이 죽는다.
    // 여기서 선언한다. 외부 HTTP 를 치지 않는 도메인 api 까지 딸려갈 이유가 없다 (jose 와 같은 판단).
    implementation(libs.spring.boot.starter.restclient)

    // MockRestServiceServer 로 카카오 호출을 검증한다. 실제 카카오를 치지 않고 요청 본문·헤더까지 본다.
    // 테스트 스코프 한정이라 런타임 산출물에 들어가지 않는다. aligner.repository-jdbc 규약이
    // integrationTest 에 같은 것을 넣는 것과 같은 용도다.
    testImplementation(libs.spring.boot.starter.test)
}
