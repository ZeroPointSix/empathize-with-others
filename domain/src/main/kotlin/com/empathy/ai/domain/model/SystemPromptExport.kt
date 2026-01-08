package com.empathy.ai.domain.model

/**
 * 系统提示词导出格式
 *
 * @property version 导出格式版本
 * @property exportTime 导出时间
 * @property scenes 场景配置映射（使用场景名称字符串作为key）
 *
 * @see PRD-00033 开发者模式与系统提示词编辑
 */
data class SystemPromptExport(
    val version: String = "1.0",
    val exportTime: String,
    val scenes: Map<String, SceneExportData>
)

/**
 * 单个场景的导出数据
 *
 * @property header 自定义Header
 * @property footer 自定义Footer
 */
data class SceneExportData(
    val header: String?,
    val footer: String?
)
