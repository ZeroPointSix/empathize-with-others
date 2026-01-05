package com.empathy.ai.domain.model

/**
 * 消息块类型
 *
 * 参考Cherry Studio的MessageBlockType设计，定义消息的组成单元类型。
 * 一条消息可以包含多个Block，支持复杂内容组合（思考+文本+工具调用）。
 *
 * 业务背景 (FD-00028):
 * - Block架构是支持思考过程、工具调用等高级功能的基础
 * - 每个Block独立状态管理，便于流式更新
 * - 支持未来扩展（代码块、引用、图片等）
 *
 * 设计决策 (TDD-00028):
 * - 初期只实现核心类型：MAIN_TEXT、THINKING、ERROR
 * - 预留扩展空间：CODE、TOOL、CITATION等
 * - 枚举名称与数据库存储一致，使用name属性
 *
 * @see FD-00028 AI军师流式对话升级功能设计
 * @see RESEARCH-00004 Cherry项目AI对话实现深度分析报告
 */
enum class MessageBlockType {
    /**
     * 主文本块
     *
     * AI回复的主要内容，是最常见的Block类型。
     * 每条AI消息至少包含一个MAIN_TEXT块。
     */
    MAIN_TEXT,

    /**
     * 思考过程块
     *
     * AI的推理过程，仅部分模型支持（如DeepSeek R1）。
     * UI层应以可折叠的形式展示，默认折叠。
     */
    THINKING,

    /**
     * 错误信息块
     *
     * 流式响应过程中发生的错误。
     * UI层应以醒目的样式展示，支持重试操作。
     */
    ERROR

    // 未来扩展：
    // CODE,      // 代码块
    // TOOL,      // 工具调用
    // CITATION,  // 引用/搜索结果
    // IMAGE      // 图片
}

/**
 * 消息块状态
 *
 * 参考Cherry Studio的MessageBlockStatus设计，定义Block的生命周期状态。
 * 支持流式更新过程中的状态跟踪和UI渲染。
 *
 * 业务背景 (FD-00028):
 * - 流式响应需要细粒度的状态管理
 * - 不同状态对应不同的UI展示（加载动画、光标闪烁等）
 * - 支持暂停/恢复等高级功能
 *
 * 设计决策 (TDD-00028):
 * - 初期实现核心状态：PENDING、STREAMING、SUCCESS、ERROR
 * - 预留PAUSED状态支持未来的暂停功能
 * - 枚举名称与数据库存储一致，使用name属性
 */
enum class MessageBlockStatus {
    /**
     * 等待处理
     *
     * Block已创建但尚未开始接收内容。
     * UI层应显示占位符或加载动画。
     */
    PENDING,

    /**
     * 流式接收中
     *
     * Block正在接收流式内容。
     * UI层应显示光标闪烁效果，实时更新内容。
     */
    STREAMING,

    /**
     * 成功完成
     *
     * Block内容已完整接收。
     * UI层应移除加载动画，显示最终内容。
     */
    SUCCESS,

    /**
     * 发生错误
     *
     * Block接收过程中发生错误。
     * UI层应显示错误提示，支持重试操作。
     */
    ERROR

    // 未来扩展：
    // PAUSED  // 暂停（用户主动暂停流式接收）
}
