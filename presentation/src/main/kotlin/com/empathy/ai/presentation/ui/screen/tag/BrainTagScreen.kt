package com.empathy.ai.presentation.ui.screen.tag

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
 * 功能：
 * - 显示所有标签列表
 * - 支持添加新标签
 * - 支持编辑标签
 * - 支持删除标签
 * - 按类型分组显示（雷区/策略）
 * - 支持搜索过滤
 *
 * @param onNavigateBack 返回回调
 * @param modifier Modifier
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
                            imageVector = Icons.Default.ArrowBack,
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
                        onDeleteTag = { tagId -> onEvent(BrainTagUiEvent.DeleteTag(tagId)) }
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

    // 错误提示
    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { onEvent(BrainTagUiEvent.ClearError) },
            title = { Text("错误") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { onEvent(BrainTagUiEvent.ClearError) }) {
                    Text("确定")
                }
            }
        )
    }
}

/**
 * 标签列表
 */
@Composable
private fun TagList(
    tags: List<BrainTag>,
    searchQuery: String,
    onDeleteTag: (Long) -> Unit,
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
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 添加标签对话框
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加标签") },
        text = {
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
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = tagContent.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
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
