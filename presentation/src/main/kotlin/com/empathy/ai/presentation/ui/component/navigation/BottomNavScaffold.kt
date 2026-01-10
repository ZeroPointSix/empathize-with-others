package com.empathy.ai.presentation.ui.component.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import com.empathy.ai.presentation.navigation.BottomNavTab

/**
 * 带页面缓存的底部导航Scaffold
 *
 * ## 业务背景
 * - PRD: PRD-00034 [界面切换性能优化-页面缓存方案]
 * - 问题：底部Tab切换时页面被销毁重建，导致黑屏卡顿
 * - 目标：页面保持在内存中，只切换可见性
 *
 * ## 设计决策
 * - TDD: TDD-00034 [Phase 2：底部Tab缓存架构]
 * - 核心策略：
 *   1. 使用 SaveableStateHolder 保持页面状态（滚动位置、筛选状态等）
 *   2. 用 alpha/zIndex 控制可见性，避免黑屏暴露
 *   3. 屏蔽不可见页面的触摸事件，防止穿透
 *   4. 懒加载：未访问的页面不创建，减少内存占用
 *
 * ## 任务追踪
 * - FD: FD-00034-界面切换性能优化-页面缓存方案
 * - Task: T34-05 [US2] 创建 BottomNavScaffold 组件
 * - Task: T34-06 [US2] 实现 visitedTabs 懒加载与 SaveableStateHolder
 * - Task: T34-07 [US2] 处理不可见 Tab 触摸拦截与可见性切换
 *
 * ## 性能指标 (PRD-00034)
 * - 切换延迟: < 100ms
 * - 消除黑屏闪动
 * - 保持页面状态
 *
 * ## 架构位置
 * ```
 * MainActivity
 *     └── BottomNavScaffold (3个Tab页面常驻内存)
 *         ├── ContactListScreen (缓存)
 *         ├── AiAdvisorScreen (缓存)
 *         └── SettingsScreen (缓存)
 * ```
 */
@Composable
fun BottomNavScaffold(
    currentTab: BottomNavTab,
    contactsContent: @Composable () -> Unit,
    aiAdvisorContent: @Composable () -> Unit,
    settingsContent: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    // [State] SaveableStateHolder 用于保存/恢复页面状态
    // 权衡：牺牲少量内存，换取页面状态持久化
    val stateHolder = rememberSaveableStateHolder()

    // [State] visitedTabs 记录已访问的Tab，用于懒加载优化
    // 初始只包含当前Tab，避免启动时一次性创建所有页面
    var visitedTabs by rememberSaveable { mutableStateOf(listOf(currentTab.route)) }

    // [Effect] 首次访问新Tab时加入已访问列表
    // 业务规则 (PRD-00034/4.2)：懒加载 - 未访问页面不创建
    LaunchedEffect(currentTab) {
        if (currentTab.route !in visitedTabs) {
            visitedTabs = visitedTabs + currentTab.route
        }
    }

    Scaffold(
        bottomBar = bottomBar
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // [Composition] Tab内容Host - 使用SaveableStateProvider保持状态
            TabContentHost(
                route = BottomNavTab.CONTACTS.route,
                currentTab = currentTab,
                visitedTabs = visitedTabs,
                stateHolder = stateHolder
            ) { contactsContent() }

            TabContentHost(
                route = BottomNavTab.AI_ADVISOR.route,
                currentTab = currentTab,
                visitedTabs = visitedTabs,
                stateHolder = stateHolder
            ) { aiAdvisorContent() }

            TabContentHost(
                route = BottomNavTab.SETTINGS.route,
                currentTab = currentTab,
                visitedTabs = visitedTabs,
                stateHolder = stateHolder
            ) { settingsContent() }
        }
    }
}

/**
 * Tab内容宿主组件
 *
 * ## 核心职责
 * 1. 控制Tab内容的可见性（alpha）
 * 2. 屏蔽不可见Tab的触摸事件（pointerInput）
 * 3. 使用SaveableStateProvider保持页面状态
 *
 * ## 设计决策
 * - zIndex: 确保可见页面在顶层，不会被隐藏页面遮挡
 * - pointerInput 拦截: 隐藏页面不接受触摸事件，防止穿透
 *
 * ## 任务: T34-07
 *
 * @param route Tab路由标识
 * @param currentTab 当前选中的Tab
 * @param visitedTabs 已访问的Tab列表（懒加载控制）
 * @param stateHolder 状态保持器
 * @param content Tab内容Composable
 */
@Composable
private fun TabContentHost(
    route: String,
    currentTab: BottomNavTab,
    visitedTabs: List<String>,
    stateHolder: SaveableStateHolder,
    content: @Composable () -> Unit
) {
    // [Condition] 懒加载：只在已访问列表中时渲染
    if (route in visitedTabs) {
        // [State] visible 由 currentTab 决定
        // 业务规则：切换时无淡入淡出，避免黑屏暴露
        val visible = currentTab.route == route

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (visible) 1f else 0f)           // 可见页面提升层级
                .alpha(if (visible) 1f else 0f)            // 隐藏页面透明
                .pointerInput(visible) {
                    // [Interaction] 不可见时拦截所有触摸事件
                    // 原因：隐藏页面不应响应用户交互
                    if (!visible) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
        ) {
            // [State] SaveableStateProvider 包裹内容，保持状态
            stateHolder.SaveableStateProvider(route) {
                content()
            }
        }
    }
}
