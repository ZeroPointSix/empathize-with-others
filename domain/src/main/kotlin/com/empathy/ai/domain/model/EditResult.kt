package com.empathy.ai.domain.model

/**
 * 编辑结果密封类
 *
 * 用于统一表示各种内容编辑操作的结果，支持成功/失败/无变化等多种状态。
 *
 * 业务背景:
 * - 联系人信息、事实、总结等内容的编辑操作需要统一的结果处理
 * - 不同错误类型需要不同的用户提示
 *
 * 设计决策:
 * - 使用密封类确保类型安全和穷尽性检查
 * - 错误消息国际化支持（目前硬编码中文）
 * - 区分数据库错误和验证错误，便于问题排查
 */
sealed class EditResult {
    /**
     * 编辑成功
     */
    data object Success : EditResult()

    /**
     * 验证错误
     *
     * @property message 错误消息
     */
    data class ValidationError(val message: String) : EditResult()

    /**
     * 数据库错误
     *
     * @property exception 异常信息
     */
    data class DatabaseError(val exception: Exception) : EditResult()

    /**
     * 数据不存在
     */
    data object NotFound : EditResult()

    /**
     * 无变化（内容相同）
     */
    data object NoChanges : EditResult()

    /**
     * 是否成功
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * 获取错误消息
     *
     * @return 错误消息，成功或无变化时返回null
     */
    fun getErrorMessage(): String? = when (this) {
        is Success -> null
        is ValidationError -> message
        is DatabaseError -> "保存失败，请重试"
        is NotFound -> "内容已被删除"
        is NoChanges -> null
    }
}
