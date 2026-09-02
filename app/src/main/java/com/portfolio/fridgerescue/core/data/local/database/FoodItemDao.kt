package com.portfolio.fridgerescue.core.data.local.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodItemDao {
    @Query("SELECT * FROM food_items")
    fun observeAll(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): FoodItemEntity?

    @Query("SELECT * FROM food_items")
    suspend fun loadAll(): List<FoodItemEntity>

    @Upsert
    suspend fun upsert(foodItem: FoodItemEntity)

    @Upsert
    suspend fun upsertAll(foodItems: List<FoodItemEntity>)
}
