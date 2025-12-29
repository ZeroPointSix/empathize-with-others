package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
 * - 高度响应式（约52dp）+ 状态栏安全区域
 * - 左侧取消按钮（iOSBlue）
 * - 中间标题（响应式字体, SemiBold）
 * - 右侧完成按钮（iOSBlue，支持isDoneEnabled状态）
 * - 背景使用iOSLightGrayBackground.copy(alpha = 0.95f)
 * 
 * BUG-00036 修复：
 * 1. 使用响应式字体尺寸，适配不同屏幕密度和系统字体设置
 * 2. 添加状态栏安全区域padding，确保按钮不会被状态栏遮挡
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
    
    // 导航栏内容高度 = 52dp（iOS标准）
    val navBarContentHeight = 52.dp
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(iOSLightGrayBackground.copy(alpha = 0.95f))
            .windowInsetsPadding(WindowInsets.statusBars)  // 添加状态栏安全区域
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(navBarContentHeight)
                .padding(horizontal = dimensions.spacingSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
        // 取消按钮 - BUG-00036: 使用响应式字体
        TextButton(onClick = onCancel) {
            Text(
                text = "取消",
                fontSize = dimensions.fontSizeTitle,  // 响应式字体
                color = iOSBlue
            )
        }
        
        // 标题 - BUG-00036: 使用响应式字体
        Text(
            text = title,
            fontSize = dimensions.fontSizeTitle,  // 响应式字体
            fontWeight = FontWeight.SemiBold,
            color = iOSTextPrimary
        )
        
        // 完成按钮 - BUG-00036: 使用响应式字体
        TextButton(
            onClick = onDone,
            enabled = isDoneEnabled
        ) {
            Text(
                text = "完成",
                fontSize = dimensions.fontSizeTitle,  // 响应式字体
                fontWeight = FontWeight.SemiBold,
                color = if (isDoneEnabled) iOSBlue else iOSTextSecondary
            )
        }
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
