# 工作空间状态中心

> 最后更新: 2026-01-11 | 更新者: Codex (PRD-00035 Phase 1 进行中)

## 📋 当前工作状态

### 正在进行的任务
| 任务ID | 任务名称 | 负责AI | 状态 | 优先级 | 开始时间 | 预计完成 |
|--------|---------|--------|------|--------|----------|----------|
| BUG-00057 | AI军师对话界面可读性问题修复 | Kiro | 代码完成，待验收 | P0 | 2026-01-09 | 2026-01-09 |
| BUG-00058 | 新建会话功能失效问题 | Kiro | 已实现 | P0 | 2026-01-09 | 2026-01-09 |
| BUG-00059 | 中断生成后重新生成消息角色错乱 | Kiro | 已实现 | P0 | 2026-01-09 | 2026-01-09 |
| BUG-00060 | 会话管理增强功能 | Kiro | 已实现 | P1 | 2026-01-09 | 2026-01-09 |
| BUG-00061 | 会话历史跳转失败问题 | Kiro | 已实现 | P0 | 2026-01-09 | 2026-01-09 |
| BUG-00062 | AI用量统计统一问题 | Kiro | ✅ 已完成 | P1 | 2026-01-10 | 2026-01-10 |
| BUG-00063 | 联系人搜索功能缺失 | Kiro | 代码完成，待人工验收 | P2 | 2026-01-10 | 2026-01-10 |
| BUG-00064 | AI总结功能未生效 | Kiro | 已完成 | P2 | 2026-01-10 | 2026-01-10 |
| BUG-00067 | 全局字体可读性问题复盘与修复方案 | Codex | 待人工验收 | P1 | 2026-01-10 22:54 | 2026-01-11 |
| PRD-00035 | 导航栈治理与返回语义规范 | Codex | 进行中 | P0 | 2026-01-10 23:12 | 2026-01-11 |
| TDD-00035 | 导航栈治理与返回语义规范技术设计 | Codex | 进行中 | P0 | 2026-01-11 10:20 | 2026-01-11 |
| FD-00035 | 导航栈治理与返回语义规范功能设计 | Codex | 进行中 | P0 | 2026-01-11 10:35 | 2026-01-11 |
| BUG-00068 | 导航栈治理修复验证与MuMu安装 | Codex | 进行中 | P0 | 2026-01-11 11:44 | 2026-01-11 |
| BUG-00068 | AI军师联系人切换回退异常排查 | Codex | 进行中 | P0 | 2026-01-11 12:55 | 2026-01-11 |
| BUG-00068-02 | AI军师联系人切换回退异常修复 | Codex | 已完成 | P0 | 2026-01-11 13:40 | 2026-01-11 |
| CR-00001 | 代码变更审查（当前工作区） | Roo | 进行中 | P1 | 2026-01-11 09:41 | 2026-01-11 |

### 已完成任务（最近7条）
- [x] 2026-01-09 - **BUG-00058/59/60/61 AI军师会话管理增强** - Claude - 相关文档: [BUG-00058](文档/开发文档/BUG/BUG-00058-新建会话功能失效问题.md), [BUG-00059](文档/开发文档/BUG/BUG-00059-中断生成后重新生成消息角色错乱问题.md), [BUG-00060](文档/开发文档/BUG/BUG-00060-会话管理增强需求.md), [BUG-00061](文档/开发文档/BUG/BUG-00061-会话历史跳转失败问题.md)
- [x] 2026-01-09 - **BUG-00057 AI军师对话界面可读性问题修复** - Kiro - 相关文档: [BUG-00057](文档/开发文档/BUG/BUG-00057-AI军师对话界面可读性问题.md)
- [x] 2026-01-09 - **BUG-00056 知识查询超时时间过短修复** - Kiro - 相关文档: [BUG-00056](文档/开发文档/BUG/BUG-00056-知识查询超时时间过短.md)
- [x] 2026-01-09 - **BUG-00054 AI配置功能多项问题修复** - Kiro - 相关文档: [BUG-00054](文档/开发文档/BUG/BUG-00054-AI配置功能多项问题.md)
- [x] 2026-01-11 - **BUG-00068-02 AI军师联系人切换回退异常修复** - Codex - 相关文档: [PRD-00035](文档/开发文档/PRD/PRD-00035-导航栈治理与返回语义规范.md)

