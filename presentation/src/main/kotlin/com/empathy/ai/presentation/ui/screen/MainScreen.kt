package com.empathy.ai.presentation.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.empathy.ai.presentation.navigation.NavRoutes
import com.empathy.ai.presentation.ui.component.navigation.EmpathyBottomNavigation

/**
 * 主屏幕容器
 *
 * 包含底部导航栏和内容区域
 * 使用 currentBackStackEntryAsState() 自动同步导航状态
 *
 * @param navController 导航控制器
 * @param onAddClick 添加按钮点击回调
 * @param content 内容区域
 */
@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController(),
    onAddClick: () -> Unit = {},
    content: @Composable (Modifier) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavRoutes.CONTACT_LIST

    // 判断是否显示底部导航栏
    val showBottomNav = currentRoute in NavRoutes.BOTTOM_NAV_ROUTES

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                EmpathyBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                // 避免重复添加到回退栈
                                popUpTo(NavRoutes.CONTACT_LIST) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onAddClick = onAddClick
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            content(Modifier.fillMaxSize())
        }
    }
}

/**
 * 判断当前路由是否为底部导航栏路由
 */
fun isBottomNavRoute(route: String?): Boolean {
    return route in NavRoutes.BOTTOM_NAV_ROUTES
}
