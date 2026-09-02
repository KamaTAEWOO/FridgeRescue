package com.portfolio.fridgerescue.debug

import androidx.room.withTransaction
import com.portfolio.fridgerescue.core.data.local.database.FridgeRescueDatabase
import java.time.LocalDate

data class DemoSeedResult(
    val foodItemCount: Int,
    val eventCount: Int,
)

class DemoDataSeeder(
    private val database: FridgeRescueDatabase,
) {
    suspend fun replace(today: LocalDate = LocalDate.now()): DemoSeedResult {
        val dataSet = DemoDataFixtures.create(today)
        database.withTransaction {
            val writableDatabase = database.openHelper.writableDatabase
            writableDatabase.execSQL(
                "DELETE FROM food_events WHERE event_id LIKE ? OR food_item_id LIKE ?",
                arrayOf("${DemoDataFixtures.ID_PREFIX}%", "${DemoDataFixtures.ID_PREFIX}%"),
            )
            writableDatabase.execSQL(
                "DELETE FROM food_items WHERE id LIKE ?",
                arrayOf("${DemoDataFixtures.ID_PREFIX}%"),
            )
            database.foodItemDao().upsertAll(dataSet.foodItems)
            for (event in dataSet.events) {
                database.foodEventDao().insert(event)
            }
        }
        return DemoSeedResult(dataSet.foodItems.size, dataSet.events.size)
    }
}
