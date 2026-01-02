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
}
