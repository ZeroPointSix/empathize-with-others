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
 * IconManager 单元测试
 * 
 * @see TDD-00024 6.1 单元测试
 */
class IconManagerTest {
    
    private lateinit var tempDir: File
    private lateinit var iconManager: IconManager
    
    @Before
    fun setUp() {
        // 创建临时测试目录
        tempDir = File(System.getProperty("java.io.tmpdir"), "icon-manager-test-${System.currentTimeMillis()}")
        tempDir.mkdirs()
        
        // 使用多阶段图标模式进行测试（useUnifiedIcon = false）
        iconManager = IconManager(tempDir, useUnifiedIcon = false)
    }
    
    @After
    fun tearDown() {
        // 清理临时目录
        tempDir.deleteRecursively()
    }
    
    // ==================== 图标映射配置测试 ====================
    
    @Test
    fun `loadIconMapping - 配置文件不存在时返回默认配置`() {
        val mapping = iconManager.loadIconMapping()
        
        assertEquals(1, mapping.version)
        assertEquals("production", mapping.defaultStage)
        assertEquals(4, mapping.iconSets.size)
        assertTrue(mapping.iconSets.containsKey("dev"))
        assertTrue(mapping.iconSets.containsKey("test"))
        assertTrue(mapping.iconSets.containsKey("beta"))
        assertTrue(mapping.iconSets.containsKey("production"))
    }
    
    @Test
    fun `loadIconMapping - 正确加载已存在的配置文件`() {
        // 准备配置文件
        val configDir = File(tempDir, "config")
        configDir.mkdirs()
        val configFile = File(configDir, "icon-mapping.json")
        configFile.writeText("""
            {
                "version": 2,
                "defaultStage": "beta",
                "iconSets": {
                    "dev": {
                        "sourceDir": "custom/icons/dev",
                        "files": ["icon.png"]
                    }
                }
            }
        """.trimIndent())
        
        val mapping = iconManager.loadIconMapping()
        
        assertEquals(2, mapping.version)
        assertEquals("beta", mapping.defaultStage)
        assertEquals(1, mapping.iconSets.size)
        assertEquals("custom/icons/dev", mapping.iconSets["dev"]?.sourceDir)
    }
    
    @Test
    fun `saveIconMapping - 正确保存配置文件`() {
        val mapping = IconMapping(
            version = 3,
            defaultStage = "test",
            iconSets = mapOf(
                "test" to IconSet(
                    sourceDir = "test/icons",
                    files = listOf("a.png", "b.png")
                )
            )
        )
        
        iconManager.saveIconMapping(mapping)
        
        val configFile = File(tempDir, "config/icon-mapping.json")
        assertTrue(configFile.exists())
        
        val loaded = iconManager.loadIconMapping()
        assertEquals(3, loaded.version)
        assertEquals("test", loaded.defaultStage)
    }
    
    // ==================== 图标切换测试 ====================
    
