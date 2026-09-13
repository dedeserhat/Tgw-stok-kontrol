package com.tgw.stock.ui.suppliers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgw.stock.ui.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierDetailScreen(
    supplierId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val viewModel: SupplierDetailViewModel = tgwViewModel { SupplierDetailViewModel(supplierId, it.supplierRepository, it.supplierPriceRepository) }
    LaunchedEffect(Unit) { viewModel.refresh() }
    val supplier by viewModel.supplier.collectAsStateWithLifecycle()
    val offers by viewModel.offers.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) { if (deleted) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(supplier?.companyName ?: "", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    IconButton(onClick = { onEdit(supplierId) }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                    IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                }
            )
        }
    ) { padding ->
        val s = supplier
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (s != null) {
                item {
                    Card {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            s.phone?.let { InfoLine("Phone", it) }
                            s.email?.let { InfoLine("Email", it) }
                            s.address?.let { InfoLine("Address", it) }
                            s.minOrderMinor?.let { InfoLine("Minimum Order", it.centsToEuro()) }
                            s.deliveryDays?.let { InfoLine("Delivery Days", it.replace(",", ", ")) }
                            s.paymentTerms?.let { InfoLine("Payment Terms", it) }
                            s.notes?.let { InfoLine("Notes", it) }
                        }
                    }
                }
            }
            item { SectionHeader("Products Supplied") }
            if (offers.isEmpty()) {
                item { EmptyState("No product prices recorded for this supplier yet.") }
            } else {
                items(offers) { (productName, offer) ->
                    ListItem(
                        headlineContent = { Text(productName) },
                        supportingContent = { Text(offer.packageDescription) },
                        trailingContent = { Text(offer.latestPriceMinor.centsToEuro(), fontWeight = FontWeight.Medium) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Delete supplier?",
            message = "This removes ${supplier?.companyName} and its recorded prices.",
            confirmLabel = "Delete",
            onConfirm = { showDeleteConfirm = false; viewModel.deleteSupplier() },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
