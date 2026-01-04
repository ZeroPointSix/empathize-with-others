package com.empathy.ai.domain.model

/**
 * 用户画像领域模型
 *
 * 存储用户自身的性格特点、价值观、兴趣爱好等信息，
 * 用于AI分析时提供个性化上下文。
 *
 * 业务背景 (PRD-00012):
 * - 用户画像与联系人画像互补，提供"我"的背景信息
 * - AI军师分析时会考虑用户的性格特点，提供适配的建议
 * - 支持5个基础维度 + 自定义维度的灵活配置
 * - 单例模式（固定ID），确保系统中只有一个用户画像
 *
 * 设计决策 (TDD-00012):
 * - 固定ID="user_profile"，使用单例模式
 * - 5个基础维度：personalityTraits/values/interests/communicationStyle/socialPreferences
 * - 支持最多10个自定义维度，每个维度最多20个标签
 * - 不可变性设计，所有修改操作返回新实例
 *
 * 任务追踪: FD-00012/用户画像功能设计
 *
 * @property id 唯一标识，固定为"user_profile"
 * @property personalityTraits 性格特点标签列表
 * @property values 价值观标签列表
 * @property interests 兴趣爱好标签列表
 * @property communicationStyle 沟通风格标签列表
 * @property socialPreferences 社交偏好标签列表
 * @property customDimensions 自定义维度，键为维度名称，值为标签列表
 * @property createdAt 创建时间戳
 * @property updatedAt 最后更新时间戳
 */
