package com.empathy.ai.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.FloatingWindowPreferencesRepository
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
 * 应用设置页面 ViewModel
 *
 * ## 业务职责
 * 管理应用的全局配置项：
 * - API服务商配置（API Key、Base URL、模型选择）
 * - 外观设置（主题、语言）
 * - 数据掩码和本地优先模式
 * - 历史对话数量配置
 * - 悬浮窗权限和服务管理
 * - 数据清除和安全设置
 *
 * ## 核心数据流
 * ```
 * LoadSettings → Modify → Validate → Save → ApplyToApp
 * ```
 *
 * ## 关键业务概念
 * - **AiProvider**: AI服务商配置（支持多服务商切换）
 * - **DataMasking**: 数据脱敏设置，保护敏感信息
 * - **LocalFirstMode**: 本地优先模式，减少API调用
 * - **FloatingWindowPermission**: 悬浮窗系统权限管理
 *
 * ## 设计决策
 * - **配置持久化**: 使用DataStore而非SharedPreferences（类型安全）
 * - **敏感信息加密**: API Key使用EncryptedSharedPreferences
 * - **权限检查**: 启动时检查悬浮窗权限，异常状态自动恢复
 * - **热更新**: 部分设置无需重启即可生效
 * - **防抖处理**: 悬浮窗切换和权限检查使用标志防止重复操作
 *
 * ## 关键设置项
 * - AI Provider: 支持多服务商切换（OpenAI/Claude/DeepSeek等）
 * - Model Selection: 不同场景可选择不同模型
 * - Theme: 浅色/深色/跟随系统
 * - History Count: 历史对话发送条数（0/5/10/自定义）
 *
 * @see com.empathy.ai.presentation.ui.screen.settings.SettingsScreen
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val floatingWindowPreferencesRepository: FloatingWindowPreferencesRepository,
    private val aiProviderRepository: AiProviderRepository,
    private val floatingWindowManager: FloatingWindowManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    /**
     * 获取设置界面显示的场景列表（按预定义顺序）
     * 只返回4个核心场景：ANALYZE、POLISH、REPLY、SUMMARY
     */
    val promptScenesOrdered: List<PromptScene> = PromptScene.SETTINGS_SCENE_ORDER
    
    private var isProcessingFloatingWindowToggle = false
    private var isProcessingPermissionCheck = false

    init {
        loadSettings()
        checkFloatingWindowPermission()
        loadFloatingWindowState()
        loadProviders()
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.SelectProvider -> selectProvider(event.provider)
            is SettingsUiEvent.ShowProviderDialog -> showProviderDialog()
            is SettingsUiEvent.HideProviderDialog -> hideProviderDialog()
            is SettingsUiEvent.ToggleDataMasking -> toggleDataMasking()
            is SettingsUiEvent.ToggleLocalFirstMode -> toggleLocalFirstMode()
            is SettingsUiEvent.ChangeHistoryConversationCount -> changeHistoryConversationCount(event.count)
            is SettingsUiEvent.ShowClearDataDialog -> showClearDataDialog()
            is SettingsUiEvent.HideClearDataDialog -> hideClearDataDialog()
            is SettingsUiEvent.ClearAllData -> clearAllData()
            is SettingsUiEvent.ToggleFloatingWindow -> toggleFloatingWindow()
            is SettingsUiEvent.RequestFloatingWindowPermission -> requestFloatingWindowPermission()
            is SettingsUiEvent.PermissionRequestHandled -> clearPendingPermissionRequest()
            is SettingsUiEvent.ShowPermissionDialog -> showPermissionDialog()
            is SettingsUiEvent.HidePermissionDialog -> hidePermissionDialog()
            is SettingsUiEvent.CheckFloatingWindowPermission -> checkFloatingWindowPermission()
            is SettingsUiEvent.ClearError -> clearError()
            is SettingsUiEvent.ClearSuccessMessage -> clearSuccessMessage()
            is SettingsUiEvent.NavigateBack -> {}
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                val dataMasking = settingsRepository.getDataMaskingEnabled()
                    .getOrDefault(true)
                val localFirst = settingsRepository.getLocalFirstModeEnabled()
                    .getOrDefault(true)
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

    private fun loadProviders() {
        viewModelScope.launch {
            aiProviderRepository.getAllProviders()
                .catch { e ->
                    android.util.Log.e("SettingsViewModel", "加载服务商列表失败", e)
                    _uiState.update {
                        it.copy(error = "加载服务商列表失败: ${e.message}")
                    }
                }
                .collect { providers ->
                    val providerNames = providers.map { it.name }
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

    private fun selectProvider(providerName: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

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

    private fun toggleDataMasking() {
        viewModelScope.launch {
            try {
                val newValue = !_uiState.value.dataMaskingEnabled
                
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

    private fun toggleLocalFirstMode() {
        viewModelScope.launch {
            try {
                val newValue = !_uiState.value.localFirstMode
                
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

    private fun showClearDataDialog() {
        _uiState.update { it.copy(showClearDataDialog = true) }
    }

    private fun hideClearDataDialog() {
        _uiState.update { it.copy(showClearDataDialog = false) }
    }

    private fun clearAllData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val providers = _uiState.value.providersList
                providers.forEach { provider ->
                    aiProviderRepository.deleteProvider(provider.id)
                }
                
                settingsRepository.setDataMaskingEnabled(true)
                settingsRepository.setLocalFirstModeEnabled(true)
                
                floatingWindowPreferencesRepository.saveEnabled(false)
                if (_uiState.value.floatingWindowEnabled) {
                    floatingWindowManager.stopService()
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

    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun checkFloatingWindowPermission() {
        if (isProcessingPermissionCheck) {
            android.util.Log.d("SettingsViewModel", "权限检查正在进行中，跳过")
            return
        }
        
        isProcessingPermissionCheck = true
        
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val permissionResult = floatingWindowManager.hasPermission()
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

    private fun toggleFloatingWindow() {
        if (isProcessingFloatingWindowToggle) {
            android.util.Log.d("SettingsViewModel", "悬浮窗切换正在进行中，跳过")
            return
        }
        
        val currentState = _uiState.value
        
        if (!currentState.floatingWindowEnabled) {
            if (!currentState.hasFloatingWindowPermission) {
                showPermissionDialog()
            } else {
                startFloatingWindowService()
            }
        } else {
            stopFloatingWindowService()
        }
    }

    private fun loadFloatingWindowState() {
        val state = floatingWindowPreferencesRepository.loadState()
        _uiState.update {
            it.copy(floatingWindowEnabled = state.isEnabled)
        }
        
        if (state.isEnabled) {
            viewModelScope.launch {
                val permissionResult = floatingWindowManager.hasPermission()
                if (permissionResult is FloatingWindowManager.PermissionResult.Granted) {
                    if (floatingWindowManager.isServiceRunning()) {
                        android.util.Log.d("SettingsViewModel", "悬浮窗服务已运行，跳过启动")
                        return@launch
                    }
                    
                    android.util.Log.d("SettingsViewModel", "检测到悬浮窗状态为启用，自动恢复服务")
                    startFloatingWindowService()
                } else {
                    android.util.Log.w("SettingsViewModel", "悬浮窗权限丢失，重置状态")
                    floatingWindowPreferencesRepository.saveEnabled(false)
                    _uiState.update { it.copy(floatingWindowEnabled = false) }
                }
            }
        }
    }

    private fun startFloatingWindowService() {
        isProcessingFloatingWindowToggle = true
        
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                val startResult = floatingWindowManager.startService()
                
                when (startResult) {
                    is FloatingWindowManager.ServiceStartResult.Success -> {
                        try {
                            floatingWindowPreferencesRepository.saveEnabled(true)
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

    private fun stopFloatingWindowService() {
        isProcessingFloatingWindowToggle = true
        
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                val stopResult = floatingWindowManager.stopService()
                
                when (stopResult) {
                    is FloatingWindowManager.ServiceStopResult.Success -> {
                        try {
                            floatingWindowPreferencesRepository.saveEnabled(false)
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
                        try {
                            floatingWindowPreferencesRepository.saveEnabled(false)
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

    private fun showPermissionDialog() {
        _uiState.update { it.copy(showPermissionDialog = true) }
    }

    private fun hidePermissionDialog() {
        _uiState.update { it.copy(showPermissionDialog = false) }
    }
    
    /**
     * 请求悬浮窗权限
     * 设置pendingPermissionRequest标志，UI层检测到后触发实际的权限请求Intent
     */
    private fun requestFloatingWindowPermission() {
        android.util.Log.d("SettingsViewModel", "触发悬浮窗权限请求")
        _uiState.update { it.copy(pendingPermissionRequest = true) }
    }
    
    /**
     * 清除权限请求标志
     * UI层处理完权限请求后调用
     */
    private fun clearPendingPermissionRequest() {
        _uiState.update { it.copy(pendingPermissionRequest = false) }
    }

}
