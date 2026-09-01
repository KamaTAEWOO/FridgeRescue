package com.portfolio.fridgerescue.feature.rescue

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.portfolio.fridgerescue.core.data.datasource.local.InMemoryFoodRepository
import com.portfolio.fridgerescue.core.model.FoodDate
import com.portfolio.fridgerescue.core.model.FoodDateSource
import com.portfolio.fridgerescue.core.model.FoodItem
import com.portfolio.fridgerescue.core.model.FoodItemId
import com.portfolio.fridgerescue.core.model.StorageLocation
import com.portfolio.fridgerescue.feature.rescue.domain.GetRescueQueueUseCase
import com.portfolio.fridgerescue.feature.rescue.domain.RescueUrgency
import com.portfolio.fridgerescue.feature.rescue.presentation.RescueAction
import com.portfolio.fridgerescue.feature.rescue.presentation.RescueRoute
import com.portfolio.fridgerescue.feature.rescue.presentation.RescueScreen
import com.portfolio.fridgerescue.feature.rescue.presentation.RescueUiState
import com.portfolio.fridgerescue.feature.rescue.presentation.RescueViewModel
import com.portfolio.fridgerescue.ui.theme.FridgeRescueTheme
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
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
    fun TC_UI_009_empty_queue_shows_next_step_guidance() {
        setScreen(contentState())

        composeRule.onNodeWithText("지금 구조할 재료가 없어요").assertIsDisplayed()
        composeRule.onNodeWithText("구매내역 공유 기능은 다음 단계에서 연결할게요.")
            .assertIsDisplayed()
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
}
