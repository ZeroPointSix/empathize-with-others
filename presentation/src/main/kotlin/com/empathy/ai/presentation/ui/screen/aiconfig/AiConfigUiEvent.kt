package com.empathy.ai.presentation.ui.screen.aiconfig

/**
 * AI 配置界面的用户事件
 *
 * 设计原则：
 * 1. 使用 sealed interface 定义，编译时类型安全
 * 2. 每个事件都是独立的类型
 * 3. 有参数用 data class，无参数用 data object
 * 4. 事件命名使用动词开头，描述用户意图
 */
sealed interface AiConfigUiEvent {
    // === 数据加载事件 ===

    /**
     * 加载服务商列表
     */
    data object LoadProviders : AiConfigUiEvent

    /**
     * 刷新服务商列表
     */
    data object RefreshProviders : AiConfigUiEvent

    // === 表单对话框事件 ===

    /**
     * 显示添加服务商对话框
     */
    data object ShowAddDialog : AiConfigUiEvent

    /**
     * 显示编辑服务商对话框
     * @param provider 要编辑的服务商
     */
    data class ShowEditDialog(val provider: com.empathy.ai.domain.model.AiProvider) : AiConfigUiEvent

    /**
     * 关闭表单对话框
     */
    data object DismissFormDialog : AiConfigUiEvent

    // === 表单字段更新事件 ===

    /**
     * 更新服务商名称
     * @param name 新的名称
     */
    data class UpdateFormName(val name: String) : AiConfigUiEvent

    /**
     * 更新 API 端点
     * @param baseUrl 新的 API 端点
     */
    data class UpdateFormBaseUrl(val baseUrl: String) : AiConfigUiEvent

    /**
     * 更新 API Key
     * @param apiKey 新的 API Key
     */
    data class UpdateFormApiKey(val apiKey: String) : AiConfigUiEvent

    /**
     * 添加模型
     * @param modelId 模型 ID
     * @param displayName 显示名称（可选）
     */
    data class AddFormModel(val modelId: String, val displayName: String = "") : AiConfigUiEvent

    /**
     * 删除模型
     * @param modelId 模型 ID
     */
    data class RemoveFormModel(val modelId: String) : AiConfigUiEvent

    /**
     * 设置默认模型
     * @param modelId 模型 ID
     */
    data class SetFormDefaultModel(val modelId: String) : AiConfigUiEvent

    // === 服务商操作事件 ===

    /**
     * 保存服务商
     */
    data object SaveProvider : AiConfigUiEvent

    /**
     * 显示删除确认对话框
     * @param providerId 服务商 ID
     */
    data class ShowDeleteConfirmDialog(val providerId: String) : AiConfigUiEvent

    /**
     * 关闭删除确认对话框
     */
    data object DismissDeleteConfirmDialog : AiConfigUiEvent

    /**
     * 确认删除服务商
     */
    data object ConfirmDeleteProvider : AiConfigUiEvent

    /**
     * 设置默认服务商
     * @param providerId 服务商 ID
     */
    data class SetDefaultProvider(val providerId: String) : AiConfigUiEvent

    /**
     * 导航到编辑服务商页面
     * @param providerId 服务商 ID
     */
    data class NavigateToEditProvider(val providerId: String) : AiConfigUiEvent

    /**
     * 导航到服务商详情页面（查看和切换模型）
     * @param providerId 服务商 ID
     */
    data class NavigateToProviderDetail(val providerId: String) : AiConfigUiEvent

    /**
     * 通过ID加载服务商进行编辑
     * @param providerId 服务商 ID
     */
    data class LoadProviderForEdit(val providerId: String) : AiConfigUiEvent

    // === 连接测试事件 ===

    /**
     * 测试连接
     */
    data object TestConnection : AiConfigUiEvent

    /**
     * 清除测试结果
     */
    data object ClearTestResult : AiConfigUiEvent

    // === 模型列表获取事件 ===

    /**
     * 获取可用模型列表
     *
     * 从服务商 API 自动获取可用的模型列表
     *
     * @see SR-00001 模型列表自动获取与调试日志优化
     */
    data object FetchModels : AiConfigUiEvent

    /**
     * 清除获取模型错误
     */
    data object ClearFetchModelsError : AiConfigUiEvent

    // === 通用事件 ===

    /**
     * 清除错误
     */
    data object ClearError : AiConfigUiEvent

    /**
     * 导航返回
     */
    data object NavigateBack : AiConfigUiEvent

    // === 搜索事件 (TD-00021 T1-04) ===

    /**
     * 更新搜索关键词
     * @param query 搜索关键词
     */
    data class UpdateSearchQuery(val query: String) : AiConfigUiEvent

    // === 高级设置事件 (TD-00021 T1-04) ===

    /**
     * 更新请求超时时间
     * @param timeout 超时时间（秒）
     */
    data class UpdateRequestTimeout(val timeout: Int) : AiConfigUiEvent

    /**
     * 更新最大Token数
     * @param tokens 最大Token数
     */
    data class UpdateMaxTokens(val tokens: Int) : AiConfigUiEvent
}
