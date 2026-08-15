-- 프론트 연동 테스트용 임시 회원. **dev 컨텍스트에서만 적재된다.**
--
-- 카카오 로그인 없이 JWT 를 손에 쥐고 나머지 API 를 부를 수 있게 하는 것이 목적이다.
-- 온보딩 자체는 프론트가 카카오 로그인과 함께 태우므로, 이 회원은 **온보딩이 끝난 상태**로 둔다
-- (키·몸무게·경력·강화 부위·난이도가 채워져 있다).
--
-- member_id 를 900001 로 고정한다. JWT 의 sub 가 이 값이라 바뀌면 발급해 둔 토큰이 전부 죽는다.
-- IDENTITY 시퀀스를 건드리지 않는다 — 실제 가입은 1 부터 올라오므로 90 만 번째 회원이
-- 생기기 전까지 충돌하지 않는다. 그 전에 이 행은 사라져야 한다.
--
-- kakao_id 를 'dev-temp-user' 로 둔다. 카카오 회원번호는 숫자라 실제 계정과 절대 겹치지 않고,
-- **DB 에서 눈으로 찾을 수 있다.**
INSERT INTO member.member (member_id, kakao_id, nickname,
                           height_cm, weight_kg, experience_level,
                           reinforcement_body_part_code, reinforcement_level)
VALUES (900001, 'dev-temp-user', '개발용테스터',
        170, 60, 'UNDER_ONE_YEAR',
        'BACK', 1)
ON CONFLICT (member_id) DO NOTHING;
