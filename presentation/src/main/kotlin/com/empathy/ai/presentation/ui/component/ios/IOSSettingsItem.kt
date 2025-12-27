package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.iOSSeparator
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * iOS风格设置项组件
 *
 * 高度: 响应式（约44dp）
 * 图标容器: 响应式（约28dp × 28dp）, 6dp圆角
 *
 * @param icon 图标
 * @param iconBackgroundColor 图标背景色
 * @param title 标题
 * @param subtitle 副标题（可选）
 * @param value 值文本（可选）
 * @param showArrow 是否显示箭头
 * @param showDivider 是否显示分隔线
 * @param trailing 尾部自定义内容（可选）
 * @param onClick 点击回调
 * @param modifier Modifier
 */
@Composable
fun IOSSettingsItem(
    icon: ImageVector,
    iconBackgroundColor: Color,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    showArrow: Boolean = true,
    showDivider: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    // 使用响应式尺寸
    val dimensions = AdaptiveDimensions.current
    
    val dividerColor = iOSSeparator
    // 分隔线起始位置 = padding(16) + iconSize(28) + spacing(12) ≈ 56dp
    val iconContainerSize = (28 * dimensions.fontScale).dp
    val dividerStartPadding = dimensions.spacingMedium + iconContainerSize + dimensions.spacingMediumSmall
    
    // 列表项高度 - 响应式
    val itemHeight = dimensions.iosListItemHeight
    val itemHeightWithSubtitle = dimensions.iosListItemHeight + dimensions.spacingMedium

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (subtitle != null) itemHeightWithSubtitle else itemHeight)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
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
                .size(iconContainerSize)
                .background(
                    color = iconBackgroundColor,
                    shape = RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(dimensions.iconSizeSmall + 2.dp)
            )
        }

        Spacer(modifier = Modifier.width(dimensions.spacingMediumSmall))

        // 标题和副标题
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = iOSTextPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = iOSTextSecondary
                )
            }
        }

        // 值文本或自定义尾部内容
        if (trailing != null) {
            trailing()
        } else if (value != null) {
            Text(
                text = value,
                fontSize = 17.sp,
                color = iOSTextSecondary
            )
            if (showArrow) {
                Spacer(modifier = Modifier.width(dimensions.spacingXSmall))
            }
        }

        // 箭头
        if (showArrow && trailing == null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = iOSTextSecondary,
                modifier = Modifier.size(dimensions.iconSizeSmall + 4.dp)
            )
        }
    }
}
