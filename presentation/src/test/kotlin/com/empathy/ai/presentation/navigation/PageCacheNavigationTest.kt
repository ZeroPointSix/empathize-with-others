package com.empathy.ai.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 页面缓存导航集成测试
 *
 * ## 业务规则 (PRD-00034)
 * - 非Tab页面使用独立的NonTabNavGraph导航
 * - Tab页面使用BottomNavScaffold进行缓存管理
 * - 页面切换无黑屏闪动
 *
 * ## 测试策略
 * - 路由配置正确性验证
 * - 页面缓存参数验证
 * - 导航行为一致性测试
 *
 * ## 任务追踪
 * - FD: FD-00034-页面切换性能优化-页面缓存方案
 * - Task: T34-08 [US2] 重构MainActivity接入BottomNavScaffold
 *
 * ## 测试用例来源
 * - TS-00034-FT-001: Tab切换无黑屏
 * - TS-00034-FT-004: 非Tab页面导航正常
 */
class PageCacheNavigationTest {

    // ============================================================
    // AI_CONFIG 路由参数测试
    // ============================================================

    @Test
    fun `AI_CONFIG route has source argument`() {
        assertEquals("source", NavRoutes.AI_CONFIG_ARG_SOURCE)
    }

    @Test
    fun `AI_CONFIG_ROUTE contains source parameter`() {
        assertEquals("ai_config?source={source}", NavRoutes.AI_CONFIG_ROUTE)
    }

    @Test
    fun `source constants are defined`() {
        assertEquals("settings_tab", NavRoutes.SOURCE_SETTINGS)
        assertEquals("advisor_chat", NavRoutes.SOURCE_ADVISOR_CHAT)
    }

    // ============================================================
    // aiConfig 辅助函数测试
    // ============================================================

    @Test
    fun `aiConfig with null source returns base route`() {
        val route = NavRoutes.aiConfig(null)
        assertEquals(NavRoutes.AI_CONFIG, route)
    }

    @Test
    fun `aiConfig with empty source returns base route`() {
        val route = NavRoutes.aiConfig("")
        assertEquals(NavRoutes.AI_CONFIG, route)
    }

    @Test
    fun `aiConfig with settings source returns parameterized route`() {
        val route = NavRoutes.aiConfig(NavRoutes.SOURCE_SETTINGS)
        assertEquals("ai_config?source=settings_tab", route)
    }

    @Test
    fun `aiConfig with advisor_chat source returns parameterized route`() {
        val route = NavRoutes.aiConfig(NavRoutes.SOURCE_ADVISOR_CHAT)
        assertEquals("ai_config?source=advisor_chat", route)
    }

    // ============================================================
    // BOTTOM_NAV_ROUTES 完整性测试
    // ============================================================

    @Test
    fun `BOTTOM_NAV_ROUTES contains all expected routes`() {
        val routes = NavRoutes.BOTTOM_NAV_ROUTES

        assertNotNull(routes)
        assertEquals(3, routes.size)
        assertTrue(routes.contains(NavRoutes.CONTACT_LIST))
        assertTrue(routes.contains(NavRoutes.AI_ADVISOR))
        assertTrue(routes.contains(NavRoutes.SETTINGS))
    }

    @Test
    fun `BOTTOM_NAV_ROUTES does not contain non-tab routes`() {
        val routes = NavRoutes.BOTTOM_NAV_ROUTES

        assertFalse(routes.contains(NavRoutes.CONTACT_DETAIL))
        assertFalse(routes.contains(NavRoutes.CHAT))
        assertFalse(routes.contains(NavRoutes.AI_ADVISOR_CHAT))
        assertFalse(routes.contains(NavRoutes.CREATE_CONTACT))
    }

    // ============================================================
    // AI军师路由测试
    // ============================================================

