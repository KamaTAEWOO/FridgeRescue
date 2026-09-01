package com.portfolio.fridgerescue.core.model

import java.time.Instant

enum class IntakeContentType { TEXT, IMAGE, PDF }
enum class IntakeDraftStatus { READY, ERROR, ARCHIVED }

data class IntakeDraft(
    val id: String,
    val contentType: IntakeContentType?,
    val mimeType: String?,
    val textContent: String?,
    val cachedFilePath: String?,
    val status: IntakeDraftStatus,
    val errorCode: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

object IntakeErrorCode {
    const val SHARE_TYPE_UNSUPPORTED = "SHARE_TYPE_UNSUPPORTED"
    const val SHARED_URI_UNAVAILABLE = "SHARED_URI_UNAVAILABLE"
    const val SHARED_FILE_TOO_LARGE = "SHARED_FILE_TOO_LARGE"
    const val SHARED_FILE_SIGNATURE_INVALID = "SHARED_FILE_SIGNATURE_INVALID"
    const val SHARED_TEXT_EMPTY = "SHARED_TEXT_EMPTY"
    const val SHARE_MULTIPLE_UNSUPPORTED = "SHARE_MULTIPLE_UNSUPPORTED"
}
