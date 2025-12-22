package com.empathy.ai.presentation.ui.screen.contact

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.DataStatus
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FilterType
import com.empathy.ai.domain.model.SummaryError
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.model.TimelineItem
import com.empathy.ai.domain.model.ViewMode
import com.empathy.ai.presentation.theme.AnimationSpec
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.emotion.EmotionalBackground
import com.empathy.ai.presentation.ui.component.state.ErrorView
import com.empathy.ai.presentation.ui.component.state.LoadingIndicatorFullScreen
import com.empathy.ai.presentation.ui.screen.contact.factstream.FactStreamTab
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailUiEvent
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailUiState
import com.empathy.ai.presentation.ui.screen.contact.overview.OverviewTab
import com.empathy.ai.presentation.ui.screen.contact.persona.PersonaTab
import com.empathy.ai.presentation.ui.screen.contact.persona.PersonaTabV2
import com.empathy.ai.presentation.ui.screen.contact.summary.ConflictResolutionDialog
import com.empathy.ai.presentation.ui.screen.contact.summary.DateRangePickerDialog
import com.empathy.ai.presentation.ui.screen.contact.summary.ManualSummaryFab
import com.empathy.ai.presentation.ui.screen.contact.summary.SummaryDetailDialog
import com.empathy.ai.presentation.ui.screen.contact.summary.RangeWarningDialog
import com.empathy.ai.presentation.ui.screen.contact.summary.SummaryErrorDialog
import com.empathy.ai.presentation.ui.screen.contact.summary.SummaryProgressDialog
import com.empathy.ai.presentation.ui.screen.contact.summary.SummaryResultDialog
import com.empathy.ai.presentation.ui.screen.contact.vault.DataSourceInfo
import com.empathy.ai.presentation.ui.screen.contact.vault.DataVaultTab
import com.empathy.ai.presentation.viewmodel.ContactDetailTabViewModel
import com.empathy.ai.presentation.viewmodel.ManualSummaryUiEvent
import com.empathy.ai.presentation.viewmodel.ManualSummaryUiState
import com.empathy.ai.presentation.viewmodel.ManualSummaryViewModel

/**
 * 联系人详情标签页屏幕
 *
 * 新的四标签页UI，包含：概览、事实流、标签画像、资料库
 *
 * @param contactId 联系人ID
 * @param onNavigateBack 返回回调
 * @param viewModel ViewModel
 * @param manualSummaryViewModel 手动总结ViewModel
 * @param modifier Modifier
 */
@Composable
fun ContactDetailTabScreen(
    contactId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPromptEditor: ((String) -> Unit)? = null,
    viewModel: ContactDetailTabViewModel = hiltViewModel(),
    manualSummaryViewModel: ManualSummaryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val summaryUiState by manualSummaryViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 加载联系人数据
    LaunchedEffect(contactId) {
        viewModel.loadContactDetail(contactId)
    }

    // 显示成功消息
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(ContactDetailUiEvent.ClearSuccessMessage)
        }
    }

    // 显示错误消息
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(ContactDetailUiEvent.ClearError)
        }
    }

    // 处理导航到时光轴
    LaunchedEffect(summaryUiState.navigateToTimeline) {
        if (summaryUiState.navigateToTimeline) {
            // 切换到事实流标签页查看总结结果
            viewModel.onEvent(ContactDetailUiEvent.SwitchTab(DetailTab.FactStream))
            // 刷新数据以显示新生成的总结
            viewModel.onEvent(ContactDetailUiEvent.RefreshData)
            manualSummaryViewModel.onEvent(ManualSummaryUiEvent.ClearNavigation)
        }
    }

    ContactDetailTabScreenContent(
        uiState = uiState,
        summaryUiState = summaryUiState,
        onEvent = viewModel::onEvent,
        onSummaryEvent = manualSummaryViewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onNavigateToPromptEditor = onNavigateToPromptEditor,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}


