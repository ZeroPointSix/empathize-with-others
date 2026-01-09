# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

**共情AI助手** - 基于 Android 平台的智能社交沟通辅助应用，采用 Clean Architecture + MVVM 架构模式。

### 核心原则
- **零后端 (Zero-Backend)**: 无服务器，无用户账户体系
- **BYOK**: AI能力通过用户自备API密钥直连第三方服务
- **隐私优先**: 敏感数据本地脱敏，密钥硬件级加密存储
- **无感接入**: 悬浮窗+无障碍服务与宿主App交互

### 技术栈
- Kotlin 2.0.21 + K2编译器
- Gradle 8.13 + AGP 8.7.3
- Jetpack Compose BOM 2024.12.01
- Hilt 2.52 + Room v11
- 最低SDK: 24, 目标SDK: 35

## 常用命令

```bash
# 构建与测试
./gradlew assembleDebug          # Debug构建
./gradlew assembleRelease        # Release构建
./gradlew test                   # 所有单元测试
./gradlew testDebugUnitTest      # Debug单元测试
./gradlew connectedAndroidTest   # 连接设备运行Android测试

# 单个模块测试
./gradlew :presentation:test     # presentation模块测试
./gradlew :domain:test           # domain模块测试
./gradlew :data:test             # data模块测试

# 运行单个测试类
./gradlew :domain:test --tests "com.empathy.ai.domain.usecase.SendAdvisorMessageUseCaseTest"

# Lint检查
./gradlew lint                   # 全局Lint
./gradlew :presentation:lint     # presentation模块Lint
./gradlew ktlintCheck            # Kotlin代码风格检查

# 调试脚本
scripts\logcat.bat -e            # ERROR日志
scripts\quick-error.bat          # 快速查看最近错误
scripts\ai-debug.bat             # AI请求调试

# ADB调试
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.empathy.ai/.ui.MainActivity
adb shell dumpsys activity activities | findstr ActivityRecord  # 查看Activity栈
```

## 测试覆盖

| 模块 | 单元测试 | Android测试 | 关键测试 |
|------|----------|-------------|----------|
| domain | 40+ | - | UseCase、Model、PromptBuilder |
| presentation | 36+ | 7+ | ViewModel、Compose UI |
| data | 23+ | 6+ | Repository、Database |
| app | 140+ | 26+ | Application初始化、服务测试 |

## 架构结构

```
共情AI助手 (根)
├── app/           应用入口、DI配置、Android服务
├── domain/        纯Kotlin业务层，无Android依赖
│   ├── model/     业务模型
│   ├── repository 仓库接口
│   └── usecase/   业务用例
├── data/          数据层实现
│   ├── local/     Room数据库、SharedPreferences
│   ├── remote/    Retrofit网络请求
│   └── repository 仓库实现
└── presentation/  UI层
    ├── ui/        Compose组件
    ├── viewmodel/ ViewModel
    └── navigation 导航系统
```

### 依赖方向
`app` → `data/presentation` → `domain`（严格单向依赖）

### 关键约束
- **domain层禁止引入Android依赖**（Room, Context, etc.）
- 所有API定义在domain层，data层实现
- 敏感数据处理使用 `PrivacyEngine`
- 错误处理统一使用 `Result<T>` 类型
- Hilt 模块配置：`di/` 目录下的 HiltModule

### 核心功能流程

**悬浮窗+无障碍服务交互**
```
用户选择文本 → 无障碍服务捕获 → FloatingWindowService → ChatViewModel → AI分析 → 结果悬浮窗显示
```

**每日总结流程**
```
应用启动 → 检查跨天 → SummarizeDailyConversationsUseCase → AI生成 → DailySummary保存 → 本地通知
```

## 关键系统组件

### 隐私引擎 (PrivacyEngine)
- **职责**: 敏感数据本地脱敏处理
- **位置**: `domain/src/main/kotlin/com/empathy/ai/domain/service/PrivacyEngine.kt`
- **功能**: 数据脱敏、加密存储、密钥硬件级保护

### 版本更新插件
- **插件**: `VersionUpdatePlugin`
- **配置位置**: `app/build.gradle.kts`
- **功能**: 版本更新管理、备份保留（最大50个备份，保留30天）

### 开发者模式
- **功能**: 系统提示词管理、调试工具
- **入口**: 设置页面 → 开发者选项
- **用途**: 调试 AI 响应、查看日志、管理提示词

## 模块文档

详细架构信息参考各模块文档：
- [domain/CLAUDE.md](./domain/CLAUDE.md) - 领域层详细文档
- [data/CLAUDE.md](./data/CLAUDE.md) - 数据层详细文档
- [presentation/CLAUDE.md](./presentation/CLAUDE.md) - 表现层详细文档
- [app/src/main/java/com/empathy/ai/app/CLAUDE.md](./app/src/main/java/com/empathy/ai/app/CLAUDE.md) - 应用层详细文档

## 编码规范

- 遵循 SOLID、KISS、DRY、YAGNI 原则
- 所有公共API必须有KDoc注释
- 使用 `Result<T>` 统一处理成功/失败
- 测试文件命名: `XxxTest.kt` / `XxxSpec.kt`

## 仓库镜像配置

项目使用国内镜像加速依赖下载，优先级：腾讯云 > 阿里云 > 官方仓库

## 关键命名约定

| 类型 | 前缀/后缀 | 示例 |
|------|-----------|------|
| UseCase | `XxxUseCase` | `AnalyzeChatUseCase` |
| Repository接口 | `XxxRepository` | `AiRepository` |
| Repository实现 | `XxxRepositoryImpl` | `AiRepositoryImpl` |
| ViewModel | `XxxViewModel` | `ChatViewModel` |
| Entity | `XxxEntity` | `ContactProfileEntity` |
| DTO | `XxxDto` | `ChatRequestDto` |
| 测试用例 | `XxxTest` | `SendAdvisorMessageUseCaseTest` |

## 多AI协作规则

项目使用 `Rules/workspace-rules.md` 和 `Rules/WORKSPACE.md` 管理多AI协作状态：

**任务开始前必须**：
1. 读取 `Rules/WORKSPACE.md` 检查是否有其他AI正在执行相关任务
2. 检查资源锁定状态和待处理冲突
3. 在 WORKSPACE 中记录任务开始信息

**任务执行中必须**：
- 重要里程碑实时更新到 WORKSPACE
- 发现的问题立即记录

**任务完成后必须**：
- 更新 WORKSPACE 任务状态
- 更新AI工具状态
- 添加变更日志

详细规则参考: [Rules/workspace-rules.md](./Rules/workspace-rules.md)
