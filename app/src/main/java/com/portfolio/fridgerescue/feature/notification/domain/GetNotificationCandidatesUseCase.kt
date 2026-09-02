package com.portfolio.fridgerescue.feature.notification.domain

import com.portfolio.fridgerescue.core.domain.model.FoodItem
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class GetNotificationCandidatesUseCase(
    private val reminderDays: Set<Long> = setOf(3, 1, 0),
) {
    operator fun invoke(foodItems: List<FoodItem>, today: LocalDate): List<FoodItem> = foodItems
        .asSequence()
        .filterNot(FoodItem::isFinalized)
        .filter { foodItem ->
            val date = foodItem.effectiveDate()?.value ?: return@filter false
            val daysRemaining = ChronoUnit.DAYS.between(today, date)
            daysRemaining <= 0 || daysRemaining in reminderDays
        }
        .sortedBy { it.effectiveDate()?.value }
        .toList()
}
