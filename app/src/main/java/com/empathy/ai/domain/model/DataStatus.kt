package com.empathy.ai.domain.model

/**
 * 数据状态枚举
 *
 * 用于资料库的状态角标，表示数据源的处理状态
 */
enum class DataStatus {
    /**
     * 已完成
     * 数据已分析完成，已合入事实流和画像
     */
    COMPLETED,
    
    /**
     * 处理中
     * 数据正在排队或分析中
     */
    PROCESSING,
    
    /**
     * 失败
     * 数据分析失败
     */
    FAILED,
    
    /**
     * 不可用
     * 功能未实现或数据源不可用
     */
    NOT_AVAILABLE
}
