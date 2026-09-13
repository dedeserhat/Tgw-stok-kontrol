package com.tgw.stock.ui.stock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgw.stock.ui.common.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onStockIn: (Long) -> Unit,
    onStockOut: (Long) -> Unit,
    onViewSupplierPrices: (Long) -> Unit,
    onRecipeClick: (Long) -> Unit
) {
    val viewModel: ProductDetailViewModel = tgwViewModel {
        ProductDetailViewModel(productId, it.productRepository, it.productDetailRepository, it.supplierPriceRepository, it.stockRepository)
    }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { viewModel.refresh() }

    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val batches by viewModel.batches.collectAsStateWithLifecycle()
    val priceHistory by viewModel.priceHistory.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.product?.name ?: "", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { onEdit(productId) }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                    IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                }
            )
        }
    ) { padding ->
        val d = detail
        if (d == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onStockIn(productId) }, modifier = Modifier.weight(1f)) { Text("Stock In") }
                    OutlinedButton(onClick = { onStockOut(productId) }, modifier = Modifier.weight(1f)) { Text("Stock Out") }
                }
            }

            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(d.categoryName ?: "Uncategorized", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            StatusChip(d.status)
                        }
                        InfoRow("Current Stock", formatQuantityWithUnit(d.product.currentStock, d.product.unit))
                        InfoRow("Minimum", formatQuantityWithUnit(d.product.minStock, d.product.unit))
                        InfoRow("Target", formatQuantityWithUnit(d.product.targetStock, d.product.unit))
                        InfoRow("Stock Value", d.stockValueMinor.centsToEuro())
                        InfoRow("Avg. Purchase Price", "${d.product.avgCostPerUnitMinor.centsToEuro()} / ${com.tgw.stock.domain.StockUnit.fromNameSafe(d.product.unit).label}")
                        InfoRow("Last Purchase Price", "${d.product.lastCostPerUnitMinor.centsToEuro()}")
                        InfoRow("Last Purchase Date", d.product.lastPurchaseDate?.formatDate() ?: "—")
                        if (d.bestOffer != null) {
                            InfoRow("Best Supplier", d.bestOffer.supplierName)
                            InfoRow("Current Price", "${d.bestOffer.latestPriceMinor.centsToEuro()} / ${d.bestOffer.packageDescription}")
                        }
                        if (d.product.barcode != null) {
                            InfoRow("Barcode", d.product.barcode)
                        }
                        TextButton(onClick = { onViewSupplierPrices(productId) }) { Text("View all supplier prices") }
                    }
                }
            }

            item { SectionHeader("Used In") }
            if (d.usedIn.isEmpty()) {
                item { EmptyState("Not used in any recipe yet") }
            } else {
                items(d.usedIn) { usage ->
                    ListItem(
                        headlineContent = { Text(usage.recipeName) },
                        trailingContent = { Text(formatQuantityWithUnit(usage.quantity, d.product.unit)) },
                        modifier = Modifier.clickableRow { onRecipeClick(usage.recipeId) }
                    )
                }
            }

            item { SectionHeader("Batches (FEFO order)") }
            if (batches.none { it.quantityRemaining > 0 }) {
                item { EmptyState("No active batches") }
            } else {
                items(batches.filter { it.quantityRemaining > 0 }) { batch ->
                    ListItem(
                        headlineContent = { Text(formatQuantityWithUnit(batch.quantityRemaining, d.product.unit)) },
                        supportingContent = { Text("Received ${batch.receivedDate.formatDate()}") },
                        trailingContent = {
                            batch.expiryDate?.let { Text("Exp: ${it.formatDate()}") } ?: Text("No expiry")
                        }
                    )
                }
            }

            item { SectionHeader("Last Movements") }
            if (d.recentMovements.isEmpty()) {
                item { EmptyState("No movements yet") }
            } else {
                items(d.recentMovements.take(10)) { m ->
                    val sign = if (m.type == com.tgw.stock.domain.MovementType.IN.name) "+" else "-"
                    ListItem(
                        headlineContent = { Text("$sign${formatQuantityWithUnit(m.quantity, m.unit)} · ${m.reason.replace('_', ' ')}") },
                        supportingContent = { Text(m.createdAt.formatDateTime()) }
                    )
                }
            }

            item { SectionHeader("Price History") }
            if (priceHistory.isEmpty()) {
                item { EmptyState("No supplier prices recorded") }
            } else {
                items(priceHistory) { p ->
                    ListItem(
                        headlineContent = { Text("${p.supplierName} · ${p.packageDescription}") },
                        supportingContent = { Text(p.effectiveDate.formatDate()) },
                        trailingContent = { Text(p.priceMinor.centsToEuro()) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Delete product?",
            message = "${detail?.product?.name} will be removed from stock, recipes and purchasing. Its stock/waste/purchase history is kept for past reports.",
            confirmLabel = "Delete",
            onConfirm = {
                showDeleteConfirm = false
                scope.launch {
                    viewModel.deleteProduct()
                    onBack()
                }
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)
