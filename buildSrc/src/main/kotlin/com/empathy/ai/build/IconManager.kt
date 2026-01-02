package com.empathy.ai.build

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 图标切换结果
 * 
 * @property stage 目标发布阶段
 * @property copiedFiles 复制的文件列表
 * @property timestamp 操作时间戳
 */
data class IconSwitchResult(
    val stage: ReleaseStage,
    val copiedFiles: List<String>,
    val timestamp: Long
)

/**
 * 图标映射配置
 * 
 * @property version 配置版本
 * @property defaultStage 默认发布阶段
 * @property iconSets 各阶段的图标配置
 */
@Serializable
data class IconMapping(
    val version: Int = 1,
    val defaultStage: String = "production",
    val iconSets: Map<String, IconSet> = emptyMap()
) {
    companion object {
        /**
         * 创建默认配置
         */
        fun createDefault(): IconMapping {
            val stages = listOf("dev", "test", "beta", "production")
            val iconSets = stages.associateWith { stage ->
                IconSet(
                    sourceDir = "assets/icons/$stage",
                    files = listOf(
                        "ic_launcher.png",
                        "ic_launcher_round.png",
                        "ic_launcher_foreground.png"
                    )
                )
            }
            return IconMapping(iconSets = iconSets)
        }
    }
}

/**
 * 图标集配置
 * 
 * @property sourceDir 源图标目录
 * @property files 图标文件列表
 */
@Serializable
data class IconSet(
    val sourceDir: String,
    val files: List<String>
)

/**
 * 图标管理器
 * 负责根据发布阶段切换应用图标
 * 
 * @property projectDir 项目根目录
 * @property backupManager 备份管理器（可选）
 * @property useUnifiedIcon 是否使用统一图标模式（个人开发者模式）
 * 
 * @see TDD-00024 4.2.4 IconManager图标管理器
 */
