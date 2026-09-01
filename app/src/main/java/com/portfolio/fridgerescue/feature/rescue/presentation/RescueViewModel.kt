package com.portfolio.fridgerescue.feature.rescue.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.portfolio.fridgerescue.FridgeRescueApplication
import com.portfolio.fridgerescue.core.data.repository.FoodRepository
import com.portfolio.fridgerescue.core.model.FoodItem
import com.portfolio.fridgerescue.core.model.FoodItemId
import com.portfolio.fridgerescue.core.model.FoodStatus
import com.portfolio.fridgerescue.feature.rescue.domain.FoodItemDraft
import com.portfolio.fridgerescue.feature.rescue.domain.GetRescueQueueUseCase
import com.portfolio.fridgerescue.feature.rescue.domain.RescueUrgency
import com.portfolio.fridgerescue.feature.rescue.domain.SaveFoodItemResult
import com.portfolio.fridgerescue.feature.rescue.domain.SaveFoodItemUseCase
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RescueViewModel(
    private val repository: FoodRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val getRescueQueue: GetRescueQueueUseCase = GetRescueQueueUseCase(),
    private val saveFoodItem: SaveFoodItemUseCase = SaveFoodItemUseCase(repository),
) : ViewModel() {
    private val actionMutex = Mutex()
    private val undoItems = mutableMapOf<FoodItemId, FoodItem>()
    private val eventChannel = Channel<RescueEvent>(Channel.BUFFERED)
    private val editorState = MutableStateFlow<FoodEditorUiState?>(null)

    val events = eventChannel.receiveAsFlow()

    val uiState = combine(repository.foodItems, editorState) { foodItems, editor ->
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
            editor = editor,
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
            RescueAction.StartAddFood -> editorState.value = FoodEditorUiState()
            is RescueAction.StartEditFood -> startEdit(action.foodItemId)
            RescueAction.DismissEditor -> editorState.value = null
            is RescueAction.ChangeEditorName -> updateEditor { copy(name = action.value) }
            is RescueAction.ChangeEditorQuantity -> updateEditor { copy(quantity = action.value) }
            is RescueAction.ChangeEditorDate -> updateEditor { copy(date = action.value) }
            is RescueAction.ChangeEditorStorage -> updateEditor {
                copy(storageLocation = action.value)
            }
            is RescueAction.ChangeEditorOpened -> updateEditor { copy(isOpened = action.value) }
            is RescueAction.ChangeEditorPinned -> updateEditor { copy(isPinned = action.value) }
            RescueAction.SaveEditor -> saveEditor()
        }
    }

    private fun startEdit(foodItemId: FoodItemId) {
        viewModelScope.launch {
            val foodItem = repository.findById(foodItemId) ?: return@launch
            editorState.value = FoodEditorUiState(
                foodItemId = foodItem.id,
                name = foodItem.name,
                quantity = foodItem.quantity?.toString().orEmpty(),
                date = foodItem.effectiveDate()?.value?.toString().orEmpty(),
                storageLocation = foodItem.storageLocation,
                isOpened = foodItem.isOpened,
                isPinned = foodItem.isPinned,
            )
        }
    }

    private fun updateEditor(transform: FoodEditorUiState.() -> FoodEditorUiState) {
        editorState.value = editorState.value
            ?.transform()
            ?.copy(error = null, saveFailed = false)
    }

    private fun saveEditor() {
        val editor = editorState.value?.takeUnless { it.isSaving } ?: return
        editorState.value = editor.copy(error = null, saveFailed = false, isSaving = true)
        viewModelScope.launch {
            val result = try {
                saveFoodItem(
                    FoodItemDraft(
                        foodItemId = editor.foodItemId,
                        nameInput = editor.name,
                        quantityInput = editor.quantity,
                        dateInput = editor.date,
                        storageLocation = editor.storageLocation,
                        isOpened = editor.isOpened,
                        isPinned = editor.isPinned,
                    ),
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                editorState.value = editor.copy(saveFailed = true, isSaving = false)
                return@launch
            }

            when (result) {
                is SaveFoodItemResult.Invalid -> {
                    editorState.value = editor.copy(error = result.error, isSaving = false)
                }
                is SaveFoodItemResult.Saved -> {
                    editorState.value = null
                    eventChannel.send(
                        RescueEvent.ShowFoodSaved(
                            foodName = result.foodItem.name,
                            isNew = result.isNew,
                        ),
                    )
                }
            }
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

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY]) as FridgeRescueApplication
                RescueViewModel(repository = application.container.foodRepository)
            }
        }
    }
}