    @Test
    fun `switchToStage - 源目录不存在时返回失败`() {
        val result = iconManager.switchToStage(ReleaseStage.DEV)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("源图标目录不存在") == true)
    }
    
    @Test
    fun `switchToStage - 成功切换图标`() {
        // 准备源图标目录和文件
        setupIconResources(ReleaseStage.DEV)
        
        val result = iconManager.switchToStage(ReleaseStage.DEV)
        
        assertTrue(result.isSuccess)
        val switchResult = result.getOrNull()
        assertNotNull(switchResult)
        assertEquals(ReleaseStage.DEV, switchResult.stage)
        assertTrue(switchResult.copiedFiles.isNotEmpty())
    }
    
    @Test
    fun `switchToStage - 切换到不同阶段`() {
        // 准备多个阶段的图标
        setupIconResources(ReleaseStage.DEV)
        setupIconResources(ReleaseStage.PRODUCTION)
        
        // 先切换到DEV
        val devResult = iconManager.switchToStage(ReleaseStage.DEV)
        assertTrue(devResult.isSuccess)
        
        // 再切换到PRODUCTION
        val prodResult = iconManager.switchToStage(ReleaseStage.PRODUCTION)
        assertTrue(prodResult.isSuccess)
        assertEquals(ReleaseStage.PRODUCTION, prodResult.getOrNull()?.stage)
    }
    
    // ==================== 图标资源检查测试 ====================
    
    @Test
    fun `checkIconResources - 目录不存在时返回错误`() {
        val missing = iconManager.checkIconResources(ReleaseStage.DEV)
        
        assertTrue(missing.isNotEmpty())
        assertTrue(missing.any { it.contains("目录不存在") })
    }
    
    @Test
    fun `checkIconResources - 文件缺失时返回缺失列表`() {
        // 创建目录但不创建文件
        val stageDir = File(tempDir, "assets/icons/dev")
        stageDir.mkdirs()
        
        val missing = iconManager.checkIconResources(ReleaseStage.DEV)
        
        assertTrue(missing.isNotEmpty())
        assertTrue(missing.contains("ic_launcher.png"))
    }
    
    @Test
    fun `checkIconResources - 资源完整时返回空列表`() {
        setupIconResources(ReleaseStage.DEV)
        
        val missing = iconManager.checkIconResources(ReleaseStage.DEV)
        
        assertTrue(missing.isEmpty())
    }
    
    @Test
    fun `checkAllIconResources - 检查所有阶段`() {
        // 只准备DEV阶段
        setupIconResources(ReleaseStage.DEV)
        
        val allMissing = iconManager.checkAllIconResources()
        
        assertEquals(4, allMissing.size)
        assertTrue(allMissing[ReleaseStage.DEV]?.isEmpty() == true)
        assertTrue(allMissing[ReleaseStage.TEST]?.isNotEmpty() == true)
        assertTrue(allMissing[ReleaseStage.BETA]?.isNotEmpty() == true)
        assertTrue(allMissing[ReleaseStage.PRODUCTION]?.isNotEmpty() == true)
    }
    
    // ==================== 图标初始化测试 ====================
    
    @Test
    fun `initIconResources - 创建目录结构`() {
        val result = iconManager.initIconResources(generatePlaceholders = false)
        
        assertTrue(result.createdDirs.isNotEmpty())
        
        // 验证目录已创建
        ReleaseStage.values().forEach { stage ->
            val stageDir = File(tempDir, "assets/icons/${stage.iconSuffix}")
            assertTrue(stageDir.exists(), "目录应存在: ${stageDir.path}")
        }
    }
    
    @Test
    fun `initIconResources - 生成占位图标`() {
        val result = iconManager.initIconResources(generatePlaceholders = true)
        
        assertTrue(result.createdFiles.isNotEmpty())
        
        // 验证占位图标已创建
        val devDir = File(tempDir, "assets/icons/dev")
        assertTrue(File(devDir, "ic_launcher.png").exists())
        assertTrue(File(devDir, "ic_launcher_round.png").exists())
        assertTrue(File(devDir, "ic_launcher_foreground.png").exists())
    }
    
    @Test
    fun `initIconResources - 创建配置文件`() {
        iconManager.initIconResources()
        
        val configFile = File(tempDir, "config/icon-mapping.json")
        assertTrue(configFile.exists())
    }
    
    @Test
    fun `initIconResources - 不覆盖已存在的文件`() {
        // 先创建一个文件
        val devDir = File(tempDir, "assets/icons/dev")
        devDir.mkdirs()
        val existingFile = File(devDir, "ic_launcher.png")
        existingFile.writeText("existing content")
        
        iconManager.initIconResources(generatePlaceholders = true)
        
        // 验证文件内容未被覆盖
        assertEquals("existing content", existingFile.readText())
    }
    
    // ==================== 当前图标阶段检测测试 ====================
    
    @Test
    fun `getCurrentIconStage - 无图标时返回null`() {
        val stage = iconManager.getCurrentIconStage()
        
        assertNull(stage)
    }
    
    @Test
    fun `getCurrentIconStage - 正确识别当前阶段`() {
        // 准备源图标
        setupIconResources(ReleaseStage.BETA)
        
        // 切换到BETA
        iconManager.switchToStage(ReleaseStage.BETA)
        
        val stage = iconManager.getCurrentIconStage()
        
        assertEquals(ReleaseStage.BETA, stage)
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 设置指定阶段的图标资源
     */
    private fun setupIconResources(stage: ReleaseStage) {
        val stageDir = File(tempDir, "assets/icons/${stage.iconSuffix}")
        stageDir.mkdirs()
        
        // 创建图标文件（使用简单内容区分不同阶段）
        val iconContent = "icon-${stage.iconSuffix}".toByteArray()
        File(stageDir, "ic_launcher.png").writeBytes(iconContent)
        File(stageDir, "ic_launcher_round.png").writeBytes(iconContent)
        File(stageDir, "ic_launcher_foreground.png").writeBytes(iconContent)
        
        // 也创建xxxhdpi密度目录（用于getCurrentIconStage检测）
        val xxxhdpiDir = File(stageDir, "xxxhdpi")
        xxxhdpiDir.mkdirs()
        File(xxxhdpiDir, "ic_launcher.png").writeBytes(iconContent)
    }
}

