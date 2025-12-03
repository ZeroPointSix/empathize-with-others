package com.empathy.ai.domain.service

/**
 * 隐私脱敏引擎
 *
 * 负责在发送数据给 AI 之前进行脱敏处理
 */
object PrivacyEngine {
    /**
     * 对文本进行脱敏处理
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
}
