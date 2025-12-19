package com.empathy.ai.presentation.ui.component

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * MaxHeightScrollView单元测试
 *
 * 测试自定义ScrollView的maxHeight约束功能
 *
 * @see BUG-00018 分析模式复制/重新生成按钮不可见问题
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MaxHeightScrollViewTest {

    private lateinit var context: Context
    private lateinit var scrollView: MaxHeightScrollView

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        scrollView = MaxHeightScrollView(context)
    }

    // ==================== 基础功能测试 ====================

    @Test
    fun `默认maxHeight应该是Int_MAX_VALUE`() {
        // Given - 新创建的ScrollView
        
        // Then
        assertEquals(Int.MAX_VALUE, scrollView.getMaxHeight())
    }

    @Test
    fun `setMaxHeight应该正确设置最大高度`() {
        // Given
        val maxHeight = 500

        // When
        scrollView.setMaxHeight(maxHeight)

        // Then
        assertEquals(maxHeight, scrollView.getMaxHeight())
    }

    @Test
    fun `setMaxHeightDp应该正确转换dp到px`() {
        // Given
        val maxHeightDp = 100f
        val density = context.resources.displayMetrics.density
        val expectedPx = (maxHeightDp * density).toInt()

        // When
        scrollView.setMaxHeightDp(maxHeightDp)

        // Then
        assertEquals(expectedPx, scrollView.getMaxHeight())
    }

    // ==================== 测量约束测试 ====================

    @Test
    fun `内容高度小于maxHeight时应该使用实际高度`() {
        // Given
        val maxHeight = 500
        scrollView.setMaxHeight(maxHeight)
        
        // 添加一个小内容
        val content = TextView(context).apply {
            text = "短内容"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                100 // 100px高度
            )
        }
        scrollView.addView(content)

        // When - 测量
        scrollView.measure(
            View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        // Then - 高度应该小于maxHeight
        assertTrue("测量高度应该小于maxHeight", scrollView.measuredHeight <= maxHeight)
    }

    @Test
    fun `内容高度大于maxHeight时应该限制为maxHeight`() {
        // Given
        val maxHeight = 200
        scrollView.setMaxHeight(maxHeight)
        
        // 添加一个大内容
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        // 添加多个子View使内容超过maxHeight
        repeat(20) {
            content.addView(TextView(context).apply {
                text = "行 $it - 这是一段很长的文本内容用于测试"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    50 // 每行50px
                )
            })
        }
        scrollView.addView(content)

        // When - 测量
        scrollView.measure(
            View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        // Then - 高度应该等于maxHeight
        assertEquals("测量高度应该等于maxHeight", maxHeight, scrollView.measuredHeight)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `maxHeight为0时应该高度为0`() {
        // Given
        scrollView.setMaxHeight(0)
        
        val content = TextView(context).apply {
            text = "内容"
        }
        scrollView.addView(content)

        // When
        scrollView.measure(
            View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        // Then
        assertEquals(0, scrollView.measuredHeight)
    }

    @Test
    fun `多次设置maxHeight应该使用最新值`() {
        // Given & When
        scrollView.setMaxHeight(100)
        assertEquals(100, scrollView.getMaxHeight())
        
        scrollView.setMaxHeight(200)
        assertEquals(200, scrollView.getMaxHeight())
        
        scrollView.setMaxHeight(50)
        assertEquals(50, scrollView.getMaxHeight())
    }

    @Test
    fun `空内容时高度应该为0`() {
        // Given
        scrollView.setMaxHeight(500)
        // 不添加任何内容

        // When
        scrollView.measure(
            View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        // Then
        assertEquals(0, scrollView.measuredHeight)
    }

    // ==================== MeasureSpec模式测试 ====================

    @Test
    fun `EXACTLY模式下应该取maxHeight和指定值的较小值`() {
        // Given
        val maxHeight = 200
        val specifiedHeight = 300
        scrollView.setMaxHeight(maxHeight)
        
        val content = TextView(context).apply {
            text = "内容"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                500 // 大于两者
            )
        }
        scrollView.addView(content)

        // When - 使用EXACTLY模式
        scrollView.measure(
            View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(specifiedHeight, View.MeasureSpec.EXACTLY)
        )

        // Then - 应该使用maxHeight（较小值）
        assertEquals(maxHeight, scrollView.measuredHeight)
    }

    @Test
    fun `AT_MOST模式下应该取maxHeight和约束值的较小值`() {
        // Given
        val maxHeight = 200
        val constraintHeight = 150
        scrollView.setMaxHeight(maxHeight)
        
        val content = TextView(context).apply {
            text = "内容"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                500 // 大于两者
            )
        }
        scrollView.addView(content)

        // When - 使用AT_MOST模式
        scrollView.measure(
            View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(constraintHeight, View.MeasureSpec.AT_MOST)
        )

        // Then - 应该使用constraintHeight（较小值）
        assertEquals(constraintHeight, scrollView.measuredHeight)
    }
}
