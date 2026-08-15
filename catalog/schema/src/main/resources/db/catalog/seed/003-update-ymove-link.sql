-- catalog.exercise 와 catalog.target_pose 에 YMove 연결 고리와 썸네일을 채운다.
-- 값의 출처는 2026-08-15 의 YMove Exercise API 응답이고, 자세 선택은
-- docs/context/routine-content.md §10 이 정본이다.
--
-- **좌우로 나뉘는 자세 8 개는 한쪽 slug 만 쓴다.** exercise 가 29 행인데 YMove 의 단위는
-- 좌우가 갈린 37 개라 1:1 이 아니다 (002-exercise.sql 주석). 어느 쪽을 쓸지는
-- **정본이 번역한 쪽**으로 정했다 — 그래야 영상·음성 큐·slug 가 한 기준으로 일관된다.
--
--   호랑이 자세      → tiger-pose-right    정본 1 번이 API right 와 문장이 같다.
--                                          left 는 다리를 펴서 드는 **다른 동작**이다
--   파이어로그       → fire-log-pose-left  정본이 "왼쪽 다리를 앞에 두고" 로 번역했다
--   소 얼굴 자세     → cow-face-pose-left  정본 순서가 left 와 같다
--   나머지 5 개      → -left               좌우 원문이 동일해 어느 쪽이든 같다. 규칙을
--                                          고정하려고 left 로 통일했다
--
-- 좌우를 각각 재생하려면 exercise 를 37 행으로 나눠야 하는데, 정본이 한 스텝의 시간을
-- 좌우로 어떻게 나누는지 주지 않고 tiger-left 의 번역도 없다. 감수가 그 둘을 주면 그때
-- 나눈다 (docs/domains.md §7-6).
--
-- thumbnail_url 은 서명도 만료도 없는 YMove 엔드포인트다 (009-add-exercise-thumbnail-url.sql).
-- video_url 은 채우지 않는다 — 47 시간 만료라 adapter-ymove 가 요청 시점에 채운다.

UPDATE catalog.exercise SET ymove_slug = 'cat-cow-pose', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/df1bcf35-11a2-4836-a9b5-f7da273f46cb?library=clean'
 WHERE exercise_id = 101;  -- 고양이-소 자세
UPDATE catalog.exercise SET ymove_slug = 'sphinx-pose', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/3daea5f0-a5fc-43db-bed0-0aa29fb39181?library=clean'
 WHERE exercise_id = 102;  -- 스핑크스 자세
UPDATE catalog.exercise SET ymove_slug = 'cow-face-pose-left', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/38e527f2-76e5-43dd-b67b-bb0c3ab163ef?library=clean'
 WHERE exercise_id = 103;  -- 소 얼굴 자세
UPDATE catalog.exercise SET ymove_slug = 'tiger-pose-right', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/0a43f314-9a82-4ceb-98ea-666fc007ce12?library=clean'
 WHERE exercise_id = 104;  -- 호랑이 자세
UPDATE catalog.exercise SET ymove_slug = 'cobra-pose', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/6e6e8f88-6915-4f93-8fac-5d66eaff7acc?library=clean'
 WHERE exercise_id = 105;  -- 코브라 자세
UPDATE catalog.exercise SET ymove_slug = 'upward-facing-dog-pose', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/fa226a4d-0c24-43c8-8eb3-0bfb58f14236?library=clean'
 WHERE exercise_id = 106;  -- 업독
UPDATE catalog.exercise SET ymove_slug = 'puppy-pose', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/5a4bd0f4-7d8f-4654-ab2b-866962a6de03?library=clean'
 WHERE exercise_id = 107;  -- 퍼피 자세
UPDATE catalog.exercise SET ymove_slug = 'low-lunge-left', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/895948c8-ec95-432c-b0c9-c2317b3dc645?library=clean'
 WHERE exercise_id = 108;  -- 로우 런지
UPDATE catalog.exercise SET ymove_slug = 'bridge-pose', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/a185f8f6-48ef-44a2-92a8-b02d6734fb9e?library=clean'
 WHERE exercise_id = 109;  -- 브릿지
UPDATE catalog.exercise SET ymove_slug = 'camel-pose', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/1b58affc-4759-452b-8292-393b2bf749ed?library=clean'
 WHERE exercise_id = 110;  -- 낙타자세
UPDATE catalog.exercise SET ymove_slug = 'bow-pose', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/511fccbc-392a-41e6-9353-0a62ab8591ce?library=clean'
 WHERE exercise_id = 111;  -- 활 자세
UPDATE catalog.exercise SET ymove_slug = 'wheel-pose', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/bebc11de-dca5-44f5-816c-467c23a8ff50?library=clean'
 WHERE exercise_id = 112;  -- 휠
UPDATE catalog.exercise SET ymove_slug = 'reclined-windshield-wipers', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/6c51164d-729b-4f92-b1b4-7512c6fc2303?library=clean'
 WHERE exercise_id = 113;  -- 누워서 와이퍼
UPDATE catalog.exercise SET ymove_slug = 'plank-pose', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/70a0e1bf-f64c-489f-b30e-468b0672bffa?library=clean'
 WHERE exercise_id = 114;  -- 플랭크 자세
