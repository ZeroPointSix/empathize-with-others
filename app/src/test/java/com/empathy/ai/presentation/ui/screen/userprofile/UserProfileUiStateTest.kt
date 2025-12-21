package com.empathy.ai.presentation.ui.screen.userprofile

import com.empathy.ai.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UserProfileUiState 单元测试
 */
class UserProfileUiStateTest {
    
    // ========== 默认值测试 ==========
    
    @Test
    fun `默认状态profile为空`() {
        val state = UserProfileUiState()
        assertTrue(state.profile.isEmpty())
    }
    
    @Test
    fun `默认状态selectedTabIndex为0`() {
        val state = UserProfileUiState()
        assertEquals(0, state.selectedTabIndex)
    }
    
    @Test
    fun `默认状态isLoading为false`() {
        val state = UserProfileUiState()
        assertFalse(state.isLoading)
    }
    
    @Test
    fun `默认状态error为null`() {
        val state = UserProfileUiState()
        assertEquals(null, state.error)
    }
    
    @Test
    fun `默认状态所有对话框都不显示`() {
        val state = UserProfileUiState()
        assertFalse(state.showAddTagDialog)
        assertFalse(state.showEditTagDialog)
        assertFalse(state.showAddDimensionDialog)
        assertFalse(state.showDeleteConfirmDialog)
        assertFalse(state.showExportDialog)
        assertFalse(state.showResetConfirmDialog)
    }
    
    @Test
    fun `默认状态编辑相关字段为null`() {
        val state = UserProfileUiState()
        assertEquals(null, state.currentEditDimension)
        assertEquals(null, state.currentEditTag)
        assertEquals(null, state.pendingDeleteDimension)
        assertEquals(null, state.pendingDeleteTag)
        assertEquals(null, state.successMessage)
    }
    
    // ========== completeness 计算属性测试 ==========
    
    @Test
    fun `completeness 空画像返回0`() {
        val state = UserProfileUiState(profile = UserProfile())
        assertEquals(0, state.completeness)
    }
    
    @Test
    fun `completeness 一个维度有标签返回20`() {
        val profile = UserProfile(personalityTraits = listOf("内向"))
        val state = UserProfileUiState(profile = profile)
        assertEquals(20, state.completeness)
    }
    
    @Test
    fun `completeness 五个维度都有标签返回100`() {
        val profile = UserProfile(
            personalityTraits = listOf("内向"),
            values = listOf("诚实"),
            interests = listOf("阅读"),
            communicationStyle = listOf("直接"),
            socialPreferences = listOf("小圈子")
        )
        val state = UserProfileUiState(profile = profile)
        assertEquals(100, state.completeness)
    }
    
    // ========== totalTagCount 计算属性测试 ==========
    
    @Test
    fun `totalTagCount 空画像返回0`() {
        val state = UserProfileUiState(profile = UserProfile())
        assertEquals(0, state.totalTagCount)
    }
    
    @Test
    fun `totalTagCount 计算所有标签总数`() {
        val profile = UserProfile(
            personalityTraits = listOf("内向", "敏感"),
            values = listOf("诚实"),
            customDimensions = mapOf("职业技能" to listOf("编程", "设计"))
        )
        val state = UserProfileUiState(profile = profile)
        assertEquals(5, state.totalTagCount)
    }
    
    // ========== isEmpty 计算属性测试 ==========
    
    @Test
    fun `isEmpty 空画像返回true`() {
        val state = UserProfileUiState(profile = UserProfile())
        assertTrue(state.isEmpty)
    }
    
    @Test
    fun `isEmpty 有标签返回false`() {
        val profile = UserProfile(personalityTraits = listOf("内向"))
        val state = UserProfileUiState(profile = profile)
        assertFalse(state.isEmpty)
    }
    
    // ========== canAddCustomDimension 计算属性测试 ==========
    
    @Test
    fun `canAddCustomDimension 未达上限返回true`() {
        val state = UserProfileUiState(profile = UserProfile())
        assertTrue(state.canAddCustomDimension)
    }
    
    @Test
    fun `canAddCustomDimension 达到上限返回false`() {
        val dimensions = (1..10).associate { "维度$it" to emptyList<String>() }
        val profile = UserProfile(customDimensions = dimensions)
        val state = UserProfileUiState(profile = profile)
        assertFalse(state.canAddCustomDimension)
    }
    
    // ========== customDimensionCount 计算属性测试 ==========
    
    @Test
    fun `customDimensionCount 无自定义维度返回0`() {
        val state = UserProfileUiState(profile = UserProfile())
        assertEquals(0, state.customDimensionCount)
    }
    
    @Test
    fun `customDimensionCount 返回自定义维度数量`() {
        val profile = UserProfile(customDimensions = mapOf(
            "职业技能" to listOf("编程"),
            "语言能力" to listOf("中文")
        ))
        val state = UserProfileUiState(profile = profile)
        assertEquals(2, state.customDimensionCount)
    }
    
    // ========== copy 测试 ==========
    
    @Test
    fun `copy 正确复制状态`() {
        val state = UserProfileUiState(
            isLoading = true,
            error = "错误"
        )
        val copied = state.copy(isLoading = false)
        
        assertFalse(copied.isLoading)
        assertEquals("错误", copied.error)
    }
}
