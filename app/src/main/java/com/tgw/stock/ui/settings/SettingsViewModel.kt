package com.tgw.stock.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgw.stock.data.backup.BackupManager
import com.tgw.stock.data.backup.BackupResult
import com.tgw.stock.data.csv.CsvExporter
import com.tgw.stock.data.repository.MaintenanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class SettingsEvent {
    data object BackupSucceeded : SettingsEvent()
    data class BackupFailed(val message: String) : SettingsEvent()
    data object RestoreSucceeded : SettingsEvent()
    data class RestoreFailed(val message: String) : SettingsEvent()
    data class CsvReady(val files: List<File>) : SettingsEvent()
    data object SampleDataCleared : SettingsEvent()
}

class SettingsViewModel(
    private val backupManager: BackupManager,
    private val csvExporter: CsvExporter,
    private val maintenanceRepository: MaintenanceRepository
) : ViewModel() {

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _event = MutableStateFlow<SettingsEvent?>(null)
    val event: StateFlow<SettingsEvent?> = _event.asStateFlow()

    fun consumeEvent() { _event.value = null }

    fun backup(destination: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            _event.value = when (val result = backupManager.backupTo(destination)) {
                is BackupResult.Success -> SettingsEvent.BackupSucceeded
                is BackupResult.Failure -> SettingsEvent.BackupFailed(result.message)
            }
            _isBusy.value = false
        }
    }

    fun restore(source: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            _event.value = when (val result = backupManager.restoreFrom(source)) {
                is BackupResult.Success -> SettingsEvent.RestoreSucceeded
                is BackupResult.Failure -> SettingsEvent.RestoreFailed(result.message)
            }
            _isBusy.value = false
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            _isBusy.value = true
            val files = csvExporter.exportAll()
            _event.value = SettingsEvent.CsvReady(files)
            _isBusy.value = false
        }
    }

    fun clearSampleData() {
        viewModelScope.launch {
            _isBusy.value = true
            maintenanceRepository.clearSampleData()
            _event.value = SettingsEvent.SampleDataCleared
            _isBusy.value = false
        }
    }
}
