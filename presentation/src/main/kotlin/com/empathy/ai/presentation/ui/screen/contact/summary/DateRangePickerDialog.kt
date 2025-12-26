package com.empathy.ai.presentation.ui.screen.contact.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.empathy.ai.domain.model.DateRange
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.viewmodel.QuickDateOption
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 现代化日期范围选择对话框
 *
 * 设计规范:
 * - 纯白背景 (#FFFFFF)，24dp圆角
 * - 无边框Chips设计，选中态使用品牌蓝色
 * - 日期输入框使用浅灰填充，无边框
 * - 实心胶囊确认按钮
 *
 * @param selectedOption 当前选中的快捷选项
 * @param selectedRange 当前选中的日期范围
 * @param existingSummaryDates 已有总结的日期列表
 * @param validationError 验证错误信息
 * @param onQuickOptionSelected 快捷选项选中回调
 * @param onCustomRangeSelected 自定义日期范围选中回调
 * @param onConfirm 确认回调
 * @param onDismiss 关闭回调
 */
@Composable
fun DateRangePickerDialog(
    selectedOption: QuickDateOption?,
    selectedRange: DateRange?,
    existingSummaryDates: List<String>,
    validationError: String?,
    onQuickOptionSelected: (QuickDateOption) -> Unit,
    onCustomRangeSelected: (String, String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // 标题
                Text(
                    text = "选择日期范围",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    ),
                    color = Color(0xFF1C1B1F)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 快捷选项
                ModernQuickDateOptions(
                    selectedOption = selectedOption,
                    onOptionSelected = onQuickOptionSelected
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 自定义日期
                ModernCustomDateRangeSection(
                    selectedRange = selectedRange,
                    onRangeChanged = onCustomRangeSelected
                )

                // 已有总结提示
                if (existingSummaryDates.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ModernExistingSummaryHint(dates = existingSummaryDates)
                }

                // 验证错误
                validationError?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = it,
                        color = Color(0xFFB3261E),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 底部按钮
                ModernDialogButtons(
                    confirmEnabled = selectedRange != null,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

/**
 * 现代化快捷日期选项组件
 *
 * 设计规范:
 * - 未选中: 浅灰填充(#F5F5F7)，无边框，中灰文字(#666666)
 * - 选中: 品牌蓝色填充，白色文字
 */
@Composable
private fun ModernQuickDateOptions(
    selectedOption: QuickDateOption?,
    onOptionSelected: (QuickDateOption) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "快捷选项",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color(0xFF666666)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickDateOption.entries.forEach { option ->
                ModernChip(
                    text = option.displayName,
                    selected = selectedOption == option,
                    onClick = { onOptionSelected(option) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 现代化Chip组件
 *
 * 设计规范:
 * - 无边框设计
 * - 未选中: #F5F5F7背景，#666666文字
 * - 选中: 品牌蓝色背景，白色文字
 */
@Composable
private fun ModernChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) iOSBlue else Color(0xFFF5F5F7)
    val textColor = if (selected) Color.White else Color(0xFF666666)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            ),
            color = textColor
        )
    }
}

/**
 * 现代化自定义日期范围区域
 *
 * 设计规范:
 * - 日期输入框: 浅灰填充(#F7F8FA)，无边框，圆角矩形
 * - 连接符"至": 浅灰色(#999999)
 */
@Composable
private fun ModernCustomDateRangeSection(
    selectedRange: DateRange?,
    onRangeChanged: (String, String) -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "自定义日期",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color(0xFF666666)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 开始日期
            ModernDateInput(
                text = selectedRange?.startDate ?: "开始日期",
                isPlaceholder = selectedRange?.startDate == null,
                onClick = { showStartPicker = true },
                modifier = Modifier.weight(1f)
            )

            // 连接符
            Text(
                text = "至",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF999999)
            )

            // 结束日期
            ModernDateInput(
                text = selectedRange?.endDate ?: "结束日期",
                isPlaceholder = selectedRange?.endDate == null,
                onClick = { showEndPicker = true },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // 开始日期选择器
    if (showStartPicker) {
        SingleDatePickerDialog(
            initialDate = selectedRange?.startDate,
            onDateSelected = { date ->
                selectedRange?.let {
                    onRangeChanged(date, it.endDate)
                } ?: onRangeChanged(date, date)
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
    }

    // 结束日期选择器
    if (showEndPicker) {
        SingleDatePickerDialog(
            initialDate = selectedRange?.endDate,
            onDateSelected = { date ->
                selectedRange?.let {
                    onRangeChanged(it.startDate, date)
                } ?: onRangeChanged(date, date)
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false }
        )
    }
}

/**
 * 现代化日期输入框
 *
 * 设计规范:
 * - 浅灰填充(#F7F8FA)，无边框
 * - 圆角矩形(12dp)
 */
@Composable
private fun ModernDateInput(
    text: String,
    isPlaceholder: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF7F8FA))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isPlaceholder) Color(0xFF999999) else Color(0xFF1C1B1F)
        )
    }
}

/**
 * 现代化底部按钮
 *
 * 设计规范:
 * - 确认按钮: 实心胶囊按钮，品牌蓝色背景，白色文字
 * - 取消按钮: 文字按钮，中灰色(#999999)
 */
@Composable
private fun ModernDialogButtons(
    confirmEnabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 取消按钮
        TextButton(onClick = onDismiss) {
            Text(
                text = "取消",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFF999999)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 确认按钮 - 实心胶囊
        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = iOSBlue,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFE0E0E0),
                disabledContentColor = Color(0xFF999999)
            ),
            modifier = Modifier.height(40.dp)
        ) {
            Text(
                text = "确认",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

/**
 * 现代化已有总结提示
 */
@Composable
private fun ModernExistingSummaryHint(dates: List<String>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF8E1))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = "⚠️ 已有总结的日期",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFFFF9800)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (dates.size <= 3) {
                    dates.joinToString(", ")
                } else {
                    "${dates.take(3).joinToString(", ")} 等${dates.size}天"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
        }
    }
}

