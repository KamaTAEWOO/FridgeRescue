package com.portfolio.fridgerescue.feature.intake.domain

import com.portfolio.fridgerescue.core.domain.model.FoodDateSource
import com.portfolio.fridgerescue.core.domain.model.FoodItemId
import com.portfolio.fridgerescue.core.domain.model.FoodStatus
import com.portfolio.fridgerescue.core.domain.model.IntakeCandidate
import com.portfolio.fridgerescue.core.domain.model.IntakeCandidateGroup
import com.portfolio.fridgerescue.core.domain.model.StorageLocation
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

    @Test
    fun TC_BARCODE_006_gs1_displayed_date_is_saved_without_estimation() = runBlocking {
        val repository = InMemoryFoodRepository()
        val useCase = SaveIntakeCandidatesUseCase(
            repository = repository,
            clock = fixedClock(),
            idFactory = { FoodItemId("barcode-food") },
            operationIdFactory = { "barcode-batch" },
        )

        useCase(
            listOf(
                candidate(
                    name = "우유",
                    selected = true,
                    shelfLifeDays = null,
                    displayedDate = LocalDate.of(2026, 9, 10),
                ),
            ),
        )

        val saved = repository.foodItems.first().single()
        assertEquals(LocalDate.of(2026, 9, 10), saved.effectiveDate()?.value)
        assertEquals(FoodDateSource.MANUFACTURER_DISPLAYED, saved.effectiveDate()?.source)
        assertEquals(FoodStatus.ACTIVE, saved.status)
    }

    private fun candidate(
        name: String,
        selected: Boolean,
        shelfLifeDays: Int?,
        displayedDate: LocalDate? = null,
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
        displayedDate = displayedDate,
    )

    private fun fixedClock() = Clock.fixed(
        Instant.parse("2026-09-01T00:00:00Z"),
        ZoneOffset.UTC,
    )
}
