package com.empathy.ai.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.ExportFormat
import com.empathy.ai.domain.repository.UserProfileRepository
import com.empathy.ai.domain.usecase.AddTagUseCase
import com.empathy.ai.domain.usecase.ExportUserProfileUseCase
import com.empathy.ai.domain.usecase.GetUserProfileUseCase
import com.empathy.ai.domain.usecase.ManageCustomDimensionUseCase
import com.empathy.ai.domain.usecase.RemoveTagUseCase
import com.empathy.ai.domain.usecase.UpdateUserProfileUseCase
import com.empathy.ai.presentation.ui.screen.userprofile.UserProfileUiEvent
import com.empathy.ai.presentation.ui.screen.userprofile.UserProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 用户画像ViewModel
 *
 * 管理用户画像界面的状态和业务逻辑。
 */
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val addTagUseCase: AddTagUseCase,
    private val removeTagUseCase: RemoveTagUseCase,
    private val manageCustomDimensionUseCase: ManageCustomDimensionUseCase,
    private val exportUserProfileUseCase: ExportUserProfileUseCase,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "UserProfileViewModel"
    }
    
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()
    
    // 导出结果（用于分享）
    private val _exportResult = MutableStateFlow<String?>(null)
    val exportResult: StateFlow<String?> = _exportResult.asStateFlow()
    
    init {
        loadUserProfile()
    }
    
    /**
     * 处理UI事件
     */
    fun onEvent(event: UserProfileUiEvent) {
        when (event) {
            // 标签操作
            is UserProfileUiEvent.ShowAddTagDialog -> showAddTagDialog(event.dimensionKey)
            is UserProfileUiEvent.AddTag -> addTag(event.dimensionKey, event.tag)
            is UserProfileUiEvent.ShowEditTagDialog -> showEditTagDialog(event.dimensionKey, event.tag)
            is UserProfileUiEvent.EditTag -> editTag(event.dimensionKey, event.oldTag, event.newTag)
            is UserProfileUiEvent.ShowDeleteTagConfirm -> showDeleteTagConfirm(event.dimensionKey, event.tag)
            is UserProfileUiEvent.ConfirmDeleteTag -> confirmDeleteTag()
            is UserProfileUiEvent.HideTagDialog -> hideTagDialog()
            
            // 维度操作
            is UserProfileUiEvent.ShowAddDimensionDialog -> showAddDimensionDialog()
            is UserProfileUiEvent.AddDimension -> addDimension(event.dimensionName)
            is UserProfileUiEvent.ShowDeleteDimensionConfirm -> showDeleteDimensionConfirm(event.dimensionName)
            is UserProfileUiEvent.ConfirmDeleteDimension -> confirmDeleteDimension()
            is UserProfileUiEvent.HideDimensionDialog -> hideDimensionDialog()
            
            // 导出操作
            is UserProfileUiEvent.ShowExportDialog -> showExportDialog()
            is UserProfileUiEvent.ExportProfile -> exportProfile(event.format)
            is UserProfileUiEvent.HideExportDialog -> hideExportDialog()
            
            // 其他操作
            is UserProfileUiEvent.SwitchTab -> switchTab(event.tabIndex)
            is UserProfileUiEvent.ShowResetConfirm -> showResetConfirm()
            is UserProfileUiEvent.ConfirmResetProfile -> confirmResetProfile()
            is UserProfileUiEvent.HideResetConfirm -> hideResetConfirm()
            is UserProfileUiEvent.ClearError -> clearError()
            is UserProfileUiEvent.ClearSuccessMessage -> clearSuccessMessage()
            is UserProfileUiEvent.RefreshProfile -> loadUserProfile(forceRefresh = true)
            
            // BUG-00037: 编辑模式操作
            is UserProfileUiEvent.LocalAddTag -> localAddTag(event.dimensionKey, event.tag)
            is UserProfileUiEvent.LocalEditTag -> localEditTag(event.dimensionKey, event.oldTag, event.newTag)
            is UserProfileUiEvent.LocalDeleteTag -> localDeleteTag(event.dimensionKey, event.tag)
            is UserProfileUiEvent.SaveAllChanges -> saveAllChanges()
            is UserProfileUiEvent.ShowDiscardChangesDialog -> showDiscardChangesDialog()
            is UserProfileUiEvent.ConfirmDiscardChanges -> confirmDiscardChanges()
            is UserProfileUiEvent.HideDiscardChangesDialog -> hideDiscardChangesDialog()
        }
    }
    
    /**
     * 加载用户画像
     */
    private fun loadUserProfile(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            getUserProfileUseCase(forceRefresh).fold(
                onSuccess = { profile ->
                    _uiState.update { it.copy(profile = profile, isLoading = false) }
                },
                onFailure = { e ->
                    Log.e(TAG, "加载用户画像失败", e)
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "加载失败: ${e.message}"
                        )
                    }
                }
            )
        }
    }
    
    // ==================== 标签操作 ====================
    
    private fun showAddTagDialog(dimensionKey: String) {
        _uiState.update { 
            it.copy(
                showAddTagDialog = true,
                currentEditDimension = dimensionKey
            )
        }
    }
    
    private fun addTag(dimensionKey: String, tag: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            addTagUseCase(dimensionKey, tag).fold(
                onSuccess = { profile ->
                    _uiState.update { 
                        it.copy(
                            profile = profile,
                            isLoading = false,
                            showAddTagDialog = false,
                            currentEditDimension = null,
                            successMessage = "标签添加成功"
                        )
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "添加标签失败", e)
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "添加标签失败"
                        )
                    }
                }
            )
        }
    }
    
    private fun showEditTagDialog(dimensionKey: String, tag: String) {
        _uiState.update { 
            it.copy(
                showEditTagDialog = true,
                currentEditDimension = dimensionKey,
                currentEditTag = tag
            )
        }
    }
    
    private fun editTag(dimensionKey: String, oldTag: String, newTag: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // 先删除旧标签，再添加新标签
            removeTagUseCase(dimensionKey, oldTag).fold(
                onSuccess = {
                    addTagUseCase(dimensionKey, newTag).fold(
                        onSuccess = { profile ->
                            _uiState.update { 
                                it.copy(
                                    profile = profile,
                                    isLoading = false,
                                    showEditTagDialog = false,
                                    currentEditDimension = null,
                                    currentEditTag = null,
                                    successMessage = "标签修改成功"
                                )
                            }
                        },
                        onFailure = { e ->
                            Log.e(TAG, "添加新标签失败", e)
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    error = e.message ?: "修改标签失败"
                                )
                            }
                        }
                    )
                },
                onFailure = { e ->
                    Log.e(TAG, "删除旧标签失败", e)
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "修改标签失败"
                        )
                    }
                }
            )
        }
    }
    
    private fun showDeleteTagConfirm(dimensionKey: String, tag: String) {
        _uiState.update { 
            it.copy(
                showDeleteConfirmDialog = true,
                currentEditDimension = dimensionKey,
                pendingDeleteTag = tag
            )
        }
    }
    
    private fun confirmDeleteTag() {
        val dimensionKey = _uiState.value.currentEditDimension ?: return
        val tag = _uiState.value.pendingDeleteTag ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            removeTagUseCase(dimensionKey, tag).fold(
                onSuccess = { profile ->
                    _uiState.update { 
                        it.copy(
                            profile = profile,
                            isLoading = false,
                            showDeleteConfirmDialog = false,
                            currentEditDimension = null,
                            pendingDeleteTag = null,
                            successMessage = "标签删除成功"
                        )
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "删除标签失败", e)
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "删除标签失败"
                        )
                    }
                }
            )
        }
    }
    
    private fun hideTagDialog() {
        _uiState.update { 
            it.copy(
                showAddTagDialog = false,
                showEditTagDialog = false,
                showDeleteConfirmDialog = false,
                currentEditDimension = null,
                currentEditTag = null,
                pendingDeleteTag = null
            )
        }
    }
    
    // ==================== 维度操作 ====================
    
    private fun showAddDimensionDialog() {
        _uiState.update { it.copy(showAddDimensionDialog = true) }
    }
    
    private fun addDimension(dimensionName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            manageCustomDimensionUseCase.addDimension(dimensionName).fold(
                onSuccess = { profile ->
                    _uiState.update { 
                        it.copy(
                            profile = profile,
                            isLoading = false,
                            showAddDimensionDialog = false,
                            successMessage = "维度添加成功"
                        )
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "添加维度失败", e)
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "添加维度失败"
                        )
                    }
                }
            )
        }
    }
    
    private fun showDeleteDimensionConfirm(dimensionName: String) {
        _uiState.update { 
            it.copy(
                showDeleteConfirmDialog = true,
                pendingDeleteDimension = dimensionName
            )
        }
    }
    
    private fun confirmDeleteDimension() {
        val dimensionName = _uiState.value.pendingDeleteDimension ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            manageCustomDimensionUseCase.removeDimension(dimensionName).fold(
                onSuccess = { profile ->
                    _uiState.update { 
                        it.copy(
                            profile = profile,
                            isLoading = false,
                            showDeleteConfirmDialog = false,
                            pendingDeleteDimension = null,
                            successMessage = "维度删除成功"
                        )
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "删除维度失败", e)
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "删除维度失败"
                        )
                    }
                }
            )
        }
    }
    
    private fun hideDimensionDialog() {
        _uiState.update { 
            it.copy(
                showAddDimensionDialog = false,
                showDeleteConfirmDialog = false,
                pendingDeleteDimension = null
            )
        }
    }
    
    // ==================== 导出操作 ====================
    
    private fun showExportDialog() {
        _uiState.update { it.copy(showExportDialog = true) }
    }
    
    private fun exportProfile(format: ExportFormat) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            exportUserProfileUseCase(format).fold(
                onSuccess = { content ->
                    _exportResult.value = content
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            showExportDialog = false,
                            successMessage = "导出成功"
                        )
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "导出失败", e)
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "导出失败"
                        )
                    }
                }
            )
        }
    }
    
    private fun hideExportDialog() {
        _uiState.update { it.copy(showExportDialog = false) }
    }
    
    /**
     * 清除导出结果
     */
    fun clearExportResult() {
        _exportResult.value = null
    }
    
    // ==================== 其他操作 ====================
    
    private fun switchTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTabIndex = tabIndex) }
    }
    
    private fun showResetConfirm() {
        _uiState.update { it.copy(showResetConfirmDialog = true) }
    }
    
    private fun confirmResetProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            userProfileRepository.clearUserProfile().fold(
                onSuccess = {
                    loadUserProfile(forceRefresh = true)
                    _uiState.update { 
                        it.copy(
                            showResetConfirmDialog = false,
                            successMessage = "画像已重置"
                        )
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "重置画像失败", e)
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "重置失败"
                        )
                    }
                }
            )
        }
    }
    
    private fun hideResetConfirm() {
        _uiState.update { it.copy(showResetConfirmDialog = false) }
    }
    
    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    private fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
    
    // ==================== BUG-00037: 编辑模式操作 ====================
    
    /**
     * 本地添加标签（不立即保存到Repository）
     * 
     * 只更新pendingChanges，不触发Repository调用，避免列表刷新
     */
    private fun localAddTag(dimensionKey: String, tag: String) {
        val currentTags = _uiState.value.getTagsForDimension(dimensionKey).toMutableList()
        if (tag !in currentTags) {
            currentTags.add(tag)
            val newPendingChanges = _uiState.value.pendingChanges.toMutableMap()
            newPendingChanges[dimensionKey] = currentTags
            _uiState.update { 
                it.copy(
                    pendingChanges = newPendingChanges,
                    hasUnsavedChanges = true,
                    showAddTagDialog = false,
                    currentEditDimension = null
                )
            }
        }
    }
    
    /**
     * 本地编辑标签（不立即保存到Repository）
     */
    private fun localEditTag(dimensionKey: String, oldTag: String, newTag: String) {
        val currentTags = _uiState.value.getTagsForDimension(dimensionKey).toMutableList()
        val index = currentTags.indexOf(oldTag)
        if (index >= 0) {
            currentTags[index] = newTag
            val newPendingChanges = _uiState.value.pendingChanges.toMutableMap()
            newPendingChanges[dimensionKey] = currentTags
            _uiState.update { 
                it.copy(
                    pendingChanges = newPendingChanges,
                    hasUnsavedChanges = true,
                    showEditTagDialog = false,
                    currentEditDimension = null,
                    currentEditTag = null
                )
            }
        }
    }
    
    /**
     * 本地删除标签（不立即保存到Repository）
     */
    private fun localDeleteTag(dimensionKey: String, tag: String) {
        val currentTags = _uiState.value.getTagsForDimension(dimensionKey).toMutableList()
        currentTags.remove(tag)
        val newPendingChanges = _uiState.value.pendingChanges.toMutableMap()
        newPendingChanges[dimensionKey] = currentTags
        _uiState.update { 
            it.copy(
                pendingChanges = newPendingChanges,
                hasUnsavedChanges = true,
                showDeleteConfirmDialog = false,
                currentEditDimension = null,
                pendingDeleteTag = null
            )
        }
    }
    
    /**
     * 保存所有变更到Repository
     */
    private fun saveAllChanges() {
        if (!_uiState.value.hasUnsavedChanges) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val pendingChanges = _uiState.value.pendingChanges
                var currentProfile = _uiState.value.profile
                
                // 逐个维度更新
                for ((dimensionKey, tags) in pendingChanges) {
                    // 先清除该维度的所有标签
                    val existingTags = currentProfile.getTagsForDimension(dimensionKey)
                    for (tag in existingTags) {
                        removeTagUseCase(dimensionKey, tag).getOrNull()
                    }
                    // 再添加新标签
                    for (tag in tags) {
                        addTagUseCase(dimensionKey, tag).fold(
                            onSuccess = { profile -> currentProfile = profile },
                            onFailure = { e -> Log.e(TAG, "添加标签失败: $tag", e) }
                        )
                    }
                }
                
                // 重新加载最新数据
                loadUserProfile(forceRefresh = true)
                
                _uiState.update { 
                    it.copy(
                        pendingChanges = emptyMap(),
                        hasUnsavedChanges = false,
                        isLoading = false,
                        successMessage = "保存成功"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "保存变更失败", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "保存失败: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * 显示放弃编辑确认对话框
     */
    private fun showDiscardChangesDialog() {
        _uiState.update { it.copy(showDiscardChangesDialog = true) }
    }
    
    /**
     * 确认放弃编辑
     */
    private fun confirmDiscardChanges() {
        _uiState.update { 
            it.copy(
                pendingChanges = emptyMap(),
                pendingCustomDimensions = emptyMap(),
                hasUnsavedChanges = false,
                showDiscardChangesDialog = false
            )
        }
    }
    
    /**
     * 隐藏放弃编辑确认对话框
     */
    private fun hideDiscardChangesDialog() {
        _uiState.update { it.copy(showDiscardChangesDialog = false) }
    }
}
