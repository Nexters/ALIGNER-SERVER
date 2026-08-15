plugins {
    id("aligner.kotlin-boot")
}

dependencies {
    // 구현할 port 는 internal 구현체 안에서만 쓰인다 (docs/architecture.md §8).
    implementation(project(":catalog:infrastructure"))

    // YMove Exercise API 호출. Boot 4 는 RestClient 자동설정을 별도 모듈로 뺐고
    // aligner.kotlin-boot 이 주는 것은 spring-boot-autoconfigure 와 spring-tx 뿐이다 —
    // 없으면 RestClient.Builder Bean 을 못 찾아 기동이 죽는다. 플러그인이 아니라 여기서
    // 선언한다. 외부 HTTP 를 치지 않는 service·adapter 까지 딸려갈 이유가 없다
    // (support-web 이 같은 판단을 했다).
    implementation(libs.spring.boot.starter.restclient)

    // MockRestServiceServer 로 YMove 호출을 검증한다. 실제 YMove 를 치지 않고 헤더·경로까지 본다
    // — 월 고유 운동 상한이 있어 테스트가 실 API 를 치면 안 된다.
    testImplementation(libs.spring.boot.starter.test)

    // 실 YMove 계약 테스트용. YMOVE_API_KEY 가 없으면 건너뛰므로 CI 는 키 없이 초록이다.
    // AutoConfiguration 을 통해 조립하므로 internal 구현체에 손대지 않는다.
    integrationTestImplementation(libs.spring.boot.starter.restclient)
}
