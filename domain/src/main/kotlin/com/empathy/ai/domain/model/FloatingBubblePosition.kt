package com.empathy.ai.domain.model

/**
 * 悬浮球位置数据类
 *
 * 保存悬浮球在屏幕上的位置信息
 *
 * @property x X坐标（像素）
 * @property y Y坐标（像素）
 *
 * @see TDD-00010 悬浮球状态指示与拖动技术设计
 */
data class FloatingBubblePosition(
    val x: Int,
    val y: Int
) {
    companion object {
        /**
         * 悬浮球默认尺寸（dp）
         */
        const val DEFAULT_SIZE_DP = 56

        /**
         * 默认边距（dp）
         */
        const val DEFAULT_MARGIN_DP = 16

        /**
         * 创建默认位置（屏幕右侧中间）
         *
         * @param screenWidth 屏幕宽度（像素）
         * @param screenHeight 屏幕高度（像素）
         * @param bubbleSizePx 悬浮球尺寸（像素）
         * @param marginPx 边距（像素）
         * @return 默认位置
         */
        fun default(
            screenWidth: Int,
            screenHeight: Int,
            bubbleSizePx: Int,
            marginPx: Int = 0
        ): FloatingBubblePosition {
            return FloatingBubblePosition(
                x = screenWidth - bubbleSizePx - marginPx,
                y = (screenHeight - bubbleSizePx) / 2
            )
        }

        /**
         * 创建无效位置（用于表示未保存的位置）
         */
        fun invalid(): FloatingBubblePosition = FloatingBubblePosition(-1, -1)

        /**
         * 检查位置是否有效
         */
        fun FloatingBubblePosition.isValid(): Boolean = x >= 0 && y >= 0
    }
}
