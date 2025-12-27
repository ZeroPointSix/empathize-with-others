package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSLightGrayBackground
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * iOS风格导航栏
 * 
 * 实现iOS原生导航栏的视觉效果
 * - 高度响应式（约52dp）
 * - 左侧取消按钮（iOSBlue）
 * - 中间标题（17sp, SemiBold）
 * - 右侧完成按钮（iOSBlue，支持isDoneEnabled状态）
 * - 背景使用iOSLightGrayBackground.copy(alpha = 0.95f)
 * 
 * @param title 标题
 * @param onCancel 取消回调
 * @param onDone 完成回调
 * @param isDoneEnabled 完成按钮是否可用
 * @param modifier 修饰符
 * 
 * @see TDD-00020 7.1 IOSNavigationBar导航栏
 */
@Composable
fun IOSNavigationBar(
    title: String,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    isDoneEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    // 使用响应式尺寸
    val dimensions = AdaptiveDimensions.current
    
    // 导航栏高度 = iOS标准高度 + 小间距
    val navBarHeight = dimensions.iosNavigationBarHeight + dimensions.spacingSmall
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(navBarHeight)
            .background(iOSLightGrayBackground.copy(alpha = 0.95f))
            .padding(horizontal = dimensions.spacingSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 取消按钮
        TextButton(onClick = onCancel) {
            Text(
                text = "取消",
                fontSize = 17.sp,
                color = iOSBlue
            )
        }
        
        // 标题
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = iOSTextPrimary
        )
        
        // 完成按钮
        TextButton(
            onClick = onDone,
            enabled = isDoneEnabled
        ) {
            Text(
                text = "完成",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDoneEnabled) iOSBlue else iOSTextSecondary
            )
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "新建联系人-可用", showBackground = true)
@Composable
private fun IOSNavigationBarEnabledPreview() {
    EmpathyTheme {
        IOSNavigationBar(
            title = "新建联系人",
            onCancel = {},
            onDone = {},
            isDoneEnabled = true
        )
    }
}

@Preview(name = "新建联系人-禁用", showBackground = true)
@Composable
private fun IOSNavigationBarDisabledPreview() {
    EmpathyTheme {
        IOSNavigationBar(
            title = "新建联系人",
            onCancel = {},
            onDone = {},
            isDoneEnabled = false
        )
    }
}

@Preview(name = "编辑标签", showBackground = true)
@Composable
private fun IOSNavigationBarEditTagPreview() {
    EmpathyTheme {
        IOSNavigationBar(
            title = "编辑标签",
            onCancel = {},
            onDone = {},
            isDoneEnabled = true
        )
    }
}
