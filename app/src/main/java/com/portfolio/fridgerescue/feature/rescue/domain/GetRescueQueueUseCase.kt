package com.portfolio.fridgerescue.feature.rescue.domain

import com.portfolio.fridgerescue.core.domain.model.FoodDate
import com.portfolio.fridgerescue.core.domain.model.FoodItem
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class RescueUrgency {
    OVERDUE,
    TODAY,
    SOON,
    LATER,
    NEEDS_DATE,
}

data class RescueQueueItem(
    val foodItem: FoodItem,
    val effectiveDate: FoodDate?,
    val daysRemaining: Long?,
    val urgency: RescueUrgency,
)

class GetRescueQueueUseCase {
    operator fun invoke(
        foodItems: List<FoodItem>,
        today: LocalDate,
    ): List<RescueQueueItem> = foodItems
        .asSequence()
        .filterNot(FoodItem::isFinalized)
        .map { foodItem -> foodItem.toQueueItem(today) }
        .sortedWith(
            compareBy<RescueQueueItem> { it.urgency.ordinal }
                .thenBy { it.daysRemaining ?: Long.MAX_VALUE }
                .thenByDescending { it.foodItem.isOpened }
                .thenByDescending { it.foodItem.isPinned }
                .thenBy { it.foodItem.name },
        )
        .toList()

    private fun FoodItem.toQueueItem(today: LocalDate): RescueQueueItem {
        val effectiveDate = effectiveDate()
        val daysRemaining = effectiveDate?.let { ChronoUnit.DAYS.between(today, it.value) }
        val urgency = when {
            daysRemaining == null -> RescueUrgency.NEEDS_DATE
            daysRemaining < 0 -> RescueUrgency.OVERDUE
            daysRemaining == 0L -> RescueUrgency.TODAY
            daysRemaining <= 3 -> RescueUrgency.SOON
            else -> RescueUrgency.LATER
        }

        return RescueQueueItem(
            foodItem = this,
            effectiveDate = effectiveDate,
            daysRemaining = daysRemaining,
            urgency = urgency,
        )
    }
}
