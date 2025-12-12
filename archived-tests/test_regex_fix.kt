// 快速测试脚本 - 验证正则表达式修复

fun main() {
    // 测试原始的错误方式
    try {
        val test1 = ",}".replace(",\\s*}".toRegex(), "}")
        println("❌ .toRegex() 方式在某些环境下会失败")
    } catch (e: Exception) {
        println("✅ 确认 .toRegex() 方式有问题: ${e.message}")
    }
    
    // 测试修复后的方式
    try {
        val test2 = ",}".replace(Regex(",\\s*}"), "}")
        println("✅ Regex() 构造函数方式成功: '$test2'")
    } catch (e: Exception) {
        println("❌ Regex() 构造函数方式失败: ${e.message}")
    }
    
    // 测试实际的JSON清理场景
    val testJson = """{"key": "value",}"""
    try {
        val cleaned = testJson.replace(Regex(",\\s*}"), "}")
        println("✅ JSON清理成功: $cleaned")
    } catch (e: Exception) {
        println("❌ JSON清理失败: ${e.message}")
    }
}
