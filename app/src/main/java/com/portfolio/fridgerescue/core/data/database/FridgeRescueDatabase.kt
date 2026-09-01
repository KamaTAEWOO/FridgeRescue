package com.portfolio.fridgerescue.core.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FoodItemEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class FridgeRescueDatabase : RoomDatabase() {
    abstract fun foodItemDao(): FoodItemDao

    companion object {
        private const val DATABASE_NAME = "fridge-rescue.db"

        fun create(context: Context): FridgeRescueDatabase = Room.databaseBuilder(
            context = context.applicationContext,
            klass = FridgeRescueDatabase::class.java,
            name = DATABASE_NAME,
        ).build()
    }
}
