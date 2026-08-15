# 콘텐츠 정본 — 9개 루틴 구성과 자세별 대본

`docs/domains.md`가 "콘텐츠 정본"으로 참조하던 문서다. 노션 원본을 옮겼다.

**이 문서는 감수 대상 데이터의 정본이다.** `AGENTS.md` §6과 `docs/architecture.md` §6에 따라
여기 있는 값을 코드에 하드코딩하지 않는다. seed changeset(`catalog/schema/seed/`,
`course/schema/seed/`)으로만 넣는다.

> **전사본이다.** 노션 원본과 diff 한 뒤 커밋한다. 특히 주의사항 문구는 사용자에게 그대로
> 읽히는 안전 문구라 한 글자도 달라지면 안 된다.

---

# 0. 개수 및 수정 사항

## 0.1 최종 동작 수

| 구분 | 개수 |
| --- | --- |
| 9개 루틴에 배치된 고유 자세 | **29개** |
| 그중 핀포즈 | 9개 |
| 그중 준비·보완 자세 | 20개 |
| ymove exercise 기준 (좌우 분리) | 37개 |

좌우 분리 자세 8개: Cow face, Tiger, Low lunge, Gate, Half lord of the fishes, Side plank, Reclined pigeon, Fire log.

**33 → 29로 줄어든 경로**
- 체크포인트 대응 전용 3개 제외: Half split, Standing forward bend, Seated wide angle straddle → 30개

## 0.2 루틴 개요

| 라인 | 레벨 | 핀포즈 (EN / KO) | 길이 | 동작 수 | 휴식 | MET |
| --- | --- | --- | --- | --- | --- | --- |
| 등 | 1 | Upward facing dog / 업독 | 15분 | 6 | 30초×5 | 2.3 |
| 등 | 2 | Camel pose / 낙타자세 | 17분 30초 | 7 | 30초×6 | 2.8 |
| 등 | 3 | Wheel pose / 휠 | 20분 | 8 | 30초×7 | 3.0 |
| 복부 | 1 | Half boat pose / 반 보트 | 15분 | 6 | 30초×5 | 2.3 |
| 복부 | 2 | Boat pose / 보트자세 | 17분 30초 | 7 | 30초×6 | 2.8 |
| 복부 | 3 | Side plank pose / 사이드 플랭크 | 20분 | 8 | 30초×7 | 3.0 |
| 골반 | 1 | Bridge pose / 브릿지 | 15분 | 6 | 30초×5 | 2.3 |
| 골반 | 2 | Garland pose / 말라사나 | 17분 30초 | 7 | 30초×6 | 2.8 |
| 골반 | 3 | Fire log pose / 파이어로그 | 20분 | 8 | 30초×7 | 3.0 |

각 자세 블록의 `중점 / 근육`은 작성자가 판정한 값이고, 순서와 주의사항 영문은 YMove API 원본, 한국어는 작성자 번역이다.

## 0.3 휴식 설계 (타이머 제외 · 자세 추가 없음)

현재 정책(`PL-REC-034`)은 동작 사이마다 30초 휴식을 균일하게 넣는다(레벨1 ×5, 레벨2 ×6, 레벨3 ×7). 이 구조를 아래와 같이 바꾼다.

### 구간 두 종류

| 종류 | 언제 | 시간 | 화면 |
| --- | --- | --- | --- |
| **전환** | 이완 자세 → 이완 자세, 이완 자세 → 근력 자세 | 10초 | 카운트다운 없음. "다음 자세 준비 — {다음 자세명}"만 표시. |
| **휴식** | 근력 자세 직후, 후굴 자세 직후, 핀포즈 직전, 핀포즈 세트 사이, 루틴 종료 | 30~60초 | 진행 표시(링 또는 바) + 안내 문구 + 다음 자세 이름. |

건너뛰기를 휴식에서 막는 이유는 이 구간의 목적이 과부하 방지이기 때문이다. 전환은 숙련자가 흐름을 끊기지 않도록 열어둔다.

### 휴식 화면 구성 요소 3가지

1. **남은 시간** — 숫자 카운트다운이 부담스러우면 링이나 바 형태의 진행 표시로 충분하다. 다만 "언제 끝나는지 알 수 있어야 한다"는 조건은 반드시 지켜야 한다.
2. **안내 문구 한 줄** — 아래 3종 중 직전 자세의 자세 형태에 따라 자동 선택한다.
3. **다음 자세 이름** — 특히 이완에서 근력으로 넘어가는 구간에서 마음의 준비를 만들어주므로 빼지 않는다.

### 안내 문구 3종

| 코드 | 조건 | 문구 |
| --- | --- | --- |
| **A** | 직전 자세를 **누운 상태**로 끝냄 (브릿지, 누운 비둘기, 해피 베이비, 누운 나비, 와이퍼) | 그대로 누워서 편하게 호흡하세요 |
| **B** | 직전 자세가 **후굴이거나 엎드림·네발기기**로 끝남 (코브라, 업독, 낙타, 활, 휠, 호랑이, 플랭크, 퍼피) | 무릎을 꿇고 상체를 앞으로 숙여 쉬세요 |
| **C** | 직전 자세를 **앉거나 선 상태**로 끝냄 (보트, 반 보트, 사이드 플랭크, 의자, 말라사나, 파이어로그, 소 얼굴, 나비, 개구리) | 편하게 앉아서 쉬세요 |

---

# 1. 등 라인 · 레벨 1 — Upward facing dog (업독) · 15분

제한 요인: 어깨 전면 경직, 흉추 신전 부족, 둔근·햄스트링 약화, 손목 지지력

**루틴 순서**

| # | 동작 | 시간 |
| --- | --- | --- |
| 1 | Cat cow pose · 고양이-소 자세 | 2분 |
| 2 | Sphinx pose · 스핑크스 자세 | 2분 |
| 3 | Cow face pose (좌/우) · 소 얼굴 자세 | 2분 |
| 4 | Tiger pose (좌/우) · 호랑이 자세 | 2분 |
| 5 | Cobra pose · 코브라 자세 | 2분 30초 |
| 6 | **Upward facing dog pose · 업독** 〔핀포즈〕 | 2분 (30초 × 3) |

**휴식 배치**

| 구간 | 종류 | 시간 | 화면 문구 |
| --- | --- | --- | --- |
| 1 → 2 | 전환 | 10초 | 다음 자세 준비 — 스핑크스 자세 |
| 2 → 3 | 전환 | 10초 | 다음 자세 준비 — 소 얼굴 자세 |
| 3 → 4 | 전환 | 10초 | 다음 자세 준비 — 호랑이 자세 |
| 4 → 5 | 휴식 〔B〕 | 30초 | 무릎을 꿇고 상체를 앞으로 숙여 쉬세요 |
| 5 → 6 (핀포즈 직전) | 휴식 〔B〕 | 30초 | 무릎을 꿇고 상체를 앞으로 숙여 쉬세요 |
| 핀포즈 세트 사이 ×2 | 휴식 〔B〕 | 핀포즈 2분 내 처리 | 무릎을 꿇고 상체를 앞으로 숙여 쉬세요 |
| 루틴 종료 | 마무리 〔B〕 | 60초 | 무릎을 꿇고 상체를 앞으로 숙여 쉬세요 |

휴식 총량 150초 (전환 30 + 휴식 60 + 마무리 60) — 기존과 동일, 15분 유지.

## 1-1. Cat cow pose · 고양이-소 자세 — 2분

`cat-cow-pose` 〔API: back〕
중점: 흉추·요추 분절 가동성 (웜업) | 근육: 척추기립근, 다열근(신전) / 복횡근(굴곡)

**순서**
1. Start on all fours with wrists under shoulders and knees under hips. / 손목은 어깨 아래, 무릎은 골반 아래에 두고 네발로 기는 자세로 시작합니다.
2. Inhale, drop your belly toward the mat, lift your chest and tailbone (Cow). / 숨을 마시며 배를 매트 쪽으로 내리고, 가슴과 꼬리뼈를 들어 올립니다 (소 자세).
3. Exhale, round your spine toward the ceiling, tuck your chin and tailbone (Cat). / 숨을 내쉬며 척추를 천장 쪽으로 둥글게 말고, 턱과 꼬리뼈를 안으로 말아 넣습니다 (고양이 자세).
4. Continue flowing between Cat and Cow with each breath. / 호흡에 맞춰 고양이와 소 자세를 이어서 반복합니다.

**주의사항**
- Keep arms straight throughout the movement. / 동작 내내 팔은 곧게 편 상태를 유지하세요.
- Initiate the movement from your pelvis. / 움직임은 골반에서부터 시작하세요.
- Move slowly and synchronize with your breath. / 천천히 움직이며 호흡과 동작을 일치시키세요.

## 1-2. Sphinx pose · 스핑크스 자세 — 2분

`sphinx-pose` 〔API: back〕
중점: 요추~흉추 하부 신전 (가장 얕은 후굴) | 근육: 척추기립근, 다열근 / 신장: 복직근

**순서**
1. Lie face down and place your forearms on the floor with elbows under shoulders. / 엎드린 자세에서 팔꿈치가 어깨 아래에 오도록 전완을 바닥에 놓습니다.
2. Press your forearms into the floor and lift your chest. / 전완으로 바닥을 밀어내며 가슴을 들어 올립니다.
3. Keep your legs extended and the tops of your feet on the floor. / 다리는 곧게 뻗고 발등은 바닥에 붙인 상태를 유지합니다.
4. Hold and breathe, gently lifting through the chest. / 가슴을 부드럽게 들어 올린 채로 호흡하며 유지합니다.

**주의사항**
- Keep your shoulders away from your ears. / 어깨가 귀 쪽으로 올라가지 않게 내려주세요.
- Engage your lower back muscles gently. / 허리 근육을 부드럽게 활성화하세요.
- This is a gentle backbend - do not force. / 부드러운 후굴 동작이므로 무리해서 밀어붙이지 마세요.

## 1-3. Cow face pose (좌/우) · 소 얼굴 자세 — 2분

`cow-face-pose-left` / `cow-face-pose-right` 〔API: shoulders〕
중점: 견관절 외회전+굴곡 / 내회전+신전 | 근육 신장: 삼각근 후면, 극하근·소원근(위팔), 광배근·대원근·상완삼두근(아래팔)

**순서**
1. Sit on the floor and stack your knees on top of each other. / 바닥에 앉아 두 무릎을 위아래로 포갭니다.
2. Reach one arm overhead and bend it behind your back. / 한 팔을 머리 위로 뻗어 등 뒤로 구부립니다.
3. Reach the other arm behind your back from below. / 다른 팔을 아래에서 등 뒤로 뻗습니다.
4. Clasp your fingers or use a strap between your hands. / 손가락을 맞잡거나 두 손 사이에 스트랩을 사용합니다.

**주의사항**
- Sit evenly on both sit bones. Keep your spine straight and avoid leaning forward. / 양쪽 좌골에 체중을 고르게 두고 앉으세요. 척추를 곧게 세우고 앞으로 기울지 마세요.
- Don't force your hands to touch if flexibility doesn't allow it - use a towel or strap. / 유연성이 부족하면 손을 억지로 닿게 하지 말고 수건이나 스트랩을 사용하세요.
- Focus on lifting through the crown of your head to maintain good posture. / 정수리를 위로 들어 올리는 느낌으로 바른 자세를 유지하세요.
- Switch sides to ensure balanced flexibility development. / 양쪽을 모두 진행해 균형 있게 유연성을 기르세요.

## 1-4. Tiger pose (좌/우) · 호랑이 자세 — 2분

`tiger-pose-left` / `tiger-pose-right` 〔API: core〕
중점: 고관절 신전 + 단측 지지 항회전 안정 | 근육: 대둔근, 척추기립근, 복횡근, 다열근 / 신장: 대퇴직근, 장요근

**순서**
1. Start in tabletop position with hands directly under shoulders and knees under hips. / 손은 어깨 바로 아래, 무릎은 골반 아래에 두고 테이블탑 자세로 시작합니다.
2. Lift your right leg behind you and bend the knee, bringing your foot toward your glutes. / 오른쪽 다리를 뒤로 들어 무릎을 구부리고 발을 엉덩이 쪽으로 가져옵니다.
3. Reach back with your right hand to grasp the top of your right foot. / 오른손을 뒤로 뻗어 오른발등을 잡습니다.
4. Pull your foot gently toward your body while lifting your chest slightly. / 가슴을 살짝 들어 올리면서 발을 몸쪽으로 부드럽게 당깁니다.
5. Hold the position while maintaining balance on your left hand and left knee. / 왼손과 왼무릎으로 균형을 잡으며 자세를 유지합니다.
6. Release the foot and return to tabletop position slowly. / 발을 놓고 천천히 테이블탑 자세로 돌아옵니다.

**주의사항**
- Keep your supporting arm strong and straight to maintain balance. / 지지하는 팔은 곧고 단단하게 유지해 균형을 잡으세요.
- Engage your core muscles throughout the movement to protect your lower back. / 동작 내내 코어에 힘을 주어 허리를 보호하세요.
- Pull your foot gently toward your body without forcing the position. / 발을 몸쪽으로 당길 때 억지로 밀어붙이지 마세요.
- Keep your hips square to the ground rather than rotating them open. / 골반이 열리지 않도록 바닥과 나란히 유지하세요.
- Breathe deeply and avoid holding your breath during the pose. / 깊게 호흡하고 숨을 참지 마세요.

## 1-5. Cobra pose · 코브라 자세 — 2분 30초

`cobra-pose` 〔API: back〕
중점: 흉추·요추 신전 | 근육: 척추기립근, 다열근, 중·하부 승모근 / 신장: 복직근

**순서**
1. Lie face down with hands under your shoulders. / 손을 어깨 아래에 두고 엎드립니다.
2. Press your pelvis and legs into the floor. / 골반과 다리로 바닥을 눌러줍니다.
3. Inhale and straighten your arms to lift your chest. / 숨을 마시며 팔을 펴 가슴을 들어 올립니다.
4. Keep a slight bend in the elbows. / 팔꿈치는 살짝 구부린 상태를 유지합니다.

