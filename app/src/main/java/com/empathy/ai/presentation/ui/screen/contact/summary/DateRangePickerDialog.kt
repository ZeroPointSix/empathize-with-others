package com.empathy.ai.presentation.ui.screen.contact.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.DateRange
import com.empathy.ai.presentation.viewmodel.QuickDateOption
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 日期范围选择对话框
 *
 * 包含快捷选项和自定义日期选择功能
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择日期范围") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 快捷选项
                QuickDateOptions(
                    selectedOption = selectedOption,
                    onOptionSelected = onQuickOptionSelected
                )

                HorizontalDivider()

                // 自定义日期
                CustomDateRangeSection(
                    selectedRange = selectedRange,
                    onRangeChanged = onCustomRangeSelected
                )

                // 已有总结提示
                if (existingSummaryDates.isNotEmpty()) {
                    ExistingSummaryHint(dates = existingSummaryDates)
                }

                // 验证错误
                validationError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = selectedRange != null
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 自定义日期范围区域
 */
@Composable
private fun CustomDateRangeSection(
    selectedRange: DateRange?,
    onRangeChanged: (String, String) -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "自定义日期",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 开始日期
            OutlinedButton(
                onClick = { showStartPicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(selectedRange?.startDate ?: "开始日期")
            }

            Text("至")

            // 结束日期
            OutlinedButton(
                onClick = { showEndPicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(selectedRange?.endDate ?: "结束日期")
            }
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
 * 单日期选择器对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleDatePickerDialog(
    initialDate: String?,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val initialMillis = initialDate?.let {
        LocalDate.parse(it, formatter)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .format(formatter)
                        onDateSelected(date)
                    }
                }
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

/**
 * 已有总结提示
 */
@Composable
private fun ExistingSummaryHint(dates: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = "⚠️ 已有总结的日期：",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (dates.size <= 3) {
                dates.joinToString(", ")
            } else {
                "${dates.take(3).joinToString(", ")} 等${dates.size}天"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
