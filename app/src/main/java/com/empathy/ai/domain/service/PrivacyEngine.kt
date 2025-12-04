package com.empathy.ai.domain.service

import java.util.regex.Pattern

/**
 * 检查两个 IntRange 是否相交
 */
private fun IntRange.intersects(other: IntRange): Boolean {
    return this.first <= other.last && this.last >= other.first
}

/**
 * 隐私脱敏引擎
 *
 * 负责在发送数据给 AI 之前进行脱敏处理
 * 提供两种脱敏模式：
 * 1. 基于映射规则的替换（已有功能）
 * 2. 基于正则表达式的自动检测与脱敏
 */
object PrivacyEngine {

    /**
     * 敏感信息正则模式定义
     *
     * 支持自动检测手机号、身份证号等敏感信息
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
     *
     * @param rawText 原始文本
     * @param privacyMapping 隐私映射规则 (例如: {"真实姓名": "[NAME_01]", "手机号": "[PHONE_01]"})
     * @return 脱敏后的文本
     */
    fun mask(rawText: String, privacyMapping: Map<String, String>): String {
        var maskedText = rawText

        // 遍历所有隐私映射规则，进行替换
        privacyMapping.forEach { (original, mask) ->
            maskedText = maskedText.replace(original, mask, ignoreCase = true)
        }

        return maskedText
    }

    /**
     * 对文本列表进行批量脱敏处理
     *
     * @param rawTexts 原始文本列表
     * @param privacyMapping 隐私映射规则
     * @return 脱敏后的文本列表
     */
    fun maskBatch(rawTexts: List<String>, privacyMapping: Map<String, String>): List<String> {
        return rawTexts.map { mask(it, privacyMapping) }
    }

    /**
     * 基于正则表达式自动检测敏感信息并脱敏
     *
     * @param rawText 原始文本
     * @param pattern 正则表达式模式
     * @param maskFormat 脱敏格式，支持占位符 {index}（例如: "[PHONE_{index}]"）
     * @return 脱敏后的文本
     */
    fun maskByPattern(
        rawText: String,
        pattern: Regex,
        maskFormat: String = "[MASK_{index}]"
    ): String {
        var maskedText = rawText

        // 步骤1: 收集所有匹配项（正序）
        val matches = pattern.findAll(rawText).toList()
        if (matches.isEmpty()) return rawText

        // 步骤2: 建立匹配结果到索引的映射（按出现顺序）
        val matchToIndex = mutableMapOf<MatchResult, Int>()
        matches.forEachIndexed { index, matchResult ->
            matchToIndex[matchResult] = index + 1
        }

        // 步骤3: 按位置倒序排序并执行替换
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
     * @param rawText 原始文本
     * @param enabledPatterns 启用的模式名称（例如: ["手机号", "身份证号"]）
     * @return 脱敏后的文本
     */
    fun maskWithAutoDetection(
        rawText: String,
        enabledPatterns: List<String> = listOf("手机号", "身份证号")
    ): String {
        var maskedText = rawText

        // 步骤1: 检测所有敏感信息（正序）
        val allMatches = mutableListOf<DetectedPattern>()

        // 按模式优先级排序（重要：手机号先于身份证号，避免18位数字被误判）
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
                // 检查是否与已检测项重叠
                val isOverlapping = allMatches.any { existing ->
                    existing.range.intersects(matchResult.range)
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

        // 步骤2: 按文本中出现顺序排序
        allMatches.sortBy { it.range.first }

        // 步骤3: 为每个模式类型分配索引（按出现顺序）
        val patternOccurrences = mutableMapOf<String, MutableList<DetectedPattern>>()
        allMatches.forEach { detected ->
            patternOccurrences.getOrPut(detected.patternName) { mutableListOf() }.add(detected)
        }

        patternOccurrences.forEach { (patternName, occurrences) ->
            occurrences.forEachIndexed { index, detected ->
                detected.index = index + 1
            }
        }

        // 步骤4: 按位置倒序执行替换（避免索引偏移）
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
     *
     * @param rawText 原始文本
     * @param privacyMapping 隐私映射规则
     * @param enabledPatterns 启用的自动检测模式
     * @return 脱敏后的文本
     */
    fun maskHybrid(
        rawText: String,
        privacyMapping: Map<String, String> = emptyMap(),
        enabledPatterns: List<String> = emptyList()
    ): String {
        // 先应用映射规则
        var maskedText = mask(rawText, privacyMapping)

        // 再应用自动检测
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
     * @param rawText 原始文本
     * @param enabledPatterns 启用的模式
     * @return 检测到的敏感信息列表
     */
    fun detectSensitiveInfo(
        rawText: String,
        enabledPatterns: List<String> = Patterns.ALL_PATTERNS.keys.toList()
    ): List<DetectedPattern> {
        // 步骤1: 收集所有匹配（可能重叠）
        val allMatches = mutableListOf<DetectedPattern>()

        // 按模式优先级排序
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

        // 步骤2: 移除被其他匹配完全覆盖的项（处理嵌套情况）
        val filteredMatches = allMatches.toMutableList()
        val toRemove = mutableSetOf<DetectedPattern>()

        for (i in filteredMatches.indices) {
            for (j in filteredMatches.indices) {
                if (i != j) {
                    val matchI = filteredMatches[i]
                    val matchJ = filteredMatches[j]
                    // 如果i完全包含j，且i不是手机号（因为手机号优先级高），则移除j
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
