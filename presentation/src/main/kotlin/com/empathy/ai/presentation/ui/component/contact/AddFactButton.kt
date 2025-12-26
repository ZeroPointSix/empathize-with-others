package com.empathy.ai.presentation.ui.component.contact

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSTextTertiary

/**
 * 添加事实按钮组件
 * 
 * 技术要点:
 * - 虚线边框按钮
 * - 显示"+"图标和"添加第一条事实"文字
 * - 使用iOSBlue作为图标和文字颜色
 * 
 * @param onClick 点击回调
 * @param text 按钮文字
 * @param modifier 修饰符
 * 
 * @see TDD-00020 7.4 AddFactButton添加事实按钮
 */
@Composable
fun AddFactButton(
    onClick: () -> Unit,
    text: String = "添加第一条事实",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // 虚线边框
        Canvas(
            modifier = Modifier
                .matchParentSize()
        ) {
            drawRoundRect(
                color = iOSTextTertiary,
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(12.dp.toPx()),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(8f, 8f),
                        0f
                    )
                )
            )
        }
        
        // 内容
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加",
                tint = iOSBlue,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = text,
                fontSize = 15.sp,
                color = iOSBlue
            )
        }
    }
}

@Preview(name = "添加事实按钮", showBackground = true)
@Composable
private fun AddFactButtonPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AddFactButton(onClick = {})
        }
    }
}

@Preview(name = "添加事实按钮-自定义文字", showBackground = true)
@Composable
private fun AddFactButtonCustomTextPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AddFactButton(
                onClick = {},
                text = "添加新记录"
            )
        }
    }
}
