/**
 * model · infrastructure · schema · contract · support-core 용.
 *
 * **Spring 이 클래스패스에 없다.** docs/architecture.md §3 의 "model 에 Spring 반입 금지"를
 * 문서가 아니라 컴파일러가 지키게 하는 자리다. 여기에 Spring 의존성을 추가하면 그 규칙이 죽는다.
 */

plugins {
    kotlin("jvm")
    // api / implementation 구분이 전이 누출을 막는다 (docs/architecture.md §8).
    // java 플러그인만으로는 `api` 설정이 없어 그 규율 자체를 쓸 수 없다.
    `java-library`
    id("org.jlleitschuh.gradle.ktlint")
}

kotlin {
    // 툴체인 버전도 카탈로그가 정본이다 (AGENTS.md §4).
    jvmToolchain(alignerLibs.findVersion("javaToolchain").get().requiredVersion.toInt())
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

// 통합 테스트 전용 소스셋. 모든 모듈이 갖게 되므로
// .github/workflows/gradle.yml 의 `./gradlew integrationTest` 가 항상 해석된다.
val integrationTestSourceSet =
    sourceSets.create("integrationTest") {
        compileClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["main"].output
    }

configurations["integrationTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["integrationTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

dependencies {
    "testImplementation"(alignerLibs.lib("kotest-runner-junit5"))
    "testImplementation"(alignerLibs.lib("kotest-assertions-core"))
    // out-port 를 스텁하는 데 쓴다. 모듈마다 반복 선언하면 같은 줄이 도메인 수만큼 흩어진다.
    "testImplementation"(alignerLibs.lib("mockk"))
}

// mockk 는 byte-buddy 로 자기 JVM 에 에이전트를 붙인다. JDK 21 부터 self-attach 가 기본
// 차단이라 이게 없으면 첫 mockk() 호출이 ExceptionInInitializerError 로 죽는다.
// byte-buddy 가 외부 프로세스로 우회를 시도하지만 그건 더 느리고 환경을 탄다.
val mockkAgentJvmArgs = listOf("-Djdk.attach.allowAttachSelf=true")

tasks.named<Test>("test") {
    useJUnitPlatform()
    jvmArgs(mockkAgentJvmArgs)
}

tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "TestContainers 기반 통합 테스트 (src/integrationTest)"
    testClassesDirs = integrationTestSourceSet.output.classesDirs
    classpath = integrationTestSourceSet.runtimeClasspath
    useJUnitPlatform()
    jvmArgs(mockkAgentJvmArgs)
    shouldRunAfter(tasks.named("test"))
}
