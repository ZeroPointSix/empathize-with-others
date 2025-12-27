package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSGreen
import com.empathy.ai.presentation.theme.iOSRed
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * iOS风格测试连接按钮
 *
 * 设计规格:
 * - 高度: 44dp
 * - 文字: iOS蓝色, 17sp
 * - 图标: wifi_tethering
 * - 成功状态: iOS绿色 + check_circle
 * - 失败状态: iOS红色 + cancel
 * - 加载状态: 旋转sync图标
 *
 * @param isLoading 是否正在加载
 * @param isSuccess 测试结果（null=未测试, true=成功, false=失败）
 * @param onClick 点击回调
 * @param modifier Modifier
 *
 * @see TDD-00021 3.4节 IOSTestConnectionButton组件规格
 */
@Composable
fun IOSTestConnectionButton(
    isLoading: Boolean,
    isSuccess: Boolean?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 旋转动画
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        when {
            isLoading -> {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "测试中",
                    tint = iOSBlue,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
            }
            isSuccess == true -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "连接成功",
                    tint = iOSGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
            isSuccess == false -> {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "连接失败",
                    tint = iOSRed,
                    modifier = Modifier.size(22.dp)
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.WifiTethering,
                    contentDescription = "测试连接",
                    tint = iOSBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 文字
        Text(
            text = when {
                isLoading -> "测试连接中..."
                isSuccess == true -> "连接成功"
                isSuccess == false -> "连接失败"
                else -> "测试连接"
            },
            fontSize = 17.sp,
            color = when {
                isLoading -> iOSTextSecondary
                isSuccess == true -> iOSGreen
                isSuccess == false -> iOSRed
                else -> iOSBlue
            }
        )
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "测试连接 - 默认", showBackground = true)
@Composable
private fun IOSTestConnectionButtonDefaultPreview() {
    EmpathyTheme {
        IOSTestConnectionButton(
            isLoading = false,
            isSuccess = null,
            onClick = {}
        )
    }
}

@Preview(name = "测试连接 - 加载中", showBackground = true)
@Composable
private fun IOSTestConnectionButtonLoadingPreview() {
    EmpathyTheme {
        IOSTestConnectionButton(
            isLoading = true,
            isSuccess = null,
            onClick = {}
        )
    }
}

@Preview(name = "测试连接 - 成功", showBackground = true)
@Composable
private fun IOSTestConnectionButtonSuccessPreview() {
    EmpathyTheme {
        IOSTestConnectionButton(
            isLoading = false,
            isSuccess = true,
            onClick = {}
        )
    }
}

@Preview(name = "测试连接 - 失败", showBackground = true)
@Composable
private fun IOSTestConnectionButtonFailurePreview() {
    EmpathyTheme {
        IOSTestConnectionButton(
            isLoading = false,
            isSuccess = false,
            onClick = {}
        )
    }
}
