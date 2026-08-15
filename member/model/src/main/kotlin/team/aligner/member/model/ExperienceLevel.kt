package team.aligner.member.model

/**
 * 운동 경력. 온보딩의 "운동을 하신지 얼마나 됐나요?" 선택지 셋이다.
 *
 * 화면 문구를 서버가 내리지 않는다. 코드만 주고 "1년 미만" 같은 표시는 프론트가 그린다 —
 * `imageAssetKey` 를 URL 대신 키로 내리는 것과 같은 판단이다 (docs/domains.md §4-3).
 *
 * 값 집합은 DDL 의 CHECK 이 함께 강제한다. 여기에 값을 더하면 changeset 도 같이 쌓아야 한다.
 */
enum class ExperienceLevel {
    /** 1년 미만 */
    UNDER_ONE_YEAR,

    /** 1~3년 */
    ONE_TO_THREE_YEARS,

    /** 3년 이상 */
    OVER_THREE_YEARS,
}
