package com.empathy.ai.presentation.ui.screen.tag

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.AppSpacing
import com.empathy.ai.presentation.ui.component.chip.TagChip
import com.empathy.ai.presentation.ui.component.dialog.EditBrainTagDialog
import com.empathy.ai.presentation.ui.component.dialog.IOSAlertDialog
import com.empathy.ai.presentation.ui.component.dialog.IOSInputDialog
import com.empathy.ai.presentation.ui.component.input.CustomTextField
import com.empathy.ai.presentation.ui.component.state.EmptyType
import com.empathy.ai.presentation.ui.component.state.EmptyView
import com.empathy.ai.presentation.ui.component.state.LoadingIndicator
import com.empathy.ai.presentation.ui.component.state.LoadingIndicatorFullScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empathy.ai.presentation.viewmodel.BrainTagViewModel

/**
 * 标签管理页面
 *
 * ## 业务职责
 * 管理应用中所有脑标签（BrainTag），包括：
 * - 标签的增删改查操作
 * - 按类型分组展示（雷区标签/策略标签）
 * - 标签搜索过滤
 * - 批量操作支持
 *
 * ## 关联文档
 * - PRD-00003: 标签管理功能需求
 * - TDD-00003: 标签管理技术设计
 *
 * ## 页面布局
 * ```
 * ┌─────────────────────────────────────┐
 * │  ←    标签管理            [🔍]    │  ← Material TopAppBar
 * ├─────────────────────────────────────┤
 * │                                     │
 * │  雷区标签 (3)                       │  ← 分组标题（红色）
 *  │  ┌───────────────────────────────┐ │
 * │  │ [×] 不要提工作压力            │ │  ← 雷区标签（红色）
 * │  │ [×] 避免讨论家庭问题          │ │
 * │  └───────────────────────────────┘ │
 * │                                     │
 * │  策略标签 (5)                       │  ← 分组标题（绿色）
 * │  ┌───────────────────────────────┐ │
 * │  │ [×] 喜欢聊摄影技巧            │ │  ← 策略标签（绿色）
 * │  │ [×] 对旅行话题感兴趣          │ │
 * │  └───────────────────────────────┘ │
 * │                                     │
 * │                          [+]       │  ← FAB（添加标签）
 * └─────────────────────────────────────┘
 * ```
 *
 * ## 核心设计决策
 * 1. **双类型分类**: 标签分为雷区（红色/RISK_RED）和策略（绿色/STRATEGY_GREEN）
 * 2. **分组展示**: 按类型分组，便于用户理解和使用
 * 3. **Material 3设计**: 使用TopAppBar和FAB，符合Material Design规范
 * 4. **iOS对话框**: 使用IOSInputDialog，保持UI风格一致
 * 5. **可删除性**: 每个标签都带有删除按钮，支持快速删除
 *
 * ## 标签类型说明
 * - **雷区标签 (RISK_RED)**: 表示需要在沟通中避免的内容/话题/行为
 * - **策略标签 (STRATEGY_GREEN)**: 表示可以主动使用的沟通策略/话题
 *
 * @param onNavigateBack 返回回调
 * @param viewModel 标签管理ViewModel
 * @param modifier Modifier
 * @see BrainTagViewModel 管理标签状态和业务逻辑
 * @see TagChip 标签组件
 * @see TagType 标签类型枚举
 */
