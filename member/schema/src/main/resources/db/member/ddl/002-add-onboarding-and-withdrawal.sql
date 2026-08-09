-- 온보딩 입력값과 탈퇴 표시를 회원에 붙인다.
--
-- docs/domains.md §4-1 이 키·몸무게·운동 경력을 member 소유로 적어뒀지만 컬럼이 없었다.
-- 몸무게는 칼로리 계산의 입력이라(§4-3) course 착수의 선행 조건이다.
--
-- 전부 nullable 이다. 가입 직후에는 아무것도 없고 온보딩이 화면마다 나눠서 채운다.
ALTER TABLE member.member
    ADD COLUMN height_cm                    SMALLINT,
    ADD COLUMN weight_kg                    SMALLINT,
    ADD COLUMN experience_level             VARCHAR(20),
    ADD COLUMN reinforcement_body_part_code VARCHAR(40),
    ADD COLUMN reinforcement_level          SMALLINT,
    ADD COLUMN withdrawn_at                 TIMESTAMPTZ;

-- 탈퇴는 행을 지우지 않고 카카오 식별자만 지운다. 운동 기록을 보존하기로 했고, 그 기록이
-- member_id 로 붙어 있기 때문이다. 남는 개인정보가 kakao_id 뿐이라 그것만 NULL 로 만든다.
--
-- NOT NULL 을 푸는 것이 그래서 필요하다. UNIQUE 는 그대로 둔다 — PostgreSQL 은 NULL 을
-- 서로 다른 값으로 보므로 탈퇴 회원이 여럿이어도 충돌하지 않고, 같은 카카오 계정이 다시
-- 가입하면 새 member_id 를 받는다.
ALTER TABLE member.member
    ALTER COLUMN kakao_id DROP NOT NULL;

-- 값 집합과 범위는 DB 도 막는다. 애그리거트가 먼저 400 으로 막지만, 거기를 지나 들어온
-- 값이 조용히 저장되면 칼로리 계산이 말없이 틀어진다.
ALTER TABLE member.member
    ADD CONSTRAINT ck_member_height_cm
        CHECK (height_cm IS NULL OR height_cm BETWEEN 100 AND 250),
    ADD CONSTRAINT ck_member_weight_kg
        CHECK (weight_kg IS NULL OR weight_kg BETWEEN 20 AND 300),
    ADD CONSTRAINT ck_member_experience_level
        CHECK (experience_level IS NULL
               OR experience_level IN ('UNDER_ONE_YEAR', 'ONE_TO_THREE_YEARS', 'OVER_THREE_YEARS')),
    ADD CONSTRAINT ck_member_reinforcement_level
        CHECK (reinforcement_level IS NULL OR reinforcement_level BETWEEN 1 AND 3),
    -- 부위와 난이도는 한 화면에서 같이 고른다. 한쪽만 있는 상태는 만들지 않는다.
    ADD CONSTRAINT ck_member_reinforcement_pair
        CHECK ((reinforcement_body_part_code IS NULL) = (reinforcement_level IS NULL));

-- 탈퇴하지 않은 회원만 조회하므로 부분 인덱스로 좁힌다.
CREATE INDEX ix_member_active ON member.member (member_id) WHERE withdrawn_at IS NULL;

COMMENT ON COLUMN member.member.height_cm IS '키(cm). 온보딩에서 받는다';
COMMENT ON COLUMN member.member.weight_kg IS '몸무게(kg). 칼로리 계산의 입력이다';
COMMENT ON COLUMN member.member.experience_level IS '운동 경력. 1년 미만 / 1~3년 / 3년 이상';
COMMENT ON COLUMN member.member.reinforcement_body_part_code IS
    '회원이 고른 강화 부위. screening.body_part 의 값이지만 도메인 간 FK 는 걸지 않는다';
COMMENT ON COLUMN member.member.reinforcement_level IS
    '강화 난이도 1(하)·2(중)·3(상). catalog.target_pose.level 과 같은 축인지는 미확정이다';
COMMENT ON COLUMN member.member.withdrawn_at IS '탈퇴 시각. NULL 이면 이용 중이다';
COMMENT ON COLUMN member.member.kakao_id IS
    '카카오 회원번호. 탈퇴하면 NULL 이 된다 — 기록은 남기고 개인정보만 지운다';
