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

    // member — 카카오 로그인·회원·프로필 (docs/architecture.md §10 8 단계).
    implementation(project(":member:api"))
    implementation(project(":member:repository-jdbc"))
    implementation(project(":member:schema"))
    // adapter-auth 로 런타임 전이되지만, §9 가 "함께 조립한다"고 명시했고 조립은
    // 클래스패스 우연이 아니라 빌드 선언이 결정해야 한다 (§5).
    implementation(project(":member:contract"))
    // 이 줄을 빼면 AuthMemberPort Bean 이 없어 기동이 실패해야 정상이다 (§9).
    implementation(project(":member:adapter-auth"))

    // catalog — 보강 운동·목표 자세·근육·음성 큐 (docs/architecture.md §10 8 단계).
    implementation(project(":catalog:api"))
    implementation(project(":catalog:repository-jdbc"))
    implementation(project(":catalog:schema"))
    // catalog:contract 를 넣지 않는다. 소비자인 course/adapter-catalog 가 아직 없고, 계약
    // 구현체 Bean 은 catalog:api → catalog:service 로 런타임 전이돼 등록된다. member 가
    // contract 를 명시한 것은 §9 가 support-web 과 함께 조립하라고 못박았기 때문이다.

    // catalog:adapter-ymove 가 없다. 영상 연동은 docs/domains.md §7-4·5·6 이 정해진 뒤
    // 후속 이슈로 붙인다.
}
