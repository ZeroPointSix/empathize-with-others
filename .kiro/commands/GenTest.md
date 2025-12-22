---
description: 自动生成测试 - 为 UseCase/ViewModel/Repository 生成测试骨架
---

# 自动生成测试命令

根据源文件自动生成对应的测试文件骨架。

## 使用方式

```
/GenTest EditFactUseCase           # 为指定类生成测试
/GenTest --current                 # 为当前打开的文件生成测试
/GenTest --missing                 # 为所有缺少测试的 UseCase 生成测试
```

## 执行流程

### 1. 分析源文件

读取源文件，提取：
- 类名和包名
- 构造函数参数（用于 Mock）
- 公开方法签名
- 方法参数和返回类型

### 2. 生成测试骨架

根据项目规范生成测试文件：

**UseCase 测试模板：**
```kotlin
package com.empathy.ai.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class {ClassName}Test {

    // Mock 依赖
    private lateinit var mockRepository: {RepositoryType}
    
    // 被测对象
    private lateinit var useCase: {ClassName}

    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = {ClassName}(mockRepository)
    }

    @Test
    fun `{methodName} 正常情况应返回成功`() = runTest {
        // Given
        coEvery { mockRepository.xxx() } returns Result.success(xxx)
        
        // When
        val result = useCase(xxx)
        
        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `{methodName} 异常情况应返回失败`() = runTest {
        // Given
        coEvery { mockRepository.xxx() } returns Result.failure(Exception("error"))
        
        // When
        val result = useCase(xxx)
        
        // Then
        assertTrue(result.isFailure)
    }
    
    // TODO: 添加更多测试用例
    // - 边界条件测试
    // - 空值处理测试
    // - 并发测试（如需要）
}
```

**ViewModel 测试模板：**
```kotlin
package com.empathy.ai.presentation.viewmodel

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class {ClassName}Test {

    private val testDispatcher = StandardTestDispatcher()
    
    // Mock 依赖
    private lateinit var mockUseCase: {UseCaseType}
    
    // 被测对象
    private lateinit var viewModel: {ClassName}

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockUseCase = mockk()
        viewModel = {ClassName}(mockUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `初始状态应为默认值`() {
        // Then
        assertEquals({DefaultState}, viewModel.uiState.value)
    }

    @Test
    fun `处理事件应更新状态`() = runTest {
        // Given
        coEvery { mockUseCase(any()) } returns Result.success(xxx)
        
        // When
        viewModel.onEvent({Event})
        advanceUntilIdle()
        
        // Then
        assertEquals({ExpectedState}, viewModel.uiState.value.xxx)
    }
}
```

### 3. 保存文件

将生成的测试文件保存到对应的测试目录：
- `app/src/main/.../Xxx.kt` → `app/src/test/.../XxxTest.kt`

### 4. 提示用户

```
✅ 测试文件已生成

📄 文件: app/src/test/java/com/empathy/ai/domain/usecase/EditFactUseCaseTest.kt

📝 生成的测试用例:
   - `invoke 正常情况应返回成功`
   - `invoke 异常情况应返回失败`
   
⚠️ TODO: 请补充以下测试场景:
   - 空内容验证
   - 超长内容处理
   - 并发编辑冲突

[打开文件] [运行测试]
```

## 项目规范遵循

生成的测试代码遵循以下规范：
1. 使用 MockK 进行 Mock
2. 使用 kotlinx-coroutines-test 进行协程测试
3. 测试方法名使用中文描述
4. 遵循 Given-When-Then 模式
5. 每个公开方法至少有正常和异常两个测试用例
