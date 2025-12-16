package com.empathy.ai.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.presentation.ui.screen.prompt.PromptEditorScreen
import java.net.URLEncoder

/**
 * 提示词编辑器路由常量
 */
object PromptEditorRoutes {
    const val ROUTE = "prompt_editor"
    const val ARG_MODE = "mode"
    const val ARG_SCENE = "scene"
    const val ARG_CONTACT_ID = "contactId"
    const val ARG_CONTACT_NAME = "contactName"

    val FULL_ROUTE = "$ROUTE?" +
        "$ARG_MODE={$ARG_MODE}&" +
        "$ARG_SCENE={$ARG_SCENE}&" +
        "$ARG_CONTACT_ID={$ARG_CONTACT_ID}&" +
        "$ARG_CONTACT_NAME={$ARG_CONTACT_NAME}"

    /**
     * 构建全局场景编辑路由
     *
     * @param scene 场景类型
     * @return 路由字符串
     */
    fun globalScene(scene: PromptScene): String {
        return "$ROUTE?$ARG_MODE=global&$ARG_SCENE=${scene.name}"
    }

    /**
     * 构建联系人专属编辑路由
     *
     * @param contactId 联系人ID
     * @param contactName 联系人名称
     * @return 路由字符串
     */
    fun contactCustom(contactId: String, contactName: String): String {
        val encodedName = URLEncoder.encode(contactName, "UTF-8")
        return "$ROUTE?$ARG_MODE=contact&$ARG_CONTACT_ID=$contactId&$ARG_CONTACT_NAME=$encodedName"
    }
}

/**
 * 提示词编辑器导航图扩展
 *
 * @param navController 导航控制器
 */
fun NavGraphBuilder.promptEditorNavigation(
    navController: NavController
) {
    composable(
        route = PromptEditorRoutes.FULL_ROUTE,
        arguments = listOf(
            navArgument(PromptEditorRoutes.ARG_MODE) {
                type = NavType.StringType
                defaultValue = "global"
            },
            navArgument(PromptEditorRoutes.ARG_SCENE) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument(PromptEditorRoutes.ARG_CONTACT_ID) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument(PromptEditorRoutes.ARG_CONTACT_NAME) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) {
        PromptEditorScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