**주의사항**
- Do not push up too high - keep it comfortable. / 너무 높이 밀어 올리지 말고 편안한 범위를 유지하세요.
- Roll your shoulders back and down. / 어깨를 뒤로, 아래로 말아 내리세요.
- Engage your back muscles, not just your arms. / 팔 힘만 쓰지 말고 등 근육을 함께 사용하세요.

## 1-6. Upward facing dog pose · 업독 〔핀포즈〕 — 2분 (30초 × 3)

`upward-facing-dog-pose` 〔API: back〕
중점: 흉추 신전 + 견갑 안정 + 손목 하중 | 근육: 척추기립근, 대둔근, 상완삼두근, 전거근 / 신장: 장요근, 복직근, 대흉근

**순서**
1. Lie face down with hands beside your lower ribs. / 손을 갈비뼈 아래쪽 옆에 두고 엎드립니다.
2. Press through your hands to lift your chest and thighs off the floor. / 손으로 바닥을 밀어 가슴과 허벅지를 바닥에서 들어 올립니다.
3. Straighten your arms and roll your shoulders back. / 팔을 곧게 펴고 어깨를 뒤로 말아줍니다.
4. Keep the tops of your feet pressing into the mat. / 발등으로 매트를 계속 눌러줍니다.

**주의사항**
- Only your hands and tops of feet touch the floor. / 바닥에 닿는 곳은 손과 발등뿐이어야 합니다.
- Engage your legs and firm your thighs. / 다리에 힘을 주고 허벅지를 단단하게 조이세요.
- Do not compress your lower back. / 허리가 눌리지 않도록 주의하세요.

> **루틴 주의사항 — 취소선 처리됨(원본).** ~~손목에 체중이 실리는 자세입니다. 손목 통증이 있으면
> Sphinx로 대체하세요. 허리 디스크 병력이 있으면 전문가와 상의 후 수행하세요.~~

---

# 2. 등 라인 · 레벨 2 — Camel pose (낙타자세) · 17분 30초

제한 요인: 흉추 신전 제한, 장요근 단축, 어깨 신전 부족, 둔근 약화

**루틴 순서**

| # | 동작 | 시간 |
| --- | --- | --- |
| 1 | Cat cow pose · 고양이-소 자세 | 1분 30초 |
| 2 | Puppy pose · 퍼피 자세 | 2분 30초 |
| 3 | Low lunge (좌/우) · 로우 런지 | 2분 30초 |
| 4 | Cobra pose · 코브라 자세 | 2분 |
| 5 | Bridge pose · 브릿지 자세 | 2분 |
| 6 | Cow face pose (좌/우) · 소 얼굴 자세 | 2분 |
| 7 | **Camel pose · 낙타자세** 〔핀포즈〕 | 2분 (40초 × 3) |

**휴식 배치**

| 구간 | 종류 | 시간 | 화면 문구 |
| --- | --- | --- | --- |
| 1 → 2 | 전환 | 10초 | 다음 자세 준비 — 퍼피 자세 |
| 2 → 3 | 전환 | 10초 | 다음 자세 준비 — 로우 런지 |
| 3 → 4 | 전환 | 10초 | 다음 자세 준비 — 코브라 자세 |
| 4 → 5 (코브라 후굴 후) | 휴식 〔B〕 | 30초 | 무릎을 꿇고 상체를 앞으로 숙여 쉬세요 |
| 5 → 6 (브릿지 후) | 휴식 〔A〕 | 30초 | 그대로 누워서 편하게 호흡하세요 |
| 6 → 7 (핀포즈 직전) | 휴식 〔C〕 | 30초 | 편하게 앉아서 쉬세요 |
| 핀포즈 세트 사이 ×2 | 휴식 〔B〕 | 핀포즈 2분 내 처리 | 무릎을 꿇고 상체를 앞으로 숙여 쉬세요 |
| 루틴 종료 | 마무리 〔B〕 | 60초 | 무릎을 꿇고 상체를 앞으로 숙여 쉬세요 |

휴식 총량 180초 (전환 30 + 휴식 90 + 마무리 60) — 기존과 동일, 17분 30초 유지.

## 2-1. Cat cow pose · 고양이-소 자세 — 1분 30초

`cat-cow-pose` 〔API: back〕
중점: 척추 분절 가동성 (웜업) | 근육: 척추기립근, 다열근, 복횡근

**순서**
1. Start on all fours with wrists under shoulders and knees under hips. / 손목은 어깨 아래, 무릎은 골반 아래에 두고 네발기기 자세로 시작합니다.
2. Inhale, drop your belly toward the mat, lift your chest and tailbone (Cow). / 숨을 마시며 배를 매트 쪽으로 내리고, 가슴과 꼬리뼈를 들어 올립니다 (소 자세).
3. Exhale, round your spine toward the ceiling, tuck your chin and tailbone (Cat). / 숨을 내쉬며 척추를 천장 쪽으로 둥글게 말고, 턱과 꼬리뼈를 안으로 말아 넣습니다 (고양이 자세).
4. Continue flowing between Cat and Cow with each breath. / 호흡에 맞춰 고양이와 소 자세를 이어서 반복합니다.

**주의사항**
- Keep arms straight throughout the movement. / 동작 내내 팔은 곧게 편 상태를 유지하세요.
- Initiate the movement from your pelvis. / 움직임은 골반에서부터 시작하세요.
- Move slowly and synchronize with your breath. / 천천히 움직이며 호흡과 동작을 일치시키세요.

## 2-2. Puppy pose · 퍼피 자세 — 2분 30초

`puppy-pose` 〔API: back〕
중점: 흉추 신전 + 견관절 굴곡 가동범위 | 근육 신장: 광배근, 대원근, 대흉근

**순서**
1. Start on all fours with hips stacked over knees. / 골반이 무릎 바로 위에 오도록 네발기기 자세로 시작합니다.
2. Walk your hands forward, lowering your chest toward the floor. / 손을 앞으로 걸어 나가며 가슴을 바닥 쪽으로 낮춥니다.
3. Keep your hips over your knees. / 골반은 계속 무릎 위에 유지합니다.
4. Rest your forehead on the mat and melt your chest down. / 이마를 매트에 대고 가슴을 아래로 녹이듯 내립니다.

**주의사항**
- Keep your arms active and shoulder-width apart. / 팔은 어깨너비로 벌리고 힘을 유지하세요.
- Do not let your elbows touch the floor. / 팔꿈치가 바닥에 닿지 않게 하세요.
- Breathe into the stretch across your chest and shoulders. / 가슴과 어깨에 걸친 스트레칭 부위로 호흡을 보내세요.

## 2-3. Low lunge (좌/우) · 로우 런지 — 2분 30초

`low-lunge-left` / `low-lunge-right` 〔API: quads〕
중점: 고관절 굴곡근 신장 | 근육 신장: 장요근, 대퇴직근(뒷다리) / 주동: 대둔근(뒷다리), 대퇴사두근(앞다리)

**순서**
1. From Downward Dog, step one foot forward between your hands. / 다운독 자세에서 한쪽 발을 두 손 사이로 내딛습니다.
2. Lower your back knee to the mat. / 뒤쪽 무릎을 매트에 내립니다.
3. Stack your front knee over the ankle. / 앞쪽 무릎이 발목 바로 위에 오도록 둡니다.
4. Raise your arms overhead or keep hands on the floor. / 팔을 머리 위로 들어 올리거나 손을 바닥에 둡니다.

**주의사항**
- Pad your back knee with a blanket if needed. / 필요하면 뒤쪽 무릎 아래에 담요를 받치세요.
- Sink your hips forward and down for a deeper stretch. / 골반을 앞쪽 아래로 가라앉히면 스트레칭이 깊어집니다.
- Keep your core engaged and spine tall. / 코어에 힘을 주고 척추를 곧게 세우세요.

## 2-4. Cobra pose · 코브라 자세 — 2분

`cobra-pose` 〔API: back〕
중점: 흉추·요추 신전 | 근육: 척추기립근, 다열근, 중·하부 승모근

**순서**
1. Lie face down with hands under your shoulders. / 손을 어깨 아래에 두고 엎드립니다.
2. Press your pelvis and legs into the floor. / 골반과 다리로 바닥을 눌러줍니다.
3. Inhale and straighten your arms to lift your chest. / 숨을 마시며 팔을 펴 가슴을 들어 올립니다.
4. Keep a slight bend in the elbows. / 팔꿈치는 살짝 구부린 상태를 유지합니다.

**주의사항**
- Do not push up too high - keep it comfortable. / 너무 높이 밀어 올리지 말고 편안한 범위를 유지하세요.
- Roll your shoulders back and down. / 어깨를 뒤로, 아래로 말아 내리세요.
- Engage your back muscles, not just your arms. / 팔 힘만 쓰지 말고 등 근육을 함께 사용하세요.

## 2-5. Bridge pose · 브릿지 자세 — 2분

`bridge-pose` 〔API: glutes〕
중점: 고관절 신전 | 근육: 대둔근, 햄스트링, 척추기립근 / 신장: 장요근, 대퇴직근

**순서**
1. Lie on your back with knees bent and feet flat on the floor, hip-width apart. / 무릎을 세우고 발을 골반 너비로 벌려 바닥에 붙인 채 눕습니다.
2. Place arms alongside your body with palms facing down. / 손바닥이 아래를 향하도록 팔을 몸 옆에 놓습니다.
3. Press your feet into the floor and lift your hips toward the ceiling. / 발로 바닥을 밀며 골반을 천장 쪽으로 들어 올립니다.
4. Hold at the top, then slowly lower back down. / 가장 높은 지점에서 유지한 뒤 천천히 내려옵니다.

**주의사항**
- Keep knees aligned over ankles. / 무릎이 발목 위에 오도록 정렬하세요.
- Engage your glutes and core to support the lift. / 둔근과 코어에 힘을 주어 들어 올리는 동작을 받쳐주세요.
- Avoid turning your head while in the pose. / 자세를 유지하는 동안 고개를 돌리지 마세요.

## 2-6. Cow face pose (좌/우) · 소 얼굴 자세 — 2분

`cow-face-pose-left` / `cow-face-pose-right` 〔API: shoulders〕
중점: 견관절 복합 가동범위 | 근육 신장: 삼각근 후면, 극하근·소원근, 광배근·대원근

**순서**
1. Sit on the floor and stack your knees on top of each other. / 바닥에 앉아 두 무릎을 위아래로 포갭니다.
2. Reach one arm overhead and bend it behind your back. / 한 팔을 머리 위로 뻗어 등 뒤로 구부립니다.
3. Reach the other arm behind your back from below. / 다른 팔을 아래에서 등 뒤로 뻗습니다.
4. Clasp your fingers or use a strap between your hands. / 손가락을 맞잡거나 두 손 사이에 스트랩을 사용합니다.

**주의사항**
- Sit evenly on both sit bones. Keep your spine straight and avoid leaning forward. / 양쪽 좌골에 체중을 고르게 두고 앉으세요. 척추를 곧게 세우고 앞으로 기울지 마세요.
- Don't force your hands to touch if flexibility doesn't allow it - use a towel or strap. / 유연성이 부족하면 손을 억지로 닿게 하지 말고 수건이나 스트랩을 사용하세요.
- Focus on lifting through the crown of your head to maintain good posture. / 정수리를 위로 들어 올리는 느낌으로 바른 자세를 유지하세요.
- Switch sides to ensure balanced flexibility development. / 양쪽을 모두 진행해 균형 있게 유연성을 기르세요.

## 2-7. Camel pose · 낙타자세 〔핀포즈〕 — 2분 (40초 × 3)

`camel-pose` 〔API: back〕
중점: 흉추 신전 + 고관절 신전 | 근육: 척추기립근, 대둔근, 중·하부 승모근 / 신장: 장요근, 대퇴직근, 복직근, 대흉근

**순서**
1. Kneel on the mat with knees hip-width apart and thighs perpendicular to the floor. / 무릎을 골반 너비로 벌리고 허벅지가 바닥과 수직이 되도록 무릎으로 섭니다.
2. Place your hands on your lower back with fingers pointing down. / 손가락이 아래를 향하도록 두 손을 허리에 얹습니다.
3. Inhale, lift your chest, and slowly lean back. / 숨을 마시며 가슴을 들어 올리고 천천히 뒤로 젖힙니다.
4. If comfortable, reach your hands back to your heels. / 편안하다면 손을 뒤로 뻗어 발뒤꿈치를 잡습니다.

**주의사항**
- Press your hips forward to protect your lower back. / 골반을 앞으로 밀어 허리를 보호하세요.
- Keep your neck neutral or gently extend it back. / 목은 중립을 유지하거나 부드럽게만 뒤로 젖히세요.
- Engage your core throughout the backbend. / 후굴하는 내내 코어에 힘을 유지하세요.

**루틴 주의사항** — 목을 뒤로 완전히 젖히지 마세요. 허리 아래쪽에 날카로운 통증이 오면 즉시 중단하세요.

---

# 3. 등 라인 · 레벨 3 — Wheel pose (휠) · 20분

제한 요인: 어깨 굴곡 가동범위, 흉추 신전, 장요근 단축, 손목 지지력, 내전근 약화

**루틴 순서**

| # | 동작 | 시간 |
| --- | --- | --- |
| 1 | Cat cow pose · 고양이-소 자세 | 1분 30초 |
| 2 | Puppy pose · 퍼피 자세 | 2분 30초 |
| 3 | Low lunge (좌/우) · 로우 런지 | 2분 30초 |
| 4 | Cow face pose (좌/우) · 소 얼굴 자세 | 2분 |
| 5 | Bridge pose · 브릿지 자세 | 2분 |
| 6 | Camel pose · 낙타자세 | 2분 |
| 7 | Bow pose · 활 자세 | 1분 30초 |
| 8 | **Wheel pose · 휠** 〔핀포즈〕 | 2분 30초 (30초 × 4) |

**휴식 배치**

