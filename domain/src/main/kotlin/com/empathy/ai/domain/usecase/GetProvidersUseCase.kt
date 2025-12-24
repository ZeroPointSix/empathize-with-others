package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.repository.AiProviderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取服务商列表用例
 *
 * 获取所有已配置的 AI 服务商
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
