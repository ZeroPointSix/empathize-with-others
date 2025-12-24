package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.model.ConnectionTestResult
import com.empathy.ai.domain.repository.AiProviderRepository
import javax.inject.Inject

/**
 * 测试连接用例
 *
 * 测试 AI 服务商配置是否可用
 * 发送真实的 API 请求验证配置是否正确
 */
class TestConnectionUseCase @Inject constructor(
    private val repository: AiProviderRepository
) {
    /**
     * 执行测试操作
     *
     * @param provider 要测试的服务商
     * @return Result 包含详细的测试结果
     */
    suspend operator fun invoke(provider: AiProvider): Result<ConnectionTestResult> {
        // 基本验证
        if (!provider.isValid()) {
            return Result.success(
                ConnectionTestResult.failure(
                    ConnectionTestResult.ErrorType.UNKNOWN,
                    "服务商配置无效：请检查名称、API 端点、API Key 和模型列表"
                )
            )
        }
        
        // 调用仓库进行连接测试
        return repository.testConnection(provider)
    }
}