| 구간 | 종류 | 시간 | 화면 문구 |
| --- | --- | --- | --- |
| 1 → 2 | 전환 | 10초 | 다음 자세 준비 — 퍼피 자세 |
| 2 → 3 | 전환 | 10초 | 다음 자세 준비 — 로우 런지 |
| 3 → 4 | 전환 | 10초 | 다음 자세 준비 — 소 얼굴 자세 |
| 4 → 5 | 전환 | 10초 | 다음 자세 준비 — 브릿지 자세 |
| 5 → 6 (브릿지 후) | 휴식 〔A〕 | 30초 | 그대로 누워서 편하게 호흡하세요 |
| 6 → 7 (낙타 후굴 후) | 휴식 〔B〕 | 30초 | 무릎을 꿇고 상체를 앞으로 숙여 쉬세요 |
| 7 → 8 (활 후굴 + 핀포즈 직전) | 휴식 〔B〕 | 50초 | 무릎을 꿇고 상체를 앞으로 숙여 쉬세요 |
| 핀포즈 세트 사이 ×3 | 휴식 〔B〕 | 핀포즈 2분 30초 내 처리 | 무릎을 꿇고 상체를 앞으로 숙여 쉬세요 |
| 루틴 종료 | 마무리 〔B〕 | 60초 | 무릎을 꿇고 상체를 앞으로 숙여 쉬세요 |

휴식 총량 210초 (전환 40 + 휴식 110 + 마무리 60) — 기존과 동일, 20분 유지. 휠 직전 휴식만 50초로 늘렸다(부하가 가장 큰 자세).

## 3-1. Cat cow pose · 고양이-소 자세 — 1분 30초

`cat-cow-pose` 〔API: back〕
중점: 척추 분절 가동성 (웜업) | 근육: 척추기립근, 다열근, 복횡근

**순서**
1. Start on all fours with wrists under shoulders and knees under hips. / 손목은 어깨 아래, 무릎은 골반 아래에 두고 네발기기 자세로 시작합니다.
2. Inhale, drop your belly toward the mat, lift your chest and tailbone (Cow). / 숨을 마시며 배를 매트 쪽으로 내리고, 가슴과 꼬리뼈를 들어 올립니다 (소 자세).
3. Exhale, round your spine toward the ceiling, tuck your chin and tailbone (Cat). / 숨을 내쉬며 척추를 천장 쪽으로 둥글게 말고, 턱과 꼬리뼈를 안으로 말아 넣습니다 (고양이 자세).
4. Continue flowing between Cat and Cow with each breath. / 호흡에 맞춰 고양이와 소 자세를 이어서 반복합니다.

**주의사항**
- Keep arms straight throughout the movement. / 동작 내내 팔은 곧게 편 상태를 유지하세요.
- Initiate the movement from your pelvis. / 움직임은 골반에서부터 시작하세요.
- Move slowly and synchronize with your breath. / 천천히 움직이며 호흡과 동작을 일치시키세요.

## 3-2. Puppy pose · 퍼피 자세 — 2분 30초

`puppy-pose` 〔API: back〕
중점: 흉추 신전 + 견관절 굴곡 | 근육 신장: 광배근, 대원근, 대흉근

**순서**
1. Start on all fours with hips stacked over knees. / 골반이 무릎 바로 위에 오도록 네발기기 자세로 시작합니다.
2. Walk your hands forward, lowering your chest toward the floor. / 손을 앞으로 걸어 나가며 가슴을 바닥 쪽으로 낮춥니다.
3. Keep your hips over your knees. / 골반은 계속 무릎 위에 유지합니다.
4. Rest your forehead on the mat and melt your chest down. / 이마를 매트에 대고 가슴을 아래로 녹이듯 내립니다.

**주의사항**
- Keep your arms active and shoulder-width apart. / 팔은 어깨너비로 벌리고 힘을 유지하세요.
- Do not let your elbows touch the floor. / 팔꿈치가 바닥에 닿지 않게 하세요.
- Breathe into the stretch across your chest and shoulders. / 가슴과 어깨에 걸친 스트레칭 부위로 호흡을 보내세요.

## 3-3. Low lunge (좌/우) · 로우 런지 — 2분 30초

`low-lunge-left` / `low-lunge-right` 〔API: quads〕
중점: 고관절 굴곡근 신장 | 근육 신장: 장요근, 대퇴직근 / 주동: 대둔근

**순서**
1. From Downward Dog, step one foot forward between your hands. / 다운독 자세에서 한쪽 발을 두 손 사이로 내딛습니다.
2. Lower your back knee to the mat. / 뒤쪽 무릎을 매트에 내립니다.
3. Stack your front knee over the ankle. / 앞쪽 무릎이 발목 바로 위에 오도록 둡니다.
4. Raise your arms overhead or keep hands on the floor. / 팔을 머리 위로 들어 올리거나 손을 바닥에 둡니다.

**주의사항**
- Pad your back knee with a blanket if needed. / 필요하면 뒤쪽 무릎 아래에 담요를 받치세요.
- Sink your hips forward and down for a deeper stretch. / 골반을 앞쪽 아래로 가라앉히면 스트레칭이 깊어집니다.
- Keep your core engaged and spine tall. / 코어에 힘을 주고 척추를 곧게 세우세요.

## 3-4. Cow face pose (좌/우) · 소 얼굴 자세 — 2분

`cow-face-pose-left` / `cow-face-pose-right` 〔API: shoulders〕
중점: 견관절 외회전·내회전 | 근육 신장: 삼각근 후면, 극하근·소원근, 광배근·대원근

**순서**
1. Sit on the floor and stack your knees on top of each other. / 바닥에 앉아 두 무릎을 위아래로 포갭니다.
2. Reach one arm overhead and bend it behind your back. / 한 팔을 머리 위로 뻗어 등 뒤로 구부립니다.
3. Reach the other arm behind your back from below. / 다른 팔을 아래에서 등 뒤로 뻗습니다.
4. Clasp your fingers or use a strap between your hands. / 손가락을 맞잡거나 두 손 사이에 스트랩을 사용합니다.

**주의사항**
- Sit evenly on both sit bones. Keep your spine straight and avoid leaning forward. / 양쪽 좌골에 체중을 고르게 두고 앉으세요. 척추를 곧게 세우고 앞으로 기울지 마세요.
- Don't force your hands to touch if flexibility doesn't allow it - use a towel or strap. / 유연성이 부족하면 손을 억지로 닿게 하지 말고 수건이나 스트랩을 사용하세요.
- Focus on lifting through the crown of your head to maintain good posture. / 정수리를 위로 들어 올리는 느낌으로 바른 자세를 유지하세요.
- Switch sides to ensure balanced flexibility development. / 양쪽을 모두 진행해 균형 있게 유연성을 기르세요.

## 3-5. Bridge pose · 브릿지 자세 — 2분

`bridge-pose` 〔API: glutes〕
중점: 고관절 신전 (휠 전 척추 예열) | 근육: 대둔근, 햄스트링, 척추기립근

**순서**
1. Lie on your back with knees bent and feet flat on the floor, hip-width apart. / 무릎을 세우고 발을 골반 너비로 벌려 바닥에 붙인 채 눕습니다.
2. Place arms alongside your body with palms facing down. / 손바닥이 아래를 향하도록 팔을 몸 옆에 놓습니다.
3. Press your feet into the floor and lift your hips toward the ceiling. / 발로 바닥을 밀며 골반을 천장 쪽으로 들어 올립니다.
4. Hold at the top, then slowly lower back down. / 가장 높은 지점에서 유지한 뒤 천천히 내려옵니다.

**주의사항**
- Keep knees aligned over ankles. / 무릎이 발목 위에 오도록 정렬하세요.
- Engage your glutes and core to support the lift. / 둔근과 코어에 힘을 주어 들어 올리는 동작을 받쳐주세요.
- Avoid turning your head while in the pose. / 자세를 유지하는 동안 고개를 돌리지 마세요.

## 3-6. Camel pose · 낙타자세 — 2분

`camel-pose` 〔API: back〕
중점: 흉추 신전 | 근육: 척추기립근, 대둔근, 중·하부 승모근 / 신장: 장요근, 대퇴직근

**순서**
1. Kneel on the mat with knees hip-width apart and thighs perpendicular to the floor. / 무릎을 골반 너비로 벌리고 허벅지가 바닥과 수직이 되도록 무릎으로 섭니다.
2. Place your hands on your lower back with fingers pointing down. / 손가락이 아래를 향하도록 두 손을 허리에 얹습니다.
3. Inhale, lift your chest, and slowly lean back. / 숨을 마시며 가슴을 들어 올리고 천천히 뒤로 젖힙니다.
4. If comfortable, reach your hands back to your heels. / 편안하다면 손을 뒤로 뻗어 발뒤꿈치를 잡습니다.

**주의사항**
- Press your hips forward to protect your lower back. / 골반을 앞으로 밀어 허리를 보호하세요.
- Keep your neck neutral or gently extend it back. / 목은 중립을 유지하거나 부드럽게만 뒤로 젖히세요.
- Engage your core throughout the backbend. / 후굴하는 내내 코어에 힘을 유지하세요.

## 3-7. Bow pose · 활 자세 — 1분 30초

`bow-pose` 〔API: back〕
중점: 전신 후굴 + 어깨 신전 | 근육: 척추기립근, 대둔근, 햄스트링 / 신장: 대퇴사두근, 장요근, 대흉근

**순서**
1. Lie face down on your mat with arms alongside your body. / 팔을 몸 옆에 둔 채 매트에 엎드립니다.
2. Bend your knees and reach back to grab your ankles. / 무릎을 구부리고 뒤로 손을 뻗어 발목을 잡습니다.
3. Inhale and lift your chest and thighs off the floor simultaneously. / 숨을 마시며 가슴과 허벅지를 동시에 바닥에서 들어 올립니다.
4. Hold the pose, breathing steadily, then release on an exhale. / 고르게 호흡하며 자세를 유지한 뒤, 숨을 내쉬며 풀어줍니다.

**주의사항**
- Keep knees hip-width apart throughout. / 무릎은 처음부터 끝까지 골반 너비로 유지하세요.
- Press your ankles into your hands to lift higher. / 발목으로 손을 밀어내면 더 높이 들어 올릴 수 있습니다.
- Avoid compressing the lower back by engaging your core. / 코어에 힘을 주어 허리가 눌리지 않게 하세요.

## 3-8. Wheel pose · 휠 〔핀포즈〕 — 2분 30초 (30초 × 4)

`wheel-pose` 〔API: back〕
중점: 전신 후굴 + 어깨 굴곡 가동범위 | 근육: 척추기립근, 대둔근, 햄스트링, 삼각근, 상완삼두근 / 신장: 장요근, 대흉근, 복직근

**순서**
1. Lie on your back with knees bent and feet flat on the floor. / 무릎을 세우고 발바닥을 바닥에 붙인 채 등을 대고 눕습니다.
2. Place your hands beside your ears with fingers pointing toward your shoulders. / 손가락이 어깨를 향하도록 두 손을 귀 옆 바닥에 놓습니다.
3. Press into your hands and feet to lift your body off the floor. / 손과 발로 바닥을 밀어 몸을 들어 올립니다.
4. Straighten your arms and legs as much as possible. / 팔과 다리를 가능한 만큼 곧게 폅니다.

**주의사항**
- Warm up your spine with Bridge Pose first. / 먼저 브릿지 자세로 척추를 충분히 풀어주세요.
- Keep your feet parallel and hip-width apart. / 발은 골반 너비로 나란히 두세요.
- Press evenly through all four limbs. / 네 개의 팔다리에 고르게 힘을 분배해 밀어내세요.

**루틴 주의사항** — 머리를 바닥에 댄 채 버티지 마세요. 팔을 못 펴면 아직 단계가 아니며 브릿지로 돌아가는 것이 안전합니다. 호흡이 막히는 것은 과부하 신호입니다.

---

# 4. 복부 라인 · 레벨 1 — Half boat pose (반 보트) · 15분

제한 요인: 복직근·장요근 약화, 요추 굴곡(허리 말림), 햄스트링 단축

**루틴 순서**

| # | 동작 | 시간 |
| --- | --- | --- |
| 1 | Cat cow pose · 고양이-소 자세 | 2분 |
| 2 | Reclined windshield wipers · 누워서 와이퍼 | 2분 |
| 3 | Tiger pose (좌/우) · 호랑이 자세 | 2분 30초 |
| 4 | Plank pose · 플랭크 자세 | 2분 |
| 5 | Seated forward bend · 앉은 전굴 자세 | 2분 |
| 6 | **Half boat pose · 반 보트** 〔핀포즈〕 | 2분 (30초 × 3) |

**휴식 배치**

| 구간 | 종류 | 시간 | 화면 문구 |
| --- | --- | --- | --- |
| 1 → 2 | 전환 | 10초 | 다음 자세 준비 — 누워서 와이퍼 |
| 2 → 3 | 전환 | 10초 | 다음 자세 준비 — 호랑이 자세 |
| 3 → 4 (호랑이 후) | 휴식 〔B〕 | 30초 | 무릎을 꿇고 상체를 앞으로 숙여 쉬세요 |
| 4 → 5 (플랭크 후) | 휴식 〔B〕 | 30초 | 무릎을 꿇고 상체를 앞으로 숙여 쉬세요 |
| 5 → 6 (핀포즈 직전) | 휴식 〔C〕 | 30초 | 편하게 앉아서 쉬세요 |
| 핀포즈 세트 사이 ×2 | 휴식 〔C〕 | 핀포즈 2분 내 처리 | 편하게 앉아서 쉬세요 |
| 루틴 종료 | 마무리 〔C〕 | 40초 | 편하게 앉아서 쉬세요 |

휴식 총량 150초 (전환 20 + 휴식 90 + 마무리 40) — 기존과 동일, 15분 유지. 근력 자세 비중이 높아 마무리가 40초로 짧다.

## 4-1. Cat cow pose · 고양이-소 자세 — 2분

`cat-cow-pose` 〔API: back〕
중점: 척추 분절 가동성 (웜업) | 근육: 척추기립근, 다열근, 복횡근

