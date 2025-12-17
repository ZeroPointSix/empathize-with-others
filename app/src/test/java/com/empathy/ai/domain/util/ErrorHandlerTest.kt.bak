package com.empathy.ai.domain.util

import android.content.Context
import android.widget.Toast
import com.empathy.ai.domain.model.FloatingWindowError
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * ErrorHandler 单元测试
 * 
 * 测试错误处理工具类的各种错误处理场景
 */
@RunWith(RobolectricTestRunner::class)
class ErrorHandlerTest {
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        
        // Mock Toast
        mockkStatic(Toast::class)
        every { Toast.makeText(any(), any<String>(), any()) } returns mockk(relaxed = true)
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `handleError - PermissionDenied 显示长时间 Toast`() {
        // Given
        val error = FloatingWindowError.PermissionDenied
        
        // When
        ErrorHandler.handleError(context, error)
        
        // Then
        verify {
            Toast.makeText(
                context,
                "需要悬浮窗权限才能使用此功能",
                Toast.LENGTH_LONG
            )
        }
    }
    
    @Test
    fun `handleError - ServiceError 显示短时间 Toast`() {
        // Given
        val error = FloatingWindowError.ServiceError("测试错误")
        
        // When
        ErrorHandler.handleError(context, error)
        
        // Then
        verify {
            Toast.makeText(
                context,
                "服务启动失败：测试错误",
                Toast.LENGTH_SHORT
            )
        }
    }
    
    @Test
    fun `handleError - ValidationError contact 显示正确消息`() {
        // Given
        val error = FloatingWindowError.ValidationError("contact")
        
        // When
        ErrorHandler.handleError(context, error)
        
        // Then
        verify {
            Toast.makeText(
                context,
                "请选择联系人",
                Toast.LENGTH_SHORT
            )
        }
    }
    
    @Test
    fun `handleError - ValidationError text 显示正确消息`() {
        // Given
        val error = FloatingWindowError.ValidationError("text")
        
        // When
        ErrorHandler.handleError(context, error)
        
        // Then
        verify {
            Toast.makeText(
                context,
                "请输入内容",
                Toast.LENGTH_SHORT
            )
        }
    }
    
    @Test
    fun `handleError - ValidationError textLength 显示正确消息`() {
        // Given
        val error = FloatingWindowError.ValidationError("textLength")
        
        // When
        ErrorHandler.handleError(context, error)
        
        // Then
        verify {
            Toast.makeText(
                context,
                "输入内容不能超过 5000 字符",
                Toast.LENGTH_SHORT
            )
        }
    }
    
    @Test
    fun `handleError - UseCaseError 显示错误消息`() {
        // Given
        val cause = Exception("网络错误")
        val error = FloatingWindowError.UseCaseError(cause)
        
        // When
        ErrorHandler.handleError(context, error)
        
        // Then
        verify {
            Toast.makeText(
                context,
                "操作失败：网络错误",
                Toast.LENGTH_SHORT
            )
        }
    }
    
    @Test
    fun `showSuccess 显示成功消息`() {
        // Given
        val message = "操作成功"
        
        // When
        ErrorHandler.showSuccess(context, message)
        
        // Then
        verify {
            Toast.makeText(
                context,
                message,
                Toast.LENGTH_SHORT
            )
        }
    }
    
    @Test
    fun `showError 显示错误消息`() {
        // Given
        val message = "操作失败"
        
        // When
        ErrorHandler.showError(context, message)
        
        // Then
        verify {
            Toast.makeText(
                context,
                message,
                Toast.LENGTH_SHORT
            )
        }
    }
}
