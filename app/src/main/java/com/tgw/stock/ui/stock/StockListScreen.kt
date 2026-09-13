package com.tgw.stock.ui.stock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgw.stock.data.repository.ProductListItem
import com.tgw.stock.ui.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockListScreen(
    viewModel: StockListViewModel = tgwViewModel { StockListViewModel(it.productRepository, it.categoryRepository, it.stockRepository) },
    onProductClick: (Long) -> Unit,
    onAddProduct: () -> Unit
) {
    OnResumeEffect { viewModel.refreshExpiring() }

    val items by viewModel.visibleItems.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Stock Items", fontWeight = FontWeight.SemiBold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProduct) { Icon(Icons.Default.Add, contentDescription = "Add product") }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search products") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = filter == StockFilter.ALL,
                        onClick = { viewModel.filter.value = StockFilter.ALL },
                        label = { Text("All") }
                    )
                }
                item {
                    FilterChip(
                        selected = filter == StockFilter.LOW,
                        onClick = { viewModel.filter.value = StockFilter.LOW },
                        label = { Text("Low Stock") }
                    )
                }
                item {
                    FilterChip(
                        selected = filter == StockFilter.CRITICAL,
                        onClick = { viewModel.filter.value = StockFilter.CRITICAL },
                        label = { Text("Critical") }
                    )
                }
                item {
                    FilterChip(
                        selected = filter == StockFilter.OUT_OF_STOCK,
                        onClick = { viewModel.filter.value = StockFilter.OUT_OF_STOCK },
                        label = { Text("Out of Stock") }
                    )
                }
                item {
                    FilterChip(
                        selected = filter == StockFilter.EXPIRING_SOON,
                        onClick = { viewModel.filter.value = StockFilter.EXPIRING_SOON },
                        label = { Text("Expiring Soon") }
                    )
                }
                items(categories) { category ->
                    FilterChip(
                        selected = categoryFilter == category.id,
                        onClick = {
                            viewModel.categoryFilter.value = if (categoryFilter == category.id) null else category.id
                        },
                        label = { Text(category.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (items.isEmpty()) {
                EmptyState("No products match your filters")
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(items, key = { it.id }) { item ->
                        ProductRow(item, onClick = { onProductClick(item.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductRow(item: ProductListItem, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(item.name, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Text("${item.categoryName ?: "Uncategorized"} · ${formatQuantityWithUnit(item.currentStock, item.unit)} · ${item.stockValueMinor.centsToEuro()}")
        },
        trailingContent = { StatusChip(item.status) }
    )
}
