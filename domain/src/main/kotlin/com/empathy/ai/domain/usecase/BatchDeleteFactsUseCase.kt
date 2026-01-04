package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.util.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 批量删除Fact用例
 *
 * 从联系人的facts列表中批量删除指定ID的Fact。
 *
 * 业务背景:
 *   - PRD-00012: 事实流内容编辑功能需求
 *   - 场景: 用户批量删除不需要的事实记录
 *
 * 设计决策:
 *   - 软删除策略: 使用 filter 过滤保留项，而非直接删除
 *   - 效率优化: 转换为 Set 进行 membership 检查（O(1) vs O(n)）
 *   - 幂等设计: 重复删除同一ID返回0，不报错
 *
 * @see ContactRepository.updateFacts 更新事实列表方法
 */
@Singleton
class BatchDeleteFactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "BatchDeleteFactsUseCase"
    }

    /**
     * 批量删除指定的Facts
     *
     * @param contactId 联系人ID
     * @param factIds 要删除的Fact ID列表
     * @return 实际删除的数量
     */
    suspend operator fun invoke(contactId: String, factIds: List<String>): Result<Int> {
        logger.d(TAG, "========== 批量删除Facts开始 ==========")
        logger.d(TAG, "contactId=$contactId")
        logger.d(TAG, "要删除的factIds数量=${factIds.size}")
        factIds.forEachIndexed { index, id ->
            logger.d(TAG, "  [$index] factId=$id")
        }

        // 空ID列表返回0
        if (factIds.isEmpty()) {
            logger.d(TAG, "factIds为空，直接返回0")
            return Result.success(0)
        }

        // 获取联系人
        val profileResult = contactRepository.getProfile(contactId)
        val profile = profileResult.getOrNull()
            ?: return Result.failure(IllegalArgumentException("联系人不存在: $contactId"))

        logger.d(TAG, "当前联系人facts数量=${profile.facts.size}")
        profile.facts.forEachIndexed { index, fact ->
            logger.d(TAG, "  [$index] id=${fact.id}, key=${fact.key}")
        }

        // 计算要删除的Fact ID集合
        val factIdsToDelete = factIds.toSet()
        
        // 过滤掉要删除的Facts
        val originalCount = profile.facts.size
        val remainingFacts = profile.facts.filter { it.id !in factIdsToDelete }
        val deletedCount = originalCount - remainingFacts.size

        logger.d(TAG, "原始数量=$originalCount, 剩余数量=${remainingFacts.size}, 删除数量=$deletedCount")

        // 如果没有实际删除任何Fact，直接返回0
        if (deletedCount == 0) {
            logger.d(TAG, "没有实际删除任何Fact，直接返回0")
            return Result.success(0)
        }

        // 更新联系人的Facts
        logger.d(TAG, "调用contactRepository.updateFacts更新Facts")
        val result = contactRepository.updateFacts(contactId, remainingFacts)
            .map { deletedCount }
        
        logger.d(TAG, "========== 批量删除Facts完成 ==========")
        return result
    }
}
