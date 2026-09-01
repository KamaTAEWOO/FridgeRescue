package com.portfolio.fridgerescue.core.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FoodItemTest {
    @Test
    fun `TC-DATE-004 earlier date is effective when no user-confirmed date exists`() {
        val foodItem = foodItem(
            dates = listOf(
                FoodDate(LocalDate.of(2026, 9, 5), FoodDateSource.MANUFACTURER_DISPLAYED),
                FoodDate(LocalDate.of(2026, 9, 3), FoodDateSource.APP_ESTIMATED),
            ),
        )

        assertEquals(LocalDate.of(2026, 9, 3), foodItem.effectiveDate()?.value)
        assertEquals(FoodDateSource.APP_ESTIMATED, foodItem.effectiveDate()?.source)
    }

    @Test
    fun `TC-DATE-009 user-confirmed date overrides automatic dates`() {
        val foodItem = foodItem(
            dates = listOf(
                FoodDate(LocalDate.of(2026, 9, 2), FoodDateSource.APP_ESTIMATED),
                FoodDate(LocalDate.of(2026, 9, 6), FoodDateSource.USER_CONFIRMED),
            ),
        )

        assertEquals(LocalDate.of(2026, 9, 6), foodItem.effectiveDate()?.value)
        assertEquals(FoodDateSource.USER_CONFIRMED, foodItem.effectiveDate()?.source)
    }

    @Test
    fun `TC-QTY-001 null quantity is allowed`() {
        val foodItem = foodItem(quantity = null)

        assertEquals(null, foodItem.quantity)
    }

    @Test
    fun `TC-QTY-002 zero quantity is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            foodItem(quantity = 0)
        }
    }

    private fun foodItem(
        quantity: Int? = 1,
        dates: List<FoodDate> = emptyList(),
    ) = FoodItem(
        id = FoodItemId("food-id"),
        name = "두부",
        quantity = quantity,
        storageLocation = StorageLocation.REFRIGERATED,
        dates = dates,
    )
}
