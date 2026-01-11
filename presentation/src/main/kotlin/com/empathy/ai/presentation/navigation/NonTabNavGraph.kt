package com.empathy.ai.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController

/**
 * 非Tab导航图
 *
 * ## 业务背景
 * - PRD: PRD-00034 [界面切换性能优化-页面缓存方案]
 * - 问题：NavHost 未挂载时，navigate() 调用会抛出异常
 *
 * ## 设计决策
 * - TDD: TDD-00034 [架构拆分 - NonTabNavGraph]
 * - 核心策略：将非 Tab 页面独立为 NavGraph，避免与 BottomNavScaffold 重复渲染
 * - 原因：NavGraph 默认包含 Tab 页面路由，若在 BottomNavScaffold 中渲染会导致 UI 重复
 *
 * ## BUG-00069修复: onAiAdvisorChatClosed回调
 * 问题: AI军师对话返回需要二次返回（对话 → 入口 → 上一Tab）
 * 方案: 通过onAiAdvisorChatClosed回调通知MainActivity恢复上一个非AI Tab
 *
 * ## 任务追踪
 * - FD: FD-00034-界面切换性能优化-页面缓存方案
 * - Task: T34-08 [US2] 重构 MainActivity 接入 BottomNavScaffold
 *
 * ## 架构说明
 * ```
 * MainActivity
 *     │
 *     ├── NonTabNavGraph (始终挂载，保证 navigate() 可用)
 *     │       └── ContactDetailTabScreen, AiAdvisorChatScreen, AiConfigScreen...
 *     │
 *     └── BottomNavScaffold (Tab页面缓存)
 *             └── ContactListScreen, AiAdvisorScreen, SettingsScreen
 * ```
 */
@Composable
fun NonTabNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onAiAdvisorChatClosed: () -> Unit = {}
) {
    NavGraph(
        navController = navController,
        modifier = modifier,
        includeTabScreens = false,  // Tab页面由 BottomNavScaffold 渲染，避免重复
        useTransition = false,      // 底部Tab页面切换时禁用转场动画，避免叠加
        onAiAdvisorChatClosed = onAiAdvisorChatClosed  // BUG-00069: AI军师返回回调
    )
}
