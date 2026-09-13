package com.tgw.stock.ui.movements

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgw.stock.domain.StockInReason
import com.tgw.stock.ui.common.ProductPickerField
import com.tgw.stock.ui.common.formatDate
import com.tgw.stock.ui.common.tgwViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockInScreen(
    preselectedProductId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val viewModel: StockInViewModel = tgwViewModel {
        StockInViewModel(preselectedProductId, it.productRepository, it.supplierRepository, it.stockRepository)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var reasonMenuExpanded by remember { mutableStateOf(false) }
    var supplierMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock In", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProductPickerField(
                products = state.products,
                selectedProductId = state.productId,
                onSelect = viewModel::selectProduct,
                isError = state.productError != null,
                supportingText = state.productError
            )

            OutlinedTextField(
                value = state.quantity,
                onValueChange = viewModel::updateQuantity,
                label = { Text("Quantity received") },
                isError = state.quantityError != null,
                supportingText = { state.quantityError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.unitCost,
                onValueChange = viewModel::updateUnitCost,
                label = { Text("Unit cost (€ per base unit)") },
                isError = state.unitCostError != null,
                supportingText = { state.unitCostError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(expanded = reasonMenuExpanded, onExpandedChange = { reasonMenuExpanded = it }) {
                OutlinedTextField(
                    value = state.reason.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Reason") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = reasonMenuExpanded, onDismissRequest = { reasonMenuExpanded = false }) {
                    StockInReason.entries.forEach { reason ->
                        DropdownMenuItem(text = { Text(reason.label) }, onClick = {
                            viewModel.updateReason(reason)
                            reasonMenuExpanded = false
                        })
                    }
                }
            }

            ExposedDropdownMenuBox(expanded = supplierMenuExpanded, onExpandedChange = { supplierMenuExpanded = it }) {
                OutlinedTextField(
                    value = state.suppliers.firstOrNull { it.id == state.supplierId }?.companyName ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Supplier") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = supplierMenuExpanded, onDismissRequest = { supplierMenuExpanded = false }) {
                    DropdownMenuItem(text = { Text("None") }, onClick = { viewModel.updateSupplier(null); supplierMenuExpanded = false })
                    state.suppliers.forEach { supplier ->
                        DropdownMenuItem(text = { Text(supplier.companyName) }, onClick = {
                            viewModel.updateSupplier(supplier.id)
                            supplierMenuExpanded = false
                        })
                    }
                }
            }

            OutlinedTextField(
                value = state.expiryDate?.formatDate() ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Expiry date (optional)") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.DateRange, contentDescription = "Pick date") }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::updateNote,
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text("Save Stock In")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.expiryDate ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateExpiryDate(datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
