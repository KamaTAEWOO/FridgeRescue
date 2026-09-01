package com.portfolio.fridgerescue.feature.rescue.domain

import com.portfolio.fridgerescue.core.model.FoodDateSource
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class EstimateConsumeDateUseCaseTest {
    private val estimateConsumeDate = EstimateConsumeDateUseCase()

    @Test
    fun `TC-DATE-001 calculated date is marked as app estimate`() {
        val result = estimateConsumeDate(
            purchasedOn = LocalDate.of(2026, 9, 1),
            shelfLifeDays = 3,
        )

        assertEquals(LocalDate.of(2026, 9, 4), result.value)
        assertEquals(FoodDateSource.APP_ESTIMATED, result.source)
    }

    @Test
    fun `TC-DATE-006 leap-year boundary includes February 29`() {
        val result = estimateConsumeDate(
            purchasedOn = LocalDate.of(2028, 2, 28),
            shelfLifeDays = 1,
        )

        assertEquals(LocalDate.of(2028, 2, 29), result.value)
    }

    @Test
    fun `TC-DATE-007 non-leap-year boundary moves to March`() {
        val result = estimateConsumeDate(
            purchasedOn = LocalDate.of(2027, 2, 28),
            shelfLifeDays = 1,
        )

        assertEquals(LocalDate.of(2027, 3, 1), result.value)
    }
}
