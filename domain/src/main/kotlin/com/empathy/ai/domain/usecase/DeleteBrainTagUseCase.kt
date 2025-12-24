package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.BrainTagRepository
import javax.inject.Inject

/**
 * 删除标签用例
 *
 * 职责：
 * 1. 根据标签ID删除标签
 * 2. 验证输入参数
 * 3. 统一错误处理
 */
class DeleteBrainTagUseCase @Inject constructor(
    private val brainTagRepository: BrainTagRepository
) {
    /**
     * 执行用例
     *
     * @param tagId 标签ID
     * @return Result<Unit> 成功时返回Unit，失败时返回异常
     */
    suspend operator fun invoke(tagId: Long): Result<Unit> {
        return try {
            if (tagId <= 0) {
                return Result.failure(IllegalArgumentException("标签ID无效"))
            }

            brainTagRepository.deleteTag(tagId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
