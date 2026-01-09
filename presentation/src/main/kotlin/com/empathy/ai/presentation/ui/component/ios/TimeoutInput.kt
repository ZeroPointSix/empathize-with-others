package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSSeparator
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * iOS风格超时设置输入组件
 *
 * BUG-00054 第二轮修复：添加超时设置UI组件
 *
 * 用于设置AI请求的超时时间
 *
 * 设计规格:
 * - 数字输入框（显示秒数）
 * - 快捷选项: 15秒, 30秒, 60秒, 90秒, 120秒
 * - 输入验证（5-120秒）
 * - 边界保护
 *
 * @param valueMs 当前超时值（毫秒）
 * @param onValueChange 值变化回调（毫秒）
 * @param modifier Modifier
 * @param enabled 是否启用
 * @param minValueMs 最小值（毫秒）
 * @param maxValueMs 最大值（毫秒）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimeoutInput(
    valueMs: Long,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minValueMs: Long = TIMEOUT_MIN_MS,
    maxValueMs: Long = TIMEOUT_MAX_MS
) {
    val dimensions = AdaptiveDimensions.current
    
    // 转换为秒显示
    val valueSeconds = (valueMs / 1000).toInt()
    val minSeconds = (minValueMs / 1000).toInt()
    val maxSeconds = (maxValueMs / 1000).toInt()
    
    // 输入框文本状态
    var textValue by remember(valueSeconds) { mutableStateOf(valueSeconds.toString()) }
    var isError by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions.spacingMedium)
    ) {
        // 标题行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "请求超时",
                fontSize = dimensions.fontSizeSubtitle,
                fontWeight = FontWeight.Medium,
                color = iOSTextPrimary
            )
            
            // 当前值显示
            Text(
                text = "${valueSeconds}秒",
                fontSize = dimensions.fontSizeCaption,
                color = iOSTextSecondary
            )
        }
        
        Spacer(modifier = Modifier.height(dimensions.spacingSmall))
        
        // 输入框
        BasicTextField(
            value = textValue,
            onValueChange = { newText ->
                // 只允许数字输入
                val filtered = newText.filter { it.isDigit() }
                textValue = filtered
                
                // 验证并更新值
                val newValue = filtered.toIntOrNull()
                if (newValue != null && newValue in minSeconds..maxSeconds) {
                    isError = false
                    onValueChange(newValue * 1000L)
                } else if (filtered.isNotEmpty()) {
                    isError = true
                }
            },
            enabled = enabled,
            textStyle = TextStyle(
                fontSize = dimensions.fontSizeTitle,
                fontWeight = FontWeight.Medium,
                color = if (isError) Color(0xFFFF3B30) else iOSTextPrimary,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            cursorBrush = SolidColor(iOSBlue),
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions.iosButtonHeight)
                .clip(RoundedCornerShape(dimensions.cornerRadiusSmall))
                .background(if (enabled) Color.White else Color(0xFFF5F5F5))
                .border(
                    width = 1.dp,
                    color = when {
                        isError -> Color(0xFFFF3B30)
                        !enabled -> iOSSeparator
                        else -> iOSSeparator
                    },
                    shape = RoundedCornerShape(dimensions.cornerRadiusSmall)
                )
                .padding(horizontal = dimensions.spacingMedium),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (textValue.isEmpty()) {
                        Text(
                            text = "输入超时秒数",
                            fontSize = dimensions.fontSizeTitle,
                            color = iOSTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                    innerTextField()
                }
            }
        )
        
        // 错误提示
        if (isError) {
            Spacer(modifier = Modifier.height(dimensions.spacingXSmall))
            Text(
                text = "请输入 $minSeconds - $maxSeconds 秒之间的数值",
                fontSize = dimensions.fontSizeCaption,
                color = Color(0xFFFF3B30)
            )
        }
        
        Spacer(modifier = Modifier.height(dimensions.spacingMedium))
        
        // 快捷选项标签
        Text(
            text = "快捷选项",
            fontSize = dimensions.fontSizeCaption,
            color = iOSTextSecondary
        )
        
        Spacer(modifier = Modifier.height(dimensions.spacingSmall))
        
        // 快捷选项按钮
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
        ) {
            TIMEOUT_QUICK_OPTIONS.forEach { optionSeconds ->
                TimeoutQuickOptionChip(
                    valueSeconds = optionSeconds,
                    isSelected = valueSeconds == optionSeconds,
                    enabled = enabled,
                    onClick = {
                        android.util.Log.d("TimeoutInput", "Quick option clicked: ${optionSeconds}s")
                        textValue = optionSeconds.toString()
                        isError = false
                        onValueChange(optionSeconds * 1000L)
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(dimensions.spacingSmall))
        
        // 说明文字
        Text(
            text = "设置AI请求的最大等待时间，网络较慢时建议增加超时时间",
            fontSize = dimensions.fontSizeCaption,
            color = iOSTextSecondary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 超时快捷选项Chip
 */
@Composable
private fun TimeoutQuickOptionChip(
    valueSeconds: Int,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    
    val backgroundColor = when {
        !enabled -> Color(0xFFF5F5F5)
        isSelected -> iOSBlue
        else -> Color.White
    }
    
    val textColor = when {
        !enabled -> iOSTextSecondary
        isSelected -> Color.White
        else -> iOSTextPrimary
    }
    
    val borderColor = when {
        !enabled -> iOSSeparator
        isSelected -> iOSBlue
        else -> iOSSeparator
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(dimensions.cornerRadiusSmall))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(dimensions.cornerRadiusSmall)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = dimensions.spacingMedium,
                vertical = dimensions.spacingSmall
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${valueSeconds}秒",
            fontSize = dimensions.fontSizeCaption,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
    }
}

// ============================================================
// 常量定义
// ============================================================

/** 超时最小值（毫秒）- 5秒 */
private const val TIMEOUT_MIN_MS = 5000L

/** 超时最大值（毫秒）- 120秒 */
private const val TIMEOUT_MAX_MS = 120000L

/** 快捷选项列表（秒） */
private val TIMEOUT_QUICK_OPTIONS = listOf(15, 30, 60, 90, 120)

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "超时设置 - 默认值", showBackground = true)
@Composable
private fun TimeoutInputDefaultPreview() {
    EmpathyTheme {
        Surface(color = iOSCardBackground) {
            var value by remember { mutableLongStateOf(30000L) }
            TimeoutInput(
                valueMs = value,
                onValueChange = { value = it },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "超时设置 - 选中快捷选项", showBackground = true)
@Composable
private fun TimeoutInputSelectedPreview() {
    EmpathyTheme {
        Surface(color = iOSCardBackground) {
            var value by remember { mutableLongStateOf(60000L) }
            TimeoutInput(
                valueMs = value,
                onValueChange = { value = it },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "超时设置 - 禁用", showBackground = true)
@Composable
private fun TimeoutInputDisabledPreview() {
    EmpathyTheme {
        Surface(color = iOSCardBackground) {
            TimeoutInput(
                valueMs = 30000L,
                onValueChange = {},
                enabled = false,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
