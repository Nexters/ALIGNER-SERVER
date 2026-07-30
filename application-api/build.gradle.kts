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
}
