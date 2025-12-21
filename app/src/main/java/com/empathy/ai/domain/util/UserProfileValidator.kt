package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.UserProfile
import com.empathy.ai.domain.model.UserProfileDimension
import com.empathy.ai.domain.model.UserProfileValidationResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户画像验证器
 *
 * 提供用户画像数据的验证功能，包括标签验证、维度名称验证等。
 * 确保用户输入的数据符合业务规则。
 */
@Singleton
class UserProfileValidator @Inject constructor() {
    
    companion object {
        /** 标签最小长度 */
        const val TAG_MIN_LENGTH = 1
        
        /** 标签最大长度 */
        const val TAG_MAX_LENGTH = 50
        
        /** 维度名称最小长度 */
        const val DIMENSION_NAME_MIN_LENGTH = 2
        
        /** 维度名称最大长度 */
        const val DIMENSION_NAME_MAX_LENGTH = 20
        
        /** 单个维度最大标签数 */
        const val MAX_TAGS_PER_DIMENSION = 20
        
        /** 最大自定义维度数 */
        const val MAX_CUSTOM_DIMENSIONS = 10
        
        /** 需要过滤的特殊字符 */
        private val INVALID_CHARS = listOf('<', '>', '&', '\'', '"', '/', '\\')
    }
    
    /**
     * 验证标签内容
     *
     * @param tag 要验证的标签内容
     * @return 验证结果
     */
    fun validateTag(tag: String): UserProfileValidationResult {
        // 检查空值
        val trimmedTag = tag.trim()
        if (trimmedTag.isEmpty()) {
            return UserProfileValidationResult.TagEmpty
        }
        
        // 检查长度
        if (trimmedTag.length > TAG_MAX_LENGTH) {
            return UserProfileValidationResult.TagTooLong(
                maxLength = TAG_MAX_LENGTH,
                actualLength = trimmedTag.length
            )
        }
        
        // 检查非法字符
        val foundInvalidChars = trimmedTag.filter { it in INVALID_CHARS }.toList().distinct()
        if (foundInvalidChars.isNotEmpty()) {
            return UserProfileValidationResult.ContainsInvalidChars(foundInvalidChars)
        }
        
        return UserProfileValidationResult.Valid
    }
    
    /**
     * 验证维度名称
     *
     * @param name 要验证的维度名称
     * @return 验证结果
     */
    fun validateDimensionName(name: String): UserProfileValidationResult {
        // 检查空值
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return UserProfileValidationResult.DimensionNameEmpty
        }
        
        // 检查最小长度
        if (trimmedName.length < DIMENSION_NAME_MIN_LENGTH) {
            return UserProfileValidationResult.DimensionNameTooShort(
                minLength = DIMENSION_NAME_MIN_LENGTH,
                actualLength = trimmedName.length
            )
        }
        
        // 检查最大长度
        if (trimmedName.length > DIMENSION_NAME_MAX_LENGTH) {
            return UserProfileValidationResult.DimensionNameTooLong(
                maxLength = DIMENSION_NAME_MAX_LENGTH,
                actualLength = trimmedName.length
            )
        }
        
        // 检查非法字符
        val foundInvalidChars = trimmedName.filter { it in INVALID_CHARS }.toList().distinct()
        if (foundInvalidChars.isNotEmpty()) {
            return UserProfileValidationResult.ContainsInvalidChars(foundInvalidChars)
        }
        
