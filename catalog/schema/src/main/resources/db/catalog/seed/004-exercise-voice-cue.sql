-- 세션 재생 중 읽어주는 음성 큐. **한글 번역본이고 catalog 가 소유한다** (docs/domains.md §4-3-1).
--
-- 원문은 YMove 의 instructions(영어)이고 번역은 docs/context/routine-content.md 의 각 자세
-- "순서" 항목이다. 정본이 곧 감수 산출물이라 여기서 문장을 새로 만들지 않는다 —
-- **사용자에게 그대로 읽히는 문장이라 한 글자도 바꾸지 않는다.**
--
-- display_order 는 정본 순서 목록의 번호 그대로다. YMove instructions 가 배열이라
-- 배열 인덱스가 그대로 여기에 대응한다.
--
-- start_offset_seconds · end_offset_seconds 는 전부 NULL 이다. 타임코드를 영상 재생 시각에
-- 맞출지 클라이언트 타이머에 맞출지가 미정이라(docs/domains.md §7-15) 지금은 순차 재생이다.
-- 확정되면 UPDATE changeset 으로 값만 채운다. 스키마는 그대로다.
--
-- **좌우로 나뉘는 자세는 003-update-ymove-link.sql 이 고른 쪽 기준이다.** 그래야 영상과
-- 대본이 어긋나지 않는다 — 호랑이 자세를 오른쪽 영상으로 재생하면서 왼쪽 지시를 읽어주면
-- 잘못된 지도가 된다. 002-exercise.sql 이 음성 큐를 미뤄둔 이유가 이것이었다.
--
-- 고양이-소 자세는 정본에 9 회 등장하는데 1 번 문장이 "네발기기 자세"(8 회)와
-- "네발로 기는 자세"(1 회)로 갈린다. 같은 뜻이라 다수인 앞의 것을 쓴다.


-- 101 고양이-소 자세 (cat-cow-pose)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (101, 1, '손목은 어깨 아래, 무릎은 골반 아래에 두고 네발기기 자세로 시작합니다.'),
       (101, 2, '숨을 마시며 배를 매트 쪽으로 내리고, 가슴과 꼬리뼈를 들어 올립니다 (소 자세).'),
       (101, 3, '숨을 내쉬며 척추를 천장 쪽으로 둥글게 말고, 턱과 꼬리뼈를 안으로 말아 넣습니다 (고양이 자세).'),
       (101, 4, '호흡에 맞춰 고양이와 소 자세를 이어서 반복합니다.');

-- 102 스핑크스 자세 (sphinx-pose)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (102, 1, '엎드린 자세에서 팔꿈치가 어깨 아래에 오도록 전완을 바닥에 놓습니다.'),
       (102, 2, '전완으로 바닥을 밀어내며 가슴을 들어 올립니다.'),
       (102, 3, '다리는 곧게 뻗고 발등은 바닥에 붙인 상태를 유지합니다.'),
       (102, 4, '가슴을 부드럽게 들어 올린 채로 호흡하며 유지합니다.');

-- 103 소 얼굴 자세 (cow-face-pose-left)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (103, 1, '바닥에 앉아 두 무릎을 위아래로 포갭니다.'),
       (103, 2, '한 팔을 머리 위로 뻗어 등 뒤로 구부립니다.'),
       (103, 3, '다른 팔을 아래에서 등 뒤로 뻗습니다.'),
       (103, 4, '손가락을 맞잡거나 두 손 사이에 스트랩을 사용합니다.');

-- 104 호랑이 자세 (tiger-pose-right)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (104, 1, '손은 어깨 바로 아래, 무릎은 골반 아래에 두고 테이블탑 자세로 시작합니다.'),
       (104, 2, '오른쪽 다리를 뒤로 들어 무릎을 구부리고 발을 엉덩이 쪽으로 가져옵니다.'),
       (104, 3, '오른손을 뒤로 뻗어 오른발등을 잡습니다.'),
       (104, 4, '가슴을 살짝 들어 올리면서 발을 몸쪽으로 부드럽게 당깁니다.'),
       (104, 5, '왼손과 왼무릎으로 균형을 잡으며 자세를 유지합니다.'),
       (104, 6, '발을 놓고 천천히 테이블탑 자세로 돌아옵니다.');

