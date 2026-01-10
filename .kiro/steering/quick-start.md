# 快速开始指南

> 这是 AI 助手的核心参考文件，保持精简。详细信息请查阅对应的专项文档。

## 语言规范
- 文档和回答：中文
- 代码注释/变量名/类名：英文

## 🆕 多模块架构 (TD-00017)

> 2025-12-25 更新 - 项目已完成Clean Architecture多模块改造

项目采用Clean Architecture多模块架构：
```
:domain/        # 纯Kotlin - 业务模型、UseCase
:data/          # Android Library - Room、Retrofit、Repository实现
:presentation/  # Android Library - Compose UI、ViewModel
:app/           # Application - 应用入口、Android服务
```

### 模块依赖关系

```
                    ┌─────────────┐
                    │    :app     │
                    │ (Application)│
                    └──────┬──────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
           ▼               ▼               ▼
    ┌──────────┐    ┌──────────┐    ┌──────────────┐
    │  :data   │    │ :domain  │    │:presentation │
    │(Library) │    │ (Kotlin) │    │  (Library)   │
    └────┬─────┘    └──────────┘    └──────┬───────┘
         │               ▲                  │
         └───────────────┴──────────────────┘
```

## 常用命令

```bash
# 快捷脚本（推荐）
scripts\quick-build.bat          # 快速构建（跳过lint和测试）
scripts\quick-test.bat           # 运行所有单元测试
scripts\quick-test.bat XxxTest   # 运行指定测试类

# 标准 Gradle 命令
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK
./gradlew testDebugUnitTest      # 单元测试
./gradlew connectedAndroidTest   # 设备测试
./gradlew clean                  # 清理构建
./gradlew --stop                 # 停止 Daemon（释放内存）

# 模块级构建命令
./gradlew :domain:build          # 构建domain模块（纯Kotlin）
./gradlew :data:assembleDebug    # 构建data模块
./gradlew :presentation:assembleDebug  # 构建presentation模块

# 运行特定Bug测试
./gradlew :presentation:test --tests "*BUG00058*"
./gradlew :presentation:test --tests "*SessionManagement*"
```

## 调试命令

### ADB设备管理
```bash
adb devices                      # 查看设备列表
adb install -r app\build\outputs\apk\debug\app-debug.apk  # 安装APK
adb shell am start -n com.empathy.ai/.ui.MainActivity     # 启动应用
adb uninstall com.empathy.ai     # 卸载应用
```

### Logcat调试脚本（推荐）
```bash
# 实时监听日志
scripts\logcat.bat           # 显示WARN及以上（默认）
scripts\logcat.bat -e        # 只看ERROR级别
scripts\logcat.bat -c -e     # 清空日志后只看ERROR
scripts\logcat.bat -f -e     # ERROR日志保存到文件
scripts\logcat.bat -crash    # 只看崩溃日志
scripts\logcat.bat -v        # 显示所有级别
scripts\logcat.bat -h        # 显示帮助

# 快速查看最近错误（一次性获取）
scripts\quick-error.bat      # 获取最近的ERROR日志
scripts\quick-error.bat 100  # 获取更多行
```

### 🆕 AI调试脚本（推荐）
```bash
# AI请求日志过滤（显示Temperature、MaxTokens等关键参数）
scripts\ai-debug.bat              # 实时监听AI日志
scripts\ai-debug.bat -h           # 获取最近100条AI日志
scripts\ai-debug.bat -h -n 200    # 获取最近200条AI日志
scripts\ai-debug.bat -d 127.0.0.1:7555  # 指定MuMu模拟器
scripts\ai-debug.bat -f ai_log.txt     # 输出到文件
scripts\ai-debug.bat --help       # 显示帮助

# 完整AI日志（包含提示词内容）
scripts\ai-debug-full.bat         # 获取完整AI请求日志
scripts\ai-debug-full.bat 127.0.0.1:7555  # 指定设备
```

**日志输出示例：**
```
╔══════════════════════════════════════════════════════════════
║ 🚀 API请求详情 (analyzeChat)
╠══════════════════════════════════════════════════════════════
║ 📍 URL: https://api.example.com/v1/chat/completions
║ 🤖 Model: deepseek-chat
║ 🏢 Provider: MyProvider
╠══════════════════════════════════════════════════════════════
║ ⚙️ 高级参数配置:
║    🌡️ Temperature: 0.7
║    📊 MaxTokens: 4096
╚══════════════════════════════════════════════════════════════
```

### 原生ADB Logcat命令
```bash
adb logcat                   # 查看所有日志
adb logcat *:E               # 只看ERROR
adb logcat -s TAG:V          # 按TAG过滤
adb logcat -c && adb logcat  # 清空后重新开始
adb logcat > logcat.txt      # 保存到文件
```

## 架构规范（必须遵守）

### 代码分层
```
用户操作 → Screen → ViewModel → UseCase → Repository → 数据源
                ↓
            UiState/UiEvent（单向数据流）
```

### 新增功能的标准流程
1. **Domain层**：创建 Model → Repository接口 → UseCase
2. **Data层**：实现 Repository → 配置 DAO/API
3. **Presentation层**：UiState → UiEvent → ViewModel → Screen
4. **DI层**：在对应 Module 中注册依赖

### 命名规范
| 类型 | 命名 | 示例 |
|------|------|------|
| UseCase | 动词+名词+UseCase | `EditFactUseCase` |
| ViewModel | 功能+ViewModel | `ContactDetailViewModel` |
| Screen | 功能+Screen | `ContactDetailScreen` |
| UiState | 功能+UiState | `ContactDetailUiState` |
| UiEvent | 功能+UiEvent | `ContactDetailUiEvent` |
| Repository | 领域+Repository | `ContactRepository` |