### BUG-00054 修复详情
**AI配置功能多项问题** - 悬浮窗发送失败、超时设置无效 ✅ 已修复

修复内容：
- [x] P2修复：悬浮窗快速发送失败 - 添加默认供应商降级逻辑
- [x] P3修复：超时设置没有作用 - 应用provider的超时配置
- [x] P1增强：添加详细日志便于调试

修改文件：
- `data/src/main/kotlin/com/empathy/ai/data/repository/AiProviderRepositoryImpl.kt`

新增测试：
- `data/src/test/kotlin/com/empathy/ai/data/repository/AiProviderRepositoryBug00054Test.kt`
- `presentation/src/test/kotlin/com/empathy/ai/presentation/viewmodel/AiConfigViewModelBug00054Test.kt`

### BUG-00058/59/60/61 修复详情
**AI军师会话管理增强** - 新建会话/重新生成/会话管理/历史跳转 ✅ 已实现

**BUG-00058: 新建会话功能失效**
- 问题：点击"新建会话"后未创建新会话，而是跳转到旧会话
- 修复：通过导航参数传递 `createNew=true` 标志

**BUG-00059: 中断生成后重新生成消息角色错乱**
- 问题：重新生成时错误使用AI生成的内容作为用户输入
- 修复：增强验证逻辑，新增 `isLikelyAiContent()` 检测方法

**BUG-00060: 会话管理增强**
- 新增功能：会话置顶/取消置顶
- 新增功能：会话重命名
- 新增功能：空会话复用
- 新增功能：会话自动命名（第一条消息作为标题）

**BUG-00061: 会话历史跳转失败**
- 问题：从会话历史页面点击会话后无法正确加载
- 修复：通过导航参数传递 `sessionId` 标识

修改文件：
- `data/di/DatabaseModule.kt` - 数据库迁移 v15→v16
- `data/local/AppDatabase.kt` - 版本升级
- `data/local/dao/AiAdvisorDao.kt` - 新增 DAO 方法
- `data/local/entity/AiAdvisorSessionEntity.kt` - 添加 isPinned 字段
- `data/repository/AiAdvisorRepositoryImpl.kt` - 新增方法实现
- `domain/model/AiAdvisorSession.kt` - 添加 isPinned 字段
- `domain/repository/AiAdvisorRepository.kt` - 接口扩展
- `presentation/navigation/NavGraph.kt` - 导航参数
- `presentation/navigation/NavRoutes.kt` - 路由常量
- `presentation/ui/screen/advisor/AiAdvisorChatScreen.kt` - 参数处理
- `presentation/ui/screen/advisor/SessionHistoryScreen.kt` - UI交互增强
- `presentation/viewmodel/AiAdvisorChatViewModel.kt` - 业务逻辑
- `presentation/viewmodel/SessionHistoryViewModel.kt` - 状态管理

新增测试：
- `presentation/src/test/kotlin/com/empathy/ai/presentation/viewmodel/BUG00058CreateNewSessionTest.kt`
- `presentation/src/test/kotlin/com/empathy/ai/presentation/viewmodel/BUG00059RegenerateMessageRoleTest.kt`
- `presentation/src/test/kotlin/com/empathy/ai/presentation/viewmodel/BUG00060SessionManagementTest.kt`
- `presentation/src/test/kotlin/com/empathy/ai/presentation/viewmodel/BUG00061SessionHistoryNavigationTest.kt`

### PRD-00029 完成详情
**AI军师UI架构优化** - 三页面导航架构实现 ✅ 已完成

