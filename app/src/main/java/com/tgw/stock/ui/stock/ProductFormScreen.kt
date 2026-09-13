package com.tgw.stock.ui.stock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgw.stock.domain.StockUnit
import com.tgw.stock.ui.common.tgwViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    productId: Long?,
    initialBarcode: String? = null,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit
) {
    val viewModel: ProductFormViewModel = tgwViewModel {
        ProductFormViewModel(productId, it.productRepository, it.categoryRepository, it.supplierRepository, initialBarcode)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedProductId) {
        state.savedProductId?.let { onSaved(it) }
    }

    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var unitMenuExpanded by remember { mutableStateOf(false) }
    var supplierMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Edit Product" else "Add Product", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::updateName,
                label = { Text("Product name") },
                isError = state.nameError != null,
                supportingText = { state.nameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(expanded = categoryMenuExpanded, onExpandedChange = { categoryMenuExpanded = it }) {
                OutlinedTextField(
                    value = categories.firstOrNull { it.id == state.categoryId }?.name ?: "Select category",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                    categories.forEach { category ->
                        DropdownMenuItem(text = { Text(category.name) }, onClick = {
                            viewModel.updateCategory(category.id)
                            categoryMenuExpanded = false
                        })
                    }
                }
            }

            ExposedDropdownMenuBox(expanded = unitMenuExpanded, onExpandedChange = { unitMenuExpanded = it }) {
                OutlinedTextField(
                    value = state.unit.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unit") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = unitMenuExpanded, onDismissRequest = { unitMenuExpanded = false }) {
                    StockUnit.entries.forEach { unit ->
                        DropdownMenuItem(text = { Text(unit.label) }, onClick = {
                            viewModel.updateUnit(unit)
                            unitMenuExpanded = false
                        })
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.minStock,
                    onValueChange = viewModel::updateMinStock,
                    label = { Text("Minimum stock") },
                    isError = state.minStockError != null,
                    supportingText = { state.minStockError?.let { Text(it) } },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.targetStock,
                    onValueChange = viewModel::updateTargetStock,
                    label = { Text("Target stock") },
                    isError = state.targetStockError != null,
                    supportingText = { state.targetStockError?.let { Text(it) } },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            ExposedDropdownMenuBox(expanded = supplierMenuExpanded, onExpandedChange = { supplierMenuExpanded = it }) {
                OutlinedTextField(
                    value = suppliers.firstOrNull { it.id == state.preferredSupplierId }?.companyName ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Preferred supplier") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = supplierMenuExpanded, onDismissRequest = { supplierMenuExpanded = false }) {
                    DropdownMenuItem(text = { Text("None") }, onClick = {
                        viewModel.updateSupplier(null)
                        supplierMenuExpanded = false
                    })
                    suppliers.forEach { supplier ->
                        DropdownMenuItem(text = { Text(supplier.companyName) }, onClick = {
                            viewModel.updateSupplier(supplier.id)
                            supplierMenuExpanded = false
                        })
                    }
                }
            }

            OutlinedTextField(
                value = state.barcode,
                onValueChange = viewModel::updateBarcode,
                label = { Text("Barcode (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (state.isEditMode) "Save Changes" else "Add Product")
                }
            }
        }
    }
}
