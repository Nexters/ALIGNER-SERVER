package team.aligner.member.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import team.aligner.member.infrastructure.MemberRepository
import team.aligner.member.model.Member
import team.aligner.member.model.MemberIdentity
import team.aligner.member.model.exception.InvalidNicknameException
import team.aligner.member.model.exception.MemberNotFoundException
import java.time.Instant

class MemberCommandServiceTest :
    DescribeSpec({
        val memberRepository = mockk<MemberRepository>()
        val memberCommandService: MemberCommandService = MemberCommandServiceImpl(memberRepository)

        // Kotest 기본 격리 모드는 SingleInstance 라 모든 it 이 같은 mock 을 공유한다.
        // 비우지 않으면 verify(exactly = 0) 이 앞 테스트의 호출까지 세어 실패한다.
        beforeTest { clearMocks(memberRepository) }

        describe("findOrRegisterByKakao") {
            it("처음 보는 카카오 계정이면 가입시킨다") {
                every { memberRepository.findByKakaoId(KAKAO_ID) } returns null
                val saved = slot<Member>()
                every { memberRepository.save(capture(saved)) } answers {
                    saved.captured.copy(memberIdentity = MemberIdentity.of(MEMBER_ID))
                }

                val member =
                    memberCommandService.findOrRegisterByKakao(
                        RegisterKakaoMemberCommand(KAKAO_ID, "강혁", PROFILE_IMAGE_URL),
                    )

                member.memberIdentity shouldBe MemberIdentity.of(MEMBER_ID)
                member.kakaoId shouldBe KAKAO_ID
                saved.captured.memberIdentity shouldBe null
            }

            it("이미 가입한 계정이면 저장하지 않는다") {
                val existing = existingMember(nickname = "내가바꾼닉네임")
                every { memberRepository.findByKakaoId(KAKAO_ID) } returns existing

                val member =
                    memberCommandService.findOrRegisterByKakao(
                        // 카카오가 주는 닉네임이 우리가 저장한 것과 달라도 덮어쓰지 않는다.
                        RegisterKakaoMemberCommand(KAKAO_ID, "카카오닉네임", PROFILE_IMAGE_URL),
                    )

                member shouldBe existing
                member.nickname shouldBe "내가바꾼닉네임"
                // 재로그인마다 저장하면 회원이 바꾼 닉네임이 되돌아간다.
                verify(exactly = 0) { memberRepository.save(any()) }
            }

            it("카카오 닉네임이 없으면 비워둔 채로 가입시킨다") {
                every { memberRepository.findByKakaoId(KAKAO_ID) } returns null
                val saved = slot<Member>()
                every { memberRepository.save(capture(saved)) } answers {
                    saved.captured.copy(memberIdentity = MemberIdentity.of(MEMBER_ID))
                }

                val member =
                    memberCommandService.findOrRegisterByKakao(
                        RegisterKakaoMemberCommand(KAKAO_ID, null, null),
                    )

                // 기본 닉네임을 만들어 넣지 않는다.
                member.nickname shouldBe null
                member.profileImageUrl shouldBe null
            }

            it("카카오 닉네임이 너무 길면 잘라서 가입시킨다") {
                val tooLong = "가".repeat(Member.NICKNAME_MAX_LENGTH + 10)
                every { memberRepository.findByKakaoId(KAKAO_ID) } returns null
                val saved = slot<Member>()
                every { memberRepository.save(capture(saved)) } answers {
                    saved.captured.copy(memberIdentity = MemberIdentity.of(MEMBER_ID))
                }

                val member =
                    memberCommandService.findOrRegisterByKakao(
                        RegisterKakaoMemberCommand(KAKAO_ID, tooLong, null),
                    )

                // 가입은 막지 않되 nickname VARCHAR(50) 을 넘기지 않는다. 안 자르면 DB 가 거부해 500 이다.
                member.nickname shouldBe "가".repeat(Member.NICKNAME_MAX_LENGTH)
            }
        }

        describe("updateProfile") {
            it("닉네임을 바꾸고 저장한다") {
                every { memberRepository.findByMemberIdentity(MEMBER_IDENTITY) } returns existingMember()
                val saved = slot<Member>()
                every { memberRepository.save(capture(saved)) } answers { saved.captured }

                val member =
                    memberCommandService.updateProfile(
                        MEMBER_IDENTITY,
                        UpdateMemberProfileCommand(nickname = "새닉네임"),
                    )

                member.nickname shouldBe "새닉네임"
                saved.captured.nickname shouldBe "새닉네임"
                // 가입 시각을 지금으로 덮어쓰지 않는다. shouldNotBeNull 로는 못 잡는다.
                saved.captured.createdAt shouldBe CREATED_AT
            }

            it("앞뒤 공백을 떼고 저장한다") {
                every { memberRepository.findByMemberIdentity(MEMBER_IDENTITY) } returns existingMember()
                val saved = slot<Member>()
                every { memberRepository.save(capture(saved)) } answers { saved.captured }

                memberCommandService
                    .updateProfile(MEMBER_IDENTITY, UpdateMemberProfileCommand("  강혁  "))
                    .nickname shouldBe "강혁"
            }

            it("없는 회원이면 MemberNotFoundException 이다") {
                every { memberRepository.findByMemberIdentity(MEMBER_IDENTITY) } returns null

                shouldThrow<MemberNotFoundException> {
                    memberCommandService.updateProfile(MEMBER_IDENTITY, UpdateMemberProfileCommand("강혁"))
                }

                verify(exactly = 0) { memberRepository.save(any()) }
            }

            it("공백만 있는 닉네임이면 InvalidNicknameException 이다") {
                every { memberRepository.findByMemberIdentity(MEMBER_IDENTITY) } returns existingMember()

                shouldThrow<InvalidNicknameException> {
                    memberCommandService.updateProfile(MEMBER_IDENTITY, UpdateMemberProfileCommand("   "))
                }

                verify(exactly = 0) { memberRepository.save(any()) }
            }

            it("50자를 넘는 닉네임이면 InvalidNicknameException 이다") {
                every { memberRepository.findByMemberIdentity(MEMBER_IDENTITY) } returns existingMember()

                shouldThrow<InvalidNicknameException> {
                    memberCommandService.updateProfile(
                        MEMBER_IDENTITY,
                        UpdateMemberProfileCommand("가".repeat(Member.NICKNAME_MAX_LENGTH + 1)),
                    )
                }

                verify(exactly = 0) { memberRepository.save(any()) }
            }
        }
    }) {
    private companion object {
        const val KAKAO_ID = "1234567890"
        const val MEMBER_ID = 1L
        const val PROFILE_IMAGE_URL = "https://k.kakaocdn.net/profile.jpg"
        val MEMBER_IDENTITY = MemberIdentity.of(MEMBER_ID)
        val CREATED_AT: Instant = Instant.parse("2026-07-01T00:00:00Z")

        fun existingMember(nickname: String? = "강혁"): Member =
            Member(
                memberIdentity = MEMBER_IDENTITY,
                kakaoId = KAKAO_ID,
                nickname = nickname,
                profileImageUrl = PROFILE_IMAGE_URL,
                createdAt = CREATED_AT,
            )
    }
}
