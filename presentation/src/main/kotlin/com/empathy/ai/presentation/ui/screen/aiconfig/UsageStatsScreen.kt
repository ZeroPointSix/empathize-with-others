package com.empathy.ai.presentation.ui.screen.aiconfig

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empathy.ai.domain.model.ApiUsageStats
import com.empathy.ai.domain.model.ModelUsageStats
import com.empathy.ai.domain.model.ProviderUsageStats
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBackground
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.ui.component.ios.IOSLargeTitleBar
import com.empathy.ai.presentation.ui.component.ios.IOSSegmentedControl
import com.empathy.ai.presentation.ui.component.ios.IOSSettingsSection
import com.empathy.ai.presentation.ui.component.ios.UsageListItem
import com.empathy.ai.presentation.ui.component.ios.UsageOverviewCard
import com.empathy.ai.presentation.viewmodel.UsageStatsViewModel

/**
 * 用量统计页面
 *
 * 显示API调用的用量统计信息
 *
 * 设计规格（TD-00025）:
 * - IOSLargeTitleBar（返回、标题、导出按钮）
 * - UsageOverviewCard区域
 * - IOSTabSwitcher（按服务商/按模型）
 * - ProviderUsageList和ModelUsageList
 * - 数据管理区域（导出、清除）
 *
 * @param onNavigateBack 返回回调
 * @param viewModel ViewModel
 * @param modifier Modifier
 *
 * @see TD-00025 Phase 6: 用量统计实现
 */
@Composable
fun UsageStatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: UsageStatsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    UsageStatsScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@Composable
private fun UsageStatsScreenContent(
    uiState: UsageStatsUiState,
    onEvent: (UsageStatsUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current

    // 清除确认对话框
    if (uiState.showClearConfirmDialog) {
        ClearConfirmDialog(
            onConfirm = { onEvent(UsageStatsUiEvent.ConfirmClearHistory) },
            onDismiss = { onEvent(UsageStatsUiEvent.DismissClearConfirmDialog) }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(iOSBackground)
    ) {
        // 导航栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(iOSBackground)
                .padding(horizontal = dimensions.spacingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IOSLargeTitleBar(
                title = "用量统计",
                onBackClick = onNavigateBack,
                modifier = Modifier.weight(1f)
            )
            // 导出按钮
            IconButton(
                onClick = { onEvent(UsageStatsUiEvent.ExportData) },
                enabled = !uiState.isExporting && uiState.hasData
            ) {
                if (uiState.isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(dimensions.iconSizeMedium),
                        color = iOSBlue,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "导出",
                        tint = if (uiState.hasData) iOSBlue else iOSTextSecondary
                    )
                }
            }
        }

        // 内容区域
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                horizontal = dimensions.spacingMedium,
                vertical = dimensions.spacingMedium
            ),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
        ) {
            // 时间范围选择器
            item {
                TimeRangeSelector(
                    selectedRange = uiState.timeRange,
                    onRangeSelected = { onEvent(UsageStatsUiEvent.SwitchTimeRange(it)) }
                )
            }

            // 概览卡片
            item {
                UsageOverviewCard(
                    totalRequests = uiState.totalRequests,
                    totalTokens = formatTokenCount(uiState.totalTokens),
                    successRate = formatSuccessRate(uiState.successRate)
                )
            }

            // Tab切换器
            item {
                val tabs = listOf(UsageStatsTab.BY_PROVIDER, UsageStatsTab.BY_MODEL)
                val selectedIndex = tabs.indexOf(uiState.selectedTab)

                IOSSegmentedControl(
                    tabs = tabs.map { it.getDisplayName() },
                    selectedIndex = selectedIndex,
                    onTabSelected = { index -> onEvent(UsageStatsUiEvent.SwitchTab(tabs[index])) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 统计列表
            when (uiState.selectedTab) {
                UsageStatsTab.BY_PROVIDER -> {
                    if (uiState.providerStats.isEmpty() && !uiState.isLoading) {
                        item {
                            EmptyStateView(message = "暂无服务商统计数据")
                        }
                    } else {
                        items(uiState.providerStats) { stats ->
                            UsageListItem(
                                title = stats.providerName,
                                subtitle = "${stats.requestCount} 次请求",
                                requestCount = stats.requestCount,
                                tokenCount = stats.formatTotalTokens(),
                                successRate = stats.getSuccessRate()
                            )
                        }
                    }
                }
                UsageStatsTab.BY_MODEL -> {
                    if (uiState.modelStats.isEmpty() && !uiState.isLoading) {
                        item {
                            EmptyStateView(message = "暂无模型统计数据")
                        }
                    } else {
                        items(uiState.modelStats) { stats ->
                            UsageListItem(
                                title = stats.modelId,
                                subtitle = stats.providerName,
                                requestCount = stats.requestCount,
                                tokenCount = stats.formatTotalTokens(),
                                successRate = stats.getSuccessRate()
                            )
                        }
                    }
                }
            }

            // 数据管理区域
            item {
                Spacer(modifier = Modifier.height(dimensions.spacingLarge))
                
                IOSSettingsSection(title = "数据管理") {
                    DataManagementButtons(
                        hasData = uiState.hasData,
                        isClearing = uiState.isClearing,
                        onClear = { onEvent(UsageStatsUiEvent.ShowClearConfirmDialog) }
                    )
                }
            }

            // 错误提示
            if (uiState.error != null) {
                item {
                    ErrorBanner(
                        message = uiState.error,
                        onDismiss = { onEvent(UsageStatsUiEvent.ClearError) }
                    )
                }
            }
        }
    }
}

/**
 * 时间范围选择器
 */
@Composable
private fun TimeRangeSelector(
    selectedRange: UsageTimeRange,
    onRangeSelected: (UsageTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val ranges = listOf(
        UsageTimeRange.TODAY,
        UsageTimeRange.THIS_WEEK,
        UsageTimeRange.THIS_MONTH,
        UsageTimeRange.ALL
    )
    val selectedIndex = ranges.indexOf(selectedRange)

    IOSSegmentedControl(
        tabs = ranges.map { it.getDisplayName() },
        selectedIndex = selectedIndex,
        onTabSelected = { index -> onRangeSelected(ranges[index]) },
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * 数据管理按钮
 */
@Composable
private fun DataManagementButtons(
    hasData: Boolean,
    isClearing: Boolean,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = iOSCardBackground,
        shape = RoundedCornerShape(dimensions.cornerRadiusMedium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.spacingMedium),
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedButton(
                onClick = onClear,
                enabled = hasData && !isClearing,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFFF3B30)
                ),
                modifier = Modifier.height(dimensions.iosButtonHeight)
            ) {
                if (isClearing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(dimensions.iconSizeMedium),
                        color = Color(0xFFFF3B30),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                }
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(dimensions.iconSizeMedium)
                )
                Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                Text(
                    text = if (isClearing) "清除中..." else "清除历史数据",
                    fontSize = dimensions.fontSizeSubtitle
                )
            }
        }
    }
}

