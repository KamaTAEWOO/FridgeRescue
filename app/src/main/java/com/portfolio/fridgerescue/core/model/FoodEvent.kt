package com.portfolio.fridgerescue.core.model

import java.time.Instant

enum class FoodEventType {
    CREATED,
    UPDATED,
    CONSUMED,
    STILL_HERE,
    PARTIALLY_USED,
    DISCARDED,
    UNDONE,
}

data class FoodEvent(
    val id: String,
    val operationId: String,
    val foodItemId: FoodItemId,
    val type: FoodEventType,
    val previousStatus: FoodStatus?,
    val newStatus: FoodStatus?,
    val discardReason: String? = null,
    val occurredAt: Instant,
    val revertsEventId: String? = null,
)

enum class FoodActionType {
    CONSUME,
    STILL_HERE,
    PARTIALLY_USE,
    DISCARD,
}

data class FoodActionRequest(
    val foodItemId: FoodItemId,
    val type: FoodActionType,
    val operationId: String,
    val discardReason: String? = null,
)

sealed interface FoodMutationResult {
    data class Applied(val event: FoodEvent) : FoodMutationResult
    data class Duplicate(val event: FoodEvent) : FoodMutationResult
    data object NotFound : FoodMutationResult
    data object AlreadyFinalized : FoodMutationResult
    data object Conflict : FoodMutationResult
}
