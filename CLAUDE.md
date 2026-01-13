# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

**共情AI助手** - 基于 Android 平台的智能社交沟通辅助应用，采用 Clean Architecture + MVVM 架构模式。

**当前分支**: BUG63-qieping（导航副作用可见性保护优化分支）
**当前版本**: v1.1.0 (versionCode: 10100, dev阶段)
**数据库版本**: Room v16

### 核心原则
- **零后端 (Zero-Backend)**: 无服务器，无用户账户体系
- **BYOK**: AI能力通过用户自备API密钥直连第三方服务
- **隐私优先**: 敏感数据本地脱敏，密钥硬件级加密存储
- **无感接入**: 悬浮窗+无障碍服务与宿主App交互

### 技术栈
- Kotlin 2.0.21 + K2编译器
- Gradle 8.13 + AGP 8.7.3
- Jetpack Compose BOM 2024.12.01
- Hilt 2.52 + Room v16
- 最低SDK: 24, 目标SDK: 35

## 常用命令

**注意**: Windows环境下使用 `gradlew.bat` 代替 `./gradlew`

### 构建与测试
```bash
# 基础构建
gradlew.bat assembleDebug          # Debug构建
gradlew.bat assembleRelease        # Release构建
gradlew.bat clean                  # 清理构建缓存

# 单元测试
gradlew.bat test                   # 所有单元测试
gradlew.bat testDebugUnitTest      # Debug单元测试
gradlew.bat connectedAndroidTest   # 连接设备运行Android测试

# 模块级测试
gradlew.bat :presentation:test     # presentation模块测试
gradlew.bat :domain:test           # domain模块测试
gradlew.bat :data:test             # data模块测试

# 运行单个测试类
gradlew.bat :domain:test --tests "com.empathy.ai.domain.usecase.SendAdvisorMessageUseCaseTest"

# 运行特定Bug测试
gradlew.bat :presentation:test --tests "*BUG00058*"
gradlew.bat :presentation:test --tests "*SessionManagement*"
gradlew.bat :presentation:test --tests "*BUG00068*"

# Lint检查
gradlew.bat lint                   # 全局Lint
gradlew.bat :presentation:lint     # presentation模块Lint
gradlew.bat ktlintCheck            # Kotlin代码风格检查
```

### 调试脚本（Windows）
```bash
# Logcat 日志调试
scripts\logcat.bat                 # 显示WARN及以上（默认）
scripts\logcat.bat -e              # 只看ERROR级别
scripts\logcat.bat -c -e           # 清空日志后只看ERROR
scripts\logcat.bat -f -e           # ERROR日志保存到文件
scripts\logcat.bat -crash          # 只看崩溃日志
scripts\quick-error.bat            # 获取最近的ERROR日志（一次性）

# AI 请求调试（显示 Temperature、MaxTokens 等关键参数）
scripts\ai-debug.bat               # 实时监听AI日志
scripts\ai-debug.bat -h            # 获取最近100条AI日志
scripts\ai-debug.bat -h -n 200     # 获取最近200条AI日志
scripts\ai-debug.bat -d 127.0.0.1:7555  # 指定MuMu模拟器
scripts\ai-debug.bat -f ai_log.txt # 输出到文件

# 完整AI日志（包含提示词内容）
scripts\ai-debug-full.bat          # 获取完整AI请求日志
scripts\ai-debug-full.bat 127.0.0.1:7555  # 指定设备

# 快捷脚本
scripts\quick-build.bat            # 快速构建（跳过lint和测试）
scripts\quick-test.bat             # 运行所有单元测试
scripts\dev-cycle.bat              # 开发循环（构建+测试+安装）
```

### ADB 调试
```bash
# 设备管理
adb devices -l                     # 列出所有连接设备
adb -s 127.0.0.1:7555 install ...   # 向指定设备安装（MuMu模拟器）
adb -s emulator-5556 install ...    # 向指定模拟器安装

# 应用安装与启动
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.empathy.ai/.ui.MainActivity

# 日志与调试
adb logcat -c && adb logcat *:E   # 清除日志并只显示ERROR级别
adb shell dumpsys activity activities | findstr ActivityRecord  # 查看Activity栈（Windows）
```

## 测试覆盖（基于实际代码架构扫描 - 2026-01-11最新）