**순서**
1. Start on all fours with wrists under shoulders and knees under hips. / 손목은 어깨 아래, 무릎은 골반 아래에 두고 네발기기 자세로 시작합니다.
2. Inhale, drop your belly toward the mat, lift your chest and tailbone (Cow). / 숨을 마시며 배를 매트 쪽으로 내리고, 가슴과 꼬리뼈를 들어 올립니다 (소 자세).
3. Exhale, round your spine toward the ceiling, tuck your chin and tailbone (Cat). / 숨을 내쉬며 척추를 천장 쪽으로 둥글게 말고, 턱과 꼬리뼈를 안으로 말아 넣습니다 (고양이 자세).
4. Continue flowing between Cat and Cow with each breath. / 호흡에 맞춰 고양이와 소 자세를 이어서 반복합니다.

**주의사항**
- Keep arms straight throughout the movement. / 동작 내내 팔은 곧게 편 상태를 유지하세요.
- Initiate the movement from your pelvis. / 움직임은 골반에서부터 시작하세요.
- Move slowly and synchronize with your breath. / 천천히 움직이며 호흡과 동작을 일치시키세요.

## 4-2. Reclined windshield wipers · 누워서 와이퍼 — 2분

`reclined-windshield-wipers` 〔API: core〕
중점: 요추 회전 가동성 + 고관절 내외회전 (웜업) | 근육: 내·외복사근(원심성), 요방형근, 다열근

**순서**
1. Lie on your back with knees bent and feet flat, wider than hip-width. / 무릎을 세우고 발을 골반보다 넓게 벌려 바닥에 붙인 채 눕습니다.
2. Extend your arms out to the sides. / 양팔을 옆으로 뻗습니다.
3. Let both knees drop to one side, then the other. / 두 무릎을 한쪽으로 넘겼다가 반대쪽으로 넘깁니다.
4. Move slowly with your breath. / 호흡에 맞춰 천천히 움직입니다.

**주의사항**
- Keep both shoulders on the floor. / 양쪽 어깨는 바닥에 붙인 상태를 유지하세요.
- Move gently and do not force the knees down. / 부드럽게 움직이고 무릎을 억지로 눌러 내리지 마세요.
- This is a gentle spinal twist and hip release. / 척추를 부드럽게 비틀고 고관절을 이완시키는 동작입니다.

## 4-3. Tiger pose (좌/우) · 호랑이 자세 — 2분 30초

`tiger-pose-left` / `tiger-pose-right` 〔API: core〕
중점: 단측 지지 항회전 안정 + 고관절 신전 | 근육: 복횡근, 다열근, 대둔근, 척추기립근

**순서**
1. Start in tabletop position with hands directly under shoulders and knees under hips. / 손은 어깨 바로 아래, 무릎은 골반 아래에 두고 테이블탑 자세로 시작합니다.
2. Lift your right leg behind you and bend the knee, bringing your foot toward your glutes. / 오른쪽 다리를 뒤로 들어 무릎을 구부리고 발을 엉덩이 쪽으로 가져옵니다.
3. Reach back with your right hand to grasp the top of your right foot. / 오른손을 뒤로 뻗어 오른발등을 잡습니다.
4. Pull your foot gently toward your body while lifting your chest slightly. / 가슴을 살짝 들어 올리면서 발을 몸쪽으로 부드럽게 당깁니다.
5. Hold the position while maintaining balance on your left hand and left knee. / 왼손과 왼무릎으로 균형을 잡으며 자세를 유지합니다.
6. Release the foot and return to tabletop position slowly. / 발을 놓고 천천히 테이블탑 자세로 돌아옵니다.

**주의사항**
- Keep your supporting arm strong and straight to maintain balance. / 지지하는 팔은 곧고 단단하게 유지해 균형을 잡으세요.
- Engage your core muscles throughout the movement to protect your lower back. / 동작 내내 코어에 힘을 주어 허리를 보호하세요.
- Pull your foot gently toward your body without forcing the position. / 발을 몸쪽으로 당길 때 억지로 밀어붙이지 마세요.
- Keep your hips square to the ground rather than rotating them open. / 골반이 열리지 않도록 바닥과 나란히 유지하세요.
- Breathe deeply and avoid holding your breath during the pose. / 깊게 호흡하고 숨을 참지 마세요.

## 4-4. Plank pose · 플랭크 자세 — 2분

`plank-pose` 〔API: core〕
중점: 코어 등척성 + 견갑 안정 | 근육: 복직근, 복횡근, 전거근, 삼각근 전면

**순서**
1. Start on all fours, then step your feet back to straighten your legs. / 네발기기 자세에서 발을 뒤로 보내 다리를 곧게 폅니다.
2. Align your body in a straight line from head to heels. / 머리부터 발뒤꿈치까지 몸을 일직선으로 정렬합니다.
3. Stack your shoulders directly over your wrists. / 어깨가 손목 바로 위에 오도록 놓습니다.
4. Hold, engaging your core and legs. / 코어와 다리에 힘을 준 채 유지합니다.

**주의사항**
- Do not let your hips sag or pike up. / 엉덩이가 아래로 처지거나 위로 솟지 않게 하세요.
- Press firmly through your hands and spread your fingers. / 손가락을 넓게 펴고 손으로 바닥을 단단히 밀어내세요.
- Keep your neck neutral, gazing at the floor. / 시선은 바닥을 향하고 목은 중립을 유지하세요.

## 4-5. Seated forward bend · 앉은 전굴 자세 — 2분

`seated-forward-bend` 〔API: hamstrings〕
중점: 후방 사슬 신장 (반 보트의 허리 말림 대비) | 근육 신장: 햄스트링, 비복근, 척추기립근

**순서**
1. Sit with legs extended straight in front of you. / 다리를 앞으로 곧게 뻗고 앉습니다.
2. Inhale and lengthen your spine. / 숨을 마시며 척추를 길게 늘입니다.
3. Exhale and fold forward from the hips, reaching for your feet. / 숨을 내쉬며 고관절에서부터 앞으로 접어 발을 향해 손을 뻗습니다.
4. Hold your feet, ankles, or shins. / 발, 발목 또는 정강이를 잡습니다.

**주의사항**
- Hinge from the hips, not the waist. / 허리가 아니라 고관절에서 접으세요.
- Keep your spine as long as possible. / 척추를 최대한 길게 유지하세요.
- Bend your knees slightly if your hamstrings are tight. / 햄스트링이 뻣뻣하면 무릎을 살짝 구부리세요.

## 4-6. Half boat pose · 반 보트 〔핀포즈〕 — 2분 (30초 × 3)

`half-boat-pose` 〔API: core〕
중점: 요추 중립을 유지한 고관절 굴곡 | 근육: 복직근, 장요근, 복횡근, 척추기립근

**순서**
1. Sit on the floor with knees bent and feet flat. / 무릎을 세우고 발바닥을 바닥에 붙인 채 앉습니다.
2. Lean back slightly and lift your feet off the floor. / 상체를 살짝 뒤로 기울이고 발을 바닥에서 들어 올립니다.
3. Keep your shins parallel to the floor (knees bent at 90 degrees). / 정강이를 바닥과 평행하게 유지합니다 (무릎 90도).
4. Extend your arms forward, parallel to the floor. / 팔을 바닥과 평행하게 앞으로 뻗습니다.

**주의사항**
- Keep your spine straight, avoid rounding your back. / 척추를 곧게 유지하고 등이 말리지 않게 하세요.
- Engage your core throughout. / 동작 내내 코어에 힘을 유지하세요.
- This is a modification of full Boat Pose. / 완전한 보트 자세의 변형(쉬운 버전) 동작입니다.

**루틴 주의사항** — 허리가 말린 채 버티면 요추 추간판에 부하가 걸립니다. 말리면 무릎을 더 굽혀 각도를 낮추세요. 목이 아니라 복부로 버텨야 합니다.

---

# 5. 복부 라인 · 레벨 2 — Boat pose (보트자세) · 17분 30초

제한 요인: 햄스트링 단축, 복직근·장요근 약화, 척추 신전근 약화

**루틴 순서**

| # | 동작 | 시간 |
| --- | --- | --- |
| 1 | Cat cow pose · 고양이-소 자세 | 1분 30초 |
| 2 | Reclined windshield wipers · 누워서 와이퍼 | 2분 |
| 3 | Plank pose · 플랭크 자세 | 2분 30초 |
| 4 | Tiger pose (좌/우) · 호랑이 자세 | 2분 |
| 5 | Seated forward bend · 앉은 전굴 자세 | 2분 30초 |
| 6 | Half boat pose · 반 보트 자세 | 2분 |
| 7 | **Boat pose · 보트자세** 〔핀포즈〕 | 2분 (35초 × 3) |

**휴식 배치**

| 구간 | 종류 | 시간 | 화면 문구 |
| --- | --- | --- | --- |
| 1 → 2 | 전환 | 10초 | 다음 자세 준비 — 누워서 와이퍼 |
| 2 → 3 | 전환 | 10초 | 다음 자세 준비 — 플랭크 자세 |
| 3 → 4 (플랭크 후) | 휴식 〔B〕 | 30초 | 무릎을 꿇고 상체를 앞으로 숙여 쉬세요 |
| 4 → 5 (호랑이 후) | 휴식 〔B〕 | 30초 | 무릎을 꿇고 상체를 앞으로 숙여 쉬세요 |
| 5 → 6 | 전환 | 10초 | 다음 자세 준비 — 반 보트 자세 |
| 6 → 7 (핀포즈 직전) | 휴식 〔C〕 | 30초 | 편하게 앉아서 쉬세요 |
| 핀포즈 세트 사이 ×2 | 휴식 〔C〕 | 핀포즈 2분 내 처리 | 편하게 앉아서 쉬세요 |
| 루틴 종료 | 마무리 〔C〕 | 60초 | 편하게 앉아서 쉬세요 |

휴식 총량 180초 (전환 30 + 휴식 90 + 마무리 60) — 기존과 동일, 17분 30초 유지.

## 5-1. Cat cow pose · 고양이-소 자세 — 1분 30초

`cat-cow-pose` 〔API: back〕
중점: 척추 분절 가동성 (웜업) | 근육: 척추기립근, 다열근, 복횡근

**순서**
1. Start on all fours with wrists under shoulders and knees under hips. / 손목은 어깨 아래, 무릎은 골반 아래에 두고 네발기기 자세로 시작합니다.
2. Inhale, drop your belly toward the mat, lift your chest and tailbone (Cow). / 숨을 마시며 배를 매트 쪽으로 내리고, 가슴과 꼬리뼈를 들어 올립니다 (소 자세).
3. Exhale, round your spine toward the ceiling, tuck your chin and tailbone (Cat). / 숨을 내쉬며 척추를 천장 쪽으로 둥글게 말고, 턱과 꼬리뼈를 안으로 말아 넣습니다 (고양이 자세).
4. Continue flowing between Cat and Cow with each breath. / 호흡에 맞춰 고양이와 소 자세를 이어서 반복합니다.

**주의사항**
- Keep arms straight throughout the movement. / 동작 내내 팔은 곧게 편 상태를 유지하세요.
- Initiate the movement from your pelvis. / 움직임은 골반에서부터 시작하세요.
- Move slowly and synchronize with your breath. / 천천히 움직이며 호흡과 동작을 일치시키세요.

## 5-2. Reclined windshield wipers · 누워서 와이퍼 — 2분

`reclined-windshield-wipers` 〔API: core〕
중점: 요추 회전 가동성 | 근육: 내·외복사근, 요방형근

**순서**
1. Lie on your back with knees bent and feet flat, wider than hip-width. / 무릎을 세우고 발을 골반보다 넓게 벌려 바닥에 붙인 채 눕습니다.
2. Extend your arms out to the sides. / 양팔을 옆으로 뻗습니다.
3. Let both knees drop to one side, then the other. / 두 무릎을 한쪽으로 넘겼다가 반대쪽으로 넘깁니다.
4. Move slowly with your breath. / 호흡에 맞춰 천천히 움직입니다.

**주의사항**
- Keep both shoulders on the floor. / 양쪽 어깨는 바닥에 붙인 상태를 유지하세요.
- Move gently and do not force the knees down. / 부드럽게 움직이고 무릎을 억지로 눌러 내리지 마세요.
- This is a gentle spinal twist and hip release. / 척추를 부드럽게 비틀고 고관절을 이완시키는 동작입니다.

## 5-3. Plank pose · 플랭크 자세 — 2분 30초

`plank-pose` 〔API: core〕
중점: 코어 등척성 | 근육: 복직근, 복횡근, 전거근

**순서**
1. Start on all fours, then step your feet back to straighten your legs. / 네발기기 자세에서 발을 뒤로 보내 다리를 곧게 폅니다.
2. Align your body in a straight line from head to heels. / 머리부터 발뒤꿈치까지 몸을 일직선으로 정렬합니다.
3. Stack your shoulders directly over your wrists. / 어깨가 손목 바로 위에 오도록 놓습니다.
4. Hold, engaging your core and legs. / 코어와 다리에 힘을 준 채 유지합니다.

**주의사항**
- Do not let your hips sag or pike up. / 엉덩이가 아래로 처지거나 위로 솟지 않게 하세요.
- Press firmly through your hands and spread your fingers. / 손가락을 넓게 펴고 손으로 바닥을 단단히 밀어내세요.
- Keep your neck neutral, gazing at the floor. / 시선은 바닥을 향하고 목은 중립을 유지하세요.

## 5-4. Tiger pose (좌/우) · 호랑이 자세 — 2분

`tiger-pose-left` / `tiger-pose-right` 〔API: core〕
중점: 단측 지지 항회전 안정 | 근육: 복횡근, 다열근, 대둔근

**순서**
1. Start in tabletop position with hands directly under shoulders and knees under hips. / 손은 어깨 바로 아래, 무릎은 골반 아래에 두고 테이블탑 자세로 시작합니다.
2. Lift your right leg behind you and bend the knee, bringing your foot toward your glutes. / 오른쪽 다리를 뒤로 들어 무릎을 구부리고 발을 엉덩이 쪽으로 가져옵니다.
3. Reach back with your right hand to grasp the top of your right foot. / 오른손을 뒤로 뻗어 오른발등을 잡습니다.
4. Pull your foot gently toward your body while lifting your chest slightly. / 가슴을 살짝 들어 올리면서 발을 몸쪽으로 부드럽게 당깁니다.
5. Hold the position while maintaining balance on your left hand and left knee. / 왼손과 왼무릎으로 균형을 잡으며 자세를 유지합니다.
6. Release the foot and return to tabletop position slowly. / 발을 놓고 천천히 테이블탑 자세로 돌아옵니다.

