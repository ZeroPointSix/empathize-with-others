package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.AiSummaryResponse

/**
 * AI总结响应解析器接口
 *
 * 用于在Domain层抽象AI总结响应的解析功能。
 * 实现类在Data层提供，可以使用Moshi或其他JSON库。
 *
 * @see TDD-00017 Clean Architecture模块化改造技术设计
 */
interface AiSummaryResponseParser {
    /**
     * 解析AI响应为总结结果
     *
     * @param jsonResponse AI返回的JSON响应
     * @return 解析后的总结响应，解析失败返回null
     */
    fun parse(jsonResponse: String): AiSummaryResponse?
}
