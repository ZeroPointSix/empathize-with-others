package com.empathy.ai.build

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

/**
 * 备份元数据
 * 
 * @property timestamp 备份时间戳
 * @property files 备份的文件列表
 * @property createdAt 创建时间（毫秒）
 * @property version 备份时的版本号
 * @property stage 备份时的发布阶段
 */
@Serializable
data class BackupMetadata(
    val timestamp: String,
    val files: List<String>,
    val createdAt: Long,
    val version: String? = null,
    val stage: String? = null
)

/**
 * 备份结果
 * 
 * @property backupPath 备份目录路径
 * @property timestamp 备份时间戳
 * @property fileCount 备份文件数量
 */
data class BackupResult(
    val backupPath: String,
    val timestamp: String,
    val fileCount: Int
)

/**
 * 恢复结果
 * 
 * @property restoredFiles 恢复的文件数量
 * @property timestamp 原备份时间戳
 */
data class RestoreResult(
    val restoredFiles: Int,
    val timestamp: String
)

/**
 * 备份管理器
 * 负责文件备份和回滚操作
 * 
 * @property projectDir 项目根目录
 * 
 * @see TDD-00024 4.2.5 BackupManager备份管理器
 */
class BackupManager(private val projectDir: File) {
    
    companion object {
        /** 备份目录名 */
        private const val BACKUP_DIR = "backups/version-update"
        
        /** 最大备份数量 */
        private const val MAX_BACKUPS = 50
        
        /** 备份保留天数 */
        private const val RETENTION_DAYS = 30
        
        /** 时间戳格式 */
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
    }
    
    private val backupDir = File(projectDir, BACKUP_DIR)
    
    private val json = Json { 
        prettyPrint = true 
        ignoreUnknownKeys = true
    }
    
    /** 支持的图标密度 */
    private val densities = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
    
    /**
     * 创建完整备份
     * @param version 当前版本号（可选）
     * @param stage 当前发布阶段（可选）
     * @return 备份结果
     */
    fun createBackup(version: String? = null, stage: String? = null): BackupResult {
        val timestamp = DATE_FORMAT.format(Date())
        val backupName = "backup-$timestamp"
        val backupPath = File(backupDir, backupName)
        
        backupPath.mkdirs()
        
        // 备份文件列表
        val filesToBackup = listOf(
            "gradle.properties",
            "config/version-config.json",
            "config/version-history.json",
            "config/icon-mapping.json"
        )
        
        // 备份图标目录
        val iconDirs = densities.map { "app/src/main/res/mipmap-$it" }
        
        var fileCount = 0
        val backedUpFiles = mutableListOf<String>()
        
        // 复制配置文件
        filesToBackup.forEach { relativePath ->
            val source = File(projectDir, relativePath)
            if (source.exists()) {
                val target = File(backupPath, relativePath)
                target.parentFile?.mkdirs()
                source.copyTo(target, overwrite = true)
                fileCount++
                backedUpFiles.add(relativePath)
            }
        }
        
        // 复制图标目录
        iconDirs.forEach { relativePath ->
            val source = File(projectDir, relativePath)
            if (source.exists() && source.isDirectory) {
                val target = File(backupPath, relativePath)
                source.copyRecursively(target, overwrite = true)
                fileCount += source.listFiles()?.size ?: 0
                backedUpFiles.add(relativePath)
            }
        }
        
        // 创建备份元数据
        val metadata = BackupMetadata(
            timestamp = timestamp,
            files = backedUpFiles,
            createdAt = System.currentTimeMillis(),
            version = version,
            stage = stage
        )
        File(backupPath, "metadata.json").writeText(
            json.encodeToString(BackupMetadata.serializer(), metadata)
        )
        
        // 清理旧备份
        cleanupOldBackups()
        
        return BackupResult(
            backupPath = backupPath.absolutePath,
            timestamp = timestamp,
            fileCount = fileCount
        )
    }
    
    /**
     * 备份图标文件
     */
    fun backupIcons(): BackupResult {
        val timestamp = System.currentTimeMillis().toString()
        val iconBackupDir = File(backupDir, "icons-$timestamp")
        iconBackupDir.mkdirs()
        
        var fileCount = 0
        
        densities.forEach { density ->
            val source = File(projectDir, "app/src/main/res/mipmap-$density")
            if (source.exists()) {
                val target = File(iconBackupDir, "mipmap-$density")
                source.copyRecursively(target, overwrite = true)
                fileCount += source.listFiles()?.size ?: 0
            }
        }
        
        return BackupResult(
            backupPath = iconBackupDir.absolutePath,
            timestamp = timestamp,
            fileCount = fileCount
        )
    }
    
    /**
     * 恢复图标文件
     * @return 是否成功恢复
     */
    fun restoreIcons(): Boolean {
        // 查找最近的图标备份
        val iconBackups = backupDir.listFiles()
            ?.filter { it.name.startsWith("icons-") }
            ?.sortedByDescending { it.name }
            ?: return false
        
        val latestBackup = iconBackups.firstOrNull() ?: return false
        
        var restored = false
        densities.forEach { density ->
            val source = File(latestBackup, "mipmap-$density")
            if (source.exists()) {
                val target = File(projectDir, "app/src/main/res/mipmap-$density")
                target.mkdirs()
                source.copyRecursively(target, overwrite = true)
                restored = true
            }
        }
        
        return restored
    }
    
