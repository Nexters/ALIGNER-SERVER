-- 002-exercise.sql 이 비워둔 met_value 를 29 개 전부 채운다.
--
-- **이걸 채우기 전까지 칼로리는 전부 안 나왔다.** CalorieCalculator.sum 이 "하나라도 null 이면
-- 합계도 null" 이라(course/service/CalorieCalculator.kt), met_value 가 하나도 없는 상태에서는
-- 몸무게를 입력한 회원도 estimatedKcal 이 항상 null 이었다. 홈 카드·코스 상세·세션 완료가
-- 전부 같은 경로다.
--
-- **감수 전 임시값이다** (docs/domains.md §11-10 "MET 값의 출처와 보간"). 출처는 Compendium of
-- Physical Activities 의 요가·스트레칭·칼리스데닉스 항목이고, 자세별 배정은 우리 보간이다.
--
--   2.3  stretching, mild        — 누워서·앉아서 하는 수동 신장
--   2.5  stretching, hatha yoga  — 지지가 들어가는 일반 요가 자세
--   3.0  Pilates, general 대역   — 체중을 버티는 유지 자세
--   3.5  calisthenics, moderate  — 등척성 코어·하체
--   4.0  yoga, Power             — 전신 후굴 최고 강도 (휠)
--
-- **정본의 루틴 MET 을 그대로 쓰지 않은 이유**는 이 컬럼이 운동 단위이기 때문이다
-- (002-exercise.sql 주석, docs/domains.md §4-3). 고양이-소 자세는 루틴 9 개에 전부 나오는데
-- 정본은 그 루틴들에 2.3·2.8·3.0 을 각각 주므로, 루틴 값을 운동으로 내리면 충돌한다.
--
-- 대신 배정 후 루틴별 시간가중 평균을 정본과 대조했다 (duration × set_count 가중):
--
--   업독 2.3→2.58   낙타자세 2.8→2.58   휠 3.0→2.80
--   반 보트 2.3→2.77  보트자세 2.8→2.88   사이드 플랭크 3.0→2.88
--   브릿지 2.3→2.58  말라사나 2.8→2.67   파이어로그 3.0→2.43
--
-- 7 개는 ±0.3 안이고 **반 보트(+0.47)·파이어로그(-0.57) 두 개가 벗어난다.** 값이 틀려서가
-- 아니라 정본의 MET 이 레벨 고정값(1→2.3, 2→2.8, 3→3.0)이라 내용과 무관하기 때문이다.
-- 파이어로그 루틴은 레벨 3 이지만 구성이 수동 고관절 이완 위주라 실제 강도가 낮고, 반 보트
-- 루틴은 레벨 1 이지만 플랭크·호랑이 자세가 들어 있어 높다. **감수가 정할 자리다** — 레벨
-- 고정값을 정본으로 볼지, 구성 기반 값을 볼지.

UPDATE catalog.exercise SET met_value = 2.5 WHERE exercise_id = 101;  -- 고양이-소 자세   동적 웜업
UPDATE catalog.exercise SET met_value = 2.3 WHERE exercise_id = 102;  -- 스핑크스 자세    엎드린 얕은 후굴
UPDATE catalog.exercise SET met_value = 2.3 WHERE exercise_id = 103;  -- 소 얼굴 자세     앉은 수동 신장
UPDATE catalog.exercise SET met_value = 3.0 WHERE exercise_id = 104;  -- 호랑이 자세      사지 지지 항회전
UPDATE catalog.exercise SET met_value = 2.5 WHERE exercise_id = 105;  -- 코브라 자세      팔 지지 후굴
UPDATE catalog.exercise SET met_value = 3.0 WHERE exercise_id = 106;  -- 업독             전신 지지 후굴
UPDATE catalog.exercise SET met_value = 2.3 WHERE exercise_id = 107;  -- 퍼피 자세        수동 신장
UPDATE catalog.exercise SET met_value = 2.5 WHERE exercise_id = 108;  -- 로우 런지        하지 지지 신장
UPDATE catalog.exercise SET met_value = 3.0 WHERE exercise_id = 109;  -- 브릿지           둔근 등척 유지
UPDATE catalog.exercise SET met_value = 3.0 WHERE exercise_id = 110;  -- 낙타자세         무릎서기 후굴
UPDATE catalog.exercise SET met_value = 3.0 WHERE exercise_id = 111;  -- 활 자세          엎드려 후굴 근력
UPDATE catalog.exercise SET met_value = 4.0 WHERE exercise_id = 112;  -- 휠               전신 후굴 최고 강도
UPDATE catalog.exercise SET met_value = 2.3 WHERE exercise_id = 113;  -- 누워서 와이퍼    누운 동적 웜업
UPDATE catalog.exercise SET met_value = 3.5 WHERE exercise_id = 114;  -- 플랭크 자세      코어 등척성
UPDATE catalog.exercise SET met_value = 2.3 WHERE exercise_id = 115;  -- 앉은 전굴 자세   앉은 수동 신장
UPDATE catalog.exercise SET met_value = 3.0 WHERE exercise_id = 116;  -- 반 보트          코어 등척성
UPDATE catalog.exercise SET met_value = 3.5 WHERE exercise_id = 117;  -- 보트자세         코어 등척성 심화
UPDATE catalog.exercise SET met_value = 2.3 WHERE exercise_id = 118;  -- 문 빗장 자세     측면 수동 신장
UPDATE catalog.exercise SET met_value = 2.3 WHERE exercise_id = 119;  -- 반 물고기의 왕   앉은 비틀기
UPDATE catalog.exercise SET met_value = 3.5 WHERE exercise_id = 120;  -- 사이드 플랭크    측면 등척성
UPDATE catalog.exercise SET met_value = 2.3 WHERE exercise_id = 121;  -- 누운 나비 자세   수동 이완
UPDATE catalog.exercise SET met_value = 2.3 WHERE exercise_id = 122;  -- 해피 베이비      수동 이완
UPDATE catalog.exercise SET met_value = 2.3 WHERE exercise_id = 123;  -- 누운 비둘기      수동 이완
UPDATE catalog.exercise SET met_value = 2.5 WHERE exercise_id = 124;  -- 다운독           전신 지지
UPDATE catalog.exercise SET met_value = 2.3 WHERE exercise_id = 125;  -- 나비 자세        앉은 수동 신장
UPDATE catalog.exercise SET met_value = 2.5 WHERE exercise_id = 126;  -- 개구리 자세      체중 실린 심화 신장
UPDATE catalog.exercise SET met_value = 3.5 WHERE exercise_id = 127;  -- 의자 자세        하체 등척성
UPDATE catalog.exercise SET met_value = 3.0 WHERE exercise_id = 128;  -- 말라사나         깊은 스쿼트 유지
UPDATE catalog.exercise SET met_value = 2.5 WHERE exercise_id = 129;  -- 파이어로그       앉은 심부 신장
