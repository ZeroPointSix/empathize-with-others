package com.empathy.ai.domain.service

/**
 * 隐私脱敏引擎
 *
 * 负责在发送数据给 AI 之前进行脱敏处理，确保敏感信息不被泄露。
 * 这是隐私优先原则（PRD核心原则）的核心实现。
 *
 * 业务背景:
 *   - PRD: 共情AI助手 - 隐私绝对优先原则
 *   - 场景: 用户与AI交互时，敏感数据(手机号、身份证、邮箱等)需脱敏后再发送
 *
 * 设计决策:
 *   - TDD-00007: 提供两种脱敏模式，基于映射规则 + 基于正则自动检测
 *   - 混合模式: 先应用用户自定义映射，再执行自动检测，双重保护
 *
 * 核心能力:
 *   1. 基于映射规则的替换（用户主动标记的敏感词）
 *   2. 基于正则表达式的自动检测与脱敏（手机号、身份证号、邮箱）
 *
 * @see PrivacyRepository 隐私仓库接口，提供用户配置的隐私映射规则
 */
object PrivacyEngine {

    /**
     * 敏感信息正则模式定义
     *
     * 使用正则表达式自动识别常见敏感信息格式。
     * 中国大陆标准：11位手机号(1开头)、18位身份证号、标准邮箱格式
     *
     * 为什么不支持其他类型?
     *   - 银行卡号: 各银行长度不一，校验规则复杂，Luhn算法更适合
     *   - 姓名: 无法通过正则可靠识别，依赖用户主动标记
     *   - 地址: 格式过于灵活，正则误报率高
     *
     * 权衡: 优先覆盖高风险、易识别的敏感信息类型
     */
    object Patterns {
        /** 中国大陆手机号：11位，1开头 */
        val PHONE_NUMBER = "1[3-9]\\d{9}".toRegex()

        /** 中国大陆身份证号：18位（支持15位旧版转18位场景） */
        val ID_CARD = "\\d{17}[\\dXx]".toRegex()

        /** 邮箱地址 */
        val EMAIL = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}".toRegex()

