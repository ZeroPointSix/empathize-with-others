package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.model.TagType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 智能上下文构建器
 *
 * 负责为AI分析构建智能上下文，包括：
 * - 分层筛选相关Facts（最多20条）
 * - 优先使用最近7天的Facts
 * - 整合所有BrainTag标签
 * - 构建结构化的分析Prompt
 *
 * 业务背景 (PRD-00003):
 * - 联系人画像包含大量Facts（用户属性、聊天记录推断等）
 * - 受限于AI Token消耗，不能将所有Facts发送给AI
 * - 需要智能筛选最相关、最新的Facts
 *
 * 筛选策略（分层优先级）:
 * 1. 手动添加的Facts（最高优先级）- 用户主动维护，更可靠
 * 2. 最近7天的AI推断Facts - 新鲜信息，最具参考价值
 * 3. 最近30天的AI推断Facts - 较新，但可能需要验证
 * 4. 更早的Facts（按时间倒序）- 补充背景信息
 *
 * 设计权衡 (TDD-00003):
 * - 限制20条Facts：平衡信息量和Token消耗
 * - 时间分层：确保新鲜信息优先
 * - 手动优先：用户维护的信息更可靠
 */
@Singleton
class ContextBuilder @Inject constructor(
    private val logger: Logger
) {
    companion object {
        private const val TAG = "ContextBuilder"
    }

    /**
     * 筛选相关Facts
     *
     * 使用分层筛选策略，确保最重要的Facts被优先选中。
     *
     * 算法说明:
     * 1. 如果Facts数量<=maxCount，全部返回（无需筛选）
     * 2. 按优先级分层筛选，每层不超过剩余配额
     * 3. 手动Facts优先 → 最近7天AI → 最近30天AI → 更早AI
     *
     * @param allFacts 所有Facts列表
     * @param maxCount 最大返回数量，默认20
     * @return 筛选后的Facts列表（已按时间倒序排列）
     */
    fun selectRelevantFacts(
        allFacts: List<Fact>,
        maxCount: Int = MemoryConstants.MAX_FACTS_COUNT
    ): List<Fact> {
        if (allFacts.isEmpty()) return emptyList()
        if (allFacts.size <= maxCount) return allFacts.sortedByDescending { it.timestamp }

        val now = System.currentTimeMillis()
        val recentThreshold = now - MemoryConstants.RECENT_DAYS * MemoryConstants.ONE_DAY_MILLIS
        val mediumThreshold = now - MemoryConstants.MEDIUM_DAYS * MemoryConstants.ONE_DAY_MILLIS

        val selectedFacts = mutableListOf<Fact>()

        // 第1层：手动添加的Facts（最高优先级）
        val manualFacts = allFacts
            .filter { it.source == FactSource.MANUAL }
            .sortedByDescending { it.timestamp }
        selectedFacts.addAll(manualFacts.take(maxCount))

        if (selectedFacts.size >= maxCount) {
            return selectedFacts.take(maxCount)
        }

        // 第2层：最近7天的AI推断Facts
        val recentAiFacts = allFacts
            .filter { it.source == FactSource.AI_INFERRED && it.timestamp >= recentThreshold }
            .filter { it !in selectedFacts }
            .sortedByDescending { it.timestamp }
        selectedFacts.addAll(recentAiFacts.take(maxCount - selectedFacts.size))

        if (selectedFacts.size >= maxCount) {
            return selectedFacts.take(maxCount)
        }

        // 第3层：最近30天的AI推断Facts
        val mediumAiFacts = allFacts
            .filter {
                it.source == FactSource.AI_INFERRED &&
                    it.timestamp >= mediumThreshold &&
                    it.timestamp < recentThreshold
            }
            .filter { it !in selectedFacts }
            .sortedByDescending { it.timestamp }
        selectedFacts.addAll(mediumAiFacts.take(maxCount - selectedFacts.size))

        if (selectedFacts.size >= maxCount) {
            return selectedFacts.take(maxCount)
        }

        // 第4层：更早的Facts（按时间倒序）
        val olderFacts = allFacts
            .filter { it !in selectedFacts }
            .sortedByDescending { it.timestamp }
        selectedFacts.addAll(olderFacts.take(maxCount - selectedFacts.size))

        return selectedFacts.take(maxCount)
    }

    /**
     * 构建完整的分析上下文
     *
     * 将联系人画像、标签、对话历史整合为结构化的Prompt文本。
     *
     * 上下文结构:
     * 1. 【联系人信息】- 姓名、关系等级、关系分数、最后互动
     * 2. 【攻略目标】- 用户设定的沟通目标
     * 3. 【已知信息】- 筛选后的Facts（标注来源）
     * 4. 【雷区警告】- RISK_RED标签（AI推断/手动）
     * 5. 【策略建议】- STRATEGY_GREEN标签（AI推断/手动）
     * 6. 【聊天记录】- 用户提供的对话内容
     *
     * @param profile 联系人画像
     * @param brainTags 标签列表
     * @param conversationHistory 对话历史
     * @return 结构化的Prompt字符串
     */
    fun buildAnalysisContext(
        profile: ContactProfile,
        brainTags: List<BrainTag>,
        conversationHistory: List<String>
    ): String {
        val relevantFacts = selectRelevantFacts(profile.facts)
        val redTags = brainTags.filter { it.type == TagType.RISK_RED }
        val greenTags = brainTags.filter { it.type == TagType.STRATEGY_GREEN }

        // 添加详细日志用于调试AI上下文传递
        logger.d(TAG, "=== 构建AI分析上下文 ===")
        logger.d(TAG, "联系人: ${profile.name}")
        logger.d(TAG, "原始Facts数量: ${profile.facts.size}")
        logger.d(TAG, "筛选后Facts数量: ${relevantFacts.size}")
        logger.d(TAG, "雷区标签数量: ${redTags.size}")
        logger.d(TAG, "策略标签数量: ${greenTags.size}")
        logger.d(TAG, "对话历史数量: ${conversationHistory.size}")

        // 详细记录Facts内容
        if (relevantFacts.isNotEmpty()) {
            logger.d(TAG, "--- Facts详情 ---")
            relevantFacts.forEachIndexed { index, fact ->
                logger.d(TAG, "  [$index] ${fact.key}: ${fact.value} (来源: ${fact.source})")
            }
        }

        // 详细记录标签内容
        if (redTags.isNotEmpty()) {
            logger.d(TAG, "--- 雷区标签详情 ---")
            redTags.forEach { tag ->
                logger.d(TAG, "  - ${tag.content}")
            }
        }
        if (greenTags.isNotEmpty()) {
            logger.d(TAG, "--- 策略标签详情 ---")
            greenTags.forEach { tag ->
                logger.d(TAG, "  - ${tag.content}")
            }
        }

        val context = buildString {
            // 基本信息
            appendLine("【联系人信息】")
            appendLine("姓名: ${profile.name}")
            appendLine("关系等级: ${profile.getRelationshipLevel().displayName}")
            appendLine("关系分数: ${profile.relationshipScore}/100")
            profile.lastInteractionDate?.let {
                appendLine("最后互动: $it")
            }
            appendLine()

            // 攻略目标
            appendLine("【攻略目标】")
            appendLine(profile.targetGoal ?: "维护良好关系")
            appendLine()

            // 已知信息（筛选后的Facts）
            if (relevantFacts.isNotEmpty()) {
                appendLine("【已知信息】(${relevantFacts.size}条)")
                relevantFacts.forEach { fact ->
                    val sourceTag = if (fact.source == FactSource.MANUAL) "[手动]" else "[AI]"
                    appendLine("- ${fact.key}: ${fact.value} $sourceTag")
                }
                appendLine()
            }

            // 雷区警告
            if (redTags.isNotEmpty()) {
                appendLine("【雷区警告】(${redTags.size}条)")
                redTags.forEach { tag ->
                    val sourceTag = if (tag.isAiInferred) "[AI推断]" else "[手动]"
                    appendLine("- ${tag.content} $sourceTag")
                }
                appendLine()
            }

            // 策略建议
            if (greenTags.isNotEmpty()) {
                appendLine("【策略建议】(${greenTags.size}条)")
                greenTags.forEach { tag ->
                    val sourceTag = if (tag.isAiInferred) "[AI推断]" else "[手动]"
                    appendLine("- ${tag.content} $sourceTag")
                }
                appendLine()
            }

            // 聊天记录
            appendLine("【聊天记录】(${conversationHistory.size}条)")
            conversationHistory.forEach { message ->
                appendLine(message)
            }
        }

        // 记录最终构建的上下文长度
        logger.d(TAG, "=== 上下文构建完成 ===")
        logger.d(TAG, "上下文总长度: ${context.length} 字符")
        logger.d(TAG, "上下文预览(前500字符): ${context.take(500)}")

        return context
    }

    /**
     * 构建每日总结的Prompt
     *
     * 用于AI总结每日对话，提取关键信息。
     *
     * @param profile 联系人画像
     * @param conversations 当日对话列表
     * @return 总结Prompt字符串
     */
    fun buildSummaryPrompt(
        profile: ContactProfile,
        conversations: List<String>
    ): String {
        // 构建已知信息部分
        val factsSection = if (profile.facts.isNotEmpty()) {
            profile.facts.take(10).joinToString("\n") { fact ->
                "- ${fact.key}: ${fact.value}"
            }
        } else {
            ""
        }

        // 构建对话记录部分
        val conversationsSection = conversations.joinToString("\n")

        // 使用模板构建Prompt
        return PromptTemplates.buildSummaryPrompt(
            contactName = profile.name,
            targetGoal = profile.targetGoal ?: "维护良好关系",
            relationshipScore = profile.relationshipScore,
            factsSection = factsSection,
            conversationsSection = conversationsSection
        )
    }
}
