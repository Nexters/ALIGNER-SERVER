-- 와이어프레임에서 확인된 두 가지를 스키마에 반영한다.
--
-- 1) 근육맵이 앞/뒤 두 장이다. 세션 플레이어 우상단에 인체 실루엣이 앞·뒤 토글로 있고
--    각각 근육이 하이라이트된다. 키가 하나면 어느 쪽 그림에 얹을지 표현할 수 없다.
--
--    척추기립근처럼 뒤에만 보이는 근육은 front 가, 복직근처럼 앞에만 보이는 근육은 back 이
--    NULL 로 남는다. 양쪽에 걸치는 근육은 둘 다 채운다. "둘 중 하나는 있어야 한다" 는 CHECK 을
--    걸지 않는다 — 자산 키 자체가 아직 감수 전이라 둘 다 비는 상태가 정상이다.
--
--    ADD 가 아니라 RENAME 인 것은 seed 가 없어 옮길 데이터가 없기 때문이다. 값이 들어간
--    뒤였다면 추가하고 이관하는 편이 맞다.
ALTER TABLE catalog.muscle
    RENAME COLUMN highlight_asset_key TO front_highlight_asset_key;

ALTER TABLE catalog.muscle
    ADD COLUMN back_highlight_asset_key VARCHAR(120);

-- 2) 코스 개요의 스텝마다 운동 이름 아래 "가동성 웜업" · "핀포즈" 같은 분류가 붙는다.
--
--    CHECK 을 걸지 않는다. 값 집합이 감수 대상이라 확정되지 않았다 — 같은 이유로 difficulty
--    에도 CHECK 이 없다. 분류가 확정되면 그때 CHECK 을 새 changeset 으로 얹는다.
--    MuscleRole(STRETCH·STRENGTHEN)처럼 docs/domains.md 가 값 집합을 확정한 어휘가 아니므로
--    코드에 enum 으로 두지도 않는다.
ALTER TABLE catalog.exercise
    ADD COLUMN category VARCHAR(40);

COMMENT ON COLUMN catalog.muscle.front_highlight_asset_key IS
    '앞쪽 근육맵 이미지 식별자. 뒤에만 보이는 근육이면 NULL 이다. URL 이 아니고 파일은 프론트가 갖는다';
COMMENT ON COLUMN catalog.muscle.back_highlight_asset_key IS
    '뒤쪽 근육맵 이미지 식별자. 앞에만 보이는 근육이면 NULL 이다';
COMMENT ON COLUMN catalog.exercise.category IS
    '코스 스텝에 표시하는 분류. 값 집합이 감수 대상이라 CHECK 을 걸지 않았다 (difficulty 와 같다)';
