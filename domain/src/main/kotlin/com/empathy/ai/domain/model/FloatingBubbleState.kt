package com.empathy.ai.domain.model

/**
 * 悬浮球状态枚举
 *
 * 定义悬浮球的四种状态，用于状态指示器显示
 *
 * @see TDD-00010 悬浮球状态指示与拖动技术设计
 * @see PRD-00010 悬浮球状态指示与拖动功能需求
 */
enum class FloatingBubbleState {
    /**
     * 空闲状态 - 显示静态App图标
     * 
     * 触发条件：
     * - 初始状态
     * - 用户点击悬浮球查看结果后
     * - 无正在处理的AI请求
     */
    IDLE,

    /**
     * 加载中 - 显示旋转动画
     * 
     * 触发条件：
     * - 真正发起AI网络请求后（不是点击发送按钮时）
     * 
     * 注意：只有真正调用AI API时才能进入此状态
     */
    LOADING,

    /**
     * 成功 - 显示绿色勾
     * 
     * 触发条件：
     * - AI API返回成功结果
     */
    SUCCESS,

    /**
     * 失败 - 显示红色叹号
     * 
     * 触发条件：
     * - AI API返回错误
     * - 网络超时
     * - 其他异常
     */
    ERROR;

    companion object {
        /**
         * 获取默认状态
         */
        fun default(): FloatingBubbleState = IDLE
    }
}
