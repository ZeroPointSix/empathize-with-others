package com.empathy.ai.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.empathy.ai.presentation.ui.screen.aiconfig.AiConfigScreen
import com.empathy.ai.presentation.ui.screen.advisor.AiAdvisorChatScreen
import com.empathy.ai.presentation.ui.screen.advisor.AiAdvisorScreen
import com.empathy.ai.presentation.ui.screen.chat.ChatScreen
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailScreen
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailTabScreen
import com.empathy.ai.presentation.ui.screen.contact.ContactListScreen
import com.empathy.ai.presentation.ui.screen.contact.CreateContactScreen
import com.empathy.ai.presentation.ui.screen.settings.SettingsScreen
import com.empathy.ai.presentation.ui.screen.tag.BrainTagScreen
import com.empathy.ai.presentation.ui.screen.userprofile.UserProfileScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.empathy.ai.presentation.viewmodel.CreateContactViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.empathy.ai.presentation.theme.AnimationSpec

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
        modifier = modifier,
        enterTransition = { AnimationSpec.PageEnterTransition },
        exitTransition = { AnimationSpec.PageExitTransition },
        popEnterTransition = { AnimationSpec.PagePopEnterTransition },
        popExitTransition = { AnimationSpec.PagePopExitTransition }
    ) {
        // 联系人列表页面
        composable(route = NavRoutes.CONTACT_LIST) {
            ContactListScreen(
                onNavigateToDetail = { contactId ->
                    // 使用新的联系人详情标签页UI
                    if (contactId.isNotEmpty()) {
                        navController.navigate(NavRoutes.createContactDetailTabRoute(contactId))
                    } else {
                        // 新建联系人使用iOS风格新页面
                        navController.navigate(NavRoutes.CREATE_CONTACT)
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.SETTINGS)
                },
                // 修复BUG-00031: 添加底部导航栏的导航回调
                onNavigate = { route ->
                    if (route != NavRoutes.CONTACT_LIST) {
                        navController.navigate(route) {
                            popUpTo(NavRoutes.CONTACT_LIST) {
                                saveState = true
                            }
                            launchSingleTop = true
                            // 移除restoreState，避免状态恢复问题
                        }
                    }
                },
                // 修复: 添加加号按钮点击回调 - 使用iOS风格新页面
                onAddClick = {
                    navController.navigate(NavRoutes.CREATE_CONTACT)
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
                },
                onNavigateToPromptEditor = { route ->
                    navController.navigate(route)
                },
                onNavigateToUserProfile = {
                    navController.navigate(NavRoutes.USER_PROFILE)
                },
                // 修复BUG-001: 添加底部导航栏的导航回调
                onNavigate = { route ->
                    if (route != NavRoutes.SETTINGS) {
                        navController.navigate(route) {
                            popUpTo(NavRoutes.CONTACT_LIST) {
                                saveState = true
                            }
                            launchSingleTop = true
                            // 移除restoreState，避免状态恢复问题
                        }
                    }
                },
                // 修复: 添加加号按钮点击回调 - 使用iOS风格新页面
                onAddClick = {
                    navController.navigate(NavRoutes.CREATE_CONTACT)
                }
            )
        }

        // AI军师页面
        composable(route = NavRoutes.AI_ADVISOR) {
            AiAdvisorScreen(
                onNavigateToChat = { contactId ->
                    navController.navigate(NavRoutes.aiAdvisorChat(contactId))
                }
            )
        }

        // AI军师对话页面
        composable(
            route = NavRoutes.AI_ADVISOR_CHAT,
            arguments = listOf(
                navArgument(NavRoutes.AI_ADVISOR_CHAT_ARG_ID) {
                    type = NavType.StringType
                }
            )
        ) {
            AiAdvisorChatScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToContact = { contactId ->
                    navController.navigate(NavRoutes.aiAdvisorChat(contactId)) {
                        popUpTo(NavRoutes.AI_ADVISOR)
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.AI_CONFIG)
                }
            )
        }

        // 用户画像页面
        composable(route = NavRoutes.USER_PROFILE) {
            UserProfileScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // AI服务商配置页面
        composable(route = NavRoutes.AI_CONFIG) {
            AiConfigScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToAddProvider = {
                    navController.navigate(NavRoutes.ADD_PROVIDER)
                },
                onNavigateToEditProvider = { providerId ->
                    navController.navigate(NavRoutes.editProvider(providerId))
                },
                // TD-00025: 添加用量统计导航
                onNavigateToUsageStats = {
                    navController.navigate(NavRoutes.USAGE_STATS)
                }
            )
        }

        // 添加服务商页面（iOS风格）
        // TD-00021 T2-04: 添加AddProviderScreen路由配置
        composable(route = NavRoutes.ADD_PROVIDER) {
            com.empathy.ai.presentation.ui.screen.aiconfig.AddProviderScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 编辑服务商页面（iOS风格）
        // TD-00021: 添加EditProviderScreen路由配置
        // BUG-00040修复：复用AddProviderScreen，确保UI一致性
        composable(
            route = NavRoutes.EDIT_PROVIDER,
            arguments = listOf(
                navArgument(NavRoutes.EDIT_PROVIDER_ARG_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val providerId = backStackEntry.arguments?.getString(NavRoutes.EDIT_PROVIDER_ARG_ID) ?: ""
            com.empathy.ai.presentation.ui.screen.aiconfig.AddProviderScreen(
                providerId = providerId,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 用量统计页面
        // TD-00025 T6-06: 添加UsageStatsScreen路由配置
        composable(route = NavRoutes.USAGE_STATS) {
            com.empathy.ai.presentation.ui.screen.aiconfig.UsageStatsScreen(
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
                onNavigateBack = { navController.navigateUp() },
                onNavigateToPromptEditor = { route ->
                    navController.navigate(route)
                }
            )
        }

        // 新建联系人页面（iOS风格）
        // TD-00020 T066: 添加CreateContactScreen路由配置
        // RESEARCH-00054: 修复保存功能和添加事实功能
        composable(route = NavRoutes.CREATE_CONTACT) {
            val viewModel: CreateContactViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            
            // 监听保存成功状态，自动返回
            LaunchedEffect(uiState.saveSuccess) {
                if (uiState.saveSuccess) {
                    viewModel.resetSaveSuccess()
                    navController.navigateUp()
                }
            }
            
            CreateContactScreen(
                onCancel = { navController.navigateUp() },
                onDone = { formData, avatarUri, facts ->
                    // 调用ViewModel保存联系人
                    viewModel.saveContact(formData, avatarUri, facts)
                },
                onPickAvatar = {
                    // TODO: 集成图片选择器
                    // 后续可以使用ActivityResultLauncher实现
                }
            )
        }

        // 提示词编辑器
        promptEditorNavigation(navController)
    }
}
