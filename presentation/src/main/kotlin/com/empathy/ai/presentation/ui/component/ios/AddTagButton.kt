package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * 添加标签按钮
 *
 * 设计规格:
 * - 边框: 虚线, #C7C7CC
 * - 圆角: 8dp
 * - 图标: add, 16dp
 * - 文字: 15sp, 灰色
 *
 * @param onClick 点击回调
 * @param modifier Modifier
 *
 * @see TDD-00021 3.9节 AddTagButton组件规格
 */
@Composable
fun AddTagButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = iOSTextSecondary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "添加",
            tint = iOSTextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "添加",
            fontSize = 15.sp,
            color = iOSTextSecondary
        )
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "添加标签按钮", showBackground = true)
@Composable
private fun AddTagButtonPreview() {
    EmpathyTheme {
        AddTagButton(
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "添加标签按钮 - 与标签组合", showBackground = true)
@Composable
private fun AddTagButtonWithTagsPreview() {
    EmpathyTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            EditableTag(text = "外向", onClick = {})
            Spacer(modifier = Modifier.width(8.dp))
            EditableTag(text = "乐观", onClick = {})
            Spacer(modifier = Modifier.width(8.dp))
            AddTagButton(onClick = {})
        }
    }
}