        return UserProfileValidationResult.Valid
    }
    
    /**
     * 验证标签是否重复
     *
     * @param dimension 维度键名
     * @param tag 要验证的标签
     * @param existingTags 已存在的标签列表
     * @return 验证结果
     */
    fun validateTagNotDuplicate(
        dimension: String,
        tag: String,
        existingTags: List<String>
    ): UserProfileValidationResult {
        val trimmedTag = tag.trim()
        if (existingTags.any { it.equals(trimmedTag, ignoreCase = true) }) {
            return UserProfileValidationResult.TagDuplicate(trimmedTag)
        }
        return UserProfileValidationResult.Valid
    }
    
    /**
     * 验证维度名称是否重复
     *
     * @param name 要验证的维度名称
     * @param existingDimensions 已存在的维度名称列表
     * @return 验证结果
     */
    fun validateDimensionNotDuplicate(
        name: String,
        existingDimensions: List<String>
    ): UserProfileValidationResult {
        val trimmedName = name.trim()
        
        // 检查是否与基础维度重复
        if (UserProfileDimension.isBaseDimension(trimmedName)) {
            return UserProfileValidationResult.DimensionNameDuplicate(trimmedName)
        }
        
        // 检查是否与已有自定义维度重复
        if (existingDimensions.any { it.equals(trimmedName, ignoreCase = true) }) {
            return UserProfileValidationResult.DimensionNameDuplicate(trimmedName)
        }
        
        return UserProfileValidationResult.Valid
    }
    
    /**
     * 验证标签数量限制
     *
     * @param existingCount 当前标签数量
     * @return 验证结果
     */
    fun validateTagLimit(existingCount: Int): UserProfileValidationResult {
        if (existingCount >= MAX_TAGS_PER_DIMENSION) {
            return UserProfileValidationResult.TagLimitExceeded(
                maxCount = MAX_TAGS_PER_DIMENSION,
                currentCount = existingCount
            )
        }
        return UserProfileValidationResult.Valid
    }
    
    /**
     * 验证自定义维度数量限制
     *
     * @param existingCount 当前自定义维度数量
     * @return 验证结果
     */
    fun validateDimensionLimit(existingCount: Int): UserProfileValidationResult {
        if (existingCount >= MAX_CUSTOM_DIMENSIONS) {
            return UserProfileValidationResult.DimensionLimitExceeded(
                maxCount = MAX_CUSTOM_DIMENSIONS,
                currentCount = existingCount
            )
        }
        return UserProfileValidationResult.Valid
    }
    
    /**
     * 清理输入内容，过滤特殊字符
     *
     * @param input 原始输入
     * @return 清理后的输入
     */
    fun sanitizeInput(input: String): String {
        return input.trim().filterNot { it in INVALID_CHARS }
    }
    
    /**
     * 综合验证添加标签的所有条件
     *
     * @param profile 当前用户画像
     * @param dimensionKey 维度键名
     * @param tag 要添加的标签
     * @return 验证结果
     */
    fun validateAddTag(
        profile: UserProfile,
        dimensionKey: String,
        tag: String
    ): UserProfileValidationResult {
        // 1. 验证标签内容
        val tagValidation = validateTag(tag)
        if (!tagValidation.isValid()) {
            return tagValidation
        }
        
        // 2. 验证标签数量限制
        val existingTags = profile.getTagsForDimension(dimensionKey)
        val limitValidation = validateTagLimit(existingTags.size)
        if (!limitValidation.isValid()) {
            return limitValidation
        }
        
        // 3. 验证标签是否重复
        return validateTagNotDuplicate(dimensionKey, tag, existingTags)
    }
    
    /**
     * 综合验证添加自定义维度的所有条件
     *
     * @param profile 当前用户画像
     * @param dimensionName 维度名称
     * @return 验证结果
     */
    fun validateAddDimension(
        profile: UserProfile,
        dimensionName: String
    ): UserProfileValidationResult {
        // 1. 验证维度名称
        val nameValidation = validateDimensionName(dimensionName)
        if (!nameValidation.isValid()) {
            return nameValidation
        }
        
        // 2. 验证维度数量限制
        val limitValidation = validateDimensionLimit(profile.getCustomDimensionCount())
        if (!limitValidation.isValid()) {
            return limitValidation
        }
        
        // 3. 验证维度是否重复
        return validateDimensionNotDuplicate(
            dimensionName,
            profile.customDimensions.keys.toList()
        )
    }
}
