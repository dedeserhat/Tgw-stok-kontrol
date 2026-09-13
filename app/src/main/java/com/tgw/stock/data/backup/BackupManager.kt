package com.tgw.stock.data.backup

import android.content.Context
import android.net.Uri
import com.tgw.stock.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed class BackupResult {
    data object Success : BackupResult()
    data class Failure(val message: String) : BackupResult()
}

/**
 * Single-file backup/restore for the offline SQLite database.
 *
 * Backup keeps the live Room instance open: it forces a WAL checkpoint with a raw
 * PRAGMA (flushing pending writes into the main .db file) and then copies that file,
 * so the rest of the app keeps working normally right after a backup.
 *
 * Restore is different - it replaces the .db file on disk, so the live Room instance
 * (and every DAO/repository built on top of it) would otherwise keep querying a
 * database object that no longer matches what's on disk. It closes the singleton and
 * relies on the caller to reinitialize the app container / restart the app before
 * touching the database again (see SettingsScreen's restart prompt).
 */
class BackupManager(private val context: Context) {

    private fun dbFile(): File = context.getDatabasePath(AppDatabase.DATABASE_NAME)

    suspend fun backupTo(destination: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val source = dbFile()
            if (!source.exists()) return@withContext BackupResult.Failure("No database to back up yet")
            AppDatabase.getInstance(context).query("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
            context.contentResolver.openOutputStream(destination)?.use { out ->
                source.inputStream().use { input -> input.copyTo(out) }
            } ?: return@withContext BackupResult.Failure("Could not open destination file")
            BackupResult.Success
        } catch (e: Exception) {
            BackupResult.Failure(e.message ?: "Backup failed")
        }
    }

    suspend fun restoreFrom(source: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            AppDatabase.closeInstance()
            val destination = dbFile()
            destination.parentFile?.mkdirs()
            context.contentResolver.openInputStream(source)?.use { input ->
                destination.outputStream().use { out -> input.copyTo(out) }
            } ?: return@withContext BackupResult.Failure("Could not open backup file")
            // Drop any stale WAL/SHM files from the previous database so the restored
            // file is read cleanly on next open.
            File(destination.path + "-wal").delete()
            File(destination.path + "-shm").delete()
            BackupResult.Success
        } catch (e: Exception) {
            BackupResult.Failure(e.message ?: "Restore failed")
        }
    }
}