-- 105 코브라 자세 (cobra-pose)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (105, 1, '손을 어깨 아래에 두고 엎드립니다.'),
       (105, 2, '골반과 다리로 바닥을 눌러줍니다.'),
       (105, 3, '숨을 마시며 팔을 펴 가슴을 들어 올립니다.'),
       (105, 4, '팔꿈치는 살짝 구부린 상태를 유지합니다.');

-- 106 업독 (upward-facing-dog-pose)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (106, 1, '손을 갈비뼈 아래쪽 옆에 두고 엎드립니다.'),
       (106, 2, '손으로 바닥을 밀어 가슴과 허벅지를 바닥에서 들어 올립니다.'),
       (106, 3, '팔을 곧게 펴고 어깨를 뒤로 말아줍니다.'),
       (106, 4, '발등으로 매트를 계속 눌러줍니다.');

-- 107 퍼피 자세 (puppy-pose)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (107, 1, '골반이 무릎 바로 위에 오도록 네발기기 자세로 시작합니다.'),
       (107, 2, '손을 앞으로 걸어 나가며 가슴을 바닥 쪽으로 낮춥니다.'),
       (107, 3, '골반은 계속 무릎 위에 유지합니다.'),
       (107, 4, '이마를 매트에 대고 가슴을 아래로 녹이듯 내립니다.');

-- 108 로우 런지 (low-lunge-left)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (108, 1, '다운독 자세에서 한쪽 발을 두 손 사이로 내딛습니다.'),
       (108, 2, '뒤쪽 무릎을 매트에 내립니다.'),
       (108, 3, '앞쪽 무릎이 발목 바로 위에 오도록 둡니다.'),
       (108, 4, '팔을 머리 위로 들어 올리거나 손을 바닥에 둡니다.');

-- 109 브릿지 (bridge-pose)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (109, 1, '무릎을 세우고 발을 골반 너비로 벌려 바닥에 붙인 채 눕습니다.'),
       (109, 2, '손바닥이 아래를 향하도록 팔을 몸 옆에 놓습니다.'),
       (109, 3, '발로 바닥을 밀며 골반을 천장 쪽으로 들어 올립니다.'),
       (109, 4, '가장 높은 지점에서 유지한 뒤 천천히 내려옵니다.');

-- 110 낙타자세 (camel-pose)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (110, 1, '무릎을 골반 너비로 벌리고 허벅지가 바닥과 수직이 되도록 무릎으로 섭니다.'),
       (110, 2, '손가락이 아래를 향하도록 두 손을 허리에 얹습니다.'),
       (110, 3, '숨을 마시며 가슴을 들어 올리고 천천히 뒤로 젖힙니다.'),
       (110, 4, '편안하다면 손을 뒤로 뻗어 발뒤꿈치를 잡습니다.');

-- 111 활 자세 (bow-pose)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (111, 1, '팔을 몸 옆에 둔 채 매트에 엎드립니다.'),
       (111, 2, '무릎을 구부리고 뒤로 손을 뻗어 발목을 잡습니다.'),
       (111, 3, '숨을 마시며 가슴과 허벅지를 동시에 바닥에서 들어 올립니다.'),
       (111, 4, '고르게 호흡하며 자세를 유지한 뒤, 숨을 내쉬며 풀어줍니다.');

-- 112 휠 (wheel-pose)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (112, 1, '무릎을 세우고 발바닥을 바닥에 붙인 채 등을 대고 눕습니다.'),
       (112, 2, '손가락이 어깨를 향하도록 두 손을 귀 옆 바닥에 놓습니다.'),
       (112, 3, '손과 발로 바닥을 밀어 몸을 들어 올립니다.'),
       (112, 4, '팔과 다리를 가능한 만큼 곧게 폅니다.');

-- 113 누워서 와이퍼 (reclined-windshield-wipers)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (113, 1, '무릎을 세우고 발을 골반보다 넓게 벌려 바닥에 붙인 채 눕습니다.'),
       (113, 2, '양팔을 옆으로 뻗습니다.'),
       (113, 3, '두 무릎을 한쪽으로 넘겼다가 반대쪽으로 넘깁니다.'),
       (113, 4, '호흡에 맞춰 천천히 움직입니다.');

