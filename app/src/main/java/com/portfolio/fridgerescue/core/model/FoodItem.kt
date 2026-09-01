package com.portfolio.fridgerescue.core.model

import java.time.LocalDate

@JvmInline
value class FoodItemId(val value: String) {
    init {
        require(value.isNotBlank()) { "FoodItemId must not be blank." }
    }
}

enum class FoodStatus {
    ACTIVE,
    NEEDS_REVIEW,
    CONSUMED,
    DISCARDED,
    ARCHIVED,
}

enum class StorageLocation {
    REFRIGERATED,
    FROZEN,
    ROOM_TEMPERATURE,
}

enum class FoodDateSource {
    MANUFACTURER_DISPLAYED,
    APP_ESTIMATED,
    USER_CONFIRMED,
}

data class FoodDate(
    val value: LocalDate,
    val source: FoodDateSource,
)

data class FoodItem(
    val id: FoodItemId,
    val name: String,
    val quantity: Int? = null,
    val storageLocation: StorageLocation,
    val dates: List<FoodDate> = emptyList(),
    val isOpened: Boolean = false,
    val isPinned: Boolean = false,
    val status: FoodStatus = FoodStatus.ACTIVE,
) {
    init {
        require(name.isNotBlank()) { "Food name must not be blank." }
        require(quantity == null || quantity > 0) { "Quantity must be null or greater than zero." }
    }

    val isFinalized: Boolean
        get() = status == FoodStatus.CONSUMED ||
            status == FoodStatus.DISCARDED ||
            status == FoodStatus.ARCHIVED

    fun effectiveDate(): FoodDate? {
        val userConfirmedDates = dates.filter { it.source == FoodDateSource.USER_CONFIRMED }
        return (userConfirmedDates.ifEmpty { dates })
            .minWithOrNull(compareBy<FoodDate> { it.value }.thenBy { it.source.ordinal })
    }
}