        /** 完整模式集合 */
        val ALL_PATTERNS = mapOf(
            "手机号" to PHONE_NUMBER,
            "身份证号" to ID_CARD,
            "邮箱" to EMAIL
        )
    }

    /**
     * 对文本进行脱敏处理（基于映射规则）
     */
    fun mask(rawText: String, privacyMapping: Map<String, String>): String {
        var maskedText = rawText
        privacyMapping.forEach { (original, mask) ->
            maskedText = maskedText.replace(original, mask, ignoreCase = true)
        }
        return maskedText
    }

    /**
     * 对文本列表进行批量脱敏处理
     */
    fun maskBatch(rawTexts: List<String>, privacyMapping: Map<String, String>): List<String> {
        return rawTexts.map { mask(it, privacyMapping) }
    }

    /**
     * 基于正则表达式自动检测敏感信息并脱敏
     *
     * 算法思路:
     *   1. 使用正则 findAll 找出所有匹配位置
     *   2. 为每个匹配分配序号(index)，支持同类型多值区分
     *   3. 按位置倒序替换，避免替换时影响后续位置索引
     *
     * 为什么要倒序替换?
     *   - 正则替换会改变字符串长度，导致后续位置索引失效
     *   - 从后往前替换不影响前面的字符位置
     *
     * @param rawText 原始文本
     * @param pattern 正则表达式
     * @param maskFormat 脱敏格式，支持{index}占位符，如"[MASK_1]"
     */
    fun maskByPattern(
        rawText: String,
        pattern: Regex,
        maskFormat: String = "[MASK_{index}]"
    ): String {
        var maskedText = rawText
        val matches = pattern.findAll(rawText).toList()
        if (matches.isEmpty()) return rawText

        val matchToIndex = mutableMapOf<MatchResult, Int>()
        matches.forEachIndexed { index, matchResult ->
            matchToIndex[matchResult] = index + 1
        }

        val matchesToReplace = matches.sortedByDescending { it.range.first }
        matchesToReplace.forEach { matchResult ->
            val index = matchToIndex[matchResult] ?: 1
            val mask = maskFormat.replace("{index}", index.toString())
            maskedText = maskedText.replaceRange(
                matchResult.range.first,
                matchResult.range.last + 1,
                mask
            )
        }

        return maskedText
    }

    /**
     * 使用内置模式自动检测并脱敏常见敏感信息
     *
     * 核心逻辑: 解决多种敏感信息可能重叠检测的问题
     *
     * 重叠检测策略:
     *   - 身份证号可能包含手机号后11位，需要避免重复脱敏
     *   - 使用 range.intersects() 检测范围重叠
     *   - 优先保留更长的匹配(如身份证号)，跳过被包含的匹配(如手机号)
     *
     * 执行顺序（按优先级）:
     *   1. 手机号 (最高优先级，11位短匹配)
     *   2. 身份证号 (18位，可能包含手机号)
     *   3. 邮箱 (格式特殊，基本不重叠)
     *
     * @param rawText 原始文本
     * @param enabledPatterns 启用的检测模式列表
     */
    fun maskWithAutoDetection(
        rawText: String,
        enabledPatterns: List<String> = listOf("手机号", "身份证号")
    ): String {
        var maskedText = rawText
        val allMatches = mutableListOf<DetectedPattern>()

        val orderedPatterns = enabledPatterns
            .sortedBy { patternName ->
                when (patternName) {
                    "手机号" -> 1
                    "身份证号" -> 2
                    "邮箱" -> 3
                    else -> 99
                }
            }

        orderedPatterns.forEach { patternName ->
            val regex = Patterns.ALL_PATTERNS[patternName] ?: return@forEach
            regex.findAll(maskedText).forEach { matchResult ->
                val isOverlapping = allMatches.any { existing ->
                    existing.range.first <= matchResult.range.last && matchResult.range.first <= existing.range.last
                }
                if (!isOverlapping) {
                    allMatches.add(
                        DetectedPattern(
                            patternName = patternName,
                            matchedText = matchResult.value,
                            range = matchResult.range
                        )
                    )
                }
            }
        }

        allMatches.sortBy { it.range.first }

        val patternOccurrences = mutableMapOf<String, MutableList<DetectedPattern>>()
        allMatches.forEach { detected ->
            patternOccurrences.getOrPut(detected.patternName) { mutableListOf() }.add(detected)
        }

        patternOccurrences.forEach { (_, occurrences) ->
            occurrences.forEachIndexed { index, detected ->
                detected.index = index + 1
            }
        }

        allMatches.sortByDescending { it.range.first }
        allMatches.forEach { detected ->
            val mask = "[${detected.patternName}_${detected.index}]"
            maskedText = maskedText.replaceRange(
                detected.range.first,
                detected.range.last + 1,
                mask
            )
        }

        return maskedText
    }

    /**
     * 组合映射规则和自动检测的混合脱敏
     */
    fun maskHybrid(
        rawText: String,
        privacyMapping: Map<String, String> = emptyMap(),
        enabledPatterns: List<String> = emptyList()
    ): String {
        var maskedText = mask(rawText, privacyMapping)
        if (enabledPatterns.isNotEmpty()) {
            maskedText = maskWithAutoDetection(maskedText, enabledPatterns)
        }
        return maskedText
    }

    /**
     * 检测的敏感信息
     */
    data class DetectedPattern(
        val patternName: String,
        val matchedText: String,
        val range: IntRange,
        var index: Int = 1
    )

    /**
     * 扫描并返回检测到的敏感信息列表
     *
     * 与 maskWithAutoDetection 的区别:
     *   - maskWithAutoDetection: 直接脱敏并返回脱敏后文本
     *   - detectSensitiveInfo: 仅扫描，返回检测到的敏感信息详情(用于UI提示)
     *
     * @param rawText 原始文本
     * @param enabledPatterns 启用的检测模式列表
     * @return 检测到的敏感信息列表，按文本位置排序
     */
    fun detectSensitiveInfo(
        rawText: String,
        enabledPatterns: List<String> = Patterns.ALL_PATTERNS.keys.toList()
    ): List<DetectedPattern> {
        val allMatches = mutableListOf<DetectedPattern>()

        val orderedPatterns = enabledPatterns
            .sortedBy { patternName ->
                when (patternName) {
                    "手机号" -> 1
                    "身份证号" -> 2
                    "邮箱" -> 3
                    else -> 99
                }
            }

        orderedPatterns.forEach { patternName ->
            val regex = Patterns.ALL_PATTERNS[patternName] ?: return@forEach
            regex.findAll(rawText).forEach { matchResult ->
                allMatches.add(
                    DetectedPattern(
                        patternName = patternName,
                        matchedText = matchResult.value,
                        range = matchResult.range,
                        index = (allMatches.filter { it.patternName == patternName }.maxOfOrNull { it.index } ?: 0) + 1
                    )
                )
            }
        }

        val filteredMatches = allMatches.toMutableList()
        val toRemove = mutableSetOf<DetectedPattern>()

        for (i in filteredMatches.indices) {
            for (j in filteredMatches.indices) {
                if (i != j) {
                    val matchI = filteredMatches[i]
                    val matchJ = filteredMatches[j]
                    if (matchI.range.first <= matchJ.range.first &&
                        matchI.range.last >= matchJ.range.last &&
                        matchI.patternName != "手机号") {
                        toRemove.add(matchJ)
                    }
                }
            }
        }

        filteredMatches.removeAll(toRemove)
        return filteredMatches.sortedBy { it.range.first }
    }
}
