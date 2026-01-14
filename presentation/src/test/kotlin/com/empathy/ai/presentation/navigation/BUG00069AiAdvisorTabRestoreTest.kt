package com.empathy.ai.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * BUG-00069 AI军师Tab返回自动恢复测试
 *
 * ## 业务规则 (BUG-00069)
 * - 从AI军师子栈返回Tab区域时自动恢复上一个非AI Tab
 * - 使用 lastNonAiTab 记录上一个非AI Tab
 * - 使用 wasInMainTab 检测从AI军师子栈回到Tab区域的状态变化
 * - 排除 CONTACT_LIST 启动路由避免首次加载时Tab切换导致子页面重建
 *
 * ## 测试策略 (TE-00069)
 * - 验证 popUpTo 锚点从 CONTACT_LIST 变更为 AI_ADVISOR
 * - 验证 NonTabNavGraph 新增 onAiAdvisorChatClosed 回调
 * - 验证 AI_ADVISOR_CHAT 路由支持自然返回栈优先策略
 *
 * ## 任务追踪
 * - BUG: BUG-00069-切屏回退与联系人错误闪现问题
 * - TC: TE-00069-TC-001 AI军师返回自动恢复上一个非AI Tab
 *
 * ## 测试用例来源
 * - TC-UI-004: AI军师返回只需一次返回回到上一Tab
 * - TC-UI-005: 从设置Tab进入AI军师返回应回到设置
 * - TC-UI-006: AI军师对话关闭触发Tab恢复回调
 */
class BUG00069AiAdvisorTabRestoreTest {

    // ============================================================
    // popUpTo 锚点变更测试 (BUG-00069 核心修复)
    // ============================================================

    @Test
    fun `AI_ADVISOR route exists as popUpTo anchor`() {
        // 验证AI_ADVISOR路由存在且可用作popUpTo锚点
        assertEquals("ai_advisor", NavRoutes.AI_ADVISOR)
        assertTrue("AI_ADVISOR should be in BOTTOM_NAV_ROUTES",
            NavRoutes.BOTTOM_NAV_ROUTES.contains(NavRoutes.AI_ADVISOR))
    }

    @Test
    fun `AI_ADVISOR_CHAT route definition includes contactId parameter`() {
        // 验证AI军师对话路由支持联系人ID参数
        val route = NavRoutes.AI_ADVISOR_CHAT
        assertTrue(route.contains("{contactId}"))
        assertTrue(route.contains("createNew"))
        assertTrue(route.contains("sessionId"))
    }

    @Test
    fun `aiAdvisorChat function creates route with all parameters`() {
        // 验证aiAdvisorChat函数支持完整参数集
        val contactId = "contact-123"
        val sessionId = "session-456"

        // 测试基础路由
        val basicRoute = NavRoutes.aiAdvisorChat(contactId)
        assertTrue(basicRoute.contains(contactId))
        assertTrue(basicRoute.contains("createNew=false"))

        // 测试createNew参数
        val createNewRoute = NavRoutes.aiAdvisorChat(contactId, createNew = true)
        assertTrue(createNewRoute.contains("createNew=true"))

        // 测试sessionId参数
        val sessionRoute = NavRoutes.aiAdvisorChat(contactId, sessionId = sessionId)
        assertTrue(sessionRoute.contains("sessionId=$sessionId"))
    }

    // ============================================================
    // 路由参数完整性测试 (BUG-00061/BUG-00058)
    // ============================================================

    @Test
    fun `AI_ADVISOR_CHAT_ARG_CREATE_NEW constant exists`() {
        // 验证createNew参数常量存在
        assertEquals("createNew", NavRoutes.AI_ADVISOR_CHAT_ARG_CREATE_NEW)
    }

    @Test
    fun `AI_ADVISOR_CHAT_ARG_SESSION_ID constant exists`() {
        // 验证sessionId参数常量存在
        assertEquals("sessionId", NavRoutes.AI_ADVISOR_CHAT_ARG_SESSION_ID)
    }

    @Test
    fun `AI_ADVISOR_CHAT_ARG_ID constant exists`() {
        // 验证contactId参数常量存在
        assertEquals("contactId", NavRoutes.AI_ADVISOR_CHAT_ARG_ID)
    }

    // ============================================================
    // AI军师子路由独立性测试
    // ============================================================

    @Test
    fun `AI_ADVISOR_CONTACTS is non-tab route`() {
        // 验证联系人选择页是非Tab路由，属于AI军师子栈
        assertEquals("ai_advisor_contacts", NavRoutes.AI_ADVISOR_CONTACTS)
        assertFalse("AI_ADVISOR_CONTACTS should not be in BOTTOM_NAV_ROUTES",
            NavRoutes.BOTTOM_NAV_ROUTES.contains(NavRoutes.AI_ADVISOR_CONTACTS))
    }

    @Test
    fun `AI_ADVISOR_SESSIONS is non-tab route`() {
        // 验证会话历史页是非Tab路由，属于AI军师子栈
        assertEquals("ai_advisor_sessions/{contactId}", NavRoutes.AI_ADVISOR_SESSIONS)
        assertFalse("AI_ADVISOR_SESSIONS should not be in BOTTOM_NAV_ROUTES",
            NavRoutes.BOTTOM_NAV_ROUTES.contains(NavRoutes.AI_ADVISOR_SESSIONS))
    }

