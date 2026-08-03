pluginManagement {
    // 컨벤션 플러그인은 별도 빌드다. buildSrc 를 쓰지 않는 이유는 docs/architecture.md §8.
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // JDK 25 가 없는 팀원의 클론 직후 빌드가 `No matching toolchains found` 로 막히지 않게 한다.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    // 모듈이 각자 저장소를 선언하면 빌드가 재현되지 않는다.
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "aligner-server"

// 루트 레벨 공유 모듈과 실행 모듈.
include(
    "support-core",
    "support-web",
    "application-api",
)

// member — 카카오 로그인·회원·프로필. 기본 6 개 + contract + adapter-auth
// (docs/domains.md §4-1, §5).
include(
    "member:model",
    "member:infrastructure",
    "member:contract",
    "member:schema",
    "member:service",
    "member:repository-jdbc",
    "member:api",
    "member:adapter-auth",
)

// catalog — 보강 운동·목표 자세·근육·음성 큐잉 대본. Command 가 없는 조회 전용 도메인
// (docs/domains.md §4-3, §5). adapter-ymove 는 §7-4·5·6 이 정해진 뒤 후속으로 붙인다.
include(
    "catalog:model",
    "catalog:infrastructure",
    "catalog:contract",
)
