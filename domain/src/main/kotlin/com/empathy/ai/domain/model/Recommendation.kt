package com.empathy.ai.domain.model

/**
 * 推荐项
 *
 * 承载相关推荐的数据，用于知识查询结果中的延伸阅读推荐。
 *
 * 业务背景 (PRD-00031):
 * - 知识查询结果需要推荐3-5条相关话题
 * - 用户点击推荐项可以发起新的查询
 *
 * @property id 唯一标识
 * @property title 推荐标题
 * @property description 简短描述（可选）
 * @property query 用于重新查询的关键词
 *
 * @see PRD-00031 悬浮窗快速知识回答功能需求
 * @see KnowledgeQueryResponse
 */
data class Recommendation(
    val id: String,
    val title: String,
    val description: String? = null,
    val query: String
) {
    companion object {
        /**
         * 从标题创建推荐项
         *
         * @param title 推荐标题，同时作为查询关键词
         * @return 新的推荐项实例
         */
        fun fromTitle(title: String): Recommendation = Recommendation(
            id = "rec_${title.hashCode()}_${System.currentTimeMillis()}",
            title = title,
            description = null,
            query = title
        )
        
        /**
         * 从标题和描述创建推荐项
         *
         * @param title 推荐标题
         * @param description 简短描述
         * @return 新的推荐项实例
         */
        fun fromTitleAndDescription(title: String, description: String): Recommendation = Recommendation(
            id = "rec_${title.hashCode()}_${System.currentTimeMillis()}",
            title = title,
            description = description,
            query = title
        )
        
        /**
         * 从标题列表批量创建推荐项
         *
         * @param titles 标题列表
         * @return 推荐项列表
         */
        fun fromTitles(titles: List<String>): List<Recommendation> = 
            titles.map { fromTitle(it) }
    }
}
