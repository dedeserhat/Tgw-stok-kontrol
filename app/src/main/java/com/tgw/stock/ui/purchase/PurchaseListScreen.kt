package com.tgw.stock.ui.purchase

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgw.stock.data.repository.PurchaseListLine
import com.tgw.stock.data.repository.SupplierPurchaseGroup
import com.tgw.stock.ui.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseListScreen(
    onBack: () -> Unit,
    onViewOrders: () -> Unit,
    onNewManualOrder: () -> Unit,
    onOrderCreated: (Long) -> Unit
) {
    val viewModel: PurchaseListViewModel = tgwViewModel { PurchaseListViewModel(it.purchaseRepository) }
    LaunchedEffect(Unit) { viewModel.refresh() }
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val createdOrderId by viewModel.createdOrderId.collectAsStateWithLifecycle()

    LaunchedEffect(createdOrderId) {
        createdOrderId?.let {
            viewModel.consumeCreatedOrderId()
            onOrderCreated(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Purchase List", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = { TextButton(onClick = onViewOrders) { Text("Orders") } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNewManualOrder, text = { Text("Manual Order") }, icon = {})
        }
    ) { padding ->
        if (isLoading && groups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
        } else if (groups.isEmpty()) {
            EmptyState("Nothing needs buying right now. All stock is above minimum.", modifier = Modifier.padding(padding))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(groups, key = { it.supplierId }) { group ->
                    SupplierGroupCard(group, onCreateOrder = { viewModel.createOrderForGroup(group) })
                }
            }
        }
    }
}

@Composable
private fun SupplierGroupCard(group: SupplierPurchaseGroup, onCreateOrder: () -> Unit) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(group.supplierName, style = MaterialTheme.typography.titleMedium)
                Text(group.totalCostMinor.centsToEuro(), fontWeight = FontWeight.SemiBold)
            }
            group.lines.forEach { line -> PurchaseLineRow(line) }
            if (group.supplierId >= 0) {
                Button(onClick = onCreateOrder, modifier = Modifier.fillMaxWidth()) { Text("Create Purchase Order") }
            }
        }
    }
}

@Composable
private fun PurchaseLineRow(line: PurchaseListLine) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(line.productName, fontWeight = FontWeight.Medium)
            Text(line.estimatedCostMinor.centsToEuro())
        }
        Text(
            "Current: ${formatQuantityWithUnit(line.currentStock, line.unit)} · Target: ${formatQuantityWithUnit(line.targetStock, line.unit)} · Buy: ${formatQuantityWithUnit(line.quantityToBuy, line.unit)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
