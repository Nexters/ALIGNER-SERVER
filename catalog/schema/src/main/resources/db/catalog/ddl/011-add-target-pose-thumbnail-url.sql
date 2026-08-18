-- 목표 자세에도 영상 썸네일을 둔다.
--
-- 운동(ddl/009)과 같은 판단이다. image_asset_key 는 프론트 정적 자산의 키이고 이쪽은 실제
-- 영상의 한 프레임이라 YMove 자산의 URL 이다. 재생 URL 과 달리 서명도 만료도 없어 DB 에
-- 담을 수 있고, **YMove 장애와 무관하게 값이 있다** (docs/domains.md §4-3-1).
--
-- 코스 개요 상단 히어로와 홈 카드가 자세 그림을 그리는데, 지금은 키만 있어 프론트가 정적
-- 파일을 갖고 있지 않으면 아무것도 못 그린다. 썸네일이 그 사이를 메운다.
ALTER TABLE catalog.target_pose
    ADD COLUMN thumbnail_url TEXT;

COMMENT ON COLUMN catalog.target_pose.thumbnail_url IS
    '영상 포스터 프레임 URL. image_asset_key 와 달리 URL 이고 파일은 YMove 가 갖는다. 재생 URL 과 달리 만료가 없다';
