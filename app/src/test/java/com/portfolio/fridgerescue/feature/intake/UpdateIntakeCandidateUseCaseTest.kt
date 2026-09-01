package com.portfolio.fridgerescue.feature.intake

import com.portfolio.fridgerescue.core.model.IntakeCandidate
import com.portfolio.fridgerescue.core.model.IntakeCandidateGroup
import com.portfolio.fridgerescue.core.model.StorageLocation
import com.portfolio.fridgerescue.core.testing.InMemoryIntakeDraftRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateIntakeCandidateUseCaseTest {
    @Test
    fun TC_OCR_007_name_and_quantity_are_normalized_and_persisted() = runBlocking {
        val repository = InMemoryIntakeDraftRepository(initialCandidates = listOf(candidate()))

        val result = UpdateIntakeCandidateUseCase(repository)("candidate", "  부침용   두부 ", "3")

        assertTrue(result.isSuccess)
        assertEquals("부침용 두부", repository.candidates("draft").single().normalizedName)
        assertEquals(3, repository.candidates("draft").single().quantity)
    }

    @Test
    fun TC_OCR_008_invalid_edit_does_not_overwrite_candidate() = runBlocking {
        val repository = InMemoryIntakeDraftRepository(initialCandidates = listOf(candidate()))
        val useCase = UpdateIntakeCandidateUseCase(repository)

        assertTrue(useCase("candidate", "  ", "2").isFailure)
        assertTrue(useCase("candidate", "두부", "0").isFailure)

        assertEquals(candidate(), repository.candidates("draft").single())
    }

    private fun candidate() = IntakeCandidate(
        id = "candidate",
        draftId = "draft",
        originalName = "두부 1개",
        normalizedName = "두부",
        quantity = 1,
        group = IntakeCandidateGroup.MANAGE,
        isSelected = true,
        reason = null,
        position = 0,
        storageLocation = StorageLocation.REFRIGERATED,
        estimatedShelfLifeDays = 5,
    )
}
