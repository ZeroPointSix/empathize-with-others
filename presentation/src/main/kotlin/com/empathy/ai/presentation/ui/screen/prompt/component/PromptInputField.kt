package com.empathy.ai.presentation.ui.screen.prompt.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.BrandWarmGold
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import kotlinx.coroutines.launch

/**
 * 提示词输入框组件
 *
 * @param value 当前输入值
 * @param onValueChange 值变化回调
 * @param placeholder 占位符文本
 * @param modifier Modifier
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PromptInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val dimensions = AdaptiveDimensions.current

    // 自定义选中颜色
    val customTextSelectionColors = TextSelectionColors(
        handleColor = BrandWarmGold,
        backgroundColor = BrandWarmGold.copy(alpha = 0.3f)
    )

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        Box(modifier = modifier) {
            BasicTextField(
                value = value,
                onValueChange = { newText ->
                    onValueChange(newText)
                    // 光标跟随
                    coroutineScope.launch {
                        bringIntoViewRequester.bringIntoView()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 200.dp)
                    .bringIntoViewRequester(bringIntoViewRequester),
                // BUG-00057修复：使用iOSTextPrimary深灰色，行高1.5倍
                textStyle = TextStyle(
                    fontSize = dimensions.fontSizeSubtitle,  // 16sp
                    lineHeight = dimensions.fontSizeSubtitle * 1.5f,  // 24sp (1.5倍行高)
                    color = iOSTextPrimary  // 深灰色，对比度充足
                ),
                cursorBrush = SolidColor(BrandWarmGold),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            // BUG-00057修复：占位符使用iOSTextSecondary，不用透明度
                            Text(
                                text = placeholder,
                                fontSize = dimensions.fontSizeSubtitle,
                                lineHeight = dimensions.fontSizeSubtitle * 1.5f,
                                color = iOSTextSecondary  // 使用辅助文字颜色
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PromptInputFieldEmptyPreview() {
    EmpathyTheme {
        PromptInputField(
            value = "",
            onValueChange = {},
            placeholder = "例如：请帮我分析她这句话的潜台词..."
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PromptInputFieldWithTextPreview() {
    EmpathyTheme {
        PromptInputField(
            value = "分析时请特别注意对方的情绪变化，如果发现有负面情绪，请给出安抚建议。",
            onValueChange = {},
            placeholder = "例如：请帮我分析她这句话的潜台词..."
        )
    }
}
