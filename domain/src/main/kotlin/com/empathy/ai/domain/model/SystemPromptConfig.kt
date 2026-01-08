package com.empathy.ai.domain.model

/**
 * 系统提示词配置
 *
 * @property version 配置版本号
 * @property scenes 各场景的提示词配置
 * @property lastModified 最后修改时间
 *
 * @see PRD-00033 开发者模式与系统提示词编辑
 */
data class SystemPromptConfig(
    val version: Int = CURRENT_VERSION,
    val scenes: Map<SystemPromptScene, SceneSystemPrompt> = emptyMap(),
    val lastModified: Long = System.currentTimeMillis()
) {
    companion object {
        const val CURRENT_VERSION = 1

        /**
         * 创建空配置（使用默认值）
         */
        fun createEmpty(): SystemPromptConfig = SystemPromptConfig()
    }

    /**
     * 获取指定场景的Header
     * @param scene 场景
     * @return 自定义Header，如果未设置则返回null
     */
    fun getHeader(scene: SystemPromptScene): String? {
        return scenes[scene]?.header
    }

    /**
     * 获取指定场景的Footer
     * @param scene 场景
     * @return 自定义Footer，如果未设置则返回null
     */
    fun getFooter(scene: SystemPromptScene): String? {
        return scenes[scene]?.footer
    }

    /**
     * 更新指定场景的配置
     */
    fun updateScene(
        scene: SystemPromptScene,
        header: String?,
        footer: String?
    ): SystemPromptConfig {
        val updatedScenes = scenes.toMutableMap()
        updatedScenes[scene] = SceneSystemPrompt(
            header = header,
            footer = footer,
            updatedAt = System.currentTimeMillis()
        )
        return copy(
            scenes = updatedScenes,
            lastModified = System.currentTimeMillis()
        )
    }

    /**
     * 重置指定场景为默认值
     */
    fun resetScene(scene: SystemPromptScene): SystemPromptConfig {
        val updatedScenes = scenes.toMutableMap()
        updatedScenes.remove(scene)
        return copy(
            scenes = updatedScenes,
            lastModified = System.currentTimeMillis()
        )
    }

    /**
     * 重置所有场景为默认值
     */
    fun resetAll(): SystemPromptConfig {
        return SystemPromptConfig()
    }
}

/**
 * 单个场景的系统提示词配置
 *
 * @property header 自定义Header（角色定义），null表示使用默认值
 * @property footer 自定义Footer（输出格式），null表示使用默认值
 * @property updatedAt 更新时间
 */
data class SceneSystemPrompt(
    val header: String? = null,
    val footer: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
