package com.tgw.stock.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgw.stock.data.local.entities.ProductEntity
import com.tgw.stock.data.local.entities.RecipeEntity
import com.tgw.stock.data.local.entities.RecipeItemEntity
import com.tgw.stock.data.repository.ProductRepository
import com.tgw.stock.data.repository.RecipeCostSummary
import com.tgw.stock.data.repository.RecipeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RecipeItemRow(
    val itemId: Long,
    val productId: Long,
    val productName: String,
    val unit: String,
    val quantity: Double
)

class RecipeDetailViewModel(
    private val recipeId: Long,
    private val recipeRepository: RecipeRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _recipe = MutableStateFlow<RecipeEntity?>(null)
    val recipe: StateFlow<RecipeEntity?> = _recipe.asStateFlow()

    private val _rows = MutableStateFlow<List<RecipeItemRow>>(emptyList())
    val rows: StateFlow<List<RecipeItemRow>> = _rows.asStateFlow()

    private val _summary = MutableStateFlow<RecipeCostSummary?>(null)
    val summary: StateFlow<RecipeCostSummary?> = _summary.asStateFlow()

    val allProducts = MutableStateFlow<List<ProductEntity>>(emptyList())

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    init {
        viewModelScope.launch {
            allProducts.value = productRepository.getAll()
            recipeRepository.observeById(recipeId).collect { recipe ->
                _recipe.value = recipe
                if (recipe == null) {
                    _deleted.value = true
                } else {
                    reloadItemsAndSummary()
                }
            }
        }
    }

    private suspend fun reloadItemsAndSummary() {
        val items = recipeRepository.getItems(recipeId)
        val productsById = productRepository.getAll().associateBy { it.id }
        _rows.value = items.mapNotNull { item ->
            val product = productsById[item.productId] ?: return@mapNotNull null
            RecipeItemRow(item.id, item.productId, product.name, product.unit, item.quantity)
        }.sortedBy { it.productName }
        _summary.value = recipeRepository.getCostSummary(recipeId)
    }

    fun updateSellPrice(sellPriceMinor: Long) {
        viewModelScope.launch {
            _recipe.value?.let { recipeRepository.upsert(it.copy(sellPriceMinor = sellPriceMinor)) }
            reloadItemsAndSummary()
        }
    }

    fun addOrUpdateIngredient(productId: Long, quantity: Double, existingItemId: Long? = null) {
        viewModelScope.launch {
            recipeRepository.upsertItem(
                RecipeItemEntity(id = existingItemId ?: 0, recipeId = recipeId, productId = productId, quantity = quantity)
            )
            reloadItemsAndSummary()
        }
    }

    fun deleteIngredient(row: RecipeItemRow) {
        viewModelScope.launch {
            recipeRepository.deleteItem(RecipeItemEntity(id = row.itemId, recipeId = recipeId, productId = row.productId, quantity = row.quantity))
            reloadItemsAndSummary()
        }
    }

    fun deleteRecipe() {
        viewModelScope.launch {
            _recipe.value?.let { recipeRepository.delete(it) }
        }
    }
}
