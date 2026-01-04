package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ProxyConfig
import com.empathy.ai.domain.repository.AiProviderRepository
import javax.inject.Inject

/**
 * 测试代理连接用例
 *
 * 验证代理配置是否有效，并返回连接延迟。
 *
 * 业务背景:
 *   - TD-00025: AI配置功能完善 - 网络代理功能
 *   - 场景: 用户配置代理后，测试代理连接是否正常
 *
 * 核心功能:
 *   1. 测试代理连接: invoke(config) 执行实际连接测试
 *   2. 验证配置格式: validateConfig(config) 仅验证不连接
 *   3. 返回延迟等级: LatencyLevel 评估连接质量
 *
 * 延迟等级评估:
 *   - EXCELLENT (<100ms): 优秀，适合实时对话
 *   - GOOD (100-300ms): 良好，可能有轻微延迟
 *   - FAIR (300-1000ms): 一般，勉强可用
 *   - POOR (>1000ms): 较差，影响体验
 *   - FAILED: 连接失败
 *
 * @see ProxyConfig 代理配置模型
 * @see ProxyTestResult 代理测试结果
 */
class TestProxyConnectionUseCase @Inject constructor(
    private val aiProviderRepository: AiProviderRepository
) {
    /**
     * 测试代理连接
     *
     * @param config 要测试的代理配置
     * @return Result 包含测试结果
     */
    suspend operator fun invoke(config: ProxyConfig): Result<ProxyTestResult> {
        // 验证配置有效性
        if (!config.enabled) {
            return Result.failure(IllegalArgumentException("代理未启用"))
        }

        if (!config.isValid()) {
            return Result.failure(IllegalArgumentException("代理配置无效：请检查服务器地址和端口"))
        }

        return try {
            val startTime = System.currentTimeMillis()
            val result = aiProviderRepository.testProxyConnection(config)
            val endTime = System.currentTimeMillis()

            result.fold(
                onSuccess = { latencyMs ->
                    Result.success(
                        ProxyTestResult(
                            success = true,
                            latencyMs = latencyMs,
                            totalTimeMs = endTime - startTime,
                            errorMessage = null
                        )
                    )
                },
                onFailure = { error ->
                    Result.success(
                        ProxyTestResult(
                            success = false,
                            latencyMs = 0,
                            totalTimeMs = endTime - startTime,
                            errorMessage = error.message ?: "连接失败"
                        )
                    )
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 验证代理配置格式
     *
     * @param config 代理配置
     * @return 验证结果
     */
    fun validateConfig(config: ProxyConfig): ProxyValidationResult {
        val errors = mutableListOf<String>()

        if (config.enabled) {
            if (config.host.isBlank()) {
                errors.add("服务器地址不能为空")
            }

            if (config.port !in ProxyConfig.PORT_MIN..ProxyConfig.PORT_MAX) {
                errors.add("端口号必须在 ${ProxyConfig.PORT_MIN}-${ProxyConfig.PORT_MAX} 之间")
            }

            // 检查认证信息完整性
            if (config.username.isNotBlank() && config.password.isBlank()) {
                errors.add("已填写用户名，请同时填写密码")
            }
            if (config.password.isNotBlank() && config.username.isBlank()) {
                errors.add("已填写密码，请同时填写用户名")
            }
        }

        return ProxyValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
}

/**
 * 代理测试结果
 *
 * @property success 测试是否成功
 * @property latencyMs 连接延迟（毫秒）
 * @property totalTimeMs 总测试时间（毫秒）
 * @property errorMessage 错误信息（失败时）
 */
data class ProxyTestResult(
    val success: Boolean,
    val latencyMs: Long,
    val totalTimeMs: Long,
    val errorMessage: String?
) {
    /**
     * 获取格式化的延迟显示
     */
    fun formatLatency(): String {
        return when {
            latencyMs < 1000 -> "${latencyMs}ms"
            else -> String.format("%.1fs", latencyMs / 1000.0)
        }
    }

    /**
     * 获取延迟等级
     */
    fun getLatencyLevel(): LatencyLevel {
        return when {
            !success -> LatencyLevel.FAILED
            latencyMs < 100 -> LatencyLevel.EXCELLENT
            latencyMs < 300 -> LatencyLevel.GOOD
            latencyMs < 1000 -> LatencyLevel.FAIR
            else -> LatencyLevel.POOR
        }
    }
}

/**
 * 延迟等级
 */
enum class LatencyLevel {
    /** 优秀 (<100ms) */
    EXCELLENT,
    /** 良好 (100-300ms) */
    GOOD,
    /** 一般 (300-1000ms) */
    FAIR,
    /** 较差 (>1000ms) */
    POOR,
    /** 失败 */
    FAILED
}

/**
 * 代理配置验证结果
 *
 * @property isValid 配置是否有效
 * @property errors 错误信息列表
 */
data class ProxyValidationResult(
    val isValid: Boolean,
    val errors: List<String>
) {
    /**
     * 获取第一个错误信息
     */
    fun getFirstError(): String? = errors.firstOrNull()

    /**
     * 获取所有错误信息（换行分隔）
     */
    fun getAllErrors(): String = errors.joinToString("\n")
}
