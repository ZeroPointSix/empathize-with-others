package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.EditResult
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.util.ContentValidator
import com.empathy.ai.domain.util.CoroutineDispatchers
import com.empathy.ai.domain.util.Logger
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 联系人信息编辑用例
 *
 * 负责联系人姓名和目标的编辑
 * 姓名和目标分别追踪编辑状态
 */
@Singleton
class EditContactInfoUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val contentValidator: ContentValidator,
    private val dispatchers: CoroutineDispatchers,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "EditContactInfoUseCase"
    }

    /**
     * 编辑联系人姓名
     *
     * @param contactId 联系人ID
     * @param newName 新的姓名
     * @return 编辑结果
     */
    suspend fun editName(
        contactId: String,
        newName: String
    ): Result<EditResult> = withContext(dispatchers.io) {
        try {
            // 1. 验证输入
            val validation = contentValidator.validateContactName(newName.trim())
            if (!validation.isValid()) {
                return@withContext Result.success(
                    EditResult.ValidationError(validation.getErrorMessage()!!)
                )
            }

            // 2. 获取联系人
            val profile = contactRepository.getProfile(contactId).getOrNull()
                ?: return@withContext Result.success(EditResult.NotFound)

            // 3. 检查是否有变化
            if (!profile.hasNameChanges(newName.trim())) {
                return@withContext Result.success(EditResult.NoChanges)
            }

            // 4. 更新姓名
            val rowsAffected = contactRepository.updateName(
                contactId = contactId,
                newName = newName.trim(),
                modifiedTime = System.currentTimeMillis(),
                originalName = profile.name
            )

            if (rowsAffected > 0) {
                logger.d(TAG, "联系人姓名编辑成功: contactId=$contactId")
                Result.success(EditResult.Success)
            } else {
                logger.w(TAG, "联系人姓名编辑失败: 未找到记录 contactId=$contactId")
                Result.success(EditResult.NotFound)
            }
        } catch (e: Exception) {
            logger.e(TAG, "联系人姓名编辑失败", e)
            Result.success(EditResult.DatabaseError(e))
        }
    }

    /**
     * 编辑联系人目标
     *
     * @param contactId 联系人ID
     * @param newGoal 新的目标
     * @return 编辑结果
     */
    suspend fun editGoal(
        contactId: String,
        newGoal: String
    ): Result<EditResult> = withContext(dispatchers.io) {
        try {
            // 1. 验证输入
            val validation = contentValidator.validateContactGoal(newGoal.trim())
            if (!validation.isValid()) {
                return@withContext Result.success(
                    EditResult.ValidationError(validation.getErrorMessage()!!)
                )
            }

            // 2. 获取联系人
            val profile = contactRepository.getProfile(contactId).getOrNull()
                ?: return@withContext Result.success(EditResult.NotFound)

            // 3. 检查是否有变化
            if (!profile.hasGoalChanges(newGoal.trim())) {
                return@withContext Result.success(EditResult.NoChanges)
            }

            // 4. 更新目标
            val rowsAffected = contactRepository.updateGoal(
                contactId = contactId,
                newGoal = newGoal.trim(),
                modifiedTime = System.currentTimeMillis(),
                originalGoal = profile.targetGoal
            )

            if (rowsAffected > 0) {
                logger.d(TAG, "联系人目标编辑成功: contactId=$contactId")
                Result.success(EditResult.Success)
            } else {
                logger.w(TAG, "联系人目标编辑失败: 未找到记录 contactId=$contactId")
                Result.success(EditResult.NotFound)
            }
        } catch (e: Exception) {
            logger.e(TAG, "联系人目标编辑失败", e)
            Result.success(EditResult.DatabaseError(e))
        }
    }
}
