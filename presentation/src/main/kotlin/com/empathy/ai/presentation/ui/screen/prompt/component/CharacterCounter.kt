package com.empathy.ai.presentation.ui.screen.prompt.component

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.empathy.ai.presentation.theme.BrandWarmOrange
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.ui.screen.prompt.PromptEditorUiState

/**
 * 字符计数器组件
 *
 * @param charCount 当前字符数
 * @param maxLength 最大字符数
 * @param isOverLimit 是否超出限制
 * @param isNearLimit 是否接近限制
 * @param modifier Modifier
 */
@Composable
fun CharacterCounter(
    charCount: Int,
    maxLength: Int,
    isOverLimit: Boolean,
    isNearLimit: Boolean,
    modifier: Modifier = Modifier
) {
    val color = remember(isOverLimit, isNearLimit) {
        when {
            isOverLimit -> Color.Red
            isNearLimit -> BrandWarmOrange
            else -> Color.Unspecified
        }
    }

    Row(modifier = modifier) {
        Text(
            text = "$charCount/$maxLength",
            style = MaterialTheme.typography.bodySmall,
            // BUG-00057修复：使用iOSTextSecondary替代透明度
            color = if (color == Color.Unspecified) {
                iOSTextSecondary
            } else {
                color
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CharacterCounterNormalPreview() {
    EmpathyTheme {
        CharacterCounter(
            charCount = 50,
            maxLength = PromptEditorUiState.MAX_PROMPT_LENGTH,
            isOverLimit = false,
            isNearLimit = false
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CharacterCounterNearLimitPreview() {
    EmpathyTheme {
        CharacterCounter(
            charCount = 850,
            maxLength = PromptEditorUiState.MAX_PROMPT_LENGTH,
            isOverLimit = false,
            isNearLimit = true
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CharacterCounterOverLimitPreview() {
    EmpathyTheme {
        CharacterCounter(
            charCount = 1050,
            maxLength = PromptEditorUiState.MAX_PROMPT_LENGTH,
            isOverLimit = true,
            isNearLimit = true
        )
    }
}
