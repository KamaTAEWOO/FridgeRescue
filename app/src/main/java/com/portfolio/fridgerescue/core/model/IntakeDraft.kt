package com.portfolio.fridgerescue.core.model

import java.time.Instant

enum class IntakeContentType { TEXT, IMAGE, PDF }
enum class IntakeDraftStatus { PROCESSING, READY, ERROR, ARCHIVED }

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
    const val OCR_NO_ITEMS = "OCR_NO_ITEMS"
    const val OCR_PROCESSING_FAILED = "OCR_PROCESSING_FAILED"
    const val OCR_PARTIAL = "OCR_PARTIAL"
}
