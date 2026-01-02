package com.empathy.ai.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
 * 仅更新版本号任务
 * 
 * @see TDD-00024 4.3.1 VersionUpdatePlugin插件入口
 */
abstract class UpdateVersionTask : DefaultTask() {
    
    @get:InputDirectory
    abstract val projectDir: DirectoryProperty
    
    @get:Input
    abstract val stage: Property<String>
    
    init {
        stage.convention("dev")
    }
    
    @Option(option = "stage", description = "发布阶段: dev, test, beta, production")
    fun setStageOption(stageValue: String) {
        stage.set(stageValue)
    }
    
    @TaskAction
    fun execute() {
        val projectDirFile = projectDir.get().asFile
        val releaseStage = ReleaseStage.fromString(stage.get())
        
        logger.lifecycle("📝 开始更新版本号...")
        
        val commitParser = CommitParser(projectDirFile)
        val versionCalculator = VersionCalculator()
        val versionManager = VersionManager(projectDirFile)
        val backupManager = BackupManager(projectDirFile)
        
        // 分析提交
        val commits = commitParser.parseCommitsSinceLastTag()
        if (commits.isEmpty()) {
            logger.lifecycle("   ⚠️ 没有新的提交，跳过版本更新")
            return
        }
        
        // 计算新版本
        val currentVersion = versionManager.getCurrentVersion()
        val nextVersion = versionCalculator.calculateNextVersion(currentVersion, commits)
        
        logger.lifecycle("   当前版本: $currentVersion")
        logger.lifecycle("   新版本: $nextVersion")
        logger.lifecycle("   发布阶段: ${releaseStage.displayName}")
        
        // 备份
        backupManager.createBackup(version = currentVersion.toString())
        
        // 更新版本
        try {
            versionManager.updateVersion(nextVersion, releaseStage)
            versionManager.updateVersionHistory(nextVersion, releaseStage, commits)
            logger.lifecycle("   ✅ 版本号更新成功")
        } catch (e: Exception) {
            throw RuntimeException("版本号更新失败: ${e.message}", e)
        }
    }
}

/**
 * 仅更新图标任务
 * 
 * @see TDD-00024 4.3.1 VersionUpdatePlugin插件入口
 */
abstract class UpdateIconTask : DefaultTask() {
    
    @get:InputDirectory
    abstract val projectDir: DirectoryProperty
    
    @get:Input
    abstract val stage: Property<String>
    
    @Option(option = "stage", description = "发布阶段: dev, test, beta, production")
    fun setStageOption(stageValue: String) {
        stage.set(stageValue)
    }
    
    @TaskAction
    fun execute() {
        val projectDirFile = projectDir.get().asFile
        val releaseStage = ReleaseStage.fromString(stage.get())
        
        logger.lifecycle("🎨 开始更新图标...")
        logger.lifecycle("   发布阶段: ${releaseStage.displayName}")
        
        val backupManager = BackupManager(projectDirFile)
        val iconManager = IconManager(projectDirFile, backupManager)
        
        // 备份图标
        backupManager.backupIcons()
        
        // 切换图标
        val result = iconManager.switchToStage(releaseStage)
        if (result.isSuccess) {
            val switchResult = result.getOrNull()!!
            logger.lifecycle("   ✅ 图标切换成功")
            logger.lifecycle("   复制文件数: ${switchResult.copiedFiles.size}")
        } else {
            throw result.exceptionOrNull() ?: Exception("图标切换失败")
        }
    }
}

/**
 * 分析Git提交任务
 * 
 * @see TDD-00024 4.3.1 VersionUpdatePlugin插件入口
 */
abstract class AnalyzeCommitsTask : DefaultTask() {
    
    @get:InputDirectory
    abstract val projectDir: DirectoryProperty
    
