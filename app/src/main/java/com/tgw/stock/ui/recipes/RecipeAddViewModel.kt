package com.tgw.stock.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgw.stock.data.local.entities.RecipeEntity
import com.tgw.stock.data.repository.RecipeRepository
import com.tgw.stock.domain.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecipeAddState(
    val name: String = "",
    val sellPrice: String = "",
    val nameError: String? = null,
    val sellPriceError: String? = null,
    val isSaving: Boolean = false,
    val savedRecipeId: Long? = null
)

class RecipeAddViewModel(private val recipeRepository: RecipeRepository) : ViewModel() {
    private val _state = MutableStateFlow(RecipeAddState())
    val state: StateFlow<RecipeAddState> = _state.asStateFlow()

    fun updateName(v: String) { _state.value = _state.value.copy(name = v, nameError = null) }
    fun updateSellPrice(v: String) { _state.value = _state.value.copy(sellPrice = v, sellPriceError = null) }

    fun save() {
        val s = _state.value
        val price = Money.parseOrNull(s.sellPrice)
        var hasError = false
        var nameError: String? = null
        var priceError: String? = null
        if (s.name.isBlank()) { nameError = "Name is required"; hasError = true }
        if (price == null) { priceError = "Enter a valid price"; hasError = true }
        if (hasError) {
            _state.value = s.copy(nameError = nameError, sellPriceError = priceError)
            return
        }
        _state.value = s.copy(isSaving = true)
        viewModelScope.launch {
            val id = recipeRepository.upsert(RecipeEntity(name = s.name.trim(), sellPriceMinor = price!!.cents))
            _state.value = _state.value.copy(isSaving = false, savedRecipeId = id)
        }
    }
}
