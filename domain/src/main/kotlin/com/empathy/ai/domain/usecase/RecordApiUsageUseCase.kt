package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ApiUsageRecord
import com.empathy.ai.domain.repository.ApiUsageRepository
import java.util.UUID
import javax.inject.Inject

/**
 * 记录 API 用量用例
 *
 * 记录单次 API 调用的用量信息
 *
 * TD-00025: AI配置功能完善 - 用量统计功能
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
