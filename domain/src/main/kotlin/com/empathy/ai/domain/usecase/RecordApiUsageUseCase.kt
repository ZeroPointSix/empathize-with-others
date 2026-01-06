package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ApiUsageRecord
import com.empathy.ai.domain.repository.ApiUsageRepository
import java.util.UUID
import javax.inject.Inject

/**
 * 记录 API 用量用例
 *
 * 记录单次 API 调用的用量信息，用于用量统计和成本分析。
 *
 * 业务背景 (TD-00025):
 * - 追踪AI API调用次数和Token消耗
 * - 区分成功/失败记录，便于问题排查
 * - 支持按服务商、模型维度统计分析
 *
 * 设计决策:
 * - 成功和失败记录分开方法，便于调用方区分处理
 * - 自动计算总Token数（prompt + completion）
 * - 使用UUID生成唯一ID，支持分布式环境
 *
 * @param providerId 服务商 ID
 * @param providerName 服务商名称
 * @param modelId 模型 ID
 * @param modelName 模型名称
 * @param promptTokens 输入 Token 数
 * @param completionTokens 输出 Token 数
 * @param requestTimeMs 请求耗时（毫秒）
 * @see ApiUsageRecord API用量记录模型
 * @see ApiUsageRepository 用量仓库接口
 */
class RecordApiUsageUseCase @Inject constructor(
    private val apiUsageRepository: ApiUsageRepository
) {
    /**
     * 记录成功的 API 调用
     *
     * @param providerId 服务商 ID
     * @param providerName 服务商名称
     * @param modelId 模型 ID
     * @param modelName 模型名称
     * @param promptTokens 输入 Token 数
     * @param completionTokens 输出 Token 数
     * @param requestTimeMs 请求耗时（毫秒）
     */
    suspend fun recordSuccess(
        providerId: String,
        providerName: String,
        modelId: String,
        modelName: String,
        promptTokens: Int = 0,
        completionTokens: Int = 0,
        requestTimeMs: Long = 0
    ) {
        val record = ApiUsageRecord(
            id = UUID.randomUUID().toString(),
            providerId = providerId,
            providerName = providerName,
            modelId = modelId,
            modelName = modelName,
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            totalTokens = promptTokens + completionTokens,
            requestTimeMs = requestTimeMs,
            isSuccess = true,
            errorMessage = null,
            createdAt = System.currentTimeMillis()
        )
        apiUsageRepository.recordUsage(record)
    }

    /**
     * 记录失败的 API 调用
     *
     * @param providerId 服务商 ID
     * @param providerName 服务商名称
     * @param modelId 模型 ID
     * @param modelName 模型名称
     * @param errorMessage 错误信息
     * @param requestTimeMs 请求耗时（毫秒）
     */
    suspend fun recordFailure(
        providerId: String,
        providerName: String,
        modelId: String,
        modelName: String,
        errorMessage: String,
        requestTimeMs: Long = 0
    ) {
        val record = ApiUsageRecord(
            id = UUID.randomUUID().toString(),
            providerId = providerId,
            providerName = providerName,
            modelId = modelId,
            modelName = modelName,
            promptTokens = 0,
            completionTokens = 0,
            totalTokens = 0,
            requestTimeMs = requestTimeMs,
            isSuccess = false,
            errorMessage = errorMessage,
            createdAt = System.currentTimeMillis()
        )
        apiUsageRepository.recordUsage(record)
    }
}
