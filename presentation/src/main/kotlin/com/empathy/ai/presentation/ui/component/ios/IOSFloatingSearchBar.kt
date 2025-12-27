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
import androidx.compose.ui.draw.shadow
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
 * iOS风格悬浮搜索栏
 * 
 * 优化设计：
 * - 纯白色圆角矩形背景
 * - 极淡的阴影，悬浮在浅灰色背景上
 * - 左侧灰色搜索图标
 * - 取消深灰色边框线
 * 
 * @param query 搜索关键词
 * @param onQueryChange 关键词变化回调
 * @param placeholder 占位文字
 * @param modifier 修饰符
 * 
 * @see 画像库页UI优化需求
 */
@Composable
fun IOSFloatingSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "搜索标签或分类",
    modifier: Modifier = Modifier
) {
    // 使用响应式尺寸
    val dimensions = AdaptiveDimensions.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = dimensions.cardElevation * 2,
                shape = RoundedCornerShape(dimensions.cornerRadiusMedium),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .background(
                color = Color.White,
                shape = RoundedCornerShape(dimensions.cornerRadiusMedium)
            )
            .padding(horizontal = dimensions.spacingMedium - 2.dp, vertical = dimensions.spacingMediumSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 搜索图标
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "搜索",
            tint = iOSTextTertiary,
            modifier = Modifier.size(dimensions.iconSizeSmall + 4.dp)
        )
        
        Spacer(modifier = Modifier.width(dimensions.spacingSmall + 2.dp))
        
        // 输入框
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 16.sp,
                    color = iOSTextTertiary
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(
                    fontSize = 16.sp,
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
                modifier = Modifier.size(dimensions.iconSizeLarge - 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "清除",
                    tint = iOSTextSecondary,
                    modifier = Modifier.size(dimensions.iconSizeSmall + 2.dp)
                )
            }
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "空状态", showBackground = true, backgroundColor = 0xFFF2F2F7)
@Composable
private fun IOSFloatingSearchBarEmptyPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            IOSFloatingSearchBar(
                query = "",
                onQueryChange = {},
                placeholder = "搜索标签或分类"
            )
        }
    }
}

@Preview(name = "有内容", showBackground = true, backgroundColor = 0xFFF2F2F7)
@Composable
private fun IOSFloatingSearchBarWithContentPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            IOSFloatingSearchBar(
                query = "兴趣爱好",
                onQueryChange = {},
                placeholder = "搜索标签或分类"
            )
        }
    }
}
