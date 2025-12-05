package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.repository.BrainTagRepository
import javax.inject.Inject

/**
 * 保存标签用例
 *
 * 职责：
 * 1. 保存新标签到数据库
 * 2. 验证标签内容
 * 3. 统一错误处理
 */
class SaveBrainTagUseCase @Inject constructor(
    private val brainTagRepository: BrainTagRepository
) {
    /**
     * 执行用例
     *
     * @param tag 要保存的标签对象
     * @return Result<Long> 成功时返回标签ID，失败时返回异常
     */
    suspend operator fun invoke(tag: BrainTag): Result<Long> {
        return try {
            // 验证输入
            if (tag.content.isBlank()) {
                return Result.failure(IllegalArgumentException("标签内容不能为空"))
            }

            if (tag.contactId.isBlank()) {
                return Result.failure(IllegalArgumentException("联系人ID不能为空"))
            }

            brainTagRepository.saveTag(tag)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}