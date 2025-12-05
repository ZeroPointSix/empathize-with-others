# 项目概览 (Project Overview)

**项目名称**: 共情AI助手 (Empathy AI Assistant)
**英文名称**: Give Love
**版本**: v1.0.2-dev (MVP)
**状态**: 开发中 (UI层 Phase 1 已完成)
**最后更新**: 2025-12-05

---

## 📖 项目简介

### 一句话描述

> 一款基于 AI 技术的社交沟通辅助应用，通过智能分析帮助用户在社交场景中更好地理解和回应对方。

### 核心理念

- 🔒 **隐私优先**: 零后端架构，所有数据本地存储，敏感信息强制脱敏
- 🤖 **AI 赋能**: 利用 LLM 提供智能化的沟通建议和风险预警
- 🎯 **实用导向**: 解决真实社交场景中的实际问题
- 🚫 **零后端**: 不维护服务器，用户自备 API Key (BYOK)

---

## 🎯 核心功能

### 1️⃣ 主动分析 (AI 军师)

**功能**: 点击 [💡 帮我分析] 按钮，AI 根据聊天上下文和联系人画像提供策略建议

**输入**:
- 当前聊天记录（自动抓取）
- 联系人画像（事实、雷区、策略）
- 用户的长期目标

**输出**:
```
💭 AI 军师分析

【对方状态】
情绪: 略显沮丧
原因: 钓鱼没有收获

【关键洞察】
• 对方热爱钓鱼（匹配画像）
• 避免提及金钱话题（雷区）

【建议回复】
"没事的，钓鱼本来就看运气。
你今天穿的这身衣服真不错！"

[复制] [填充]
```

---

### 2️⃣ 主动风控 (防踩雷)

**功能**: 点击 [🛡️ 帮我检查] 按钮，实时检测输入内容是否触发雷区

**检测机制**:
- **Layer 1**: 本地关键词匹配（< 500ms）
- **Layer 2**: 云端语义检查（可选，< 3s）

**示例**:

输入: "今晚能借钱吗？"
```
⚠️ 检测到风险！

命中雷区: 不要提钱

建议: 删除敏感内容
```

输入: "今晚一起吃饭？"
```
✅ 检查通过！
未发现风险内容
```

---

### 3️⃣ 数据喂养 (智能提取)

**功能**: 从文本或媒体文件中自动提取联系人信息

**支持格式**:
- 📝 文本: 直接粘贴聊天记录
- 🎵 音频: .mp3 (ASR 转录)
- 🎬 视频: .mp4 (音频提取 + OCR)

**提取内容**:
- 📋 事实信息: 姓名、住址、爱好等
- 🔴 雷区: 不喜欢的话题
- 🟢 策略: 沟通建议

**示例**:

输入文本:
```
他叫李四，住朝阳区，爱好钓鱼。
不喜欢吃香菜，不要提钱。
多夸他衣品好。
```

AI 提取:
```
【事实】住址: 朝阳区, 爱好: 钓鱼
【雷区】不吃香菜, 不要提钱
【策略】多夸他衣品好
```

---

## 🏗️ 技术架构

### 架构模式

