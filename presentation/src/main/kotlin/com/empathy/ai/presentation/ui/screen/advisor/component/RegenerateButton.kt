package com.empathy.ai.presentation.ui.screen.advisor.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.iOSBlue

/**
 * 重新生成按钮
 *
 * 在AI响应完成后显示，允许用户重新生成最后一条回复。
 * 采用iOS风格设计，使用蓝色主题色。
 *
 * 业务背景 (FD-00028):
 * - 用户可能对AI回复不满意
 * - 提供重新生成功能，无需重新输入问题
 * - 删除当前AI回复，重新调用API
 *
 * @param onClick 点击回调
 * @param modifier 修饰符
 *
 * @see FD-00028 AI军师流式对话升级功能设计
 */
@Composable
fun RegenerateButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = iOSBlue
        )
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "重新生成",
            fontSize = 14.sp
        )
    }
}
