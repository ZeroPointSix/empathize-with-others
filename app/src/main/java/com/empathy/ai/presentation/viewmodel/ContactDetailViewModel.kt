package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.usecase.GetContactUseCase
import com.empathy.ai.domain.usecase.DeleteContactUseCase
import com.empathy.ai.domain.usecase.GetBrainTagsUseCase
import com.empathy.ai.domain.usecase.SaveBrainTagUseCase
import com.empathy.ai.domain.usecase.DeleteBrainTagUseCase
import com.empathy.ai.domain.usecase.SaveProfileUseCase
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailUiEvent
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * 联系人详情界面的ViewModel
 *
 * 职责：
 * 1. 管理联系人详情的 UI 状态
 * 2. 处理用户交互事件
 * 3. 调用 UseCase 执行业务逻辑
 * 4. 表单验证和数据管理
 * 5. 标签管理和操作
 */
@HiltViewModel
class ContactDetailViewModel @Inject constructor(
    private val getContactUseCase: GetContactUseCase,
    private val deleteContactUseCase: DeleteContactUseCase,
    private val getBrainTagsUseCase: GetBrainTagsUseCase,
    private val saveBrainTagUseCase: SaveBrainTagUseCase,
    private val deleteBrainTagUseCase: DeleteBrainTagUseCase,
    private val saveProfileUseCase: SaveProfileUseCase
) : ViewModel() {

    // 私有可变状态（只能内部修改）
    private val _uiState = MutableStateFlow(ContactDetailUiState())

    // 公开不可变状态（外部只读）
    val uiState: StateFlow<ContactDetailUiState> = _uiState.asStateFlow()

    /**
     * 统一的事件处理入口
     *
     * 设计意图：
     * 1. 单一入口，便于追踪和调试
     * 2. when 表达式确保处理所有事件类型
     */
    fun onEvent(event: ContactDetailUiEvent) {
        when (event) {
            // === 数据加载事件 ===
            is ContactDetailUiEvent.LoadContact -> loadContact(event.contactId)
            is ContactDetailUiEvent.ReloadContact -> reloadContact()

            // === 编辑相关事件 ===
            is ContactDetailUiEvent.StartEdit -> startEdit()
            is ContactDetailUiEvent.CancelEdit -> cancelEdit()
            is ContactDetailUiEvent.SaveContact -> saveContact()
            is ContactDetailUiEvent.DeleteContact -> deleteContact()

            // === 表单字段更新事件 ===
            is ContactDetailUiEvent.UpdateName -> updateName(event.name)
            is ContactDetailUiEvent.UpdateTargetGoal -> updateTargetGoal(event.targetGoal)
            is ContactDetailUiEvent.UpdateContextDepth -> updateContextDepth(event.contextDepth)

            // === 事实管理事件 ===
            is ContactDetailUiEvent.AddFact -> addFact(event.key, event.value)
            is ContactDetailUiEvent.UpdateFact -> updateFact(event.key, event.value)
            is ContactDetailUiEvent.DeleteFact -> deleteFact(event.key)
            is ContactDetailUiEvent.UpdateNewFactKey -> updateNewFactKey(event.key)
            is ContactDetailUiEvent.UpdateNewFactValue -> updateNewFactValue(event.value)

            // === 标签管理事件 ===
            is ContactDetailUiEvent.LoadBrainTags -> loadBrainTags(event.contactId)
            is ContactDetailUiEvent.AddBrainTag -> addBrainTag(event.tag, event.type)
            is ContactDetailUiEvent.DeleteBrainTag -> deleteBrainTag(event.tagId)
            is ContactDetailUiEvent.UpdateTagSearchQuery -> updateTagSearchQuery(event.query)
            is ContactDetailUiEvent.ToggleTagTypeSelection -> toggleTagTypeSelection(event.tagType)
            is ContactDetailUiEvent.StartTagSearch -> startTagSearch()
            is ContactDetailUiEvent.ClearTagSearch -> clearTagSearch()

            // === 对话框事件 ===
            is ContactDetailUiEvent.ShowDeleteConfirmDialog -> showDeleteConfirmDialog()
            is ContactDetailUiEvent.HideDeleteConfirmDialog -> hideDeleteConfirmDialog()
            is ContactDetailUiEvent.ShowUnsavedChangesDialog -> showUnsavedChangesDialog()
            is ContactDetailUiEvent.HideUnsavedChangesDialog -> hideUnsavedChangesDialog()
            is ContactDetailUiEvent.ShowAddFactDialog -> showAddFactDialog()
            is ContactDetailUiEvent.HideAddFactDialog -> hideAddFactDialog()
            is ContactDetailUiEvent.ShowAddTagDialog -> showAddTagDialog()
            is ContactDetailUiEvent.HideAddTagDialog -> hideAddTagDialog()

            // === 字段验证事件 ===
            is ContactDetailUiEvent.ValidateName -> validateName()
            is ContactDetailUiEvent.ValidateTargetGoal -> validateTargetGoal()
            is ContactDetailUiEvent.ValidateContextDepth -> validateContextDepth()
            is ContactDetailUiEvent.ValidateFactKey -> validateFactKey()
            is ContactDetailUiEvent.ValidateFactValue -> validateFactValue()

            // === 通用事件 ===
            is ContactDetailUiEvent.ClearError -> clearError()
            is ContactDetailUiEvent.ResetForm -> resetForm()
            is ContactDetailUiEvent.NavigateToChat -> navigateToChat(event.contactId)
            is ContactDetailUiEvent.NavigateBack -> navigateBack()
            is ContactDetailUiEvent.ConfirmNavigateBack -> confirmNavigateBack()
        }
    }

    /**
     * 加载联系人详情
     */
    private fun loadContact(contactId: String) {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        contactId = contactId,
                        error = null
                    )
                }

                if (contactId.isBlank()) {
                    // 新建联系人
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEditMode = true
                        )
                    }
                    return@launch
                }

                // 加载现有联系人
                val profile = getContactUseCase(contactId).getOrNull()
                if (profile != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            originalProfile = profile,
                            editedProfile = profile,
                            name = profile.name,
                            targetGoal = profile.targetGoal,
                            contextDepth = profile.contextDepth,
                            facts = profile.facts
                        )
                    }

                    // 加载标签
                    loadBrainTags(contactId)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "未找到联系人"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
            }
        }
    }

    /**
     * 重新加载联系人
     */
    private fun reloadContact() {
        val currentState = _uiState.value
        loadContact(currentState.contactId)
    }

    // === 编辑相关方法 ===

    private fun startEdit() {
        val currentState = _uiState.value
        val editedProfile = currentState.originalProfile?.copy(
            name = currentState.name,
            targetGoal = currentState.targetGoal,
            contextDepth = currentState.contextDepth,
            facts = currentState.facts
        ) ?: ContactProfile(
            id = UUID.randomUUID().toString(),
            name = currentState.name,
            targetGoal = currentState.targetGoal,
            contextDepth = currentState.contextDepth,
            facts = currentState.facts
        )

        _uiState.update {
            it.copy(
                isEditMode = true,
                editedProfile = editedProfile
            )
        }
    }

    private fun cancelEdit() {
        val currentState = _uiState.value
        if (currentState.hasUnsavedChanges) {
            showUnsavedChangesDialog()
        } else {
            resetForm()
        }
    }

    private fun saveContact() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true, error = null) }

                val currentState = _uiState.value
                val profileToSave = currentState.editedProfile ?: return@launch

                // 验证表单
                if (!validateForm()) {
                    _uiState.update { it.copy(isSaving = false) }
                    return@launch
                }

                // 调用UseCase保存
                val result = saveProfileUseCase(profileToSave)

                result.onSuccess {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isEditMode = false,
                            hasUnsavedChanges = false,
                            originalProfile = profileToSave,
                            shouldNavigateBack = true
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = error.message ?: "保存失败"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "保存失败"
                    )
                }
            }
        }
    }

    private fun deleteContact() {
        val currentState = _uiState.value
        val contactId = currentState.contactId

        if (contactId.isBlank()) return

        viewModelScope.launch {
            try {
                deleteContactUseCase(contactId).onSuccess {
                    _uiState.update {
                        it.copy(
                            shouldNavigateBack = true
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "删除失败")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "删除失败")
                }
            }
        }
    }

    // === 表单字段更新方法 ===

    private fun updateName(name: String) {
        val currentState = _uiState.value
        val hasChanges = name != currentState.originalProfile?.name

        _uiState.update {
            it.copy(
                name = name,
                hasUnsavedChanges = hasChanges || currentState.hasUnsavedChanges,
                nameError = null
            )
        }

        if (currentState.isEditMode) {
            updateEditedProfile()
        }
    }

    private fun updateTargetGoal(targetGoal: String) {
        val currentState = _uiState.value
        val hasChanges = targetGoal != currentState.originalProfile?.targetGoal

        _uiState.update {
            it.copy(
                targetGoal = targetGoal,
                hasUnsavedChanges = hasChanges || currentState.hasUnsavedChanges,
                targetGoalError = null
            )
        }

        if (currentState.isEditMode) {
            updateEditedProfile()
        }
    }

    private fun updateContextDepth(contextDepth: Int) {
        val currentState = _uiState.value
        val hasChanges = contextDepth != currentState.originalProfile?.contextDepth

        _uiState.update {
            it.copy(
                contextDepth = contextDepth,
                hasUnsavedChanges = hasChanges || currentState.hasUnsavedChanges,
                contextDepthError = null
            )
        }

        if (currentState.isEditMode) {
            updateEditedProfile()
        }
    }

    // === 事实管理方法 ===

    private fun addFact(key: String, value: String) {
        if (key.isBlank() || value.isBlank()) return

        val currentState = _uiState.value
        val newFacts = currentState.facts.toMutableMap()
        newFacts[key] = value

        _uiState.update {
            it.copy(
                facts = newFacts,
                hasUnsavedChanges = true,
                newFactKey = "",
                newFactValue = ""
            )
        }

        if (currentState.isEditMode) {
            updateEditedProfile()
        }
    }

    private fun updateFact(key: String, value: String) {
        if (key.isBlank()) return

        val currentState = _uiState.value
        val newFacts = currentState.facts.toMutableMap()

        if (value.isBlank()) {
            newFacts.remove(key)
        } else {
            newFacts[key] = value
        }

        _uiState.update {
            it.copy(
                facts = newFacts,
                hasUnsavedChanges = true
            )
        }

        if (currentState.isEditMode) {
            updateEditedProfile()
        }
    }

    private fun deleteFact(key: String) {
        val currentState = _uiState.value
        val newFacts = currentState.facts.toMutableMap()
        newFacts.remove(key)

        _uiState.update {
            it.copy(
                facts = newFacts,
                hasUnsavedChanges = true
            )
        }

        if (currentState.isEditMode) {
            updateEditedProfile()
        }
    }

    private fun updateNewFactKey(key: String) {
        _uiState.update { it.copy(newFactKey = key) }
    }

    private fun updateNewFactValue(value: String) {
        _uiState.update { it.copy(newFactValue = value) }
    }

    // === 标签管理方法 ===

    private fun loadBrainTags(contactId: String) {
        if (contactId.isBlank()) return

        viewModelScope.launch {
            try {
                getBrainTagsUseCase(contactId).collect { tags ->
                    _uiState.update {
                        it.copy(
                            brainTags = tags,
                            filteredBrainTags = tags
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "加载标签失败")
                }
            }
        }
    }

    private fun addBrainTag(tag: String, type: String) {
        if (tag.isBlank()) return

        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val tagType = try {
                    TagType.valueOf(type.uppercase())
                } catch (e: IllegalArgumentException) {
                    TagType.STRATEGY_GREEN
                }

                val brainTag = BrainTag(
                    id = 0, // 由数据库生成
                    contactId = currentState.contactId,
                    content = tag,
                    type = tagType,
                    source = "MANUAL"
                )

                saveBrainTagUseCase(brainTag).onSuccess {
                    // 标签保存成功，Flow会自动更新UI
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "添加标签失败")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "添加标签失败")
                }
            }
        }
    }

    private fun deleteBrainTag(tagId: Long) {
        viewModelScope.launch {
            try {
                deleteBrainTagUseCase(tagId).onSuccess {
                    // 标签删除成功，Flow会自动更新UI
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "删除标签失败")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "删除标签失败")
                }
            }
        }
    }

    private fun updateTagSearchQuery(query: String) {
        _uiState.update { it.copy(tagSearchQuery = query) }

        if (query.isNotBlank()) {
            performTagSearch(query)
        } else {
            clearTagSearchResults()
        }
    }

    private fun performTagSearch(query: String) {
        val currentState = _uiState.value
        val selectedTypes = currentState.selectedTagTypes

        val filteredTags = currentState.brainTags.filter { tag ->
            val matchesQuery = tag.content.contains(query, ignoreCase = true)
            val matchesType = selectedTypes.isEmpty() || selectedTypes.contains(tag.type.name)
            matchesQuery && matchesType
        }

        _uiState.update {
            it.copy(
                isSearchingTags = true,
                filteredBrainTags = filteredTags
            )
        }
    }

    private fun clearTagSearchResults() {
        val currentState = _uiState.value
        val selectedTypes = currentState.selectedTagTypes

        val filteredTags = if (selectedTypes.isEmpty()) {
            currentState.brainTags
        } else {
            currentState.brainTags.filter { tag ->
                selectedTypes.contains(tag.type.name)
            }
        }

        _uiState.update {
            it.copy(
                isSearchingTags = false,
                tagSearchQuery = "",
                filteredBrainTags = filteredTags
            )
        }
    }

    private fun toggleTagTypeSelection(tagType: String) {
        _uiState.update { currentState ->
            val newSelection = if (currentState.selectedTagTypes.contains(tagType)) {
                currentState.selectedTagTypes - tagType
            } else {
                currentState.selectedTagTypes + tagType
            }

            // 重新过滤标签
            val filteredTags = if (currentState.tagSearchQuery.isNotBlank()) {
                currentState.brainTags.filter { tag ->
                    val matchesQuery = tag.content.contains(currentState.tagSearchQuery, ignoreCase = true)
                    val matchesType = newSelection.isEmpty() || newSelection.contains(tag.type.name)
                    matchesQuery && matchesType
                }
            } else if (newSelection.isEmpty()) {
                currentState.brainTags
            } else {
                currentState.brainTags.filter { tag ->
                    newSelection.contains(tag.type.name)
                }
            }

            currentState.copy(
                selectedTagTypes = newSelection,
                filteredBrainTags = filteredTags
            )
        }
    }

    private fun startTagSearch() {
        val currentState = _uiState.value
        if (currentState.tagSearchQuery.isNotBlank()) {
            performTagSearch(currentState.tagSearchQuery)
        }
    }

    private fun clearTagSearch() {
        _uiState.update {
            it.copy(
                tagSearchQuery = "",
                isSearchingTags = false,
                selectedTagTypes = emptySet()
            )
        }
        clearTagSearchResults()
    }

    // === 对话框管理方法 ===

    private fun showDeleteConfirmDialog() {
        _uiState.update { it.copy(showDeleteConfirmDialog = true) }
    }

    private fun hideDeleteConfirmDialog() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false) }
    }

    private fun showUnsavedChangesDialog() {
        _uiState.update { it.copy(showUnsavedChangesDialog = true) }
    }

    private fun hideUnsavedChangesDialog() {
        _uiState.update { it.copy(showUnsavedChangesDialog = false) }
    }

    private fun showAddFactDialog() {
        _uiState.update { it.copy(showAddFactDialog = true) }
    }

    private fun hideAddFactDialog() {
        _uiState.update {
            it.copy(
                showAddFactDialog = false,
                newFactKey = "",
                newFactValue = ""
            )
        }
    }

    private fun showAddTagDialog() {
        _uiState.update { it.copy(showAddTagDialog = true) }
    }

    private fun hideAddTagDialog() {
        _uiState.update { it.copy(showAddTagDialog = false) }
    }

    // === 字段验证方法 ===

    private fun validateName() {
        val currentState = _uiState.value
        val nameError = if (currentState.name.isBlank()) {
            "名称不能为空"
        } else null

        _uiState.update { it.copy(nameError = nameError) }
    }

    private fun validateTargetGoal() {
        val currentState = _uiState.value
        val targetGoalError = if (currentState.targetGoal.isBlank()) {
            "目标不能为空"
        } else null

        _uiState.update { it.copy(targetGoalError = targetGoalError) }
    }

    private fun validateContextDepth() {
        val currentState = _uiState.value
        val contextDepthError = if (currentState.contextDepth <= 0) {
            "上下文深度必须大于0"
        } else null

        _uiState.update { it.copy(contextDepthError = contextDepthError) }
    }

    private fun validateFactKey() {
        val currentState = _uiState.value
        val factKeyError = if (currentState.newFactKey.isBlank()) {
            "事实键不能为空"
        } else if (currentState.facts.containsKey(currentState.newFactKey)) {
            "事实键已存在"
        } else null

        _uiState.update { it.copy(factKeyError = factKeyError) }
    }

    private fun validateFactValue() {
        val currentState = _uiState.value
        val factValueError = if (currentState.newFactValue.isBlank()) {
            "事实值不能为空"
        } else null

        _uiState.update { it.copy(factValueError = factValueError) }
    }

    // === 辅助方法 ===

    private fun updateEditedProfile() {
        val currentState = _uiState.value
        val editedProfile = ContactProfile(
            id = currentState.contactId.ifBlank { UUID.randomUUID().toString() },
            name = currentState.name,
            targetGoal = currentState.targetGoal,
            contextDepth = currentState.contextDepth,
            facts = currentState.facts
        )

        _uiState.update { it.copy(editedProfile = editedProfile) }
    }

    private fun validateForm(): Boolean {
        validateName()
        validateTargetGoal()
        validateContextDepth()

        val currentState = _uiState.value
        return currentState.nameError == null &&
               currentState.targetGoalError == null &&
               currentState.contextDepthError == null
    }

    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun resetForm() {
        val currentState = _uiState.value
        val originalProfile = currentState.originalProfile

        if (originalProfile != null) {
            _uiState.update {
                it.copy(
                    isEditMode = false,
                    hasUnsavedChanges = false,
                    name = originalProfile.name,
                    targetGoal = originalProfile.targetGoal,
                    contextDepth = originalProfile.contextDepth,
                    facts = originalProfile.facts,
                    editedProfile = originalProfile,
                    // 清除错误
                    nameError = null,
                    targetGoalError = null,
                    contextDepthError = null,
                    factKeyError = null,
                    factValueError = null
                )
            }
        } else {
            // 新建联系人，重置为默认值
            _uiState.update {
                it.copy(
                    isEditMode = true,
                    hasUnsavedChanges = false,
                    name = "",
                    targetGoal = "",
                    contextDepth = 10,
                    facts = emptyMap(),
                    editedProfile = null,
                    // 清除错误
                    nameError = null,
                    targetGoalError = null,
                    contextDepthError = null,
                    factKeyError = null,
                    factValueError = null
                )
            }
        }
    }

    private fun navigateToChat(contactId: String) {
        _uiState.update { it.copy(shouldNavigateToChat = contactId) }
    }

    private fun navigateBack() {
        val currentState = _uiState.value
        if (currentState.hasUnsavedChanges && currentState.isEditMode) {
            showUnsavedChangesDialog()
        } else {
            _uiState.update { it.copy(shouldNavigateBack = true) }
        }
    }

    private fun confirmNavigateBack() {
        _uiState.update { it.copy(shouldNavigateBack = true) }
    }
}