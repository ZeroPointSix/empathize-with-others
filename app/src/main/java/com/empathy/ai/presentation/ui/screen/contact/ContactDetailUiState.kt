package com.empathy.ai.presentation.ui.screen.contact

import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.RelationshipLevel
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.util.MemoryConstants

/**
 * 联系人详情界面的UI状态
 *
 * 设计原则：
 * 1. 所有字段都有默认值，避免空指针
 * 2. 使用 data class，自动获得 copy() 方法
 * 3. 状态是不可变的 (val)，通过 copy() 更新
 * 4. 包含所有UI信息：数据 + 加载状态 + 错误信息 + 编辑状态
 */
data class ContactDetailUiState(
    // 通用状态字段
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,

    // 联系人数据
    val contactId: String = "",
    val originalProfile: ContactProfile? = null,
    val editedProfile: ContactProfile? = null,

    // 编辑状态
    val isEditMode: Boolean = false,
    val hasUnsavedChanges: Boolean = false,

    // 表单字段
    val name: String = "",
    val targetGoal: String = "",
    val contextDepth: Int = 10,
    val newFactKey: String = "",
    val newFactValue: String = "",

    // 标签数据
    val brainTags: List<BrainTag> = emptyList(),
    val filteredBrainTags: List<BrainTag> = emptyList(),
    val selectedTagTypes: Set<String> = emptySet(),

    // 搜索状态
    val tagSearchQuery: String = "",
    val isSearchingTags: Boolean = false,

    // UI交互状态
    val showDeleteConfirmDialog: Boolean = false,
    val showUnsavedChangesDialog: Boolean = false,
    val showAddFactDialog: Boolean = false,
    val showAddTagDialog: Boolean = false,

    // 字段验证状态
    val nameError: String? = null,
    val targetGoalError: String? = null,
    val contextDepthError: String? = null,
    val factKeyError: String? = null,
    val factValueError: String? = null,

    // 导航状态
    val shouldNavigateBack: Boolean = false,
    val shouldNavigateToChat: String = "",

    // 关系进展状态（阶段6新增）
    val relationshipScore: Int = MemoryConstants.DEFAULT_RELATIONSHIP_SCORE,
    val relationshipLevel: RelationshipLevel = RelationshipLevel.ACQUAINTANCE,
    val relationshipTrend: RelationshipTrend = RelationshipTrend.STABLE,
    val lastInteractionDate: String? = null,
    val facts: List<Fact> = emptyList(),
    val isLoadingRelationship: Boolean = false
) {
    // 计算属性：是否为新建联系人
    val isNewContact: Boolean
        get() = contactId.isBlank()

    // 计算属性：是否可以保存
    val canSave: Boolean
        get() = hasUnsavedChanges && !isSaving && isFormValid()

    // 计算属性：是否可以删除
    val canDelete: Boolean
        get() = !isNewContact && originalProfile != null

    // 计算属性：显示的标签列表
    val displayTags: List<BrainTag>
        get() = if (tagSearchQuery.isNotBlank()) filteredBrainTags else brainTags

    // 计算属性：是否显示搜索结果
    val isShowingTagSearchResults: Boolean
        get() = tagSearchQuery.isNotBlank() && isSearchingTags

    // 计算属性：事实数量
    val factsCount: Int
        get() = facts.size
    
    // 计算属性：最近的事实（7天内）
    val recentFacts: List<Fact>
        get() {
            val recentThreshold = System.currentTimeMillis() - 
                MemoryConstants.RECENT_DAYS * MemoryConstants.ONE_DAY_MILLIS
            return facts.filter { it.timestamp >= recentThreshold }
        }

    // 计算属性：标签数量（按类型分组统计）
    val tagStats: Map<String, Int>
        get() = brainTags.groupBy { it.type.name }.mapValues { it.value.size }

    /**
     * 验证表单是否有效
     */
    private fun isFormValid(): Boolean {
        return name.isNotBlank() &&
               targetGoal.isNotBlank() &&
               contextDepth > 0 &&
               nameError == null &&
               targetGoalError == null &&
               contextDepthError == null
    }
}