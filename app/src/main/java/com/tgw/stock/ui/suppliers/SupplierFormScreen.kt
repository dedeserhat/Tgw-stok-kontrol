package com.tgw.stock.ui.suppliers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgw.stock.ui.common.tgwViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierFormScreen(
    supplierId: Long?,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit
) {
    val viewModel: SupplierFormViewModel = tgwViewModel { SupplierFormViewModel(supplierId, it.supplierRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedId) { state.savedId?.let { onSaved(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Edit Supplier" else "Add Supplier", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.companyName,
                onValueChange = { name -> viewModel.updateField { it.copy(companyName = name, nameError = null) } },
                label = { Text("Company name") },
                isError = state.nameError != null,
                supportingText = { state.nameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.phone,
                onValueChange = { v -> viewModel.updateField { it.copy(phone = v) } },
                label = { Text("Phone") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.email,
                onValueChange = { v -> viewModel.updateField { it.copy(email = v) } },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.address,
                onValueChange = { v -> viewModel.updateField { it.copy(address = v) } },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.minOrder,
                onValueChange = { v -> viewModel.updateField { it.copy(minOrder = v) } },
                label = { Text("Minimum order (€, optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Delivery days", style = MaterialTheme.typography.labelLarge)
            FlowRowDays(state.selectedDays, viewModel::toggleDay)

            OutlinedTextField(
                value = state.paymentTerms,
                onValueChange = { v -> viewModel.updateField { it.copy(paymentTerms = v) } },
                label = { Text("Payment terms") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.notes,
                onValueChange = { v -> viewModel.updateField { it.copy(notes = v) } },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = viewModel::save, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text("Save Supplier")
            }
        }
    }
}

@Composable
private fun FlowRowDays(selected: Set<String>, onToggle: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(WEEK_DAYS) { day ->
            FilterChip(selected = day in selected, onClick = { onToggle(day) }, label = { Text(day) })
        }
    }
}
