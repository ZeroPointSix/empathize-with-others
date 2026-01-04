package com.empathy.ai.presentation.navigation

/**
 * 导航路由定义
 *
 * 定义应用中所有页面的路由路径和参数
 */
object NavRoutes {
    /**
     * 联系人列表页面
     */
    const val CONTACT_LIST = "contact_list"

    /**
     * AI军师页面
     */
    const val AI_ADVISOR = "ai_advisor"

    /**
     * 底部导航栏路由列表
     * 用于底部导航栏的Tab切换
     */
    val BOTTOM_NAV_ROUTES = listOf(
        CONTACT_LIST,
        AI_ADVISOR,
        SETTINGS
    )

    /**
     * 联系人详情页面
     * 参数: contactId (String) - 联系人ID，空字符串表示新建
     */
    const val CONTACT_DETAIL = "contact_detail/{contactId}"
    const val CONTACT_DETAIL_ARG_ID = "contactId"

    /**
     * 聊天分析页面
     * 参数: contactId (String) - 联系人ID
     */
    const val CHAT = "chat/{contactId}"
    const val CHAT_ARG_ID = "contactId"

    /**
     * 标签管理页面
     */
    const val BRAIN_TAG = "brain_tag"

    /**
     * 设置页面
     */
    const val SETTINGS = "settings"

    /**
     * AI服务商配置页面
     */
    const val AI_CONFIG = "ai_config"

    /**
     * 用户画像页面
     */
    const val USER_PROFILE = "user_profile"

    /**
     * 联系人详情标签页（新UI）
     * 参数: contactId (String) - 联系人ID
     */
    const val CONTACT_DETAIL_TAB = "contact_detail_tab/{contactId}"
    const val CONTACT_DETAIL_TAB_ARG_ID = "contactId"

    /**
     * 新建联系人页面（iOS风格）
     * TD-00020 T065: 添加CREATE_CONTACT路由常量
     */
    const val CREATE_CONTACT = "create_contact"

    /**
     * 添加服务商页面（iOS风格）
     * TD-00021 T2-04: 添加ADD_PROVIDER路由常量
     */
    const val ADD_PROVIDER = "add_provider"

    /**
     * 编辑服务商页面（iOS风格）
     * TD-00021 T2-04: 添加EDIT_PROVIDER路由常量
     */
    const val EDIT_PROVIDER = "edit_provider/{providerId}"
    const val EDIT_PROVIDER_ARG_ID = "providerId"

    /**
     * 用量统计页面
     * TD-00025 T6-06: 添加USAGE_STATS路由常量
     */
    const val USAGE_STATS = "usage_stats"

    /**
     * AI军师对话页面
     * TD-00026: 添加AI_ADVISOR_CHAT路由常量
     * 参数: contactId (String) - 联系人ID
     */
    const val AI_ADVISOR_CHAT = "ai_advisor_chat/{contactId}"
    const val AI_ADVISOR_CHAT_ARG_ID = "contactId"

    /**
     * 创建联系人详情路由
     */
    fun createContactDetailRoute(contactId: String): String {
        return "contact_detail/$contactId"
    }

    /**
     * 创建联系人详情标签页路由（新UI）
     */
    fun createContactDetailTabRoute(contactId: String): String {
        return "contact_detail_tab/$contactId"
    }

    /**
     * 创建聊天路由
     */
    fun createChatRoute(contactId: String): String {
        return "chat/$contactId"
    }

    /**
     * 创建编辑服务商路由
     * TD-00021 T2-04: 添加editProvider辅助函数
     */
    fun editProvider(providerId: String): String {
        return "edit_provider/$providerId"
    }

    /**
     * 创建AI军师对话路由
     * TD-00026: 添加aiAdvisorChat辅助函数
     */
    fun aiAdvisorChat(contactId: String): String {
        return "ai_advisor_chat/$contactId"
    }
}
