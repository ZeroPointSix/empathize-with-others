package com.empathy.ai.ui

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import com.empathy.ai.presentation.navigation.BottomNavTab
import com.empathy.ai.presentation.navigation.NonTabNavGraph
import com.empathy.ai.presentation.navigation.NavRoutes
import com.empathy.ai.presentation.ui.component.navigation.BottomNavScaffold
import com.empathy.ai.presentation.ui.component.navigation.EmpathyBottomNavigation
import com.empathy.ai.presentation.ui.screen.advisor.AiAdvisorScreen
import com.empathy.ai.presentation.ui.screen.contact.ContactListScreen
import com.empathy.ai.presentation.ui.screen.settings.SettingsScreen
import com.empathy.ai.ui.theme.AppTheme
import com.empathy.ai.presentation.theme.iOSBackground
import android.util.Log

/**
 * 应用主Activity
 *
 * ## 职责
 * 1. 设置Compose内容
 * 2. 应用主题
 * 3. 初始化导航
 * 4. 依赖注入入口
 * 5. 管理底部Tab状态和AI军师返回逻辑（BUG-00068/BUG-00069）
 *
 * ## 架构说明 (BUG-00068/BUG-00069)
 * - **Tab缓存策略**: 使用 BottomNavScaffold 实现三大Tab页面内存缓存
 * - **双层NavHost**: NonTabNavGraph始终挂载 + BottomNavScaffold条件渲染
 * - **AI军师返回优化**: 检测从AI军师子栈返回Tab时自动恢复上一个非AI Tab
 *
 * ## 关联文档
 * - BUG-00068: AI军师入口与设置回退及非Tab性能覆盖问题
 * - BUG-00069: 切屏回退与联系人错误闪现问题
 * - PRD-00034: 界面切换性能优化-页面缓存方案
 * - TDD-00034: 界面切换性能优化-页面缓存方案技术设计
 *
 * ## 注意事项
 * - 此Activity必须放在app模块中，因为Hilt的@AndroidEntryPoint需要AGP字节码转换
 * - 使用AppTheme而非EmpathyTheme，避免多模块运行时ThemeKt类找不到（BUG-00050）
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            // 应用主题 - 使用app模块本地的AppTheme
            AppTheme {
                // Surface容器,提供背景色
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

/**
 * 主屏幕容器
 *
 * ## BUG-00069修复: AI军师返回自动恢复上一个非AI Tab
 *
 * ### 问题场景
 * - 用户在联系人Tab → AI军师Tab → 进入对话 → 返回
 * - 期望: 一次返回回到联系人Tab
 * - 实际: 需要返回两次（对话 → AI军师入口 → 联系人）
 *
 * ### 解决方案
 * 1. **lastNonAiTab**: 记录上一个非AI军师Tab，用于返回恢复
 * 2. **wasInMainTab**: 检测从AI军师子栈回到Tab区域的状态变化
 * 3. **自动恢复逻辑**: 检测到 `!wasInMainTab && isInMainTab && currentTab == AI_ADVISOR` 时恢复
 *
 * ### 设计权衡 (BUG-00069)
 * - 选择状态变量追踪而非导航栈清理，避免破坏现有导航结构
 * - 原因: 导航栈清理可能影响其他返回路径，风险较高
 */
