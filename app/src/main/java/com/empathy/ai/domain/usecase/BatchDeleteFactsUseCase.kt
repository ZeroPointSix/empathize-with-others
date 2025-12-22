package com.empathy.ai.domain.usecase

import android.util.Log
import com.empathy.ai.domain.repository.ContactRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 批量删除Fact用例
 *
 * 从联系人的facts列表中批量删除指定ID的Fact
 */
@Singleton
class BatchDeleteFactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
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
        Log.d(TAG, "========== 批量删除Facts开始 ==========")
        Log.d(TAG, "contactId=$contactId")
        Log.d(TAG, "要删除的factIds数量=${factIds.size}")
        factIds.forEachIndexed { index, id ->
            Log.d(TAG, "  [$index] factId=$id")
        }

        // 空ID列表返回0
        if (factIds.isEmpty()) {
            Log.d(TAG, "factIds为空，直接返回0")
            return Result.success(0)
        }

        // 获取联系人
        val profileResult = contactRepository.getProfile(contactId)
        val profile = profileResult.getOrNull()
            ?: return Result.failure(IllegalArgumentException("联系人不存在: $contactId"))

        Log.d(TAG, "当前联系人facts数量=${profile.facts.size}")
        profile.facts.forEachIndexed { index, fact ->
            Log.d(TAG, "  [$index] id=${fact.id}, key=${fact.key}")
        }

        // 计算要删除的Fact ID集合
        val factIdsToDelete = factIds.toSet()
        
        // 过滤掉要删除的Facts
        val originalCount = profile.facts.size
        val remainingFacts = profile.facts.filter { it.id !in factIdsToDelete }
        val deletedCount = originalCount - remainingFacts.size

        Log.d(TAG, "原始数量=$originalCount, 剩余数量=${remainingFacts.size}, 删除数量=$deletedCount")

        // 如果没有实际删除任何Fact，直接返回0
        if (deletedCount == 0) {
            Log.d(TAG, "没有实际删除任何Fact，直接返回0")
            return Result.success(0)
        }

        // 更新联系人的Facts
        Log.d(TAG, "调用contactRepository.updateFacts更新Facts")
        val result = contactRepository.updateFacts(contactId, remainingFacts)
            .map { deletedCount }
        
        Log.d(TAG, "========== 批量删除Facts完成 ==========")
        return result
    }
}
