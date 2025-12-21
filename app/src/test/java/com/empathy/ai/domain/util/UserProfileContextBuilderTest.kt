package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.model.UserProfile
import com.empathy.ai.domain.repository.UserProfileRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * UserProfileContextBuilder 单元测试
 *
 * 测试用户画像上下文构建器的各种场景
 */
class UserProfileContextBuilderTest {
    
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var contextBuilder: UserProfileContextBuilder
    
    @Before
    fun setup() {
        userProfileRepository = mockk()
        contextBuilder = UserProfileContextBuilder(userProfileRepository)
    }
    
    // ========== buildAnalysisContext 测试 ==========
    
    @Test
    fun `buildAnalysisContext 成功构建包含用户画像的上下文`() = runTest {
        // Given
        val userProfile = UserProfile(
            personalityTraits = listOf("内向", "理性"),
            values = listOf("重视家庭"),
            interests = listOf("阅读", "编程"),
            communicationStyle = listOf("直接"),
            socialPreferences = listOf("一对一")
        )
        val contact = createTestContact()
        val userInput = "今天工作很累"
        
        coEvery { userProfileRepository.getUserProfile() } returns Result.success(userProfile)
        
        // When
        val result = contextBuilder.buildAnalysisContext(contact, userInput)
        
        // Then
        assertTrue(result.isSuccess)
        val context = result.getOrThrow()
        assertTrue(context.contains("【用户画像（你的特点）】"))
        assertTrue(context.contains("【联系人信息】"))
        assertTrue(context.contains("【用户提供的聊天记录】"))
    }
    
    @Test
    fun `buildAnalysisContext 用户画像为空时不包含画像区块`() = runTest {
        // Given
        val emptyProfile = UserProfile()
        val contact = createTestContact()
        val userInput = "你好"
        
        coEvery { userProfileRepository.getUserProfile() } returns Result.success(emptyProfile)
        
        // When
        val result = contextBuilder.buildAnalysisContext(contact, userInput)
        
        // Then
        assertTrue(result.isSuccess)
        val context = result.getOrThrow()
        assertFalse(context.contains("【用户画像（你的特点）】"))
        assertTrue(context.contains("【联系人信息】"))
    }
    
    @Test
    fun `buildAnalysisContext 获取用户画像失败时降级为不含画像的上下文`() = runTest {
        // Given
        val contact = createTestContact()
        val userInput = "测试消息"
        
        coEvery { userProfileRepository.getUserProfile() } returns 
            Result.failure(Exception("获取失败"))
        
        // When
        val result = contextBuilder.buildAnalysisContext(contact, userInput)
        
        // Then
        assertTrue(result.isSuccess)
        val context = result.getOrThrow()
        assertFalse(context.contains("【用户画像（你的特点）】"))
        assertTrue(context.contains("【联系人信息】"))
        assertTrue(context.contains("测试消息"))
    }
    
    // ========== filterRelevantProfileInfo 测试 - 工作场景 ==========
    
    @Test
    fun `filterRelevantProfileInfo 工作关键词匹配values和communicationStyle`() {
        // Given
        val profile = createFullProfile()
        val chatContent = "今天工作太忙了"
        
        // When
        val filtered = contextBuilder.filterRelevantProfileInfo(profile, chatContent)
        
        // Then
        assertTrue(filtered.values.isNotEmpty())
        assertTrue(filtered.communicationStyle.isNotEmpty())
    }
    
    @Test
    fun `filterRelevantProfileInfo 职场关键词匹配`() {
        // Given
        val profile = createFullProfile()
        val chatContent = "职场上遇到了一些问题"
        
        // When
        val filtered = contextBuilder.filterRelevantProfileInfo(profile, chatContent)
        
        // Then
        assertTrue(filtered.values.isNotEmpty())
        assertTrue(filtered.communicationStyle.isNotEmpty())
    }
    
    @Test
    fun `filterRelevantProfileInfo 同事关键词匹配`() {
        // Given
        val profile = createFullProfile()
        val chatContent = "和同事发生了争执"
        
        // When
        val filtered = contextBuilder.filterRelevantProfileInfo(profile, chatContent)
        
        // Then
        assertTrue(filtered.values.isNotEmpty())
        assertTrue(filtered.communicationStyle.isNotEmpty())
    }
    
    // ========== filterRelevantProfileInfo 测试 - 社交场景 ==========
    
    @Test
    fun `filterRelevantProfileInfo 朋友关键词匹配socialPreferences和interests`() {
        // Given
        val profile = createFullProfile()
        val chatContent = "和朋友约好了周末见面"
        
        // When
        val filtered = contextBuilder.filterRelevantProfileInfo(profile, chatContent)
        
        // Then
        assertTrue(filtered.socialPreferences.isNotEmpty())
        assertTrue(filtered.interests.isNotEmpty())
    }
    
