package com.empathy.ai.presentation.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.ui.graphics.vector.ImageVector
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

/**
 * 用户友好错误信息
 */
data class UserFriendlyError(
    val title: String,
    val message: String,
    val actionLabel: String = "重试",
    val icon: ImageVector
)

/**
 * 错误信息映射器
 *
 * 将技术异常转换为用户友好的错误提示
 */
object ErrorMessageMapper {
    fun map(throwable: Throwable?): UserFriendlyError {
        // 可以在这里处理特定领域的异常，例如 com.empathy.ai.domain.exception.NetworkException
        
        return when (throwable) {
            is UnknownHostException -> UserFriendlyError(
                title = "无法连接服务器",
                message = "请检查您的网络连接或稍后重试",
                actionLabel = "刷新",
                icon = Icons.Default.WifiOff
            )
            is SocketTimeoutException, is TimeoutException -> UserFriendlyError(
                title = "连接超时",
                message = "服务器响应时间过长，请稍后重试",
                actionLabel = "重试",
                icon = Icons.Default.CloudOff
            )
            is IOException -> UserFriendlyError(
                title = "网络错误",
                message = "网络通讯出现问题，请检查网络设置",
                actionLabel = "刷新",
                icon = Icons.Default.WifiOff
            )
            else -> UserFriendlyError(
                title = "发生错误",
                message = throwable?.localizedMessage ?: "遇到未知问题，请联系支持团队",
                actionLabel = "重试",
                icon = Icons.Default.ErrorOutline
            )
        }
    }
}
