package com.empathy.ai.build

import java.io.File

/**
 * 多模块版本同步管理器
 * 支持在多模块项目中同步版本号
 * 
 * @property projectDir 项目根目录
 * @property updaters 版本更新器列表
 * 
 * @see TDD-00024 4.5.4 VersionSyncManager多模块版本同步管理器
 */
class VersionSyncManager(
    private val projectDir: File,
    private val updaters: List<VersionFileUpdater> = listOf(
        GradlePropertiesUpdater()
    )
) {
    
    /**
     * 同步所有模块的版本号
     * @param version 目标版本号
     * @param modules 要同步的模块列表（相对路径），为空则同步所有发现的模块
     * @return 同步结果
     */
    fun syncVersions(
        version: SemanticVersion,
        modules: List<String> = emptyList()
    ): VersionSyncResult {
        val targetDirs = if (modules.isEmpty()) {
            discoverModules()
        } else {
            modules.map { File(projectDir, it) }
        }
        
        val results = mutableListOf<ModuleSyncResult>()
        
        targetDirs.forEach { moduleDir ->
            val moduleResults = syncModuleVersion(moduleDir, version)
            results.addAll(moduleResults)
        }
        
        return VersionSyncResult(
            version = version,
            syncedModules = results.filter { it.success },
            failedModules = results.filter { !it.success }
        )
    }
    
    /**
     * 同步所有模块的版本号和发布阶段
     * @param version 目标版本号
     * @param stage 发布阶段
     * @param modules 要同步的模块列表（相对路径），为空则同步所有发现的模块
     * @return 同步结果
     */
    fun syncVersionsWithStage(
        version: SemanticVersion,
        stage: ReleaseStage,
        modules: List<String> = emptyList()
    ): VersionSyncResult {
        val targetDirs = if (modules.isEmpty()) {
            discoverModules()
        } else {
            modules.map { File(projectDir, it) }
        }
        
        val results = mutableListOf<ModuleSyncResult>()
        
        targetDirs.forEach { moduleDir ->
            val moduleResults = syncModuleVersionWithStage(moduleDir, version, stage)
            results.addAll(moduleResults)
        }
        
        return VersionSyncResult(
            version = version,
            syncedModules = results.filter { it.success },
            failedModules = results.filter { !it.success }
        )
    }
    
    /**
     * 发现项目中的所有模块
     * @return 模块目录列表
     */
    fun discoverModules(): List<File> {
        val modules = mutableListOf<File>()
        
        // 添加根目录
        modules.add(projectDir)
        
        // 查找子模块（包含 build.gradle.kts 或 build.gradle 的目录）
        projectDir.listFiles()?.forEach { file ->
            if (file.isDirectory && !file.name.startsWith(".") && !file.name.startsWith("build")) {
                val hasBuildFile = File(file, "build.gradle.kts").exists() ||
                                   File(file, "build.gradle").exists()
                if (hasBuildFile) {
                    modules.add(file)
                }
            }
        }
        
        return modules
    }
    
    /**
     * 同步单个模块的版本号
     */
    private fun syncModuleVersion(
        moduleDir: File,
        version: SemanticVersion
    ): List<ModuleSyncResult> {
        val results = mutableListOf<ModuleSyncResult>()
        
        updaters.forEach { updater ->
            updater.supportedFiles.forEach { fileName ->
                val file = File(moduleDir, fileName)
                if (file.exists()) {
                    try {
                        val success = updater.updateVersion(file, version)
                        results.add(ModuleSyncResult(
                            modulePath = moduleDir.relativeTo(projectDir).path.ifEmpty { "." },
                            fileName = fileName,
                            updaterName = updater.name,
                            success = success,
                            error = null
                        ))
                    } catch (e: Exception) {
                        results.add(ModuleSyncResult(
                            modulePath = moduleDir.relativeTo(projectDir).path.ifEmpty { "." },
                            fileName = fileName,
                            updaterName = updater.name,
                            success = false,
                            error = e.message
                        ))
                    }
                }
            }
        }
        
        return results
    }
    
    /**
     * 同步单个模块的版本号和发布阶段
     */
    private fun syncModuleVersionWithStage(
        moduleDir: File,
        version: SemanticVersion,
        stage: ReleaseStage
    ): List<ModuleSyncResult> {
        val results = mutableListOf<ModuleSyncResult>()
        
        updaters.forEach { updater ->
            updater.supportedFiles.forEach { fileName ->
                val file = File(moduleDir, fileName)
                if (file.exists()) {
                    try {
                        val success = if (updater is GradlePropertiesUpdater) {
                            updater.updateVersionWithStage(file, version, stage)
                        } else {
                            updater.updateVersion(file, version)
                        }
                        results.add(ModuleSyncResult(
                            modulePath = moduleDir.relativeTo(projectDir).path.ifEmpty { "." },
                            fileName = fileName,
                            updaterName = updater.name,
                            success = success,
                            error = null
                        ))
                    } catch (e: Exception) {
                        results.add(ModuleSyncResult(
                            modulePath = moduleDir.relativeTo(projectDir).path.ifEmpty { "." },
                            fileName = fileName,
                            updaterName = updater.name,
                            success = false,
                            error = e.message
                        ))
                    }
                }
            }
        }
        
        return results
    }
    
    /**
     * 获取所有模块的当前版本
     * @return 模块版本映射
     */
    fun getAllVersions(): Map<String, SemanticVersion?> {
        val modules = discoverModules()
        val versions = mutableMapOf<String, SemanticVersion?>()
        
        modules.forEach { moduleDir ->
            val modulePath = moduleDir.relativeTo(projectDir).path.ifEmpty { "." }
            
            // 尝试从每个更新器读取版本
            for (updater in updaters) {
                for (fileName in updater.supportedFiles) {
                    val file = File(moduleDir, fileName)
                    if (file.exists()) {
                        val version = updater.readVersion(file)
                        if (version != null) {
                            versions[modulePath] = version
                            break
                        }
                    }
                }
                if (versions.containsKey(modulePath)) break
            }
            
            // 如果没有找到版本，设置为null
            if (!versions.containsKey(modulePath)) {
                versions[modulePath] = null
            }
        }
        
        return versions
    }
    
    /**
     * 检查所有模块版本是否一致
     * @return 版本一致性检查结果
     */
    fun checkVersionConsistency(): VersionConsistencyResult {
        val versions = getAllVersions()
        val nonNullVersions = versions.values.filterNotNull()
        
        if (nonNullVersions.isEmpty()) {
            return VersionConsistencyResult(
                isConsistent = true,
                versions = versions,
                inconsistentModules = emptyList()
            )
        }
        
        val expectedVersion = nonNullVersions.first()
        val inconsistent = versions.filter { (_, version) ->
            version != null && version != expectedVersion
        }.keys.toList()
        
        return VersionConsistencyResult(
            isConsistent = inconsistent.isEmpty(),
            versions = versions,
            inconsistentModules = inconsistent
        )
    }
}

