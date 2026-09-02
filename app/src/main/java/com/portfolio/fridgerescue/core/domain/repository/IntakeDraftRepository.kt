package com.portfolio.fridgerescue.core.domain.repository

import com.portfolio.fridgerescue.core.domain.model.IntakeCandidate
import com.portfolio.fridgerescue.core.domain.model.IntakeDraft
import kotlinx.coroutines.flow.Flow

/** 공유로 들어온 구매 내역 초안과 후보 재료를 다루는 도메인 계약이다. */
interface IntakeDraftRepository {
    val latestActiveDraft: Flow<IntakeDraft?>
    suspend fun save(draft: IntakeDraft)
    suspend fun archive(id: String)
    fun observeCandidates(draftId: String): Flow<List<IntakeCandidate>>
    suspend fun candidates(draftId: String): List<IntakeCandidate>
    suspend fun replaceCandidates(draftId: String, candidates: List<IntakeCandidate>)
    suspend fun updateCandidateSelected(id: String, selected: Boolean)
    suspend fun updateCandidateContent(id: String, name: String, quantity: Int?)
}
