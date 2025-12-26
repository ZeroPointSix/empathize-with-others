package com.empathy.ai.presentation.ui.component.tag

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSTextTertiary

/**
 * 添加标签按钮
 * 
 * 虚线边框的添加按钮
 * - 使用iOSTextTertiary作为边框颜色
 * - 内部显示"+"图标和"添加"文字（iOSBlue）
 * 
 * @param onClick 点击回调
 * @param modifier 修饰符
 * @param text 按钮文字，默认"添加"
 * 
 * @see TDD-00020 5.1 CategoryCard中的AddTagButton
 */
@Composable
fun AddTagButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "添加"
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = iOSTextTertiary
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加",
                tint = iOSBlue,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                fontSize = 14.sp,
                color = iOSBlue
            )
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "默认", showBackground = true)
@Composable
private fun AddTagButtonPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AddTagButton(onClick = {})
        }
    }
}

@Preview(name = "自定义文字", showBackground = true)
@Composable
private fun AddTagButtonCustomTextPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AddTagButton(
                onClick = {},
                text = "添加标签"
            )
        }
    }
}
