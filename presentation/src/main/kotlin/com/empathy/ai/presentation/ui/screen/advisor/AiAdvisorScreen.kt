package com.empathy.ai.presentation.ui.screen.advisor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.empathy.ai.presentation.theme.iOSBackground
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.viewmodel.AiAdvisorEntryViewModel
import android.util.Log

/**
 * AI军师入口页面
 *
 * ## 业务职责
 * 作为AI军师功能的入口路由页面，负责检查偏好设置并路由到正确的页面：
 * - 有上次联系人记录 → 导航到对话界面（自动创建新会话）
 * - 无上次联系人记录 → 导航到联系人选择页面
 *
 * ## 关联文档
 * - PRD-00029: AI军师UI架构优化需求
 * - TDD-00029: AI军师UI架构优化技术设计
 * - FD-00029: AI军师UI架构优化功能设计
 *
 * ## 导航流程
 * ```
 * 用户点击AI军师Tab
 *         │
 *         ▼
 * AiAdvisorScreen (入口页面)
 *         │
 *         ▼
 * 检查 AiAdvisorPreferences.lastContactId
 *         │
 *    ┌────┴────┐
 *    ▼         ▼
 * 有值      无值
 *    │         │
 *    ▼         ▼
 * 对话界面  联系人选择
 * ```
 *
 * @param onNavigateToChat 导航到对话界面的回调，参数为联系人ID
 * @param onNavigateToContactSelect 导航到联系人选择页面的回调
 * @param viewModel 注入的ViewModel
 */
@Composable
fun AiAdvisorScreen(
    onNavigateToChat: (String) -> Unit,
    onNavigateToContactSelect: () -> Unit,
    isVisible: Boolean = true,
    viewModel: AiAdvisorEntryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var hasInitialized by rememberSaveable { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(isVisible) {
        if (isVisible) {
            // BUG-00068修复: 防止首帧重复刷新导航导致重复入栈
            // hasInitialized=false时跳过刷新，仅在后续切换回来时才触发
            // 配合MainActivity.kt中的launchSingleTop实现稳定的导航行为
            if (hasInitialized) {
                viewModel.refreshNavigationTarget()
            } else {
                hasInitialized = true
            }
        }
    }

    // [BUG-00063修复] ON_RESUME时强制刷新导航目标，修复Tab缓存场景下白屏问题
    // 问题根因: 从AI配置页返回时，AiAdvisorScreen可见但navigationTarget为空
    // 解决方案: 监听ON_RESUME生命周期事件，可见时重新检查导航目标
    // 设计权衡 (BUG-00063): 使用DisposableEffect而非LaunchedEffect，确保生命周期观察者正确注册和清理
    DisposableEffect(lifecycleOwner, isVisible) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isVisible) {
                viewModel.refreshNavigationTarget()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 根据偏好设置决定导航目标
    // [BUG-00063修复] 增加可见性门控：不可见时清理navigationTarget，避免隐藏Tab干扰导航
    LaunchedEffect(uiState.navigationTarget, isVisible) {
        // [调试日志] BUG-00063: 记录导航状态变化，定位白屏根因
        Log.d(
            "AiAdvisorScreen",
            "NavEffect visible=$isVisible loading=${uiState.isLoading} target=${uiState.navigationTarget}"
        )
        // [可见性门控] 不可见时清理导航目标，防止隐藏Tab触发副作用
        if (!isVisible) {
            if (uiState.navigationTarget != null) {
                viewModel.resetNavigationState()
            }
            return@LaunchedEffect
        }

        when (val target = uiState.navigationTarget) {
            is AiAdvisorNavigationTarget.Chat -> {
                onNavigateToChat(target.contactId)
                viewModel.resetNavigationState()
            }
            is AiAdvisorNavigationTarget.ContactSelect -> {
                onNavigateToContactSelect()
                viewModel.resetNavigationState()
            }
            null -> {
                // 等待加载完成
            }
        }
    }

    // 显示加载状态
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(iOSBackground),
        contentAlignment = Alignment.Center
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(color = iOSBlue)
        }
    }
}

/**
 * AI军师导航目标
 */
sealed class AiAdvisorNavigationTarget {
    /**
     * 导航到对话界面
     * @property contactId 联系人ID
     */
    data class Chat(val contactId: String) : AiAdvisorNavigationTarget()

    /**
     * 导航到联系人选择页面
     */
    data object ContactSelect : AiAdvisorNavigationTarget()
}