| 模块 | 单元测试 | Android测试 | 总计 | 关键测试 |
|------|----------|-------------|------|----------|
| domain | 27 | 0 | 27 | UseCase、Model、PromptBuilder |
| presentation | 62 | 7 | 69 | ViewModel、Compose UI、Bug回归测试 |
| data | 25 | 6 | 31 | Repository、Database |
| app | 141 | 8 | 149 | Application初始化、服务测试 |
| **总计** | **255** | **21** | **276** | - |

**Bug回归测试**:
- `BUG00058CreateNewSessionTest.kt` - 新建会话功能测试
- `BUG00059RegenerateMessageRoleTest.kt` - 消息重新生成角色测试
- `BUG00060SessionManagementTest.kt` - 会话管理增强测试
- `BUG00061SessionHistoryNavigationTest.kt` - 会话历史导航测试
- `BUG00063VisibilityGateTest.kt` - 导航副作用可见性保护测试
- `BUG00063ContactSearchTest.kt` - 联系人列表搜索功能测试
- `BUG00064ManualSummaryTest.kt` - AI手动总结功能测试
- `BUG00066EditBrainTagTest.kt` - 大脑标签编辑功能测试
- `BUG00068NavigationStackTest.kt` - 导航栈治理与返回语义测试
- `BUG00068AiAdvisorEntryRefreshTest.kt` - AI军师入口刷新测试

**测试注意事项**:
- 部分 androidTest 已隔离到 `app/src/androidTest-disabled/` 目录
- 迁移测试需要历史 schema 文件（位于 `data/schemas/`）
- 当前存在 27 个既有用例失败（与导航改动无直接关联）

## 架构结构

```
共情AI助手 (根)
├── app/              应用入口、DI配置、Android服务
├── build/            自定义Gradle插件
├── domain/           纯Kotlin业务层，无Android依赖
│   ├── model/        业务模型
│   ├── repository    仓库接口
│   ├── usecase/      业务用例
│   ├── service/      领域服务
│   └── util/         工具类
├── data/             数据层实现
│   ├── local/        Room数据库、SharedPreferences
│   ├── remote/       Retrofit网络请求
│   ├── repository/   仓库实现
│   └── parser/       AI响应解析器
├── presentation/     UI层
│   ├── ui/           Compose组件
│   ├── viewmodel/    ViewModel
│   ├── navigation/   导航系统
│   └── theme/        主题配置
└── Rules/            多AI协作规则
```

### 依赖方向
`app` → `data/presentation` → `domain`（严格单向依赖）

### 关键约束
- **domain层禁止引入Android依赖**（Room, Context, etc.）
- 所有API定义在domain层，data层实现
- 敏感数据处理使用 `PrivacyEngine`
- 错误处理统一使用 `Result<T>` 类型
- Hilt 模块配置：`di/` 目录下的 HiltModule

### 注解处理器配置
| 模块 | 处理器 | 用途 |
|------|--------|------|
| data | KSP | Room 数据库注解处理 |
| data | Kapt | Retrofit/Moshi 注解处理 |
| app/presentation | Kapt | Hilt 依赖注入 |

### Room 数据库版本
- **当前版本**: v16
- **迁移路径**: `data/src/main/kotlin/com/empathy/ai/data/local/MIGRATION_*.kt`
- **Schema目录**: `data/schemas/com.empathy.ai.data.local.AppDatabase/`
- **迁移策略**: 每次架构变更添加增量迁移脚本
- **版本查看**: 检查 `AppDatabase.kt` 中的 `@Database(version = 16)` 注解
- **迁移历史**:
  - v15→v16: 添加 `is_pinned` 字段（会话置顶功能，BUG-00060）

### Hilt 模块索引（基于实际代码扫描 - 2026-01-11最新）

**Data模块（8个DI模块）**：
| 模块 | 职责 |
|------|------|
| `DatabaseModule.kt` | Room数据库v16、16个迁移脚本、11个DAO |
| `NetworkModule.kt` | Retrofit、OkHttp、Moshi、SSE流式读取 |
| `RepositoryModule.kt` | 18个Repository接口绑定 |
| `MemoryModule.kt` | 内存缓存依赖 |
| `PromptModule.kt` | 提示词系统依赖 |
| `DispatcherModule.kt` | 协程调度器 |
| `OkHttpClientFactory.kt` | OkHttp工厂（代理支持） |
| `Qualifiers.kt` | 限定符定义 |

