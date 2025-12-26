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
  - 68个业务模型
  - 12个Repository接口
  - 37个UseCase
  - 2个领域服务
  - 28个工具类
- **:data模块**：Android Library，包含Room、Retrofit、Repository实现、DI模块
  - 6个DI模块
  - 7个DAO
  - 7个Entity
  - 10个Repository实现
- **:presentation模块**：Android Library，包含Compose UI、ViewModel、Navigation
  - 180+个UI组件文件
  - 13个ViewModel
- **:app模块**：Application，包含应用入口、Android服务、应用级DI模块
  - 9个应用级DI模块
  - FloatingWindowService
  - EmpathyApplication

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

## 文档位置

- 产品概览：`.kiro/steering/product.md`
- 技术栈：`.kiro/steering/tech.md`
- 项目结构：`.kiro/steering/structure.md`
- 设置功能：`.kiro/steering/settings-feature.md`
- 当前任务：`WORKSPACE.md`