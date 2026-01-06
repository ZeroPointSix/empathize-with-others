package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.repository.AiProviderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取服务商列表用例
 *
 * 负责提供所有已配置AI服务商的列表。
 *
 * 业务背景 (PRD-00002):
 * - 用户可以配置多个AI服务商（如OpenAI、DeepSeek等）
 * - 支持服务商切换和默认服务商设置
 *
 * 设计决策:
 * - 直接委托给Repository获取所有服务商
 * - 返回Flow支持实时更新
 *
 * @return Flow<List<AiProvider>> 服务商列表数据流
 * @see AiProviderRepository 服务商仓库接口
 */
class GetProvidersUseCase @Inject constructor(
    private val repository: AiProviderRepository
) {
    /**
     * 执行获取操作
     *
     * @return Flow 发射服务商列表
     */
    operator fun invoke(): Flow<List<AiProvider>> {
        return repository.getAllProviders()
    }
}
