-- 근육 마스터 22 행.
--
-- **행 집합의 정본은 Figma 근육맵 컴포넌트다** — `front/기본`(758:8956) 12 개,
-- `back`(758:8957) 10 개. 프론트가 칠할 수 있는 근육이 이 22 개뿐이므로 그 밖의 근육을
-- 여기에 넣으면 응답에는 나가지만 화면에 아무 일도 일어나지 않는다.
--
-- `muscle_code` 는 해부학 영문이고 `name` 은 Figma 레이어명 그대로다. 프론트는 name 으로
-- 레이어를 찾고 서버·DB 는 code 로 다룬다. 콘텐츠 정본의 근육 표기(다열근·복횡근 등)를
-- 이 22 개 중 하나로 흡수하는 대응표는 docs/frontend-integration.md 에 둔다.
--
-- **`muscle_code` 를 코드에 enum 으로 두지 않는다.** 값 집합이 요가 지도자 감수 대상이라
-- seed 하드코딩 금지에 걸린다 (docs/architecture.md §6). MuscleRole 은 반대로 docs/domains.md
-- §4-3 이 값 집합을 확정한 닫힌 어휘라 enum 이다. 코드가 22 개와 어긋나는지는
-- CatalogSeedIntegrationTest 가 잡는다.
--
-- body_part_code 는 screening 소유 어휘이고 BACK·ABDOMEN·PELVIS 셋뿐이다. 정본에서 그 근육이
-- 실제로 등장하는 맥락을 따랐다 — 대흉근·광배근은 "흉추 신전" 의 신장근으로 등 라인에,
-- 비복근·전경골근은 "발목 배측굴곡" 으로 말라사나(골반) 라인에 나온다.
--
-- **팔 근육 5 개(델토근·상완이두근·상완삼두근·전완근·전완굴곡근)의 BACK 배정은 잠정이다.**
-- 세 부위 중 자연스러운 자리가 없어 상체로 몰았다. 근육맵 탭 구성이 확정되면 UPDATE
-- changeset 을 새로 쌓는다.
--
-- 하이라이트 키는 `muscle/{소문자 코드}_{front|back}` 규칙이다. URL 이 아니라 키이고 파일은
-- 프론트가 갖는다 (docs/domains.md §4-3). target_pose 의 `target-pose/{slug}`,
-- exercise 의 `exercise/{slug}` 와 같은 형태다.
--
-- **앞뒤 양쪽 키를 가진 근육이 하나도 없다.** Figma 가 22 개를 앞/뒤로 완전히 갈라놨기
-- 때문이고, 한쪽이 NULL 인 것은 정상이다 (ddl/007). 양쪽에 걸치는 레이어가 생기면 그때 채운다.
INSERT INTO catalog.muscle (muscle_code, name, body_part_code, front_highlight_asset_key, back_highlight_asset_key)
VALUES
    -- front/기본 (758:8956) — 12 개
    ('DELTOID', '델토근', 'BACK', 'muscle/deltoid_front', NULL),
    ('PECTORALIS_MAJOR', '대흉근', 'BACK', 'muscle/pectoralis_major_front', NULL),
    ('SERRATUS_ANTERIOR', '전거근', 'BACK', 'muscle/serratus_anterior_front', NULL),
    ('BICEPS_BRACHII', '상완이두근', 'BACK', 'muscle/biceps_brachii_front', NULL),
    ('FOREARM', '전완근', 'BACK', 'muscle/forearm_front', NULL),
    ('FOREARM_FLEXOR', '전완굴곡근', 'BACK', 'muscle/forearm_flexor_front', NULL),
    ('RECTUS_ABDOMINIS', '복직근', 'ABDOMEN', 'muscle/rectus_abdominis_front', NULL),
    ('EXTERNAL_OBLIQUE', '외복사근', 'ABDOMEN', 'muscle/external_oblique_front', NULL),
    ('ILIOPSOAS', '장요근', 'PELVIS', 'muscle/iliopsoas_front', NULL),
    ('RECTUS_FEMORIS', '대퇴직근', 'PELVIS', 'muscle/rectus_femoris_front', NULL),
    ('ADDUCTOR', '내전근', 'PELVIS', 'muscle/adductor_front', NULL),
    ('TIBIALIS_ANTERIOR', '전경골근', 'PELVIS', 'muscle/tibialis_anterior_front', NULL),

    -- back (758:8957) — 10 개
    ('TRAPEZIUS', '승모근', 'BACK', NULL, 'muscle/trapezius_back'),
    ('TRAPEZIUS_MID_LOWER', '중 하부 승모근', 'BACK', NULL, 'muscle/trapezius_mid_lower_back'),
    ('ROTATOR_CUFF', '회전근개', 'BACK', NULL, 'muscle/rotator_cuff_back'),
    ('LATISSIMUS_DORSI', '광배근', 'BACK', NULL, 'muscle/latissimus_dorsi_back'),
    -- Figma 레이어명은 `이부근`(758:9032)이지만 뒷면 양팔 위쪽이라 상완삼두근이다.
    -- 레이어명 오타로 보고 해부학 이름을 쓴다.
    ('TRICEPS_BRACHII', '상완삼두근', 'BACK', NULL, 'muscle/triceps_brachii_back'),
    ('ERECTOR_SPINAE', '척추기립근', 'BACK', NULL, 'muscle/erector_spinae_back'),
    ('GLUTEUS_MEDIUS', '중둔근', 'PELVIS', NULL, 'muscle/gluteus_medius_back'),
    ('GLUTEUS_MAXIMUS', '대둔근', 'PELVIS', NULL, 'muscle/gluteus_maximus_back'),
    ('HAMSTRING', '햄스트링', 'PELVIS', NULL, 'muscle/hamstring_back'),
    -- Figma 에 프레임이 둘(758:9033, 758:9035)이지만 좌우 분리로 보고 한 행에 키 하나를 준다.
    ('GASTROCNEMIUS', '비복근', 'PELVIS', NULL, 'muscle/gastrocnemius_back');
