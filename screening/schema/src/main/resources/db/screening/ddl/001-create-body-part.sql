-- 신경 쓰이는 부위. 온보딩 첫 화면의 선택지다.
--
-- 코드가 PK 다. 대리키를 두지 않는 것은 body_part_code 가 screening.cause·catalog.target_pose·
-- screening.screening_result 세 곳에 값으로 퍼지기 때문이다. 자동 증가 id 를 쓰면 seed 를
-- 다시 쌓을 때 값이 달라져 흩어진 참조가 조용히 어긋난다.
CREATE TABLE screening.body_part (
    body_part_code VARCHAR(40) PRIMARY KEY,
    name           VARCHAR(50) NOT NULL,
    display_order  SMALLINT    NOT NULL,
    CONSTRAINT uk_body_part_display_order UNIQUE (display_order),
    CONSTRAINT ck_body_part_display_order CHECK (display_order > 0)
);

COMMENT ON TABLE screening.body_part IS '신경 쓰이는 부위. 온보딩의 진입점이다';
COMMENT ON COLUMN screening.body_part.body_part_code IS 'NECK_SHOULDER 처럼 우리가 정한 코드. 표시용 한글은 name 이다';
COMMENT ON COLUMN screening.body_part.display_order IS '화면 노출 순서. 목록 조회가 이 순서로 돌려준다';
