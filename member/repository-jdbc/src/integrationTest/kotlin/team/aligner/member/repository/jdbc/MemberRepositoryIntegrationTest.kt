package team.aligner.member.repository.jdbc

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.simple.JdbcClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import team.aligner.member.infrastructure.MemberQueryRepository
import team.aligner.member.infrastructure.MemberRepository
import team.aligner.member.model.ExperienceLevel
import team.aligner.member.model.Member
import team.aligner.member.model.MemberIdentity
import team.aligner.member.model.ReinforcementSetting
import team.aligner.member.repository.jdbc.bootstrap.MemberRepositoryTestApplication
import java.time.Instant

/**
 * 러너는 Kotest 가 아니라 JUnit5 다. kotest-extensions-spring 이 버전 카탈로그에 없다.
 * 단언만 kotest-assertions-core 를 쓴다 — 러너와 무관한 순수 단언 라이브러리다.
 *
 * 여기서 처음으로 실제 PostgreSQL 에 대고 확인되는 것들이다.
 * - @Table(schema = "member") 가 실제로 먹었는가 (안 먹으면 public 을 친다)
 * - JdbcClient SQL 이 schema-qualified 인가
 * - changelog 가 돌아 테이블과 유니크 제약이 생겼는가
 */
@Testcontainers
@SpringBootTest(classes = [MemberRepositoryTestApplication::class])
class MemberRepositoryIntegrationTest {
    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var memberQueryRepository: MemberQueryRepository

    @Autowired
    private lateinit var jdbcClient: JdbcClient

    @Test
    fun `회원을 저장하면 식별자가 채워진다`() {
        val saved = memberRepository.save(newMember(kakaoId = "1001"))

        saved.memberIdentity.shouldNotBeNull()
        saved.createdAt.shouldNotBeNull()
    }

    @Test
    fun `테이블은 member 스키마에 만들어진다`() {
        memberRepository.save(newMember(kakaoId = "1002"))

        jdbcClient
            .sql("SELECT count(*) FROM member.member WHERE kakao_id = '1002'")
            .query(Int::class.java)
            .single() shouldBe 1

        // public 에 같은 이름의 테이블이 생기지 않았어야 한다.
        jdbcClient
            .sql("SELECT to_regclass('public.member')")
            .query(String::class.java)
            .optional()
            .orElse(null)
            .shouldBeNull()
    }

    @Test
    fun `같은 카카오 계정은 두 번 저장되지 않는다`() {
        memberRepository.save(newMember(kakaoId = "1003"))

        assertThrows<DataIntegrityViolationException> {
            memberRepository.save(newMember(kakaoId = "1003"))
        }
    }

    @Test
    fun `카카오 식별자로 조회한다`() {
        memberRepository.save(newMember(kakaoId = "1004"))

        memberRepository.findByKakaoId("1004").shouldNotBeNull()
        memberRepository.findByKakaoId("존재하지-않는-카카오-아이디").shouldBeNull()
    }

    @Test
    fun `저장한 값이 그대로 되읽힌다`() {
        val saved =
            memberRepository.save(
                Member.register(kakaoId = "1005", nickname = null, profileImageUrl = null),
            )
        val identity = saved.memberIdentity.shouldNotBeNull()

        val found = memberRepository.findByMemberIdentity(identity).shouldNotBeNull()

        found.kakaoId shouldBe "1005"
        // 닉네임을 비워둔 채로 가입한 회원이 그대로 돌아와야 한다.
        found.nickname.shouldBeNull()
        found.profileImageUrl.shouldBeNull()
        // save() 가 돌려준 시각은 DB 에 실제로 들어간 값이어야 한다. TIMESTAMPTZ 가
        // 마이크로초로 자르므로 나노초를 그대로 넣으면 여기서 갈린다 (Linux 에서만 재현된다).
        found.createdAt shouldBe saved.createdAt
    }

    @Test
    fun `수정해도 가입 시각은 유지된다`() {
        val saved = memberRepository.save(newMember(kakaoId = "1006"))
        val identity = saved.memberIdentity.shouldNotBeNull()
        val createdAt = saved.createdAt.shouldNotBeNull()

        memberRepository.save(saved.changeProfile("바뀐닉네임"))

        val found = memberRepository.findByMemberIdentity(identity).shouldNotBeNull()
        found.nickname shouldBe "바뀐닉네임"
        found.createdAt shouldBe createdAt
    }

    @Test
    fun `프로필 조회 SQL 이 member 스키마를 친다`() {
        val saved = memberRepository.save(newMember(kakaoId = "1007", nickname = "강혁"))
        val identity = saved.memberIdentity.shouldNotBeNull()

        val profile = memberQueryRepository.findProfile(identity).shouldNotBeNull()

        profile.memberId shouldBe identity.value
        profile.nickname shouldBe "강혁"
        memberQueryRepository.findProfile(MemberIdentity.of(-1L)).shouldBeNull()
    }

    @Test
    fun `changelog 가 적용됐다`() {
        jdbcClient
            .sql("SELECT count(*) FROM public.databasechangelog WHERE id IN (:ids)")
            .param(
                "ids",
                listOf(
                    "member-0001-create-schema",
                    "member-0002-create-member",
                    "member-0003-add-onboarding-and-withdrawal",
                ),
            ).query(Int::class.java)
            .single() shouldBe 3
    }