class IconManager(
    private val projectDir: File,
    private val backupManager: BackupManager? = null,
    private val useUnifiedIcon: Boolean = true  // 默认使用统一图标
) {
    
    private val iconMappingFile = File(projectDir, "config/icon-mapping.json")
    
    /** 统一图标文件路径（根目录的软件图标.png） */
    private val unifiedIconFile = File(projectDir, "软件图标.png")
    
    private val json = Json { 
        prettyPrint = true 
        ignoreUnknownKeys = true
    }
    
    /** 支持的图标密度 */
    private val densities = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
    
    /**
     * 切换到指定发布阶段的图标
     * @param stage 目标发布阶段
     * @return 切换结果
     */
    fun switchToStage(stage: ReleaseStage): Result<IconSwitchResult> {
        // 如果使用统一图标模式，直接使用根目录的软件图标.png
        if (useUnifiedIcon) {
            return switchToUnifiedIcon(stage)
        }
        
        return switchToStageIcon(stage)
    }
    
    /**
     * 使用统一图标（个人开发者模式）
     * 所有阶段都使用根目录的软件图标.png
     */
    private fun switchToUnifiedIcon(stage: ReleaseStage): Result<IconSwitchResult> {
        return try {
            // 1. 检查统一图标文件是否存在
            if (!unifiedIconFile.exists()) {
                return Result.failure(Exception("统一图标文件不存在: ${unifiedIconFile.path}"))
            }
            
            // 2. 备份当前图标（如果有备份管理器）
            backupManager?.backupIcons()
            
            // 3. 复制图标到所有密度目录
            val copiedFiles = mutableListOf<String>()
            
            densities.forEach { density ->
                val targetDir = File(projectDir, "app/src/main/res/mipmap-$density")
                targetDir.mkdirs()
                
                // 复制为ic_launcher.png和ic_launcher_round.png
                listOf("ic_launcher.png", "ic_launcher_round.png").forEach { fileName ->
                    val target = File(targetDir, fileName)
                    unifiedIconFile.copyTo(target, overwrite = true)
                    copiedFiles.add(target.relativeTo(projectDir).path)
                }
            }
            
            Result.success(IconSwitchResult(
                stage = stage,
                copiedFiles = copiedFiles,
                timestamp = System.currentTimeMillis()
            ))
        } catch (e: Exception) {
            // 回滚
            backupManager?.restoreIcons()
            Result.failure(e)
        }
    }
    
    /**
     * 切换到指定阶段的图标（多阶段图标模式）
     */
    private fun switchToStageIcon(stage: ReleaseStage): Result<IconSwitchResult> {
        return try {
            // 1. 备份当前图标（如果有备份管理器）
            backupManager?.backupIcons()
            
            // 2. 获取图标配置
            val iconMapping = loadIconMapping()
            val iconSet = iconMapping.iconSets[stage.iconSuffix]
                ?: return Result.failure(Exception("未找到${stage.displayName}的图标配置"))
            
            // 3. 验证源图标存在
            val sourceDir = File(projectDir, iconSet.sourceDir)
            if (!sourceDir.exists()) {
                return Result.failure(Exception("源图标目录不存在: ${sourceDir.path}"))
            }
            
            // 4. 复制图标文件
            val copiedFiles = mutableListOf<String>()
            
            densities.forEach { density ->
                val targetDir = File(projectDir, "app/src/main/res/mipmap-$density")
                targetDir.mkdirs()
                
                iconSet.files.forEach { fileName ->
                    val source = File(sourceDir, "$density/$fileName")
                    val target = File(targetDir, fileName)
                    
                    if (source.exists()) {
                        source.copyTo(target, overwrite = true)
                        copiedFiles.add(target.relativeTo(projectDir).path)
                    } else {
                        // 尝试从根目录复制（兼容简单结构）
                        val rootSource = File(sourceDir, fileName)
                        if (rootSource.exists()) {
                            rootSource.copyTo(target, overwrite = true)
                            copiedFiles.add(target.relativeTo(projectDir).path)
                        }
                    }
                }
            }
            
            // 5. 更新自适应图标（Android 8.0+）
            updateAdaptiveIcon(stage, sourceDir)
            
            Result.success(IconSwitchResult(
                stage = stage,
                copiedFiles = copiedFiles,
                timestamp = System.currentTimeMillis()
            ))
        } catch (e: Exception) {
            // 回滚
            backupManager?.restoreIcons()
            Result.failure(e)
        }
    }
    
    /**
     * 更新自适应图标
     */
    private fun updateAdaptiveIcon(stage: ReleaseStage, sourceDir: File) {
        val targetDir = File(projectDir, "app/src/main/res/drawable-v26")
        targetDir.mkdirs()
        
        val foregroundFile = File(sourceDir, "ic_launcher_foreground.png")
        if (foregroundFile.exists()) {
            foregroundFile.copyTo(
                File(targetDir, "ic_launcher_foreground.png"),
                overwrite = true
            )
        }
        
        // 也检查各密度目录
        densities.forEach { density ->
            val densityForeground = File(sourceDir, "$density/ic_launcher_foreground.png")
            if (densityForeground.exists()) {
                val targetDensityDir = File(projectDir, "app/src/main/res/drawable-$density-v26")
                targetDensityDir.mkdirs()
                densityForeground.copyTo(
                    File(targetDensityDir, "ic_launcher_foreground.png"),
                    overwrite = true
                )
            }
        }
    }
    
    /**
     * 加载图标映射配置
     */
    fun loadIconMapping(): IconMapping {
        return if (iconMappingFile.exists()) {
            try {
                json.decodeFromString(iconMappingFile.readText())
            } catch (e: Exception) {
                IconMapping.createDefault()
            }
        } else {
            IconMapping.createDefault()
        }
    }
    
    /**
     * 保存图标映射配置
     */
    fun saveIconMapping(mapping: IconMapping) {
        iconMappingFile.parentFile?.mkdirs()
        iconMappingFile.writeText(json.encodeToString(IconMapping.serializer(), mapping))
    }
    
    /**
     * 检查图标资源是否完整
     * @param stage 发布阶段
     * @return 缺失的文件列表，为空表示完整
     */
    fun checkIconResources(stage: ReleaseStage): List<String> {
        val iconMapping = loadIconMapping()
        val iconSet = iconMapping.iconSets[stage.iconSuffix] ?: return listOf("配置缺失")
        
        val sourceDir = File(projectDir, iconSet.sourceDir)
        if (!sourceDir.exists()) {
            return listOf("目录不存在: ${iconSet.sourceDir}")
        }
        
        val missingFiles = mutableListOf<String>()
        
        iconSet.files.forEach { fileName ->
            val rootFile = File(sourceDir, fileName)
            if (!rootFile.exists()) {
                // 检查各密度目录
                val hasAnyDensity = densities.any { density ->
                    File(sourceDir, "$density/$fileName").exists()
                }
                if (!hasAnyDensity) {
                    missingFiles.add(fileName)
                }
            }
        }
        
        return missingFiles
    }
    
    /**
     * 检查所有阶段的图标资源
     * @return 各阶段缺失文件的映射
     */
    fun checkAllIconResources(): Map<ReleaseStage, List<String>> {
        return ReleaseStage.values().associateWith { stage ->
            checkIconResources(stage)
        }
    }
    
    /**
     * 获取当前应用的图标阶段（基于文件比较）
     * @return 当前阶段，无法确定时返回null
     */
    fun getCurrentIconStage(): ReleaseStage? {
        val iconMapping = loadIconMapping()
        
        // 获取当前应用图标的哈希
        val currentIconFile = File(projectDir, "app/src/main/res/mipmap-xxxhdpi/ic_launcher.png")
        if (!currentIconFile.exists()) return null
        
        val currentHash = currentIconFile.readBytes().contentHashCode()
        
        // 与各阶段图标比较
        for (stage in ReleaseStage.values()) {
            val iconSet = iconMapping.iconSets[stage.iconSuffix] ?: continue
            val sourceDir = File(projectDir, iconSet.sourceDir)
            
            val sourceFile = File(sourceDir, "xxxhdpi/ic_launcher.png")
            if (sourceFile.exists() && sourceFile.readBytes().contentHashCode() == currentHash) {
                return stage
            }
            
            // 也检查根目录
            val rootSourceFile = File(sourceDir, "ic_launcher.png")
            if (rootSourceFile.exists() && rootSourceFile.readBytes().contentHashCode() == currentHash) {
                return stage
            }
        }
        
        return null
    }
    
    /**
     * 初始化图标资源目录
     * @param generatePlaceholders 是否生成占位图标
     */
    fun initIconResources(generatePlaceholders: Boolean = true): InitIconResult {
        val createdDirs = mutableListOf<String>()
        val createdFiles = mutableListOf<String>()
        
        // 创建各阶段目录
        ReleaseStage.values().forEach { stage ->
            val stageDir = File(projectDir, "assets/icons/${stage.iconSuffix}")
            if (!stageDir.exists()) {
                stageDir.mkdirs()
                createdDirs.add(stageDir.relativeTo(projectDir).path)
            }
            
            // 创建各密度子目录
            densities.forEach { density ->
                val densityDir = File(stageDir, density)
                if (!densityDir.exists()) {
                    densityDir.mkdirs()
                }
            }
        }
        
        // 生成占位图标
        if (generatePlaceholders) {
            val placeholderPng = createPlaceholderPng()
            
            ReleaseStage.values().forEach { stage ->
                val stageDir = File(projectDir, "assets/icons/${stage.iconSuffix}")
                val iconFiles = listOf("ic_launcher.png", "ic_launcher_round.png", "ic_launcher_foreground.png")
                
                iconFiles.forEach { fileName ->
                    val iconFile = File(stageDir, fileName)
                    if (!iconFile.exists()) {
                        iconFile.writeBytes(placeholderPng)
                        createdFiles.add(iconFile.relativeTo(projectDir).path)
                    }
                }
            }
        }
        
        // 创建配置文件
        if (!iconMappingFile.exists()) {
            saveIconMapping(IconMapping.createDefault())
            createdFiles.add(iconMappingFile.relativeTo(projectDir).path)
        }
        
        return InitIconResult(
            createdDirs = createdDirs,
            createdFiles = createdFiles
        )
    }
    
    /**
     * 创建占位PNG（1x1像素透明图片）
     */
    private fun createPlaceholderPng(): ByteArray {
        // 最小有效PNG文件（1x1像素，透明）
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

/**
 * 图标初始化结果
 */
data class InitIconResult(
    val createdDirs: List<String>,
    val createdFiles: List<String>
) {
    val totalCreated: Int get() = createdDirs.size + createdFiles.size
    
    override fun toString(): String {
        return buildString {
            appendLine("图标资源初始化完成:")
            appendLine("  创建目录: ${createdDirs.size} 个")
            appendLine("  创建文件: ${createdFiles.size} 个")
        }
    }
}
