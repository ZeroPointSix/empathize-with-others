package com.empathy.ai.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.model.FloatingWindowState
import com.empathy.ai.domain.model.MinimizedRequestInfo
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 悬浮窗状态持久化集成测试
 *
 * 测试范围：
 * - 状态保存和加载
 * - 位置信息持久化
 * - 最小化请求信息序列化
 * - 边界条件处理
 */
@RunWith(AndroidJUnit4::class)
class FloatingWindowPreferencesTest {

    private lateinit var context: Context
    private lateinit var moshi: Moshi
    private lateinit var preferences: FloatingWindowPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        preferences = FloatingWindowPreferences(context, moshi)
        // 清除之前的测试数据
        preferences.clear()
    }

    @After
    fun tearDown() {
        // 清理测试数据
        preferences.clear()
    }

    // ==================== 状态保存和加载测试 ====================

    @Test
    fun saveState_andLoadState_returnsCorrectState() {
        // Given
        val state = FloatingWindowState(
            isEnabled = true,
            buttonX = 100,
            buttonY = 200
        )

        // When
        preferences.saveState(state)
        val loadedState = preferences.loadState()

        // Then
        assertEquals(state.isEnabled, loadedState.isEnabled)
        assertEquals(state.buttonX, loadedState.buttonX)
        assertEquals(state.buttonY, loadedState.buttonY)
    }

    @Test
    fun loadState_withoutSaving_returnsDefaultState() {
        // When
        val state = preferences.loadState()

        // Then
        assertFalse("默认状态应为禁用", state.isEnabled)
        assertEquals("默认X坐标应为0", 0, state.buttonX)
        assertEquals("默认Y坐标应为0", 0, state.buttonY)
    }

    // ==================== 启用状态测试 ====================

    @Test
    fun saveEnabled_true_andIsEnabled_returnsTrue() {
        // When
        preferences.saveEnabled(true)

        // Then
        assertTrue(preferences.isEnabled())
    }

    @Test
    fun saveEnabled_false_andIsEnabled_returnsFalse() {
        // Given
        preferences.saveEnabled(true)

        // When
        preferences.saveEnabled(false)

        // Then
        assertFalse(preferences.isEnabled())
    }

    @Test
    fun isEnabled_withoutSaving_returnsFalse() {
        // Then
        assertFalse("默认应为禁用", preferences.isEnabled())
    }

    // ==================== 按钮位置测试 ====================

    @Test
    fun saveButtonPosition_andGetPosition_returnsCorrectPosition() {
        // Given
        val x = 150
        val y = 250

        // When
        preferences.saveButtonPosition(x, y)

        // Then
        assertEquals(x, preferences.getButtonX())
        assertEquals(y, preferences.getButtonY())
    }

    @Test
    fun getButtonPosition_withoutSaving_returnsZero() {
        // Then
        assertEquals(0, preferences.getButtonX())
        assertEquals(0, preferences.getButtonY())
    }

    @Test
    fun saveButtonPosition_negativeValues_savesCorrectly() {
        // Given
        val x = -50
        val y = -100

        // When
        preferences.saveButtonPosition(x, y)

        // Then
        assertEquals(x, preferences.getButtonX())
        assertEquals(y, preferences.getButtonY())
    }

    @Test
    fun saveButtonPosition_largeValues_savesCorrectly() {
        // Given
        val x = Int.MAX_VALUE
        val y = Int.MAX_VALUE

        // When
        preferences.saveButtonPosition(x, y)

        // Then
        assertEquals(x, preferences.getButtonX())
        assertEquals(y, preferences.getButtonY())
    }

    @Test
    fun getButtonPosition_returnsPair() {
        // Given
        val x = 100
        val y = 200
        preferences.saveButtonPosition(x, y)

        // When
        val position = preferences.getButtonPosition()

        // Then
        assertEquals(x, position.first)
        assertEquals(y, position.second)
    }

    // ==================== 最小化请求信息测试 ====================

    @Test
    fun saveRequestInfo_andGetRequestInfo_returnsCorrectInfo() {
        // Given
        val requestInfo = MinimizedRequestInfo(
            id = "test-id-123",
            type = ActionType.ANALYZE,
            timestamp = System.currentTimeMillis()
        )

        // When
        preferences.saveRequestInfo(requestInfo)
        val loadedInfo = preferences.getRequestInfo()

        // Then
        assertNotNull(loadedInfo)
        assertEquals(requestInfo.id, loadedInfo!!.id)
        assertEquals(requestInfo.type, loadedInfo.type)
        assertEquals(requestInfo.timestamp, loadedInfo.timestamp)
    }

    @Test
    fun getRequestInfo_withoutSaving_returnsNull() {
        // Then
        assertNull(preferences.getRequestInfo())
    }

    @Test
    fun clearRequestInfo_removesRequestInfo() {
        // Given
        val requestInfo = MinimizedRequestInfo(
            id = "test-id",
            type = ActionType.CHECK,
            timestamp = System.currentTimeMillis()
        )
        preferences.saveRequestInfo(requestInfo)

        // When
        preferences.clearRequestInfo()

        // Then
        assertNull(preferences.getRequestInfo())
    }

    @Test
    fun saveRequestInfo_withCheckType_savesCorrectly() {
        // Given
        val requestInfo = MinimizedRequestInfo(
            id = "check-id",
            type = ActionType.CHECK,
            timestamp = 1234567890L
        )

        // When
        preferences.saveRequestInfo(requestInfo)
        val loadedInfo = preferences.getRequestInfo()

        // Then
        assertNotNull(loadedInfo)
        assertEquals(ActionType.CHECK, loadedInfo!!.type)
    }

    // ==================== 指示器位置测试 ====================

    @Test
    fun saveIndicatorPosition_andGetIndicatorPosition_returnsCorrectPosition() {
        // Given
        val x = 300
        val y = 400

        // When
        preferences.saveIndicatorPosition(x, y)
        val position = preferences.getIndicatorPosition()

        // Then
        assertEquals(x, position.first)
        assertEquals(y, position.second)
    }

    @Test
    fun getIndicatorPosition_withoutSaving_returnsButtonPosition() {
        // Given
        val buttonX = 100
        val buttonY = 200
        preferences.saveButtonPosition(buttonX, buttonY)

        // When
        val indicatorPosition = preferences.getIndicatorPosition()

        // Then
        assertEquals("未保存指示器位置时应返回按钮位置", buttonX, indicatorPosition.first)
        assertEquals("未保存指示器位置时应返回按钮位置", buttonY, indicatorPosition.second)
    }

    // ==================== 清除数据测试 ====================

    @Test
    fun clear_removesAllData() {
        // Given
        preferences.saveEnabled(true)
        preferences.saveButtonPosition(100, 200)
        preferences.saveIndicatorPosition(300, 400)
        preferences.saveRequestInfo(
            MinimizedRequestInfo(
                id = "test",
                type = ActionType.ANALYZE,
                timestamp = System.currentTimeMillis()
            )
        )

        // When
        preferences.clear()

        // Then
        assertFalse(preferences.isEnabled())
        assertEquals(0, preferences.getButtonX())
        assertEquals(0, preferences.getButtonY())
        assertNull(preferences.getRequestInfo())
    }

    // ==================== 并发测试 ====================

    @Test
    fun multipleWrites_lastWriteWins() {
        // When
        preferences.saveButtonPosition(100, 100)
        preferences.saveButtonPosition(200, 200)
        preferences.saveButtonPosition(300, 300)

        // Then
        assertEquals(300, preferences.getButtonX())
        assertEquals(300, preferences.getButtonY())
    }

    @Test
    fun saveState_overwritesPreviousState() {
        // Given
        val state1 = FloatingWindowState(isEnabled = true, buttonX = 100, buttonY = 100)
        val state2 = FloatingWindowState(isEnabled = false, buttonX = 200, buttonY = 200)

        // When
        preferences.saveState(state1)
        preferences.saveState(state2)
        val loadedState = preferences.loadState()

        // Then
        assertEquals(state2.isEnabled, loadedState.isEnabled)
        assertEquals(state2.buttonX, loadedState.buttonX)
        assertEquals(state2.buttonY, loadedState.buttonY)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun saveRequestInfo_withEmptyId_savesCorrectly() {
        // Given
        val requestInfo = MinimizedRequestInfo(
            id = "",
            type = ActionType.ANALYZE,
            timestamp = 0L
        )

        // When
        preferences.saveRequestInfo(requestInfo)
        val loadedInfo = preferences.getRequestInfo()

        // Then
        assertNotNull(loadedInfo)
        assertEquals("", loadedInfo!!.id)
    }

    @Test
    fun saveButtonPosition_zeroValues_savesCorrectly() {
        // Given
        preferences.saveButtonPosition(100, 100) // 先设置非零值

        // When
        preferences.saveButtonPosition(0, 0)

        // Then
        assertEquals(0, preferences.getButtonX())
        assertEquals(0, preferences.getButtonY())
    }
}