@Composable
fun BrainTagScreen(
    onNavigateBack: () -> Unit,
    viewModel: BrainTagViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BrainTagScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

/**
 * 标签管理页面内容（无状态）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrainTagScreenContent(
    uiState: BrainTagUiState,
    onEvent: (BrainTagUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("标签管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: 实现搜索 */ }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEvent(BrainTagUiEvent.ShowAddDialog) }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加标签"
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
                    LoadingIndicatorFullScreen(
                        message = "加载标签..."
                    )
                }
                uiState.isEmptyState -> {
                    EmptyView(
                        message = "还没有标签",
                        actionText = "添加标签",
                        onAction = { onEvent(BrainTagUiEvent.ShowAddDialog) },
                        emptyType = EmptyType.NoTags
                    )
                }
                else -> {
                    TagList(
                        tags = uiState.displayTags,
                        searchQuery = uiState.searchQuery,
                        onDeleteTag = { tagId -> onEvent(BrainTagUiEvent.DeleteTag(tagId)) },
                        onEditTag = { tag -> onEvent(BrainTagUiEvent.StartEditTag(tag)) }
                    )
                }
            }
        }
    }

    // 添加标签对话框
    if (uiState.showAddDialog) {
        AddTagDialog(
            tagContent = uiState.newTagContent,
            selectedType = uiState.selectedTagType,
            onContentChange = { onEvent(BrainTagUiEvent.UpdateNewTagContent(it)) },
            onTypeChange = { onEvent(BrainTagUiEvent.UpdateSelectedTagType(it)) },
            onDismiss = { onEvent(BrainTagUiEvent.HideAddDialog) },
            onConfirm = { onEvent(BrainTagUiEvent.ConfirmAddTag) }
        )
    }

    // 编辑标签对话框 (BUG-00066)
    if (uiState.showEditDialog && uiState.editingTag != null) {
        EditBrainTagDialog(
            tag = uiState.editingTag,
            onConfirm = { tagId, newContent, newType ->
                onEvent(BrainTagUiEvent.ConfirmEditTag(tagId, newContent, newType))
            },
            onDismiss = { onEvent(BrainTagUiEvent.CancelEditTag) }
        )
    }

    // 错误提示 - iOS风格
    uiState.error?.let { error ->
        IOSAlertDialog(
            title = "错误",
            message = error,
            confirmText = "确定",
            onConfirm = { onEvent(BrainTagUiEvent.ClearError) },
            onDismiss = { onEvent(BrainTagUiEvent.ClearError) },
            showDismissButton = false
        )
    }
}

/**
 * 标签列表
 *
 * @param tags 标签列表
 * @param searchQuery 搜索关键词
 * @param onDeleteTag 删除标签回调
 * @param onEditTag 编辑标签回调 (BUG-00066)
 * @param modifier Modifier
 */