**주의사항**
- Keep your supporting arm strong and straight to maintain balance. / 지지하는 팔은 곧고 단단하게 유지해 균형을 잡으세요.
- Engage your core muscles throughout the movement to protect your lower back. / 동작 내내 코어에 힘을 주어 허리를 보호하세요.
- Pull your foot gently toward your body without forcing the position. / 발을 몸쪽으로 당길 때 억지로 밀어붙이지 마세요.
- Keep your hips square to the ground rather than rotating them open. / 골반이 열리지 않도록 바닥과 나란히 유지하세요.
- Breathe deeply and avoid holding your breath during the pose. / 깊게 호흡하고 숨을 참지 마세요.

## 5-5. Seated forward bend · 앉은 전굴 자세 — 2분 30초

`seated-forward-bend` 〔API: hamstrings〕
중점: 후방 사슬 신장 (보트에서 다리 펴기 대비) | 근육 신장: 햄스트링, 비복근, 척추기립근

**순서**
1. Sit with legs extended straight in front of you. / 다리를 앞으로 곧게 뻗고 앉습니다.
2. Inhale and lengthen your spine. / 숨을 마시며 척추를 길게 늘입니다.
3. Exhale and fold forward from the hips, reaching for your feet. / 숨을 내쉬며 고관절에서부터 앞으로 접어 발을 향해 손을 뻗습니다.
4. Hold your feet, ankles, or shins. / 발, 발목 또는 정강이를 잡습니다.

**주의사항**
- Hinge from the hips, not the waist. / 허리가 아니라 고관절에서 접으세요.
- Keep your spine as long as possible. / 척추를 최대한 길게 유지하세요.
- Bend your knees slightly if your hamstrings are tight. / 햄스트링이 뻣뻣하면 무릎을 살짝 구부리세요.

## 5-6. Half boat pose · 반 보트 자세 — 2분

`half-boat-pose` 〔API: core〕
중점: 코어 등척성 (보트 직전 단계) | 근육: 복직근, 장요근, 복횡근

**순서**
1. Sit on the floor with knees bent and feet flat. / 무릎을 세우고 발바닥을 바닥에 붙인 채 앉습니다.
2. Lean back slightly and lift your feet off the floor. / 상체를 살짝 뒤로 기울이고 발을 바닥에서 들어 올립니다.
3. Keep your shins parallel to the floor (knees bent at 90 degrees). / 정강이를 바닥과 평행하게 유지합니다 (무릎 90도).
4. Extend your arms forward, parallel to the floor. / 팔을 바닥과 평행하게 앞으로 뻗습니다.

**주의사항**
- Keep your spine straight, avoid rounding your back. / 척추를 곧게 유지하고 등이 말리지 않게 하세요.
- Engage your core throughout. / 동작 내내 코어에 힘을 유지하세요.
- This is a modification of full Boat Pose. / 완전한 보트 자세의 변형(쉬운 버전) 동작입니다.

## 5-7. Boat pose · 보트자세 〔핀포즈〕 — 2분 (35초 × 3)

`boat-pose` 〔API: core〕
중점: 코어 근력 + 햄스트링 유연성 동시 요구 | 근육: 복직근, 장요근, 척추기립근, 대퇴사두근 / 신장 요구: 햄스트링

**순서**
1. Sit on the floor with knees bent and feet flat. / 무릎을 세우고 발바닥을 바닥에 붙인 채 앉습니다.
2. Lean back slightly and lift your feet off the floor. / 상체를 살짝 뒤로 기울이고 발을 바닥에서 들어 올립니다.
3. Extend your arms forward, parallel to the floor. / 팔을 바닥과 평행하게 앞으로 뻗습니다.
4. Straighten your legs to form a V-shape with your body. / 다리를 곧게 펴 몸으로 V자를 만듭니다.

**주의사항**
- Keep your spine straight, avoid rounding the back. / 척추를 곧게 유지하고 등이 말리지 않게 하세요.
- Engage your core to maintain balance. / 균형을 잡기 위해 코어에 힘을 주세요.
- Breathe steadily and hold for 5-10 breaths. / 고르게 호흡하며 5~10회 호흡 동안 유지하세요.

**루틴 주의사항** — 허리 말림은 즉시 중단 신호입니다. 다리를 펴는 것보다 척추를 세우는 것이 우선입니다.

---

# 6. 복부 라인 · 레벨 3 — Side plank pose (사이드 플랭크) · 20분

제한 요인: 복사근·요방형근 약화, 어깨 안정성, 손목 지지력, 균형

**루틴 순서**

| # | 동작 | 시간 |
| --- | --- | --- |
| 1 | Cat cow pose · 고양이-소 자세 | 1분 30초 |
| 2 | Reclined windshield wipers · 누워서 와이퍼 | 2분 |
| 3 | Tiger pose (좌/우) · 호랑이 자세 | 2분 |
| 4 | Plank pose · 플랭크 자세 | 2분 30초 |
| 5 | Gate pose (좌/우) · 문 빗장 자세 | 2분 30초 |
| 6 | Half lord of the fishes (좌/우) · 반 물고기의 왕 자세 | 2분 |
| 7 | Boat pose · 보트자세 | 2분 |
| 8 | **Side plank pose (좌/우) · 사이드 플랭크** 〔핀포즈〕 | 2분 (좌우 각 30초 × 2) |

**휴식 배치**

| 구간 | 종류 | 시간 | 화면 문구 |
| --- | --- | --- | --- |
| 1 → 2 | 전환 | 10초 | 다음 자세 준비 — 누워서 와이퍼 |
| 2 → 3 | 전환 | 10초 | 다음 자세 준비 — 호랑이 자세 |
| 3 → 4 (호랑이 후) | 휴식 〔B〕 | 30초 | 무릎을 꿇고 상체를 앞으로 숙여 쉬세요 |
| 4 → 5 (플랭크 후) | 휴식 〔B〕 | 30초 | 무릎을 꿇고 상체를 앞으로 숙여 쉬세요 |
| 5 → 6 | 전환 | 10초 | 다음 자세 준비 — 반 물고기의 왕 자세 |
| 6 → 7 | 전환 | 10초 | 다음 자세 준비 — 보트자세 |
| 7 → 8 (보트 후 + 핀포즈 직전) | 휴식 〔C〕 | 50초 | 편하게 앉아서 쉬세요 |
| 핀포즈 좌우 전환 및 세트 사이 | 휴식 〔C〕 | 핀포즈 2분 내 처리 | 편하게 앉아서 쉬세요 |
| 루틴 종료 | 마무리 〔C〕 | 60초 | 편하게 앉아서 쉬세요 |

휴식 총량 210초 (전환 40 + 휴식 110 + 마무리 60) — 기존과 동일, 20분 유지.

## 6-1. Cat cow pose · 고양이-소 자세 — 1분 30초

`cat-cow-pose` 〔API: back〕
중점: 척추 분절 가동성 (웜업) | 근육: 척추기립근, 다열근, 복횡근

**순서**
1. Start on all fours with wrists under shoulders and knees under hips. / 손목은 어깨 아래, 무릎은 골반 아래에 두고 네발기기 자세로 시작합니다.
2. Inhale, drop your belly toward the mat, lift your chest and tailbone (Cow). / 숨을 마시며 배를 매트 쪽으로 내리고, 가슴과 꼬리뼈를 들어 올립니다 (소 자세).
3. Exhale, round your spine toward the ceiling, tuck your chin and tailbone (Cat). / 숨을 내쉬며 척추를 천장 쪽으로 둥글게 말고, 턱과 꼬리뼈를 안으로 말아 넣습니다 (고양이 자세).
4. Continue flowing between Cat and Cow with each breath. / 호흡에 맞춰 고양이와 소 자세를 이어서 반복합니다.

**주의사항**
- Keep arms straight throughout the movement. / 동작 내내 팔은 곧게 편 상태를 유지하세요.
- Initiate the movement from your pelvis. / 움직임은 골반에서부터 시작하세요.
- Move slowly and synchronize with your breath. / 천천히 움직이며 호흡과 동작을 일치시키세요.

## 6-2. Reclined windshield wipers · 누워서 와이퍼 — 2분

`reclined-windshield-wipers` 〔API: core〕
중점: 요추 회전 가동성 | 근육: 내·외복사근, 요방형근

**순서**
1. Lie on your back with knees bent and feet flat, wider than hip-width. / 무릎을 세우고 발을 골반보다 넓게 벌려 바닥에 붙인 채 눕습니다.
2. Extend your arms out to the sides. / 양팔을 옆으로 뻗습니다.
3. Let both knees drop to one side, then the other. / 두 무릎을 한쪽으로 넘겼다가 반대쪽으로 넘깁니다.
4. Move slowly with your breath. / 호흡에 맞춰 천천히 움직입니다.

**주의사항**
- Keep both shoulders on the floor. / 양쪽 어깨는 바닥에 붙인 상태를 유지하세요.
- Move gently and do not force the knees down. / 부드럽게 움직이고 무릎을 억지로 눌러 내리지 마세요.
- This is a gentle spinal twist and hip release. / 척추를 부드럽게 비틀고 고관절을 이완시키는 동작입니다.

## 6-3. Tiger pose (좌/우) · 호랑이 자세 — 2분

`tiger-pose-left` / `tiger-pose-right` 〔API: core〕
중점: 단측 지지 항회전 안정 | 근육: 복횡근, 다열근, 대둔근

**순서**
1. Start in tabletop position with hands directly under shoulders and knees under hips. / 손은 어깨 바로 아래, 무릎은 골반 아래에 두고 테이블탑 자세로 시작합니다.
2. Lift your right leg behind you and bend the knee, bringing your foot toward your glutes. / 오른쪽 다리를 뒤로 들어 무릎을 구부리고 발을 엉덩이 쪽으로 가져옵니다.
3. Reach back with your right hand to grasp the top of your right foot. / 오른손을 뒤로 뻗어 오른발등을 잡습니다.
4. Pull your foot gently toward your body while lifting your chest slightly. / 가슴을 살짝 들어 올리면서 발을 몸쪽으로 부드럽게 당깁니다.
5. Hold the position while maintaining balance on your left hand and left knee. / 왼손과 왼무릎으로 균형을 잡으며 자세를 유지합니다.
6. Release the foot and return to tabletop position slowly. / 발을 놓고 천천히 테이블탑 자세로 돌아옵니다.

**주의사항**
- Keep your supporting arm strong and straight to maintain balance. / 지지하는 팔은 곧고 단단하게 유지해 균형을 잡으세요.
- Engage your core muscles throughout the movement to protect your lower back. / 동작 내내 코어에 힘을 주어 허리를 보호하세요.
- Pull your foot gently toward your body without forcing the position. / 발을 몸쪽으로 당길 때 억지로 밀어붙이지 마세요.
- Keep your hips square to the ground rather than rotating them open. / 골반이 열리지 않도록 바닥과 나란히 유지하세요.
- Breathe deeply and avoid holding your breath during the pose. / 깊게 호흡하고 숨을 참지 마세요.

## 6-4. Plank pose · 플랭크 자세 — 2분 30초

`plank-pose` 〔API: core〕
중점: 코어 등척성 + 견갑 전인 (사이드 플랭크의 어깨 지지 대비) | 근육: 복직근, 복횡근, 전거근, 삼각근 전면

**순서**
1. Start on all fours, then step your feet back to straighten your legs. / 네발기기 자세에서 발을 뒤로 보내 다리를 곧게 폅니다.
2. Align your body in a straight line from head to heels. / 머리부터 발뒤꿈치까지 몸을 일직선으로 정렬합니다.
3. Stack your shoulders directly over your wrists. / 어깨가 손목 바로 위에 오도록 놓습니다.
4. Hold, engaging your core and legs. / 코어와 다리에 힘을 준 채 유지합니다.

**주의사항**
- Do not let your hips sag or pike up. / 엉덩이가 아래로 처지거나 위로 솟지 않게 하세요.
- Press firmly through your hands and spread your fingers. / 손가락을 넓게 펴고 손으로 바닥을 단단히 밀어내세요.
- Keep your neck neutral, gazing at the floor. / 시선은 바닥을 향하고 목은 중립을 유지하세요.

## 6-5. Gate pose (좌/우) · 문 빗장 자세 — 2분 30초

`gate-pose-left` / `gate-pose-right` 〔API: core〕
중점: 측면 사슬(체간 외측) | 근육 신장: 요방형근, 광배근, 내전근 / 주동: 반대측 복사근

**순서**
1. Kneel on the mat and extend one leg out to the side. / 무릎으로 선 자세에서 한쪽 다리를 옆으로 뻗습니다.
2. Place the extended foot flat on the floor, toes pointing sideways. / 뻗은 발의 발바닥을 바닥에 붙이고 발끝은 옆을 향하게 합니다.
3. Inhale and raise the arm on the kneeling side overhead. / 숨을 마시며 무릎을 꿇은 쪽 팔을 머리 위로 들어 올립니다.
4. Exhale and lean toward the extended leg, sliding the other hand down the leg. / 숨을 내쉬며 뻗은 다리 쪽으로 기울이고, 반대쪽 손을 다리를 따라 내립니다.

**주의사항**
- Keep your hips stacked over the kneeling knee. / 골반이 꿇은 무릎 바로 위에 오도록 유지하세요.
- Open your chest toward the ceiling. / 가슴을 천장 쪽으로 열어주세요.
- Do not collapse forward. / 상체가 앞으로 무너지지 않게 하세요.

## 6-6. Half lord of the fishes pose (좌/우) · 반 물고기의 왕 자세 — 2분

`half-lord-of-the-fishes-pose-left` / `half-lord-of-the-fishes-pose-right` 〔API: core〕
중점: 흉추 회전 | 근육: 내·외복사근, 다열근, 회선근 / 신장: 대둔근, 이상근, 광배근

