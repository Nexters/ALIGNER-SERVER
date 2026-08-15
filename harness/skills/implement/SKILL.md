---
name: implement
description: Aligner 서버 코드를 아키텍처 규칙에 맞게 구현한다. "구현해줘", "만들어줘", "코드 짜줘", 설계가 끝나 실제 Kotlin 코드·모듈·테스트를 작성할 때 사용한다.
---

# 구현

설계된 배치대로 코드를 쓴다. **설계가 없으면 먼저 `/design`을 돌린다.**

## 1. 준비

```bash
git status --short
git branch --show-current
```

- 사용자의 기존 변경사항을 먼저 파악한다. 남의 작업을 덮어쓰지 않는다.
- 브랜치가 `main`·`develop`이면 **작업 브랜치를 만들어야 한다.**
  절차는 `harness/rules/git-workflow.md`. 이슈 번호가 필요하다.

## 2. 읽기

- `docs/architecture.md` — 코드 위치 판단의 정본
- `harness/rules/architecture.md` — 위반 체크리스트
- 기존 코드가 있으면 **가장 가까운 도메인의 같은 계층 파일**을 읽고 그 관례를 따른다.
  주석 밀도, 네이밍, 패키지 구조를 새로 발명하지 않는다.

## 3. 새 도메인이라면

`docs/architecture.md` §10의 11단계를 **전부** 밟아야 한다. 하나만 빠져도 Bean이나
테이블을 못 찾는다. 이때는 **`module-scaffolder` 서브에이전트**에 골격 생성을 맡기고,
돌아온 결과에 비즈니스 로직을 채운다.

## 4. 작성 순서

1. **`model`** — 순수 도메인 모델. Spring·JDBC import가 있으면 컴파일이 깨져야 정상이다.
2. **`infrastructure`** — out-port 인터페이스만. 구현 금지.
3. **테스트** — 가능하면 실패하는 테스트를 먼저 쓴다. Kotest `DescribeSpec`.
4. **`service`** — 공개 interface + `internal` Impl. `@Transactional`(Query는 `readOnly = true`).
5. **`repository-jdbc`** — Entity, `CrudRepository`, port 구현. `@Table(schema = "{domain}")` 필수.
6. **`schema`** — Liquibase changeset. 이미 적용된 changeset은 수정하지 않고 새로 쌓는다.
7. **`api`** — Controller + DTO.
8. **`@AutoConfiguration` + `AutoConfiguration.imports`** — 세 모듈(`service`, `repository-jdbc`, `api`) 모두.
9. **`build.gradle.kts`** — 필요한 의존성만. `api`/`implementation` 구분은 §8 표.

## 5. 절대 하지 않는 것

- JPA·QueryDSL·Exposed 추가 (`docs/architecture.md` §2에서 탈락 확정)
- `@SpringBootApplication`·`@ComponentScan` 사용 (§5)
- `api` → `repository-jdbc` 직접 참조, `service` → `JdbcClient`·`CrudRepository` 직접 참조
- 도메인 간 직접 참조, 도메인 간 DB FK
- **스크리닝 문항·원인 매핑·자세 포인트 하드코딩** — `schema/seed/`로 간다
- P1 기능 선제 구현 (`AGENTS.md` §3)
- 버전 임의 변경 (`AGENTS.md` §4 표가 정본)
- 요청하지 않은 리팩터링. 지나가다 발견한 문제는 고치지 말고 **보고만** 한다

## 6. 한글

코드 주석·문서·커밋 메시지는 **한글**로 쓴다. 팀 전원이 한국어 사용자다.
클래스·변수명은 영문(용어집 기준), 주석은 한글이다.

## 7. 검증

```bash
./gradlew ktlintCheck
./gradlew build
```

- **`build-verifier` 서브에이전트**에 맡기면 실패 로그 분석까지 돌아온다.
- `gradlew`가 아직 없으면(초기 단계) 그 사실을 명시하고, 실행하지 못한 검증을 최종 응답에 적는다.
  **돌리지 않은 검증을 통과했다고 말하지 않는다.**

## 8. 마무리 보고

- 만든 파일 목록과 각 파일이 어느 계층인지
- 추가한 의존성과 `api`/`implementation` 선택 근거
- 실행한 검증과 **실행하지 못한 검증**
- 남은 작업 / 열린 질문

이어서 `/review`로 셀프 리뷰를 돌린다. **자동으로 커밋하지 않는다.**
