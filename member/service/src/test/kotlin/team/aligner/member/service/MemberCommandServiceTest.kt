package team.aligner.member.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import team.aligner.member.infrastructure.MemberRepository
import team.aligner.member.model.ExperienceLevel
import team.aligner.member.model.Member
import team.aligner.member.model.MemberIdentity
import team.aligner.member.model.ReinforcementSetting
import team.aligner.member.model.exception.InvalidHeightException
import team.aligner.member.model.exception.InvalidNicknameException
import team.aligner.member.model.exception.InvalidWeightException
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
                        UpdateMemberProfileCommand(nickname = "가".repeat(Member.NICKNAME_MAX_LENGTH + 1)),
                    )
                }

                verify(exactly = 0) { memberRepository.save(any()) }
            }
        }
        describe("updateProfile 부분 수정") {
            /**
             * 온보딩이 경력 화면, 키·몸무게 화면, 강화 설정 화면으로 나뉘어 있어 한 번에 한
             * 조각씩 PATCH 한다. 앞 화면에서 채운 값이 뒤 화면 요청에 지워지면 안 된다.
             */
            it("보내지 않은 필드는 그대로 둔다") {
                val existing = existingMember(nickname = "강혁", heightCm = 170, weightKg = 60)
                every { memberRepository.findByMemberIdentity(MEMBER_IDENTITY) } returns existing
                val saved = slot<Member>()
                every { memberRepository.save(capture(saved)) } answers { saved.captured }

                memberCommandService.updateProfile(
                    MEMBER_IDENTITY,
                    UpdateMemberProfileCommand(experienceLevel = ExperienceLevel.UNDER_ONE_YEAR),
                )

                saved.captured.experienceLevel shouldBe ExperienceLevel.UNDER_ONE_YEAR
                saved.captured.nickname shouldBe "강혁"
                saved.captured.heightCm shouldBe 170
                saved.captured.weightKg shouldBe 60
            }

            it("강화 부위와 난이도를 함께 저장한다") {
                every { memberRepository.findByMemberIdentity(MEMBER_IDENTITY) } returns existingMember()
                val saved = slot<Member>()
                every { memberRepository.save(capture(saved)) } answers { saved.captured }

                memberCommandService.updateProfile(
                    MEMBER_IDENTITY,
                    UpdateMemberProfileCommand(reinforcement = ReinforcementSetting("BACK", 1)),
                )

                saved.captured.reinforcement shouldBe ReinforcementSetting("BACK", 1)
            }

            it("난이도를 다시 고르면 덮어쓴다") {
                every { memberRepository.findByMemberIdentity(MEMBER_IDENTITY) } returns
                    existingMember(reinforcement = ReinforcementSetting("BACK", 1))
                val saved = slot<Member>()
                every { memberRepository.save(capture(saved)) } answers { saved.captured }

                memberCommandService.updateProfile(
                    MEMBER_IDENTITY,
                    UpdateMemberProfileCommand(reinforcement = ReinforcementSetting("BACK", 3)),
                )

                saved.captured.reinforcement shouldBe ReinforcementSetting("BACK", 3)
            }

            it("키가 범위를 벗어나면 막고 저장하지 않는다") {
                every { memberRepository.findByMemberIdentity(MEMBER_IDENTITY) } returns existingMember()

                shouldThrow<InvalidHeightException> {
                    memberCommandService.updateProfile(MEMBER_IDENTITY, UpdateMemberProfileCommand(heightCm = 99))
                }

                verify(exactly = 0) { memberRepository.save(any()) }
            }

            it("몸무게가 범위를 벗어나면 막고 저장하지 않는다") {
                every { memberRepository.findByMemberIdentity(MEMBER_IDENTITY) } returns existingMember()

                shouldThrow<InvalidWeightException> {
                    memberCommandService.updateProfile(MEMBER_IDENTITY, UpdateMemberProfileCommand(weightKg = 19))
                }

                verify(exactly = 0) { memberRepository.save(any()) }
            }
        }

        describe("withdraw") {
            /**
             * 기록 보존이라 행을 지우지 않는다. 남는 개인정보인 카카오 식별자만 지운다.
             */
            it("행을 지우지 않고 카카오 식별자만 비운 뒤 탈퇴 시각을 남긴다") {
                every { memberRepository.findByMemberIdentity(MEMBER_IDENTITY) } returns existingMember()
                val saved = slot<Member>()
                every { memberRepository.save(capture(saved)) } answers { saved.captured }

                memberCommandService.withdraw(MEMBER_IDENTITY)

                saved.captured.kakaoId shouldBe null
                saved.captured.withdrawnAt.shouldNotBeNull()
                // 식별자와 가입 시각은 남는다 — 기록이 member_id 로 붙어 있다.
                saved.captured.memberIdentity shouldBe MEMBER_IDENTITY
                saved.captured.createdAt shouldBe CREATED_AT
            }

            it("이미 탈퇴했거나 없는 회원이면 404 이고 저장하지 않는다") {
                every { memberRepository.findByMemberIdentity(MEMBER_IDENTITY) } returns null

                shouldThrow<MemberNotFoundException> { memberCommandService.withdraw(MEMBER_IDENTITY) }

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

        fun existingMember(
            nickname: String? = "강혁",
            heightCm: Int? = null,
            weightKg: Int? = null,
            experienceLevel: ExperienceLevel? = null,
            reinforcement: ReinforcementSetting? = null,
        ): Member =
            Member(
                memberIdentity = MEMBER_IDENTITY,
                kakaoId = KAKAO_ID,
                nickname = nickname,
                profileImageUrl = PROFILE_IMAGE_URL,
                heightCm = heightCm,
                weightKg = weightKg,
                experienceLevel = experienceLevel,
                reinforcement = reinforcement,
                withdrawnAt = null,
                createdAt = CREATED_AT,
            )
    }
}
