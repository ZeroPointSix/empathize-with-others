[根目录](../CLAUDE.md) > **presentation**

# Presentation 表现层模块

## 模块职责

表现层负责所有 UI 相关的实现，包括 Jetpack Compose 组件、ViewModel、导航和主题配置。该模块使用 Compose 构建声明式 UI，遵循 MVVM 架构模式。

## 入口与启动

### 构建配置 (build.gradle.kts)
- **插件**: android-library, kotlin-android, kotlin-compose, hilt
- **命名空间**: com.empathy.ai.presentation
- **最小SDK**: 24
- **编译SDK**: 35
- **依赖**: domain 模块（使用 api 暴露）

### 关键依赖
- Jetpack Compose BOM 2024.12.01
- Compose Material3
- Compose Navigation
- Hilt Navigation Compose
- Coil Compose（图片加载）
- Paging Compose（分页加载）

## 对外接口

### ViewModel
- `ContactListViewModel` - 联系人列表视图模型
- `ContactDetailViewModel` - 联系人详情视图模型
- `ContactDetailTabViewModel` - 联系人详情标签页视图模型
- `ChatViewModel` - 聊天视图模型
- `SettingsViewModel` - 设置视图模型
- `AiConfigViewModel` - AI 配置视图模型
- `PromptEditorViewModel` - 提示词编辑器视图模型
- `ManualSummaryViewModel` - 手动总结视图模型
- `BrainTagViewModel` - 标签视图模型
- `UserProfileViewModel` - 用户画像视图模型
- `TopicViewModel` - 主题视图模型

### 导航系统
- `NavGraph` - 导航图
- `NavRoutes` - 导航路由
- `ContactDetailNavigation` - 联系人详情导航
- `PromptEditorNavigation` - 提示词编辑器导航

## 关键组件 (UI)

### 屏幕组件 (Screen)
- `ContactListScreen` - 联系人列表
- `ContactDetailScreen` - 联系人详情
- `ChatScreen` - 聊天界面
- `SettingsScreen` - 设置界面
- `AiConfigScreen` - AI 配置界面
- `PromptEditorScreen` - 提示词编辑器
- `UserProfileScreen` - 用户画像

### 悬浮窗组件 (Floating)
- `FloatingViewV2` - 悬浮窗主视图
- `FloatingBubbleView` - 悬浮球视图
- `RefinementOverlay` - 细化覆盖层
- `ResultCard` - 结果卡片
- `TabSwitcher` - 标签切换器

### 卡片组件 (Card)
- `ContactCard` - 联系人卡片
- `AnalysisCard` - 分析卡片
- `ConversationCard` - 对话卡片
- `ProviderCard` - 服务商卡片
- `ProfileCard` - 画像卡片
- `MilestoneCard` - 里程碑卡片
- `PhotoMomentCard` - 照片时刻卡片
- `AiSummaryCard` - AI 总结卡片

### 标签组件 (Chip)
- `TagChip` - 标签芯片
- `SolidTagChip` - 实心标签芯片
- `ConfirmedTag` - 确认标签
- `GuessedTag` - 猜测标签

### 控制组件 (Control)
- `SegmentedControl` - 分段控制器
- `QuickFilterChips` - 快速过滤芯片

### 对话框组件 (Dialog)
- `AddContactDialog` - 添加联系人对话框
- `AddTagDialog` - 添加标签对话框
- `DeleteConfirmDialog` - 删除确认对话框
- `DeleteTagConfirmDialog` - 删除标签确认对话框
- `EditConversationDialog` - 编辑对话对话框
- `EditFactDialog` - 编辑事实对话框
- `EditSummaryDialog` - 编辑总结对话框
- `PermissionRequestDialog` - 权限请求对话框
- `ProviderFormDialog` - 服务商表单对话框
- `TagConfirmationDialog` - 标签确认对话框

### 输入组件 (Input)
- `ContactSearchBar` - 联系人搜索栏
- `TagSearchBar` - 标签搜索栏
- `CustomTextField` - 自定义文本框
- `TopicInputField` - 主题输入框

### 消息组件 (Message)
- `MessageBubble` - 消息气泡
- `ConversationBubble` - 对话气泡

### 情感组件 (Emotion)
- `EmotionalBackground` - 情感背景
- `EmotionalTimelineNode` - 情感时间轴节点
- `GlassmorphicCard` - 玻璃态卡片

