package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.UserProfile
import com.empathy.ai.domain.model.UserProfileValidationResult
import com.empathy.ai.domain.util.UserProfileValidator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * AddTagUseCase 单元测试
 */
class AddTagUseCaseTest {
    
    private lateinit var getUserProfileUseCase: GetUserProfileUseCase
    private lateinit var updateUserProfileUseCase: UpdateUserProfileUseCase
    private lateinit var validator: UserProfileValidator
    private lateinit var useCase: AddTagUseCase
    
    @Before
    fun setup() {
        getUserProfileUseCase = mockk()
        updateUserProfileUseCase = mockk()
        validator = mockk()
        useCase = AddTagUseCase(getUserProfileUseCase, updateUserProfileUseCase, validator)
    }
    
    @Test
    fun `成功添加标签到基础维度`() = runTest {
        // Given
        val profile = UserProfile()
        val updatedProfile = profile.addTag("PERSONALITY_TRAITS", "内向")
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        every { validator.sanitizeInput("内向") } returns "内向"
        every { validator.validateAddTag(profile, "PERSONALITY_TRAITS", "内向") } returns 
            UserProfileValidationResult.Valid
        coEvery { updateUserProfileUseCase(any()) } returns Result.success(updatedProfile)
        
        // When
        val result = useCase("PERSONALITY_TRAITS", "内向")
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("内向"), result.getOrNull()?.personalityTraits)
        coVerify { updateUserProfileUseCase(any()) }
    }
    
    @Test
    fun `成功添加标签到自定义维度`() = runTest {
        // Given
        val profile = UserProfile(customDimensions = mapOf("职业技能" to emptyList()))
        val updatedProfile = profile.addTag("职业技能", "编程")
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        every { validator.sanitizeInput("编程") } returns "编程"
        every { validator.validateAddTag(profile, "职业技能", "编程") } returns 
            UserProfileValidationResult.Valid
        coEvery { updateUserProfileUseCase(any()) } returns Result.success(updatedProfile)
        
        // When
        val result = useCase("职业技能", "编程")
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("编程"), result.getOrNull()?.customDimensions?.get("职业技能"))
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
    fun `空标签验证失败`() = runTest {
        // Given
        val profile = UserProfile()
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        every { validator.sanitizeInput("") } returns ""
        every { validator.validateAddTag(profile, "PERSONALITY_TRAITS", "") } returns 
            UserProfileValidationResult.TagEmpty
        
        // When
        val result = useCase("PERSONALITY_TRAITS", "")
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AddTagUseCase.ValidationException)
    }
    
    @Test
    fun `重复标签验证失败`() = runTest {
        // Given
        val profile = UserProfile(personalityTraits = listOf("内向"))
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        every { validator.sanitizeInput("内向") } returns "内向"
        every { validator.validateAddTag(profile, "PERSONALITY_TRAITS", "内向") } returns 
            UserProfileValidationResult.TagDuplicate("内向")
        
        // When
        val result = useCase("PERSONALITY_TRAITS", "内向")
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as AddTagUseCase.ValidationException
        assertTrue(exception.validationResult is UserProfileValidationResult.TagDuplicate)
    }
    
    @Test
    fun `超长标签验证失败`() = runTest {
        // Given
        val profile = UserProfile()
        val longTag = "a".repeat(51)
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        every { validator.sanitizeInput(longTag) } returns longTag
        every { validator.validateAddTag(profile, "PERSONALITY_TRAITS", longTag) } returns 
            UserProfileValidationResult.TagTooLong(50, 51)
        
        // When
        val result = useCase("PERSONALITY_TRAITS", longTag)
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as AddTagUseCase.ValidationException
        assertTrue(exception.validationResult is UserProfileValidationResult.TagTooLong)
    }
    
    @Test
    fun `达到标签数量上限验证失败`() = runTest {
        // Given
        val tags = (1..20).map { "标签$it" }
        val profile = UserProfile(personalityTraits = tags)
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        every { validator.sanitizeInput("新标签") } returns "新标签"
        every { validator.validateAddTag(profile, "PERSONALITY_TRAITS", "新标签") } returns 
            UserProfileValidationResult.TagLimitExceeded(20, 20)
        
        // When
        val result = useCase("PERSONALITY_TRAITS", "新标签")
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as AddTagUseCase.ValidationException
        assertTrue(exception.validationResult is UserProfileValidationResult.TagLimitExceeded)
    }
    
    @Test
    fun `标签内容被清理后添加`() = runTest {
        // Given
        val profile = UserProfile()
        val updatedProfile = profile.addTag("PERSONALITY_TRAITS", "内向")
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        every { validator.sanitizeInput("  内向  ") } returns "内向"
        every { validator.validateAddTag(profile, "PERSONALITY_TRAITS", "内向") } returns 
            UserProfileValidationResult.Valid
        coEvery { updateUserProfileUseCase(any()) } returns Result.success(updatedProfile)
        
        // When
        val result = useCase("PERSONALITY_TRAITS", "  内向  ")
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { updateUserProfileUseCase(match { it.personalityTraits.contains("内向") }) }
    }
    
    @Test
    fun `保存失败时返回错误`() = runTest {
        // Given
        val profile = UserProfile()
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        every { validator.sanitizeInput("内向") } returns "内向"
        every { validator.validateAddTag(profile, "PERSONALITY_TRAITS", "内向") } returns 
            UserProfileValidationResult.Valid
        coEvery { updateUserProfileUseCase(any()) } returns Result.failure(Exception("保存失败"))
        
        // When
        val result = useCase("PERSONALITY_TRAITS", "内向")
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("保存失败") == true)
    }
    
    @Test
    fun `ValidationException包含正确的错误消息`() {
        val validationResult = UserProfileValidationResult.TagDuplicate("内向")
        val exception = AddTagUseCase.ValidationException(validationResult)
        
        assertEquals(validationResult.getErrorMessage(), exception.message)
        assertEquals(validationResult, exception.validationResult)
    }
}
