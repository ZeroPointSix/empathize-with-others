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
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.UserProfile
import com.empathy.ai.domain.model.UserProfileDimension

/**
 * 用于测试的UserProfileScreen内容组件
 *
 * 这是一个独立的Composable，不依赖ViewModel，方便UI测试。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreenTestContent(
    uiState: UserProfileUiState,
    onEvent: (UserProfileUiEvent) -> Unit = {},
    onNavigateBack: () -> Unit = {},
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TestProfileCompletenessCard(uiState.completeness, uiState.totalTagCount)

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
                    0 -> TestBaseDimensionsContent(uiState.profile, onEvent)
                    1 -> TestCustomDimensionsContent(uiState.profile, uiState.canAddCustomDimension, onEvent)
                }
            }
        }
    }
}

@Composable
private fun TestProfileCompletenessCard(completeness: Int, totalTagCount: Int, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(targetValue = completeness / 100f, label = "completeness")
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
private fun TestBaseDimensionsContent(profile: UserProfile, onEvent: (UserProfileUiEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        UserProfileDimension.entries.forEach { dimension ->
            TestDimensionCard(
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
private fun TestCustomDimensionsContent(profile: UserProfile, canAddDimension: Boolean, onEvent: (UserProfileUiEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (canAddDimension) {
            OutlinedButton(onClick = { onEvent(UserProfileUiEvent.ShowAddDimensionDialog) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加自定义维度")
            }
        } else {
            Text("已达到自定义维度上限（最多10个）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        profile.customDimensions.forEach { (name, tags) ->
            TestDimensionCard(
                title = name, description = "自定义维度", tags = tags, presetTags = emptyList(), isCustom = true,
                onAddTag = { onEvent(UserProfileUiEvent.ShowAddTagDialog(name)) },
                onEditTag = { tag -> onEvent(UserProfileUiEvent.ShowEditTagDialog(name, tag)) },
                onSelectPresetTag = {},
                onDeleteDimension = { onEvent(UserProfileUiEvent.ShowDeleteDimensionConfirm(name)) }
            )
        }

        if (profile.customDimensions.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("暂无自定义维度", style = MaterialTheme.typography.bodyLarge)
                    Text("点击上方按钮添加您的专属维度", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun TestDimensionCard(
    title: String, description: String, tags: List<String>, presetTags: List<String>,
    isCustom: Boolean = false, onAddTag: () -> Unit, onEditTag: (String) -> Unit,
    onSelectPresetTag: (String) -> Unit, onDeleteDimension: (() -> Unit)? = null, modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }
    val rotationAngle by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "rotation")

    Card(modifier = modifier.fillMaxWidth().animateContentSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        Icon(
                            Icons.Default.ExpandMore, 
                            contentDescription = if (expanded) "收起" else "展开", 
                            modifier = Modifier.rotate(rotationAngle)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (tags.isNotEmpty()) TestTagChipGroup(tags = tags, onTagClick = onEditTag)
                    AssistChip(onClick = onAddTag, label = { Text("添加标签") }, leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) })
                    if (presetTags.isNotEmpty()) {
                        Text("快速选择", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
private fun TestTagChipGroup(tags: List<String>, onTagClick: (String) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.forEach { tag ->
            InputChip(
                selected = false, onClick = { onTagClick(tag) },
                label = { Text(tag, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                trailingIcon = { Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(16.dp)) }
            )
        }
    }
}
