package com.empathy.ai.presentation.ui.screen.userprofile

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empathy.ai.domain.model.ExportFormat
import com.empathy.ai.domain.model.UserProfile
import com.empathy.ai.domain.model.UserProfileDimension
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBackground
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSGreen
import com.empathy.ai.presentation.theme.iOSOrange
import com.empathy.ai.presentation.theme.iOSPurple
import com.empathy.ai.presentation.theme.iOSRed
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.ui.component.dialog.IOSAlertDialog
import com.empathy.ai.presentation.ui.component.dialog.IOSInputDialog
import com.empathy.ai.presentation.ui.component.ios.DimensionCard
import com.empathy.ai.presentation.ui.component.ios.IOSTabSwitcher
import com.empathy.ai.presentation.ui.component.ios.ProfileCompletionCard
import com.empathy.ai.presentation.viewmodel.UserProfileViewModel

/**
 * 用户画像界面 - iOS风格
 * 
 * 基于PRD-00021设计稿实现，使用iOS原生风格组件
 * 
 * @see PRD-00021 个人画像页面设计规范
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

@Composable
private fun UserProfileScreenContent(
    uiState: UserProfileUiState,
    onEvent: (UserProfileUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    
    // BUG-00053 P2修复：维度展开状态管理
    // 初始化时所有基础维度默认展开，避免空集合导致的逻辑问题
    var expandedDimensions by remember { 
        mutableStateOf(UserProfileDimension.entries.map { it.name }.toSet()) 
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(iOSBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // iOS风格导航栏 - BUG-00037修复：添加重置和保存按钮
            // BUG-00038 P4修复：移除刷新按钮
            IOSUserProfileTopBar(
                onNavigateBack = {
                    if (uiState.hasUnsavedChanges) {
                        onEvent(UserProfileUiEvent.ShowDiscardChangesDialog)
                    } else {
                        onNavigateBack()
                    }
                },
                onShare = { onEvent(UserProfileUiEvent.ShowExportDialog) },
                onReset = { onEvent(UserProfileUiEvent.ShowResetConfirm) },
                onSave = { onEvent(UserProfileUiEvent.SaveAllChanges) },
                hasUnsavedChanges = uiState.hasUnsavedChanges
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { 
                    CircularProgressIndicator(color = iOSBlue) 
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = dimensions.spacingMedium),
                    verticalArrangement = Arrangement.spacedBy(dimensions.spacingMediumSmall)
                ) {
                    Spacer(modifier = Modifier.height(dimensions.spacingMedium))
                    
                    // 画像完整度卡片
                    ProfileCompletionCard(
                        completeness = uiState.completeness,
                        tagCount = uiState.totalTagCount
                    )

                    Spacer(modifier = Modifier.height(dimensions.spacingSmall))

                    // Tab切换器
                    var selectedTab by remember { mutableIntStateOf(uiState.selectedTabIndex) }
                    IOSTabSwitcher(
                        tabs = listOf("基础信息", "自定义维度"),
                        selectedIndex = selectedTab,
                        onTabSelected = { 
                            selectedTab = it
                            onEvent(UserProfileUiEvent.SwitchTab(it)) 
                        }
                    )

                    Spacer(modifier = Modifier.height(dimensions.spacingSmall))

                    // 内容区域
                    when (selectedTab) {
                        0 -> IOSBaseDimensionsContent(
                            uiState = uiState,
                            expandedDimensions = expandedDimensions,
                            onToggleExpand = { dimensionKey ->
                                expandedDimensions = if (dimensionKey in expandedDimensions) {
                                    expandedDimensions - dimensionKey
                                } else {
                                    expandedDimensions + dimensionKey
                                }
                            },
                            onEvent = onEvent
                        )
                        1 -> IOSCustomDimensionsContent(
                            uiState = uiState,
                            canAddDimension = uiState.canAddCustomDimension,
                            expandedDimensions = expandedDimensions,
                            onToggleExpand = { dimensionKey ->
                                expandedDimensions = if (dimensionKey in expandedDimensions) {
                                    expandedDimensions - dimensionKey
                                } else {
                                    expandedDimensions + dimensionKey
                                }
                            },
                            onEvent = onEvent
                        )
                    }

                    // 底部间距
                    Spacer(modifier = Modifier.height(dimensions.spacingXLarge))
                }
            }
        }

        // 错误/成功提示
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(dimensions.spacingMedium),
                action = {
                    TextButton(onClick = { onEvent(UserProfileUiEvent.ClearError) }) {
                        Text("关闭", color = iOSBlue)
                    }
                }
            ) { Text(error) }
        }
        uiState.successMessage?.let { message ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(dimensions.spacingMedium)
            ) { Text(message) }
        }

        // Dialogs
        // BUG-00037: 使用LocalAddTag等本地操作，避免每次操作都刷新列表
        if (uiState.showAddTagDialog) {
            IOSAddTagDialog(
                onConfirm = { tag -> onEvent(UserProfileUiEvent.LocalAddTag(uiState.currentEditDimension ?: "", tag)) },
                onDismiss = { onEvent(UserProfileUiEvent.HideTagDialog) }
            )
        }
        if (uiState.showEditTagDialog) {
            IOSEditTagDialog(
                currentTag = uiState.currentEditTag ?: "",
                onConfirm = { newTag ->
                    onEvent(UserProfileUiEvent.LocalEditTag(uiState.currentEditDimension ?: "", uiState.currentEditTag ?: "", newTag))
                },
                onDelete = { onEvent(UserProfileUiEvent.LocalDeleteTag(uiState.currentEditDimension ?: "", uiState.currentEditTag ?: "")) },
                onDismiss = { onEvent(UserProfileUiEvent.HideTagDialog) }
            )
        }
        if (uiState.showAddDimensionDialog) {
            IOSAddDimensionDialog(
                onConfirm = { name -> onEvent(UserProfileUiEvent.AddDimension(name)) },
                onDismiss = { onEvent(UserProfileUiEvent.HideDimensionDialog) }
            )
        }
        if (uiState.showDeleteConfirmDialog) {
            IOSDeleteConfirmDialog(
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
            IOSExportDialog(
                onExport = { format -> onEvent(UserProfileUiEvent.ExportProfile(format)) },
                onDismiss = { onEvent(UserProfileUiEvent.HideExportDialog) }
            )
        }
        if (uiState.showResetConfirmDialog) {
            IOSResetConfirmDialog(
                onConfirm = { onEvent(UserProfileUiEvent.ConfirmResetProfile) },
                onDismiss = { onEvent(UserProfileUiEvent.HideResetConfirm) }
            )
        }
        
        // BUG-00037: 放弃编辑确认对话框
        if (uiState.showDiscardChangesDialog) {
            IOSAlertDialog(
                title = "放弃编辑",
                message = "您有未保存的更改，确定要放弃吗？",
                confirmText = "放弃",
                dismissText = "继续编辑",
                onConfirm = { 
                    onEvent(UserProfileUiEvent.ConfirmDiscardChanges)
                    onNavigateBack()
                },
                onDismiss = { onEvent(UserProfileUiEvent.HideDiscardChangesDialog) },
                isDestructive = true,
                showDismissButton = true
            )
        }
    }
}


// ============================================================
// iOS风格导航栏
// ============================================================

/**
 * iOS风格顶部导航栏
 * 
 * 设计规格:
 * - 背景: 白色/95%透明度 + 模糊效果
 * - 返回按钮: iOS蓝色 chevron_left
 * - 标题: 17sp, SemiBold
 * - 右侧按钮: 重置 + 分享（BUG-00038 P4修复：移除刷新按钮）
 * 
 * BUG-00037 修复: 添加重置按钮和保存按钮（编辑模式）
 * BUG-00038 P4修复: 移除刷新按钮（本地编辑模式下不需要）
 */
