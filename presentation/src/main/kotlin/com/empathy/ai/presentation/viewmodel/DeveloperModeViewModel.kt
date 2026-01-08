package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.repository.DeveloperModeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 开发者模式ViewModel
 *
 * 管理开发者模式的解锁状态，支持状态持久化。
 * 
 * 【BUG-00050 修复】
 * - 使用 DeveloperModeRepository 持久化开发者模式状态
 * - 导航离开设置页面后返回，开发者模式保持激活
 * - 应用重启后，开发者模式自动退出（通过 Session ID 机制）
 *
 * 【状态持久化策略】
 * 1. 解锁时：保存解锁状态和当前 Session ID
 * 2. 初始化时：检查 Session ID 是否匹配
 *    - 匹配：恢复开发者模式状态
 *    - 不匹配：重置状态（应用已重启）
 * 3. 退出时：清除解锁状态
 *
 * @see PRD-00033 开发者模式与系统提示词编辑
 * @see BUG-00050 开发者模式状态持久化问题
 */
@HiltViewModel
class DeveloperModeViewModel @Inject constructor(
    private val developerModeRepository: DeveloperModeRepository
) : ViewModel() {

    companion object {
        private const val UNLOCK_CLICK_COUNT = 7
        private const val SHOW_HINT_THRESHOLD = 3
    }

    /**
     * 开发者模式是否激活
     * 
     * 初始化时从 Repository 读取状态，确保导航返回后状态保持。
     */
    private val _isDeveloperMode = MutableStateFlow(
        developerModeRepository.isDeveloperModeUnlocked() && 
        developerModeRepository.isCurrentSession()
    )
    val isDeveloperMode: StateFlow<Boolean> = _isDeveloperMode.asStateFlow()

    private val _clickCount = MutableStateFlow(0)
    val clickCount: StateFlow<Int> = _clickCount.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        // 如果 Session 不匹配（应用重启），重置状态
        if (!developerModeRepository.isCurrentSession()) {
            developerModeRepository.reset()
            _isDeveloperMode.value = false
        }
    }

    /**
     * 处理版本号点击
     * 
     * 连续点击7次解锁开发者模式，点击3次后显示提示。
     */
    fun onVersionClick() {
        if (_isDeveloperMode.value) {
            // 已解锁，不再处理
            return
        }

        _clickCount.value++

        viewModelScope.launch {
            when {
                _clickCount.value >= UNLOCK_CLICK_COUNT -> {
                    // 解锁开发者模式
                    _isDeveloperMode.value = true
                    // 持久化状态
                    developerModeRepository.setDeveloperModeUnlocked(true)
                    developerModeRepository.updateSessionId()
                    _toastMessage.emit("已进入开发者模式")
                }
                _clickCount.value >= SHOW_HINT_THRESHOLD -> {
                    val remaining = UNLOCK_CLICK_COUNT - _clickCount.value
                    _toastMessage.emit("再点击 $remaining 次进入开发者模式")
                }
            }
        }
    }

    /**
     * 退出开发者模式
     *
     * 重置内存状态和持久化状态，包括 Session ID。
     * 这样退出后重新进入时，点击计数会从 0 开始。
     *
     * @see BUG-00050 修复：添加 reset() 调用以完整清除状态
     */
    fun exitDeveloperMode() {
        _isDeveloperMode.value = false
        _clickCount.value = 0
        // 完整重置：清除解锁状态和 Session ID
        // 这样确保退出后重新进入时，需要重新点击 7 次
        developerModeRepository.reset()
    }

    /**
     * 重置点击计数（用于超时重置等场景）
     */
    fun resetClickCount() {
        _clickCount.value = 0
    }
}
