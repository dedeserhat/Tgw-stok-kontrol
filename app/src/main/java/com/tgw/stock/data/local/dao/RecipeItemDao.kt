package com.tgw.stock.data.local.dao

import androidx.room.*
import com.tgw.stock.data.local.entities.RecipeItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeItemDao {
    @Query("SELECT * FROM recipe_items WHERE recipeId = :recipeId")
    fun observeForRecipe(recipeId: Long): Flow<List<RecipeItemEntity>>

    @Query("SELECT * FROM recipe_items WHERE recipeId = :recipeId")
    suspend fun getForRecipe(recipeId: Long): List<RecipeItemEntity>

    @Query("SELECT * FROM recipe_items WHERE productId = :productId")
    fun observeForProduct(productId: Long): Flow<List<RecipeItemEntity>>

    @Query("SELECT * FROM recipe_items WHERE productId = :productId")
    suspend fun getForProduct(productId: Long): List<RecipeItemEntity>

    @Query("SELECT * FROM recipe_items")
    suspend fun getAll(): List<RecipeItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RecipeItemEntity): Long

    @Update
    suspend fun update(item: RecipeItemEntity)

    @Delete
    suspend fun delete(item: RecipeItemEntity)

    @Query("DELETE FROM recipe_items WHERE isSample = 1")
    suspend fun deleteSample()
}