/**
 * 联系人详情标签页屏幕（无状态版本，用于测试）
 *
 * @param uiState UI状态
 * @param onEvent 事件处理
 * @param onNavigateBack 返回回调
 * @param modifier Modifier
 */
@Composable
fun ContactDetailTabScreen(
    uiState: ContactDetailUiState,
    onEvent: (ContactDetailUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    ContactDetailTabScreenContent(
        uiState = uiState,
        summaryUiState = ManualSummaryUiState(),
        onEvent = onEvent,
        onSummaryEvent = {},
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

/**
 * 联系人详情标签页屏幕内容（无状态）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactDetailTabScreenContent(
    uiState: ContactDetailUiState,
    summaryUiState: ManualSummaryUiState,
    onEvent: (ContactDetailUiEvent) -> Unit,
    onSummaryEvent: (ManualSummaryUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToPromptEditor: ((String) -> Unit)? = null,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // 在事实流标签页显示手动总结FAB
            if (uiState.currentTab == DetailTab.FactStream && uiState.contact != null) {
                ManualSummaryFab(
                    onClick = {
                        uiState.contact?.let { contact ->
                            onSummaryEvent(ManualSummaryUiEvent.ShowDatePicker(contact.id))
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicatorFullScreen(message = "加载联系人信息...")
                }
                uiState.error != null && uiState.contact == null -> {
                    ErrorView(
                        message = uiState.error,
                        onRetry = { onEvent(ContactDetailUiEvent.RefreshData) }
                    )
                }
                uiState.contact != null -> {
                    // 情感化背景
                    EmotionalBackground(relationshipScore = uiState.contact.relationshipScore)
                    
                    // 主内容
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 标签页导航
                        TabNavigation(
                            currentTab = uiState.currentTab,
                            onTabSelected = { tab ->
                                onEvent(ContactDetailUiEvent.SwitchTab(tab))
                            }
                        )
                        
                        // 标签页内容
                        TabContent(
                            uiState = uiState,
                            onEvent = onEvent,
                            onNavigateBack = onNavigateBack,
                            onNavigateToPromptEditor = onNavigateToPromptEditor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                else -> {
                    // 空状态
                    ErrorView(
                        message = "未找到联系人",
                        onRetry = { onEvent(ContactDetailUiEvent.RefreshData) }
                    )
                }
            }
        }
    }

    // 手动总结相关对话框
    ManualSummaryDialogs(
        summaryUiState = summaryUiState,
        onSummaryEvent = onSummaryEvent
    )
}

/**
 * 标签页导航
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabNavigation(
    currentTab: DetailTab,
    onTabSelected: (DetailTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = DetailTab.entries
    val selectedIndex = tabs.indexOf(currentTab)
    
    PrimaryTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = tab.toDisplayName(),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }
    }
}

/**
 * 标签页内容
 */
@Composable
private fun TabContent(
    uiState: ContactDetailUiState,
    onEvent: (ContactDetailUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToPromptEditor: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = uiState.currentTab,
        transitionSpec = {
            fadeIn(animationSpec = tween(AnimationSpec.DurationNormal))
                .togetherWith(fadeOut(animationSpec = tween(AnimationSpec.DurationNormal)))
        },
        label = "TabContentTransition",
        modifier = modifier
    ) { tab ->
        when (tab) {
            DetailTab.Overview -> OverviewTabContent(
                uiState = uiState,
                onEvent = onEvent,
                onNavigateBack = onNavigateBack,
                onViewFactStream = {
                    onEvent(ContactDetailUiEvent.SwitchTab(DetailTab.FactStream))
                },
                onNavigateToPromptEditor = onNavigateToPromptEditor
            )
            DetailTab.FactStream -> FactStreamTabContent(
                uiState = uiState,
                onEvent = onEvent
            )
            DetailTab.Persona -> PersonaTabContent(
                uiState = uiState,
                onEvent = onEvent
            )
            DetailTab.DataVault -> DataVaultTabContent(
                uiState = uiState
            )
        }
    }
}

/**
 * 概览标签页内容
 */
@Composable
private fun OverviewTabContent(
    uiState: ContactDetailUiState,
    onEvent: (ContactDetailUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onViewFactStream: () -> Unit,
    onNavigateToPromptEditor: ((String) -> Unit)? = null
) {
    val contact = uiState.contact ?: return
    
    OverviewTab(
        contact = contact,
        topTags = uiState.topTags,
        latestFact = uiState.latestFact,
        daysSinceFirstMet = uiState.daysSinceFirstMet,
        onBackClick = onNavigateBack,
        onViewFactStream = onViewFactStream,
        onEditCustomPrompt = onNavigateToPromptEditor?.let { navigate ->
            {
                navigate(
                    com.empathy.ai.presentation.navigation.PromptEditorRoutes.contactCustom(
                        contactId = contact.id,
                        contactName = contact.name
                    )
                )
            }
        },
        // TD-00012: 编辑联系人信息回调
        onEditContactInfo = {
            onEvent(ContactDetailUiEvent.StartEditContactInfo)
        }
    )
    
    // TD-00012: 编辑联系人信息对话框
    if (uiState.showEditContactInfoDialog) {
        com.empathy.ai.presentation.ui.component.dialog.EditContactInfoDialog(
            initialName = contact.name,
            initialTargetGoal = contact.targetGoal,
            onDismiss = { onEvent(ContactDetailUiEvent.CancelEditContactInfo) },
            onConfirm = { newName, newTargetGoal ->
                onEvent(ContactDetailUiEvent.ConfirmEditContactInfo(newName, newTargetGoal))
            }
        )
    }
}

/**
 * 事实流标签页内容
 */
@Composable
private fun FactStreamTabContent(
    uiState: ContactDetailUiState,
    onEvent: (ContactDetailUiEvent) -> Unit
) {
    FactStreamTab(
        items = uiState.filteredTimelineItems,
        viewMode = uiState.viewMode,
        selectedFilters = uiState.selectedFilters,
        onViewModeChange = { mode ->
            onEvent(ContactDetailUiEvent.SwitchViewMode(mode))
        },
        onFilterToggle = { filter ->
            onEvent(ContactDetailUiEvent.ToggleFilter(filter))
        },
        onConversationEdit = { logId ->
            onEvent(ContactDetailUiEvent.SelectConversation(logId))
        },
        // TD-00012: 事实编辑回调
        onFactEdit = { factId ->
            val fact = uiState.facts.find { it.id == factId }
            fact?.let { onEvent(ContactDetailUiEvent.StartEditFact(it)) }
        },
        // TD-00012: 总结编辑回调
        onSummaryEdit = { summaryId ->
            onEvent(ContactDetailUiEvent.StartEditSummary(summaryId))
        },
        onAddFactClick = {
            onEvent(ContactDetailUiEvent.ShowAddFactToStreamDialog)
        }
    )
    
    // 编辑对话对话框
    if (uiState.showEditConversationDialog && uiState.selectedConversationId != null) {
        com.empathy.ai.presentation.ui.component.dialog.EditConversationDialog(
            initialContent = uiState.editingConversationContent,
            onDismiss = { onEvent(ContactDetailUiEvent.HideEditConversationDialog) },
            onConfirm = { newContent ->
                onEvent(ContactDetailUiEvent.EditConversation(
                    uiState.selectedConversationId,
                    newContent
                ))
            },
            onDelete = {
                onEvent(ContactDetailUiEvent.DeleteConversation(uiState.selectedConversationId))
            }
        )
    }
    
    // 添加事实对话框
    if (uiState.showAddFactToStreamDialog) {
        com.empathy.ai.presentation.ui.component.dialog.AddFactToStreamDialog(
            onDismiss = { onEvent(ContactDetailUiEvent.HideAddFactToStreamDialog) },
            onConfirm = { key, value ->
                onEvent(ContactDetailUiEvent.AddFactToStream(key, value))
            }
        )
    }
    
    // TD-00012: 编辑事实对话框
    if (uiState.showEditFactDialog && uiState.editingFact != null) {
        com.empathy.ai.presentation.ui.component.dialog.EditFactDialog(
            fact = uiState.editingFact,
            onDismiss = { onEvent(ContactDetailUiEvent.CancelEditFact) },
            onSave = { newKey, newValue ->
                onEvent(ContactDetailUiEvent.ConfirmEditFact(
                    uiState.editingFact.id,
                    newKey,
                    newValue
                ))
            },
            onDelete = {
                onEvent(ContactDetailUiEvent.DeleteFactById(uiState.editingFact.id))
            }
        )
    }
    
    // TD-00012: 编辑总结对话框
    if (uiState.showEditSummaryDialog && uiState.editingSummaryId != null) {
        val editingSummary = uiState.summaries.find { it.id == uiState.editingSummaryId }
        if (editingSummary != null) {
            com.empathy.ai.presentation.ui.component.dialog.EditSummaryDialog(
                summary = editingSummary,
                onDismiss = { onEvent(ContactDetailUiEvent.CancelEditSummary) },
                onSave = { newContent ->
                    onEvent(ContactDetailUiEvent.ConfirmEditSummary(
                        uiState.editingSummaryId,
                        newContent
                    ))
                }
            )
        }
    }
}

/**
 * 标签画像标签页内容
 * 
 * 根据Feature Flag切换使用PersonaTab或PersonaTabV2
 * - PersonaTab: 旧版本，简单的雷区/策略标签展示
 * - PersonaTabV2: 新版本，支持分类搜索、编辑模式、批量操作
 */
@Composable
private fun PersonaTabContent(
    uiState: ContactDetailUiState,
    onEvent: (ContactDetailUiEvent) -> Unit
) {
    if (uiState.usePersonaTabV2) {
        // 使用新版PersonaTabV2
        // TODO: 从系统设置获取深色模式状态
        val isDarkMode = false
        
        PersonaTabV2(
            categories = uiState.factCategories,
            searchState = uiState.personaSearchState,
            editModeState = uiState.editModeState,
            availableCategories = uiState.availableCategories,
            isDarkMode = isDarkMode,
            onSearchQueryChange = { query ->
                onEvent(ContactDetailUiEvent.UpdatePersonaSearch(query))
            },
            onClearSearch = {
                onEvent(ContactDetailUiEvent.ClearPersonaSearch)
            },
            onToggleCategoryExpand = { categoryKey ->
                onEvent(ContactDetailUiEvent.ToggleCategoryExpand(categoryKey))
            },
            onFactClick = { factId ->
                // 普通点击：查看详情或编辑
                val fact = uiState.facts.find { it.id == factId }
                fact?.let { onEvent(ContactDetailUiEvent.StartEditFact(it)) }
            },
            onFactLongClick = { factId ->
                // 长按：进入编辑模式
                onEvent(ContactDetailUiEvent.EnterEditMode(factId))
            },
            onToggleFactSelection = { factId ->
                onEvent(ContactDetailUiEvent.ToggleFactSelection(factId))
            },
            onExitEditMode = {
                onEvent(ContactDetailUiEvent.ExitEditMode)
            },
            onSelectAll = {
                onEvent(ContactDetailUiEvent.SelectAllFacts)
            },
            onDeselectAll = {
                onEvent(ContactDetailUiEvent.DeselectAllFacts)
            },
            onShowDeleteConfirm = {
                onEvent(ContactDetailUiEvent.ShowBatchDeleteConfirm)
            },
            onHideDeleteConfirm = {
                onEvent(ContactDetailUiEvent.HideBatchDeleteConfirm)
            },
            onConfirmDelete = {
                onEvent(ContactDetailUiEvent.ConfirmBatchDelete)
            },
            onShowMoveDialog = {
                onEvent(ContactDetailUiEvent.ShowBatchMoveDialog)
            },
            onHideMoveDialog = {
                onEvent(ContactDetailUiEvent.HideBatchMoveDialog)
            },
            onConfirmMove = { targetCategory ->
                onEvent(ContactDetailUiEvent.ConfirmBatchMove(targetCategory))
            }
        )
    } else {
        // 使用旧版PersonaTab
        // 将Facts转换为BrainTags用于显示
        val riskTags = uiState.facts
            .filter { it.key.contains("雷区") || it.key.contains("禁忌") || it.key.contains("不喜欢") }
            .mapIndexed { index, fact ->
                BrainTag(
                    id = index.toLong(),
                    contactId = uiState.contact?.id ?: "",
                    content = fact.value,
                    type = TagType.RISK_RED,
                    isConfirmed = fact.source == com.empathy.ai.domain.model.FactSource.MANUAL,
                    source = fact.source.name
                )
            }
        
        val strategyTags = uiState.facts
            .filter { it.key.contains("策略") || it.key.contains("喜欢") || it.key.contains("兴趣") }
            .mapIndexed { index, fact ->
                BrainTag(
                    id = (index + 1000).toLong(),
                    contactId = uiState.contact?.id ?: "",
                    content = fact.value,
                    type = TagType.STRATEGY_GREEN,
                    isConfirmed = fact.source == com.empathy.ai.domain.model.FactSource.MANUAL,
                    source = fact.source.name
                )
            }
        
        PersonaTab(
            riskTags = riskTags,
            strategyTags = strategyTags,
            onConfirmTag = { tag ->
                onEvent(ContactDetailUiEvent.ConfirmTag(tag.id))
            },
            onRejectTag = { tag ->
                onEvent(ContactDetailUiEvent.RejectTag(tag.id))
            }
        )
    }
}

/**
 * 资料库标签页内容
 */
@Composable
private fun DataVaultTabContent(
    uiState: ContactDetailUiState
) {
    val dataSources = listOf(
        DataSourceInfo(
            id = "chat",
            title = "聊天记录",
            icon = Icons.Default.Chat,
            count = uiState.conversationCount,
            status = if (uiState.conversationCount > 0) DataStatus.COMPLETED else DataStatus.NOT_AVAILABLE
        ),
        DataSourceInfo(
            id = "summary",
            title = "AI总结",
            icon = Icons.Default.Note,
            count = uiState.summaryCount,
            status = if (uiState.summaryCount > 0) DataStatus.COMPLETED else DataStatus.NOT_AVAILABLE
        ),
        DataSourceInfo(
            id = "image",
            title = "图片",
            icon = Icons.Default.Image,
            count = 0,
            status = DataStatus.NOT_AVAILABLE
        ),
        DataSourceInfo(
            id = "voice",
            title = "语音消息",
            icon = Icons.Default.Mic,
            count = 0,
            status = DataStatus.NOT_AVAILABLE
        ),
        DataSourceInfo(
            id = "video",
            title = "视频",
            icon = Icons.Default.VideoFile,
            count = 0,
            status = DataStatus.NOT_AVAILABLE
        ),
        DataSourceInfo(
            id = "folder",
            title = "文件",
            icon = Icons.Default.Folder,
            count = 0,
            status = DataStatus.NOT_AVAILABLE
        )
    )
    
    DataVaultTab(dataSources = dataSources)
}

/**
 * DetailTab扩展函数：获取显示名称
 */
private fun DetailTab.toDisplayName(): String {
    return when (this) {
        DetailTab.Overview -> "概览"
        DetailTab.FactStream -> "事实流"
        DetailTab.Persona -> "画像"
        DetailTab.DataVault -> "资料库"
    }
}

/**
 * 手动总结相关对话框
 *
 * 集中管理所有手动总结功能的对话框显示逻辑
 */
@Composable
private fun ManualSummaryDialogs(
    summaryUiState: ManualSummaryUiState,
    onSummaryEvent: (ManualSummaryUiEvent) -> Unit
) {
    // 日期选择对话框
    if (summaryUiState.showDatePicker) {
        DateRangePickerDialog(
            selectedOption = summaryUiState.selectedQuickOption,
            selectedRange = summaryUiState.selectedDateRange,
            existingSummaryDates = emptyList(), // TODO: 从状态获取已有总结日期
            validationError = summaryUiState.validationError,
            onQuickOptionSelected = { option ->
                onSummaryEvent(ManualSummaryUiEvent.SelectQuickOption(option))
            },
            onCustomRangeSelected = { start, end ->
                onSummaryEvent(ManualSummaryUiEvent.SelectCustomRange(start, end))
            },
            onConfirm = { onSummaryEvent(ManualSummaryUiEvent.ConfirmDateRange) },
            onDismiss = { onSummaryEvent(ManualSummaryUiEvent.DismissDatePicker) }
        )
    }

    // 范围警告对话框
    if (summaryUiState.showRangeWarning) {
        RangeWarningDialog(
            message = summaryUiState.rangeWarningMessage ?: "选择的日期范围较长，可能需要较长时间处理。",
            onConfirm = { onSummaryEvent(ManualSummaryUiEvent.ConfirmRangeWarning) },
            onDismiss = { onSummaryEvent(ManualSummaryUiEvent.DismissRangeWarning) }
        )
    }

    // 冲突处理对话框
    if (summaryUiState.showConflictDialog && summaryUiState.conflictResult != null) {
        ConflictResolutionDialog(
            conflict = summaryUiState.conflictResult,
            selectedResolution = summaryUiState.selectedConflictResolution,
            onResolutionSelected = { resolution ->
                onSummaryEvent(ManualSummaryUiEvent.SelectConflictResolution(resolution))
            },
            onConfirm = { onSummaryEvent(ManualSummaryUiEvent.ConfirmConflictResolution) },
            onDismiss = { onSummaryEvent(ManualSummaryUiEvent.DismissConflictDialog) }
        )
    }

    // 进度对话框
    if (summaryUiState.showProgressDialog && summaryUiState.task != null) {
        SummaryProgressDialog(
            task = summaryUiState.task,
            onCancel = { onSummaryEvent(ManualSummaryUiEvent.CancelSummary) }
        )
    }

    // 结果统计对话框
    if (summaryUiState.showResultDialog && summaryUiState.summaryResult != null) {
        SummaryResultDialog(
            result = summaryUiState.summaryResult,
            onViewSummary = { onSummaryEvent(ManualSummaryUiEvent.ViewResult) },
            onDismiss = { onSummaryEvent(ManualSummaryUiEvent.DismissResult) }
        )
    }

    // 总结详情对话框
    if (summaryUiState.showSummaryDetailDialog && summaryUiState.summaryResult != null) {
        SummaryDetailDialog(
            summary = summaryUiState.summaryResult.summary,
            onDismiss = { onSummaryEvent(ManualSummaryUiEvent.DismissSummaryDetail) }
        )
    }

    // 错误对话框
    if (summaryUiState.showErrorDialog && summaryUiState.task?.error != null) {
        SummaryErrorDialog(
            error = summaryUiState.task.error,
            onRetry = { onSummaryEvent(ManualSummaryUiEvent.RetryFailed) },
            onDismiss = { onSummaryEvent(ManualSummaryUiEvent.DismissError) }
        )
    }
}

// ==================== Previews ====================

@Preview(name = "联系人详情标签页 - 概览", showBackground = true)
@Composable
private fun ContactDetailTabScreenOverviewPreview() {
    EmpathyTheme {
        ContactDetailTabScreenContent(
            uiState = createPreviewUiState(DetailTab.Overview),
            summaryUiState = ManualSummaryUiState(),
            onEvent = {},
            onSummaryEvent = {},
            onNavigateBack = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(name = "联系人详情标签页 - 事实流", showBackground = true)
@Composable
private fun ContactDetailTabScreenFactStreamPreview() {
    EmpathyTheme {
        ContactDetailTabScreenContent(
            uiState = createPreviewUiState(DetailTab.FactStream),
            summaryUiState = ManualSummaryUiState(),
            onEvent = {},
            onSummaryEvent = {},
            onNavigateBack = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(name = "联系人详情标签页 - 画像", showBackground = true)
@Composable
private fun ContactDetailTabScreenPersonaPreview() {
    EmpathyTheme {
        ContactDetailTabScreenContent(
            uiState = createPreviewUiState(DetailTab.Persona),
            summaryUiState = ManualSummaryUiState(),
            onEvent = {},
            onSummaryEvent = {},
            onNavigateBack = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(name = "联系人详情标签页 - 资料库", showBackground = true)
@Composable
private fun ContactDetailTabScreenDataVaultPreview() {
    EmpathyTheme {
        ContactDetailTabScreenContent(
            uiState = createPreviewUiState(DetailTab.DataVault),
            summaryUiState = ManualSummaryUiState(),
            onEvent = {},
            onSummaryEvent = {},
            onNavigateBack = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(name = "联系人详情标签页 - 加载中", showBackground = true)
@Composable
private fun ContactDetailTabScreenLoadingPreview() {
    EmpathyTheme {
        ContactDetailTabScreenContent(
            uiState = ContactDetailUiState(isLoading = true),
            summaryUiState = ManualSummaryUiState(),
            onEvent = {},
            onSummaryEvent = {},
            onNavigateBack = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(
    name = "联系人详情标签页 - 深色模式",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ContactDetailTabScreenDarkPreview() {
    EmpathyTheme {
        ContactDetailTabScreenContent(
            uiState = createPreviewUiState(DetailTab.Overview),
            summaryUiState = ManualSummaryUiState(),
            onEvent = {},
            onSummaryEvent = {},
            onNavigateBack = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

/**
 * 创建预览用的UiState
 */
private fun createPreviewUiState(tab: DetailTab): ContactDetailUiState {
    val sampleFacts = listOf(
        Fact(
            key = "兴趣爱好",
            value = "喜欢吃辣",
            source = com.empathy.ai.domain.model.FactSource.MANUAL,
            timestamp = 1L
        ),
        Fact(
            key = "兴趣爱好",
            value = "猫奴",
            source = com.empathy.ai.domain.model.FactSource.MANUAL,
            timestamp = 2L
        ),
        Fact(
            key = "禁忌话题",
            value = "不要提工作压力",
            source = com.empathy.ai.domain.model.FactSource.AI_INFERRED,
            timestamp = 3L
        )
    )
    
    val sampleTimelineItems = listOf(
        TimelineItem.Milestone(
            id = "1",
            timestamp = System.currentTimeMillis(),
            emotionType = EmotionType.GIFT,
            title = "相识100天",
            description = "从陌生到熟悉",
            icon = "🏆"
        )
    )
    
    return ContactDetailUiState(
        contact = ContactProfile(
            id = "1",
            name = "小明",
            targetGoal = "建立良好关系",
            avatarUrl = "",
            relationshipScore = 85
        ),
        facts = sampleFacts,
        topTags = sampleFacts.take(3),
        latestFact = sampleFacts.lastOrNull(),
        daysSinceFirstMet = 105,
        currentTab = tab,
        viewMode = ViewMode.Timeline,
        selectedFilters = emptySet(),
        timelineItems = sampleTimelineItems,
        conversationCount = 128,
        summaryCount = 15
    )
}
