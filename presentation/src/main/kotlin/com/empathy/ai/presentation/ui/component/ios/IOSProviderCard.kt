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
import com.empathy.ai.presentation.theme.AdaptiveDimensions
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
 * 设计规格（BUG-00036 响应式字体适配）:
 * - 图标容器: 响应式尺寸, 圆角10dp, 带背景色
 * - 标题: 响应式字体（fontSizeTitle）, SemiBold
 * - 描述: 响应式字体（fontSizeBody）, 灰色
 * - 默认标记: 蓝色勾选图标
 * - 分隔线: 从图标右侧开始
 * - 高度: 响应式（约72dp）
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
    // 使用响应式尺寸
    val dimensions = AdaptiveDimensions.current
    val density = LocalDensity.current
    
    val dividerColor = iOSSeparator
    // 分隔线起始位置 = padding(16) + iconSize(40) + spacing(12)
    val dividerStartPadding = dimensions.spacingMedium + dimensions.iosIconContainerSize + dimensions.spacingMediumSmall
    
    // 滑动阈值 - 使用响应式尺寸
    val swipeThresholdDp = dimensions.swipeActionTotalWidth
    val swipeThreshold = with(density) { swipeThresholdDp.toPx() }
    val buttonWidthDp = dimensions.swipeActionButtonWidth
    
    // 列表项高度 - 响应式
    val itemHeight = dimensions.iosListItemHeight + dimensions.spacingLarge // 约72dp
    
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
            .height(itemHeight)
    ) {
        // Action buttons (behind the card)
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
        ) {
            // Delete button (左边)
            if (onDelete != null) {
                Box(
                    modifier = Modifier
                        .width(buttonWidthDp)
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
                            modifier = Modifier.size(dimensions.iconSizeMedium)
                        )
                        Text(
                            text = "删除",
                            color = Color.White,
                            fontSize = dimensions.fontSizeXSmall
                        )
                    }
                }
            }
            
            // Edit button (右边，滑动时先露出)
            if (onEdit != null) {
                Box(
                    modifier = Modifier
                        .width(buttonWidthDp)
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
                            modifier = Modifier.size(dimensions.iconSizeMedium)
                        )
                        Text(
                            text = "编辑",
                            color = Color.White,
                            fontSize = dimensions.fontSizeXSmall
                        )
                    }
                }
            }
        }

        // Main card content (swipeable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .background(Color.White)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
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
                                val newOffset = (offsetX + dragAmount).coerceIn(-swipeThreshold, 0f)
                                offsetX = newOffset
                            } else {
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
                .padding(horizontal = dimensions.spacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标容器 (响应式尺寸)
            Box(
                modifier = Modifier
                    .size(dimensions.iosIconContainerSize)
                    .background(
                        color = iconBackgroundColor,
                        shape = RoundedCornerShape(dimensions.cornerRadiusSmall)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(dimensions.iconSizeMedium)
                )
            }

            Spacer(modifier = Modifier.width(dimensions.spacingMediumSmall))

            // 标题和描述 - 使用响应式字体
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = provider.name,
                    fontSize = dimensions.fontSizeTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = getProviderDescription(provider),
                    fontSize = dimensions.fontSizeBody,
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
                    modifier = Modifier.size(dimensions.iconSizeMedium)
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