data class UserProfile(
    val id: String = DEFAULT_ID,
    val personalityTraits: List<String> = emptyList(),
    val values: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    val communicationStyle: List<String> = emptyList(),
    val socialPreferences: List<String> = emptyList(),
    val customDimensions: Map<String, List<String>> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_ID = "user_profile"
        
        /** 基础维度数量 */
        const val BASE_DIMENSION_COUNT = 5
        
        /** 单个维度最大标签数 */
        const val MAX_TAGS_PER_DIMENSION = 20
        
        /** 最大自定义维度数 */
        const val MAX_CUSTOM_DIMENSIONS = 10
    }
    
    /**
     * 判断画像是否为空
     * 当所有维度都没有标签时返回true
     */
    fun isEmpty(): Boolean {
        return personalityTraits.isEmpty() &&
            values.isEmpty() &&
            interests.isEmpty() &&
            communicationStyle.isEmpty() &&
            socialPreferences.isEmpty() &&
            customDimensions.values.all { it.isEmpty() }
    }
    
    /**
     * 计算画像完整度百分比
     * 基于5个基础维度是否有标签来计算
     * 每个有标签的维度贡献20%
     */
    fun getCompleteness(): Int {
        var filledDimensions = 0
        
        if (personalityTraits.isNotEmpty()) filledDimensions++
        if (values.isNotEmpty()) filledDimensions++
        if (interests.isNotEmpty()) filledDimensions++
        if (communicationStyle.isNotEmpty()) filledDimensions++
        if (socialPreferences.isNotEmpty()) filledDimensions++
        
        return (filledDimensions * 100) / BASE_DIMENSION_COUNT
    }
    
    /**
     * 获取所有标签的总数
     * 包括基础维度和自定义维度的所有标签
     */
    fun getTotalTagCount(): Int {
        val baseTags = personalityTraits.size +
            values.size +
            interests.size +
            communicationStyle.size +
            socialPreferences.size
        
        val customTags = customDimensions.values.sumOf { it.size }
        
        return baseTags + customTags
    }
    
    /**
     * 根据维度名称获取标签列表
     *
     * @param dimensionKey 维度键名（基础维度使用枚举的name，自定义维度使用维度名称）
     * @return 标签列表，如果维度不存在返回空列表
     */
    fun getTagsForDimension(dimensionKey: String): List<String> {
        return when (dimensionKey) {
            UserProfileDimension.PERSONALITY_TRAITS.name -> personalityTraits
            UserProfileDimension.VALUES.name -> values
            UserProfileDimension.INTERESTS.name -> interests
            UserProfileDimension.COMMUNICATION_STYLE.name -> communicationStyle
            UserProfileDimension.SOCIAL_PREFERENCES.name -> socialPreferences
            else -> customDimensions[dimensionKey] ?: emptyList()
        }
    }
    
    /**
     * 添加标签到指定维度
     *
     * @param dimensionKey 维度键名
     * @param tag 要添加的标签
     * @return 添加标签后的新UserProfile实例
     */
    fun addTag(dimensionKey: String, tag: String): UserProfile {
        val currentTime = System.currentTimeMillis()
        
        return when (dimensionKey) {
            UserProfileDimension.PERSONALITY_TRAITS.name -> copy(
                personalityTraits = personalityTraits + tag,
                updatedAt = currentTime
            )
            UserProfileDimension.VALUES.name -> copy(
                values = values + tag,
                updatedAt = currentTime
            )
            UserProfileDimension.INTERESTS.name -> copy(
                interests = interests + tag,
                updatedAt = currentTime
            )
            UserProfileDimension.COMMUNICATION_STYLE.name -> copy(
                communicationStyle = communicationStyle + tag,
                updatedAt = currentTime
            )
            UserProfileDimension.SOCIAL_PREFERENCES.name -> copy(
                socialPreferences = socialPreferences + tag,
                updatedAt = currentTime
            )
            else -> {
                val updatedCustom = customDimensions.toMutableMap()
                val currentTags = updatedCustom[dimensionKey] ?: emptyList()
                updatedCustom[dimensionKey] = currentTags + tag
                copy(customDimensions = updatedCustom, updatedAt = currentTime)
            }
        }
    }
    
    /**
     * 从指定维度移除标签
     *
     * @param dimensionKey 维度键名
     * @param tag 要移除的标签
     * @return 移除标签后的新UserProfile实例
     */
    fun removeTag(dimensionKey: String, tag: String): UserProfile {
        val currentTime = System.currentTimeMillis()
        
        return when (dimensionKey) {
            UserProfileDimension.PERSONALITY_TRAITS.name -> copy(
                personalityTraits = personalityTraits - tag,
                updatedAt = currentTime
            )
            UserProfileDimension.VALUES.name -> copy(
                values = values - tag,
                updatedAt = currentTime
            )
            UserProfileDimension.INTERESTS.name -> copy(
                interests = interests - tag,
                updatedAt = currentTime
            )
            UserProfileDimension.COMMUNICATION_STYLE.name -> copy(
                communicationStyle = communicationStyle - tag,
                updatedAt = currentTime
            )
            UserProfileDimension.SOCIAL_PREFERENCES.name -> copy(
                socialPreferences = socialPreferences - tag,
                updatedAt = currentTime
            )
            else -> {
                val updatedCustom = customDimensions.toMutableMap()
                val currentTags = updatedCustom[dimensionKey] ?: emptyList()
                updatedCustom[dimensionKey] = currentTags - tag
                copy(customDimensions = updatedCustom, updatedAt = currentTime)
            }
        }
    }
    
    /**
     * 添加自定义维度
     *
     * @param dimensionName 维度名称
     * @return 添加维度后的新UserProfile实例
     */
    fun addCustomDimension(dimensionName: String): UserProfile {
        val updatedCustom = customDimensions.toMutableMap()
        updatedCustom[dimensionName] = emptyList()
        return copy(
            customDimensions = updatedCustom,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * 移除自定义维度（包含其所有标签）
     *
     * @param dimensionName 维度名称
     * @return 移除维度后的新UserProfile实例
     */
    fun removeCustomDimension(dimensionName: String): UserProfile {
        val updatedCustom = customDimensions.toMutableMap()
        updatedCustom.remove(dimensionName)
        return copy(
            customDimensions = updatedCustom,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * 获取自定义维度数量
     */
    fun getCustomDimensionCount(): Int = customDimensions.size
    
    /**
     * 检查是否可以添加更多自定义维度
     */
    fun canAddCustomDimension(): Boolean = customDimensions.size < MAX_CUSTOM_DIMENSIONS
    
    /**
     * 检查指定维度是否可以添加更多标签
     *
     * @param dimensionKey 维度键名
     */
    fun canAddTagToDimension(dimensionKey: String): Boolean {
        return getTagsForDimension(dimensionKey).size < MAX_TAGS_PER_DIMENSION
    }
}
