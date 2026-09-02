package com.portfolio.fridgerescue.debug

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.portfolio.fridgerescue.core.data.local.database.FridgeRescueDatabase
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoDataSeederTest {
    private lateinit var database: FridgeRescueDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FridgeRescueDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun replace_isIdempotent() = runBlocking {
        val seeder = DemoDataSeeder(database)

        val first = seeder.replace(LocalDate.of(2026, 9, 2))
        val second = seeder.replace(LocalDate.of(2026, 9, 3))

        assertEquals(DemoSeedResult(foodItemCount = 18, eventCount = 7), first)
        assertEquals(first, second)
        assertEquals(18, database.foodItemDao().loadAll().size)
        assertEquals(7, database.foodEventDao().observeAll().first().size)
        assertEquals(
            "2026-09-04",
            database.foodItemDao().findById("demo-active-milk")?.manufacturerDisplayedDate,
        )
    }
}
