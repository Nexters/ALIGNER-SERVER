## 목표

- MVP에 대응하는 올바른 동작을 제공하는 서버를 구축한다.
- k8s를 활용해 Kubernetes 생태계 구축 및 운영을 학습한다.

## 언어 & 프레임워크

- Kotlin
- Spring Boot, ~~Ktor~~
- PostgreSQL
- JPA / ~~Exposed~~
- Spring Security
- QueryDSL
- TestContainers
- corutine → 비동기 Async 다 코루틴으로?
- Kotest + Ktlint
    - DescribeSpec
- build-logic 모듈두는거 어떰? or buildSrc

  !image.png


## 아키텍처

- 멀티모듈
    - 모듈 별로 배포 단위를 가져가도록.
    - 서버간 통신은 gRPC
    - 모듈간 직접 의존 금지 (gradle 의존성 설정)
- 테스트 코드 작성
    - 통합 테스트

## 인프라

> K8s 활용: @이동훈 주도로 진행
>
- k3s
- ArgoCD or FluxCD + Github Actions
- Helm Chart

모니터링 = 굳이

## 협업

- 브랜치 전략: Github Flow
    - main, develop, feature, fix, refactor
- 이슈는 관리는 깃허브 이슈 활용
- 회의: 온라인 / 필요시 허들 진행

---
