package com.empathy.ai.presentation.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * AppSpacing单元测试
 * 
 * 验证间距值与Dimensions的对应关系和正确性
 */
class AppSpacingTest {
    
    // ========== 间距值与Dimensions对应关系测试 ==========
    
    @Test
    fun `AppSpacing xs should equal Dimensions SpacingXSmall`() {
        assertEquals(Dimensions.SpacingXSmall, AppSpacing.xs)
    }
    
    @Test
    fun `AppSpacing sm should equal Dimensions SpacingSmall`() {
        assertEquals(Dimensions.SpacingSmall, AppSpacing.sm)
    }
    
    @Test
    fun `AppSpacing md should equal Dimensions SpacingMediumSmall`() {
        assertEquals(Dimensions.SpacingMediumSmall, AppSpacing.md)
    }
    
    @Test
    fun `AppSpacing lg should equal Dimensions SpacingMedium`() {
        assertEquals(Dimensions.SpacingMedium, AppSpacing.lg)
    }
    
    @Test
    fun `AppSpacing xl should equal Dimensions SpacingLarge`() {
        assertEquals(Dimensions.SpacingLarge, AppSpacing.xl)
    }
    
    @Test
    fun `AppSpacing xxl should equal Dimensions SpacingXLarge`() {
        assertEquals(Dimensions.SpacingXLarge, AppSpacing.xxl)
    }
    
    // ========== 间距值正确性测试 ==========
    
    @Test
    fun `AppSpacing xs should be 4dp`() {
        assertEquals(4.dp, AppSpacing.xs)
    }
    
    @Test
    fun `AppSpacing sm should be 8dp`() {
        assertEquals(8.dp, AppSpacing.sm)
    }
    
    @Test
    fun `AppSpacing md should be 12dp`() {
        assertEquals(12.dp, AppSpacing.md)
    }
    
    @Test
    fun `AppSpacing lg should be 16dp`() {
        assertEquals(16.dp, AppSpacing.lg)
    }
    
    @Test
    fun `AppSpacing xl should be 24dp`() {
        assertEquals(24.dp, AppSpacing.xl)
    }
    
    @Test
    fun `AppSpacing xxl should be 32dp`() {
        assertEquals(32.dp, AppSpacing.xxl)
    }
    
    // ========== 间距递增顺序测试 ==========
    
    @Test
    fun `AppSpacing values should be in ascending order`() {
        assert(AppSpacing.xs < AppSpacing.sm) { "xs should be less than sm" }
        assert(AppSpacing.sm < AppSpacing.md) { "sm should be less than md" }
        assert(AppSpacing.md < AppSpacing.lg) { "md should be less than lg" }
        assert(AppSpacing.lg < AppSpacing.xl) { "lg should be less than xl" }
        assert(AppSpacing.xl < AppSpacing.xxl) { "xl should be less than xxl" }
    }
    
    // ========== Dimensions新增间距测试 ==========
    
    @Test
    fun `Dimensions SpacingMediumSmall should be 12dp`() {
        assertEquals(12.dp, Dimensions.SpacingMediumSmall)
    }
    
    @Test
    fun `Dimensions SpacingMediumSmall should be between SpacingSmall and SpacingMedium`() {
        assert(Dimensions.SpacingSmall < Dimensions.SpacingMediumSmall) {
            "SpacingMediumSmall should be greater than SpacingSmall"
        }
        assert(Dimensions.SpacingMediumSmall < Dimensions.SpacingMedium) {
            "SpacingMediumSmall should be less than SpacingMedium"
        }
    }
}
