# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概览

**共情AI助手 (Empathy AI)** - Android 隐私优先智能社交沟通辅助应用
- **架构**: Clean Architecture + MVVM + Jetpack Compose
- **语言**: Kotlin 2.0.21 (K2 Compiler)
- **版本**: v1.14.17 (versionCode: 11417, dev)
- **模块**: `:domain` | `:data` | `:presentation` | `:app`
- **平台**: Android (minSdk 24, targetSdk 35)
- **当前分支**: main

## 常用命令

```bash
# 构建 (Windows)
gradlew.bat assembleDebug

gradlew.bat assembleRelease

gradlew.bat installDebug

# 测试
gradlew.bat test

gradlew.bat :domain:test

gradlew.bat :data:test

gradlew.bat :presentation:test

gradlew.bat :presentation:testDebugUnitTest --tests "*BUG00058*"

gradlew.bat connectedAndroidTest

# 清理
gradlew.bat clean
```

## 架构

```
:domain/        -> 纯 Kotlin 业务逻辑、UseCase、Repository 接口 (无 Android 依赖)
:data/          -> Room DB、Retrofit、Repository 实现、DI
:presentation/  -> Compose UI、ViewModel、Navigation、Theme (依赖 :domain)
:app/           -> Application 入口、Service、通知、应用级 DI
```

**依赖规则**: `app` -> `data`/`presentation` -> `domain`。`domain` 层**严禁**依赖 Android SDK。

**数据流**: UI -> ViewModel -> UseCase -> Repository (Interface) -> Repository (Impl) -> Data Source

## 关键规则

1. **状态管理**: 使用 `StateFlow` + `data class UiState`
2. **错误处理**: 统一使用 `Result<T>`
3. **协程**: ViewModel 用 `viewModelScope`，Compose 用 `rememberCoroutineScope`，**严禁** `GlobalScope`
4. **数据库**: Room Schema 变更必须伴随 Migration 脚本 (`MIGRATION_x_y`) 和测试
   - 迁移测试集中在 `app/src/androidTest-disabled`
5. **UI 组件**: 优先复用 `presentation/ui/component/` 下的组件，保持 iOS 风格一致性
6. **文档语言**: 所有文档和回答必须使用中文（代码注释保持英文）

## 导航

- 单一 Activity (`MainActivity`) + Compose Navigation
- 路由定义: `presentation/navigation/NavRoutes.kt`
- 路由图: `presentation/navigation/NavGraph.kt`
- 底部 Tab: CONTACT_LIST / AI_ADVISOR / SETTINGS

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

## AI 服务商预设

- OpenAI GPT-4 / GPT-3.5
- Google Gemini Pro
- DeepSeek
- 自定义 OpenAI 兼容服务商

## 数据库

- **版本**: Room v17（迁移 1→17）
- **位置**: `data/src/main/kotlin/com/empathy/ai/data/local/AppDatabase.kt`
- **迁移**: `data/src/main/kotlin/com/empathy/ai/data/di/DatabaseModule.kt`
- **核心表**:
  - `profiles` - 联系人画像
  - `brain_tags` - 大脑标签
  - `ai_providers` - AI 服务商配置
  - `conversation_logs` - 对话记录
  - `conversation_topics` - 对话主题
  - `daily_summaries` - 每日总结
  - `failed_summary_tasks` - 失败任务
  - `api_usage_records` - 用量记录
  - `ai_advisor_sessions/conversations/message_blocks` - AI 军师会话

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
- **Multi-Agent Explorer**: 独立工作树并行开发

**工作树管理**:
```bash
# 创建新工作树
git worktree add ../feature-branch main

# 列出所有工作树
git worktree list

# 清理已合并的工作树
git worktree prune
```

文档位置:
- BUG 文档: `文档/开发文档/BUG/`
- 测试用例: `文档/开发文档/TE/`
- 需求文档: `文档/开发文档/PRD/`
- 规范文档: `文档/规范文档/`

## 构建优化

`gradle.properties` 已配置:
- 并行构建: `org.gradle.parallel=true`
- 构建缓存: `org.gradle.caching=true`
- JVM 内存: `-Xmx4g -Xms1g`
- 最大工作线程: `org.gradle.workers.max=8`
- Kotlin 增量编译: 已启用

## 重要文件位置

- **构建配置**: `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
- **版本管理**: `config/version-history.json`, `buildSrc/src/main/kotlin/com/empathy/ai/build/VersionUpdatePlugin.kt`
- **依赖声明**: `gradle/libs.versions.toml`
- **导航系统**: `presentation/navigation/NavGraph.kt`, `presentation/navigation/NavRoutes.kt`
- **DI 配置**: `data/src/main/kotlin/com/empathy/ai/data/di/` + `app/src/main/java/com/empathy/ai/di/`
- **数据库**: `data/src/main/kotlin/com/empathy/ai/data/local/AppDatabase.kt`
- **AI 军师模块**: `presentation/ui/screen/advisor/`, `domain/model/AiAdvisor*.kt`
- **提示词场景**: `domain/model/PromptScene.kt`
- **服务商预设**: `domain/model/ProviderPresets.kt`
- **悬浮窗服务**: `app/src/main/java/com/empathy/ai/service/FloatingWindowService.kt`
- **工作空间**: `WORKSPACE.md`
- **决策日志**: `DECISION_JOURNAL.md`
