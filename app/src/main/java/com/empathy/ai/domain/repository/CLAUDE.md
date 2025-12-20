[根目录](../../../../CLAUDE.md) > [domain](../) > **repository**

# domain/repository - 数据仓库接口

## 模块职责

本模块定义了Clean Architecture中Repository接口，作为领域层与数据层之间的抽象层。所有Repository接口都遵循"依赖倒置原则"，确保领域层不依赖具体的数据访问实现。

## 接口列表

### 1. AiRepository - AI服务仓库接口
**文件**: `AiRepository.kt`
**职责**: AI服务的统一接口，提供分析、检查、润色、回复等核心AI功能
**服务对象**: AnalyzeChatUseCase, CheckDraftUseCase, PolishDraftUseCase, GenerateReplyUseCase等

**核心方法**:
- `analyzeChat()` - 分析聊天内容，提供策略建议
- `checkDraftSafety()` - 实时安全检查
- `polishDraft()` - 润色文本
- `generateReply()` - 生成回复
- `transcribeMedia()` - 媒体转文字
- `extractTextInfo()` - 文本信息萃取

### 2. ContactRepository - 联系人画像仓库接口
**文件**: `ContactRepository.kt`
**职责**: 管理联系人画像的CRUD操作，支持事实字段的增量更新
**服务对象**: 联系人列表、画像详情、RAG上下文构建

**核心方法**:
- `getAllProfiles()` - 获取所有联系人画像（Flow响应式）
- `getProfile()` - 获取单个联系人画像
- `saveProfile()` - 保存/更新完整画像
- `updateContactFacts()` - 增量更新事实字段
- `updateRelationshipScore()` - 更新关系分数
- `updateLastInteractionDate()` - 更新最后互动日期

### 3. ConversationRepository - 对话记录仓库接口
**文件**: `ConversationRepository.kt`
**职责**: 管理用户对话历史，支持总结标记和清理策略
**服务对象**: 对话管理、每日总结、上下文构建

**核心方法**:
- `saveUserInput()` - 保存用户输入
- `updateAiResponse()` - 更新AI回复
- `getUnsummarizedLogs()` - 获取未总结记录
- `getRecentConversations()` - 获取最近N条对话
- `markAsSummarized()` - 标记为已总结
- `cleanupOldSummarizedLogs()` - 清理过期记录

### 4. PrivacyRepository - 隐私规则仓库接口
**文件**: `PrivacyRepository.kt`
**职责**: 管理隐私脱敏规则，提供文本脱敏和还原功能
**服务对象**: PrivacyEngine（脱敏引擎）

**核心方法**:
- `getPrivacyMapping()` - 获取隐私映射规则
- `addRule()`/`removeRule()` - 管理隐私规则
- `maskText()`/`unmaskText()` - 文本脱敏/还原

### 5. BrainTagRepository - 军师锦囊仓库接口
**文件**: `BrainTagRepository.kt`
**职责**: 管理标签系统，支持分类浏览和快速检索

### 6. SettingsRepository - 设置仓库接口
**文件**: `SettingsRepository.kt`
**职责**: 管理应用配置和用户偏好设置

### 7. PromptRepository - 提示词仓库接口
**文件**: `PromptRepository.kt`
**职责**: 管理提示词模板和用户自定义提示词

### 8. AiProviderRepository - AI服务商仓库接口
**文件**: `AiProviderRepository.kt`
**职责**: 管理多个AI服务商的配置信息

### 9. DailySummaryRepository - 每日总结仓库接口
**文件**: `DailySummaryRepository.kt`
**职责**: 管理每日AI生成的对话总结

### 10. FailedTaskRepository - 失败任务仓库接口
**文件**: `FailedTaskRepository.kt`
**职责**: 管理失败任务的重试机制

## 设计特点

1. **响应式设计**: 使用Flow提供响应式数据流，UI能自动响应数据变化
2. **Result模式**: 所有操作返回Result类型，统一处理成功/失败
3. **清晰的服务对象标注**: 每个接口都标注了主要的使用方
4. **增量更新支持**: 支持部分字段的增量更新，减少数据传输
5. **事务支持**: 提供批量更新的事务方法

## 实现层

这些接口由data层的对应Repository实现类实现：
- ContactRepositoryImpl
- AiRepositoryImpl
- ConversationRepositoryImpl
- PrivacyRepositoryImpl
- 等等...

## 相关文件清单

- `AiRepository.kt` - AI服务接口定义
- `ContactRepository.kt` - 联系人画像接口定义
- `ConversationRepository.kt` - 对话记录接口定义
- `PrivacyRepository.kt` - 隐私规则接口定义
- `BrainTagRepository.kt` - 标签系统接口定义
- `SettingsRepository.kt` - 设置管理接口定义
- `PromptRepository.kt` - 提示词管理接口定义
- `AiProviderRepository.kt` - AI服务商接口定义
- `DailySummaryRepository.kt` - 每日总结接口定义
- `FailedTaskRepository.kt` - 失败任务接口定义

## 变更记录 (Changelog)

### 2025-12-20 - Claude (模块文档创建)
- **创建domain/repository模块的CLAUDE.md文档**
- **整理所有Repository接口的核心功能和职责**
- **标注服务对象和使用场景**

---

**最后更新**: 2025-12-20 | 更新者: Claude
**维护者**: hushaokang
**文档版本**: v1.0.0
**Git提交**: 阶段C-深度补捞