**Clean Architecture + MVVM**

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (UI, ViewModel, Service)               │
│  - Jetpack Compose                      │
│  - FloatingWindowService                │
│  - AccessibilityService                 │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│          Domain Layer                   │
│  (纯业务逻辑，无 Android 依赖)          │
│  - UseCase (AnalyzeChat, CheckDraft)    │
│  - Repository Interface                 │
│  - Business Models                      │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│           Data Layer                    │
│  (数据获取与持久化)                     │
│  - Room Database                        │
│  - Retrofit (AI API)                    │
│  - EncryptedSharedPreferences           │
└─────────────────────────────────────────┘
```

---

### 技术栈

| 类别 | 技术 | 版本 | 用途 |
|------|------|------|------|
| **语言** | Kotlin | 2.0.21 | 主开发语言 |
| **UI** | Jetpack Compose | 2024.12.01 | 声明式 UI |
| **架构** | Hilt | 2.52 | 依赖注入 |
| **数据库** | Room | 2.6.1 | 本地存储 |
| **网络** | Retrofit + OkHttp | 2.11.0 / 4.12.0 | AI API 调用 |
| **JSON** | Moshi | 1.15.1 | JSON 解析 |
| **协程** | Kotlin Coroutines | 1.9.0 | 异步处理 |
| **安全** | Security Crypto | 1.1.0-alpha06 | 加密存储 |
| **测试** | JUnit + Mockk | 4.13.2 / 1.13.13 | 单元测试 |

---

## 📊 项目进度

### 当前完成度: 92%

| 模块 | 状态 | 进度 | 说明 |
|------|------|------|------|
| **Domain Layer** | ✅ 完成 | 100% | 5 模型 + 5 接口 + 11 UseCase + 1 服务 |
| **单元测试** | ✅ 完成 | 100% | 113/114 测试通过 |
| **Data Layer** | ✅ 完成 | 100% | Room + Retrofit + Hilt 全部实现 |
| **Presentation - ViewModel** | ✅ 完成 | 100% | 4个核心ViewModel完成 |
| **Presentation - Theme** | ✅ 完成 | 100% | Material Design 3主题系统 |
| **Presentation - Navigation** | ✅ 完成 | 100% | 完整的导航系统 + NavGraph |
| **Presentation - UI** | ✅ 完成 | 100% | Phase 1、2、3全部完成（4个Screen + 10个组件） |
| **Presentation - MainActivity** | ✅ 完成 | 100% | MainActivity集成完成 |
| **Presentation - Service** | ⏳ 待开始 | 0% | 悬浮窗 + 无障碍服务 |
| **集成测试** | 📄 文档完成 | - | 黑盒测试指南已完成 |

---

### 开发路线图

```
✅ Phase 0: 项目初始化 (已完成)
   - 依赖配置
   - 架构设计文档
   - 业务逻辑设计

✅ Phase 1: Domain Layer (已完成 - 2025-12-02)
   - 数据模型定义
   - Repository 接口
   - UseCase 业务逻辑
   - PrivacyEngine 脱敏引擎
   - 单元测试 (113/114 通过)

✅ Phase 2: Data Layer (已完成 - 2025-12-05)
   - Room 数据库实现
   - Retrofit API 实现
   - Hilt 依赖注入配置
   - Repository 实现类

✅ Phase 3: Presentation Layer (已完成 - 2025-12-05)
   ✅ Phase 3.1: 基础设施 (已完成)
      - Material Design 3 主题系统
      - 类型安全的导航系统
      - NavGraph 完整实现
   ✅ Phase 3.2: 可复用组件 (已完成)
      - 10个高质量组件（7个P0 + 3个P1）
      - 53个Preview函数
   ✅ Phase 3.3: 核心Screen (已完成)
      - ContactListScreen (联系人列表)
      - ContactDetailScreen (联系人详情)
      - ChatScreen (聊天分析)
      - BrainTagScreen (标签管理)
      - 25个Preview函数
   🔄 Phase 3.4: 测试优化 (进行中)
      - ✅ MainActivity集成完成
      - ⏳ 导航流程测试
      - ⏳ 代码质量检查

⏳ Phase 4: Service Layer (待开始)
   - FloatingWindowService (悬浮窗)
   - AccessibilityService (屏幕抓取)

⏳ Phase 5: 集成测试与优化
   - 黑盒测试
   - 性能优化
   - UI/UX 优化
   - Bug 修复

🎯 Phase 6: MVP 发布
   - Beta 测试
   - 用户反馈
   - 迭代优化
```

---

## 🎨 设计亮点

### 1. 隐私优先设计

**强制脱敏机制**:
```kotlin
// ❌ 错误: 直接发送原始数据
aiRepository.analyze(rawText)