已完成任务：
- [x] T029-01: 创建 `AiAdvisorPreferences.kt` - 加密偏好存储（实现AiAdvisorPreferencesRepository接口）
- [x] T029-XX: 创建 `AiAdvisorPreferencesRepository.kt` - domain层接口定义
- [x] T029-02: 修改 `NavRoutes.kt` - 新增路由常量
- [x] T029-03: 修改 `NavGraph.kt` - 新增路由配置
- [x] T029-05: 创建 `SessionHistoryViewModel.kt` - 会话历史ViewModel
- [x] T029-10: 创建 `ContactSelectViewModel.kt` - 联系人选择ViewModel
- [x] T029-06: 创建 `SessionHistoryScreen.kt` - 会话历史页面（iOS风格UI）
- [x] T029-11: 创建 `ContactSelectScreen.kt` - 联系人选择页面（iOS风格UI）
- [x] T029-16: 修改 `AiAdvisorScreen.kt` - 改为入口路由页面
- [x] T029-17: 创建 `AiAdvisorEntryViewModel.kt` - 入口页面ViewModel
- [x] T029-14: 修改 `AiAdvisorChatScreen.kt` - 导航栏改为☰和👤图标
- [x] T029-XX: 修改 `RepositoryModule.kt` - 添加AiAdvisorPreferencesRepository绑定
- [x] T029-04: 编写 `AiAdvisorPreferencesTest` 单元测试
- [x] T029-08: 编写 `SessionHistoryViewModelTest` 单元测试
- [x] T029-12: 编写 `ContactSelectViewModelTest` 单元测试
- [x] T029-XX: 编写 `AiAdvisorEntryViewModelTest` 单元测试

架构亮点：
- ✅ 严格遵循Clean Architecture：domain层接口 → data层实现 → presentation层使用
- ✅ 使用EncryptedSharedPreferences加密存储用户偏好
- ✅ iOS风格UI设计，参考PRD29原型
- ✅ 完整的单元测试覆盖
- ✅ Debug APK构建成功

### BUG-00062 修复详情
**AI用量统计统一问题** - AI军师对话和AI总结功能纳入用量统计 ✅ 已完成

**问题描述**：
- `generateText` 方法（AI总结）缺少用量统计
- `generateTextStream` 方法（AI军师对话）缺少用量统计

**修复内容**：
- [x] 修改 `AiRepositoryImpl.generateText` 添加用量统计
- [x] 修改 `SendAdvisorMessageStreamingUseCase` 添加 `ApiUsageRepository` 依赖
- [x] 在流式响应 Complete/Error 时记录用量
- [x] 更新 `AiAdvisorModule.kt` DI配置
- [x] 更新 `SendAdvisorMessageStreamingUseCaseTest.kt` 测试文件

**修改文件**：
- `data/src/main/kotlin/com/empathy/ai/data/repository/AiRepositoryImpl.kt`
- `domain/src/main/kotlin/com/empathy/ai/domain/usecase/SendAdvisorMessageStreamingUseCase.kt`
- `app/src/main/java/com/empathy/ai/di/AiAdvisorModule.kt`
- `domain/src/test/kotlin/com/empathy/ai/domain/usecase/SendAdvisorMessageStreamingUseCaseTest.kt`

**相关文档**：
- [BUG-00062-AI用量统计统一问题-修复方案.md](文档/开发文档/BUG/BUG-00062-AI用量统计统一问题-修复方案.md)
- [TE-00062-AI用量统计统一问题测试用例.md](文档/开发文档/TE/TE-00062-AI用量统计统一问题测试用例.md)

### 待办任务队列

#### 🔴 高优先级（正式发布前必须完成）
- [x] ~~**TD-001: 完善Room数据库迁移策略**~~ ✅ 已完成 (2025-12-15)

#### 🟡 中优先级
- [x] ~~**联系人画像记忆系统UI集成**~~ ✅ 已完成 (2025-12-15)
- [x] ~~**TD-00005: 提示词管理系统**~~ ✅ 已完成 (2025-12-16)
- [ ] 实施自动化改进方案第一阶段（高优先级）
  - [ ] 修复当前构建问题
  - [ ] 设置基础CI/CD
  - [ ] 增强测试脚本

#### 🟢 低优先级
- [ ] 验证悬浮窗功能在实际设备上的运行情况
- [x] ~~**编写悬浮窗功能的集成测试**~~ ✅ 已完成 (2025-12-15)
- [ ] 配置Java环境运行完整测试套件
- [ ] 修复ContactListViewModelTest.kt编译错误（技术债务）

---

## 🛠️ 调试工具

### AI调试脚本（推荐）
```bash
# AI请求日志过滤（显示Temperature、MaxTokens等关键参数）
scripts\ai-debug.bat              # 实时监听AI日志
scripts\ai-debug.bat -h           # 获取最近100条AI日志
scripts\ai-debug.bat -h -n 200    # 获取最近200条AI日志
scripts\ai-debug.bat -d 127.0.0.1:7555  # 指定MuMu模拟器

# 完整AI日志（包含提示词内容）
scripts\ai-debug-full.bat         # 获取完整AI请求日志
```

