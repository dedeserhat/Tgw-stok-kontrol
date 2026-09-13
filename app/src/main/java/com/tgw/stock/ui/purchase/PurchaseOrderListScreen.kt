package com.tgw.stock.ui.purchase

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgw.stock.domain.PurchaseOrderStatus
import com.tgw.stock.ui.common.EmptyState
import com.tgw.stock.ui.common.formatDate
import com.tgw.stock.ui.common.tgwViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseOrderListScreen(
    onBack: () -> Unit,
    onOrderClick: (Long) -> Unit
) {
    val viewModel: PurchaseOrderListViewModel = tgwViewModel { PurchaseOrderListViewModel(it.purchaseRepository, it.supplierRepository) }
    val rows by viewModel.rows.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Purchase Orders", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        if (rows.isEmpty()) {
            EmptyState("No purchase orders yet.", modifier = Modifier.padding(padding))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(rows, key = { it.id }) { row ->
                    ListItem(
                        headlineContent = { Text(row.poNumber, fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("${row.supplierName} · ${row.createdAt.formatDate()}") },
                        trailingContent = { StatusBadge(row.status) },
                        modifier = Modifier.clickable { onOrderClick(row.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status) {
        PurchaseOrderStatus.DRAFT.name -> MaterialTheme.colorScheme.onSurfaceVariant
        PurchaseOrderStatus.ORDERED.name -> com.tgw.stock.ui.theme.StatusLow
        PurchaseOrderStatus.PARTIALLY_DELIVERED.name -> com.tgw.stock.ui.theme.StatusLow
        PurchaseOrderStatus.DELIVERED.name -> com.tgw.stock.ui.theme.StatusNormal
        PurchaseOrderStatus.CANCELLED.name -> com.tgw.stock.ui.theme.StatusOutOfStock
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val label = PurchaseOrderStatus.entries.firstOrNull { it.name == status }?.label ?: status
    Text(label, color = color, fontWeight = FontWeight.Medium)
}
