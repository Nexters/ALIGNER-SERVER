package team.aligner.catalog.model

/**
 * 자세·운동이 근육을 쓰는 방식.
 *
 * 감수 전 데이터가 아니라 **닫힌 구조 어휘**다. docs/domains.md §4-3 이 값 집합을 확정했으므로
 * 코드에 두는 것이 seed 하드코딩 금지(docs/architecture.md §6)에 걸리지 않는다.
 * 운동명·MET·난이도·금기·근육 이름은 반대로 전부 seed 다.
 *
 * 콘텐츠 정본이 주동근을 "장요근(신장)" 처럼 표기하는데, 그 구분이 이것이다.
 */
enum class MuscleRole {
    /** 늘리는 근육. 정본 표기의 "(신장)" 이다. */
    STRETCH,

    /** 쓰는 근육. 표기가 없으면 이쪽이다. */
    STRENGTHEN,
}
