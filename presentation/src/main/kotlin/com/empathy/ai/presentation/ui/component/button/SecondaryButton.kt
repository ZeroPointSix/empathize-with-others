package com.empathy.ai.presentation.ui.component.button

import android.content.res.Configuration
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.AnimationSpec
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 次要按钮组件
 *
 * 用于次要操作，如取消、删除等
 *
 * @param text 按钮文本
 * @param onClick 点击回调
 * @param modifier 修饰符
 * @param enabled 是否启用
 * @param icon 按钮图标
 * @param size 按钮大小
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    size: ButtonSize = ButtonSize.Medium
) {
    val dimensions = AdaptiveDimensions.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(
            durationMillis = AnimationSpec.DurationFast,
            easing = AnimationSpec.EasingStandard
        ),
        label = "ButtonScale"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        interactionSource = interactionSource,
        enabled = enabled,
        contentPadding = size.contentPadding,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(size.iconSize)
            )
            Spacer(modifier = Modifier.width(dimensions.spacingSmall))
        }
        
        Text(
            text = text,
            style = size.textStyle()
        )
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "默认状态", showBackground = true)
@Composable
private fun SecondaryButtonDefaultPreview() {
    EmpathyTheme {
        SecondaryButton(
            text = "取消",
            onClick = {}
        )
    }
}

@Preview(name = "带图标", showBackground = true)
@Composable
private fun SecondaryButtonWithIconPreview() {
    EmpathyTheme {
        SecondaryButton(
            text = "删除",
            onClick = {},
            icon = Icons.Default.Delete
        )
    }
}

@Preview(name = "禁用状态", showBackground = true)
@Composable
private fun SecondaryButtonDisabledPreview() {
    EmpathyTheme {
        SecondaryButton(
            text = "取消",
            onClick = {},
            enabled = false
        )
    }
}

@Preview(name = "小尺寸", showBackground = true)
@Composable
private fun SecondaryButtonSmallPreview() {
    EmpathyTheme {
        SecondaryButton(
            text = "关闭",
            onClick = {},
            size = ButtonSize.Small,
            icon = Icons.Default.Close
        )
    }
}

@Preview(name = "大尺寸", showBackground = true)
@Composable
private fun SecondaryButtonLargePreview() {
    EmpathyTheme {
        SecondaryButton(
            text = "取消操作",
            onClick = {},
            size = ButtonSize.Large
        )
    }
}

@Preview(name = "深色模式", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecondaryButtonDarkPreview() {
    EmpathyTheme {
        SecondaryButton(
            text = "取消",
            onClick = {},
            icon = Icons.Default.Close
        )
    }
}
