package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * iOS风格Temperature滑块组件
 *
 * 用于调整AI模型的Temperature参数，控制输出的随机性
 *
 * 设计规格（TD-00025）:
 * - 范围: 0.0 - 2.0
 * - 步进: 0.1
 * - 实时显示当前值
 * - 显示"精确"和"创意"范围标签
 * - 边界保护（coerceIn）
 *
 * @param value 当前Temperature值
 * @param onValueChange 值变化回调
 * @param modifier Modifier
 * @param enabled 是否启用
 *
 * @see TD-00025 Phase 3: 高级选项UI实现
 */
@Composable
fun TemperatureSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val dimensions = AdaptiveDimensions.current
    
    // 边界保护
    val safeValue = value.coerceIn(TEMPERATURE_MIN, TEMPERATURE_MAX)
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions.spacingMedium)
    ) {
        // 标题行：标签 + 当前值
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Temperature",
                fontSize = dimensions.fontSizeSubtitle,
                fontWeight = FontWeight.Medium,
                color = iOSTextPrimary
            )
            
            // 当前值显示
            Text(
                text = String.format("%.1f", safeValue),
                fontSize = dimensions.fontSizeTitle,
                fontWeight = FontWeight.SemiBold,
                color = iOSBlue
            )
        }
        
        Spacer(modifier = Modifier.height(dimensions.spacingSmall))
        
        // 滑块
        Slider(
            value = safeValue,
            onValueChange = { newValue ->
                // 四舍五入到0.1
                val roundedValue = (newValue * 10).toInt() / 10f
                onValueChange(roundedValue.coerceIn(TEMPERATURE_MIN, TEMPERATURE_MAX))
            },
            valueRange = TEMPERATURE_MIN..TEMPERATURE_MAX,
            steps = 19, // (2.0 - 0.0) / 0.1 - 1 = 19 steps
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = iOSBlue,
                activeTrackColor = iOSBlue,
                inactiveTrackColor = Color(0xFFE5E5EA),
                disabledThumbColor = Color(0xFFBDBDBD),
                disabledActiveTrackColor = Color(0xFFBDBDBD),
                disabledInactiveTrackColor = Color(0xFFE5E5EA)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        // 范围标签
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 精确标签
            TemperatureRangeLabel(
                text = "精确",
                description = "0.0 - 0.5",
                isActive = safeValue <= 0.5f
            )
            
            // 平衡标签
            TemperatureRangeLabel(
                text = "平衡",
                description = "0.6 - 1.0",
                isActive = safeValue in 0.6f..1.0f
            )
            
            // 创意标签
            TemperatureRangeLabel(
                text = "创意",
                description = "1.1 - 2.0",
                isActive = safeValue > 1.0f
            )
        }
        
        Spacer(modifier = Modifier.height(dimensions.spacingSmall))
        
        // 说明文字
        Text(
            text = "较低的值使输出更确定和一致，较高的值使输出更多样和创意",
            fontSize = dimensions.fontSizeCaption,
            color = iOSTextSecondary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Temperature范围标签
 */
@Composable
private fun TemperatureRangeLabel(
    text: String,
    description: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            fontSize = dimensions.fontSizeCaption,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isActive) iOSBlue else iOSTextSecondary
        )
        Text(
            text = description,
            fontSize = dimensions.fontSizeXSmall,
            color = iOSTextSecondary
        )
    }
}

// ============================================================
// 常量定义
// ============================================================

/** Temperature最小值 */
private const val TEMPERATURE_MIN = 0.0f

/** Temperature最大值 */
private const val TEMPERATURE_MAX = 2.0f

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "Temperature滑块 - 默认值", showBackground = true)
@Composable
private fun TemperatureSliderDefaultPreview() {
    EmpathyTheme {
        Surface(color = iOSCardBackground) {
            var value by remember { mutableFloatStateOf(0.7f) }
            TemperatureSlider(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Temperature滑块 - 低值", showBackground = true)
@Composable
private fun TemperatureSliderLowPreview() {
    EmpathyTheme {
        Surface(color = iOSCardBackground) {
            var value by remember { mutableFloatStateOf(0.2f) }
            TemperatureSlider(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Temperature滑块 - 高值", showBackground = true)
@Composable
private fun TemperatureSliderHighPreview() {
    EmpathyTheme {
        Surface(color = iOSCardBackground) {
            var value by remember { mutableFloatStateOf(1.5f) }
            TemperatureSlider(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Temperature滑块 - 禁用", showBackground = true)
@Composable
private fun TemperatureSliderDisabledPreview() {
    EmpathyTheme {
        Surface(color = iOSCardBackground) {
            TemperatureSlider(
                value = 0.7f,
                onValueChange = {},
                enabled = false,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
