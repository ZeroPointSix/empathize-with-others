package com.empathy.ai.domain.model

/**
 * 编辑模式状态
 *
 * 管理标签画像页面的编辑模式状态，包括选中项、对话框显示等
 *
 * @property isActive 是否处于编辑模式
 * @property selectedFactIds 已选中的Fact ID集合
 * @property showDeleteConfirm 是否显示删除确认对话框
 * @property showMoveDialog 是否显示移动分类对话框
 */
data class EditModeState(
    val isActive: Boolean = false,
    val selectedFactIds: Set<String> = emptySet(),
    val showDeleteConfirm: Boolean = false,
    val showMoveDialog: Boolean = false
) {
    val selectedCount: Int get() = selectedFactIds.size
    val hasSelection: Boolean get() = selectedFactIds.isNotEmpty()

    fun toggleSelection(factId: String): EditModeState {
        val newSelection = if (selectedFactIds.contains(factId)) {
            selectedFactIds - factId
        } else {
            selectedFactIds + factId
        }
        return copy(selectedFactIds = newSelection)
    }

    fun selectAll(factIds: List<String>): EditModeState =
        copy(selectedFactIds = selectedFactIds + factIds.toSet())

    fun deselectAll(factIds: List<String>): EditModeState =
        copy(selectedFactIds = selectedFactIds - factIds.toSet())

    fun clearSelection(): EditModeState = copy(selectedFactIds = emptySet())

    fun exit(): EditModeState = EditModeState()

    fun showDeleteConfirmDialog(): EditModeState = copy(showDeleteConfirm = true)

    fun hideDeleteConfirmDialog(): EditModeState = copy(showDeleteConfirm = false)

    fun showMoveCategoryDialog(): EditModeState = copy(showMoveDialog = true)

    fun hideMoveCategoryDialog(): EditModeState = copy(showMoveDialog = false)

    companion object {
        fun activated(initialFactId: String? = null): EditModeState = EditModeState(
            isActive = true,
            selectedFactIds = if (initialFactId != null) setOf(initialFactId) else emptySet()
        )
    }
}
