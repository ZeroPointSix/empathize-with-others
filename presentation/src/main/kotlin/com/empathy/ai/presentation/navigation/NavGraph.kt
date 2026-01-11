package com.empathy.ai.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.empathy.ai.presentation.ui.screen.aiconfig.AiConfigScreen
import com.empathy.ai.presentation.ui.screen.advisor.AiAdvisorChatScreen
import com.empathy.ai.presentation.ui.screen.advisor.AiAdvisorScreen
import com.empathy.ai.presentation.ui.screen.advisor.ContactSelectScreen
import com.empathy.ai.presentation.ui.screen.advisor.SessionHistoryScreen
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
 * ## 业务职责
 * 定义完整的应用导航结构，使用Navigation Compose实现页面路由管理。
 * 采用单Activity多页面的导航模式，支持参数传递和页面转场动画。
 *
 * ## 架构设计
 * - **单入口**: 从联系人列表(CONTACT_LIST)作为启动页
 * - **扁平化**: 所有页面平铺在NavHost中，无嵌套导航
 * - **参数化**: 通过NavArgument传递页面参数（联系人ID、服务商ID等）
 * - **动画化**: 统一的页面转场动画（AnimationSpec定义）
 *
 * ## 导航流程
 * ```
 * ┌─────────────────────────────────────────────────────────┐
 * │                      MainActivity                        │
 * │              NavController + NavHost                    │
 * └─────────────────────────────────────────────────────────┘
 *                           │
 *                           ▼
 * ┌─────────────────────────────────────────────────────────┐
 * │                  NavGraph (startDestination=CONTACT_LIST)│
 * ├─────────────────────────────────────────────────────────┤
 * │  CONTACT_LIST ──→ CONTACT_DETAIL_TAB/{id}              │  ← 联系人管理
 * │       │           CREATE_CONTACT                        │
 * │       │                                                   │
 * │       ├──→ CHAT/{id}                                   │  ← AI分析
 * │       │                                                   │
 * │       ├──→ BRAIN_TAG                                   │  ← 标签管理
 * │       │                                                   │
 * │       ├──→ AI_ADVISOR ──→ AI_ADVISOR_CHAT/{id}         │  ← AI军师 (TD-00026)
 * │       │                                                   │
 * │       └──→ SETTINGS ──→ AI_CONFIG ──→ ADD_PROVIDER     │  ← 设置
 * │                       │          EDIT_PROVIDER/{id}     │
 * │                       │          USAGE_STATS            │
 * │                       └──→ USER_PROFILE                │
 * └─────────────────────────────────────────────────────────┘
 * ```
 *
 * ## 页面转场动画
 * - **进入动画**: 从右侧滑入
 * - **退出动画**: 向左滑出
 * - **弹出进入**: 从左侧滑入（返回时）
 * - **弹出退出**: 向右滑出（返回时）
 * - 动画时长: 200ms/150ms，缓动曲线: FastOutSlowInEasing
 *
 * ## 设计决策
 * 1. **底部导航路由**: BOTTOM_NAV_ROUTES定义底部Tab页，支持单例启动
 * 2. **参数化路由**: 联系人/服务商等使用ID参数，支持动态路由生成
 * 3. **状态恢复**: 使用saveState/restoreState保持页面状态
 * 4. **页面复用**: AddProviderScreen被EditProviderScreen复用（BUG-00040修复）
 *
 * ## 关联文档
 * - TD-00026: AI军师功能（新增AI_ADVISOR和AI_ADVISOR_CHAT路由）
 * - TD-00020: 新建联系人功能（新增CREATE_CONTACT路由）
 * - TD-00021: 服务商配置功能（新增ADD_PROVIDER/EDIT_PROVIDER路由）
 * - TD-00025: 用量统计功能（新增USAGE_STATS路由）
 *
 * @param navController 导航控制器，由MainActivity创建并传入
 * @param modifier 组合修饰符
 * @param includeTabScreens 是否渲染底部Tab页面（用于区分主Tab缓存与非Tab导航）
 * @see NavRoutes 路由常量定义
 * @see AnimationSpec 转场动画定义
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    includeTabScreens: Boolean = true,
    useTransition: Boolean = true
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.CONTACT_LIST,
        modifier = modifier,
        enterTransition = {
            if (useTransition) AnimationSpec.PageEnterTransition else EnterTransition.None
        },
        exitTransition = {
            if (useTransition) AnimationSpec.PageExitTransition else ExitTransition.None
        },
        popEnterTransition = {
            if (useTransition) AnimationSpec.PagePopEnterTransition else EnterTransition.None
        },
        popExitTransition = {
            if (useTransition) AnimationSpec.PagePopExitTransition else ExitTransition.None
        }
    ) {
        // 联系人列表页面
        composable(route = NavRoutes.CONTACT_LIST) {
            if (includeTabScreens) {
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
                        navController.navigate(NavRoutes.SETTINGS) {
                            popUpTo(NavRoutes.CONTACT_LIST) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
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
            } else {
                EmptyScreen()
            }
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
            if (includeTabScreens) {
                SettingsScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToAiConfig = {
                        navController.navigate(NavRoutes.aiConfig(NavRoutes.SOURCE_SETTINGS)) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToPromptEditor = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToUserProfile = {
                        navController.navigate(NavRoutes.USER_PROFILE) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSystemPromptList = {
                        navController.navigate(NavRoutes.SYSTEM_PROMPT_LIST) {
                            launchSingleTop = true
                        }
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
            } else {
                EmptyScreen()
            }
        }

        // AI军师入口页面
        // PRD-00029: 修改为入口路由页面，检查偏好设置决定导航目标
        composable(route = NavRoutes.AI_ADVISOR) {
            if (includeTabScreens) {
                AiAdvisorScreen(
                    onNavigateToChat = { contactId ->
                        // BUG-00068修复: 使用CONTACT_LIST作为稳定锚点，避免回退栈残留旧会话
                        // popUpTo(CONTACT_LIST)确保返回时能正确回到联系人Tab
                        // saveState=true保持Tab页面状态，避免重建
                        navController.navigate(NavRoutes.aiAdvisorChat(contactId)) {
                            popUpTo(NavRoutes.CONTACT_LIST) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToContactSelect = {
                        // BUG-00068修复: 同样使用CONTACT_LIST锚点，保持返回一致性
                        navController.navigate(NavRoutes.AI_ADVISOR_CONTACTS) {
                            popUpTo(NavRoutes.CONTACT_LIST) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            } else {
                EmptyScreen()
            }
        }

        // AI军师对话页面
        composable(
            route = NavRoutes.AI_ADVISOR_CHAT,
            arguments = listOf(
                navArgument(NavRoutes.AI_ADVISOR_CHAT_ARG_ID) {
                    type = NavType.StringType
                },
                // BUG-00058: 添加createNew参数
                navArgument(NavRoutes.AI_ADVISOR_CHAT_ARG_CREATE_NEW) {
                    type = NavType.BoolType
                    defaultValue = false
                },
                // BUG-00061: 添加sessionId参数，支持加载指定会话
                navArgument(NavRoutes.AI_ADVISOR_CHAT_ARG_SESSION_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString(NavRoutes.AI_ADVISOR_CHAT_ARG_ID) ?: ""
            // BUG-00058: 获取createNew参数
            val createNew = backStackEntry.arguments?.getBoolean(NavRoutes.AI_ADVISOR_CHAT_ARG_CREATE_NEW) ?: false
            // BUG-00061: 获取sessionId参数
            val sessionId = backStackEntry.arguments?.getString(NavRoutes.AI_ADVISOR_CHAT_ARG_SESSION_ID)
            AiAdvisorChatScreen(
                createNew = createNew,  // BUG-00058: 传递createNew参数
                sessionId = sessionId,  // BUG-00061: 传递sessionId参数
                onNavigateBack = {
                    // 统一返回到联系人列表，避免AI军师入口叠加导致双层返回
                    navController.popBackStack(NavRoutes.CONTACT_LIST, false)
                },
                onNavigateToContact = { newContactId ->
                    navController.navigate(NavRoutes.aiAdvisorChat(newContactId)) {
                        popUpTo(NavRoutes.AI_ADVISOR_CHAT) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.aiConfig(NavRoutes.SOURCE_ADVISOR_CHAT)) {
                        popUpTo(NavRoutes.AI_ADVISOR_CHAT) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToSessionHistory = {
                    navController.navigate(NavRoutes.aiAdvisorSessions(contactId)) {
                        popUpTo(NavRoutes.AI_ADVISOR_CHAT) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToContactSelect = {
                    navController.navigate(NavRoutes.AI_ADVISOR_CONTACTS) {
                        popUpTo(NavRoutes.AI_ADVISOR_CHAT) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // AI军师会话历史页面
        // PRD-00029: 新增会话历史路由
        composable(
            route = NavRoutes.AI_ADVISOR_SESSIONS,
            arguments = listOf(
                navArgument(NavRoutes.AI_ADVISOR_SESSIONS_ARG_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString(NavRoutes.AI_ADVISOR_SESSIONS_ARG_ID) ?: ""
            SessionHistoryScreen(
                contactId = contactId,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToChat = { sessionId ->
                    // BUG-00061修复：传递sessionId参数，加载指定会话
                    navController.navigate(NavRoutes.aiAdvisorChat(contactId, sessionId = sessionId)) {
                        popUpTo(NavRoutes.AI_ADVISOR_SESSIONS) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onCreateNewSession = {
                    // BUG-00058修复：传递createNew=true，触发新会话创建
                    navController.navigate(NavRoutes.aiAdvisorChat(contactId, createNew = true)) {
                        popUpTo(NavRoutes.AI_ADVISOR_SESSIONS) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // AI军师联系人选择页面
        // PRD-00029: 新增联系人选择路由
        composable(route = NavRoutes.AI_ADVISOR_CONTACTS) {
            ContactSelectScreen(
                onNavigateBack = { navController.navigateUp() },
                onSelectContact = { contactId ->
                    // 保存联系人ID并导航到对话界面
                    navController.navigate(NavRoutes.aiAdvisorChat(contactId)) {
                        popUpTo(NavRoutes.AI_ADVISOR_CONTACTS) { inclusive = true }
                        launchSingleTop = true
                    }
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
        composable(
            route = NavRoutes.AI_CONFIG_ROUTE,
            arguments = listOf(
                navArgument(NavRoutes.AI_CONFIG_ARG_SOURCE) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val source = backStackEntry.arguments?.getString(NavRoutes.AI_CONFIG_ARG_SOURCE)
            AiConfigScreen(
                onNavigateBack = {
                    if (source == NavRoutes.SOURCE_SETTINGS) {
                        navController.navigate(NavRoutes.SETTINGS) {
                            popUpTo(NavRoutes.AI_CONFIG_ROUTE) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigateUp()
                    }
                },
                onNavigateToAddProvider = {
                    navController.navigate(NavRoutes.ADD_PROVIDER) {
                        launchSingleTop = true
                    }
                },
                onNavigateToEditProvider = { providerId ->
                    navController.navigate(NavRoutes.editProvider(providerId)) {
                        launchSingleTop = true
                    }
                },
                // TD-00025: 添加用量统计导航
                onNavigateToUsageStats = {
                    navController.navigate(NavRoutes.USAGE_STATS) {
                        launchSingleTop = true
                    }
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
                onNavigateBack = {
                    val popped = navController.popBackStack(NavRoutes.CONTACT_LIST, false)
                    if (!popped) {
                        navController.navigate(NavRoutes.CONTACT_LIST) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onNavigateToPromptEditor = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
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

        // 系统提示词列表页面（开发者模式）
        // PRD-00033: 开发者模式 - 系统提示词编辑
        composable(route = NavRoutes.SYSTEM_PROMPT_LIST) {
            com.empathy.ai.presentation.ui.screen.settings.SystemPromptListScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToEdit = { scene ->
                    navController.navigate(NavRoutes.systemPromptEdit(scene)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // 系统提示词编辑页面（开发者模式）
        // PRD-00033: 开发者模式 - 系统提示词编辑
        composable(
            route = NavRoutes.SYSTEM_PROMPT_EDIT,
            arguments = listOf(
                navArgument(NavRoutes.SYSTEM_PROMPT_EDIT_ARG_SCENE) {
                    type = NavType.StringType
                }
            )
        ) {
            com.empathy.ai.presentation.ui.screen.settings.SystemPromptEditScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}

@Composable
private fun EmptyScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "", style = MaterialTheme.typography.bodySmall)
    }
}
