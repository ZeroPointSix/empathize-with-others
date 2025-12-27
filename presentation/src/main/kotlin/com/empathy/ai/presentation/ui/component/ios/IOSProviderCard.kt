package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.domain.model.AiModel
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSGreen
import com.empathy.ai.presentation.theme.iOSOrange
import com.empathy.ai.presentation.theme.iOSPurple
import com.empathy.ai.presentation.theme.iOSRed
import com.empathy.ai.presentation.theme.iOSSeparator
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import kotlin.math.roundToInt

/**
 * iOS风格服务商卡片（支持滑动操作）
 *
 * 设计规格:
 * - 图标容器: 40x40dp, 圆角10dp, 带背景色
 * - 标题: 17sp, SemiBold
 * - 描述: 15sp, 灰色
 * - 默认标记: 蓝色勾选图标
 * - 分隔线: 从图标右侧开始(68dp)
 * - 高度: 自适应（约72dp）
 * - 滑动操作: 向左滑动显示编辑/删除按钮
 *
 * @param provider AI服务商数据
 * @param isDefault 是否为默认服务商
 * @param onClick 点击回调（进入详情页）
 * @param onEdit 编辑回调
 * @param onDelete 删除回调
 * @param modifier Modifier
 * @param showDivider 是否显示分隔线
 * @param icon 自定义图标（可选）
 * @param iconBackgroundColor 图标背景色
 *
 * @see TDD-00021 3.2节 IOSProviderCard组件规格
 */
