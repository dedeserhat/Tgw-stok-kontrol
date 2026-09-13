package com.tgw.stock.ui.recipes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgw.stock.ui.common.tgwViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeAddScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit
) {
    val viewModel: RecipeAddViewModel = tgwViewModel { RecipeAddViewModel(it.recipeRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedRecipeId) { state.savedRecipeId?.let { onSaved(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Recipe", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::updateName,
                label = { Text("Recipe / menu item name") },
                isError = state.nameError != null,
                supportingText = { state.nameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.sellPrice,
                onValueChange = viewModel::updateSellPrice,
                label = { Text("Sell price (€)") },
                isError = state.sellPriceError != null,
                supportingText = { state.sellPriceError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = viewModel::save, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text("Create & Add Ingredients")
            }
        }
    }
}
