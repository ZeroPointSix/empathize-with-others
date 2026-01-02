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
 * IconManager 边界测试
 * 测试各种边界情况和异常场景
 * 
 * @see TDD-00024 6.1 单元测试
 * @see TD-00024 T017b
 */
class IconManagerBoundaryTest {
    
    private lateinit var tempDir: File
    private lateinit var iconManager: IconManager
    
    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "icon-boundary-test-${System.currentTimeMillis()}")
        tempDir.mkdirs()
        // 使用多阶段图标模式进行测试（useUnifiedIcon = false）
        iconManager = IconManager(tempDir, useUnifiedIcon = false)
    }
    
    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }
    
    // ==================== 图标文件不存在时的处理 ====================
    
    @Test
    fun `switchToStage - 部分图标文件缺失时仍能切换`() {
        // 只创建部分图标文件
        val stageDir = File(tempDir, "assets/icons/dev")
        stageDir.mkdirs()
        File(stageDir, "ic_launcher.png").writeBytes("icon".toByteArray())
        // 不创建 ic_launcher_round.png 和 ic_launcher_foreground.png
        
        val result = iconManager.switchToStage(ReleaseStage.DEV)
        
        // 应该成功，但只复制存在的文件
        assertTrue(result.isSuccess)
        val switchResult = result.getOrNull()
        assertNotNull(switchResult)
        // 复制的文件数量应该较少
        assertTrue(switchResult.copiedFiles.size >= 1)
    }
    
    @Test
    fun `switchToStage - 空目录时返回成功但无文件复制`() {
        // 创建空目录
        val stageDir = File(tempDir, "assets/icons/dev")
        stageDir.mkdirs()
        
        val result = iconManager.switchToStage(ReleaseStage.DEV)
        
        assertTrue(result.isSuccess)
        val switchResult = result.getOrNull()
        assertNotNull(switchResult)
        assertTrue(switchResult.copiedFiles.isEmpty())
    }
    
    // ==================== 图标文件损坏时的处理 ====================
    
    @Test
    fun `loadIconMapping - 配置文件格式错误时返回默认配置`() {
        // 创建格式错误的配置文件
        val configDir = File(tempDir, "config")
        configDir.mkdirs()
        File(configDir, "icon-mapping.json").writeText("{ invalid json }")
        
        val mapping = iconManager.loadIconMapping()
        
        // 应该返回默认配置
        assertEquals(1, mapping.version)
        assertEquals("production", mapping.defaultStage)
    }
    
    @Test
    fun `loadIconMapping - 配置文件为空时返回默认配置`() {
        val configDir = File(tempDir, "config")
        configDir.mkdirs()
        File(configDir, "icon-mapping.json").writeText("")
        
        val mapping = iconManager.loadIconMapping()
        
        assertEquals(1, mapping.version)
    }
    
    @Test
    fun `loadIconMapping - 配置文件缺少必要字段时使用默认值`() {
        val configDir = File(tempDir, "config")
        configDir.mkdirs()
        File(configDir, "icon-mapping.json").writeText("""
            {
                "version": 5
            }
        """.trimIndent())
        
        val mapping = iconManager.loadIconMapping()
        
        assertEquals(5, mapping.version)
        assertEquals("production", mapping.defaultStage) // 使用默认值
        assertTrue(mapping.iconSets.isEmpty())
    }
    
    // ==================== 无效阶段参数的处理 ====================
    
    @Test
    fun `checkIconResources - 配置中不存在的阶段返回配置缺失`() {
        // 使用自定义配置，不包含某个阶段
        val configDir = File(tempDir, "config")
        configDir.mkdirs()
        File(configDir, "icon-mapping.json").writeText("""
            {
                "version": 1,
                "defaultStage": "production",
                "iconSets": {
                    "production": {
                        "sourceDir": "assets/icons/production",
                        "files": ["ic_launcher.png"]
                    }
                }
            }
        """.trimIndent())
        
        // DEV阶段不在配置中
        val missing = iconManager.checkIconResources(ReleaseStage.DEV)
        
        assertTrue(missing.contains("配置缺失"))
    }
    
    @Test
    fun `switchToStage - 配置中不存在的阶段返回失败`() {
        // 使用自定义配置，不包含DEV阶段
        val configDir = File(tempDir, "config")
        configDir.mkdirs()
        File(configDir, "icon-mapping.json").writeText("""
            {
                "version": 1,
                "defaultStage": "production",
                "iconSets": {
                    "production": {
                        "sourceDir": "assets/icons/production",
                        "files": ["ic_launcher.png"]
                    }
                }
            }
        """.trimIndent())
        
        val result = iconManager.switchToStage(ReleaseStage.DEV)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("未找到") == true)
    }
    
    // ==================== 目标目录权限不足时的处理 ====================
    
    @Test
    fun `switchToStage - 目标目录自动创建`() {
        // 准备源图标
        val stageDir = File(tempDir, "assets/icons/dev")
        stageDir.mkdirs()
        File(stageDir, "ic_launcher.png").writeBytes("icon".toByteArray())
        
        // 确保目标目录不存在
        val targetDir = File(tempDir, "app/src/main/res/mipmap-xxxhdpi")
        assertFalse(targetDir.exists())
        
        val result = iconManager.switchToStage(ReleaseStage.DEV)
        
        assertTrue(result.isSuccess)
        // 目标目录应该被自动创建
        assertTrue(targetDir.exists())
    }
    
    // ==================== 并发操作测试 ====================
    
    @Test
    fun `initIconResources - 多次调用是幂等的`() {
        // 第一次调用
        val result1 = iconManager.initIconResources()
        assertTrue(result1.totalCreated > 0)
        
        // 第二次调用
        val result2 = iconManager.initIconResources()
        
        // 第二次不应该创建新文件（已存在）
        assertEquals(0, result2.createdDirs.size)
        assertEquals(0, result2.createdFiles.size)
    }
    
    @Test
    fun `saveIconMapping - 多次保存不会损坏文件`() {
        val mapping1 = IconMapping(version = 1, defaultStage = "dev")
        val mapping2 = IconMapping(version = 2, defaultStage = "test")
        val mapping3 = IconMapping(version = 3, defaultStage = "beta")
        
        iconManager.saveIconMapping(mapping1)
        iconManager.saveIconMapping(mapping2)
        iconManager.saveIconMapping(mapping3)
        
        val loaded = iconManager.loadIconMapping()
        assertEquals(3, loaded.version)
        assertEquals("beta", loaded.defaultStage)
    }
    
    // ==================== 特殊字符和路径测试 ====================
    
    @Test
    fun `loadIconMapping - 配置中包含特殊字符`() {
        val configDir = File(tempDir, "config")
        configDir.mkdirs()
        File(configDir, "icon-mapping.json").writeText("""
            {
                "version": 1,
                "defaultStage": "production",
                "iconSets": {
                    "dev": {
                        "sourceDir": "assets/icons/dev",
                        "files": ["ic_launcher.png"],
                        "description": "开发版图标 - 带DEV标识"
                    }
                }
            }
        """.trimIndent())
        
        val mapping = iconManager.loadIconMapping()
        
        assertNotNull(mapping.iconSets["dev"])
    }
    
    // ==================== 大文件处理测试 ====================
    
    @Test
    fun `switchToStage - 处理较大的图标文件`() {
        val stageDir = File(tempDir, "assets/icons/dev")
        stageDir.mkdirs()
        
        // 创建一个较大的文件（1MB）
        val largeContent = ByteArray(1024 * 1024) { it.toByte() }
        File(stageDir, "ic_launcher.png").writeBytes(largeContent)
        
        val result = iconManager.switchToStage(ReleaseStage.DEV)
        
        assertTrue(result.isSuccess)
        
        // 验证文件被正确复制
        val targetFile = File(tempDir, "app/src/main/res/mipmap-mdpi/ic_launcher.png")
        assertTrue(targetFile.exists())
        assertEquals(largeContent.size, targetFile.length().toInt())
    }
    
    // ==================== 密度目录结构测试 ====================
    
    @Test
    fun `switchToStage - 支持密度子目录结构`() {
        val stageDir = File(tempDir, "assets/icons/dev")
        
        // 创建各密度目录
        listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi").forEach { density ->
            val densityDir = File(stageDir, density)
            densityDir.mkdirs()
            File(densityDir, "ic_launcher.png").writeBytes("icon-$density".toByteArray())
        }
        
        val result = iconManager.switchToStage(ReleaseStage.DEV)
        
        assertTrue(result.isSuccess)
        val switchResult = result.getOrNull()
        assertNotNull(switchResult)
        
        // 验证各密度目录的文件都被复制
        assertTrue(switchResult.copiedFiles.size >= 5)
    }
    
    @Test
    fun `switchToStage - 混合结构（根目录和密度目录）`() {
        val stageDir = File(tempDir, "assets/icons/dev")
        stageDir.mkdirs()
        
        // 根目录放一个文件
        File(stageDir, "ic_launcher.png").writeBytes("root-icon".toByteArray())
        
        // xxxhdpi目录放另一个文件
        val xxxhdpiDir = File(stageDir, "xxxhdpi")
        xxxhdpiDir.mkdirs()
        File(xxxhdpiDir, "ic_launcher_round.png").writeBytes("xxxhdpi-icon".toByteArray())
        
        val result = iconManager.switchToStage(ReleaseStage.DEV)
        
        assertTrue(result.isSuccess)
    }
    
    // ==================== 自适应图标测试 ====================
    
    @Test
    fun `switchToStage - 更新自适应图标`() {
        val stageDir = File(tempDir, "assets/icons/dev")
        stageDir.mkdirs()
        File(stageDir, "ic_launcher_foreground.png").writeBytes("foreground".toByteArray())
        
        val result = iconManager.switchToStage(ReleaseStage.DEV)
        
        assertTrue(result.isSuccess)
        
        // 验证自适应图标目录被创建
        val adaptiveDir = File(tempDir, "app/src/main/res/drawable-v26")
        assertTrue(adaptiveDir.exists())
    }
    
    // ==================== 空值和边界值测试 ====================
    
    @Test
    fun `IconMapping - 创建默认配置包含所有阶段`() {
        val defaultMapping = IconMapping.createDefault()
        
        assertEquals(4, defaultMapping.iconSets.size)
        ReleaseStage.values().forEach { stage ->
            assertTrue(defaultMapping.iconSets.containsKey(stage.iconSuffix))
        }
    }
    
    @Test
    fun `IconSet - 空文件列表`() {
        val iconSet = IconSet(
            sourceDir = "test/dir",
            files = emptyList()
        )
        
        assertTrue(iconSet.files.isEmpty())
    }
    
    @Test
    fun `InitIconResult - toString格式正确`() {
        val result = InitIconResult(
            createdDirs = listOf("dir1", "dir2"),
            createdFiles = listOf("file1", "file2", "file3")
        )
        
        assertEquals(5, result.totalCreated)
        assertTrue(result.toString().contains("2 个"))
        assertTrue(result.toString().contains("3 个"))
    }
}
