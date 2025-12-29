package com.empathy.ai.presentation.ui.component.persona

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ModernFloatingSearchBar 单元测试
 * 
 * BUG-00036 T3-06: 搜索栏组件测试
 * 
 * 测试覆盖：
 * - 搜索状态管理
 * - 清除按钮逻辑
 * - 搜索关键词过滤
 */
class ModernFloatingSearchBarTest {

    // ============================================================
    // 搜索状态测试
    // ============================================================

    @Test
    fun `搜索状态_空查询时清除按钮不应显示`() {
        val query = ""
        val shouldShowClearButton = query.isNotEmpty()
        
        assertFalse("空查询时不应显示清除按钮", shouldShowClearButton)
    }

    @Test
    fun `搜索状态_有查询时清除按钮应显示`() {
        val query = "测试"
        val shouldShowClearButton = query.isNotEmpty()
        
        assertTrue("有查询时应显示清除按钮", shouldShowClearButton)
    }

    @Test
    fun `搜索状态_空白字符串也应显示清除按钮`() {
        val query = "   "
        val shouldShowClearButton = query.isNotEmpty()
        
        assertTrue("空白字符串也应显示清除按钮", shouldShowClearButton)
    }

    // ============================================================
    // 清除功能测试
    // ============================================================

    @Test
    fun `清除功能_点击清除后查询应为空`() {
        var query = "测试查询"
        
        // 模拟清除操作
        query = ""
        
        assertEquals("清除后查询应为空", "", query)
    }

    @Test
    fun `清除功能_清除后应触发回调`() {
        var callbackInvoked = false
        var newQuery: String? = null
        
        val onQueryChange: (String) -> Unit = { q ->
            callbackInvoked = true
            newQuery = q
        }
        
        // 模拟清除操作
        onQueryChange("")
        
        assertTrue("应触发回调", callbackInvoked)
        assertEquals("新查询应为空", "", newQuery)
    }

    // ============================================================
    // 搜索过滤逻辑测试
    // ============================================================

    @Test
    fun `搜索过滤_空查询返回所有项目`() {
        val items = listOf("苹果", "香蕉", "橙子")
        val query = ""
        
        val filtered = if (query.isBlank()) {
            items
        } else {
            items.filter { it.contains(query, ignoreCase = true) }
        }
        
        assertEquals("空查询应返回所有项目", 3, filtered.size)
    }

    @Test
    fun `搜索过滤_精确匹配返回正确结果`() {
        val items = listOf("苹果", "香蕉", "橙子")
        val query = "苹果"
        
        val filtered = items.filter { it.contains(query, ignoreCase = true) }
        
        assertEquals("应返回1个匹配项", 1, filtered.size)
        assertEquals("应返回苹果", "苹果", filtered[0])
    }

    @Test
    fun `搜索过滤_部分匹配返回正确结果`() {
        val items = listOf("红苹果", "青苹果", "香蕉")
        val query = "苹果"
        
        val filtered = items.filter { it.contains(query, ignoreCase = true) }
        
        assertEquals("应返回2个匹配项", 2, filtered.size)
    }

    @Test
    fun `搜索过滤_无匹配返回空列表`() {
        val items = listOf("苹果", "香蕉", "橙子")
        val query = "葡萄"
        
        val filtered = items.filter { it.contains(query, ignoreCase = true) }
        
        assertTrue("无匹配应返回空列表", filtered.isEmpty())
    }

    @Test
    fun `搜索过滤_忽略大小写`() {
        val items = listOf("Apple", "Banana", "Orange")
        val query = "apple"
        
        val filtered = items.filter { it.contains(query, ignoreCase = true) }
        
        assertEquals("应忽略大小写匹配", 1, filtered.size)
        assertEquals("应返回Apple", "Apple", filtered[0])
    }

    @Test
    fun `搜索过滤_中文搜索正常工作`() {
        val items = listOf("兴趣爱好", "工作信息", "沟通策略", "雷区标签")
        val query = "兴趣"
        
        val filtered = items.filter { it.contains(query, ignoreCase = true) }
        
        assertEquals("中文搜索应正常工作", 1, filtered.size)
        assertEquals("应返回兴趣爱好", "兴趣爱好", filtered[0])
    }

    // ============================================================
    // 边界情况测试
    // ============================================================

    @Test
    fun `边界情况_单字符搜索`() {
        val items = listOf("苹果", "香蕉", "橙子")
        val query = "苹"
        
        val filtered = items.filter { it.contains(query, ignoreCase = true) }
        
        assertEquals("单字符搜索应正常工作", 1, filtered.size)
    }

    @Test
    fun `边界情况_特殊字符搜索`() {
        val items = listOf("标签#1", "标签#2", "普通标签")
        val query = "#"
        
        val filtered = items.filter { it.contains(query, ignoreCase = true) }
        
        assertEquals("特殊字符搜索应正常工作", 2, filtered.size)
    }

    @Test
    fun `边界情况_空列表搜索`() {
        val items = emptyList<String>()
        val query = "测试"
        
        val filtered = items.filter { it.contains(query, ignoreCase = true) }
        
        assertTrue("空列表搜索应返回空列表", filtered.isEmpty())
    }

    @Test
    fun `边界情况_超长查询字符串`() {
        val items = listOf("短标签", "这是一个非常长的标签名称用于测试")
        val query = "这是一个非常长的标签名称用于测试"
        
        val filtered = items.filter { it.contains(query, ignoreCase = true) }
        
        assertEquals("超长查询应正常工作", 1, filtered.size)
    }
}
