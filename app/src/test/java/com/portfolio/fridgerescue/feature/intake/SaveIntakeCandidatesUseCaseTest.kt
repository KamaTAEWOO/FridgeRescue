package com.portfolio.fridgerescue.feature.intake

import com.portfolio.fridgerescue.core.model.FoodDateSource
import com.portfolio.fridgerescue.core.model.FoodItemId
import com.portfolio.fridgerescue.core.model.FoodStatus
import com.portfolio.fridgerescue.core.model.IntakeCandidate
import com.portfolio.fridgerescue.core.model.IntakeCandidateGroup
import com.portfolio.fridgerescue.core.model.StorageLocation
import com.portfolio.fridgerescue.core.testing.InMemoryFoodRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SaveIntakeCandidatesUseCaseTest {
    @Test
    fun `TC-UI-002 only selected candidates are saved in one batch`() = runBlocking {
        val repository = InMemoryFoodRepository()
        var nextId = 0
        val useCase = SaveIntakeCandidatesUseCase(
            repository = repository,
            clock = fixedClock(),
            idFactory = { FoodItemId("food-${++nextId}") },
            operationIdFactory = { "batch" },
        )

        val count = useCase(
            listOf(
                candidate("두부", selected = true, shelfLifeDays = 5),
                candidate("물티슈", selected = false, shelfLifeDays = null),
                candidate("새 식재료", selected = true, shelfLifeDays = null),
            ),
        )
        val saved = repository.foodItems.first()

        assertEquals(2, count)
        assertEquals(listOf("두부", "새 식재료"), saved.map { it.name })
        assertEquals(LocalDate.of(2026, 9, 6), saved[0].effectiveDate()?.value)
        assertEquals(FoodDateSource.APP_ESTIMATED, saved[0].effectiveDate()?.source)
        assertEquals(FoodStatus.NEEDS_REVIEW, saved[1].status)
        assertNull(saved[1].effectiveDate())
    }

    @Test
    fun `TC-UI-002 empty selection does not write anything`() = runBlocking {
        val repository = InMemoryFoodRepository()
        val useCase = SaveIntakeCandidatesUseCase(repository, fixedClock())

        val count = useCase(listOf(candidate("물티슈", selected = false, shelfLifeDays = null)))

        assertEquals(0, count)
        assertEquals(emptyList<Any>(), repository.foodItems.first())
    }

    private fun candidate(
        name: String,
        selected: Boolean,
        shelfLifeDays: Int?,
    ) = IntakeCandidate(
        id = "candidate-$name",
        draftId = "draft",
        originalName = name,
        normalizedName = name,
        quantity = 1,
        group = if (selected) IntakeCandidateGroup.MANAGE else IntakeCandidateGroup.EXCLUDED,
        isSelected = selected,
        reason = null,
        position = 0,
        storageLocation = StorageLocation.REFRIGERATED,
        estimatedShelfLifeDays = shelfLifeDays,
    )

    private fun fixedClock() = Clock.fixed(
        Instant.parse("2026-09-01T00:00:00Z"),
        ZoneOffset.UTC,
    )
}
