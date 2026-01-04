package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.GlobalPromptConfig
import com.empathy.ai.domain.model.PromptHistoryItem
import com.empathy.ai.domain.model.PromptScene

/**
 * 提示词仓储接口
 *
 * 业务背景 (PRD-00005):
 * - 提示词从硬编码中抽离，存储为可配置的JSON文件
 * - 区分系统提示词（不可修改）和用户提示词（可自定义）
 * - 支持全局提示词和联系人单独提示词两套配置
 * - 支持变量插值，如{{contact_name}}、{{relationship_status}}
 *
 * 设计决策:
 * - 提示词分层：系统约束（不可变）+ 用户指令（可编辑）
 * - 优先级：联系人提示词 > 全局提示词
 * - 历史记录：每场景保留最近3条，支持回滚
 * - 存储位置：应用私有目录JSON文件
 *
 * 提示词场景 (PromptScene):
 * - ANALYZE: 聊天分析
 * - CHECK: 安全检查
 * - EXTRACT: 信息提取
 * - SUMMARY: 每日总结
 */
interface PromptRepository {

    /**
     * 获取全局提示词配置
     *
     * @return GlobalPromptConfig或错误
     */
    suspend fun getGlobalConfig(): Result<GlobalPromptConfig>

    /**
     * 获取指定场景的全局提示词
     *
     * @param scene 场景类型（ANALYZE/CHECK/EXTRACT/SUMMARY）
     * @return 用户自定义的提示词内容，为空时返回系统默认值
     */
    suspend fun getGlobalPrompt(scene: PromptScene): Result<String>

    /**
     * 保存全局提示词
     *
     * 业务规则:
     * - 保存前进行验证：长度限制（最大1000字符）
     * - 自动保存历史记录（每场景最多3条）
     * - 检查潜在的安全风险
     *
     * @param scene 场景类型
     * @param prompt 提示词内容
     * @return 成功返回Unit，失败返回PromptError
     */
    suspend fun saveGlobalPrompt(scene: PromptScene, prompt: String): Result<Unit>

    /**
     * 获取联系人自定义提示词
     *
     * 业务规则:
     * - 存储在ContactProfile.customPrompt字段
     * - 为空时返回null，表示使用全局提示词
     *
     * @param contactId 联系人ID
     * @return 提示词内容（可能为null）或错误
     */
    suspend fun getContactPrompt(contactId: String): Result<String?>

    /**
     * 保存联系人自定义提示词
     *
     * 业务规则:
     * - 覆盖保存，不追加历史
     * - 传null表示清除自定义提示词
     * - 优先级高于全局提示词
     *
     * @param contactId 联系人ID
     * @param prompt 提示词内容，传null表示清除
     * @return 成功返回Unit，失败返回错误
     */
    suspend fun saveContactPrompt(contactId: String, prompt: String?): Result<Unit>

    /**
     * 恢复场景默认提示词
     *
     * 业务规则:
     * - 清除用户自定义的提示词
     * - 恢复为系统内置的默认提示词
     * - 不影响历史记录
     *
     * @param scene 场景类型
     * @return 成功返回Unit，失败返回错误
     */
    suspend fun restoreDefault(scene: PromptScene): Result<Unit>

    /**
     * 从历史记录恢复
     *
     * 业务规则:
     * - historyIndex=0为最近一条，1为次新，以此类推
     * - 恢复后该记录成为最新提示词
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
     * @return 历史记录列表（按时间倒序）
     */
    suspend fun getHistory(scene: PromptScene): Result<List<PromptHistoryItem>>
}
