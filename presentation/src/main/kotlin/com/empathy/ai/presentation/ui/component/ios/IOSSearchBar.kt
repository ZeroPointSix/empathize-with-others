package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.theme.iOSTextTertiary

/**
 * iOS风格搜索栏
 * 
 * 实现iOS原生搜索栏的视觉效果（BUG-00036 响应式字体适配）
 * - 响应式圆角
 * - 背景#F8F8FA
 * - 搜索图标+输入框+清除按钮
 * - 响应式字体（fontSizeTitle）
 * 
 * @param query 搜索关键词
 * @param onQueryChange 关键词变化回调
 * @param placeholder 占位文字
 * @param modifier 修饰符
 * 
 * @see FD-00020 画像库页搜索栏
 */
@Composable
fun IOSSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "搜索",
    modifier: Modifier = Modifier
) {
    // 使用响应式尺寸
    val dimensions = AdaptiveDimensions.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFF8F8FA),
                shape = RoundedCornerShape(dimensions.cornerRadiusSmall + 2.dp)
            )
            .padding(horizontal = dimensions.spacingMediumSmall, vertical = dimensions.spacingSmall + 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 搜索图标
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "搜索",
            tint = iOSTextSecondary,
            modifier = Modifier.size(dimensions.iconSizeSmall + 4.dp)
        )
        
        Spacer(modifier = Modifier.width(dimensions.spacingSmall))
        
        // 输入框 - 使用响应式字体
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = dimensions.fontSizeTitle,
                    color = iOSTextTertiary
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(
                    fontSize = dimensions.fontSizeTitle,
                    color = iOSTextPrimary
                ),
                singleLine = true,
                cursorBrush = SolidColor(iOSTextPrimary),
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // 清除按钮
        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(dimensions.iconSizeSmall + 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "清除",
                    tint = iOSTextSecondary,
                    modifier = Modifier.size(dimensions.iconSizeSmall)
                )
            }
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "空状态", showBackground = true)
@Composable
private fun IOSSearchBarEmptyPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            IOSSearchBar(
                query = "",
                onQueryChange = {},
                placeholder = "搜索标签或分类..."
            )
        }
    }
}

@Preview(name = "有内容", showBackground = true)
@Composable
private fun IOSSearchBarWithContentPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            IOSSearchBar(
                query = "兴趣爱好",
                onQueryChange = {},
                placeholder = "搜索标签或分类..."
            )
        }
    }
}
