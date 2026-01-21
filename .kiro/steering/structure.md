# 项目结构

## 🔴 必读文档（开始工作前必须阅读）

**在开始任何工作之前，请务必先阅读以下文档：**

1. **[Rules/RulesReadMe.md](../../Rules/RulesReadMe.md)** - 项目通用规则和文档规范
2. **[WORKSPACE.md](../../WORKSPACE.md)** - 当前工作状态和任务协调

---

## 语言规范

**所有文档和回答必须使用中文。** 代码注释、变量名、类名等保持英文，但所有说明文档、开发指南和与开发者的沟通必须使用中文。

## 架构模式

**Clean Architecture + MVVM**，严格分层与单向依赖。

## 多模块架构（基于当前代码）

> 2026-01-21 更新
>
> 当前分支: main
> 当前版本: v1.14.17 (versionCode: 11417, dev)
> 数据库版本: Room v17

### 模块结构

```
:domain/        # 纯 Kotlin - 业务模型、UseCase、Repository 接口
:data/          # Android Library - Room、Retrofit、Repository 实现、DI
:presentation/  # Android Library - Compose UI、ViewModel、Navigation
:app/           # Application - 应用入口、系统服务、应用级 DI
```

### 模块依赖关系

```
                    ┌─────────────┐
                    │    :app     │
                    │ (Application)│
                    └──────┬──────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
           ▼               ▼               ▼
    ┌──────────┐    ┌──────────┐    ┌──────────────┐
    │  :data   │    │ :domain  │    │:presentation │
    │(Library) │    │ (Kotlin) │    │  (Library)   │
    └────┬─────┘    └──────────┘    └──────┬───────┘
         │               ▲                  │
         └───────────────┴──────────────────┘
```

### 模块职责

| 模块 | 类型 | 职责 | 依赖 |
|------|------|------|------|
| `:domain` | Kotlin Library | 业务模型、Repository 接口、UseCase、领域服务、工具类 | kotlinx.coroutines + javax.inject |
| `:data` | Android Library | Room、网络、Repository 实现、数据偏好设置、DI | :domain |
| `:presentation` | Android Library | Compose UI、ViewModel、Navigation、Theme | :domain |
| `:app` | Application | 应用入口、系统服务、应用级 DI | :domain, :data, :presentation |

### DI 模块分布（基于实际代码）

**:data 模块（8 个）**
- DatabaseModule
- NetworkModule
- RepositoryModule
- MemoryModule
- PromptModule
- DispatcherModule
- OkHttpClientFactory
- Qualifiers

**:app 模块（16 个）**
- LoggerModule
- AppDispatcherModule
- ServiceModule
- FloatingWindowModule
- FloatingWindowManagerModule
- NotificationModule
- SummaryModule
- EditModule
- PersonaModule
- TopicModule
- UserProfileModule
- AiAdvisorModule
- ProxyModule
- ApiUsageModule
- SystemPromptModule

---

## 包组织结构（基于当前代码扫描）

### :domain 模块 (纯 Kotlin)

```
domain/src/main/kotlin/com/empathy/ai/domain/
├── model/        # 业务模型（86 个 .kt）
├── repository/   # 仓库接口（21 个 .kt）
├── usecase/      # 用例（65 个 .kt）
├── service/      # 领域服务（2 个 .kt）
└── util/         # 领域工具（30 个 .kt）
```

### :data 模块 (Android Library)

```
data/src/main/kotlin/com/empathy/ai/data/
├── di/           # DI 模块（8 个）
├── local/        # 本地存储
│   ├── dao/      # DAO（10 个）
│   ├── entity/   # Entity（11 个）
│   └── converter/# TypeConverter（2 个）
├── remote/       # 网络层
│   ├── api/      # OpenAI 兼容 API（1 个）
│   └── model/    # DTO（7 个）
├── parser/       # AI 响应解析器（6 个）
└── repository/   # Repository 实现（17 个）
```

### :presentation 模块 (Android Library)

```
presentation/src/main/kotlin/com/empathy/ai/presentation/
├── navigation/   # 导航（5 个）
├── theme/        # 主题（14 个）
├── viewmodel/    # ViewModel（28 个）
└── ui/
    ├── component/ # 复用组件（含 iOS 风格组件）
    └── screen/    # 业务页面
        ├── advisor/
        ├── aiconfig/
        ├── chat/
        ├── contact/
        ├── prompt/
        ├── settings/
        ├── tag/
        └── userprofile/
```

### :app 模块 (Application)

```
app/src/main/java/com/empathy/ai/
├── app/          # Application
├── di/           # 应用级 DI（16 个）
├── notification/ # 通知
├── service/      # 系统服务（FloatingWindowService）
├── ui/           # MainActivity / ScreenshotPermissionActivity
└── util/         # Android 平台工具
```

---

## 层级职责

### 领域层（:domain）
- 纯 Kotlin，无 Android 依赖
- 模型、UseCase、Repository 接口、领域服务

### 数据层（:data）
- Repository 实现、Room 数据库、网络请求与解析
- EncryptedSharedPreferences + 偏好设置

### 表现层（:presentation）
- Compose UI + ViewModel + Navigation
- UI 组件与 iOS 风格组件库

### 应用层（:app）
- Application 入口
- 系统服务（悬浮窗、通知）
- 应用级依赖注入聚合

---

## 测试结构（.kt 统计）

| 模块 | main | test | androidTest |
|------|------|------|-------------|
| `:domain` | 204 | 60 | 0 |
| `:data` | 89 | 27 | 7 |
| `:presentation` | 287 | 75 | 10 |
| `:app` | 30 | 5 | 9 |

> 说明：`app/src/test-disabled` 与 `app/src/androidTest-disabled` 仍保留大量集成/性能测试，但默认不参与构建。

---

## 数据库概览

- **版本**: Room v17
- **迁移**: MIGRATION_1_2 → MIGRATION_16_17（完整链）
- **核心表（11 张）**: profiles、brain_tags、ai_providers、conversation_logs、conversation_topics、daily_summaries、failed_summary_tasks、api_usage_records、ai_advisor_sessions、ai_advisor_conversations、ai_advisor_message_blocks

---

## 导航系统概览

- 路由定义集中在 `presentation/navigation/NavRoutes.kt`
- 单 Activity + Compose Navigation
- 底部 Tab: CONTACT_LIST / AI_ADVISOR / SETTINGS

---

**文档版本**: 4.0
**最后更新**: 2026-01-21
**更新内容**:
- 基于当前代码结构与统计数据更新模块与架构说明
