package com.tgw.stock.ui.movements

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgw.stock.domain.StockCountStatus
import com.tgw.stock.ui.common.formatQuantityWithUnit
import com.tgw.stock.ui.common.tgwViewModel
import com.tgw.stock.ui.theme.StatusCritical
import com.tgw.stock.ui.theme.StatusNormal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockCountDetailScreen(
    countId: Long,
    onBack: () -> Unit
) {
    val viewModel: StockCountDetailViewModel = tgwViewModel {
        StockCountDetailViewModel(countId, it.stockCountRepository, it.productRepository)
    }
    val count by viewModel.count.collectAsStateWithLifecycle()
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val isCommitting by viewModel.isCommitting.collectAsStateWithLifecycle()
    var showConfirm by remember { mutableStateOf(false) }

    val isCompleted = count?.status == StockCountStatus.COMPLETED.name

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock Count", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        bottomBar = {
            if (!isCompleted) {
                Surface(shadowElevation = 8.dp) {
                    Button(
                        onClick = { showConfirm = true },
                        enabled = !isCommitting,
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp)
                    ) {
                        if (isCommitting) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text("Confirm Count & Adjust Stock")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
            items(rows, key = { it.itemId }) { row ->
                StockCountRowItem(row, isCompleted, onCountedChanged = { viewModel.updateCounted(row.itemId, it) })
                HorizontalDivider()
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Confirm stock count?") },
            text = { Text("Every counted item will update the system stock level. Uncounted items are left unchanged.") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    viewModel.commit(onDone = onBack)
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StockCountRowItem(row: StockCountRow, readOnly: Boolean, onCountedChanged: (Double?) -> Unit) {
    var text by remember(row.itemId) { mutableStateOf(row.countedQuantity?.toString() ?: "") }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(row.productName, fontWeight = FontWeight.Medium)
        Text("System: ${formatQuantityWithUnit(row.systemQuantity, row.unit)}", style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    onCountedChanged(it.toDoubleOrNull())
                },
                label = { Text("Counted") },
                enabled = !readOnly,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(140.dp)
            )
            val diff = row.difference
            if (diff != null) {
                val color = if (diff == 0.0) StatusNormal else StatusCritical
                Text(
                    "Difference: ${if (diff > 0) "+" else ""}${formatQuantityWithUnit(diff, row.unit)}",
                    color = color,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
