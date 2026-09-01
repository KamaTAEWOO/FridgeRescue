package com.portfolio.fridgerescue.core.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.portfolio.fridgerescue.core.data.repository.RoomFoodRepository
import com.portfolio.fridgerescue.core.model.FoodActionRequest
import com.portfolio.fridgerescue.core.model.FoodActionType
import com.portfolio.fridgerescue.core.model.FoodDate
import com.portfolio.fridgerescue.core.model.FoodDateSource
import com.portfolio.fridgerescue.core.model.FoodItem
import com.portfolio.fridgerescue.core.model.FoodItemId
import com.portfolio.fridgerescue.core.model.FoodEventType
import com.portfolio.fridgerescue.core.model.FoodMutationResult
import com.portfolio.fridgerescue.core.model.FoodStatus
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
import org.junit.Assert.assertTrue
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
            database = database,
            foodItemDao = database.foodItemDao(),
            foodEventDao = database.foodEventDao(),
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

    @Test
    fun TC_ACTION_001_consume_is_stored_with_event_and_can_be_undone() = runBlocking {
        val original = foodItem()
        repository.upsert(original)

        val result = repository.performAction(
            FoodActionRequest(original.id, FoodActionType.CONSUME, "consume-once"),
        ) as FoodMutationResult.Applied

        assertEquals(FoodStatus.CONSUMED, repository.findById(original.id)?.status)
        assertEquals(FoodEventType.CONSUMED, result.event.type)
        assertEquals(result.event, repository.observeEvents(original.id).first().single())

        val undo = repository.undo(result.event.id, "undo-once")

        assertTrue(undo is FoodMutationResult.Applied)
        assertEquals(FoodStatus.ACTIVE, repository.findById(original.id)?.status)
        assertEquals(
            listOf(FoodEventType.UNDONE, FoodEventType.CONSUMED),
            repository.observeEvents(original.id).first().map { it.type },
        )
    }

    @Test
    fun TC_ACTION_004_same_operation_id_is_applied_only_once() = runBlocking {
        val original = foodItem()
        repository.upsert(original)
        val request = FoodActionRequest(original.id, FoodActionType.CONSUME, "same-operation")

        val first = repository.performAction(request)
        val second = repository.performAction(request)

        assertTrue(first is FoodMutationResult.Applied)
        assertTrue(second is FoodMutationResult.Duplicate)
        assertEquals(1, repository.observeEvents(original.id).first().size)
    }

    @Test
    fun TC_ACTION_009_discard_reason_can_be_empty() = runBlocking {
        val original = foodItem()
        repository.upsert(original)

        val result = repository.performAction(
            FoodActionRequest(original.id, FoodActionType.DISCARD, "discard", "   "),
        ) as FoodMutationResult.Applied

        assertEquals(FoodStatus.DISCARDED, repository.findById(original.id)?.status)
        assertNull(result.event.discardReason)
    }

    @Test
    fun TC_DATA_015_batch_save_is_idempotent_and_records_each_created_event() = runBlocking {
        val firstItem = foodItem()
        val secondItem = firstItem.copy(id = FoodItemId("milk-id"), name = "우유")

        val firstCount = repository.saveAll(listOf(firstItem, secondItem), "batch-operation")
        val duplicateCount = repository.saveAll(listOf(firstItem, secondItem), "batch-operation")

        assertEquals(2, firstCount)
        assertEquals(0, duplicateCount)
        assertEquals(2, repository.foodItems.first().size)
        assertEquals(FoodEventType.CREATED, repository.observeEvents(firstItem.id).first().single().type)
        assertEquals(FoodEventType.CREATED, repository.observeEvents(secondItem.id).first().single().type)
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
