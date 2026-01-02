package com.empathy.ai.build

/**
 * 版本更新错误类型
 * 定义版本更新过程中可能发生的各种错误
 * 
 * @see TDD-00024 5.1 错误类型定义
 */
sealed class VersionUpdateError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {
    
    // ==================== Git相关错误 ====================
    
    /**
     * Git未安装或不可用
     */
    class GitNotFound(
        message: String = "Git未安装或不在PATH中"
    ) : VersionUpdateError(message)
    
    /**
     * Git操作失败
     */
    class GitOperationFailed(
        val operation: String,
        val exitCode: Int,
        val errorOutput: String,
        message: String = "Git操作失败: $operation (退出码: $exitCode)"
    ) : VersionUpdateError(message)
    
    /**
     * 没有找到提交记录
     */
    class NoCommitsFound(
        val since: String? = null,
        message: String = if (since != null) "自 $since 以来没有找到提交记录" else "没有找到提交记录"
    ) : VersionUpdateError(message)
    
    /**
     * 没有找到Git标签
     */
    class NoTagsFound(
        message: String = "没有找到Git标签"
    ) : VersionUpdateError(message)
    
    // ==================== 文件相关错误 ====================
    
    /**
     * 文件未找到
     */
    class FileNotFound(
        val filePath: String,
        message: String = "文件未找到: $filePath"
    ) : VersionUpdateError(message)
    
    /**
     * 文件写入失败
     */
    class FileWriteFailed(
        val filePath: String,
        cause: Throwable? = null,
        message: String = "文件写入失败: $filePath"
    ) : VersionUpdateError(message, cause)
    
    /**
     * 文件读取失败
     */
    class FileReadFailed(
        val filePath: String,
        cause: Throwable? = null,
        message: String = "文件读取失败: $filePath"
    ) : VersionUpdateError(message, cause)
    
    /**
     * 目录创建失败
     */
    class DirectoryCreationFailed(
        val dirPath: String,
        message: String = "目录创建失败: $dirPath"
    ) : VersionUpdateError(message)
    
    // ==================== 图标相关错误 ====================
    
    /**
     * 图标文件未找到
     */
    class IconNotFound(
        val stage: ReleaseStage,
        val iconName: String,
        message: String = "图标文件未找到: ${stage.displayName}/$iconName"
    ) : VersionUpdateError(message)
    
    /**
     * 图标切换失败
     */
    class IconSwitchFailed(
        val stage: ReleaseStage,
        cause: Throwable? = null,
        message: String = "图标切换失败: ${stage.displayName}"
    ) : VersionUpdateError(message, cause)
    
    /**
     * 图标配置缺失
     */
    class IconConfigMissing(
        val stage: ReleaseStage,
        message: String = "图标配置缺失: ${stage.displayName}"
    ) : VersionUpdateError(message)
    
    // ==================== 配置相关错误 ====================
    
    /**
     * 配置解析错误
     */
    class ConfigParseError(
        val configFile: String,
        cause: Throwable? = null,
        message: String = "配置文件解析失败: $configFile"
    ) : VersionUpdateError(message, cause)
    
    /**
     * 无效的版本号
     */
    class InvalidVersion(
        val versionString: String,
        message: String = "无效的版本号格式: $versionString"
    ) : VersionUpdateError(message)
    
    /**
     * 无效的发布阶段
     */
    class InvalidReleaseStage(
        val stageString: String,
        message: String = "无效的发布阶段: $stageString"
    ) : VersionUpdateError(message)
    
    /**
     * 配置验证失败
     */
    class ConfigValidationFailed(
        val errors: List<String>,
        message: String = "配置验证失败: ${errors.joinToString(", ")}"
    ) : VersionUpdateError(message)
    
    // ==================== 备份相关错误 ====================
    
    /**
     * 备份失败
     */
    class BackupFailed(
        cause: Throwable? = null,
        message: String = "备份创建失败"
    ) : VersionUpdateError(message, cause)
    
    /**
     * 恢复失败
     */
    class RestoreFailed(
        val backupPath: String,
        cause: Throwable? = null,
        message: String = "从备份恢复失败: $backupPath"
    ) : VersionUpdateError(message, cause)
    
    /**
     * 备份未找到
     */
    class BackupNotFound(
        val backupPath: String? = null,
        message: String = if (backupPath != null) "备份未找到: $backupPath" else "没有可用的备份"
    ) : VersionUpdateError(message)
    
    /**
     * 备份已损坏
     */
    class BackupCorrupted(
        val backupPath: String,
        message: String = "备份文件已损坏: $backupPath"
    ) : VersionUpdateError(message)
    
    // ==================== 版本更新相关错误 ====================
    
