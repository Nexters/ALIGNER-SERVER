-- 온보딩에서 "느끼는 부위" 선입력이 사라졌다 (docs/domains.md §4-2).
--
-- 와이어프레임의 순서는 자세 체감 선택 → 원인 판별 → 강화 부위·난이도 선택이다. 부위는
-- 진단의 입력이 아니라 판별 결과 이후의 선택이 됐고, 그 선택은 회원의 지속 설정이라
-- member 가 갖는다. screening 은 자세 체감만 받는다.
--
-- 컬럼을 지우지 않고 NULL 허용까지만 간다. DROP COLUMN 은 되돌려도 데이터가 돌아오지 않는데,
-- "느끼는 부위를 영영 받지 않는다" 까지는 확정되지 않았다. 확정되면 그때 지운다.
--
-- FK 는 그대로 둔다 — NULL 은 참조 무결성 검사를 통과하고, 값이 들어오는 날에는 검사가
-- 다시 필요하다.
ALTER TABLE screening.screening_result
    ALTER COLUMN perceived_body_part_code DROP NOT NULL;

COMMENT ON COLUMN screening.screening_result.perceived_body_part_code IS
    '옛 온보딩의 "느끼는 부위". 현재 흐름은 채우지 않아 항상 NULL 이다';
