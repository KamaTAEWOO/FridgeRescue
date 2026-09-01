package com.portfolio.fridgerescue.core.data.repository

import com.portfolio.fridgerescue.core.data.database.IntakeDraftDao
import com.portfolio.fridgerescue.core.data.database.IntakeDraftEntity
import com.portfolio.fridgerescue.core.model.IntakeContentType
import com.portfolio.fridgerescue.core.model.IntakeDraft
import com.portfolio.fridgerescue.core.model.IntakeDraftStatus
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface IntakeDraftRepository {
    val latestActiveDraft: Flow<IntakeDraft?>
    suspend fun save(draft: IntakeDraft)
    suspend fun archive(id: String)
}

class RoomIntakeDraftRepository(
    private val dao: IntakeDraftDao,
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
