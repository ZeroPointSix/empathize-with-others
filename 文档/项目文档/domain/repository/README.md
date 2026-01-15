# Domain Repository 模块文档

> [根目录](../../../../CLAUDE.md) > [项目文档](../../README.md) > [domain](../README.md) > **repository**

## 模块职责

Domain Repository模块定义了数据访问的抽象接口，是Clean Architecture中领域层与数据层之间的契约：
- **接口定义**: 定义数据操作的业务契约
- **抽象隔离**: 隔离领域层与具体数据实现
- **类型安全**: 使用领域模型作为数据类型
- **依赖倒置**: 领域层依赖接口，数据层实现接口

## 仓库接口列表

### 核心业务仓库

#### ContactRepository
- **文件**: `ContactRepository.kt`
- **职责**: 联系人画像数据管理
- **主要方法**:
  - `getContact(id: String)`: 获取单个联系人
  - `getAllContacts()`: 获取所有联系人
  - `saveContact(contact: ContactProfile)`: 保存联系人
  - `deleteContact(id: String)`: 删除联系人
  - `searchContacts(query: String)`: 搜索联系人

#### BrainTagRepository
- **文件**: `BrainTag.kt`
- **职责**: 脑标签（知识标签）管理
- **主要方法**:
  - `getTags(contactId: String)`: 获取联系人标签
  - `saveTag(tag: BrainTag)`: 保存标签
  - `deleteTag(tagId: String)`: 删除标签
  - `addFact(tagId: String, fact: Fact)`: 添加事实

#### AiRepository
- **文件**: `AiRepository.kt`
- **职责**: AI服务交互
- **主要方法**:
  - `analyze(request: AnalysisRequest)`: 分析聊天内容
  - `generateReply(request: ReplyRequest)`: 生成回复建议
  - `polishDraft(request: PolishRequest)`: 润色草稿
  - `feedText(request: FeedRequest)`: 喂养文本

### 配置管理仓库

#### AiProviderRepository
- **文件**: `AiProviderRepository.kt`
- **职责**: AI服务商配置管理
- **主要方法**:
  - `getProviders()`: 获取所有服务商
  - `saveProvider(provider: AiProvider)`: 保存服务商
  - `deleteProvider(id: String)`: 删除服务商
  - `testConnection(provider: AiProvider)`: 测试连接

#### PromptRepository
- **文件**: `PromptRepository.kt`
- **职责**: 提示词模板管理
- **主要方法**:
  - `getPrompts()`: 获取所有提示词
  - `getPromptByScene(scene: PromptScene)`: 按场景获取提示词
  - `savePrompt(prompt: Prompt)`: 保存提示词
  - `deletePrompt(id: String)`: 删除提示词

#### SettingsRepository
- **文件**: `SettingsRepository.kt`
- **职责**: 应用设置管理
- **主要方法**:
  - `getSettings()`: 获取应用设置
  - `updateSettings(settings: Settings)`: 更新设置
  - `resetToDefault()`: 重置为默认设置

### 对话与总结仓库

#### ConversationRepository
- **文件**: `ConversationRepository.kt`
- **职责**: 对话数据管理
- **主要方法**:
  - `getConversation(id: String)`: 获取对话
  - `saveConversation(conversation: Conversation)`: 保存对话
  - `getConversationsByContact(contactId: String)`: 获取联系人的对话

#### DailySummaryRepository
- **文件**: `DailySummaryRepository.kt`
- **职责**: 每日对话总结管理
- **主要方法**:
  - `getSummary(date: LocalDate)`: 获取指定日期总结
  - `saveSummary(summary: DailySummary)`: 保存总结
  - `getSummariesInRange(start: LocalDate, end: LocalDate)`: 获取日期范围总结

### 用户画像仓库

#### UserProfileRepository
- **文件**: `UserProfileRepository.kt`
- **职责**: 用户画像管理
- **主要方法**:
  - `getUserProfile()`: 获取用户画像
  - `updateUserProfile(profile: UserProfile)`: 更新用户画像
  - `exportProfile()`: 导出用户画像
  - `importProfile(data: String)`: 导入用户画像

### 隐私与安全仓库

#### PrivacyRepository
- **文件**: `PrivacyRepository.kt`
- **职责**: 隐私配置管理
- **主要方法**:
  - `getPrivacySettings()`: 获取隐私设置
  - `updatePrivacySettings(settings: PrivacySettings)`: 更新隐私设置
  - `isSensitiveDataAllowed()`: 检查敏感数据是否允许

