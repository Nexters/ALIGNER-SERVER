plugins {
    id("aligner.kotlin-lib")
}

// 의존성 없음. docs/architecture.md §3 표에서 schema 의 허용 의존성은 "없음"이다.
// Kotlin 소스가 없고 Liquibase changelog 리소스만 담는다.
