package com.empathy.ai.data.local

import android.content.Context
import com.empathy.ai.data.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 提示词配置文件备份机制
 *
 * 提供配置文件的备份和恢复功能，防止配置损坏导致数据丢失
 */
@Singleton
class PromptFileBackup @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private const val BACKUP_DIR = "prompts_backup"
        private const val MAX_BACKUPS = 3
        private const val BACKUP_PREFIX = "backup_"
        private const val BACKUP_SUFFIX = ".json"
    }

    private val backupDir: File
        get() = File(context.filesDir, BACKUP_DIR).also { it.mkdirs() }

    suspend fun createBackup(sourceFile: File): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (!sourceFile.exists()) return@withContext Result.success(Unit)
            backupDir.mkdirs()
            val timestamp = System.currentTimeMillis()
            val backupFile = File(backupDir, "$BACKUP_PREFIX$timestamp$BACKUP_SUFFIX")
            sourceFile.copyTo(backupFile, overwrite = true)
            cleanOldBackups()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun restoreFromLatestBackup(targetFile: File): Result<Unit> = withContext(ioDispatcher) {
        try {
            val latestBackup = findLatestBackup()
                ?: return@withContext Result.failure(Exception("无可用备份"))
            targetFile.parentFile?.mkdirs()
            latestBackup.copyTo(targetFile, overwrite = true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listBackups(): List<File> = withContext(ioDispatcher) {
        backupDir.listFiles()
            ?.filter { it.name.startsWith(BACKUP_PREFIX) && it.name.endsWith(BACKUP_SUFFIX) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    private fun cleanOldBackups() {
        val backups = backupDir.listFiles()
            ?.filter { it.name.startsWith(BACKUP_PREFIX) && it.name.endsWith(BACKUP_SUFFIX) }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        backups.drop(MAX_BACKUPS).forEach { it.delete() }
    }

    private fun findLatestBackup(): File? {
        return backupDir.listFiles()
            ?.filter { it.name.startsWith(BACKUP_PREFIX) && it.name.endsWith(BACKUP_SUFFIX) }
            ?.maxByOrNull { it.lastModified() }
    }
}
