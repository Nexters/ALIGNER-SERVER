/**
 * 프론트 연동용 더미 목 서버. **임시 개발 도구이고 배포 대상이 아니다.**
 *
 * `aligner.boot-application` 을 그대로 쓴다. 이 플러그인이 JDBC·Liquibase starter 를 끌고
 * 오지만 목은 DB 를 쓰지 않으므로 **자동설정을 application.yml 에서 끈다.** 목을 위해
 * 컨벤션 플러그인을 새로 만드는 것보다 build-logic 을 건드리지 않는 편이 낫다 —
 * 이 모듈은 곧 지운다.
 *
 * seed 와 catalog/adapter-ymove 가 들어오면 이 모듈을 통째로 지운다 (이슈 #29).
 */
plugins {
    id("aligner.boot-application")
}

/**
 * **배포 가능한 산출물을 만들지 않는다.**
 *
 * `aligner.boot-application` 이 bootJar 를 켜므로 그대로 두면 CI 의 전체 build 에서
 * 실행 가능한 jar 가 만들어지고, 배포 파이프라인이 실수로 집어갈 수 있다. 목은 개발자가
 * `bootRun` 으로만 띄운다 — bootRun 은 클래스패스로 돌기 때문에 jar 가 없어도 된다.
 *
 * 이렇게 두면 "배포에서 제외한다" 는 약속을 파이프라인 설정이 아니라 **빌드가 강제한다.**
 */
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

dependencies {
    // 인증·CORS·에러 포맷·OpenAPI 를 실제 그대로 쓴다. 목이 다르게 흉내내면 프론트가
    // 실제 서버로 옮길 때 그 차이만큼 다시 고쳐야 한다.
    implementation(project(":support-web"))

    // **응답 DTO 를 재사용한다.** 서버가 필드를 바꾸면 이 모듈이 컴파일에서 깨져
    // 계약 드리프트를 막는다. 컨트롤러는 application.yml 의 exclude 로 꺼둔다.
    implementation(project(":member:api"))
    implementation(project(":screening:api"))
    implementation(project(":catalog:api"))
    implementation(project(":course:api"))
    implementation(project(":training:api"))

    // 요청 DTO 의 toCommand()·toAnswers() 를 그대로 호출해 **실제와 같은 검증**을 태운다.
    // 그 반환 타입이 service 모듈에 있어 컴파일에 필요하다. AutoConfiguration 은 꺼두므로
    // 서비스 Bean 이 올라오지는 않는다.
    implementation(project(":member:service"))

    // DTO 가 도메인 model 의 enum·View 를 노출한다.
    implementation(project(":member:model"))
    implementation(project(":screening:model"))
    implementation(project(":catalog:model"))
    implementation(project(":course:model"))
    implementation(project(":training:model"))
}
