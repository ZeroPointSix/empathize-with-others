package com.empathy.ai.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.repository.SettingsRepository
import com.empathy.ai.domain.util.FloatingWindowManager
import com.empathy.ai.presentation.ui.screen.settings.SettingsUiEvent
import com.empathy.ai.presentation.ui.screen.settings.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设置页面的 ViewModel
 *
 * 职责：
 * 1. 管理设置页面的 UI 状态
 * 2. 处理用户交互事件
 * 3. 调用 Repository 保存和读取设置
 * 4. 管理 API Key 的安全存储
 * 5. 管理悬浮窗权限和服务
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val floatingWindowPreferences: com.empathy.ai.data.local.FloatingWindowPreferences,
    private val aiProviderRepository: com.empathy.ai.domain.repository.AiProviderRepository
) : AndroidViewModel(application) {

    // 私有可变状态
    private val _uiState = MutableStateFlow(SettingsUiState())

    // 公开不可变状态
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    // 处理状态标志
    private var isProcessingFloatingWindowToggle = false
    private var isProcessingPermissionCheck = false

    init {
        // 初始化时加载设置
        loadSettings()
        // 检查悬浮窗权限
        checkFloatingWindowPermission()
        // 加载悬浮窗状态
        loadFloatingWindowState()
        // 加载服务商列表
        loadProviders()
    }

    /**
     * 统一的事件处理入口
     *
     * 注意：API Key 配置已移至服务商配置中统一管理
     */
    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            // === AI 服务商事件 ===
            is SettingsUiEvent.SelectProvider -> selectProvider(event.provider)
            is SettingsUiEvent.ShowProviderDialog -> showProviderDialog()
            is SettingsUiEvent.HideProviderDialog -> hideProviderDialog()

            // === 隐私设置事件 ===
            is SettingsUiEvent.ToggleDataMasking -> toggleDataMasking()
            is SettingsUiEvent.ToggleLocalFirstMode -> toggleLocalFirstMode()

            // === AI 分析设置事件 ===
            is SettingsUiEvent.ChangeHistoryConversationCount -> changeHistoryConversationCount(event.count)

            // === 数据管理事件 ===
            is SettingsUiEvent.ShowClearDataDialog -> showClearDataDialog()
            is SettingsUiEvent.HideClearDataDialog -> hideClearDataDialog()
            is SettingsUiEvent.ClearAllData -> clearAllData()

            // === 悬浮窗事件 ===
            is SettingsUiEvent.ToggleFloatingWindow -> toggleFloatingWindow()
            is SettingsUiEvent.RequestFloatingWindowPermission -> {} // 由 UI 处理
            is SettingsUiEvent.ShowPermissionDialog -> showPermissionDialog()
            is SettingsUiEvent.HidePermissionDialog -> hidePermissionDialog()
            is SettingsUiEvent.CheckFloatingWindowPermission -> checkFloatingWindowPermission()

            // === 通用事件 ===
            is SettingsUiEvent.ClearError -> clearError()
            is SettingsUiEvent.ClearSuccessMessage -> clearSuccessMessage()
            is SettingsUiEvent.NavigateBack -> {} // 由 UI 处理
        }
    }

    /**
     * 加载所有设置
     *
     * 注意：API Key 配置已移至服务商配置中统一管理
     */
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                // 加载隐私设置
                val dataMasking = settingsRepository.getDataMaskingEnabled()
                    .getOrDefault(true)
                val localFirst = settingsRepository.getLocalFirstModeEnabled()
                    .getOrDefault(true)
                
                // 加载历史对话条数配置
                val historyCount = settingsRepository.getHistoryConversationCount()
                    .getOrDefault(5)
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        dataMaskingEnabled = dataMasking,
                        localFirstMode = localFirst,
                        historyConversationCount = historyCount
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "加载设置失败"
                    )
                }
            }
        }
    }

    // === AI 服务商方法 ===

    /**
     * 加载服务商列表
     *
     * 从数据库加载所有服务商，并更新 UI 状态
     * 
     * 注意：使用 Flow.catch 操作符处理异常，而不是 try-catch
     * 这样可以正确处理 CancellationException（协程取消时的正常信号）
     * Flow.catch 不会捕获 CancellationException，允许协程正常取消
     * 
     * @see BUG-00028 悬浮窗加载时 JobCancellationException 问题
     */
    private fun loadProviders() {
        viewModelScope.launch {
            aiProviderRepository.getAllProviders()
                .catch { e ->
                    // Flow.catch 不会捕获 CancellationException
                    // 只有真正的业务异常会到达这里
                    android.util.Log.e("SettingsViewModel", "加载服务商列表失败", e)
                    _uiState.update {
                        it.copy(error = "加载服务商列表失败: ${e.message}")
                    }
                }
                .collect { providers ->
                    val providerNames = providers.map { it.name }
                    
                    // 查找默认服务商
                    val defaultProvider = providers.find { it.isDefault }
                    val selectedName = defaultProvider?.name 
                        ?: providers.firstOrNull()?.name 
                        ?: ""
                    
                    _uiState.update {
                        it.copy(
                            availableProviders = providerNames,
                            providersList = providers,
                            selectedProvider = selectedName
                        )
                    }
                }
        }
    }

    /**
     * 选择服务商
     *
     * 从已加载的服务商列表中查找，而不是通过 ID 查询数据库
     *
     * @param providerName 服务商名称
     */
    private fun selectProvider(providerName: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // 1. 从已加载的列表中查找服务商（通过名称）
                val currentState = _uiState.value
                val selectedProvider = currentState.providersList.find { it.name == providerName }
                
                if (selectedProvider == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "服务商 $providerName 不存在"
                        )
                    }
                    return@launch
                }

                // 2. 设置为默认服务商（通过 ID）
                val setDefaultResult = aiProviderRepository.setDefaultProvider(selectedProvider.id)
                
                setDefaultResult.onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedProvider = providerName,
                            showProviderDialog = false,
                            successMessage = "AI 服务商已切换为 $providerName"
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "切换服务商失败"
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "切换服务商失败", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "切换服务商失败"
                    )
                }
            }
        }
    }

    private fun showProviderDialog() {
        _uiState.update { it.copy(showProviderDialog = true) }
    }

    private fun hideProviderDialog() {
        _uiState.update { it.copy(showProviderDialog = false) }
    }

    // === 隐私设置方法 ===

    /**
     * 切换数据掩码开关
     */
    private fun toggleDataMasking() {
        viewModelScope.launch {
            try {
                val newValue = !_uiState.value.dataMaskingEnabled
                
                // 保存到 Repository
                settingsRepository.setDataMaskingEnabled(newValue).onSuccess {
                    _uiState.update {
                        it.copy(
                            dataMaskingEnabled = newValue,
                            successMessage = "数据掩码已${if (newValue) "开启" else "关闭"}"
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(error = "保存设置失败: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "切换数据掩码失败", e)
                _uiState.update {
                    it.copy(error = "切换数据掩码失败: ${e.message}")
                }
            }
        }
    }

    /**
     * 切换本地优先模式开关
     */
    private fun toggleLocalFirstMode() {
        viewModelScope.launch {
            try {
                val newValue = !_uiState.value.localFirstMode
                
                // 保存到 Repository
                settingsRepository.setLocalFirstModeEnabled(newValue).onSuccess {
                    _uiState.update {
                        it.copy(
                            localFirstMode = newValue,
                            successMessage = "本地优先模式已${if (newValue) "开启" else "关闭"}"
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(error = "保存设置失败: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "切换本地优先模式失败", e)
                _uiState.update {
                    it.copy(error = "切换本地优先模式失败: ${e.message}")
                }
            }
        }
    }

    // === AI 分析设置方法 ===

    /**
     * 更改历史对话条数
     *
     * @param count 条数，必须是 0/5/10 之一
     */
    private fun changeHistoryConversationCount(count: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setHistoryConversationCount(count).onSuccess {
                    val label = when (count) {
                        0 -> "不发送历史"
                        5 -> "最近5条"
                        10 -> "最近10条"
                        else -> "${count}条"
                    }
                    _uiState.update {
                        it.copy(
                            historyConversationCount = count,
                            successMessage = "历史对话条数已设置为$label"
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(error = "保存设置失败: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "更改历史对话条数失败", e)
                _uiState.update {
                    it.copy(error = "更改历史对话条数失败: ${e.message}")
                }
            }
        }
    }

    // === 数据管理方法 ===

    private fun showClearDataDialog() {
        _uiState.update { it.copy(showClearDataDialog = true) }
    }

    private fun hideClearDataDialog() {
        _uiState.update { it.copy(showClearDataDialog = false) }
    }

    /**
     * 清除所有设置数据
     * 
     * MVP阶段清除范围：
     * - AI服务商配置
     * - 隐私保护设置
     * - 悬浮窗设置
     * 
     * 不清除：
     * - 联系人数据
     * - 标签数据
     */
    private fun clearAllData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // 1. 清除所有服务商数据
                val providers = _uiState.value.providersList
                providers.forEach { provider ->
                    aiProviderRepository.deleteProvider(provider.id)
                }
                
                // 2. 重置隐私设置为默认值
                settingsRepository.setDataMaskingEnabled(true)
                settingsRepository.setLocalFirstModeEnabled(true)
                
                // 3. 清除悬浮窗设置
                floatingWindowPreferences.saveEnabled(false)
                if (_uiState.value.floatingWindowEnabled) {
                    com.empathy.ai.domain.util.FloatingWindowManager.stopService(getApplication())
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedProvider = "",
                        availableProviders = emptyList(),
                        providersList = emptyList(),
                        dataMaskingEnabled = true,
                        localFirstMode = true,
                        floatingWindowEnabled = false,
                        showClearDataDialog = false,
                        successMessage = "所有设置已清除"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "清除数据失败", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "清除数据失败"
                    )
                }
            }
        }
    }

    // === 通用方法 ===

    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    // === 悬浮窗方法 ===

    /**
     * 检查悬浮窗权限
     */
    fun checkFloatingWindowPermission() {
        // 防止重复处理
        if (isProcessingPermissionCheck) {
            android.util.Log.d("SettingsViewModel", "权限检查正在进行中，跳过")
            return
        }
        
        isProcessingPermissionCheck = true
        
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val permissionResult = FloatingWindowManager.hasPermission(getApplication())
                val hasPermission = when (permissionResult) {
                    is FloatingWindowManager.PermissionResult.Granted -> true
                    is FloatingWindowManager.PermissionResult.Denied -> {
                        android.util.Log.w("SettingsViewModel", "悬浮窗权限被拒绝: ${permissionResult.message}")
                        false
                    }
                    is FloatingWindowManager.PermissionResult.Error -> {
                        android.util.Log.e("SettingsViewModel", "检查悬浮窗权限出错: ${permissionResult.message}")
                        false
                    }
                }
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasFloatingWindowPermission = hasPermission,
                        error = if (permissionResult is FloatingWindowManager.PermissionResult.Error) {
                            permissionResult.message
                        } else null
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "检查悬浮窗权限失败", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasFloatingWindowPermission = false,
                        error = "检查权限失败: ${e.message}"
                    )
                }
            } finally {
                isProcessingPermissionCheck = false
            }
        }
    }

    /**
     * 切换悬浮窗功能
     */
    private fun toggleFloatingWindow() {
        // 防止重复处理
        if (isProcessingFloatingWindowToggle) {
            android.util.Log.d("SettingsViewModel", "悬浮窗切换正在进行中，跳过")
            return
        }
        
        val currentState = _uiState.value
        
        if (!currentState.floatingWindowEnabled) {
            // 启用悬浮窗
            if (!currentState.hasFloatingWindowPermission) {
                // 没有权限，显示权限说明对话框
                showPermissionDialog()
            } else {
                // 有权限，直接启动服务
                startFloatingWindowService()
            }
        } else {
            // 禁用悬浮窗
            stopFloatingWindowService()
        }
    }

    /**
     * 加载悬浮窗状态
     * 
     * 不仅恢复 UI 状态，还会在状态为启用时自动恢复服务
     * 这解决了应用重启后悬浮窗不自动显示的问题
     * 
     * BUG-00019修复：添加服务运行状态检查，避免重复启动服务导致状态重置
     */
    private fun loadFloatingWindowState() {
        val state = floatingWindowPreferences.loadState()
        _uiState.update {
            it.copy(floatingWindowEnabled = state.isEnabled)
        }
        
        // 如果状态为启用，检查权限并自动启动服务
        if (state.isEnabled) {
            viewModelScope.launch {
                val permissionResult = FloatingWindowManager.hasPermission(getApplication())
                if (permissionResult is FloatingWindowManager.PermissionResult.Granted) {
                    // BUG-00019修复：检查服务是否已运行，避免重复启动
                    if (FloatingWindowManager.isServiceRunning(getApplication())) {
                        android.util.Log.d("SettingsViewModel", "悬浮窗服务已运行，跳过启动")
                        return@launch
                    }
                    
                    // 有权限且服务未运行，自动启动服务恢复悬浮窗
                    android.util.Log.d("SettingsViewModel", "检测到悬浮窗状态为启用，自动恢复服务")
                    startFloatingWindowService()
                } else {
                    // 权限丢失，重置状态以保持 UI 与实际状态同步
                    android.util.Log.w("SettingsViewModel", "悬浮窗权限丢失，重置状态")
                    floatingWindowPreferences.saveEnabled(false)
                    _uiState.update { it.copy(floatingWindowEnabled = false) }
                }
            }
        }
    }

    /**
     * 启动悬浮窗服务
     */
    private fun startFloatingWindowService() {
        // 设置处理标志
        isProcessingFloatingWindowToggle = true
        
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                val startResult = FloatingWindowManager.startService(getApplication())
                
                when (startResult) {
                    is FloatingWindowManager.ServiceStartResult.Success -> {
                        // 保存启用状态
                        try {
                            floatingWindowPreferences.saveEnabled(true)
                        } catch (e: Exception) {
                            android.util.Log.e("SettingsViewModel", "保存悬浮窗状态失败", e)
                        }
                        
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                floatingWindowEnabled = true,
                                successMessage = "悬浮窗服务已启动"
                            )
                        }
                    }
                    is FloatingWindowManager.ServiceStartResult.PermissionDenied -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                floatingWindowEnabled = false,
                                error = startResult.message
                            )
                        }
                    }
                    is FloatingWindowManager.ServiceStartResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                floatingWindowEnabled = false,
                                error = startResult.message
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "启动悬浮窗服务失败", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        floatingWindowEnabled = false,
                        error = "启动悬浮窗服务失败：${e.message}"
                    )
                }
            } finally {
                isProcessingFloatingWindowToggle = false
            }
        }
    }

    /**
     * 停止悬浮窗服务
     */
    private fun stopFloatingWindowService() {
        // 设置处理标志
        isProcessingFloatingWindowToggle = true
        
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                val stopResult = FloatingWindowManager.stopService(getApplication())
                
                when (stopResult) {
                    is FloatingWindowManager.ServiceStopResult.Success -> {
                        // 保存禁用状态
                        try {
                            floatingWindowPreferences.saveEnabled(false)
                        } catch (e: Exception) {
                            android.util.Log.e("SettingsViewModel", "保存悬浮窗状态失败", e)
                        }
                        
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                floatingWindowEnabled = false,
                                successMessage = "悬浮窗服务已停止"
                            )
                        }
                    }
                    is FloatingWindowManager.ServiceStopResult.NotRunning -> {
                        // 服务未运行，但仍然更新状态
                        try {
                            floatingWindowPreferences.saveEnabled(false)
                        } catch (e: Exception) {
                            android.util.Log.e("SettingsViewModel", "保存悬浮窗状态失败", e)
                        }
                        
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                floatingWindowEnabled = false,
                                successMessage = "悬浮窗服务已停止"
                            )
                        }
                    }
                    is FloatingWindowManager.ServiceStopResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = stopResult.message
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "停止悬浮窗服务失败", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "停止悬浮窗服务失败：${e.message}"
                    )
                }
            } finally {
                isProcessingFloatingWindowToggle = false
            }
        }
    }

    /**
     * 显示权限说明对话框
     */
    private fun showPermissionDialog() {
        _uiState.update { it.copy(showPermissionDialog = true) }
    }

    /**
     * 隐藏权限说明对话框
     */
    private fun hidePermissionDialog() {
        _uiState.update { it.copy(showPermissionDialog = false) }
    }
}
