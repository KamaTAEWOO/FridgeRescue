package com.portfolio.fridgerescue.feature.rescue

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.portfolio.fridgerescue.core.testing.InMemoryFoodRepository
import com.portfolio.fridgerescue.core.testing.InMemoryIntakeDraftRepository
import com.portfolio.fridgerescue.core.model.FoodDate
import com.portfolio.fridgerescue.core.model.FoodDateSource
import com.portfolio.fridgerescue.core.model.FoodItem
import com.portfolio.fridgerescue.core.model.FoodItemId
import com.portfolio.fridgerescue.core.model.StorageLocation
import com.portfolio.fridgerescue.core.model.IntakeContentType
import com.portfolio.fridgerescue.core.model.IntakeCandidate
import com.portfolio.fridgerescue.core.model.IntakeCandidateGroup
import com.portfolio.fridgerescue.core.model.IntakeDraft
import com.portfolio.fridgerescue.core.model.IntakeDraftStatus
import com.portfolio.fridgerescue.core.model.IntakeErrorCode
import com.portfolio.fridgerescue.feature.rescue.domain.GetRescueQueueUseCase
import com.portfolio.fridgerescue.feature.rescue.domain.RescueUrgency
import com.portfolio.fridgerescue.feature.rescue.presentation.RescueAction
import com.portfolio.fridgerescue.feature.rescue.presentation.FoodEditorTestTags
import com.portfolio.fridgerescue.feature.rescue.presentation.RescueRoute
import com.portfolio.fridgerescue.feature.rescue.presentation.RescueScreen
import com.portfolio.fridgerescue.feature.rescue.presentation.RescueUiState
import com.portfolio.fridgerescue.feature.rescue.presentation.IntakeReviewUiState
import com.portfolio.fridgerescue.feature.rescue.presentation.RescueViewModel
import com.portfolio.fridgerescue.ui.theme.FridgeRescueTheme
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RescueScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 9, 1)

    @Test
    fun TC_UI_005_displayed_and_estimated_dates_use_distinct_labels() {
        val state = contentState(
            foodItem(
                id = "displayed",
                name = "두부",
                date = FoodDate(today.plusDays(1), FoodDateSource.MANUFACTURER_DISPLAYED),
            ),
            foodItem(
                id = "estimated",
                name = "우유",
                date = FoodDate(today.plusDays(3), FoodDateSource.APP_ESTIMATED),
            ),
        )

        setScreen(state)

        composeRule.onNodeWithText("표시기한 · 9월 2일").assertIsDisplayed()
        composeRule.onNodeWithText("앱 예상 소비일 · 9월 4일").assertIsDisplayed()
    }

    @Test
    fun TC_UI_006_consumed_action_can_be_undone() {
        val foodItem = foodItem(
            id = "spinach",
            name = "시금치",
            date = FoodDate(today, FoodDateSource.APP_ESTIMATED),
        )
        val viewModel = RescueViewModel(
            clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC),
            repository = InMemoryFoodRepository(listOf(foodItem)),
        )

        composeRule.setContent {
            FridgeRescueTheme {
                RescueRoute(viewModel = viewModel)
            }
        }
        composeRule.onNodeWithText("먹었어요").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("시금치").fetchSemanticsNodes().isEmpty()
        }

        composeRule.onNodeWithText("실행 취소").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("시금치").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("시금치").assertIsDisplayed()
    }

    @Test
    fun TC_ACTION_010_still_here_is_visible_in_persistent_history() {
        val item = foodItem(
            id = "milk",
            name = "우유",
            date = FoodDate(today.plusDays(1), FoodDateSource.APP_ESTIMATED),
        )
        val viewModel = RescueViewModel(
            repository = InMemoryFoodRepository(listOf(item)),
            clock = fixedClock(),
        )
        setRoute(viewModel)

        composeRule.onNodeWithText("상태 기록").performClick()
        composeRule.onNodeWithText("아직 있어요").performClick()
        composeRule.onNodeWithText("실행 취소").assertIsDisplayed()

        composeRule.onNodeWithText("상태 기록").performClick()
        composeRule.onNodeWithText("아직 있다고 확인했어요").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun TC_UI_009_empty_queue_shows_next_step_guidance() {
        setScreen(contentState())

        composeRule.onNodeWithText("지금 구조할 재료가 없어요").assertIsDisplayed()
        composeRule.onNodeWithText("재료 추가를 눌러 첫 식재료를 담아보세요.")
            .assertIsDisplayed()
    }

    @Test
    fun TC_INTAKE_001_shared_text_draft_is_shown_for_review() {
        val draft = IntakeDraft(
            id = "shared-text",
            contentType = IntakeContentType.TEXT,
            mimeType = "text/plain",
            textContent = "두부 2개\n시금치 1봉",
            cachedFilePath = null,
            status = IntakeDraftStatus.READY,
            errorCode = null,
            createdAt = Instant.parse("2026-09-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-09-01T00:00:00Z"),
        )

        setScreen(contentState().copy(intakeReview = IntakeReviewUiState(draft, emptyList())))

        composeRule.onNodeWithText("구매내역을 받았어요").assertIsDisplayed()
        composeRule.onNodeWithText("두부 2개\n시금치 1봉").assertIsDisplayed()
    }

    @Test
    fun TC_UI_001_candidate_groups_and_default_selection_are_visible() {
        val draft = IntakeDraft(
            id = "grouped",
            contentType = IntakeContentType.TEXT,
            mimeType = "text/plain",
            textContent = "두부\n새 품목\n물티슈",
            cachedFilePath = null,
            status = IntakeDraftStatus.READY,
            errorCode = null,
            createdAt = Instant.parse("2026-09-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-09-01T00:00:00Z"),
        )
        val candidates = listOf(
            intakeCandidate("두부", IntakeCandidateGroup.MANAGE, true, 0),
            intakeCandidate("새 품목", IntakeCandidateGroup.REVIEW, false, 1),
            intakeCandidate("물티슈", IntakeCandidateGroup.EXCLUDED, false, 2),
        )

        setScreen(contentState().copy(intakeReview = IntakeReviewUiState(draft, candidates)))

        composeRule.onNodeWithText("관리 후보 1개").assertIsDisplayed()
        composeRule.onNodeWithText("확인 필요 1개").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("기본 제외 1개").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("선택한 1개 담기").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun AT_001_selected_shared_items_are_saved_to_rescue_queue() {
        val draft = IntakeDraft(
            id = "grouped",
            contentType = IntakeContentType.TEXT,
            mimeType = "text/plain",
            textContent = "두부\n시금치",
            cachedFilePath = null,
            status = IntakeDraftStatus.READY,
            errorCode = null,
            createdAt = Instant.parse("2026-09-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-09-01T00:00:00Z"),
        )
        val foodRepository = InMemoryFoodRepository()
        val intakeRepository = InMemoryIntakeDraftRepository(
            initialDraft = draft,
            initialCandidates = listOf(
                intakeCandidate("두부", IntakeCandidateGroup.MANAGE, true, 0),
                intakeCandidate("시금치", IntakeCandidateGroup.MANAGE, true, 1),
            ),
        )
        val viewModel = RescueViewModel(
            repository = foodRepository,
            intakeDraftRepository = intakeRepository,
            clock = fixedClock(),
        )
        setRoute(viewModel)

        composeRule.onNodeWithText("선택한 2개 담기").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { foodRepository.foodItems.first().size == 2 }
        }
        composeRule.onNodeWithText("두부").assertIsDisplayed()
        composeRule.onNodeWithText("시금치").assertIsDisplayed()
    }

    @Test
    fun TC_INTAKE_012_import_options_open_manual_entry() {
        val viewModel = RescueViewModel(
            repository = InMemoryFoodRepository(),
            clock = fixedClock(),
        )
        setRoute(viewModel)

        composeRule.onNodeWithText("구매내역 가져오기").performClick()
        composeRule.onNodeWithText("종이 영수증 사진 선택").assertIsDisplayed()
        composeRule.onNodeWithText("직접 입력").performClick()

        composeRule.onNodeWithText("새 식재료 추가").assertIsDisplayed()
    }

    @Test
    fun TC_OCR_006_failed_analysis_keeps_direct_entry_fallback() {
        val draft = IntakeDraft(
            id = "failed-image",
            contentType = IntakeContentType.IMAGE,
            mimeType = "image/png",
            textContent = null,
            cachedFilePath = null,
            status = IntakeDraftStatus.ERROR,
            errorCode = IntakeErrorCode.SHARED_FILE_SIGNATURE_INVALID,
            createdAt = Instant.parse("2026-09-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-09-01T00:00:00Z"),
        )
        val viewModel = RescueViewModel(
            repository = InMemoryFoodRepository(),
            intakeDraftRepository = InMemoryIntakeDraftRepository(initialDraft = draft),
            clock = fixedClock(),
        )
        setRoute(viewModel)

        composeRule.onNodeWithText("직접 입력").performClick()

        composeRule.onNodeWithText("새 식재료 추가").assertIsDisplayed()
    }

    @Test
    fun TC_EDITOR_007_add_food_updates_queue() {
        val repository = InMemoryFoodRepository()
        val viewModel = RescueViewModel(
            repository = repository,
            clock = fixedClock(),
        )
        setRoute(viewModel)

        composeRule.onNodeWithText("재료 추가").performClick()
        composeRule.onNodeWithTag(FoodEditorTestTags.NAME).performTextInput("두부")
        composeRule.onNodeWithTag(FoodEditorTestTags.QUANTITY).performTextInput("2")
        composeRule.onNodeWithTag(FoodEditorTestTags.DATE).performTextInput("2026-09-03")
        composeRule.onNodeWithTag(FoodEditorTestTags.storage(StorageLocation.FROZEN))
            .performClick()
        composeRule.onNodeWithTag(FoodEditorTestTags.SAVE).performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("두부").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("확정한 날짜 · 9월 3일").assertIsDisplayed()
        composeRule.onNodeWithText("냉동").assertIsDisplayed()
    }

    @Test
    fun TC_EDITOR_008_edit_food_preserves_id_and_updates_content() {
        val original = foodItem(
            id = "tofu",
            name = "두부",
            date = FoodDate(today.plusDays(1), FoodDateSource.MANUFACTURER_DISPLAYED),
        )
        val repository = InMemoryFoodRepository(listOf(original))
        val viewModel = RescueViewModel(
            repository = repository,
            clock = fixedClock(),
        )
        setRoute(viewModel)

        composeRule.onNodeWithText("수정").performClick()
        composeRule.onNodeWithTag(FoodEditorTestTags.NAME).performTextReplacement("부침용 두부")
        composeRule.onNodeWithTag(FoodEditorTestTags.SAVE).performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("부침용 두부").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(original.id, runBlocking { repository.findById(original.id) }?.id)
    }

    private fun setScreen(
        uiState: RescueUiState,
        onAction: (RescueAction) -> Unit = {},
    ) {
        composeRule.setContent {
            FridgeRescueTheme {
                RescueScreen(
                    uiState = uiState,
                    snackbarHostState = SnackbarHostState(),
                    onAction = onAction,
                )
            }
        }
    }

    private fun setRoute(viewModel: RescueViewModel) {
        composeRule.setContent {
            FridgeRescueTheme {
                RescueRoute(viewModel = viewModel)
            }
        }
    }

    private fun fixedClock(): Clock = Clock.fixed(
        today.atStartOfDay().toInstant(ZoneOffset.UTC),
        ZoneOffset.UTC,
    )

    private fun contentState(vararg items: FoodItem): RescueUiState.Content {
        val queueItems = GetRescueQueueUseCase()(items.toList(), today)
        return RescueUiState.Content(
            items = queueItems,
            urgentCount = queueItems.count {
                it.urgency == RescueUrgency.OVERDUE ||
                    it.urgency == RescueUrgency.TODAY ||
                    it.urgency == RescueUrgency.SOON
            },
            needsReviewCount = queueItems.count { it.urgency == RescueUrgency.NEEDS_DATE },
        )
    }

    private fun foodItem(
        id: String,
        name: String,
        date: FoodDate,
    ) = FoodItem(
        id = FoodItemId(id),
        name = name,
        storageLocation = StorageLocation.REFRIGERATED,
        dates = listOf(date),
    )

    private fun intakeCandidate(
        name: String,
        group: IntakeCandidateGroup,
        selected: Boolean,
        position: Int,
    ) = IntakeCandidate(
        id = "candidate-$position",
        draftId = "grouped",
        originalName = name,
        normalizedName = name,
        quantity = 1,
        group = group,
        isSelected = selected,
        reason = null,
        position = position,
        storageLocation = StorageLocation.REFRIGERATED,
        estimatedShelfLifeDays = null,
    )
}
