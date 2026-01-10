package com.empathy.ai.presentation.navigation

/**
 * 底部导航Tab枚举
 *
 * ## 业务背景
 * - PRD: PRD-00034 [界面切换性能优化-页面缓存方案]
 * - 用户故事: US-2 [底部导航页面缓存]
 *
 * ## 设计决策
 * - TDD: TDD-00034 [界面切换性能优化-页面缓存方案技术设计]
 * - 选择枚举而非配置文件：类型安全 + IDE自动完成 + 编译期检查
 * - 保持与 NavRoutes.BOTTOM_NAV_ROUTES 严格一致，避免运行时不一致
 *
 * ## 任务追踪
 * - FD: FD-00034-界面切换性能优化-页面缓存方案
 * - Task: T34-04 [US2] 创建 BottomNavTab 枚举
 */
enum class BottomNavTab(val route: String) {
    CONTACTS(NavRoutes.CONTACT_LIST),
    AI_ADVISOR(NavRoutes.AI_ADVISOR),
    SETTINGS(NavRoutes.SETTINGS);

    companion object {
        /**
         * 从路由字符串反解析为Tab枚举
         *
         * ## 业务规则 (PRD-00034)
         * - 底部导航切换时，通过 route 匹配确定目标 Tab
         * - 匹配失败返回 null，由调用方决定兜底策略
         *
         * ## 设计权衡
         * - 使用 entries.find 而非 values().toList()：更高效（无对象创建）
         * - 可空返回：明确表示"未知路由"语义
         *
         * ## 任务: T34-04
         * @param route 导航路由字符串
         * @return 对应的 BottomNavTab，若无匹配则返回 null
         */
        fun fromRoute(route: String?): BottomNavTab? {
            return entries.find { it.route == route }
        }
    }
}
