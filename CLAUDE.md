# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with this repository.

## 项目概览

**共情AI助手 (Empathy AI)** - Android 智能社交沟通辅助应用
- **架构**: Clean Architecture + MVVM + Jetpack Compose
- **语言**: Kotlin 2.0.21 (K2 Compiler)
- **版本**: v1.12.1 (versionCode: 11201, Dev)
- **模块**: app/ | domain/ | data/ | presentation/
- **当前任务**: Contact Search History Feature (2026-01-18)

## 核心特性

- 🔒 **隐私优先**: 零后端架构，所有数据本地存储
- 🔑 **BYOK模式**: 支持7+ AI服务商 (OpenAI, DeepSeek等)
- 🎯 **本地优先**: 高频操作使用本地规则实现零延迟
- 🏗️ **严格多模块**: 纯Kotlin domain层，无Android依赖
- 🎨 **Material Design 3**: 现代化Jetpack Compose UI

## 技术栈

- **构建工具**: Gradle 8.13 + Kotlin DSL
- **UI框架**: Jetpack Compose (BOM 2024.12.01) + Material 3
- **依赖注入**: Hilt 2.52
- **数据库**: Room 2.6.1 (Schema v16)
- **网络**: Retrofit 2.11.0 + OkHttp 4.12.0
- **异步**: Kotlin Coroutines 1.9.0 + Flow

## 模块架构

```
app/              -> Application入口、Android服务、前台服务、悬浮窗
presentation/     -> Compose UI、ViewModel、Navigation (依赖domain)
data/             -> Room DB、Retrofit、Repository实现、Hilt模块
domain/           -> 纯Kotlin业务逻辑、UseCase、Repository接口
buildSrc/         -> 自定义构建工具 (版本管理、图标管理)
```

**依赖规则**: `app` → `data`/`presentation` → `domain`。**domain层严禁依赖Android SDK**。

**数据流**: UI → ViewModel → UseCase → Repository (Interface) → Repository (Impl) → Data Source

## 常用命令

### 构建
```bash
# Windows
gradlew.bat assembleDebug      # Debug构建
gradlew.bat assembleRelease    # Release构建
gradlew.bat installDebug       # 安装到设备

# macOS/Linux
./gradlew assembleDebug
./gradlew installDebug
```

### 测试
```bash
gradlew.bat test               # 所有单元测试
gradlew.bat :domain:test       # domain模块测试 (纯Kotlin，最快)
gradlew.bat :data:test         # data模块测试
gradlew.bat :presentation:test # presentation模块测试

# 运行特定测试
gradlew.bat test --tests "*ContactListViewModel*"
gradlew.bat :domain:test --tests "*UseCase*"
gradlew.bat :presentation:test --tests "*ContactSearch*"

# Android集成测试 (需要连接设备)
gradlew.bat connectedAndroidTest
```

### 代码质量
```bash
gradlew.bat lint               # Android Lint检查
gradlew.bat ktlintCheck        # Kotlin代码风格检查
gradlew.bat ktlintFormat       # 自动格式化代码
```

### 调试与日志
```bash
# AI请求日志 (过滤显示Temperature、MaxTokens等参数)
gradlew.bat ai-debug            # 实时监听AI日志
gradlew.bat ai-debug -h         # 获取最近100条AI日志
gradlew.bat ai-debug -h -n 200  # 获取最近200条AI日志

# 完整AI日志 (包含提示词内容)
gradlew.bat ai-debug-full

# 系统日志
gradlew.bat logcat -e          # 仅ERROR级别
gradlew.bat quick-error        # 获取最近的ERROR日志
```

### 清理与维护
```bash
gradlew.bat clean              # 清理构建缓存
gradlew.bat cleanBuildCache    # 清理构建缓存
gradlew.bat --stop             # 停止Gradle Daemon
```

## 关键开发规则

