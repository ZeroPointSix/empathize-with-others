package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ApiUsageRecord
import com.empathy.ai.domain.repository.ApiUsageRepository
import javax.inject.Inject

/**
 * 导出 API 用量数据用例
 *
 * 支持导出用量数据为 JSON 格式
 *
 * TD-00025: AI配置功能完善 - 用量统计功能
 */
class ExportApiUsageUseCase @Inject constructor(
    private val apiUsageRepository: ApiUsageRepository
) {
    /**
     * 导出所有用量数据为 JSON 格式
     *
     * @return JSON 格式的用量数据
     */
    suspend operator fun invoke(): Result<String> {
        return try {
            val jsonData = apiUsageRepository.exportUsageData()
            Result.success(jsonData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取最近的用量记录
     *
     * @param limit 记录数量限制
     * @return 用量记录列表
     */
    suspend fun getRecentRecords(limit: Int = DEFAULT_EXPORT_LIMIT): Result<List<ApiUsageRecord>> {
        return try {
            val records = apiUsageRepository.getRecentRecords(limit)
            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 导出指定数量的最近记录为 JSON 格式
     *
     * @param limit 记录数量限制
     * @return JSON 格式的用量数据
     */
    suspend fun exportRecent(limit: Int = DEFAULT_EXPORT_LIMIT): Result<String> {
        return try {
            val records = apiUsageRepository.getRecentRecords(limit)
            val json = buildJsonFromRecords(records)
            Result.success(json)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 将记录列表转换为 JSON 格式
     */
    private fun buildJsonFromRecords(records: List<ApiUsageRecord>): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"exportTime\": ${System.currentTimeMillis()},\n")
        sb.append("  \"recordCount\": ${records.size},\n")
        sb.append("  \"records\": [\n")

        records.forEachIndexed { index, record ->
            sb.append("    {\n")
            sb.append("      \"id\": \"${record.id}\",\n")
            sb.append("      \"providerId\": \"${record.providerId}\",\n")
            sb.append("      \"providerName\": \"${escapeJson(record.providerName)}\",\n")
            sb.append("      \"modelId\": \"${record.modelId}\",\n")
            sb.append("      \"modelName\": \"${escapeJson(record.modelName)}\",\n")
            sb.append("      \"promptTokens\": ${record.promptTokens},\n")
            sb.append("      \"completionTokens\": ${record.completionTokens},\n")
            sb.append("      \"totalTokens\": ${record.totalTokens},\n")
            sb.append("      \"requestTimeMs\": ${record.requestTimeMs},\n")
            sb.append("      \"isSuccess\": ${record.isSuccess},\n")
            sb.append("      \"errorMessage\": ${record.errorMessage?.let { "\"${escapeJson(it)}\"" } ?: "null"},\n")
            sb.append("      \"createdAt\": ${record.createdAt}\n")
            sb.append("    }")
            if (index < records.size - 1) {
                sb.append(",")
            }
            sb.append("\n")
        }

        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }

    /**
     * 转义 JSON 特殊字符
     */
    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    companion object {
        /** 默认导出记录数量限制 */
        const val DEFAULT_EXPORT_LIMIT = 1000
    }
}
