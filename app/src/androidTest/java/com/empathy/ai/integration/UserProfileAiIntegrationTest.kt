package com.empathy.ai.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.UserProfile
import com.empathy.ai.domain.repository.UserProfileRepository
import com.empathy.ai.domain.util.UserProfileContextBuilder
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 用户画像AI上下文集成测试
 *
 * 测试用户画像与AI分析功能的集成。
 */
@RunWith(AndroidJUnit4::class)
class UserProfileAiIntegrationTest {

    private lateinit var contextBuilder: UserProfileContextBuilder
    private lateinit var userProfileRepository: UserProfileRepository

    @Before
    fun setup() {
        userProfileRepository = mockk()
        contextBuilder = UserProfileContextBuilder(userProfileRepository)
    }

    // ========== 上下文构建测试 ==========

    @Test
    fun buildContext_includesUserProfile() = runTest {
        // Given
        val userProfile = createTestUserProfile()
        val contact = createTestContact()
        val userInput = "我想约她出去吃饭"
        
        coEvery { userProfileRepository.getUserProfile() } returns Result.success(userProfile)

        // When
        val result = contextBuilder.buildAnalysisContext(
            contact = contact,
            userInput = userInput
        )

        // Then
        assertTrue("构建应该成功", result.isSuccess)
        val context = result.getOrNull()!!
        assertTrue("上下文应包含用户画像标题", context.contains("用户画像"))
        assertTrue("上下文应包含性格特点", context.contains("内向") || context.contains("性格特点"))
    }

    @Test
    fun buildContext_includesContactProfile() = runTest {
        // Given
        val userProfile = createTestUserProfile()
        val contact = createTestContact()
        val userInput = "她最近怎么样"
        
        coEvery { userProfileRepository.getUserProfile() } returns Result.success(userProfile)

        // When
        val result = contextBuilder.buildAnalysisContext(
            contact = contact,
            userInput = userInput
        )

        // Then
        assertTrue("构建应该成功", result.isSuccess)
        val context = result.getOrNull()!!
        assertTrue("上下文应包含联系人信息", context.contains("联系人") || context.contains(contact.name))
    }

    @Test
    fun buildContext_handlesEmptyUserProfile() = runTest {
        // Given
        val emptyProfile = UserProfile()
        val contact = createTestContact()
        val userInput = "你好"
        
        coEvery { userProfileRepository.getUserProfile() } returns Result.success(emptyProfile)

        // When
        val result = contextBuilder.buildAnalysisContext(
            contact = contact,
            userInput = userInput
        )

        // Then - 空画像时应该正常返回，不抛异常
        assertTrue("构建应该成功", result.isSuccess)
        val context = result.getOrNull()!!
        assertTrue("上下文不应为空", context.isNotEmpty())
    }

    @Test
    fun buildContext_handlesUserProfileLoadFailure() = runTest {
        // Given
        val contact = createTestContact()
        val userInput = "帮我分析一下"
        
        coEvery { userProfileRepository.getUserProfile() } returns Result.failure(Exception("加载失败"))

        // When
        val result = contextBuilder.buildAnalysisContext(
            contact = contact,
            userInput = userInput
        )

        // Then - 获取画像失败时应该降级
        assertTrue("构建应该成功（降级）", result.isSuccess)
        val context = result.getOrNull()!!
        assertTrue("上下文应包含联系人信息", context.contains("联系人"))
    }

    // ========== 智能筛选测试 ==========

    @Test
    fun filterRelevantInfo_workScenario() = runTest {
        // Given
        val userProfile = UserProfile(
            personalityTraits = listOf("内向", "理性"),
            values = listOf("职业发展", "工作效率"),
            interests = listOf("编程", "阅读"),
            communicationStyle = listOf("直接", "简洁"),
            socialPreferences = listOf("小圈子")
        )
        val userInput = "我要和同事开会讨论项目"

        // When
        val filteredProfile = contextBuilder.filterRelevantProfileInfo(userProfile, userInput)

        // Then - 工作场景应该筛选出相关维度
        assertTrue("应包含沟通风格", filteredProfile.communicationStyle.isNotEmpty())
    }

    @Test
    fun filterRelevantInfo_socialScenario() = runTest {
        // Given
        val userProfile = UserProfile(
            personalityTraits = listOf("外向", "热情"),
            values = listOf("友谊", "真诚"),
            interests = listOf("聚会", "旅行"),
            communicationStyle = listOf("幽默", "随和"),
            socialPreferences = listOf("大圈子", "喜欢热闹")
        )
        val userInput = "周末朋友聚会怎么安排"

        // When
        val filteredProfile = contextBuilder.filterRelevantProfileInfo(userProfile, userInput)

        // Then - 社交场景应该筛选出相关维度
        assertTrue("应包含社交偏好", filteredProfile.socialPreferences.isNotEmpty())
    }

