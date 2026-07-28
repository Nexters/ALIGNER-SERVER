plugins {
    id("aligner.kotlin-lib")
}

// 의존성 없음. docs/architecture.md §3 표에서 support-core 의 허용 의존성은 "없음"이다.
// 도메인 model 이 이 모듈을 의존하므로, 여기에 무언가 들어오면 model 까지 따라 들어간다.
