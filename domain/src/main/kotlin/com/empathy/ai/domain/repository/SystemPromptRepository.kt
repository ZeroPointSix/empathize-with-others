package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.SystemPromptConfig
import com.empathy.ai.domain.model.SystemPromptScene

/**
 * 系统提示词仓库接口
 *
 * 提供系统提示词配置的读写、导入导出功能
 *
 * @see PRD-00033 开发者模式与系统提示词编辑
 */
interface SystemPromptRepository {

    /**
     * 获取系统提示词配置
     * @return 配置结果
     */
    suspend fun getConfig(): Result<SystemPromptConfig>

    /**
     * 保存系统提示词配置
     * @param config 配置
     * @return 保存结果
     */
    suspend fun saveConfig(config: SystemPromptConfig): Result<Unit>

    /**
     * 获取指定场景的Header
     * @param scene 场景
     * @return 自定义Header，如果未设置则返回null
     */
    suspend fun getHeader(scene: SystemPromptScene): String?

    /**
     * 获取指定场景的Footer
     * @param scene 场景
     * @return 自定义Footer，如果未设置则返回null
     */
    suspend fun getFooter(scene: SystemPromptScene): String?

    /**
     * 更新指定场景的配置
     * @param scene 场景
     * @param header 新的Header
     * @param footer 新的Footer
     * @return 更新结果
     */
    suspend fun updateScene(
        scene: SystemPromptScene,
        header: String?,
        footer: String?
    ): Result<Unit>

    /**
     * 重置指定场景为默认值
     * @param scene 场景
     * @return 重置结果
     */
    suspend fun resetScene(scene: SystemPromptScene): Result<Unit>

    /**
     * 重置所有场景为默认值
     * @return 重置结果
     */
    suspend fun resetAll(): Result<Unit>

    /**
     * 导出配置为JSON字符串
     * @return JSON字符串
     */
    suspend fun exportToJson(): Result<String>

    /**
     * 导出配置为Markdown字符串
     * @return Markdown字符串
     */
    suspend fun exportToMarkdown(): Result<String>

    /**
     * 从JSON字符串导入配置
     * @param json JSON字符串
     * @return 导入结果
     */
    suspend fun importFromJson(json: String): Result<Unit>

    /**
     * 检查是否有自定义配置
     * @return 是否有自定义配置
     */
    suspend fun hasCustomConfig(): Boolean

    /**
     * 获取已自定义的场景列表
     * @return 已自定义的场景列表
     */
    suspend fun getCustomizedScenes(): List<SystemPromptScene>
}
