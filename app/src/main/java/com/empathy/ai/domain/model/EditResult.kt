package com.empathy.ai.domain.model

/**
 * 编辑结果密封类
 *
 * 用于统一表示各种内容编辑操作的结果
 */
sealed class EditResult {
    /**
     * 编辑成功
     */
    object Success : EditResult()

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
    object NotFound : EditResult()

    /**
     * 无变化（内容相同）
     */
    object NoChanges : EditResult()

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
