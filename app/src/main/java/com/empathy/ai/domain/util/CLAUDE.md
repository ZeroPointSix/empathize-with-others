# Domain Util - 领域工具模块

[根目录](../../../../CLAUDE.md) > [app](../../../) > [domain](../../) > [util](../) > **util**

## 模块职责

Domain Util模块提供领域层所需的各种工具类和辅助功能，包括错误处理、性能监控、数据清理、隐私保护、提示词处理等通用功能。这些工具类不依赖Android框架，确保了领域层的纯净性。

## 核心组件

### 1. 错误处理工具

#### ErrorHandler
- **文件**: `ErrorHandler.kt`
- **职责**: 统一的错误处理和用户提示
- **功能**:
  - 处理FloatingWindowError各种错误类型
  - 提供用户友好的错误提示
  - 记录详细错误日志
  - 提供错误恢复建议

#### ErrorMapper
- **文件**: `ErrorMapper.kt`
- **职责**: 错误映射和转换
- **功能**: 将底层错误转换为领域层错误模型

### 2. 悬浮窗管理

#### FloatingWindowManager
- **文件**: `FloatingWindowManager.kt`
- **职责**: 悬浮窗权限检查和服务管理
- **功能**:
  - 检查悬浮窗权限
  - 检查前台服务权限
  - 请求必要权限
  - 启动/停止悬浮窗服务

#### FloatingView
- **文件**: `FloatingView.kt`
- **职责**: 悬浮窗视图管理

### 3. 性能监控

#### PerformanceTracker
- **文件**: `PerformanceTracker.kt`
- **职责**: 性能数据追踪和统计

#### PerformanceMonitor
- **文件**: `PerformanceMonitor.kt`
- **职责**: 实时性能监控

#### PerformanceMetrics
- **文件**: `PerformanceMetrics.kt`
- **职责**: 性能指标定义和计算

### 4. 数据管理

#### DataCleanupManager
- **文件**: `DataCleanupManager.kt`
- **职责**: 数据清理管理
- **功能**:
  - 过期数据清理
  - 缓存管理
  - 存储优化

#### DataEncryption
- **文件**: `DataEncryption.kt`
- **职责**: 数据加密工具
- **功能**:
  - 敏感数据加密
  - 密钥管理
  - 解密验证

### 5. 隐私保护

#### PrivacyConfig
- **文件**: `PrivacyConfig.kt`
- **职责**: 隐私配置管理

#### PrivacyDataHandler
- **文件**: `PrivacyDataHandler.kt`
- **职责**: 隐私数据处理
- **功能**:
  - 数据脱敏
  - 隐私规则应用
  - 敏感信息过滤

### 6. 提示词处理

#### PromptBuilder
- **文件**: `PromptBuilder.kt`
- **职责**: 构建AI提示词
- **功能**:
  - 动态提示词生成
  - 模板填充
  - 上下文整合

#### PromptSanitizer
- **文件**: `PromptSanitizer.kt`
- **职责**: 提示词清理和验证
- **功能**:
  - 敏感信息过滤
  - 格式标准化
  - 安全检查

#### PromptValidator
- **文件**: `PromptValidator.kt`
- **职责**: 提示词有效性验证

#### PromptVariableResolver
- **文件**: `PromptVariableResolver.kt`
- **职责**: 提示词变量解析

#### PromptTemplates
- **文件**: `PromptTemplates.kt`
- **职责**: 提示词模板管理
- **功能**:
  - 预定义模板
  - 模板分类
  - 快速检索

### 7. AI响应处理

#### AiResponseCleaner
- **文件**: `AiResponseCleaner.kt`
- **职责**: AI响应清理
- **功能**:
  - 格式清理
  - 无用内容过滤
  - 结构化提取

#### AiSummaryProcessor
- **文件**: `AiSummaryProcessor.kt`
- **职责**: AI摘要处理

#### LocalSummaryProcessor
- **文件**: `LocalSummaryProcessor.kt`
- **职责**: 本地摘要处理

### 8. 上下文构建

#### ConversationContextBuilder
- **文件**: `ConversationContextBuilder.kt`
- **职责**: 对话上下文构建

