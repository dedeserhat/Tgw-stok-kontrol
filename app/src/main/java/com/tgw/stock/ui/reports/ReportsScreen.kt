package com.tgw.stock.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgw.stock.ui.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(onBack: () -> Unit) {
    val viewModel: ReportsViewModel = tgwViewModel { ReportsViewModel(it.reportsRepository) }
    val period by viewModel.period.collectAsStateWithLifecycle()
    val data by viewModel.data.collectAsStateWithLifecycle()
    var showCustomRangeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyRow(contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = period == ReportPeriod.TODAY, onClick = { viewModel.setPeriod(ReportPeriod.TODAY) }, label = { Text("Today") }) }
                item { FilterChip(selected = period == ReportPeriod.LAST_7_DAYS, onClick = { viewModel.setPeriod(ReportPeriod.LAST_7_DAYS) }, label = { Text("7 Days") }) }
                item { FilterChip(selected = period == ReportPeriod.LAST_30_DAYS, onClick = { viewModel.setPeriod(ReportPeriod.LAST_30_DAYS) }, label = { Text("30 Days") }) }
                item { FilterChip(selected = period == ReportPeriod.CUSTOM, onClick = { showCustomRangeDialog = true }, label = { Text("Custom") }) }
            }

            val d = data
            if (d == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatCard("Current Stock Value", d.currentStockValueMinor.centsToEuro(), modifier = Modifier.weight(1f))
                            StatCard("Waste Cost", d.wasteCostMinor.centsToEuro(), modifier = Modifier.weight(1f), valueColor = com.tgw.stock.ui.theme.StatusOutOfStock)
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatCard("Purchase Spend", d.purchaseSpendMinor.centsToEuro(), modifier = Modifier.weight(1f))
                            StatCard("Price Changes", d.priceChanges.size.toString(), modifier = Modifier.weight(1f))
                        }
                    }

                    item { SectionHeader("Most Used Ingredients") }
                    if (d.mostUsedIngredients.isEmpty()) {
                        item { EmptyState("No usage recorded in this period") }
                    } else {
                        items(d.mostUsedIngredients) { (name, qty) ->
                            ReportLine(name, qty.formatQuantity())
                        }
                    }

                    item { SectionHeader("Supplier Spend") }
                    if (d.supplierSpend.isEmpty()) {
                        item { EmptyState("No purchases recorded in this period") }
                    } else {
                        items(d.supplierSpend) { (name, cents) -> ReportLine(name, cents.centsToEuro()) }
                    }

                    item { SectionHeader("Low Stock Frequency") }
                    if (d.lowStockFrequency.isEmpty()) {
                        item { EmptyState("No completed stock counts flagged low stock in this period") }
                    } else {
                        items(d.lowStockFrequency) { (name, count) -> ReportLine(name, "$count times") }
                    }

                    item { SectionHeader("Recent Price Changes") }
                    if (d.priceChanges.isEmpty()) {
                        item { EmptyState("No supplier price changes in this period") }
                    } else {
                        items(d.priceChanges) { change ->
                            ListItem(
                                headlineContent = { Text("${change.productName} · ${change.supplierName}") },
                                supportingContent = { Text("${change.oldPriceMinor.centsToEuro()} → ${change.newPriceMinor.centsToEuro()}") },
                                trailingContent = {
                                    val color = if (change.changePercent >= 0) com.tgw.stock.ui.theme.StatusCritical else com.tgw.stock.ui.theme.StatusNormal
                                    Text("${if (change.changePercent >= 0) "+" else ""}${change.changePercent.formatPercent()}", color = color)
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (showCustomRangeDialog) {
        CustomRangeDialog(
            onDismiss = { showCustomRangeDialog = false },
            onConfirm = { from, to -> viewModel.setCustomRange(from, to); showCustomRangeDialog = false }
        )
    }
}

@Composable
private fun ReportLine(label: String, value: String) {
    ListItem(headlineContent = { Text(label) }, trailingContent = { Text(value, fontWeight = FontWeight.Medium) })
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangeDialog(onDismiss: () -> Unit, onConfirm: (Long, Long) -> Unit) {
    val fromState = rememberDatePickerState()
    val toState = rememberDatePickerState()
    var step by remember { mutableStateOf(0) }

    if (step == 0) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = { TextButton(onClick = { step = 1 }) { Text("Next") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        ) { DatePicker(state = fromState, title = { Text("From date") }) }
    } else {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val from = fromState.selectedDateMillis ?: System.currentTimeMillis()
                    val to = toState.selectedDateMillis ?: System.currentTimeMillis()
                    onConfirm(minOf(from, to), maxOf(from, to))
                }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        ) { DatePicker(state = toState, title = { Text("To date") }) }
    }
}
