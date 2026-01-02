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
 * VersionSyncManager 单元测试
 * 
 * @see TDD-00024 6.1 单元测试
 */
class VersionSyncManagerTest {
    
    private lateinit var tempDir: File
    private lateinit var syncManager: VersionSyncManager
    
    @Before
    fun setUp() {
        tempDir = createTempDir("version-sync-manager-test")
        syncManager = VersionSyncManager(tempDir)
    }
    
    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }
    
    // ========== discoverModules 测试 ==========
    
    @Test
    fun `discoverModules finds root module`() {
        createRootGradleProperties()
        
        val modules = syncManager.discoverModules()
        
        assertTrue(modules.contains(tempDir))
    }
    
    @Test
    fun `discoverModules finds submodules with build_gradle_kts`() {
        createRootGradleProperties()
        createSubmodule("app")
        createSubmodule("domain")
        
        val modules = syncManager.discoverModules()
        
        assertEquals(3, modules.size)
        assertTrue(modules.any { it.name == "app" })
        assertTrue(modules.any { it.name == "domain" })
    }
    
    @Test
    fun `discoverModules ignores hidden directories`() {
        createRootGradleProperties()
        createSubmodule(".hidden")
        
        val modules = syncManager.discoverModules()
        
        assertEquals(1, modules.size)
        assertFalse(modules.any { it.name == ".hidden" })
    }
    
    @Test
    fun `discoverModules ignores build directories`() {
        createRootGradleProperties()
        createSubmodule("build")
        createSubmodule("buildSrc")
        
        val modules = syncManager.discoverModules()
        
        assertEquals(1, modules.size)
    }
    
    @Test
    fun `discoverModules ignores directories without build file`() {
        createRootGradleProperties()
        File(tempDir, "docs").mkdirs()
        
        val modules = syncManager.discoverModules()
        
        assertEquals(1, modules.size)
    }
    
    // ========== syncVersions 测试 ==========
    
    @Test
    fun `syncVersions updates root module`() {
        createRootGradleProperties()
        
        val result = syncManager.syncVersions(SemanticVersion(2, 0, 0))
        
        assertTrue(result.isSuccess)
        assertEquals(1, result.totalSynced)
        
        val content = File(tempDir, "gradle.properties").readText()
        assertTrue(content.contains("APP_VERSION_NAME=2.0.0"))
    }
    
    @Test
    fun `syncVersions updates all discovered modules`() {
        createRootGradleProperties()
        createSubmoduleWithProperties("app")
        createSubmoduleWithProperties("domain")
        
        val result = syncManager.syncVersions(SemanticVersion(2, 0, 0))
        
        assertTrue(result.isSuccess)
        assertEquals(3, result.totalSynced)
    }
    
    @Test
    fun `syncVersions updates only specified modules`() {
        createRootGradleProperties()
        createSubmoduleWithProperties("app")
        createSubmoduleWithProperties("domain")
        
        val result = syncManager.syncVersions(
            SemanticVersion(2, 0, 0),
            modules = listOf("app")
        )
        
        assertTrue(result.isSuccess)
        assertEquals(1, result.totalSynced)
        
        // 验证只有app模块被更新
        val appContent = File(tempDir, "app/gradle.properties").readText()
        assertTrue(appContent.contains("APP_VERSION_NAME=2.0.0"))
        
        // domain模块应该保持原样
        val domainContent = File(tempDir, "domain/gradle.properties").readText()
        assertTrue(domainContent.contains("APP_VERSION_NAME=1.0.0"))
    }
    
    @Test
    fun `syncVersions reports failed modules`() {
        createRootGradleProperties()
        // 创建一个没有版本键的模块
        val appDir = File(tempDir, "app")
        appDir.mkdirs()
        File(appDir, "build.gradle.kts").writeText("plugins {}")
        File(appDir, "gradle.properties").writeText("OTHER_KEY=value")
        
        val result = syncManager.syncVersions(SemanticVersion(2, 0, 0))
        
        // 根模块成功，app模块失败（没有版本键）
        assertEquals(1, result.syncedModules.size)
        assertEquals(1, result.failedModules.size)
    }
    
    // ========== syncVersionsWithStage 测试 ==========
    
    @Test
    fun `syncVersionsWithStage updates version and stage`() {
        createRootGradlePropertiesWithStage()
        
        val result = syncManager.syncVersionsWithStage(
            SemanticVersion(2, 0, 0),
            ReleaseStage.PRODUCTION
        )
        
        assertTrue(result.isSuccess)
        
        val content = File(tempDir, "gradle.properties").readText()
        assertTrue(content.contains("APP_VERSION_NAME=2.0.0"))
        assertTrue(content.contains("APP_RELEASE_STAGE=production"))
    }
    
    @Test
    fun `syncVersionsWithStage updates all modules`() {
        createRootGradlePropertiesWithStage()
        createSubmoduleWithPropertiesAndStage("app")
        
        val result = syncManager.syncVersionsWithStage(
            SemanticVersion(2, 0, 0),
            ReleaseStage.BETA
        )
        
        assertTrue(result.isSuccess)
        assertEquals(2, result.totalSynced)
        
        val appContent = File(tempDir, "app/gradle.properties").readText()
        assertTrue(appContent.contains("APP_RELEASE_STAGE=beta"))
    }
    
    // ========== getAllVersions 测试 ==========
    
    @Test
    fun `getAllVersions returns versions for all modules`() {
        createRootGradleProperties()
        createSubmoduleWithProperties("app", "2.0.0")
        createSubmoduleWithProperties("domain", "1.5.0")
        
        val versions = syncManager.getAllVersions()
        
        assertEquals(3, versions.size)
        assertEquals(SemanticVersion(1, 0, 0), versions["."])
        assertEquals(SemanticVersion(2, 0, 0), versions["app"])
        assertEquals(SemanticVersion(1, 5, 0), versions["domain"])
    }
    
    @Test
    fun `getAllVersions returns null for modules without version`() {
        createRootGradleProperties()
        val appDir = File(tempDir, "app")
        appDir.mkdirs()
        File(appDir, "build.gradle.kts").writeText("plugins {}")
        
        val versions = syncManager.getAllVersions()
        
        assertNotNull(versions["."])
        assertNull(versions["app"])
    }
    
    // ========== checkVersionConsistency 测试 ==========
    
    @Test
    fun `checkVersionConsistency returns consistent when all versions match`() {
        createRootGradleProperties()
        createSubmoduleWithProperties("app", "1.0.0")
        createSubmoduleWithProperties("domain", "1.0.0")
        
        val result = syncManager.checkVersionConsistency()
        
        assertTrue(result.isConsistent)
        assertTrue(result.inconsistentModules.isEmpty())
    }
    
    @Test
    fun `checkVersionConsistency returns inconsistent when versions differ`() {
        createRootGradleProperties()
        createSubmoduleWithProperties("app", "2.0.0")
        createSubmoduleWithProperties("domain", "1.0.0")
        
        val result = syncManager.checkVersionConsistency()
        
        assertFalse(result.isConsistent)
        assertTrue(result.inconsistentModules.isNotEmpty())
    }
    
    @Test
    fun `checkVersionConsistency ignores modules without version`() {
        createRootGradleProperties()
        val appDir = File(tempDir, "app")
        appDir.mkdirs()
        File(appDir, "build.gradle.kts").writeText("plugins {}")
        
        val result = syncManager.checkVersionConsistency()
        
        assertTrue(result.isConsistent)
    }
    
    @Test
    fun `checkVersionConsistency returns consistent when no versions found`() {
        // 不创建任何gradle.properties
        
        val result = syncManager.checkVersionConsistency()
        
        assertTrue(result.isConsistent)
    }
    
    // ========== VersionSyncResult 测试 ==========
    
    @Test
    fun `VersionSyncResult toString formats correctly`() {
        val result = VersionSyncResult(
            version = SemanticVersion(1, 0, 0),
            syncedModules = listOf(
                ModuleSyncResult(".", "gradle.properties", "Gradle Properties", true, null)
            ),
            failedModules = emptyList()
        )
        
        val str = result.toString()
        assertTrue(str.contains("1.0.0"))
        assertTrue(str.contains("成功: 1"))
        assertTrue(str.contains("失败: 0"))
    }
    
    @Test
    fun `VersionSyncResult toString includes failure details`() {
        val result = VersionSyncResult(
            version = SemanticVersion(1, 0, 0),
            syncedModules = emptyList(),
            failedModules = listOf(
                ModuleSyncResult("app", "gradle.properties", "Gradle Properties", false, "File not found")
            )
        )
        
        val str = result.toString()
        assertTrue(str.contains("失败详情"))
        assertTrue(str.contains("File not found"))
    }
    
    // ========== VersionConsistencyResult 测试 ==========
    
    @Test
    fun `VersionConsistencyResult toString formats correctly`() {
        val result = VersionConsistencyResult(
            isConsistent = true,
            versions = mapOf("." to SemanticVersion(1, 0, 0)),
            inconsistentModules = emptyList()
        )
        
        val str = result.toString()
        assertTrue(str.contains("✓ 一致"))
        assertTrue(str.contains("1.0.0"))
    }
    
    @Test
    fun `VersionConsistencyResult toString shows inconsistent modules`() {
        val result = VersionConsistencyResult(
            isConsistent = false,
            versions = mapOf(
                "." to SemanticVersion(1, 0, 0),
                "app" to SemanticVersion(2, 0, 0)
            ),
            inconsistentModules = listOf("app")
        )
        
        val str = result.toString()
        assertTrue(str.contains("✗ 不一致"))
        assertTrue(str.contains("app"))
    }
    
    // ========== 辅助方法 ==========
    
    private fun createRootGradleProperties() {
        File(tempDir, "gradle.properties").writeText("""
            APP_VERSION_NAME=1.0.0
            APP_VERSION_CODE=10000
        """.trimIndent())
    }
    
    private fun createRootGradlePropertiesWithStage() {
        File(tempDir, "gradle.properties").writeText("""
            APP_VERSION_NAME=1.0.0
            APP_VERSION_CODE=10000
            APP_RELEASE_STAGE=dev
        """.trimIndent())
    }
    
    private fun createSubmodule(name: String) {
        val moduleDir = File(tempDir, name)
        moduleDir.mkdirs()
        File(moduleDir, "build.gradle.kts").writeText("plugins {}")
    }
    
    private fun createSubmoduleWithProperties(name: String, version: String = "1.0.0") {
        val moduleDir = File(tempDir, name)
        moduleDir.mkdirs()
        File(moduleDir, "build.gradle.kts").writeText("plugins {}")
        File(moduleDir, "gradle.properties").writeText("""
            APP_VERSION_NAME=$version
            APP_VERSION_CODE=${SemanticVersion.parse(version).toVersionCode()}
        """.trimIndent())
    }
    
    private fun createSubmoduleWithPropertiesAndStage(name: String, version: String = "1.0.0", stage: String = "dev") {
        val moduleDir = File(tempDir, name)
        moduleDir.mkdirs()
        File(moduleDir, "build.gradle.kts").writeText("plugins {}")
        File(moduleDir, "gradle.properties").writeText("""
            APP_VERSION_NAME=$version
            APP_VERSION_CODE=${SemanticVersion.parse(version).toVersionCode()}
            APP_RELEASE_STAGE=$stage
        """.trimIndent())
    }
}