// ✅ 正确: 必须先脱敏
val maskedText = PrivacyEngine.mask(rawText, privacyMapping)
aiRepository.analyze(maskedText)
```

**加密存储**:
- API Key 使用 `EncryptedSharedPreferences`
- 硬件级加密（Android Keystore）
- 敏感数据排除备份

---

### 2. 零 Android 依赖的 Domain Layer

**优势**:
- ✅ 可独立测试（无需 Android 模拟器）
- ✅ 可复用于其他平台（iOS、Desktop）
- ✅ 业务逻辑清晰，易于维护

**目录结构**:
```
domain/
├── model/           # 纯 Kotlin data class
├── repository/      # 接口定义（无实现）
├── usecase/         # 纯业务逻辑
└── service/         # 领域服务（如 PrivacyEngine）
```

---

### 3. RAG (Retrieval-Augmented Generation) 架构

**数据流**:
```
联系人画像 (Profile)
    ↓
雷区 & 策略 (BrainTags)
    ↓
聊天上下文 (Messages)
    ↓
Prompt 拼装
    ↓
隐私脱敏
    ↓
AI 推理
    ↓
结构化输出 (AnalysisResult)
```

---

### 4. 错误处理设计

**统一的 Result 模式**:
```kotlin
// 所有 Repository 方法返回 Result<T>
suspend fun getProfile(id: String): Result<ContactProfile?>

// UseCase 层统一处理异常
try {
    val profile = repository.getProfile(id).getOrThrow()
    // 业务逻辑
    Result.success(result)
} catch (e: Exception) {
    Result.failure(e)
}
```

---

## 🧪 质量保证

### 单元测试

**覆盖率**: Domain Layer 100%

| 测试类 | 测试数 | 状态 |
|--------|--------|------|
| PrivacyEngineTest | 4 | ✅ 全部通过 |
| CheckDraftUseCaseTest | 5 | ✅ 全部通过 |
| AnalyzeChatUseCaseTest | 7 | ✅ 全部通过 |

**关键测试场景**:
- ✅ Prompt 正确拼装
- ✅ 隐私脱敏验证
- ✅ 雷区检测准确性
- ✅ 边界情况处理
- ✅ 错误处理机制

---

### 代码质量

**遵循原则**: SOLID / KISS / DRY / YAGNI

**示例**:

**KISS (简单至上)**:
```kotlin
// 简单的数据模型
data class ContactProfile(
    val id: String,
    val name: String,
    val targetGoal: String,
    val facts: Map<String, String> = emptyMap()
)
```

**DRY (杜绝重复)**:
```kotlin
// 统一的脱敏引擎
object PrivacyEngine {
    fun mask(text: String, mapping: Map<String, String>): String
    fun maskBatch(texts: List<String>, mapping: Map<String, String>): List<String>
}
```

**SOLID**:
- 每个 UseCase 只负责一个业务流
- Repository 使用接口，方便替换实现
- 依赖抽象而非具体实现

---

## 🚀 快速开始

### 前置要求

- ✅ JDK 17
- ✅ Android Studio (最新版)
- ✅ Android SDK 24+ (Android 7.0+)
- ✅ Gradle 8.13

---

### 1. 克隆项目

```bash
git clone <repository-url>
cd Love
```

---

### 2. 打开项目

```
Android Studio → Open → 选择项目目录
```

等待 Gradle 同步完成

---

### 3. 运行单元测试

```bash
./gradlew :app:testDebugUnitTest
```

**预期输出**:
```
BUILD SUCCESSFUL in 1m 23s
16 tests completed, 0 failed ✅
```

---

### 4. 编译 APK

```bash
./gradlew assembleDebug
```

**输出位置**:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

### 5. 安装到设备

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

**注意**: Data Layer 未完成前，应用功能受限

---

## 📁 项目结构

### 源代码结构

```
app/src/main/java/com/empathy/ai/
├── app/
│   └── EmpathyApplication.kt       # Application 入口
├── domain/                          # ✅ 已完成
│   ├── model/                       # 数据模型 (5 个)
│   ├── repository/                  # 接口定义 (5 个)
│   ├── usecase/                     # 业务用例 (3 个)
│   └── service/                     # 领域服务 (1 个)
├── data/                            # ⏳ 待实现
│   ├── local/                       # Room 数据库
│   ├── remote/                      # Retrofit API
│   └── repository/                  # 实现类
├── presentation/                    # ⏳ 部分完成
│   ├── ui/                          # Compose UI
│   ├── viewmodel/                   # ViewModel
│   ├── theme/                       # 主题
│   └── service/                     # Android Service
└── di/                              # ⏳ 待实现
    └── modules/                     # Hilt 模块
