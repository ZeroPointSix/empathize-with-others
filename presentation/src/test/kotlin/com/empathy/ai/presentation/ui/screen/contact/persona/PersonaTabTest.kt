package com.empathy.ai.presentation.ui.screen.contact.persona

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PersonaTab 单元测试
 * 
 * TD-00020 T070: 测试搜索功能、分类折叠、AI推测交互
 * 
 * 关键测试场景:
 * - 搜索关键词过滤
 * - 分类卡片折叠/展开动画
 * - AI推测标签确认/拒绝
 */
class PersonaTabTest {

    // ============================================================
    // PersonaUiState 测试
    // ============================================================

    @Test
    fun `PersonaUiState default values are correct`() {
        val state = PersonaUiState()
        
        assertEquals("", state.searchQuery)
        assertTrue(state.expandedCategories.isEmpty())
        assertTrue(state.categoryTags.isEmpty())
        assertTrue(state.inferredTags.isEmpty())
        assertFalse(state.isLoading)
        assertEquals(null, state.error)
    }

    @Test
    fun `PersonaUiState with search query is valid`() {
        val state = PersonaUiState(searchQuery = "test")
        
        assertEquals("test", state.searchQuery)
        assertTrue(state.hasSearchQuery)
    }

    @Test
    fun `PersonaUiState with empty search query has no query`() {
        val state = PersonaUiState(searchQuery = "")
        
        assertEquals("", state.searchQuery)
        assertFalse(state.hasSearchQuery)
    }

    // ============================================================
    // 分类折叠测试
    // ============================================================

    @Test
    fun `PersonaUiState with expanded categories is valid`() {
        val expandedCategories = setOf("INTERESTS", "WORK")
        val state = PersonaUiState(expandedCategories = expandedCategories)
        
        assertEquals(2, state.expandedCategories.size)
        assertTrue(state.isCategoryExpanded("INTERESTS"))
        assertTrue(state.isCategoryExpanded("WORK"))
        assertFalse(state.isCategoryExpanded("STRATEGY"))
    }

    @Test
    fun `PersonaUiState with no expanded categories`() {
        val state = PersonaUiState(expandedCategories = emptySet())
        
        assertTrue(state.expandedCategories.isEmpty())
        assertFalse(state.isCategoryExpanded("INTERESTS"))
    }

    @Test
    fun `toggleCategory adds category when not expanded`() {
        val state = PersonaUiState(expandedCategories = emptySet())
        val newState = state.toggleCategory("INTERESTS")
        
        assertTrue(newState.isCategoryExpanded("INTERESTS"))
    }

    @Test
    fun `toggleCategory removes category when expanded`() {
        val state = PersonaUiState(expandedCategories = setOf("INTERESTS"))
        val newState = state.toggleCategory("INTERESTS")
        
        assertFalse(newState.isCategoryExpanded("INTERESTS"))
    }

    // ============================================================
    // 分类标签测试
    // ============================================================

    @Test
    fun `PersonaUiState with category tags is valid`() {
        val categoryTags = mapOf(
            "INTERESTS" to listOf(
                PersonaTag(id = "1", name = "音乐", category = "INTERESTS"),
                PersonaTag(id = "2", name = "电影", category = "INTERESTS")
            ),
            "WORK" to listOf(
                PersonaTag(id = "3", name = "程序员", category = "WORK")
            )
        )
        val state = PersonaUiState(categoryTags = categoryTags)
        
        assertEquals(2, state.categoryTags.size)
        assertEquals(2, state.getTagsForCategory("INTERESTS").size)
        assertEquals(1, state.getTagsForCategory("WORK").size)
    }

    @Test
    fun `getTagsForCategory returns empty list for unknown category`() {
        val state = PersonaUiState(categoryTags = emptyMap())
        
        assertTrue(state.getTagsForCategory("UNKNOWN").isEmpty())
    }

    @Test
    fun `getTagCountForCategory returns correct count`() {
        val categoryTags = mapOf(
            "INTERESTS" to listOf(
                PersonaTag(id = "1", name = "音乐", category = "INTERESTS"),
                PersonaTag(id = "2", name = "电影", category = "INTERESTS")
            )
        )
        val state = PersonaUiState(categoryTags = categoryTags)
        
        assertEquals(2, state.getTagCountForCategory("INTERESTS"))
        assertEquals(0, state.getTagCountForCategory("WORK"))
    }

    // ============================================================
    // 搜索过滤测试
    // ============================================================

    @Test
    fun `filteredCategoryTags returns all tags when no search query`() {
        val categoryTags = mapOf(
            "INTERESTS" to listOf(
                PersonaTag(id = "1", name = "音乐", category = "INTERESTS"),
                PersonaTag(id = "2", name = "电影", category = "INTERESTS")
            )
        )
        val state = PersonaUiState(categoryTags = categoryTags, searchQuery = "")
        
        assertEquals(2, state.filteredCategoryTags["INTERESTS"]?.size)
    }