@Composable
private fun TagList(
    tags: List<BrainTag>,
    searchQuery: String,
    onDeleteTag: (Long) -> Unit,
    onEditTag: (BrainTag) -> Unit,
    modifier: Modifier = Modifier
) {
    // 过滤标签
    val filteredTags = if (searchQuery.isBlank()) {
        tags
    } else {
        tags.filter { it.content.contains(searchQuery, ignoreCase = true) }
    }

    // 按类型分组
    val landmineTags = filteredTags.filter { it.type == TagType.RISK_RED }
    val strategyTags = filteredTags.filter { it.type == TagType.STRATEGY_GREEN }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
    ) {
        // 雷区标签
        if (landmineTags.isNotEmpty()) {
            item {
                Text(
                    text = "雷区标签 (${landmineTags.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            items(
                items = landmineTags,
                key = { it.id }
            ) { tag ->
                TagChip(
                    text = tag.content,
                    tagType = tag.type,
                    onDelete = { onDeleteTag(tag.id) },
                    onClick = { onEditTag(tag) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
        }

        // 策略标签
        if (strategyTags.isNotEmpty()) {
            item {
                Text(
                    text = "策略标签 (${strategyTags.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(
                items = strategyTags,
                key = { it.id }
            ) { tag ->
                TagChip(
                    text = tag.content,
                    tagType = tag.type,
                    onDelete = { onDeleteTag(tag.id) },
                    onClick = { onEditTag(tag) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 添加标签对话框 - iOS风格
 */
@Composable
private fun AddTagDialog(
    tagContent: String,
    selectedType: String,
    onContentChange: (String) -> Unit,
    onTypeChange: (TagType) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val selectedTagType = try {
        TagType.valueOf(selectedType)
    } catch (e: IllegalArgumentException) {
        TagType.STRATEGY_GREEN
    }

    IOSInputDialog(
        title = "添加标签",
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
            ) {
                CustomTextField(
                    value = tagContent,
                    onValueChange = onContentChange,
                    label = "标签内容",
                    placeholder = "请输入标签内容",
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "标签类型",
                    style = MaterialTheme.typography.labelMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    FilterChip(
                        selected = selectedTagType == TagType.RISK_RED,
                        onClick = { onTypeChange(TagType.RISK_RED) },
                        label = { Text("雷区") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedTagType == TagType.STRATEGY_GREEN,
                        onClick = { onTypeChange(TagType.STRATEGY_GREEN) },
                        label = { Text("策略") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmText = "添加",
        dismissText = "取消",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmEnabled = tagContent.isNotBlank()
    )
}

// ==================== Previews ====================

@Preview(name = "标签管理 - 默认", showBackground = true)
@Composable
private fun BrainTagScreenPreview() {
    EmpathyTheme {
        BrainTagScreenContent(
            uiState = BrainTagUiState(
                tags = listOf(
                    BrainTag(
                        id = 1,
                        contactId = "1",
                        content = "不要提工作压力",
                        type = TagType.RISK_RED,
                        source = "MANUAL"
                    ),
                    BrainTag(
                        id = 2,
                        contactId = "1",
                        content = "避免讨论家庭问题",
                        type = TagType.RISK_RED,
                        source = "MANUAL"
                    ),
                    BrainTag(
                        id = 3,
                        contactId = "1",
                        content = "喜欢聊摄影技巧",
                        type = TagType.STRATEGY_GREEN,
                        source = "MANUAL"
                    ),
                    BrainTag(
                        id = 4,
                        contactId = "1",
                        content = "对旅行话题感兴趣",
                        type = TagType.STRATEGY_GREEN,
                        source = "MANUAL"
                    ),
                    BrainTag(
                        id = 5,
                        contactId = "1",
                        content = "喜欢美食推荐",
                        type = TagType.STRATEGY_GREEN,
                        source = "MANUAL"
                    )
                ),
                filteredTags = listOf(
                    BrainTag(
                        id = 1,
                        contactId = "1",
                        content = "不要提工作压力",
                        type = TagType.RISK_RED,
                        source = "MANUAL"
                    ),
                    BrainTag(
                        id = 2,
                        contactId = "1",
                        content = "避免讨论家庭问题",
                        type = TagType.RISK_RED,
                        source = "MANUAL"
                    ),
                    BrainTag(
                        id = 3,
                        contactId = "1",
                        content = "喜欢聊摄影技巧",
                        type = TagType.STRATEGY_GREEN,
                        source = "MANUAL"
                    ),
                    BrainTag(
                        id = 4,
                        contactId = "1",
                        content = "对旅行话题感兴趣",
                        type = TagType.STRATEGY_GREEN,
                        source = "MANUAL"
                    ),
                    BrainTag(
                        id = 5,
                        contactId = "1",
                        content = "喜欢美食推荐",
                        type = TagType.STRATEGY_GREEN,
                        source = "MANUAL"
                    )
                )
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "标签管理 - 空状态", showBackground = true)
@Composable
private fun BrainTagScreenEmptyPreview() {
    EmpathyTheme {
        BrainTagScreenContent(
            uiState = BrainTagUiState(),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "标签管理 - 加载中", showBackground = true)
@Composable
private fun BrainTagScreenLoadingPreview() {
    EmpathyTheme {
        BrainTagScreenContent(
            uiState = BrainTagUiState(isLoading = true),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "标签管理 - 添加对话框", showBackground = true)
@Composable
private fun AddTagDialogPreview() {
    EmpathyTheme {
        AddTagDialog(
            tagContent = "",
            selectedType = "STRATEGY_GREEN",
            onContentChange = {},
            onTypeChange = {},
            onDismiss = {},
            onConfirm = {}
        )
    }
}

@Preview(
    name = "标签管理 - 深色模式",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun BrainTagScreenDarkPreview() {
    EmpathyTheme {
        BrainTagScreenContent(
            uiState = BrainTagUiState(
                tags = listOf(
                    BrainTag(
                        id = 1,
                        contactId = "1",
                        content = "不要提工作压力",
                        type = TagType.RISK_RED,
                        source = "MANUAL"
                    ),
                    BrainTag(
                        id = 2,
                        contactId = "1",
                        content = "喜欢聊摄影技巧",
                        type = TagType.STRATEGY_GREEN,
                        source = "MANUAL"
                    )
                ),
                filteredTags = listOf(
                    BrainTag(
                        id = 1,
                        contactId = "1",
                        content = "不要提工作压力",
                        type = TagType.RISK_RED,
                        source = "MANUAL"
                    ),
                    BrainTag(
                        id = 2,
                        contactId = "1",
                        content = "喜欢聊摄影技巧",
                        type = TagType.STRATEGY_GREEN,
                        source = "MANUAL"
                    )
                )
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
