package com.empathy.ai.build

/**
 * 发布阶段枚举
 * 定义应用的不同发布阶段及其对应的图标配置
 *
 * @property displayName 显示名称（中文）
 * @property iconSuffix 图标目录后缀
 * @property badgeText 角标文字（正式版为null）
 * @property badgeColor 角标颜色（十六进制）
 * @property priority 优先级（用于排序，数值越大优先级越高）
 * 
 * @see TDD-00024 4.1.2 ReleaseStage枚举
 */
enum class ReleaseStage(
    val displayName: String,
    val iconSuffix: String,
    val badgeText: String?,
    val badgeColor: String,
    val priority: Int
) {
    /** 开发版 - 内部开发使用 */
    DEV(
        displayName = "开发版",
        iconSuffix = "dev",
        badgeText = "DEV",
        badgeColor = "#FF9800",  // 橙色
        priority = 1
    ),
    
    /** 测试版 - 内部测试使用 */
    TEST(
        displayName = "测试版",
        iconSuffix = "test",
        badgeText = "TEST",
        badgeColor = "#2196F3",  // 蓝色
        priority = 2
    ),
    
    /** 预发布版 - 公开测试使用 */
    BETA(
        displayName = "预发布版",
        iconSuffix = "beta",
        badgeText = "BETA",
        badgeColor = "#9C27B0",  // 紫色
        priority = 3
    ),
    
    /** 正式版 - 生产环境使用 */
    PRODUCTION(
        displayName = "正式版",
        iconSuffix = "production",
        badgeText = null,       // 正式版无标识
        badgeColor = "#4CAF50",  // 绿色
        priority = 4
    );
    
    /**
     * 是否为正式发布版本
     */
    val isRelease: Boolean
        get() = this == PRODUCTION
    
    /**
     * 是否为预发布版本（包括dev、test、beta）
     */
    val isPrerelease: Boolean
        get() = this != PRODUCTION
    
    /**
     * 获取图标资源目录路径
     */
    fun getIconSourceDir(): String = "assets/icons/$iconSuffix"
    
    /**
     * 获取预发布标识（用于版本号）
     * 正式版返回null
     */
    fun getPrereleaseTag(): String? = when (this) {
        DEV -> "dev"
        TEST -> "test"
        BETA -> "beta"
        PRODUCTION -> null
    }
    
    companion object {
        /**
         * 默认发布阶段
         */
        val DEFAULT = PRODUCTION
        
        /**
         * 从字符串解析发布阶段
         * 支持名称、后缀、显示名称等多种格式
         * 
         * @param value 阶段名称或后缀
         * @return 对应的ReleaseStage，默认返回PRODUCTION
         */
        fun fromString(value: String): ReleaseStage {
            val normalized = value.lowercase().trim()
            return values().find { stage ->
                stage.name.lowercase() == normalized ||
                stage.iconSuffix.lowercase() == normalized ||
                stage.displayName == value ||
                stage.badgeText?.lowercase() == normalized
            } ?: PRODUCTION
        }
        
        /**
         * 安全解析，失败时返回null
         */
        fun fromStringOrNull(value: String): ReleaseStage? {
            val normalized = value.lowercase().trim()
            return values().find { stage ->
                stage.name.lowercase() == normalized ||
                stage.iconSuffix.lowercase() == normalized ||
                stage.displayName == value ||
                stage.badgeText?.lowercase() == normalized
            }
        }
        
        /**
         * 获取所有预发布阶段
         */
        fun getPrereleaseStages(): List<ReleaseStage> {
            return values().filter { it.isPrerelease }
        }
        
        /**
         * 按优先级排序的所有阶段
         */
        fun sortedByPriority(): List<ReleaseStage> {
            return values().sortedBy { it.priority }
        }
    }
}