@Composable
private fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isInMainTab = currentRoute == null || currentRoute in NavRoutes.BOTTOM_NAV_ROUTES
    val initialTab = BottomNavTab.fromRoute(currentRoute) ?: BottomNavTab.CONTACTS
    var currentTab by rememberSaveable { mutableStateOf(initialTab) }
    var lastNonAiTab by rememberSaveable { mutableStateOf(BottomNavTab.CONTACTS) }
    var wasInMainTab by rememberSaveable { mutableStateOf(isInMainTab) }

    // [Route监听] 排除CONTACT_LIST避免首次加载时Tab切换导致子页面重建
    // BUG-00069: CONTACT_LIST是启动路由，首次加载时不应该更新currentTab
    LaunchedEffect(currentRoute) {
        if (currentRoute != null && currentRoute != NavRoutes.CONTACT_LIST) {
            BottomNavTab.fromRoute(currentRoute)?.let {
                Log.d("MainActivity", "RouteChanged -> update currentTab=${it.route}")
                currentTab = it
            }
        } else {
            Log.d("MainActivity", "RouteChanged ignored route=$currentRoute")
        }
    }

    // [非AI Tab记录] 持续追踪上一个非AI Tab，用于AI军师返回恢复
    LaunchedEffect(currentTab) {
        if (currentTab != BottomNavTab.AI_ADVISOR) {
            lastNonAiTab = currentTab
        }
    }

    // [AI军师返回恢复] BUG-00069: 检测从AI军师子栈回到Tab区域时自动恢复上一个非AI Tab
    // 触发条件: 之前不在Tab区域 + 现在在Tab区域 + 当前Tab是AI军师
    // 效果: 将currentTab切换回lastNonAiTab，避免需要二次返回
    LaunchedEffect(isInMainTab, currentTab, lastNonAiTab) {
        if (!wasInMainTab && isInMainTab && currentTab == BottomNavTab.AI_ADVISOR) {
            Log.d(
                "MainActivity",
                "AiAdvisorFlowClosed -> restore lastNonAiTab=${lastNonAiTab.route}"
            )
            currentTab = lastNonAiTab
        }
        wasInMainTab = isInMainTab
    }

    // [调试日志] BUG-00063: 添加导航状态日志用于排查白屏问题
    // 记录路由、Tab位置和主Tab状态，便于分析切换流程
    LaunchedEffect(currentRoute, currentTab, isInMainTab) {
        Log.d(
            "MainActivity",
            "NavState route=$currentRoute isInMainTab=$isInMainTab currentTab=${currentTab.route}"
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NonTabNavGraph(
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                .background(iOSBackground)
                .alpha(if (isInMainTab) 0f else 1f)
                .pointerInput(isInMainTab) {
                    if (isInMainTab) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                },
            // BUG-00069: AI军师对话关闭时恢复上一个非AI Tab
            // 与NavGraph.kt中的onNavigateBack回调配合，实现自动Tab恢复
            onAiAdvisorChatClosed = {
                currentTab = lastNonAiTab
            }
        )

        if (isInMainTab) {
            BottomNavScaffold(
                currentTab = currentTab,
                contactsContent = {
                    ContactListScreen(
                        onNavigateToDetail = { contactId ->
                            if (contactId.isNotEmpty()) {
                                navController.navigate(NavRoutes.createContactDetailTabRoute(contactId))
                            } else {
                                navController.navigate(NavRoutes.CREATE_CONTACT)
                            }
                        },
                        onNavigateToSettings = { currentTab = BottomNavTab.SETTINGS },
                        onNavigate = { route ->
                            handleBottomNavRoute(route) { tab -> currentTab = tab }
                                ?: navController.navigate(route)
                        },
                        onAddClick = { navController.navigate(NavRoutes.CREATE_CONTACT) },
                        currentRoute = currentTab.route,
                        showBottomBar = false
                    )
                },
                aiAdvisorContent = {
                    AiAdvisorScreen(
                        onNavigateToChat = { contactId ->
                            // BUG-00068修复: 添加launchSingleTop防止Tab切换时重复入栈
                            // 配合NavGraph.kt中的popUpTo(CONTACT_LIST)实现稳定的返回行为
                            navController.navigate(NavRoutes.aiAdvisorChat(contactId)) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToContactSelect = {
                            // BUG-00068修复: 同样添加launchSingleTop防重复入栈
                            navController.navigate(NavRoutes.AI_ADVISOR_CONTACTS) {
                                launchSingleTop = true
                            }
                        },
                        isVisible = currentTab == BottomNavTab.AI_ADVISOR
                    )
                },
                settingsContent = {
                    SettingsScreen(
                        onNavigateBack = { currentTab = BottomNavTab.CONTACTS },
                        onNavigateToAiConfig = {
                            navController.navigate(NavRoutes.aiConfig(NavRoutes.SOURCE_SETTINGS))
                        },
                        onNavigateToPromptEditor = { route -> navController.navigate(route) },
                        onNavigateToUserProfile = { navController.navigate(NavRoutes.USER_PROFILE) },
                        onNavigateToSystemPromptList = { navController.navigate(NavRoutes.SYSTEM_PROMPT_LIST) },
                        onNavigate = { route ->
                            handleBottomNavRoute(route) { tab -> currentTab = tab }
                                ?: navController.navigate(route)
                        },
                        onAddClick = { navController.navigate(NavRoutes.CREATE_CONTACT) },
                        currentRoute = currentTab.route,
                        showBottomBar = false,
                        isVisible = currentTab == BottomNavTab.SETTINGS
                    )
                },
                bottomBar = {
                    EmpathyBottomNavigation(
                        currentRoute = currentTab.route,
                        onNavigate = { route ->
                            BottomNavTab.fromRoute(route)?.let { currentTab = it }
                        },
                        onAddClick = { navController.navigate(NavRoutes.CREATE_CONTACT) }
                    )
                }
            )
        }
    }
}

private fun handleBottomNavRoute(
    route: String,
    onTabChange: (BottomNavTab) -> Unit
): Unit? {
    return when (route) {
        NavRoutes.CONTACT_LIST -> {
            onTabChange(BottomNavTab.CONTACTS)
            Unit
        }
        NavRoutes.AI_ADVISOR -> {
            onTabChange(BottomNavTab.AI_ADVISOR)
            Unit
        }
        NavRoutes.SETTINGS -> {
            onTabChange(BottomNavTab.SETTINGS)
            Unit
        }
        else -> null
    }
}
