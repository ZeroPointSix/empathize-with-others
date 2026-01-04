package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.AiProviderRepository
import javax.inject.Inject

/**
 * 删除服务商用例
 *
 * 删除指定的 AI 服务商配置。
 *
 * 业务背景:
 *   - TD-00025: AI配置功能完善
 *   - 场景: 用户删除不再使用的AI服务商配置
 *
 * 注意事项:
 *   - 删除服务商前需确保无联系人正在使用
 *   - 默认服务商不能删除（需要在UI层面限制）
 *   - 删除后相关配置不可恢复
 *
 * @see AiProviderRepository.deleteProvider 仓库删除方法
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
