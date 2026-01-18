# 共情 AI 助手 (Empathy AI Assistant)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-2024.12.01-green.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache%202.0-orange.svg)](LICENSE)

[English Documentation](README.md)

---

## ⚠️ 重要声明

### 免责声明

**本项目为个人学习项目。** 开发者不对本应用程序提供任何保证或担保。使用风险自负。

- ❌ **无担保**：本软件按"原样"提供，不提供任何形式的保证
- ❌ **无责任**：开发者不对使用本应用程序产生的任何问题、损害或后果承担责任
- ⚠️ **可能存在缺陷**：本项目可能包含 Bug、安全漏洞或其他缺陷
- 📚 **学习目的**：这是一个用于学习 Android 开发的练手项目

### 隐私与数据安全

**关于数据处理的重要信息：**

- 🏠 **纯本地架构**：本应用完全在您的设备上运行，没有后端服务器
- 💾 **本地数据存储**：所有数据（聊天记录、联系人信息、标签等）均存储在您的设备本地
- 🔐 **您的责任**：您需要负责保护您的设备和存储在设备上的数据
- 🌐 **API 通信**：仅在您向第三方 AI API（OpenAI、DeepSeek 等）发送请求时才会传输数据
- ⚠️ **敏感信息**：本应用可能会处理包括聊天消息和联系人详情在内的敏感信息。请谨慎使用，并确保您有适当的权限处理此类数据

**使用本应用程序即表示您确认：**
- 您理解这是一个实验性的学习项目
- 您接受与使用相关的所有风险
- 您有责任遵守适用的隐私法律法规
- 您不会因任何问题或损害追究开发者的责任

---

一款隐私优先的 Android 社交沟通助手，通过 AI 技术帮助用户更好地理解和应对社交场景。

## ✨ 核心特性

- 🔒 **隐私优先**：零后端架构，所有数据本地存储
- 🔑 **自带密钥（BYOK）**：支持 OpenAI、DeepSeek 等 7+ 家 AI 服务商
- 🎯 **本地优先**：高频操作使用本地规则，零延迟响应
- 🏗️ **Clean Architecture**：严格的多模块架构，domain 层纯 Kotlin
- 🎨 **Material Design 3**：现代化的 Jetpack Compose UI

## 🏛️ 架构设计

项目采用 Clean Architecture + MVVM 多模块架构：

```
:domain/        # 纯 Kotlin - 业务模型、UseCase、Repository 接口
:data/          # Android Library - Room、Retrofit、Repository 实现
:presentation/  # Android Library - Compose UI、ViewModel
:app/           # Application - 应用入口、Android 服务
```

### 依赖规则

- `app` 依赖 `data` 和 `presentation`
- `data` 和 `presentation` 依赖 `domain`
- `domain` 层是**纯 Kotlin**，**严禁**依赖 Android SDK

### 数据流

```
UI → ViewModel → UseCase → Repository (Interface) → Repository (Impl) → Data Source
```

### 技术栈

- **语言**：Kotlin 2.0.21 (K2 编译器)
- **构建工具**：Gradle 8.13 + Kotlin DSL
- **UI 框架**：Jetpack Compose (BOM 2024.12.01) + Material 3
- **依赖注入**：Hilt 2.52
- **数据库**：Room 2.6.1 (Schema v16)
- **网络**：Retrofit 2.11.0 + OkHttp 4.12.0
- **异步**：Kotlin Coroutines 1.9.0 + Flow

## 🚀 快速开始

### 环境要求

- Android Studio Ladybug | 2024.2.1 或更高版本
- JDK 17
- Android SDK 34
- Gradle 8.13

### 配置步骤

1. **克隆项目**
```bash
git clone https://github.com/yourusername/empathy-ai.git
cd empathy-ai
```

2. **配置 SDK 路径**
```bash
# 复制示例配置
cp local.properties.example local.properties

# 编辑 local.properties，设置你的 Android SDK 路径
# Windows: sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
# macOS/Linux: sdk.dir=/Users/YourUsername/Library/Android/sdk
```

3. **构建项目**
```bash
# Windows
.\gradlew assembleDebug

# macOS/Linux
./gradlew assembleDebug
```

4. **运行应用**
```bash
# 安装到设备
.\gradlew installDebug

# 或在 Android Studio 中直接运行
```

