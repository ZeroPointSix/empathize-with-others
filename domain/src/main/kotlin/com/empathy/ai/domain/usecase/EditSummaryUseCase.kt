package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.EditResult
import com.empathy.ai.domain.repository.DailySummaryRepository
import com.empathy.ai.domain.util.ContentValidator
import com.empathy.ai.domain.util.CoroutineDispatchers
import com.empathy.ai.domain.util.Logger
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI总结编辑用例
 *
 * 负责总结内容的编辑和保存
 * 支持编辑追踪：首次编辑保存原始值，后续编辑保留首次原始值
 */
@Singleton
class EditSummaryUseCase @Inject constructor(
    private val dailySummaryRepository: DailySummaryRepository,
    private val contentValidator: ContentValidator,
    private val dispatchers: CoroutineDispatchers,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "EditSummaryUseCase"
    }

    /**
     * 编辑总结
     *
     * @param summaryId 总结ID
     * @param newContent 新的内容
     * @return 编辑结果
     */
    suspend operator fun invoke(
        summaryId: Long,
        newContent: String
    ): Result<EditResult> = withContext(dispatchers.io) {
        try {
            // 1. 验证输入
            val validation = contentValidator.validateSummary(newContent.trim())
            if (!validation.isValid()) {
                return@withContext Result.success(
                    EditResult.ValidationError(validation.getErrorMessage()!!)
                )
            }

            // 2. 获取原总结
            val summary = dailySummaryRepository.getById(summaryId)
                ?: return@withContext Result.success(EditResult.NotFound)

            // 3. 检查是否有变化
            if (!summary.hasChanges(newContent.trim())) {
                return@withContext Result.success(EditResult.NoChanges)
            }

            // 4. 更新总结
            val rowsAffected = dailySummaryRepository.updateContent(
                summaryId = summaryId,
                newContent = newContent.trim(),
                modifiedTime = System.currentTimeMillis(),
                originalContent = summary.content
            )

            if (rowsAffected > 0) {
                logger.d(TAG, "总结编辑成功: summaryId=$summaryId")
                Result.success(EditResult.Success)
            } else {
                logger.w(TAG, "总结编辑失败: 未找到记录 summaryId=$summaryId")
                Result.success(EditResult.NotFound)
            }
        } catch (e: Exception) {
            logger.e(TAG, "总结编辑失败", e)
            Result.success(EditResult.DatabaseError(e))
        }
    }
}
