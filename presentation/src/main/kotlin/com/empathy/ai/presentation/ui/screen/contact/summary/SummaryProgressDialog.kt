package com.empathy.ai.presentation.ui.screen.contact.summary

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.domain.model.SummaryTask
import com.empathy.ai.domain.model.SummaryTaskStatus
import com.empathy.ai.presentation.ui.component.dialog.IOSInputDialog

/**
 * 总结进度对话框 - iOS风格
 *
 * 显示总结任务的执行进度，包括：
 * - 进度条和百分比
 * - 当前步骤描述
 * - 阶段图标动画
 * - 取消按钮（仅在可取消状态下显示）
 *
 * @param task 当前任务
 * @param onCancel 取消回调
 */
@Composable
fun SummaryProgressDialog(
    task: SummaryTask,
    onCancel: () -> Unit
) {
    IOSInputDialog(
        title = "正在生成总结",
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 阶段图标动画
                ProgressStageIndicator(status = task.status)

                // 进度条
                LinearProgressIndicator(
                    progress = { task.progress },
                    modifier = Modifier.fillMaxWidth()
                )

                // 进度百分比
                Text(
                    text = "${(task.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // 当前步骤
                Text(
                    text = task.currentStep.ifEmpty { getDefaultStepText(task.status) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmText = if (task.status.isCancellable()) "取消" else "确定",
        onConfirm = onCancel,
        onDismiss = { /* 不允许点击外部关闭 */ },
        showDismissButton = false
    )
}

/**
 * 进度阶段指示器
 *
 * 根据当前状态显示对应的图标，并添加呼吸动画效果
 */
@Composable
private fun ProgressStageIndicator(status: SummaryTaskStatus) {
    val icon = when (status) {
        SummaryTaskStatus.IDLE -> "⏳"
        SummaryTaskStatus.FETCHING_DATA -> "📥"
        SummaryTaskStatus.ANALYZING -> "🤖"
        SummaryTaskStatus.GENERATING -> "✍️"
        SummaryTaskStatus.SAVING -> "💾"
        SummaryTaskStatus.SUCCESS -> "✅"
        SummaryTaskStatus.FAILED -> "❌"
        SummaryTaskStatus.CANCELLED -> "🚫"
    }

    // 呼吸动画（仅在非终态时显示）
    val alpha = if (!status.isTerminal()) {
        val infiniteTransition = rememberInfiniteTransition(label = "progress_alpha")
        val animatedAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        animatedAlpha
    } else {
        1f
    }

    Text(
        text = icon,
        fontSize = 48.sp,
        modifier = Modifier
            .size(64.dp)
            .alpha(alpha)
    )
}

/**
 * 获取默认步骤文案
 */
private fun getDefaultStepText(status: SummaryTaskStatus): String {
    return when (status) {
        SummaryTaskStatus.IDLE -> "准备中..."
        SummaryTaskStatus.FETCHING_DATA -> "正在获取对话记录..."
        SummaryTaskStatus.ANALYZING -> "AI正在分析对话内容..."
        SummaryTaskStatus.GENERATING -> "正在生成总结..."
        SummaryTaskStatus.SAVING -> "正在保存结果..."
        SummaryTaskStatus.SUCCESS -> "完成"
        SummaryTaskStatus.FAILED -> "处理失败"
        SummaryTaskStatus.CANCELLED -> "已取消"
    }
}
