package com.empathy.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UserProfile 领域模型单元测试
 */
class UserProfileTest {
    
    // ========== isEmpty() 测试 ==========
    
    @Test
    fun `isEmpty 空画像返回true`() {
        val profile = UserProfile()
        assertTrue(profile.isEmpty())
    }
    
    @Test
    fun `isEmpty 有性格特点标签返回false`() {
        val profile = UserProfile(personalityTraits = listOf("内向"))
        assertFalse(profile.isEmpty())
    }
    
    @Test
    fun `isEmpty 有价值观标签返回false`() {
        val profile = UserProfile(values = listOf("诚实"))
        assertFalse(profile.isEmpty())
    }
    
    @Test
    fun `isEmpty 有兴趣爱好标签返回false`() {
        val profile = UserProfile(interests = listOf("阅读"))
        assertFalse(profile.isEmpty())
    }
    
    @Test
    fun `isEmpty 有沟通风格标签返回false`() {
        val profile = UserProfile(communicationStyle = listOf("直接"))
        assertFalse(profile.isEmpty())
    }
    
    @Test
    fun `isEmpty 有社交偏好标签返回false`() {
        val profile = UserProfile(socialPreferences = listOf("小圈子"))
        assertFalse(profile.isEmpty())
    }
    
    @Test
    fun `isEmpty 有自定义维度标签返回false`() {
        val profile = UserProfile(customDimensions = mapOf("职业技能" to listOf("编程")))
        assertFalse(profile.isEmpty())
    }
    
    @Test
    fun `isEmpty 自定义维度存在但无标签返回true`() {
        val profile = UserProfile(customDimensions = mapOf("职业技能" to emptyList()))
        assertTrue(profile.isEmpty())
    }
    
    // ========== getCompleteness() 测试 ==========
    
    @Test
    fun `getCompleteness 空画像返回0`() {
        val profile = UserProfile()
        assertEquals(0, profile.getCompleteness())
    }
    
    @Test
    fun `getCompleteness 一个维度有标签返回20`() {
        val profile = UserProfile(personalityTraits = listOf("内向"))
        assertEquals(20, profile.getCompleteness())
    }
    
    @Test
    fun `getCompleteness 两个维度有标签返回40`() {
        val profile = UserProfile(
            personalityTraits = listOf("内向"),
            values = listOf("诚实")
        )
        assertEquals(40, profile.getCompleteness())
    }
    
    @Test
    fun `getCompleteness 三个维度有标签返回60`() {
        val profile = UserProfile(
            personalityTraits = listOf("内向"),
            values = listOf("诚实"),
            interests = listOf("阅读")
        )
        assertEquals(60, profile.getCompleteness())
    }
    
    @Test
    fun `getCompleteness 四个维度有标签返回80`() {
        val profile = UserProfile(
            personalityTraits = listOf("内向"),
            values = listOf("诚实"),
            interests = listOf("阅读"),
            communicationStyle = listOf("直接")
        )
        assertEquals(80, profile.getCompleteness())
    }
    
    @Test
    fun `getCompleteness 五个维度都有标签返回100`() {
        val profile = UserProfile(
            personalityTraits = listOf("内向"),
            values = listOf("诚实"),
            interests = listOf("阅读"),
            communicationStyle = listOf("直接"),
            socialPreferences = listOf("小圈子")
        )
        assertEquals(100, profile.getCompleteness())
    }
    
    @Test
    fun `getCompleteness 自定义维度不影响完整度计算`() {
        val profile = UserProfile(
            customDimensions = mapOf("职业技能" to listOf("编程", "设计"))
        )
        assertEquals(0, profile.getCompleteness())
    }
    
    // ========== getTotalTagCount() 测试 ==========
    
    @Test
    fun `getTotalTagCount 空画像返回0`() {
        val profile = UserProfile()
        assertEquals(0, profile.getTotalTagCount())
    }
    
    @Test
    fun `getTotalTagCount 计算基础维度标签总数`() {
        val profile = UserProfile(
            personalityTraits = listOf("内向", "敏感"),
            values = listOf("诚实"),
            interests = listOf("阅读", "音乐", "电影")
        )
        assertEquals(6, profile.getTotalTagCount())
    }
    
