package com.portfolio.fridgerescue.core.data.local.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "intake_drafts",
    indices = [Index(value = ["status", "updated_at_epoch_millis"])],
)
data class IntakeDraftEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "content_type") val contentType: String?,
    @ColumnInfo(name = "mime_type") val mimeType: String?,
    @ColumnInfo(name = "text_content") val textContent: String?,
    @ColumnInfo(name = "cached_file_path") val cachedFilePath: String?,
    val status: String,
    @ColumnInfo(name = "error_code") val errorCode: String?,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
)