**순서**
1. Sit with legs extended. Bend one knee and place the foot outside the opposite thigh. / 다리를 뻗고 앉습니다. 한쪽 무릎을 구부려 발을 반대쪽 허벅지 바깥에 놓습니다.
2. Bend the bottom leg and bring the foot near the opposite hip. / 아래쪽 다리를 구부려 발을 반대쪽 엉덩이 가까이 가져옵니다.
3. Twist your torso toward the bent knee. / 구부린 무릎 쪽으로 상체를 비틉니다.
4. Use the opposite elbow against the outside of the bent knee for leverage. / 반대쪽 팔꿈치를 구부린 무릎 바깥에 대어 지렛대로 사용합니다.

**주의사항**
- Sit tall before you twist - lengthen your spine first. / 비틀기 전에 먼저 척추를 길게 늘여 바르게 앉으세요.
- Twist from your mid-back, not just your shoulders. / 어깨만이 아니라 등 중간(흉추)에서부터 비트세요.
- Keep both sit bones grounded. / 양쪽 좌골이 바닥에 고르게 닿아 있게 하세요.

## 6-7. Boat pose · 보트자세 — 2분

`boat-pose` 〔API: core〕
중점: 코어 등척성 | 근육: 복직근, 장요근, 척추기립근

**순서**
1. Sit on the floor with knees bent and feet flat. / 무릎을 세우고 발바닥을 바닥에 붙인 채 앉습니다.
2. Lean back slightly and lift your feet off the floor. / 상체를 살짝 뒤로 기울이고 발을 바닥에서 들어 올립니다.
3. Extend your arms forward, parallel to the floor. / 팔을 바닥과 평행하게 앞으로 뻗습니다.
4. Straighten your legs to form a V-shape with your body. / 다리를 곧게 펴 몸으로 V자를 만듭니다.

**주의사항**
- Keep your spine straight, avoid rounding the back. / 척추를 곧게 유지하고 등이 말리지 않게 하세요.
- Engage your core to maintain balance. / 균형을 잡기 위해 코어에 힘을 주세요.
- Breathe steadily and hold for 5-10 breaths. / 고르게 호흡하며 5~10회 호흡 동안 유지하세요.

## 6-8. Side plank pose (좌/우) · 사이드 플랭크 〔핀포즈〕 — 2분 (좌우 각 30초 × 2)

`side-plank-pose-left` / `side-plank-pose-right` 〔API: core〕
중점: 측면 코어 + 견갑·어깨 안정 | 근육: 내·외복사근, 요방형근, 중둔근, 삼각근, 전거근

**순서**
1. From Plank Pose, shift your weight onto one hand and the side of the corresponding foot. / 플랭크 자세에서 한쪽 손과 같은 쪽 발 옆면으로 체중을 옮깁니다.
2. Stack your feet and open your top arm toward the ceiling. / 두 발을 포개고 위쪽 팔을 천장을 향해 엽니다.
3. Keep your body in a straight line from head to feet. / 머리부터 발까지 몸을 일직선으로 유지합니다.
4. Hold and breathe steadily. / 고르게 호흡하며 유지합니다.

**주의사항**
- Press firmly through your supporting hand. / 지지하는 손으로 바닥을 단단히 밀어내세요.
- Engage your core and obliques. / 코어와 복사근에 힘을 주세요.
- Modify by lowering your bottom knee to the floor. / 어렵다면 아래쪽 무릎을 바닥에 내려 강도를 낮추세요.

**루틴 주의사항** — 손목 통증이 있으면 팔꿈치 지지 변형으로 바꾸세요. 어깨가 손목보다 앞으로 나가면 어깨 관절에 전단력이 걸립니다.

---

# 7. 골반 라인 · 레벨 1 — Bridge pose (브릿지) · 15분

제한 요인: 대둔근 약화, 고관절 앞쪽 경직, 내전근 약화

**루틴 순서**

| # | 동작 | 시간 |
| --- | --- | --- |
| 1 | Cat cow pose · 고양이-소 자세 | 2분 |
| 2 | Reclined butterfly · 누운 나비 자세 | 2분 |
| 3 | Happy baby pose · 해피 베이비 자세 | 2분 |
| 4 | Tiger pose (좌/우) · 호랑이 자세 | 2분 30초 |
| 5 | Reclined pigeon pose (좌/우) · 누운 비둘기 자세 | 2분 |
| 6 | **Bridge pose · 브릿지** 〔핀포즈〕 | 2분 (40초 × 3) |

**휴식 배치**

| 구간 | 종류 | 시간 | 화면 문구 |
| --- | --- | --- | --- |
| 1 → 2 | 전환 | 10초 | 다음 자세 준비 — 누운 나비 자세 |
| 2 → 3 | 전환 | 10초 | 다음 자세 준비 — 해피 베이비 자세 |
| 3 → 4 | 전환 | 10초 | 다음 자세 준비 — 호랑이 자세 |
| 4 → 5 (호랑이 후) | 휴식 〔B〕 | 30초 | 무릎을 꿇고 상체를 앞으로 숙여 쉬세요 |
| 5 → 6 (핀포즈 직전) | 휴식 〔A〕 | 30초 | 그대로 누워서 편하게 호흡하세요 |
| 핀포즈 세트 사이 ×2 | 휴식 〔A〕 | 핀포즈 2분 내 처리 | 그대로 누워서 편하게 호흡하세요 |
| 루틴 종료 | 마무리 〔A〕 | 60초 | 그대로 누워서 편하게 호흡하세요 |

휴식 총량 150초 (전환 30 + 휴식 60 + 마무리 60) — 기존과 동일, 15분 유지. 2→3 구간이 기존에 30초 휴식이던 곳으로, 누운 나비에서 해피 베이비로 넘어가는 이완-이완 구간이라 전환 10초로 줄였다.

## 7-1. Cat cow pose · 고양이-소 자세 — 2분

`cat-cow-pose` 〔API: back〕
중점: 척추 분절 가동성 (웜업) | 근육: 척추기립근, 다열근, 복횡근

**순서**
1. Start on all fours with wrists under shoulders and knees under hips. / 손목은 어깨 아래, 무릎은 골반 아래에 두고 네발기기 자세로 시작합니다.
2. Inhale, drop your belly toward the mat, lift your chest and tailbone (Cow). / 숨을 마시며 배를 매트 쪽으로 내리고, 가슴과 꼬리뼈를 들어 올립니다 (소 자세).
3. Exhale, round your spine toward the ceiling, tuck your chin and tailbone (Cat). / 숨을 내쉬며 척추를 천장 쪽으로 둥글게 말고, 턱과 꼬리뼈를 안으로 말아 넣습니다 (고양이 자세).
4. Continue flowing between Cat and Cow with each breath. / 호흡에 맞춰 고양이와 소 자세를 이어서 반복합니다.

**주의사항**
- Keep arms straight throughout the movement. / 동작 내내 팔은 곧게 편 상태를 유지하세요.
- Initiate the movement from your pelvis. / 움직임은 골반에서부터 시작하세요.
- Move slowly and synchronize with your breath. / 천천히 움직이며 호흡과 동작을 일치시키세요.

## 7-2. Reclined butterfly · 누운 나비 자세 — 2분

`reclined-butterfly` 〔API: glutes〕 ※ 실제 타깃은 내전근
중점: 고관절 외회전·외전 수동 이완 | 근육 신장: 장·단내전근, 박근, 치골근

**순서**
1. Lie on your back and bring the soles of your feet together. / 등을 대고 누워 양 발바닥을 마주 붙입니다.
2. Let your knees fall open to the sides. / 무릎이 양옆으로 자연스럽게 벌어지도록 둡니다.
3. Rest your arms at your sides with palms up. / 손바닥이 위를 향하도록 팔을 몸 옆에 놓습니다.
4. Close your eyes and relax for several minutes. / 눈을 감고 몇 분간 이완합니다.

**주의사항**
- Place pillows or blocks under your knees for support. / 무릎 아래에 베개나 블록을 받쳐 지지해 주세요.
- Let gravity do the work - do not force the stretch. / 중력에 맡기고 억지로 스트레칭하지 마세요.
- This is a restorative pose - focus on deep breathing. / 회복(리스토러티브) 자세이므로 깊은 호흡에 집중하세요.

## 7-3. Happy baby pose · 해피 베이비 자세 — 2분

`happy-baby-pose` 〔API: glutes〕
중점: 고관절 굴곡 가동범위 + 요추 이완 | 근육 신장: 대둔근, 햄스트링, 내전근 (※ 고관절 굴곡근은 신장되지 않음)

**순서**
1. Lie on your back and bring your knees toward your chest. / 등을 대고 누워 무릎을 가슴 쪽으로 가져옵니다.
2. Grab the outsides of your feet with your hands. / 두 손으로 발 바깥쪽을 잡습니다.
3. Open your knees wider than your torso. / 무릎을 몸통보다 넓게 벌립니다.
4. Gently pull your feet down, keeping your lower back on the mat. / 허리를 매트에 붙인 채 발을 아래로 부드럽게 당깁니다.

**주의사항**
- Keep your lower back and sacrum on the floor. / 허리와 천골이 바닥에서 뜨지 않게 하세요.
- Rock gently side to side for a spinal massage. / 좌우로 부드럽게 흔들면 척추를 마사지하는 효과가 있습니다.
- Relax your shoulders and neck. / 어깨와 목의 힘을 빼세요.

## 7-4. Tiger pose (좌/우) · 호랑이 자세 — 2분 30초

`tiger-pose-left` / `tiger-pose-right` 〔API: core〕
중점: 고관절 신전 + 대둔근 활성 | 근육: 대둔근, 복횡근, 다열근, 척추기립근 / 신장: 대퇴직근

**순서**
1. Start in tabletop position with hands directly under shoulders and knees under hips. / 손은 어깨 바로 아래, 무릎은 골반 아래에 두고 테이블탑 자세로 시작합니다.
2. Lift your right leg behind you and bend the knee, bringing your foot toward your glutes. / 오른쪽 다리를 뒤로 들어 무릎을 구부리고 발을 엉덩이 쪽으로 가져옵니다.
3. Reach back with your right hand to grasp the top of your right foot. / 오른손을 뒤로 뻗어 오른발등을 잡습니다.
4. Pull your foot gently toward your body while lifting your chest slightly. / 가슴을 살짝 들어 올리면서 발을 몸쪽으로 부드럽게 당깁니다.
5. Hold the position while maintaining balance on your left hand and left knee. / 왼손과 왼무릎으로 균형을 잡으며 자세를 유지합니다.
6. Release the foot and return to tabletop position slowly. / 발을 놓고 천천히 테이블탑 자세로 돌아옵니다.

**주의사항**
- Keep your supporting arm strong and straight to maintain balance. / 지지하는 팔은 곧고 단단하게 유지해 균형을 잡으세요.
- Engage your core muscles throughout the movement to protect your lower back. / 동작 내내 코어에 힘을 주어 허리를 보호하세요.
- Pull your foot gently toward your body without forcing the position. / 발을 몸쪽으로 당길 때 억지로 밀어붙이지 마세요.
- Keep your hips square to the ground rather than rotating them open. / 골반이 열리지 않도록 바닥과 나란히 유지하세요.
- Breathe deeply and avoid holding your breath during the pose. / 깊게 호흡하고 숨을 참지 마세요.

## 7-5. Reclined pigeon pose (좌/우) · 누운 비둘기 자세 — 2분

`reclined-pigeon-pose-left` / `reclined-pigeon-pose-right` 〔API: glutes〕
중점: 심부 외회전근 이완 | 근육 신장: 이상근, 중둔근, 소둔근, 대둔근

**순서**
1. Lie on your back with knees bent and feet flat. / 무릎을 세우고 발바닥을 바닥에 붙인 채 눕습니다.
2. Cross one ankle over the opposite knee. / 한쪽 발목을 반대쪽 무릎 위에 올려 교차시킵니다.
3. Thread your hands behind the supporting thigh. / 두 손을 지지하는 쪽 허벅지 뒤로 넣어 깍지 낍니다.
4. Gently pull the thigh toward your chest. / 허벅지를 가슴 쪽으로 부드럽게 당깁니다.

**주의사항**
- Keep your head and shoulders on the floor. / 머리와 어깨는 바닥에 붙인 상태를 유지하세요.
- Flex the crossed foot to protect the knee. / 교차한 발을 플렉스(발목 꺾기)해 무릎을 보호하세요.
- Breathe deeply into the hip stretch. / 고관절 스트레칭 부위로 깊게 호흡하세요.

## 7-6. Bridge pose · 브릿지 〔핀포즈〕 — 2분 (40초 × 3)

`bridge-pose` 〔API: glutes〕
중점: 고관절 신전 | 근육: 대둔근, 햄스트링, 척추기립근, 내전근(무릎 모으기) / 신장: 장요근, 대퇴직근

**순서**
1. Lie on your back with knees bent and feet flat on the floor, hip-width apart. / 무릎을 세우고 발을 골반 너비로 벌려 바닥에 붙인 채 눕습니다.
2. Place arms alongside your body with palms facing down. / 손바닥이 아래를 향하도록 팔을 몸 옆에 놓습니다.
3. Press your feet into the floor and lift your hips toward the ceiling. / 발로 바닥을 밀며 골반을 천장 쪽으로 들어 올립니다.
4. Hold at the top, then slowly lower back down. / 가장 높은 지점에서 유지한 뒤 천천히 내려옵니다.

**주의사항**
- Keep knees aligned over ankles. / 무릎이 발목 위에 오도록 정렬하세요.
- Engage your glutes and core to support the lift. / 둔근과 코어에 힘을 주어 들어 올리는 동작을 받쳐주세요.
- Avoid turning your head while in the pose. / 자세를 유지하는 동안 고개를 돌리지 마세요.

**루틴 주의사항** — 자세를 유지하는 동안 목을 좌우로 돌리지 마세요. 경추에 체중이 실린 상태의 회전은 위험합니다. 허리가 조이면 절반 높이에서 멈추세요.

---

# 8. 골반 라인 · 레벨 2 — Garland pose (말라사나) · 17분 30초

제한 요인: 발목 배측굴곡 제한, 고관절 외회전 부족, 내전근 단축, 척추기립근 약화

**루틴 순서**

