package com.empathy.ai.presentation.ui.screen.userprofile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empathy.ai.domain.model.ExportFormat
import com.empathy.ai.domain.model.UserProfile
import com.empathy.ai.domain.model.UserProfileDimension
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.viewmodel.UserProfileViewModel

/**
 * 用户画像界面
 */
@Composable
fun UserProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: UserProfileViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            kotlinx.coroutines.delay(3000)
            viewModel.onEvent(UserProfileUiEvent.ClearSuccessMessage)
        }
    }

    UserProfileScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileScreenContent(
    uiState: UserProfileUiState,
    onEvent: (UserProfileUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("个人画像") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(UserProfileUiEvent.ShowExportDialog) }) {
                        Icon(Icons.Default.Share, contentDescription = "导出")
                    }
                    IconButton(onClick = { onEvent(UserProfileUiEvent.ShowResetConfirm) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "重置")
                    }
                }
            )
        },
        snackbarHost = {
            uiState.error?.let { error ->
                Snackbar(
                    action = {
                        TextButton(onClick = { onEvent(UserProfileUiEvent.ClearError) }) {
                            Text("关闭")
                        }
                    }
                ) { Text(error) }
            }
            uiState.successMessage?.let { message ->
                Snackbar { Text(message) }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            val dimensions = AdaptiveDimensions.current
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(dimensions.spacingMedium),
                verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
            ) {
                ProfileCompletenessCard(uiState.completeness, uiState.totalTagCount)

                var selectedTab by remember { mutableIntStateOf(uiState.selectedTabIndex) }
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; onEvent(UserProfileUiEvent.SwitchTab(0)) },
                        text = { Text("基础信息") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; onEvent(UserProfileUiEvent.SwitchTab(1)) },
                        text = { Text("自定义维度") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (selectedTab) {
                    0 -> BaseDimensionsContent(uiState.profile, onEvent)
                    1 -> CustomDimensionsContent(uiState.profile, uiState.canAddCustomDimension, onEvent)
                }
            }
        }

        // Dialogs
        if (uiState.showAddTagDialog) {
            AddTagDialog(
                onConfirm = { tag -> onEvent(UserProfileUiEvent.AddTag(uiState.currentEditDimension ?: "", tag)) },
                onDismiss = { onEvent(UserProfileUiEvent.HideTagDialog) }
            )
        }
        if (uiState.showEditTagDialog) {
            EditTagDialog(
                currentTag = uiState.currentEditTag ?: "",
                onConfirm = { newTag ->
                    onEvent(UserProfileUiEvent.EditTag(uiState.currentEditDimension ?: "", uiState.currentEditTag ?: "", newTag))
                },
                onDelete = { onEvent(UserProfileUiEvent.ShowDeleteTagConfirm(uiState.currentEditDimension ?: "", uiState.currentEditTag ?: "")) },
                onDismiss = { onEvent(UserProfileUiEvent.HideTagDialog) }
            )
        }
        if (uiState.showAddDimensionDialog) {
            AddDimensionDialog(
                onConfirm = { name -> onEvent(UserProfileUiEvent.AddDimension(name)) },
                onDismiss = { onEvent(UserProfileUiEvent.HideDimensionDialog) }
            )
        }
        if (uiState.showDeleteConfirmDialog) {
            DeleteConfirmDialog(
                title = if (uiState.pendingDeleteDimension != null) "删除维度" else "删除标签",
                message = if (uiState.pendingDeleteDimension != null) "确定要删除维度「${uiState.pendingDeleteDimension}」及其所有标签吗？"
                else "确定要删除标签「${uiState.pendingDeleteTag}」吗？",
                onConfirm = {
                    if (uiState.pendingDeleteDimension != null) onEvent(UserProfileUiEvent.ConfirmDeleteDimension)
                    else onEvent(UserProfileUiEvent.ConfirmDeleteTag)
                },
                onDismiss = { onEvent(UserProfileUiEvent.HideTagDialog) }
            )
        }
        if (uiState.showExportDialog) {
            ExportDialog(
                onExport = { format -> onEvent(UserProfileUiEvent.ExportProfile(format)) },
                onDismiss = { onEvent(UserProfileUiEvent.HideExportDialog) }
            )
        }
        if (uiState.showResetConfirmDialog) {
            ResetConfirmDialog(
                onConfirm = { onEvent(UserProfileUiEvent.ConfirmResetProfile) },
                onDismiss = { onEvent(UserProfileUiEvent.HideResetConfirm) }
            )
        }
    }
}