@Composable
fun IOSProviderCard(
    provider: AiProvider,
    isDefault: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    showDivider: Boolean = true,
    icon: ImageVector = Icons.Default.Cloud,
    iconBackgroundColor: Color = getProviderColor(provider.name)
) {
    val dividerColor = iOSSeparator
    val dividerStartPadding = 68.dp
    
    // Swipe state - 固定显示两个按钮的宽度（160px）
    val swipeThreshold = 160f // 两个按钮的总宽度
    var offsetX by remember { mutableFloatStateOf(0f) }
    var isSwipeOpen by remember { mutableStateOf(false) }
    
    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isSwipeOpen) -swipeThreshold else offsetX,
        animationSpec = tween(durationMillis = 200),
        label = "swipeOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        // Action buttons (behind the card)
        // 按钮顺序：从左到右是 删除 -> 编辑
        // 这样滑动时先露出编辑按钮（最右边），再露出删除按钮
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
        ) {
            // Delete button (左边)
            if (onDelete != null) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .fillMaxHeight()
                        .background(iOSRed)
                        .clickable {
                            isSwipeOpen = false
                            offsetX = 0f
                            onDelete()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "删除",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            // Edit button (右边，滑动时先露出)
            if (onEdit != null) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .fillMaxHeight()
                        .background(iOSOrange)
                        .clickable {
                            isSwipeOpen = false
                            offsetX = 0f
                            onEdit()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "编辑",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Main card content (swipeable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .background(Color.White)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // Snap to open or closed position
                            if (offsetX < -swipeThreshold / 2) {
                                isSwipeOpen = true
                            } else {
                                isSwipeOpen = false
                            }
                            offsetX = 0f
                        },
                        onDragCancel = {
                            offsetX = 0f
                            isSwipeOpen = false
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            if (!isSwipeOpen) {
                                // Only allow left swipe (negative values)
                                val newOffset = (offsetX + dragAmount).coerceIn(-swipeThreshold, 0f)
                                offsetX = newOffset
                            } else {
                                // When open, allow right swipe to close
                                if (dragAmount > 0) {
                                    isSwipeOpen = false
                                    offsetX = 0f
                                }
                            }
                        }
                    )
                }
                .clickable {
                    if (isSwipeOpen) {
                        isSwipeOpen = false
                        offsetX = 0f
                    } else {
                        onClick()
                    }
                }
                .drawBehind {
                    if (showDivider) {
                        val startX = dividerStartPadding.toPx()
                        drawLine(
                            color = dividerColor,
                            start = Offset(startX, size.height - 0.5.dp.toPx()),
                            end = Offset(size.width, size.height - 0.5.dp.toPx()),
                            strokeWidth = 0.5.dp.toPx()
                        )
                    }
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标容器 (40x40dp)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = iconBackgroundColor,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 标题和描述
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = provider.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = getProviderDescription(provider),
                    fontSize = 15.sp,
                    color = iOSTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 默认标记
            if (isDefault) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "默认",
                    tint = iOSBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * 根据服务商名称获取对应的颜色
 */
private fun getProviderColor(name: String): Color {
    return when {
        name.contains("OpenAI", ignoreCase = true) -> iOSGreen
        name.contains("DeepSeek", ignoreCase = true) -> iOSBlue
        name.contains("Claude", ignoreCase = true) -> iOSOrange
        name.contains("Gemini", ignoreCase = true) -> iOSBlue
        name.contains("通义", ignoreCase = true) -> iOSPurple
        name.contains("文心", ignoreCase = true) -> iOSBlue
        else -> iOSPurple
    }
}

/**
 * 获取服务商描述文本
 */
private fun getProviderDescription(provider: AiProvider): String {
    val modelCount = provider.models.size
    val defaultModel = provider.models.find { it.id == provider.defaultModelId }
    return if (defaultModel != null) {
        val displayName = defaultModel.displayName?.ifBlank { defaultModel.id } ?: defaultModel.id
        "$displayName · $modelCount 个模型"
    } else {
        "$modelCount 个模型"
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "服务商卡片 - 默认", showBackground = true)
@Composable
private fun IOSProviderCardDefaultPreview() {
    EmpathyTheme {
        IOSProviderCard(
            provider = AiProvider(
                id = "1",
                name = "OpenAI",
                baseUrl = "https://api.openai.com/v1",
                apiKey = "sk-xxx",
                models = listOf(
                    AiModel(id = "gpt-4", displayName = "GPT-4"),
                    AiModel(id = "gpt-3.5-turbo", displayName = "GPT-3.5 Turbo")
                ),
                defaultModelId = "gpt-4",
                isDefault = true
            ),
            isDefault = true,
            onClick = {},
            onEdit = {},
            onDelete = {}
        )
    }
}

@Preview(name = "服务商卡片 - 非默认", showBackground = true)
@Composable
private fun IOSProviderCardNonDefaultPreview() {
    EmpathyTheme {
        IOSProviderCard(
            provider = AiProvider(
                id = "2",
                name = "DeepSeek",
                baseUrl = "https://api.deepseek.com/v1",
                apiKey = "sk-xxx",
                models = listOf(
                    AiModel(id = "deepseek-chat", displayName = "DeepSeek Chat")
                ),
                defaultModelId = "deepseek-chat",
                isDefault = false
            ),
            isDefault = false,
            onClick = {},
            onEdit = {},
            onDelete = {}
        )
    }
}

@Preview(name = "服务商卡片 - 无分隔线", showBackground = true)
@Composable
private fun IOSProviderCardNoDividerPreview() {
    EmpathyTheme {
        IOSProviderCard(
            provider = AiProvider(
                id = "3",
                name = "Claude",
                baseUrl = "https://api.anthropic.com/v1",
                apiKey = "sk-xxx",
                models = listOf(
                    AiModel(id = "claude-3", displayName = "Claude 3")
                ),
                defaultModelId = "claude-3",
                isDefault = false
            ),
            isDefault = false,
            onClick = {},
            onEdit = {},
            onDelete = {},
            showDivider = false
        )
    }
}

@Preview(name = "服务商卡片列表", showBackground = true)
@Composable
private fun IOSProviderCardListPreview() {
    EmpathyTheme {
        Column(
            modifier = Modifier.background(Color.White)
        ) {
            IOSProviderCard(
                provider = AiProvider(
                    id = "1",
                    name = "OpenAI",
                    baseUrl = "https://api.openai.com/v1",
                    apiKey = "sk-xxx",
                    models = listOf(
                        AiModel(id = "gpt-4", displayName = "GPT-4"),
                        AiModel(id = "gpt-3.5-turbo", displayName = "GPT-3.5")
                    ),
                    defaultModelId = "gpt-4",
                    isDefault = true
                ),
                isDefault = true,
                onClick = {},
                onEdit = {},
                onDelete = {}
            )
            IOSProviderCard(
                provider = AiProvider(
                    id = "2",
                    name = "DeepSeek",
                    baseUrl = "https://api.deepseek.com/v1",
                    apiKey = "sk-xxx",
                    models = listOf(
                        AiModel(id = "deepseek-chat", displayName = "DeepSeek Chat")
                    ),
                    defaultModelId = "deepseek-chat",
                    isDefault = false
                ),
                isDefault = false,
                onClick = {},
                onEdit = {},
                onDelete = {},
                showDivider = false
            )
        }
    }
}
