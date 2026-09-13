package com.tgw.stock.ui.suppliers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgw.stock.ui.common.EmptyState
import com.tgw.stock.ui.common.tgwViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierListScreen(
    onBack: () -> Unit,
    onSupplierClick: (Long) -> Unit,
    onAddSupplier: () -> Unit
) {
    val viewModel: SupplierListViewModel = tgwViewModel { SupplierListViewModel(it.supplierRepository) }
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suppliers", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSupplier) { Icon(Icons.Default.Add, contentDescription = "Add supplier") }
        }
    ) { padding ->
        if (suppliers.isEmpty()) {
            EmptyState("No suppliers yet.", modifier = Modifier.padding(padding))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(suppliers, key = { it.id }) { supplier ->
                    ListItem(
                        headlineContent = { Text(supplier.companyName, fontWeight = FontWeight.Medium) },
                        supportingContent = { Text(listOfNotNull(supplier.phone, supplier.email).joinToString(" · ")) },
                        modifier = Modifier.clickable { onSupplierClick(supplier.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
