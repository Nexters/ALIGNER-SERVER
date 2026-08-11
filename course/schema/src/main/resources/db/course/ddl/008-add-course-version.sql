-- 코스에 낙관적 락을 건다.
--
-- 스텝 완료는 읽기 → 도메인 판단 → 애그리거트 통째 저장이다. Spring Data JDBC 의 애그리거트
-- 저장은 자식을 지우고 다시 넣으므로, 두 세션 완료가 동시에 들어오면 **나중 저장이 앞선
-- 완료를 덮어쓴다.** training 이 세션 완료를 push 하고 재시도까지 하므로 실제로 일어난다.
--
-- 판정을 SQL 로 내리는 대신 버전으로 막는 이유는, 이 저장소가 완수 판정을 도메인에 두기로
-- 했기 때문이다. 충돌하면 서비스가 다시 읽어 한 번 재시도한다.
--
-- 기존 행에는 0 을 채운다. NOT NULL 로 두려면 기본값이 필요하고, @Version 은 null 을
-- "새 행" 으로 읽으므로 값이 있어야 update 로 동작한다.
ALTER TABLE course.course
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN course.course.version IS '낙관적 락. 동시 세션 완료가 서로를 덮지 않게 한다';
