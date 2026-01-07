package com.empathy.ai.presentation.navigation

/**
 * 导航路由定义
 *
 * ## 业务职责
 * 定义应用中所有页面的路由路径字符串常量，作为导航的唯一数据源。
 * 采用object单例模式，确保路由常量的唯一性和可访问性。
 *
 * ## 命名规范
 * - **页面路由**: 使用小写字母和下划线，如 `contact_list`
 * - **参数路由**: 使用 `{paramName}` 占位符，如 `contact_detail/{contactId}`
 * - **参数常量**: 使用 `ARG_ID` 后缀，如 `CONTACT_DETAIL_ARG_ID`
 * - **辅助函数**: 使用 `create`/`edit`/`xxx` 动词前缀，如 `createContactDetailRoute`
 *
 * ## 路由层级
 * ```
 * presentation/navigation/
 * ├── NavRoutes.kt      # 路由常量定义（当前文件）
 * ├── NavGraph.kt       # 导航图实现
 * └── prompt/           # 提示词编辑器导航（嵌套）
 * ```
 *
 * ## 路由清单
 * | 路由 | 功能 | 参数 | 关联功能 |
 * |------|------|------|----------|
 * | CONTACT_LIST | 联系人列表 | 无 | 底部Tab首页 |
 * | CONTACT_DETAIL | 联系人详情 | contactId | 旧版详情页（已废弃） |
 * | CONTACT_DETAIL_TAB | 联系人详情(新) | contactId | 四标签页设计 |
 * | CREATE_CONTACT | 新建联系人 | 无 | TD-00020 |
 * | CHAT | AI分析 | contactId | 聊天分析界面 |
 * | BRAIN_TAG | 标签管理 | 无 | 标签管理界面 |
 * | AI_ADVISOR | AI军师主界面 | 无 | TD-00026 |
 * | AI_ADVISOR_CHAT | AI军师对话 | contactId | TD-00026 |
 * | SETTINGS | 设置 | 无 | 底部Tab页 |
 * | AI_CONFIG | AI配置 | 无 | 服务商管理 |
 * | ADD_PROVIDER | 添加服务商 | 无 | TD-00021 |
 * | EDIT_PROVIDER | 编辑服务商 | providerId | TD-00021 |
 * | USAGE_STATS | 用量统计 | 无 | TD-00025 |
 * | USER_PROFILE | 用户画像 | 无 | 个人信息 |
 *
 * ## 底部导航路由
 * BOTTOM_NAV_ROUTES 定义了底部Tab栏对应的三个页面：
 * 1. CONTACT_LIST - 联系人
 * 2. AI_ADVISOR - AI军师
 * 3. SETTINGS - 设置
 *
 * ## 设计决策
 * 1. **路由字符串硬编码**: 在object中定义常量，便于IDE自动完成和重构
 * 2. **参数常量分离**: 参数名单独定义，避免字符串重复
 * 3. **辅助函数封装**: 复杂的路由拼接逻辑封装为函数，降低调用方复杂度
 * 4. **命名一致性**: 路由名与Screen Composable命名保持一致
 *
 * @see NavGraph 导航图实现
 * @see AnimationSpec 转场动画定义
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
     * AI军师会话历史页面
     * PRD-00029: 新增会话历史路由
     * 参数: contactId (String) - 联系人ID
     */
    const val AI_ADVISOR_SESSIONS = "ai_advisor_sessions/{contactId}"
    const val AI_ADVISOR_SESSIONS_ARG_ID = "contactId"

    /**
     * AI军师联系人选择页面
     * PRD-00029: 新增联系人选择路由
     */
    const val AI_ADVISOR_CONTACTS = "ai_advisor_contacts"

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

    /**
     * 创建AI军师会话历史路由
     * PRD-00029: 新增辅助函数
     */
    fun aiAdvisorSessions(contactId: String): String {
        return "ai_advisor_sessions/$contactId"
    }
}
