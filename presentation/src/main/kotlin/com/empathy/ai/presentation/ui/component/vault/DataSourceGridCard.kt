package com.empathy.ai.presentation.ui.component.vault

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat


/**
 * 数据来源卡片组件 (Material Design 3 糖果色风格)
 * 
 * 设计原则:
 * - 高饱和度背景色 + 纯白图标 (Solid Color Container + White Icon)
 * - 圆角20dp，营造圆润的"鹅卵石"质感
 * - 数字加粗，成为绝对的视觉中心
 * - 按压缩放动画增强交互反馈
 * 
 * 技术要点:
 * - 使用animateFloatAsState实现按压缩放动画（0.98f）
 * - 图标+数量+标题+副标题布局
 * - 右上角状态角标（StatusBadge）
 * - 不可用状态禁用点击
 * 
 * @param icon 图标
 * @param title 标题
 * @param subtitle 副标题
 * @param count 数量
 * @param status 数据状态
 * @param iconBackgroundColor 图标背景色（高饱和度）
 * @param onClick 点击回调
 * @param modifier 修饰符
 * 
 * @see TDD-00020 6.2 DataSourceGridCard数据来源卡片
 */
@Composable
fun DataSourceGridCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    count: Int,
    status: DataStatus,
    iconBackgroundColor: Color = iOSBlue,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isEnabled = status != DataStatus.NOT_AVAILABLE
    
    // 按压缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isPressed && isEnabled) 0.98f else 1f,
        label = "scale"
    )
    
    Card(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isEnabled,
                onClick = onClick
            ),
        // 增大圆角到20dp，营造圆润的"鹅卵石"质感
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) iOSCardBackground else Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEnabled) 2.dp else 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 右上角状态角标
            StatusBadge(
                status = status,
                modifier = Modifier.align(Alignment.TopEnd)
            )
            
            Column {
                // 图标容器 - 始终使用高饱和度背景色 + 纯白图标
                // 即使数据不可用，也保持彩色图标以增强视觉吸引力
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = iconBackgroundColor,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        // 纯白图标，与高饱和度背景形成对比
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 数量 - 加粗字重，成为视觉中心
                Text(
                    text = count.toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = iOSTextPrimary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 标题
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSTextPrimary
                )
                
                // 副标题
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = iOSTextSecondary
                )
            }
        }
    }
}

@Preview(name = "数据来源卡片-已完成", showBackground = true)
@Composable
private fun DataSourceGridCardCompletedPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            DataSourceGridCard(
                icon = Icons.Default.Chat,
                title = "聊天记录",
                subtitle = "最近更新：今天",
                count = 256,
                status = DataStatus.COMPLETED,
                onClick = {}
            )
        }
    }
}

@Preview(name = "数据来源卡片-不可用", showBackground = true)
@Composable
private fun DataSourceGridCardNotAvailablePreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            DataSourceGridCard(
                icon = Icons.Default.Chat,
                title = "语音消息",
                subtitle = "暂不支持",
                count = 0,
                status = DataStatus.NOT_AVAILABLE,
                onClick = {}
            )
        }
    }
}
