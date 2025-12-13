# 共情 AI 助手 项目宪法

## 核心原则

### I. 架构优先

**Clean Architecture + MVVM** 是本项目的核心架构模式。

- 领域层必须是纯 Kotlin，无 Android 依赖
- 数据层实现领域层定义的接口
- 表现层使用 Jetpack Compose + ViewModel
- 依赖注入使用 Hilt

### II. 隐私优先

**零后端架构，所有数据本地存储。**

- 敏感数据使用 EncryptedSharedPreferences
- AI 处理前强制掩码敏感信息
- 用户自带 API 密钥 (BYOK)

### III. 中文文档

**所有文档和回答必须使用中文。**

- 代码注释、变量名、类名保持英文
- 说明文档、开发指南使用中文
- 与开发者的沟通使用中文

### IV. 文档规范

**开发过程文档和项目长期文档分类管理。**

开发过程文档 (PRD, FD, TDD, TP 等):
- 存放路径: `文档/开发文档/[类型]/`
- 命名规则: `[类型]-xxxxx-功能描述`

项目长期文档 (SD, AD, API, UM 等):
- 存放路径: `文档/长期文档/[类型]/`
- 命名规则: `[类型]-xxxxx-主题描述`

### V. 测试驱动

**TDD 推荐但非强制。**

- 单元测试: JUnit 4.13.2 + MockK 1.13.13
- 协程测试: kotlinx-coroutines-test
- UI 测试: Compose UI Test

### VI. 简洁原则

**保持简单，避免过度设计。**

- 只实现当前需要的功能
- 避免不必要的抽象层
- 代码清晰优于代码简短

## 技术约束

### 构建系统

- Gradle 8.13 + Kotlin DSL
- AGP 8.7.3
- Kotlin 2.0.21
- JDK 17

### 目标平台

- Min SDK: 24 (Android 7.0)
- Target SDK: 35

### 依赖管理

- 使用 Gradle 版本目录 (`gradle/libs.versions.toml`)
- 始终使用目录引用: `libs.androidx.core.ktx`

## 开发流程

### 需求阶段

1. 阅读 `Rules/RulesReadMe.md` 和 `WORKSPACE.md`
2. 与用户讨论需求细节
3. 编写 PRD 文档

### 设计阶段

1. 编写 TDD 技术设计文档
2. 确认架构符合 Clean Architecture
3. 定义接口和数据模型

### 实现阶段

1. 编写 FD 功能文档（任务列表）
2. 按优先级实现用户故事
3. 每个故事独立可测试

### 验收阶段

1. 运行测试确保通过
2. 更新相关文档
3. 代码审查

## 治理

- 本宪法优先于所有其他实践
- 修改需要文档记录和批准
- 使用 `Rules/workspace-rules.md` 进行运行时开发指导

**版本**: 1.0.0 | **批准日期**: 2025-06-13 | **最后修改**: 2025-12-13