@Composable
private fun ProfileCompletenessCard(completeness: Int, totalTagCount: Int, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(targetValue = completeness / 100f, label = "completeness")
    val dimensions = AdaptiveDimensions.current
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(dimensions.spacingMedium), verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("画像完整度", style = MaterialTheme.typography.titleMedium)
                Text("$completeness%", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth())
            Text("已添加 $totalTagCount 个标签", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BaseDimensionsContent(profile: UserProfile, onEvent: (UserProfileUiEvent) -> Unit, modifier: Modifier = Modifier) {
    val dimensions = AdaptiveDimensions.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(dimensions.spacingMediumSmall)) {
        UserProfileDimension.entries.forEach { dimension ->
            DimensionCard(
                title = dimension.displayName,
                description = dimension.description,
                tags = profile.getTagsForDimension(dimension.name),
                presetTags = dimension.presetTags,
                onAddTag = { onEvent(UserProfileUiEvent.ShowAddTagDialog(dimension.name)) },
                onEditTag = { tag -> onEvent(UserProfileUiEvent.ShowEditTagDialog(dimension.name, tag)) },
                onSelectPresetTag = { tag -> onEvent(UserProfileUiEvent.AddTag(dimension.name, tag)) }
            )
        }
    }
}

@Composable
private fun CustomDimensionsContent(profile: UserProfile, canAddDimension: Boolean, onEvent: (UserProfileUiEvent) -> Unit, modifier: Modifier = Modifier) {
    val dimensions = AdaptiveDimensions.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(dimensions.spacingMediumSmall)) {
        if (canAddDimension) {
            OutlinedButton(onClick = { onEvent(UserProfileUiEvent.ShowAddDimensionDialog) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                Text("添加自定义维度")
            }
        } else {
            Text("已达到自定义维度上限（最多10个）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        profile.customDimensions.forEach { (name, tags) ->
            DimensionCard(
                title = name, description = "自定义维度", tags = tags, presetTags = emptyList(), isCustom = true,
                onAddTag = { onEvent(UserProfileUiEvent.ShowAddTagDialog(name)) },
                onEditTag = { tag -> onEvent(UserProfileUiEvent.ShowEditTagDialog(name, tag)) },
                onSelectPresetTag = {},
                onDeleteDimension = { onEvent(UserProfileUiEvent.ShowDeleteDimensionConfirm(name)) }
            )
        }

        if (profile.customDimensions.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.fillMaxWidth().padding(dimensions.spacingLarge), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(dimensions.iconSizeXLarge), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                    Text("暂无自定义维度", style = MaterialTheme.typography.bodyLarge)
                    Text("点击上方按钮添加您的专属维度", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}


@Composable
private fun DimensionCard(
    title: String, description: String, tags: List<String>, presetTags: List<String>,
    isCustom: Boolean = false, onAddTag: () -> Unit, onEditTag: (String) -> Unit,
    onSelectPresetTag: (String) -> Unit, onDeleteDimension: (() -> Unit)? = null, modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }
    val rotationAngle by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "rotation")
    val dimensions = AdaptiveDimensions.current

    Card(modifier = modifier.fillMaxWidth().animateContentSize()) {
        Column(modifier = Modifier.padding(dimensions.spacingMedium)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    if (isCustom && onDeleteDimension != null) {
                        IconButton(onClick = onDeleteDimension) {
                            Icon(Icons.Default.Delete, contentDescription = "删除维度", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(Icons.Default.ExpandMore, contentDescription = if (expanded) "收起" else "展开", modifier = Modifier.rotate(rotationAngle))
                    }
                }
            }

            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(top = dimensions.spacingMediumSmall), verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)) {
                    if (tags.isNotEmpty()) TagChipGroup(tags = tags, onTagClick = onEditTag)
                    AssistChip(onClick = onAddTag, label = { Text("添加标签") }, leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(dimensions.iconSizeSmall + 2.dp)) })
                    if (presetTags.isNotEmpty()) {
                        Text("快速选择", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)) {
                            items(presetTags.filter { it !in tags }) { tag ->
                                SuggestionChip(onClick = { onSelectPresetTag(tag) }, label = { Text(tag) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagChipGroup(tags: List<String>, onTagClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val dimensions = AdaptiveDimensions.current
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall), verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)) {
        tags.forEach { tag ->
            InputChip(
                selected = false, onClick = { onTagClick(tag) },
                label = { Text(tag, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                trailingIcon = { Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(dimensions.iconSizeSmall)) }
            )
        }
    }
}


// ==================== Dialogs ====================

@Composable
private fun AddTagDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var tagInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加标签") },
        text = {
            OutlinedTextField(
                value = tagInput, onValueChange = { tagInput = it; isError = false },
                label = { Text("标签内容") }, placeholder = { Text("请输入标签") },
                singleLine = true, isError = isError,
                supportingText = if (isError) { { Text("标签不能为空且长度不超过20字符") } } else null,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = tagInput.trim()
                if (trimmed.isNotEmpty() && trimmed.length <= 20) { onConfirm(trimmed); onDismiss() }
                else isError = true
            }) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun EditTagDialog(currentTag: String, onConfirm: (String) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    var tagInput by remember { mutableStateOf(currentTag) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑标签") },
        text = {
            OutlinedTextField(
                value = tagInput, onValueChange = { tagInput = it; isError = false },
                label = { Text("标签内容") }, singleLine = true, isError = isError,
                supportingText = if (isError) { { Text("标签不能为空且长度不超过20字符") } } else null,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Row {
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("删除") }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {
                    val trimmed = tagInput.trim()
                    if (trimmed.isNotEmpty() && trimmed.length <= 20) { onConfirm(trimmed); onDismiss() }
                    else isError = true
                }) { Text("保存") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun AddDimensionDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var nameInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加自定义维度") },
        text = {
            OutlinedTextField(
                value = nameInput, onValueChange = { nameInput = it; isError = false },
                label = { Text("维度名称") }, placeholder = { Text("例如：职业技能、生活习惯") },
                singleLine = true, isError = isError,
                supportingText = if (isError) { { Text(errorMessage) } } else { { Text("2-10个字符") } },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = nameInput.trim()
                when {
                    trimmed.isEmpty() -> { isError = true; errorMessage = "维度名称不能为空" }
                    trimmed.length < 2 -> { isError = true; errorMessage = "维度名称至少2个字符" }
                    trimmed.length > 10 -> { isError = true; errorMessage = "维度名称不超过10个字符" }
                    UserProfileDimension.isBaseDimension(trimmed) -> { isError = true; errorMessage = "不能与基础维度名称重复" }
                    else -> { onConfirm(trimmed); onDismiss() }
                }
            }) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun DeleteConfirmDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("删除") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ExportDialog(onExport: (ExportFormat) -> Unit, onDismiss: () -> Unit) {
    val dimensions = AdaptiveDimensions.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出画像") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)) {
                Text("选择导出格式", style = MaterialTheme.typography.bodyMedium)
                ExportFormat.entries.forEach { format ->
                    OutlinedButton(onClick = { onExport(format); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(imageVector = when (format) {
                            ExportFormat.JSON -> Icons.Default.Code
                            ExportFormat.PLAIN_TEXT -> Icons.Default.Description
                        }, contentDescription = null)
                        Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                        Text(format.displayName)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ResetConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("重置画像") },
        text = { Text("确定要重置所有画像数据吗？此操作不可撤销，所有标签和自定义维度都将被清除。") },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("重置") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}


// ==================== Previews ====================

@Preview(showBackground = true)
@Composable
private fun UserProfileScreenPreview() {
    EmpathyTheme {
        UserProfileScreenContent(
            uiState = UserProfileUiState(
                profile = UserProfile(
                    personalityTraits = listOf("内向", "理性", "细心"),
                    values = listOf("重视家庭", "追求成长"),
                    interests = listOf("阅读", "编程", "旅行"),
                    customDimensions = mapOf("职业技能" to listOf("Kotlin", "Android", "架构设计"))
                ),
                isLoading = false
            ),
            onEvent = {}, onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileCompletenessCardPreview() {
    EmpathyTheme { ProfileCompletenessCard(completeness = 65, totalTagCount = 12) }
}

@Preview(showBackground = true)
@Composable
private fun DimensionCardPreview() {
    EmpathyTheme {
        DimensionCard(
            title = "性格特点", description = "描述你的性格特征",
            tags = listOf("内向", "理性", "细心"), presetTags = listOf("外向", "感性", "乐观", "谨慎"),
            onAddTag = {}, onEditTag = {}, onSelectPresetTag = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyCustomDimensionsPreview() {
    EmpathyTheme { CustomDimensionsContent(profile = UserProfile(), canAddDimension = true, onEvent = {}) }
}
