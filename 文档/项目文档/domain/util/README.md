# Domain Util 模块文档

> [根目录](../../../../CLAUDE.md) > [项目文档](../../README.md) > [domain](../README.md) > **util**

## 模块职责

Domain Util模块提供领域层通用的工具类和辅助功能：
- **工具函数**: 提供可复用的纯函数工具
- **构建器**: 复杂对象的构建辅助
- **验证器**: 业务规则验证
- **转换器**: 数据格式转换

## 工具类列表

### 核心工具

#### Logger
- **文件**: `Logger.kt`
- **职责**: 统一日志记录
- **功能**: 分级日志、标签管理、性能追踪

#### PromptBuilder
- **文件**: `PromptBuilder.kt`
- **职责**: AI提示词构建
- **功能**: 模板渲染、变量替换、上下文注入

#### IdentityPrefixHelper
- **文件**: `IdentityPrefixHelper.kt`
- **职责**: ID前缀生成与管理
- **功能**: 唯一ID生成、前缀解析

#### ErrorMapper
- **文件**: `ErrorMapper.kt`
- **职责**: 错误映射与转换
- **功能**: 异常到业务错误转换、错误链追踪

#### PerformanceMetrics
- **文件**: `PerformanceMetrics.kt`
- **职责**: 性能指标收集
- **功能**: 执行时间统计、内存使用监控

### 提示词工具

#### PromptSanitizer
- **文件**: `PromptSanitizer.kt`
- **职责**: 提示词清洗与验证
- **功能**: 格式化、安全检查、长度限制

#### PromptValidator
- **文件**: `PromptValidator.kt`
- **职责**: 提示词有效性验证
- **功能**: 语法检查、变量验证

#### PromptVariableResolver
- **文件**: `PromptVariableResolver.kt`
- **职责**: 提示词变量解析
- **功能**: 变量替换、上下文注入

#### PromptTemplates
- **文件**: `PromptTemplates.kt`
- **职责**: 预定义提示词模板
- **功能**: 模板管理、快速获取

#### SystemPrompts
- **文件**: `SystemPrompts.kt`
- **职责**: 系统级提示词
- **功能**: 系统角色定义、行为约束

### 上下文构建器

#### ContextBuilder
- **文件**: `ContextBuilder.kt`
- **职责**: 通用上下文构建
- **功能**: DSL风格构建

#### ConversationContextBuilder
- **文件**: `ConversationContextBuilder.kt`
- **职责**: 对话上下文构建
- **功能**: 历史消息整理、上下文压缩

#### UserProfileContextBuilder
- **文件**: `UserProfileContextBuilder.kt`
- **职责**: 用户画像上下文构建
- **功能**: 画像信息提取、标签聚合

### 验证器

#### ContentValidator
- **文件**: `ContentValidator.kt`
- **职责**: 内容验证
- **功能**: 长度检查、格式验证、敏感词检测

#### DateRangeValidator
- **文件**: `DateRangeValidator.kt`
- **职责**: 日期范围验证
- **功能**: 日期合法性检查、范围验证

#### UserProfileValidator
- **文件**: `UserProfileValidator.kt`
- **职责**: 用户画像验证
- **功能**: 必填字段检查、数据完整性验证

### 数据处理

#### JsonParser
- **文件**: `JsonParser.kt`
- **职责**: JSON解析工具
- **功能**: 安全解析、错误恢复

#### AiSummaryResponseParser
- **文件**: `AiSummaryResponseParser.kt`
- **职责**: AI总结响应解析
- **功能**: 结构化提取、字段映射

#### FactSearchFilter
- **文件**: `FactSearchFilter.kt`
- **职责**: 事实搜索过滤
- **功能**: 关键词匹配、相关性排序

#### CategoryColorAssigner
- **文件**: `CategoryColorAssigner.kt`
- **职责**: 分类颜色分配
- **功能**: 自动配色、颜色冲突避免

### 总结处理

#### SummaryConflictChecker
- **文件**: `SummaryConflictChecker.kt`
- **职责**: 总结冲突检查
- **功能**: 重复检测、矛盾识别

#### LocalSummaryProcessor
- **文件**: `LocalSummaryProcessor.kt`
- **职责**: 本地总结处理
- **功能**: 规则总结、模板填充

#### AiSummaryProcessor
- **文件**: `AiSummaryProcessor.kt`
- **职责**: AI总结处理
- **功能**: AI调用、结果处理

### 任务恢复

#### FailedTaskRecovery
- **文件**: `FailedTaskRecovery.kt`
- **职责**: 失败任务恢复
- **功能**: 重试策略、错误处理

### 数据清理

#### DataCleanupManager
- **文件**: `DataCleanupManager.kt`
- **职责**: 数据清理管理
- **功能**: 过期数据清理、空间回收

### 悬浮窗管理

#### FloatingWindowManager
- **文件**: `FloatingWindowManager.kt`
- **职责**: 悬浮窗状态管理
- **功能**: 窗口状态、位置记忆

### 其他工具

#### DateUtils
- **文件**: `DateUtils.kt`
- **职责**: 日期工具
- **功能**: 日期格式化、时区转换

#### MemoryConstants
- **文件**: `MemoryConstants.kt`
- **职责**: 内存常量定义
- **功能**: 缓存大小、限制配置

#### CoroutineDispatchers
- **文件**: `CoroutineDispatchers.kt`
- **职责**: 协程调度器
- **功能**: IO调度、计算调度

## 设计原则

### 1. 纯函数优先
- 工具函数应该是纯函数
- 避免副作用和可变状态
- 便于测试和复用

### 2. 无依赖
- 不依赖Android框架
- 不依赖其他层
- 保持纯粹的工具性质

### 3. 可组合
- 小而专注的工具函数
- 支持函数组合
- DSL友好

## 使用示例

### PromptBuilder使用

```kotlin
val prompt = PromptBuilder()
    .setTemplate("分析{{content}}的情感")
    .setVariable("content", chatMessage.content)
    .setSystemPrompt(SystemPrompts.EMPATHY_ANALYST)
    .build()
```

### ContextBuilder使用

```kotlin
val context = ConversationContextBuilder()
    .addMessages(recentMessages)
    .setContactInfo(contactProfile)
    .setConstraints(maxTokens = 2000)
    .build()
```

## 相关文件清单

### 核心工具
- `Logger.kt` - 日志工具
- `PromptBuilder.kt` - 提示词构建
- `IdentityPrefixHelper.kt` - ID辅助
- `ErrorMapper.kt` - 错误映射
- `PerformanceMetrics.kt` - 性能监控

### 提示词工具
- `PromptSanitizer.kt` - 提示词清洗
- `PromptValidator.kt` - 提示词验证
- `PromptVariableResolver.kt` - 变量解析
- `PromptTemplates.kt` - 提示词模板
- `SystemPrompts.kt` - 系统提示词

### 上下文构建
- `ContextBuilder.kt` - 上下文构建
- `ConversationContextBuilder.kt` - 对话上下文
- `UserProfileContextBuilder.kt` - 用户画像上下文

### 验证器
- `ContentValidator.kt` - 内容验证
- `DateRangeValidator.kt` - 日期验证
- `UserProfileValidator.kt` - 画像验证

## 变更记录

### 2025-12-25 - 初始创建
- 创建domain/util模块文档
- 列出所有工具类及其职责
