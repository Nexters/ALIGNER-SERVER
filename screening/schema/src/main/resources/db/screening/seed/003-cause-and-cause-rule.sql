-- 원인 3 개와 (자세, 체감) → 원인 분기표 9 행.
--
-- 이게 없으면 회원이 무엇을 고르든 판별이 규칙을 하나도 못 찾아 POST /screening/results 가
-- 항상 422 CAUSE_NOT_DETERMINED 다. 자세·부위 seed 는 이슈 #43 에서 들어왔는데 분기표만
-- 빠져 있어서 온보딩이 자세 그리드 다음에서 끊겼다.
--
-- **원인을 부위 단위로 둔다.** 정본(docs/context/routine-content.md)의 자세별 `제한 요인` 을
-- 그대로 원인으로 쓰면 표기가 제각각인데 단순 통일이 안 된다 — 햄스트링이 업독에서는 `약화`,
-- 반 보트·보트에서는 `단축` 이고 내전근도 마찬가지로 갈린다. 반대 개념이라 한 원인으로 합칠 수
-- 없고, 어느 쪽으로 정규화할지는 요가 지도자 감수 영역이다. 그 판단을 서버가 지어내는 대신
-- 핀포즈가 속한 라인을 원인으로 둔다. 진단 결과 화면의 순위와 course 의 부위별 추천은 이것으로
-- 성립하고, 감수된 제한 요인 목록이 들어오면 그때 원인을 잘게 쪼개는 changeset 을 새로 쌓는다.

INSERT INTO screening.cause (cause_code, name, body_part_code, description)
VALUES ('BACK_WEAK', '등 라인 부족', 'BACK', '등을 펴고 버티는 힘이 부족합니다'),
       ('ABDOMEN_WEAK', '복부 라인 부족', 'ABDOMEN', '몸통을 지탱하는 힘이 부족합니다'),
       ('PELVIS_WEAK', '골반 라인 부족', 'PELVIS', '골반 주변이 뻣뻣하거나 힘이 부족합니다');

-- weight 를 전부 1 로 둔다. 판별(ScreeningResult.determineCauses)이 매칭된 규칙의 weight 를
-- 원인별로 합산하므로, 1 이면 점수가 곧 **그 라인에서 어려운 자세를 몇 개 골랐는지**가 된다.
-- 집계 방식을 코드가 아니라 이 값으로 조절한다는 §4-2 의 전제가 그대로 지켜진다.
--
-- **HARD 만 넣는다.** 판별 입력은 "부족한 자세" 이고, EASY 는 응답으로 저장만 되고 점수에
-- 관여하지 않는다. EASY 행이 없으므로 회원이 쉬웠던 자세만 고르면 규칙이 하나도 안 걸려
-- 422 가 그대로 난다 — 온보딩이 어려웠던 자세를 최소 1 개 받게 해야 한다.
--
-- target_pose_id 는 catalog seed(001-target-pose.sql)가 명시한 값이다. 1~3 등, 4~6 복부,
-- 7~9 골반. 도메인 간 FK 가 없어 DB 가 어긋남을 막지 않으므로 저쪽 값이 바뀌면 여기도 바꾼다.
INSERT INTO screening.cause_rule (target_pose_id, perceived_difficulty, cause_code, weight)
VALUES (1, 'HARD', 'BACK_WEAK', 1),
       (2, 'HARD', 'BACK_WEAK', 1),
       (3, 'HARD', 'BACK_WEAK', 1),
       (4, 'HARD', 'ABDOMEN_WEAK', 1),
       (5, 'HARD', 'ABDOMEN_WEAK', 1),
       (6, 'HARD', 'ABDOMEN_WEAK', 1),
       (7, 'HARD', 'PELVIS_WEAK', 1),
       (8, 'HARD', 'PELVIS_WEAK', 1),
       (9, 'HARD', 'PELVIS_WEAK', 1);
