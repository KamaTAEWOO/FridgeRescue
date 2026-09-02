package com.portfolio.fridgerescue.feature.rescue.domain

import com.portfolio.fridgerescue.core.testing.InMemoryFoodRepository
import com.portfolio.fridgerescue.core.domain.model.FoodDate
import com.portfolio.fridgerescue.core.domain.model.FoodDateSource
import com.portfolio.fridgerescue.core.domain.model.FoodItem
import com.portfolio.fridgerescue.core.domain.model.FoodItemId
import com.portfolio.fridgerescue.core.domain.model.FoodStatus
import com.portfolio.fridgerescue.core.domain.model.StorageLocation
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveFoodItemUseCaseTest {
    private val newId = FoodItemId("new-food-id")

    @Test
    fun `TC-EDITOR-001 blank name is rejected without saving`() = runBlocking {
        val repository = InMemoryFoodRepository()
        val saveFoodItem = SaveFoodItemUseCase(repository) { newId }

        val result = saveFoodItem(draft(name = "   "))

        assertEquals(
            SaveFoodItemResult.Invalid(FoodItemDraftError.NAME_REQUIRED),
            result,
        )
        assertTrue(repository.foodItems.first().isEmpty())
    }

    @Test
    fun `TC-EDITOR-002 non-positive or non-numeric quantity is rejected`() = runBlocking {
        val repository = InMemoryFoodRepository()
        val saveFoodItem = SaveFoodItemUseCase(repository) { newId }

        val zero = saveFoodItem(draft(quantity = "0"))
        val text = saveFoodItem(draft(quantity = "한 개"))

        assertEquals(SaveFoodItemResult.Invalid(FoodItemDraftError.INVALID_QUANTITY), zero)
        assertEquals(SaveFoodItemResult.Invalid(FoodItemDraftError.INVALID_QUANTITY), text)
        assertTrue(repository.foodItems.first().isEmpty())
    }

    @Test
    fun `TC-EDITOR-003 malformed date is rejected`() = runBlocking {
        val repository = InMemoryFoodRepository()
        val saveFoodItem = SaveFoodItemUseCase(repository) { newId }

        val result = saveFoodItem(draft(date = "2026-02-30"))

        assertEquals(SaveFoodItemResult.Invalid(FoodItemDraftError.INVALID_DATE), result)
        assertTrue(repository.foodItems.first().isEmpty())
    }

    @Test
    fun `TC-EDITOR-004 food without date is saved as needs review`() = runBlocking {
        val repository = InMemoryFoodRepository()
        val saveFoodItem = SaveFoodItemUseCase(repository) { newId }

        val result = saveFoodItem(draft(date = "", quantity = "")) as SaveFoodItemResult.Saved
        val saved = result.foodItem

        assertTrue(result.isNew)
        assertEquals(FoodStatus.NEEDS_REVIEW, saved.status)
        assertNull(saved.quantity)
        assertNull(saved.effectiveDate())
    }

    @Test
    fun `TC-EDITOR-005 entered date is stored as user confirmed`() = runBlocking {
        val repository = InMemoryFoodRepository()
        val saveFoodItem = SaveFoodItemUseCase(repository) { newId }

        val result = saveFoodItem(
            draft(date = "2026-09-03", quantity = "2"),
        ) as SaveFoodItemResult.Saved
        val saved = result.foodItem

        assertEquals(2, saved.quantity)
        assertEquals(FoodStatus.ACTIVE, saved.status)
        assertEquals(LocalDate.of(2026, 9, 3), saved.effectiveDate()?.value)
        assertEquals(FoodDateSource.USER_CONFIRMED, saved.effectiveDate()?.source)
    }

    @Test
    fun `TC-EDITOR-006 editing preserves identity and original date evidence`() = runBlocking {
        val original = FoodItem(
            id = FoodItemId("existing-id"),
            name = "두부",
            quantity = 1,
            storageLocation = StorageLocation.REFRIGERATED,
            dates = listOf(
                FoodDate(
                    LocalDate.of(2026, 9, 5),
                    FoodDateSource.MANUFACTURER_DISPLAYED,
                ),
            ),
        )
        val repository = InMemoryFoodRepository(listOf(original))
        val saveFoodItem = SaveFoodItemUseCase(repository) { newId }

        val result = saveFoodItem(
            draft(
                id = original.id,
                name = "부침용 두부",
                date = "2026-09-04",
                storage = StorageLocation.FROZEN,
                isOpened = true,
                isPinned = true,
            ),
        ) as SaveFoodItemResult.Saved
        val saved = result.foodItem

        assertEquals(original.id, saved.id)
        assertEquals("부침용 두부", saved.name)
        assertEquals(StorageLocation.FROZEN, saved.storageLocation)
        assertTrue(saved.isOpened)
        assertTrue(saved.isPinned)
        assertTrue(saved.dates.any { it.source == FoodDateSource.MANUFACTURER_DISPLAYED })
        assertEquals(FoodDateSource.USER_CONFIRMED, saved.effectiveDate()?.source)
    }

    private fun draft(
        id: FoodItemId? = null,
        name: String = "두부",
        quantity: String = "1",
        date: String = "2026-09-03",
        storage: StorageLocation = StorageLocation.REFRIGERATED,
        isOpened: Boolean = false,
        isPinned: Boolean = false,
    ) = FoodItemDraft(
        foodItemId = id,
        nameInput = name,
        quantityInput = quantity,
        dateInput = date,
        storageLocation = storage,
        isOpened = isOpened,
        isPinned = isPinned,
    )
}
