package team.aligner.catalog.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import team.aligner.catalog.infrastructure.ExerciseQueryRepository
import team.aligner.catalog.infrastructure.PoseVideoPlayback
import team.aligner.catalog.infrastructure.PoseVideoPort
import team.aligner.catalog.model.ExerciseIdentity
import team.aligner.catalog.model.MuscleRole
import team.aligner.catalog.model.exception.ExerciseNotFoundException
import team.aligner.catalog.model.view.ExerciseDetailView
import team.aligner.catalog.model.view.ExerciseSummaryView
import team.aligner.catalog.model.view.ExerciseVoiceCueView
import team.aligner.catalog.model.view.MuscleView
import java.math.BigDecimal

class ExerciseQueryServiceTest :
    DescribeSpec({
        val exerciseQueryRepository = mockk<ExerciseQueryRepository>()
        val poseVideoPort = mockk<PoseVideoPort>()
        val exerciseQueryService: ExerciseQueryService = ExerciseQueryServiceImpl(exerciseQueryRepository, poseVideoPort)

        val exerciseIdentity = ExerciseIdentity.of(1L)

        // slug 가 없는 상태가 기본이다. seed 가 ymove_slug 를 아직 안 채웠다.
        beforeTest {
            clearMocks(exerciseQueryRepository, poseVideoPort)
            every { exerciseQueryRepository.findYmoveSlugs(any()) } returns emptyMap()
        }

        describe("getDetail") {
            it("근육과 음성 큐를 함께 돌려준다") {
                every { exerciseQueryRepository.findDetail(exerciseIdentity) } returns
                    ExerciseDetailView(
                        exerciseId = 1L,
                        name = "낙타자세",
                        imageAssetKey = "exercise/camel",
                        videoUrl = null,
                        thumbnailUrl = "https://exercise-api.ymove.app/api/v2/thumbnail/x?library=clean",
                        defaultSetCount = 3,
                        defaultRepCount = null,
                        defaultDurationSeconds = 120,
                        metValue = BigDecimal("3.00"),
                        difficulty = "하",
                        category = "핀포즈",
                        cautionNote = "목을 뒤로 완전히 젖히지 마세요",
                        muscles =
                            listOf(
                                MuscleView("ERECTOR_SPINAE", "척추기립근", "BACK", null, "erector-spinae-back", MuscleRole.STRENGTHEN, 1),
                                MuscleView("ILIOPSOAS", "장요근", "PELVIS", "iliopsoas-front", null, MuscleRole.STRETCH, 2),
                            ),
                        voiceCues =
                            listOf(
                                ExerciseVoiceCueView(1, null, null, "무릎을 골반 너비로 벌리세요"),
                                ExerciseVoiceCueView(2, 35, 75, "명치를 천장으로 끌어올리고 40 초 유지하세요"),
                            ),
                    )

                val detail = exerciseQueryService.getDetail(exerciseIdentity)

                detail.name shouldBe "낙타자세"
                detail.muscles.map { it.role } shouldBe listOf(MuscleRole.STRENGTHEN, MuscleRole.STRETCH)
                detail.category shouldBe "핀포즈"
                // 척추기립근은 뒤에만, 장요근은 앞에만 보인다. 세션 플레이어가 앞·뒤 그림을
                // 따로 칠하므로 반대쪽은 null 로 남아야 한다.
                detail.muscles.map { it.frontHighlightAssetKey } shouldBe listOf(null, "iliopsoas-front")
                detail.muscles.map { it.backHighlightAssetKey } shouldBe listOf("erector-spinae-back", null)
                detail.voiceCues.map { it.startOffsetSeconds } shouldBe listOf(null, 35)
                detail.voiceCues.map { it.endOffsetSeconds } shouldBe listOf(null, 75)
            }

            it("없는 운동이면 ExerciseNotFoundException 이다") {
                every { exerciseQueryRepository.findDetail(exerciseIdentity) } returns null

                shouldThrow<ExerciseNotFoundException> {
                    exerciseQueryService.getDetail(exerciseIdentity)
                }
            }

            it("ymove_slug 가 있으면 videoUrl 을 YMove 에서 채운다") {
                every { exerciseQueryRepository.findDetail(exerciseIdentity) } returns detailView()
                every { exerciseQueryRepository.findYmoveSlugs(listOf(exerciseIdentity)) } returns mapOf(1L to "camel-pose")
                every { poseVideoPort.findPlayback(listOf("camel-pose")) } returns
                    mapOf("camel-pose" to PoseVideoPlayback("https://cdn/camel.mp4"))

                exerciseQueryService.getDetail(exerciseIdentity).videoUrl shouldBe "https://cdn/camel.mp4"
            }

            /**
             * seed 가 ymove_slug 를 채우기 전 상태다. 연동이 붙어도 이 경로가 지금과 같아야 한다.
             */
            it("ymove_slug 가 없으면 YMove 를 치지 않고 videoUrl 은 null 이다") {
                every { exerciseQueryRepository.findDetail(exerciseIdentity) } returns detailView()

                exerciseQueryService.getDetail(exerciseIdentity).videoUrl shouldBe null

                verify(exactly = 0) { poseVideoPort.findPlayback(any()) }
            }

            /**
             * YMove 장애를 502 로 올리지 않는다. 근육·음성 큐·주의사항은 우리 DB 라 살아 있고,
             * videoUrl 이 null 인 화면은 이미 프론트 계약이다 (docs/domains.md §4-3-1).
             */
            it("YMove 가 못 주면 videoUrl 만 null 이고 나머지는 살아 있다") {
                every { exerciseQueryRepository.findDetail(exerciseIdentity) } returns detailView()
                every { exerciseQueryRepository.findYmoveSlugs(listOf(exerciseIdentity)) } returns mapOf(1L to "camel-pose")
                every { poseVideoPort.findPlayback(listOf("camel-pose")) } returns emptyMap()

                val detail = exerciseQueryService.getDetail(exerciseIdentity)

                detail.videoUrl shouldBe null
                // 썸네일은 DB 값이라 YMove 가 죽어도 살아 있다. videoUrl 과 다른 점이다.
                detail.thumbnailUrl shouldBe "https://exercise-api.ymove.app/api/v2/thumbnail/x?library=clean"
                detail.voiceCues.size shouldBe 2
                detail.muscles.size shouldBe 2
                detail.cautionNote shouldBe "목을 뒤로 완전히 젖히지 마세요"
            }
        }

        describe("getAll") {
            it("찾은 것만 돌려준다") {
                val identities = listOf(ExerciseIdentity.of(1L), ExerciseIdentity.of(99L))
                every { exerciseQueryRepository.findAllByIdentities(identities) } returns
                    listOf(
                        ExerciseSummaryView(
                            exerciseId = 1L,
                            name = "낙타자세",
                            imageAssetKey = "exercise/camel",
                            thumbnailUrl = "https://ymove.test/camel.jpg",
                            defaultSetCount = 3,
                            defaultRepCount = null,
                            defaultDurationSeconds = 120,
                            metValue = BigDecimal("3.00"),
                            difficulty = "하",
                            category = "핀포즈",
                        ),
                    )

                val summaries = exerciseQueryService.getAll(identities)

                summaries.map { it.exerciseId } shouldBe listOf(1L)
            }

            /**
             * IN (:ids) 에 빈 리스트를 넘기면 SQL 이 깨진다. member 에 없던 위험이라 회귀를 막는다.
             */
            it("빈 목록이면 DB 를 치지 않는다") {
                exerciseQueryService.getAll(emptyList()) shouldBe emptyList()

                verify(exactly = 0) { exerciseQueryRepository.findAllByIdentities(any()) }
            }
        }
    })

