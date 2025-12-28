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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    
    // 维度展开状态管理
    var expandedDimensions by remember { mutableStateOf(setOf<String>()) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(iOSBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // iOS风格导航栏
            IOSUserProfileTopBar(
                onNavigateBack = onNavigateBack,
                onShare = { onEvent(UserProfileUiEvent.ShowExportDialog) },
                onRefresh = { onEvent(UserProfileUiEvent.RefreshProfile) }
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
                            profile = uiState.profile,
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
                            profile = uiState.profile,
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
        if (uiState.showAddTagDialog) {
            IOSAddTagDialog(
                onConfirm = { tag -> onEvent(UserProfileUiEvent.AddTag(uiState.currentEditDimension ?: "", tag)) },
                onDismiss = { onEvent(UserProfileUiEvent.HideTagDialog) }
            )
        }
        if (uiState.showEditTagDialog) {
            IOSEditTagDialog(
                currentTag = uiState.currentEditTag ?: "",
                onConfirm = { newTag ->
                    onEvent(UserProfileUiEvent.EditTag(uiState.currentEditDimension ?: "", uiState.currentEditTag ?: "", newTag))
                },
                onDelete = { onEvent(UserProfileUiEvent.ShowDeleteTagConfirm(uiState.currentEditDimension ?: "", uiState.currentEditTag ?: "")) },
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
 * - 右侧按钮: 分享 + 刷新
 */
@Composable
private fun IOSUserProfileTopBar(
    onNavigateBack: () -> Unit,
    onShare: () -> Unit,
    onRefresh: () -> Unit,
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
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSTextPrimary
                )
            }

            // 右侧: 分享 + 刷新按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
            ) {
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "分享",
                        tint = iOSBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新",
                        tint = iOSBlue,
                        modifier = Modifier.size(22.dp)
                    )
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
 */
@Composable
private fun IOSBaseDimensionsContent(
    profile: UserProfile,
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
            val dimensionIcon = getDimensionIcon(dimension)
            val dimensionColor = getDimensionColor(dimension)
            
            DimensionCard(
                icon = dimensionIcon,
                iconBackgroundColor = dimensionColor,
                title = dimension.displayName,
                description = dimension.description,
                tags = profile.getTagsForDimension(dimension.name),
                presetTags = dimension.presetTags.filter { it !in profile.getTagsForDimension(dimension.name) },
                isExpanded = dimension.name in expandedDimensions || expandedDimensions.isEmpty(),
                onToggleExpand = { onToggleExpand(dimension.name) },
                onAddTag = { onEvent(UserProfileUiEvent.ShowAddTagDialog(dimension.name)) },
                onEditTag = { tag -> onEvent(UserProfileUiEvent.ShowEditTagDialog(dimension.name, tag)) },
                onSelectPresetTag = { tag -> onEvent(UserProfileUiEvent.AddTag(dimension.name, tag)) }
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
        UserProfileDimension.COMMUNICATION_STYLE -> Icons.Default.Chat
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
 */
@Composable
private fun IOSCustomDimensionsContent(
    profile: UserProfile,
    canAddDimension: Boolean,
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
        // 添加自定义维度按钮
        if (canAddDimension) {
            IOSAddDimensionButton(
                onClick = { onEvent(UserProfileUiEvent.ShowAddDimensionDialog) }
            )
        } else {
            Text(
                text = "已达到自定义维度上限（最多10个）",
                fontSize = 13.sp,
                color = iOSRed,
                modifier = Modifier.padding(vertical = dimensions.spacingSmall)
            )
        }

        // 自定义维度列表
        profile.customDimensions.forEach { (name, tags) ->
            IOSCustomDimensionCard(
                name = name,
                tags = tags,
                isExpanded = name in expandedDimensions || expandedDimensions.isEmpty(),
                onToggleExpand = { onToggleExpand(name) },
                onAddTag = { onEvent(UserProfileUiEvent.ShowAddTagDialog(name)) },
                onEditTag = { tag -> onEvent(UserProfileUiEvent.ShowEditTagDialog(name, tag)) },
                onDeleteDimension = { onEvent(UserProfileUiEvent.ShowDeleteDimensionConfirm(name)) }
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
            fontSize = 15.sp,
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
            fontSize = 15.sp,
            color = iOSTextPrimary
        )
        Text(
            text = "点击上方按钮添加您的专属维度",
            fontSize = 13.sp,
            color = iOSTextSecondary
        )
    }
}


// ============================================================
// iOS风格对话框
// ============================================================

/**
 * iOS风格添加标签对话框
 */
@Composable
private fun IOSAddTagDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tagInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = iOSCardBackground,
        title = { 
            Text(
                "添加标签",
                fontWeight = FontWeight.SemiBold,
                color = iOSTextPrimary
            ) 
        },
        text = {
            OutlinedTextField(
                value = tagInput,
                onValueChange = { tagInput = it; isError = false },
                label = { Text("标签内容") },
                placeholder = { Text("请输入标签") },
                singleLine = true,
                isError = isError,
                supportingText = if (isError) { { Text("标签不能为空且长度不超过20字符", color = iOSRed) } } else null,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = iOSBlue,
                    cursorColor = iOSBlue
                )
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = tagInput.trim()
                if (trimmed.isNotEmpty() && trimmed.length <= 20) { 
                    onConfirm(trimmed)
                    onDismiss() 
                } else {
                    isError = true
                }
            }) { 
                Text("添加", color = iOSBlue, fontWeight = FontWeight.SemiBold) 
            }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text("取消", color = iOSBlue) 
            } 
        }
    )
}

