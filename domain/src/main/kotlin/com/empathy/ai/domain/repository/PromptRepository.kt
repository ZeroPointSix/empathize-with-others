package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.GlobalPromptConfig
import com.empathy.ai.domain.model.PromptHistoryItem
import com.empathy.ai.domain.model.PromptScene

/**
 * 提示词仓库接口
 *
 * 定义提示词管理的所有数据访问操作
 */
interface PromptRepository {

    /**
     * 获取全局提示词配置
     *
     * @return 全局配置或错误
     */
    suspend fun getGlobalConfig(): Result<GlobalPromptConfig>

    /**
     * 获取指定场景的全局提示词
     *
     * @param scene 场景类型
     * @return 提示词内容或错误
     */
    suspend fun getGlobalPrompt(scene: PromptScene): Result<String>

    /**
     * 保存全局提示词
     *
     * 保存前会进行验证和安全检查
     *
     * @param scene 场景类型
     * @param prompt 提示词内容
     * @return 成功返回Unit，失败返回PromptError
     */
    suspend fun saveGlobalPrompt(scene: PromptScene, prompt: String): Result<Unit>

    /**
     * 获取联系人自定义提示词
     *
     * @param contactId 联系人ID
     * @return 提示词内容（可能为null）或错误
     */
    suspend fun getContactPrompt(contactId: String): Result<String?>

    /**
     * 保存联系人自定义提示词
     *
     * @param contactId 联系人ID
     * @param prompt 提示词内容，传null表示清除
     * @return 成功返回Unit，失败返回错误
     */
    suspend fun saveContactPrompt(contactId: String, prompt: String?): Result<Unit>

    /**
     * 恢复场景默认提示词
     *
     * @param scene 场景类型
     * @return 成功返回Unit，失败返回错误
     */
    suspend fun restoreDefault(scene: PromptScene): Result<Unit>

    /**
     * 从历史记录恢复
     *
     * @param scene 场景类型
     * @param historyIndex 历史记录索引（0为最近一条）
     * @return 成功返回Unit，失败返回错误
     */
    suspend fun restoreFromHistory(scene: PromptScene, historyIndex: Int): Result<Unit>

    /**
     * 获取场景历史记录
     *
     * @param scene 场景类型
     * @return 历史记录列表或错误
     */
    suspend fun getHistory(scene: PromptScene): Result<List<PromptHistoryItem>>
}
