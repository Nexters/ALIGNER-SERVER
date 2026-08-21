-- 002-exercise.sql 이 비워둔 default_set_count 를 29 개 전부 1 로 채운다.
--
-- 정본이 "세트 표기가 없는 준비 자세는 총 시간 1 세트" 라고 적었는데
-- (course/schema/seed/001-course-template.sql 주석) 그 1 이 데이터에 없었다. 준비 자세
-- 20 스텝은 course.template_step_exercise.set_count 가 NULL 이고 폴백인 이 컬럼도 NULL 이라
-- 응답까지 null 로 나갔고, 화면은 그것이 "1 세트" 인지 "아직 안 정함" 인지 알 수 없었다.
--
-- **핀포즈에도 1 을 넣는다.** 이 컬럼은 코스 override 가 없을 때만 쓰는 폴백인데
-- (docs/domains.md, CourseTemplateViews 주석) 핀포즈의 3·4 세트는 그 루틴에서만 참인 값이다.
-- 같은 자세가 다른 루틴에서는 준비 자세로 1 세트씩 쓰인다 — 브릿지는 루틴 7 의 핀포즈지만
-- 루틴 2·3 에서는 준비 자세이고, 보트자세·말라사나·낙타자세도 같다. 루틴과 무관하게 참인
-- 기본값은 1 이고, 루틴별 세트는 지금처럼 template_step_exercise 의 override 가 갖는다.
--
-- default_rep_count 는 계속 비운다. 정본이 반복 횟수로 표기하는 자세가 없다 — 전부 시간이다.

UPDATE catalog.exercise SET default_set_count = 1 WHERE exercise_id BETWEEN 101 AND 129;
