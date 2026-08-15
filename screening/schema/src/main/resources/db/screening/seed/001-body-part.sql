-- 부위 3 개. 온보딩 진단 뒤 "강화할 부위" 선택지이고, 자세 도전 현황의 섹션 구분이다.
--
-- 값 집합은 `BACK` · `ABDOMEN` · `PELVIS` 셋으로 확정됐고 다섯 도메인의 api 모듈이 같은 enum 을
-- 사본으로 갖는다. **표시용 한글은 여기 name 이 정본이다** — 프론트가 매핑을 따로 만들지 않는다.
--
-- display_order 는 정본의 루틴 순서(등 → 복부 → 골반)를 따른다.

INSERT INTO screening.body_part (body_part_code, name, display_order)
VALUES ('BACK', '등', 1),
       ('ABDOMEN', '복부', 2),
       ('PELVIS', '골반', 3);
