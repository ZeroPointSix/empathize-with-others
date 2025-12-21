package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.UserProfile
import com.empathy.ai.domain.model.UserProfileValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * UserProfileValidator 单元测试
 */
class UserProfileValidatorTest {
    
    private lateinit var validator: UserProfileValidator
    
    @Before
    fun setup() {
        validator = UserProfileValidator()
    }
    
    // ========== validateTag() 测试 ==========
    
    @Test
    fun `validateTag 正常标签返回Valid`() {
        val result = validator.validateTag("内向")
        assertTrue(result.isValid())
    }
    
    @Test
    fun `validateTag 空字符串返回TagEmpty`() {
        val result = validator.validateTag("")
        assertTrue(result is UserProfileValidationResult.TagEmpty)
    }
    
    @Test
    fun `validateTag 纯空格返回TagEmpty`() {
        val result = validator.validateTag("   ")
        assertTrue(result is UserProfileValidationResult.TagEmpty)
    }
    
    @Test
    fun `validateTag 超长标签返回TagTooLong`() {
        val longTag = "a".repeat(51)
        val result = validator.validateTag(longTag)
        assertTrue(result is UserProfileValidationResult.TagTooLong)
        assertEquals(50, (result as UserProfileValidationResult.TagTooLong).maxLength)
        assertEquals(51, result.actualLength)
    }
    
    @Test
    fun `validateTag 50字符标签返回Valid`() {
        val tag = "a".repeat(50)
        val result = validator.validateTag(tag)
        assertTrue(result.isValid())
    }
    
    @Test
    fun `validateTag 包含小于号返回ContainsInvalidChars`() {
        val result = validator.validateTag("标签<script>")
        assertTrue(result is UserProfileValidationResult.ContainsInvalidChars)
    }
    
    @Test
    fun `validateTag 包含大于号返回ContainsInvalidChars`() {
        val result = validator.validateTag("标签>内容")
        assertTrue(result is UserProfileValidationResult.ContainsInvalidChars)
    }
    
    @Test
    fun `validateTag 包含引号返回ContainsInvalidChars`() {
        val result = validator.validateTag("标签\"内容")
        assertTrue(result is UserProfileValidationResult.ContainsInvalidChars)
    }
    
    @Test
    fun `validateTag 包含斜杠返回ContainsInvalidChars`() {
        val result = validator.validateTag("标签/内容")
        assertTrue(result is UserProfileValidationResult.ContainsInvalidChars)
    }
    
    @Test
    fun `validateTag 包含反斜杠返回ContainsInvalidChars`() {
        val result = validator.validateTag("标签\\内容")
        assertTrue(result is UserProfileValidationResult.ContainsInvalidChars)
    }
    
    @Test
    fun `validateTag 包含和号返回ContainsInvalidChars`() {
        val result = validator.validateTag("标签&内容")
        assertTrue(result is UserProfileValidationResult.ContainsInvalidChars)
    }
    
    @Test
    fun `validateTag 包含单引号返回ContainsInvalidChars`() {
        val result = validator.validateTag("标签'内容")
        assertTrue(result is UserProfileValidationResult.ContainsInvalidChars)
    }
    
    // ========== validateDimensionName() 测试 ==========
    
    @Test
    fun `validateDimensionName 正常名称返回Valid`() {
        val result = validator.validateDimensionName("职业技能")
        assertTrue(result.isValid())
    }
    
    @Test
    fun `validateDimensionName 空字符串返回DimensionNameEmpty`() {
        val result = validator.validateDimensionName("")
        assertTrue(result is UserProfileValidationResult.DimensionNameEmpty)
    }
    
    @Test
    fun `validateDimensionName 纯空格返回DimensionNameEmpty`() {
        val result = validator.validateDimensionName("   ")
        assertTrue(result is UserProfileValidationResult.DimensionNameEmpty)
    }
    
    @Test
    fun `validateDimensionName 单字符返回DimensionNameTooShort`() {
        val result = validator.validateDimensionName("技")
        assertTrue(result is UserProfileValidationResult.DimensionNameTooShort)
        assertEquals(2, (result as UserProfileValidationResult.DimensionNameTooShort).minLength)
    }
    
    @Test
    fun `validateDimensionName 2字符返回Valid`() {
        val result = validator.validateDimensionName("技能")
        assertTrue(result.isValid())
    }
    
    @Test
    fun `validateDimensionName 超长名称返回DimensionNameTooLong`() {
        val longName = "a".repeat(21)
        val result = validator.validateDimensionName(longName)
        assertTrue(result is UserProfileValidationResult.DimensionNameTooLong)
        assertEquals(20, (result as UserProfileValidationResult.DimensionNameTooLong).maxLength)
    }
    
    @Test
    fun `validateDimensionName 20字符返回Valid`() {
        val name = "a".repeat(20)
        val result = validator.validateDimensionName(name)
        assertTrue(result.isValid())
    }
    