    @Test
    fun `getTotalTagCount 计算自定义维度标签总数`() {
        val profile = UserProfile(
            customDimensions = mapOf(
                "职业技能" to listOf("编程", "设计"),
                "语言能力" to listOf("中文", "英文", "日文")
            )
        )
        assertEquals(5, profile.getTotalTagCount())
    }
    
    @Test
    fun `getTotalTagCount 计算所有维度标签总数`() {
        val profile = UserProfile(
            personalityTraits = listOf("内向"),
            values = listOf("诚实"),
            customDimensions = mapOf("职业技能" to listOf("编程"))
        )
        assertEquals(3, profile.getTotalTagCount())
    }
    
    // ========== getTagsForDimension() 测试 ==========
    
    @Test
    fun `getTagsForDimension 获取性格特点标签`() {
        val tags = listOf("内向", "敏感")
        val profile = UserProfile(personalityTraits = tags)
        assertEquals(tags, profile.getTagsForDimension("PERSONALITY_TRAITS"))
    }
    
    @Test
    fun `getTagsForDimension 获取价值观标签`() {
        val tags = listOf("诚实", "正直")
        val profile = UserProfile(values = tags)
        assertEquals(tags, profile.getTagsForDimension("VALUES"))
    }
    
    @Test
    fun `getTagsForDimension 获取兴趣爱好标签`() {
        val tags = listOf("阅读", "音乐")
        val profile = UserProfile(interests = tags)
        assertEquals(tags, profile.getTagsForDimension("INTERESTS"))
    }
    
    @Test
    fun `getTagsForDimension 获取沟通风格标签`() {
        val tags = listOf("直接", "幽默")
        val profile = UserProfile(communicationStyle = tags)
        assertEquals(tags, profile.getTagsForDimension("COMMUNICATION_STYLE"))
    }
    
    @Test
    fun `getTagsForDimension 获取社交偏好标签`() {
        val tags = listOf("小圈子", "深度交流")
        val profile = UserProfile(socialPreferences = tags)
        assertEquals(tags, profile.getTagsForDimension("SOCIAL_PREFERENCES"))
    }
    
    @Test
    fun `getTagsForDimension 获取自定义维度标签`() {
        val tags = listOf("编程", "设计")
        val profile = UserProfile(customDimensions = mapOf("职业技能" to tags))
        assertEquals(tags, profile.getTagsForDimension("职业技能"))
    }
    
    @Test
    fun `getTagsForDimension 不存在的维度返回空列表`() {
        val profile = UserProfile()
        assertEquals(emptyList<String>(), profile.getTagsForDimension("不存在的维度"))
    }
    
    // ========== addTag() 测试 ==========
    
    @Test
    fun `addTag 添加标签到性格特点`() {
        val profile = UserProfile()
        val updated = profile.addTag("PERSONALITY_TRAITS", "内向")
        assertEquals(listOf("内向"), updated.personalityTraits)
    }
    
    @Test
    fun `addTag 添加标签到自定义维度`() {
        val profile = UserProfile(customDimensions = mapOf("职业技能" to emptyList()))
        val updated = profile.addTag("职业技能", "编程")
        assertEquals(listOf("编程"), updated.customDimensions["职业技能"])
    }
    
    @Test
    fun `addTag 更新updatedAt时间戳`() {
        val profile = UserProfile(updatedAt = 1000L)
        val updated = profile.addTag("PERSONALITY_TRAITS", "内向")
        assertTrue(updated.updatedAt > 1000L)
    }
    
    // ========== removeTag() 测试 ==========
    
    @Test
    fun `removeTag 从性格特点移除标签`() {
        val profile = UserProfile(personalityTraits = listOf("内向", "敏感"))
        val updated = profile.removeTag("PERSONALITY_TRAITS", "内向")
        assertEquals(listOf("敏感"), updated.personalityTraits)
    }
    
