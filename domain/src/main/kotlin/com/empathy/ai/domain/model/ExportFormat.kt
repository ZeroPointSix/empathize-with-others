package com.empathy.ai.domain.model

/**
 * 用户画像导出格式枚举
 *
 * 定义支持的导出格式类型，包括JSON和纯文本格式。
 *
 * @property displayName 格式的中文显示名称
 * @property fileExtension 文件扩展名
 * @property mimeType MIME类型
 */
enum class ExportFormat(
    val displayName: String,
    val fileExtension: String,
    val mimeType: String
) {
    /**
     * JSON格式
     * 推荐格式，支持完整的数据结构导入导出
     */
    JSON(
        displayName = "JSON格式",
        fileExtension = ".json",
        mimeType = "application/json"
    ),
    
    /**
     * 纯文本格式
     * 可读性好，适合查看和分享
     */
    PLAIN_TEXT(
        displayName = "纯文本格式",
        fileExtension = ".txt",
        mimeType = "text/plain"
    );
    
    companion object {
        /**
         * 根据文件扩展名获取格式
         *
         * @param extension 文件扩展名（包含或不包含点号）
         * @return 对应的格式，如果不支持返回null
         */
        fun fromExtension(extension: String): ExportFormat? {
            val normalizedExt = if (extension.startsWith(".")) extension else ".$extension"
            return entries.find { it.fileExtension.equals(normalizedExt, ignoreCase = true) }
        }
        
        /**
         * 获取默认导出格式
         */
        fun getDefault(): ExportFormat = JSON
        
        /**
         * 获取所有支持的格式列表
         */
        fun getAllFormats(): List<ExportFormat> = entries.toList()
    }
    
    /**
     * 生成带时间戳的文件名
     *
     * @param baseName 基础文件名
     * @return 完整的文件名（包含扩展名）
     */
    fun generateFileName(baseName: String = "user_profile"): String {
        val timestamp = System.currentTimeMillis()
        return "${baseName}_$timestamp$fileExtension"
    }
}