```

---

### 文档结构

```
docs/
├── README.md                        # 文档导航
├── 00-项目概述/
│   └── OVERVIEW.md                  # 本文档
├── 01-架构设计/
│   ├── 项目架构设计.md
│   ├── 功能设计.md
│   └── 业务层/
│       ├── 数据字段设计.md
│       ├── 接口设计.md
│       └── 业务逻辑设计.md
├── 02-开发指南/
│   ├── 依赖配置说明.md
│   ├── 依赖快速参考.md
│   └── Step1完成总结.md
├── 03-测试文档/
│   ├── 单元测试总结.md
│   ├── 黑盒测试指南.md
│   ├── 快速测试清单.md
│   └── 测试逻辑.md
└── 04-Backup/
    └── (已废弃文档)
```

---

## 🔐 安全性

### 隐私保护措施

1. **零后端架构**
   - 不维护服务器
   - 无用户账户体系
   - 所有数据本地存储

2. **强制脱敏**
   - 所有 AI 请求前必须脱敏
   - 使用占位符替换敏感信息
   - 支持自定义脱敏规则

3. **加密存储**
   - API Key 使用 EncryptedSharedPreferences
   - 硬件级加密（Android Keystore）
   - 敏感数据排除云备份

4. **权限最小化**
   - 只申请必需权限
   - 无障碍服务仅用于屏幕抓取
   - 不收集用户隐私数据

---

## 📈 性能指标

### 设计目标

| 指标 | 目标 | 说明 |
|------|------|------|
| **本地检查** | < 500ms | 关键词匹配响应时间 |
| **屏幕抓取** | < 2s | 完成文字抓取 |
| **AI 分析** | < 10s | 完整分析流程 |
| **内存占用** | < 200MB | 正常使用状态 |
| **APK 大小** | < 30MB | Release 版本 |

---

## 🤝 贡献指南

### 开发规范

1. **代码风格**: 遵循 Kotlin 官方规范
2. **提交规范**: 使用 Conventional Commits
3. **分支策略**: Git Flow
4. **测试要求**: 新功能必须包含单元测试

### 提交消息格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

**类型 (type)**:
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `test`: 测试相关
- `refactor`: 重构
- `style`: 代码格式
- `chore`: 构建/工具变更

---

## 📞 联系方式

- **项目维护者**: hushaokang
- **问题反馈**: [GitHub Issues](待补充)
- **开发讨论**: (待补充)

---

## 📄 许可证

(待补充)

---

## 🎯 里程碑

### v1.0.0-MVP (目标: 2025-12-15)

- ✅ Domain Layer 完成
- ✅ 单元测试完成
- ⏳ Data Layer 实现
- ⏳ Presentation Layer 实现
- ⏳ 集成测试完成
- ⏳ Beta 版本发布

### v1.1.0 (目标: 2026-01)

- 性能优化
- UI/UX 改进
- 多 AI 模型支持优化
- 用户反馈迭代

---

## 📚 延伸阅读

- [项目架构设计](../01-架构设计/项目架构设计.md)
- [业务层设计文档](../01-架构设计/业务层/)
- [单元测试总结](../03-测试文档/单元测试总结.md)
- [黑盒测试指南](../03-测试文档/黑盒测试指南.md)

---

**最后更新**: 2025-12-05 (Phase 4 MainActivity集成完成)
**维护者**: hushaokang
**版本**: v1.0.4-dev
