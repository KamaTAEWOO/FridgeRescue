package com.portfolio.fridgerescue.core.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.portfolio.fridgerescue.core.data.repository.RoomFoodRepository
import com.portfolio.fridgerescue.core.model.FoodDate
import com.portfolio.fridgerescue.core.model.FoodDateSource
import com.portfolio.fridgerescue.core.model.FoodItem
import com.portfolio.fridgerescue.core.model.FoodItemId
import com.portfolio.fridgerescue.core.model.StorageLocation
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoodItemDaoTest {
    private lateinit var database: FridgeRescueDatabase
    private lateinit var repository: RoomFoodRepository

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            FridgeRescueDatabase::class.java,
        ).build()
        repository = RoomFoodRepository(
            foodItemDao = database.foodItemDao(),
            clock = Clock.fixed(Instant.parse("2026-09-01T00:00:00Z"), ZoneOffset.UTC),
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun TC_DATA_010_room_round_trip_preserves_food_fields() = runBlocking {
        val original = foodItem()

        repository.upsert(original)
        val restored = repository.foodItems.first().single()

        assertEquals(original, restored)
    }

    @Test
    fun TC_DATA_011_upsert_with_same_id_updates_without_duplicate() = runBlocking {
        val original = foodItem()
        repository.upsert(original)

        repository.upsert(
            original.copy(
                name = "부침용 두부",
                storageLocation = StorageLocation.FROZEN,
            ),
        )
        val items = repository.foodItems.first()

        assertEquals(1, items.size)
        assertEquals("부침용 두부", items.single().name)
        assertEquals(StorageLocation.FROZEN, items.single().storageLocation)
    }

    @Test
    fun TC_DATA_012_unknown_id_returns_null() = runBlocking {
        assertNull(repository.findById(FoodItemId("missing")))
    }

    private fun foodItem() = FoodItem(
        id = FoodItemId("tofu-id"),
        name = "두부",
        quantity = 2,
        storageLocation = StorageLocation.REFRIGERATED,
        dates = listOf(
            FoodDate(LocalDate.of(2026, 9, 3), FoodDateSource.MANUFACTURER_DISPLAYED),
            FoodDate(LocalDate.of(2026, 9, 2), FoodDateSource.APP_ESTIMATED),
            FoodDate(LocalDate.of(2026, 9, 4), FoodDateSource.USER_CONFIRMED),
        ),
        isOpened = true,
        isPinned = true,
    )
}
