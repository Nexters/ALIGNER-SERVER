-- 보강 운동 29 개가 쓰는 근육.
--
-- 원본은 콘텐츠 정본의 자세 블록마다 붙은 `근육:` 줄이다. `근육:`·`주동:` 은 STRENGTHEN,
-- `신장:` 은 STRETCH 다 — 정본의 "(신장)" 표기가 후자다 (docs/domains.md §4-3).
--
-- **한 운동이 여러 루틴에 나오면 근육 목록을 합집합으로 넣는다.** 예를 들어 고양이-소 자세는
-- 9 개 루틴에 나오는데 표기가 두 가지다("다열근(신전) / 복횡근(굴곡)" 과 "다열근, 복횡근").
-- 운동 상세는 루틴과 무관한 화면이라 한쪽을 고를 근거가 없다.
--
-- 정본 표기를 Figma 22 개로 흡수한 결과다. 다열근·요방형근 → 척추기립근, 복횡근·복사근 →
-- 외복사근, 극하근·소원근 → 회전근개, 대원근 → 광배근, 대퇴사두근 → 대퇴직근,
-- 이상근·소둔근·심부 외회전근 → 중둔근, 가자미근·아킬레스 → 비복근, 장·단내전근·박근·치골근
-- → 내전근. 대응표는 docs/frontend-integration.md 에 둔다.
--
-- display_order 는 정본 표기 순서다. 주동근이 먼저 나오고 신장근이 뒤에 온다.
--
-- **Figma 에 있지만 어느 운동에도 걸리지 않는 근육이 5 개 있다** — 상완이두근·전완근·
-- 전완굴곡근·전경골근·승모근(상부). 요가 정본이 이들을 주동·신장으로 지목하지 않아서이고
-- 데이터 누락이 아니다. 근육맵에서는 회색으로 남는다.
INSERT INTO catalog.exercise_muscle (exercise_id, muscle_code, role, display_order)
VALUES
    -- 101 고양이-소 자세 — 척추 분절 가동성 (웜업)
    (101, 'ERECTOR_SPINAE', 'STRENGTHEN', 1),
    (101, 'EXTERNAL_OBLIQUE', 'STRENGTHEN', 2),

    -- 102 스핑크스 자세 — 요추~흉추 하부 신전
    (102, 'ERECTOR_SPINAE', 'STRENGTHEN', 1),
    (102, 'RECTUS_ABDOMINIS', 'STRETCH', 2),

    -- 103 소 얼굴 자세 — 견관절 외회전+굴곡 / 내회전+신전
    (103, 'DELTOID', 'STRETCH', 1),
    (103, 'ROTATOR_CUFF', 'STRETCH', 2),
    (103, 'LATISSIMUS_DORSI', 'STRETCH', 3),
    (103, 'TRICEPS_BRACHII', 'STRETCH', 4),

    -- 104 호랑이 자세 — 고관절 신전 + 단측 지지 항회전 안정
    (104, 'GLUTEUS_MAXIMUS', 'STRENGTHEN', 1),
    (104, 'ERECTOR_SPINAE', 'STRENGTHEN', 2),
    (104, 'EXTERNAL_OBLIQUE', 'STRENGTHEN', 3),
    (104, 'RECTUS_FEMORIS', 'STRETCH', 4),
    (104, 'ILIOPSOAS', 'STRETCH', 5),

    -- 105 코브라 자세 — 흉추·요추 신전
    (105, 'ERECTOR_SPINAE', 'STRENGTHEN', 1),
    (105, 'TRAPEZIUS_MID_LOWER', 'STRENGTHEN', 2),
    (105, 'RECTUS_ABDOMINIS', 'STRETCH', 3),

    -- 106 업독 〔핀포즈〕 — 흉추 신전 + 견갑 안정 + 손목 하중
    (106, 'ERECTOR_SPINAE', 'STRENGTHEN', 1),
    (106, 'GLUTEUS_MAXIMUS', 'STRENGTHEN', 2),
    (106, 'TRICEPS_BRACHII', 'STRENGTHEN', 3),
    (106, 'SERRATUS_ANTERIOR', 'STRENGTHEN', 4),
    (106, 'ILIOPSOAS', 'STRETCH', 5),
    (106, 'RECTUS_ABDOMINIS', 'STRETCH', 6),
    (106, 'PECTORALIS_MAJOR', 'STRETCH', 7),

    -- 107 퍼피 자세 — 흉추 신전 + 견관절 굴곡 가동범위
    (107, 'LATISSIMUS_DORSI', 'STRETCH', 1),
    (107, 'PECTORALIS_MAJOR', 'STRETCH', 2),

    -- 108 로우 런지 — 고관절 굴곡근 신장
    -- 정본이 대퇴직근을 뒷다리 신장으로, 대퇴사두근을 앞다리 주동으로 함께 적는데 둘 다
    -- RECTUS_FEMORIS 로 흡수된다. 중점이 "고관절 굴곡근 신장" 이라 STRETCH 를 택했다.
    (108, 'GLUTEUS_MAXIMUS', 'STRENGTHEN', 1),
    (108, 'ILIOPSOAS', 'STRETCH', 2),
    (108, 'RECTUS_FEMORIS', 'STRETCH', 3),

    -- 109 브릿지 〔핀포즈〕 — 고관절 신전
    (109, 'GLUTEUS_MAXIMUS', 'STRENGTHEN', 1),
    (109, 'HAMSTRING', 'STRENGTHEN', 2),
    (109, 'ERECTOR_SPINAE', 'STRENGTHEN', 3),
    (109, 'ADDUCTOR', 'STRENGTHEN', 4),
    (109, 'ILIOPSOAS', 'STRETCH', 5),
    (109, 'RECTUS_FEMORIS', 'STRETCH', 6),

    -- 110 낙타자세 〔핀포즈〕 — 흉추 신전 + 고관절 신전
    (110, 'ERECTOR_SPINAE', 'STRENGTHEN', 1),
    (110, 'GLUTEUS_MAXIMUS', 'STRENGTHEN', 2),
    (110, 'TRAPEZIUS_MID_LOWER', 'STRENGTHEN', 3),
    (110, 'ILIOPSOAS', 'STRETCH', 4),
    (110, 'RECTUS_FEMORIS', 'STRETCH', 5),
    (110, 'RECTUS_ABDOMINIS', 'STRETCH', 6),
    (110, 'PECTORALIS_MAJOR', 'STRETCH', 7),

    -- 111 활 자세 — 전신 후굴 + 어깨 신전
    (111, 'ERECTOR_SPINAE', 'STRENGTHEN', 1),
    (111, 'GLUTEUS_MAXIMUS', 'STRENGTHEN', 2),
    (111, 'HAMSTRING', 'STRENGTHEN', 3),
    (111, 'RECTUS_FEMORIS', 'STRETCH', 4),
    (111, 'ILIOPSOAS', 'STRETCH', 5),
    (111, 'PECTORALIS_MAJOR', 'STRETCH', 6),

    -- 112 휠 〔핀포즈〕 — 전신 후굴 + 어깨 굴곡 가동범위
    (112, 'ERECTOR_SPINAE', 'STRENGTHEN', 1),
    (112, 'GLUTEUS_MAXIMUS', 'STRENGTHEN', 2),
    (112, 'HAMSTRING', 'STRENGTHEN', 3),
    (112, 'DELTOID', 'STRENGTHEN', 4),
    (112, 'TRICEPS_BRACHII', 'STRENGTHEN', 5),
    (112, 'ILIOPSOAS', 'STRETCH', 6),
    (112, 'PECTORALIS_MAJOR', 'STRETCH', 7),
    (112, 'RECTUS_ABDOMINIS', 'STRETCH', 8),

    -- 113 누워서 와이퍼 — 요추 회전 가동성 (웜업)
    (113, 'EXTERNAL_OBLIQUE', 'STRENGTHEN', 1),
    (113, 'ERECTOR_SPINAE', 'STRENGTHEN', 2),

    -- 114 플랭크 자세 — 코어 등척성 + 견갑 안정
    (114, 'RECTUS_ABDOMINIS', 'STRENGTHEN', 1),
    (114, 'EXTERNAL_OBLIQUE', 'STRENGTHEN', 2),
    (114, 'SERRATUS_ANTERIOR', 'STRENGTHEN', 3),
    (114, 'DELTOID', 'STRENGTHEN', 4),

    -- 115 앉은 전굴 자세 — 후방 사슬 신장
    (115, 'HAMSTRING', 'STRETCH', 1),
    (115, 'GASTROCNEMIUS', 'STRETCH', 2),
    (115, 'ERECTOR_SPINAE', 'STRETCH', 3),

    -- 116 반 보트 〔핀포즈〕 — 요추 중립을 유지한 고관절 굴곡
    (116, 'RECTUS_ABDOMINIS', 'STRENGTHEN', 1),
    (116, 'ILIOPSOAS', 'STRENGTHEN', 2),
    (116, 'EXTERNAL_OBLIQUE', 'STRENGTHEN', 3),
    (116, 'ERECTOR_SPINAE', 'STRENGTHEN', 4),

    -- 117 보트자세 〔핀포즈〕 — 코어 근력 + 햄스트링 유연성 동시 요구
    (117, 'RECTUS_ABDOMINIS', 'STRENGTHEN', 1),
    (117, 'ILIOPSOAS', 'STRENGTHEN', 2),
    (117, 'ERECTOR_SPINAE', 'STRENGTHEN', 3),
    (117, 'RECTUS_FEMORIS', 'STRENGTHEN', 4),
    (117, 'HAMSTRING', 'STRETCH', 5),

    -- 118 문 빗장 자세 — 측면 사슬(체간 외측)
    (118, 'EXTERNAL_OBLIQUE', 'STRENGTHEN', 1),
    (118, 'ERECTOR_SPINAE', 'STRETCH', 2),
    (118, 'LATISSIMUS_DORSI', 'STRETCH', 3),
    (118, 'ADDUCTOR', 'STRETCH', 4),

    -- 119 반 물고기의 왕 자세 — 흉추 회전
    (119, 'EXTERNAL_OBLIQUE', 'STRENGTHEN', 1),
    (119, 'ERECTOR_SPINAE', 'STRENGTHEN', 2),
    (119, 'GLUTEUS_MAXIMUS', 'STRETCH', 3),
    (119, 'GLUTEUS_MEDIUS', 'STRETCH', 4),
    (119, 'LATISSIMUS_DORSI', 'STRETCH', 5),

    -- 120 사이드 플랭크 〔핀포즈〕 — 측면 코어 + 견갑·어깨 안정
    (120, 'EXTERNAL_OBLIQUE', 'STRENGTHEN', 1),
    (120, 'ERECTOR_SPINAE', 'STRENGTHEN', 2),
    (120, 'GLUTEUS_MEDIUS', 'STRENGTHEN', 3),
    (120, 'DELTOID', 'STRENGTHEN', 4),
    (120, 'SERRATUS_ANTERIOR', 'STRENGTHEN', 5),

    -- 121 누운 나비 자세 — 고관절 외회전·외전 수동 이완
    (121, 'ADDUCTOR', 'STRETCH', 1),

    -- 122 해피 베이비 자세 — 고관절 굴곡 가동범위 + 요추 이완
    (122, 'GLUTEUS_MAXIMUS', 'STRETCH', 1),
    (122, 'HAMSTRING', 'STRETCH', 2),
    (122, 'ADDUCTOR', 'STRETCH', 3),

    -- 123 누운 비둘기 자세 — 심부 외회전근 이완
    (123, 'GLUTEUS_MEDIUS', 'STRETCH', 1),
    (123, 'GLUTEUS_MAXIMUS', 'STRETCH', 2),

    -- 124 다운독 — 발목 배측굴곡 + 후방 사슬
    (124, 'SERRATUS_ANTERIOR', 'STRENGTHEN', 1),
    (124, 'DELTOID', 'STRENGTHEN', 2),
    (124, 'GASTROCNEMIUS', 'STRETCH', 3),
    (124, 'HAMSTRING', 'STRETCH', 4),
    (124, 'LATISSIMUS_DORSI', 'STRETCH', 5),

    -- 125 나비 자세 — 고관절 외회전 + 내전근
    (125, 'GLUTEUS_MEDIUS', 'STRENGTHEN', 1),
    (125, 'ADDUCTOR', 'STRETCH', 2),

    -- 126 개구리 자세 — 고관절 외전 + 내전근 심화
    (126, 'ADDUCTOR', 'STRETCH', 1),

    -- 127 의자 자세 — 하체 등척성 근력 + 발목 배측굴곡
    (127, 'RECTUS_FEMORIS', 'STRENGTHEN', 1),
    (127, 'GLUTEUS_MAXIMUS', 'STRENGTHEN', 2),
    (127, 'ERECTOR_SPINAE', 'STRENGTHEN', 3),
    (127, 'GASTROCNEMIUS', 'STRENGTHEN', 4),

    -- 128 말라사나 〔핀포즈〕 — 깊은 고관절 굴곡·외회전 + 발목 배측굴곡
    (128, 'GLUTEUS_MAXIMUS', 'STRENGTHEN', 1),
    (128, 'ERECTOR_SPINAE', 'STRENGTHEN', 2),
    (128, 'RECTUS_FEMORIS', 'STRENGTHEN', 3),
    (128, 'ADDUCTOR', 'STRETCH', 4),
    (128, 'GASTROCNEMIUS', 'STRETCH', 5),

    -- 129 파이어로그 〔핀포즈〕 — 극단적 고관절 외회전
    (129, 'GLUTEUS_MEDIUS', 'STRETCH', 1),
    (129, 'GLUTEUS_MAXIMUS', 'STRETCH', 2);