/**
 * 재생 URL 을 뺀 운동 상세. videoUrl 은 adapter-ymove 가 채우므로 여기서는 항상 null 이다
 * (ExerciseQueryRepositoryImpl 이 video_url 컬럼을 더 이상 읽지 않는다).
 */
private fun detailView() =
    ExerciseDetailView(
        exerciseId = 1L,
        name = "낙타자세",
        imageAssetKey = "exercise/camel",
        videoUrl = null,
        thumbnailUrl = "https://exercise-api.ymove.app/api/v2/thumbnail/x?library=clean",
        defaultSetCount = 3,
        defaultRepCount = null,
        defaultDurationSeconds = 120,
        metValue = BigDecimal("3.00"),
        difficulty = "하",
        category = "핀포즈",
        cautionNote = "목을 뒤로 완전히 젖히지 마세요",
        muscles =
            listOf(
                MuscleView("ERECTOR_SPINAE", "척추기립근", "BACK", null, "erector-spinae-back", MuscleRole.STRENGTHEN, 1),
                MuscleView("ILIOPSOAS", "장요근", "PELVIS", "iliopsoas-front", null, MuscleRole.STRETCH, 2),
            ),
        voiceCues =
            listOf(
                ExerciseVoiceCueView(1, null, null, "무릎을 골반 너비로 벌리세요"),
                ExerciseVoiceCueView(2, 35, 75, "명치를 천장으로 끌어올리고 40 초 유지하세요"),
            ),
    )
