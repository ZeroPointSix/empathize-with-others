package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.ExportFormat
import com.empathy.ai.domain.model.UserProfile
import com.empathy.ai.domain.model.UserProfileValidationResult
import com.empathy.ai.domain.repository.UserProfileRepository
import com.empathy.ai.domain.usecase.AddTagUseCase
import com.empathy.ai.domain.usecase.ExportUserProfileUseCase
import com.empathy.ai.domain.usecase.GetUserProfileUseCase
import com.empathy.ai.domain.usecase.ManageCustomDimensionUseCase
import com.empathy.ai.domain.usecase.RemoveTagUseCase
import com.empathy.ai.domain.usecase.UpdateUserProfileUseCase
import com.empathy.ai.presentation.ui.screen.userprofile.UserProfileUiEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * UserProfileViewModel 单元测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var getUserProfileUseCase: GetUserProfileUseCase
    private lateinit var updateUserProfileUseCase: UpdateUserProfileUseCase
    private lateinit var addTagUseCase: AddTagUseCase
    private lateinit var removeTagUseCase: RemoveTagUseCase
    private lateinit var manageCustomDimensionUseCase: ManageCustomDimensionUseCase
    private lateinit var exportUserProfileUseCase: ExportUserProfileUseCase
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var viewModel: UserProfileViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        getUserProfileUseCase = mockk()
        updateUserProfileUseCase = mockk()
        addTagUseCase = mockk()
        removeTagUseCase = mockk()
        manageCustomDimensionUseCase = mockk()
        exportUserProfileUseCase = mockk()
        userProfileRepository = mockk()
        
        // 默认返回空画像
        coEvery { getUserProfileUseCase(any()) } returns Result.success(UserProfile())
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    private fun createViewModel(): UserProfileViewModel {
        return UserProfileViewModel(
            getUserProfileUseCase = getUserProfileUseCase,
            updateUserProfileUseCase = updateUserProfileUseCase,
            addTagUseCase = addTagUseCase,
            removeTagUseCase = removeTagUseCase,
            manageCustomDimensionUseCase = manageCustomDimensionUseCase,
            exportUserProfileUseCase = exportUserProfileUseCase,
            userProfileRepository = userProfileRepository
        )
    }
    
    // ========== 初始化测试 ==========
    
    @Test
    fun `初始化时加载用户画像`() = runTest {
        // Given
        val profile = createTestProfile()
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        
        // When
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // Then
        assertEquals(profile, viewModel.uiState.value.profile)
        assertFalse(viewModel.uiState.value.isLoading)
    }
    
    @Test
    fun `初始化加载失败时显示错误`() = runTest {
        // Given
        coEvery { getUserProfileUseCase(any()) } returns Result.failure(Exception("加载失败"))
        
        // When
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.uiState.value.error?.contains("加载失败") == true)
        assertFalse(viewModel.uiState.value.isLoading)
    }
    
    // ========== 标签操作测试 ==========
    
    @Test
    fun `ShowAddTagDialog 事件显示添加标签对话框`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(UserProfileUiEvent.ShowAddTagDialog("PERSONALITY_TRAITS"))
        
        // Then
        assertTrue(viewModel.uiState.value.showAddTagDialog)
        assertEquals("PERSONALITY_TRAITS", viewModel.uiState.value.currentEditDimension)
    }
    
    @Test
    fun `AddTag 事件成功添加标签`() = runTest {
        // Given
        val updatedProfile = createTestProfile().copy(
            personalityTraits = listOf("内向", "新标签")
        )
        coEvery { addTagUseCase(any(), any()) } returns Result.success(updatedProfile)
        
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(UserProfileUiEvent.ShowAddTagDialog("PERSONALITY_TRAITS"))
        
        // When
        viewModel.onEvent(UserProfileUiEvent.AddTag("PERSONALITY_TRAITS", "新标签"))
        advanceUntilIdle()
        
        // Then
        assertEquals(updatedProfile, viewModel.uiState.value.profile)
        assertFalse(viewModel.uiState.value.showAddTagDialog)
        assertNull(viewModel.uiState.value.currentEditDimension)
        assertEquals("标签添加成功", viewModel.uiState.value.successMessage)
    }
    
    @Test
    fun `AddTag 事件失败时显示错误`() = runTest {
        // Given
        coEvery { addTagUseCase(any(), any()) } returns 
            Result.failure(AddTagUseCase.ValidationException(
                UserProfileValidationResult.TagDuplicate("重复标签")
            ))
        
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(UserProfileUiEvent.AddTag("PERSONALITY_TRAITS", "重复标签"))
        advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.uiState.value.error?.contains("重复标签") == true)
    }
    
    @Test
    fun `ShowEditTagDialog 事件显示编辑标签对话框`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(UserProfileUiEvent.ShowEditTagDialog("PERSONALITY_TRAITS", "内向"))
        
        // Then
        assertTrue(viewModel.uiState.value.showEditTagDialog)
        assertEquals("PERSONALITY_TRAITS", viewModel.uiState.value.currentEditDimension)
        assertEquals("内向", viewModel.uiState.value.currentEditTag)
    }
    
    @Test
    fun `EditTag 事件成功编辑标签`() = runTest {
        // Given
        val profile = createTestProfile()
        val updatedProfile = profile.copy(personalityTraits = listOf("外向"))
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        coEvery { removeTagUseCase(any(), any()) } returns Result.success(profile.copy(personalityTraits = emptyList()))
        coEvery { addTagUseCase(any(), any()) } returns Result.success(updatedProfile)
        
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(UserProfileUiEvent.ShowEditTagDialog("PERSONALITY_TRAITS", "内向"))
        
        // When
        viewModel.onEvent(UserProfileUiEvent.EditTag("PERSONALITY_TRAITS", "内向", "外向"))
        advanceUntilIdle()
        
        // Then
        assertEquals(updatedProfile, viewModel.uiState.value.profile)
        assertFalse(viewModel.uiState.value.showEditTagDialog)
        assertEquals("标签修改成功", viewModel.uiState.value.successMessage)
    }
    
    @Test
    fun `ShowDeleteTagConfirm 事件显示删除确认对话框`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(UserProfileUiEvent.ShowDeleteTagConfirm("PERSONALITY_TRAITS", "内向"))
        
        // Then
        assertTrue(viewModel.uiState.value.showDeleteConfirmDialog)
        assertEquals("PERSONALITY_TRAITS", viewModel.uiState.value.currentEditDimension)
        assertEquals("内向", viewModel.uiState.value.pendingDeleteTag)
    }
    
    @Test
    fun `ConfirmDeleteTag 事件成功删除标签`() = runTest {
        // Given
        val profile = createTestProfile()
        val updatedProfile = profile.copy(personalityTraits = emptyList())
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        coEvery { removeTagUseCase(any(), any()) } returns Result.success(updatedProfile)
        
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(UserProfileUiEvent.ShowDeleteTagConfirm("PERSONALITY_TRAITS", "内向"))
        
        // When
        viewModel.onEvent(UserProfileUiEvent.ConfirmDeleteTag)
        advanceUntilIdle()
        
        // Then
        assertEquals(updatedProfile, viewModel.uiState.value.profile)
        assertFalse(viewModel.uiState.value.showDeleteConfirmDialog)
        assertEquals("标签删除成功", viewModel.uiState.value.successMessage)
    }
    
    @Test
    fun `HideTagDialog 事件隐藏所有标签对话框`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(UserProfileUiEvent.ShowAddTagDialog("PERSONALITY_TRAITS"))
        
        // When
        viewModel.onEvent(UserProfileUiEvent.HideTagDialog)
        
        // Then
        assertFalse(viewModel.uiState.value.showAddTagDialog)
        assertFalse(viewModel.uiState.value.showEditTagDialog)
        assertFalse(viewModel.uiState.value.showDeleteConfirmDialog)
        assertNull(viewModel.uiState.value.currentEditDimension)
    }
    
    // ========== 维度操作测试 ==========
    
    @Test
    fun `ShowAddDimensionDialog 事件显示添加维度对话框`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(UserProfileUiEvent.ShowAddDimensionDialog)
        
        // Then
        assertTrue(viewModel.uiState.value.showAddDimensionDialog)
    }
    
    @Test
    fun `AddDimension 事件成功添加维度`() = runTest {
        // Given
        val updatedProfile = UserProfile(
            customDimensions = mapOf("职业技能" to emptyList())
        )
        coEvery { manageCustomDimensionUseCase.addDimension(any()) } returns Result.success(updatedProfile)
        
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(UserProfileUiEvent.ShowAddDimensionDialog)
        
        // When
        viewModel.onEvent(UserProfileUiEvent.AddDimension("职业技能"))
        advanceUntilIdle()
        
        // Then
        assertEquals(updatedProfile, viewModel.uiState.value.profile)
        assertFalse(viewModel.uiState.value.showAddDimensionDialog)
        assertEquals("维度添加成功", viewModel.uiState.value.successMessage)
    }
    
    @Test
    fun `AddDimension 事件失败时显示错误`() = runTest {
        // Given
        coEvery { manageCustomDimensionUseCase.addDimension(any()) } returns 
            Result.failure(Exception("维度名称已存在"))
        
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(UserProfileUiEvent.AddDimension("重复维度"))
        advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.uiState.value.error?.contains("维度名称已存在") == true)
    }
    
    @Test
    fun `ShowDeleteDimensionConfirm 事件显示删除维度确认对话框`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(UserProfileUiEvent.ShowDeleteDimensionConfirm("职业技能"))
        
        // Then
        assertTrue(viewModel.uiState.value.showDeleteConfirmDialog)
        assertEquals("职业技能", viewModel.uiState.value.pendingDeleteDimension)
    }
    
    @Test
    fun `ConfirmDeleteDimension 事件成功删除维度`() = runTest {
        // Given
        val profile = UserProfile(customDimensions = mapOf("职业技能" to listOf("编程")))
        val updatedProfile = UserProfile(customDimensions = emptyMap())
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile)
        coEvery { manageCustomDimensionUseCase.removeDimension(any()) } returns Result.success(updatedProfile)
        
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(UserProfileUiEvent.ShowDeleteDimensionConfirm("职业技能"))
        
        // When
        viewModel.onEvent(UserProfileUiEvent.ConfirmDeleteDimension)
        advanceUntilIdle()
        
        // Then
        assertEquals(updatedProfile, viewModel.uiState.value.profile)
        assertFalse(viewModel.uiState.value.showDeleteConfirmDialog)
        assertNull(viewModel.uiState.value.pendingDeleteDimension)
        assertEquals("维度删除成功", viewModel.uiState.value.successMessage)
    }
    
    @Test
    fun `HideDimensionDialog 事件隐藏维度对话框`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(UserProfileUiEvent.ShowAddDimensionDialog)
        
        // When
        viewModel.onEvent(UserProfileUiEvent.HideDimensionDialog)
        
        // Then
        assertFalse(viewModel.uiState.value.showAddDimensionDialog)
        assertFalse(viewModel.uiState.value.showDeleteConfirmDialog)
        assertNull(viewModel.uiState.value.pendingDeleteDimension)
    }
    
    // ========== 导出操作测试 ==========
    
    @Test
    fun `ShowExportDialog 事件显示导出对话框`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(UserProfileUiEvent.ShowExportDialog)
        
        // Then
        assertTrue(viewModel.uiState.value.showExportDialog)
    }
    
    @Test
    fun `ExportProfile 事件成功导出JSON格式`() = runTest {
        // Given
        val exportContent = """{"personalityTraits":["内向"]}"""
        coEvery { exportUserProfileUseCase(ExportFormat.JSON) } returns Result.success(exportContent)
        
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(UserProfileUiEvent.ShowExportDialog)
        
        // When
        viewModel.onEvent(UserProfileUiEvent.ExportProfile(ExportFormat.JSON))
        advanceUntilIdle()
        
        // Then
        assertFalse(viewModel.uiState.value.showExportDialog)
        assertEquals("导出成功", viewModel.uiState.value.successMessage)
        assertEquals(exportContent, viewModel.exportResult.value)
    }
    
    @Test
    fun `ExportProfile 事件成功导出纯文本格式`() = runTest {
        // Given
        val exportContent = "性格特点: 内向"
        coEvery { exportUserProfileUseCase(ExportFormat.PLAIN_TEXT) } returns Result.success(exportContent)
        
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(UserProfileUiEvent.ExportProfile(ExportFormat.PLAIN_TEXT))
        advanceUntilIdle()
        
        // Then
        assertEquals("导出成功", viewModel.uiState.value.successMessage)
        assertEquals(exportContent, viewModel.exportResult.value)
    }
    
    @Test
    fun `ExportProfile 事件失败时显示错误`() = runTest {
        // Given
        coEvery { exportUserProfileUseCase(any()) } returns Result.failure(Exception("导出失败"))
        
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(UserProfileUiEvent.ExportProfile(ExportFormat.JSON))
        advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.uiState.value.error?.contains("导出失败") == true)
    }
    
    @Test
    fun `HideExportDialog 事件隐藏导出对话框`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(UserProfileUiEvent.ShowExportDialog)
        
        // When
        viewModel.onEvent(UserProfileUiEvent.HideExportDialog)
        
        // Then
        assertFalse(viewModel.uiState.value.showExportDialog)
    }
    
    @Test
    fun `clearExportResult 清除导出结果`() = runTest {
        // Given
        coEvery { exportUserProfileUseCase(any()) } returns Result.success("content")
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(UserProfileUiEvent.ExportProfile(ExportFormat.JSON))
        advanceUntilIdle()
        
        // When
        viewModel.clearExportResult()
        
        // Then
        assertNull(viewModel.exportResult.value)
    }
    
    // ========== 其他操作测试 ==========
    
    @Test
    fun `SwitchTab 事件切换标签页`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(UserProfileUiEvent.SwitchTab(1))
        
        // Then
        assertEquals(1, viewModel.uiState.value.selectedTabIndex)
    }
    
    @Test
    fun `ShowResetConfirm 事件显示重置确认对话框`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(UserProfileUiEvent.ShowResetConfirm)
        
        // Then
        assertTrue(viewModel.uiState.value.showResetConfirmDialog)
    }
    
    @Test
    fun `ConfirmResetProfile 事件成功重置画像`() = runTest {
        // Given
        val profile = createTestProfile()
        val emptyProfile = UserProfile()
        
        coEvery { getUserProfileUseCase(any()) } returns Result.success(profile) andThen Result.success(emptyProfile)
        coEvery { userProfileRepository.clearUserProfile() } returns Result.success(Unit)
        
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(UserProfileUiEvent.ShowResetConfirm)
        
        // When
        viewModel.onEvent(UserProfileUiEvent.ConfirmResetProfile)
        advanceUntilIdle()
        
        // Then
        assertFalse(viewModel.uiState.value.showResetConfirmDialog)
        assertEquals("画像已重置", viewModel.uiState.value.successMessage)
        coVerify { userProfileRepository.clearUserProfile() }
    }
    
    @Test
    fun `ConfirmResetProfile 事件失败时显示错误`() = runTest {
        // Given
        coEvery { userProfileRepository.clearUserProfile() } returns Result.failure(Exception("重置失败"))
        
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(UserProfileUiEvent.ShowResetConfirm)
        
        // When
        viewModel.onEvent(UserProfileUiEvent.ConfirmResetProfile)
        advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.uiState.value.error?.contains("重置失败") == true)
    }
    
    @Test
    fun `HideResetConfirm 事件隐藏重置确认对话框`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(UserProfileUiEvent.ShowResetConfirm)
        
        // When
        viewModel.onEvent(UserProfileUiEvent.HideResetConfirm)
        
        // Then
        assertFalse(viewModel.uiState.value.showResetConfirmDialog)
    }
    
    @Test
    fun `ClearError 事件清除错误消息`() = runTest {
        // Given
        coEvery { getUserProfileUseCase(any()) } returns Result.failure(Exception("错误"))
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(UserProfileUiEvent.ClearError)
        
        // Then
        assertNull(viewModel.uiState.value.error)
    }
    
    @Test
    fun `ClearSuccessMessage 事件清除成功消息`() = runTest {
        // Given
        val updatedProfile = createTestProfile()
        coEvery { addTagUseCase(any(), any()) } returns Result.success(updatedProfile)
        
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(UserProfileUiEvent.AddTag("PERSONALITY_TRAITS", "新标签"))
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(UserProfileUiEvent.ClearSuccessMessage)
        
        // Then
        assertNull(viewModel.uiState.value.successMessage)
    }
    
    @Test
    fun `RefreshProfile 事件强制刷新画像`() = runTest {
        // Given
        val profile = createTestProfile()
        coEvery { getUserProfileUseCase(true) } returns Result.success(profile)
        
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(UserProfileUiEvent.RefreshProfile)
        advanceUntilIdle()
        
        // Then
        coVerify { getUserProfileUseCase(true) }
        assertEquals(profile, viewModel.uiState.value.profile)
    }
    
    // ========== 辅助方法 ==========
    
    private fun createTestProfile(): UserProfile {
        return UserProfile(
            personalityTraits = listOf("内向"),
            values = listOf("诚实"),
            interests = listOf("阅读"),
            communicationStyle = listOf("直接"),
            socialPreferences = listOf("小圈子"),
            customDimensions = mapOf("职业技能" to listOf("编程"))
        )
    }

}