| # | 동작 | 시간 |
| --- | --- | --- |
| 1 | Cat cow pose · 고양이-소 자세 | 1분 30초 |
| 2 | Downward dog · 다운독 | 2분 |
| 3 | Low lunge (좌/우) · 로우 런지 | 2분 30초 |
| 4 | Cobbler's pose · 나비 자세 | 2분 30초 |
| 5 | Frog pose · 개구리 자세 | 2분 |
| 6 | Chair pose · 의자 자세 | 2분 |
| 7 | **Garland pose · 말라사나** 〔핀포즈〕 | 2분 (40초 × 3) |

**휴식 배치**

| 구간 | 종류 | 시간 | 화면 문구 |
| --- | --- | --- | --- |
| 1 → 2 | 전환 | 10초 | 다음 자세 준비 — 다운독 |
| 2 → 3 | 전환 | 10초 | 다음 자세 준비 — 로우 런지 |
| 3 → 4 | 전환 | 10초 | 다음 자세 준비 — 나비 자세 |
| 4 → 5 | 전환 | 10초 | 다음 자세 준비 — 개구리 자세 |
| 5 → 6 (개구리 후) | 휴식 〔C〕 | 30초 | 편하게 앉아서 쉬세요 |
| 6 → 7 (의자 후 + 핀포즈 직전) | 휴식 〔C〕 | 50초 | 편하게 앉아서 쉬세요 |
| 핀포즈 세트 사이 ×2 | 휴식 〔C〕 | 핀포즈 2분 내 처리 | 편하게 앉아서 쉬세요 |
| 루틴 종료 | 마무리 〔C〕 | 60초 | 편하게 앉아서 쉬세요 |

휴식 총량 180초 (전환 40 + 휴식 80 + 마무리 60) — 기존과 동일, 17분 30초 유지. 개구리 자세는 내전근 부하가 커서 이완 자세지만 휴식으로 처리했다.

## 8-1. Cat cow pose · 고양이-소 자세 — 1분 30초

`cat-cow-pose` 〔API: back〕
중점: 척추 분절 가동성 (웜업) | 근육: 척추기립근, 다열근, 복횡근

**순서**
1. Start on all fours with wrists under shoulders and knees under hips. / 손목은 어깨 아래, 무릎은 골반 아래에 두고 네발기기 자세로 시작합니다.
2. Inhale, drop your belly toward the mat, lift your chest and tailbone (Cow). / 숨을 마시며 배를 매트 쪽으로 내리고, 가슴과 꼬리뼈를 들어 올립니다 (소 자세).
3. Exhale, round your spine toward the ceiling, tuck your chin and tailbone (Cat). / 숨을 내쉬며 척추를 천장 쪽으로 둥글게 말고, 턱과 꼬리뼈를 안으로 말아 넣습니다 (고양이 자세).
4. Continue flowing between Cat and Cow with each breath. / 호흡에 맞춰 고양이와 소 자세를 이어서 반복합니다.

**주의사항**
- Keep arms straight throughout the movement. / 동작 내내 팔은 곧게 편 상태를 유지하세요.
- Initiate the movement from your pelvis. / 움직임은 골반에서부터 시작하세요.
- Move slowly and synchronize with your breath. / 천천히 움직이며 호흡과 동작을 일치시키세요.

## 8-2. Downward dog · 다운독 — 2분

`downward-dog` 〔API: full_body〕
중점: 발목 배측굴곡 + 후방 사슬 (말라사나의 발뒤꿈치 접지 대비) | 근육 신장: 비복근, 가자미근, 햄스트링, 광배근 / 주동: 전거근, 삼각근

**순서**
1. Start on all fours, then tuck your toes and lift your hips up and back. / 네발기기 자세에서 발끝을 세우고 골반을 위쪽 뒤로 들어 올립니다.
2. Straighten your legs as much as possible, pressing heels toward the floor. / 발뒤꿈치를 바닥 쪽으로 누르며 다리를 최대한 곧게 폅니다.
3. Spread your fingers wide and press firmly through your palms. / 손가락을 넓게 펴고 손바닥으로 바닥을 단단히 밀어냅니다.
4. Let your head hang naturally between your arms. / 머리는 두 팔 사이에서 자연스럽게 늘어뜨립니다.

**주의사항**
- Create an inverted V-shape with your body. / 몸으로 역V자 모양을 만드세요.
- Keep a slight bend in your knees if your hamstrings are tight. / 햄스트링이 뻣뻣하다면 무릎을 살짝 구부리세요.
- Rotate your upper arms outward to broaden the shoulders. / 위팔을 바깥으로 회전시켜 어깨를 넓게 열어주세요.

## 8-3. Low lunge (좌/우) · 로우 런지 — 2분 30초

`low-lunge-left` / `low-lunge-right` 〔API: quads〕
중점: 고관절 굴곡근 신장 | 근육 신장: 장요근, 대퇴직근 / 주동: 대둔근

**순서**
1. From Downward Dog, step one foot forward between your hands. / 다운독 자세에서 한쪽 발을 두 손 사이로 내딛습니다.
2. Lower your back knee to the mat. / 뒤쪽 무릎을 매트에 내립니다.
3. Stack your front knee over the ankle. / 앞쪽 무릎이 발목 바로 위에 오도록 둡니다.
4. Raise your arms overhead or keep hands on the floor. / 팔을 머리 위로 들어 올리거나 손을 바닥에 둡니다.

**주의사항**
- Pad your back knee with a blanket if needed. / 필요하면 뒤쪽 무릎 아래에 담요를 받치세요.
- Sink your hips forward and down for a deeper stretch. / 골반을 앞쪽 아래로 가라앉히면 스트레칭이 깊어집니다.
- Keep your core engaged and spine tall. / 코어에 힘을 주고 척추를 곧게 세우세요.

## 8-4. Cobbler's pose · 나비 자세 — 2분 30초

`cobblers-pose` 〔API: glutes〕
중점: 고관절 외회전 + 내전근 | 근육 신장: 장·단내전근, 박근, 치골근 / 주동: 심부 외회전근

**순서**
1. Sit on the floor with the soles of your feet together. / 양 발바닥을 마주 붙이고 바닥에 앉습니다.
2. Let your knees drop open to the sides. / 무릎이 양옆으로 벌어지도록 둡니다.
3. Hold your feet or ankles with your hands. / 두 손으로 발이나 발목을 잡습니다.
4. Sit tall, lengthening your spine. / 척추를 길게 늘이며 바르게 앉습니다.

**주의사항**
- Do not force your knees down. / 무릎을 억지로 눌러 내리지 마세요.
- Sit on a cushion if your hips are tight. / 고관절이 뻣뻣하다면 쿠션 위에 앉으세요.
- Keep your spine long and shoulders relaxed. / 척추는 길게, 어깨는 편안하게 유지하세요.

## 8-5. Frog pose · 개구리 자세 — 2분

`frog-pose` 〔API: glutes〕 ※ 실제 타깃은 내전근
중점: 고관절 외전 + 내전근 심화 | 근육 신장: 대내전근, 장내전근, 박근

**순서**
1. Begin by kneeling on your mat and gradually lower down to your forearms. / 매트 위에 무릎을 대고 앉았다가 전완까지 천천히 내려옵니다.
2. Spread your knees wide apart while keeping your shins parallel to each other. / 정강이를 서로 나란히 유지한 채 무릎을 넓게 벌립니다.
3. Rest your forearms on the ground and allow your hips to sink toward the floor. / 전완을 바닥에 놓고 골반이 바닥 쪽으로 가라앉도록 둡니다.
4. Breathe deeply and hold the position, focusing on relaxing into the stretch. / 깊게 호흡하며 스트레칭에 몸을 맡기는 데 집중하며 자세를 유지합니다.
5. Only go as deep as feels comfortable and avoid forcing the stretch. / 편안하게 느껴지는 범위까지만 내려가고 억지로 밀어붙이지 않습니다.
6. Hold for 30 seconds to 2 minutes depending on your flexibility level. / 유연성 수준에 따라 30초~2분간 유지합니다.

**주의사항**
- Keep your knees and ankles aligned to protect your knee joints. / 무릎과 발목을 일직선으로 정렬해 무릎 관절을 보호하세요.
- Only sink as low as your body naturally allows without pain or discomfort. / 통증이나 불편함 없이 자연스럽게 내려가는 만큼만 내려가세요.
- Place a pillow or bolster under your torso if you need additional support. / 지지가 더 필요하면 상체 아래에 베개나 볼스터를 받치세요.
- Stop immediately if you feel any sharp pain in your knees, hips, or lower back. / 무릎, 고관절, 허리에 날카로운 통증이 느껴지면 즉시 중단하세요.

## 8-6. Chair pose · 의자 자세 — 2분

`chair-pose` 〔API: quads〕
중점: 하체 등척성 근력 + 발목 배측굴곡 | 근육: 대퇴사두근, 대둔근, 척추기립근, 가자미근

**순서**
1. Stand with feet together or hip-width apart. / 발을 모으거나 골반 너비로 벌리고 섭니다.
2. Bend your knees and lower your hips as if sitting in a chair. / 무릎을 굽히고 의자에 앉듯 골반을 낮춥니다.
3. Raise your arms overhead alongside your ears. / 팔을 귀 옆으로 나란히 머리 위로 들어 올립니다.
4. Hold, keeping weight in your heels. / 체중을 발뒤꿈치에 실은 채 유지합니다.

**주의사항**
- Keep knees behind your toes. / 무릎이 발끝을 넘어가지 않게 하세요.
- Engage your core and lengthen your spine. / 코어에 힘을 주고 척추를 길게 늘이세요.
- Shift weight into your heels. / 체중을 발뒤꿈치 쪽으로 실으세요.

## 8-7. Garland pose · 말라사나 〔핀포즈〕 — 2분 (40초 × 3)

`garland-pose` 〔API: glutes〕
중점: 깊은 고관절 굴곡·외회전 + 발목 배측굴곡 | 근육: 대둔근, 척추기립근, 대퇴사두근(등척) / 신장: 내전근, 가자미근·아킬레스

**순서**
1. Stand with feet slightly wider than hip-width, toes turned out. / 발을 골반보다 약간 넓게 벌리고 발끝을 바깥으로 돌려 섭니다.
2. Bend your knees and lower into a deep squat. / 무릎을 굽혀 깊은 스쿼트로 내려갑니다.
3. Bring your palms together at your chest. / 가슴 앞에서 두 손바닥을 마주 붙입니다.
4. Press your elbows against your inner knees to open the hips. / 팔꿈치로 무릎 안쪽을 밀어 고관절을 열어줍니다.

**주의사항**
- Keep your heels on the floor (place a rolled mat under them if needed). / 발뒤꿈치를 바닥에 붙이세요 (필요하면 말은 매트를 받치세요).
- Lengthen your spine and lift your chest. / 척추를 길게 늘이고 가슴을 들어 올리세요.
- Engage your core for balance. / 균형을 위해 코어에 힘을 주세요.

**루틴 주의사항** — 발뒤꿈치가 안 닿으면 수건을 접어 받치세요. 억지로 누르면 발목과 무릎에 무리가 갑니다. 무릎 통증이 있으면 중단하세요.

---

# 9. 골반 라인 · 레벨 3 — Fire log pose (파이어로그) · 20분

제한 요인: 고관절 외회전 극단, 이상근·둔근 심부 경직, 골반 후방경사

**루틴 순서**

| # | 동작 | 시간 |
| --- | --- | --- |
| 1 | Cat cow pose · 고양이-소 자세 | 1분 30초 |
| 2 | Reclined butterfly · 누운 나비 자세 | 2분 |
| 3 | Happy baby pose · 해피 베이비 자세 | 2분 |
| 4 | Reclined pigeon pose (좌/우) · 누운 비둘기 자세 | 2분 30초 |
| 5 | Cobbler's pose · 나비 자세 | 2분 30초 |
| 6 | Half lord of the fishes (좌/우) · 반 물고기의 왕 자세 | 2분 〔Half frog 대체〕 |
| 7 | Garland pose · 말라사나 | 2분 |
| 8 | **Fire log pose (좌/우) · 파이어로그** 〔핀포즈〕 | 2분 (좌우 각 30초 × 2) |

**휴식 배치**

| 구간 | 종류 | 시간 | 화면 문구 |
| --- | --- | --- | --- |
| 1 → 2 | 전환 | 10초 | 다음 자세 준비 — 누운 나비 자세 |
| 2 → 3 | 전환 | 10초 | 다음 자세 준비 — 해피 베이비 자세 |
| 3 → 4 | 전환 | 10초 | 다음 자세 준비 — 누운 비둘기 자세 |
| 4 → 5 (누운 비둘기 후) | 휴식 〔A〕 | 30초 | 그대로 누워서 편하게 호흡하세요 |
| 5 → 6 | 전환 | 10초 | 다음 자세 준비 — 반 물고기의 왕 자세 |
| 6 → 7 (반 물고기의 왕 후) | 휴식 〔C〕 | 30초 | 편하게 앉아서 쉬세요 |
| 7 → 8 (말라사나 후 + 핀포즈 직전) | 휴식 〔C〕 | 50초 | 편하게 앉아서 쉬세요 |
| 핀포즈 좌우 전환 및 세트 사이 | 휴식 〔C〕 | 핀포즈 2분 내 처리 | 편하게 앉아서 쉬세요 |
| 루틴 종료 | 마무리 〔C〕 | 60초 | 편하게 앉아서 쉬세요 |

휴식 총량 210초 (전환 40 + 휴식 110 + 마무리 60) — 기존과 동일, 20분 유지. 무릎 부담이 큰 파이어로그 전에 회복 구간을 두 번 넣었다.

> **변경**: 6번 Half frog → Half lord of the fishes (좌/우)로 교체했다. 사유는 문서 상단 0.2 참조.

## 9-1. Cat cow pose · 고양이-소 자세 — 1분 30초

`cat-cow-pose` 〔API: back〕
중점: 척추 분절 가동성 (웜업) | 근육: 척추기립근, 다열근, 복횡근