### 关系组件 (Relationship)
- `RelationshipScoreSection` - 关系分数区域
- `TrendIcon` - 趋势图标
- `FactItem` - 事实项

### 状态组件 (State)
- `LoadingIndicator` - 加载指示器
- `ErrorView` - 错误视图
- `StatusBadge` - 状态徽章
- `EditedBadge` - 编辑徽章

### 主题组件 (Topic)
- `TopicBadge` - 主题徽章
- `TopicHistorySection` - 主题历史区域
- `TopicSettingDialog` - 主题设置对话框

### 联系人详情组件
- `overview/` - 概览标签页组件
- `persona/` - 人设标签页组件
- `summary/` - 总结标签页组件
- `vault/` - 数据保险库标签页组件
- `factstream/` - 事实流标签页组件

### 设置组件
- `PromptSettingsSection` - 提示词设置区域
- `HistoryConversationCountSection` - 历史对话数量区域

### 提示词编辑器组件
- `PromptEditorTopBar` - 提示词编辑器顶栏
- `PromptInputField` - 提示词输入框
- `CharacterCounter` - 字符计数器
- `DiscardConfirmDialog` - 丢弃确认对话框
- `InlineErrorBanner` - 内联错误横幅

### 其他组件
- `MaxHeightScrollView` - 最大高度滚动视图

## 主题系统 (Theme)

- `Theme.kt` - 主主题定义
- `Color.kt` - 颜色定义
- `Type.kt` - 字体排版
- `CategoryColorPalette.kt` - 分类颜色调色板
- `RelationshipColors.kt` - 关系颜色
- `SemanticColors.kt` - 语义化颜色

## 工具类 (Util)

- `DebugLogger` - 调试日志
- `ErrorMessageMapper` - 错误消息映射器
- `FilterTypeIcons` - 过滤类型图标

## 测试与质量

### 单元测试
- 位置: `presentation/src/test/kotlin/`
- 测试文件数: 22个
- 测试覆盖:
  - ViewModel 测试
  - UI 组件测试
  - 主题测试
  - 导航测试

### UI 测试
- 位置: `presentation/src/androidTest/kotlin/`
- 测试文件数: 5个
- 框架: Compose UI Test
- 覆盖范围: 主要用户流程和界面交互

## 常见问题 (FAQ)

### Q: 如何添加新的 UI 组件？
A:
1. 在 `ui/component/` 对应子目录中创建 Composable 函数
2. 遵循 Material3 设计规范
3. 使用主题中定义的颜色和样式
4. 编写 Compose UI 测试

### Q: ViewModel 如何获取数据？
A: ViewModel 通过 Hilt 注入对应的 UseCase，调用 UseCase 获取数据并转换为 UI State。

### Q: 导航如何工作？
A: 使用 Navigation Compose，在 NavGraph 中定义所有路由，通过 NavRoutes 管理路由常量。

## 相关文件清单

### 核心文件结构
```
presentation/src/main/kotlin/com/empathy/ai/presentation/
├── ui/              # UI组件（187个文件）
│   ├── screen/      # 屏幕组件
│   ├── component/   # 可复用组件
│   └── floating/    # 悬浮窗组件
├── viewmodel/       # ViewModel（18个文件）
├── navigation/      # 导航（4个文件）
├── theme/           # 主题（7个文件）
└── util/            # 工具类（3个文件）
```

### 关键文件
- `navigation/NavGraph.kt` - 导航图
- `navigation/NavRoutes.kt` - 路由定义
- `viewmodel/ContactListViewModel.kt` - 联系人列表视图模型
- `viewmodel/ChatViewModel.kt` - 聊天视图模型
- `theme/Theme.kt` - 主题定义
- `ui/floating/FloatingViewV2.kt` - 悬浮窗主视图

## 变更记录 (Changelog)

### 2025-12-27 - Claude (表现层模块文档初始化)
- 创建 presentation 模块 CLAUDE.md 文档
- 添加导航面包屑
- 整理模块职责和关键组件说明

### 2025-12-20 - Kiro (UI 功能完善)
- 完成联系人详情页多标签页
- 实现悬浮窗 V2 版本
- 添加 iOS 风格组件
- 完善主题系统

---

**最后更新**: 2025-12-27 | 更新者: Claude
**模块状态**: ✅ 完成
**代码质量**: A级（完整注释、Compose 最佳实践）
**测试覆盖**: 包含22个单元测试文件和5个UI测试文件
