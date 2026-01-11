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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.DataStatus
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FilterType
import com.empathy.ai.domain.model.SummaryError
import com.empathy.ai.domain.model.TimelineItem
import com.empathy.ai.domain.model.ViewMode
import com.empathy.ai.presentation.theme.AnimationSpec
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.animation.AnimatedViewSwitch
import com.empathy.ai.presentation.ui.component.emotion.EmotionalBackground
import com.empathy.ai.presentation.ui.component.state.ErrorView
import com.empathy.ai.presentation.ui.component.state.LoadingIndicatorFullScreen
import com.empathy.ai.presentation.ui.screen.contact.factstream.FactStreamTab
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailUiEvent
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailUiState
import com.empathy.ai.presentation.ui.screen.contact.overview.OverviewTab
import com.empathy.ai.presentation.ui.screen.contact.persona.PersonaTab
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
 * ## 关联文档
 * - PRD-00003: 联系人画像记忆系统需求
 * - BUG-00064: AI总结功能未生效修复
 *
 * ## 标签页结构
 * - **概览 (Overview)**: 关系分数、关键事实、顶部标签
 * - **事实流 (FactStream)**: 时间线形式展示所有事实记录
 * - **标签画像 (Persona)**: 已确认和AI推测标签管理
 * - **资料库 (Vault)**: 数据源管理和导入记录
 *
 * ## BUG-00064 UI变更
 * 在 ManualSummaryDialogs 中新增无AI服务商警告对话框
 * 当用户未配置AI服务商时，点击AI总结FAB会显示友好提示
 *
 * ## 设计决策
 * - **标签导航**: 使用 PrimaryTabRow 实现Material3风格的标签页
 * - **状态提升**: 标签状态提升到顶层，跨标签页保持
 * - **动画过渡**: 使用 AnimatedContent 实现标签切换动画
 *
 * @param contactId 联系人ID
 * @param onNavigateBack 返回回调
 *
 * @see ManualSummaryDialogs
 * @see BUG-00064-AI总结功能未生效-修复方案.md
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

    BackHandler {
        onNavigateBack()
    }

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
    LaunchedEffect(uiState.error, uiState.isLoading, uiState.hasLoadedContact) {
        if (uiState.hasLoadedContact && !uiState.isLoading) {
            uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(ContactDetailUiEvent.ClearError)
            }
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
                !uiState.hasLoadedContact -> {
                    LoadingIndicatorFullScreen(message = "加载联系人信息...")
                }
                uiState.error != null && uiState.contact == null && uiState.hasLoadedContact && !uiState.isLoading -> {
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
                uiState.hasLoadedContact -> {
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
 * 标签页导航 (Material Design 3 风格优化)
 * 
 * 设计原则:
 * - 选中指示器使用圆角短棒状（Capsule shape）
 * - 指示器厚度4dp，符合圆润的设计语言
 * - 避免尖锐的直角线条
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
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (currentTab == tab) 
                            androidx.compose.ui.text.font.FontWeight.SemiBold 
                        else 
                            androidx.compose.ui.text.font.FontWeight.Normal
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
    AnimatedViewSwitch(
        targetState = uiState.currentTab,
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
        },
        // TD-00016: 主题设置回调
        onTopicClick = {
            onEvent(ContactDetailUiEvent.ShowTopicDialog)
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
    
    // TD-00016: 对话主题设置对话框
    if (uiState.showTopicDialog) {
        val topicViewModel: com.empathy.ai.presentation.viewmodel.TopicViewModel = 
            androidx.hilt.navigation.compose.hiltViewModel()
        val topicUiState by topicViewModel.uiState.collectAsStateWithLifecycle()
        
        // 初始化时加载主题数据
        LaunchedEffect(contact.id) {
            topicViewModel.loadTopic(contact.id)
        }
        
        // 监听保存成功，关闭对话框
        LaunchedEffect(topicUiState.saveSuccess) {
            if (topicUiState.saveSuccess) {
                onEvent(ContactDetailUiEvent.HideTopicDialog)
                topicViewModel.onEvent(com.empathy.ai.presentation.viewmodel.TopicUiEvent.ClearSaveSuccess)
            }
        }
        
        com.empathy.ai.presentation.ui.component.topic.TopicSettingDialog(
            uiState = topicUiState.copy(showSettingDialog = true),
            onEvent = { event ->
                when (event) {
                    is com.empathy.ai.presentation.viewmodel.TopicUiEvent.HideSettingDialog -> {
                        onEvent(ContactDetailUiEvent.HideTopicDialog)
                    }
                    else -> topicViewModel.onEvent(event)
                }
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
    
    // 添加事实对话框 - 使用iOS风格底部弹窗
    if (uiState.showAddFactToStreamDialog) {
        com.empathy.ai.presentation.ui.component.dialog.IOSAddFactBottomSheet(
            onDismiss = { onEvent(ContactDetailUiEvent.HideAddFactToStreamDialog) },
            onConfirm = { key, value ->
                onEvent(ContactDetailUiEvent.AddFactToStream(key, value))
            }
        )
    }

    // BUG-00066: 编辑标签对话框
    if (uiState.showEditBrainTagDialog && uiState.editingBrainTag != null) {
        com.empathy.ai.presentation.ui.component.dialog.EditBrainTagDialog(
            tag = uiState.editingBrainTag,
            onConfirm = { tagId, newContent, newType ->
                onEvent(ContactDetailUiEvent.ConfirmEditBrainTag(tagId, newContent, newType))
            },
            onDismiss = {
                onEvent(ContactDetailUiEvent.CancelEditBrainTag)
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
 * 简化版：直接按Fact.key分类展示所有标签
 * - 无"全部/已确认"分段控制器
 * - 无固定的"雷区/策略"分类
 * - 根据实际Fact数据动态生成分类
 */
@Composable
private fun PersonaTabContent(
    uiState: ContactDetailUiState,
    onEvent: (ContactDetailUiEvent) -> Unit
) {
    // 直接使用简化版PersonaTab，传递所有facts
    PersonaTab(
        facts = uiState.facts,
        onFactClick = { fact ->
            // 点击：编辑事实
            onEvent(ContactDetailUiEvent.StartEditFact(fact))
        },
        onFactLongClick = { fact ->
            // 长按：删除事实
            onEvent(ContactDetailUiEvent.DeleteFactById(fact.id))
        }
    )
    
    // BUG-00066: 编辑事实对话框（画像标签页）
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
    // 无AI服务商警告对话框（BUG-00064新增）
    if (summaryUiState.showNoProviderWarning) {
        AlertDialog(
            onDismissRequest = { onSummaryEvent(ManualSummaryUiEvent.DismissNoProviderWarning) },
            title = { Text("配置提示") },
            text = {
                Text(summaryUiState.noProviderWarningMessage ?: "请先在设置中配置AI服务商")
            },
            confirmButton = {
                TextButton(onClick = { onSummaryEvent(ManualSummaryUiEvent.DismissNoProviderWarning) }) {
                    Text("知道了")
                }
            }
        )
    }

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
    val taskError = summaryUiState.task?.error
    if (summaryUiState.showErrorDialog && taskError != null) {
        SummaryErrorDialog(
            error = taskError,
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
