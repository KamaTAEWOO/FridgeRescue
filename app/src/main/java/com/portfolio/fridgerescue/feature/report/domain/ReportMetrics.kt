package com.portfolio.fridgerescue.feature.report.domain

import com.portfolio.fridgerescue.core.domain.model.FoodEvent
import com.portfolio.fridgerescue.core.domain.model.FoodEventType
import com.portfolio.fridgerescue.core.domain.model.FoodItem

data class ReportMetrics(
    val rescuedCount: Int = 0,
    val discardedCount: Int = 0,
    val shoppingHint: String? = null,
)

class GetReportMetricsUseCase {
    operator fun invoke(foodItems: List<FoodItem>, events: List<FoodEvent>): ReportMetrics {
        val revertedIds = events.mapNotNull(FoodEvent::revertsEventId).toSet()
        val effective = events.filterNot { it.id in revertedIds }
        val consumed = effective.filter { it.type == FoodEventType.CONSUMED }
        val discarded = effective.filter { it.type == FoodEventType.DISCARDED }
        val namesById = foodItems.associate { it.id to it.name }
        val mostDiscardedName = discarded
            .mapNotNull { namesById[it.foodItemId] }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.takeIf { it.value >= 2 }
            ?.key
        return ReportMetrics(
            rescuedCount = consumed.size,
            discardedCount = discarded.size,
            shoppingHint = mostDiscardedName?.let { "${it}은(는) 다음 장보기에서 수량을 줄여보세요." },
        )
    }
}
