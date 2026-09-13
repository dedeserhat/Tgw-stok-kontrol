package com.tgw.stock.ui.movements

import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgw.stock.domain.StockOutReason
import com.tgw.stock.ui.common.ProductPickerField
import com.tgw.stock.ui.common.formatQuantityWithUnit
import com.tgw.stock.ui.common.tgwViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockOutScreen(
    preselectedProductId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val viewModel: StockOutViewModel = tgwViewModel {
        StockOutViewModel(preselectedProductId, it.productRepository, it.stockRepository)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var reasonMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock Out", fontWeight = FontWeight.SemiBold) },
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

            if (state.currentStock != null) {
                Text(
                    "Available: ${formatQuantityWithUnit(state.currentStock!!, state.unit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = state.quantity,
                onValueChange = viewModel::updateQuantity,
                label = { Text("Quantity used") },
                isError = state.quantityError != null,
                supportingText = { state.quantityError?.let { Text(it) } },
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
                    StockOutReason.entries.forEach { reason ->
                        DropdownMenuItem(text = { Text(reason.label) }, onClick = {
                            viewModel.updateReason(reason)
                            reasonMenuExpanded = false
                        })
                    }
                }
            }

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
                if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text("Save Stock Out")
            }
        }
    }
}
