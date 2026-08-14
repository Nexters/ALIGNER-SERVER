-- 운동에 이미지와 영상 자리를 만든다.
--
-- 디자인의 코스 순서 카드 6 개, 운동 가이드 상단, 세션 플레이어가 전부 운동의 그림을 쓴다.
-- 지금까지는 목표 자세에만 image_asset_key 가 있어서 운동 쪽 화면을 그릴 값이 없었다.
--
-- image_asset_key 는 target_pose.image_asset_key 와 같은 규칙이다 — URL 이 아니라 안정된
-- 키이고 파일은 프론트가 정적으로 갖는다. 썸네일과 대표 이미지를 따로 두지 않는다. 화면이
-- 쓰는 크기가 카드·상단 두 곳뿐이라 같은 그림을 크기만 달리 쓰고, 나뉘어야 할 근거가
-- 생기면 그때 키를 하나 더 만든다.
--
-- video_url 은 반대로 URL 이다. 재생 소스가 YMove 이고 우리가 파일을 갖지 않는다
-- (docs/domains.md §4-3-1). **YMove 연동(catalog/adapter-ymove) 전까지는 전부 NULL 이다** —
-- 계약에 자리를 먼저 만들어 두는 것은 프론트가 플레이어 코드를 미리 짤 수 있게 하기 위해서다.
--
-- CHECK 을 걸지 않는다. URL 형식 검증을 DB 가 하면 YMove 가 형식을 바꿀 때 seed 가 막힌다.
ALTER TABLE catalog.exercise
    ADD COLUMN image_asset_key VARCHAR(120);

ALTER TABLE catalog.exercise
    ADD COLUMN video_url VARCHAR(500);

COMMENT ON COLUMN catalog.exercise.image_asset_key IS
    'URL 이 아니라 안정된 키다. 이미지 파일은 프론트가 정적으로 갖는다 (target_pose.image_asset_key 와 같은 규칙)';
COMMENT ON COLUMN catalog.exercise.video_url IS
    '재생 영상 URL. 소스가 YMove 라 우리가 파일을 갖지 않는다. adapter-ymove 연동 전까지는 NULL 이다';