    @Test
    fun filterRelevantInfo_emotionalScenario() = runTest {
        // Given
        val userProfile = UserProfile(
            personalityTraits = listOf("敏感", "细腻"),
            values = listOf("感情", "家庭"),
            interests = listOf("音乐", "电影"),
            communicationStyle = listOf("温柔", "体贴"),
            socialPreferences = listOf("亲密关系")
        )
        val userInput = "她好像生气了，我该怎么办"

        // When
        val filteredProfile = contextBuilder.filterRelevantProfileInfo(userProfile, userInput)

        // Then - 情感场景应该筛选出相关维度
        assertTrue("应包含性格特点", filteredProfile.personalityTraits.isNotEmpty())
    }

    // ========== 上下文格式测试 ==========

    @Test
    fun contextFormat_isStructured() = runTest {
        // Given
        val userProfile = createTestUserProfile()
        val contact = createTestContact()
        val userInput = "测试输入"
        
        coEvery { userProfileRepository.getUserProfile() } returns Result.success(userProfile)

        // When
        val result = contextBuilder.buildAnalysisContext(
            contact = contact,
            userInput = userInput
        )

        // Then - 上下文应该有结构化格式
        assertTrue("构建应该成功", result.isSuccess)
        val context = result.getOrNull()!!
        assertTrue("上下文应包含分隔符或标题", 
            context.contains("---") || 
            context.contains("##") || 
            context.contains("【") ||
            context.contains(":")
        )
    }

    @Test
    fun contextFormat_userProfileFirst() = runTest {
        // Given
        val userProfile = createTestUserProfile()
        val contact = createTestContact()
        val userInput = "测试"
        
        coEvery { userProfileRepository.getUserProfile() } returns Result.success(userProfile)

        // When
        val result = contextBuilder.buildAnalysisContext(
            contact = contact,
            userInput = userInput
        )

        // Then - 用户画像应该在联系人信息之前（优先级更高）
        assertTrue("构建应该成功", result.isSuccess)
        val context = result.getOrNull()!!
        val userProfileIndex = context.indexOf("用户画像").takeIf { it >= 0 } 
            ?: context.indexOf("性格")
        val contactIndex = context.indexOf("联系人").takeIf { it >= 0 }
            ?: context.indexOf(contact.name)
        
        if (userProfileIndex >= 0 && contactIndex >= 0) {
            assertTrue("用户画像应在联系人之前", userProfileIndex < contactIndex)
        }
    }

    // ========== 降级策略测试 ==========

    @Test
    fun degradation_worksWithoutUserProfile() = runTest {
        // Given
        val contact = createTestContact()
        val userInput = "帮我分析"
        
        coEvery { userProfileRepository.getUserProfile() } returns Result.success(UserProfile())

        // When
        val result = contextBuilder.buildAnalysisContext(
            contact = contact,
            userInput = userInput
        )

        // Then - 没有用户画像时应该正常工作
        assertTrue("构建应该成功", result.isSuccess)
        val context = result.getOrNull()!!
        assertTrue("上下文不应为空", context.isNotEmpty())
        assertTrue("应包含联系人信息", context.contains("联系人") || context.contains(contact.name))
    }

    // ========== 自定义维度测试 ==========

    @Test
    fun buildContext_includesCustomDimensions() = runTest {
        // Given
        val userProfile = UserProfile(
            personalityTraits = listOf("内向"),
            customDimensions = mapOf(
                "职业技能" to listOf("Kotlin", "Android"),
                "生活习惯" to listOf("早起", "运动")
            )
        )
        val contact = createTestContact()
        val userInput = "我想学习新技术"
        
        coEvery { userProfileRepository.getUserProfile() } returns Result.success(userProfile)

        // When
        val result = contextBuilder.buildAnalysisContext(
            contact = contact,
            userInput = userInput
        )

        // Then
        assertTrue("构建应该成功", result.isSuccess)
        val context = result.getOrNull()!!
        assertTrue("上下文应包含自定义维度内容", 
            context.contains("职业技能") || 
            context.contains("Kotlin") ||
            context.contains("自定义")
        )
    }

    // ========== 辅助方法 ==========

    private fun createTestUserProfile(): UserProfile {
        return UserProfile(
            personalityTraits = listOf("内向", "理性", "细心"),
            values = listOf("诚实", "责任", "成长"),
            interests = listOf("阅读", "编程", "旅行"),
            communicationStyle = listOf("直接", "简洁"),
            socialPreferences = listOf("小圈子", "深度交流"),
            customDimensions = mapOf(
                "职业技能" to listOf("Kotlin", "Android")
            )
        )
    }

    private fun createTestContact(): ContactProfile {
        return ContactProfile(
            id = 1L,
            name = "测试联系人",
            relationship = "朋友",
            notes = "测试备注"
        )
    }
}
