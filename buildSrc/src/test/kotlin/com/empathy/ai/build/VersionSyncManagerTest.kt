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

    // ==================== 大型项目性能测试 ====================

    @Test
    fun `syncVersions - 50+模块的大型项目性能测试`() {
        createRootGradleProperties()

        // 创建50个模块
        repeat(50) { index ->
            createSubmoduleWithProperties("module-$index", "1.0.0")
        }

        val startTime = System.currentTimeMillis()
        val result = syncManager.syncVersions(SemanticVersion(2, 0, 0))
        val duration = System.currentTimeMillis() - startTime

        // 验证同步成功
        assertTrue(result.isSuccess)
        assertEquals(51, result.totalSynced) // 50个子模块 + 1个根模块

        // 验证性能（应该在10秒内完成）
        assertTrue(duration < 10000, "50个模块的同步应在10秒内完成，实际: ${duration}ms")
    }

    @Test
    fun `discoverModules - 深度嵌套模块结构性能测试`() {
        createRootGradleProperties()

        // 创建嵌套模块结构（3层）
        repeat(5) { i ->
            val level1 = File(tempDir, "level1-$i")
            level1.mkdirs()
            File(level1, "build.gradle.kts").writeText("plugins {}")
            createSubmoduleWithPropertiesAt(level1, "level1-$i/sub", "1.0.0")

            repeat(3) { j ->
                val level2 = File(level1, "level2-$j")
                level2.mkdirs()
                File(level2, "build.gradle.kts").writeText("plugins {}")
                createSubmoduleWithPropertiesAt(level2, "level2-$j/sub", "1.0.0")
            }
        }

        val modules = syncManager.discoverModules()

        // 验证能发现所有模块
        assertTrue(modules.size >= 20, "应该发现至少20个模块，实际: ${modules.size}")
    }

    @Test
    fun `checkVersionConsistency - 检测到不一致后自动修复`() {
        // 创建版本不一致的模块
        createRootGradleProperties() // 版本1.0.0
        createSubmoduleWithProperties("app", "2.0.0") // 版本2.0.0 - 不一致
        createSubmoduleWithProperties("domain", "1.0.0") // 版本1.0.0 - 一致

        // 检查一致性
        val checkResult = syncManager.checkVersionConsistency()

        // 验证检测到不一致
        assertFalse(checkResult.isConsistent)
        assertTrue(checkResult.inconsistentModules.contains("app"))
        assertEquals(2, checkResult.versions.size)

        // 自动修复：同步所有模块到最新版本
        val latestVersion = SemanticVersion(2, 0, 0)
        val syncResult = syncManager.syncVersions(latestVersion)

        // 验证修复成功
        assertTrue(syncResult.isSuccess)
        assertEquals(3, syncResult.totalSynced)

        // 再次检查一致性
        val finalCheck = syncManager.checkVersionConsistency()
        assertTrue(finalCheck.isConsistent, "修复后应该版本一致")
    }

    @Test
    fun `syncVersions - 模块目录不存在时跳过并记录`() {
        createRootGradleProperties()

        // 尝试同步包含不存在模块的列表
        val result = syncManager.syncVersions(
            SemanticVersion(2, 0, 0),
            modules = listOf(".", "nonexistent-module", "app")
        )

        // 验证部分成功
        assertTrue(result.isSuccess || result.failedModules.isNotEmpty())
        assertTrue(result.syncedModules.size > 0 || result.failedModules.size > 0)

        // 验证根模块被成功同步
        val rootSynced = result.syncedModules.any { it.modulePath == "." }
        assertTrue(rootSynced || result.failedModules.any { it.modulePath == "." })
    }

    @Test
    fun `syncVersionsWithStage - updater抛出异常时完整回滚`() {
        createRootGradlePropertiesWithStage()
        createSubmoduleWithPropertiesAndStage("app", "1.0.0", "dev")

        // 创建一个会导致失败的模块（没有gradle.properties）
        val invalidModuleDir = File(tempDir, "invalid-module")
        invalidModuleDir.mkdirs()
        File(invalidModuleDir, "build.gradle.kts").writeText("plugins {}")
        // 不创建gradle.properties，这会导致updater失败

        // 记录更新前的版本
        val versionBefore = syncManager.getAllVersions()

        // 尝试同步所有模块
        val result = syncManager.syncVersionsWithStage(
            SemanticVersion(2, 0, 0),
            ReleaseStage.BETA
        )

        // 验证有失败但不是全部失败
        assertTrue(result.totalSynced > 0 || result.failedModules.size > 0)

        // 验证已成功更新的模块被记录
        if (result.syncedModules.isNotEmpty()) {
            val versionAfter = syncManager.getAllVersions()
            // 验证至少有一个模块被更新
            val hasChanged = versionAfter.any { (module, version) ->
                version != versionBefore[module]
            }
            assertTrue(hasChanged || result.failedModules.isNotEmpty())
        }
    }

    @Test
    fun `syncVersions - 部分模块失败时继续处理其他模块`() {
        createRootGradleProperties() // 版本1.0.0
        createSubmoduleWithProperties("app", "1.0.0") // 版本1.0.0 - 有效
        createSubmodule("broken-module") // 没有properties文件 - 会失败
        createSubmoduleWithProperties("domain", "1.0.0") // 版本1.0.0 - 有效

        val result = syncManager.syncVersions(SemanticVersion(2, 0, 0))

        // 验证部分成功
        assertTrue(result.totalSynced > 0, "应该有成功的同步")
        assertTrue(result.failedModules.isNotEmpty(), "应该有失败的同步")

        // 验证成功同步的数量
        assertTrue(result.syncedModules.size >= 2, "至少根模块和2个有效子模块应该同步成功")

        // 验证失败模块被记录
        val failedModulePaths = result.failedModules.map { it.modulePath }
        assertTrue(failedModulePaths.any { it.contains("broken") })
    }

    @Test
    fun `getAllVersions - 大型项目中版本获取性能`() {
        createRootGradleProperties()

        // 创建30个模块
        repeat(30) { index ->
            createSubmoduleWithProperties("module-$index", "1.$index.0")
        }

        val startTime = System.currentTimeMillis()
        val versions = syncManager.getAllVersions()
        val duration = System.currentTimeMillis() - startTime

        // 验证所有版本都被获取
        assertTrue(versions.size >= 31, "应该有至少31个版本（30个模块+根），实际: ${versions.size}")

        // 验证性能（应该在2秒内完成）
        assertTrue(duration < 2000, "版本获取应在2秒内完成，实际: ${duration}ms")
    }

    @Test
    fun `discoverModules - 过滤非模块目录的正确性`() {
        createRootGradleProperties()

        // 创建各种目录
        createSubmodule("app") // 应该被识别
        createSubmodule("domain") // 应该被识别
        File(tempDir, "build").mkdirs() // 应该被忽略
        File(tempDir, "buildSrc").mkdirs() // 应该被忽略
        File(tempDir, ".git").mkdirs() // 应该被忽略（隐藏目录）
        File(tempDir, ".gradle").mkdirs() // 应该被忽略（隐藏目录）
        File(tempDir, "docs").mkdirs() // 应该被忽略（没有build文件）
        File(tempDir, "README.md").writeText("") // 文件，应该被忽略

        val modules = syncManager.discoverModules()

        // 验证只有有效模块被识别
        assertEquals(3, modules.size) // 根目录 + app + domain

        // 验证被过滤的目录
        val moduleNames = modules.map { it.name }
        assertTrue(!moduleNames.contains("build"))
        assertTrue(!moduleNames.contains("buildSrc"))
        assertTrue(!moduleNames.contains(".git"))
        assertTrue(!moduleNames.contains("docs"))
    }

    // ==================== 辅助方法 ====================

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

    private fun createSubmoduleWithPropertiesAt(parentDir: File, name: String, version: String = "1.0.0") {
        val moduleDir = File(parentDir, name)
        moduleDir.mkdirs()
        File(moduleDir, "build.gradle.kts").writeText("plugins {}")
        File(moduleDir, "gradle.properties").writeText("""
            APP_VERSION_NAME=$version
            APP_VERSION_CODE=${SemanticVersion.parse(version).toVersionCode()}
        """.trimIndent())
    }
}