### 其他仓库

#### TopicRepository
- **文件**: `TopicRepository.kt`
- **职责**: 对话主题管理
- **主要方法**:
  - `getTopics()`: 获取所有主题
  - `saveTopic(topic: Topic)`: 保存主题
  - `deleteTopic(id: String)`: 删除主题

#### FailedTaskRepository
- **文件**: `FailedTaskRepository.kt`
- **职责**: 失败任务管理
- **主要方法**:
  - `saveFailedTask(task: FailedTask)`: 保存失败任务
  - `getFailedTasks()`: 获取失败任务列表
  - `retryTask(taskId: String)`: 重试失败任务

#### FloatingWindowPreferencesRepository
- **文件**: `FloatingWindowPreferencesRepository.kt`
- **职责**: 悬浮窗偏好设置
- **主要方法**:
  - `getPreferences()`: 获取悬浮窗设置
  - `updatePreferences(prefs: FloatingWindowPrefs)`: 更新设置

## 设计原则

### 1. 接口隔离
- 每个Repository只负责一种数据类型
- 接口方法应简洁明了
- 避免臃肿的"上帝接口"

### 2. 协程支持
- 所有数据操作使用suspend函数
- 支持异步操作和取消
- 使用Flow提供响应式数据流

### 3. 错误处理
- 返回Result类型封装成功/失败
- 提供有意义的错误信息
- 支持错误恢复机制

## 接口示例

```kotlin
interface ContactRepository {
    /**
     * 获取单个联系人
     * @param id 联系人ID
     * @return Result<ContactProfile> 成功返回联系人，失败返回错误
     */
    suspend fun getContact(id: String): Result<ContactProfile>

    /**
     * 获取所有联系人
     * @return Flow<List<ContactProfile>> 联系人列表的响应式流
     */
    fun getAllContacts(): Flow<List<ContactProfile>>

    /**
     * 保存联系人
     * @param contact 联系人画像
     * @return Result<ContactProfile> 成功返回保存后的联系人
     */
    suspend fun saveContact(contact: ContactProfile): Result<ContactProfile>

    /**
     * 删除联系人
     * @param id 联系人ID
     * @return Result<Unit> 成功返回Unit，失败返回错误
     */
    suspend fun deleteContact(id: String): Result<Unit>
}
```

## 实现指南

### data层实现
```kotlin
@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val contactDao: ContactDao,
    private val privacyEngine: PrivacyEngine
) : ContactRepository {

    override suspend fun getContact(id: String): Result<ContactProfile> {
        return try {
            val entity = contactDao.getById(id)
            val profile = entity.toDomainModel()
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e))
        }
    }

    // ... 其他方法实现
}
```

## 最佳实践

### 1. 命名规范
- 接口以"Repository"结尾
- 方法名使用动词+名词形式（如getContact, saveContact）
- 返回类型明确表示成功/失败

### 2. 数据转换
- Repository接口使用领域模型
- 实现层负责领域模型与数据模型转换
- 保持接口类型一致性

### 3. 缓存策略
- 优先从本地缓存读取
- 后台同步最新数据
- 提供数据刷新机制

### 4. 测试策略
- 使用Mock Repository进行单元测试
- 测试成功和失败场景
- 验证数据转换正确性

## 相关文件清单

### 仓库接口
- `ContactRepository.kt` - 联系人仓库
- `BrainTagRepository.kt` - 脑标签仓库
- `AiRepository.kt` - AI服务仓库
- `AiProviderRepository.kt` - AI服务商仓库
- `PromptRepository.kt` - 提示词仓库
- `SettingsRepository.kt` - 设置仓库
- `ConversationRepository.kt` - 对话仓库
- `DailySummaryRepository.kt` - 每日总结仓库
- `UserProfileRepository.kt` - 用户画像仓库
- `PrivacyRepository.kt` - 隐私仓库
- `TopicRepository.kt` - 主题仓库
- `FailedTaskRepository.kt` - 失败任务仓库
- `FloatingWindowPreferencesRepository.kt` - 悬浮窗设置仓库

## 变更记录

### 2025-12-25 - 初始创建
- 创建domain/repository模块文档
- 列出所有仓库接口及其职责
- 定义设计原则和最佳实践
