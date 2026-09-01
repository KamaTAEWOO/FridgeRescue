package com.portfolio.fridgerescue.core.testing

import com.portfolio.fridgerescue.core.data.repository.IntakeDraftRepository
import com.portfolio.fridgerescue.core.model.IntakeCandidate
import com.portfolio.fridgerescue.core.model.IntakeDraft
import com.portfolio.fridgerescue.core.model.IntakeDraftStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemoryIntakeDraftRepository(
    initialDraft: IntakeDraft? = null,
    initialCandidates: List<IntakeCandidate> = emptyList(),
) : IntakeDraftRepository {
    private val drafts = MutableStateFlow(listOfNotNull(initialDraft))
    private val candidateValues = MutableStateFlow(initialCandidates)

    override val latestActiveDraft: Flow<IntakeDraft?> = drafts.map { values ->
        values.filter { it.status != IntakeDraftStatus.ARCHIVED }.maxByOrNull { it.updatedAt }
    }

    override suspend fun save(draft: IntakeDraft) {
        drafts.update { values -> values.filterNot { it.id == draft.id } + draft }
    }

    override suspend fun archive(id: String) {
        drafts.update { values ->
            values.map { draft ->
                if (draft.id == id) draft.copy(status = IntakeDraftStatus.ARCHIVED) else draft
            }
        }
    }

    override fun observeCandidates(draftId: String): Flow<List<IntakeCandidate>> =
        candidateValues.asStateFlow().map { values -> values.filter { it.draftId == draftId } }

    override suspend fun candidates(draftId: String): List<IntakeCandidate> =
        candidateValues.value.filter { it.draftId == draftId }

    override suspend fun replaceCandidates(
        draftId: String,
        candidates: List<IntakeCandidate>,
    ) {
        candidateValues.update { values -> values.filterNot { it.draftId == draftId } + candidates }
    }

    override suspend fun updateCandidateSelected(id: String, selected: Boolean) {
        candidateValues.update { values ->
            values.map { candidate ->
                if (candidate.id == id) candidate.copy(isSelected = selected) else candidate
            }
        }
    }

    override suspend fun updateCandidateContent(id: String, name: String, quantity: Int?) {
        candidateValues.update { values ->
            values.map { candidate ->
                if (candidate.id == id) {
                    candidate.copy(normalizedName = name, quantity = quantity)
                } else {
                    candidate
                }
            }
        }
    }
}