## 边界情况检查清单

实现任何功能时，必须考虑：
- [ ] 空值/null 处理
- [ ] 空列表处理
- [ ] 网络错误处理
- [ ] 数据库错误处理
- [ ] 并发/竞态条件
- [ ] 超长文本/边界值
- [ ] 用户取消操作

## 测试要求

- 每个 UseCase 必须有对应的单元测试
- 测试文件命名：`XxxTest.kt`
- 测试方法命名：`` `功能描述_条件_预期结果` ``

## 常见错误模式（避免）

1. **不要**在 ViewModel 中直接调用 Repository（应通过UseCase）
2. **不要**在 Domain 层引入 Android 依赖
3. **不要**忘记处理 Result.failure 情况
4. **不要**在 Composable 中执行耗时操作

## 最新架构规范实践

### 🆕 多模块Clean Architecture (TD-00017已完成)
- **:domain模块**：纯Kotlin，无Android依赖，包含Model、Repository接口、UseCase、Service、Util
  - 业务模型、Repository接口、UseCase、领域服务、工具类
  - 无Android依赖，可独立测试
- **:data模块**：Android Library，包含Room、Retrofit、Repository实现、DI模块
  - Room数据库、Retrofit网络层、Repository实现
  - DI模块配置
- **:presentation模块**：Android Library，包含Compose UI、ViewModel、Navigation
  - UI组件、ViewModel、Navigation系统
  - Theme配置
- **:app模块**：Application，包含应用入口、Android服务、应用级DI模块
  - 应用入口、Android服务
  - DI聚合模块

### DI模块分布
- **:data模块**：DatabaseModule、NetworkModule、RepositoryModule、MemoryModule、PromptModule、DispatcherModule
- **:app模块**：LoggerModule、AppDispatcherModule、ServiceModule、FloatingWindowModule、NotificationModule、SummaryModule、EditModule、PersonaModule、TopicModule、UserProfileModule

### Clean Architecture 层级依赖规则
- **Domain层**：不依赖任何其他层级，纯Kotlin代码
- **Data层**：只依赖Domain层，实现Repository接口
- **Presentation层**：只依赖Domain层，通过ViewModel调用UseCase
- **App层**：聚合所有模块依赖，管理应用级组件

### 提示词系统最佳实践（TD-00015已完成）
- 使用4个核心场景：ANALYZE、POLISH、REPLY、SUMMARY
- 废弃场景（CHECK、EXTRACT）保留代码兼容性，隐藏UI
- GlobalPromptConfig版本管理，当前版本v3
- 使用PromptFileStorage进行文件管理和迁移

### 悬浮窗架构最佳实践
- 使用FloatingWindowServiceV2进行生命周期管理
- 通过SessionContextService实现三种模式上下文共享
- 使用MaxHeightScrollView防止内容过长导致按钮不可见
- 通过FloatingBubbleState管理悬浮球状态显示

### 数据库迁移最佳实践
- 使用Room的Migration API进行增量式迁移
- 导出Schema文件到$projectDir/schemas目录
- 为每个Migration编写单元测试
- 避免使用fallbackToDestructiveMigration()
- **当前数据库版本**: v16 (包含AI军师会话表)

### AI军师模块最佳实践（v16新增）
- 使用AiAdvisorSession管理会话
- 使用AiAdvisorChatViewModel管理对话状态
- 支持Markdown渲染的消息展示
- 支持流式响应和重新生成

## 文档位置

### 快速参考文档（steering目录）
- 产品概览：`.kiro/steering/product.md`
- 技术栈：`.kiro/steering/tech.md`
- 项目结构：`.kiro/steering/structure.md`
- 设置功能：`.kiro/steering/settings-feature.md`
- 当前任务：`WORKSPACE.md`

### 📚 长期文档体系（需要分析时自动读取）

项目的长期文档存放在 `文档/项目文档/` 目录下：

```
文档/项目文档/
├── README.md              # 项目文档总入口
├── domain/                # 领域层（业务模型、接口、用例）
├── data/                  # 数据层（数据库、网络、实现）
├── presentation/          # 表现层（UI、ViewModel）
├── app/                   # 应用层文档
└── di/                    # 依赖注入文档
```

### 自动读取规则

| 场景 | 应读取的文档路径 |
|------|-----------------|
| **理解整体架构** | `文档/项目文档/README.md` |
| **领域层开发** | `文档/项目文档/domain/` |
| **数据层开发** | `文档/项目文档/data/` |
| **UI开发** | `文档/项目文档/presentation/` |
| **理解业务模型** | `文档/项目文档/domain/model/README.md` |
| **理解Repository** | `文档/项目文档/domain/repository/README.md` |
| **理解数据库** | `文档/项目文档/data/local/README.md` |
| **理解API接口** | `文档/项目文档/data/remote/README.md` |

### Bug回归测试

项目维护以下Bug回归测试：

| 测试文件 | 测试内容 |
|----------|----------|
| `BUG00058CreateNewSessionTest.kt` | 新建会话功能测试 |
| `BUG00059RegenerateMessageRoleTest.kt` | 消息重新生成角色测试 |
| `BUG00060SessionManagementTest.kt` | 会话管理增强测试 |
| `BUG00061SessionHistoryNavigationTest.kt` | 会话历史导航测试 |
| `BUG00064ManualSummaryTest.kt` | AI手动总结功能测试 |

运行测试：
```bash
./gradlew :presentation:test --tests "*BUG00058*"
```
