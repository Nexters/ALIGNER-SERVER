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

// screening — 부위·자세 체감 선택·원인 판별. 어떤 도메인도 의존하지 않는다
// (docs/domains.md §4-2, §5). 기본 6 개 + contract.
include(
    "screening:model",
    "screening:infrastructure",
    "screening:contract",
    "screening:schema",
    "screening:service",
    "screening:repository-jdbc",
    "screening:api",
)

// catalog — 보강 운동·목표 자세·근육·음성 큐잉 대본. 쓰기가 없는 조회 전용 도메인이지만
// 재생 URL 만 YMove 에서 읽어 온다 (docs/domains.md §4-3, §4-3-1, §5).
//
// adapter-ymove 가 별도 모듈인 것은 catalog:infrastructure 가 aligner.kotlin-lib 이라
// Spring·HTTP 타입이 클래스패스에 아예 없기 때문이다. port 를 순수하게 유지하려면 구현을
// 다른 플러그인 모듈로 뺄 수밖에 없다.
include(
    "catalog:model",
    "catalog:infrastructure",
    "catalog:contract",
    "catalog:schema",
    "catalog:service",
    "catalog:repository-jdbc",
    "catalog:api",
    "catalog:adapter-ymove",
)

// course — 원인별 코스 템플릿, 회원별 처방 코스·스텝·진행 상태, 도장
// (docs/domains.md §4-4, §5). 기본 6 개 + contract + adapter 3 개.
//
// adapter 가 셋인 것은 처방에 원인 검증(screening), 운동·자세 조회(catalog),
// 칼로리 계산용 몸무게(member)가 모두 필요하기 때문이다 (§3).
include(
    "course:model",
    "course:infrastructure",
    "course:contract",
    "course:schema",
    "course:service",
    "course:repository-jdbc",
    "course:api",
    "course:adapter-screening",
    "course:adapter-catalog",
    "course:adapter-member",
)

// training — 세션 시작·수행 기록·완료 push (docs/domains.md §4-5, §5).
// 기본 6 개 + adapter 2 개. **contract 를 만들지 않는다** — training 을 읽는 도메인이 없다 (§3).
include(
    "training:model",
    "training:infrastructure",
    "training:schema",
    "training:service",
    "training:repository-jdbc",
    "training:api",
    "training:adapter-course",
    "training:adapter-catalog",
)
