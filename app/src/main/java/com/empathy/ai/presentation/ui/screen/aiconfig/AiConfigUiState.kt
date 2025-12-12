package com.empathy.ai.presentation.ui.screen.aiconfig

import com.empathy.ai.domain.model.AiProvider

/**
 * AI 配置界面的 UI 状态
 *
 * 设计原则：
 * 1. 所有字段都有默认值，避免空指针
 * 2. 使用 data class，自动获得 copy() 方法
 * 3. 状态是不可变的 (val)，通过 copy() 更新
 * 4. 包含所有 UI 信息：数据 + 加载状态 + 错误信息
 */
data class AiConfigUiState(
    // 通用状态字段
    val isLoading: Boolean = false,
    val error: String? = null,

    // 服务商列表
    val providers: List<AiProvider> = emptyList(),

    // 表单对话框状态
    val showFormDialog: Boolean = false,
    val editingProvider: AiProvider? = null,

    // 表单字段
    val formName: String = "",
    val formBaseUrl: String = "",
    val formApiKey: String = "",
    val formModels: List<FormModel> = emptyList(),
    val formDefaultModelId: String = "",

    // 表单验证错误
    val formNameError: String? = null,
    val formBaseUrlError: String? = null,
    val formApiKeyError: String? = null,
    val formModelsError: String? = null,

    // 保存状态
    val isSaving: Boolean = false,

    // 删除确认对话框
    val showDeleteConfirmDialog: Boolean = false,
    val deletingProviderId: String? = null,

    // 连接测试状态
    val isTestingConnection: Boolean = false,
    val testConnectionResult: TestConnectionResult? = null,

    // 导航状态
    val shouldNavigateBack: Boolean = false
) {
    /**
     * 计算属性：是否有服务商
     */
    val hasProviders: Boolean
        get() = providers.isNotEmpty()

    /**
     * 计算属性：表单是否有效
     */
    val isFormValid: Boolean
        get() = formName.isNotBlank() &&
                formBaseUrl.isNotBlank() &&
                formApiKey.isNotBlank() &&
                formModels.isNotEmpty() &&
                formModels.any { it.id == formDefaultModelId } &&
                formNameError == null &&
                formBaseUrlError == null &&
                formApiKeyError == null &&
                formModelsError == null

    /**
     * 计算属性：是否正在编辑
     */
    val isEditing: Boolean
        get() = editingProvider != null
}

/**
 * 表单模型数据
 */
data class FormModel(
    val id: String,
    val displayName: String = ""
)

/**
 * 连接测试结果
 */
sealed class TestConnectionResult {
    /**
     * 连接成功
     *
     * @param latencyMs 响应延迟（毫秒）
     */
    data class Success(val latencyMs: Long = 0) : TestConnectionResult()
    
    /**
     * 连接失败
     *
     * @param message 错误消息
     */
    data class Failure(val message: String) : TestConnectionResult()
}
