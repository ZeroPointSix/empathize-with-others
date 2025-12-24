package com.empathy.ai.presentation.ui.component.input

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 自定义文本输入框组件
 *
 * 支持标签、占位符、错误状态、前缀/后缀图标等功能
 *
 * @param value 输入框的值
 * @param onValueChange 值变化回调
 * @param modifier 修饰符
 * @param label 标签文字
 * @param placeholder 占位符文字
 * @param isError 是否显示错误状态
 * @param errorMessage 错误提示消息
 * @param leadingIcon 前缀图标
 * @param trailingIcon 后缀图标
 * @param singleLine 是否单行输入
 * @param maxLines 最大行数
 * @param keyboardType 键盘类型
 * @param imeAction IME动作
 * @param keyboardActions 键盘动作
 * @param enabled 是否启用
 */
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = if (label != null) {
                { Text(label) }
            } else null,
            placeholder = if (placeholder != null) {
                { Text(placeholder) }
            } else null,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = isError,
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = keyboardActions,
            enabled = enabled,
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            )
        )
        
        // 错误提示
        if (isError && errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "基本输入框", showBackground = true)
@Composable
private fun CustomTextFieldBasicPreview() {
    var text by remember { mutableStateOf("") }
    EmpathyTheme {
        CustomTextField(
            value = text,
            onValueChange = { text = it },
            label = "姓名",
            placeholder = "请输入姓名"
        )
    }
}

@Preview(name = "带前缀图标", showBackground = true)
@Composable
private fun CustomTextFieldWithLeadingIconPreview() {
    var text by remember { mutableStateOf("张三") }
    EmpathyTheme {
        CustomTextField(
            value = text,
            onValueChange = { text = it },
            label = "联系人姓名",
            placeholder = "请输入姓名",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "姓名图标"
                )
            }
        )
    }
}

@Preview(name = "带清除按钮", showBackground = true)
@Composable
private fun CustomTextFieldWithClearButtonPreview() {
    var text by remember { mutableStateOf("这是一段文本") }
    EmpathyTheme {
        CustomTextField(
            value = text,
            onValueChange = { text = it },
            label = "输入内容",
            placeholder = "请输入内容",
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(onClick = { text = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "清除"
                        )
                    }
                }
            }
        )
    }
}

@Preview(name = "错误状态", showBackground = true)
@Composable
private fun CustomTextFieldErrorPreview() {
    var text by remember { mutableStateOf("") }
    EmpathyTheme {
        CustomTextField(
            value = text,
            onValueChange = { text = it },
            label = "姓名",
            placeholder = "请输入姓名",
            isError = true,
            errorMessage = "姓名不能为空"
        )
    }
}

@Preview(name = "多行输入", showBackground = true)
@Composable
private fun CustomTextFieldMultilinePreview() {
    var text by remember { mutableStateOf("这是一段\n多行文本\n示例") }
    EmpathyTheme {
        CustomTextField(
            value = text,
            onValueChange = { text = it },
            label = "备注",
            placeholder = "请输入备注信息",
            singleLine = false,
            maxLines = 5
        )
    }
}

@Preview(name = "禁用状态", showBackground = true)
@Composable
private fun CustomTextFieldDisabledPreview() {
    EmpathyTheme {
        CustomTextField(
            value = "不可编辑的内容",
            onValueChange = {},
            label = "只读字段",
            enabled = false
        )
    }
}

@Preview(name = "深色模式", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CustomTextFieldDarkPreview() {
    var text by remember { mutableStateOf("张三") }
    EmpathyTheme {
        CustomTextField(
            value = text,
            onValueChange = { text = it },
            label = "姓名",
            placeholder = "请输入姓名",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "姓名图标"
                )
            }
        )
    }
}