**App模块（16个DI模块）**：
| 模块 | 职责 |
|------|------|
| `LoggerModule.kt` | Logger接口绑定 |
| `AppDispatcherModule.kt` | 应用级协程调度器 |
| `ServiceModule.kt` | Android服务注入 |
| `FloatingWindowModule.kt` | 悬浮窗组件 |
| `NotificationModule.kt` | 通知系统 |
| `SummaryModule.kt` | 总结功能 |
| `EditModule.kt` | 编辑功能 |
| `PersonaModule.kt` | 人设功能 |
| `TopicModule.kt` | 主题功能 |
| `UserProfileModule.kt` | 用户画像功能 |
| `AiAdvisorModule.kt` | AI军师模块 |
| `ProxyModule.kt` | 代理配置 |
| `ApiUsageModule.kt` | API用量统计 |
| `SystemPromptModule.kt` | 系统提示词 |
| `FloatingWindowManagerModule.kt` | 悬浮窗管理器 |

### 导航系统结构（2026-01-11更新）
- **NavGraph.kt** - 主导航图，定义所有导航路由（23个路由）
- **NavRoutes.kt** - 路由常量定义
- **BottomNavTab.kt** - 底部导航标签枚举（CONTACTS, AI_ADVISOR, SETTINGS）
- **NonTabNavGraph.kt** - 非Tab页面导航容器，用于承载详情页等非Tab路由
- **BottomNavScaffold.kt** - 底部导航栏组件，实现Tab页面内存缓存
- **MainScreen.kt** - 主屏幕容器，管理底部导航和内容区域
- **MainActivity.kt** - 单一Activity，共享NavController管理所有导航
- **PromptEditorNavigation.kt** - 提示词编辑器导航配置

**导航路由清单（23个）**：
- CONTACT_LIST - 联系人列表（首页）
- CONTACT_DETAIL_TAB/{id} - 联系人详情标签页
- CREATE_CONTACT - 新建联系人
- CHAT/{id} - AI分析界面
- BRAIN_TAG - 标签管理
- AI_ADVISOR - AI军师入口
- AI_ADVISOR_CHAT/{id} - AI军师对话
- AI_ADVISOR_SESSIONS/{id} - AI军师会话历史
- AI_ADVISOR_CONTACTS - AI军师联系人选择
- SETTINGS - 设置页面
- AI_CONFIG - AI配置
- ADD_PROVIDER - 添加服务商
- EDIT_PROVIDER/{id} - 编辑服务商
- USAGE_STATS - 用量统计
- USER_PROFILE - 用户画像
- SYSTEM_PROMPT_LIST - 系统提示词列表
- SYSTEM_PROMPT_EDIT/{scene} - 系统提示词编辑

**导航架构关键点**:
- 使用 BottomNavScaffold 实现 Tab 页面内存缓存，避免切换时重建
- NonTabNavGraph 始终挂载承载非 Tab 页面
- 所有页面共享同一个 NavController
- 隐藏 Tab 通过 alpha 和 pointerInput 阻止交互，但不卸载

**导航副作用可见性保护（BUG-00063修复）**:
- 隐藏 Tab 不触发导航或跨 Activity 跳转副作用
- AiAdvisorScreen 和 SettingsScreen 的导航副作用增加可见性门控
- 页面从可见切换为不可见时清理待导航状态

**导航栈治理（PRD-00035/BUG-00068）**:
- AI军师子页面使用 `popUpTo` 清理返回栈，防止栈堆积
- AI军师联系人切换以 `CONTACT_LIST` 为稳定锚点
- 入口跳转增加 `launchSingleTop` 防止重复入栈
- 设置页面导航增加 `launchSingleTop` 避免重复入栈

**悬浮窗+无障碍服务交互**
```
用户选择文本 → 无障碍服务捕获 → FloatingWindowService → ChatViewModel → AI分析 → 结果悬浮窗显示
```
- **悬浮窗服务**: `app/src/main/java/com/empathy/ai/domain/service/FloatingWindowService.kt`
- **无障碍服务**: 捕获用户选中的文本并传递给悬浮窗服务

**每日总结流程**
```
应用启动 → 检查跨天 → SummarizeDailyConversationsUseCase → AI生成 → DailySummary保存 → 本地通知
```
- **触发条件**: 跨天或首次运行
- **清理策略**: 每7天清理90天前的对话记录

