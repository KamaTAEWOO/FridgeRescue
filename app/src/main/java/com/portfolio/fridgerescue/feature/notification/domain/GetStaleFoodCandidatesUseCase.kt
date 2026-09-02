package com.portfolio.fridgerescue.feature.notification.domain

import com.portfolio.fridgerescue.core.domain.model.FoodEvent
import com.portfolio.fridgerescue.core.domain.model.FoodItem
import com.portfolio.fridgerescue.core.domain.model.FoodStatus
import java.time.Duration
import java.time.Instant

class GetStaleFoodCandidatesUseCase(
    private val unansweredThreshold: Duration = Duration.ofDays(7),
) {
    operator fun invoke(
        foods: List<FoodItem>,
        events: List<FoodEvent>,
        now: Instant,
    ): List<FoodItem> {
        val lastEventByFood = events.groupBy(FoodEvent::foodItemId)
            .mapValues { (_, values) -> values.maxOf(FoodEvent::occurredAt) }
        val cutoff = now.minus(unansweredThreshold)
        return foods.filter { food ->
            food.status == FoodStatus.ACTIVE &&
                lastEventByFood[food.id]?.isBefore(cutoff) == true
        }
    }
}