#### ContextBuilder
- **文件**: `ContextBuilder.kt`
- **职责**: 通用上下文构建器

### 9. 工具类

#### DateUtils
- **文件**: `DateUtils.kt`
- **职责**: 日期时间工具

#### DateRangeValidator
- **文件**: `DateRangeValidator.kt`
- **职责**: 日期范围验证

#### WeChatDetector
- **文件**: `WeChatDetector.kt`
- **职责**: 微信环境检测

#### IdentityPrefixHelper
- **文件**: `IdentityPrefixHelper.kt`
- **职责**: 身份前缀处理

#### SystemPrompts
- **文件**: `SystemPrompts.kt`
- **职责**: 系统提示词管理

### 10. 配置管理

#### SecurityConfig
- **文件**: `SecurityConfig.kt`
- **职责**: 安全配置管理

#### CleanupConfig
- **文件**: `CleanupConfig.kt`
- **职责**: 清理配置管理

#### RetryConfig
- **文件**: `RetryConfig.kt`
- **职责**: 重试配置管理

### 11. 执行器

#### OperationExecutor
- **文件**: `OperationExecutor.kt`
- **职责**: 操作执行器

#### FailedTaskRecovery
- **文件**: `FailedTaskRecovery.kt`
- **职责**: 失败任务恢复

#### FallbackStrategy
- **文件**: `FallbackStrategy.kt`
- **职责**: 降级策略

### 12. 调试工具

#### DebugLogger
- **文件**: `DebugLogger.kt`
- **职责**: 调试日志管理

#### FloatingViewDebugLogger
- **文件**: `FloatingViewDebugLogger.kt`
- **职责**: 悬浮窗调试日志

#### MemoryLogger
- **文件**: `MemoryLogger.kt`
- **职责**: 内存日志记录

#### MemoryConstants
- **文件**: `MemoryConstants.kt`
- **职责**: 内存常量定义

### 13. 特殊功能

#### SummaryConflictChecker
- **文件**: `SummaryConflictChecker.kt`
- **职责**: 摘要冲突检查

#### PermissionManager
- **文件**: `PermissionManager.kt`
- **职责**: 权限管理器

#### ContactDetailError
- **文件**: `ContactDetailError.kt`
- **职责**: 联系人详情错误定义

## 使用示例

### 错误处理示例
```kotlin
// 处理悬浮窗错误
val error = FloatingWindowError.PermissionDenied("请授予悬浮窗权限")
ErrorHandler.handleError(context, error)
```

### 提示词构建示例
```kotlin
// 构建AI提示词
val prompt = PromptBuilder()
    .addContext("当前聊天内容: $chatContent")
    .addTemplate("分析模板")
    .addVariable("userName", "张三")
    .build()
```

### 性能监控示例
```kotlin
// 追踪性能
val tracker = PerformanceTracker.start("operation_name")
// ... 执行操作
tracker.end()
```

## 设计原则

1. **纯净性**: 不依赖Android框架，保持领域层纯净
2. **可复用性**: 提供通用工具，可在多个用例中复用
3. **安全性**: 包含必要的加密和验证机制
4. **性能意识**: 提供性能监控和优化工具
5. **隐私优先**: 内置隐私保护机制

## 注意事项

1. 所有工具类都设计为object，提供全局访问点
2. 错误处理要优雅，避免向用户暴露技术细节
3. 性能监控工具本身不应成为性能瓶颈
4. 隐私相关工具要格外小心，确保数据安全
5. 提示词处理要考虑输入验证和清理

## 相关文件清单

- ErrorHandler.kt - 错误处理器
- FloatingWindowManager.kt - 悬浮窗管理器
- PerformanceTracker.kt - 性能追踪器
- DataCleanupManager.kt - 数据清理管理器
- PromptBuilder.kt - 提示词构建器
- AiResponseCleaner.kt - AI响应清理器
- ConversationContextBuilder.kt - 对话上下文构建器
- 以及其他40+个工具类文件

## 变更记录 (Changelog)

### 2025-12-20 - Claude (模块文档创建)
- **创建domain/util模块文档**
- **记录40+个工具类的功能说明**
- **提供使用示例和设计原则说明**