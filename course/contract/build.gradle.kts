plugins {
    id("aligner.kotlin-lib")
}

// 의존성 없음. docs/architecture.md §3 표에서 contract 의 허용 의존성은 "없음"이다.
// 식별자는 원시 타입으로 받고 자기 발행 DTO 로 반환한다 (§7).
