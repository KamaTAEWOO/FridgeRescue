package com.portfolio.fridgerescue.core.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface IntakeDraftDao {
    @Query(
        "SELECT * FROM intake_drafts WHERE status != 'ARCHIVED' " +
            "ORDER BY updated_at_epoch_millis DESC LIMIT 1",
    )
    fun observeLatestActive(): Flow<IntakeDraftEntity?>

    @Query("SELECT * FROM intake_drafts WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): IntakeDraftEntity?

    @Query("SELECT MAX(updated_at_epoch_millis) FROM intake_drafts")
    suspend fun latestUpdatedAt(): Long?

    @Upsert
    suspend fun upsert(draft: IntakeDraftEntity)

    @Query(
        "UPDATE intake_drafts SET status = 'ARCHIVED', updated_at_epoch_millis = :updatedAt " +
            "WHERE id = :id",
    )
    suspend fun archive(id: String, updatedAt: Long)
}
