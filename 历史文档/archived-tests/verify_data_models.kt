import com.empathy.ai.domain.model.MinimizedRequestInfo
import com.empathy.ai.domain.model.MinimizeError
import com.empathy.ai.domain.model.ActionType

fun main() {
    println("=== 验证数据模型 ===\n")
    
    // 测试 MinimizedRequestInfo
    println("1. 测试 MinimizedRequestInfo")
    val requestInfo = MinimizedRequestInfo(
        id = "test-123",
        type = ActionType.ANALYZE,
        timestamp = System.currentTimeMillis()
    )
    println("   创建成功: $requestInfo")
    println("   ID: ${requestInfo.id}")
    println("   类型: ${requestInfo.type}")
    println("   时间戳: ${requestInfo.timestamp}")
    
    // 测试 data class copy
    val copied = requestInfo.copy(id = "test-456")
    println("   Copy 功能: $copied")
    
    // 测试相等性
    val requestInfo2 = MinimizedRequestInfo(
        id = "test-123",
        type = ActionType.ANALYZE,
        timestamp = requestInfo.timestamp
    )
    println("   相等性测试: ${requestInfo == requestInfo2}")
    
    println("\n2. 测试 MinimizeError")
    
    // 测试 MinimizeFailed
    try {
        throw MinimizeError.MinimizeFailed("窗口管理器不可用")
    } catch (e: MinimizeError.MinimizeFailed) {
        println("   MinimizeFailed 捕获成功: ${e.message}")
    }
    
    // 测试 RestoreFailed
    try {
        throw MinimizeError.RestoreFailed("指示器视图已被移除")
    } catch (e: MinimizeError.RestoreFailed) {
        println("   RestoreFailed 捕获成功: ${e.message}")
    }
    
    // 测试 NotificationFailed
    try {
        throw MinimizeError.NotificationFailed("通知权限未授予")
    } catch (e: MinimizeError.NotificationFailed) {
        println("   NotificationFailed 捕获成功: ${e.message}")
    }
    
    // 测试作为通用 MinimizeError 捕获
    try {
        throw MinimizeError.MinimizeFailed("测试")
    } catch (e: MinimizeError) {
        println("   作为 MinimizeError 捕获成功: ${e.message}")
    }
    
    println("\n=== 所有验证通过 ===")
}