    @Test
    fun `온보딩 입력값이 그대로 되읽힌다`() {
        val saved = memberRepository.save(newMember(kakaoId = "1008"))
        val identity = saved.memberIdentity.shouldNotBeNull()

        memberRepository.save(
            saved.changeProfile(
                heightCm = 170,
                weightKg = 60,
                experienceLevel = ExperienceLevel.ONE_TO_THREE_YEARS,
                reinforcement = ReinforcementSetting("BACK", 1),
            ),
        )

        val found = memberRepository.findByMemberIdentity(identity).shouldNotBeNull()
        found.heightCm shouldBe 170
        found.weightKg shouldBe 60
        // VARCHAR 로 저장한 뒤 enum 으로 되돌아와야 한다.
        found.experienceLevel shouldBe ExperienceLevel.ONE_TO_THREE_YEARS
        found.reinforcement shouldBe ReinforcementSetting("BACK", 1)
    }

    /**
     * SMALLINT 컬럼이 NULL 일 때 getInt 가 0 을 돌려주면 "입력 안 함" 과 0 이 같아진다.
     * 프론트가 이 null 로 온보딩 완료 여부를 판단하므로 조회 모델에서도 구분돼야 한다.
     */
    @Test
    fun `온보딩 전이면 조회 모델의 신체 정보가 0 이 아니라 null 이다`() {
        val saved = memberRepository.save(newMember(kakaoId = "1009"))
        val identity = saved.memberIdentity.shouldNotBeNull()

        val profile = memberQueryRepository.findProfile(identity).shouldNotBeNull()

        profile.heightCm.shouldBeNull()
        profile.weightKg.shouldBeNull()
        profile.experienceLevel.shouldBeNull()
        profile.reinforcementBodyPartCode.shouldBeNull()
        profile.reinforcementLevel.shouldBeNull()
    }

    @Test
    fun `범위를 벗어난 신체 정보는 DB 가 막는다`() {
        val saved = memberRepository.save(newMember(kakaoId = "1010"))

        // 애그리거트를 우회해도 CHECK 이 남는다. copy 로 검증을 건너뛰고 직접 넣는다.
        assertThrows<DataIntegrityViolationException> {
            memberRepository.save(saved.copy(heightCm = 300))
        }
    }

    /**
     * 부위와 난이도는 한 화면에서 같이 고른다. 한쪽만 있는 상태를 ck_member_reinforcement_pair 가 막는다.
     */
    @Test
    fun `강화 부위만 있고 난이도가 없으면 DB 가 막는다`() {
        val saved = memberRepository.save(newMember(kakaoId = "1011"))

        assertThrows<DataIntegrityViolationException> {
            jdbcClient
                .sql(
                    """
                    UPDATE member.member SET reinforcement_body_part_code = 'BACK'
                    WHERE member_id = :memberId
                    """.trimIndent(),
                ).param("memberId", saved.memberIdentity.shouldNotBeNull().value)
                .update()
        }
    }

    @Test
    fun `탈퇴하면 행은 남고 카카오 식별자만 비워진다`() {
        val saved = memberRepository.save(newMember(kakaoId = "1012"))
        val identity = saved.memberIdentity.shouldNotBeNull()

        memberRepository.save(saved.withdraw(at = Instant.parse("2026-08-08T00:00:00Z")))

        // 행은 그대로다 — 운동 기록이 member_id 로 붙어 있어 지우지 않는다.
        jdbcClient
            .sql("SELECT count(*) FROM member.member WHERE member_id = :memberId")
            .param("memberId", identity.value)
            .query(Int::class.java)
            .single() shouldBe 1

        // 그러나 모든 조회에서는 사라진다.
        memberRepository.findByMemberIdentity(identity).shouldBeNull()
        memberRepository.findByKakaoId("1012").shouldBeNull()
        memberQueryRepository.findProfile(identity).shouldBeNull()
    }

    /**
     * kakao_id 가 NULL 이 되면 UNIQUE 자리가 풀린다. 같은 계정이 다시 가입할 수 있어야 하고,
     * 그때는 새 member_id 를 받아 이전 기록이 이어지지 않아야 한다.
     */
    @Test
    fun `탈퇴한 계정으로 다시 가입하면 새 회원이 된다`() {
        val first = memberRepository.save(newMember(kakaoId = "1013"))
        val firstIdentity = first.memberIdentity.shouldNotBeNull()
        memberRepository.save(first.withdraw(at = Instant.parse("2026-08-08T00:00:00Z")))

        val second = memberRepository.save(newMember(kakaoId = "1013"))

        second.memberIdentity.shouldNotBeNull() shouldNotBe firstIdentity
    }

    /**
     * 탈퇴 회원이 둘 이상이어도 kakao_id NULL 끼리는 UNIQUE 에 걸리지 않아야 한다.
     * PostgreSQL 이 NULL 을 서로 다른 값으로 보는 것에 기대는 설계라 실제로 확인한다.
     */
    @Test
    fun `탈퇴 회원이 여럿이어도 유니크 제약에 걸리지 않는다`() {
        val at = Instant.parse("2026-08-08T00:00:00Z")
        val one = memberRepository.save(newMember(kakaoId = "1014"))
        val two = memberRepository.save(newMember(kakaoId = "1015"))

        memberRepository.save(one.withdraw(at = at))
        memberRepository.save(two.withdraw(at = at))

        // 이 클래스는 테스트 사이에 테이블을 비우지 않는다. 다른 테스트가 남긴 탈퇴 회원이
        // 같이 세어지지 않도록 방금 만든 두 행으로 좁힌다.
        val identities = listOf(one, two).map { it.memberIdentity.shouldNotBeNull().value }
        jdbcClient
            .sql("SELECT count(*) FROM member.member WHERE kakao_id IS NULL AND member_id IN (:ids)")
            .param("ids", identities)
            .query(Int::class.java)
            .single() shouldBe 2
    }

    private fun newMember(
        kakaoId: String,
        nickname: String? = "강혁",
    ): Member =
        Member.register(
            kakaoId = kakaoId,
            nickname = nickname,
            profileImageUrl = "https://k.kakaocdn.net/profile.jpg",
        )

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17-alpine")
    }
}
