# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with this repository.

## 项目概览

**共情AI助手 (Empathy AI)** - Android 隐私优先智能社交沟通辅助应用
- **架构**: Clean Architecture + MVVM + Jetpack Compose
- **语言**: Kotlin 2.0.21 (K2 Compiler)
- **版本**: v1.12.3 (versionCode: 11203, Dev)
- **模块**: `:domain` | `:data` | `:presentation` | `:app`

## 常用命令

```bash
# 构建 (Windows)
gradlew.bat assembleDebug      # Debug 构建
gradlew.bat assembleRelease    # Release 构建
gradlew.bat installDebug       # 安装到设备

# 测试
gradlew.bat test               # 所有单元测试
gradlew.bat :domain:test       # domain 模块测试
gradlew.bat :presentation:test # presentation 模块测试
gradlew.bat :data:test         # data 模块测试
gradlew.bat connectedAndroidTest # 仪器测试

# 代码质量
gradlew.bat lint               # Lint 检查
gradlew.bat ktlintCheck        # 代码风格检查

# 清理
gradlew.bat clean

# AI 调试
scripts\ai-debug.bat           # AI请求日志（简略）
scripts\ai-debug-full.bat      # 完整日志（含Prompt）
```

## 架构

```
:app/           -> Application 入口、Service、通知、应用级DI (16个模块)
:presentation/  -> Compose UI、ViewModel、Navigation、Theme (依赖 :domain)
:domain/        -> 纯 Kotlin 业务逻辑、UseCase、Repository 接口 (无 Android 依赖)
:data/          -> Room DB、Retrofit、Repository 实现、DI模块 (8个模块)
```

**依赖规则**: `app` -> `data`/`presentation` -> `domain`。`domain` 层**严禁**依赖 Android SDK。

**数据流**: UI -> ViewModel -> UseCase -> Repository (Interface) -> Repository (Impl) -> Data Source

## 关键规则

1. **状态管理**: 使用 `StateFlow` + `data class UiState`，Compose 中避免直接使用 `mutableStateOf`
2. **错误处理**: 统一使用 `Result<T>`
3. **协程**: ViewModel 用 `viewModelScope`，Compose 用 `rememberCoroutineScope`，**严禁**使用 `GlobalScope`
4. **数据库**: Room Schema 变更必须伴随 Migration 脚本 (`MIGRATION_x_y`) 和测试
5. **UI 组件**: 优先复用 `presentation/ui/component/` 下的现有组件，使用 `Ios*` 组件保持 iOS 风格一致性
6. **文档语言**: 所有文档和回答必须使用中文（代码注释保持英文）

## 导航

- 单一 Activity (`MainActivity`) + Compose Navigation
- 路由定义: `presentation/navigation/NavRoutes.kt`
- 路由图: `presentation/navigation/NavGraph.kt`
- 使用 `BottomNavScaffold` 管理 Tab 页和页面缓存
- **重要**: 注意 `popUpTo` 策略避免栈堆积，AI军师模块需使用锚点回退机制

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 构建 | Gradle | 8.13 |
| 编译 | AGP | 8.7.3 |
| 语言 | Kotlin | 2.0.21 |
| DI | Hilt | 2.52 |
| UI | Jetpack Compose | BOM 2024.12.01 |
| 数据库 | Room | 2.6.1 |
| 网络 | Retrofit + OkHttp | 2.11.0 / 4.12.0 |
| JSON | Moshi | 1.15.1 |
| 异步 | Coroutines | 1.9.0 |

**AI 服务商支持**: OpenAI、Azure OpenAI、阿里云、百度、智谱、腾讯混元、讯飞星火（7家）

## 数据库

- **版本**: Room v16（16个增量迁移脚本）
- **表**: profiles、brain_tags、ai_providers、conversation_logs、conversation_topics、daily_summaries、failed_summary_tasks、api_usage_records、ai_advisor_sessions、ai_advisor_conversations、ai_advisor_message_blocks

## 组件系统

| 层级 | 示例 | 说明 |
|------|------|------|
| 原子组件 | `IosButton`, `IosTextField` | 基础UI单元 |
| 分子组件 | `IosSearchBar`, `ModernPersonaTab` | 组合原子组件 |
| 有机体组件 | `FactStreamCard`, `EmotionTimelineView` | 复杂业务逻辑 |
| 模板组件 | `AiAdvisorChatScreen` | 页面级组合 |

## 任务协作

**强制**: 开始任务前读取 `WORKSPACE.md` 检查冲突并记录任务开始。

**多AI工具协作**:
- **Codex**: Free Explorer（功能开发、Bug修复）
- **Roo**: Review（代码审查）
- **Kiro**: Code & Debug（代码与调试）

文档位置:
- BUG 文档: `文档/开发文档/BUG/`
- 测试用例: `文档/开发文档/TE/`
- 需求文档: `文档/开发文档/PRD/`
- 规范文档: `文档/规范文档/`

## 构建优化

`gradle.properties` 已配置:
- 并行构建: `org.gradle.parallel=true`
- 构建缓存: `org.gradle.caching=true`
- JVM 内存: `-Xmx4g`
- 最大工作线程: 8
