package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.RelationshipLevel
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.model.FilterType
import com.empathy.ai.domain.model.ViewMode
import com.empathy.ai.domain.repository.DailySummaryRepository
import com.empathy.ai.presentation.ui.screen.contact.DetailTab
import com.empathy.ai.domain.usecase.DeleteBrainTagUseCase
import com.empathy.ai.domain.usecase.DeleteContactUseCase
import com.empathy.ai.domain.usecase.GetBrainTagsUseCase
import com.empathy.ai.domain.usecase.GetContactUseCase
import com.empathy.ai.domain.usecase.SaveBrainTagUseCase
import com.empathy.ai.domain.usecase.SaveProfileUseCase
import com.empathy.ai.domain.util.MemoryConstants
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailUiEvent
import com.empathy.ai.presentation.ui.screen.contact.ContactDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val saveProfileUseCase: SaveProfileUseCase,
    private val dailySummaryRepository: DailySummaryRepository
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
     * 2. 使用 if-else 链而非 when 表达式，避免 Kotlin 编译器的类型推断问题
     */
    fun onEvent(event: ContactDetailUiEvent) {
        // === 数据加载事件 ===
        if (event is ContactDetailUiEvent.LoadContact) {
            loadContact(event.contactId)
        } else if (event is ContactDetailUiEvent.ReloadContact) {
            reloadContact()
        } else if (event is ContactDetailUiEvent.LoadRelationshipData) {
            loadRelationshipData(event.contactId)
        } else if (event is ContactDetailUiEvent.LoadBrainTags) {
            loadBrainTags(event.contactId)
        }
        // === 编辑相关事件 ===
        else if (event is ContactDetailUiEvent.StartEdit) {
            startEdit()
        } else if (event is ContactDetailUiEvent.CancelEdit) {
            cancelEdit()
        } else if (event is ContactDetailUiEvent.SaveContact) {
            saveContact()
        } else if (event is ContactDetailUiEvent.DeleteContact) {
            deleteContact()
        }
        // === 表单字段更新事件 ===
        else if (event is ContactDetailUiEvent.UpdateName) {
            updateName(event.name)
        } else if (event is ContactDetailUiEvent.UpdateTargetGoal) {
            updateTargetGoal(event.targetGoal)
        } else if (event is ContactDetailUiEvent.UpdateContextDepth) {
            updateContextDepth(event.contextDepth)
        }
        // === 事实管理事件 ===
        else if (event is ContactDetailUiEvent.AddFact) {
            addFact(event.key, event.value)
        } else if (event is ContactDetailUiEvent.UpdateFact) {
            updateFact(event.key, event.value)
        } else if (event is ContactDetailUiEvent.DeleteFact) {
            deleteFact(event.key)
        } else if (event is ContactDetailUiEvent.DeleteFactItem) {
            deleteFactItem(event.fact)
        } else if (event is ContactDetailUiEvent.UpdateNewFactKey) {
            updateNewFactKey(event.key)
        } else if (event is ContactDetailUiEvent.UpdateNewFactValue) {
            updateNewFactValue(event.value)
        }
        // === 标签管理事件 ===
        else if (event is ContactDetailUiEvent.AddBrainTag) {
            addBrainTag(event.tag, event.type)
        } else if (event is ContactDetailUiEvent.DeleteBrainTag) {
            deleteBrainTag(event.tagId)
        } else if (event is ContactDetailUiEvent.UpdateTagSearchQuery) {
            updateTagSearchQuery(event.query)
        } else if (event is ContactDetailUiEvent.ToggleTagTypeSelection) {
            toggleTagTypeSelection(event.tagType)
        } else if (event is ContactDetailUiEvent.StartTagSearch) {
            startTagSearch()
        } else if (event is ContactDetailUiEvent.ClearTagSearch) {
            clearTagSearch()
        }
        // === 对话框事件 ===
        else if (event is ContactDetailUiEvent.ShowDeleteConfirmDialog) {
            showDeleteConfirmDialog()
        } else if (event is ContactDetailUiEvent.HideDeleteConfirmDialog) {
            hideDeleteConfirmDialog()
        } else if (event is ContactDetailUiEvent.ShowUnsavedChangesDialog) {
            showUnsavedChangesDialog()
        } else if (event is ContactDetailUiEvent.HideUnsavedChangesDialog) {
            hideUnsavedChangesDialog()
        } else if (event is ContactDetailUiEvent.ShowAddFactDialog) {
            showAddFactDialog()
        } else if (event is ContactDetailUiEvent.HideAddFactDialog) {
            hideAddFactDialog()
        } else if (event is ContactDetailUiEvent.ShowAddTagDialog) {
            showAddTagDialog()
        } else if (event is ContactDetailUiEvent.HideAddTagDialog) {
            hideAddTagDialog()
        } else if (event is ContactDetailUiEvent.UpdateNewTagContent) {
            updateNewTagContent(event.content)
        } else if (event is ContactDetailUiEvent.UpdateNewTagType) {
            updateNewTagType(event.type)
        } else if (event is ContactDetailUiEvent.ConfirmAddTag) {
            confirmAddTag()
        }
        // === 对话主题事件（TD-00016） ===
        else if (event is ContactDetailUiEvent.ShowTopicDialog) {
            showTopicDialog()
        } else if (event is ContactDetailUiEvent.HideTopicDialog) {
            hideTopicDialog()
        }
        // === 字段验证事件 ===
        else if (event is ContactDetailUiEvent.ValidateName) {
            validateName()
        } else if (event is ContactDetailUiEvent.ValidateTargetGoal) {
            validateTargetGoal()
        } else if (event is ContactDetailUiEvent.ValidateContextDepth) {
            validateContextDepth()
        } else if (event is ContactDetailUiEvent.ValidateFactKey) {
            validateFactKey()
        } else if (event is ContactDetailUiEvent.ValidateFactValue) {
            validateFactValue()
        }
        // === Tab页面事件 ===
        else if (event is ContactDetailUiEvent.SwitchTab) {
            switchTab(event.tab)
        } else if (event is ContactDetailUiEvent.SwitchViewMode) {
            switchViewMode(event.mode)
        } else if (event is ContactDetailUiEvent.ToggleFilter) {
            toggleFilter(event.filter)
        } else if (event is ContactDetailUiEvent.ConfirmTag) {
            confirmTag(event.factId)
        } else if (event is ContactDetailUiEvent.RejectTag) {
            rejectTag(event.factId)
        }
        // === 通用事件 ===
        else if (event is ContactDetailUiEvent.ClearError) {
            clearError()
        } else if (event is ContactDetailUiEvent.ClearSuccessMessage) {
            clearSuccessMessage()
        } else if (event is ContactDetailUiEvent.ResetForm) {
            resetForm()
        } else if (event is ContactDetailUiEvent.NavigateToChat) {
            navigateToChat(event.contactId)
        } else if (event is ContactDetailUiEvent.NavigateBack) {
            navigateBack()
        } else if (event is ContactDetailUiEvent.ConfirmNavigateBack) {
            confirmNavigateBack()
        } else if (event is ContactDetailUiEvent.RefreshData) {
            refreshData()
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
                    // 新建联系人 - 生成临时ID以支持标签添加
                    val newContactId = UUID.randomUUID().toString()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEditMode = true,
                            isNewContact = true,
                            contactId = newContactId  // 设置临时ID
                        )
                    }
                    // 启动标签监听，确保新建联系人时添加的标签能正确显示
                    loadBrainTags(newContactId)
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
            // 使用已生成的临时 contactId，确保与 BrainTag 的 contactId 一致
            id = currentState.contactId.ifBlank { UUID.randomUUID().toString() },
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

    /**
     * 添加事实
     *
     * BUG-00017修复：允许相同key的多个事实，只对完全相同的key+value组合去重
     * 
     * 修改原因：
     * - 原逻辑使用key作为唯一标识，导致相同类型的事实被覆盖
     * - 用户可能需要记录多个"性格特点"或"兴趣爱好"
     * - 现在只有key和value都相同时才视为重复
     */
    private fun addFact(key: String, value: String) {
        if (key.isBlank() || value.isBlank()) return

        val currentState = _uiState.value
        val newFacts = currentState.facts.toMutableList()
        
        // BUG-00017修复：基于key+value组合去重，而非仅key
        // 允许相同key的多个不同value的事实
        val isDuplicate = newFacts.any { it.key == key && it.value == value }
        if (!isDuplicate) {
            newFacts.add(
                Fact(
                    key = key,
                    value = value,
                    timestamp = System.currentTimeMillis(),
                    source = com.empathy.ai.domain.model.FactSource.MANUAL
                )
            )
        }

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
        val newFacts = currentState.facts.toMutableList()

        if (value.isBlank()) {
            // 删除该Fact
            newFacts.removeAll { it.key == key }
        } else {
            // 更新该Fact
            val existingIndex = newFacts.indexOfFirst { it.key == key }
            if (existingIndex >= 0) {
                newFacts[existingIndex] = newFacts[existingIndex].copy(
                    value = value,
                    timestamp = System.currentTimeMillis()
                )
            }
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
        val newFacts = currentState.facts.toMutableList()
        newFacts.removeAll { it.key == key }

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

    private fun addBrainTag(tag: String, type: TagType) {
        if (tag.isBlank()) return

        viewModelScope.launch {
            try {
                val currentState = _uiState.value

                val brainTag = BrainTag(
                    id = 0, // 由数据库生成
                    contactId = currentState.contactId,
                    content = tag,
                    type = type,
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
            val matchesType = selectedTypes.isEmpty() || selectedTypes.contains(tag.type)
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
                selectedTypes.contains(tag.type)
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

    private fun toggleTagTypeSelection(tagType: TagType) {
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
                    val matchesType = newSelection.isEmpty() || newSelection.contains(tag.type)
                    matchesQuery && matchesType
                }
            } else if (newSelection.isEmpty()) {
                currentState.brainTags
            } else {
                currentState.brainTags.filter { tag ->
                    newSelection.contains(tag.type)
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
        _uiState.update { 
            it.copy(
                showAddTagDialog = false,
                newTagContent = "",
                newTagType = TagType.STRATEGY_GREEN,
                newTagContentError = null
            ) 
        }
    }

    // === 对话主题对话框管理方法（TD-00016） ===

    private fun showTopicDialog() {
        _uiState.update { 
            it.copy(
                showTopicDialog = true,
                topicInputContent = it.currentTopic?.content ?: ""
            ) 
        }
    }

    private fun hideTopicDialog() {
        _uiState.update { 
            it.copy(
                showTopicDialog = false,
                topicInputContent = ""
            ) 
        }
    }

    private fun updateNewTagContent(content: String) {
        _uiState.update { 
            it.copy(
                newTagContent = content,
                newTagContentError = null
            ) 
        }
    }

    private fun updateNewTagType(type: TagType) {
        _uiState.update { it.copy(newTagType = type) }
    }

    private fun confirmAddTag() {
        val currentState = _uiState.value
        
        // 验证内容
        if (currentState.newTagContent.isBlank()) {
            _uiState.update { it.copy(newTagContentError = "标签内容不能为空") }
            return
        }
        
        // 调用添加标签方法
        addBrainTag(currentState.newTagContent, currentState.newTagType)
        
        // 关闭对话框并重置状态
        hideAddTagDialog()
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
        } else if (currentState.facts.any { it.key == currentState.newFactKey }) {
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
        
        // 空值检查：name为空时不更新editedProfile，避免触发require断言
        if (currentState.name.isBlank()) {
            return
        }
        
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
                    facts = emptyList(),
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
        _uiState.update { it.copy(shouldNavigateToChat = true) }
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

    // === 关系进展相关方法（阶段6新增）===

    /**
     * 加载关系进展数据
     */
    private fun loadRelationshipData(contactId: String) {
        if (contactId.isBlank()) return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingRelationship = true) }

                // 获取联系人信息
                val profile = getContactUseCase(contactId).getOrNull()
                if (profile != null) {
                    val score = profile.relationshipScore
                    val level = calculateRelationshipLevel(score)
                    val trend = calculateRelationshipTrend(contactId)

                    _uiState.update {
                        it.copy(
                            isLoadingRelationship = false,
                            relationshipScore = score,
                            relationshipLevel = level,
                            relationshipTrend = trend,
                            lastInteractionDate = profile.lastInteractionDate,
                            facts = profile.facts
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingRelationship = false) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingRelationship = false,
                        error = e.message ?: "加载关系数据失败"
                    )
                }
            }
        }
    }

    /**
     * 根据分数计算关系等级
     */
    private fun calculateRelationshipLevel(score: Int): RelationshipLevel {
        return when {
            score <= MemoryConstants.STRANGER_THRESHOLD -> RelationshipLevel.STRANGER
            score <= MemoryConstants.ACQUAINTANCE_THRESHOLD -> RelationshipLevel.ACQUAINTANCE
            score <= MemoryConstants.FAMILIAR_THRESHOLD -> RelationshipLevel.FAMILIAR
            else -> RelationshipLevel.CLOSE
        }
    }

    /**
     * 计算关系趋势
     * 基于最近7天的总结记录分析趋势
     */
    private suspend fun calculateRelationshipTrend(contactId: String): RelationshipTrend {
        return try {
            val recentSummaries = dailySummaryRepository.getRecentSummaries(
                contactId = contactId,
                days = 7
            )

            if (recentSummaries.size < 2) {
                return RelationshipTrend.STABLE
            }

            // 计算分数变化总和
            val totalChange = recentSummaries.sumOf { it.relationshipScoreChange }

            when {
                totalChange > 5 -> RelationshipTrend.IMPROVING
                totalChange < -5 -> RelationshipTrend.DECLINING
                else -> RelationshipTrend.STABLE
            }
        } catch (e: Exception) {
            RelationshipTrend.STABLE
        }
    }

    /**
     * 删除Fact事实
     */
    private fun deleteFactItem(fact: Fact) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val newFacts = currentState.facts.filter { it.key != fact.key }

                // 更新联系人的facts
                val profile = currentState.originalProfile?.copy(
                    facts = newFacts
                )

                if (profile != null) {
                    saveProfileUseCase(profile).onSuccess {
                        _uiState.update {
                            it.copy(
                                facts = newFacts,
                                originalProfile = profile
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(error = error.message ?: "删除事实失败")
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "删除事实失败")
                }
            }
        }
    }

    // === Tab页面相关方法 ===

    /**
     * 切换Tab页面
     */
    private fun switchTab(tab: DetailTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    /**
     * 切换视图模式
     */
    private fun switchViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    /**
     * 切换筛选条件
     */
    private fun toggleFilter(filter: FilterType) {
        _uiState.update { currentState ->
            val newFilters = if (currentState.selectedFilters.contains(filter)) {
                currentState.selectedFilters - filter
            } else {
                currentState.selectedFilters + filter
            }
            currentState.copy(selectedFilters = newFilters)
        }
    }

    /**
     * 确认标签
     */
    private fun confirmTag(factId: Long) {
        // TODO: 实现标签确认逻辑
        _uiState.update { it.copy(successMessage = "标签已确认") }
    }

    /**
     * 拒绝标签
     */
    private fun rejectTag(factId: Long) {
        // TODO: 实现标签拒绝逻辑
        _uiState.update { it.copy(successMessage = "标签已拒绝") }
    }

    /**
     * 清除成功消息
     */
    private fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    /**
     * 刷新数据
     */
    private fun refreshData() {
        val currentState = _uiState.value
        if (currentState.contactId.isNotBlank()) {
            loadContact(currentState.contactId)
        }
    }
}