@Composable
private fun IOSUserProfileTopBar(
    onNavigateBack: () -> Unit,
    onShare: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit = {},
    hasUnsavedChanges: Boolean = false,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = iOSCardBackground.copy(alpha = 0.95f),
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = dimensions.spacingMedium, vertical = dimensions.spacingMediumSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧: 返回按钮 + 标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(dimensions.iconSizeLarge + 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = iOSBlue,
                        modifier = Modifier.size(dimensions.iconSizeLarge)
                    )
                }
                Text(
                    text = "个人画像",
                    fontSize = dimensions.fontSizeTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSTextPrimary
                )
            }

            // 右侧按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
            ) {
                // 有未保存变更时显示保存按钮
                if (hasUnsavedChanges) {
                    TextButton(onClick = onSave) {
                        Text(
                            text = "保存",
                            color = iOSBlue,
                            fontSize = dimensions.fontSizeTitle,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    // 重置按钮 - BUG-00037 P2修复
                    IconButton(onClick = onReset) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "重置",
                            tint = iOSRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    // 分享按钮
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "分享",
                            tint = iOSBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    // BUG-00038 P4修复：移除刷新按钮（本地编辑模式下不需要）
                }
            }
        }
    }
}

// ============================================================
// 基础维度内容
// ============================================================

