package com.tgw.stock.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgw.stock.data.repository.MovementView
import com.tgw.stock.domain.MovementType
import com.tgw.stock.ui.common.*
import com.tgw.stock.ui.theme.StatusCritical
import com.tgw.stock.ui.theme.StatusOutOfStock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = tgwViewModel { DashboardViewModel(it.dashboardRepository) },
    onNavigateToStock: () -> Unit,
    onNavigateToStockIn: () -> Unit,
    onNavigateToStockOut: () -> Unit,
    onNavigateToStockCount: () -> Unit,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToNewPurchaseOrder: () -> Unit,
    onNavigateToPurchaseList: () -> Unit,
    onNavigateToBarcodeScan: () -> Unit
) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TGW Stock", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onNavigateToBarcodeScan) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan barcode")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && data == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val d = data ?: return@Scaffold

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(quickActions(onNavigateToStockIn, onNavigateToStockOut, onNavigateToStockCount, onNavigateToNewPurchaseOrder, onNavigateToAddProduct)) { action ->
                        QuickActionButton(
                            label = action.label,
                            icon = { Icon(action.icon, contentDescription = action.label) },
                            modifier = Modifier.width(96.dp),
                            onClick = action.onClick
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Total Products", d.totalProducts.toString(), modifier = Modifier.weight(1f), onClick = onNavigateToStock)
                        StatCard("Stock Value", d.totalStockValueMinor.centsToEuro(), modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Critical Stock", d.criticalCount.toString(), modifier = Modifier.weight(1f), valueColor = StatusCritical, onClick = onNavigateToStock)
                        StatCard("Out of Stock", d.outOfStockCount.toString(), modifier = Modifier.weight(1f), valueColor = StatusOutOfStock, onClick = onNavigateToStock)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Expiring Soon", d.expiringSoonCount.toString(), modifier = Modifier.weight(1f), onClick = onNavigateToStock)
                        StatCard("Buy Today", d.purchaseNeededCount.toString(), modifier = Modifier.weight(1f), onClick = onNavigateToPurchaseList)
                    }
                }
            }

            item {
                Button(
                    onClick = onNavigateToPurchaseList,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Purchase List")
                }
            }

            item { SectionHeader("Recent Stock In") }
            if (d.recentStockIn.isEmpty()) {
                item { EmptyState("No stock received yet") }
            } else {
                items(d.recentStockIn) { MovementRow(it) }
            }

            item { SectionHeader("Recent Stock Out") }
            if (d.recentStockOut.isEmpty()) {
                item { EmptyState("No stock used yet") }
            } else {
                items(d.recentStockOut) { MovementRow(it) }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

private data class QuickAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

private fun quickActions(
    onStockIn: () -> Unit,
    onStockOut: () -> Unit,
    onStockCount: () -> Unit,
    onNewPurchase: () -> Unit,
    onAddProduct: () -> Unit
): List<QuickAction> = listOf(
    QuickAction("Stock In", Icons.Default.AddCircle, onStockIn),
    QuickAction("Stock Out", Icons.Default.RemoveCircle, onStockOut),
    QuickAction("Stock Count", Icons.Default.FactCheck, onStockCount),
    QuickAction("New Purchase", Icons.Default.ShoppingCart, onNewPurchase),
    QuickAction("Add Product", Icons.Default.Add, onAddProduct)
)

@Composable
private fun MovementRow(movement: MovementView) {
    ListItem(
        headlineContent = { Text(movement.productName) },
        supportingContent = { Text("${movement.reason.replace('_', ' ')} · ${movement.createdAt.formatDateTime()}") },
        trailingContent = {
            val sign = if (movement.type == MovementType.IN.name) "+" else "-"
            val color = if (movement.type == MovementType.IN.name) com.tgw.stock.ui.theme.StatusNormal else com.tgw.stock.ui.theme.StatusOutOfStock
            Text("$sign${formatQuantityWithUnit(movement.quantity, movement.unit)}", color = color, fontWeight = FontWeight.Medium)
        }
    )
}
