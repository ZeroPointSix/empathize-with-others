[根目录](../CLAUDE.md) > **data**

# Data 数据层模块

## 模块职责

数据层实现模块，负责所有数据的访问、存储和远程调用。该模块实现了 domain 层定义的仓库接口，提供具体的数据访问能力。

## 入口与启动

### 构建配置 (build.gradle.kts)
- **插件**: android-library, kotlin-android, hilt, ksp, kapt
- **命名空间**: com.empathy.ai.data
- **最小SDK**: 24
- **编译SDK**: 35
- **依赖**: domain 模块（使用 api 暴露）

### 关键依赖
- Room: 本地数据库
- Retrofit: 网络请求
- Moshi: JSON 序列化
- Hilt: 依赖注入

## 对外接口

### 仓库实现 (Repository)
- `ContactRepositoryImpl` - 联系人数据访问
- `BrainTagRepositoryImpl` - 大脑标签数据访问
- `AiRepositoryImpl` - AI 请求处理
- `AiProviderRepositoryImpl` - AI 服务商管理
- `ConversationRepositoryImpl` - 对话记录管理
- `DailySummaryRepositoryImpl` - 每日总结管理
- `PromptRepositoryImpl` - 提示词配置管理
- `TopicRepositoryImpl` - 对话主题管理
- `UserProfileRepositoryImpl` - 用户画像管理
- `FailedTaskRepositoryImpl` - 失败任务管理
- `PrivacyRepositoryImpl` - 隐私数据管理
- `AiAdvisorRepositoryImpl` - AI军师数据访问（TD-00026）

## 关键依赖与配置

### 本地存储 (Local)
- `AppDatabase` - Room 数据库主类（版本 v11）
- `PromptFileStorage` - 提示词文件存储
- `ApiKeyStorage` - API 密钥加密存储
- `FloatingWindowPreferences` - 悬浮窗偏好设置
- `UserProfilePreferences` - 用户画像配置
- `MemoryPreferences` - 记忆系统配置

### DAO (数据访问对象)
- `ContactDao` - 联系人数据操作
- `BrainTagDao` - 标签数据操作
- `AiProviderDao` - AI 服务商数据操作
- `ConversationLogDao` - 对话记录操作
- `ConversationTopicDao` - 对话主题操作
- `DailySummaryDao` - 每日总结操作
- `FailedSummaryTaskDao` - 失败任务操作
- `AiAdvisorDao` - AI军师数据操作（TD-00026）

### Entity (数据库实体)
- `ContactProfileEntity` - 联系人实体
- `BrainTagEntity` - 标签实体
- `AiProviderEntity` - AI 服务商实体
- `ConversationLogEntity` - 对话记录实体
- `ConversationTopicEntity` - 对话主题实体
- `DailySummaryEntity` - 每日总结实体
- `FailedSummaryTaskEntity` - 失败任务实体
- `AiAdvisorConversationEntity` - AI军师对话实体（TD-00026）
- `AiAdvisorSessionEntity` - AI军师会话实体（TD-00026）

### 远程访问 (Remote)
- `OpenAiApi` - OpenAI API 接口定义
- `MessageDto` - 消息数据传输对象
- `ChatRequestDto` - 聊天请求数据传输对象
- `ChatResponseDto` - 聊天响应数据传输对象
- `ModelsResponseDto` - 模型列表响应
- `AiSummaryResponseDto` - 总结响应数据传输对象

### 解析器 (Parser)
- `AiResponseParser` - AI 响应解析接口
- `JsonCleaner` - JSON 清理工具
- `EnhancedJsonCleaner` - 增强型 JSON 清理
- `FieldMapper` - 字段映射器
- `FallbackHandler` - 降级处理器
- `AiSummaryResponseParserImpl` - 总结响应解析实现

## 数据模型

### 数据库类型转换器
- `RoomTypeConverters` - Room 类型转换（枚举、列表等）
- `FactListConverter` - Fact 列型转换

## 测试与质量