    /**
     * 从备份恢复
     * @param backupResult 备份结果
     * @return 恢复结果
     */
    fun restore(backupResult: BackupResult): RestoreResult {
        val backupPath = File(backupResult.backupPath)
        if (!backupPath.exists()) {
            throw IllegalStateException("备份目录不存在: ${backupResult.backupPath}")
        }
        
        val metadataFile = File(backupPath, "metadata.json")
        val metadata = if (metadataFile.exists()) {
            json.decodeFromString<BackupMetadata>(metadataFile.readText())
        } else {
            // 兼容旧格式备份
            BackupMetadata(
                timestamp = backupResult.timestamp,
                files = emptyList(),
                createdAt = System.currentTimeMillis()
            )
        }
        
        var restoredCount = 0
        
        // 恢复文件
        metadata.files.forEach { relativePath ->
            val source = File(backupPath, relativePath)
            val target = File(projectDir, relativePath)
            
            if (source.exists()) {
                if (source.isDirectory) {
                    target.mkdirs()
                    source.copyRecursively(target, overwrite = true)
                    restoredCount += source.listFiles()?.size ?: 1
                } else {
                    target.parentFile?.mkdirs()
                    source.copyTo(target, overwrite = true)
                    restoredCount++
                }
            }
        }
        
        // 如果没有元数据文件，尝试恢复所有内容
        if (metadata.files.isEmpty()) {
            backupPath.listFiles()?.forEach { file ->
                if (file.name != "metadata.json") {
                    val target = File(projectDir, file.name)
                    if (file.isDirectory) {
                        file.copyRecursively(target, overwrite = true)
                    } else {
                        file.copyTo(target, overwrite = true)
                    }
                    restoredCount++
                }
            }
        }
        
        return RestoreResult(
            restoredFiles = restoredCount,
            timestamp = backupResult.timestamp
        )
    }
    
    /**
     * 获取最近的备份
     * @return 最近的备份结果，如果没有备份则返回null
     */
    fun getLatestBackup(): BackupResult? {
        val backups = backupDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("backup-") }
            ?.sortedByDescending { it.name }
            ?: return null
        
        val latestBackup = backups.firstOrNull() ?: return null
        
        val metadataFile = File(latestBackup, "metadata.json")
        val metadata = if (metadataFile.exists()) {
            try {
                json.decodeFromString<BackupMetadata>(metadataFile.readText())
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
        
        return BackupResult(
            backupPath = latestBackup.absolutePath,
            timestamp = metadata?.timestamp ?: latestBackup.name.removePrefix("backup-"),
            fileCount = latestBackup.listFiles()?.size ?: 0
        )
    }
    
    /**
     * 获取所有备份列表
     * @return 备份列表（按时间倒序）
     */
    fun listBackups(): List<BackupResult> {
        val backups = backupDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("backup-") }
            ?.sortedByDescending { it.name }
            ?: return emptyList()
        
        return backups.map { backupPath ->
            val metadataFile = File(backupPath, "metadata.json")
            val metadata = if (metadataFile.exists()) {
                try {
                    json.decodeFromString<BackupMetadata>(metadataFile.readText())
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
            
            BackupResult(
                backupPath = backupPath.absolutePath,
                timestamp = metadata?.timestamp ?: backupPath.name.removePrefix("backup-"),
                fileCount = backupPath.listFiles()?.size ?: 0
            )
        }
    }
    
    /**
     * 删除指定备份
     * @param backupResult 要删除的备份
     * @return 是否成功删除
     */
    fun deleteBackup(backupResult: BackupResult): Boolean {
        val backupPath = File(backupResult.backupPath)
        return if (backupPath.exists()) {
            backupPath.deleteRecursively()
        } else {
            false
        }
    }
    
    /**
     * 清理旧备份
     */
    fun cleanupOldBackups() {
        if (!backupDir.exists()) return
        
        val backups = backupDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("backup-") }
            ?.sortedByDescending { it.name }
            ?: return
        
        // 保留最近N个备份
        if (backups.size > MAX_BACKUPS) {
            backups.drop(MAX_BACKUPS).forEach { it.deleteRecursively() }
        }
        
        // 删除超过保留期限的备份
        val cutoffTime = System.currentTimeMillis() - RETENTION_DAYS * 24 * 60 * 60 * 1000L
        backups.forEach { backup ->
            val metadataFile = File(backup, "metadata.json")
            if (metadataFile.exists()) {
                try {
                    val metadata = json.decodeFromString<BackupMetadata>(metadataFile.readText())
                    if (metadata.createdAt < cutoffTime) {
                        backup.deleteRecursively()
                    }
                } catch (e: Exception) {
                    // 忽略解析错误
                }
            }
        }
        
        // 清理图标备份（只保留最近5个）
        val iconBackups = backupDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("icons-") }
            ?.sortedByDescending { it.name }
            ?: return
        
        if (iconBackups.size > 5) {
            iconBackups.drop(5).forEach { it.deleteRecursively() }
        }
    }
    
    /**
     * 获取备份目录大小（字节）
     */
    fun getBackupDirSize(): Long {
        if (!backupDir.exists()) return 0
        return backupDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
    
    /**
     * 检查备份目录是否存在
     */
    fun hasBackups(): Boolean {
        return backupDir.exists() && (backupDir.listFiles()?.isNotEmpty() == true)
    }
}