/**
 * 基础维度内容 - iOS风格
 * 
 * 使用DimensionCard组件展示各个维度
 * 
 * BUG-00037 修复：使用uiState.getTagsForDimension()获取标签（包含待保存的变更）
 * 使用LocalAddTag等本地操作事件，避免每次操作都刷新列表
 * BUG-00053 P2修复：修复闭包捕获问题和默认展开逻辑
 */
@Composable
private fun IOSBaseDimensionsContent(
    uiState: UserProfileUiState,
    expandedDimensions: Set<String>,
    onToggleExpand: (String) -> Unit,
    onEvent: (UserProfileUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingMediumSmall)
    ) {
        UserProfileDimension.entries.forEach { dimension ->
            // BUG-00053 P2修复：捕获当前迭代值，避免闭包捕获问题
            val currentDimensionName = dimension.name
            val dimensionIcon = getDimensionIcon(dimension)
            val dimensionColor = getDimensionColor(dimension)
            // BUG-00037: 使用uiState.getTagsForDimension()获取标签（包含待保存的变更）
            val currentTags = uiState.getTagsForDimension(currentDimensionName)
            
            DimensionCard(
                icon = dimensionIcon,
                iconBackgroundColor = dimensionColor,
                title = dimension.displayName,
                description = dimension.description,
                tags = currentTags,
                presetTags = dimension.presetTags.filter { it !in currentTags },
                // BUG-00053 P2修复：移除 expandedDimensions.isEmpty() 的默认展开逻辑
                isExpanded = currentDimensionName in expandedDimensions,
                // BUG-00053 P2修复：使用捕获的当前值
                onToggleExpand = { onToggleExpand(currentDimensionName) },
                onAddTag = { onEvent(UserProfileUiEvent.ShowAddTagDialog(currentDimensionName)) },
                onEditTag = { tag -> onEvent(UserProfileUiEvent.ShowEditTagDialog(currentDimensionName, tag)) },
                // BUG-00037: 使用LocalAddTag本地操作，避免每次操作都刷新列表
                onSelectPresetTag = { tag -> onEvent(UserProfileUiEvent.LocalAddTag(currentDimensionName, tag)) }
            )
        }
    }
}

/**
 * 获取维度图标
 */
private fun getDimensionIcon(dimension: UserProfileDimension): ImageVector {
    return when (dimension) {
        UserProfileDimension.PERSONALITY_TRAITS -> Icons.Default.Psychology
        UserProfileDimension.VALUES -> Icons.Default.Diamond
        UserProfileDimension.INTERESTS -> Icons.Default.Palette
        UserProfileDimension.COMMUNICATION_STYLE -> Icons.AutoMirrored.Filled.Chat
        UserProfileDimension.SOCIAL_PREFERENCES -> Icons.Default.Groups
    }
}

/**
 * 获取维度颜色
 */
private fun getDimensionColor(dimension: UserProfileDimension): Color {
    return when (dimension) {
        UserProfileDimension.PERSONALITY_TRAITS -> iOSPurple
        UserProfileDimension.VALUES -> iOSBlue
        UserProfileDimension.INTERESTS -> iOSGreen
        UserProfileDimension.COMMUNICATION_STYLE -> iOSOrange
        UserProfileDimension.SOCIAL_PREFERENCES -> iOSRed
    }
}

// ============================================================
// 自定义维度内容
// ============================================================

/**
 * 自定义维度内容 - iOS风格
 * 
 * BUG-00037 修复：使用uiState获取自定义维度标签（包含待保存的变更）
 * BUG-00053 P2修复：修复闭包捕获问题和默认展开逻辑
 */
