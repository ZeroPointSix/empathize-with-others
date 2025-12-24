package com.empathy.ai.domain.model

/**
 * 用户画像基础维度枚举
 *
 * 定义5个基础维度，每个维度包含显示名称、描述和预设标签。
 * 用于UI展示和标签快速选择。
 *
 * @property displayName 维度的中文显示名称
 * @property description 维度的描述说明
 * @property presetTags 预设标签列表，供用户快速选择
 */
enum class UserProfileDimension(
    val displayName: String,
    val description: String,
    val presetTags: List<String>
) {
    /**
     * 性格特点维度
     */
    PERSONALITY_TRAITS(
        displayName = "性格特点",
        description = "描述你的性格特征",
        presetTags = listOf(
            "内向", "外向", "理性", "感性", "乐观", "谨慎",
            "热情", "冷静", "细心", "大方", "幽默", "严肃"
        )
    ),
    
    /**
     * 价值观维度
     */
    VALUES(
        displayName = "价值观",
        description = "你最看重的事物",
        presetTags = listOf(
            "重视家庭", "追求事业", "注重健康", "珍惜友情",
            "追求自由", "重视诚信", "追求成长", "享受生活"
        )
    ),
    
    /**
     * 兴趣爱好维度
     */
    INTERESTS(
        displayName = "兴趣爱好",
        description = "你喜欢做的事情",
        presetTags = listOf(
            "阅读", "运动", "旅行", "音乐", "电影", "游戏",
            "美食", "摄影", "绘画", "写作", "编程", "园艺"
        )
    ),
    
    /**
     * 沟通风格维度
     */
    COMMUNICATION_STYLE(
        displayName = "沟通风格",
        description = "你习惯的沟通方式",
        presetTags = listOf(
            "直接", "委婉", "幽默", "严肃", "主动", "被动",
            "倾听型", "表达型", "逻辑型", "情感型"
        )
    ),
    
    /**
     * 社交偏好维度
     */
    SOCIAL_PREFERENCES(
        displayName = "社交偏好",
        description = "你喜欢的社交方式",
        presetTags = listOf(
            "大群体", "小群体", "一对一", "线上社交", "线下社交",
            "深度交流", "轻松闲聊", "工作社交", "兴趣社交"
        )
    );
    
    companion object {
        /**
         * 根据键名获取维度枚举
         *
         * @param key 维度键名（枚举的name）
         * @return 对应的维度枚举，如果不存在返回null
         */
        fun fromKey(key: String): UserProfileDimension? {
            return entries.find { it.name == key }
        }
        
        /**
         * 根据显示名称获取维度枚举
         *
         * @param displayName 维度的中文显示名称
         * @return 对应的维度枚举，如果不存在返回null
         */
        fun fromDisplayName(displayName: String): UserProfileDimension? {
            return entries.find { it.displayName == displayName }
        }
        
        /**
         * 获取所有基础维度的键名列表
         */
        fun getAllKeys(): List<String> = entries.map { it.name }
        
        /**
         * 获取所有基础维度的显示名称列表
         */
        fun getAllDisplayNames(): List<String> = entries.map { it.displayName }
        
        /**
         * 检查给定的名称是否是基础维度名称
         *
         * @param name 要检查的名称
         * @return 如果是基础维度名称返回true
         */
        fun isBaseDimension(name: String): Boolean {
            return entries.any { it.name == name || it.displayName == name }
        }
    }
    
    /**
     * 获取未被使用的预设标签
     *
     * @param usedTags 已使用的标签列表
     * @return 未被使用的预设标签列表
     */
    fun getAvailablePresetTags(usedTags: List<String>): List<String> {
        return presetTags.filter { it !in usedTags }
    }
}
