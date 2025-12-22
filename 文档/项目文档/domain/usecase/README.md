# Domain UseCase 模块文档

> [根目录](../../../../CLAUDE.md) > [项目文档](../../README.md) > [domain](../README.md) > **usecase**

## 模块职责

UseCase模块封装了应用的核心业务操作，每个用例：
- **单一职责**: 只执行一个特定的业务操作
- **可组合性**: 可以组合其他用例实现复杂流程
- **异步支持**: 使用协程处理异步操作
- **错误处理**: 统一的错误处理机制

## 用例分类

### 联系人管理用例
- `GetContactUseCase` - 获取单个联系人
- `GetAllContactsUseCase` - 获取所有联系人
- `SaveProfileUseCase` - 保存联系人画像
- `DeleteContactUseCase` - 删除联系人
- `UpdateUserProfileUseCase` - 更新用户画像

### AI分析用例
- `AnalyzeChatUseCase` - 分析聊天内容
- `GenerateReplyUseCase` - 生成回复建议
- `PolishDraftUseCase` - 润色草稿
- `FeedTextUseCase` - 喂养文本到AI
- `SummarizeDailyConversationsUseCase` - 每日对话总结

### 脑标签管理用例
- `GetBrainTagsUseCase` - 获取脑标签
- `SaveBrainTagUseCase` - 保存脑标签
- `DeleteBrainTagUseCase` - 删除脑标签
- `AddTagUseCase` - 添加标签
- `RemoveTagUseCase` - 移除标签

### 用户画像用例
- `GetUserProfileUseCase` - 获取用户画像
- `AddUserProfileTagUseCase` - 添加用户画像标签
- `RemoveUserProfileTagUseCase` - 移除用户画像标签
- `ManageCustomDimensionUseCase` - 管理自定义维度
- `ExportUserProfileUseCase` - 导出用户画像

### 服务商管理用例
- `GetProvidersUseCase` - 获取AI服务商
- `SaveProviderUseCase` - 保存服务商配置
- `DeleteProviderUseCase` - 删除服务商
- `TestConnectionUseCase` - 测试连接

## 设计模式

### 1. 标准用例模式
```kotlin
class ExampleUseCase @Inject constructor(
    private val repository: ExampleRepository
) {
    suspend operator fun invoke(param: Param): Result<Type> {
        return try {
            val result = repository operation(param)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(AppError.ExampleError(e))
        }
    }
}
```

### 2. 组合用例模式
```kotlin
class ComplexUseCase @Inject constructor(
    private val useCase1: UseCase1,
    private val useCase2: UseCase2
) {
    suspend operator fun invoke(param: Param): Result<Type> {
        return useCase1(param)
            .flatMap { result1 -> useCase2(result1) }
    }
}
```

## 使用指南

### 1. 注入依赖
- 使用Hilt注入Repository和其他UseCase
- 避免在构造函数中注入过多依赖

### 2. 错误处理
- 使用Result类型封装返回值
- 转换底层异常为业务异常
- 提供有意义的错误信息

### 3. 测试策略
- Mock所有外部依赖
- 测试成功和失败场景
- 验证参数传递

## 性能优化

### 1. 协程使用
- 使用Dispatchers.IO处理IO操作
- 避免不必要的线程切换
- 合理使用withContext

### 2. 缓存策略
- 缓存频繁访问的数据
- 实现失效机制
- 考虑内存使用

### 3. 批量操作
- 支持批量处理
- 使用事务保证一致性
- 提供进度反馈

## 相关文件清单

### 联系人相关
- `GetContactUseCase.kt`
- `GetAllContactsUseCase.kt`
- `SaveProfileUseCase.kt`
- `DeleteContactUseCase.kt`

### AI相关
- `AnalyzeChatUseCase.kt`
- `GenerateReplyUseCase.kt`
- `PolishDraftUseCase.kt`
- `FeedTextUseCase.kt`

### 标签相关
- `GetBrainTagsUseCase.kt`
- `SaveBrainTagUseCase.kt`
- `DeleteBrainTagUseCase.kt`

### 用户画像相关
- `GetUserProfileUseCase.kt`
- `AddUserProfileTagUseCase.kt`
- `ExportUserProfileUseCase.kt`

## 变更记录

### 2025-12-21 - 初始创建
- 创建usecase模块文档
- 分类整理现有用例
- 定义设计模式和最佳实践