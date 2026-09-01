package com.portfolio.fridgerescue.core.data.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    @After
    fun deleteDatabase() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun TC_DATA_008_migration_1_to_2_preserves_food_and_adds_event_table() {
        openHelper(version = 1).use { helper ->
            helper.writableDatabase.execSQL(
                """
                INSERT INTO food_items (
                    id, name, quantity, storage_location,
                    manufacturer_displayed_date, app_estimated_date, user_confirmed_date,
                    is_opened, is_pinned, status, updated_at_epoch_millis
                ) VALUES ('legacy', '두부', 1, 'REFRIGERATED', NULL, NULL, '2026-09-02',
                    0, 0, 'ACTIVE', 0)
                """.trimIndent(),
            )
        }

        openHelper(version = 2).use { helper ->
            val migrated = helper.writableDatabase
            assertEquals(1, migrated.count("SELECT * FROM food_items WHERE id = 'legacy'"))
            assertEquals(0, migrated.count("SELECT * FROM food_events"))
        }
    }

    @Test
    fun TC_DATA_013_migration_2_to_3_preserves_food_and_adds_intake_drafts() {
        openHelper(version = 2).use { helper ->
            helper.writableDatabase.execSQL(
                """
                INSERT INTO food_items (
                    id, name, quantity, storage_location,
                    manufacturer_displayed_date, app_estimated_date, user_confirmed_date,
                    is_opened, is_pinned, status, updated_at_epoch_millis
                ) VALUES ('v2-food', '우유', 1, 'REFRIGERATED', NULL, NULL, NULL,
                    0, 0, 'NEEDS_REVIEW', 0)
                """.trimIndent(),
            )
        }

        openHelper(version = 3).use { helper ->
            val migrated = helper.writableDatabase
            assertEquals(1, migrated.count("SELECT * FROM food_items WHERE id = 'v2-food'"))
            assertEquals(0, migrated.count("SELECT * FROM intake_drafts"))
        }
    }

    private fun openHelper(version: Int): SupportSQLiteOpenHelper =
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(TEST_DATABASE)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(version) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(CREATE_FOOD_ITEMS)
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) {
                            if (oldVersion < 2) FridgeRescueDatabase.MIGRATION_1_2.migrate(db)
                            if (oldVersion < 3) FridgeRescueDatabase.MIGRATION_2_3.migrate(db)
                        }
                    },
                )
                .build(),
        )

    private fun SupportSQLiteDatabase.count(query: String): Int =
        query(query).use { cursor -> cursor.count }

    private companion object {
        const val TEST_DATABASE = "migration-test.db"
        val CREATE_FOOD_ITEMS =
            """
            CREATE TABLE IF NOT EXISTS food_items (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                quantity INTEGER,
                storage_location TEXT NOT NULL,
                manufacturer_displayed_date TEXT,
                app_estimated_date TEXT,
                user_confirmed_date TEXT,
                is_opened INTEGER NOT NULL,
                is_pinned INTEGER NOT NULL,
                status TEXT NOT NULL,
                updated_at_epoch_millis INTEGER NOT NULL
            )
            """.trimIndent()
    }
}
