package com.portfolio.fridgerescue.core.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodEventDao {
    @Query("SELECT * FROM food_events ORDER BY occurred_at_epoch_millis DESC, event_id DESC")
    fun observeAll(): Flow<List<FoodEventEntity>>

    @Query(
        "SELECT * FROM food_events WHERE food_item_id = :foodItemId " +
            "ORDER BY occurred_at_epoch_millis DESC, event_id DESC",
    )
    fun observeForFood(foodItemId: String): Flow<List<FoodEventEntity>>

    @Query("SELECT * FROM food_events WHERE event_id = :eventId LIMIT 1")
    suspend fun findById(eventId: String): FoodEventEntity?

    @Query("SELECT * FROM food_events WHERE operation_id = :operationId LIMIT 1")
    suspend fun findByOperationId(operationId: String): FoodEventEntity?

    @Query("SELECT * FROM food_events WHERE reverts_event_id = :eventId LIMIT 1")
    suspend fun findUndoFor(eventId: String): FoodEventEntity?

    @Query(
        "SELECT * FROM food_events WHERE food_item_id = :foodItemId " +
            "ORDER BY occurred_at_epoch_millis DESC, event_id DESC LIMIT 1",
    )
    suspend fun findLatestForFood(foodItemId: String): FoodEventEntity?

    @Query("SELECT MAX(occurred_at_epoch_millis) FROM food_events")
    suspend fun latestOccurredAt(): Long?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: FoodEventEntity)
}