/**
 * 现代化单日期选择器对话框
 *
 * 设计规范:
 * - 纯白背景 (#FFFFFF)，28dp大圆角
 * - 头部融合设计，无分割线
 * - 标题"选择日期"使用浅灰色(#999999)
 * - 大日期使用品牌蓝色，加粗
 * - 星期顶栏简化为"日一二三四五六"，极浅灰色(#B0B0B0)
 * - 选中态：品牌蓝色实心圆，直径适中
 * - 今日态：极浅蓝色背景填充(#E3F2FD)，无描边
 * - 底部按钮：实心胶囊确认按钮，灰色取消按钮
 */
@Composable
private fun SingleDatePickerDialog(
    initialDate: String?,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val displayFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")
    
    val initialLocalDate = initialDate?.let {
        LocalDate.parse(it, formatter)
    } ?: LocalDate.now()
    
    var selectedDate by remember { mutableStateOf(initialLocalDate) }
    var displayedMonth by remember { mutableStateOf(initialLocalDate.withDayOfMonth(1)) }
    
    val today = LocalDate.now()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // 头部区域 - 融合设计
                ModernDatePickerHeader(
                    selectedDate = selectedDate,
                    displayFormatter = displayFormatter
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 月份导航
                MonthNavigationRow(
                    displayedMonth = displayedMonth,
                    onPreviousMonth = { displayedMonth = displayedMonth.minusMonths(1) },
                    onNextMonth = { displayedMonth = displayedMonth.plusMonths(1) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 星期顶栏 - 简化设计
                WeekDayHeader()

                Spacer(modifier = Modifier.height(8.dp))

                // 日历网格
                CalendarGrid(
                    displayedMonth = displayedMonth,
                    selectedDate = selectedDate,
                    today = today,
                    onDateClick = { selectedDate = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 底部按钮 - 左右撑满布局
                ModernDatePickerButtons(
                    onConfirm = { onDateSelected(selectedDate.format(formatter)) },
                    onDismiss = onDismiss
                )
            }
        }
    }
}

/**
 * 现代化日期选择器头部
 */
@Composable
private fun ModernDatePickerHeader(
    selectedDate: LocalDate,
    displayFormatter: DateTimeFormatter
) {
    Column {
        // 小标题 - 浅灰色
        Text(
            text = "选择日期",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF999999)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 大日期 - 品牌蓝色，加粗
        Text(
            text = selectedDate.format(displayFormatter),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = iOSBlue
        )
    }
}

/**
 * 月份导航行
 */
@Composable
private fun MonthNavigationRow(
    displayedMonth: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthFormatter = DateTimeFormatter.ofPattern("yyyy年M月")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 上一月按钮
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onPreviousMonth)
                .padding(8.dp)
        ) {
            Text(
                text = "‹",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF666666)
            )
        }
        
        // 当前月份
        Text(
            text = displayedMonth.format(monthFormatter),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color(0xFF333333)
        )
        
        // 下一月按钮
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onNextMonth)
                .padding(8.dp)
        ) {
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF666666)
            )
        }
    }
}

