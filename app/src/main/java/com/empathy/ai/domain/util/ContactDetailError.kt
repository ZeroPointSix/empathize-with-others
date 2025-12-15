package com.empathy.ai.domain.util

/**
 * 联系人详情页错误类型
 *
 * 提供细粒度的错误分类和用户友好的错误消息
 *
 * 参考标准：
 * - [CR-00010] 错误处理细化建议
 * - [SD-00001] 代码规范和编码标准
 *
 * 使用示例：
 * ```kotlin
 * when (error) {
 *     is ContactDetailError.NetworkError -> showNetworkErrorDialog()
 *     is ContactDetailError.DataLoadError -> showRetryButton()
 *     is ContactDetailError.TagOperationError -> showTagErrorToast()
 *     is ContactDetailError.ValidationError -> showValidationError(error.field)
 * }
 * ```
 */
sealed class ContactDetailError : Exception() {

    /**
     * 网络错误
     * 当网络连接失败或超时时抛出
     */
    data class NetworkError(
        val originalCause: Throwable? = null,
        val isTimeout: Boolean = false
    ) : ContactDetailError() {
        override val message: String
            get() = if (isTimeout) "网络请求超时，请检查网络连接" else "网络连接失败，请检查网络设置"
        override val cause: Throwable?
            get() = originalCause
    }

    /**
     * 数据加载错误
     * 当从数据库或远程加载数据失败时抛出
     */
    data class DataLoadError(
        val dataType: DataType,
        val originalCause: Throwable? = null
    ) : ContactDetailError() {
        override val message: String
            get() = when (dataType) {
                DataType.CONTACT -> "联系人信息加载失败"
                DataType.TIMELINE -> "时间线数据加载失败"
                DataType.TAGS -> "标签数据加载失败"
                DataType.FACTS -> "事实数据加载失败"
                DataType.SUMMARY -> "AI总结加载失败"
            }
        override val cause: Throwable?
            get() = originalCause

        enum class DataType {
            CONTACT, TIMELINE, TAGS, FACTS, SUMMARY
        }
    }

    /**
     * 标签操作错误
     * 当标签确认、驳回或删除操作失败时抛出
     */
    data class TagOperationError(
        val operation: Operation,
        val tagId: Long,
        val originalCause: Throwable? = null
    ) : ContactDetailError() {
        override val message: String
            get() = when (operation) {
                Operation.CONFIRM -> "标签确认失败，请重试"
                Operation.REJECT -> "标签驳回失败，请重试"
                Operation.DELETE -> "标签删除失败，请重试"
                Operation.ADD -> "标签添加失败，请重试"
            }
        override val cause: Throwable?
            get() = originalCause

        enum class Operation {
            CONFIRM, REJECT, DELETE, ADD
        }
    }

    /**
     * 输入验证错误
     * 当用户输入不符合要求时抛出
     */
    class ValidationError(
        val field: String,
        val reason: ValidationReason
    ) : ContactDetailError() {
        override val message: String = when (reason) {
            ValidationReason.EMPTY -> "$field 不能为空"
            ValidationReason.TOO_LONG -> "$field 超过最大长度限制"
            ValidationReason.INVALID_FORMAT -> "$field 格式不正确"
            ValidationReason.DUPLICATE -> "$field 已存在"
        }

        enum class ValidationReason {
            EMPTY, TOO_LONG, INVALID_FORMAT, DUPLICATE
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ValidationError) return false
            if (field != other.field) return false
            if (reason != other.reason) return false
            return true
        }

        override fun hashCode(): Int {
            var result = field.hashCode()
            result = 31 * result + reason.hashCode()
            return result
        }

        override fun toString(): String {
            return "ValidationError(field='$field', reason=$reason)"
        }
    }

    /**
     * 权限错误
     * 当缺少必要权限时抛出
     */
    data class PermissionError(
        val permission: PermissionType
    ) : ContactDetailError() {
        override val message: String
            get() = when (permission) {
                PermissionType.STORAGE -> "需要存储权限才能访问照片"
                PermissionType.CONTACTS -> "需要联系人权限才能同步数据"
                PermissionType.OVERLAY -> "需要悬浮窗权限才能使用此功能"
            }

        enum class PermissionType {
            STORAGE, CONTACTS, OVERLAY
        }
    }

    /**
     * 数据库错误
     * 当数据库操作失败时抛出
     */
    data class DatabaseError(
        val operation: String,
        val originalCause: Throwable? = null
    ) : ContactDetailError() {
        override val message: String
            get() = "数据库操作失败：$operation"
        override val cause: Throwable?
            get() = originalCause
    }

    /**
     * 未知错误
     * 当发生未预期的错误时抛出
     */
    data class UnknownError(
        val originalCause: Throwable? = null
    ) : ContactDetailError() {
        override val message: String
            get() = "发生未知错误，请稍后重试"
        override val cause: Throwable?
            get() = originalCause
    }

    /**
     * 获取用户友好的错误消息
     */
    fun getUserMessage(): String = message ?: "发生错误，请稍后重试"

    /**
     * 获取错误代码（用于日志和分析）
     */
    fun getErrorCode(): String = when (this) {
        is NetworkError -> "ERR_NETWORK"
        is DataLoadError -> "ERR_DATA_LOAD_${dataType.name}"
        is TagOperationError -> "ERR_TAG_${operation.name}"
        is ValidationError -> "ERR_VALIDATION_${reason.name}"
        is PermissionError -> "ERR_PERMISSION_${permission.name}"
        is DatabaseError -> "ERR_DATABASE"
        is UnknownError -> "ERR_UNKNOWN"
    }

    /**
     * 是否可重试
     */
    fun isRetryable(): Boolean = when (this) {
        is NetworkError -> true
        is DataLoadError -> true
        is TagOperationError -> true
        is ValidationError -> false
        is PermissionError -> false
        is DatabaseError -> true
        is UnknownError -> true
    }

    companion object {
        /**
         * 从通用异常创建ContactDetailError
         */
        fun fromException(e: Throwable): ContactDetailError = when (e) {
            is ContactDetailError -> e
            is java.net.SocketTimeoutException -> NetworkError(e, isTimeout = true)
            is java.net.UnknownHostException -> NetworkError(e)
            is java.io.IOException -> NetworkError(e)
            is android.database.sqlite.SQLiteException -> DatabaseError("SQLite操作", e)
            else -> UnknownError(e)
        }
    }
}
