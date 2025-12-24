package com.empathy.ai.presentation.ui.screen.contact.persona

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.R
import com.empathy.ai.domain.model.EditModeState
import com.empathy.ai.domain.model.FactCategory
import com.empathy.ai.domain.model.PersonaSearchState

/**
 * 标签画像页面V2
 *
 * 升级后的标签画像页面，支持：
 * - 分类搜索
 * - 编辑模式（批量选择、删除、移动）
 * - 动态颜色分类卡片
 * - 折叠/展开动画
 *
 * @param categories 分类列表
 * @param searchState 搜索状态
 * @param editModeState 编辑模式状态
 * @param availableCategories 可用的分类列表（用于移动对话框）
 * @param isDarkMode 是否为深色模式
 * @param onSearchQueryChange 搜索关键词变化回调
 * @param onClearSearch 清除搜索回调
 * @param onToggleCategoryExpand 切换分类展开/折叠回调
 * @param onFactClick 标签点击回调
 * @param onFactLongClick 标签长按回调（进入编辑模式）
 * @param onToggleFactSelection 切换标签选中状态回调
 * @param onExitEditMode 退出编辑模式回调
 * @param onSelectAll 全选回调
 * @param onDeselectAll 取消全选回调
 * @param onShowDeleteConfirm 显示删除确认对话框回调
 * @param onHideDeleteConfirm 隐藏删除确认对话框回调
 * @param onConfirmDelete 确认删除回调
 * @param onShowMoveDialog 显示移动对话框回调
 * @param onHideMoveDialog 隐藏移动对话框回调
 * @param onConfirmMove 确认移动回调
 * @param modifier Modifier
 */
@Composable
fun PersonaTabV2(
    categories: List<FactCategory>,
    searchState: PersonaSearchState,
    editModeState: EditModeState,
    availableCategories: List<String>,
    isDarkMode: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onToggleCategoryExpand: (String) -> Unit,
    onFactClick: (String) -> Unit,
    onFactLongClick: (String) -> Unit,
    onToggleFactSelection: (String) -> Unit,
    onExitEditMode: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onShowDeleteConfirm: () -> Unit,
    onHideDeleteConfirm: () -> Unit,
    onConfirmDelete: () -> Unit,
    onShowMoveDialog: () -> Unit,
    onHideMoveDialog: () -> Unit,
    onConfirmMove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 顶部栏：编辑模式显示EditModeTopBar，否则显示CategorySearchBar
        if (editModeState.isActive) {
            EditModeTopBar(
                selectedCount = editModeState.selectedCount,
                onExitEditMode = onExitEditMode,
                onSelectAll = onSelectAll,
                onDeselectAll = onDeselectAll
            )
        } else {
            CategorySearchBar(
                query = searchState.query,
                onQueryChange = onSearchQueryChange,
                onClearSearch = onClearSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // 分类列表
        if (categories.isEmpty()) {
            // 空状态
            EmptyPersonaState(
                hasSearchQuery = searchState.hasQuery,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = if (editModeState.hasSelection) 80.dp else 16.dp
                )
            ) {
                items(
                    items = categories,
                    key = { it.key }
                ) { category ->
                    DynamicCategoryCard(
                        category = category,
                        isEditMode = editModeState.isActive,
                        selectedFactIds = editModeState.selectedFactIds,
                        searchQuery = searchState.query,
                        isDarkMode = isDarkMode,
                        onToggleExpand = { onToggleCategoryExpand(category.key) },
                        onFactClick = onFactClick,
                        onFactLongClick = onFactLongClick,
                        onToggleFactSelection = onToggleFactSelection,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        // 批量操作栏：编辑模式下有选中项时显示
        if (editModeState.isActive && editModeState.hasSelection) {
            BatchActionBar(
                selectedCount = editModeState.selectedCount,
                onMoveClick = onShowMoveDialog,
                onDeleteClick = onShowDeleteConfirm
            )
        }
    }

    // 删除确认对话框
    if (editModeState.showDeleteConfirm) {
        BatchDeleteConfirmDialog(
            selectedCount = editModeState.selectedCount,
            onConfirm = onConfirmDelete,
            onDismiss = onHideDeleteConfirm
        )
    }

    // 移动分类对话框
    if (editModeState.showMoveDialog) {
        MoveCategoryDialog(
            selectedCount = editModeState.selectedCount,
            existingCategories = availableCategories,
            onConfirm = onConfirmMove,
            onDismiss = onHideMoveDialog
        )
    }
}

/**
 * 空状态组件
 */
@Composable
private fun EmptyPersonaState(
    hasSearchQuery: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (hasSearchQuery) {
                stringResource(R.string.no_search_results)
            } else {
                stringResource(R.string.no_tags_yet)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
