package com.tgw.stock.ui.movements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgw.stock.domain.StockCountStatus
import com.tgw.stock.ui.common.EmptyState
import com.tgw.stock.ui.common.formatDate
import com.tgw.stock.ui.common.tgwViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockCountListScreen(
    onBack: () -> Unit,
    onOpenCount: (Long) -> Unit
) {
    val viewModel: StockCountListViewModel = tgwViewModel { StockCountListViewModel(it.stockCountRepository) }
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    val newCountId by viewModel.newCountId.collectAsStateWithLifecycle()

    LaunchedEffect(newCountId) {
        newCountId?.let {
            viewModel.consumeNewCountId()
            onOpenCount(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock Counts", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = viewModel::startNewCount, icon = { Icon(Icons.Default.Add, contentDescription = null) }, text = { Text("New Count") })
        }
    ) { padding ->
        if (counts.isEmpty()) {
            EmptyState("No stock counts yet. Start one to compare physical stock against the system.", modifier = Modifier.padding(padding))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(counts) { count ->
                    ListItem(
                        headlineContent = { Text(count.countDate.formatDate()) },
                        supportingContent = { Text(if (count.status == StockCountStatus.COMPLETED.name) "Completed" else "In progress") },
                        modifier = Modifier.clickable { onOpenCount(count.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
