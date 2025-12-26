package com.empathy.ai.presentation.ui.screen.contact

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.presentation.theme.AppSpacing
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.button.PrimaryButton
import com.empathy.ai.presentation.ui.component.button.SecondaryButton
import com.empathy.ai.presentation.ui.component.card.ProfileCard
import com.empathy.ai.presentation.ui.component.input.CustomTextField
import com.empathy.ai.presentation.ui.component.relationship.FactList
import com.empathy.ai.presentation.ui.component.relationship.RelationshipScoreSection
import com.empathy.ai.presentation.ui.component.dialog.AddFactToStreamDialog
import com.empathy.ai.presentation.ui.component.state.FriendlyErrorCard
import com.empathy.ai.presentation.ui.component.state.LoadingIndicatorFullScreen
import com.empathy.ai.presentation.util.UserFriendlyError
import androidx.compose.material.icons.filled.Warning
import com.empathy.ai.presentation.viewmodel.ContactDetailViewModel
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailUiEvent
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailUiState

/**
 * 联系人详情页面
 *
 * 功能：
 * - 查看联系人详细信息
 * - 编辑联系人信息
 * - 管理脑标签（添加/删除）
 * - 保存修改
 *
 * @param contactId 联系人ID，空字符串表示新建联系人
 * @param onNavigateBack 返回回调
 * @param viewModel 联系人详情ViewModel
 * @param modifier Modifier
 */
