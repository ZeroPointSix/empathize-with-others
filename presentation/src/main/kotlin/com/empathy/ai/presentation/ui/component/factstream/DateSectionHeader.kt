package com.empathy.ai.presentation.ui.component.factstream

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.EmpathyTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 日期分组标题组件
 * 
 * 设计规范：
 * - 在时间轴上增加日期标题（Section Headers）
 * - 居中的灰色小胶囊，写着"今天"或"12月26日"
 * - 减少视觉噪点，让阅读更流畅
 * 
 * @param timestamp 时间戳
 * @param modifier 修饰符
 */
@Composable
fun DateSectionHeader(
    timestamp: Long,
    modifier: Modifier = Modifier
) {
    val dateText = formatDateHeader(timestamp)
    
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE5E5EA))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = dateText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF636366)
            )
        }
    }
}

/**
 * 格式化日期标题
 * - 今天 -> "今天"
 * - 昨天 -> "昨天"
 * - 本周内 -> "周一"、"周二"等
 * - 其他 -> "12月26日"
 */
private fun formatDateHeader(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    val today = calendar.clone() as Calendar
    
    calendar.timeInMillis = timestamp
    
    // 重置时间部分，只比较日期
    today.set(Calendar.HOUR_OF_DAY, 0)
    today.set(Calendar.MINUTE, 0)
    today.set(Calendar.SECOND, 0)
    today.set(Calendar.MILLISECOND, 0)
    
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    
    val diffDays = ((today.timeInMillis - calendar.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()
    
    return when {
        diffDays == 0 -> "今天"
        diffDays == 1 -> "昨天"
        diffDays in 2..6 -> {
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            when (dayOfWeek) {
                Calendar.SUNDAY -> "周日"
                Calendar.MONDAY -> "周一"
                Calendar.TUESDAY -> "周二"
                Calendar.WEDNESDAY -> "周三"
                Calendar.THURSDAY -> "周四"
                Calendar.FRIDAY -> "周五"
                Calendar.SATURDAY -> "周六"
                else -> formatFullDate(timestamp)
            }
        }
        else -> formatFullDate(timestamp)
    }
}

/**
 * 格式化完整日期
 */
private fun formatFullDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("M月d日", Locale.CHINESE)
    return sdf.format(Date(timestamp))
}

/**
 * 格式化时间（仅时刻）
 */
fun formatTimeOnly(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * 判断两个时间戳是否属于同一天
 */
fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
    
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "今天", showBackground = true)
@Composable
private fun DateSectionHeaderTodayPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            DateSectionHeader(timestamp = System.currentTimeMillis())
        }
    }
}

@Preview(name = "昨天", showBackground = true)
@Composable
private fun DateSectionHeaderYesterdayPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            DateSectionHeader(timestamp = System.currentTimeMillis() - 24 * 60 * 60 * 1000)
        }
    }
}

@Preview(name = "更早的日期", showBackground = true)
@Composable
private fun DateSectionHeaderOlderPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            DateSectionHeader(timestamp = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000)
        }
    }
}
