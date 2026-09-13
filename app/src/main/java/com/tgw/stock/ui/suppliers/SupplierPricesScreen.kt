package com.tgw.stock.ui.suppliers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.tgw.stock.data.local.entities.SupplierEntity
import com.tgw.stock.data.repository.SupplierOffer
import com.tgw.stock.ui.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierPricesScreen(
    productId: Long,
    onBack: () -> Unit
) {
    val viewModel: SupplierPricesViewModel = tgwViewModel {
        SupplierPricesViewModel(productId, it.productRepository, it.supplierRepository, it.supplierPriceRepository)
    }
    val product by viewModel.product.collectAsStateWithLifecycle()
    val offers by viewModel.offers.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val history by viewModel.historyBySupplierProduct.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var expandedOfferId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${product?.name ?: ""} Prices", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, contentDescription = "Add supplier price") }
        }
    ) { padding ->
        if (offers.isEmpty()) {
            EmptyState("No supplier prices recorded yet.", modifier = Modifier.padding(padding))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(offers) { index, offer ->
                    OfferCard(
                        offer = offer,
                        isCheapest = index == 0,
                        expanded = expandedOfferId == offer.supplierProductId,
                        history = history[offer.supplierProductId].orEmpty(),
                        onToggleExpand = {
                            expandedOfferId = if (expandedOfferId == offer.supplierProductId) null else offer.supplierProductId
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddOfferDialog(
            suppliers = suppliers,
            onDismiss = { showAddDialog = false },
            onConfirm = { supplierId, desc, qty, price, date ->
                viewModel.addOffer(supplierId, desc, qty, price, date)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun OfferCard(
    offer: SupplierOffer,
    isCheapest: Boolean,
    expanded: Boolean,
    history: List<com.tgw.stock.data.local.entities.SupplierPriceEntity>,
    onToggleExpand: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(offer.supplierName, fontWeight = FontWeight.SemiBold)
                    Text(offer.packageDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Text(offer.latestPriceMinor.centsToEuro(), fontWeight = FontWeight.Bold)
                    if (isCheapest) {
                        Text("Cheapest", color = com.tgw.stock.ui.theme.StatusNormal, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            TextButton(onClick = onToggleExpand) { Text(if (expanded) "Hide price history" else "Show price history") }
            if (expanded) {
                history.forEach { p ->
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(p.effectiveDate.formatDate(), style = MaterialTheme.typography.bodySmall)
                        Text(p.priceMinor.centsToEuro(), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddOfferDialog(
    suppliers: List<SupplierEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, Double, Long, Long) -> Unit
) {
    var supplierMenuExpanded by remember { mutableStateOf(false) }
    var supplierId by remember { mutableStateOf<Long?>(null) }
    var packageDescription by remember { mutableStateOf("") }
    var packageQuantity by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Supplier Price") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(expanded = supplierMenuExpanded, onExpandedChange = { supplierMenuExpanded = it }) {
                    OutlinedTextField(
                        value = suppliers.firstOrNull { it.id == supplierId }?.companyName ?: "Select supplier",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = supplierMenuExpanded, onDismissRequest = { supplierMenuExpanded = false }) {
                        suppliers.forEach { s ->
                            DropdownMenuItem(text = { Text(s.companyName) }, onClick = { supplierId = s.id; supplierMenuExpanded = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = packageDescription,
                    onValueChange = { packageDescription = it },
                    label = { Text("Package (e.g. 10 kg bag)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = packageQuantity,
                    onValueChange = { packageQuantity = it },
                    label = { Text("Package quantity (base unit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price for this package (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val qty = packageQuantity.toDoubleOrNull()
                val money = com.tgw.stock.domain.Money.parseOrNull(price)
                if (supplierId == null || packageDescription.isBlank() || qty == null || qty <= 0.0 || money == null) {
                    error = "Fill in all fields with valid values"
                } else {
                    onConfirm(supplierId!!, packageDescription, qty, money.cents, System.currentTimeMillis())
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