**순서**
1. Start on all fours with wrists under shoulders and knees under hips. / 손목은 어깨 아래, 무릎은 골반 아래에 두고 네발기기 자세로 시작합니다.
2. Inhale, drop your belly toward the mat, lift your chest and tailbone (Cow). / 숨을 마시며 배를 매트 쪽으로 내리고, 가슴과 꼬리뼈를 들어 올립니다 (소 자세).
3. Exhale, round your spine toward the ceiling, tuck your chin and tailbone (Cat). / 숨을 내쉬며 척추를 천장 쪽으로 둥글게 말고, 턱과 꼬리뼈를 안으로 말아 넣습니다 (고양이 자세).
4. Continue flowing between Cat and Cow with each breath. / 호흡에 맞춰 고양이와 소 자세를 이어서 반복합니다.

**주의사항**
- Keep arms straight throughout the movement. / 동작 내내 팔은 곧게 편 상태를 유지하세요.
- Initiate the movement from your pelvis. / 움직임은 골반에서부터 시작하세요.
- Move slowly and synchronize with your breath. / 천천히 움직이며 호흡과 동작을 일치시키세요.

## 9-2. Reclined butterfly · 누운 나비 자세 — 2분

`reclined-butterfly` 〔API: glutes〕 ※ 실제 타깃은 내전근
중점: 고관절 외회전 수동 이완 | 근육 신장: 장·단내전근, 박근, 치골근

**순서**
1. Lie on your back and bring the soles of your feet together. / 등을 대고 누워 양 발바닥을 마주 붙입니다.
2. Let your knees fall open to the sides. / 무릎이 양옆으로 자연스럽게 벌어지도록 둡니다.
3. Rest your arms at your sides with palms up. / 손바닥이 위를 향하도록 팔을 몸 옆에 놓습니다.
4. Close your eyes and relax for several minutes. / 눈을 감고 몇 분간 이완합니다.

**주의사항**
- Place pillows or blocks under your knees for support. / 무릎 아래에 베개나 블록을 받쳐 지지해 주세요.
- Let gravity do the work - do not force the stretch. / 중력에 맡기고 억지로 스트레칭하지 마세요.
- This is a restorative pose - focus on deep breathing. / 회복(리스토러티브) 자세이므로 깊은 호흡에 집중하세요.

## 9-3. Happy baby pose · 해피 베이비 자세 — 2분

`happy-baby-pose` 〔API: glutes〕
중점: 고관절 굴곡 가동범위 | 근육 신장: 대둔근, 햄스트링, 내전근

**순서**
1. Lie on your back and bring your knees toward your chest. / 등을 대고 누워 무릎을 가슴 쪽으로 가져옵니다.
2. Grab the outsides of your feet with your hands. / 두 손으로 발 바깥쪽을 잡습니다.
3. Open your knees wider than your torso. / 무릎을 몸통보다 넓게 벌립니다.
4. Gently pull your feet down, keeping your lower back on the mat. / 허리를 매트에 붙인 채 발을 아래로 부드럽게 당깁니다.

**주의사항**
- Keep your lower back and sacrum on the floor. / 허리와 천골이 바닥에서 뜨지 않게 하세요.
- Rock gently side to side for a spinal massage. / 좌우로 부드럽게 흔들면 척추를 마사지하는 효과가 있습니다.
- Relax your shoulders and neck. / 어깨와 목의 힘을 빼세요.

## 9-4. Reclined pigeon pose (좌/우) · 누운 비둘기 자세 — 2분 30초

`reclined-pigeon-pose-left` / `reclined-pigeon-pose-right` 〔API: glutes〕
중점: 심부 외회전근 (파이어로그의 직접 예행) | 근육 신장: 이상근, 중둔근, 소둔근, 대둔근

**순서**
1. Lie on your back with knees bent and feet flat. / 무릎을 세우고 발바닥을 바닥에 붙인 채 눕습니다.
2. Cross one ankle over the opposite knee. / 한쪽 발목을 반대쪽 무릎 위에 올려 교차시킵니다.
3. Thread your hands behind the supporting thigh. / 두 손을 지지하는 쪽 허벅지 뒤로 넣어 깍지 낍니다.
4. Gently pull the thigh toward your chest. / 허벅지를 가슴 쪽으로 부드럽게 당깁니다.

**주의사항**
- Keep your head and shoulders on the floor. / 머리와 어깨는 바닥에 붙인 상태를 유지하세요.
- Flex the crossed foot to protect the knee. / 교차한 발을 플렉스(발목 꺾기)해 무릎을 보호하세요.
- Breathe deeply into the hip stretch. / 고관절 스트레칭 부위로 깊게 호흡하세요.

## 9-5. Cobbler's pose · 나비 자세 — 2분 30초

`cobblers-pose` 〔API: glutes〕
중점: 고관절 외회전 + 내전근 | 근육 신장: 내전근군 / 주동: 심부 외회전근

**순서**
1. Sit on the floor with the soles of your feet together. / 양 발바닥을 마주 붙이고 바닥에 앉습니다.
2. Let your knees drop open to the sides. / 무릎이 양옆으로 벌어지도록 둡니다.
3. Hold your feet or ankles with your hands. / 두 손으로 발이나 발목을 잡습니다.
4. Sit tall, lengthening your spine. / 척추를 길게 늘이며 바르게 앉습니다.

**주의사항**
- Do not force your knees down. / 무릎을 억지로 눌러 내리지 마세요.
- Sit on a cushion if your hips are tight. / 고관절이 뻣뻣하다면 쿠션 위에 앉으세요.
- Keep your spine long and shoulders relaxed. / 척추는 길게, 어깨는 편안하게 유지하세요.

## 9-6. Half lord of the fishes pose (좌/우) · 반 물고기의 왕 자세 — 2분 〔Half frog 대체〕

`half-lord-of-the-fishes-pose-left` / `half-lord-of-the-fishes-pose-right` 〔API: core〕
중점: 대둔근·이상근 신장 + 좌골 위 골반 세우기 | 근육 신장: 대둔근, 이상근, 광배근 / 주동: 복사근, 다열근, 척추기립근

**순서**
1. Sit with legs extended. Bend one knee and place the foot outside the opposite thigh. / 다리를 뻗고 앉습니다. 한쪽 무릎을 구부려 발을 반대쪽 허벅지 바깥에 놓습니다.
2. Bend the bottom leg and bring the foot near the opposite hip. / 아래쪽 다리를 구부려 발을 반대쪽 엉덩이 가까이 가져옵니다.
3. Twist your torso toward the bent knee. / 구부린 무릎 쪽으로 상체를 비틉니다.
4. Use the opposite elbow against the outside of the bent knee for leverage. / 반대쪽 팔꿈치를 구부린 무릎 바깥에 대어 지렛대로 사용합니다.

**주의사항**
- Sit tall before you twist - lengthen your spine first. / 비틀기 전에 먼저 척추를 길게 늘여 바르게 앉으세요.
- Twist from your mid-back, not just your shoulders. / 어깨만이 아니라 등 중간(흉추)에서부터 비트세요.
- Keep both sit bones grounded. / 양쪽 좌골이 바닥에 고르게 닿아 있게 하세요.

## 9-7. Garland pose · 말라사나 — 2분

`garland-pose` 〔API: glutes〕
중점: 깊은 고관절 굴곡·외회전 | 근육: 대둔근, 척추기립근 / 신장: 내전근, 가자미근

**순서**
1. Stand with feet slightly wider than hip-width, toes turned out. / 발을 골반보다 약간 넓게 벌리고 발끝을 바깥으로 돌려 섭니다.
2. Bend your knees and lower into a deep squat. / 무릎을 굽혀 깊은 스쿼트로 내려갑니다.
3. Bring your palms together at your chest. / 가슴 앞에서 두 손바닥을 마주 붙입니다.
4. Press your elbows against your inner knees to open the hips. / 팔꿈치로 무릎 안쪽을 밀어 고관절을 열어줍니다.

**주의사항**
- Keep your heels on the floor (place a rolled mat under them if needed). / 발뒤꿈치를 바닥에 붙이세요 (필요하면 말은 매트를 받치세요).
- Lengthen your spine and lift your chest. / 척추를 길게 늘이고 가슴을 들어 올리세요.
- Engage your core for balance. / 균형을 위해 코어에 힘을 주세요.

## 9-8. Fire log pose (좌/우) · 파이어로그 〔핀포즈〕 — 2분 (좌우 각 30초 × 2)

`fire-log-pose-left` / `fire-log-pose-right` 〔API: glutes〕
중점: 극단적 고관절 외회전 | 근육 신장: 이상근, 상·하쌍자근, 내·외폐쇄근, 대퇴방형근(심부 6근), 중둔근, 대둔근

**순서**
1. Sit with your left leg in front, knee bent at 90 degrees with the shin parallel to the front edge of your mat. / 왼쪽 다리를 앞에 두고 무릎을 90도로 구부려 정강이가 매트 앞쪽 가장자리와 나란하도록 앉습니다.
2. Place your right leg on top, stacking the right shin directly over the left shin with both knees bent. / 오른쪽 다리를 위에 올려, 두 무릎을 구부린 채 오른쪽 정강이를 왼쪽 정강이 바로 위에 포갭니다.
3. Keep both feet flexed to protect your knee joints. / 무릎 관절을 보호하기 위해 양발을 플렉스 상태로 유지합니다.
4. Sit tall with your spine straight and hands resting on your shins or the floor. / 척추를 곧게 세워 앉고 두 손은 정강이나 바닥에 얹습니다.
5. Hold the position for 30-60 seconds while breathing deeply. / 깊게 호흡하며 30~60초간 유지합니다.
6. To release, carefully lift the top leg and extend both legs forward. / 풀 때는 위쪽 다리를 조심스럽게 들어 올린 뒤 두 다리를 앞으로 폅니다.

**주의사항**
- Keep both feet flexed throughout to protect your knees from strain. / 무릎에 무리가 가지 않도록 동작 내내 양발을 플렉스 상태로 유지하세요.
- If your hips are tight, sit on a bolster or folded blanket to elevate your pelvis. / 고관절이 뻣뻣하면 볼스터나 접은 담요 위에 앉아 골반을 높이세요.
- Never force your knees down - let gravity and time create the opening. / 무릎을 절대 억지로 눌러 내리지 말고, 중력과 시간이 열어주도록 두세요.
- Stop immediately if you feel any sharp pain in your knees or hips. / 무릎이나 고관절에 날카로운 통증이 느껴지면 즉시 중단하세요.

**루틴 주의사항** — 회전은 고관절에서 나와야 합니다. 무릎에서 비틀면 반월판 손상 위험이 있습니다. 무릎에 통증이나 조이는 느낌이 오면 즉시 중단하고 Reclined pigeon으로 돌아가세요. 골반이 뒤로 말리면 담요나 쿠션 위에 앉으세요.

---

# 10. 최종 자세 목록 (29개)

| # | EN | KO | slug | API 태그 | 등장 루틴 수 |
| --- | --- | --- | --- | --- | --- |
| 1 | Cat cow pose | 고양이-소 자세 | `cat-cow-pose` | back | 9 |
| 2 | Sphinx pose | 스핑크스 자세 | `sphinx-pose` | back | 1 |
| 3 | Cow face pose (L/R) | 소 얼굴 자세 | `cow-face-pose-*` | shoulders | 3 |
| 4 | Tiger pose (L/R) | 호랑이 자세 | `tiger-pose-*` | core | 5 |
| 5 | Cobra pose | 코브라 자세 | `cobra-pose` | back | 2 |
| 6 | **Upward facing dog pose** | **업독** | `upward-facing-dog-pose` | back | 1 (핀) |
| 7 | Puppy pose | 퍼피 자세 | `puppy-pose` | back | 2 |
| 8 | Low lunge (L/R) | 로우 런지 | `low-lunge-*` | quads | 3 |
| 9 | **Bridge pose** | **브릿지** | `bridge-pose` | glutes | 3 (핀 1) |
| 10 | **Camel pose** | **낙타자세** | `camel-pose` | back | 2 (핀 1) |
| 11 | Bow pose | 활 자세 | `bow-pose` | back | 1 |
| 12 | **Wheel pose** | **휠** | `wheel-pose` | back | 1 (핀) |
| 13 | Reclined windshield wipers | 누워서 와이퍼 | `reclined-windshield-wipers` | core | 3 |
| 14 | Plank pose | 플랭크 자세 | `plank-pose` | core | 3 |
| 15 | Seated forward bend | 앉은 전굴 자세 | `seated-forward-bend` | hamstrings | 2 |
| 16 | **Half boat pose** | **반 보트** | `half-boat-pose` | core | 2 (핀 1) |
| 17 | **Boat pose** | **보트자세** | `boat-pose` | core | 2 (핀 1) |
| 18 | Gate pose (L/R) | 문 빗장 자세 | `gate-pose-*` | core | 1 |
| 19 | Half lord of the fishes (L/R) | 반 물고기의 왕 자세 | `half-lord-of-the-fishes-pose-*` | core | 2 |
| 20 | **Side plank pose (L/R)** | **사이드 플랭크** | `side-plank-pose-*` | core | 1 (핀) |
| 21 | Reclined butterfly | 누운 나비 자세 | `reclined-butterfly` | glutes | 2 |
| 22 | Happy baby pose | 해피 베이비 자세 | `happy-baby-pose` | glutes | 2 |
| 23 | Reclined pigeon pose (L/R) | 누운 비둘기 자세 | `reclined-pigeon-pose-*` | glutes | 2 |
| 24 | Downward dog | 다운독 | `downward-dog` | full_body | 1 |
| 25 | Cobbler's pose | 나비 자세 | `cobblers-pose` | glutes | 2 |
| 26 | Frog pose | 개구리 자세 | `frog-pose` | glutes | 1 |
| 27 | Chair pose | 의자 자세 | `chair-pose` | quads | 1 |
| 28 | **Garland pose** | **말라사나** | `garland-pose` | glutes | 2 (핀 1) |
| 29 | **Fire log pose (L/R)** | **파이어로그** | `fire-log-pose-*` | glutes | 1 (핀) |

**제외된 자세 4개**
- Half split (`half-split-*`), Standing forward bend (`standing-forward-bend`), Seated wide angle straddle (`seated-wide-angle-straddle`) — 체크포인트 대응 전용
- Half frog (`half-frog-pose-*`) — 배치 오류로 교체 (라이브러리에는 유지, 등 라인 재배치 권장)
