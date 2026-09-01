package com.portfolio.fridgerescue.core.data.repository

import com.portfolio.fridgerescue.core.data.database.IntakeCandidateDao
import com.portfolio.fridgerescue.core.data.database.IntakeCandidateEntity
import com.portfolio.fridgerescue.core.data.database.IntakeDraftDao
import com.portfolio.fridgerescue.core.data.database.IntakeDraftEntity
import com.portfolio.fridgerescue.core.model.IntakeCandidate
import com.portfolio.fridgerescue.core.model.IntakeCandidateGroup
import com.portfolio.fridgerescue.core.model.IntakeContentType
import com.portfolio.fridgerescue.core.model.IntakeDraft
import com.portfolio.fridgerescue.core.model.IntakeDraftStatus
import com.portfolio.fridgerescue.core.model.StorageLocation
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

class RoomIntakeDraftRepository(
    private val dao: IntakeDraftDao,
    private val candidateDao: IntakeCandidateDao,
    private val clock: Clock = Clock.systemUTC(),
) : IntakeDraftRepository {
    private val mutex = Mutex()
    override val latestActiveDraft = dao.observeLatestActive().map { it?.toDomain() }

    override suspend fun save(draft: IntakeDraft) = mutex.withLock {
        val latest = dao.latestUpdatedAt()
        val nextUpdatedAt = maxOf(
            draft.updatedAt.toEpochMilli(),
            latest?.let { if (it == Long.MAX_VALUE) it else it + 1 } ?: Long.MIN_VALUE,
        )
        dao.upsert(draft.copy(updatedAt = Instant.ofEpochMilli(nextUpdatedAt)).toEntity())
    }

    override suspend fun archive(id: String) = mutex.withLock {
        val latest = dao.latestUpdatedAt()
        val updatedAt = maxOf(
            clock.millis(),
            latest?.let { if (it == Long.MAX_VALUE) it else it + 1 } ?: Long.MIN_VALUE,
        )
        dao.archive(id, updatedAt)
    }

    override fun observeCandidates(draftId: String): Flow<List<IntakeCandidate>> =
        candidateDao.observeForDraft(draftId).map { values -> values.map { it.toDomain() } }

    override suspend fun candidates(draftId: String): List<IntakeCandidate> =
        candidateDao.findForDraft(draftId).map { it.toDomain() }

    override suspend fun replaceCandidates(
        draftId: String,
        candidates: List<IntakeCandidate>,
    ) = candidateDao.replaceForDraft(draftId, candidates.map { it.toEntity() })

    override suspend fun updateCandidateSelected(id: String, selected: Boolean) =
        candidateDao.updateSelected(id, selected)

    override suspend fun updateCandidateContent(id: String, name: String, quantity: Int?) =
        candidateDao.updateContent(id, name, quantity)
}

private fun IntakeDraftEntity.toDomain() = IntakeDraft(
    id = id,
    contentType = contentType?.let { runCatching { IntakeContentType.valueOf(it) }.getOrNull() },
    mimeType = mimeType,
    textContent = textContent,
    cachedFilePath = cachedFilePath,
    status = runCatching { IntakeDraftStatus.valueOf(status) }.getOrDefault(IntakeDraftStatus.ERROR),
    errorCode = errorCode,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
)

private fun IntakeDraft.toEntity() = IntakeDraftEntity(
    id = id,
    contentType = contentType?.name,
    mimeType = mimeType,
    textContent = textContent,
    cachedFilePath = cachedFilePath,
    status = status.name,
    errorCode = errorCode,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
)

private fun IntakeCandidateEntity.toDomain() = IntakeCandidate(
    id = id,
    draftId = draftId,
    originalName = originalName,
    normalizedName = normalizedName,
    quantity = quantity,
    group = runCatching { IntakeCandidateGroup.valueOf(candidateGroup) }
        .getOrDefault(IntakeCandidateGroup.REVIEW),
    isSelected = isSelected,
    reason = reason,
    position = position,
    storageLocation = runCatching { StorageLocation.valueOf(storageLocation) }
        .getOrDefault(StorageLocation.REFRIGERATED),
    estimatedShelfLifeDays = estimatedShelfLifeDays,
    displayedDate = displayedDate?.let(LocalDate::parse),
)

private fun IntakeCandidate.toEntity() = IntakeCandidateEntity(
    id = id,
    draftId = draftId,
    originalName = originalName,
    normalizedName = normalizedName,
    quantity = quantity,
    candidateGroup = group.name,
    isSelected = isSelected,
    reason = reason,
    position = position,
    storageLocation = storageLocation.name,
    estimatedShelfLifeDays = estimatedShelfLifeDays,
    displayedDate = displayedDate?.toString(),
)
