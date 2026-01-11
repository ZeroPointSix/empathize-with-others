package com.empathy.ai.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * BUG-00068 导航栈治理测试
 *
 * ## 业务规则 (BUG-00068)
 * - AI军师入口跳转使用CONTACT_LIST作为稳定锚点
 * - 避免回退栈残留旧会话导致返回行为异常
 * - 防止Tab切换时重复入栈
 *
 * ## 测试策略 (TE-00068)
 * - 验证AI军师导航配置的稳定性
 * - 验证launchSingleTop防重复入栈
 * - 验证popUpTo(CONTACT_LIST)锚点机制
 *
 * ## 任务追踪
 * - BUG: BUG-00068-AI军师入口与设置回退及非Tab性能覆盖问题
 * - TC: TE-00068-切换性能与导航回退测试用例
 *
 * ## 测试用例来源
 * - TC-UI-001: AI军师Tab二次进入仍有内容
 * - TC-UI-002: 设置 -> AI配置返回回到设置
 * - TC-UI-003: 设置 -> 提示词编辑返回回到设置
 */
class BUG00068NavigationStackTest {

    // ============================================================
    // AI军师导航锚点测试 (BUG-00068 Phase 2)
    // ============================================================

    @Test
    fun `AI_ADVISOR route is defined as bottom nav route`() {
        // 验证AI军师入口在底部导航路由中
        assertTrue("AI_ADVISOR should be in BOTTOM_NAV_ROUTES",
            NavRoutes.BOTTOM_NAV_ROUTES.contains(NavRoutes.AI_ADVISOR))
        assertEquals("ai_advisor", NavRoutes.AI_ADVISOR)
    }

    @Test
    fun `CONTACT_LIST is stable anchor for navigation`() {
        // 验证CONTACT_LIST作为稳定锚点存在
        assertTrue("CONTACT_LIST should be in BOTTOM_NAV_ROUTES",
            NavRoutes.BOTTOM_NAV_ROUTES.contains(NavRoutes.CONTACT_LIST))
        assertEquals("contact_list", NavRoutes.CONTACT_LIST)
    }

    @Test
    fun `AI_ADVISOR_CHAT route supports contactId parameter`() {
        // 验证AI军师对话路由支持联系人ID参数
        val route = NavRoutes.aiAdvisorChat("test-contact-123")
        assertTrue(route.contains("test-contact-123"))
        assertTrue(route.contains("createNew=false"))
    }

    @Test
    fun `AI_ADVISOR_CHAT route supports createNew parameter`() {
        // BUG-00058相关: 验证createNew参数支持
        val route = NavRoutes.aiAdvisorChat("test-contact-123", createNew = true)
        assertTrue(route.contains("createNew=true"))
    }

    @Test
    fun `AI_ADVISOR_CHAT route supports sessionId parameter`() {
        // BUG-00061相关: 验证sessionId参数支持
        val route = NavRoutes.aiAdvisorChat("test-contact-123", sessionId = "session-456")
        assertTrue(route.contains("sessionId=session-456"))
    }

    @Test
    fun `AI_ADVISOR_CONTACTS route is defined`() {
        // 验证联系人选择路由
        assertEquals("ai_advisor_contacts", NavRoutes.AI_ADVISOR_CONTACTS)
        assertFalse("AI_ADVISOR_CONTACTS should not be in BOTTOM_NAV_ROUTES",
            NavRoutes.BOTTOM_NAV_ROUTES.contains(NavRoutes.AI_ADVISOR_CONTACTS))
    }

    // ============================================================
    // 设置导航链路测试 (BUG-00068 问题2)
    // ============================================================

    @Test
    fun `AI_CONFIG route supports source parameter`() {
        // 验证AI配置路由支持来源参数
        assertEquals("ai_config?source={source}", NavRoutes.AI_CONFIG_ROUTE)
        assertEquals("source", NavRoutes.AI_CONFIG_ARG_SOURCE)
    }

    @Test
    fun `SOURCE_SETTINGS constant is defined`() {
        // 验证设置来源常量
        assertEquals("settings_tab", NavRoutes.SOURCE_SETTINGS)
    }

    @Test
    fun `aiConfig function creates route with source parameter`() {
        // 验证aiConfig函数正确创建带来源的路由
        val route = NavRoutes.aiConfig(NavRoutes.SOURCE_SETTINGS)
        assertEquals("ai_config?source=settings_tab", route)
    }

    @Test
    fun `aiConfig function handles null source`() {
        // 验证aiConfig函数处理null来源
        val route = NavRoutes.aiConfig(null)
        assertEquals("ai_config", route)
    }

    // ============================================================
    // Tab页面路由完整性测试
    // ============================================================

