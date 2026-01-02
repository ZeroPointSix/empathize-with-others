package com.empathy.ai.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

/**
 * 版本和图标更新主任务
 * 
 * 执行完整的版本更新流程：
 * 1. 分析Git提交
 * 2. 计算新版本号
 * 3. 创建备份
 * 4. 更新版本号
 * 5. 更新图标
 * 6. 更新版本历史
 * 
 * 使用方式:
 * ```
 * ./gradlew updateVersionAndIcon
 * ./gradlew updateVersionAndIcon --stage=beta
 * ./gradlew updateVersionAndIcon --dry-run
 * ./gradlew updateVersionAndIcon --force
 * ```
 * 
 * @see TDD-00024 4.3.2 UpdateVersionAndIconTask主任务
 */
abstract class UpdateVersionAndIconTask : DefaultTask() {
    
    @get:InputDirectory
    abstract val projectDir: DirectoryProperty
    
    @get:Input
    abstract val stage: Property<String>
    
    @get:Input
    abstract val dryRun: Property<Boolean>
    
    @get:Input
    abstract val force: Property<Boolean>
    
    @Option(option = "stage", description = "发布阶段: dev, test, beta, production")
    fun setStageOption(stageValue: String) {
        stage.set(stageValue)
    }
    
    @Option(option = "dry-run", description = "预览模式，不实际执行更新")
    fun setDryRunOption(dryRunValue: Boolean) {
        dryRun.set(dryRunValue)
    }
    
    @Option(option = "force", description = "强制更新，忽略未提交的更改")
    fun setForceOption(forceValue: Boolean) {
        force.set(forceValue)
    }
    
    @TaskAction
    fun execute() {
        val projectDirFile = projectDir.get().asFile
        val releaseStage = ReleaseStage.fromString(stage.get())
        val isDryRun = dryRun.get()
        val isForce = force.get()
        
        logger.lifecycle("🚀 开始版本更新流程...")
        logger.lifecycle("   项目目录: ${projectDirFile.absolutePath}")
        logger.lifecycle("   发布阶段: ${releaseStage.displayName}")
        logger.lifecycle("   预览模式: $isDryRun")
        logger.lifecycle("   强制模式: $isForce")
        logger.lifecycle("")
        
        // 初始化管理器
        val commitParser = CommitParser(projectDirFile)
        val versionCalculator = VersionCalculator()
        val versionManager = VersionManager(projectDirFile)
        val backupManager = BackupManager(projectDirFile)
        val iconManager = IconManager(projectDirFile, backupManager)
        
        try {
            // Step 1: 分析Git提交
            logger.lifecycle("📊 Step 1/6: 分析Git提交...")
            val commits = commitParser.parseCommitsSinceLastTag()
            if (commits.isEmpty() && !isForce) {
                logger.lifecycle("   ⚠️ 没有新的提交，跳过版本更新")
                logger.lifecycle("   💡 使用 --force 参数强制更新")
                return
            }
            logger.lifecycle("   找到 ${commits.size} 个新提交")
            
            // Step 2: 计算新版本号
            logger.lifecycle("📊 Step 2/6: 计算新版本号...")
            val currentVersion = versionManager.getCurrentVersion()
            val nextVersion = if (commits.isNotEmpty()) {
                versionCalculator.calculateNextVersion(currentVersion, commits)
            } else {
                currentVersion
            }
            logger.lifecycle("   当前版本: $currentVersion")
            logger.lifecycle("   新版本: $nextVersion")
            
            if (isDryRun) {
                logger.lifecycle("")
                logger.lifecycle("🔍 预览模式 - 以下操作将被执行:")
                logger.lifecycle("   - 更新版本号: $currentVersion → $nextVersion")
                logger.lifecycle("   - 切换图标: ${releaseStage.displayName}")
                logger.lifecycle("   - 更新版本历史")
                
                // 生成变更日志预览
                if (commits.isNotEmpty()) {
                    logger.lifecycle("")
                    logger.lifecycle("📝 变更日志预览:")
                    val changelog = versionCalculator.generateChangelog(commits)
                    changelog.lines().take(20).forEach { line ->
                        logger.lifecycle("   $line")
                    }
                    if (changelog.lines().size > 20) {
                        logger.lifecycle("   ... (更多内容省略)")
                    }
                }
                return
            }
            
            // Step 3: 创建备份
            logger.lifecycle("💾 Step 3/6: 创建备份...")
            val backupResult = backupManager.createBackup(
                version = currentVersion.toString(),
                stage = releaseStage.iconSuffix
            )
            logger.lifecycle("   备份已创建: ${backupResult.backupPath}")
            logger.lifecycle("   备份文件数: ${backupResult.fileCount}")
            
            // Step 4: 更新版本号
            logger.lifecycle("📝 Step 4/6: 更新版本号...")
            try {
                versionManager.updateVersion(nextVersion, releaseStage)
                logger.lifecycle("   ✅ 版本号更新成功")
            } catch (e: Exception) {
                throw RuntimeException("版本号更新失败: ${e.message}", e)
            }
            
            // Step 5: 更新图标
            logger.lifecycle("🎨 Step 5/6: 更新图标...")
            val iconResult = iconManager.switchToStage(releaseStage)
            if (iconResult.isSuccess) {
                val switchResult = iconResult.getOrNull()!!
                logger.lifecycle("   ✅ 图标切换成功")
                logger.lifecycle("   复制文件数: ${switchResult.copiedFiles.size}")
            } else {
                logger.warn("   ⚠️ 图标切换失败: ${iconResult.exceptionOrNull()?.message}")
                logger.warn("   继续执行其他步骤...")
            }
            
            // Step 6: 更新版本历史
            logger.lifecycle("📚 Step 6/6: 更新版本历史...")
            versionManager.updateVersionHistory(nextVersion, releaseStage, commits)
            logger.lifecycle("   ✅ 版本历史已更新")
            
            // 完成
            logger.lifecycle("")
            logger.lifecycle("🎉 版本更新完成!")
            logger.lifecycle("   新版本: $nextVersion")
            logger.lifecycle("   版本代码: ${nextVersion.toVersionCode()}")
            logger.lifecycle("   发布阶段: ${releaseStage.displayName}")
            
        } catch (e: Exception) {
            logger.error("❌ 版本更新失败: ${e.message}")
            
            // 尝试回滚
            logger.lifecycle("🔄 尝试回滚...")
            try {
                val latestBackup = backupManager.getLatestBackup()
                if (latestBackup != null) {
                    backupManager.restore(latestBackup)
                    logger.lifecycle("   ✅ 已回滚到备份: ${latestBackup.timestamp}")
                }
            } catch (rollbackError: Exception) {
                logger.error("   ❌ 回滚失败: ${rollbackError.message}")
            }
            
            throw e
        }
    }
}
