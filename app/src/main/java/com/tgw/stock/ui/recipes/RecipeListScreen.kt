package com.tgw.stock.ui.recipes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgw.stock.data.repository.RecipeCostSummary
import com.tgw.stock.ui.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    onRecipeClick: (Long) -> Unit,
    onAddRecipe: () -> Unit
) {
    val viewModel: RecipeListViewModel = tgwViewModel { RecipeListViewModel(it.recipeRepository) }
    OnResumeEffect { viewModel.refresh() }
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Recipes", fontWeight = FontWeight.SemiBold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRecipe) { Icon(Icons.Default.Add, contentDescription = "Add recipe") }
        }
    ) { padding ->
        if (summaries.isEmpty()) {
            EmptyState("No recipes yet. Add your first menu item.", modifier = Modifier.padding(padding))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 96.dp)) {
                items(summaries, key = { it.recipeId }) { summary ->
                    RecipeRow(summary, onClick = { onRecipeClick(summary.recipeId) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun RecipeRow(summary: RecipeCostSummary, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(summary.recipeName, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Text("Cost ${summary.costMinor.centsToEuro()} · Food cost ${summary.foodCostPercent.formatPercent()} · ${summary.portionsAvailable} portions available")
        },
        trailingContent = { Text(summary.sellPriceMinor.centsToEuro(), fontWeight = FontWeight.SemiBold) }
    )
}
