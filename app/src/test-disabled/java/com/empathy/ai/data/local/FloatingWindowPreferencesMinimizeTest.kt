package com.empathy.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.model.MinimizedRequestInfo
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * FloatingWindowPreferences 最小化功能单元测试
 * 
 * 测试最小化请求信息和指示器位置的持久化功能
 */
class FloatingWindowPreferencesMinimizeTest {
    
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var moshi: Moshi
    private lateinit var floatingWindowPreferences: FloatingWindowPreferences
    
    @Before
    fun setup() {
        // Mock Context
        context = mockk(relaxed = true)
        
        // Mock SharedPreferences
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.apply() } returns Unit
        
        // 创建真实的 Moshi 实例用于序列化测试
        moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        
        floatingWindowPreferences = FloatingWindowPreferences(context, moshi)
    }
    
    @Test
    fun `testSaveAndGetRequestInfo - 保存和读取请求信息应一致`() {
        // Given
        val requestInfo = MinimizedRequestInfo(
            id = "test-123",
            type = ActionType.ANALYZE,
            timestamp = 1234567890L
        )
        
        // 序列化请求信息
        val adapter = moshi.adapter(MinimizedRequestInfo::class.java)
        val json = adapter.toJson(requestInfo)
        
        // Mock SharedPreferences 返回序列化的 JSON
        every { sharedPreferences.getString("minimized_request", null) } returns json
        
        // When
        floatingWindowPreferences.saveRequestInfo(requestInfo)
        val retrieved = floatingWindowPreferences.getRequestInfo()
        
        // Then
        verify { editor.putString("minimized_request", json) }
        verify { editor.apply() }
        
        assertNotNull(retrieved)
        assertEquals(requestInfo.id, retrieved?.id)
        assertEquals(requestInfo.type, retrieved?.type)
        assertEquals(requestInfo.timestamp, retrieved?.timestamp)
    }
    
    @Test
    fun `testClearRequestInfo - 清除后应返回 null`() {
        // Given
        val requestInfo = MinimizedRequestInfo(
            id = "test-123",
            type = ActionType.CHECK,
            timestamp = 1234567890L
        )
        
        // 先保存
        val adapter = moshi.adapter(MinimizedRequestInfo::class.java)
        val json = adapter.toJson(requestInfo)
        every { sharedPreferences.getString("minimized_request", null) } returns json
        floatingWindowPreferences.saveRequestInfo(requestInfo)
        
        // 清除后 mock 返回 null
        every { sharedPreferences.getString("minimized_request", null) } returns null
        
        // When
        floatingWindowPreferences.clearRequestInfo()
        val retrieved = floatingWindowPreferences.getRequestInfo()
        
        // Then
        verify { editor.remove("minimized_request") }
        verify { editor.apply() }
        assertNull(retrieved)
    }
    
    @Test
    fun `testSaveAndGetIndicatorPosition - 保存和读取位置应一致`() {
        // Given
        val x = 100
        val y = 200
        
        // Mock SharedPreferences 返回保存的位置
        every { sharedPreferences.getInt("indicator_x", -1) } returns x
        every { sharedPreferences.getInt("indicator_y", -1) } returns y
        
        // When
        floatingWindowPreferences.saveIndicatorPosition(x, y)
        val (retrievedX, retrievedY) = floatingWindowPreferences.getIndicatorPosition()
        
        // Then
        verify { editor.putInt("indicator_x", x) }
        verify { editor.putInt("indicator_y", y) }
        verify { editor.apply() }
        
        assertEquals(x, retrievedX)
        assertEquals(y, retrievedY)
    }
    
    @Test
    fun `testGetIndicatorPosition - 未保存时应返回悬浮按钮位置`() {
        // Given
        val buttonX = 50
        val buttonY = 100
        
        // Mock 指示器位置未保存（返回 -1）
        every { sharedPreferences.getInt("indicator_x", -1) } returns -1
        every { sharedPreferences.getInt("indicator_y", -1) } returns -1
        
        // Mock 悬浮按钮位置
        every { sharedPreferences.getInt("button_x", 0) } returns buttonX
        every { sharedPreferences.getInt("button_y", 0) } returns buttonY
        
        // When
        val (x, y) = floatingWindowPreferences.getIndicatorPosition()
        
        // Then
        assertEquals(buttonX, x)
        assertEquals(buttonY, y)
    }
    
    @Test
    fun `testGetRequestInfoWithInvalidJson - 处理损坏的 JSON 应返回 null`() {
        // Given
        val invalidJson = "{invalid json"
        
        // Mock SharedPreferences 返回损坏的 JSON
        every { sharedPreferences.getString("minimized_request", null) } returns invalidJson
        
        // When
        val retrieved = floatingWindowPreferences.getRequestInfo()
        
        // Then
        assertNull(retrieved)
        // 应该清除损坏的数据
        verify { editor.remove("minimized_request") }
        verify { editor.apply() }
    }
    
    @Test
    fun `testGetRequestInfo - 不存在时应返回 null`() {
        // Given
        every { sharedPreferences.getString("minimized_request", null) } returns null
        
        // When
        val retrieved = floatingWindowPreferences.getRequestInfo()
        
        // Then
        assertNull(retrieved)
    }
    
    @Test
    fun `testSaveRequestInfo - 不同操作类型应正确序列化`() {
        // Given - ANALYZE 类型
        val analyzeRequest = MinimizedRequestInfo(
            id = "analyze-123",
            type = ActionType.ANALYZE,
            timestamp = 1234567890L
        )
        
        val adapter = moshi.adapter(MinimizedRequestInfo::class.java)
        val analyzeJson = adapter.toJson(analyzeRequest)
        every { sharedPreferences.getString("minimized_request", null) } returns analyzeJson
        
        // When
        floatingWindowPreferences.saveRequestInfo(analyzeRequest)
        val retrievedAnalyze = floatingWindowPreferences.getRequestInfo()
        
        // Then
        assertNotNull(retrievedAnalyze)
        assertEquals(ActionType.ANALYZE, retrievedAnalyze?.type)
        
        // Given - CHECK 类型
        val checkRequest = MinimizedRequestInfo(
            id = "check-456",
            type = ActionType.CHECK,
            timestamp = 9876543210L
        )
        
        val checkJson = adapter.toJson(checkRequest)
        every { sharedPreferences.getString("minimized_request", null) } returns checkJson
        
        // When
        floatingWindowPreferences.saveRequestInfo(checkRequest)
        val retrievedCheck = floatingWindowPreferences.getRequestInfo()
        
        // Then
        assertNotNull(retrievedCheck)
        assertEquals(ActionType.CHECK, retrievedCheck?.type)
    }
    
    @Test
    fun `testGetButtonPosition - 应返回悬浮按钮位置`() {
        // Given
        val buttonX = 75
        val buttonY = 150
        
        every { sharedPreferences.getInt("button_x", 0) } returns buttonX
        every { sharedPreferences.getInt("button_y", 0) } returns buttonY
        
        // When
        val (x, y) = floatingWindowPreferences.getButtonPosition()
        
        // Then
        assertEquals(buttonX, x)
        assertEquals(buttonY, y)
    }
}
