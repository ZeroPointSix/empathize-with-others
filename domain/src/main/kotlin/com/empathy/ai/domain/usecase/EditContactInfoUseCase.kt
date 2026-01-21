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
 * 负责联系人姓名和目标的编辑。
 * 姓名和目标分别追踪编辑状态，支持独立修改和回滚。
 *
 * 业务背景:
 *   - PRD-00003: 联系人画像记忆系统需求
 *   - 场景: 用户编辑联系人的基本信息（姓名、关系目标）
 *
 * 设计决策:
 *   - 分离编辑: editName 和 editGoal 分开，便于独立追踪
 *   - 变化检测: hasNameChanges/hasGoalChanges 避免无意义更新
 *   - 保留原始值: originalName/originalGoal 支持撤销操作
 *   - 统一返回: EditResult 封装各种结果状态
 *
 * @see EditResult 编辑结果密封类
 * @see ContentValidator 内容验证器
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

    /**
     * 编辑联系方式
     *
     * @param contactId 联系人ID
     * @param newContactInfo 新的联系方式（允许为空）
     * @return 编辑结果
     */
    suspend fun editContactInfo(
        contactId: String,
        newContactInfo: String?
    ): Result<EditResult> = withContext(dispatchers.io) {
        try {
            val profile = contactRepository.getProfile(contactId).getOrNull()
                ?: return@withContext Result.success(EditResult.NotFound)

            val normalized = newContactInfo?.trim().orEmpty()
            val normalizedValue = normalized.ifBlank { null }

            if (profile.contactInfo == normalizedValue) {
                return@withContext Result.success(EditResult.NoChanges)
            }

            val rowsAffected = contactRepository.updateContactInfo(
                contactId = contactId,
                contactInfo = normalizedValue
            )

            if (rowsAffected > 0) {
                logger.d(TAG, "联系人联系方式编辑成功: contactId=$contactId")
                Result.success(EditResult.Success)
            } else {
                logger.w(TAG, "联系人联系方式编辑失败: 未找到记录 contactId=$contactId")
                Result.success(EditResult.NotFound)
            }
        } catch (e: Exception) {
            logger.e(TAG, "联系人联系方式编辑失败", e)
            Result.success(EditResult.DatabaseError(e))
        }
    }

    /**
     * 更新联系人头像
     *
     * @param contactId 联系人ID
     * @param avatarUrl 头像URI
     * @param avatarColorSeed 默认头像颜色索引
     * @return 编辑结果
     */
    suspend fun editAvatar(
        contactId: String,
        avatarUrl: String?,
        avatarColorSeed: Int
    ): Result<EditResult> = withContext(dispatchers.io) {
        try {
            val profile = contactRepository.getProfile(contactId).getOrNull()
                ?: return@withContext Result.success(EditResult.NotFound)

            if (profile.avatarUrl == avatarUrl && profile.avatarColorSeed == avatarColorSeed) {
                return@withContext Result.success(EditResult.NoChanges)
            }

            val rowsAffected = contactRepository.updateAvatar(
                contactId = contactId,
                avatarUrl = avatarUrl,
                avatarColorSeed = avatarColorSeed
            )

            if (rowsAffected > 0) {
                logger.d(TAG, "联系人头像更新成功: contactId=$contactId")
                Result.success(EditResult.Success)
            } else {
                logger.w(TAG, "联系人头像更新失败: 未找到记录 contactId=$contactId")
                Result.success(EditResult.NotFound)
            }
        } catch (e: Exception) {
            logger.e(TAG, "联系人头像更新失败", e)
            Result.success(EditResult.DatabaseError(e))
        }
    }
}
