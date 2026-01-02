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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
 * iOS风格Token限制输入组件
 *
 * 用于设置AI模型的最大Token数限制
 *
 * 设计规格（TD-00025）:
 * - 数字输入框
 * - 快捷选项: 1024, 2048, 4096, 8192, 16384
 * - 输入验证（正整数）
 * - 边界保护
 *
 * @param value 当前Token限制值
 * @param onValueChange 值变化回调
 * @param modifier Modifier
 * @param enabled 是否启用
 * @param minValue 最小值
 * @param maxValue 最大值
 *
 * @see TD-00025 Phase 3: 高级选项UI实现
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TokenLimitInput(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minValue: Int = TOKEN_MIN,
    maxValue: Int = TOKEN_MAX
) {
    val dimensions = AdaptiveDimensions.current
    
    // 输入框文本状态
    var textValue by remember(value) { mutableStateOf(value.toString()) }
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
                text = "最大Token数",
                fontSize = dimensions.fontSizeSubtitle,
                fontWeight = FontWeight.Medium,
                color = iOSTextPrimary
            )
            
            // 当前值格式化显示
            Text(
                text = formatTokenCount(value),
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
                if (newValue != null && newValue in minValue..maxValue) {
                    isError = false
                    onValueChange(newValue)
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
                            text = "输入Token数量",
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
                text = "请输入 $minValue - $maxValue 之间的数值",
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
            QUICK_OPTIONS.forEach { option ->
                QuickOptionChip(
                    value = option,
                    isSelected = value == option,
                    enabled = enabled,
                    onClick = {
                        textValue = option.toString()
                        isError = false
                        onValueChange(option)
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(dimensions.spacingSmall))
        
        // 说明文字
        Text(
            text = "限制AI响应的最大长度，较大的值允许更长的回复但消耗更多Token",
            fontSize = dimensions.fontSizeCaption,
            color = iOSTextSecondary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 快捷选项Chip
 */
@Composable
private fun QuickOptionChip(
    value: Int,
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
            text = formatTokenCount(value),
            fontSize = dimensions.fontSizeCaption,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
    }
}

/**
 * 格式化Token数量显示
 *
 * @param count Token数量
 * @return 格式化后的字符串（如 "4K", "16K"）
 */
fun formatTokenCount(count: Int): String {
    return when {
        count >= 1000 -> "${count / 1000}K"
        else -> count.toString()
    }
}

// ============================================================
// 常量定义
// ============================================================

/** Token最小值 */
private const val TOKEN_MIN = 1

/** Token最大值 */
private const val TOKEN_MAX = 128000

/** 快捷选项列表 */
private val QUICK_OPTIONS = listOf(1024, 2048, 4096, 8192, 16384)

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "Token限制输入 - 默认值", showBackground = true)
@Composable
private fun TokenLimitInputDefaultPreview() {
    EmpathyTheme {
        Surface(color = iOSCardBackground) {
            var value by remember { mutableIntStateOf(4096) }
            TokenLimitInput(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Token限制输入 - 选中快捷选项", showBackground = true)
@Composable
private fun TokenLimitInputSelectedPreview() {
    EmpathyTheme {
        Surface(color = iOSCardBackground) {
            var value by remember { mutableIntStateOf(8192) }
            TokenLimitInput(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Token限制输入 - 禁用", showBackground = true)
@Composable
private fun TokenLimitInputDisabledPreview() {
    EmpathyTheme {
        Surface(color = iOSCardBackground) {
            TokenLimitInput(
                value = 4096,
                onValueChange = {},
                enabled = false,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
