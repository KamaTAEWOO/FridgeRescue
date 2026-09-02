package com.portfolio.fridgerescue.feature.rescue.domain

import com.portfolio.fridgerescue.core.domain.model.FoodDate
import com.portfolio.fridgerescue.core.domain.model.FoodDateSource
import java.time.LocalDate

class EstimateConsumeDateUseCase {
    operator fun invoke(
        purchasedOn: LocalDate,
        shelfLifeDays: Long,
    ): FoodDate {
        require(shelfLifeDays >= 0) { "Shelf life must not be negative." }
        return FoodDate(
            value = purchasedOn.plusDays(shelfLifeDays),
            source = FoodDateSource.APP_ESTIMATED,
        )
    }
}
