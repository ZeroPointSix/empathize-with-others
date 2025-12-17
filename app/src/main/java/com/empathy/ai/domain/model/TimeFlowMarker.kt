package com.empathy.ai.domain.model

/**
 * 时间流逝标记类型
 *
 * 用于在对话历史中插入时间间隔提示，模拟人类翻看聊天记录的心理活动
 */
sealed class TimeFlowMarker {

    companion object {
        /**
         * 时间单位常量
         *
         * 【本地化说明】当前硬编码中文，后续版本建议提取到strings.xml
         * 提取为常量便于后续国际化改造
         */
        const val UNIT_MINUTE = "分钟"
        const val UNIT_HOUR = "小时"

        /**
         * 格式化模板常量
         */
        const val TEMPLATE_DATE_CHANGE = "--- [%s] ---"
        const val TEMPLATE_SHORT_GAP = "--- (对话暂停了 %s) ---"
        const val TEMPLATE_LONG_GAP = "--- (对话暂停了 %d %s，注意对方可能的情绪变化) ---"
    }

    /**
     * 日期变更标记
     * 示例: --- [2025-12-16] ---
     */
    data class DateChange(val date: String) : TimeFlowMarker() {
        override fun toDisplayString(): String = TEMPLATE_DATE_CHANGE.format(date)
    }

    /**
     * 短间隔标记（10分钟 ~ 3小时）
     * 示例: --- (对话暂停了 45 分钟) ---
     */
    data class ShortGap(val minutes: Int) : TimeFlowMarker() {
        override fun toDisplayString(): String {
            val duration = if (minutes >= 60) {
                "${minutes / 60} $UNIT_HOUR"
            } else {
                "$minutes $UNIT_MINUTE"
            }
            return TEMPLATE_SHORT_GAP.format(duration)
        }
    }

    /**
     * 长间隔标记（> 3小时）
     * 示例: --- (对话暂停了 6 小时，注意对方可能的情绪变化) ---
     */
    data class LongGap(val hours: Int) : TimeFlowMarker() {
        override fun toDisplayString(): String {
            return TEMPLATE_LONG_GAP.format(hours, UNIT_HOUR)
        }
    }

    /**
     * 无标记（热对话，< 10分钟）
     */
    data object None : TimeFlowMarker() {
        override fun toDisplayString(): String = ""
    }

    /**
     * 转换为显示字符串
     */
    abstract fun toDisplayString(): String
}
