-- 도장을 자세당 하나에서 **회차당 하나**로 넓힌다.
--
-- 완료 리포트의 "파이어로그 1 / 4회" 는 핀포즈별 완주 횟수다. (member_id, target_pose_id)
-- 유니크는 자세당 도장을 하나로 묶어 두 번째 완주를 기록할 자리가 없다.
--
-- 회차를 키에 넣어도 재시도 멱등성은 그대로다 — 같은 회차의 완료 push 가 두 번 들어와도
-- ON CONFLICT DO NOTHING 이 두 번째를 흡수한다.
--
-- **상한 4 를 CHECK 으로 걸지 않는다.** 4 는 감수 데이터가 아니라 화면 규칙이라, 바뀔 때
-- changeset 이 아니라 코드만 고치는 편이 맞다 (docs/domains.md §4-2 의 "난이도별 최대 4 개"
-- 와 같은 판단). 상한은 course 애그리거트가 지킨다.
--
-- 기존 도장은 1 회차다.
ALTER TABLE course.stamp
    ADD COLUMN attempt_no INT NOT NULL DEFAULT 1;

ALTER TABLE course.stamp
    DROP CONSTRAINT uk_stamp_member_target_pose;

ALTER TABLE course.stamp
    ADD CONSTRAINT uk_stamp_member_target_pose_attempt UNIQUE (member_id, target_pose_id, attempt_no);

COMMENT ON TABLE course.stamp IS '완주할 때마다 붙는 도장. 자세당 4 개를 채우면 완성이다';
COMMENT ON COLUMN course.stamp.attempt_no IS '몇 번째 완주인가. course.course.attempt_no 의 사본이다';
