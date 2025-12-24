package com.empathy.ai.presentation.ui.component.topic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization

/**
 * 主题输入框组件
 *
 * 提供主题内容输入功能，包含：
 * - 多行文本输入
 * - 实时字符计数
 * - 超出限制提示
 *
 * @param value 当前输入值
 * @param onValueChange 输入变化回调
 * @param charCount 当前字符数
 * @param maxLength 最大字符数
 * @param isError 是否显示错误状态
 * @param modifier Modifier
 */
@Composable
fun TopicInputField(
    value: String,
    onValueChange: (String) -> Unit,
    charCount: Int,
    maxLength: Int,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("主题描述") },
            placeholder = {
                Text(
                    text = "例如：讨论项目进度、学习编程知识、情感咨询...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            supportingText = {
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (isError) {
                        Text(
                            text = "已超出字符限制",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "$charCount / $maxLength",
                        color = if (isError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            },
            isError = isError,
            minLines = 3,
            maxLines = 6,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            )
        )
    }
}
