package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
 * iOS风格表单输入组件
 *
 * 设计规格（BUG-00036 响应式字体适配）:
 * - 高度: 响应式（约44dp）
 * - 标签: 响应式宽度, 响应式字体（fontSizeTitle）
 * - 输入框: 右对齐, 响应式字体（fontSizeTitle）
 * - 分隔线: 从标签右侧开始
 * - 密码模式: 支持显示/隐藏切换
 *
 * BUG-00038 P2修复:
 * - 新增isUrl参数，URL类型使用更小的标签宽度和左对齐
 * - URL类型充分利用空间，避免斜杠换行
 *
 * @param label 标签文本
 * @param value 输入值
 * @param onValueChange 值变化回调
 * @param modifier Modifier
 * @param placeholder 占位符文本
 * @param isPassword 是否为密码模式
 * @param isUrl 是否为URL类型（BUG-00038 P2修复）
 * @param showDivider 是否显示分隔线
 * @param keyboardType 键盘类型
 * @param trailingIcon 尾部自定义图标
 *
 * @see TDD-00021 3.3节 IOSFormField组件规格
 */
@Composable
fun IOSFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isPassword: Boolean = false,
    isUrl: Boolean = false,
    showDivider: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    // 使用响应式尺寸
    val dimensions = AdaptiveDimensions.current
    
    var passwordVisible by remember { mutableStateOf(false) }
    val dividerColor = iOSSeparator
    
    // BUG-00038 P2修复：URL类型使用更小的标签宽度，给输入框更多空间
    val labelWidth = if (isUrl) {
        (48 * dimensions.fontScale).dp
    } else {
        (64 * dimensions.fontScale).dp
    }
    val dividerStartPadding = labelWidth + dimensions.spacingMedium
    
    // BUG-00038 P2修复：URL类型使用左对齐，普通类型使用右对齐
    val textAlignment = if (isUrl) TextAlign.Start else TextAlign.End

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensions.iosListItemHeight)
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
        // 标签（响应式宽度）- 使用响应式字体
        Text(
            text = label,
            fontSize = dimensions.fontSizeTitle,
            color = iOSTextPrimary,
            modifier = Modifier.width(labelWidth)
        )

        // 输入框 - 使用响应式字体
        // BUG-00038 P2修复：URL类型使用左对齐
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(
                fontSize = dimensions.fontSizeTitle,
                color = iOSTextPrimary,
                textAlign = textAlignment
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            cursorBrush = SolidColor(iOSBlue),
            decorationBox = { innerTextField ->
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = dimensions.fontSizeTitle,
                        color = iOSTextSecondary,
                        textAlign = textAlignment,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                innerTextField()
            }
        )

        // 密码显示/隐藏按钮
        if (isPassword) {
            IconButton(
                onClick = { passwordVisible = !passwordVisible },
                modifier = Modifier.padding(start = dimensions.spacingSmall)
            ) {
                Icon(
                    imageVector = if (passwordVisible) {
                        Icons.Default.VisibilityOff
                    } else {
                        Icons.Default.Visibility
                    },
                    contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                    tint = iOSTextSecondary
                )
            }
        } else if (trailingIcon != null) {
            trailingIcon()
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "表单输入 - 基础", showBackground = true)
@Composable
private fun IOSFormFieldBasicPreview() {
    EmpathyTheme {
        IOSFormField(
            label = "名称",
            value = "OpenAI",
            onValueChange = {},
            placeholder = "请输入名称"
        )
    }
}

@Preview(name = "表单输入 - 空值", showBackground = true)
@Composable
private fun IOSFormFieldEmptyPreview() {
    EmpathyTheme {
        IOSFormField(
            label = "端点",
            value = "",
            onValueChange = {},
            placeholder = "https://api.openai.com/v1"
        )
    }
}

@Preview(name = "表单输入 - 密码", showBackground = true)
@Composable
private fun IOSFormFieldPasswordPreview() {
    EmpathyTheme {
        IOSFormField(
            label = "密钥",
            value = "sk-xxxxxxxxxxxxx",
            onValueChange = {},
            placeholder = "请输入API密钥",
            isPassword = true
        )
    }
}

@Preview(name = "表单输入 - 无分隔线", showBackground = true)
@Composable
private fun IOSFormFieldNoDividerPreview() {
    EmpathyTheme {
        IOSFormField(
            label = "备注",
            value = "",
            onValueChange = {},
            placeholder = "可选",
            showDivider = false
        )
    }
}
