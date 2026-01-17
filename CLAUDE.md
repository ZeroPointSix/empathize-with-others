# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with this repository.

## 项目概览

**共情AI助手 (Empathy AI)** - Android 智能社交沟通辅助应用
- **架构**: Clean Architecture + MVVM + Jetpack Compose
- **语言**: Kotlin 2.0.21 (K2 Compiler)
- **版本**: v1.11.0 (versionCode: 11100, Dev)
- **模块**: app/ | domain/ | data/ | presentation/

## 常用命令

```bash
# 构建 (Windows)
gradlew.bat assembleDebug      # Debug 构建
gradlew.bat assembleRelease    # Release 构建

# 测试
gradlew.bat test               # 所有单元测试
gradlew.bat :domain:test       # domain 模块测试
gradlew.bat :presentation:test # presentation 模块测试

# 代码质量
gradlew.bat lint               # Lint 检查
gradlew.bat ktlintCheck        # 代码风格检查

# 清理
gradlew.bat clean
```

## 架构

```
app/           -> Application 入口、Service、通知
presentation/  -> Compose UI、ViewModel、Navigation (依赖 domain)
domain/        -> 纯 Kotlin 业务逻辑、UseCase、Repository 接口
data/          -> Room DB、Retrofit、Repository 实现
buildSrc/      -> 自定义构建工具
```

**依赖规则**: `app` -> `data`/`presentation` -> `domain`。`domain` 层**严禁**依赖 Android SDK。

**数据流**: UI -> ViewModel -> UseCase -> Repository (Interface) -> Repository (Impl) -> Data Source

## 关键规则

1. **状态管理**: 使用 `StateFlow` + `data class UiState`，Compose 中避免直接使用 `mutableStateOf`
2. **错误处理**: 统一使用 `Result<T>`
3. **协程**: ViewModel 用 `viewModelScope`，Compose 用 `rememberCoroutineScope`，**严禁**使用 `GlobalScope`
4. **数据库**: Room Schema 变更必须伴随 Migration 脚本 (`MIGRATION_x_y`) 和测试
5. **UI 组件**: 优先复用 `presentation/ui/component/` 下的现有组件，使用 `Ios*` 组件保持一致性

## 导航

- 单一 Activity (`MainActivity`) + Compose Navigation
- 路由定义: `NavRoutes.kt`
- 路由图: `NavGraph.kt`
- 使用 `BottomNavScaffold` 管理 Tab 页
- 注意 `popUpTo` 策略避免栈堆积

## 调试

```bash
# AI 日志
scripts\ai-debug.bat               # 实时监听 (简略)
scripts\ai-debug-full.bat          # 完整日志 (含 Prompt)

# 系统日志
scripts\logcat.bat -e              # 仅 ERROR
scripts\quick-error.bat            # 最近 ERROR
```

## 构建优化

`gradle.properties` 已配置:
- 并行构建: `org.gradle.parallel=true`
- 构建缓存: `org.gradle.caching=true`
- JVM 内存: `-Xmx4g`

## 任务协作

**强制**: 开始任务前读取 `WORKSPACE.md` 检查冲突并记录任务开始。

文档位置:
- BUG 文档: `文档/开发文档/BUG/`
- 测试用例: `文档/开发文档/TE/`
- 需求文档: `文档/开发文档/PRD/`
