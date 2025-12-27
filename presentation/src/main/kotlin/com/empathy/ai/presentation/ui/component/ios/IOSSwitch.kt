package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.iOSGreen

/**
 * iOS风格开关组件
 *
 * 尺寸: 响应式（约51dp × 31dp）
 * 滑块: 响应式（约27dp）, 白色, 2dp阴影
 * 动画: spring动画实现滑块位置和轨道颜色变化
 *
 * @param checked 开关状态
 * @param onCheckedChange 状态变化回调
 * @param modifier Modifier
 * @param enabled 是否启用
 */
@Composable
fun IOSSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // 使用响应式尺寸
    val dimensions = AdaptiveDimensions.current
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // 响应式尺寸计算
    val switchWidth = dimensions.iosSwitchWidth
    val switchHeight = dimensions.iosSwitchHeight
    val thumbBaseSize = (27 * dimensions.fontScale).dp
    val thumbPressedSize = (29 * dimensions.fontScale).dp
    val thumbOffsetOn = (22 * dimensions.fontScale).dp
    val thumbOffsetOff = (2 * dimensions.fontScale).dp

    // 轨道颜色动画
    val trackColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color(0xFFE5E5EA)
            checked -> iOSGreen
            else -> Color(0xFFE5E5EA)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "trackColor"
    )

    // 滑块位置动画
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) thumbOffsetOn else thumbOffsetOff,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "thumbOffset"
    )

    // 按压时滑块缩放效果
    val thumbSize by animateDpAsState(
        targetValue = if (isPressed && enabled) thumbPressedSize else thumbBaseSize,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "thumbSize"
    )

    Box(
        modifier = modifier
            .size(width = switchWidth, height = switchHeight)
            .clip(RoundedCornerShape(switchHeight / 2))
            .background(trackColor)
            .semantics { role = Role.Switch }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        // 滑块
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .shadow(
                    elevation = dimensions.cardElevation,
                    shape = CircleShape,
                    clip = false
                )
                .clip(CircleShape)
                .background(
                    if (enabled) Color.White else Color(0xFFF5F5F5)
                )
        )
    }
}
