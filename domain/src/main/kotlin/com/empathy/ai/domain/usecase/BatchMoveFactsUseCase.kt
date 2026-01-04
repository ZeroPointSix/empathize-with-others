package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.ContactRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 批量移动Fact到新分类用例
 *
 * 将指定的Facts移动到目标分类（更新key字段）。
 *
 * 业务背景:
 *   - PRD-00012: 事实流内容编辑功能需求
 *   - 场景: 用户批量调整事实的分类归属
 *
 * 设计决策:
 *   - 原地修改: 更新现有Fact的key字段，而非创建新对象
 *   - 保留修改痕迹: 设置 isUserModified = true，记录用户手动编辑
 *   - 保存原始值: originalKey 保存首次编辑前的分类
 *   - 效率优化: 单次遍历完成所有更新
 *
 * @see Fact.isUserModified 标记是否为用户手动修改
 * @see Fact.originalKey 原始分类名称
 */
@Singleton
class BatchMoveFactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    companion object {
        /** 分类名称最大长度 */
        const val MAX_CATEGORY_LENGTH = 20
    }

    /**
     * 批量移动指定的Facts到目标分类
     *
     * @param contactId 联系人ID
     * @param factIds 要移动的Fact ID列表
     * @param targetCategory 目标分类名称
     * @return 实际移动的数量
     */
    suspend operator fun invoke(
        contactId: String,
        factIds: List<String>,
        targetCategory: String
    ): Result<Int> {
        // 空ID列表返回0
        if (factIds.isEmpty()) {
            return Result.success(0)
        }

        // 验证目标分类名称
        if (targetCategory.isBlank()) {
            return Result.failure(IllegalArgumentException("目标分类名称不能为空"))
        }
        if (targetCategory.length > MAX_CATEGORY_LENGTH) {
            return Result.failure(
                IllegalArgumentException("目标分类名称不能超过${MAX_CATEGORY_LENGTH}个字符")
            )
        }

        // 获取联系人
        val profileResult = contactRepository.getProfile(contactId)
        val profile = profileResult.getOrNull()
            ?: return Result.failure(IllegalArgumentException("联系人不存在: $contactId"))

        // 计算要移动的Fact ID集合
        val factIdsToMove = factIds.toSet()
        val currentTime = System.currentTimeMillis()
        var movedCount = 0

        // 更新Facts的key字段
        val updatedFacts = profile.facts.map { fact ->
            if (fact.id in factIdsToMove && fact.key != targetCategory) {
                movedCount++
                fact.copy(
                    key = targetCategory,
                    isUserModified = true,
                    lastModifiedTime = currentTime,
                    // 仅首次编辑时保存原始key
                    originalKey = fact.originalKey ?: fact.key
                )
            } else {
                fact
            }
        }

        // 如果没有实际移动任何Fact，直接返回0
        if (movedCount == 0) {
            return Result.success(0)
        }

        // 更新联系人的Facts
        return contactRepository.updateFacts(contactId, updatedFacts)
            .map { movedCount }
    }
}