### 单元测试
- 位置: `data/src/test/kotlin/`
- 测试文件数: 21个（含AI军师仓库测试）
- 测试覆盖:
  - `FloatingWindowPreferencesTest` - 悬浮窗偏好测试
  - `PromptFileStorageTest` - 提示词存储测试
  - `PromptFileBackupTest` - 提示词备份测试
  - `AiRepositoryImplExtTest` - AI 仓库扩展测试
  - `AiProviderRepositoryFetchModelsTest` - 服务商模型获取测试
  - `DailySummaryRepositoryImplTest` - 每日总结仓库测试
  - `AiAdvisorRepositoryImplTest` - AI军师仓库测试（TD-00026）

### 集成测试
- 位置: `data/src/androidTest/kotlin/`
- 重点: Room 数据库迁移测试

## 常见问题 (FAQ)

### Q: 如何添加新的数据表？
A:
1. 在 `local/entity/` 中创建 Entity 类
2. 在 `local/dao/` 中创建 DAO 接口
3. 在 `AppDatabase` 中添加该实体
4. 创建数据库迁移（如果需要）
5. 实现对应的 Repository

### Q: 如何处理 API 响应解析失败？
A: 使用 `FallbackHandler` 和 `FieldMapper` 提供多级降级策略，确保即使 AI 响应格式异常也能提取有效数据。

### Q: API 密钥如何安全存储？
A: 使用 `EncryptedSharedPreferences` 通过 `ApiKeyStorage` 类进行硬件级加密存储，密钥永不以明文形式持久化。

## 相关文件清单

### 核心文件
- `local/AppDatabase.kt` - Room 数据库
- `di/DatabaseModule.kt` - 数据库 DI 配置
- `di/NetworkModule.kt` - 网络 DI 配置
- `di/RepositoryModule.kt` - 仓库绑定配置

### DAO 文件
- `local/dao/ContactDao.kt`
- `local/dao/BrainTagDao.kt`
- `local/dao/AiProviderDao.kt`
- `local/dao/ConversationLogDao.kt`
- `local/dao/ConversationTopicDao.kt`
- `local/dao/DailySummaryDao.kt`
- `local/dao/FailedSummaryTaskDao.kt`

### Repository 实现文件
- `repository/ContactRepositoryImpl.kt`
- `repository/BrainTagRepositoryImpl.kt`
- `repository/AiRepositoryImpl.kt`
- `repository/AiProviderRepositoryImpl.kt`
- `repository/ConversationRepositoryImpl.kt`
- `repository/DailySummaryRepositoryImpl.kt`
- `repository/PromptRepositoryImpl.kt`
- `repository/TopicRepositoryImpl.kt`
- `repository/UserProfileRepositoryImpl.kt`
- `repository/AiAdvisorRepositoryImpl.kt`（TD-00026）

## 变更记录 (Changelog)

### 2026-01-04 - Claude (AI军师功能文档更新)
- 新增AiAdvisorDao数据访问接口
- 新增AiAdvisorConversationEntity和AiAdvisorSessionEntity实体
- 新增AiAdvisorRepositoryImpl仓库实现
- 新增AiAdvisorRepositoryImplTest单元测试
- 更新测试覆盖信息（21个测试文件）
- 更新Android集成测试（数据库迁移测试）

### 2025-12-27 - Claude (数据层模块文档初始化)
- 创建 data 模块 CLAUDE.md 文档
- 添加导航面包屑
- 整理模块职责和关键组件说明

### 2025-12-15 - Kiro (数据层完善)
- 完成 Room 数据库 v11 迁移
- 添加 PromptFileStorage 文件存储
- 实现 ApiKeyStorage 加密存储
- 完善所有 Repository 实现

---

**最后更新**: 2026-01-04 02:59:33 | 更新者: Claude
**模块状态**: 完成（AI军师功能 TD-00026）
**代码质量**: A级（完整注释、错误处理）
**测试覆盖**: 包含21个单元测试和5个Android测试（74主源码 + 21测试 + 5Android测试）
