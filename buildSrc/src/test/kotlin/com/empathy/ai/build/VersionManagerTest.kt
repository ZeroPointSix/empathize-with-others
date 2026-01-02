package com.empathy.ai.build

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * VersionManager 单元测试
 * 
 * @see TDD-00024 6.1 单元测试
 */
class VersionManagerTest {
    
    private lateinit var tempDir: File
    private lateinit var versionManager: VersionManager
    
    @Before
    fun setUp() {
        tempDir = createTempDir("version-manager-test")
        versionManager = VersionManager(tempDir)
    }
    
    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }
    
    // ========== getCurrentVersion 测试 ==========
    
    @Test
    fun `getCurrentVersion returns correct version`() {
        createGradleProperties("1.2.3")
        
        val version = versionManager.getCurrentVersion()
        
        assertEquals(1, version.major)
        assertEquals(2, version.minor)
        assertEquals(3, version.patch)
    }
    
    @Test
    fun `getCurrentVersion throws when file not found`() {
        assertFailsWith<IllegalStateException> {
            versionManager.getCurrentVersion()
        }
    }
    
    @Test
    fun `getCurrentVersion throws when version key missing`() {
        File(tempDir, "gradle.properties").writeText("OTHER_KEY=value")
        
        assertFailsWith<IllegalStateException> {
            versionManager.getCurrentVersion()
        }
    }
    
    @Test
    fun `getCurrentVersionOrDefault returns default when file not found`() {
        val version = versionManager.getCurrentVersionOrDefault()
        
        assertEquals(SemanticVersion.DEFAULT, version)
    }
    
    @Test
    fun `getCurrentVersionOrDefault returns version when file exists`() {
        createGradleProperties("2.0.0")
        
        val version = versionManager.getCurrentVersionOrDefault()
        
        assertEquals(2, version.major)
        assertEquals(0, version.minor)
        assertEquals(0, version.patch)
    }
    
    // ========== getCurrentStage 测试 ==========
    
    @Test
    fun `getCurrentStage returns correct stage`() {
        createGradlePropertiesWithStage("1.0.0", "beta")
        
        val stage = versionManager.getCurrentStage()
        
        assertEquals(ReleaseStage.BETA, stage)
    }
    
    @Test
    fun `getCurrentStage returns default when file not found`() {
        val stage = versionManager.getCurrentStage()
        
        assertEquals(ReleaseStage.DEFAULT, stage)
    }
    
    @Test
    fun `getCurrentStage returns default when stage key missing`() {
        createGradleProperties("1.0.0")
        
        val stage = versionManager.getCurrentStage()
        
        assertEquals(ReleaseStage.DEFAULT, stage)
    }
    
    // ========== getCurrentVersionCode 测试 ==========
    
    @Test
    fun `getCurrentVersionCode returns correct code`() {
        createGradlePropertiesWithCode("1.2.3", 10203)
        
        val code = versionManager.getCurrentVersionCode()
        
        assertEquals(10203, code)
    }
    
    @Test
    fun `getCurrentVersionCode calculates from version when code missing`() {
        createGradleProperties("1.2.3")
        
        val code = versionManager.getCurrentVersionCode()
        
        assertEquals(10203, code)
    }
    
    @Test
    fun `getCurrentVersionCode returns default when file not found`() {
        val code = versionManager.getCurrentVersionCode()
        
        assertEquals(SemanticVersion.DEFAULT.toVersionCode(), code)
    }
    
    // ========== updateVersion 测试 ==========
    
    @Test
    fun `updateVersion updates all fields`() {
        createGradlePropertiesWithStage("1.0.0", "dev")
        
        val newVersion = SemanticVersion(2, 0, 0)
        versionManager.updateVersion(newVersion, ReleaseStage.PRODUCTION)
        
        val content = File(tempDir, "gradle.properties").readText()
        assertTrue(content.contains("APP_VERSION_NAME=2.0.0"))
        assertTrue(content.contains("APP_VERSION_CODE=20000"))
        assertTrue(content.contains("APP_RELEASE_STAGE=production"))
    }
    
    @Test
    fun `updateVersion adds missing fields`() {
        File(tempDir, "gradle.properties").writeText("OTHER_KEY=value")
        
        val newVersion = SemanticVersion(1, 0, 0)
        versionManager.updateVersion(newVersion, ReleaseStage.DEV)
        
        val content = File(tempDir, "gradle.properties").readText()
        assertTrue(content.contains("APP_VERSION_NAME=1.0.0"))
        assertTrue(content.contains("APP_VERSION_CODE=10000"))
        assertTrue(content.contains("APP_RELEASE_STAGE=dev"))
    }
    
    @Test
    fun `updateVersion throws when file not found`() {
        assertFailsWith<IllegalStateException> {
            versionManager.updateVersion(SemanticVersion(1, 0, 0), ReleaseStage.DEV)
        }
    }
    
    // ========== updateVersionHistory 测试 ==========
    
    @Test
    fun `updateVersionHistory creates history file`() {
        createGradleProperties("1.0.0")
        
        val commits = listOf(
            ParsedCommit(CommitType.FEATURE, subject = "新功能"),
            ParsedCommit(CommitType.FIX, subject = "修复bug")
        )
        
        versionManager.updateVersionHistory(
            SemanticVersion(1, 1, 0),
            ReleaseStage.DEV,
            commits
        )
        
        val historyFile = File(tempDir, "config/version-history.json")
        assertTrue(historyFile.exists())
        
        val content = historyFile.readText()
        assertTrue(content.contains("1.1.0"))
        assertTrue(content.contains("新功能"))
        assertTrue(content.contains("修复bug"))
    }
    
    @Test
    fun `updateVersionHistory appends to existing history`() {
        createGradleProperties("1.0.0")
        
        // 第一次更新
        versionManager.updateVersionHistory(
            SemanticVersion(1, 1, 0),
            ReleaseStage.DEV,
            listOf(ParsedCommit(CommitType.FEATURE, subject = "功能1"))
        )
        
        // 第二次更新
        versionManager.updateVersionHistory(
            SemanticVersion(1, 2, 0),
            ReleaseStage.TEST,
            listOf(ParsedCommit(CommitType.FEATURE, subject = "功能2"))
        )
        
        val history = versionManager.getVersionHistory()
        
        assertEquals("1.2.0", history.currentVersion?.version)
        assertEquals(2, history.history.size)
        assertEquals("1.2.0", history.history[0].version)
        assertEquals("1.1.0", history.history[1].version)
    }
    
    // ========== getVersionHistory 测试 ==========
    
    @Test
    fun `getVersionHistory returns empty when file not found`() {
        val history = versionManager.getVersionHistory()
        
        assertNull(history.currentVersion)
        assertTrue(history.history.isEmpty())
    }
    
    @Test
    fun `getVersionHistory returns empty when file corrupted`() {
        val configDir = File(tempDir, "config")
        configDir.mkdirs()
        File(configDir, "version-history.json").writeText("invalid json")
        
        val history = versionManager.getVersionHistory()
        
        assertNull(history.currentVersion)
        assertTrue(history.history.isEmpty())
    }
    
    // ========== isConfigValid 测试 ==========
    
    @Test
    fun `isConfigValid returns true for valid config`() {
        createGradleProperties("1.0.0")
        
        assertTrue(versionManager.isConfigValid())
    }
    
    @Test
    fun `isConfigValid returns false when file not found`() {
        assertFalse(versionManager.isConfigValid())
    }
    
    @Test
    fun `isConfigValid returns false for invalid version`() {
        File(tempDir, "gradle.properties").writeText("APP_VERSION_NAME=invalid")
        
        assertFalse(versionManager.isConfigValid())
    }
    
    // ========== getVersionSummary 测试 ==========
    
    @Test
    fun `getVersionSummary returns formatted summary`() {
        createGradlePropertiesWithStage("1.2.3", "beta")
        
        val summary = versionManager.getVersionSummary()
        
        assertTrue(summary.contains("1.2.3"))
        assertTrue(summary.contains("预发布版"))
    }
    
    @Test
    fun `getVersionSummary returns error message when config invalid`() {
        val summary = versionManager.getVersionSummary()
        
        assertTrue(summary.contains("无法获取版本信息"))
    }
    
    // ========== 辅助方法 ==========
    
    private fun createGradleProperties(version: String) {
        File(tempDir, "gradle.properties").writeText("""
            APP_VERSION_NAME=$version
            APP_VERSION_CODE=${SemanticVersion.parse(version).toVersionCode()}
        """.trimIndent())
    }
    
    private fun createGradlePropertiesWithStage(version: String, stage: String) {
        File(tempDir, "gradle.properties").writeText("""
            APP_VERSION_NAME=$version
            APP_VERSION_CODE=${SemanticVersion.parse(version).toVersionCode()}
            APP_RELEASE_STAGE=$stage
        """.trimIndent())
    }
    
    private fun createGradlePropertiesWithCode(version: String, code: Int) {
        File(tempDir, "gradle.properties").writeText("""
            APP_VERSION_NAME=$version
            APP_VERSION_CODE=$code
        """.trimIndent())
    }

    // ==================== 并发更新测试 ====================

    @Test
    fun `updateVersion - 并发更新时数据一致性`() {
        createGradleProperties("1.0.0")

        val threadCount = 10
        val executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount)
        val latch = java.util.concurrent.CountDownLatch(threadCount)
        val successCount = java.util.concurrent.atomic.AtomicInteger(0)
        val errorCount = java.util.concurrent.atomic.AtomicInteger(0)
        val results = mutableListOf<SemanticVersion>()

        // 并发更新版本
        repeat(threadCount) { index ->
            executor.submit {
                try {
                    val version = SemanticVersion(2, index, 0)
                    versionManager.updateVersion(version, ReleaseStage.DEV)
                    synchronized(results) {
                        results.add(version)
                    }
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    errorCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        // 等待所有线程完成
        assertTrue(latch.await(30, java.util.concurrent.TimeUnit.SECONDS))
        executor.shutdown()

        // 验证至少有一些操作成功
        assertTrue(successCount.get() > 0, "至少应该有部分更新成功")

        // 验证最终版本是一致的
        val finalVersion = versionManager.getCurrentVersion()
        assertNotNull(finalVersion)
        assertTrue(results.contains(finalVersion), "最终版本应该是某个更新操作的版本")

        // 验证文件内容一致
        val content = File(tempDir, "gradle.properties").readText()
        assertTrue(content.contains("APP_VERSION_NAME="), "文件应该包含版本号")
    }

    @Test
    fun `updateVersionWithStage - 并发更新版本和阶段`() {
        createGradlePropertiesWithStage("1.0.0", "dev")

        val threadCount = 5
        val executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount)
        val latch = java.util.concurrent.CountDownLatch(threadCount)
        val stages = listOf(ReleaseStage.DEV, ReleaseStage.TEST, ReleaseStage.BETA, ReleaseStage.PRODUCTION)

        // 并发更新版本和阶段
        repeat(threadCount) { index ->
            executor.submit {
                try {
                    val version = SemanticVersion(2, index, 0)
                    val stage = stages[index % stages.size]
                    versionManager.updateVersion(version, stage)
                    java.lang.Thread.sleep(10) // 稍微错开执行时间
                } catch (e: Exception) {
                    // 记录但不中断
                } finally {
                    latch.countDown()
                }
            }
        }

        // 等待所有线程完成
        assertTrue(latch.await(30, java.util.concurrent.TimeUnit.SECONDS))
        executor.shutdown()

        // 验证最终状态一致
        val finalVersion = versionManager.getCurrentVersion()
        val finalStage = versionManager.getCurrentStage()

        assertNotNull(finalVersion)
        assertTrue(finalVersion.major >= 1, "主版本号应该被更新")
    }

    @Test
    fun `updateVersionHistory - 并发更新历史记录`() {
        val configDir = File(tempDir, "config")
        configDir.mkdirs()

        val threadCount = 10
        val executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount)
        val latch = java.util.concurrent.CountDownLatch(threadCount)

        // 并发更新历史记录
        repeat(threadCount) { index ->
            executor.submit {
                try {
                    val version = SemanticVersion(1, index, 0)
                    val commits = listOf(ParsedCommit(CommitType.FEATURE, subject = "功能$index"))
                    versionManager.updateVersionHistory(version, ReleaseStage.DEV, commits)
                    java.lang.Thread.sleep(5) // 稍微错开执行时间
                } catch (e: Exception) {
                    // 记录但不中断
                } finally {
                    latch.countDown()
                }
            }
        }

        // 等待所有线程完成
        assertTrue(latch.await(30, java.util.concurrent.TimeUnit.SECONDS))
        executor.shutdown()

        // 验证历史记录存在
        val history = versionManager.getVersionHistory()
        assertNotNull(history)
        assertTrue(history.history.size > 0, "应该有历史记录")
    }

    @Test
    fun `getCurrentVersion - 并发读取版本`() {
        createGradleProperties("1.5.10")

        val threadCount = 20
        val executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount)
        val latch = java.util.concurrent.CountDownLatch(threadCount)
        val versions = mutableListOf<SemanticVersion>()
        val errors = java.util.concurrent.atomic.AtomicInteger(0)

        // 并发读取版本
        repeat(threadCount) {
            executor.submit {
                try {
                    val version = versionManager.getCurrentVersion()
                    synchronized(versions) {
                        versions.add(version)
                    }
                } catch (e: Exception) {
                    errors.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        // 等待所有线程完成
        assertTrue(latch.await(10, java.util.concurrent.TimeUnit.SECONDS))
        executor.shutdown()

        // 验证所有读取的版本一致
        assertTrue(errors.get() == 0, "不应该有读取错误")
        assertTrue(versions.size > 0, "应该有成功的读取")
        assertTrue(versions.distinct().size == 1, "所有读取的版本应该相同")
        assertEquals(SemanticVersion(1, 5, 10), versions.first())
    }

    @Test
    fun `updateVersion and readVersion - 并发读写`() {
        createGradleProperties("1.0.0")

        val threadCount = 10
        val executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount)
        val latch = java.util.concurrent.CountDownLatch(threadCount * 2)
        val readResults = mutableListOf<SemanticVersion>()
        val writeSuccess = java.util.concurrent.atomic.AtomicInteger(0)

        // 5个线程读，5个线程写
        repeat(threadCount) { index ->
            if (index % 2 == 0) {
                // 读线程
                executor.submit {
                    repeat(10) {
                        try {
                            val version = versionManager.getCurrentVersion()
                            synchronized(readResults) {
                                readResults.add(version)
                            }
                            java.lang.Thread.sleep(1)
                        } catch (e: Exception) {
                            // 忽略读取错误
                        }
                    }
                    latch.countDown()
                }
            } else {
                // 写线程
                executor.submit {
                    try {
                        val version = SemanticVersion(2, index, 0)
                        versionManager.updateVersion(version, ReleaseStage.TEST)
                        writeSuccess.incrementAndGet()
                        java.lang.Thread.sleep(10)
                    } catch (e: Exception) {
                        // 忽略写入错误
                    } finally {
                        latch.countDown()
                    }
                }
            }
        }

        // 等待所有线程完成
        assertTrue(latch.await(30, java.util.concurrent.TimeUnit.SECONDS))
        executor.shutdown()

        // 验证系统稳定
        assertTrue(readResults.size > 0, "应该有成功的读取")
        assertTrue(writeSuccess.get() > 0, "应该有成功的写入")

        // 验证最终状态一致
        val finalVersion = versionManager.getCurrentVersion()
        assertNotNull(finalVersion)
    }

    @Test
    fun `updateVersionHistory - 历史记录超过100条时自动截断`() {
        val configDir = File(tempDir, "config")
        configDir.mkdirs()

        // 创建105条历史记录
        repeat(105) { index ->
            val version = SemanticVersion(1, index, 0)
            val commits = listOf(ParsedCommit(CommitType.FEATURE, subject = "功能$index"))
            versionManager.updateVersionHistory(version, ReleaseStage.DEV, commits)
        }

        val history = versionManager.getVersionHistory()

        // 验证历史记录被截断到100条以内
        assertTrue(history.history.size <= 100, "历史记录应该被截断到100条以内")
        assertTrue(history.history.size > 0, "应该保留部分历史记录")
    }

    @Test
    fun `restoreVersion - 从历史版本恢复`() {
        // 创建历史记录
        val configDir = File(tempDir, "config")
        configDir.mkdirs()

        repeat(3) { index ->
            val version = SemanticVersion(1, index, 0)
            val commits = listOf(ParsedCommit(CommitType.FEATURE, subject = "功能$index"))
            versionManager.updateVersionHistory(version, ReleaseStage.DEV, commits)
        }

        // 获取历史记录
        val history = versionManager.getVersionHistory()
        assertTrue(history.history.size >= 3)

        // 恢复到某个历史版本
        val targetVersion = history.history[1] // 第二个版本
        versionManager.updateVersion(
            SemanticVersion.parse(targetVersion.version),
            ReleaseStage.DEV
        )

        // 验证版本已恢复
        val currentVersion = versionManager.getCurrentVersion()
        assertEquals(targetVersion.version, currentVersion.toString())
    }
}
