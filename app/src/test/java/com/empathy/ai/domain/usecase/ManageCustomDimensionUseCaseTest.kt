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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ManageCustomDimensionUseCase 单元测试
 */
class ManageCustomDimensionUseCaseTest {
    
    private lateinit var getUserProfileUseCase: GetUserProfileUseCase
    private lateinit var updateUserProfileUseCase: UpdateUserProfileUseCase
    private lateinit var validator: UserProfileValidator
    private lateinit var useCase: ManageCustomDimensionUseCase
    
    @Before
    fun setup() {
        getUserProfileUseCase = mockk()
        updateUserProfileUseCase = mockk()
        validator = mockk()
        useCase = ManageCustomDimensionUseCase(getUserProfileUseCase, updateUserProfileUseCase, validator)
    }
    
    // ========== addDimension 测试 ==========
    
    @Test
    fun `addDimension 成功添加维度`() = runTest {
        // Given
        val profile = UserProfile()
        val updatedProfile = profile.addCustomDimension("职业技能")
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        every { validator.sanitizeInput("职业技能") } returns "职业技能"
        every { validator.validateAddDimension(profile, "职业技能") } returns 
            UserProfileValidationResult.Valid
        coEvery { updateUserProfileUseCase(any()) } returns Result.success(updatedProfile)
        
        // When
        val result = useCase.addDimension("职业技能")
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.customDimensions?.containsKey("职业技能") == true)
        coVerify { updateUserProfileUseCase(any()) }
    }
    
    @Test
    fun `addDimension 获取画像失败时返回错误`() = runTest {
        // Given
        coEvery { getUserProfileUseCase(any()) } returns Result.failure(Exception("获取失败"))
        
        // When
        val result = useCase.addDimension("职业技能")
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("获取失败") == true)
    }
    
    @Test
    fun `addDimension 空名称验证失败`() = runTest {
        // Given
        val profile = UserProfile()
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        every { validator.sanitizeInput("") } returns ""
        every { validator.validateAddDimension(profile, "") } returns 
            UserProfileValidationResult.DimensionNameEmpty
        
        // When
        val result = useCase.addDimension("")
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ManageCustomDimensionUseCase.ValidationException)
    }
    
    @Test
    fun `addDimension 名称过短验证失败`() = runTest {
        // Given
        val profile = UserProfile()
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        every { validator.sanitizeInput("技") } returns "技"
        every { validator.validateAddDimension(profile, "技") } returns 
            UserProfileValidationResult.DimensionNameTooShort(2, 1)
        
        // When
        val result = useCase.addDimension("技")
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as ManageCustomDimensionUseCase.ValidationException
        assertTrue(exception.validationResult is UserProfileValidationResult.DimensionNameTooShort)
    }
    
    @Test
    fun `addDimension 名称过长验证失败`() = runTest {
        // Given
        val profile = UserProfile()
        val longName = "a".repeat(21)
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        every { validator.sanitizeInput(longName) } returns longName
        every { validator.validateAddDimension(profile, longName) } returns 
            UserProfileValidationResult.DimensionNameTooLong(20, 21)
        
        // When
        val result = useCase.addDimension(longName)
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as ManageCustomDimensionUseCase.ValidationException
        assertTrue(exception.validationResult is UserProfileValidationResult.DimensionNameTooLong)
    }
    
    @Test
    fun `addDimension 重复名称验证失败`() = runTest {
        // Given
        val profile = UserProfile(customDimensions = mapOf("职业技能" to emptyList()))
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        every { validator.sanitizeInput("职业技能") } returns "职业技能"
        every { validator.validateAddDimension(profile, "职业技能") } returns 
            UserProfileValidationResult.DimensionNameDuplicate("职业技能")
        
        // When
        val result = useCase.addDimension("职业技能")
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as ManageCustomDimensionUseCase.ValidationException
        assertTrue(exception.validationResult is UserProfileValidationResult.DimensionNameDuplicate)
    }
    
    @Test
    fun `addDimension 达到上限验证失败`() = runTest {
        // Given
        val dimensions = (1..10).associate { "维度$it" to emptyList<String>() }
        val profile = UserProfile(customDimensions = dimensions)
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        every { validator.sanitizeInput("新维度") } returns "新维度"
        every { validator.validateAddDimension(profile, "新维度") } returns 
            UserProfileValidationResult.DimensionLimitExceeded(10, 10)
        
        // When
        val result = useCase.addDimension("新维度")
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as ManageCustomDimensionUseCase.ValidationException
        assertTrue(exception.validationResult is UserProfileValidationResult.DimensionLimitExceeded)
    }
    
    @Test
    fun `addDimension 名称被清理后添加`() = runTest {
        // Given
        val profile = UserProfile()
        val updatedProfile = profile.addCustomDimension("职业技能")
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        every { validator.sanitizeInput("  职业技能  ") } returns "职业技能"
        every { validator.validateAddDimension(profile, "职业技能") } returns 
            UserProfileValidationResult.Valid
        coEvery { updateUserProfileUseCase(any()) } returns Result.success(updatedProfile)
        
        // When
        val result = useCase.addDimension("  职业技能  ")
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { updateUserProfileUseCase(match { it.customDimensions.containsKey("职业技能") }) }
    }
    
    @Test
    fun `addDimension 保存失败时返回错误`() = runTest {
        // Given
        val profile = UserProfile()
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        every { validator.sanitizeInput("职业技能") } returns "职业技能"
        every { validator.validateAddDimension(profile, "职业技能") } returns 
            UserProfileValidationResult.Valid
        coEvery { updateUserProfileUseCase(any()) } returns Result.failure(Exception("保存失败"))
        
        // When
        val result = useCase.addDimension("职业技能")
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("保存失败") == true)
    }
    
    // ========== removeDimension 测试 ==========
    
    @Test
    fun `removeDimension 成功删除维度`() = runTest {
        // Given
        val profile = UserProfile(customDimensions = mapOf("职业技能" to listOf("编程")))
        val updatedProfile = profile.removeCustomDimension("职业技能")
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        coEvery { updateUserProfileUseCase(any()) } returns Result.success(updatedProfile)
        
        // When
        val result = useCase.removeDimension("职业技能")
        
        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()?.customDimensions?.containsKey("职业技能") == true)
        coVerify { updateUserProfileUseCase(any()) }
    }
    
    @Test
    fun `removeDimension 删除不存在的维度返回成功（幂等性）`() = runTest {
        // Given
        val profile = UserProfile()
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        
        // When
        val result = useCase.removeDimension("不存在的维度")
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(profile, result.getOrNull())
        coVerify(exactly = 0) { updateUserProfileUseCase(any()) }
    }
    
    @Test
    fun `removeDimension 获取画像失败时返回错误`() = runTest {
        // Given
        coEvery { getUserProfileUseCase(any()) } returns Result.failure(Exception("获取失败"))
        
        // When
        val result = useCase.removeDimension("职业技能")
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("获取失败") == true)
    }
    
    @Test
    fun `removeDimension 保存失败时返回错误`() = runTest {
        // Given
        val profile = UserProfile(customDimensions = mapOf("职业技能" to listOf("编程")))
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        coEvery { updateUserProfileUseCase(any()) } returns Result.failure(Exception("保存失败"))
        
        // When
        val result = useCase.removeDimension("职业技能")
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("保存失败") == true)
    }
    
    @Test
    fun `removeDimension 级联删除维度下的所有标签`() = runTest {
        // Given
        val profile = UserProfile(customDimensions = mapOf(
            "职业技能" to listOf("编程", "设计", "测试")
        ))
        val updatedProfile = profile.removeCustomDimension("职业技能")
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        coEvery { updateUserProfileUseCase(any()) } returns Result.success(updatedProfile)
        
        // When
        val result = useCase.removeDimension("职业技能")
        
        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()?.customDimensions?.containsKey("职业技能") == true)
    }
    
    // ========== ValidationException 测试 ==========
    
    @Test
    fun `ValidationException 包含正确的错误消息`() {
        val validationResult = UserProfileValidationResult.DimensionNameDuplicate("职业技能")
        val exception = ManageCustomDimensionUseCase.ValidationException(validationResult)
        
        assertEquals(validationResult.getErrorMessage(), exception.message)
        assertEquals(validationResult, exception.validationResult)
    }
}
