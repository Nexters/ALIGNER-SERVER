-- 원인. 진단 결과가 순위로 보여주는 값이고 course 가 처방에 쓴다.
--
-- body_part_code 를 갖는 이유는 "느끼는 부위" 와 "원인 부위" 가 다르기 때문이다
-- (docs/domains.md §4-2). 목을 신경 써서 들어와도 원인은 흉추·어깨로 나올 수 있다.
--
-- 같은 도메인 안이라 FK 를 건다. 도메인 간 FK 금지(§6)는 screening ↔ catalog 처럼
-- 경계를 넘는 경우다.
CREATE TABLE screening.cause (
    cause_code     VARCHAR(40) PRIMARY KEY,
    name           VARCHAR(50) NOT NULL,
    body_part_code VARCHAR(40) NOT NULL,
    description    TEXT,
    CONSTRAINT fk_cause_body_part FOREIGN KEY (body_part_code)
        REFERENCES screening.body_part (body_part_code)
);

-- 원인 목록을 부위로 거르는 조회가 생긴다.
CREATE INDEX ix_cause_body_part_code ON screening.cause (body_part_code);

COMMENT ON TABLE screening.cause IS '판별 대상 원인. course 가 코스 처방의 입력으로 쓴다';
COMMENT ON COLUMN screening.cause.body_part_code IS '원인이 있는 부위. 회원이 고른 부위와 다를 수 있다';
COMMENT ON COLUMN screening.cause.description IS '진단 결과 화면에 보여줄 설명. 감수 대상이다';
