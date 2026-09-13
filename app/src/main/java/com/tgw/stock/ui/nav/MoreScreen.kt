package com.tgw.stock.ui.nav

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class MoreItem(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onSuppliers: () -> Unit,
    onPurchaseOrders: () -> Unit,
    onStockCounts: () -> Unit,
    onWaste: () -> Unit,
    onReports: () -> Unit,
    onSettings: () -> Unit
) {
    val items = listOf(
        MoreItem("Suppliers", Icons.Default.LocalShipping, onSuppliers),
        MoreItem("Purchase Orders", Icons.Default.Receipt, onPurchaseOrders),
        MoreItem("Stock Counts", Icons.Default.FactCheck, onStockCounts),
        MoreItem("Waste", Icons.Default.DeleteOutline, onWaste),
        MoreItem("Reports", Icons.Default.BarChart, onReports),
        MoreItem("Settings", Icons.Default.Settings, onSettings)
    )

    Scaffold(topBar = { TopAppBar(title = { Text("More", fontWeight = FontWeight.SemiBold) }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(items) { item ->
                ListItem(
                    headlineContent = { Text(item.label) },
                    leadingContent = { Icon(item.icon, contentDescription = null) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.clickable(onClick = item.onClick)
                )
                HorizontalDivider()
            }
        }
    }
}
