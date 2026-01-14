package com.empathy.ai.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.repository.AiAdvisorPreferencesRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.presentation.ui.screen.advisor.AiAdvisorNavigationTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AI军师入口页面ViewModel
 *
 * ## 业务职责
 * 管理AI军师入口页面的状态和导航逻辑：
 * - 检查偏好设置中的上次联系人ID
 * - 验证联系人是否仍然存在
 * - 决定导航目标（对话界面或联系人选择页面）
 *
 * ## 关联文档
 * - PRD-00029: AI军师UI架构优化需求
 * - TDD-00029: AI军师UI架构优化技术设计
 * - FD-00029: AI军师UI架构优化功能设计
 * - BUG-00068: AI军师入口与设置回退及非Tab性能覆盖问题
 *
 * ## 导航决策逻辑
 * ```
 * 1. 读取 lastContactId
 * 2. 如果为空 → ContactSelect
 * 3. 如果非空 → 验证联系人存在
 *    - 存在 → Chat(contactId)
 *    - 不存在 → 清除偏好 → ContactSelect
 * ```
 *
 * ## BUG-00068修复: Tab缓存场景下导航刷新
 * 问题: AiAdvisorScreen被BottomNavScaffold缓存，二次进入不触发导航
 * 方案: 新增refreshNavigationTarget()方法，在Tab可见时强制刷新
 *
 * @see com.empathy.ai.presentation.ui.screen.advisor.AiAdvisorScreen
 */
private const val TAG = "AiAdvisorEntryVM"

@HiltViewModel
class AiAdvisorEntryViewModel @Inject constructor(
    private val aiAdvisorPreferences: AiAdvisorPreferencesRepository,
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAdvisorEntryUiState())
    val uiState: StateFlow<AiAdvisorEntryUiState> = _uiState.asStateFlow()

    init {
        checkNavigationTarget()
    }

    /**
     * 检查导航目标
     *
     * 业务规则 (PRD-00029/US-003):
     * - 有上次联系人记录且联系人存在 → 导航到对话界面
     * - 无上次联系人记录或联系人不存在 → 导航到联系人选择页面
     */
    private fun checkNavigationTarget() {
        Log.d(TAG, "checkNavigationTarget() start")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val lastContactId = aiAdvisorPreferences.getLastContactId()
            Log.d(TAG, "lastContactId=$lastContactId")

            if (lastContactId.isNullOrEmpty()) {
                // 无历史记录，导航到联系人选择页面
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        navigationTarget = AiAdvisorNavigationTarget.ContactSelect
                    )
                }
                Log.d(TAG, "navigate -> ContactSelect (no last contact)")
            } else {
                // 验证联系人是否存在
                contactRepository.getProfile(lastContactId)
                    .onSuccess { contact ->
                        Log.d(TAG, "getProfile success contactExists=${contact != null}")
                        if (contact != null) {
                            // 联系人存在，导航到对话界面
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    navigationTarget = AiAdvisorNavigationTarget.Chat(lastContactId)
                                )
                            }
                            Log.d(TAG, "navigate -> Chat contactId=$lastContactId")
                        } else {
                            // 联系人不存在，清除偏好并导航到联系人选择页面
                            aiAdvisorPreferences.clear()
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    navigationTarget = AiAdvisorNavigationTarget.ContactSelect
                                )
                            }
                            Log.d(TAG, "contact missing -> cleared prefs + ContactSelect")
                        }
                    }
                    .onFailure {
                        // 查询失败，清除偏好并导航到联系人选择页面
                        aiAdvisorPreferences.clear()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                navigationTarget = AiAdvisorNavigationTarget.ContactSelect
                            )
                        }
                        Log.e(TAG, "getProfile failed, fallback ContactSelect", it)
                    }
            }
        }
    }

    /**
     * 重置导航状态
     * 导航完成后调用，避免重复导航
     */
    fun resetNavigationState() {
        Log.d(TAG, "resetNavigationState()")
        _uiState.update { it.copy(navigationTarget = null) }
    }

    /**
     * 外部触发刷新导航目标
     * 用于Tab缓存场景下重新进入页面时刷新导航。
     */
    fun refreshNavigationTarget() {
        Log.d(TAG, "refreshNavigationTarget()")
        checkNavigationTarget()
    }
}

/**
 * AI军师入口页面UI状态
 *
 * @property isLoading 是否正在加载
 * @property navigationTarget 导航目标，非空时触发导航
 */
data class AiAdvisorEntryUiState(
    val isLoading: Boolean = true,
    val navigationTarget: AiAdvisorNavigationTarget? = null
)