    @Test
    fun `AI_ADVISOR_CHAT route contains all parameters`() {
        val route = NavRoutes.AI_ADVISOR_CHAT

        assertTrue(route.contains("contactId"))
        assertTrue(route.contains("createNew"))
        assertTrue(route.contains("sessionId"))
    }

    @Test
    fun `aiAdvisorChat with default parameters`() {
        val route = NavRoutes.aiAdvisorChat("contact-123")

        assertEquals("ai_advisor_chat/contact-123?createNew=false", route)
        assertFalse(route.contains("sessionId"))
    }

    @Test
    fun `aiAdvisorChat with createNew true`() {
        val route = NavRoutes.aiAdvisorChat("contact-123", createNew = true)

        assertEquals("ai_advisor_chat/contact-123?createNew=true", route)
    }

    @Test
    fun `aiAdvisorChat with sessionId`() {
        val route = NavRoutes.aiAdvisorChat("contact-123", sessionId = "session-456")

        assertTrue(route.contains("sessionId=session-456"))
    }

    @Test
    fun `aiAdvisorChat with both createNew and sessionId`() {
        val route = NavRoutes.aiAdvisorChat("contact-123", createNew = true, sessionId = "session-456")

        assertTrue(route.contains("createNew=true"))
        assertTrue(route.contains("sessionId=session-456"))
    }

    // ============================================================
    // 非Tab路由验证测试
    // ============================================================

    @Test
    fun `CONTACT_DETAIL_TAB is a non-tab route`() {
        val nonTabRoutes = listOf(
            NavRoutes.CONTACT_DETAIL_TAB,
            NavRoutes.CHAT,
            NavRoutes.AI_CONFIG,
            NavRoutes.CREATE_CONTACT,
            NavRoutes.USER_PROFILE,
            NavRoutes.BRAIN_TAG
        )

        nonTabRoutes.forEach { route ->
            assertFalse("$route should not be in BOTTOM_NAV_ROUTES",
                NavRoutes.BOTTOM_NAV_ROUTES.contains(route))
        }
    }

    // ============================================================
    // AI军师会话路由测试
    // ============================================================

    @Test
    fun `AI_ADVISOR_SESSIONS route is valid`() {
        assertEquals("ai_advisor_sessions/{contactId}", NavRoutes.AI_ADVISOR_SESSIONS)
        assertEquals("contactId", NavRoutes.AI_ADVISOR_SESSIONS_ARG_ID)
    }

    @Test
    fun `aiAdvisorSessions creates correct route`() {
        val route = NavRoutes.aiAdvisorSessions("contact-789")
        assertEquals("ai_advisor_sessions/contact-789", route)
    }

    @Test
    fun `AI_ADVISOR_CONTACTS is a non-tab route`() {
        assertEquals("ai_advisor_contacts", NavRoutes.AI_ADVISOR_CONTACTS)
        assertFalse(NavRoutes.BOTTOM_NAV_ROUTES.contains(NavRoutes.AI_ADVISOR_CONTACTS))
    }

    // ============================================================
    // 系统提示词路由测试
    // ============================================================

    @Test
    fun `SYSTEM_PROMPT_LIST is a non-tab route`() {
        assertEquals("system_prompt_list", NavRoutes.SYSTEM_PROMPT_LIST)
        assertFalse(NavRoutes.BOTTOM_NAV_ROUTES.contains(NavRoutes.SYSTEM_PROMPT_LIST))
    }

    @Test
    fun `SYSTEM_PROMPT_EDIT route contains scene parameter`() {
        assertEquals("system_prompt_edit/{scene}", NavRoutes.SYSTEM_PROMPT_EDIT)
        assertEquals("scene", NavRoutes.SYSTEM_PROMPT_EDIT_ARG_SCENE)
    }

    @Test
    fun `systemPromptEdit creates correct route`() {
        val route = NavRoutes.systemPromptEdit("ANALYZE")
        assertEquals("system_prompt_edit/ANALYZE", route)
    }
}
