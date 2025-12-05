package com.empathy.ai.presentation.ui.component.button

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 主按钮组件
 *
 * 用于主要操作，如提交、确认等
 *
 * @param text 按钮文本
 * @param onClick 点击回调
 * @param modifier 修饰符
 * @param enabled 是否启用
 * @param loading 是否显示加载状态
 * @param icon 按钮图标
 * @param size 按钮大小
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    size: ButtonSize = ButtonSize.Medium
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !loading,
        contentPadding = size.contentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(size.iconSize),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(size.iconSize)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Text(
            text = text,
            style = size.textStyle()
        )
    }
}

/**
 * 按钮大小枚举
 */
enum class ButtonSize(
    val contentPadding: PaddingValues,
    val iconSize: Dp,
    val textStyle: @Composable () -> androidx.compose.ui.text.TextStyle
) {
    Small(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        iconSize = 16.dp,
        textStyle = { MaterialTheme.typography.labelMedium }
    ),
    Medium(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        iconSize = 18.dp,
        textStyle = { MaterialTheme.typography.labelLarge }
    ),
    Large(
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        iconSize = 20.dp,
        textStyle = { MaterialTheme.typography.titleMedium }
    )
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "默认状态", showBackground = true)
@Composable
private fun PrimaryButtonDefaultPreview() {
    EmpathyTheme {
        PrimaryButton(
            text = "确认",
            onClick = {}
        )
    }
}

@Preview(name = "带图标", showBackground = true)
@Composable
private fun PrimaryButtonWithIconPreview() {
    EmpathyTheme {
        PrimaryButton(
            text = "添加联系人",
            onClick = {},
            icon = Icons.Default.Add
        )
    }
}

@Preview(name = "加载状态", showBackground = true)
@Composable
private fun PrimaryButtonLoadingPreview() {
    EmpathyTheme {
        PrimaryButton(
            text = "保存中...",
            onClick = {},
            loading = true
        )
    }
}

@Preview(name = "禁用状态", showBackground = true)
@Composable
private fun PrimaryButtonDisabledPreview() {
    EmpathyTheme {
        PrimaryButton(
            text = "确认",
            onClick = {},
            enabled = false
        )
    }
}

@Preview(name = "小尺寸", showBackground = true)
@Composable
private fun PrimaryButtonSmallPreview() {
    EmpathyTheme {
        PrimaryButton(
            text = "保存",
            onClick = {},
            size = ButtonSize.Small,
            icon = Icons.Default.Check
        )
    }
}

@Preview(name = "大尺寸", showBackground = true)
@Composable
private fun PrimaryButtonLargePreview() {
    EmpathyTheme {
        PrimaryButton(
            text = "开始分析",
            onClick = {},
            size = ButtonSize.Large
        )
    }
}

@Preview(name = "深色模式", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrimaryButtonDarkPreview() {
    EmpathyTheme {
        PrimaryButton(
            text = "确认",
            onClick = {},
            icon = Icons.Default.Check
        )
    }
}
