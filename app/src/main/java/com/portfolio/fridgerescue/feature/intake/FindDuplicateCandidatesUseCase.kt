package com.portfolio.fridgerescue.feature.intake

import com.portfolio.fridgerescue.core.model.FoodItem
import com.portfolio.fridgerescue.core.model.IntakeCandidate

class FindDuplicateCandidatesUseCase {
    operator fun invoke(
        candidates: List<IntakeCandidate>,
        foods: List<FoodItem>,
    ): Set<String> {
        val activeNames = foods.asSequence()
            .filterNot(FoodItem::isFinalized)
            .map { normalize(it.name) }
            .toSet()
        return candidates.asSequence()
            .filter { normalize(it.normalizedName) in activeNames }
            .map(IntakeCandidate::id)
            .toSet()
    }

    private fun normalize(value: String): String = value
        .trim()
        .lowercase()
        .replace(WHITESPACE, " ")

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
