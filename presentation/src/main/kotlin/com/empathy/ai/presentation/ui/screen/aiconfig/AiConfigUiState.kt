package com.empathy.ai.presentation.ui.screen.aiconfig

import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.model.ProxyConfig
import com.empathy.ai.domain.model.ProxyType

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

    // 搜索相关 (TD-00021 T1-03)
    val searchQuery: String = "",

    // 高级设置 (TD-00021 T1-03)
    val requestTimeout: Int = 30,
    val maxTokens: Int = 4096,

    // 表单对话框状态
    val showFormDialog: Boolean = false,
    val editingProvider: AiProvider? = null,

    // 表单字段
    val formName: String = "",
    val formBaseUrl: String = "",
    val formApiKey: String = "",
    val formModels: List<FormModel> = emptyList(),
    val formDefaultModelId: String = "",

    // TD-00025: 高级选项表单字段
    val formTemperature: Float = 0.7f,
    val formMaxTokens: Int = 4096,
    
    // BUG-00054 第二轮修复：表单超时设置（毫秒）
    val formTimeoutMs: Long = 30000L,

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

    // 模型列表获取状态
    // @see SR-00001 模型列表自动获取与调试日志优化
    val isFetchingModels: Boolean = false,
    val fetchModelsError: String? = null,

    // 导航状态
    val shouldNavigateBack: Boolean = false,

    // ==================== TD-00025: 代理配置状态 ====================

    // 代理设置对话框状态
    val showProxyDialog: Boolean = false,

    // 代理配置表单字段
    val proxyEnabled: Boolean = false,
    val proxyType: ProxyType = ProxyType.HTTP,
    val proxyHost: String = "",
    val proxyPort: Int = 0,
    val proxyUsername: String = "",
    val proxyPassword: String = "",

    // 代理测试状态
    val isTestingProxy: Boolean = false,
    val proxyTestResult: ProxyTestResult? = null,

    // 代理配置验证错误
    val proxyHostError: String? = null,
    val proxyPortError: String? = null,

    // TD-00025: 当前代理配置（用于显示）
    val proxyConfig: ProxyConfig? = null,

    // TD-00025: 用量统计导航状态
    val shouldNavigateToUsageStats: Boolean = false
) {
    /**
     * 计算属性：是否有服务商
     */
    val hasProviders: Boolean
        get() = providers.isNotEmpty()

    /**
     * 计算属性：过滤后的服务商列表 (TD-00021 T1-03)
     * 根据搜索关键词过滤服务商
     */
    val filteredProviders: List<AiProvider>
        get() = if (searchQuery.isBlank()) {
            providers
        } else {
            providers.filter { provider ->
                provider.name.contains(searchQuery, ignoreCase = true) ||
                provider.baseUrl.contains(searchQuery, ignoreCase = true)
            }
        }

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

    /**
     * TD-00025: 计算属性：代理配置是否有效
     */
    val isProxyConfigValid: Boolean
        get() = !proxyEnabled || (
            proxyHost.isNotBlank() &&
            proxyPort in 1..65535 &&
            proxyHostError == null &&
            proxyPortError == null
        )

    /**
     * TD-00025: 获取当前代理配置
     */
    fun toProxyConfig(): ProxyConfig = ProxyConfig(
        enabled = proxyEnabled,
        type = proxyType,
        host = proxyHost,
        port = proxyPort,
        username = proxyUsername,
        password = proxyPassword
    )
}

/**
 * 表单模型数据
 */
data class FormModel(
    val id: String,
    val displayName: String = "",
    val supportsImage: Boolean = false
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

/**
 * TD-00025: 代理测试结果
 */
sealed class ProxyTestResult {
    /**
     * 测试成功
     *
     * @param latencyMs 响应延迟（毫秒）
     */
    data class Success(val latencyMs: Long) : ProxyTestResult()

    /**
     * 测试失败
     *
     * @param message 错误消息
     */
    data class Failure(val message: String) : ProxyTestResult()
}
