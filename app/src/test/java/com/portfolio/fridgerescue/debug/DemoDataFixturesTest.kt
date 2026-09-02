package com.portfolio.fridgerescue.debug

import com.portfolio.fridgerescue.core.domain.model.FoodEventType
import com.portfolio.fridgerescue.core.domain.model.FoodStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoDataFixturesTest {
    private val today = LocalDate.of(2026, 9, 2)

    @Test
    fun `fixture covers urgent safe and review states`() {
        val fixture = DemoDataFixtures.create(today)

        assertEquals(18, fixture.foodItems.size)
        assertTrue(fixture.foodItems.all { it.id.startsWith(DemoDataFixtures.ID_PREFIX) })
        assertTrue(fixture.foodItems.any { it.manufacturerDisplayedDate == today.minusDays(1).toString() })
        assertTrue(fixture.foodItems.any { it.appEstimatedDate == today.plusDays(5).toString() })
        assertTrue(fixture.foodItems.any { it.userConfirmedDate == today.plusDays(7).toString() })
        assertTrue(fixture.foodItems.any { it.status == FoodStatus.NEEDS_REVIEW.name })
        assertTrue(fixture.foodItems.any { it.storageLocation == "FROZEN" })
    }

    @Test
    fun `fixture creates useful report history`() {
        val fixture = DemoDataFixtures.create(today)

        assertEquals(7, fixture.events.size)
        assertEquals(4, fixture.events.count { it.type == FoodEventType.CONSUMED.name })
        assertEquals(3, fixture.events.count { it.type == FoodEventType.DISCARDED.name })
        assertEquals(
            2,
            fixture.events.count { event ->
                fixture.foodItems.first { it.id == event.foodItemId }.name == "양송이버섯" &&
                    event.type == FoodEventType.DISCARDED.name
            },
        )
    }
}
