package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.AiModel
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.usecase.DeleteProviderUseCase
import com.empathy.ai.domain.usecase.GetProvidersUseCase
import com.empathy.ai.domain.usecase.SaveProviderUseCase
import com.empathy.ai.domain.usecase.TestConnectionUseCase
import com.empathy.ai.presentation.ui.screen.aiconfig.AiConfigUiEvent
import com.empathy.ai.presentation.ui.screen.aiconfig.AiConfigUiState
import com.empathy.ai.presentation.ui.screen.aiconfig.FormModel
import com.empathy.ai.presentation.ui.screen.aiconfig.TestConnectionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * AI 配置界面的 ViewModel
 *
 * 职责：
 * 1. 管理 AI 配置界面的 UI 状态
 * 2. 处理用户交互事件
 * 3. 调用 UseCase 执行业务逻辑
 * 4. 异常处理和状态更新
 */
@HiltViewModel
class AiConfigViewModel @Inject constructor(
    private val getProvidersUseCase: GetProvidersUseCase,
    private val saveProviderUseCase: SaveProviderUseCase,
    private val deleteProviderUseCase: DeleteProviderUseCase,
    private val testConnectionUseCase: TestConnectionUseCase,
    private val aiProviderRepository: com.empathy.ai.domain.repository.AiProviderRepository
) : BaseViewModel() {

    // 私有可变状态（只能内部修改）
    private val _uiState = MutableStateFlow(AiConfigUiState())

    // 公开不可变状态（外部只读）
    val uiState: StateFlow<AiConfigUiState> = _uiState.asStateFlow()

    init {
        // 初始化时加载服务商列表
        loadProviders()
    }

    /**
     * 统一的事件处理入口
     */
    fun onEvent(event: AiConfigUiEvent) {
        when (event) {
            // === 数据加载事件 ===
            is AiConfigUiEvent.LoadProviders -> loadProviders()
            is AiConfigUiEvent.RefreshProviders -> refreshProviders()

            // === 表单对话框事件 ===
            is AiConfigUiEvent.ShowAddDialog -> showAddDialog()
            is AiConfigUiEvent.ShowEditDialog -> showEditDialog(event.provider)
            is AiConfigUiEvent.DismissFormDialog -> dismissFormDialog()

            // === 表单字段更新事件 ===
            is AiConfigUiEvent.UpdateFormName -> updateFormName(event.name)
            is AiConfigUiEvent.UpdateFormBaseUrl -> updateFormBaseUrl(event.baseUrl)
            is AiConfigUiEvent.UpdateFormApiKey -> updateFormApiKey(event.apiKey)
            is AiConfigUiEvent.AddFormModel -> addFormModel(event.modelId, event.displayName)
            is AiConfigUiEvent.RemoveFormModel -> removeFormModel(event.modelId)
            is AiConfigUiEvent.SetFormDefaultModel -> setFormDefaultModel(event.modelId)

            // === 服务商操作事件 ===
            is AiConfigUiEvent.SaveProvider -> saveProvider()
            is AiConfigUiEvent.ShowDeleteConfirmDialog -> showDeleteConfirmDialog(event.providerId)
            is AiConfigUiEvent.DismissDeleteConfirmDialog -> dismissDeleteConfirmDialog()
            is AiConfigUiEvent.ConfirmDeleteProvider -> confirmDeleteProvider()
            is AiConfigUiEvent.SetDefaultProvider -> setDefaultProvider(event.providerId)

            // === 连接测试事件 ===
            is AiConfigUiEvent.TestConnection -> testConnection()
            is AiConfigUiEvent.ClearTestResult -> clearTestResult()

            // === 模型列表获取事件 ===
            is AiConfigUiEvent.FetchModels -> fetchModels()
            is AiConfigUiEvent.ClearFetchModelsError -> clearFetchModelsError()

            // === 通用事件 ===
            is AiConfigUiEvent.ClearError -> clearError()
            is AiConfigUiEvent.NavigateBack -> navigateBack()
        }
    }

    /**
     * 加载服务商列表
     */
    private fun loadProviders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            getProvidersUseCase()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "加载服务商列表失败"
                        )
                    }
                }
                .collect { providers ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            providers = providers,
                            error = null
                        )
                    }
                }
        }
    }

    /**
     * 刷新服务商列表
     */
    private fun refreshProviders() {
        loadProviders()
    }

    /**
     * 显示添加服务商对话框
     */
    private fun showAddDialog() {
        _uiState.update {
            it.copy(
                showFormDialog = true,
                editingProvider = null,
                formName = "",
                formBaseUrl = "",
                formApiKey = "",
                formModels = emptyList(),
                formDefaultModelId = "",
                formNameError = null,
                formBaseUrlError = null,
                formApiKeyError = null,
                formModelsError = null,
                testConnectionResult = null
            )
        }
    }

    /**
     * 显示编辑服务商对话框
     */
    private fun showEditDialog(provider: AiProvider) {
        _uiState.update {
            it.copy(
                showFormDialog = true,
                editingProvider = provider,
                formName = provider.name,
                formBaseUrl = provider.baseUrl,
                formApiKey = provider.apiKey,
                formModels = provider.models.map { model ->
                    FormModel(id = model.id, displayName = model.displayName ?: "")
                },
                formDefaultModelId = provider.defaultModelId,
                formNameError = null,
                formBaseUrlError = null,
                formApiKeyError = null,
                formModelsError = null,
                testConnectionResult = null
            )
        }
    }

    /**
     * 关闭表单对话框
     */
    private fun dismissFormDialog() {
        _uiState.update {
            it.copy(
                showFormDialog = false,
                editingProvider = null,
                testConnectionResult = null
            )
        }
    }

    /**
     * 更新服务商名称
     */
    private fun updateFormName(name: String) {
        _uiState.update {
            it.copy(
                formName = name,
                formNameError = validateName(name)
            )
        }
    }

    /**
     * 更新 API 端点
     */
    private fun updateFormBaseUrl(baseUrl: String) {
        _uiState.update {
            it.copy(
                formBaseUrl = baseUrl,
                formBaseUrlError = validateBaseUrl(baseUrl)
            )
        }
    }

    /**
     * 更新 API Key
     */
    private fun updateFormApiKey(apiKey: String) {
        _uiState.update {
            it.copy(
                formApiKey = apiKey,
                formApiKeyError = validateApiKey(apiKey)
            )
        }
    }

    /**
     * 添加模型
     */
    private fun addFormModel(modelId: String, displayName: String) {
        if (modelId.isBlank()) {
            _uiState.update { it.copy(formModelsError = "模型 ID 不能为空") }
            return
        }

        val currentState = _uiState.value
        
        // 检查是否已存在
        if (currentState.formModels.any { it.id == modelId }) {
            _uiState.update { it.copy(formModelsError = "模型 ID 已存在") }
            return
        }

        val newModel = FormModel(id = modelId, displayName = displayName)
        val updatedModels = currentState.formModels + newModel

        _uiState.update {
            it.copy(
                formModels = updatedModels,
                formModelsError = null,
                // 如果是第一个模型，自动设置为默认
                formDefaultModelId = if (currentState.formModels.isEmpty()) modelId else currentState.formDefaultModelId
            )
        }
    }

    /**
     * 删除模型
     */
    private fun removeFormModel(modelId: String) {
        val currentState = _uiState.value
        val updatedModels = currentState.formModels.filter { it.id != modelId }

        _uiState.update {
            it.copy(
                formModels = updatedModels,
                // 如果删除的是默认模型，清空默认模型 ID
                formDefaultModelId = if (currentState.formDefaultModelId == modelId) {
                    updatedModels.firstOrNull()?.id ?: ""
                } else {
                    currentState.formDefaultModelId
                },
                formModelsError = if (updatedModels.isEmpty()) "至少需要一个模型" else null
            )
        }
    }

    /**
     * 设置默认模型
     */
    private fun setFormDefaultModel(modelId: String) {
        _uiState.update { it.copy(formDefaultModelId = modelId) }
    }

    /**
     * 保存服务商
     */
    private fun saveProvider() {
        val currentState = _uiState.value

        // 验证表单
        val nameError = validateName(currentState.formName)
        val baseUrlError = validateBaseUrl(currentState.formBaseUrl)
        val apiKeyError = validateApiKey(currentState.formApiKey)
        val modelsError = validateModels(currentState.formModels, currentState.formDefaultModelId)

        if (nameError != null || baseUrlError != null || apiKeyError != null || modelsError != null) {
            _uiState.update {
                it.copy(
                    formNameError = nameError,
                    formBaseUrlError = baseUrlError,
                    formApiKeyError = apiKeyError,
                    formModelsError = modelsError
                )
            }
            return
        }

        // 构建 AiProvider 对象
        val provider = AiProvider(
            id = currentState.editingProvider?.id ?: UUID.randomUUID().toString(),
            name = currentState.formName.trim(),
            baseUrl = currentState.formBaseUrl.trim(),
            apiKey = currentState.formApiKey.trim(),
            models = currentState.formModels.map { formModel ->
                AiModel(
                    id = formModel.id,
                    displayName = formModel.displayName.ifBlank { null }
                )
            },
            defaultModelId = currentState.formDefaultModelId,
            isDefault = currentState.editingProvider?.isDefault ?: false,
            createdAt = currentState.editingProvider?.createdAt ?: System.currentTimeMillis()
        )

        // 保存
        performOperation(
            operation = { saveProviderUseCase(provider) },
            onSuccess = {
                _uiState.update {
                    it.copy(
                        showFormDialog = false,
                        editingProvider = null,
                        isSaving = false
                    )
                }
            },
            onError = { errorMessage ->
                _uiState.update {
                    it.copy(
                        error = errorMessage,
                        isSaving = false
                    )
                }
            }
        )

        _uiState.update { it.copy(isSaving = true) }
    }

    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmDialog(providerId: String) {
        _uiState.update {
            it.copy(
                showDeleteConfirmDialog = true,
                deletingProviderId = providerId
            )
        }
    }

    /**
     * 关闭删除确认对话框
     */
    private fun dismissDeleteConfirmDialog() {
        _uiState.update {
            it.copy(
                showDeleteConfirmDialog = false,
                deletingProviderId = null
            )
        }
    }

    /**
     * 确认删除服务商
     */
    private fun confirmDeleteProvider() {
        val providerId = _uiState.value.deletingProviderId ?: return

        performOperation(
            operation = { deleteProviderUseCase(providerId) },
            onSuccess = {
                _uiState.update {
                    it.copy(
                        showDeleteConfirmDialog = false,
                        deletingProviderId = null
                    )
                }
            },
            onError = { errorMessage ->
                _uiState.update {
                    it.copy(
                        error = errorMessage,
                        showDeleteConfirmDialog = false,
                        deletingProviderId = null
                    )
                }
            }
        )
    }

    /**
     * 设置默认服务商
     */
    private fun setDefaultProvider(providerId: String) {
        viewModelScope.launch {
            // 获取当前服务商列表
            val currentProviders = _uiState.value.providers

            // 更新所有服务商的 isDefault 状态
            currentProviders.forEach { provider ->
                val updatedProvider = provider.copy(isDefault = provider.id == providerId)
                saveProviderUseCase(updatedProvider)
            }
        }
    }

    /**
     * 测试连接
     *
     * 发送真实的 API 请求验证配置是否正确
     */
    private fun testConnection() {
        val currentState = _uiState.value

        // 构建临时 Provider 用于测试
        val testProvider = AiProvider(
            id = UUID.randomUUID().toString(),
            name = currentState.formName.trim(),
            baseUrl = currentState.formBaseUrl.trim(),
            apiKey = currentState.formApiKey.trim(),
            models = currentState.formModels.map { formModel ->
                AiModel(
                    id = formModel.id,
                    displayName = formModel.displayName.ifBlank { null }
                )
            },
            defaultModelId = currentState.formDefaultModelId
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, testConnectionResult = null) }

            val result = testConnectionUseCase(testProvider)

            result.onSuccess { connectionResult ->
                _uiState.update {
                    it.copy(
                        isTestingConnection = false,
                        testConnectionResult = if (connectionResult.isSuccess) {
                            TestConnectionResult.Success(connectionResult.latencyMs)
                        } else {
                            TestConnectionResult.Failure(connectionResult.getUserFriendlyMessage())
                        }
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isTestingConnection = false,
                        testConnectionResult = TestConnectionResult.Failure(
                            error.message ?: "连接测试失败"
                        )
                    )
                }
            }
        }
    }

    /**
     * 清除测试结果
     */
    private fun clearTestResult() {
        _uiState.update { it.copy(testConnectionResult = null) }
    }

    /**
     * 获取可用模型列表
     *
     * 从服务商 API 自动获取可用的模型列表
     * 需要先填写 API 端点和 API Key
     *
     * @see SR-00001 模型列表自动获取与调试日志优化
     */
    private fun fetchModels() {
        val currentState = _uiState.value

        // 验证必要字段
        if (currentState.formBaseUrl.isBlank()) {
            _uiState.update { it.copy(fetchModelsError = "请先填写 API 端点") }
            return
        }
        if (currentState.formApiKey.isBlank()) {
            _uiState.update { it.copy(fetchModelsError = "请先填写 API Key") }
            return
        }

        // 构建临时 Provider 用于获取模型
        val tempProvider = AiProvider(
            id = UUID.randomUUID().toString(),
            name = currentState.formName.ifBlank { "临时服务商" },
            baseUrl = currentState.formBaseUrl.trim(),
            apiKey = currentState.formApiKey.trim(),
            models = emptyList(),
            defaultModelId = ""
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingModels = true, fetchModelsError = null) }

            val result = aiProviderRepository.fetchAvailableModels(tempProvider)

            result.onSuccess { models ->
                if (models.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isFetchingModels = false,
                            fetchModelsError = "未找到可用的聊天模型"
                        )
                    }
                    return@launch
                }

                // 将获取到的模型添加到表单
                val newFormModels = models.map { model: AiModel ->
                    FormModel(
                        id = model.id,
                        displayName = model.displayName ?: ""
                    )
                }

                // 合并现有模型（保留用户手动添加的）
                val existingIds = currentState.formModels.map { it.id }.toSet()
                val mergedModels = currentState.formModels + newFormModels.filter { it.id !in existingIds }

                // 如果没有默认模型，设置第一个为默认
                val defaultModelId = if (currentState.formDefaultModelId.isBlank() || 
                    mergedModels.none { it.id == currentState.formDefaultModelId }) {
                    mergedModels.firstOrNull()?.id ?: ""
                } else {
                    currentState.formDefaultModelId
                }

                _uiState.update {
                    it.copy(
                        isFetchingModels = false,
                        fetchModelsError = null,
                        formModels = mergedModels,
                        formDefaultModelId = defaultModelId,
                        formModelsError = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isFetchingModels = false,
                        fetchModelsError = error.message ?: "获取模型列表失败"
                    )
                }
            }
        }
    }

    /**
     * 清除获取模型错误
     */
    private fun clearFetchModelsError() {
        _uiState.update { it.copy(fetchModelsError = null) }
    }

    /**
     * 清除错误
     */
    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * 导航返回
     */
    private fun navigateBack() {
        _uiState.update { it.copy(shouldNavigateBack = true) }
    }

    // === 验证方法 ===

    /**
     * 验证服务商名称
     */
    private fun validateName(name: String): String? {
        return when {
            name.isBlank() -> "请输入服务商名称"
            name.length > 50 -> "名称不能超过 50 个字符"
            else -> null
        }
    }

    /**
     * 验证 API 端点
     */
    private fun validateBaseUrl(baseUrl: String): String? {
        return when {
            baseUrl.isBlank() -> "请输入 API 端点"
            !baseUrl.startsWith("http://") && !baseUrl.startsWith("https://") -> "URL 必须以 http:// 或 https:// 开头"
            else -> null
        }
    }

    /**
     * 验证 API Key
     */
    private fun validateApiKey(apiKey: String): String? {
        return when {
            apiKey.isBlank() -> "请输入 API Key"
            apiKey.length < 8 -> "API Key 长度不能少于 8 个字符"
            else -> null
        }
    }

    /**
     * 验证模型列表
     */
    private fun validateModels(models: List<FormModel>, defaultModelId: String): String? {
        return when {
            models.isEmpty() -> "请至少添加一个模型"
            defaultModelId.isBlank() -> "请选择默认模型"
            models.none { it.id == defaultModelId } -> "默认模型不在模型列表中"
            else -> null
        }
    }
}
