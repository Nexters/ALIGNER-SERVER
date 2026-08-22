plugins {
    id("aligner.boot-application")
}

/**
 * 조립 종착점. 여기에 선언하지 않은 모듈은 런타임에 아예 로딩되지 않는다.
 *
 * 도메인이 추가되면 그 도메인의 api · repository-jdbc · schema · adapter-* 를
 * 여기에 추가한다 (docs/architecture.md §10 8단계). 지금은 도메인이 없다.
 */
dependencies {
    implementation(project(":support-web"))
    implementation(project(":support-core"))
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.micrometer.tracing.bridge.otel)

    // member — 카카오 로그인·회원·프로필 (docs/architecture.md §10 8 단계).
    implementation(project(":member:api"))
    implementation(project(":member:repository-jdbc"))
    implementation(project(":member:schema"))
    // adapter-auth 로 런타임 전이되지만, §9 가 "함께 조립한다"고 명시했고 조립은
    // 클래스패스 우연이 아니라 빌드 선언이 결정해야 한다 (§5).
    implementation(project(":member:contract"))
    // 이 줄을 빼면 AuthMemberPort Bean 이 없어 기동이 실패해야 정상이다 (§9).
    implementation(project(":member:adapter-auth"))

    // screening — 부위·자세 체감 선택·원인 판별 (docs/architecture.md §10 8 단계).
    implementation(project(":screening:api"))
    implementation(project(":screening:repository-jdbc"))
    implementation(project(":screening:schema"))
    // screening:contract 를 넣지 않는다. 소비자인 course/adapter-screening 이 아직 없고, 계약
    // 구현체 Bean 은 screening:api → screening:service 로 런타임 전이돼 등록된다. catalog 와 같다.

    // catalog — 보강 운동·목표 자세·근육·음성 큐 (docs/architecture.md §10 8 단계).
    implementation(project(":catalog:api"))
    implementation(project(":catalog:repository-jdbc"))
    implementation(project(":catalog:schema"))
    // catalog:contract 를 넣지 않는다. 소비자인 course/adapter-catalog 가 아직 없고, 계약
    // 구현체 Bean 은 catalog:api → catalog:service 로 런타임 전이돼 등록된다. member 가
    // contract 를 명시한 것은 §9 가 support-web 과 함께 조립하라고 못박았기 때문이다.

    // 재생 URL 을 YMove 에서 읽는다 (docs/domains.md §4-3-1). 이 줄을 빼면 PoseVideoPort
    // Bean 이 없어 **기동이 실패해야 정상이다** — CatalogServiceAutoConfiguration 이
    // ExerciseQueryService 를 만들 때 요구한다 (§9 의 adapter-auth 와 같다).
    implementation(project(":catalog:adapter-ymove"))

    // course — 코스 처방·오늘의 코스·진행도·도장 (docs/architecture.md §10 8 단계).
    implementation(project(":course:api"))
    implementation(project(":course:repository-jdbc"))
    implementation(project(":course:schema"))
    // adapter 셋을 명시한다. 하나라도 빠지면 그 port 의 Bean 이 없어 **기동이 실패해야
    // 정상이다** (§9 의 adapter-auth 와 같다).
    implementation(project(":course:adapter-screening"))
    implementation(project(":course:adapter-catalog"))
    implementation(project(":course:adapter-member"))
    // adapter 가 의존하는 상대 도메인 contract 를 함께 선언한다. 런타임 전이에 기대지 않고
    // 조립을 빌드 선언이 결정하게 한다 (§5).
    implementation(project(":screening:contract"))
    implementation(project(":catalog:contract"))
    // course:contract 는 training/adapter-course 가 쓴다. 아래에서 함께 선언한다.

    // training — 세션 시작·수행 기록·완료 push (docs/architecture.md §10 8 단계).
    implementation(project(":training:api"))
    implementation(project(":training:repository-jdbc"))
    implementation(project(":training:schema"))
    // adapter 둘을 명시한다. 빠지면 그 port 의 Bean 이 없어 **기동이 실패해야 정상이다**.
    implementation(project(":training:adapter-course"))
    implementation(project(":training:adapter-catalog"))
    // adapter 가 의존하는 상대 도메인 contract 를 함께 선언한다 (§5).
    implementation(project(":course:contract"))
    // training:contract 가 없다. training 을 읽는 도메인이 없다 (docs/domains.md §3).

    // 조립이 실제로 서는지는 띄워봐야 안다. Bean 누락·AutoConfiguration.imports 누락은
    // 컴파일에 걸리지 않는다 (docs/architecture.md §5). 목록은 aligner.repository-jdbc 가
    // 통합 테스트에 넣는 것과 같다 — liquibase starter 는 boot-application 이 이미 준다.
    testImplementation(libs.spring.boot.starter.test)
    integrationTestImplementation(libs.spring.boot.starter.test)
    integrationTestImplementation(libs.spring.boot.testcontainers)
    integrationTestImplementation(libs.testcontainers.postgresql)
    integrationTestImplementation(libs.testcontainers.junit.jupiter)

    // E2E 가 외부 경계를 통제하려면 그 타입이 보여야 한다. 카카오와 YMove 를 실제로 치면
    // 테스트가 남의 서버 상태에 매달리고, YMove 는 월 고유 운동 상한까지 있다.
    // **integrationTest 스코프라 런타임 산출물에 들어가지 않는다** — 조립 모듈이
    // catalog:infrastructure 를 컴파일에 보지 못한다는 계층 규칙은 그대로다.
    integrationTestImplementation(project(":catalog:infrastructure"))
}