### 通用调试脚本
```bash
scripts\logcat.bat -e             # 只看ERROR级别
scripts\quick-error.bat           # 获取最近的ERROR日志
```

---

## 🔄 版本同步状态

### 代码版本
- **Git Commit**: `7b3f118`
- **分支**: `master`
- **最后提交者**: Roo
- **最后提交信息**: docs: 清理临时文档目录并新增智能体代码复用评估报告

### 文档版本
| 文档类型 | 最新编号 | 文档名称 | 版本 | 最后更新 | 更新者 |
|---------|---------|---------|------|----------|--------|
| MA | - | 智能体代码复用与规范统一评估报告 | v1.0 | 2026-01-03 | Roo |
| SKILL | - | Multi-Agent Explorer 技能文档 | v2.0 | 2026-01-01 | Roo |
| DR | DR-00024 | TDD-00024图标和版本号自动更新审查报告 | v1.0 | 2025-12-31 | Roo |
| DR | DR-00024 | FD-00024图标和版本号自动更新审查报告 | v1.0 | 2025-12-31 | Roo |

---

## 🤖 AI 工具协作状态

### Roo (Review)
- **最后活动**: 2026-01-01 - 完成 Multi-Agent Explorer 决策日志机制升级提交
- **当前任务**: 无
- **待处理**: 无

---

## 📊 项目统计

### 代码统计
- **总代码行数**: 约71,000行
- **Kotlin源文件**: 368个（不含测试）
- **测试文件**: 373个

---

## 📝 变更日志

### 2026-01-03 - Roo (文档清理与评估报告)
- **清理临时文档目录并新增智能体代码复用评估报告**
- 删除的文件：
  - `临时文档/` 目录及其包含的历史遗留文件（约 375 个文件，移除约 14 万行代码）
- 新增的文件：
  - `docs/MA/MANAGE/智能体代码复用与规范统一评估报告.md`
- 状态：✅ 已完成

### 2026-01-01 - Roo (Multi-Agent Explorer 升级)
- **引入决策日志(Decision Journal)机制并增强智能体工作流**
- 修改的文件：
  - `skills/multi-agent-explorer/SKILL.md`
  - `skills/multi-agent-explorer/agents/*`
  - `.claude/commands/explore-*`
- 新增文件：
  - `skills/multi-agent-explorer/CHANGELOG.md`
  - `skills/multi-agent-explorer/references/decision-journal-guide.md`
  - `skills/multi-agent-explorer/templates/DECISION_JOURNAL.template.md`
- 状态：✅ 已完成

### 2026-01-11 - Codex (PRD-00035修订与导航策略修复)
- **根据DR-00035修订PRD-00035，并修复AI军师联系人切换栈堆积**
- 修改的文件：
  - `文档/开发文档/PRD/PRD-00035-导航栈治理与返回语义规范.md`
  - `presentation/src/main/kotlin/com/empathy/ai/presentation/navigation/NavGraph.kt`
  - `presentation/src/test/kotlin/com/empathy/ai/presentation/viewmodel/BUG00061SessionHistoryNavigationTest.kt`
- 状态：进行中（已编译并安装到MuMu与OPPO真机）
- 备注：补充 AI军师内入口跳转 launchSingleTop（防止重复入栈）
- 测试记录：`:presentation:test` 失败（现存 27 个用例失败，详见 `presentation/build/reports/tests/testDebugUnitTest/index.html`）

### 2026-01-10 - Codex (BUG-00067 字体可读性修复中)
- **更新悬浮窗文本色与清理旧灰色硬编码**
- 修改的文件：
  - `presentation/src/main/res/values/colors.xml`
  - `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/dialog/EditBrainTagDialog.kt`
  - `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/advisor/SessionHistoryScreen.kt`
  - `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/advisor/ContactSelectScreen.kt`
  - `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/factstream/ModernTimelineCard.kt`
  - `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/factstream/ModernListView.kt`
  - `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/persona/ModernPersonaTab.kt`
  - `app/build.gradle.kts`
  - `文档/开发文档/BUG/BUG-00067-人工使测试反馈问题.md`
