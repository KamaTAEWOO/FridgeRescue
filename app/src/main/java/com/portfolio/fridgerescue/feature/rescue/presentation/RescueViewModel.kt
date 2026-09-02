package com.portfolio.fridgerescue.feature.rescue.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.fridgerescue.core.data.repository.FoodRepository
import com.portfolio.fridgerescue.core.data.repository.IntakeDraftRepository
import com.portfolio.fridgerescue.core.data.DataDeletionManager
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
import com.portfolio.fridgerescue.feature.intake.UpdateIntakeCandidateUseCase
import com.portfolio.fridgerescue.feature.intake.FindDuplicateCandidatesUseCase
import com.portfolio.fridgerescue.feature.report.GetReportMetricsUseCase
import com.portfolio.fridgerescue.feature.notification.NotificationSettings
import com.portfolio.fridgerescue.feature.notification.NotificationSettingsRepository
import com.portfolio.fridgerescue.feature.family.FamilySyncManager
import com.portfolio.fridgerescue.feature.family.FamilySyncSettings
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.portfolio.fridgerescue.feature.rescue.domain.FilterRescueQueueUseCase
import com.portfolio.fridgerescue.feature.rescue.domain.PantryFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RescueViewModel(
    private val repository: FoodRepository,
    private val intakeDraftRepository: IntakeDraftRepository? = null,
    private val notificationSettingsRepository: NotificationSettingsRepository? = null,
    private val dataDeletionManager: DataDeletionManager? = null,
    private val familySyncManager: FamilySyncManager? = null,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val getRescueQueue: GetRescueQueueUseCase = GetRescueQueueUseCase(),
    private val filterRescueQueue: FilterRescueQueueUseCase = FilterRescueQueueUseCase(),
    private val saveFoodItem: SaveFoodItemUseCase = SaveFoodItemUseCase(repository),
    private val saveCandidateBatch: SaveIntakeCandidatesUseCase =
        SaveIntakeCandidatesUseCase(repository, clock),
    private val updateIntakeCandidate: UpdateIntakeCandidateUseCase? =
        intakeDraftRepository?.let(::UpdateIntakeCandidateUseCase),
    private val findDuplicateCandidates: FindDuplicateCandidatesUseCase =
        FindDuplicateCandidatesUseCase(),
    private val getReportMetrics: GetReportMetricsUseCase = GetReportMetricsUseCase(),
) : ViewModel() {
    @Inject
    constructor(dependencies: RescueDependencies) : this(
        repository = dependencies.foodRepository,
        intakeDraftRepository = dependencies.intakeDraftRepository,
        notificationSettingsRepository = dependencies.notificationSettingsRepository,
        dataDeletionManager = dependencies.dataDeletionManager,
        familySyncManager = dependencies.familySyncManager,
        clock = dependencies.clock,
        getRescueQueue = dependencies.getRescueQueue,
        filterRescueQueue = dependencies.filterRescueQueue,
        saveFoodItem = dependencies.saveFoodItem,
        saveCandidateBatch = dependencies.saveCandidateBatch,
        updateIntakeCandidate = dependencies.updateIntakeCandidate,
        findDuplicateCandidates = dependencies.findDuplicateCandidates,
        getReportMetrics = dependencies.getReportMetrics,
    )

    private val eventChannel = Channel<RescueEvent>(Channel.BUFFERED)
    private val editorState = MutableStateFlow<FoodEditorUiState?>(null)
    private val detailSelection = MutableStateFlow<DetailSelection?>(null)
    private val savingIntakeDraftId = MutableStateFlow<String?>(null)
    private val showImportOptions = MutableStateFlow(false)
    private val pantryFilter = MutableStateFlow(PantryFilter())
    private val deletingData = MutableStateFlow(false)
    private val familySyncBusy = MutableStateFlow(false)
    private val familySyncFeedback = MutableStateFlow<FamilySyncFeedback?>(null)

    val isDeletingData = deletingData.asStateFlow()

    val familySyncState = combine(
        familySyncManager?.settings ?: flowOf(FamilySyncSettings()),
        familySyncBusy,
        familySyncFeedback,
    ) { settings, busy, feedback -> FamilySyncUiState(settings, busy, feedback) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FamilySyncUiState(),
        )

    val events = eventChannel.receiveAsFlow()

    val notificationSettings = (notificationSettingsRepository?.settings ?: flowOf(NotificationSettings()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotificationSettings(),
        )

    fun setQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationSettingsRepository?.setQuietHoursEnabled(enabled)
        }
    }

    fun deleteAllData() {
        if (deletingData.value) return
        viewModelScope.launch {
            deletingData.value = true
            try {
                dataDeletionManager?.deleteAll()
                    ?: error("DataDeletionManager is unavailable")
                pantryFilter.value = PantryFilter()
                editorState.value = null
                detailSelection.value = null
                eventChannel.send(RescueEvent.ShowDataDeleted)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                eventChannel.send(RescueEvent.ShowDataDeletionFailed)
            } finally {
                deletingData.value = false
            }
        }
    }

    fun createFamilyAccount(serverUrl: String, displayName: String) = familyOperation(
        success = FamilySyncFeedback.ACCOUNT_CREATED,
    ) { require(displayName.isNotBlank()); familySyncManager?.createAccount(serverUrl, displayName) }

    fun joinFamily(inviteCode: String) = familyOperation(FamilySyncFeedback.FAMILY_JOINED) {
        require(inviteCode.isNotBlank()); familySyncManager?.joinFamily(inviteCode)
    }

    fun syncFamily() = familyOperation(FamilySyncFeedback.SYNCED) {
        familySyncManager?.syncNow()
    }

    private fun familyOperation(
        success: FamilySyncFeedback,
        block: suspend () -> Unit,
    ) {
        if (familySyncBusy.value) return
        viewModelScope.launch {
            familySyncBusy.value = true
            familySyncFeedback.value = null
            try {
                block()
                familySyncFeedback.value = success
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                familySyncFeedback.value = FamilySyncFeedback.FAILED
            } finally {
                familySyncBusy.value = false
            }
        }
    }

    val reportMetrics = combine(repository.foodItems, repository.events) { foodItems, events ->
        getReportMetrics(foodItems, events)
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

    private val queueState = combine(repository.foodItems, pantryFilter) { foodItems, filter ->
        val allItems = getRescueQueue(foodItems, LocalDate.now(clock))
        QueueSnapshot(
            foodItems = foodItems,
            allItems = allItems,
            filteredItems = filterRescueQueue(allItems, filter),
            filter = filter,
        )
    }

    val uiState = combine(
        queueState,
        editorState,
        detailState,
        intakeReview,
        showImportOptions,
    ) {
            queue,
            editor,
            detail,
            review,
            importOptionsVisible,
        ->
        val reviewWithDuplicates = review?.copy(
            duplicateCandidateIds = findDuplicateCandidates(
                candidates = review.candidates,
                foods = queue.foodItems,
            ),
        )
        RescueUiState.Content(
            items = queue.filteredItems,
            totalItemCount = queue.allItems.size,
            pantryFilter = queue.filter,
            urgentCount = queue.allItems.count {
                it.urgency == RescueUrgency.OVERDUE ||
                    it.urgency == RescueUrgency.TODAY ||
                    it.urgency == RescueUrgency.SOON
            },
            needsReviewCount = queue.allItems.count {
                it.urgency == RescueUrgency.NEEDS_DATE ||
                    it.foodItem.status == FoodStatus.NEEDS_REVIEW
            },
            editor = editor,
            detail = detail,
            intakeReview = reviewWithDuplicates,
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
            is RescueAction.UpdateIntakeCandidate -> viewModelScope.launch {
                updateIntakeCandidate?.invoke(
                    candidateId = action.candidateId,
                    name = action.name,
                    quantityText = action.quantity,
                )
            }
            is RescueAction.SaveIntakeCandidates -> saveIntakeCandidates(action.draftId)
            RescueAction.OpenImportOptions -> showImportOptions.value = true
            RescueAction.DismissImportOptions -> showImportOptions.value = false
            is RescueAction.ChangePantrySearch -> pantryFilter.value = pantryFilter.value.copy(
                query = action.value,
            )
            is RescueAction.FilterPantryStorage -> pantryFilter.value = pantryFilter.value.copy(
                storageLocation = action.storageLocation,
            )
            is RescueAction.FilterPantryStatus -> pantryFilter.value = pantryFilter.value.copy(
                status = action.status,
            )
            RescueAction.ClearPantryFilters -> pantryFilter.value = PantryFilter()
            is RescueAction.StartManualFromIntake -> startManualFromIntake(action.draftId)
            RescueAction.StartAddFood -> {
                showImportOptions.value = false
                editorState.value = FoodEditorUiState()
            }
            is RescueAction.StartEditFood -> {
                detailSelection.value = null
                startEdit(action.foodItemId)
            }
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

    private data class DetailSelection(
        val foodItemId: FoodItemId,
        val discardReason: String = "",
        val actionInProgress: Boolean = false,
    )

    private data class QueueSnapshot(
        val foodItems: List<com.portfolio.fridgerescue.core.model.FoodItem>,
        val allItems: List<com.portfolio.fridgerescue.feature.rescue.domain.RescueQueueItem>,
        val filteredItems: List<com.portfolio.fridgerescue.feature.rescue.domain.RescueQueueItem>,
        val filter: PantryFilter,
    )
}

data class RescueDependencies(
    val foodRepository: FoodRepository,
    val intakeDraftRepository: IntakeDraftRepository,
    val notificationSettingsRepository: NotificationSettingsRepository,
    val dataDeletionManager: DataDeletionManager,
    val familySyncManager: FamilySyncManager,
    val clock: Clock,
    val getRescueQueue: GetRescueQueueUseCase,
    val filterRescueQueue: FilterRescueQueueUseCase,
    val saveFoodItem: SaveFoodItemUseCase,
    val saveCandidateBatch: SaveIntakeCandidatesUseCase,
    val updateIntakeCandidate: UpdateIntakeCandidateUseCase,
    val findDuplicateCandidates: FindDuplicateCandidatesUseCase,
    val getReportMetrics: GetReportMetricsUseCase,
)

enum class FamilySyncFeedback { ACCOUNT_CREATED, FAMILY_JOINED, SYNCED, FAILED }

data class FamilySyncUiState(
    val settings: FamilySyncSettings = FamilySyncSettings(),
    val isWorking: Boolean = false,
    val feedback: FamilySyncFeedback? = null,
)