### 1. 状态管理
- 使用 `StateFlow` + `data class UiState`
- Compose中避免直接使用 `mutableStateOf`
- ViewModel中用 `viewModelScope`，Compose中用 `rememberCoroutineScope`
- **严禁使用 GlobalScope**

### 2. 错误处理
- 统一使用 `Result<T>` 类型
- Repository层返回Result
- ViewModel中处理Result并更新UiState

### 3. 数据库 (Room)
- Schema版本: 当前v16
- **Schema变更必须伴随Migration脚本** (`MIGRATION_x_y`)
- 必须添加Migration测试
- 更新 `gradle.properties` 中的版本号

### 4. UI组件
- 优先复用 `presentation/ui/component/` 下的现有组件
- 使用 `Ios*` 组件保持iOS风格一致性
- 遵循Material Design 3规范

### 5. Clean Architecture
- domain层为纯Kotlin，无Android依赖
- 数据流: UI → ViewModel → UseCase → Repository
- 依赖倒置: presentation/data依赖domain接口

## 导航架构

- **单一Activity**: `MainActivity` + Compose Navigation
- **路由定义**: `presentation/navigation/NavRoutes.kt`
- **路由图**: `presentation/navigation/NavGraph.kt`
- **Tab导航**: 使用 `BottomNavScaffold` 管理
- **返回栈**: 注意 `popUpTo` 策略避免栈堆积

主要路由:
- `contact_list` - 联系人列表
- `chat` - 聊天界面
- `ai_advisor` - AI军师
- `brain_tag` - 大脑标签
- `settings` - 设置

## 自定义构建工具 (buildSrc)

项目使用 `buildSrc` 模块提供自动化构建功能:

### 版本管理
- **自动版本更新**: `UpdateVersionAndIconTask`
- **语义化版本**: `SemanticVersion` (major.minor.patch)
- **发布阶段**: dev/test/beta/production
- **配置位置**: `gradle.properties`

### 图标管理
- **多版本图标**: 根据发布阶段切换不同图标
- **InitIconResourcesTask**: 初始化图标资源
- **IconManager**: 图标切换逻辑

### 版本配置示例 (gradle.properties)
```properties
APP_VERSION_NAME=1.12.1          # 语义化版本
APP_VERSION_CODE=11201            # versionCode (major*10000 + minor*100 + patch)
APP_RELEASE_STAGE=dev             # 发布阶段
```

## 构建优化配置

`gradle.properties` 已针对高性能开发优化:

```properties
# JVM优化 (24GB内存)
org.gradle.jvmargs=-Xmx4g -Xms1g -XX:+UseG1GC

# 并行构建
org.gradle.parallel=true
org.gradle.workers.max=8

# 构建缓存
org.gradle.caching=true
org.gradle.configureondemand=true

# Kotlin优化
kotlin.incremental=true
kotlin.incremental.android=true

# KAPT优化
kapt.incremental.apt=true
kapt.use.worker.api=true

# Hilt多模块
android.enableAggregatingTask=false
```

## 当前开发任务 (2026-01-18)

**进行中**: Contact Search History Feature
- 新增联系人搜索历史功能
- 相关文件:
  - `data/src/main/kotlin/com/empathy/ai/data/local/ContactSearchHistoryPreferences.kt`
  - `domain/src/main/kotlin/com/empathy/ai/domain/repository/ContactSearchHistoryRepository.kt`
  - `domain/src/main/kotlin/com/empathy/ai/domain/usecase/*ContactSearchHistory*.kt`
  - `presentation/src/test/kotlin/com/empathy/ai/presentation/viewmodel/ContactSearchHistoryFeatureTest.kt`

## 测试策略

### 单元测试
- **domain层**: 纯Kotlin，无Android依赖，测试速度最快
- **data层**: 包含Repository实现测试
- **presentation层**: ViewModel和UI逻辑测试

