package com.empathy.ai.data.repository

import android.util.Log
import com.empathy.ai.data.local.DefaultPrompts
import com.empathy.ai.data.local.PromptFileStorage
import com.empathy.ai.data.local.dao.ContactDao
import com.empathy.ai.domain.model.GlobalPromptConfig
import com.empathy.ai.domain.model.PromptError
import com.empathy.ai.domain.model.PromptHistoryItem
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.PromptValidationResult
import com.empathy.ai.domain.model.ScenePromptConfig
import com.empathy.ai.domain.repository.PromptRepository
import com.empathy.ai.domain.util.PromptSanitizer
import com.empathy.ai.domain.util.PromptValidator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PromptRepositoryImpl 实现了提示词配置的数据访问层
 *
 * 【架构位置】Clean Architecture Data层
 * 【业务背景】(PRD-00005)提示词管理系统
 *   - 全局提示词：适用于所有联系人的默认配置
 *   - 联系人级提示词：针对特定联系人的个性化配置
 *   - 提示词历史：支持回滚到历史版本
 *
 * 【设计决策】(TDD-00005)
 *   - Mutex.withLock保证并发写入的原子性
 *   - PromptValidator验证提示词格式和长度
 *   - PromptSanitizer安全检查，检测潜在风险内容
 *
 * 【关键逻辑】
 *   - updateSceneConfig：使用锁保证并发安全
 *   - saveGlobalPrompt：验证+安全检查后写入
 *   - 继承策略：联系人无自定义提示词时继承全局配置
 *
 * 【任务追踪】
 *   - FD-00005/Task-001: 提示词CRUD基础功能
 *   - FD-00005/Task-002: 提示词验证和安全检查
 *   - FD-00005/Task-003: 提示词历史记录管理
 */
@Singleton
class PromptRepositoryImpl @Inject constructor(
    private val fileStorage: PromptFileStorage,
    private val contactDao: ContactDao,
    private val validator: PromptValidator
) : PromptRepository {

    companion object {
        private const val TAG = "PromptRepository"
    }

    private val mutex = Mutex()

    /**
     * updateSceneConfig 更新场景配置（线程安全）
     *
     * 【并发策略】使用Mutex.withLock保证并发写入的原子性
     * 原因：提示词配置涉及读写文件，多线程并发可能导致数据竞争
     * 设计权衡：锁粒度最小化，只锁住配置更新操作
     *
     * 【操作步骤】
     *   1. 读取当前全局配置（文件）
     *   2. 调用configUpdater更新场景配置
     *   3. 写回全局配置（文件）
     *
     * 【错误处理】文件读取失败时返回PromptError.StorageError
     */
    private suspend fun updateSceneConfig(
        scene: PromptScene,
        configUpdater: (ScenePromptConfig?) -> ScenePromptConfig
    ): Result<Unit> {
        return mutex.withLock {
            val currentConfig = fileStorage.readGlobalConfig().getOrElse {
                return@withLock Result.failure(
                    PromptError.StorageError("读取配置失败", it)
                )
            }

            val currentSceneConfig = currentConfig.prompts[scene]
            val newSceneConfig = configUpdater(currentSceneConfig)

            val newPrompts = currentConfig.prompts.toMutableMap()
            newPrompts[scene] = newSceneConfig

            val newConfig = currentConfig.copy(prompts = newPrompts)
            fileStorage.writeGlobalConfig(newConfig)
        }
    }

    override suspend fun getGlobalConfig(): Result<GlobalPromptConfig> {
        return fileStorage.readGlobalConfig()
    }

    override suspend fun getGlobalPrompt(scene: PromptScene): Result<String> {
        return fileStorage.readGlobalConfig().map { config ->
            config.prompts[scene]?.userPrompt ?: DefaultPrompts.getDefault(scene)
        }
    }

    override suspend fun saveGlobalPrompt(scene: PromptScene, prompt: String): Result<Unit> {
        val validationResult = validator.validate(prompt, scene, allowEmpty = true)
        if (validationResult is PromptValidationResult.Error) {
            return Result.failure(
                PromptError.ValidationError(
                    message = validationResult.message,
                    errorType = validationResult.errorType
                )
            )
        }

        if (prompt.isNotBlank()) {
            val sanitizeResult = PromptSanitizer.detectDangerousContent(prompt)
            if (!sanitizeResult.isSafe) {
                Log.w(TAG, "检测到潜在风险: ${sanitizeResult.warnings}")
            }
        }

        return updateSceneConfig(scene) { currentSceneConfig ->
            if (currentSceneConfig != null) {
                currentSceneConfig.addHistory(currentSceneConfig.userPrompt)
                    .copy(userPrompt = prompt)
            } else {
                ScenePromptConfig(userPrompt = prompt)
            }
        }
    }

    override suspend fun getContactPrompt(contactId: String): Result<String?> {
        return try {
            val prompt = contactDao.getCustomPrompt(contactId)
            Result.success(prompt)
        } catch (e: Exception) {
            Log.e(TAG, "获取联系人提示词失败", e)
            Result.failure(PromptError.DatabaseError("获取联系人提示词失败", e))
        }
    }

    override suspend fun saveContactPrompt(contactId: String, prompt: String?): Result<Unit> {
        return try {
            if (prompt != null) {
                val sanitizeResult = PromptSanitizer.detectDangerousContent(prompt)
                if (!sanitizeResult.isSafe) {
                    Log.w(TAG, "联系人提示词检测到潜在风险: ${sanitizeResult.warnings}")
                }
            }

            contactDao.updateCustomPrompt(contactId, prompt)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "保存联系人提示词失败", e)
            Result.failure(PromptError.DatabaseError("保存联系人提示词失败", e))
        }
    }

    override suspend fun restoreDefault(scene: PromptScene): Result<Unit> {
        val defaultPrompt = DefaultPrompts.getDefault(scene)
        return updateSceneConfig(scene) { currentSceneConfig ->
            if (currentSceneConfig != null) {
                currentSceneConfig.addHistory(currentSceneConfig.userPrompt)
                    .copy(userPrompt = defaultPrompt)
            } else {
                ScenePromptConfig(userPrompt = defaultPrompt)
            }
        }
    }

    override suspend fun restoreFromHistory(
        scene: PromptScene,
        historyIndex: Int
    ): Result<Unit> {
        val currentConfig = fileStorage.readGlobalConfig().getOrElse {
            return Result.failure(PromptError.StorageError("读取配置失败", it))
        }

        val sceneConfig = currentConfig.prompts[scene]
            ?: return Result.failure(PromptError.StorageError("场景配置不存在"))

        if (historyIndex < 0 || historyIndex >= sceneConfig.history.size) {
            return Result.failure(
                PromptError.ValidationError(
                    "历史记录索引无效",
                    PromptValidationResult.ErrorType.INVALID_FORMAT
                )
            )
        }

        val historyItem = sceneConfig.history[historyIndex]
        return updateSceneConfig(scene) { currentSceneConfig ->
            currentSceneConfig!!.addHistory(currentSceneConfig.userPrompt)
                .copy(userPrompt = historyItem.userPrompt)
        }
    }

    override suspend fun getHistory(scene: PromptScene): Result<List<PromptHistoryItem>> {
        return fileStorage.readGlobalConfig().map { config ->
            config.prompts[scene]?.history ?: emptyList()
        }
    }
}