    @Test
    fun `filteredCategoryTags filters tags by search query`() {
        val categoryTags = mapOf(
            "INTERESTS" to listOf(
                PersonaTag(id = "1", name = "音乐", category = "INTERESTS"),
                PersonaTag(id = "2", name = "电影", category = "INTERESTS")
            )
        )
        val state = PersonaUiState(categoryTags = categoryTags, searchQuery = "音乐")
        
        assertEquals(1, state.filteredCategoryTags["INTERESTS"]?.size)
        assertEquals("音乐", state.filteredCategoryTags["INTERESTS"]?.first()?.name)
    }

    @Test
    fun `filteredCategoryTags returns empty when no tags match`() {
        val categoryTags = mapOf(
            "INTERESTS" to listOf(
                PersonaTag(id = "1", name = "音乐", category = "INTERESTS")
            )
        )
        val state = PersonaUiState(categoryTags = categoryTags, searchQuery = "不存在")
        
        assertTrue(state.filteredCategoryTags["INTERESTS"]?.isEmpty() ?: true)
    }

    // ============================================================
    // AI推测标签测试
    // ============================================================

    @Test
    fun `PersonaUiState with inferred tags is valid`() {
        val inferredTags = listOf(
            InferredTag(id = "1", name = "喜欢旅行", source = "聊天记录分析"),
            InferredTag(id = "2", name = "爱吃甜食", source = "AI推测")
        )
        val state = PersonaUiState(inferredTags = inferredTags)
        
        assertEquals(2, state.inferredTags.size)
        assertTrue(state.hasInferredTags)
    }

    @Test
    fun `PersonaUiState with no inferred tags`() {
        val state = PersonaUiState(inferredTags = emptyList())
        
        assertTrue(state.inferredTags.isEmpty())
        assertFalse(state.hasInferredTags)
    }

    @Test
    fun `confirmInferredTag removes tag from inferred list`() {
        val inferredTags = listOf(
            InferredTag(id = "1", name = "喜欢旅行", source = "AI推测"),
            InferredTag(id = "2", name = "爱吃甜食", source = "AI推测")
        )
        val state = PersonaUiState(inferredTags = inferredTags)
        val newState = state.removeInferredTag("1")
        
        assertEquals(1, newState.inferredTags.size)
        assertEquals("2", newState.inferredTags.first().id)
    }

    @Test
    fun `rejectInferredTag removes tag from inferred list`() {
        val inferredTags = listOf(
            InferredTag(id = "1", name = "喜欢旅行", source = "AI推测")
        )
        val state = PersonaUiState(inferredTags = inferredTags)
        val newState = state.removeInferredTag("1")
        
        assertTrue(newState.inferredTags.isEmpty())
    }

    // ============================================================
    // 加载和错误状态测试
    // ============================================================

    @Test
    fun `PersonaUiState loading state is correct`() {
        val state = PersonaUiState(isLoading = true)
        
        assertTrue(state.isLoading)
    }

    @Test
    fun `PersonaUiState error state is correct`() {
        val errorMessage = "Failed to load tags"
        val state = PersonaUiState(error = errorMessage)
        
        assertEquals(errorMessage, state.error)
        assertTrue(state.hasError)
    }

    @Test
    fun `PersonaUiState without error has no error`() {
        val state = PersonaUiState(error = null)
        
        assertEquals(null, state.error)
        assertFalse(state.hasError)
    }

    // ============================================================
    // PersonaTag 测试
    // ============================================================

    @Test
    fun `PersonaTag creation is correct`() {
        val tag = PersonaTag(
            id = "test-id",
            name = "测试标签",
            category = "INTERESTS",
            source = "手动添加",
            createdAt = 1234567890L
        )
        
        assertEquals("test-id", tag.id)
        assertEquals("测试标签", tag.name)
        assertEquals("INTERESTS", tag.category)
        assertEquals("手动添加", tag.source)
        assertEquals(1234567890L, tag.createdAt)
    }

    // ============================================================
    // InferredTag 测试
    // ============================================================

    @Test
    fun `InferredTag creation is correct`() {
        val tag = InferredTag(
            id = "inferred-1",
            name = "推测标签",
            source = "聊天记录分析",
            confidence = 0.85f
        )
        
        assertEquals("inferred-1", tag.id)
        assertEquals("推测标签", tag.name)
        assertEquals("聊天记录分析", tag.source)
        assertEquals(0.85f, tag.confidence, 0.001f)
    }

    @Test
    fun `InferredTag with high confidence is confident`() {
        val tag = InferredTag(id = "1", name = "标签", source = "AI", confidence = 0.9f)
        
        assertTrue(tag.isHighConfidence)
    }

    @Test
    fun `InferredTag with low confidence is not confident`() {
        val tag = InferredTag(id = "1", name = "标签", source = "AI", confidence = 0.5f)
        
        assertFalse(tag.isHighConfidence)
    }
}
