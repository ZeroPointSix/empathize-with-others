package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.AiProviderRepository
import javax.inject.Inject

/**
 * 删除服务商用例
 *
 * 删除指定的 AI 服务商配置
 */
class DeleteProviderUseCase @Inject constructor(
    private val repository: AiProviderRepository
) {
    /**
     * 执行删除操作
     *
     * @param id 要删除的服务商 ID
     * @return Result 表示操作成功或失败
     */
    suspend operator fun invoke(id: String): Result<Unit> {
        if (id.isBlank()) {
            return Result.failure(IllegalArgumentException("服务商 ID 不能为空"))
        }
        
        return repository.deleteProvider(id)
    }
}
