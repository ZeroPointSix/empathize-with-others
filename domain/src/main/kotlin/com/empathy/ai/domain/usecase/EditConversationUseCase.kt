package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.EditResult
import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.util.ContentValidator
import com.empathy.ai.domain.util.CoroutineDispatchers
import com.empathy.ai.domain.util.IdentityPrefixHelper
import com.empathy.ai.domain.util.Logger
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 对话记录编辑用例
 *
 * 负责对话内容的编辑和保存
 * 注意：需要处理身份前缀（"我："或"对方："）
 */
@Singleton
class EditConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val contentValidator: ContentValidator,
    private val dispatchers: CoroutineDispatchers,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "EditConversationUseCase"
    }

    /**
     * 编辑对话记录
     *
     * @param logId 对话记录ID
     * @param newContent 新的内容（已包含身份前缀）
     * @return 编辑结果
     */
    suspend operator fun invoke(
        logId: Long,
        newContent: String
    ): Result<EditResult> = withContext(dispatchers.io) {
        try {
            // 1. 验证输入（解析后的纯文本）
            val parseResult = IdentityPrefixHelper.parse(newContent)
            val validation = contentValidator.validateConversation(parseResult.content.trim())
            if (!validation.isValid()) {
                return@withContext Result.success(
                    EditResult.ValidationError(validation.getErrorMessage()!!)
                )
            }

            // 2. 获取原对话记录
            val log = conversationRepository.getById(logId)
                ?: return@withContext Result.success(EditResult.NotFound)

            // 3. 检查是否有变化
            if (!log.hasChanges(newContent.trim())) {
                return@withContext Result.success(EditResult.NoChanges)
            }

            // 4. 更新对话记录
            val rowsAffected = conversationRepository.updateUserInputWithTracking(
                logId = logId,
                newUserInput = newContent.trim(),
                modifiedTime = System.currentTimeMillis(),
                originalInput = log.userInput
            )

            if (rowsAffected > 0) {
                logger.d(TAG, "对话编辑成功: logId=$logId")
                Result.success(EditResult.Success)
            } else {
                logger.w(TAG, "对话编辑失败: 未找到记录 logId=$logId")
                Result.success(EditResult.NotFound)
            }
        } catch (e: Exception) {
            logger.e(TAG, "对话编辑失败", e)
            Result.success(EditResult.DatabaseError(e))
        }
    }
}