    @Test
    fun `removeTag 从自定义维度移除标签`() {
        val profile = UserProfile(customDimensions = mapOf("职业技能" to listOf("编程", "设计")))
        val updated = profile.removeTag("职业技能", "编程")
        assertEquals(listOf("设计"), updated.customDimensions["职业技能"])
    }
    
    @Test
    fun `removeTag 移除不存在的标签不报错`() {
        val profile = UserProfile(personalityTraits = listOf("内向"))
        val updated = profile.removeTag("PERSONALITY_TRAITS", "不存在")
        assertEquals(listOf("内向"), updated.personalityTraits)
    }
    
    // ========== addCustomDimension() 测试 ==========
    
    @Test
    fun `addCustomDimension 添加新维度`() {
        val profile = UserProfile()
        val updated = profile.addCustomDimension("职业技能")
        assertTrue(updated.customDimensions.containsKey("职业技能"))
        assertEquals(emptyList<String>(), updated.customDimensions["职业技能"])
    }
    
    @Test
    fun `addCustomDimension 更新updatedAt时间戳`() {
        val profile = UserProfile(updatedAt = 1000L)
        val updated = profile.addCustomDimension("职业技能")
        assertTrue(updated.updatedAt > 1000L)
    }
    
    // ========== removeCustomDimension() 测试 ==========
    
    @Test
    fun `removeCustomDimension 移除维度及其标签`() {
        val profile = UserProfile(customDimensions = mapOf("职业技能" to listOf("编程")))
        val updated = profile.removeCustomDimension("职业技能")
        assertFalse(updated.customDimensions.containsKey("职业技能"))
    }
    
    @Test
    fun `removeCustomDimension 移除不存在的维度不报错`() {
        val profile = UserProfile()
        val updated = profile.removeCustomDimension("不存在")
        assertTrue(updated.customDimensions.isEmpty())
    }
    
    // ========== getCustomDimensionCount() 测试 ==========
    
    @Test
    fun `getCustomDimensionCount 返回自定义维度数量`() {
        val profile = UserProfile(customDimensions = mapOf(
            "职业技能" to listOf("编程"),
            "语言能力" to listOf("中文")
        ))
        assertEquals(2, profile.getCustomDimensionCount())
    }
    
    // ========== canAddCustomDimension() 测试 ==========
    
    @Test
    fun `canAddCustomDimension 未达上限返回true`() {
        val profile = UserProfile()
        assertTrue(profile.canAddCustomDimension())
    }
    
    @Test
    fun `canAddCustomDimension 达到上限返回false`() {
        val dimensions = (1..10).associate { "维度$it" to emptyList<String>() }
        val profile = UserProfile(customDimensions = dimensions)
        assertFalse(profile.canAddCustomDimension())
    }
    
    // ========== canAddTagToDimension() 测试 ==========
    
    @Test
    fun `canAddTagToDimension 未达上限返回true`() {
        val profile = UserProfile(personalityTraits = listOf("内向"))
        assertTrue(profile.canAddTagToDimension("PERSONALITY_TRAITS"))
    }
    
    @Test
    fun `canAddTagToDimension 达到上限返回false`() {
        val tags = (1..20).map { "标签$it" }
        val profile = UserProfile(personalityTraits = tags)
        assertFalse(profile.canAddTagToDimension("PERSONALITY_TRAITS"))
    }
    
    // ========== 常量测试 ==========
    
    @Test
    fun `DEFAULT_ID 值正确`() {
        assertEquals("user_profile", UserProfile.DEFAULT_ID)
    }
    
    @Test
    fun `BASE_DIMENSION_COUNT 值正确`() {
        assertEquals(5, UserProfile.BASE_DIMENSION_COUNT)
    }
    
    @Test
    fun `MAX_TAGS_PER_DIMENSION 值正确`() {
        assertEquals(20, UserProfile.MAX_TAGS_PER_DIMENSION)
    }
    
    @Test
    fun `MAX_CUSTOM_DIMENSIONS 值正确`() {
        assertEquals(10, UserProfile.MAX_CUSTOM_DIMENSIONS)
    }
}
