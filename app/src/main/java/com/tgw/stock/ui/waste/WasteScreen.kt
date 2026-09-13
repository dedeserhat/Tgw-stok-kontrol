package com.tgw.stock.ui.waste

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgw.stock.domain.WasteReason
import com.tgw.stock.ui.common.*
import com.tgw.stock.ui.theme.StatusOutOfStock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasteScreen(onBack: () -> Unit) {
    val viewModel: WasteViewModel = tgwViewModel { WasteViewModel(it.wasteRepository, it.productRepository) }
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val weeklyTotal by viewModel.weeklyTotal.collectAsStateWithLifecycle()
    val monthlyTotal by viewModel.monthlyTotal.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Waste", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, contentDescription = "Record waste") }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("This Week", weeklyTotal.centsToEuro(), modifier = Modifier.weight(1f), valueColor = StatusOutOfStock)
                StatCard("This Month", monthlyTotal.centsToEuro(), modifier = Modifier.weight(1f), valueColor = StatusOutOfStock)
            }
            if (rows.isEmpty()) {
                EmptyState("No waste recorded yet.")
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(rows) { row ->
                        ListItem(
                            headlineContent = { Text(row.productName) },
                            supportingContent = { Text("${row.reason.replace('_', ' ')} · ${row.recordedDate.formatDate()}${row.note?.let { " · $it" } ?: ""}") },
                            trailingContent = {
                                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                    Text(formatQuantityWithUnit(row.quantity, row.unit))
                                    Text(row.costMinor.centsToEuro(), color = StatusOutOfStock, fontWeight = FontWeight.Medium)
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddWasteDialog(
            products = products,
            onDismiss = { showAddDialog = false },
            onConfirm = { productId, qty, reason, note ->
                viewModel.recordWaste(productId, qty, reason, note)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWasteDialog(
    products: List<com.tgw.stock.data.local.entities.ProductEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Double, WasteReason, String?) -> Unit
) {
    var productId by remember { mutableStateOf<Long?>(null) }
    var quantity by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf(WasteReason.EXPIRED) }
    var reasonMenuExpanded by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Waste") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProductPickerField(products = products, selectedProductId = productId, onSelect = { productId = it })
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = reasonMenuExpanded, onExpandedChange = { reasonMenuExpanded = it }) {
                    OutlinedTextField(
                        value = reason.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Reason") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = reasonMenuExpanded, onDismissRequest = { reasonMenuExpanded = false }) {
                        WasteReason.entries.forEach { r ->
                            DropdownMenuItem(text = { Text(r.label) }, onClick = { reason = r; reasonMenuExpanded = false })
                        }
                    }
                }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val qty = quantity.toDoubleOrNull()
                if (productId == null || qty == null || qty <= 0.0) {
                    error = "Select a product and enter a valid quantity"
                } else {
                    onConfirm(productId!!, qty, reason, note.trim().ifBlank { null })
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
