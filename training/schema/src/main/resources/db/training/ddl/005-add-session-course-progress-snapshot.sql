-- 완료된 세션의 코스 진행도 스냅샷을 세션 행에 직접 보존한다.
--
-- **과거 리포트 불변성.** 회원이 다음 스텝을 진행하거나(completed_step_count 증가),
-- 코스를 완주한 뒤 재도전(restart)하여 스텝이 NOT_STARTED 로 돌아가거나,
-- 이후 도장을 추가로 획득하더라도 "그 세션을 완료했을 당시의 리포트" 는 영구히 보존되어야 한다.
--
-- 세션 조회 시마다 현재 코스 상태를 읽으면 지난 완료 리포트가 미래의 상태로 왜곡된다
-- (estimated_kcal 을 완료 시점에 저장하는 것과 같은 원칙이다).
--
-- 완료되지 않은 세션(IN_PROGRESS)에서는 모든 스냅샷 컬럼이 NULL 이다.
ALTER TABLE training.session
    ADD COLUMN course_progress_completed_step_count INT,
    ADD COLUMN course_progress_total_step_count INT,
    ADD COLUMN course_progress_course_completed BOOLEAN,
    ADD COLUMN course_progress_stamp_acquired BOOLEAN,
    ADD COLUMN course_progress_target_pose_id BIGINT,
    ADD COLUMN course_progress_target_pose_name VARCHAR(255),
    ADD COLUMN course_progress_body_part_code VARCHAR(50),
    ADD COLUMN course_progress_level INT,
    ADD COLUMN course_progress_acquired_stamp_count INT,
    ADD COLUMN course_progress_required_stamp_count INT,
    ADD COLUMN course_progress_target_pose_completed BOOLEAN;

ALTER TABLE training.session
    ADD CONSTRAINT ck_session_course_progress_status
        CHECK (course_progress_completed_step_count IS NULL OR status = 'COMPLETED');

ALTER TABLE training.session
    ADD CONSTRAINT ck_session_course_progress_completed_step_count
        CHECK (course_progress_completed_step_count IS NULL OR course_progress_completed_step_count >= 0);

ALTER TABLE training.session
    ADD CONSTRAINT ck_session_course_progress_total_step_count
        CHECK (course_progress_total_step_count IS NULL OR course_progress_total_step_count > 0);

ALTER TABLE training.session
    ADD CONSTRAINT ck_session_course_progress_acquired_stamp_count
        CHECK (course_progress_acquired_stamp_count IS NULL OR course_progress_acquired_stamp_count >= 0);

ALTER TABLE training.session
    ADD CONSTRAINT ck_session_course_progress_required_stamp_count
        CHECK (course_progress_required_stamp_count IS NULL OR course_progress_required_stamp_count > 0);