### 集成测试 (androidTest)
- **数据库迁移测试**: Room Schema变更必须测试
- **UI测试**: Compose UI交互测试
- **位置**: `src/androidTest/`

### 测试统计
- 总测试文件: 373个
- domain: 40个测试文件
- data: 23个测试文件
- presentation: 大量UI测试

## 数据库Schema

**当前版本**: v16
**主要表**:
- `ContactProfileEntity` - 联系人
- `BrainTagEntity` - 大脑标签
- `AiProviderEntity` - AI服务商
- `ConversationLogEntity` - 对话记录
- `AiAdvisorSessionEntity` - AI军师会话 (TD-00026)
- `AiAdvisorConversationEntity` - AI军师对话 (TD-00026)

**迁移目录**: `app/schemas/` 和 `data/schemas/`

## 项目文档

**强制**: 开始任务前读取 `WORKSPACE.md` 检查冲突并记录任务开始。

文档位置:
- **工作空间**: `WORKSPACE.md` - 当前任务状态
- **BUG文档**: `文档/开发文档/BUG/`
- **测试用例**: `文档/开发文档/TE/`
- **需求文档**: `文档/开发文档/PRD/`
- **决策日志**: `DECISION_JOURNAL.md`
- **模块文档**: 各模块 `CLAUDE.md` (domain/, data/, presentation/)

## 开发工作流

1. **任务开始**:
   - 读取 `WORKSPACE.md` 确认任务状态
   - 记录任务开始时间和负责人
   - 检查是否有冲突

2. **代码开发**:
   - 遵循Clean Architecture原则
   - 编写单元测试
   - 运行测试: `gradlew.bat test`

3. **提交前检查**:
   - `gradlew.bat ktlintCheck`
   - `gradlew.bat lint`
   - `gradlew.bat test`

4. **任务完成**:
   - 更新 `WORKSPACE.md`
   - 添加变更记录
   - 更新相关文档

## 常见问题

### Q: 如何添加新的Repository实现？
A:
1. 在 `domain/` 定义Repository接口
2. 在 `data/` 实现Repository接口
3. 在 `data/di/RepositoryModule.kt` 绑定Hilt
4. 编写对应测试

### Q: Room Schema如何升级？
A:
1. 增加Entity版本号
2. 创建 `MIGRATION_x_y` 脚本
3. 在 `AppDatabase` 添加迁移
4. 编写Migration测试
5. 更新 `gradle.properties` 版本号

### Q: 如何添加新的UseCase？
A:
1. 在 `domain/usecase/` 创建UseCase类
2. 注入必要的Repository
3. 返回 `Result<T>` 类型
4. 编写单元测试
5. 在ViewModel中使用

## 重要文件路径

### 核心配置
- `gradle.properties` - 版本和构建配置
- `settings.gradle.kts` - 模块和仓库配置
- `build.gradle.kts` - 根项目构建脚本

### 架构核心
- `presentation/navigation/NavRoutes.kt` - 路由定义
- `presentation/navigation/NavGraph.kt` - 导航图
- `app/src/main/java/com/empathy/ai/MainActivity.kt` - 主Activity
- `data/src/main/kotlin/com/empathy/ai/data/local/AppDatabase.kt` - Room数据库

### 自定义构建工具
- `buildSrc/src/main/kotlin/com/empathy/ai/build/` - 构建工具源码
- `config/version-history.json` - 版本历史

## 版本历史

- **v1.12.1** (2026-01-18) - Contact Search History Feature
- **v1.12.0** (2026-01-01) - Multi-Agent Explorer升级
- **v1.11.0** (2025-12-31) - 图标和版本号自动更新
- **v1.10.0** (2025-12-27) - Clean Architecture改造完成

---

**重要提醒**:
- domain层为纯Kotlin，严禁引入Android依赖
- 所有数据库变更必须包含Migration
- 测试是开发的一部分，不是可选项
- 遵循既定的架构模式和数据流
