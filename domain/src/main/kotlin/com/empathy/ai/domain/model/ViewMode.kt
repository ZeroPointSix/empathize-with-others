package com.empathy.ai.domain.model

/**
 * 视图模式枚举
 *
 * 定义事实流的两种展示模式
 *
 * @property displayName 显示名称
 */
enum class ViewMode(val displayName: String) {
    /**
     * 时光轴模式
     * 瀑布流布局，强调视觉多样性和沉浸式体验
     */
    Timeline("时光轴"),
    
    /**
     * 清单列表模式
     * 单列列表布局，强调信息密度和快速检索
     */
    List("清单");
    
    /**
     * 切换到另一个模式
     */
    fun toggle(): ViewMode = when (this) {
        Timeline -> List
        List -> Timeline
    }
}
