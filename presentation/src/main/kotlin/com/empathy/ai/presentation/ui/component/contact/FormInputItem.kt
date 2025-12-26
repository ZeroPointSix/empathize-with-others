package com.empathy.ai.presentation.ui.component.contact

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * 表单输入项组件
 * 
 * 技术要点:
 * - 标签（左侧，固定宽度）
 * - 输入框（右侧，flex）
 * 
 * @param label 标签文字
 * @param value 输入值
 * @param onValueChange 值变化回调
 * @param placeholder 占位符文字
 * @param modifier 修饰符
 * 
 * @see TDD-00020 7.3 ContactFormCard表单项
 */
@Composable
fun FormInputItem(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧标签
        Text(
            text = label,
            fontSize = 15.sp,
            color = iOSTextPrimary,
            modifier = Modifier.width(80.dp)
        )
        
        // 右侧输入框
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(
                fontSize = 15.sp,
                color = iOSTextPrimary
            ),
            cursorBrush = SolidColor(iOSBlue),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 15.sp,
                        color = iOSTextSecondary
                    )
                }
                innerTextField()
            }
        )
    }
}

@Preview(name = "表单输入项-有值", showBackground = true)
@Composable
private fun FormInputItemWithValuePreview() {
    EmpathyTheme {
        FormInputItem(
            label = "姓名",
            value = "张三",
            onValueChange = {},
            placeholder = "请输入姓名"
        )
    }
}

@Preview(name = "表单输入项-空值", showBackground = true)
@Composable
private fun FormInputItemEmptyPreview() {
    EmpathyTheme {
        FormInputItem(
            label = "备注名",
            value = "",
            onValueChange = {},
            placeholder = "请输入备注名"
        )
    }
}
