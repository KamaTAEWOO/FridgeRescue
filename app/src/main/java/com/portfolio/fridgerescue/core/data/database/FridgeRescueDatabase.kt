package com.portfolio.fridgerescue.core.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FoodItemEntity::class, FoodEventEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class FridgeRescueDatabase : RoomDatabase() {
    abstract fun foodItemDao(): FoodItemDao
    abstract fun foodEventDao(): FoodEventDao

    companion object {
        private const val DATABASE_NAME = "fridge-rescue.db"

        fun create(context: Context): FridgeRescueDatabase = Room.databaseBuilder(
            context = context.applicationContext,
            klass = FridgeRescueDatabase::class.java,
            name = DATABASE_NAME,
        ).addMigrations(MIGRATION_1_2).build()

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `food_events` (
                        `event_id` TEXT NOT NULL,
                        `operation_id` TEXT NOT NULL,
                        `food_item_id` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `previous_status` TEXT,
                        `new_status` TEXT,
                        `discard_reason` TEXT,
                        `occurred_at_epoch_millis` INTEGER NOT NULL,
                        `reverts_event_id` TEXT,
                        PRIMARY KEY(`event_id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_food_events_operation_id` " +
                        "ON `food_events` (`operation_id`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_food_events_food_item_id` " +
                        "ON `food_events` (`food_item_id`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_food_events_reverts_event_id` " +
                        "ON `food_events` (`reverts_event_id`)",
                )
            }
        }
    }
}
