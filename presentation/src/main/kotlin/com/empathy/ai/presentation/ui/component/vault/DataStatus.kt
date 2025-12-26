package com.empathy.ai.presentation.ui.component.vault

import androidx.compose.ui.graphics.Color
import com.empathy.ai.presentation.theme.iOSGreen
import com.empathy.ai.presentation.theme.iOSOrange
import com.empathy.ai.presentation.theme.iOSRed
import com.empathy.ai.presentation.theme.iOSTextTertiary

/**
 * 数据状态枚举
 * 
 * @param color 状态对应的颜色
 * @param displayName 显示名称
 * 
 * @see TDD-00020 6.2 DataSourceGridCard数据状态
 */
enum class DataStatus(
    val color: Color,
    val displayName: String
) {
    /** 已完成 - 绿色 */
    COMPLETED(iOSGreen, "已完成"),
    
    /** 处理中 - 橙色 */
    PROCESSING(iOSOrange, "处理中"),
    
    /** 不可用 - 灰色 */
    NOT_AVAILABLE(iOSTextTertiary, "不可用"),
    
    /** 失败 - 红色 */
    FAILED(iOSRed, "失败")
}