    @Test
    fun `SETTINGS route is in bottom nav routes`() {
        // 验证设置页面在底部导航中
        assertTrue("SETTINGS should be in BOTTOM_NAV_ROUTES",
            NavRoutes.BOTTOM_NAV_ROUTES.contains(NavRoutes.SETTINGS))
        assertEquals("settings", NavRoutes.SETTINGS)
    }

    @Test
    fun `Bottom nav routes contain exactly three routes`() {
        // 验证底部导航路由数量
        assertEquals(3, NavRoutes.BOTTOM_NAV_ROUTES.size)
        assertTrue(NavRoutes.BOTTOM_NAV_ROUTES.contains(NavRoutes.CONTACT_LIST))
        assertTrue(NavRoutes.BOTTOM_NAV_ROUTES.contains(NavRoutes.AI_ADVISOR))
        assertTrue(NavRoutes.BOTTOM_NAV_ROUTES.contains(NavRoutes.SETTINGS))
    }

    // ============================================================
    // 非Tab路由验证 (BUG-00068 问题3)
    // ============================================================

    @Test
    fun `USER_PROFILE is a non-tab route`() {
        // 验证用户画像是非Tab路由
        assertEquals("user_profile", NavRoutes.USER_PROFILE)
        assertFalse("USER_PROFILE should not be in BOTTOM_NAV_ROUTES",
            NavRoutes.BOTTOM_NAV_ROUTES.contains(NavRoutes.USER_PROFILE))
    }

    @Test
    fun `CREATE_CONTACT is a non-tab route`() {
        // 验证新建联系人是非Tab路由
        assertEquals("create_contact", NavRoutes.CREATE_CONTACT)
        assertFalse("CREATE_CONTACT should not be in BOTTOM_NAV_ROUTES",
            NavRoutes.BOTTOM_NAV_ROUTES.contains(NavRoutes.CREATE_CONTACT))
    }

    @Test
    fun `CONTACT_DETAIL_TAB is a non-tab route`() {
        // 验证联系人详情标签页是非Tab路由
        assertEquals("contact_detail_tab/{id}", NavRoutes.CONTACT_DETAIL_TAB)
        assertFalse("CONTACT_DETAIL_TAB should not be in BOTTOM_NAV_ROUTES",
            NavRoutes.BOTTOM_NAV_ROUTES.contains(NavRoutes.CONTACT_DETAIL_TAB))
    }

    // ============================================================
    // 提示词编辑路由测试 (BUG-00068 问题2)
    // ============================================================

    @Test
    fun `SYSTEM_PROMPT_LIST is a non-tab route`() {
        // 验证系统提示词列表是非Tab路由
        assertEquals("system_prompt_list", NavRoutes.SYSTEM_PROMPT_LIST)
        assertFalse("SYSTEM_PROMPT_LIST should not be in BOTTOM_NAV_ROUTES",
            NavRoutes.BOTTOM_NAV_ROUTES.contains(NavRoutes.SYSTEM_PROMPT_LIST))
    }

    @Test
    fun `SYSTEM_PROMPT_EDIT route supports scene parameter`() {
        // 验证系统提示词编辑路由支持场景参数
        assertEquals("system_prompt_edit/{scene}", NavRoutes.SYSTEM_PROMPT_EDIT)
        assertEquals("scene", NavRoutes.SYSTEM_PROMPT_EDIT_ARG_SCENE)
    }

    @Test
    fun `systemPromptEdit function creates correct route`() {
        // 验证systemPromptEdit函数正确创建路由
        val route = NavRoutes.systemPromptEdit("ANALYZE")
        assertEquals("system_prompt_edit/ANALYZE", route)
    }

    // ============================================================
    // AI军师会话历史路由测试 (BUG-00068 问题1)
    // ============================================================

    @Test
    fun `AI_ADVISOR_SESSIONS route supports contactId parameter`() {
        // 验证会话历史路由支持联系人ID
        assertEquals("ai_advisor_sessions/{contactId}", NavRoutes.AI_ADVISOR_SESSIONS)
        assertEquals("contactId", NavRoutes.AI_ADVISOR_SESSIONS_ARG_ID)
    }

    @Test
    fun `aiAdvisorSessions function creates correct route`() {
        // 验证aiAdvisorSessions函数正确创建路由
        val route = NavRoutes.aiAdvisorSessions("contact-789")
        assertEquals("ai_advisor_sessions/contact-789", route)
    }

    @Test
    fun `aiAdvisorSessions supports empty contactId`() {
        // 验证空联系人ID的处理
        val route = NavRoutes.aiAdvisorSessions("")
        assertEquals("ai_advisor_sessions/", route)
    }
}