UPDATE catalog.exercise SET ymove_slug = 'seated-forward-bend', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/5cf19263-6b93-42f0-bea8-f3a420fbddce?library=clean'
 WHERE exercise_id = 115;  -- 앉은 전굴 자세
UPDATE catalog.exercise SET ymove_slug = 'half-boat-pose', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/c762f5cf-ea79-4ae1-a844-3b7949b964db?library=clean'
 WHERE exercise_id = 116;  -- 반 보트
UPDATE catalog.exercise SET ymove_slug = 'boat-pose', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/9322839b-727e-4b41-8574-bce758f9922f?library=clean'
 WHERE exercise_id = 117;  -- 보트자세
UPDATE catalog.exercise SET ymove_slug = 'gate-pose-left', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/4eb5bb8b-da6f-4db1-87db-f59752bf52d5?library=clean'
 WHERE exercise_id = 118;  -- 문 빗장 자세
UPDATE catalog.exercise SET ymove_slug = 'half-lord-of-the-fishes-pose-left', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/20bc1c09-a0af-49f8-bfc3-d28f0d071e67?library=clean'
 WHERE exercise_id = 119;  -- 반 물고기의 왕 자세
UPDATE catalog.exercise SET ymove_slug = 'side-plank-pose-left', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/5cf74a6a-b041-4446-b418-f826ea5030af?library=clean'
 WHERE exercise_id = 120;  -- 사이드 플랭크
UPDATE catalog.exercise SET ymove_slug = 'reclined-butterfly', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/b246084b-e457-4dd7-9336-7dca912511de?library=clean'
 WHERE exercise_id = 121;  -- 누운 나비 자세
UPDATE catalog.exercise SET ymove_slug = 'happy-baby-pose', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/9f97ef51-9061-4f9a-bebf-b524d6af03ff?library=clean'
 WHERE exercise_id = 122;  -- 해피 베이비 자세
UPDATE catalog.exercise SET ymove_slug = 'reclined-pigeon-pose-left', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/8b1e7929-601e-4593-b794-fa3f821359fc?library=clean'
 WHERE exercise_id = 123;  -- 누운 비둘기 자세
UPDATE catalog.exercise SET ymove_slug = 'downward-dog', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/baf5c832-9ca6-4366-8986-891e80824e86?library=clean'
 WHERE exercise_id = 124;  -- 다운독
UPDATE catalog.exercise SET ymove_slug = 'cobblers-pose', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/b923729f-b164-4978-8e7e-5b8726c20a43?library=clean'
 WHERE exercise_id = 125;  -- 나비 자세
UPDATE catalog.exercise SET ymove_slug = 'frog-pose', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/ae39b0b9-3cf4-4209-bdc2-e927137e62cd?library=clean'
 WHERE exercise_id = 126;  -- 개구리 자세
UPDATE catalog.exercise SET ymove_slug = 'chair-pose', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/4525209f-a329-414d-b919-f53c06d84bb3?library=clean'
 WHERE exercise_id = 127;  -- 의자 자세
UPDATE catalog.exercise SET ymove_slug = 'garland-pose', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/5e32de6d-897e-4780-8193-9afc05b93a90?library=clean'
 WHERE exercise_id = 128;  -- 말라사나
UPDATE catalog.exercise SET ymove_slug = 'fire-log-pose-left', thumbnail_url = 'https://exercise-api.ymove.app/api/v2/thumbnail/45013b4a-848d-4821-8783-e040c1da6994?library=clean'
 WHERE exercise_id = 129;  -- 파이어로그

-- target_pose 의 slug 는 exercise 와 같은 자세를 가리킨다. 핀포즈가 양쪽에 행을 갖기
-- 때문이고(docs/domains.md §4-3), 두 테이블의 UNIQUE 는 서로 독립이라 충돌하지 않는다.
--
-- 썸네일을 target_pose 에 두지 않는다. 자세 그리드는 image_asset_key(우리 정적 자산)로
-- 그리고, 영상 포스터가 필요한 화면은 세션 플레이어뿐이라 exercise 로 충분하다.
UPDATE catalog.target_pose SET ymove_slug = 'upward-facing-dog-pose' WHERE target_pose_id = 1;  -- 업독
UPDATE catalog.target_pose SET ymove_slug = 'camel-pose' WHERE target_pose_id = 2;  -- 낙타자세
UPDATE catalog.target_pose SET ymove_slug = 'wheel-pose' WHERE target_pose_id = 3;  -- 휠
UPDATE catalog.target_pose SET ymove_slug = 'half-boat-pose' WHERE target_pose_id = 4;  -- 반 보트
UPDATE catalog.target_pose SET ymove_slug = 'boat-pose' WHERE target_pose_id = 5;  -- 보트자세
UPDATE catalog.target_pose SET ymove_slug = 'side-plank-pose-left' WHERE target_pose_id = 6;  -- 사이드 플랭크
UPDATE catalog.target_pose SET ymove_slug = 'bridge-pose' WHERE target_pose_id = 7;  -- 브릿지
UPDATE catalog.target_pose SET ymove_slug = 'garland-pose' WHERE target_pose_id = 8;  -- 말라사나
UPDATE catalog.target_pose SET ymove_slug = 'fire-log-pose-left' WHERE target_pose_id = 9;  -- 파이어로그
