package com.portfolio.fridgerescue.core.data.datasource.local

import com.portfolio.fridgerescue.core.model.FoodDate
import com.portfolio.fridgerescue.core.model.FoodDateSource
import com.portfolio.fridgerescue.core.model.FoodItem
import com.portfolio.fridgerescue.core.model.FoodItemId
import com.portfolio.fridgerescue.core.model.FoodStatus
import com.portfolio.fridgerescue.core.model.StorageLocation
import java.time.LocalDate

object DemoFoodItems {
    fun create(today: LocalDate): List<FoodItem> = listOf(
        FoodItem(
            id = FoodItemId("demo-spinach"),
            name = "시금치",
            quantity = 1,
            storageLocation = StorageLocation.REFRIGERATED,
            dates = listOf(FoodDate(today, FoodDateSource.APP_ESTIMATED)),
            isOpened = true,
        ),
        FoodItem(
            id = FoodItemId("demo-tofu"),
            name = "찌개용 두부",
            quantity = 2,
            storageLocation = StorageLocation.REFRIGERATED,
            dates = listOf(FoodDate(today.plusDays(1), FoodDateSource.MANUFACTURER_DISPLAYED)),
            isPinned = true,
        ),
        FoodItem(
            id = FoodItemId("demo-milk"),
            name = "저지방 우유",
            quantity = 1,
            storageLocation = StorageLocation.REFRIGERATED,
            dates = listOf(FoodDate(today.plusDays(3), FoodDateSource.APP_ESTIMATED)),
        ),
        FoodItem(
            id = FoodItemId("demo-dumplings"),
            name = "냉동 만두",
            storageLocation = StorageLocation.FROZEN,
            status = FoodStatus.NEEDS_REVIEW,
        ),
    )
}
