package com.tgw.stock.ui.purchase

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgw.stock.ui.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseOrderNewScreen(
    preselectedSupplierId: Long?,
    onBack: () -> Unit,
    onCreated: (Long) -> Unit
) {
    val viewModel: PurchaseOrderNewViewModel = tgwViewModel {
        PurchaseOrderNewViewModel(preselectedSupplierId, it.productRepository, it.supplierRepository, it.supplierPriceRepository, it.purchaseRepository)
    }
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val supplierId by viewModel.supplierId.collectAsStateWithLifecycle()
    val lines by viewModel.lines.collectAsStateWithLifecycle()
    val createdOrderId by viewModel.createdOrderId.collectAsStateWithLifecycle()

    var supplierMenuExpanded by remember { mutableStateOf(false) }
    var showAddLineDialog by remember { mutableStateOf(false) }

    LaunchedEffect(createdOrderId) { createdOrderId?.let { onCreated(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Purchase Order", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ExposedDropdownMenuBox(expanded = supplierMenuExpanded, onExpandedChange = { supplierMenuExpanded = it }) {
                OutlinedTextField(
                    value = suppliers.firstOrNull { it.id == supplierId }?.companyName ?: "Select supplier",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Supplier") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = supplierMenuExpanded, onDismissRequest = { supplierMenuExpanded = false }) {
                    suppliers.forEach { s ->
                        DropdownMenuItem(text = { Text(s.companyName) }, onClick = { viewModel.selectSupplier(s.id); supplierMenuExpanded = false })
                    }
                }
            }

            OutlinedButton(onClick = { showAddLineDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Add Product") }

            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(lines) { index, line ->
                    ListItem(
                        headlineContent = { Text(line.productName) },
                        supportingContent = { Text("${line.quantity} × ${line.unitCostMinor.centsToEuro()} = ${Math.round(line.quantity * line.unitCostMinor).centsToEuro()}") },
                        trailingContent = {
                            IconButton(onClick = { viewModel.removeLine(index) }) { Icon(Icons.Default.Delete, contentDescription = "Remove") }
                        }
                    )
                    HorizontalDivider()
                }
            }

            Button(
                onClick = viewModel::submit,
                enabled = supplierId != null && lines.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("Create Order") }
        }
    }

    if (showAddLineDialog) {
        AddLineDialog(
            products = products,
            onDismiss = { showAddLineDialog = false },
            onConfirm = { productId, qty ->
                viewModel.addLine(productId, qty)
                showAddLineDialog = false
            }
        )
    }
}

@Composable
private fun AddLineDialog(
    products: List<com.tgw.stock.data.local.entities.ProductEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Double) -> Unit
) {
    var productId by remember { mutableStateOf<Long?>(null) }
    var quantity by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProductPickerField(products = products, selectedProductId = productId, onSelect = { productId = it })
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
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
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