@Composable
private fun IOSCustomDimensionsContent(
    uiState: UserProfileUiState,
    canAddDimension: Boolean,
    expandedDimensions: Set<String>,
    onToggleExpand: (String) -> Unit,
    onEvent: (UserProfileUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    val profile = uiState.profile
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingMediumSmall)
    ) {
        // 添加自定义维度按钮
        if (canAddDimension) {
            IOSAddDimensionButton(
                onClick = { onEvent(UserProfileUiEvent.ShowAddDimensionDialog) }
            )
        } else {
            Text(
                text = "已达到自定义维度上限（最多10个）",
                fontSize = dimensions.fontSizeCaption,
                color = iOSRed,
                modifier = Modifier.padding(vertical = dimensions.spacingSmall)
            )
        }

        // 自定义维度列表
        // BUG-00037: 使用uiState.getCustomDimensionTags()获取标签（包含待保存的变更）
        profile.customDimensions.forEach { (name, _) ->
            // BUG-00053 P2修复：捕获当前迭代值，避免闭包捕获问题
            val currentDimensionName = name
            val currentTags = uiState.getCustomDimensionTags(currentDimensionName)
            IOSCustomDimensionCard(
                name = currentDimensionName,
                tags = currentTags,
                // BUG-00053 P2修复：移除 expandedDimensions.isEmpty() 的默认展开逻辑
                isExpanded = currentDimensionName in expandedDimensions,
                // BUG-00053 P2修复：使用捕获的当前值
                onToggleExpand = { onToggleExpand(currentDimensionName) },
                onAddTag = { onEvent(UserProfileUiEvent.ShowAddTagDialog(currentDimensionName)) },
                onEditTag = { tag -> onEvent(UserProfileUiEvent.ShowEditTagDialog(currentDimensionName, tag)) },
                onDeleteDimension = { onEvent(UserProfileUiEvent.ShowDeleteDimensionConfirm(currentDimensionName)) }
            )
        }

        // 空状态
        if (profile.customDimensions.isEmpty()) {
            IOSEmptyCustomDimensionCard()
        }
    }
}

/**
 * iOS风格添加维度按钮
 */
@Composable
private fun IOSAddDimensionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensions.cornerRadiusMedium))
            .background(iOSCardBackground)
            .clickable(onClick = onClick)
            .padding(dimensions.spacingMedium),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = iOSBlue,
            modifier = Modifier.size(dimensions.iconSizeSmall + 4.dp)
        )
        Spacer(modifier = Modifier.width(dimensions.spacingSmall))
        Text(
            text = "添加自定义维度",
            fontSize = dimensions.fontSizeSubtitle,
            color = iOSBlue,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * iOS风格自定义维度卡片
 */
@Composable
private fun IOSCustomDimensionCard(
    name: String,
    tags: List<String>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddTag: () -> Unit,
    onEditTag: (String) -> Unit,
    onDeleteDimension: () -> Unit,
    modifier: Modifier = Modifier
) {
    DimensionCard(
        icon = Icons.Default.Category,
        iconBackgroundColor = iOSPurple,
        title = name,
        description = "自定义维度",
        tags = tags,
        presetTags = emptyList(),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        onAddTag = onAddTag,
        onEditTag = onEditTag,
        onSelectPresetTag = {},
        modifier = modifier
    )
}

/**
 * iOS风格空自定义维度卡片
 */
@Composable
private fun IOSEmptyCustomDimensionCard(
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensions.cornerRadiusMedium))
            .background(iOSCardBackground)
            .padding(dimensions.spacingLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Category,
            contentDescription = null,
            modifier = Modifier.size(dimensions.iconSizeXLarge),
            tint = iOSTextSecondary
        )
        Spacer(modifier = Modifier.height(dimensions.spacingSmall))
        Text(
            text = "暂无自定义维度",
            fontSize = dimensions.fontSizeSubtitle,
            color = iOSTextPrimary
        )
        Text(
            text = "点击上方按钮添加您的专属维度",
            fontSize = dimensions.fontSizeCaption,
            color = iOSTextSecondary
        )
    }
}


// ============================================================
// iOS风格对话框
// ============================================================

/**
 * iOS风格添加标签对话框
 * 
 * BUG-00036 修复：迁移到真正的iOS风格对话框
 */
