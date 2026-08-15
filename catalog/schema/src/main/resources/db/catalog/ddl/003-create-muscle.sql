-- 근육 마스터. muscle_code 는 자연키다.
--
-- screening 은 이 테이블을 참조하지 않는다. 진단 결과의 "어떤 근육이 부족하다" 문구는
-- screening.cause.description 에 감수 문구로 넣는다 (docs/domains.md §4-3).
CREATE TABLE catalog.muscle (
    muscle_code         VARCHAR(40) PRIMARY KEY,
    name                VARCHAR(50) NOT NULL,
    body_part_code      VARCHAR(40) NOT NULL,
    highlight_asset_key VARCHAR(120)
);

COMMENT ON TABLE catalog.muscle IS '근육 마스터. 운동 가이드의 부위 탭과 근육맵에 쓴다';
COMMENT ON COLUMN catalog.muscle.name IS '척추기립근·대둔근처럼 감수자가 정한 표기다';
COMMENT ON COLUMN catalog.muscle.body_part_code IS 'screening.body_part 의 값이지만 도메인 간 FK 는 걸지 않는다';
COMMENT ON COLUMN catalog.muscle.highlight_asset_key IS '근육맵 이미지 식별자. URL 이 아니고 파일은 프론트가 갖는다';
