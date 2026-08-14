package team.aligner.training.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import team.aligner.training.infrastructure.CourseProgressPort
import team.aligner.training.infrastructure.CourseStepPort
import team.aligner.training.infrastructure.ExerciseDetailPort
import team.aligner.training.infrastructure.SessionAchievementQueryRepository
import team.aligner.training.infrastructure.SessionRepository
import java.time.LocalDate
import java.time.ZoneId

/**
 * 연속 달성 계산을 고정한다. **날짜 경계 규칙이 화면 문구를 그대로 결정**하는 자리라
 * 회귀가 눈에 띄지 않는다 — "5일 연속" 이 하루 만에 0 이 되어도 서버는 200 을 낸다.
 *
 * 오늘 날짜에 의존하므로 `Asia/Seoul` 의 오늘을 기준으로 상대 날짜를 만든다. 고정 날짜를
 * 박으면 그 주가 지나는 순간 테스트가 깨진다.
 */
class SessionAchievementTest :
    DescribeSpec({
        val zone = ZoneId.of("Asia/Seoul")
        val achievementQueryRepository = mockk<SessionAchievementQueryRepository>()

        fun service() =
            SessionServiceImpl(
                sessionRepository = mockk<SessionRepository>(),
                sessionAchievementQueryRepository = achievementQueryRepository,
                courseStepPort = mockk<CourseStepPort>(),
                courseProgressPort = mockk<CourseProgressPort>(),
                exerciseDetailPort = mockk<ExerciseDetailPort>(),
            )

        fun givenAchieved(dates: List<LocalDate>) {
            every { achievementQueryRepository.findCompletedDates(any(), any()) } returns dates
        }

        describe("getAchievement") {
            it("오늘부터 이어진 날을 센다") {
                val today = LocalDate.now(zone)
                givenAchieved(listOf(today, today.minusDays(1), today.minusDays(2)))

                service().getAchievement(1L).currentStreakDays shouldBe 3
            }

            /**
             * **오늘 아직 안 했다고 0 이 되면 안 된다.** 하루가 지나기도 전에 연속이 끊긴 것처럼
             * 보이면 화면이 회원을 잘못 다그친다.
             */
            it("오늘 아직 안 했어도 어제까지 이어져 있으면 끊기지 않는다") {
                val today = LocalDate.now(zone)
                givenAchieved(listOf(today.minusDays(1), today.minusDays(2)))

                service().getAchievement(1L).currentStreakDays shouldBe 2
            }

            it("어제도 없으면 0 이다") {
                val today = LocalDate.now(zone)
                givenAchieved(listOf(today.minusDays(2), today.minusDays(3)))

                service().getAchievement(1L).currentStreakDays shouldBe 0
            }

            it("중간에 빈 날이 있으면 거기서 끊는다") {
                val today = LocalDate.now(zone)
                givenAchieved(listOf(today, today.minusDays(1), today.minusDays(3)))

                service().getAchievement(1L).currentStreakDays shouldBe 2
            }

            it("완료한 세션이 하나도 없으면 0 일이고 이번 주가 전부 false 다") {
                givenAchieved(emptyList())

                val achievement = service().getAchievement(1L)

                achievement.currentStreakDays shouldBe 0
                achievement.weeklyAchievedCount shouldBe 0
                achievement.days.size shouldBe 7
                achievement.days.all { !it.achieved } shouldBe true
            }

            it("이번 주는 월요일부터 일요일까지 7 개이고 오늘 이후 날짜도 실린다") {
                val today = LocalDate.now(zone)
                val monday = today.with(java.time.DayOfWeek.MONDAY)
                givenAchieved(listOf(monday))

                val achievement = service().getAchievement(1L)

                achievement.days.first().date shouldBe monday
                achievement.days.last().date shouldBe monday.plusDays(6)
                achievement.weeklyAchievedCount shouldBe 1
                achievement.days.single { it.achieved }.date shouldBe monday
            }

            /**
             * 지난 주 날짜는 연속에는 들어가지만 **이번 주 카운트에는 들어가지 않는다.**
             * 화면이 "이번 주 5 / 7" 을 그대로 보여준다.
             */
            it("지난 주 달성은 이번 주 카운트에 들어가지 않는다") {
                val today = LocalDate.now(zone)
                val lastMonday = today.with(java.time.DayOfWeek.MONDAY).minusWeeks(1)
                givenAchieved(listOf(lastMonday))

                service().getAchievement(1L).weeklyAchievedCount shouldBe 0
            }
        }
    })