@Composable
fun ContactDetailScreen(
    contactId: String,
    onNavigateBack: () -> Unit,
    viewModel: ContactDetailViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 加载联系人数据
    LaunchedEffect(contactId) {
        viewModel.onEvent(ContactDetailUiEvent.LoadContact(contactId))
        if (contactId.isNotBlank()) {
            viewModel.onEvent(ContactDetailUiEvent.LoadRelationshipData(contactId))
        }
    }

    // 监听保存成功，自动返回
    LaunchedEffect(uiState.shouldNavigateBack) {
        if (uiState.shouldNavigateBack) {
            onNavigateBack()
        }
    }

    ContactDetailScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

/**
 * 联系人详情页面内容（无状态）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactDetailScreenContent(
    uiState: ContactDetailUiState,
    onEvent: (ContactDetailUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNewContact) "新建联系人" else "联系人详情") },
                navigationIcon = {
                    IconButton(onClick = { onEvent(ContactDetailUiEvent.NavigateBack) }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 【TD-00016】主题设置图标
                    if (!uiState.isNewContact) {
                        IconButton(onClick = { onEvent(ContactDetailUiEvent.ShowTopicDialog) }) {
                            Icon(
                                imageVector = Icons.Default.Topic,
                                contentDescription = "设置对话主题"
                            )
                        }
                    }
                    if (!uiState.isEditMode && !uiState.isNewContact) {
                        IconButton(onClick = { onEvent(ContactDetailUiEvent.StartEdit) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "编辑"
                            )
                        }
                    }
                    if (uiState.isEditMode) {
                        IconButton(
                            onClick = { onEvent(ContactDetailUiEvent.SaveContact) },
                            enabled = uiState.canSave
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "保存"
                            )
                        }
                    }
                    if (!uiState.isNewContact) {
                        IconButton(onClick = { onEvent(ContactDetailUiEvent.ShowDeleteConfirmDialog) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除"
                            )
                        }
                    }
                }
            )
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
                        message = "加载联系人信息..."
                    )
                }
                uiState.error != null -> {
                    FriendlyErrorCard(
                        error = UserFriendlyError(
                            title = "出错了",
                            message = uiState.error ?: "未知错误",
                            icon = Icons.Default.Warning
                        ),
                        onAction = { onEvent(ContactDetailUiEvent.ReloadContact) }
                    )
                }
                else -> {
                    ContactDetailContent(
                        uiState = uiState,
                        onEvent = onEvent
                    )
                }
            }

            // 保存中的加载指示器
            if (uiState.isSaving) {
                LoadingIndicatorFullScreen(
                    message = "保存中..."
                )
            }
        }
    }

    // 删除确认对话框
    if (uiState.showDeleteConfirmDialog) {
        DeleteConfirmDialog(
            onConfirm = {
                onEvent(ContactDetailUiEvent.DeleteContact)
                onEvent(ContactDetailUiEvent.HideDeleteConfirmDialog)
            },
            onDismiss = { onEvent(ContactDetailUiEvent.HideDeleteConfirmDialog) }
        )
    }

    // 未保存更改对话框
    if (uiState.showUnsavedChangesDialog) {
        UnsavedChangesDialog(
            onSave = { onEvent(ContactDetailUiEvent.SaveContact) },
            onDiscard = {
                onEvent(ContactDetailUiEvent.HideUnsavedChangesDialog)
                onEvent(ContactDetailUiEvent.ConfirmNavigateBack)
            },
            onCancel = { onEvent(ContactDetailUiEvent.HideUnsavedChangesDialog) }
        )
    }

    // BUG-00017修复：移除脑标签功能，只保留事实录入
    // 添加事实对话框 - 使用iOS风格底部弹窗
    if (uiState.showAddFactDialog) {
        com.empathy.ai.presentation.ui.component.dialog.IOSAddFactBottomSheet(
            onDismiss = { onEvent(ContactDetailUiEvent.HideAddFactDialog) },
            onConfirm = { key, value ->
                onEvent(ContactDetailUiEvent.AddFact(key, value))
                onEvent(ContactDetailUiEvent.HideAddFactDialog)
            }
        )
    }

    // 【TD-00016】对话主题设置对话框
    // 注意：主题功能使用独立的TopicViewModel管理状态
    // 这里只是显示对话框的入口，实际的状态管理由TopicViewModel处理
    if (uiState.showTopicDialog) {
        // 使用TopicViewModel的状态和事件
        val topicViewModel: com.empathy.ai.presentation.viewmodel.TopicViewModel = 
            androidx.hilt.navigation.compose.hiltViewModel()
        val topicUiState by topicViewModel.uiState.collectAsStateWithLifecycle()
        
        // 初始化时加载主题数据
        LaunchedEffect(uiState.contactId) {
            topicViewModel.loadTopic(uiState.contactId)
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
 * 联系人详情内容
 */
@Composable
private fun ContactDetailContent(
    uiState: ContactDetailUiState,
    onEvent: (ContactDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
    ) {
        // 如果是查看模式，显示ProfileCard
        if (!uiState.isEditMode && uiState.originalProfile != null) {
            item {
                ProfileCard(
                    contact = uiState.originalProfile,
                    onEdit = { onEvent(ContactDetailUiEvent.StartEdit) }
                )
            }

            // 关系进展展示（阶段6新增）
            item {
                RelationshipScoreSection(
                    score = uiState.relationshipScore,
                    level = uiState.relationshipLevel,
                    trend = uiState.relationshipTrend,
                    lastInteractionDate = uiState.lastInteractionDate
                )
            }

            // Facts列表展示（阶段6新增）
            if (uiState.facts.isNotEmpty()) {
                item {
                    Text(
                        text = "画像记录",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    FactList(
                        facts = uiState.facts,
                        isEditMode = false
                    )
                }
            }
        }

        // 如果是编辑模式，显示表单
        if (uiState.isEditMode) {
            item {
                Text(
                    text = "基本信息",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                CustomTextField(
                    value = uiState.name,
                    onValueChange = { onEvent(ContactDetailUiEvent.UpdateName(it)) },
                    label = "姓名",
                    placeholder = "请输入联系人姓名",
                    isError = uiState.nameError != null,
                    errorMessage = uiState.nameError,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                CustomTextField(
                    value = uiState.targetGoal,
                    onValueChange = { onEvent(ContactDetailUiEvent.UpdateTargetGoal(it)) },
                    label = "沟通目标",
                    placeholder = "请输入与此联系人的沟通目标",
                    isError = uiState.targetGoalError != null,
                    errorMessage = uiState.targetGoalError,
                    singleLine = false,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 上下文深度已移除，使用默认值10
            // 事实和标签在联系人创建后通过事实流添加
        }

        // BUG-00017修复：移除脑标签UI，只保留事实录入功能
        // 事实部分（统一使用Fact类型，与联系人画像系统保持一致）
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "画像事实",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (uiState.isEditMode) {
                    TextButton(onClick = { onEvent(ContactDetailUiEvent.ShowAddFactDialog) }) {
                        Text("添加事实")
                    }
                }
            }
        }

        if (uiState.facts.isNotEmpty()) {
            item {
                FactList(
                    facts = uiState.facts,
                    isEditMode = uiState.isEditMode,
                    onDeleteFact = { fact ->
                        onEvent(ContactDetailUiEvent.DeleteFactItem(fact))
                    }
                )
            }
        } else {
            item {
                Text(
                    text = "还没有画像事实",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 编辑模式下的操作按钮
        if (uiState.isEditMode) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    SecondaryButton(
                        text = "取消",
                        onClick = { onEvent(ContactDetailUiEvent.CancelEdit) },
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = "保存",
                        onClick = { onEvent(ContactDetailUiEvent.SaveContact) },
                        enabled = uiState.canSave,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 删除确认对话框
 */
@Composable
private fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认删除") },
        text = { Text("确定要删除这个联系人吗？此操作无法撤销。") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除", color = MaterialTheme.colorScheme.error)
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
 * 未保存更改对话框
 */
@Composable
private fun UnsavedChangesDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("未保存的更改") },
        text = { Text("您有未保存的更改，是否保存？") },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("保存")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscard) {
                    Text("放弃", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onCancel) {
                    Text("取消")
                }
            }
        }
    )
}

// ==================== Previews ====================

@Preview(name = "联系人详情 - 查看模式", showBackground = true)
@Composable
private fun ContactDetailScreenViewPreview() {
    EmpathyTheme {
        ContactDetailScreenContent(
            uiState = ContactDetailUiState(
                contactId = "1",
                originalProfile = ContactProfile(
                    id = "1",
                    name = "张三",
                    targetGoal = "建立良好的合作关系，促进项目顺利进行",
                    contextDepth = 10,
                    facts = emptyList()
                ),
                name = "张三",
                targetGoal = "建立良好的合作关系，促进项目顺利进行",
                contextDepth = 10,
                facts = emptyList()
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "联系人详情 - 编辑模式", showBackground = true)
@Composable
private fun ContactDetailScreenEditPreview() {
    EmpathyTheme {
        ContactDetailScreenContent(
            uiState = ContactDetailUiState(
                contactId = "1",
                isEditMode = true,
                name = "张三",
                targetGoal = "建立良好的合作关系",
                contextDepth = 10
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "联系人详情 - 新建", showBackground = true)
@Composable
private fun ContactDetailScreenNewPreview() {
    EmpathyTheme {
        ContactDetailScreenContent(
            uiState = ContactDetailUiState(
                isEditMode = true,
                name = "",
                targetGoal = "",
                contextDepth = 10
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "联系人详情 - 加载中", showBackground = true)
@Composable
private fun ContactDetailScreenLoadingPreview() {
    EmpathyTheme {
        ContactDetailScreenContent(
            uiState = ContactDetailUiState(isLoading = true),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(
    name = "联系人详情 - 深色模式",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ContactDetailScreenDarkPreview() {
    EmpathyTheme {
        ContactDetailScreenContent(
            uiState = ContactDetailUiState(
                contactId = "1",
                originalProfile = ContactProfile(
                    id = "1",
                    name = "张三",
                    targetGoal = "建立良好的合作关系",
                    contextDepth = 10,
                    facts = emptyList()
                ),
                name = "张三",
                targetGoal = "建立良好的合作关系",
                contextDepth = 10
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}



