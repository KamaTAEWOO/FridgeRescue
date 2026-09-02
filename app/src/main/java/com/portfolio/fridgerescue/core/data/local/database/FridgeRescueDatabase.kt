package com.portfolio.fridgerescue.core.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FoodItemEntity::class,
        FoodEventEntity::class,
        IntakeDraftEntity::class,
        IntakeCandidateEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class FridgeRescueDatabase : RoomDatabase() {
    abstract fun foodItemDao(): FoodItemDao
    abstract fun foodEventDao(): FoodEventDao
    abstract fun intakeDraftDao(): IntakeDraftDao
    abstract fun intakeCandidateDao(): IntakeCandidateDao

    companion object {
        private const val DATABASE_NAME = "fridge-rescue.db"

        fun create(context: Context): FridgeRescueDatabase = Room.databaseBuilder(
            context = context.applicationContext,
            klass = FridgeRescueDatabase::class.java,
            name = DATABASE_NAME,
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `intake_drafts` (
                        `id` TEXT NOT NULL,
                        `content_type` TEXT,
                        `mime_type` TEXT,
                        `text_content` TEXT,
                        `cached_file_path` TEXT,
                        `status` TEXT NOT NULL,
                        `error_code` TEXT,
                        `created_at_epoch_millis` INTEGER NOT NULL,
                        `updated_at_epoch_millis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_intake_drafts_status_updated_at_epoch_millis` " +
                        "ON `intake_drafts` (`status`, `updated_at_epoch_millis`)",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `intake_candidates` (
                        `id` TEXT NOT NULL,
                        `draft_id` TEXT NOT NULL,
                        `original_name` TEXT NOT NULL,
                        `normalized_name` TEXT NOT NULL,
                        `quantity` INTEGER,
                        `candidate_group` TEXT NOT NULL,
                        `is_selected` INTEGER NOT NULL,
                        `reason` TEXT,
                        `position` INTEGER NOT NULL,
                        `storage_location` TEXT NOT NULL,
                        `estimated_shelf_life_days` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_intake_candidates_draft_id_position` " +
                        "ON `intake_candidates` (`draft_id`, `position`)",
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `intake_candidates` ADD COLUMN `displayed_date` TEXT")
            }
        }
    }
}