/**
 * 空状态视图
 */
@Composable
private fun EmptyStateView(
    message: String,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = dimensions.spacingXLarge),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontSize = dimensions.fontSizeBody,
            color = iOSTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 错误横幅
 */
@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFFFEBEE),
        shape = RoundedCornerShape(dimensions.cornerRadiusSmall)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.spacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                fontSize = dimensions.fontSizeBody,
                color = Color(0xFFD32F2F),
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text(
                    text = "关闭",
                    color = Color(0xFFD32F2F)
                )
            }
        }
    }
}

/**
 * 清除确认对话框
 */
@Composable
private fun ClearConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "清除历史数据",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(text = "确定要清除90天前的用量统计数据吗？此操作不可撤销。")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF3B30)
                )
            ) {
                Text("清除")
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
 * 格式化Token数量
 */
private fun formatTokenCount(tokens: Long): String {
    return when {
        tokens < 1000 -> "$tokens"
        tokens < 1000000 -> String.format("%.1fK", tokens / 1000.0)
        else -> String.format("%.2fM", tokens / 1000000.0)
    }
}

/**
 * 格式化成功率
 */
private fun formatSuccessRate(rate: Float): String {
    return String.format("%.1f%%", rate)
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "用量统计页面 - 有数据", showBackground = true)
@Composable
private fun UsageStatsScreenWithDataPreview() {
    EmpathyTheme {
        UsageStatsScreenContent(
            uiState = UsageStatsUiState(
                stats = ApiUsageStats(
                    totalRequests = 1234,
                    successRequests = 1172,
                    failedRequests = 62,
                    totalTokens = 567890,
                    averageRequestTimeMs = 1500,
                    providerStats = listOf(
                        ProviderUsageStats(
                            providerId = "1",
                            providerName = "OpenAI",
                            requestCount = 800,
                            successCount = 768,
                            totalTokens = 400000,
                            averageRequestTimeMs = 1200
                        ),
                        ProviderUsageStats(
                            providerId = "2",
                            providerName = "DeepSeek",
                            requestCount = 434,
                            successCount = 404,
                            totalTokens = 167890,
                            averageRequestTimeMs = 1800
                        )
                    ),
                    modelStats = emptyList()
                )
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "用量统计页面 - 空数据", showBackground = true)
@Composable
private fun UsageStatsScreenEmptyPreview() {
    EmpathyTheme {
        UsageStatsScreenContent(
            uiState = UsageStatsUiState(),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "用量统计页面 - 加载中", showBackground = true)
@Composable
private fun UsageStatsScreenLoadingPreview() {
    EmpathyTheme {
        UsageStatsScreenContent(
            uiState = UsageStatsUiState(isLoading = true),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