-- 114 플랭크 자세 (plank-pose)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (114, 1, '네발기기 자세에서 발을 뒤로 보내 다리를 곧게 폅니다.'),
       (114, 2, '머리부터 발뒤꿈치까지 몸을 일직선으로 정렬합니다.'),
       (114, 3, '어깨가 손목 바로 위에 오도록 놓습니다.'),
       (114, 4, '코어와 다리에 힘을 준 채 유지합니다.');

-- 115 앉은 전굴 자세 (seated-forward-bend)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (115, 1, '다리를 앞으로 곧게 뻗고 앉습니다.'),
       (115, 2, '숨을 마시며 척추를 길게 늘입니다.'),
       (115, 3, '숨을 내쉬며 고관절에서부터 앞으로 접어 발을 향해 손을 뻗습니다.'),
       (115, 4, '발, 발목 또는 정강이를 잡습니다.');

-- 116 반 보트 (half-boat-pose)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (116, 1, '무릎을 세우고 발바닥을 바닥에 붙인 채 앉습니다.'),
       (116, 2, '상체를 살짝 뒤로 기울이고 발을 바닥에서 들어 올립니다.'),
       (116, 3, '정강이를 바닥과 평행하게 유지합니다 (무릎 90도).'),
       (116, 4, '팔을 바닥과 평행하게 앞으로 뻗습니다.');

-- 117 보트자세 (boat-pose)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (117, 1, '무릎을 세우고 발바닥을 바닥에 붙인 채 앉습니다.'),
       (117, 2, '상체를 살짝 뒤로 기울이고 발을 바닥에서 들어 올립니다.'),
       (117, 3, '팔을 바닥과 평행하게 앞으로 뻗습니다.'),
       (117, 4, '다리를 곧게 펴 몸으로 V자를 만듭니다.');

-- 118 문 빗장 자세 (gate-pose-left)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (118, 1, '무릎으로 선 자세에서 한쪽 다리를 옆으로 뻗습니다.'),
       (118, 2, '뻗은 발의 발바닥을 바닥에 붙이고 발끝은 옆을 향하게 합니다.'),
       (118, 3, '숨을 마시며 무릎을 꿇은 쪽 팔을 머리 위로 들어 올립니다.'),
       (118, 4, '숨을 내쉬며 뻗은 다리 쪽으로 기울이고, 반대쪽 손을 다리를 따라 내립니다.');

-- 119 반 물고기의 왕 자세 (half-lord-of-the-fishes-pose-left)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (119, 1, '다리를 뻗고 앉습니다. 한쪽 무릎을 구부려 발을 반대쪽 허벅지 바깥에 놓습니다.'),
       (119, 2, '아래쪽 다리를 구부려 발을 반대쪽 엉덩이 가까이 가져옵니다.'),
       (119, 3, '구부린 무릎 쪽으로 상체를 비틉니다.'),
       (119, 4, '반대쪽 팔꿈치를 구부린 무릎 바깥에 대어 지렛대로 사용합니다.');

-- 120 사이드 플랭크 (side-plank-pose-left)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (120, 1, '플랭크 자세에서 한쪽 손과 같은 쪽 발 옆면으로 체중을 옮깁니다.'),
       (120, 2, '두 발을 포개고 위쪽 팔을 천장을 향해 엽니다.'),
       (120, 3, '머리부터 발까지 몸을 일직선으로 유지합니다.'),
       (120, 4, '고르게 호흡하며 유지합니다.');

-- 121 누운 나비 자세 (reclined-butterfly)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (121, 1, '등을 대고 누워 양 발바닥을 마주 붙입니다.'),
       (121, 2, '무릎이 양옆으로 자연스럽게 벌어지도록 둡니다.'),
       (121, 3, '손바닥이 위를 향하도록 팔을 몸 옆에 놓습니다.'),
       (121, 4, '눈을 감고 몇 분간 이완합니다.');

-- 122 해피 베이비 자세 (happy-baby-pose)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (122, 1, '등을 대고 누워 무릎을 가슴 쪽으로 가져옵니다.'),
       (122, 2, '두 손으로 발 바깥쪽을 잡습니다.'),
       (122, 3, '무릎을 몸통보다 넓게 벌립니다.'),
       (122, 4, '허리를 매트에 붙인 채 발을 아래로 부드럽게 당깁니다.');

