package com.tgw.stock.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgw.stock.data.repository.RecipeCostSummary
import com.tgw.stock.data.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecipeListViewModel(private val recipeRepository: RecipeRepository) : ViewModel() {

    private val _summaries = MutableStateFlow<List<RecipeCostSummary>>(emptyList())
    val summaries: StateFlow<List<RecipeCostSummary>> = _summaries.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _summaries.value = recipeRepository.getAllCostSummaries().sortedBy { it.recipeName }
            _isLoading.value = false
        }
    }
}
