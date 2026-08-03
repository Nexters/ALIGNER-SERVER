package team.aligner.member.adapter.auth

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import team.aligner.member.contract.MemberAuthContract
import team.aligner.support.web.AuthMemberPort

/**
 * 이 도메인은 @AutoConfiguration 이 4 개다 — service, repository-jdbc, api, 그리고 여기.
 *
 * docs/architecture.md §10 의 5 단계는 세 모듈만 적었지만 adapter-auth 의 Bean 도 등록돼야 한다.
 * 여기가 빠지면 AuthMemberPort Bean 이 없어 기동이 실패하는데, 그 증상은 §9 가 "adapter-auth 가
 * 빠지면 실패해야 정상"이라고 한 것과 똑같아서 원인을 잘못 짚기 쉽다.
 */
@AutoConfiguration
class MemberAuthAdapterAutoConfiguration {
    @Bean
    fun authMemberPort(memberAuthContract: MemberAuthContract): AuthMemberPort = AuthMemberAdapter(memberAuthContract)
}
