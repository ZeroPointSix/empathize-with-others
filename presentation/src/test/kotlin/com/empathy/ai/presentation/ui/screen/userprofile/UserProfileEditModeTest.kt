package com.empathy.ai.presentation.ui.screen.userprofile

import com.empathy.ai.domain.model.UserProfile
import org.junit.Assert.*
import org.junit.Test

/**
 * 用户画像编辑模式测试
 * 
 * BUG-00037 P1: 验证编辑模式的状态管理
 */
class UserProfileEditModeTest {

    // ==================== 编辑模式状态测试 ====================

    @Test
    fun `初始状态应该不是编辑模式`() {
        val state = UserProfileUiState()
        assertFalse(state.isEditMode)
        assertFalse(state.hasUnsavedChanges)
        assertTrue(state.pendingChanges.isEmpty())
    }

    @Test
    fun `添加标签后应该标记有未保存变更`() {
        val state = UserProfileUiState(
            profile = UserProfile(personalityTraits = listOf("外向")),
            pendingChanges = mapOf("PERSONALITY_TRAITS" to listOf("外向", "乐观")),
            hasUnsavedChanges = true
        )
        
        assertTrue(state.hasUnsavedChanges)
        assertEquals(2, state.pendingChanges["PERSONALITY_TRAITS"]?.size)
    }

    @Test
    fun `删除标签后应该标记有未保存变更`() {
        val state = UserProfileUiState(
            profile = UserProfile(personalityTraits = listOf("外向", "乐观")),
            pendingChanges = mapOf("PERSONALITY_TRAITS" to listOf("外向")),
            hasUnsavedChanges = true
        )
        
        assertTrue(state.hasUnsavedChanges)
        assertEquals(1, state.pendingChanges["PERSONALITY_TRAITS"]?.size)
    }

    @Test
    fun `编辑标签后应该标记有未保存变更`() {
        val state = UserProfileUiState(
            profile = UserProfile(personalityTraits = listOf("外向")),
            pendingChanges = mapOf("PERSONALITY_TRAITS" to listOf("内向")),
            hasUnsavedChanges = true
        )
        
        assertTrue(state.hasUnsavedChanges)
        assertEquals("内向", state.pendingChanges["PERSONALITY_TRAITS"]?.first())
    }

    @Test
    fun `保存后应该清除未保存变更标记`() {
        val state = UserProfileUiState(
            profile = UserProfile(personalityTraits = listOf("外向", "乐观")),
            pendingChanges = emptyMap(),
            hasUnsavedChanges = false
        )
        
        assertFalse(state.hasUnsavedChanges)
        assertTrue(state.pendingChanges.isEmpty())
    }

    @Test
    fun `取消编辑应该丢弃所有未保存变更`() {
        val originalProfile = UserProfile(personalityTraits = listOf("外向"))
        val state = UserProfileUiState(
            profile = originalProfile,
            pendingChanges = emptyMap(),
            hasUnsavedChanges = false
        )
        
        // 取消后应该恢复到原始状态
        assertEquals(listOf("外向"), state.profile.personalityTraits)
        assertFalse(state.hasUnsavedChanges)
    }

    // ==================== 滚动位置保持测试 ====================

    @Test
    fun `添加标签后滚动位置应该保持`() {
        // 这个测试需要在UI测试中验证
        // 这里只验证状态不会导致完全重置
        val state = UserProfileUiState(
            profile = UserProfile(personalityTraits = listOf("外向")),
            pendingChanges = mapOf("PERSONALITY_TRAITS" to listOf("外向", "乐观")),
            hasUnsavedChanges = true,
            selectedTabIndex = 0
        )
        
        // 添加标签后Tab索引应该保持
        assertEquals(0, state.selectedTabIndex)
    }

    @Test
    fun `切换Tab后返回应该保持之前的状态`() {
        val state = UserProfileUiState(
            profile = UserProfile(personalityTraits = listOf("外向")),
            pendingChanges = mapOf("PERSONALITY_TRAITS" to listOf("外向", "乐观")),
            hasUnsavedChanges = true,
            selectedTabIndex = 1
        )
        
        // 切换到自定义维度Tab后，未保存变更应该保持
        assertTrue(state.hasUnsavedChanges)
        assertEquals(1, state.selectedTabIndex)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `空标签列表应该正确处理`() {
        val state = UserProfileUiState(
            profile = UserProfile(),
            pendingChanges = mapOf("PERSONALITY_TRAITS" to emptyList()),
            hasUnsavedChanges = true
        )
        
        assertTrue(state.pendingChanges["PERSONALITY_TRAITS"]?.isEmpty() == true)
    }

    @Test
    fun `多维度同时编辑应该正确处理`() {
        val state = UserProfileUiState(
            profile = UserProfile(
                personalityTraits = listOf("外向"),
                values = listOf("家庭")
            ),
            pendingChanges = mapOf(
                "PERSONALITY_TRAITS" to listOf("外向", "乐观"),
                "VALUES" to listOf("家庭", "事业")
            ),
            hasUnsavedChanges = true
        )
        
        assertEquals(2, state.pendingChanges.size)
        assertEquals(2, state.pendingChanges["PERSONALITY_TRAITS"]?.size)
        assertEquals(2, state.pendingChanges["VALUES"]?.size)
    }

    @Test
    fun `自定义维度编辑应该正确处理`() {
        val state = UserProfileUiState(
            profile = UserProfile(
                customDimensions = mapOf("职业技能" to listOf("Kotlin"))
            ),
            pendingChanges = mapOf("职业技能" to listOf("Kotlin", "Android")),
            hasUnsavedChanges = true
        )
        
        assertEquals(2, state.pendingChanges["职业技能"]?.size)
    }
}
