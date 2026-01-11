package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.repository.BrainTagRepository
import javax.inject.Inject

/**
 * 编辑脑标签用例
 *
 * ## 业务背景 (BUG-00066)
 * 画像标签编辑功能缺失，用户添加标签后无法修改。
 * 此用例提供标签内容和类型的编辑能力。
 *
 * ## 业务规则
 * - 支持修改标签内容（content）
 * - 支持切换标签类型（RISK_RED ↔ STRATEGY_GREEN）
 * - 不允许修改标签来源（source）
 * - 内容不能为空
 *
 * ## 数据流
 * ```
 * UI Event → ViewModel → EditBrainTagUseCase → BrainTagRepository → BrainTagDao
 * ```
 *
 * @see BrainTagRepository
 * @see BrainTag
 */
class EditBrainTagUseCase @Inject constructor(
    private val brainTagRepository: BrainTagRepository
) {
    /**
     * 执行编辑操作
     *
     * @param tagId 标签ID
     * @param newContent 新的标签内容
     * @param newType 新的标签类型
     * @return 操作结果，成功返回 Result.success(Unit)，失败返回 Result.failure(Exception)
     */
    suspend operator fun invoke(
        tagId: Long,
        newContent: String,
        newType: TagType
    ): Result<Unit> {
        // 验证内容不为空
        if (newContent.isBlank()) {
            return Result.failure(IllegalArgumentException("标签内容不能为空"))
        }

        // 构建更新后的标签
        // 注意：contactId 和 source 由 Repository 层从数据库获取并保留
        val updatedTag = BrainTag(
            id = tagId,
            contactId = "", // 由 Repository 层处理
            content = newContent.trim(),
            type = newType
        )

        return brainTagRepository.updateTag(updatedTag)
    }
}
