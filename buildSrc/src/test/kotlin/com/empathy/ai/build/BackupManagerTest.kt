package com.empathy.ai.build

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * BackupManager 单元测试
 * 
 * @see TDD-00024 6.1 单元测试
 * @see TD-00024 T021
 */
class BackupManagerTest {
    
    private lateinit var tempDir: File
    private lateinit var backupManager: BackupManager
    
    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "backup-manager-test-${System.currentTimeMillis()}")
        tempDir.mkdirs()
        backupManager = BackupManager(tempDir)
    }
    
    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }
    
    // ==================== 备份创建测试 ====================
    
    @Test
    fun `createBackup - 创建空备份`() {
        val result = backupManager.createBackup()
        
        assertNotNull(result)
        assertTrue(result.backupPath.isNotEmpty())
        assertTrue(File(result.backupPath).exists())
    }
    
    @Test
    fun `createBackup - 备份配置文件`() {
        // 准备配置文件
        File(tempDir, "gradle.properties").writeText("version=1.0.0")
        File(tempDir, "config").mkdirs()
        File(tempDir, "config/version-config.json").writeText("{}")
        
        val result = backupManager.createBackup(version = "1.0.0", stage = "dev")
        
        assertTrue(result.fileCount >= 2)
        assertTrue(File(result.backupPath, "gradle.properties").exists())
        assertTrue(File(result.backupPath, "config/version-config.json").exists())
    }
    
    @Test
    fun `createBackup - 备份图标目录`() {
        // 准备图标目录
        val mipmapDir = File(tempDir, "app/src/main/res/mipmap-xxxhdpi")
        mipmapDir.mkdirs()
        File(mipmapDir, "ic_launcher.png").writeBytes("icon".toByteArray())
        
        val result = backupManager.createBackup()
        
        assertTrue(result.fileCount >= 1)
        assertTrue(File(result.backupPath, "app/src/main/res/mipmap-xxxhdpi/ic_launcher.png").exists())
    }
    
    @Test
    fun `createBackup - 创建元数据文件`() {
        val result = backupManager.createBackup(version = "2.0.0", stage = "beta")
        
        val metadataFile = File(result.backupPath, "metadata.json")
        assertTrue(metadataFile.exists())
        
        val content = metadataFile.readText()
        assertTrue(content.contains("2.0.0"))
        assertTrue(content.contains("beta"))
    }
    
    // ==================== 图标备份测试 ====================
    
    @Test
    fun `backupIcons - 备份所有密度图标`() {
        // 准备各密度图标
        listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi").forEach { density ->
            val dir = File(tempDir, "app/src/main/res/mipmap-$density")
            dir.mkdirs()
            File(dir, "ic_launcher.png").writeBytes("icon-$density".toByteArray())
        }
        
        val result = backupManager.backupIcons()
        
        assertTrue(result.fileCount >= 5)
        assertTrue(File(result.backupPath).exists())
    }
    
    @Test
    fun `restoreIcons - 恢复图标`() {
        // 准备并备份图标
        val xxxhdpiDir = File(tempDir, "app/src/main/res/mipmap-xxxhdpi")
        xxxhdpiDir.mkdirs()
        File(xxxhdpiDir, "ic_launcher.png").writeBytes("original".toByteArray())
        
        backupManager.backupIcons()
        
        // 修改图标
        File(xxxhdpiDir, "ic_launcher.png").writeBytes("modified".toByteArray())
        
        // 恢复
        val restored = backupManager.restoreIcons()
        
        assertTrue(restored)
        assertEquals("original", File(xxxhdpiDir, "ic_launcher.png").readText())
    }
    
    // ==================== 恢复测试 ====================
    
    @Test
    fun `restore - 从备份恢复`() {
        // 准备并创建备份
        File(tempDir, "gradle.properties").writeText("version=1.0.0")
        val backupResult = backupManager.createBackup()
        
        // 修改文件
        File(tempDir, "gradle.properties").writeText("version=2.0.0")
        
        // 恢复
        val restoreResult = backupManager.restore(backupResult)
        
        assertTrue(restoreResult.restoredFiles >= 1)
        assertEquals("version=1.0.0", File(tempDir, "gradle.properties").readText())
    }
    
    @Test
    fun `restore - 备份目录不存在时抛出异常`() {
        val fakeBackup = BackupResult(
            backupPath = "/nonexistent/path",
            timestamp = "2024-01-01",
            fileCount = 0
        )
        
        try {
            backupManager.restore(fakeBackup)
            assertTrue(false, "应该抛出异常")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("备份目录不存在") == true)
        }
    }
    
    // ==================== 备份列表测试 ====================
    
    @Test
    fun `getLatestBackup - 没有备份时返回null`() {
        val latest = backupManager.getLatestBackup()
        
        assertNull(latest)
    }
    
    @Test
    fun `getLatestBackup - 返回最新备份`() {
        // 创建多个备份
        backupManager.createBackup()
        Thread.sleep(1100) // 等待超过1秒以确保时间戳不同
        backupManager.createBackup()
        
        val latest = backupManager.getLatestBackup()
        
        assertNotNull(latest)
        // 验证返回了备份
        assertTrue(latest.backupPath.isNotEmpty())
    }
    
    @Test
    fun `listBackups - 列出所有备份`() {
        backupManager.createBackup()
        Thread.sleep(1100)
        backupManager.createBackup()
        Thread.sleep(1100)
        backupManager.createBackup()
        
        val backups = backupManager.listBackups()
        
        assertTrue(backups.size >= 3, "应该有至少3个备份，实际: ${backups.size}")
    }
    
    @Test
    fun `listBackups - 按时间倒序排列`() {
        backupManager.createBackup()
        Thread.sleep(1100) // 等待超过1秒以确保时间戳不同
        backupManager.createBackup()
        
        val backups = backupManager.listBackups()
        
        assertTrue(backups.size >= 2, "应该有至少2个备份")
        assertTrue(backups[0].timestamp >= backups[1].timestamp)
    }
    
    // ==================== 删除备份测试 ====================
    
    @Test
    fun `deleteBackup - 删除指定备份`() {
        val backup = backupManager.createBackup()
        assertTrue(File(backup.backupPath).exists())
        
        val deleted = backupManager.deleteBackup(backup)
        
        assertTrue(deleted)
        assertFalse(File(backup.backupPath).exists())
    }
    
    @Test
    fun `deleteBackup - 删除不存在的备份返回false`() {
        val fakeBackup = BackupResult(
            backupPath = "/nonexistent/path",
            timestamp = "2024-01-01",
            fileCount = 0
        )
        
        val deleted = backupManager.deleteBackup(fakeBackup)
        
        assertFalse(deleted)
    }
    
    // ==================== 清理测试 ====================
    
    @Test
    fun `cleanupOldBackups - 保留最近N个备份`() {
        // 创建超过限制的备份
        repeat(55) {
            backupManager.createBackup()
        }
        
        backupManager.cleanupOldBackups()
        
        val backups = backupManager.listBackups()
        assertTrue(backups.size <= 50)
    }
    
    // ==================== 辅助方法测试 ====================
    
    @Test
    fun `hasBackups - 没有备份时返回false`() {
        assertFalse(backupManager.hasBackups())
    }
    
    @Test
    fun `hasBackups - 有备份时返回true`() {
        backupManager.createBackup()
        
        assertTrue(backupManager.hasBackups())
    }
    
    @Test
    fun `getBackupDirSize - 返回备份目录大小`() {
        File(tempDir, "gradle.properties").writeText("version=1.0.0")
        backupManager.createBackup()
        
        val size = backupManager.getBackupDirSize()
        
        assertTrue(size > 0)
    }
}
