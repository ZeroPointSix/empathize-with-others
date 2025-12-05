package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.usecase.DeleteBrainTagUseCase
import com.empathy.ai.domain.usecase.GetBrainTagsUseCase
import com.empathy.ai.domain.usecase.SaveBrainTagUseCase
import com.empathy.ai.presentation.ui.screen.tag.BrainTagUiEvent
import com.empathy.ai.presentation.ui.screen.tag.BrainTagUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 标签管理界面的ViewModel
 *
 * 职责：
 * 1. 管理标签管理界面的 UI 状态
 * 2. 处理用户交互事件
 * 3. 调用 UseCase 执行业务逻辑
 * 4. 标签的增删查改操作
 */
@HiltViewModel
class BrainTagViewModel @Inject constructor(
    private val getBrainTagsUseCase: GetBrainTagsUseCase,
    private val saveBrainTagUseCase: SaveBrainTagUseCase,
    private val deleteBrainTagUseCase: DeleteBrainTagUseCase
) : ViewModel() {

    // 私有可变状态（只能内部修改）
    private val _uiState = MutableStateFlow(BrainTagUiState())

    // 公开不可变状态（外部只读）
    val uiState: StateFlow<BrainTagUiState> = _uiState.asStateFlow()

    init {
        // ViewModel创建时自动加载数据
        loadTags()
    }

    /**
     * 统一的事件处理入口
     *
     * 设计意图：
     * 1. 单一入口，便于追踪和调试
     * 2. when 表达式确保处理所有事件类型
     */
    fun onEvent(event: BrainTagUiEvent) {
        when (event) {
            // === 数据加载事件 ===
            is BrainTagUiEvent.LoadTags -> loadTags()
            is BrainTagUiEvent.RefreshTags -> refreshTags()

            // === 搜索相关事件 ===
            is BrainTagUiEvent.UpdateSearchQuery -> updateSearchQuery(event.query)
            is BrainTagUiEvent.ClearSearch -> clearSearch()

            // === 标签操作事件 ===
            is BrainTagUiEvent.DeleteTag -> deleteTag(event.tagId)

            // === 添加标签对话框事件 ===
            is BrainTagUiEvent.ShowAddDialog -> showAddDialog()
            is BrainTagUiEvent.HideAddDialog -> hideAddDialog()
            is BrainTagUiEvent.UpdateNewTagContent -> updateNewTagContent(event.content)
            is BrainTagUiEvent.UpdateSelectedTagType -> updateSelectedTagType(event.type)
            is BrainTagUiEvent.ConfirmAddTag -> confirmAddTag()

            // === 通用事件 ===
            is BrainTagUiEvent.ClearError -> clearError()
            is BrainTagUiEvent.NavigateBack -> navigateBack()
        }
    }

    /**
     * 加载标签列表
     *
     * 注意：这里使用空字符串作为contactId，表示加载所有标签
     * 实际项目中可能需要根据具体需求调整
     */
    private fun loadTags() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // 收集标签数据流
                // 注意：这里传入空字符串，实际使用时可能需要传入具体的contactId
                getBrainTagsUseCase("").collect { tags ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            tags = tags,
                            filteredTags = tags
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "加载标签失败"
                    )
                }
            }
        }
    }

    /**
     * 刷新标签列表
     */
    private fun refreshTags() {
        loadTags()
    }

    // === 搜索相关方法 ===

    private fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        // 实时搜索
        if (query.isNotBlank()) {
            performSearch(query)
        } else {
            clearSearchResults()
        }
    }

    private fun performSearch(query: String) {
        val currentState = _uiState.value
        val filteredTags = currentState.tags.filter { tag ->
            tag.content.contains(query, ignoreCase = true)
        }

        _uiState.update {
            it.copy(
                isSearching = true,
                filteredTags = filteredTags
            )
        }
    }

    private fun clearSearchResults() {
        val currentState = _uiState.value
        _uiState.update {
            it.copy(
                isSearching = false,
                filteredTags = currentState.tags
            )
        }
    }

    private fun clearSearch() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                isSearching = false,
                filteredTags = it.tags
            )
        }
    }

    // === 标签操作方法 ===

    private fun deleteTag(tagId: Long) {
        viewModelScope.launch {
            try {
                deleteBrainTagUseCase(tagId).onSuccess {
                    // 删除成功，Flow会自动更新UI
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

    // === 添加标签对话框方法 ===

    private fun showAddDialog() {
        _uiState.update {
            it.copy(
                showAddDialog = true,
                newTagContent = "",
                selectedTagType = "STRATEGY_GREEN"
            )
        }
    }

    private fun hideAddDialog() {
        _uiState.update {
            it.copy(
                showAddDialog = false,
                newTagContent = "",
                error = null
            )
        }
    }

    private fun updateNewTagContent(content: String) {
        _uiState.update { it.copy(newTagContent = content) }
    }

    private fun updateSelectedTagType(type: TagType) {
        _uiState.update { it.copy(selectedTagType = type.name) }
    }

    private fun confirmAddTag() {
        val currentState = _uiState.value
        val content = currentState.newTagContent.trim()

        // 验证标签内容
        if (content.isEmpty()) {
            _uiState.update { it.copy(error = "标签内容不能为空") }
            return
        }

        // 检查重复
        if (currentState.tags.any { it.content == content }) {
            _uiState.update { it.copy(error = "标签已存在") }
            return
        }

        viewModelScope.launch {
            try {
                val tagType = try {
                    TagType.valueOf(currentState.selectedTagType)
                } catch (e: IllegalArgumentException) {
                    TagType.STRATEGY_GREEN
                }

                val newTag = BrainTag(
                    id = 0, // 由数据库生成
                    contactId = "", // 这里使用空字符串，实际使用时可能需要传入具体的contactId
                    content = content,
                    type = tagType,
                    source = "MANUAL"
                )

                saveBrainTagUseCase(newTag).onSuccess {
                    // 标签保存成功，隐藏对话框
                    hideAddDialog()
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

    // === 通用方法 ===

    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun navigateBack() {
        _uiState.update { it.copy(shouldNavigateBack = true) }
    }
}
