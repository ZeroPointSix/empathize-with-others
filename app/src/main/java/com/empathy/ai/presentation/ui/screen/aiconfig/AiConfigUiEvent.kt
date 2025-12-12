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

    // === 连接测试事件 ===

    /**
     * 测试连接
     */
    data object TestConnection : AiConfigUiEvent

    /**
     * 清除测试结果
     */
    data object ClearTestResult : AiConfigUiEvent

    // === 通用事件 ===

    /**
     * 清除错误
     */
    data object ClearError : AiConfigUiEvent

    /**
     * 导航返回
     */
    data object NavigateBack : AiConfigUiEvent
}
