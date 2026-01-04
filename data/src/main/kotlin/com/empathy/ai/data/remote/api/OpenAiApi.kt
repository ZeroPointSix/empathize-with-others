package com.empathy.ai.data.remote.api

import com.empathy.ai.data.remote.model.ChatRequestDto
import com.empathy.ai.data.remote.model.ChatResponseDto
import com.empathy.ai.data.remote.model.ModelsResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Package remote.api 实现了 OpenAI 兼容 API 接口
 *
 * 业务背景:
 *   - PRD: PRD-00026 [AI军师对话功能需求]
 *   - 用户故事: US-026-01 [AI军师独立对话界面]
 *
 * 设计决策 (TDD-00026):
 *   - 选择 @Url 动态路由而非多接口方案 → 灵活性高，用户可切换任意兼容服务商
 *   - 使用 @HeaderMap 注入鉴权 → 密钥管理集中在 Repository 层，API层无密钥暴露
 *
 * 任务追踪:
 *   - FD: FD-00026 [AI军师对话功能设计]
 *   - Task: T026-001 [AI API通信模块]
 */
interface OpenAiApi {

    /**
     * 聊天补全 API
     *
     * 【实现策略】挂起函数 + Retrofit 异步
     * - 使用 Kotlin 协程挂起函数，避免回调嵌套
     * - Retrofit 自动在 IO 线程执行，切换到主线程需调用方处理
     *
     * 【异常处理】Result 模式
     * - 网络错误、超时等会抛出异常
     * - 调用方应使用 Result<ChatResponseDto> 包装处理
     *
     * 【设计权衡】动态URL模式 vs 多接口方案
     * ┌─────────────────────────────────────────────────────────────────────┐
     * │ 方案A: @Url动态路由 (当前实现)          │ 方案B: 每个服务商独立接口   │
     * ├─────────────────────────────────────────────────────────────────────┤
     * │ ✅ 单一接口，代码简洁                    │ ❌ 接口数量随服务商增长     │
     * │ ✅ 运行时切换，无需重启                  │ ❌ 切换需要重新DI           │
     * │ ✅ 易于扩展新服务商                     │ ❌ 新增服务商需新增接口     │
     * │ ⚠️ URL拼接需调用方保证正确性            │ ✅ 接口定义保证正确性       │
     * └─────────────────────────────────────────────────────────────────────┘
     *
     * @param fullUrl 完整的 API URL (例如: "https://api.openai.com/v1/chat/completions")
     *                使用 @Url 注解覆盖 Retrofit 的 baseUrl
     *                【注意】URL必须包含完整路径，末尾无斜杠
     * @param headers 请求头映射,包含鉴权信息 (例如: {"Authorization": "Bearer xxx"})
     *                【安全】密钥通过 Repository 层注入，API 层不直接接触明文密钥
     * @param request 聊天请求体,包含模型、消息列表等参数
     *                【性能】请求体会被 Moshi 序列化，建议预先构建复用
     * @return AI 回复响应
     *         【时序】通常2-5秒返回，受模型和输入长度影响
     */
    @POST
    suspend fun chatCompletion(
        @Url fullUrl: String,
        @HeaderMap headers: Map<String, String>,
        @Body request: ChatRequestDto
    ): ChatResponseDto

    /**
     * 获取可用模型列表 API
     *
     * 【SR-00001】模型列表自动获取与调试日志优化
     * - 用户切换服务商后自动获取可用模型列表
     * - 用于 AI 配置界面的模型选择下拉框
     *
     * 【调用时机】
     * - 用户新增/编辑 AI 服务商时
     * - 进入 AI 配置页面时刷新列表
     *
     * @param fullUrl 完整的 API URL (例如: "https://api.openai.com/v1/models")
     *                使用 @Url 注解覆盖 Retrofit 的 baseUrl
     * @param headers 请求头映射,包含鉴权信息 (例如: {"Authorization": "Bearer xxx"})
     * @return 模型列表响应
     *         【筛选】调用方需过滤不兼容的模型
     */
    @GET
    suspend fun listModels(
        @Url fullUrl: String,
        @HeaderMap headers: Map<String, String>
    ): ModelsResponseDto
}
