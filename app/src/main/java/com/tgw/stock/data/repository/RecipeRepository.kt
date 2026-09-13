package com.tgw.stock.data.repository

import com.tgw.stock.data.local.dao.ProductDao
import com.tgw.stock.data.local.dao.RecipeDao
import com.tgw.stock.data.local.dao.RecipeItemDao
import com.tgw.stock.data.local.entities.RecipeEntity
import com.tgw.stock.data.local.entities.RecipeItemEntity
import com.tgw.stock.domain.Money
import com.tgw.stock.domain.StockCalculations
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    fun observeAll(): Flow<List<RecipeEntity>>
    fun observeById(id: Long): Flow<RecipeEntity?>
    suspend fun getById(id: Long): RecipeEntity?
    suspend fun upsert(recipe: RecipeEntity): Long
    suspend fun delete(recipe: RecipeEntity)

    fun observeItems(recipeId: Long): Flow<List<RecipeItemEntity>>
    suspend fun getItems(recipeId: Long): List<RecipeItemEntity>
    suspend fun upsertItem(item: RecipeItemEntity): Long
    suspend fun deleteItem(item: RecipeItemEntity)

    suspend fun getCostSummary(recipeId: Long): RecipeCostSummary?
    suspend fun getAllCostSummaries(): List<RecipeCostSummary>
    suspend fun getUsageForProduct(productId: Long): List<RecipeUsage>
}

class RoomRecipeRepository(
    private val recipeDao: RecipeDao,
    private val recipeItemDao: RecipeItemDao,
    private val productDao: ProductDao
) : RecipeRepository {

    override fun observeAll(): Flow<List<RecipeEntity>> = recipeDao.observeAll()
    override fun observeById(id: Long): Flow<RecipeEntity?> = recipeDao.observeById(id)
    override suspend fun getById(id: Long): RecipeEntity? = recipeDao.getById(id)
    override suspend fun upsert(recipe: RecipeEntity): Long = recipeDao.insert(recipe)
    override suspend fun delete(recipe: RecipeEntity) = recipeDao.delete(recipe)

    override fun observeItems(recipeId: Long): Flow<List<RecipeItemEntity>> = recipeItemDao.observeForRecipe(recipeId)
    override suspend fun getItems(recipeId: Long): List<RecipeItemEntity> = recipeItemDao.getForRecipe(recipeId)
    override suspend fun upsertItem(item: RecipeItemEntity): Long = recipeItemDao.insert(item)
    override suspend fun deleteItem(item: RecipeItemEntity) = recipeItemDao.delete(item)

    override suspend fun getCostSummary(recipeId: Long): RecipeCostSummary? {
        val recipe = recipeDao.getById(recipeId) ?: return null
        val items = recipeItemDao.getForRecipe(recipeId)
        return buildSummary(recipe, items)
    }

    private suspend fun buildSummary(recipe: RecipeEntity, items: List<RecipeItemEntity>): RecipeCostSummary {
        val ingredientCosts = mutableListOf<Pair<Double, Long>>()
        val portionInputs = mutableListOf<Pair<Double, Double>>()
        val missing = mutableListOf<String>()

        for (item in items) {
            val product = productDao.getById(item.productId)
            if (product == null) {
                missing.add("Unknown ingredient")
                continue
            }
            ingredientCosts.add(item.quantity to product.avgCostPerUnitMinor)
            portionInputs.add(item.quantity to product.currentStock)
            if (product.currentStock <= 0.0) missing.add(product.name)
        }

        val cost = StockCalculations.recipeCost(ingredientCosts)
        val sellPrice = Money(recipe.sellPriceMinor)
        val foodCostPct = StockCalculations.foodCostPercent(cost, sellPrice)
        val profit = StockCalculations.estimatedProfit(cost, sellPrice)
        val portions = StockCalculations.portionsAvailable(portionInputs)

        return RecipeCostSummary(
            recipeId = recipe.id,
            recipeName = recipe.name,
            sellPriceMinor = recipe.sellPriceMinor,
            costMinor = cost.cents,
            foodCostPercent = foodCostPct,
            profitMinor = profit.cents,
            portionsAvailable = portions,
            missingIngredients = missing
        )
    }

    override suspend fun getAllCostSummaries(): List<RecipeCostSummary> {
        val recipes = recipeDao.getAll()
        return recipes.map { recipe -> buildSummary(recipe, recipeItemDao.getForRecipe(recipe.id)) }
    }

    override suspend fun getUsageForProduct(productId: Long): List<RecipeUsage> {
        val items = recipeItemDao.getForProduct(productId)
        return items.mapNotNull { item ->
            val recipe = recipeDao.getById(item.recipeId) ?: return@mapNotNull null
            RecipeUsage(recipe.id, recipe.name, item.quantity)
        }
    }
}