    /**
     * 版本更新失败
     */
    class VersionUpdateFailed(
        val reason: String,
        cause: Throwable? = null,
        message: String = "版本更新失败: $reason"
    ) : VersionUpdateError(message, cause)
    
    /**
     * 版本同步失败
     */
    class VersionSyncFailed(
        val targetFile: String,
        cause: Throwable? = null,
        message: String = "版本同步失败: $targetFile"
    ) : VersionUpdateError(message, cause)
    
    /**
     * 版本回滚失败
     */
    class RollbackFailed(
        cause: Throwable? = null,
        message: String = "版本回滚失败"
    ) : VersionUpdateError(message, cause)
    
    // ==================== 系统相关错误 ====================
    
    /**
     * 磁盘空间不足
     */
    class InsufficientDiskSpace(
        val requiredBytes: Long,
        val availableBytes: Long,
        message: String = "磁盘空间不足: 需要 ${requiredBytes / 1024 / 1024}MB, 可用 ${availableBytes / 1024 / 1024}MB"
    ) : VersionUpdateError(message)
    
    /**
     * 权限不足
     */
    class PermissionDenied(
        val path: String,
        message: String = "权限不足: $path"
    ) : VersionUpdateError(message)
    
    /**
     * 操作超时
     */
    class OperationTimeout(
        val operation: String,
        val timeoutMs: Long,
        message: String = "操作超时: $operation (${timeoutMs}ms)"
    ) : VersionUpdateError(message)
    
    /**
     * 并发冲突
     */
    class ConcurrencyConflict(
        val resource: String,
        message: String = "并发冲突: $resource 正在被其他进程使用"
    ) : VersionUpdateError(message)
    
    // ==================== 辅助方法 ====================
    
    /**
     * 是否可重试
     */
    val isRetryable: Boolean
        get() = when (this) {
            is GitOperationFailed -> true
            is FileWriteFailed -> true
            is OperationTimeout -> true
            is ConcurrencyConflict -> true
            else -> false
        }
    
    /**
     * 建议的重试次数
     */
    val suggestedRetryCount: Int
        get() = when (this) {
            is GitOperationFailed -> 3
            is FileWriteFailed -> 2
            is OperationTimeout -> 2
            is ConcurrencyConflict -> 5
            else -> 0
        }
    
    /**
     * 建议的重试延迟（毫秒）
     */
    val suggestedRetryDelayMs: Long
        get() = when (this) {
            is GitOperationFailed -> 1000L
            is FileWriteFailed -> 500L
            is OperationTimeout -> 2000L
            is ConcurrencyConflict -> 1000L
            else -> 0L
        }
    
    /**
     * 错误代码
     */
    val errorCode: String
        get() = when (this) {
            is GitNotFound -> "GIT_001"
            is GitOperationFailed -> "GIT_002"
            is NoCommitsFound -> "GIT_003"
            is NoTagsFound -> "GIT_004"
            is FileNotFound -> "FILE_001"
            is FileWriteFailed -> "FILE_002"
            is FileReadFailed -> "FILE_003"
            is DirectoryCreationFailed -> "FILE_004"
            is IconNotFound -> "ICON_001"
            is IconSwitchFailed -> "ICON_002"
            is IconConfigMissing -> "ICON_003"
            is ConfigParseError -> "CONFIG_001"
            is InvalidVersion -> "CONFIG_002"
            is InvalidReleaseStage -> "CONFIG_003"
            is ConfigValidationFailed -> "CONFIG_004"
            is BackupFailed -> "BACKUP_001"
            is RestoreFailed -> "BACKUP_002"
            is BackupNotFound -> "BACKUP_003"
            is BackupCorrupted -> "BACKUP_004"
            is VersionUpdateFailed -> "VERSION_001"
            is VersionSyncFailed -> "VERSION_002"
            is RollbackFailed -> "VERSION_003"
            is InsufficientDiskSpace -> "SYSTEM_001"
            is PermissionDenied -> "SYSTEM_002"
            is OperationTimeout -> "SYSTEM_003"
            is ConcurrencyConflict -> "SYSTEM_004"
        }
    
    override fun toString(): String {
        return "[$errorCode] $message"
    }
}

/**
 * 错误处理结果
 */
sealed class ErrorHandlingResult {
    /**
     * 已恢复
     */
    data class Recovered(val message: String) : ErrorHandlingResult()
    
    /**
     * 已回滚
     */
    data class RolledBack(val backupPath: String) : ErrorHandlingResult()
    
    /**
     * 需要人工干预
     */
    data class ManualInterventionRequired(val instructions: List<String>) : ErrorHandlingResult()
    
    /**
     * 无法恢复
     */
    data class Unrecoverable(val error: VersionUpdateError) : ErrorHandlingResult()
}
