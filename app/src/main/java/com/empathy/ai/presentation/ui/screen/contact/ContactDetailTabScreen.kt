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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.DataStatus
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FilterType
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
import com.empathy.ai.presentation.ui.screen.contact.vault.DataSourceInfo
import com.empathy.ai.presentation.ui.screen.contact.vault.DataVaultTab
import com.empathy.ai.presentation.viewmodel.ContactDetailTabViewModel

/**
 * 联系人详情标签页屏幕
 *
 * 新的四标签页UI，包含：概览、事实流、标签画像、资料库
 *
 * @param contactId 联系人ID
 * @param onNavigateBack 返回回调
 * @param viewModel ViewModel
 * @param modifier Modifier
 */
@Composable
fun ContactDetailTabScreen(
    contactId: String,
    onNavigateBack: () -> Unit,
    viewModel: ContactDetailTabViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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

    ContactDetailTabScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
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
        onEvent = onEvent,
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
    onEvent: (ContactDetailUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                onNavigateBack = onNavigateBack,
                onViewFactStream = {
                    onEvent(ContactDetailUiEvent.SwitchTab(DetailTab.FactStream))
                }
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
    onNavigateBack: () -> Unit,
    onViewFactStream: () -> Unit
) {
    val contact = uiState.contact ?: return
    
    OverviewTab(
        contact = contact,
        topTags = uiState.topTags,
        latestFact = uiState.latestFact,
        daysSinceFirstMet = uiState.daysSinceFirstMet,
        onBackClick = onNavigateBack,
        onViewFactStream = onViewFactStream
    )
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
        }
    )
}

/**
 * 标签画像标签页内容
 */
@Composable
private fun PersonaTabContent(
    uiState: ContactDetailUiState,
    onEvent: (ContactDetailUiEvent) -> Unit
) {
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

// ==================== Previews ====================

@Preview(name = "联系人详情标签页 - 概览", showBackground = true)
@Composable
private fun ContactDetailTabScreenOverviewPreview() {
    EmpathyTheme {
        ContactDetailTabScreenContent(
            uiState = createPreviewUiState(DetailTab.Overview),
            onEvent = {},
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
            onEvent = {},
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
            onEvent = {},
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
            onEvent = {},
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
            onEvent = {},
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
            onEvent = {},
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