### 配置 AI 服务商

应用首次运行时，需要配置 AI 服务商：

1. 进入「设置」→「AI 配置」
2. 点击「添加服务商」
3. 选择服务商类型（OpenAI、DeepSeek 等）
4. 填入你的 API 密钥
5. 设置为默认服务商

## 📱 主要功能

- **AI 军师**：分析聊天上下文，提供沟通建议
- **联系人画像**：智能记录和分析联系人信息
- **标签系统**：雷区标签和策略标签管理
- **悬浮窗服务**：系统级悬浮窗快速访问
- **隐私保护**：数据脱敏引擎，保护敏感信息
- **提示词管理**：自定义 AI 提示词模板

## 🧪 测试

```bash
# 运行所有单元测试
.\gradlew test

# 运行特定模块测试
.\gradlew :domain:test
.\gradlew :data:test
.\gradlew :presentation:test

# 运行 Android 测试（需要连接设备）
.\gradlew connectedAndroidTest
```

## 🤝 参与贡献

我们欢迎各种形式的贡献！以下是参与方式：

### 开始贡献

1. **Fork 本仓库**并克隆到本地
2. **创建功能分支**（从 `main` 分支）
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **阅读文档**
   - [CLAUDE.md](CLAUDE.md) - 项目概览和开发指南
   - [WORKSPACE.md](WORKSPACE.md) - 任务协调和冲突预防

### 开发规范

1. **代码风格**
   - 遵循 Kotlin 编码规范
   - 使用有意义的变量和函数名
   - 保持函数简洁专注
   - 提交前运行 `.\gradlew ktlintCheck`

2. **架构规则**
   - 遵守模块边界（参见架构部分）
   - Domain 层必须保持纯 Kotlin（不依赖 Android）
   - 使用 `StateFlow` 进行状态管理
   - 使用 `Result<T>` 进行错误处理
   - ViewModel 中使用 `viewModelScope`，禁止使用 `GlobalScope`

3. **数据库变更**
   - Room Schema 变更必须包含迁移脚本
   - 添加迁移测试
   - 更新 `gradle.properties` 中的 schema 版本

4. **测试**
   - 为业务逻辑编写单元测试
   - 关键路径需要测试覆盖
   - 提交 PR 前运行测试

5. **提交信息**
   - 使用约定式提交格式：`type(scope): description`
   - 类型：`feat`、`fix`、`docs`、`style`、`refactor`、`test`、`chore`
   - 示例：`feat(domain): add contact profile use case`

### 提交变更

1. **确保所有测试通过**
   ```bash
   .\gradlew test
   .\gradlew lint
   ```

2. **提交你的更改**
   ```bash
   git add .
   git commit -m "feat(scope): your feature description"
   ```

3. **推送到你的 Fork**
   ```bash
   git push origin feature/your-feature-name
   ```

4. **创建 Pull Request**
   - 提供清晰的变更描述
   - 引用相关的 Issue
   - 确保 CI 检查通过

### 贡献方向

- 🐛 Bug 修复
- ✨ 新功能（查看 Issues 中的功能请求）
- 📝 文档改进
- 🌐 翻译
- 🧪 测试覆盖率提升
- ♿ 无障碍功能增强

### 代码审查流程

- 所有提交都需要审查
- 维护者会提供反馈
- 处理审查意见
- 审查通过后，你的 PR 将被合并

## 📖 文档

- [项目文档](文档/项目文档/README.md)
- [开发文档](文档/开发文档/)
- [技术栈说明](.kiro/steering/tech.md)
- [架构设计](.kiro/steering/structure.md)

## 🐛 报告问题

发现 Bug？请创建 Issue 并包含：
- 问题的清晰描述
- 复现步骤
- 期望行为 vs 实际行为
- 设备/操作系统信息
- 相关日志或截图

## 📄 开源协议

本项目采用 [Apache License 2.0](LICENSE) 开源协议。

## 🙏 致谢

感谢所有开源项目的贡献者，特别是：

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Hilt](https://dagger.dev/hilt/)
- [Room](https://developer.android.com/training/data-storage/room)

---

**注意**：本项目仅供学习和研究使用，请遵守相关 AI 服务商的使用条款。
