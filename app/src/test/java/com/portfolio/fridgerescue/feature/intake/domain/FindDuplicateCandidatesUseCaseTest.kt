package com.portfolio.fridgerescue.feature.intake.domain

import com.portfolio.fridgerescue.core.domain.model.FoodItem
import com.portfolio.fridgerescue.core.domain.model.FoodItemId
import com.portfolio.fridgerescue.core.domain.model.FoodStatus
import com.portfolio.fridgerescue.core.domain.model.IntakeCandidate
import com.portfolio.fridgerescue.core.domain.model.IntakeCandidateGroup
import com.portfolio.fridgerescue.core.domain.model.StorageLocation
import org.junit.Assert.assertEquals
import org.junit.Test

class FindDuplicateCandidatesUseCaseTest {
    @Test
    fun TC_DUP_001_only_active_normalized_name_matches_are_warned() {
        val candidates = listOf(candidate("milk", "  우유  "), candidate("tofu", "두부"))
        val foods = listOf(
            food("우유", FoodStatus.ACTIVE),
            food("두부", FoodStatus.CONSUMED),
        )

        assertEquals(setOf("milk"), FindDuplicateCandidatesUseCase()(candidates, foods))
    }

    private fun candidate(id: String, name: String) = IntakeCandidate(
        id = id,
        draftId = "draft",
        originalName = name,
        normalizedName = name,
        quantity = 1,
        group = IntakeCandidateGroup.MANAGE,
        isSelected = true,
        reason = null,
        position = 0,
        storageLocation = StorageLocation.REFRIGERATED,
        estimatedShelfLifeDays = null,
    )

    private fun food(name: String, status: FoodStatus) = FoodItem(
        id = FoodItemId("food-$name"),
        name = name,
        storageLocation = StorageLocation.REFRIGERATED,
        status = status,
    )
}
