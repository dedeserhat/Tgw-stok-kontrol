package com.tgw.stock.ui.recipes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgw.stock.domain.Money
import com.tgw.stock.ui.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: Long,
    onBack: () -> Unit
) {
    val viewModel: RecipeDetailViewModel = tgwViewModel { RecipeDetailViewModel(recipeId, it.recipeRepository, it.productRepository) }
    val recipe by viewModel.recipe.collectAsStateWithLifecycle()
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingRow by remember { mutableStateOf<RecipeItemRow?>(null) }
    var showPriceDialog by remember { mutableStateOf(false) }
    var showDeleteRecipeConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) { if (deleted) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe?.name ?: "", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    IconButton(onClick = { showDeleteRecipeConfirm = true }) { Icon(Icons.Default.Delete, contentDescription = "Delete recipe") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingRow = null; showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add ingredient")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Sell price", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { showPriceDialog = true }) {
                                Text(recipe?.sellPriceMinor?.centsToEuro() ?: "€0.00", fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Edit, contentDescription = "Edit price", modifier = Modifier.size(16.dp))
                            }
                        }
                        summary?.let { s ->
                            StatRow("Ingredient cost", s.costMinor.centsToEuro())
                            StatRow("Food cost", s.foodCostPercent.formatPercent())
                            StatRow("Estimated profit", s.profitMinor.centsToEuro())
                            StatRow("Producible with current stock", "${s.portionsAvailable} portions")
                        }
                    }
                }
            }

            item { SectionHeader("Ingredients") }
            if (rows.isEmpty()) {
                item { EmptyState("No ingredients yet. Tap + to add.") }
            } else {
                items(rows, key = { it.itemId }) { row ->
                    ListItem(
                        headlineContent = { Text(row.productName) },
                        supportingContent = { Text(formatQuantityWithUnit(row.quantity, row.unit)) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { editingRow = row; showAddDialog = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { viewModel.deleteIngredient(row) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showAddDialog) {
        IngredientDialog(
            products = products,
            initial = editingRow,
            onDismiss = { showAddDialog = false },
            onConfirm = { productId, quantity ->
                viewModel.addOrUpdateIngredient(productId, quantity, editingRow?.itemId)
                showAddDialog = false
            }
        )
    }

    if (showPriceDialog) {
        PriceDialog(
            initialCents = recipe?.sellPriceMinor ?: 0,
            onDismiss = { showPriceDialog = false },
            onConfirm = { cents -> viewModel.updateSellPrice(cents); showPriceDialog = false }
        )
    }

    if (showDeleteRecipeConfirm) {
        ConfirmDialog(
            title = "Delete recipe?",
            message = "This removes ${recipe?.name} and its ingredient list.",
            confirmLabel = "Delete",
            onConfirm = { showDeleteRecipeConfirm = false; viewModel.deleteRecipe() },
            onDismiss = { showDeleteRecipeConfirm = false }
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientDialog(
    products: List<com.tgw.stock.data.local.entities.ProductEntity>,
    initial: RecipeItemRow?,
    onDismiss: () -> Unit,
    onConfirm: (Long, Double) -> Unit
) {
    var productId by remember { mutableStateOf(initial?.productId) }
    var quantity by remember { mutableStateOf(initial?.quantity?.toString() ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    val selectedProductUnit = products.firstOrNull { it.id == productId }?.unit
        ?.let { com.tgw.stock.domain.StockUnit.fromNameSafe(it) }
    val smallUnitHint = when (selectedProductUnit) {
        com.tgw.stock.domain.StockUnit.KG -> "e.g. 150 g is 0.15"
        com.tgw.stock.domain.StockUnit.L -> "e.g. 150 ml is 0.15"
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Ingredient" else "Edit Ingredient") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProductPickerField(products = products, selectedProductId = productId, onSelect = { productId = it })
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text(if (selectedProductUnit != null) "Quantity (${selectedProductUnit.label})" else "Quantity") },
                    isError = error != null,
                    supportingText = { Text(error ?: smallUnitHint ?: " ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val qty = quantity.toDoubleOrNull()
                if (productId == null || qty == null || qty <= 0.0) {
                    error = "Select a product and enter a valid quantity"
                } else {
                    onConfirm(productId!!, qty)
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PriceDialog(initialCents: Long, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var text by remember { mutableStateOf((initialCents / 100.0).toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sell price") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Price (€)") },
                isError = error != null,
                supportingText = { error?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val money = Money.parseOrNull(text)
                if (money == null) error = "Enter a valid price" else onConfirm(money.cents)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