**AI 请求流程**
```
ViewModel → UseCase → Repository → AI Provider → Retrofit → AI Service
```
- **多服务商支持**: OpenAI、Azure OpenAI、阿里云、百度、智谱、腾讯混元、讯飞星火
- **响应解析**: 增强型 JSON 解析 + 多级降级策略

## 关键系统组件

### 隐私引擎 (PrivacyEngine)
- **职责**: 敏感数据本地脱敏处理
- **位置**: `domain/src/main/kotlin/com/empathy/ai/domain/service/PrivacyEngine.kt`
- **功能**: 数据脱敏、加密存储、密钥硬件级保护

### 数据清理系统
- **管理类**: `DataCleanupManager`
- **清理周期**: 每7天执行一次
- **清理范围**: 90天前的对话记录和每日总结
- **保留数据**: 联系人画像、大脑标签、用户配置等核心数据

### 版本更新插件
- **插件**: `VersionUpdatePlugin`
- **配置位置**: `app/build.gradle.kts`
- **功能**: 版本更新管理、备份保留（最大50个备份，保留30天）
- **实现**: `build/src/main/kotlin/com/empathy/ai/build/VersionUpdatePlugin.kt`

### AI 服务商系统
- **接口定义**: `domain/src/main/kotlin/com/empathy/ai/domain/model/AiProvider.kt`
- **仓库实现**: `data/src/main/kotlin/com/empathy/ai/data/repository/AiProviderRepositoryImpl.kt`
- **支持服务商**: OpenAI、Azure OpenAI、阿里云、百度、智谱、腾讯混元、讯飞星火（7家主流AI服务商）
- **模型配置**: 每个服务商支持自定义模型名称和 API 版本

### 开发者模式
- **功能**: 系统提示词管理、调试工具
- **入口**: 设置页面 → 开发者选项
- **用途**: 调试 AI 响应、查看日志、管理提示词

### 最近完成的问题修复（2026-01-11）
| BUG编号 | 问题描述 | 状态 |
|---------|----------|------|
| BUG-00057 | AI军师对话界面可读性问题 | 已完成 |
| BUG-00058 | 新建会话功能失效问题 | 已实现 |
| BUG-00059 | 中断生成后重新生成消息角色错乱 | 已实现 |
| BUG-00060 | 会话管理增强功能 | 已实现 |
| BUG-00061 | 会话历史跳转失败问题 | 已实现 |
| BUG-00062 | AI用量统计统一问题 | 已完成 |
| BUG-00063 | 导航回退与白屏闪烁问题 | 已实现 |
| BUG-00064 | AI总结功能未生效 | 已完成 |
| BUG-00066 | 画像标签编辑功能缺失 | 已完成 |
| BUG-00067 | 全局字体可读性修复 | 已完成 |
| BUG-00068 | 导航栈治理与返回语义规范 | 已实现 |

详细修复方案见: [文档/开发文档/BUG/](./文档/开发文档/BUG/)
- BUG-00063 修复见: [BUG-00063-切屏优化.md](./BUG-00063-切屏优化.md)

### 最近修复详情（2026-01-09 ~ 2026-01-11）

**BUG-00058/59/60/61: AI军师会话管理增强**
- 新建会话功能修复（通过导航参数传递 `createNew=true`）
- 重新生成消息角色错乱修复（新增 `isLikelyAiContent()` 检测）
- 会话置顶/重命名/空会话复用功能
- 会话历史跳转修复
- 数据库迁移 v15→v16

**BUG-00062: AI用量统计统一**
- `generateText` 和 `generateTextStream` 方法添加用量统计
- 修改 `AiRepositoryImpl` 和 `SendAdvisorMessageStreamingUseCase`

**BUG-00067: 全局字体可读性**
- 更新悬浮窗文本色
- 清理旧灰色硬编码（多个UI组件）

**BUG-00068/PRD-00035: 导航栈治理**
- AI军师子页面去栈与设置链路防重复入栈
- 联系人切换以 `CONTACT_LIST` 为稳定锚点
- 连接测试资源补齐与 androidTest 隔离
- 新增 `BUG00068NavigationStackTest.kt` 和 `BUG00068AiAdvisorEntryRefreshTest.kt` 测试

