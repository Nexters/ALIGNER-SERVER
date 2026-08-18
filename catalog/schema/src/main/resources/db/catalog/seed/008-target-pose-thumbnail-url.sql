-- 목표 자세 9 개의 썸네일 URL.
--
-- **값을 새로 받아오지 않고 운동에서 가져온다.** 핀포즈는 같은 slug 로 exercise 와
-- target_pose 양쪽에 존재하고(seed/003), 두 행이 가리키는 YMove 자산이 같은 영상이다.
-- 같은 URL 을 두 번 적어 두면 한쪽만 갱신되는 순간 어긋난다.
--
-- ymove_slug 가 NULL 인 자세는 조인에서 빠져 thumbnail_url 이 NULL 로 남는다. 지금은 9 개
-- 전부 slug 가 있지만, 자세가 늘고 연동이 뒤따라오는 순간이 정상 경로다.
UPDATE catalog.target_pose tp
SET thumbnail_url = e.thumbnail_url
FROM catalog.exercise e
WHERE e.ymove_slug = tp.ymove_slug
  AND e.thumbnail_url IS NOT NULL;
