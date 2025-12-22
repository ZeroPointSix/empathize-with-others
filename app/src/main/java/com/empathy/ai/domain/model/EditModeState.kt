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
    /**
     * 已选中的数量
     */
    val selectedCount: Int
        get() = selectedFactIds.size

    /**
     * 是否有选中项
     */
    val hasSelection: Boolean
        get() = selectedFactIds.isNotEmpty()

    /**
     * 切换指定Fact的选中状态
     *
     * @param factId 要切换的Fact ID
     * @return 新的EditModeState实例
     */
    fun toggleSelection(factId: String): EditModeState {
        val newSelection = if (selectedFactIds.contains(factId)) {
            selectedFactIds - factId
        } else {
            selectedFactIds + factId
        }
        return copy(selectedFactIds = newSelection)
    }

    /**
     * 全选指定的Fact ID列表
     *
     * @param factIds 要选中的Fact ID列表
     * @return 新的EditModeState实例
     */
    fun selectAll(factIds: List<String>): EditModeState {
        return copy(selectedFactIds = selectedFactIds + factIds.toSet())
    }

    /**
     * 取消选中指定的Fact ID列表
     *
     * @param factIds 要取消选中的Fact ID列表
     * @return 新的EditModeState实例
     */
    fun deselectAll(factIds: List<String>): EditModeState {
        return copy(selectedFactIds = selectedFactIds - factIds.toSet())
    }

    /**
     * 清除所有选中项
     *
     * @return 新的EditModeState实例
     */
    fun clearSelection(): EditModeState {
        return copy(selectedFactIds = emptySet())
    }

    /**
     * 退出编辑模式
     *
     * @return 新的EditModeState实例（重置为初始状态）
     */
    fun exit(): EditModeState {
        return EditModeState()
    }

    /**
     * 显示删除确认对话框
     *
     * @return 新的EditModeState实例
     */
    fun showDeleteConfirmDialog(): EditModeState {
        return copy(showDeleteConfirm = true)
    }

    /**
     * 隐藏删除确认对话框
     *
     * @return 新的EditModeState实例
     */
    fun hideDeleteConfirmDialog(): EditModeState {
        return copy(showDeleteConfirm = false)
    }

    /**
     * 显示移动分类对话框
     *
     * @return 新的EditModeState实例
     */
    fun showMoveCategoryDialog(): EditModeState {
        return copy(showMoveDialog = true)
    }

    /**
     * 隐藏移动分类对话框
     *
     * @return 新的EditModeState实例
     */
    fun hideMoveCategoryDialog(): EditModeState {
        return copy(showMoveDialog = false)
    }

    companion object {
        /**
         * 创建激活的编辑模式状态
         *
         * @param initialFactId 初始选中的Fact ID（可选）
         * @return 新的EditModeState实例
         */
        fun activated(initialFactId: String? = null): EditModeState {
            return EditModeState(
                isActive = true,
                selectedFactIds = if (initialFactId != null) setOf(initialFactId) else emptySet()
            )
        }
    }
}
