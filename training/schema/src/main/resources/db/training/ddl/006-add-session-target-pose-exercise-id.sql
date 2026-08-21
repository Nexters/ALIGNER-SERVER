-- 완료 리포트가 핀포즈 영상을 재생할 수 있게 exercise 식별자를 스냅샷에 더한다.
--
-- **course_progress_target_pose_id 로는 영상을 받을 수 없다.** 같은 자세가 catalog.target_pose
-- (1~9)와 catalog.exercise(101~129) 양쪽에 행을 갖고, 영상·음성 큐·MET 은 exercise 쪽에만
-- 있다 (catalog/ddl/002-create-target-pose.sql). 핀포즈 직후 체감 화면이 이 값으로
-- GET /catalog/exercises/{id} 를 부른다.
--
-- 정적 카탈로그 값인데도 스냅샷에 두는 것은 course_progress_target_pose_name 과 같은
-- 판단이다 (005). 조회 때 catalog 를 다시 읽지 않고, 자세와 운동의 연결이 나중에 바뀌어도
-- 지난 리포트가 흔들리지 않는다.
--
-- NULL 을 허용한다. 두 행은 ymove_slug 로 잇는데 slug 가 없거나 짝이 없는 자세가 정상
-- 경로다. 005 로 이미 저장된 세션도 이 값이 NULL 로 남는다 — 화면은 그때 영상 없이 그린다.
ALTER TABLE training.session
    ADD COLUMN course_progress_target_pose_exercise_id BIGINT;

COMMENT ON COLUMN training.session.course_progress_target_pose_exercise_id IS
    '핀포즈의 catalog.exercise 식별자. target_pose_id 와 다른 값이고 영상 조회에 쓴다. 도메인 간 FK 는 걸지 않는다';
