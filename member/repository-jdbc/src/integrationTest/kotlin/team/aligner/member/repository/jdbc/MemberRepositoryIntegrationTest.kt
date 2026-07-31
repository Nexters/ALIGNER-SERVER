package team.aligner.member.repository.jdbc

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
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
import team.aligner.member.model.Member
import team.aligner.member.model.MemberIdentity
import team.aligner.member.repository.jdbc.bootstrap.MemberRepositoryTestApplication

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
            .param("ids", listOf("member-0001-create-schema", "member-0002-create-member"))
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
