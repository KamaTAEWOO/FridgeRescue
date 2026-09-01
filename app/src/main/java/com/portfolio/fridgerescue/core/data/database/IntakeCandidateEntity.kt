package com.portfolio.fridgerescue.core.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "intake_candidates",
    indices = [Index(value = ["draft_id", "position"])],
)
data class IntakeCandidateEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "draft_id") val draftId: String,
    @ColumnInfo(name = "original_name") val originalName: String,
    @ColumnInfo(name = "normalized_name") val normalizedName: String,
    val quantity: Int?,
    @ColumnInfo(name = "candidate_group") val candidateGroup: String,
    @ColumnInfo(name = "is_selected") val isSelected: Boolean,
    val reason: String?,
    val position: Int,
    @ColumnInfo(name = "storage_location") val storageLocation: String,
    @ColumnInfo(name = "estimated_shelf_life_days") val estimatedShelfLifeDays: Int?,
)
