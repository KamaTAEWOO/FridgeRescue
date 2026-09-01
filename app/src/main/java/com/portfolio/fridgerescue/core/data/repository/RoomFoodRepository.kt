package com.portfolio.fridgerescue.core.data.repository

import androidx.room.withTransaction
import com.portfolio.fridgerescue.core.data.database.FoodEventDao
import com.portfolio.fridgerescue.core.data.database.FoodEventEntity
import com.portfolio.fridgerescue.core.data.database.FoodItemDao
import com.portfolio.fridgerescue.core.data.database.FridgeRescueDatabase
import com.portfolio.fridgerescue.core.data.database.toDomain
import com.portfolio.fridgerescue.core.data.database.toEntity
import com.portfolio.fridgerescue.core.model.FoodActionRequest
import com.portfolio.fridgerescue.core.model.FoodActionType
import com.portfolio.fridgerescue.core.model.FoodEvent
import com.portfolio.fridgerescue.core.model.FoodEventType
import com.portfolio.fridgerescue.core.model.FoodItem
import com.portfolio.fridgerescue.core.model.FoodItemId
import com.portfolio.fridgerescue.core.model.FoodMutationResult
import com.portfolio.fridgerescue.core.model.FoodStatus
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomFoodRepository(
    private val database: FridgeRescueDatabase,
    private val foodItemDao: FoodItemDao,
    private val foodEventDao: FoodEventDao,
    private val clock: Clock = Clock.systemUTC(),
) : FoodRepository {
    override val foodItems: Flow<List<FoodItem>> = foodItemDao.observeAll()
        .map { entities -> entities.map { it.toDomain() } }
    override val events: Flow<List<FoodEvent>> = foodEventDao.observeAll()
        .map { entities -> entities.map { it.toDomain() } }

    override suspend fun findById(id: FoodItemId): FoodItem? =
        foodItemDao.findById(id.value)?.toDomain()

    override suspend fun upsert(foodItem: FoodItem) {
        foodItemDao.upsert(foodItem.toEntity(clock.millis()))
    }

    override suspend fun save(foodItem: FoodItem, operationId: String): FoodMutationResult =
        database.withTransaction {
            duplicate(operationId)?.let { return@withTransaction it }
            val existing = foodItemDao.findById(foodItem.id.value)?.toDomain()
            foodItemDao.upsert(foodItem.toEntity(clock.millis()))
            insertEvent(
                operationId = operationId,
                foodItemId = foodItem.id,
                type = if (existing == null) FoodEventType.CREATED else FoodEventType.UPDATED,
                previousStatus = existing?.status,
                newStatus = foodItem.status,
            )
        }

    override suspend fun saveAll(foodItems: List<FoodItem>, operationId: String): Int =
        database.withTransaction {
            var savedCount = 0
            foodItems.forEach { foodItem ->
                val itemOperationId = "$operationId:${foodItem.id.value}"
                if (foodEventDao.findByOperationId(itemOperationId) == null) {
                    val existing = foodItemDao.findById(foodItem.id.value)?.toDomain()
                    foodItemDao.upsert(foodItem.toEntity(clock.millis()))
                    insertEvent(
                        operationId = itemOperationId,
                        foodItemId = foodItem.id,
                        type = if (existing == null) FoodEventType.CREATED else FoodEventType.UPDATED,
                        previousStatus = existing?.status,
                        newStatus = foodItem.status,
                    )
                    savedCount++
                }
            }
            savedCount
        }

    override suspend fun performAction(request: FoodActionRequest): FoodMutationResult =
        database.withTransaction {
            duplicate(request.operationId)?.let { return@withTransaction it }
            val current = foodItemDao.findById(request.foodItemId.value)?.toDomain()
                ?: return@withTransaction FoodMutationResult.NotFound
            if (current.isFinalized) return@withTransaction FoodMutationResult.AlreadyFinalized

            val newStatus = when (request.type) {
                FoodActionType.CONSUME -> FoodStatus.CONSUMED
                FoodActionType.DISCARD -> FoodStatus.DISCARDED
                FoodActionType.STILL_HERE,
                FoodActionType.PARTIALLY_USE,
                -> current.status
            }
            val eventType = when (request.type) {
                FoodActionType.CONSUME -> FoodEventType.CONSUMED
                FoodActionType.STILL_HERE -> FoodEventType.STILL_HERE
                FoodActionType.PARTIALLY_USE -> FoodEventType.PARTIALLY_USED
                FoodActionType.DISCARD -> FoodEventType.DISCARDED
            }
            if (newStatus != current.status) {
                foodItemDao.upsert(current.copy(status = newStatus).toEntity(clock.millis()))
            }
            insertEvent(
                operationId = request.operationId,
                foodItemId = current.id,
                type = eventType,
                previousStatus = current.status,
                newStatus = newStatus,
                discardReason = request.discardReason?.trim()?.takeIf { it.isNotEmpty() },
            )
        }

    override suspend fun undo(eventId: String, operationId: String): FoodMutationResult =
        database.withTransaction {
            duplicate(operationId)?.let { return@withTransaction it }
            val target = foodEventDao.findById(eventId)
                ?: return@withTransaction FoodMutationResult.NotFound
            if (target.type !in REVERSIBLE_EVENT_TYPES || foodEventDao.findUndoFor(eventId) != null) {
                return@withTransaction FoodMutationResult.Conflict
            }
            val latest = foodEventDao.findLatestForFood(target.foodItemId)
            if (latest?.eventId != target.eventId) return@withTransaction FoodMutationResult.Conflict
            val current = foodItemDao.findById(target.foodItemId)?.toDomain()
                ?: return@withTransaction FoodMutationResult.NotFound
            val restoredStatus = target.previousStatus?.let(::statusOf) ?: FoodStatus.ACTIVE
            if (target.newStatus?.let(::statusOf) != current.status) {
                return@withTransaction FoodMutationResult.Conflict
            }
            foodItemDao.upsert(current.copy(status = restoredStatus).toEntity(clock.millis()))
            insertEvent(
                operationId = operationId,
                foodItemId = current.id,
                type = FoodEventType.UNDONE,
                previousStatus = current.status,
                newStatus = restoredStatus,
                revertsEventId = target.eventId,
            )
        }

    override fun observeEvents(foodItemId: FoodItemId): Flow<List<FoodEvent>> =
        foodEventDao.observeForFood(foodItemId.value)
            .map { events -> events.map { it.toDomain() } }

    private suspend fun duplicate(operationId: String): FoodMutationResult.Duplicate? =
        foodEventDao.findByOperationId(operationId)?.toDomain()?.let(FoodMutationResult::Duplicate)

    private suspend fun insertEvent(
        operationId: String,
        foodItemId: FoodItemId,
        type: FoodEventType,
        previousStatus: FoodStatus?,
        newStatus: FoodStatus?,
        discardReason: String? = null,
        revertsEventId: String? = null,
    ): FoodMutationResult.Applied {
        val entity = FoodEventEntity(
            eventId = UUID.randomUUID().toString(),
            operationId = operationId,
            foodItemId = foodItemId.value,
            type = type.name,
            previousStatus = previousStatus?.name,
            newStatus = newStatus?.name,
            discardReason = discardReason,
            occurredAtEpochMillis = maxOf(
                clock.millis(),
                (foodEventDao.latestOccurredAt() ?: Long.MIN_VALUE).let { latest ->
                    if (latest == Long.MAX_VALUE) latest else latest + 1
                },
            ),
            revertsEventId = revertsEventId,
        )
        foodEventDao.insert(entity)
        return FoodMutationResult.Applied(entity.toDomain())
    }

    private fun statusOf(value: String): FoodStatus =
        enumValues<FoodStatus>().firstOrNull { it.name == value } ?: FoodStatus.NEEDS_REVIEW

    private companion object {
        val REVERSIBLE_EVENT_TYPES = setOf(
            FoodEventType.CONSUMED.name,
            FoodEventType.STILL_HERE.name,
            FoodEventType.PARTIALLY_USED.name,
            FoodEventType.DISCARDED.name,
        )
    }
}