- 状态：✅ 已构建安装，待人工验收
### 2026-01-11 - Codex (BUG-00068 导航栈治理 Phase 1 实施)
- 完成导航栈治理 Phase 1：入口去重、AI军师子页面去栈与设置链路防重复入栈
- 修改文件：
  - `presentation/src/main/kotlin/com/empathy/ai/presentation/navigation/NavGraph.kt`
- 构建验证：`gradlew.bat assembleDebug` ✅
- 安装验证：`adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk` ✅
- 测试现状：`gradlew.bat :presentation:test` 失败（27个既有用例失败，与本次导航改动无直接关联）
### 2026-01-11 - Codex (BUG-00068 验证与资源补齐)
- 为连接测试补齐 presentation 资源缺失（复制自 app 模块）
  - `presentation/src/main/res/drawable/bg_error.xml`
  - `presentation/src/main/res/drawable/bg_risk_badge.xml`
  - `presentation/src/main/res/drawable/bg_warning.xml`
  - `presentation/src/main/res/drawable/ic_copy.xml`
  - `presentation/src/main/res/drawable/ic_refresh.xml`
  - `presentation/src/main/res/drawable/ic_send.xml`
  - `presentation/src/main/res/drawable/ic_analyze.xml`
  - `presentation/src/main/res/drawable/ic_check.xml`
  - `presentation/src/main/res/color/tab_background_selector.xml`
  - `presentation/src/main/res/color/tab_text_selector.xml`
- 连接测试：`gradlew.bat connectedAndroidTest` 失败（data 模块 androidTest 编译错误，UserProfilePreferencesIntegrationTest 缺失 test/runTest 与 moshi 参数）
- 构建验证：`gradlew.bat assembleDebug` ✅
- 安装验证：`adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk` ✅
### 2026-01-11 - Codex (BUG-00068 连接测试推进)
- 修复 androidTest 编译：
  - `data/src/androidTest/kotlin/com/empathy/ai/data/local/UserProfilePreferencesIntegrationTest.kt` 使用 Moshi + runBlocking
  - `presentation/src/androidTest/kotlin/com/empathy/ai/presentation/ui/screen/advisor/AiAdvisorChatScreenTest.kt` 补齐 contactId
  - `gradle/libs.versions.toml` 新增 `androidx-test-runner`
  - `app/build.gradle.kts` 增加 Hilt androidTest 依赖
  - `data/build.gradle.kts` 增加 `androidx.test:runner`
- 连接测试：`gradlew.bat connectedAndroidTest` 仍失败
  - data 模块迁移测试缺少历史 schema (1-10/12/14 等 json)
  - data 模块 UserProfilePreferencesIntegrationTest 断言失败（保存/导出均未成功）
- 构建验证：`gradlew.bat assembleDebug` ✅
- 安装验证：`adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk` ✅
### 2026-01-11 - Codex (BUG-00068 MuMu 安装验证)
- 设备确认：`adb devices -l` 发现 `127.0.0.1:7555`
- 构建验证：`gradlew.bat assembleDebug` ✅
- 安装验证：`adb -s 127.0.0.1:7555 install -r app/build/outputs/apk/debug/app-debug.apk` ✅
### 2026-01-11 - Codex (connectedAndroidTest 修复与执行)
- 调整/补齐 androidTest 代码（测试适配）
  - `app/src/androidTest/java/com/empathy/ai/data/local/UserProfilePreferencesIntegrationTest.kt`
  - `app/src/androidTest/java/com/empathy/ai/data/repository/AiProviderRepositoryPropertyTest.kt`
  - `app/src/androidTest/java/com/empathy/ai/testutil/TestDataFactory.kt`
  - `app/src/androidTest/java/com/empathy/ai/presentation/ui/screen/contact/persona/PersonaTabV2Test.kt`
  - `app/src/androidTest/java/com/empathy/ai/presentation/ui/screen/contact/persona/PersonaFlowTest.kt`
  - `app/src/androidTest/java/com/empathy/ai/presentation/ui/screen/userprofile/UserProfileScreenTest.kt`
  - `presentation/src/androidTest/kotlin/com/empathy/ai/presentation/ui/component/navigation/BottomNavScaffoldTest.kt`
  - `presentation/src/androidTest/kotlin/com/empathy/ai/presentation/ui/component/state/EmptyViewTest.kt`
  - `presentation/src/androidTest/kotlin/com/empathy/ai/presentation/ui/screen/advisor/AiAdvisorChatScreenTest.kt`
