package com.empathy.ai.presentation.ui.component.state

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.DataStatus
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.LocalSemanticColors

/**
 * 状态角标组件
 *
 * 用于显示数据源的处理状态
 *
 * 状态颜色：
 * - COMPLETED: 绿色 + 对勾
 * - PROCESSING: 橙色 + 沙漏
 * - FAILED: 红色 + 叉号
 * - NOT_AVAILABLE: 灰色 + 问号
 *
 * @param status 数据状态
 * @param size 角标大小
 * @param modifier Modifier
 */
@Composable
fun StatusBadge(
    status: DataStatus,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    val semanticColors = LocalSemanticColors.current
    val (backgroundColor, icon, contentDescription) = when (status) {
        DataStatus.COMPLETED -> Triple(
            semanticColors.success,
            Icons.Default.Check,
            "已完成"
        )
        DataStatus.PROCESSING -> Triple(
            semanticColors.warning,
            Icons.Default.HourglassEmpty,
            "处理中"
        )
        DataStatus.FAILED -> Triple(
            semanticColors.error,
            Icons.Default.Close,
            "失败"
        )
        DataStatus.NOT_AVAILABLE -> Triple(
            semanticColors.disabled,
            Icons.Default.QuestionMark,
            "不可用"
        )
    }
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier
                .size(size * 0.6f)
                .padding(2.dp)
        )
    }
}

// ========== 预览 ==========

@Preview(name = "已完成", showBackground = true)
@Composable
private fun PreviewStatusBadgeCompleted() {
    EmpathyTheme {
        StatusBadge(status = DataStatus.COMPLETED)
    }
}

@Preview(name = "处理中", showBackground = true)
@Composable
private fun PreviewStatusBadgeProcessing() {
    EmpathyTheme {
        StatusBadge(status = DataStatus.PROCESSING)
    }
}

@Preview(name = "失败", showBackground = true)
@Composable
private fun PreviewStatusBadgeFailed() {
    EmpathyTheme {
        StatusBadge(status = DataStatus.FAILED)
    }
}

@Preview(name = "不可用", showBackground = true)
@Composable
private fun PreviewStatusBadgeNotAvailable() {
    EmpathyTheme {
        StatusBadge(status = DataStatus.NOT_AVAILABLE)
    }
}