@Composable
private fun IOSAddTagDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tagInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val dimensions = AdaptiveDimensions.current

    IOSInputDialog(
        title = "添加标签",
        content = {
            OutlinedTextField(
                value = tagInput,
                onValueChange = { tagInput = it; isError = false },
                label = { Text("标签内容") },
                placeholder = { Text("请输入标签") },
                singleLine = true,
                isError = isError,
                supportingText = if (isError) { 
                    { Text("标签不能为空且长度不超过20字符", color = iOSRed, fontSize = dimensions.fontSizeCaption) } 
                } else null,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = iOSBlue,
                    cursorColor = iOSBlue
                )
            )
        },
        confirmText = "添加",
        onConfirm = {
            val trimmed = tagInput.trim()
            if (trimmed.isNotEmpty() && trimmed.length <= 20) { 
                onConfirm(trimmed)
                onDismiss() 
            } else {
                isError = true
            }
        },
        onDismiss = onDismiss,
        confirmEnabled = tagInput.trim().isNotEmpty() && tagInput.trim().length <= 20
    )
}

/**
 * iOS风格编辑标签对话框
 * 
 * BUG-00036 修复：迁移到真正的iOS风格对话框
 */
@Composable
private fun IOSEditTagDialog(
    currentTag: String,
    onConfirm: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var tagInput by remember { mutableStateOf(currentTag) }
    var isError by remember { mutableStateOf(false) }
    val dimensions = AdaptiveDimensions.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .padding(dimensions.spacingMedium),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.98f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Text(
                    text = "编辑标签",
                    fontSize = dimensions.fontSizeTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.padding(
                        top = dimensions.spacingLarge,
                        start = dimensions.spacingMedium,
                        end = dimensions.spacingMedium
                    )
                )
                
                // 输入框
                Box(
                    modifier = Modifier.padding(
                        horizontal = dimensions.spacingMedium,
                        vertical = dimensions.spacingMedium
                    )
                ) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it; isError = false },
                        label = { Text("标签内容") },
                        singleLine = true,
                        isError = isError,
                        supportingText = if (isError) { 
                            { Text("标签不能为空且长度不超过20字符", color = iOSRed, fontSize = dimensions.fontSizeCaption) } 
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = iOSBlue,
                            cursorColor = iOSBlue
                        )
                    )
                }
                
                // 分隔线
                HorizontalDivider(
                    color = Color.Black.copy(alpha = 0.1f),
                    thickness = 0.5.dp
                )
                
                // 三按钮区域：删除 | 取消 | 保存
                Row(modifier = Modifier.fillMaxWidth()) {
                    // 删除按钮
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RectangleShape
                    ) {
                        Text(
                            text = "删除",
                            fontSize = dimensions.fontSizeTitle,
                            color = iOSRed
                        )
                    }
                    
                    // 垂直分隔线
                    Box(
                        modifier = Modifier
                            .width(0.5.dp)
                            .height(44.dp)
                            .background(Color.Black.copy(alpha = 0.1f))
                    )
                    
                    // 取消按钮
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RectangleShape
                    ) {
                        Text(
                            text = "取消",
                            fontSize = dimensions.fontSizeTitle,
                            color = iOSBlue
                        )
                    }
                    
                    // 垂直分隔线
                    Box(
                        modifier = Modifier
                            .width(0.5.dp)
                            .height(44.dp)
                            .background(Color.Black.copy(alpha = 0.1f))
                    )
                    
                    // 保存按钮
                    val canSave = tagInput.trim().isNotEmpty() && tagInput.trim().length <= 20
                    TextButton(
                        onClick = {
                            val trimmed = tagInput.trim()
                            if (trimmed.isNotEmpty() && trimmed.length <= 20) { 
                                onConfirm(trimmed)
                                onDismiss() 
                            } else {
                                isError = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RectangleShape,
                        enabled = canSave
                    ) {
                        Text(
                            text = "保存",
                            fontSize = dimensions.fontSizeTitle,
                            color = if (canSave) iOSBlue else iOSBlue.copy(alpha = 0.4f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * iOS风格添加维度对话框
 * 
 * BUG-00036 修复：迁移到真正的iOS风格对话框
 */
@Composable
private fun IOSAddDimensionDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val dimensions = AdaptiveDimensions.current

    IOSInputDialog(
        title = "添加自定义维度",
        content = {
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it; isError = false },
                label = { Text("维度名称") },
                placeholder = { Text("例如：职业技能、生活习惯") },
                singleLine = true,
                isError = isError,
                supportingText = if (isError) { 
                    { Text(errorMessage, color = iOSRed, fontSize = dimensions.fontSizeCaption) } 
                } else { 
                    { Text("2-10个字符", color = iOSTextSecondary, fontSize = dimensions.fontSizeCaption) } 
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = iOSBlue,
                    cursorColor = iOSBlue
                )
            )
        },
        confirmText = "添加",
        onConfirm = {
            val trimmed = nameInput.trim()
            when {
                trimmed.isEmpty() -> { isError = true; errorMessage = "维度名称不能为空" }
                trimmed.length < 2 -> { isError = true; errorMessage = "维度名称至少2个字符" }
                trimmed.length > 10 -> { isError = true; errorMessage = "维度名称不超过10个字符" }
                UserProfileDimension.isBaseDimension(trimmed) -> { isError = true; errorMessage = "不能与基础维度名称重复" }
                else -> { onConfirm(trimmed); onDismiss() }
            }
        },
        onDismiss = onDismiss,
        confirmEnabled = nameInput.trim().length in 2..10 && !UserProfileDimension.isBaseDimension(nameInput.trim())
    )
}

/**
 * iOS风格删除确认对话框
 * 
 * BUG-00036 修复：迁移到真正的iOS风格对话框
 */
@Composable
private fun IOSDeleteConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    IOSAlertDialog(
        title = title,
        message = message,
        confirmText = "删除",
        dismissText = "取消",
        onConfirm = { onConfirm(); onDismiss() },
        onDismiss = onDismiss,
        isDestructive = true,
        showDismissButton = true
    )
}

/**
 * iOS风格导出对话框
 * 
 * BUG-00036 修复：迁移到真正的iOS风格对话框
 */
@Composable
private fun IOSExportDialog(
    onExport: (ExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    val dimensions = AdaptiveDimensions.current
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 270.dp)
                .padding(dimensions.spacingMedium),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.98f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Text(
                    text = "导出画像",
                    fontSize = dimensions.fontSizeTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.padding(
                        top = dimensions.spacingLarge,
                        start = dimensions.spacingMedium,
                        end = dimensions.spacingMedium
                    )
                )
                
                // 选项列表
                Column(
                    modifier = Modifier.padding(
                        horizontal = dimensions.spacingMedium,
                        vertical = dimensions.spacingMedium
                    ),
                    verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
                ) {
                    Text(
                        "选择导出格式", 
                        fontSize = dimensions.fontSizeCaption,
                        color = Color.Black.copy(alpha = 0.5f)
                    )
                    ExportFormat.entries.forEach { format ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(dimensions.cornerRadiusSmall))
                                .clickable { onExport(format); onDismiss() }
                                .padding(dimensions.spacingMediumSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (format) {
                                    ExportFormat.JSON -> Icons.Default.Code
                                    ExportFormat.PLAIN_TEXT -> Icons.Default.Description
                                },
                                contentDescription = null,
                                tint = iOSBlue
                            )
                            Spacer(modifier = Modifier.width(dimensions.spacingMediumSmall))
                            Text(
                                format.displayName,
                                color = Color.Black
                            )
                        }
                    }
                }
                
                // 分隔线
                HorizontalDivider(
                    color = Color.Black.copy(alpha = 0.1f),
                    thickness = 0.5.dp
                )
                
                // 取消按钮
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RectangleShape
                ) {
                    Text(
                        text = "取消",
                        fontSize = dimensions.fontSizeTitle,
                        color = iOSBlue
                    )
                }
            }
        }
    }
}

/**
 * iOS风格重置确认对话框
 * 
 * BUG-00036 修复：迁移到真正的iOS风格对话框
 */
@Composable
private fun IOSResetConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    IOSAlertDialog(
        title = "重置画像",
        message = "确定要重置所有画像数据吗？此操作不可撤销，所有标签和自定义维度都将被清除。",
        confirmText = "重置",
        dismissText = "取消",
        onConfirm = { onConfirm(); onDismiss() },
        onDismiss = onDismiss,
        isDestructive = true,
        showDismissButton = true
    )
}

// ============================================================
// 预览函数
// ============================================================

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
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IOSUserProfileTopBarPreview() {
    EmpathyTheme {
        IOSUserProfileTopBar(
            onNavigateBack = {},
            onShare = {},
            onReset = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IOSEmptyCustomDimensionCardPreview() {
    EmpathyTheme {
        IOSEmptyCustomDimensionCard(
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IOSAddDimensionButtonPreview() {
    EmpathyTheme {
        IOSAddDimensionButton(
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
