-- 영상 썸네일 URL. 세션 플레이어의 재생 전 포스터 프레임에 쓴다.
--
-- **image_asset_key 를 대체하는 것이 아니다.** 자리가 다르다 — image_asset_key 는 자세
-- 일러스트(자세 그리드·코스 순서 카드·가이드 상단)이고 프론트가 정적으로 갖는 키이며,
-- thumbnail_url 은 실제 영상의 한 프레임이고 YMove 자산이다 (docs/domains.md §4-3).
--
-- **video_url 과 달리 저장할 수 있다.** 재생 URL(videoUrl)은 Bunny CDN 의 서명 URL 이라
-- token·expires 를 달고 47 시간 만에 죽지만, thumbnail_url 은 YMove 의 API 엔드포인트
-- (`/api/v2/thumbnail/{videoId}?library=clean`)이고 **서명도 만료도 인증도 없다.**
-- API 키 없이 HTTP 200 · image/jpeg 가 내려오는 것을 2026-08-15 에 실측했다.
-- 그래서 프론트가 <img src> 에 그대로 쓸 수 있다.
--
-- 만료가 없으므로 expires_at 컬럼을 두지 않는다. 다만 URL 의 키가 slug 가 아니라 videoId 라
-- **영상이 교체되면 값이 바뀐다.** 갱신은 UPDATE changeset 으로 한다.
--
-- CHECK 을 걸지 않는다. URL 형식 검증을 DB 가 하면 YMove 가 형식을 바꿀 때 seed 가 막힌다
-- (008-add-exercise-media.sql 과 같은 판단).
ALTER TABLE catalog.exercise
    ADD COLUMN thumbnail_url TEXT;

COMMENT ON COLUMN catalog.exercise.thumbnail_url IS
    '영상 썸네일 URL. YMove 자산이지만 서명·만료가 없어 저장한다. 키가 videoId 라 영상 교체 시 바뀐다';

-- video_url 은 죽은 컬럼이다. 재생 URL 은 47 시간 만료라 DB 에 담을 수 없고
-- catalog/adapter-ymove 가 요청 시점에 채운다. 드롭은 연동이 도는 것을 확인한 뒤 별도로 한다.
COMMENT ON COLUMN catalog.exercise.video_url IS
    '쓰지 않는다. adapter-ymove 가 요청 시점에 채운다 (47 시간 만료라 저장 불가). 드롭 예정';
