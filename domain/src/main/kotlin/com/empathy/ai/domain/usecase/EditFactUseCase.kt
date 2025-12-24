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
 * 事实编辑用例
 *
 * 负责事实内容的编辑和保存
 * 支持编辑追踪：首次编辑保存原始值，后续编辑保留首次原始值
 */
@Singleton
class EditFactUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val contentValidator: ContentValidator,
    private val dispatchers: CoroutineDispatchers,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "EditFactUseCase"
    }

    /**
     * 编辑事实
     *
     * @param contactId 联系人ID
     * @param factId 事实ID
     * @param newKey 新的键（类型）
     * @param newValue 新的值（内容）
     * @return 编辑结果
     */
    suspend operator fun invoke(
        contactId: String,
        factId: String,
        newKey: String,
        newValue: String
    ): Result<EditResult> = withContext(dispatchers.io) {
        try {
            // 1. 验证输入
            val keyValidation = contentValidator.validateFactKey(newKey.trim())
            if (!keyValidation.isValid()) {
                return@withContext Result.success(
                    EditResult.ValidationError(keyValidation.getErrorMessage()!!)
                )
            }

            val valueValidation = contentValidator.validateFactValue(newValue.trim())
            if (!valueValidation.isValid()) {
                return@withContext Result.success(
                    EditResult.ValidationError(valueValidation.getErrorMessage()!!)
                )
            }

            // 2. 获取联系人
            val profile = contactRepository.getProfile(contactId).getOrNull()
            if (profile == null) {
                logger.e(TAG, "联系人不存在: contactId=$contactId")
                return@withContext Result.success(EditResult.NotFound)
            }

            // 3. 查找事实 - 添加详细日志
            logger.d(TAG, "========== 事实查找调试 ==========")
            logger.d(TAG, "查找目标factId: $factId")
            logger.d(TAG, "数据库中facts数量: ${profile.facts.size}")
            profile.facts.forEachIndexed { index, f ->
                logger.d(TAG, "  [$index] id=${f.id}, key=${f.key}, value=${f.value.take(20)}...")
            }
            
            val fact = profile.facts.find { it.id == factId }
            if (fact == null) {
                logger.e(TAG, "未找到事实! factId=$factId 不在数据库的facts列表中")
                logger.e(TAG, "可能原因: 1.新添加的事实未持久化 2.id在序列化时丢失 3.内存状态与数据库不同步")
                return@withContext Result.success(EditResult.NotFound)
            }
            logger.d(TAG, "找到事实: id=${fact.id}, key=${fact.key}")

            // 4. 检查是否有变化
            if (!fact.hasChanges(newKey.trim(), newValue.trim())) {
                return@withContext Result.success(EditResult.NoChanges)
            }

            // 5. 创建编辑后的事实
            val updatedFact = fact.copyWithEdit(newKey.trim(), newValue.trim())

            // 6. 更新联系人的facts列表
            val updatedFacts = profile.facts.map {
                if (it.id == factId) updatedFact else it
            }
            val updatedProfile = profile.copy(facts = updatedFacts)

            // 7. 保存
            contactRepository.updateProfile(updatedProfile)

            logger.d(TAG, "事实编辑成功: factId=$factId")
            Result.success(EditResult.Success)
        } catch (e: Exception) {
            logger.e(TAG, "事实编辑失败", e)
            Result.success(EditResult.DatabaseError(e))
        }
    }
}