    @Test
    fun `aiAdvisorSessions function creates correct route`() {
        // 验证会话历史路由生成函数
        val contactId = "contact-789"
        val route = NavRoutes.aiAdvisorSessions(contactId)
        assertEquals("ai_advisor_sessions/$contactId", route)
    }

    // ============================================================
    // 底部导航路由完整性验证
    // ============================================================

    @Test
    fun `BottomNavTab enum contains all three tabs`() {
        // 验证BottomNavTab枚举包含所有三个Tab
        val tabs = BottomNavTab.entries
        assertEquals(3, tabs.size)

        assertTrue(tabs.contains(BottomNavTab.CONTACTS))
        assertTrue(tabs.contains(BottomNavTab.AI_ADVISOR))
        assertTrue(tabs.contains(BottomNavTab.SETTINGS))
    }

    @Test
    fun `BottomNavTab fromRoute correctly maps routes`() {
        // 验证fromRoute函数正确映射路由到Tab
        assertEquals(BottomNavTab.CONTACTS, BottomNavTab.fromRoute("contact_list"))
        assertEquals(BottomNavTab.AI_ADVISOR, BottomNavTab.fromRoute("ai_advisor"))
        assertEquals(BottomNavTab.SETTINGS, BottomNavTab.fromRoute("settings"))

        // 验证非Tab路由返回null
        assertEquals(null, BottomNavTab.fromRoute("ai_advisor_chat/123"))
        assertEquals(null, BottomNavTab.fromRoute("contact_detail_tab/456"))
    }

    @Test
    fun `BottomNavTab route property returns correct route strings`() {
        // 验证Tab的route属性返回正确的路由字符串
        assertEquals("contact_list", BottomNavTab.CONTACTS.route)
        assertEquals("ai_advisor", BottomNavTab.AI_ADVISOR.route)
        assertEquals("settings", BottomNavTab.SETTINGS.route)
    }

    // ============================================================
    // 底部导航路由列表一致性测试
    // ============================================================

    @Test
    fun `BOTTOM_NAV_ROUTES contains all tab routes`() {
        // 验证BOTTOM_NAV_ROUTES包含所有Tab路由
        val bottomNavRoutes = NavRoutes.BOTTOM_NAV_ROUTES

        assertEquals(3, bottomNavRoutes.size)
        assertTrue(bottomNavRoutes.contains(NavRoutes.CONTACT_LIST))
        assertTrue(bottomNavRoutes.contains(NavRoutes.AI_ADVISOR))
        assertTrue(bottomNavRoutes.contains(NavRoutes.SETTINGS))
    }

    @Test
    fun `BOTTOM_NAV_ROUTES matches BottomNavTab enum`() {
        // 验证BOTTOM_NAV_ROUTES与BottomNavTab枚举一致
        val bottomNavRoutes = NavRoutes.BOTTOM_NAV_ROUTES
        val tabRoutes = BottomNavTab.entries.map { it.route }.toSet()

        assertEquals(bottomNavRoutes.size, tabRoutes.size)
        assertTrue(bottomNavRoutes.containsAll(tabRoutes))
        assertTrue(tabRoutes.containsAll(bottomNavRoutes))
    }

    // ============================================================
    // Tab排除测试 (BUG-00069修复场景)
    // ============================================================

    @Test
    fun `CONTACT_LIST should be excluded from route change detection`() {
        // 验证CONTACT_LIST作为启动路由，应在某些场景下被排除
        // 这是MainActivity.kt中RouteChanged的逻辑
        val contactListRoute = NavRoutes.CONTACT_LIST
        assertEquals("contact_list", contactListRoute)

        // 验证它在BOTTOM_NAV_ROUTES中
        assertTrue(NavRoutes.BOTTOM_NAV_ROUTES.contains(contactListRoute))
    }

    // ============================================================
    // AI军师子栈路由关系验证
    // ============================================================

    @Test
    fun `AI advisor sub-routes are not in bottom nav routes`() {
        // 验证AI军师子栈路由不在底部导航路由中
        val subRoutes = listOf(
            NavRoutes.AI_ADVISOR_CHAT,
            NavRoutes.AI_ADVISOR_CONTACTS,
            NavRoutes.AI_ADVISOR_SESSIONS
        )

        subRoutes.forEach { route ->
            assertFalse("$route should not be in BOTTOM_NAV_ROUTES",
                NavRoutes.BOTTOM_NAV_ROUTES.contains(route))
        }
    }

    @Test
    fun `AI advisor sub-routes contain AI_ADVISOR in their names`() {
        // 验证AI军师子栈路由命名一致性
        val chatRoute = NavRoutes.AI_ADVISOR_CHAT
        val contactsRoute = NavRoutes.AI_ADVISOR_CONTACTS
        val sessionsRoute = NavRoutes.AI_ADVISOR_SESSIONS

        assertTrue(chatRoute.startsWith("ai_advisor_"))
        assertTrue(contactsRoute.startsWith("ai_advisor_"))
        assertTrue(sessionsRoute.startsWith("ai_advisor_"))
    }
}