/**
 * IconManager 统一图标模式测试
 * 测试个人开发者模式（所有阶段使用同一图标）
 */
class IconManagerUnifiedModeTest {
    
    private lateinit var tempDir: File
    private lateinit var iconManager: IconManager
    
    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "icon-unified-test-${System.currentTimeMillis()}")
        tempDir.mkdirs()
        
        // 使用统一图标模式（默认）
        iconManager = IconManager(tempDir, useUnifiedIcon = true)
    }
    
    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }
    
    @Test
    fun `switchToStage - 统一图标文件不存在时返回失败`() {
        val result = iconManager.switchToStage(ReleaseStage.DEV)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("统一图标文件不存在") == true)
    }
    
    @Test
    fun `switchToStage - 使用统一图标成功切换`() {
        // 创建统一图标文件
        val unifiedIcon = File(tempDir, "软件图标.png")
        unifiedIcon.writeBytes("unified-icon-content".toByteArray())
        
        val result = iconManager.switchToStage(ReleaseStage.DEV)
        
        assertTrue(result.isSuccess)
        val switchResult = result.getOrNull()
        assertNotNull(switchResult)
        assertEquals(ReleaseStage.DEV, switchResult.stage)
        // 应该复制到所有密度目录（5个密度 × 2个文件 = 10个文件）
        assertEquals(10, switchResult.copiedFiles.size)
    }
    
    @Test
    fun `switchToStage - 所有阶段使用相同图标`() {
        // 创建统一图标文件
        val unifiedIcon = File(tempDir, "软件图标.png")
        unifiedIcon.writeBytes("unified-icon-content".toByteArray())
        
        // 切换到不同阶段
        val devResult = iconManager.switchToStage(ReleaseStage.DEV)
        val betaResult = iconManager.switchToStage(ReleaseStage.BETA)
        val prodResult = iconManager.switchToStage(ReleaseStage.PRODUCTION)
        
        assertTrue(devResult.isSuccess)
        assertTrue(betaResult.isSuccess)
        assertTrue(prodResult.isSuccess)
        
        // 验证所有阶段复制的文件数量相同
        assertEquals(devResult.getOrNull()?.copiedFiles?.size, betaResult.getOrNull()?.copiedFiles?.size)
        assertEquals(betaResult.getOrNull()?.copiedFiles?.size, prodResult.getOrNull()?.copiedFiles?.size)
    }
    
    @Test
    fun `switchToStage - 图标复制到正确的目标目录`() {
        // 创建统一图标文件
        val unifiedIcon = File(tempDir, "软件图标.png")
        val iconContent = "unified-icon-content".toByteArray()
        unifiedIcon.writeBytes(iconContent)
        
        iconManager.switchToStage(ReleaseStage.DEV)
        
        // 验证图标已复制到所有密度目录
        listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi").forEach { density ->
            val targetDir = File(tempDir, "app/src/main/res/mipmap-$density")
            assertTrue(targetDir.exists(), "目录应存在: $density")
            
            val launcher = File(targetDir, "ic_launcher.png")
            val launcherRound = File(targetDir, "ic_launcher_round.png")
            
            assertTrue(launcher.exists(), "ic_launcher.png应存在于$density")
            assertTrue(launcherRound.exists(), "ic_launcher_round.png应存在于$density")
            
            // 验证内容相同
            assertTrue(launcher.readBytes().contentEquals(iconContent))
            assertTrue(launcherRound.readBytes().contentEquals(iconContent))
        }
    }
}
