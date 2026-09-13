package com.tgw.stock.ui.purchase

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgw.stock.domain.PurchaseOrderStatus
import com.tgw.stock.ui.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseOrderDetailScreen(
    orderId: Long,
    onBack: () -> Unit
) {
    val viewModel: PurchaseOrderDetailViewModel = tgwViewModel {
        PurchaseOrderDetailViewModel(orderId, it.purchaseRepository, it.productRepository, it.supplierRepository)
    }
    val order by viewModel.order.collectAsStateWithLifecycle()
    val supplierName by viewModel.supplierName.collectAsStateWithLifecycle()
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    var showReceiveDialog by remember { mutableStateOf(false) }

    val totalCostMinor = rows.sumOf { Math.round(it.quantity * it.unitCostMinor) }
    val canReceive = order?.status in listOf(PurchaseOrderStatus.ORDERED.name, PurchaseOrderStatus.PARTIALLY_DELIVERED.name, PurchaseOrderStatus.DRAFT.name)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(order?.poNumber ?: "", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(supplierName, style = MaterialTheme.typography.titleMedium)
                    order?.let { StatusBadge(it.status) }
                }
                Text("Total: ${totalCostMinor.centsToEuro()}", style = MaterialTheme.typography.bodyMedium)
            }

            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp)) {
                items(rows, key = { it.itemId }) { row ->
                    ListItem(
                        headlineContent = { Text(row.productName) },
                        supportingContent = {
                            Text("${formatQuantityWithUnit(row.quantity, row.unit)} × ${row.unitCostMinor.centsToEuro()} · Received: ${formatQuantityWithUnit(row.receivedQuantity, row.unit)}")
                        },
                        trailingContent = { Text(Math.round(row.quantity * row.unitCostMinor).centsToEuro(), fontWeight = FontWeight.Medium) }
                    )
                    HorizontalDivider()
                }
            }

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (order?.status == PurchaseOrderStatus.DRAFT.name) {
                    Button(onClick = viewModel::markOrdered, modifier = Modifier.fillMaxWidth()) { Text("Mark as Ordered") }
                }
                if (canReceive) {
                    Button(onClick = { showReceiveDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Receive Stock") }
                }
                if (order?.status !in listOf(PurchaseOrderStatus.DELIVERED.name, PurchaseOrderStatus.CANCELLED.name)) {
                    OutlinedButton(onClick = viewModel::markCancelled, modifier = Modifier.fillMaxWidth()) { Text("Cancel Order") }
                }
            }
        }
    }

    if (showReceiveDialog) {
        ReceiveStockDialog(
            rows = rows,
            onDismiss = { showReceiveDialog = false },
            onConfirm = { received, expiries ->
                viewModel.receiveStock(received, expiries)
                showReceiveDialog = false
            }
        )
    }
}

@Composable
private fun ReceiveStockDialog(
    rows: List<PurchaseOrderItemRow>,
    onDismiss: () -> Unit,
    onConfirm: (Map<Long, Double>, Map<Long, Long?>) -> Unit
) {
    val quantities = remember { mutableStateMapOf<Long, String>().apply { rows.forEach { put(it.itemId, (it.quantity - it.receivedQuantity).coerceAtLeast(0.0).toString()) } } }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Receive Stock", style = MaterialTheme.typography.titleMedium)
                rows.forEach { row ->
                    Column {
                        Text(row.productName, fontWeight = FontWeight.Medium)
                        OutlinedTextField(
                            value = quantities[row.itemId] ?: "",
                            onValueChange = { quantities[row.itemId] = it },
                            label = { Text("Received (${row.unit})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = {
                        val received = quantities.mapNotNull { (id, text) -> text.toDoubleOrNull()?.let { id to it } }.toMap()
                        onConfirm(received, emptyMap())
                    }) { Text("Confirm Receipt") }
                }
            }
        }
    }
}
