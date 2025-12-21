package com.empathy.ai.domain.model

/**
 * 用户画像验证结果密封类
 *
 * 用于表示用户画像数据验证的各种结果状态。
 * 包括验证通过和各种验证失败的情况。
 */
sealed class UserProfileValidationResult {
    
    /**
     * 验证通过
     */
    data object Valid : UserProfileValidationResult()
    
    /**
     * 标签内容为空
     */
    data object TagEmpty : UserProfileValidationResult()
    
    /**
     * 标签内容过长
     *
     * @property maxLength 允许的最大长度
     * @property actualLength 实际长度
     */
    data class TagTooLong(
        val maxLength: Int,
        val actualLength: Int
    ) : UserProfileValidationResult()
    
    /**
     * 标签重复
     *
     * @property tag 重复的标签内容
     */
    data class TagDuplicate(val tag: String) : UserProfileValidationResult()
    
    /**
     * 维度名称为空
     */
    data object DimensionNameEmpty : UserProfileValidationResult()
    
    /**
     * 维度名称过长
     *
     * @property maxLength 允许的最大长度
     * @property actualLength 实际长度
     */
    data class DimensionNameTooLong(
        val maxLength: Int,
        val actualLength: Int
    ) : UserProfileValidationResult()
    
    /**
     * 维度名称过短
     *
     * @property minLength 允许的最小长度
     * @property actualLength 实际长度
     */
    data class DimensionNameTooShort(
        val minLength: Int,
        val actualLength: Int
    ) : UserProfileValidationResult()
    
    /**
     * 维度名称重复
     *
     * @property name 重复的维度名称
     */
    data class DimensionNameDuplicate(val name: String) : UserProfileValidationResult()
    
    /**
     * 标签数量超出限制
     *
     * @property maxCount 允许的最大数量
     * @property currentCount 当前数量
     */
    data class TagLimitExceeded(
        val maxCount: Int,
        val currentCount: Int
    ) : UserProfileValidationResult()
    
    /**
     * 自定义维度数量超出限制
     *
     * @property maxCount 允许的最大数量
     * @property currentCount 当前数量
     */
    data class DimensionLimitExceeded(
        val maxCount: Int,
        val currentCount: Int
    ) : UserProfileValidationResult()
    
    /**
     * 包含非法字符
     *
     * @property invalidChars 检测到的非法字符列表
     */
    data class ContainsInvalidChars(val invalidChars: List<Char>) : UserProfileValidationResult()
    
    /**
     * 判断验证是否通过
     */
    fun isValid(): Boolean = this is Valid
    
    /**
     * 获取用户友好的错误消息
     *
     * @return 中文错误消息，验证通过时返回空字符串
     */
    fun getErrorMessage(): String {
        return when (this) {
            is Valid -> ""
            is TagEmpty -> "标签内容不能为空"
            is TagTooLong -> "标签不能超过${maxLength}个字符（当前${actualLength}个）"
            is TagDuplicate -> "标签「$tag」已存在"
            is DimensionNameEmpty -> "维度名称不能为空"
            is DimensionNameTooLong -> "维度名称不能超过${maxLength}个字符（当前${actualLength}个）"
            is DimensionNameTooShort -> "维度名称至少需要${minLength}个字符（当前${actualLength}个）"
            is DimensionNameDuplicate -> "维度「$name」已存在"
            is TagLimitExceeded -> "标签数量已达上限（最多${maxCount}个）"
            is DimensionLimitExceeded -> "自定义维度已达上限（最多${maxCount}个）"
            is ContainsInvalidChars -> "包含非法字符：${invalidChars.joinToString("")}"
        }
    }
}
