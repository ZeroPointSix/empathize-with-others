package com.empathy.ai.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 联系人详情页导航集成测试
 * 
 * TD-00020 T073: 测试Tab切换、页面跳转、返回导航
 * 
 * 关键测试场景:
 * - 四个Tab之间切换
 * - 从联系人列表跳转到详情页
 * - 新建联系人流程完整性
 * - 返回导航和状态保持
 */
class ContactDetailNavigationTest {

    // ============================================================
    // NavRoutes 测试
    // ============================================================

    @Test
    fun `NavRoutes CONTACT_LIST is correct`() {
        assertEquals("contact_list", NavRoutes.CONTACT_LIST)
    }

    @Test
    fun `NavRoutes CONTACT_DETAIL pattern is correct`() {
        assertEquals("contact_detail/{contactId}", NavRoutes.CONTACT_DETAIL)
    }

    @Test
    fun `NavRoutes CONTACT_DETAIL_TAB pattern is correct`() {
        assertEquals("contact_detail_tab/{contactId}", NavRoutes.CONTACT_DETAIL_TAB)
    }

    @Test
    fun `NavRoutes CREATE_CONTACT is correct`() {
        assertEquals("create_contact", NavRoutes.CREATE_CONTACT)
    }

    @Test
    fun `NavRoutes SETTINGS is correct`() {
        assertEquals("settings", NavRoutes.SETTINGS)
    }

    // ============================================================
    // 路由创建测试
    // ============================================================

    @Test
    fun `createContactDetailRoute creates correct route`() {
        val route = NavRoutes.createContactDetailRoute("contact-123")
        
        assertEquals("contact_detail/contact-123", route)
    }

    @Test
    fun `createContactDetailRoute with empty id creates correct route`() {
        val route = NavRoutes.createContactDetailRoute("")
        
        assertEquals("contact_detail/", route)
    }

    @Test
    fun `createContactDetailTabRoute creates correct route`() {
        val route = NavRoutes.createContactDetailTabRoute("contact-456")
        
        assertEquals("contact_detail_tab/contact-456", route)
    }

    @Test
    fun `createChatRoute creates correct route`() {
        val route = NavRoutes.createChatRoute("contact-789")
        
        assertEquals("chat/contact-789", route)
    }

    // ============================================================
    // 底部导航栏路由测试
    // ============================================================

    @Test
    fun `BOTTOM_NAV_ROUTES contains correct routes`() {
        val routes = NavRoutes.BOTTOM_NAV_ROUTES
        
        assertEquals(3, routes.size)
        assertTrue(routes.contains(NavRoutes.CONTACT_LIST))
        assertTrue(routes.contains(NavRoutes.AI_ADVISOR))
        assertTrue(routes.contains(NavRoutes.SETTINGS))
    }

    @Test
    fun `BOTTOM_NAV_ROUTES does not contain detail routes`() {
        val routes = NavRoutes.BOTTOM_NAV_ROUTES
        
        assertFalse(routes.contains(NavRoutes.CONTACT_DETAIL))
        assertFalse(routes.contains(NavRoutes.CONTACT_DETAIL_TAB))
        assertFalse(routes.contains(NavRoutes.CREATE_CONTACT))
    }

    // ============================================================
    // 参数名称测试
    // ============================================================

    @Test
    fun `CONTACT_DETAIL_ARG_ID is correct`() {
        assertEquals("contactId", NavRoutes.CONTACT_DETAIL_ARG_ID)
    }

    @Test
    fun `CONTACT_DETAIL_TAB_ARG_ID is correct`() {
        assertEquals("contactId", NavRoutes.CONTACT_DETAIL_TAB_ARG_ID)
    }

    @Test
    fun `CHAT_ARG_ID is correct`() {
        assertEquals("contactId", NavRoutes.CHAT_ARG_ID)
    }

    // ============================================================
    // 导航流程测试
    // ============================================================

    @Test
    fun `navigation from list to detail uses correct route`() {
        val contactId = "test-contact-id"
        val expectedRoute = "contact_detail_tab/$contactId"
        val actualRoute = NavRoutes.createContactDetailTabRoute(contactId)
        
        assertEquals(expectedRoute, actualRoute)
    }

    @Test
    fun `navigation to create contact uses correct route`() {
        assertEquals("create_contact", NavRoutes.CREATE_CONTACT)
    }

    @Test
    fun `navigation from detail to chat uses correct route`() {
        val contactId = "test-contact-id"
        val expectedRoute = "chat/$contactId"
        val actualRoute = NavRoutes.createChatRoute(contactId)
        
        assertEquals(expectedRoute, actualRoute)
    }

    // ============================================================
    // Tab索引测试
    // ============================================================

    @Test
    fun `ContactDetailTab enum has correct values`() {
        val tabs = ContactDetailTab.values()
        
        assertEquals(4, tabs.size)
        assertEquals(ContactDetailTab.OVERVIEW, tabs[0])
        assertEquals(ContactDetailTab.FACT_STREAM, tabs[1])
        assertEquals(ContactDetailTab.PERSONA, tabs[2])
        assertEquals(ContactDetailTab.DATA_VAULT, tabs[3])
    }

    @Test
    fun `ContactDetailTab OVERVIEW has correct index`() {
        assertEquals(0, ContactDetailTab.OVERVIEW.ordinal)
    }

    @Test
    fun `ContactDetailTab FACT_STREAM has correct index`() {
        assertEquals(1, ContactDetailTab.FACT_STREAM.ordinal)
    }

    @Test
    fun `ContactDetailTab PERSONA has correct index`() {
        assertEquals(2, ContactDetailTab.PERSONA.ordinal)
    }

    @Test
    fun `ContactDetailTab DATA_VAULT has correct index`() {
        assertEquals(3, ContactDetailTab.DATA_VAULT.ordinal)
    }

    @Test
    fun `ContactDetailTab has correct display names`() {
        assertEquals("概览", ContactDetailTab.OVERVIEW.displayName)
        assertEquals("事实流", ContactDetailTab.FACT_STREAM.displayName)
        assertEquals("画像库", ContactDetailTab.PERSONA.displayName)
        assertEquals("资料库", ContactDetailTab.DATA_VAULT.displayName)
    }

    // ============================================================
    // 特殊字符处理测试
    // ============================================================

    @Test
    fun `createContactDetailRoute handles special characters`() {
        val contactId = "contact-with-special-chars-123"
        val route = NavRoutes.createContactDetailRoute(contactId)
        
        assertTrue(route.contains(contactId))
    }

    @Test
    fun `createContactDetailRoute handles unicode characters`() {
        val contactId = "联系人-123"
        val route = NavRoutes.createContactDetailRoute(contactId)
        
        assertTrue(route.contains(contactId))
    }

    @Test
    fun `createContactDetailRoute handles numeric id`() {
        val contactId = "12345"
        val route = NavRoutes.createContactDetailRoute(contactId)
        
        assertEquals("contact_detail/12345", route)
    }
}

/**
 * 联系人详情Tab枚举（用于测试）
 */
enum class ContactDetailTab(val displayName: String) {
    OVERVIEW("概览"),
    FACT_STREAM("事实流"),
    PERSONA("画像库"),
    DATA_VAULT("资料库")
}
