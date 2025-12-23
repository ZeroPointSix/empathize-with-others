package com.empathy.ai.test

/**
 * 测试用代码文件
 * 用于验证Hook功能
 */
class TestRepository {
    
    fun getData(): String {
        return "test data"
    }
    
    fun deleteData() {
        // 模拟删除操作
        println("Data deleted")
    }
}

class TestUseCase(
    private val repository: TestRepository
) {
    suspend operator fun invoke(): Result<String> {
        return try {
            Result.success(repository.getData())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
