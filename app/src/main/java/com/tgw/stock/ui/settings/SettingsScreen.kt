package com.tgw.stock.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Process
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgw.stock.TgwStockApp
import com.tgw.stock.ui.common.ConfirmDialog
import com.tgw.stock.ui.common.tgwViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = tgwViewModel { SettingsViewModel(it.backupManager, it.csvExporter, it.maintenanceRepository) }
    val context = LocalContext.current
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val event by viewModel.event.collectAsStateWithLifecycle()
    val themeMode by AppSettings.themeMode.collectAsStateWithLifecycle()

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showClearSampleConfirm by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestartPrompt by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { viewModel.backup(it) }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { pendingRestoreUri = it; showRestoreConfirm = true }
    }

    LaunchedEffect(event) {
        when (val e = event) {
            is SettingsEvent.BackupSucceeded -> { snackbarMessage = "Backup saved successfully"; viewModel.consumeEvent() }
            is SettingsEvent.BackupFailed -> { snackbarMessage = "Backup failed: ${e.message}"; viewModel.consumeEvent() }
            is SettingsEvent.RestoreSucceeded -> { showRestartPrompt = true; viewModel.consumeEvent() }
            is SettingsEvent.RestoreFailed -> { snackbarMessage = "Restore failed: ${e.message}"; viewModel.consumeEvent() }
            is SettingsEvent.SampleDataCleared -> { snackbarMessage = "Sample data removed"; viewModel.consumeEvent() }
            is SettingsEvent.CsvReady -> {
                shareCsvFiles(context, e.files)
                viewModel.consumeEvent()
            }
            null -> {}
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { snackbarHostState.showSnackbar(it); snackbarMessage = null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

            SettingsSection(title = "Appearance") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = themeMode == mode,
                            onClick = { AppSettings.setThemeMode(context, mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size)
                        ) { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    }
                }
            }

            SettingsSection(title = "Backup & Restore") {
                Text("Save your entire database to a single file, or restore from a previous backup.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { backupLauncher.launch(defaultBackupFileName()) },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Backup Database") }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { restoreLauncher.launch(arrayOf("*/*")) },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Restore from Backup") }
            }

            SettingsSection(title = "Export") {
                Text("Export products, suppliers, recipes, purchase orders, movements and waste as CSV files.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = viewModel::exportCsv, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) { Text("Export CSV") }
            }

            SettingsSection(title = "Sample Data") {
                Text("Removes the demo products, suppliers and recipes created on first launch.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showClearSampleConfirm = true },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Clear Sample Data") }
            }

            if (isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showClearSampleConfirm) {
        ConfirmDialog(
            title = "Clear sample data?",
            message = "This removes the demo products, suppliers, recipes and movements created on first launch. Your own data is not affected.",
            confirmLabel = "Clear",
            onConfirm = { showClearSampleConfirm = false; viewModel.clearSampleData() },
            onDismiss = { showClearSampleConfirm = false }
        )
    }

    if (showRestoreConfirm) {
        ConfirmDialog(
            title = "Restore this backup?",
            message = "This replaces all current data with the contents of the backup file. This cannot be undone.",
            confirmLabel = "Restore",
            onConfirm = {
                showRestoreConfirm = false
                pendingRestoreUri?.let { viewModel.restore(it) }
            },
            onDismiss = { showRestoreConfirm = false; pendingRestoreUri = null }
        )
    }

    if (showRestartPrompt) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Restore complete") },
            text = { Text("The app needs to restart to load the restored data.") },
            confirmButton = {
                TextButton(onClick = { restartApp(context) }) { Text("Restart Now") }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

private fun defaultBackupFileName(): String {
    val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(java.util.Date())
    return "tgw_stock_backup_$stamp.db"
}

private fun shareCsvFiles(context: android.content.Context, files: List<java.io.File>) {
    if (files.isEmpty()) return
    val uris = files.map { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it) }
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "text/csv"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export CSV").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
}

private fun restartApp(context: android.content.Context) {
    (context.applicationContext as TgwStockApp).reinitializeContainer()
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    Process.killProcess(Process.myPid())
}