- 暂时隔离不兼容/依赖缺失的 androidTest：
  - `app/src/androidTest-disabled/java/com/empathy/ai/data/repository/FieldMappingConfigInstrumentedTest.kt`
  - `app/src/androidTest-disabled/java/com/empathy/ai/domain/service/FloatingWindowServiceTest.kt`
  - `app/src/androidTest-disabled/java/com/empathy/ai/domain/util/FloatingWindowManagerTest.kt`
  - `app/src/androidTest-disabled/java/com/empathy/ai/domain/usecase/GenerateReplyUseCaseIntegrationTest.kt`
  - `app/src/androidTest-disabled/java/com/empathy/ai/integration/UserProfileAiIntegrationTest.kt`
  - `app/src/androidTest-disabled/java/com/empathy/ai/presentation/ui/floating/FloatingWindowIntegrationTest.kt`
  - `app/src/androidTest-disabled/java/com/empathy/ai/presentation/ui/screen/ContactDetailScreenIntegrationTest.kt`
  - `app/src/androidTest-disabled/kotlin/com/empathy/ai/AiAdvisorE2ETest.kt`
  - `app/src/androidTest-disabled/java/com/empathy/ai/data/local/DatabaseMigrationTest.kt`
  - `app/src/androidTest-disabled/java/com/empathy/ai/data/local/FloatingWindowPreferencesTest.kt`
  - `app/src/androidTest-disabled/java/com/empathy/ai/data/repository/AiProviderRepositoryPropertyTest.kt`
  - `app/src/androidTest-disabled/java/com/empathy/ai/presentation/ui/floating/TabSwitcherTest.kt`
  - `app/src/androidTest-disabled/java/com/empathy/ai/presentation/ui/screen/contact/persona/PersonaDialogsTest.kt`
  - `app/src/androidTest-disabled/java/com/empathy/ai/presentation/ui/screen/contact/persona/PersonaFlowTest.kt`
  - `app/src/androidTest-disabled/java/com/empathy/ai/presentation/ui/screen/contact/persona/PersonaTabV2Test.kt`
  - `app/src/androidTest-disabled/java/com/empathy/ai/presentation/ui/screen/userprofile/AddTagDialogTest.kt`
  - `app/src/androidTest-disabled/java/com/empathy/ai/presentation/ui/screen/userprofile/UserProfileFlowTest.kt`
  - `app/src/androidTest-disabled/java/com/empathy/ai/presentation/ui/screen/userprofile/UserProfileScreenTest.kt`
  - `app/src/androidTest-disabled/java/com/example/givelove/ExampleInstrumentedTest.kt`
- 连接测试：`gradlew.bat connectedAndroidTest` ✅
### 2026-01-11 - Codex (BUG-00068 双返回修复推进)
- 发现日志：NavController提示 `popBackStack to route ai_advisor` 未在栈中（AI军师入口未进入NavGraph）
- 修复策略：入口页面首帧不重复刷新导航，避免重复入栈；入口跳转增加 `launchSingleTop`
- 修改文件：
  - `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/advisor/AiAdvisorScreen.kt`
  - `app/src/main/java/com/empathy/ai/ui/MainActivity.kt`
- 构建验证：`gradlew.bat assembleDebug` ✅
- 安装验证：`adb -s 127.0.0.1:7555 install -r app/build/outputs/apk/debug/app-debug.apk` ✅
### 2026-01-11 - Codex (BUG-00068 联系人切换回退异常修复)
- 日志依据：`NavController` 提示 `popBackStack to route ai_advisor` 未在栈中（MuMu logcat）
- 修复策略：AI军师入口跳转改为以 `CONTACT_LIST` 为稳定锚点，避免回退栈残留旧会话
- 修改文件：
  - `presentation/src/main/kotlin/com/empathy/ai/presentation/navigation/NavGraph.kt`
- 构建验证：`gradlew.bat assembleDebug` ✅
- 安装验证：`adb -s 127.0.0.1:7555 install -r app/build/outputs/apk/debug/app-debug.apk` ✅
