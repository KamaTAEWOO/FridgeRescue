package com.portfolio.fridgerescue.core.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface IntakeCandidateDao {
    @Query("SELECT * FROM intake_candidates WHERE draft_id = :draftId ORDER BY position")
    fun observeForDraft(draftId: String): Flow<List<IntakeCandidateEntity>>

    @Query("SELECT * FROM intake_candidates WHERE draft_id = :draftId ORDER BY position")
    suspend fun findForDraft(draftId: String): List<IntakeCandidateEntity>

    @Query("DELETE FROM intake_candidates WHERE draft_id = :draftId")
    suspend fun deleteForDraft(draftId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(candidates: List<IntakeCandidateEntity>)

    @Query("UPDATE intake_candidates SET is_selected = :selected WHERE id = :id")
    suspend fun updateSelected(id: String, selected: Boolean)

    @Query(
        "UPDATE intake_candidates " +
            "SET normalized_name = :name, quantity = :quantity WHERE id = :id",
    )
    suspend fun updateContent(id: String, name: String, quantity: Int?)

    @Transaction
    suspend fun replaceForDraft(draftId: String, candidates: List<IntakeCandidateEntity>) {
        deleteForDraft(draftId)
        insertAll(candidates)
    }
}