    @Test
    fun `validateDimensionName 包含非法字符返回ContainsInvalidChars`() {
        val result = validator.validateDimensionName("维度<名称")
        assertTrue(result is UserProfileValidationResult.ContainsInvalidChars)
    }
    
    // ========== validateTagNotDuplicate() 测试 ==========
    
    @Test
    fun `validateTagNotDuplicate 不重复返回Valid`() {
        val existingTags = listOf("内向", "敏感")
        val result = validator.validateTagNotDuplicate("PERSONALITY_TRAITS", "外向", existingTags)
        assertTrue(result.isValid())
    }
    
    @Test
    fun `validateTagNotDuplicate 完全相同返回TagDuplicate`() {
        val existingTags = listOf("内向", "敏感")
        val result = validator.validateTagNotDuplicate("PERSONALITY_TRAITS", "内向", existingTags)
        assertTrue(result is UserProfileValidationResult.TagDuplicate)
    }
    
    @Test
    fun `validateTagNotDuplicate 忽略大小写返回TagDuplicate`() {
        val existingTags = listOf("ABC", "DEF")
        val result = validator.validateTagNotDuplicate("PERSONALITY_TRAITS", "abc", existingTags)
        assertTrue(result is UserProfileValidationResult.TagDuplicate)
    }
    
    @Test
    fun `validateTagNotDuplicate 空列表返回Valid`() {
        val result = validator.validateTagNotDuplicate("PERSONALITY_TRAITS", "内向", emptyList())
        assertTrue(result.isValid())
    }
    
    // ========== validateDimensionNotDuplicate() 测试 ==========
    
    @Test
    fun `validateDimensionNotDuplicate 不重复返回Valid`() {
        val existingDimensions = listOf("职业技能", "语言能力")
        val result = validator.validateDimensionNotDuplicate("学历背景", existingDimensions)
        assertTrue(result.isValid())
    }
    
    @Test
    fun `validateDimensionNotDuplicate 与自定义维度重复返回DimensionNameDuplicate`() {
        val existingDimensions = listOf("职业技能", "语言能力")
        val result = validator.validateDimensionNotDuplicate("职业技能", existingDimensions)
        assertTrue(result is UserProfileValidationResult.DimensionNameDuplicate)
    }
    
    @Test
    fun `validateDimensionNotDuplicate 忽略大小写返回DimensionNameDuplicate`() {
        val existingDimensions = listOf("ABC", "DEF")
        val result = validator.validateDimensionNotDuplicate("abc", existingDimensions)
        assertTrue(result is UserProfileValidationResult.DimensionNameDuplicate)
    }
    
    // ========== validateTagLimit() 测试 ==========
    
    @Test
    fun `validateTagLimit 未达上限返回Valid`() {
        val result = validator.validateTagLimit(19)
        assertTrue(result.isValid())
    }
    
    @Test
    fun `validateTagLimit 达到上限返回TagLimitExceeded`() {
        val result = validator.validateTagLimit(20)
        assertTrue(result is UserProfileValidationResult.TagLimitExceeded)
        assertEquals(20, (result as UserProfileValidationResult.TagLimitExceeded).maxCount)
    }
    
    @Test
    fun `validateTagLimit 超过上限返回TagLimitExceeded`() {
        val result = validator.validateTagLimit(25)
        assertTrue(result is UserProfileValidationResult.TagLimitExceeded)
    }
    
    // ========== validateDimensionLimit() 测试 ==========
    
    @Test
    fun `validateDimensionLimit 未达上限返回Valid`() {
        val result = validator.validateDimensionLimit(9)
        assertTrue(result.isValid())
    }
    
    @Test
    fun `validateDimensionLimit 达到上限返回DimensionLimitExceeded`() {
        val result = validator.validateDimensionLimit(10)
        assertTrue(result is UserProfileValidationResult.DimensionLimitExceeded)
        assertEquals(10, (result as UserProfileValidationResult.DimensionLimitExceeded).maxCount)
    }
    
    // ========== sanitizeInput() 测试 ==========
    
    @Test
    fun `sanitizeInput 正常输入不变`() {
        val result = validator.sanitizeInput("正常内容")
        assertEquals("正常内容", result)
    }
    
    @Test
    fun `sanitizeInput 去除首尾空格`() {
        val result = validator.sanitizeInput("  内容  ")
        assertEquals("内容", result)
    }
    
    @Test
    fun `sanitizeInput 过滤小于号`() {
        val result = validator.sanitizeInput("内容<script>")
        assertEquals("内容script", result)
    }
    
    @Test
    fun `sanitizeInput 过滤大于号`() {
        val result = validator.sanitizeInput("内容>结束")
        assertEquals("内容结束", result)
    }
    
