package com.empathy.ai.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
 * 图标资源初始化任务
 * 
 * 创建图标资源目录结构和占位图标文件，用于项目初始化或重置图标资源。
 * 
 * 使用方式:
 * ```
 * ./gradlew initIconResources
 * ./gradlew initIconResources --no-placeholders
 * ./gradlew initIconResources --force
 * ```
 * 
 * @see TDD-00024 4.4 InitIconResourcesTask图标资源初始化任务
 * @see TD-00024 T016
 */
abstract class InitIconResourcesTask : DefaultTask() {
    
    init {
        group = "version"
        description = "初始化图标资源目录结构和占位图标"
    }
    
    /**
     * 是否生成占位图标
     * 默认为true，使用--no-placeholders可禁用
     */
    @get:Input
    abstract val generatePlaceholders: Property<Boolean>
    
    /**
     * 是否强制重新初始化（覆盖已存在的文件）
     * 默认为false，使用--force-init可启用
     */
    @get:Input
    @get:Option(option = "force-init", description = "强制重新初始化，覆盖已存在的文件")
    abstract val forceOverwrite: Property<Boolean>
    
    /**
     * 图标资源输出目录
     */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
    
    /**
     * 设置是否生成占位图标
     */
    @Option(option = "no-placeholders", description = "不生成占位图标文件")
    fun setNoPlaceholders(noPlaceholders: Boolean) {
        generatePlaceholders.set(!noPlaceholders)
    }
    
    @TaskAction
    fun execute() {
        val projectDir = project.projectDir
        val iconManager = IconManager(projectDir)
        
        logger.lifecycle("🎨 开始初始化图标资源...")
        logger.lifecycle("   项目目录: ${projectDir.absolutePath}")
        logger.lifecycle("   生成占位图标: ${generatePlaceholders.get()}")
        logger.lifecycle("   强制模式: ${forceOverwrite.get()}")
        
        // 如果是强制模式，先清理已存在的资源
        if (forceOverwrite.get()) {
            cleanExistingResources(projectDir)
        }
        
        // 执行初始化
        val result = if (forceOverwrite.get()) {
            // 强制模式：使用自定义初始化逻辑
            forceInitIconResources(projectDir, generatePlaceholders.get())
        } else {
            // 普通模式：使用IconManager的初始化
            iconManager.initIconResources(generatePlaceholders.get())
        }
        
        // 输出结果
        logger.lifecycle("")
        logger.lifecycle("✅ 图标资源初始化完成!")
        logger.lifecycle("   创建目录: ${result.createdDirs.size} 个")
        result.createdDirs.forEach { dir ->
            logger.lifecycle("      📁 $dir")
        }
        logger.lifecycle("   创建文件: ${result.createdFiles.size} 个")
        result.createdFiles.forEach { file ->
            logger.lifecycle("      📄 $file")
        }
        
        // 检查资源完整性
        logger.lifecycle("")
        logger.lifecycle("📋 资源完整性检查:")
        val allMissing = iconManager.checkAllIconResources()
        allMissing.forEach { (stage, missing) ->
            val status = if (missing.isEmpty()) "✅" else "⚠️"
            val missingInfo = if (missing.isEmpty()) "完整" else "缺失: ${missing.joinToString(", ")}"
            logger.lifecycle("   $status ${stage.displayName}: $missingInfo")
        }
        
        // 提示下一步操作
        logger.lifecycle("")
        logger.lifecycle("💡 下一步操作:")
        logger.lifecycle("   1. 将实际图标文件放入 assets/icons/<stage>/ 目录")
        logger.lifecycle("   2. 支持的密度: mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi")
        logger.lifecycle("   3. 使用 ./gradlew updateIcon --stage=<stage> 切换图标")
    }
    
    /**
     * 清理已存在的图标资源
     */
    private fun cleanExistingResources(projectDir: java.io.File) {
        logger.lifecycle("🗑️ 清理已存在的图标资源...")
        
        val iconsDir = java.io.File(projectDir, "assets/icons")
        if (iconsDir.exists()) {
            iconsDir.deleteRecursively()
            logger.lifecycle("   已删除: assets/icons/")
        }
        
        val configFile = java.io.File(projectDir, "config/icon-mapping.json")
        if (configFile.exists()) {
            configFile.delete()
            logger.lifecycle("   已删除: config/icon-mapping.json")
        }
    }
    
    /**
     * 强制初始化图标资源（覆盖已存在的文件）
     */
    private fun forceInitIconResources(projectDir: java.io.File, generatePlaceholders: Boolean): InitIconResult {
        val createdDirs = mutableListOf<String>()
        val createdFiles = mutableListOf<String>()
        val densities = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
        
        // 创建各阶段目录
        ReleaseStage.values().forEach { stage ->
            val stageDir = java.io.File(projectDir, "assets/icons/${stage.iconSuffix}")
            stageDir.mkdirs()
            createdDirs.add(stageDir.relativeTo(projectDir).path)
            
            // 创建各密度子目录
            densities.forEach { density ->
                val densityDir = java.io.File(stageDir, density)
                densityDir.mkdirs()
            }
        }
        
        // 生成占位图标
        if (generatePlaceholders) {
            val placeholderPng = createPlaceholderPng()
            val iconFiles = listOf("ic_launcher.png", "ic_launcher_round.png", "ic_launcher_foreground.png")
            
            ReleaseStage.values().forEach { stage ->
                val stageDir = java.io.File(projectDir, "assets/icons/${stage.iconSuffix}")
                
                iconFiles.forEach { fileName ->
                    val iconFile = java.io.File(stageDir, fileName)
                    iconFile.writeBytes(placeholderPng)
                    createdFiles.add(iconFile.relativeTo(projectDir).path)
                }
            }
        }
        
        // 创建配置文件
        val configDir = java.io.File(projectDir, "config")
        configDir.mkdirs()
        val configFile = java.io.File(configDir, "icon-mapping.json")
        val iconManager = IconManager(projectDir)
        iconManager.saveIconMapping(IconMapping.createDefault())
        createdFiles.add(configFile.relativeTo(projectDir).path)
        
        return InitIconResult(createdDirs, createdFiles)
    }
    
    /**
     * 创建占位PNG（1x1像素透明图片）
     */
    private fun createPlaceholderPng(): ByteArray {
        return byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15.toByte(), 0xC4.toByte(),
            0x89.toByte(), 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
            0x54, 0x08, 0xD7.toByte(), 0x63, 0x00, 0x01, 0x00, 0x00,
            0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, 0xB4.toByte(), 0x00,
            0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, 0xAE.toByte(),
            0x42, 0x60, 0x82.toByte()
        )
    }
}
