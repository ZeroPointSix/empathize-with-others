package com.empathy.ai.domain.model

/**
 * 总结任务状态枚举
 *
 * 定义手动触发AI总结任务的各个状态
 *
 * @property displayName 状态显示名称
 * @property progressRange 该状态对应的进度范围
 */
enum class SummaryTaskStatus(
    val displayName: String,
    val progressRange: ClosedFloatingPointRange<Float>
) {
    /** 空闲 - 未开始 */
    IDLE("空闲", 0f..0f),

    /** 获取数据 - 正在获取对话记录 */
    FETCHING_DATA("获取数据", 0f..0.2f),

    /** 分析中 - AI正在处理 */
    ANALYZING("AI分析中", 0.2f..0.7f),

    /** 生成中 - 正在生成总结内容 */
    GENERATING("生成总结", 0.7f..0.9f),

    /** 保存中 - 正在保存结果 */
    SAVING("保存结果", 0.9f..1f),

    /** 成功 - 总结完成 */
    SUCCESS("完成", 1f..1f),

    /** 失败 - 处理出错 */
    FAILED("失败", 0f..0f),

    /** 已取消 - 用户取消 */
    CANCELLED("已取消", 0f..0f);

    /**
     * 是否为终态
     *
     * 终态表示任务已结束，不会再有状态变化
     */
    fun isTerminal(): Boolean = this in listOf(SUCCESS, FAILED, CANCELLED)

    /**
     * 是否可取消
     *
     * 只有在执行中的状态才可以取消
     */
    fun isCancellable(): Boolean = this in listOf(FETCHING_DATA, ANALYZING, GENERATING)

    /**
     * 是否正在执行中
     */
    fun isInProgress(): Boolean = this in listOf(FETCHING_DATA, ANALYZING, GENERATING, SAVING)
}
