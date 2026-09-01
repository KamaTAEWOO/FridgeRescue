package com.portfolio.fridgerescue.core.testing

import com.portfolio.fridgerescue.core.data.repository.FoodRepository
import com.portfolio.fridgerescue.core.model.FoodActionRequest
import com.portfolio.fridgerescue.core.model.FoodActionType
import com.portfolio.fridgerescue.core.model.FoodEvent
import com.portfolio.fridgerescue.core.model.FoodEventType
import com.portfolio.fridgerescue.core.model.FoodItem
import com.portfolio.fridgerescue.core.model.FoodItemId
import com.portfolio.fridgerescue.core.model.FoodMutationResult
import com.portfolio.fridgerescue.core.model.FoodStatus
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryFoodRepository(
    initialItems: List<FoodItem> = emptyList(),
) : FoodRepository {
    private val items = MutableStateFlow(initialItems)
    private val events = MutableStateFlow<List<FoodEvent>>(emptyList())
    private val mutex = Mutex()

    override val foodItems: Flow<List<FoodItem>> = items.asStateFlow()

    override suspend fun findById(id: FoodItemId): FoodItem? =
        items.value.firstOrNull { it.id == id }

    override suspend fun upsert(foodItem: FoodItem) {
        setItem(foodItem)
    }

    override suspend fun save(foodItem: FoodItem, operationId: String): FoodMutationResult =
        mutex.withLock {
            duplicate(operationId)?.let { return@withLock it }
            val previous = findById(foodItem.id)
            setItem(foodItem)
            addEvent(
                operationId = operationId,
                foodItemId = foodItem.id,
                type = if (previous == null) FoodEventType.CREATED else FoodEventType.UPDATED,
                previousStatus = previous?.status,
                newStatus = foodItem.status,
            )
        }

    override suspend fun performAction(request: FoodActionRequest): FoodMutationResult =
        mutex.withLock {
            duplicate(request.operationId)?.let { return@withLock it }
            val current = findById(request.foodItemId) ?: return@withLock FoodMutationResult.NotFound
            if (current.isFinalized) return@withLock FoodMutationResult.AlreadyFinalized
            val newStatus = when (request.type) {
                FoodActionType.CONSUME -> FoodStatus.CONSUMED
                FoodActionType.DISCARD -> FoodStatus.DISCARDED
                FoodActionType.STILL_HERE, FoodActionType.PARTIALLY_USE -> current.status
            }
            setItem(current.copy(status = newStatus))
            addEvent(
                operationId = request.operationId,
                foodItemId = current.id,
                type = when (request.type) {
                    FoodActionType.CONSUME -> FoodEventType.CONSUMED
                    FoodActionType.DISCARD -> FoodEventType.DISCARDED
                    FoodActionType.STILL_HERE -> FoodEventType.STILL_HERE
                    FoodActionType.PARTIALLY_USE -> FoodEventType.PARTIALLY_USED
                },
                previousStatus = current.status,
                newStatus = newStatus,
                discardReason = request.discardReason?.trim()?.takeIf(String::isNotEmpty),
            )
        }

    override suspend fun undo(eventId: String, operationId: String): FoodMutationResult =
        mutex.withLock {
            duplicate(operationId)?.let { return@withLock it }
            val target = events.value.firstOrNull { it.id == eventId }
                ?: return@withLock FoodMutationResult.NotFound
            if (target.type !in REVERSIBLE_TYPES || events.value.any { it.revertsEventId == eventId }) {
                return@withLock FoodMutationResult.Conflict
            }
            if (events.value.lastOrNull { it.foodItemId == target.foodItemId }?.id != eventId) {
                return@withLock FoodMutationResult.Conflict
            }
            val current = findById(target.foodItemId) ?: return@withLock FoodMutationResult.NotFound
            val restored = target.previousStatus ?: FoodStatus.ACTIVE
            if (target.newStatus != current.status) return@withLock FoodMutationResult.Conflict
            setItem(current.copy(status = restored))
            addEvent(
                operationId = operationId,
                foodItemId = current.id,
                type = FoodEventType.UNDONE,
                previousStatus = current.status,
                newStatus = restored,
                revertsEventId = eventId,
            )
        }

    override fun observeEvents(foodItemId: FoodItemId): Flow<List<FoodEvent>> =
        events.map { all -> all.filter { it.foodItemId == foodItemId }.asReversed() }

    private fun setItem(foodItem: FoodItem) {
        items.update { currentItems ->
            val currentIndex = currentItems.indexOfFirst { it.id == foodItem.id }
            if (currentIndex == -1) {
                currentItems + foodItem
            } else {
                currentItems.toMutableList().apply {
                    set(currentIndex, foodItem)
                }
            }
        }
    }

    private fun duplicate(operationId: String): FoodMutationResult.Duplicate? =
        events.value.firstOrNull { it.operationId == operationId }
            ?.let(FoodMutationResult::Duplicate)

    private fun addEvent(
        operationId: String,
        foodItemId: FoodItemId,
        type: FoodEventType,
        previousStatus: FoodStatus?,
        newStatus: FoodStatus?,
        discardReason: String? = null,
        revertsEventId: String? = null,
    ): FoodMutationResult.Applied {
        val event = FoodEvent(
            id = UUID.randomUUID().toString(),
            operationId = operationId,
            foodItemId = foodItemId,
            type = type,
            previousStatus = previousStatus,
            newStatus = newStatus,
            discardReason = discardReason,
            occurredAt = Instant.now(),
            revertsEventId = revertsEventId,
        )
        events.update { it + event }
        return FoodMutationResult.Applied(event)
    }

    private companion object {
        val REVERSIBLE_TYPES = setOf(
            FoodEventType.CONSUMED,
            FoodEventType.STILL_HERE,
            FoodEventType.PARTIALLY_USED,
            FoodEventType.DISCARDED,
        )
    }
}