/**
 * iOS风格编辑标签对话框
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = iOSCardBackground,
        title = { 
            Text(
                "编辑标签",
                fontWeight = FontWeight.SemiBold,
                color = iOSTextPrimary
            ) 
        },
        text = {
            OutlinedTextField(
                value = tagInput,
                onValueChange = { tagInput = it; isError = false },
                label = { Text("标签内容") },
                singleLine = true,
                isError = isError,
                supportingText = if (isError) { { Text("标签不能为空且长度不超过20字符", color = iOSRed) } } else null,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = iOSBlue,
                    cursorColor = iOSBlue
                )
            )
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = onDelete
                ) { 
                    Text("删除", color = iOSRed) 
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {
                    val trimmed = tagInput.trim()
                    if (trimmed.isNotEmpty() && trimmed.length <= 20) { 
                        onConfirm(trimmed)
                        onDismiss() 
                    } else {
                        isError = true
                    }
                }) { 
                    Text("保存", color = iOSBlue, fontWeight = FontWeight.SemiBold) 
                }
            }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text("取消", color = iOSBlue) 
            } 
        }
    )
}

/**
 * iOS风格添加维度对话框
 */
@Composable
private fun IOSAddDimensionDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = iOSCardBackground,
        title = { 
            Text(
                "添加自定义维度",
                fontWeight = FontWeight.SemiBold,
                color = iOSTextPrimary
            ) 
        },
        text = {
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it; isError = false },
                label = { Text("维度名称") },
                placeholder = { Text("例如：职业技能、生活习惯") },
                singleLine = true,
                isError = isError,
                supportingText = if (isError) { 
                    { Text(errorMessage, color = iOSRed) } 
                } else { 
                    { Text("2-10个字符", color = iOSTextSecondary) } 
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = iOSBlue,
                    cursorColor = iOSBlue
                )
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
            }) { 
                Text("添加", color = iOSBlue, fontWeight = FontWeight.SemiBold) 
            }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text("取消", color = iOSBlue) 
            } 
        }
    )
}

/**
 * iOS风格删除确认对话框
 */
@Composable
private fun IOSDeleteConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = iOSCardBackground,
        icon = { 
            Icon(
                Icons.Default.Warning, 
                contentDescription = null, 
                tint = iOSRed,
                modifier = Modifier.size(32.dp)
            ) 
        },
        title = { 
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                color = iOSTextPrimary
            ) 
        },
        text = { 
            Text(message, color = iOSTextSecondary) 
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) { 
                Text("删除", color = iOSRed, fontWeight = FontWeight.SemiBold) 
            }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text("取消", color = iOSBlue) 
            } 
        }
    )
}

/**
 * iOS风格导出对话框
 */
@Composable
private fun IOSExportDialog(
    onExport: (ExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    val dimensions = AdaptiveDimensions.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = iOSCardBackground,
        title = { 
            Text(
                "导出画像",
                fontWeight = FontWeight.SemiBold,
                color = iOSTextPrimary
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)) {
                Text(
                    "选择导出格式", 
                    fontSize = 13.sp,
                    color = iOSTextSecondary
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
                            color = iOSTextPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text("取消", color = iOSBlue) 
            } 
        }
    )
}

/**
 * iOS风格重置确认对话框
 */
@Composable
private fun IOSResetConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = iOSCardBackground,
        icon = { 
            Icon(
                Icons.Default.Warning, 
                contentDescription = null, 
                tint = iOSRed,
                modifier = Modifier.size(32.dp)
            ) 
        },
        title = { 
            Text(
                "重置画像",
                fontWeight = FontWeight.SemiBold,
                color = iOSTextPrimary
            ) 
        },
        text = { 
            Text(
                "确定要重置所有画像数据吗？此操作不可撤销，所有标签和自定义维度都将被清除。",
                color = iOSTextSecondary
            ) 
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) { 
                Text("重置", color = iOSRed, fontWeight = FontWeight.SemiBold) 
            }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text("取消", color = iOSBlue) 
            } 
        }
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
            onRefresh = {}
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
