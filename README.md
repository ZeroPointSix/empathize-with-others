# 共情 AI 助手 (Empathy AI Assistant)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-2024.12.01-green.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache%202.0-orange.svg)](LICENSE)

一款隐私优先的 Android 社交沟通助手，通过 AI 技术帮助用户更好地理解和应对社交场景。

## ✨ 核心特性

- 🔒 **隐私优先**：零后端架构，所有数据本地存储
- 🔑 **自带密钥（BYOK）**：支持 OpenAI、DeepSeek 等 7 家 AI 服务商
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

### 技术栈

- **语言**：Kotlin 2.0.21 (K2 编译器)
- **构建工具**：Gradle 8.13 + Kotlin DSL
- **UI 框架**：Jetpack Compose (BOM 2024.12.01) + Material 3
- **依赖注入**：Hilt 2.52
- **数据库**：Room 2.6.1 (v16)
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
git clone https://github.com/你的用户名/empathy-ai.git
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

## 📖 文档

- [项目文档](文档/项目文档/README.md)
- [开发文档](文档/开发文档/)
- [技术栈说明](.kiro/steering/tech.md)
- [架构设计](.kiro/steering/structure.md)

## 🔀 分支合并流程 (必读)

本项目使用 `BUG-FIX` 作为主要开发分支，`main` 作为稳定分支。合并时请遵循以下流程：

### 标准合并流程

```bash
# 1. 确保在 BUG-FIX 分支
git checkout BUG-FIX

# 2. 先将 main 合并到 BUG-FIX，解决冲突
git merge origin/main

# 3. 解决冲突后提交
git add <冲突文件>
git commit -m "merge: 合并 main 分支到 BUG-FIX 并解决冲突"

# 4. 切换到 main 分支（在 main 工作树执行）
git checkout main

# 5. 拉取最新 main
git pull origin main

# 6. 合并 BUG-FIX 到 main
git merge BUG-FIX

# 7. 推送到远程（需用户确认）
git push origin main
```

### 冲突处理原则

| 文件类型 | 处理策略 |
|---------|---------|
| `gradle.properties` | 保留**更高版本号**的变更 |
| `version-history.json` | 保留**更完整版本历史**的变更 |
| 业务代码 | 根据实际功能判断，必要时人工介入 |
| 文档文件 | 保留包含更多内容的版本 |

### 常见冲突场景

1. **版本号冲突**: BUG-FIX (1.11.0) vs main (1.4.0) → 保留 BUG-FIX
2. **版本历史冲突**: 保留版本号更高的完整历史
3. **steering 文件冲突**: 保留包含最新决策的版本

### 注意事项

- ⚠️ **禁止**直接在 main 分支上开发
- ⚠️ **禁止**跳过在 BUG-FIX 合并 main 的步骤
- ✅ 每次合并前确保本地 main 是最新的
- ✅ 冲突解决后先在本地测试通过

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

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