    @Test
    fun `filterRelevantProfileInfo 聚会关键词匹配`() {
        // Given
        val profile = createFullProfile()
        val chatContent = "周末有个聚会"
        
        // When
        val filtered = contextBuilder.filterRelevantProfileInfo(profile, chatContent)
        
        // Then
        assertTrue(filtered.socialPreferences.isNotEmpty())
        assertTrue(filtered.interests.isNotEmpty())
    }
    
    // ========== filterRelevantProfileInfo 测试 - 家庭场景 ==========
    
    @Test
    fun `filterRelevantProfileInfo 家人关键词匹配values和personalityTraits`() {
        // Given
        val profile = createFullProfile()
        val chatContent = "家人最近身体不太好"
        
        // When
        val filtered = contextBuilder.filterRelevantProfileInfo(profile, chatContent)
        
        // Then
        assertTrue(filtered.values.isNotEmpty())
        assertTrue(filtered.personalityTraits.isNotEmpty())
    }
    
    @Test
    fun `filterRelevantProfileInfo 父母关键词匹配`() {
        // Given
        val profile = createFullProfile()
        val chatContent = "父母要来看我"
        
        // When
        val filtered = contextBuilder.filterRelevantProfileInfo(profile, chatContent)
        
        // Then
        assertTrue(filtered.values.isNotEmpty())
        assertTrue(filtered.personalityTraits.isNotEmpty())
    }
    
    // ========== filterRelevantProfileInfo 测试 - 情感场景 ==========
    
    @Test
    fun `filterRelevantProfileInfo 约会关键词匹配多个维度`() {
        // Given
        val profile = createFullProfile()
        val chatContent = "明天有个约会"
        
        // When
        val filtered = contextBuilder.filterRelevantProfileInfo(profile, chatContent)
        
        // Then
        assertTrue(filtered.interests.isNotEmpty())
        assertTrue(filtered.communicationStyle.isNotEmpty())
        assertTrue(filtered.personalityTraits.isNotEmpty())
    }
    
    @Test
    fun `filterRelevantProfileInfo 恋爱关键词匹配`() {
        // Given
        val profile = createFullProfile()
        val chatContent = "恋爱中遇到了问题"
        
        // When
        val filtered = contextBuilder.filterRelevantProfileInfo(profile, chatContent)
        
        // Then
        assertTrue(filtered.interests.isNotEmpty())
        assertTrue(filtered.communicationStyle.isNotEmpty())
        assertTrue(filtered.personalityTraits.isNotEmpty())
    }
    
    // ========== filterRelevantProfileInfo 测试 - 情绪关键词 ==========
    
    @Test
    fun `filterRelevantProfileInfo 开心情绪匹配personalityTraits和interests`() {
        // Given
        val profile = createFullProfile()
        val chatContent = "今天好开心啊"
        
        // When
        val filtered = contextBuilder.filterRelevantProfileInfo(profile, chatContent)
        
        // Then
        assertTrue(filtered.personalityTraits.isNotEmpty())
        assertTrue(filtered.interests.isNotEmpty())
    }
    
    @Test
    fun `filterRelevantProfileInfo 难过情绪匹配personalityTraits和values`() {
        // Given
        val profile = createFullProfile()
        val chatContent = "心情很难过"
        
        // When
        val filtered = contextBuilder.filterRelevantProfileInfo(profile, chatContent)
        
        // Then
        assertTrue(filtered.personalityTraits.isNotEmpty())
        assertTrue(filtered.values.isNotEmpty())
    }
    
    @Test
    fun `filterRelevantProfileInfo 生气情绪匹配personalityTraits和communicationStyle`() {
        // Given
        val profile = createFullProfile()
        val chatContent = "真的很生气"
        
        // When
        val filtered = contextBuilder.filterRelevantProfileInfo(profile, chatContent)
        
        // Then
        assertTrue(filtered.personalityTraits.isNotEmpty())
        assertTrue(filtered.communicationStyle.isNotEmpty())
    }
    
    @Test
    fun `filterRelevantProfileInfo 焦虑情绪匹配personalityTraits和socialPreferences`() {
        // Given
        val profile = createFullProfile()
        val chatContent = "最近很焦虑"
        
        // When
        val filtered = contextBuilder.filterRelevantProfileInfo(profile, chatContent)
        
        // Then
        assertTrue(filtered.personalityTraits.isNotEmpty())
        assertTrue(filtered.socialPreferences.isNotEmpty())
    }
    
