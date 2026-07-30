package team.aligner.api

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.runApplication

/**
 * 단일 실행·배포 단위.
 *
 * **@SpringBootApplication 을 쓰지 않는다.** 그 어노테이션은 ComponentScan 을 포함하므로
 * 패키지 위치에 따라 @Component·@RestController 가 우연히 잡힌다. 이 프로젝트는 조립을
 * 클래스패스 우연이 아니라 Gradle 의존성 선언이 결정하게 한다 (docs/architecture.md §5).
 */
@SpringBootConfiguration
@EnableAutoConfiguration
class AlignerApplication

fun main(args: Array<String>) {
    runApplication<AlignerApplication>(*args)
}
