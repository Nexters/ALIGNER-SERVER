-- 코스 재도전 회차.
--
-- 하나의 핀포즈가 코스 하나이고, 그 코스를 **완주할 때마다 도장이 하나 붙는다** — 완료
-- 리포트의 "파이어로그 1 / 4회" 가 그 개수다. 4 개를 채워야 자세 완성이므로 완주한 코스를
-- 다시 돌 수 있어야 한다.
--
-- 다시 시작하면 스텝이 전부 NOT_STARTED 로 돌아가 "몇 번째 도전인가" 를 스텝 상태로는 알 수
-- 없다. 회차를 컬럼으로 두는 것은 그래서이고, 도장의 중복 방지 키도 이 값이다
-- (010-add-stamp-attempt-no.sql).
--
-- 기존 행은 1 회차다.
ALTER TABLE course.course
    ADD COLUMN attempt_no INT NOT NULL DEFAULT 1;

ALTER TABLE course.course
    ADD CONSTRAINT ck_course_attempt_no CHECK (attempt_no > 0);

COMMENT ON COLUMN course.course.attempt_no IS '재도전 회차. 완주한 코스를 다시 시작하면 1 씩 오른다';
