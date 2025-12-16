package com.empathy.ai.domain.model

/**
 * 提示词系统错误类型
 *
 * 定义提示词系统可能发生的各种错误，支持错误恢复和用户友好提示
 */
sealed class PromptError : Exception() {

    /**
     * 验证错误
     *
     * @property message 错误消息
     * @property errorType 错误类型
     */
    data class ValidationError(
        override val message: String,
        val errorType: PromptValidationResult.ErrorType
    ) : PromptError()

    /**
     * 文件存储错误
     *
     * @property message 错误消息
     * @property cause 原始异常
     */
    data class StorageError(
        override val message: String,
        override val cause: Throwable? = null
    ) : PromptError()

    /**
     * JSON解析错误
     *
     * @property message 错误消息
     * @property jsonContent 导致错误的JSON内容（用于调试）
     */
    data class ParseError(
        override val message: String,
        val jsonContent: String? = null
    ) : PromptError()

    /**
     * 备份恢复错误
     *
     * @property message 错误消息
     */
    data class BackupError(
        override val message: String
    ) : PromptError()

    /**
     * 数据库错误
     *
     * @property message 错误消息
     * @property cause 原始异常
     */
    data class DatabaseError(
        override val message: String,
        override val cause: Throwable? = null
    ) : PromptError()

    /**
     * 获取用户友好的错误提示
     *
     * @return 适合显示给用户的错误消息
     */
    fun getUserMessage(): String = when (this) {
        is ValidationError -> message
        is StorageError -> "保存失败，请重试"
        is ParseError -> "配置文件损坏，已恢复默认设置"
        is BackupError -> "备份恢复失败"
        is DatabaseError -> "数据库操作失败，请重试"
    }

    /**
     * 判断错误是否可恢复
     *
     * @return true表示可以通过重试或恢复默认值解决
     */
    fun isRecoverable(): Boolean = when (this) {
        is ValidationError -> true
        is StorageError -> true
        is ParseError -> true  // 可以恢复到默认值
        is BackupError -> false
        is DatabaseError -> true
    }
}
