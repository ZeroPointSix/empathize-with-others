package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.model.ConnectionTestResult
import com.empathy.ai.domain.repository.AiProviderRepository
import javax.inject.Inject

/**
 * 测试连接用例
 *
 * 测试 AI 服务商配置是否可用。
 * 发送真实的 API 请求验证配置是否正确。
 *
 * 业务背景:
 *   - TD-00025: AI配置功能完善
 *   - 场景: 用户新增或编辑服务商后，测试配置是否有效
 *
 * 测试流程:
 *   1. 基本验证: 调用 provider.isValid() 检查配置完整性
 *   2. 发送测试请求: 调用 repository.testConnection() 执行真实API调用
 *   3. 返回详细结果: ConnectionTestResult 包含成功/失败状态和详细信息
 *
 * 设计权衡:
 *   - 为什么先 isValid() 再测试? 避免无效配置发起无效请求
 *   - 为什么返回 ConnectionTestResult 而非 Boolean? 便于UI展示具体错误类型
 *
 * @see ConnectionTestResult 连接测试结果
 * @see AiProviderRepository.testConnection 仓库测试方法
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