    @Test
    fun `sanitizeInput 过滤引号`() {
        val result = validator.sanitizeInput("内容\"引号")
        assertEquals("内容引号", result)
    }
    
    @Test
    fun `sanitizeInput 过滤斜杠`() {
        val result = validator.sanitizeInput("内容/斜杠")
        assertEquals("内容斜杠", result)
    }
    
    @Test
    fun `sanitizeInput 过滤反斜杠`() {
        val result = validator.sanitizeInput("内容\\反斜杠")
        assertEquals("内容反斜杠", result)
    }
    
    @Test
    fun `sanitizeInput 过滤和号`() {
        val result = validator.sanitizeInput("内容&和号")
        assertEquals("内容和号", result)
    }
    
    @Test
    fun `sanitizeInput 过滤单引号`() {
        val result = validator.sanitizeInput("内容'单引号")
        assertEquals("内容单引号", result)
    }
    
    @Test
    fun `sanitizeInput 过滤多个非法字符`() {
        val result = validator.sanitizeInput("<script>alert('xss')</script>")
        assertEquals("scriptalert(xss)script", result)
    }
    
    // ========== validateAddTag() 综合测试 ==========
    
    @Test
    fun `validateAddTag 正常添加返回Valid`() {
        val profile = UserProfile(personalityTraits = listOf("内向"))
        val result = validator.validateAddTag(profile, "PERSONALITY_TRAITS", "外向")
        assertTrue(result.isValid())
    }
    
    @Test
    fun `validateAddTag 空标签返回TagEmpty`() {
        val profile = UserProfile()
        val result = validator.validateAddTag(profile, "PERSONALITY_TRAITS", "")
        assertTrue(result is UserProfileValidationResult.TagEmpty)
    }
    
    @Test
    fun `validateAddTag 重复标签返回TagDuplicate`() {
        val profile = UserProfile(personalityTraits = listOf("内向"))
        val result = validator.validateAddTag(profile, "PERSONALITY_TRAITS", "内向")
        assertTrue(result is UserProfileValidationResult.TagDuplicate)
    }
    
    @Test
    fun `validateAddTag 达到上限返回TagLimitExceeded`() {
        val tags = (1..20).map { "标签$it" }
        val profile = UserProfile(personalityTraits = tags)
        val result = validator.validateAddTag(profile, "PERSONALITY_TRAITS", "新标签")
        assertTrue(result is UserProfileValidationResult.TagLimitExceeded)
    }
    
    // ========== validateAddDimension() 综合测试 ==========
    
    @Test
    fun `validateAddDimension 正常添加返回Valid`() {
        val profile = UserProfile()
        val result = validator.validateAddDimension(profile, "职业技能")
        assertTrue(result.isValid())
    }
    
    @Test
    fun `validateAddDimension 空名称返回DimensionNameEmpty`() {
        val profile = UserProfile()
        val result = validator.validateAddDimension(profile, "")
        assertTrue(result is UserProfileValidationResult.DimensionNameEmpty)
    }
    
    @Test
    fun `validateAddDimension 重复名称返回DimensionNameDuplicate`() {
        val profile = UserProfile(customDimensions = mapOf("职业技能" to emptyList()))
        val result = validator.validateAddDimension(profile, "职业技能")
        assertTrue(result is UserProfileValidationResult.DimensionNameDuplicate)
    }
    
    @Test
    fun `validateAddDimension 达到上限返回DimensionLimitExceeded`() {
        val dimensions = (1..10).associate { "维度$it" to emptyList<String>() }
        val profile = UserProfile(customDimensions = dimensions)
        val result = validator.validateAddDimension(profile, "新维度")
        assertTrue(result is UserProfileValidationResult.DimensionLimitExceeded)
    }
    
    // ========== 常量测试 ==========
    
    @Test
    fun `TAG_MIN_LENGTH 值正确`() {
        assertEquals(1, UserProfileValidator.TAG_MIN_LENGTH)
    }
    
    @Test
    fun `TAG_MAX_LENGTH 值正确`() {
        assertEquals(50, UserProfileValidator.TAG_MAX_LENGTH)
    }
    
    @Test
    fun `DIMENSION_NAME_MIN_LENGTH 值正确`() {
        assertEquals(2, UserProfileValidator.DIMENSION_NAME_MIN_LENGTH)
    }
    
    @Test
    fun `DIMENSION_NAME_MAX_LENGTH 值正确`() {
        assertEquals(20, UserProfileValidator.DIMENSION_NAME_MAX_LENGTH)
    }
    
    @Test
    fun `MAX_TAGS_PER_DIMENSION 值正确`() {
        assertEquals(20, UserProfileValidator.MAX_TAGS_PER_DIMENSION)
    }
    
    @Test
    fun `MAX_CUSTOM_DIMENSIONS 值正确`() {
        assertEquals(10, UserProfileValidator.MAX_CUSTOM_DIMENSIONS)
    }
}
