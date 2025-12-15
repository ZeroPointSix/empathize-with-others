package com.empathy.ai.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.empathy.ai.presentation.ui.screen.aiconfig.AiConfigScreen
import com.empathy.ai.presentation.ui.screen.chat.ChatScreen
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailScreen
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailTabScreen
import com.empathy.ai.presentation.ui.screen.contact.ContactListScreen
import com.empathy.ai.presentation.ui.screen.settings.SettingsScreen
import com.empathy.ai.presentation.ui.screen.tag.BrainTagScreen

/**
 * 应用导航图
 *
 * 定义应用的导航结构和页面跳转逻辑
 *
 * @param navController 导航控制器
 * @param modifier Modifier
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.CONTACT_LIST,
        modifier = modifier
    ) {
        // 联系人列表页面
        composable(route = NavRoutes.CONTACT_LIST) {
            ContactListScreen(
                onNavigateToDetail = { contactId ->
                    // 使用新的联系人详情标签页UI
                    if (contactId.isNotEmpty()) {
                        navController.navigate(NavRoutes.createContactDetailTabRoute(contactId))
                    } else {
                        // 新建联系人仍使用旧页面
                        navController.navigate(NavRoutes.createContactDetailRoute(contactId))
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.SETTINGS)
                }
            )
        }

        // 联系人详情页面
        composable(
            route = NavRoutes.CONTACT_DETAIL,
            arguments = listOf(
                navArgument(NavRoutes.CONTACT_DETAIL_ARG_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString(NavRoutes.CONTACT_DETAIL_ARG_ID) ?: ""
            ContactDetailScreen(
                contactId = contactId,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 聊天分析页面
        composable(
            route = NavRoutes.CHAT,
            arguments = listOf(
                navArgument(NavRoutes.CHAT_ARG_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString(NavRoutes.CHAT_ARG_ID) ?: ""
            ChatScreen(
                contactId = contactId,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 标签管理页面
        composable(route = NavRoutes.BRAIN_TAG) {
            BrainTagScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 设置页面
        composable(route = NavRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToAiConfig = {
                    navController.navigate(NavRoutes.AI_CONFIG)
                }
            )
        }

        // AI服务商配置页面
        composable(route = NavRoutes.AI_CONFIG) {
            AiConfigScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 联系人详情标签页（新UI）
        composable(
            route = NavRoutes.CONTACT_DETAIL_TAB,
            arguments = listOf(
                navArgument(NavRoutes.CONTACT_DETAIL_TAB_ARG_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString(NavRoutes.CONTACT_DETAIL_TAB_ARG_ID) ?: ""
            ContactDetailTabScreen(
                contactId = contactId,
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}
