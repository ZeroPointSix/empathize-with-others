package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.UserProfile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * RemoveTagUseCase 单元测试
 */
class RemoveTagUseCaseTest {
    
    private lateinit var getUserProfileUseCase: GetUserProfileUseCase
    private lateinit var updateUserProfileUseCase: UpdateUserProfileUseCase
    private lateinit var useCase: RemoveTagUseCase
    
    @Before
    fun setup() {
        getUserProfileUseCase = mockk()
        updateUserProfileUseCase = mockk()
        useCase = RemoveTagUseCase(getUserProfileUseCase, updateUserProfileUseCase)
    }
    
    @Test
    fun `成功从基础维度移除标签`() = runTest {
        // Given
        val profile = UserProfile(personalityTraits = listOf("内向", "敏感"))
        val updatedProfile = profile.removeTag("PERSONALITY_TRAITS", "内向")
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        coEvery { updateUserProfileUseCase(any()) } returns Result.success(updatedProfile)
        
        // When
        val result = useCase("PERSONALITY_TRAITS", "内向")
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("敏感"), result.getOrNull()?.personalityTraits)
        coVerify { updateUserProfileUseCase(any()) }
    }
    
    @Test
    fun `成功从自定义维度移除标签`() = runTest {
        // Given
        val profile = UserProfile(customDimensions = mapOf("职业技能" to listOf("编程", "设计")))
        val updatedProfile = profile.removeTag("职业技能", "编程")
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        coEvery { updateUserProfileUseCase(any()) } returns Result.success(updatedProfile)
        
        // When
        val result = useCase("职业技能", "编程")
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("设计"), result.getOrNull()?.customDimensions?.get("职业技能"))
    }
    
    @Test
    fun `移除不存在的标签返回成功（幂等性）`() = runTest {
        // Given
        val profile = UserProfile(personalityTraits = listOf("内向"))
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        
        // When
        val result = useCase("PERSONALITY_TRAITS", "不存在的标签")
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(profile, result.getOrNull())
        // 不应该调用更新方法
        coVerify(exactly = 0) { updateUserProfileUseCase(any()) }
    }
    
    @Test
    fun `从空维度移除标签返回成功（幂等性）`() = runTest {
        // Given
        val profile = UserProfile()
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        
        // When
        val result = useCase("PERSONALITY_TRAITS", "内向")
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(profile, result.getOrNull())
        coVerify(exactly = 0) { updateUserProfileUseCase(any()) }
    }
    
    @Test
    fun `获取画像失败时返回错误`() = runTest {
        // Given
        coEvery { getUserProfileUseCase(any()) } returns Result.failure(Exception("获取失败"))
        
        // When
        val result = useCase("PERSONALITY_TRAITS", "内向")
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("获取失败") == true)
    }
    
    @Test
    fun `保存失败时返回错误`() = runTest {
        // Given
        val profile = UserProfile(personalityTraits = listOf("内向"))
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        coEvery { updateUserProfileUseCase(any()) } returns Result.failure(Exception("保存失败"))
        
        // When
        val result = useCase("PERSONALITY_TRAITS", "内向")
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("保存失败") == true)
    }
    
    @Test
    fun `移除最后一个标签后维度为空`() = runTest {
        // Given
        val profile = UserProfile(personalityTraits = listOf("内向"))
        val updatedProfile = profile.removeTag("PERSONALITY_TRAITS", "内向")
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        coEvery { updateUserProfileUseCase(any()) } returns Result.success(updatedProfile)
        
        // When
        val result = useCase("PERSONALITY_TRAITS", "内向")
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.personalityTraits?.isEmpty() == true)
    }
    
    @Test
    fun `从不存在的自定义维度移除标签返回成功`() = runTest {
        // Given
        val profile = UserProfile()
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        
        // When
        val result = useCase("不存在的维度", "标签")
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(profile, result.getOrNull())
        coVerify(exactly = 0) { updateUserProfileUseCase(any()) }
    }
}