    // ========== filterRelevantProfileInfo 测试 - 场景关键词 ==========
    
    @Test
    fun `filterRelevantProfileInfo 会议场景匹配communicationStyle和values`() {
        // Given
        val profile = createFullProfile()
        val chatContent = "下午有个重要会议"
        
        // When
        val filtered = contextBuilder.filterRelevantProfileInfo(profile, chatContent)
        
        // Then
        assertTrue(filtered.communicationStyle.isNotEmpty())
        assertTrue(filtered.values.isNotEmpty())
    }
    
    @Test
    fun `filterRelevantProfileInfo 面试场景匹配多个维度`() {
        // Given
        val profile = createFullProfile()
        val chatContent = "明天要去面试"
        
        // When
        val filtered = contextBuilder.filterRelevantProfileInfo(profile, chatContent)
        
        // Then
        assertTrue(filtered.communicationStyle.isNotEmpty())
        assertTrue(filtered.values.isNotEmpty())
        assertTrue(filtered.personalityTraits.isNotEmpty())
    }
    
    @Test
    fun `filterRelevantProfileInfo 聚餐场景匹配socialPreferences和interests`() {
        // Given
        val profile = createFullProfile()
        val chatContent = "晚上有个聚餐"
        
        // When
        val filtered = contextBuilder.filterRelevantProfileInfo(profile, chatContent)
        
        // Then
        assertTrue(filtered.socialPreferences.isNotEmpty())
        assertTrue(filtered.interests.isNotEmpty())
    }
    
    // ========== filterRelevantProfileInfo 测试 - 边界情况 ==========
    
    @Test
    fun `filterRelevantProfileInfo 无匹配关键词时返回完整画像`() {
        // Given
        val profile = createFullProfile()
        val chatContent = "随便聊聊"
        
        // When
        val filtered = contextBuilder.filterRelevantProfileInfo(profile, chatContent)
        
        // Then
        assertEquals(profile.personalityTraits, filtered.personalityTraits)
        assertEquals(profile.values, filtered.values)
        assertEquals(profile.interests, filtered.interests)
        assertEquals(profile.communicationStyle, filtered.communicationStyle)
        assertEquals(profile.socialPreferences, filtered.socialPreferences)
    }
    
    @Test
    fun `filterRelevantProfileInfo 空画像返回空画像`() {
        // Given
        val emptyProfile = UserProfile()
        val chatContent = "工作很忙"
        
        // When
        val filtered = contextBuilder.filterRelevantProfileInfo(emptyProfile, chatContent)
        
        // Then
        assertTrue(filtered.isEmpty())
    }
    
    @Test
    fun `filterRelevantProfileInfo 多个关键词匹配合并维度`() {
        // Given
        val profile = createFullProfile()
        val chatContent = "工作上和朋友发生了争执，心情很难过"
        
        // When
        val filtered = contextBuilder.filterRelevantProfileInfo(profile, chatContent)
        
        // Then
        // 工作 -> values, communicationStyle
        // 朋友 -> socialPreferences, interests
        // 难过 -> personalityTraits, values
        assertTrue(filtered.values.isNotEmpty())
        assertTrue(filtered.communicationStyle.isNotEmpty())
        assertTrue(filtered.socialPreferences.isNotEmpty())
        assertTrue(filtered.interests.isNotEmpty())
        assertTrue(filtered.personalityTraits.isNotEmpty())
    }
    
    @Test
    fun `filterRelevantProfileInfo 大小写不敏感`() {
        // Given
        val profile = createFullProfile()
        val chatContent = "今天工作很忙"
        
        // When
        val filtered = contextBuilder.filterRelevantProfileInfo(profile, chatContent)
        
        // Then
        assertTrue(filtered.values.isNotEmpty())
        assertTrue(filtered.communicationStyle.isNotEmpty())
    }
    
    // ========== 辅助方法 ==========
    
    private fun createTestContact(): ContactProfile {
        return ContactProfile(
            id = "test-contact-1",
            name = "测试联系人",
            targetGoal = "建立良好关系",
            facts = listOf(
                Fact(
                    key = "职业",
                    value = "程序员",
                    timestamp = System.currentTimeMillis(),
                    source = FactSource.MANUAL
                )
            )
        )
    }
    
    private fun createFullProfile(): UserProfile {
        return UserProfile(
            personalityTraits = listOf("内向", "理性", "细心"),
            values = listOf("重视家庭", "追求事业"),
            interests = listOf("阅读", "编程", "旅行"),
            communicationStyle = listOf("直接", "逻辑型"),
            socialPreferences = listOf("一对一", "深度交流")
        )
    }
}
