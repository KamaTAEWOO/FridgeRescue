package com.portfolio.fridgerescue.feature.rescue.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.portfolio.fridgerescue.FridgeRescueApplication
import com.portfolio.fridgerescue.core.data.repository.FoodRepository
import com.portfolio.fridgerescue.core.data.repository.IntakeDraftRepository
import com.portfolio.fridgerescue.core.model.FoodActionRequest
import com.portfolio.fridgerescue.core.model.FoodActionType
import com.portfolio.fridgerescue.core.model.FoodItemId
import com.portfolio.fridgerescue.core.model.FoodMutationResult
import com.portfolio.fridgerescue.core.model.FoodStatus
import com.portfolio.fridgerescue.feature.rescue.domain.FoodItemDraft
import com.portfolio.fridgerescue.feature.rescue.domain.GetRescueQueueUseCase
import com.portfolio.fridgerescue.feature.rescue.domain.RescueUrgency
import com.portfolio.fridgerescue.feature.rescue.domain.SaveFoodItemResult
import com.portfolio.fridgerescue.feature.rescue.domain.SaveFoodItemUseCase
import com.portfolio.fridgerescue.feature.intake.SaveIntakeCandidatesUseCase
import com.portfolio.fridgerescue.feature.report.GetReportMetricsUseCase
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RescueViewModel(
    private val repository: FoodRepository,
    private val intakeDraftRepository: IntakeDraftRepository? = null,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val getRescueQueue: GetRescueQueueUseCase = GetRescueQueueUseCase(),
    private val saveFoodItem: SaveFoodItemUseCase = SaveFoodItemUseCase(repository),
    private val saveCandidateBatch: SaveIntakeCandidatesUseCase =
        SaveIntakeCandidatesUseCase(repository, clock),
) : ViewModel() {
    private val eventChannel = Channel<RescueEvent>(Channel.BUFFERED)
    private val editorState = MutableStateFlow<FoodEditorUiState?>(null)
    private val detailSelection = MutableStateFlow<DetailSelection?>(null)
    private val savingIntakeDraftId = MutableStateFlow<String?>(null)
    private val showImportOptions = MutableStateFlow(false)

    val events = eventChannel.receiveAsFlow()

    val reportMetrics = combine(repository.foodItems, repository.events) { foodItems, events ->
        GetReportMetricsUseCase()(foodItems, events)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = com.portfolio.fridgerescue.feature.report.ReportMetrics(),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val detailState = detailSelection.flatMapLatest { selection ->
        if (selection == null) {
            flowOf(null)
        } else {
            combine(repository.foodItems, repository.observeEvents(selection.foodItemId)) {
                    foodItems,
                    events,
                ->
                foodItems.firstOrNull { it.id == selection.foodItemId }?.let { foodItem ->
                    FoodDetailUiState(
                        foodItem = foodItem,
                        events = events,
                        discardReason = selection.discardReason,
                        actionInProgress = selection.actionInProgress,
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val intakeReview = intakeDraftRepository?.latestActiveDraft?.flatMapLatest { draft ->
        if (draft == null) {
            flowOf(null)
        } else {
            combine(
                intakeDraftRepository.observeCandidates(draft.id),
                savingIntakeDraftId,
            ) { candidates, savingId ->
                IntakeReviewUiState(
                    draft = draft,
                    candidates = candidates,
                    isSaving = savingId == draft.id,
                )
            }
        }
    } ?: flowOf(null)

    val uiState = combine(
        repository.foodItems,
        editorState,
        detailState,
        intakeReview,
        showImportOptions,
    ) {
            foodItems,
            editor,
            detail,
            review,
            importOptionsVisible,
        ->
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
            detail = detail,
            intakeReview = review,
            showImportOptions = importOptionsVisible,
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
            is RescueAction.OpenFoodActions -> detailSelection.value =
                DetailSelection(action.foodItemId)
            RescueAction.DismissFoodActions -> detailSelection.value = null
            is RescueAction.RecordFoodAction -> recordAction(action.foodItemId, action.type)
            is RescueAction.ChangeDiscardReason -> detailSelection.value = detailSelection.value
                ?.copy(discardReason = action.value)
            is RescueAction.UndoMutation -> undoMutation(action.eventId)
            is RescueAction.DismissIntakeDraft -> dismissIntakeDraft(action.draftId)
            is RescueAction.ToggleIntakeCandidate -> viewModelScope.launch {
                intakeDraftRepository?.updateCandidateSelected(action.candidateId, action.selected)
            }
            is RescueAction.SaveIntakeCandidates -> saveIntakeCandidates(action.draftId)
            RescueAction.OpenImportOptions -> showImportOptions.value = true
            RescueAction.DismissImportOptions -> showImportOptions.value = false
            is RescueAction.StartManualFromIntake -> startManualFromIntake(action.draftId)
            RescueAction.StartAddFood -> {
                showImportOptions.value = false
                editorState.value = FoodEditorUiState()
            }
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
        recordAction(foodItemId, FoodActionType.CONSUME)
    }

    private fun recordAction(foodItemId: FoodItemId, type: FoodActionType) {
        viewModelScope.launch {
            val currentItem = repository.findById(foodItemId) ?: return@launch
            detailSelection.value = detailSelection.value
                ?.takeIf { it.foodItemId == foodItemId }
                ?.copy(actionInProgress = true)
            val result = repository.performAction(
                FoodActionRequest(
                    foodItemId = foodItemId,
                    type = type,
                    operationId = UUID.randomUUID().toString(),
                    discardReason = if (type == FoodActionType.DISCARD) {
                        detailSelection.value?.discardReason
                    } else {
                        null
                    },
                ),
            )
            if (result is FoodMutationResult.Applied) {
                detailSelection.value = null
                eventChannel.send(
                    RescueEvent.ShowMutationUndo(
                        eventId = result.event.id,
                        foodName = currentItem.name,
                        actionType = type,
                    ),
                )
            } else {
                detailSelection.value = detailSelection.value?.copy(actionInProgress = false)
            }
        }
    }

    private fun undoMutation(eventId: String) {
        viewModelScope.launch {
            repository.undo(eventId, UUID.randomUUID().toString())
        }
    }

    private fun dismissIntakeDraft(draftId: String) {
        viewModelScope.launch { intakeDraftRepository?.archive(draftId) }
    }

    private fun startManualFromIntake(draftId: String) {
        viewModelScope.launch {
            intakeDraftRepository?.archive(draftId)
            editorState.value = FoodEditorUiState()
        }
    }

    private fun saveIntakeCandidates(draftId: String) {
        if (savingIntakeDraftId.value != null) return
        viewModelScope.launch {
            savingIntakeDraftId.value = draftId
            try {
                val candidates = intakeDraftRepository?.candidates(draftId).orEmpty()
                val count = saveCandidateBatch(candidates)
                if (count > 0) {
                    intakeDraftRepository?.archive(draftId)
                    eventChannel.send(RescueEvent.ShowBatchSaved(count))
                }
            } finally {
                savingIntakeDraftId.value = null
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY]) as FridgeRescueApplication
                RescueViewModel(
                    repository = application.container.foodRepository,
                    intakeDraftRepository = application.container.intakeDraftRepository,
                )
            }
        }
    }

    private data class DetailSelection(
        val foodItemId: FoodItemId,
        val discardReason: String = "",
        val actionInProgress: Boolean = false,
    )
}
