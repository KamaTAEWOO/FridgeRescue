package com.portfolio.fridgerescue.feature.rescue.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.fridgerescue.core.data.datasource.local.DemoFoodItems
import com.portfolio.fridgerescue.core.data.datasource.local.InMemoryFoodRepository
import com.portfolio.fridgerescue.core.data.repository.FoodRepository
import com.portfolio.fridgerescue.core.model.FoodItem
import com.portfolio.fridgerescue.core.model.FoodItemId
import com.portfolio.fridgerescue.core.model.FoodStatus
import com.portfolio.fridgerescue.feature.rescue.domain.GetRescueQueueUseCase
import com.portfolio.fridgerescue.feature.rescue.domain.RescueUrgency
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RescueViewModel(
    private val clock: Clock = Clock.systemDefaultZone(),
    private val repository: FoodRepository = InMemoryFoodRepository(
        DemoFoodItems.create(LocalDate.now(clock)),
    ),
    private val getRescueQueue: GetRescueQueueUseCase = GetRescueQueueUseCase(),
) : ViewModel() {
    private val actionMutex = Mutex()
    private val undoItems = mutableMapOf<FoodItemId, FoodItem>()
    private val eventChannel = Channel<RescueEvent>(Channel.BUFFERED)

    val events = eventChannel.receiveAsFlow()

    val uiState = repository.foodItems
        .map { foodItems ->
            val queueItems = getRescueQueue(foodItems, LocalDate.now(clock))
            RescueUiState.Content(
                items = queueItems,
                urgentCount = queueItems.count {
                    it.urgency == RescueUrgency.OVERDUE ||
                        it.urgency == RescueUrgency.TODAY ||
                        it.urgency == RescueUrgency.SOON
                },
                needsReviewCount = queueItems.count {
                    it.urgency == RescueUrgency.NEEDS_DATE ||
                        it.foodItem.status == FoodStatus.NEEDS_REVIEW
                },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RescueUiState.Loading,
        )

    fun onAction(action: RescueAction) {
        when (action) {
            is RescueAction.MarkConsumed -> markConsumed(action.foodItemId)
            is RescueAction.UndoConsumed -> undoConsumed(action.foodItemId)
        }
    }

    private fun markConsumed(foodItemId: FoodItemId) {
        viewModelScope.launch {
            actionMutex.withLock {
                val currentItem = repository.findById(foodItemId)
                    ?.takeUnless(FoodItem::isFinalized)
                    ?: return@withLock

                undoItems[foodItemId] = currentItem
                repository.upsert(currentItem.copy(status = FoodStatus.CONSUMED))
                eventChannel.send(
                    RescueEvent.ShowConsumedUndo(
                        foodItemId = foodItemId,
                        foodName = currentItem.name,
                    ),
                )
            }
        }
    }

    private fun undoConsumed(foodItemId: FoodItemId) {
        viewModelScope.launch {
            actionMutex.withLock {
                val previousItem = undoItems.remove(foodItemId) ?: return@withLock
                repository.upsert(previousItem)
            }
        }
    }
}
