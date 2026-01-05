package com.empathy.ai.presentation.ui.screen.advisor.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.iOSRed

/**
 * 停止生成按钮
 *
 * 在流式响应过程中显示，允许用户中断AI响应。
 * 采用iOS风格设计，使用红色警示色。
 *
 * 业务背景 (FD-00028):
 * - 流式响应可能需要较长时间
 * - 用户可能想要中断当前响应，重新提问
 * - 提供明确的停止操作入口
 *
 * @param onClick 点击回调
 * @param modifier 修饰符
 *
 * @see FD-00028 AI军师流式对话升级功能设计
 */
@Composable
fun StopGenerationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = iOSRed
        ),
        border = BorderStroke(1.dp, iOSRed)
    ) {
        Icon(
            imageVector = Icons.Default.Stop,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "停止生成",
            fontSize = 14.sp
        )
    }
}
