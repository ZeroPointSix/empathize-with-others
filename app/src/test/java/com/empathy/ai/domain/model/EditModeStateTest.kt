package com.empathy.ai.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * EditModeState 模型单元测试
 */
class EditModeStateTest {

    @Test
    fun `初始状态_所有字段为默认值`() {
        val state = EditModeState()

        assertFalse(state.isActive)
        assertTrue(state.selectedFactIds.isEmpty())
        assertFalse(state.showDeleteConfirm)
        assertFalse(state.showMoveDialog)
    }

    @Test
    fun `selectedCount_返回正确的选中数量`() {
        val state = EditModeState(
            selectedFactIds = setOf("id_1", "id_2", "id_3")
        )

        assertEquals(3, state.selectedCount)
    }

    @Test
    fun `selectedCount_空集合返回0`() {
        val state = EditModeState()

        assertEquals(0, state.selectedCount)
    }

    @Test
    fun `hasSelection_有选中项返回true`() {
        val state = EditModeState(
            selectedFactIds = setOf("id_1")
        )

        assertTrue(state.hasSelection)
    }

    @Test
    fun `hasSelection_无选中项返回false`() {
        val state = EditModeState()

        assertFalse(state.hasSelection)
    }

    @Test
    fun `toggleSelection_添加未选中的ID`() {
        val state = EditModeState(
            selectedFactIds = setOf("id_1")
        )

        val newState = state.toggleSelection("id_2")

        assertTrue(newState.selectedFactIds.contains("id_1"))
        assertTrue(newState.selectedFactIds.contains("id_2"))
        assertEquals(2, newState.selectedCount)
    }

    @Test
    fun `toggleSelection_移除已选中的ID`() {
        val state = EditModeState(
            selectedFactIds = setOf("id_1", "id_2")
        )

        val newState = state.toggleSelection("id_1")

        assertFalse(newState.selectedFactIds.contains("id_1"))
        assertTrue(newState.selectedFactIds.contains("id_2"))
        assertEquals(1, newState.selectedCount)
    }

    @Test
    fun `selectAll_添加所有ID到选中集合`() {
        val state = EditModeState(
            selectedFactIds = setOf("id_1")
        )

        val newState = state.selectAll(listOf("id_2", "id_3"))

        assertTrue(newState.selectedFactIds.contains("id_1"))
        assertTrue(newState.selectedFactIds.contains("id_2"))
        assertTrue(newState.selectedFactIds.contains("id_3"))
        assertEquals(3, newState.selectedCount)
    }

    @Test
    fun `selectAll_空列表不改变状态`() {
        val state = EditModeState(
            selectedFactIds = setOf("id_1")
        )

        val newState = state.selectAll(emptyList())

        assertEquals(state.selectedFactIds, newState.selectedFactIds)
    }

    @Test
    fun `deselectAll_从选中集合移除指定ID`() {
        val state = EditModeState(
            selectedFactIds = setOf("id_1", "id_2", "id_3")
        )

        val newState = state.deselectAll(listOf("id_1", "id_2"))

        assertFalse(newState.selectedFactIds.contains("id_1"))
        assertFalse(newState.selectedFactIds.contains("id_2"))
        assertTrue(newState.selectedFactIds.contains("id_3"))
        assertEquals(1, newState.selectedCount)
    }

    @Test
    fun `clearSelection_清空所有选中项`() {
        val state = EditModeState(
            selectedFactIds = setOf("id_1", "id_2", "id_3")
        )

        val newState = state.clearSelection()

        assertTrue(newState.selectedFactIds.isEmpty())
        assertEquals(0, newState.selectedCount)
    }

    @Test
    fun `exit_重置为初始状态`() {
        val state = EditModeState(
            isActive = true,
            selectedFactIds = setOf("id_1", "id_2"),
            showDeleteConfirm = true,
            showMoveDialog = true
        )

        val newState = state.exit()

        assertFalse(newState.isActive)
        assertTrue(newState.selectedFactIds.isEmpty())
        assertFalse(newState.showDeleteConfirm)
        assertFalse(newState.showMoveDialog)
    }

    @Test
    fun `showDeleteConfirmDialog_设置showDeleteConfirm为true`() {
        val state = EditModeState()

        val newState = state.showDeleteConfirmDialog()

        assertTrue(newState.showDeleteConfirm)
    }

    @Test
    fun `hideDeleteConfirmDialog_设置showDeleteConfirm为false`() {
        val state = EditModeState(showDeleteConfirm = true)

        val newState = state.hideDeleteConfirmDialog()

        assertFalse(newState.showDeleteConfirm)
    }

    @Test
    fun `showMoveCategoryDialog_设置showMoveDialog为true`() {
        val state = EditModeState()

        val newState = state.showMoveCategoryDialog()

        assertTrue(newState.showMoveDialog)
    }

    @Test
    fun `hideMoveCategoryDialog_设置showMoveDialog为false`() {
        val state = EditModeState(showMoveDialog = true)

        val newState = state.hideMoveCategoryDialog()

        assertFalse(newState.showMoveDialog)
    }

    @Test
    fun `activated_创建激活状态_无初始选中`() {
        val state = EditModeState.activated()

        assertTrue(state.isActive)
        assertTrue(state.selectedFactIds.isEmpty())
    }

    @Test
    fun `activated_创建激活状态_有初始选中`() {
        val state = EditModeState.activated("id_1")

        assertTrue(state.isActive)
        assertTrue(state.selectedFactIds.contains("id_1"))
        assertEquals(1, state.selectedCount)
    }

    @Test
    fun `activated_null初始ID不添加选中项`() {
        val state = EditModeState.activated(null)

        assertTrue(state.isActive)
        assertTrue(state.selectedFactIds.isEmpty())
    }
}
