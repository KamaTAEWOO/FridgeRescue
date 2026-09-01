package com.portfolio.fridgerescue.feature.intake

import com.portfolio.fridgerescue.core.data.repository.IntakeDraftRepository

class UpdateIntakeCandidateUseCase(
    private val repository: IntakeDraftRepository,
) {
    suspend operator fun invoke(candidateId: String, name: String, quantityText: String): Result<Unit> {
        val normalizedName = name.trim().replace(WHITESPACE, " ")
        if (normalizedName.isEmpty()) return Result.failure(IllegalArgumentException("blank name"))

        val quantity = if (quantityText.isBlank()) null else quantityText.toIntOrNull()
        if (quantityText.isNotBlank() && (quantity == null || quantity <= 0)) {
            return Result.failure(IllegalArgumentException("invalid quantity"))
        }

        repository.updateCandidateContent(candidateId, normalizedName, quantity)
        return Result.success(Unit)
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
