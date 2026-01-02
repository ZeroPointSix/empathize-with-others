package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ProxyType
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSSeparator
import com.empathy.ai.presentation.theme.iOSTextPrimary

/**
 * iOS风格代理类型选择器
 *
 * 分段选择器样式，用于选择代理类型
 *
 * 设计规格（TD-00025）:
 * - iOS风格分段选择器
 * - 支持HTTP、HTTPS、SOCKS4、SOCKS5
 * - 选中项高亮显示
 *
 * @param selectedType 当前选中的代理类型
 * @param onTypeSelected 类型选择回调
 * @param modifier Modifier
 * @param enabled 是否启用
 *
 * @see TD-00025 Phase 5: 网络代理实现
 */
@Composable
fun ProxyTypePicker(
    selectedType: ProxyType,
    onTypeSelected: (ProxyType) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val dimensions = AdaptiveDimensions.current
    val types = listOf(ProxyType.HTTP, ProxyType.HTTPS, ProxyType.SOCKS4, ProxyType.SOCKS5)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensions.iosSegmentedControlHeight),
        color = iOSSeparator.copy(alpha = 0.3f),
        shape = RoundedCornerShape(dimensions.cornerRadiusSmall)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            types.forEach { type ->
                val isSelected = type == selectedType
                
                ProxyTypeSegment(
                    type = type,
                    isSelected = isSelected,
                    enabled = enabled,
                    onClick = { onTypeSelected(type) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 代理类型分段项
 */
@Composable
private fun ProxyTypeSegment(
    type: ProxyType,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    
    val backgroundColor = when {
        !enabled -> Color.Transparent
        isSelected -> Color.White
        else -> Color.Transparent
    }
    
    val textColor = when {
        !enabled -> iOSTextPrimary.copy(alpha = 0.5f)
        isSelected -> iOSBlue
        else -> iOSTextPrimary
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensions.cornerRadiusSmall - 2.dp))
            .background(backgroundColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = dimensions.spacingSmall),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = type.getDisplayName(),
            fontSize = dimensions.fontSizeCaption,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "代理类型选择器 - HTTP", showBackground = true)
@Composable
private fun ProxyTypePickerHttpPreview() {
    EmpathyTheme {
        Surface(color = iOSCardBackground) {
            ProxyTypePicker(
                selectedType = ProxyType.HTTP,
                onTypeSelected = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "代理类型选择器 - SOCKS5", showBackground = true)
@Composable
private fun ProxyTypePickerSocks5Preview() {
    EmpathyTheme {
        Surface(color = iOSCardBackground) {
            ProxyTypePicker(
                selectedType = ProxyType.SOCKS5,
                onTypeSelected = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "代理类型选择器 - 禁用", showBackground = true)
@Composable
private fun ProxyTypePickerDisabledPreview() {
    EmpathyTheme {
        Surface(color = iOSCardBackground) {
            ProxyTypePicker(
                selectedType = ProxyType.HTTP,
                onTypeSelected = {},
                enabled = false,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