    @TaskAction
    fun execute() {
        val projectDirFile = projectDir.get().asFile
        
        logger.lifecycle("📊 分析Git提交...")
        
        val commitParser = CommitParser(projectDirFile)
        val versionCalculator = VersionCalculator()
        val versionManager = VersionManager(projectDirFile)
        
        // 获取当前版本
        val currentVersion = versionManager.getCurrentVersion()
        logger.lifecycle("   当前版本: $currentVersion")
        
        // 分析提交
        val commits = commitParser.parseCommitsSinceLastTag()
        logger.lifecycle("   找到 ${commits.size} 个新提交")
        
        if (commits.isEmpty()) {
            logger.lifecycle("   没有新的提交")
            return
        }
        
        // 按类型分组
        val groupedCommits = commits.groupBy { it.type }
        
        logger.lifecycle("")
        logger.lifecycle("📋 提交分类:")
        groupedCommits.forEach { (type, typeCommits) ->
            logger.lifecycle("   ${type.emoji} ${type.description}: ${typeCommits.size} 个")
            typeCommits.take(5).forEach { commit ->
                logger.lifecycle("      - ${commit.subject.take(60)}${if (commit.subject.length > 60) "..." else ""}")
            }
            if (typeCommits.size > 5) {
                logger.lifecycle("      ... 还有 ${typeCommits.size - 5} 个")
            }
        }
        
        // 计算新版本
        val nextVersion = versionCalculator.calculateNextVersion(currentVersion, commits)
        logger.lifecycle("")
        logger.lifecycle("📈 版本变更预测:")
        logger.lifecycle("   当前版本: $currentVersion")
        logger.lifecycle("   预测版本: $nextVersion")
        logger.lifecycle("   版本代码: ${nextVersion.toVersionCode()}")
        
        // 生成变更日志
        val changelog = versionCalculator.generateChangelog(commits)
        logger.lifecycle("")
        logger.lifecycle("📝 变更日志预览:")
        changelog.lines().take(30).forEach { line ->
            logger.lifecycle("   $line")
        }
    }
}

/**
 * 回滚版本任务
 * 
 * @see TDD-00024 4.3.1 VersionUpdatePlugin插件入口
 */
abstract class RollbackVersionTask : DefaultTask() {
    
    @get:InputDirectory
    abstract val projectDir: DirectoryProperty
    
    @TaskAction
    fun execute() {
        val projectDirFile = projectDir.get().asFile
        
        logger.lifecycle("🔄 开始回滚版本...")
        
        val backupManager = BackupManager(projectDirFile)
        
        // 获取最新备份
        val latestBackup = backupManager.getLatestBackup()
        if (latestBackup == null) {
            logger.lifecycle("   ❌ 没有可用的备份")
            return
        }
        
        logger.lifecycle("   找到备份: ${latestBackup.timestamp}")
        logger.lifecycle("   备份路径: ${latestBackup.backupPath}")
        
        // 恢复备份
        val result = backupManager.restore(latestBackup)
        logger.lifecycle("   ✅ 回滚成功")
        logger.lifecycle("   恢复文件数: ${result.restoredFiles}")
    }
}

/**
 * 显示当前版本任务
 * 
 * @see TDD-00024 4.3.1 VersionUpdatePlugin插件入口
 */
abstract class ShowVersionTask : DefaultTask() {
    
    @get:InputDirectory
    abstract val projectDir: DirectoryProperty
    
    @TaskAction
    fun execute() {
        val projectDirFile = projectDir.get().asFile
        
        val versionManager = VersionManager(projectDirFile)
        val iconManager = IconManager(projectDirFile)
        val backupManager = BackupManager(projectDirFile)
        
        // 获取版本信息
        val currentVersion = versionManager.getCurrentVersion()
        val currentStage = versionManager.getCurrentStage()
        val currentIconStage = iconManager.getCurrentIconStage()
        
        logger.lifecycle("📋 当前版本信息")
        logger.lifecycle("═══════════════════════════════════════")
        logger.lifecycle("   版本号: $currentVersion")
        logger.lifecycle("   版本代码: ${currentVersion.toVersionCode()}")
        logger.lifecycle("   发布阶段: ${currentStage.displayName}")
        logger.lifecycle("   图标阶段: ${currentIconStage?.displayName ?: "未知"}")
        logger.lifecycle("")
        
        // 显示版本历史
        val history = versionManager.getVersionHistory()
        if (history.history.isNotEmpty()) {
            logger.lifecycle("📚 最近版本历史 (最近5条)")
            logger.lifecycle("───────────────────────────────────────")
            history.history.take(5).forEach { entry ->
                logger.lifecycle("   ${entry.version} (${entry.stage}) - ${entry.date}")
            }
        }
        
        // 显示备份信息
        val backups = backupManager.listBackups()
        if (backups.isNotEmpty()) {
            logger.lifecycle("")
            logger.lifecycle("💾 可用备份: ${backups.size} 个")
            logger.lifecycle("   最新备份: ${backups.first().timestamp}")
            logger.lifecycle("   备份目录大小: ${backupManager.getBackupDirSize() / 1024}KB")
        }
        
        logger.lifecycle("═══════════════════════════════════════")
    }
}
