-- 운동 완료 리포트가 읽는 두 값을 세션에 붙인다.
--
-- 1) estimated_kcal — 이 세션의 소모 칼로리.
--
--    **계산은 course 가 하고 training 은 받은 값을 저장만 한다.** kcal 은 MET(catalog)과
--    몸무게(member)의 함수인데 그 둘을 이미 읽는 쪽이 course 다 (docs/domains.md §4-3).
--
--    코스·홈의 예상 칼로리는 조회할 때마다 계산하지만 이것은 **저장한다.** 그쪽은 "이 코스를
--    하면 얼마나 태울까" 라 지금 몸무게로 다시 계산하는 게 맞고, 이것은 "그날 얼마나 태웠나"
--    라 그날의 값으로 남아야 한다. 몸무게가 바뀌었다고 지난 리포트가 달라지면 기록이 아니다.
--
--    계산이 성립하지 않으면 NULL 이다. 0 은 "운동량 없음" 이라 "모름" 과 다르다.
--
-- 2) perceived_result — 핀포즈를 수행한 직후의 체감.
--
--    화면의 "오늘 파이어로그, 어땠어요?" 3 지선다다. training 은 **무슨 일이 있었나만
--    기록한다** (§2) — 이 값을 보고 자세를 바꿀지는 여기서 판단하지 않는다. 교체 규칙 자체가
--    아직 기획 미확정이라 서버가 자동으로 코스를 바꾸지 않는다.
--
--    값 집합이 화면의 선택지와 1:1 이라 CHECK 을 건다. difficulty·category 처럼 감수로
--    늘어날 어휘가 아니다.
ALTER TABLE training.session
    ADD COLUMN estimated_kcal INT;

ALTER TABLE training.session
    ADD COLUMN perceived_result VARCHAR(20);

ALTER TABLE training.session
    ADD CONSTRAINT ck_session_estimated_kcal CHECK (estimated_kcal >= 0);

ALTER TABLE training.session
    ADD CONSTRAINT ck_session_perceived_result
        CHECK (perceived_result IN ('SUCCEEDED', 'STILL_HARD', 'TOO_HARD'));

-- 연속 달성은 완료한 날짜를 역순으로 훑는다. member_id + started_at 인덱스로는 완료
-- 세션만 고르는 조건이 걸러지지 않아 전량을 읽는다.
CREATE INDEX ix_session_member_completed_at
    ON training.session (member_id, completed_at DESC)
    WHERE completed_at IS NOT NULL;

COMMENT ON COLUMN training.session.estimated_kcal IS
    '이 세션의 소모 칼로리. course 가 계산해 push 응답으로 돌려준 값을 그대로 저장한다. 계산이 안 되면 NULL';
COMMENT ON COLUMN training.session.perceived_result IS
    '핀포즈 직후 체감. SUCCEEDED(잘됐어요)·STILL_HARD(아직 어려워요)·TOO_HARD(안될 거 같아요). 기록만 하고 판단하지 않는다';
