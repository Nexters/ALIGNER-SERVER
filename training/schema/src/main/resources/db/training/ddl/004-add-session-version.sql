-- 세션에 낙관적 락을 건다. course.course 에 이미 같은 이유로 걸려 있다 (course 008).
--
-- 세션 완료는 읽기 → 도메인 판단 → 애그리거트 통째 저장이다. Spring Data JDBC 의 애그리거트
-- 저장은 자식을 지우고 다시 넣으므로, 두 완료 요청이 동시에 같은 IN_PROGRESS 세션을 읽으면
-- **나중 저장이 앞선 수행 기록을 덮어쓴다.**
--
-- Session.complete 가 이미 완료된 세션을 그대로 돌려주므로 순차 재시도는 안전하다. 그러나
-- 동시에 들어온 두 요청은 **둘 다 IN_PROGRESS 를 읽어** 그 보호를 지나간다. 버전이 있어야
-- 나중 저장이 실패하고 서비스가 다시 읽어 재시도한다.
--
-- 기존 행에는 0 을 채운다. NOT NULL 로 두려면 기본값이 필요하고, @Version 은 null 을
-- "새 행" 으로 읽으므로 값이 있어야 update 로 동작한다.
ALTER TABLE training.session
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN training.session.version IS '낙관적 락. 동시 세션 완료가 서로의 수행 기록을 덮지 않게 한다';
