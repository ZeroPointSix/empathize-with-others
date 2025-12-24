package com.empathy.ai.data.local

import android.content.Context
import com.empathy.ai.domain.model.UserProfile
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户画像备份管理器
 *
 * 实现自动备份机制，每次修改前创建备份，最多保留5个历史版本。
 */
@Singleton
class UserProfileBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi
) {
    companion object {
        private const val BACKUP_DIR = "user_profile_backups"
        private const val BACKUP_PREFIX = "profile_backup_"
        private const val BACKUP_EXTENSION = ".json"
        private const val MAX_BACKUP_VERSIONS = 5
    }
    
    private val backupDir: File by lazy {
        File(context.filesDir, BACKUP_DIR).apply { if (!exists()) mkdirs() }
    }
    
    private val adapter by lazy { moshi.adapter(UserProfile::class.java) }
    
    fun createBackup(profile: UserProfile): Result<Unit> {
        return try {
            cleanupOldBackups()
            val timestamp = System.currentTimeMillis()
            val backupFile = File(backupDir, "$BACKUP_PREFIX$timestamp$BACKUP_EXTENSION")
            backupFile.writeText(adapter.toJson(profile))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("创建备份失败: ${e.message}", e))
        }
    }
    
    fun restoreFromBackup(version: Int): Result<UserProfile> {
        return try {
            val backups = getBackupFiles()
            if (version < 0 || version >= backups.size) {
                return Result.failure(Exception("备份版本不存在: $version"))
            }
            val json = backups[version].readText()
            val profile = adapter.fromJson(json) ?: return Result.failure(Exception("备份数据解析失败"))
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(Exception("恢复备份失败: ${e.message}", e))
        }
    }

    
    fun getBackupVersions(): List<BackupInfo> {
        return try {
            getBackupFiles().mapIndexed { index, file ->
                BackupInfo(
                    version = index,
                    timestamp = extractTimestamp(file.name),
                    fileName = file.name,
                    fileSize = file.length()
                )
            }
        } catch (e: Exception) { emptyList() }
    }
    
    fun cleanupOldBackups() {
        try {
            val backups = getBackupFiles()
            if (backups.size >= MAX_BACKUP_VERSIONS) {
                backups.drop(MAX_BACKUP_VERSIONS - 1).forEach { it.delete() }
            }
        } catch (e: Exception) { /* 清理失败不影响主流程 */ }
    }
    
    fun deleteAllBackups(): Result<Unit> {
        return try {
            getBackupFiles().forEach { it.delete() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("删除备份失败: ${e.message}", e))
        }
    }
    
    fun getBackupCount(): Int = try { getBackupFiles().size } catch (e: Exception) { 0 }
    
    fun hasBackups(): Boolean = getBackupCount() > 0
    
    private fun getBackupFiles(): List<File> {
        return backupDir.listFiles { file ->
            file.name.startsWith(BACKUP_PREFIX) && file.name.endsWith(BACKUP_EXTENSION)
        }?.sortedByDescending { extractTimestamp(it.name) } ?: emptyList()
    }
    
    private fun extractTimestamp(fileName: String): Long {
        return try {
            fileName.removePrefix(BACKUP_PREFIX).removeSuffix(BACKUP_EXTENSION).toLong()
        } catch (e: Exception) { 0L }
    }
}

data class BackupInfo(
    val version: Int,
    val timestamp: Long,
    val fileName: String,
    val fileSize: Long
)
