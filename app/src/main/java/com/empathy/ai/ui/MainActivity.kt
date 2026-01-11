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
 * 职责:
 * 1. 设置Compose内容
 * 2. 应用主题
 * 3. 初始化导航
 * 4. 依赖注入入口
 * 
 * 注意: 此Activity必须放在app模块（Application模块）中，
 * 因为Hilt的@AndroidEntryPoint注解需要AGP的字节码转换支持，
 * 而字节码转换只在Application模块中生效。
 * 
 * 主题说明: 使用app模块本地的AppTheme而非presentation模块的EmpathyTheme，
 * 以解决多模块架构下ThemeKt类在运行时无法被找到的问题。
 * 
 * 开发者模式: DeveloperModeViewModel使用Activity作为ViewModelStoreOwner，
 * 在SettingsScreen中通过hiltViewModel()获取同一个实例
 * @see BUG-00050 开发者模式导航时意外退出
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

@Composable
private fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isInMainTab = currentRoute == null || currentRoute in NavRoutes.BOTTOM_NAV_ROUTES
    val initialTab = BottomNavTab.fromRoute(currentRoute) ?: BottomNavTab.CONTACTS
    var currentTab by rememberSaveable { mutableStateOf(initialTab) }

    LaunchedEffect(currentRoute) {
        BottomNavTab.fromRoute(currentRoute)?.let { currentTab = it }
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