/**
 * 星期顶栏 - 简化设计
 */
@Composable
private fun WeekDayHeader() {
    val weekDays = listOf("日", "一", "二", "三", "四", "五", "六")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekDays.forEach { day ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = Color(0xFFB0B0B0)
                )
            }
        }
    }
}

/**
 * 日历网格
 */
@Composable
private fun CalendarGrid(
    displayedMonth: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate,
    onDateClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = displayedMonth.withDayOfMonth(1)
    val lastDayOfMonth = displayedMonth.withDayOfMonth(displayedMonth.lengthOfMonth())
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 周日为0
    
    val daysInMonth = displayedMonth.lengthOfMonth()
    val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7
    
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        var cellIndex = 0
        while (cellIndex < totalCells) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(7) { dayOfWeek ->
                    val dayNumber = cellIndex - firstDayOfWeek + 1
                    val isValidDay = dayNumber in 1..daysInMonth
                    
                    if (isValidDay) {
                        val date = displayedMonth.withDayOfMonth(dayNumber)
                        val isSelected = date == selectedDate
                        val isToday = date == today
                        
                        CalendarDayCell(
                            day = dayNumber,
                            isSelected = isSelected,
                            isToday = isToday,
                            onClick = { onDateClick(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    cellIndex++
                }
            }
        }
    }
}

/**
 * 日历日期单元格
 *
 * 设计规范:
 * - 选中态：品牌蓝色实心圆，白色文字
 * - 今日态：极浅蓝色背景(#E3F2FD)，无描边，蓝色文字
 * - 普通态：无背景，深灰文字
 */
@Composable
private fun CalendarDayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> iOSBlue
        isToday -> Color(0xFFE3F2FD)
        else -> Color.Transparent
    }
    
    val textColor = when {
        isSelected -> Color.White
        isToday -> iOSBlue
        else -> Color(0xFF333333)
    }
    
    val fontWeight = when {
        isSelected || isToday -> FontWeight.Medium
        else -> FontWeight.Normal
    }

    Box(
        modifier = modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = fontWeight
            ),
            color = textColor
        )
    }
}

/**
 * 现代化日期选择器底部按钮
 *
 * 设计规范:
 * - 左右撑满布局
 * - 取消按钮：灰色文字，浅灰背景
 * - 确认按钮：品牌蓝色背景，白色文字，48dp高度胶囊形
 */
@Composable
private fun ModernDatePickerButtons(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 取消按钮 - 浅灰背景
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF5F5F7),
                contentColor = Color(0xFF666666)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp
            )
        ) {
            Text(
                text = "取消",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }
        
        // 确认按钮 - 品牌蓝色
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = iOSBlue,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "确认",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}
