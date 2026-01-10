package com.empathy.ai.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 底部导航Tab枚举测试
 *
 * ## 业务规则 (PRD-00034)
 * - 底部导航Tab与NavRoutes.BOTTOM_NAV_ROUTES保持一致
 * - Tab切换通过路由字符串匹配确定目标
 *
 * ## 测试策略
 * - 使用参数化测试覆盖所有枚举值
 * - 边界测试覆盖null和未知路由
 *
 * ## 任务追踪
 * - FD: FD-00034-界面切换性能优化-页面缓存方案
 * - Task: T34-04 [US2] 创建 BottomNavTab 枚举
 *
 * ## 测试用例来源
 * - TS-00034-FT-001: Tab切换无黑屏
 * - TS-00034-FT-005: 配置变更后状态恢复
 */
class BottomNavTabTest {

    // ============================================================
    // 枚举值测试
    // ============================================================

    @Test
    fun `BottomNavTab has exactly three tabs`() {
        assertEquals(3, BottomNavTab.entries.size)
    }

    @Test
    fun `BottomNavTab CONTACTS has correct route`() {
        assertEquals(NavRoutes.CONTACT_LIST, BottomNavTab.CONTACTS.route)
    }

    @Test
    fun `BottomNavTab AI_ADVISOR has correct route`() {
        assertEquals(NavRoutes.AI_ADVISOR, BottomNavTab.AI_ADVISOR.route)
    }

    @Test
    fun `BottomNavTab SETTINGS has correct route`() {
        assertEquals(NavRoutes.SETTINGS, BottomNavTab.SETTINGS.route)
    }

    // ============================================================
    // fromRoute 路由解析测试 (参数化)
    // ============================================================

    @Test
    fun `fromRoute returns CONTACTS for contact_list`() {
        val result = BottomNavTab.fromRoute("contact_list")
        assertEquals(BottomNavTab.CONTACTS, result)
    }

    @Test
    fun `fromRoute returns AI_ADVISOR for ai_advisor`() {
        val result = BottomNavTab.fromRoute("ai_advisor")
        assertEquals(BottomNavTab.AI_ADVISOR, result)
    }

    @Test
    fun `fromRoute returns SETTINGS for settings`() {
        val result = BottomNavTab.fromRoute("settings")
        assertEquals(BottomNavTab.SETTINGS, result)
    }

    // ============================================================
    // 边界条件测试
    // ============================================================

    @Test
    fun `fromRoute returns null for null input`() {
        val result = BottomNavTab.fromRoute(null)
        assertNull(result)
    }

    @Test
    fun `fromRoute returns null for unknown route`() {
        val result = BottomNavTab.fromRoute("unknown_route")
        assertNull(result)
    }

    @Test
    fun `fromRoute returns null for empty string`() {
        val result = BottomNavTab.fromRoute("")
        assertNull(result)
    }

    @Test
    fun `fromRoute returns null for partial route match`() {
        val result = BottomNavTab.fromRoute("contact")
        assertNull(result)
    }

    // ============================================================
    // NavRoutes 一致性测试
    // ============================================================

    @Test
    fun `BottomNavTab routes match BOTTOM_NAV_ROUTES`() {
        val bottomNavRoutes = NavRoutes.BOTTOM_NAV_ROUTES

        assertEquals(3, bottomNavRoutes.size)
        assertTrue(bottomNavRoutes.contains(BottomNavTab.CONTACTS.route))
        assertTrue(bottomNavRoutes.contains(BottomNavTab.AI_ADVISOR.route))
        assertTrue(bottomNavRoutes.contains(BottomNavTab.SETTINGS.route))
    }

    @Test
    fun `BottomNavTab entries order matches BOTTOM_NAV_ROUTES`() {
        val entries = BottomNavTab.entries.map { it.route }
        val bottomNavRoutes = NavRoutes.BOTTOM_NAV_ROUTES

        assertEquals(bottomNavRoutes[0], entries[0])
        assertEquals(bottomNavRoutes[1], entries[1])
        assertEquals(bottomNavRoutes[2], entries[2])
    }

    // ============================================================
    // 循环一致性测试 (Property-Based)
    // ============================================================

    @Test
    fun `fromRoute is inverse of route property`() {
        BottomNavTab.entries.forEach { tab ->
            val result = BottomNavTab.fromRoute(tab.route)
            assertEquals(tab, result)
        }
    }

    @Test
    fun `all BOTTOM_NAV_ROUTES can be parsed by fromRoute`() {
        NavRoutes.BOTTOM_NAV_ROUTES.forEach { route ->
            val result = BottomNavTab.fromRoute(route)
            assertTrue("Route '$route' should be parseable", result != null)
        }
    }
}
