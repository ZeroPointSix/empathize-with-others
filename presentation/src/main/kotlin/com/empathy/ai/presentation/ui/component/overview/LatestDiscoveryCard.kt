package com.empathy.ai.presentation.ui.component.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.theme.iOSTextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * iOS通知风格最新发现卡片
 *
 * 设计原则：
 * 1. 标准iOS Table View Cell样式
 * 2. 左侧灯泡图标放在淡黄色圆形背景中
 * 3. 标题加粗，正文常规
 * 4. 时间戳放在右上角，颜色最浅
 * 5. 纯白底色，无灰色背景
 *
 * @param latestFact 最新的事实记录
 * @param onViewMore 查看更多点击回调
 * @param modifier 修饰符
 */
@Composable
fun LatestDiscoveryCard(
    latestFact: Fact?,
    modifier: Modifier = Modifier,
    onViewMore: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onViewMore != null) {
                    Modifier.clickable(onClick = onViewMore)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = iOSCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        if (latestFact == null) {
            // 空状态
            EmptyDiscoveryContent()
        } else {
            // 有数据状态
            DiscoveryContent(fact = latestFact)
        }
    }
}

/**
 * 有数据时的内容
 */
@Composable
private fun DiscoveryContent(
    fact: Fact,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 左侧图标 - 淡黄色圆形背景
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = Color(0xFFFFF8E1),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = Color(0xFFFFB300),
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 中间内容
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 标题
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "最新发现",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iOSTextPrimary
                    )
                    
                    // AI推测标识
                    if (fact.source == FactSource.AI_INFERRED) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "AI推测",
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF007AFF)
                        )
                    }
                }
                
                // 时间戳 - 右上角
                Text(
                    text = formatRelativeTime(fact.timestamp),
                    fontSize = 12.sp,
                    color = iOSTextTertiary
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 内容
            Text(
                text = fact.value,
                fontSize = 14.sp,
                color = iOSTextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * 空状态内容
 */
@Composable
private fun EmptyDiscoveryContent(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧图标 - 灰色圆形背景
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = Color(0xFFF2F2F7),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = iOSTextTertiary,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 内容
        Column {
            Text(
                text = "等待发现",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = iOSTextSecondary
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "开始聊天，AI会帮你记录重要信息",
                fontSize = 14.sp,
                color = iOSTextTertiary
            )
        }
    }
}

/**
 * 格式化相对时间
 */
private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "刚刚"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}分钟前"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}小时前"
        diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}天前"
        else -> {
            val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

// ==================== Previews ====================

@Preview(name = "有数据", showBackground = true)
@Composable
private fun LatestDiscoveryCardWithDataPreview() {
    EmpathyTheme {
        LatestDiscoveryCard(
            latestFact = Fact(
                key = "兴趣爱好",
                value = "喜欢周末去爬山，特别是秋天的时候",
                source = FactSource.AI_INFERRED,
                timestamp = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(36)
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "空状态", showBackground = true)
@Composable
private fun LatestDiscoveryCardEmptyPreview() {
    EmpathyTheme {
        LatestDiscoveryCard(
            latestFact = null,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "手动添加", showBackground = true)
@Composable
private fun LatestDiscoveryCardManualPreview() {
    EmpathyTheme {
        LatestDiscoveryCard(
            latestFact = Fact(
                key = "禁忌话题",
                value = "不要提起她的前任，这是敏感话题",
                source = FactSource.MANUAL,
                timestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
