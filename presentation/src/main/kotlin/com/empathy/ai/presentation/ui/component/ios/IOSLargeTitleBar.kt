package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBackground
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSTextPrimary

/**
 * iOS风格大标题导航栏
 *
 * 设计规格（BUG-00036 响应式字体适配）:
 * - 大标题: 响应式字体（fontSizeLargeTitle）, Bold, 黑色
 * - 返回按钮: iOS蓝色, chevron_left图标
 * - 添加按钮: iOS蓝色, add图标
 * - 背景: iOSBackground (#F2F2F7)
 * - 导航栏高度: 响应式（约44dp）
 * - 大标题区域: 自适应高度
 *
 * @param title 大标题文本
 * @param onBackClick 返回按钮点击回调
 * @param modifier Modifier
 * @param onAddClick 添加按钮点击回调（可选）
 * @param searchQuery 搜索文本（可选）
 * @param onSearchQueryChange 搜索文本变化回调（可选）
 * @param searchPlaceholder 搜索占位符文本
 *
 * @see TDD-00021 3.1节 IOSLargeTitleBar组件规格
 */
@Composable
fun IOSLargeTitleBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAddClick: (() -> Unit)? = null,
    searchQuery: String = "",
    onSearchQueryChange: ((String) -> Unit)? = null,
    searchPlaceholder: String = "搜索"
) {
    // 使用响应式尺寸
    val dimensions = AdaptiveDimensions.current
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(iOSBackground)
    ) {
        // 导航栏（响应式高度）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions.iosNavigationBarHeight)
                .padding(horizontal = dimensions.spacingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮
            Box(
                modifier = Modifier
                    .size(dimensions.iosNavigationBarHeight)
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = iOSBlue,
                    modifier = Modifier.size(dimensions.iconSizeLarge - 8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 添加按钮（可选）
            if (onAddClick != null) {
                Box(
                    modifier = Modifier
                        .size(dimensions.iosNavigationBarHeight)
                        .clickable(onClick = onAddClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加",
                        tint = iOSBlue,
                        modifier = Modifier.size(dimensions.iconSizeLarge - 8.dp)
                    )
                }
            }
        }

        // 大标题区域 - 使用响应式字体
        Text(
            text = title,
            fontSize = dimensions.fontSizeLargeTitle,
            fontWeight = FontWeight.Bold,
            color = iOSTextPrimary,
            modifier = Modifier.padding(horizontal = dimensions.spacingMedium, vertical = dimensions.spacingSmall)
        )

        // 搜索栏（可选）
        if (onSearchQueryChange != null) {
            IOSSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = searchPlaceholder,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.spacingMedium)
                    .padding(bottom = dimensions.spacingSmall)
            )
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "大标题导航栏 - 基础", showBackground = true)
@Composable
private fun IOSLargeTitleBarBasicPreview() {
    EmpathyTheme {
        IOSLargeTitleBar(
            title = "AI 配置",
            onBackClick = {}
        )
    }
}

@Preview(name = "大标题导航栏 - 带添加按钮", showBackground = true)
@Composable
private fun IOSLargeTitleBarWithAddPreview() {
    EmpathyTheme {
        IOSLargeTitleBar(
            title = "AI 配置",
            onBackClick = {},
            onAddClick = {}
        )
    }
}

@Preview(name = "大标题导航栏 - 带搜索栏", showBackground = true)
@Composable
private fun IOSLargeTitleBarWithSearchPreview() {
    EmpathyTheme {
        IOSLargeTitleBar(
            title = "AI 配置",
            onBackClick = {},
            onAddClick = {},
            searchQuery = "",
            onSearchQueryChange = {},
            searchPlaceholder = "搜索服务商"
        )
    }
}

@Preview(name = "大标题导航栏 - 完整功能", showBackground = true)
@Composable
private fun IOSLargeTitleBarFullPreview() {
    EmpathyTheme {
        IOSLargeTitleBar(
            title = "AI 配置",
            onBackClick = {},
            onAddClick = {},
            searchQuery = "OpenAI",
            onSearchQueryChange = {},
            searchPlaceholder = "搜索服务商"
        )
    }
}
