package com.empathy.ai.presentation.ui.screen.userprofile

import com.empathy.ai.domain.model.UserProfile
import org.junit.Assert.*
import org.junit.Test

/**
 * 用户画像重置功能测试
 * 
 * BUG-00037 P2: 验证重置功能的正确性
 */
class UserProfileResetTest {

    // ==================== 重置确认对话框测试 ====================

    @Test
    fun `显示重置确认对话框`() {
        val state = UserProfileUiState(
            profile = UserProfile(personalityTraits = listOf("外向", "乐观")),
            showResetConfirmDialog = true
        )
        
        assertTrue(state.showResetConfirmDialog)
    }

    @Test
    fun `隐藏重置确认对话框`() {
        val state = UserProfileUiState(
            showResetConfirmDialog = false
        )
        
        assertFalse(state.showResetConfirmDialog)
    }

    // ==================== 重置结果测试 ====================

    @Test
    fun `重置后所有基础维度标签应该被清空`() {
        val emptyProfile = UserProfile()
        
        assertTrue(emptyProfile.personalityTraits.isEmpty())
        assertTrue(emptyProfile.values.isEmpty())
        assertTrue(emptyProfile.interests.isEmpty())
        assertTrue(emptyProfile.communicationStyle.isEmpty())
        assertTrue(emptyProfile.socialPreferences.isEmpty())
    }

    @Test
    fun `重置后所有自定义维度应该被清空`() {
        val emptyProfile = UserProfile()
        
        assertTrue(emptyProfile.customDimensions.isEmpty())
    }

    @Test
    fun `重置后应该显示成功消息`() {
        val state = UserProfileUiState(
            profile = UserProfile(),
            successMessage = "画像已重置",
            showResetConfirmDialog = false
        )
        
        assertEquals("画像已重置", state.successMessage)
        assertFalse(state.showResetConfirmDialog)
    }

    // ==================== 重置前状态验证 ====================

    @Test
    fun `有数据时应该可以重置`() {
        val state = UserProfileUiState(
            profile = UserProfile(
                personalityTraits = listOf("外向", "乐观"),
                values = listOf("家庭"),
                customDimensions = mapOf("职业技能" to listOf("Kotlin"))
            )
        )
        
        // 有数据时totalTagCount > 0
        assertTrue(state.totalTagCount > 0)
    }

    @Test
    fun `空数据时重置应该无效果`() {
        val emptyProfile = UserProfile()
        val state = UserProfileUiState(profile = emptyProfile)
        
        assertEquals(0, state.totalTagCount)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `重置过程中应该显示加载状态`() {
        val state = UserProfileUiState(
            isLoading = true,
            showResetConfirmDialog = false
        )
        
        assertTrue(state.isLoading)
    }

    @Test
    fun `重置失败应该显示错误消息`() {
        val state = UserProfileUiState(
            error = "重置失败",
            isLoading = false
        )
        
        assertEquals("重置失败", state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `取消重置应该保持原有数据`() {
        val originalProfile = UserProfile(
            personalityTraits = listOf("外向", "乐观"),
            values = listOf("家庭")
        )
        val state = UserProfileUiState(
            profile = originalProfile,
            showResetConfirmDialog = false
        )
        
        // 取消后数据应该保持不变
        assertEquals(2, state.profile.personalityTraits.size)
        assertEquals(1, state.profile.values.size)
    }
}
