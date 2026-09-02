package com.portfolio.fridgerescue.feature.intake.domain

import java.io.File

data class TextExtractionResult(
    val text: String,
    val isPartial: Boolean = false,
)

/** 이미지나 PDF에서 구매 내역 텍스트를 추출하는 도메인 계약이다. */
fun interface IntakeTextExtractor {
    suspend fun extract(file: File, isPdf: Boolean): TextExtractionResult
}
