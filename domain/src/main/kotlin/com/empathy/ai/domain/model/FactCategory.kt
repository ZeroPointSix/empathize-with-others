package com.empathy.ai.domain.model

/**
 * 分类颜色配置
 *
 * @property titleColor 分类标题颜色（ARGB格式）
 * @property tagBackgroundColor 标签背景颜色（ARGB格式）
 * @property tagTextColor 标签文字颜色（ARGB格式）
 */
data class CategoryColor(
    val titleColor: Long,
    val tagBackgroundColor: Long,
    val tagTextColor: Long
)

/**
 * 事实分类分组模型
 *
 * 用于将Fact按key字段分组显示，支持折叠/展开和颜色标识
 *
 * @property key 分类名称（对应Fact的key字段）
 * @property facts 该分类下的标签列表
 * @property color 分类颜色配置
 * @property isExpanded 是否展开显示
 */
data class FactCategory(
    val key: String,
    val facts: List<Fact>,
    val color: CategoryColor,
    val isExpanded: Boolean = true
) {
    init {
        require(key.isNotBlank()) { "分类名称不能为空" }
    }

    /**
     * 分类下的标签数量
     */
    val factCount: Int
        get() = facts.size

    /**
     * 分类是否为空
     */
    val isEmpty: Boolean
        get() = facts.isEmpty()

    /**
     * 获取所有Fact的ID列表
     */
    fun getFactIds(): List<String> = facts.map { it.id }

    /**
     * 检查分类是否包含指定ID的Fact
     *
     * @param factId 要检查的Fact ID
     * @return 是否包含
     */
    fun containsFact(factId: String): Boolean = facts.any { it.id == factId }

    /**
     * 切换展开/折叠状态
     *
     * @return 新的FactCategory实例
     */
    fun toggleExpanded(): FactCategory = copy(isExpanded = !isExpanded)
}