-- 123 누운 비둘기 자세 (reclined-pigeon-pose-left)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (123, 1, '무릎을 세우고 발바닥을 바닥에 붙인 채 눕습니다.'),
       (123, 2, '한쪽 발목을 반대쪽 무릎 위에 올려 교차시킵니다.'),
       (123, 3, '두 손을 지지하는 쪽 허벅지 뒤로 넣어 깍지 낍니다.'),
       (123, 4, '허벅지를 가슴 쪽으로 부드럽게 당깁니다.');

-- 124 다운독 (downward-dog)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (124, 1, '네발기기 자세에서 발끝을 세우고 골반을 위쪽 뒤로 들어 올립니다.'),
       (124, 2, '발뒤꿈치를 바닥 쪽으로 누르며 다리를 최대한 곧게 폅니다.'),
       (124, 3, '손가락을 넓게 펴고 손바닥으로 바닥을 단단히 밀어냅니다.'),
       (124, 4, '머리는 두 팔 사이에서 자연스럽게 늘어뜨립니다.');

-- 125 나비 자세 (cobblers-pose)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (125, 1, '양 발바닥을 마주 붙이고 바닥에 앉습니다.'),
       (125, 2, '무릎이 양옆으로 벌어지도록 둡니다.'),
       (125, 3, '두 손으로 발이나 발목을 잡습니다.'),
       (125, 4, '척추를 길게 늘이며 바르게 앉습니다.');

-- 126 개구리 자세 (frog-pose)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (126, 1, '매트 위에 무릎을 대고 앉았다가 전완까지 천천히 내려옵니다.'),
       (126, 2, '정강이를 서로 나란히 유지한 채 무릎을 넓게 벌립니다.'),
       (126, 3, '전완을 바닥에 놓고 골반이 바닥 쪽으로 가라앉도록 둡니다.'),
       (126, 4, '깊게 호흡하며 스트레칭에 몸을 맡기는 데 집중하며 자세를 유지합니다.'),
       (126, 5, '편안하게 느껴지는 범위까지만 내려가고 억지로 밀어붙이지 않습니다.'),
       (126, 6, '유연성 수준에 따라 30초~2분간 유지합니다.');

-- 127 의자 자세 (chair-pose)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (127, 1, '발을 모으거나 골반 너비로 벌리고 섭니다.'),
       (127, 2, '무릎을 굽히고 의자에 앉듯 골반을 낮춥니다.'),
       (127, 3, '팔을 귀 옆으로 나란히 머리 위로 들어 올립니다.'),
       (127, 4, '체중을 발뒤꿈치에 실은 채 유지합니다.');

-- 128 말라사나 (garland-pose)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (128, 1, '발을 골반보다 약간 넓게 벌리고 발끝을 바깥으로 돌려 섭니다.'),
       (128, 2, '무릎을 굽혀 깊은 스쿼트로 내려갑니다.'),
       (128, 3, '가슴 앞에서 두 손바닥을 마주 붙입니다.'),
       (128, 4, '팔꿈치로 무릎 안쪽을 밀어 고관절을 열어줍니다.');

-- 129 파이어로그 (fire-log-pose-left)
INSERT INTO catalog.exercise_voice_cue (exercise_id, display_order, content)
VALUES (129, 1, '왼쪽 다리를 앞에 두고 무릎을 90도로 구부려 정강이가 매트 앞쪽 가장자리와 나란하도록 앉습니다.'),
       (129, 2, '오른쪽 다리를 위에 올려, 두 무릎을 구부린 채 오른쪽 정강이를 왼쪽 정강이 바로 위에 포갭니다.'),
       (129, 3, '무릎 관절을 보호하기 위해 양발을 플렉스 상태로 유지합니다.'),
       (129, 4, '척추를 곧게 세워 앉고 두 손은 정강이나 바닥에 얹습니다.'),
       (129, 5, '깊게 호흡하며 30~60초간 유지합니다.'),
       (129, 6, '풀 때는 위쪽 다리를 조심스럽게 들어 올린 뒤 두 다리를 앞으로 폅니다.');