**BUG-00063: 导航回退与白屏闪烁问题**
- 为缓存 Tab 增加可见性保护，隐藏 Tab 不触发导航副作用
- AiAdvisorScreen 导航副作用增加可见性门控
- SettingsScreen 权限跳转副作用增加可见性门控
- 解决从联系人列表进入详情页时自动回退、白屏闪烁、层级混乱问题
- 修复详情：见 `BUG-00063-切屏优化.md`

### 组件复用系统（基于实际代码扫描 - 2026-01-11最新）

项目采用 **原子-分子-有机体-模板** 四级组件架构：

| 层级 | 示例 | 说明 |
|------|------|------|
| 原子组件 | `IosButton`, `IosTextField`, `AvatarView` | 基础UI单元，无业务逻辑 |
| 分子组件 | `IosSearchBar`, `ModernPersonaTab` | 组合原子组件，有简单交互 |
| 有机体组件 | `FactStreamCard`, `EmotionTimelineView` | 复杂业务逻辑，独立功能单元 |
| 模板组件 | `AiAdvisorChatScreen` | 页面级组合，特定场景使用 |

**组件位置**: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/`

**组件目录组织（25个子目录）**：
- animation/ - 动画组件
- button/ - 按钮组件
- card/ - 卡片组件
- chart/ - 图表组件
- chip/ - 标签组件
- contact/ - 联系人组件
- control/ - 控制组件
- dialog/ - 对话框组件（12个）
- emotion/ - 情感组件
- factstream/ - 事实流组件（8个）
- filter/ - 过滤组件
- input/ - 输入组件
- ios/ - iOS风格组件（18个）
- list/ - 列表组件
- message/ - 消息组件
- navigation/ - 导航组件
- overview/ - 概览组件
- persona/ - 人设组件
- relationship/ - 关系组件
- state/ - 状态组件
- tag/ - 标签组件
- text/ - 文本组件
- timeline/ - 时间轴组件（7个）
- topic/ - 主题组件
- vault/ - 数据保险库组件

**ViewModel统计（23个）**：
- BaseViewModel、AiAdvisorChatViewModel、AiAdvisorEntryViewModel、AiConfigViewModel
- BrainTagViewModel、ChatViewModel、ContactDetailTabViewModel、ContactDetailViewModel
- ContactListViewModel、ContactSelectViewModel、CreateContactViewModel
- DeveloperModeViewModel、ManualSummaryViewModel、PromptEditorViewModel
- SessionHistoryViewModel、SettingsViewModel、SystemPromptEditViewModel
- SystemPromptListViewModel、TopicViewModel、UserProfileViewModel、UsageStatsViewModel

## 多AI协作与文档系统

### 协作规则目录

项目使用 `.kiro/` 和 `.claude/` 目录管理多AI协作状态：

| 目录/文件 | 用途 |
|-----------|------|
| `.kiro/steering/` | 项目决策、快速启动指南 |
| `.kiro/specs/` | 功能规格文档（需求、设计、任务） |
| `.kiro/commands/` | 命令模板（QuickBuild、QuickTest等） |
| `.claude/steering/` | Claude专属项目决策文档 |
| `文档/项目文档/` | 长期文档体系（架构、接口说明） |

**协作流程**:
1. 任务开始前 → 读取 `.kiro/steering/quick-start.md`
2. 任务进行中 → 更新对应 spec 文档中的任务进度
3. 任务完成后 → 更新状态并记录变更日志

**关键规则（必须遵守）**:
- 任务执行前读取 `.kiro/steering/quick-start.md`
- 如发现其他AI正在执行相关任务，**必须停止并询问用户**
- 所有回答和文档使用中文编写

## 功能规格文档

项目使用 `.kiro/specs/` 目录存储功能规格文档，`.kiro/test/` 存储测试用例：

| 目录 | 内容 |
|------|------|
| `.kiro/specs/` | 需求、设计、任务分解文档 |
| `.kiro/specs/_archived/` | 已归档的历史文档 |
| `.kiro/steering/` | 产品决策、quick-start 指南 |
| `.kiro/commands/` | 命令模板（QuickBuild, QuickTest 等） |
| `.kiro/test/` | 测试用例和测试指南 |

**典型规格文档结构**:
- `requirements.md` - 需求定义
- `design.md` - 设计方案
- `tasks.md` - 任务分解

**当前分支**: `BUG63-qieping`（基于 Git，导航副作用可见性保护优化分支）

**最近提交**:
- 583fe34 fix: 修复切屏回退与联系人错误闪现问题 (BUG-00069)
- aad3781 fix: 修复Tab缓存场景下导航副作用可见性问题 (BUG-00063)
- 3a4b295 docs: add BUG-00063 screen switch optimization

## 模块文档

详细架构信息参考各模块文档：
- [domain/CLAUDE.md](./domain/CLAUDE.md) - 领域层详细文档（纯Kotlin业务逻辑）
- [data/CLAUDE.md](./data/CLAUDE.md) - 数据层详细文档（Room/Retrofit实现）
- [presentation/CLAUDE.md](./presentation/CLAUDE.md) - 表现层详细文档（Compose UI/ViewModel）
- [app/src/main/java/com/empathy/ai/app/CLAUDE.md](./app/src/main/java/com/empathy/ai/app/CLAUDE.md) - 应用层详细文档（Application/Service）

## 编码规范

- 遵循 SOLID、KISS、DRY、YAGNI 原则
- 所有公共API必须有KDoc注释
- 使用 `Result<T>` 统一处理成功/失败
- 测试文件命名: `XxxTest.kt` / `XxxSpec.kt`

### 导入分组顺序（必须严格遵守）
```
1. Kotlin 标准库
2. 第三方库（按字母顺序）
3. AndroidX 库
4. Jetpack Compose 库
5. 项目内部模块（按模块依赖顺序）
6. 当前包的类
```

### 模块间依赖规则
```
app → data, presentation, domain
data → domain（使用 api 暴露，解决 Hilt 多模块类型解析问题）
presentation → domain（使用 api 暴露，解决 Hilt 多模块类型解析问题）
domain → 纯 Kotlin，无 Android 依赖
```

### KSP vs KAPT 使用
- **Room**: 使用 KSP（注解处理器）
- **Moshi**: 使用 KSP（代码生成）
- **Hilt**: 使用 KAPT（解决多模块兼容性问题，`android.enableAggregatingTask=false`）

### 日志级别规范
| 级别 | 使用场景 | 示例 |
|------|----------|------|
| ERROR | 应用崩溃、关键功能失败 | `Logger.e("AI响应解析失败", e)` |
| WARN | 可恢复的错误、异常情况 | `Logger.w("网络请求超时，使用缓存")` |
| INFO | 重要业务状态变化 | `Logger.i("用户登录成功")` |
| DEBUG | 开发调试信息 | `Logger.d("消息发送: $content")` |

### 协程作用域使用
| 作用域 | 使用场景 | 禁止场景 |
|--------|----------|----------|
| `viewModelScope` | ViewModel 中的业务操作 | 长时间后台任务 |
| `rememberCoroutineScope()` | Composable 中的一次性操作 | 需要跟随 ViewModel 生命周期 |
| `GlobalScope` | **禁止使用** | 所有场景 |

### 测试覆盖率目标
| 模块 | 最低覆盖率 | 建议覆盖率 |
|------|-----------|-----------|
| domain | 80% | 90% |
| data | 70% | 80% |
| presentation | 60% | 70% |

### 测试框架说明
- **单元测试**: JUnit 4 + Mockk + Coroutines Test
- **Android测试**: AndroidX Test Runner + Espresso
- **Compose UI测试**: Compose Testing（部分用例）
- **Room迁移测试**: MigrationTestHelper + 历史Schema验证

## 仓库镜像配置

项目使用国内镜像加速依赖下载，优先级：**腾讯云 > 阿里云 > JitPack > 官方仓库**

**settings.gradle.kts 配置**:
- 腾讯云 Maven 镜像（优先使用）：mirrors.cloud.tencent.com
- 阿里云镜像（备用）：maven.aliyun.com
- JitPack（第三方库）
- Google/MavenCentral（最后备用）

**JVM 配置（24GB RAM 优化）**:
- 最大堆: 4GB（可调整）
- 初始堆: 1GB
- GC: G1垃圾收集器
- 工作线程: 最大8个（14核20线程优化）

## Build 变体

| 变体 | 说明 | 用途 |
|------|------|------|
| debug | Debug 构建 | 开发调试 |
| release | Release 构建 | 发布版本（混淆 + 签名） |
| dev | 开发专用变体 | 本地测试（applicationIdSuffix: .dev） |

**版本号配置**（gradle.properties）:
- APP_VERSION_NAME: 1.1.0
- APP_VERSION_CODE: 10100
- APP_RELEASE_STAGE: dev

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
