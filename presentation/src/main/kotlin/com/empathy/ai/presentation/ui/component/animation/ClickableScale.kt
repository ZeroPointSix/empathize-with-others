package com.empathy.ai.presentation.ui.component.animation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.empathy.ai.presentation.theme.AnimationSpec

/**
 * 点击缩放组件
 *
 * 为点击操作提供缩放反馈动画
 *
 * @param onClick 点击回调
 * @param modifier Modifier
 * @param enabled 是否启用
 * @param scaleOnPressed 按下时的缩放比例，默认 0.95f
 * @param content 内容
 */
@Composable
fun ClickableScale(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    scaleOnPressed: Float = 0.95f,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleOnPressed else 1f,
        animationSpec = tween(
            durationMillis = AnimationSpec.DurationFast,
            easing = AnimationSpec.EasingStandard
        ),
        label = "ScaleAnimation"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Disable default ripple if desired, or keep it? Task implies scale feedback. Usually used with ripple or instead of. I'll disable ripple to emphasize scale, or keep it. Let's keep ripple? No, usually scale replaces ripple or works with it. Let's keep ripple null to focus on scale as requested "press feedback". But standard buttons should have ripple. Let's leave ripple enabled? Actually clickable adds ripple by default if indication is not null. To disable, pass null.
                // Wait, if I wrap a Card or Button, they have their own click handling.
                // This component "Wraps" content to make it clickable and scale.
                // If the content itself handles click (like Button), then this wrapper might conflict.
                // But the task says "Wrap clickable cards/buttons".
                // If I wrap a Button, the Button handles click. The wrapper won't know it's pressed unless I intercept.
                // Actually, ClickableScale is usually a replacement for surface clickable.
                enabled = enabled,
                onClick = onClick
            ),
        content = content
    )
}
