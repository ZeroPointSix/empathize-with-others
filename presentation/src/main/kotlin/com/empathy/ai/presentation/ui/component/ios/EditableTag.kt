package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBackground
import com.empathy.ai.presentation.theme.iOSSeparator
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * 可编辑标签
 *
 * 设计规格:
 * - 背景: #F2F2F7
 * - 边框: #E5E5EA
 * - 圆角: 响应式
 * - 文字: 15sp
 * - 编辑图标: 响应式尺寸, 灰色
 *
 * @param text 标签文本
 * @param onClick 点击回调
 * @param modifier Modifier
 *
 * @see TDD-00021 3.9节 EditableTag组件规格
 */
@Composable
fun EditableTag(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 使用响应式尺寸
    val dimensions = AdaptiveDimensions.current
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(dimensions.cornerRadiusSmall))
            .background(iOSBackground)
            .border(
                width = 1.dp,
                color = iOSSeparator,
                shape = RoundedCornerShape(dimensions.cornerRadiusSmall)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = dimensions.spacingMediumSmall, vertical = dimensions.spacingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            color = iOSTextPrimary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "编辑",
            tint = iOSTextSecondary,
            modifier = Modifier.size(dimensions.iconSizeSmall)
        )
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "可编辑标签 - 短文本", showBackground = true)
@Composable
private fun EditableTagShortPreview() {
    EmpathyTheme {
        EditableTag(
            text = "外向",
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "可编辑标签 - 长文本", showBackground = true)
@Composable
private fun EditableTagLongPreview() {
    EmpathyTheme {
        EditableTag(
            text = "喜欢户外运动",
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "可编辑标签 - 多个", showBackground = true)
@Composable
private fun EditableTagMultiplePreview() {
    EmpathyTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            EditableTag(text = "外向", onClick = {})
            Spacer(modifier = Modifier.width(8.dp))
            EditableTag(text = "乐观", onClick = {})
            Spacer(modifier = Modifier.width(8.dp))
            EditableTag(text = "热情", onClick = {})
        }
    }
}
