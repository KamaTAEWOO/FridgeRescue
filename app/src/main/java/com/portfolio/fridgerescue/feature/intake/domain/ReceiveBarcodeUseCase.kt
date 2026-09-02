package com.portfolio.fridgerescue.feature.intake.domain

import com.portfolio.fridgerescue.core.domain.repository.IntakeDraftRepository
import com.portfolio.fridgerescue.core.domain.model.IntakeCandidate
import com.portfolio.fridgerescue.core.domain.model.IntakeCandidateGroup
import com.portfolio.fridgerescue.core.domain.model.IntakeContentType
import com.portfolio.fridgerescue.core.domain.model.IntakeDraft
import com.portfolio.fridgerescue.core.domain.model.IntakeDraftStatus
import com.portfolio.fridgerescue.core.domain.model.StorageLocation
import java.time.Clock
import java.time.Instant
import java.util.UUID

class ReceiveBarcodeUseCase(
    private val repository: IntakeDraftRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val parser: Gs1BarcodeParser = Gs1BarcodeParser(),
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) {
    suspend operator fun invoke(rawValue: String, format: String?): String? {
        if (rawValue.isBlank()) return null
        val parsed = parser.parse(rawValue)
        val now = Instant.now(clock)
        val draftId = idFactory()
        val candidateId = idFactory()
        val candidateName = parsed.gtin?.let { "GTIN $it" }
            ?: "바코드 ${rawValue.trim().take(MAX_NAME_VALUE_LENGTH)}"
        repository.save(
            IntakeDraft(
                id = draftId,
                contentType = IntakeContentType.TEXT,
                mimeType = format?.let { "application/vnd.barcode.$it" } ?: "application/vnd.barcode",
                textContent = rawValue,
                cachedFilePath = null,
                status = IntakeDraftStatus.READY,
                errorCode = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        repository.replaceCandidates(
            draftId,
            listOf(
                IntakeCandidate(
                    id = candidateId,
                    draftId = draftId,
                    originalName = rawValue,
                    normalizedName = candidateName,
                    quantity = 1,
                    group = IntakeCandidateGroup.REVIEW,
                    isSelected = false,
                    reason = if (parsed.isGs1) {
                        "GS1 정보에서 상품명을 확인해 주세요"
                    } else {
                        "바코드만으로 상품명을 확인할 수 없어요"
                    },
                    position = 0,
                    storageLocation = StorageLocation.REFRIGERATED,
                    estimatedShelfLifeDays = null,
                    displayedDate = parsed.displayedDate,
                ),
            ),
        )
        return draftId
    }

    private companion object {
        const val MAX_NAME_VALUE_LENGTH = 48
    }
}
