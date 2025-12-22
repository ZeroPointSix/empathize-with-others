package com.empathy.ai.presentation.ui.screen.contact

import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.model.ConversationTopic
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FilterType
import com.empathy.ai.domain.model.RelationshipLevel
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.model.TimelineItem
import com.empathy.ai.domain.model.ViewMode

/**
 * 联系人详情UI状态
 *
 * 包含所有界面需要的数据和状态
 *
 * 设计理念：
 * - 单一数据源：所有UI状态集中管理
 * - 不可变性：使用data class确保状态不可变
 * - 派生状态：通过计算属性提供派生数据
 */
data class ContactDetailUiState(
    // ========== 基础标识 ==========
    
    /**
     * 联系人ID
     */
    val contactId: String = "",
    
    /**
     * 联系人信息
     */
    val contact: ContactProfile? = null,
    
    /**
     * 原始联系人信息（用于编辑时对比）
     */
    val originalProfile: ContactProfile? = null,
    
    /**
     * 编辑中的联系人信息
     */
    val editedProfile: ContactProfile? = null,
    
    // ========== 编辑模式相关 ==========
    
    /**
     * 是否处于编辑模式
     */
    val isEditMode: Boolean = false,
    
    /**
     * 是否为新建联系人
     */
    val isNewContact: Boolean = false,
    
    /**
     * 是否正在保存
     */
    val isSaving: Boolean = false,
    
    /**
     * 是否有未保存的更改
     */
    val hasUnsavedChanges: Boolean = false,
    
    /**
     * 是否应该返回上一页
     */
    val shouldNavigateBack: Boolean = false,
    
    /**
     * 是否应该导航到聊天页
     */
    val shouldNavigateToChat: Boolean = false,
    
    // ========== 表单字段 ==========
    
    /**
     * 联系人姓名
     */
    val name: String = "",
    
    /**
     * 沟通目标
     */
    val targetGoal: String = "",
    
    /**
     * 上下文深度
     */
    val contextDepth: Int = 10,
    
    // ========== 表单验证错误 ==========
    
    /**
     * 姓名验证错误
     */
    val nameError: String? = null,
    
    /**
     * 目标验证错误
     */
    val targetGoalError: String? = null,
    
    /**
     * 上下文深度验证错误
     */
    val contextDepthError: String? = null,
    
    /**
     * 新事实Key验证错误
     */
    val factKeyError: String? = null,
    
    /**
     * 新事实Value验证错误
     */
    val factValueError: String? = null,
    
    // ========== 新事实输入 ==========
    
    /**
     * 新事实的Key
     */
    val newFactKey: String = "",
    
    /**
     * 新事实的Value
     */
    val newFactValue: String = "",
    
    // ========== 标签相关 ==========
    
    /**
     * 标签列表
     */
    val brainTags: List<BrainTag> = emptyList(),
    
    /**
     * 筛选后的标签列表
     */
    val filteredBrainTags: List<BrainTag> = emptyList(),
    
    /**
     * 标签搜索关键词
     */
    val tagSearchQuery: String = "",
    
    /**
     * 是否正在搜索标签
     */
    val isSearchingTags: Boolean = false,
    
    /**
     * 选中的标签类型筛选
     */
    val selectedTagTypes: Set<TagType> = emptySet(),
    
    // ========== 对话框状态 ==========
    
    /**
     * 是否显示删除确认对话框
     */
    val showDeleteConfirmDialog: Boolean = false,
    
    /**
     * 是否显示未保存更改对话框
     */
    val showUnsavedChangesDialog: Boolean = false,
    
    /**
     * 是否显示添加事实对话框
     */
    val showAddFactDialog: Boolean = false,
    
    /**
     * 是否显示添加标签对话框
     */
    val showAddTagDialog: Boolean = false,
    
    /**
     * 添加标签对话框 - 标签内容
     */
    val newTagContent: String = "",
    
    /**
     * 添加标签对话框 - 选中的标签类型
     */
    val newTagType: TagType = TagType.STRATEGY_GREEN,
    
    /**
     * 添加标签对话框 - 内容验证错误
     */
    val newTagContentError: String? = null,
    
    // ========== 关系进展（阶段6新增） ==========
    
    /**
     * 是否正在加载关系数据
     */
    val isLoadingRelationship: Boolean = false,
    
    /**
     * 关系分数
     */
    val relationshipScore: Int = 0,
    
    /**
     * 关系等级
     */
    val relationshipLevel: RelationshipLevel = RelationshipLevel.STRANGER,
    
    /**
     * 关系趋势
     */
    val relationshipTrend: RelationshipTrend = RelationshipTrend.STABLE,
    
    /**
     * 最后互动日期
     */
    val lastInteractionDate: String? = null,
    
    // ========== 数据列表 ==========
    
    /**
     * 对话记录列表
     */
    val conversations: List<ConversationLog> = emptyList(),
    
    /**
     * 每日总结列表
     */
    val summaries: List<DailySummary> = emptyList(),
    
    /**
     * 事实标签列表
     */
    val facts: List<Fact> = emptyList(),
    
    // ========== 界面一：概览 ==========
    
    /**
     * 权重最高的标签（最多5个）
     */
    val topTags: List<Fact> = emptyList(),
    
    /**
     * 最新的事实记录
     */
    val latestFact: Fact? = null,
    
    /**
     * 相识天数
     */
    val daysSinceFirstMet: Int = 0,
    
    // ========== 界面二：事实流 ==========
    
    /**
     * 当前选中的标签页
     */
    val currentTab: DetailTab = DetailTab.Overview,
    
    /**
     * 视图模式（时光轴/清单列表）
     */
    val viewMode: ViewMode = ViewMode.Timeline,
    
    /**
     * 选中的筛选条件
     */
    val selectedFilters: Set<FilterType> = emptySet(),
    
    /**
     * 时间线项目列表
     */
    val timelineItems: List<TimelineItem> = emptyList(),
    
    // ========== 界面三：标签画像 ==========
    
    /**
     * 按类别分组的标签
     */
    val groupedFacts: Map<String, List<Fact>> = emptyMap(),
    
    /**
     * 已确认标签数量
     */
    val confirmedTagsCount: Int = 0,
    
    /**
     * AI推测标签数量
     */
    val guessedTagsCount: Int = 0,
    
    // ========== 界面四：资料库 ==========
    
    /**
     * 对话记录数量
     */
    val conversationCount: Int = 0,
    
    /**
     * 总结数量
     */
    val summaryCount: Int = 0,
    
    // ========== 状态管理 ==========
    
    /**
     * 是否正在加载
     */
    val isLoading: Boolean = false,
    
    /**
     * 是否正在刷新
     */
    val isRefreshing: Boolean = false,
    
    /**
     * 错误信息
     */
    val error: String? = null,
    
    /**
     * 成功提示信息
     */
    val successMessage: String? = null,
    
    // ========== 对话记录编辑相关 ==========
    
    /**
     * 是否显示编辑对话对话框
     */
    val showEditConversationDialog: Boolean = false,
    
    /**
     * 当前选中的对话记录ID
     */
    val selectedConversationId: Long? = null,
    
    /**
     * 编辑中的对话内容
     */
    val editingConversationContent: String = "",
    
    /**
     * 是否显示添加事实到事实流对话框
     */
    val showAddFactToStreamDialog: Boolean = false,
    
    // ========== 对话主题相关（TD-00016） ==========
    
    /**
     * 是否显示主题设置对话框
     */
    val showTopicDialog: Boolean = false,
    
    /**
     * 当前活跃的对话主题
     */
    val currentTopic: ConversationTopic? = null,
    
    /**
     * 主题历史记录
     */
    val topicHistory: List<ConversationTopic> = emptyList(),
    
    /**
     * 主题输入内容
     */
    val topicInputContent: String = "",
    
    /**
     * 是否正在保存主题
     */
    val isSavingTopic: Boolean = false,
    
    // ========== 编辑功能相关（TD-00012） ==========
    
    /**
     * 是否显示编辑事实对话框
     */
    val showEditFactDialog: Boolean = false,
    
    /**
     * 当前编辑的事实
     */
    val editingFact: Fact? = null,
    
    /**
     * 是否显示编辑总结对话框
     */
    val showEditSummaryDialog: Boolean = false,
    
    /**
     * 当前编辑的总结ID
     */
    val editingSummaryId: Long? = null,
    
    /**
     * 当前编辑的总结内容
     */
    val editingSummaryContent: String = "",
    
    /**
     * 是否显示编辑联系人信息对话框
     */
    val showEditContactInfoDialog: Boolean = false,
    
    /**
     * 是否显示删除事实确认对话框
     */
    val showDeleteFactConfirmDialog: Boolean = false,
    
    /**
     * 待删除的事实ID
     */
    val pendingDeleteFactId: Long? = null,
    
    /**
     * 是否显示删除总结确认对话框
     */
    val showDeleteSummaryConfirmDialog: Boolean = false,
    
    /**
     * 待删除的总结ID
     */
    val pendingDeleteSummaryId: Long? = null,
    
    // ========== 标签画像V2相关（TD-00014） ==========
    
    /**
     * 分类列表（用于PersonaTabV2）
     */
    val factCategories: List<com.empathy.ai.domain.model.FactCategory> = emptyList(),
    
    /**
     * 标签画像搜索状态
     */
    val personaSearchState: com.empathy.ai.domain.model.PersonaSearchState = com.empathy.ai.domain.model.PersonaSearchState(),
    
    /**
     * 编辑模式状态
     */
    val editModeState: com.empathy.ai.domain.model.EditModeState = com.empathy.ai.domain.model.EditModeState(),
    
    /**
     * 可用的分类列表（用于移动对话框）
     */
    val availableCategories: List<String> = emptyList(),
    
    /**
     * 是否使用PersonaTabV2
     */
    val usePersonaTabV2: Boolean = false
) {
    // ========== 派生状态 ==========
    
    /**
     * 是否有数据
     */
    val hasData: Boolean
        get() = contact != null || originalProfile != null
    
    /**
     * 是否为空状态
     */
    val isEmpty: Boolean
        get() = !hasData && !isLoading
    
    /**
     * 是否可以保存
     */
    val canSave: Boolean
        get() = name.isNotBlank() && nameError == null && targetGoalError == null
    
    /**
     * 显示用的标签列表
     */
    val displayTags: List<BrainTag>
        get() = if (isSearchingTags) filteredBrainTags else brainTags
    
    /**
     * 筛选后的时间线项目
     */
    val filteredTimelineItems: List<TimelineItem>
        get() {
            if (selectedFilters.isEmpty() || selectedFilters.contains(FilterType.ALL)) {
                return timelineItems
            }
            return timelineItems.filter { item ->
                selectedFilters.any { filter ->
                    when (filter) {
                        FilterType.ALL -> true
                        FilterType.AI_SUMMARY -> item is TimelineItem.AiSummary
                        FilterType.CONFLICT -> item.emotionType == com.empathy.ai.domain.model.EmotionType.CONFLICT
                        FilterType.DATE -> item.emotionType == com.empathy.ai.domain.model.EmotionType.DATE
                        FilterType.SWEET -> item.emotionType == com.empathy.ai.domain.model.EmotionType.SWEET
                    }
                }
            }
        }
}
