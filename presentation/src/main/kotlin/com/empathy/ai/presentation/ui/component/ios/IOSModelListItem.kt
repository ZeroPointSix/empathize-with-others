package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSSeparator
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * iOS风格模型列表项
 *
 * 设计规格:
 * - 高度: 响应式（约52dp）
 * - 模型名: 17sp
 * - 默认标记: 蓝色背景标签
 * - 拖拽手柄: drag_handle图标, 灰色
 * - 分隔线: 从左侧padding开始
 *
 * BUG-00038 P3修复：添加拖拽排序支持
 * - 新增onMoveUp/onMoveDown回调用于上下移动
 * - 新增canMoveUp/canMoveDown参数控制按钮显示
 *
 * @param modelId 模型ID
 * @param displayName 显示名称（可选，为空时使用modelId）
 * @param isDefault 是否为默认模型
 * @param onClick 点击回调
 * @param modifier Modifier
 * @param showDivider 是否显示分隔线
 * @param showDragHandle 是否显示拖拽手柄
 * @param onMoveUp 上移回调（BUG-00038 P3修复）
 * @param onMoveDown 下移回调（BUG-00038 P3修复）
 * @param canMoveUp 是否可以上移（BUG-00038 P3修复）
 * @param canMoveDown 是否可以下移（BUG-00038 P3修复）
 *
 * @see TDD-00021 3.5节 IOSModelListItem组件规格
 */
@Composable
fun IOSModelListItem(
    modelId: String,
    isDefault: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    displayName: String = "",
    showDivider: Boolean = true,
    showDragHandle: Boolean = true,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    canMoveUp: Boolean = true,
    canMoveDown: Boolean = true
) {
    // 使用响应式尺寸
    val dimensions = AdaptiveDimensions.current
    
    val dividerColor = iOSSeparator
    val dividerStartPadding = dimensions.spacingMedium
    
    // 列表项高度 = iOS标准高度 + 小间距
    val itemHeight = dimensions.iosListItemHeight + dimensions.spacingSmall

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(itemHeight)
            .clickable(onClick = onClick)
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
        // 模型名称
        Text(
            text = displayName.ifBlank { modelId },
            fontSize = 17.sp,
            color = iOSTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // 默认标记
        if (isDefault) {
            Box(
                modifier = Modifier
                    .background(
                        color = iOSBlue,
                        shape = RoundedCornerShape(dimensions.spacingXSmall)
                    )
                    .padding(horizontal = dimensions.spacingSmall, vertical = dimensions.spacingXSmall)
            ) {
                Text(
                    text = "默认",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(dimensions.spacingMediumSmall))
        }

        // BUG-00038 P3修复：拖拽排序按钮
        if (showDragHandle && (onMoveUp != null || onMoveDown != null)) {
            // 上移按钮
            if (onMoveUp != null) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "上移",
                    tint = if (canMoveUp) iOSBlue else iOSTextSecondary.copy(alpha = 0.3f),
                    modifier = Modifier
                        .size(dimensions.iconSizeLarge - 4.dp)
                        .clickable(enabled = canMoveUp) { onMoveUp() }
                )
            }
            // 下移按钮
            if (onMoveDown != null) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "下移",
                    tint = if (canMoveDown) iOSBlue else iOSTextSecondary.copy(alpha = 0.3f),
                    modifier = Modifier
                        .size(dimensions.iconSizeLarge - 4.dp)
                        .clickable(enabled = canMoveDown) { onMoveDown() }
                )
            }
        } else if (showDragHandle) {
            // 原有的拖拽手柄图标（仅显示，无功能）
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "拖拽排序",
                tint = iOSTextSecondary,
                modifier = Modifier.size(dimensions.iconSizeLarge - 8.dp)
            )
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "模型列表项 - 默认模型", showBackground = true)
@Composable
private fun IOSModelListItemDefaultPreview() {
    EmpathyTheme {
        IOSModelListItem(
            modelId = "gpt-4",
            displayName = "GPT-4",
            isDefault = true,
            onClick = {}
        )
    }
}

@Preview(name = "模型列表项 - 普通模型", showBackground = true)
@Composable
private fun IOSModelListItemNormalPreview() {
    EmpathyTheme {
        IOSModelListItem(
            modelId = "gpt-3.5-turbo",
            displayName = "GPT-3.5 Turbo",
            isDefault = false,
            onClick = {}
        )
    }
}

@Preview(name = "模型列表项 - 无显示名称", showBackground = true)
@Composable
private fun IOSModelListItemNoDisplayNamePreview() {
    EmpathyTheme {
        IOSModelListItem(
            modelId = "deepseek-chat",
            displayName = "",
            isDefault = false,
            onClick = {}
        )
    }
}

@Preview(name = "模型列表项 - 无拖拽手柄", showBackground = true)
@Composable
private fun IOSModelListItemNoDragHandlePreview() {
    EmpathyTheme {
        IOSModelListItem(
            modelId = "claude-3",
            displayName = "Claude 3",
            isDefault = true,
            onClick = {},
            showDragHandle = false
        )
    }
}