/**
 * 版本同步结果
 */
data class VersionSyncResult(
    val version: SemanticVersion,
    val syncedModules: List<ModuleSyncResult>,
    val failedModules: List<ModuleSyncResult>
) {
    val isSuccess: Boolean get() = failedModules.isEmpty()
    val totalSynced: Int get() = syncedModules.size
    
    override fun toString(): String {
        return buildString {
            appendLine("版本同步结果: $version")
            appendLine("成功: ${syncedModules.size} 个模块")
            appendLine("失败: ${failedModules.size} 个模块")
            if (failedModules.isNotEmpty()) {
                appendLine("失败详情:")
                failedModules.forEach { result ->
                    appendLine("  - ${result.modulePath}/${result.fileName}: ${result.error}")
                }
            }
        }
    }
}

/**
 * 模块同步结果
 */
data class ModuleSyncResult(
    val modulePath: String,
    val fileName: String,
    val updaterName: String,
    val success: Boolean,
    val error: String?
)

/**
 * 版本一致性检查结果
 */
data class VersionConsistencyResult(
    val isConsistent: Boolean,
    val versions: Map<String, SemanticVersion?>,
    val inconsistentModules: List<String>
) {
    override fun toString(): String {
        return buildString {
            appendLine("版本一致性检查: ${if (isConsistent) "✓ 一致" else "✗ 不一致"}")
            appendLine("模块版本:")
            versions.forEach { (module, version) ->
                appendLine("  - $module: ${version ?: "未配置"}")
            }
            if (inconsistentModules.isNotEmpty()) {
                appendLine("不一致的模块: ${inconsistentModules.joinToString(", ")}")
            }
        }
    }
}
