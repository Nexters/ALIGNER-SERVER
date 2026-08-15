-- 프론트 연동 테스트용 진단 결과. **dev 컨텍스트에서만 적재된다.**
--
-- 임시 회원(member 900001)이 온보딩 이후 API 를 바로 부를 수 있게 하는 것이 목적이다.
-- 이게 없으면 POST /courses 가 SCREENING_REQUIRED 로 막혀 코스·세션·리포트를 전부 못 본다.
--
-- **여기 값은 감수 데이터가 아니다.** 원인 분기표(cause_rule)는 요가 지도자 감수 대상이고
-- 아직 없다 (AGENTS.md §6). 이 파일은 그것을 대신하지 않는다 — 화면이 그려지는지 보려고
-- 넣는 자리 표시용이고, 그래서 dev 컨텍스트에 가둬 둔다. 감수 seed 가 들어오면 지운다.
--
-- cause 는 넣지만 cause_rule 은 넣지 않는다. 분기표를 지어내면 dev 에서 스크리닝을 다시
-- 제출했을 때 **감수받지 않은 판별 결과**가 나온다. 결과만 미리 꽂아 두는 편이 정직하다.
INSERT INTO screening.cause (cause_code, name, body_part_code, description)
VALUES ('DEV_PLACEHOLDER_CAUSE', '개발용 임시 원인', 'BACK', '연동 테스트용 자리 표시입니다. 감수된 원인이 아닙니다')
ON CONFLICT (cause_code) DO NOTHING;

INSERT INTO screening.screening_result (result_id, member_id, perceived_body_part_code, created_at)
VALUES (900001, 900001, 'BACK', now())
ON CONFLICT (result_id) DO NOTHING;

INSERT INTO screening.screening_answer (result_id, target_pose_id, perceived_difficulty)
VALUES (900001, 1, 'HARD')
ON CONFLICT (result_id, target_pose_id) DO NOTHING;

INSERT INTO screening.screening_cause (result_id, cause_code, rank, score)
VALUES (900001, 'DEV_PLACEHOLDER_CAUSE', 1, 3)
ON CONFLICT (result_id, cause_code) DO NOTHING;
