package team.aligner.course.adapter.member

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import team.aligner.course.infrastructure.MemberBodyPort
import team.aligner.member.contract.MemberBodyContract

/**
 * `course → member` 는 초판 의존 지도에 없던 방향이고 뒤늦게 허용됐다 (docs/domains.md §3).
 */
@AutoConfiguration
class CourseMemberAdapterAutoConfiguration {
    @Bean(name = ["memberBodyPort"])
    internal fun memberBodyPort(memberBodyContract: MemberBodyContract): MemberBodyPort = MemberBodyAdapter(memberBodyContract)
}
