package com.empathy.ai.presentation.ui.screen.advisor.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.domain.model.AiAdvisorMessageBlock
import com.empathy.ai.domain.model.MessageBlockStatus
import com.empathy.ai.domain.model.MessageBlockType
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSPurple
import com.empathy.ai.presentation.theme.iOSTextPrimary

/**
 * 流式消息气泡组件
 *
 * 支持Block架构的AI消息展示：
 * - 实时文本显示（打字机效果）
 * - 思考过程折叠展示
 * - 流式状态指示（光标闪烁）
 * - iOS风格设计
 *
 * 业务背景 (FD-00028):
 * - 一条AI消息可能包含多个Block（思考+文本）
 * - 每个Block独立渲染，支持不同样式
 * - 流式更新时显示光标动画
 *
 * @param blocks 消息块列表
 * @param isStreaming 是否正在流式接收
 * @param onStopGeneration 停止生成回调
 * @param onRegenerate 重新生成回调
 * @param modifier 修饰符
 *
 * @see FD-00028 AI军师流式对话升级功能设计
 * @see AiAdvisorMessageBlock 消息块模型
 */
@Composable
fun StreamingMessageBubble(
    blocks: List<AiAdvisorMessageBlock>,
    isStreaming: Boolean,
    onStopGeneration: () -> Unit,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        // AI头像
        Surface(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            color = iOSPurple.copy(alpha = 0.15f)
        ) {
            Icon(
                imageVector = Icons.Outlined.Psychology,
                contentDescription = null,
                modifier = Modifier.padding(8.dp),
                tint = iOSPurple
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.widthIn(max = screenWidth * 0.75f)
        ) {
            // 渲染所有Block
            blocks.forEachIndexed { index, block ->
                when (block.type) {
                    MessageBlockType.THINKING -> {
                        ThinkingSection(
                            content = block.content,
                            isStreaming = block.status == MessageBlockStatus.STREAMING,
                            metadata = block.metadata
                        )
                        if (index < blocks.lastIndex) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    MessageBlockType.MAIN_TEXT -> {
                        MainTextBubble(
                            content = block.content,
                            isStreaming = block.status == MessageBlockStatus.STREAMING
                        )
                    }

                    MessageBlockType.ERROR -> {
                        ErrorSection(content = block.content)
                    }
                }
            }

            // 操作按钮区域
            Spacer(modifier = Modifier.height(8.dp))
            ActionButtons(
                isStreaming = isStreaming,
                onStopGeneration = onStopGeneration,
                onRegenerate = onRegenerate
            )
        }
    }
}

/**
 * 主文本气泡
 *
 * 显示AI回复的主要内容，流式时显示光标。
 */
@Composable
private fun MainTextBubble(
    content: String,
    isStreaming: Boolean
) {
    Surface(
        shape = RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = 4.dp,
            bottomEnd = 18.dp
        ),
        color = iOSCardBackground,
        shadowElevation = 1.dp
    ) {
        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            if (content.isEmpty() && isStreaming) {
                // 等待内容时显示占位
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "正在生成",
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        color = iOSTextPrimary.copy(alpha = 0.5f)
                    )
                    StreamingCursor()
                }
            } else {
                Row {
                    Text(
                        text = content,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        color = iOSTextPrimary
                    )
                    if (isStreaming) {
                        StreamingCursor()
                    }
                }
            }
        }
    }
}

/**
 * 操作按钮区域
 *
 * 根据流式状态显示不同的操作按钮：
 * - 流式中：显示停止生成按钮
 * - 完成后：显示重新生成按钮
 */
@Composable
private fun ActionButtons(
    isStreaming: Boolean,
    onStopGeneration: () -> Unit,
    onRegenerate: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        if (isStreaming) {
            StopGenerationButton(onClick = onStopGeneration)
        } else {
            RegenerateButton(onClick = onRegenerate)
        }
    }
}

/**
 * 简化版流式消息气泡
 *
 * 用于没有Block数据时的流式显示，直接使用累积的文本内容。
 *
 * @param content 累积的文本内容
 * @param thinkingContent 累积的思考内容
 * @param thinkingElapsedMs 思考耗时
 * @param isStreaming 是否正在流式接收
 * @param onStopGeneration 停止生成回调
 * @param onRegenerate 重新生成回调
 * @param modifier 修饰符
 */
@Composable
fun StreamingMessageBubbleSimple(
    content: String,
    thinkingContent: String,
    thinkingElapsedMs: Long,
    isStreaming: Boolean,
    onStopGeneration: () -> Unit,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        // AI头像
        Surface(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            color = iOSPurple.copy(alpha = 0.15f)
        ) {
            Icon(
                imageVector = Icons.Outlined.Psychology,
                contentDescription = null,
                modifier = Modifier.padding(8.dp),
                tint = iOSPurple
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.widthIn(max = screenWidth * 0.75f)
        ) {
            // 思考过程（如果有）
            if (thinkingContent.isNotEmpty()) {
                ThinkingSection(
                    content = thinkingContent,
                    isStreaming = isStreaming && content.isEmpty(),
                    metadata = com.empathy.ai.domain.model.BlockMetadata(
                        thinkingMs = thinkingElapsedMs
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 主文本
            MainTextBubble(
                content = content,
                isStreaming = isStreaming
            )

            // 操作按钮
            Spacer(modifier = Modifier.height(8.dp))
            ActionButtons(
                isStreaming = isStreaming,
                onStopGeneration = onStopGeneration,
                onRegenerate = onRegenerate
            )
        }
    }
}
