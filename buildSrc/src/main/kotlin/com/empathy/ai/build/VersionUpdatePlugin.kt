package com.empathy.ai.build

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 版本更新Gradle插件
 * 
 * 提供版本号和图标自动更新功能的Gradle插件入口。
 * 
 * 注册的任务:
 * - updateVersionAndIcon: 主任务，更新版本号和图标
 * - updateVersion: 仅更新版本号
 * - updateIcon: 仅更新图标
 * - analyzeCommits: 分析Git提交信息
 * - rollbackVersion: 回滚到上一个版本
 * - showCurrentVersion: 显示当前版本信息
 * - initIconResources: 初始化图标资源目录
 * 
 * @see TDD-00024 4.3.1 VersionUpdatePlugin插件入口
 */
class VersionUpdatePlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        // 创建扩展配置
        val extension = project.extensions.create(
            "versionUpdate",
            VersionUpdateExtension::class.java
        )
        
        // 注册主任务
        project.tasks.register("updateVersionAndIcon", UpdateVersionAndIconTask::class.java) {
            group = "version"
            description = "更新版本号和图标（基于Git提交分析）"
            projectDir.set(project.projectDir)
            stage.set(extension.defaultStage)
            dryRun.set(false)
            force.set(false)
        }
        
        // 注册仅更新版本号任务
        project.tasks.register("updateVersion", UpdateVersionTask::class.java) {
            group = "version"
            description = "仅更新版本号（基于Git提交分析）"
            projectDir.set(project.projectDir)
            stage.set(extension.defaultStage)
        }
        
        // 注册仅更新图标任务
        project.tasks.register("updateIcon", UpdateIconTask::class.java) {
            group = "version"
            description = "仅更新图标（根据发布阶段）"
            projectDir.set(project.projectDir)
            stage.set(extension.defaultStage)
        }
        
        // 注册分析提交任务
        project.tasks.register("analyzeCommits", AnalyzeCommitsTask::class.java) {
            group = "version"
            description = "分析Git提交信息，预览版本变更"
            projectDir.set(project.projectDir)
        }
        
        // 注册回滚任务
        project.tasks.register("rollbackVersion", RollbackVersionTask::class.java) {
            group = "version"
            description = "回滚到上一个版本"
            projectDir.set(project.projectDir)
        }
        
        // 注册显示版本任务
        project.tasks.register("showCurrentVersion", ShowVersionTask::class.java) {
            group = "version"
            description = "显示当前版本信息"
            projectDir.set(project.projectDir)
        }
        
        // 注册图标初始化任务
        project.tasks.register("initIconResources", InitIconResourcesTask::class.java) {
            generatePlaceholders.set(true)
            forceOverwrite.set(false)
            outputDirectory.set(project.file("assets/icons"))
        }
    }
}

/**
 * 版本更新扩展配置
 */
open class VersionUpdateExtension {
    /** 默认发布阶段 */
    var defaultStage: String = "dev"
    
    /** 是否启用自动备份 */
    var autoBackup: Boolean = true
    
    /** 最大备份数量 */
    var maxBackups: Int = 50
    
    /** 备份保留天数 */
    var backupRetentionDays: Int = 30
    
    /** 是否启用版本历史记录 */
    var enableVersionHistory: Boolean = true
    
    /** 版本号前缀 */
    var versionPrefix: String = ""
    
    /** 是否在CI环境中运行 */
    var ciMode: Boolean = false
}
