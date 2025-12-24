# Presentation UI Screen - 屏幕组件模块

[根目录](../../../../../../CLAUDE.md) > [app](../../../../) > [presentation](../../) > [ui](../) > [screen](../) > **screen**

## 模块职责

Presentation UI Screen模块包含应用的所有屏幕页面组件，每个屏幕对应一个独立的功能界面。该模块遵循MVVM架构模式，使用Jetpack Compose构建声明式UI，并通过ViewModel管理状态和业务逻辑。

## 屏幕架构

### 通用架构模式
每个屏幕都遵循以下架构模式：
```
Screen Composable
    ├── UiState (状态管理)
    ├── UiEvent (事件处理)
    ├── ViewModel (业务逻辑)
    └── Sub-components (子组件)
```

## 核心屏幕组件

### 1. 联系人管理 (Contact)

#### ContactListScreen
- **文件**: `contact/ContactListScreen.kt`
- **职责**: 联系人列表展示和管理
- **功能**:
  - 显示所有联系人列表
  - 支持实时搜索过滤
  - 下拉刷新数据
  - 导航到联系人详情
  - 导航到设置页面

**状态管理**:
- `ContactListUiState.kt` - 列表状态定义
- `ContactListUiEvent.kt` - 用户事件定义

#### ContactDetailScreen
- **文件**: `contact/ContactDetailScreen.kt`
- **职责**: 联系人详情页面（旧版）

#### ContactDetailTabScreen
- **文件**: `contact/ContactDetailTabScreen.kt`
- **职责**: 联系人详情标签页（新版）
- **标签页设计**:
  - Overview - 概览信息
  - FactStream - 事实流时间线
  - Persona - 标签画像
  - Vault - 资料库

**状态管理**:
- `ContactDetailUiState.kt` - 详情状态
- `ContactDetailUiEvent.kt` - 详情事件

**子组件**:
- `DetailTab.kt` - 标签页切换器
- `overview/` - 概览标签页组件
- `factstream/` - 事实流标签页组件
- `persona/` - 画像标签页组件
- `vault/` - 资料库标签页组件
- `summary/` - 总结功能组件

### 2. AI对话 (Chat)

#### ChatScreen
- **文件**: `chat/ChatScreen.kt`
- **职责**: AI对话分析界面
- **功能**:
  - 输入聊天内容
  - 显示AI分析结果
  - 提供润色和回复建议
  - 支持三Tab模式（分析/润色/回复）

**状态管理**:
- `ChatUiState.kt` - 对话状态
- `ChatUiEvent.kt` - 对话事件

### 3. 标签管理 (Tag)

#### BrainTagScreen
- **文件**: `tag/BrainTagScreen.kt`
- **职责**: 标签管理界面
- **功能**:
  - 管理所有标签
  - 标签分类显示
  - 批量操作支持
  - 标签搜索过滤

**状态管理**:
- `BrainTagUiState.kt` - 标签状态
- `BrainTagUiEvent.kt` - 标签事件

### 4. 设置配置 (Settings)

#### SettingsScreen
- **文件**: `settings/SettingsScreen.kt`
- **职责**: 应用设置界面
- **功能**:
  - API配置
  - 权限管理
  - 数据管理
  - 应用偏好

**状态管理**:
- `SettingsUiState.kt` - 设置状态
- `SettingsUiEvent.kt` - 设置事件

**子组件**:
- `HistoryConversationCountSection.kt` - 历史对话统计

#### AiConfigScreen
- **文件**: `aiconfig/AiConfigScreen.kt`
- **职责**: AI服务商配置界面
- **功能**:
  - 添加/编辑AI服务商
  - 测试连接
  - 模型配置

**状态管理**:
- `AiConfigUiState.kt` - AI配置状态
- `AiConfigUiEvent.kt` - AI配置事件

### 5. 提示词编辑 (Prompt)

#### PromptEditorScreen
- **文件**: `prompt/PromptEditorScreen.kt`
- **职责**: 提示词编辑器
- **功能**:
  - 创建/编辑提示词
  - 实时字符计数
  - 语法验证
  - 预览功能

**状态管理**:
- `PromptEditorUiState.kt` - 编辑器状态
- `PromptEditorUiEvent.kt` - 编辑器事件
- `PromptEditorResult.kt` - 编辑结果
- `PromptEditMode.kt` - 编辑模式

**子组件**:
- `component/` - 编辑器专用组件
  - `PromptEditorTopBar.kt` - 顶部工具栏
  - `PromptInputField.kt` - 输入框
  - `CharacterCounter.kt` - 字符计数器
  - `InlineErrorBanner.kt` - 错误提示
  - `DiscardConfirmDialog.kt` - 丢弃确认

## 标签页子组件详情

### Overview标签页 (contact/overview/)
- `OverviewTab.kt` - 主容器
- `DynamicEmotionalHeader.kt` - 情感化头部
- `TopTagsSection.kt` - 顶部标签
- `LatestFactHookCard.kt` - 最新事实卡片

### FactStream标签页 (contact/factstream/)
- `FactStreamTab.kt` - 主容器
- `FactStreamTopBar.kt` - 顶部栏
- `ListView.kt` - 列表视图
- `ListViewRow.kt` - 列表项
- `TimelineView.kt` - 时间线视图

### Persona标签页 (contact/persona/)
- `PersonaTab.kt` - 主容器
- `CategorySection.kt` - 分类区域

### Vault标签页 (contact/vault/)
- `VaultTab.kt` - 主容器
- `DataSourceCard.kt` - 数据源卡片

### Summary功能 (contact/summary/)
- `ManualSummaryFab.kt` - 手动总结按钮
- `QuickDateOptions.kt` - 快速日期选择
- `DateRangePickerDialog.kt` - 日期范围选择
- `ConflictResolutionDialog.kt` - 冲突解决
- `SummaryProgressDialog.kt` - 总结进度
- `SummaryResultDialog.kt` - 总结结果
- `SummaryErrorDialog.kt` - 总结错误
- `RangeWarningDialog.kt` - 范围警告
- `MissingSummaryCard.kt` - 缺失总结卡片
- `SummaryDetailDialog.kt` - 总结详情
- `SummarySourceBadge.kt` - 总结来源标识

## 状态管理模式

### UiState设计原则
```kotlin
data class ScreenUiState(
    val isLoading: Boolean = false,
    val data: T? = null,
    val error: String? = null,
    // 其他特定状态...
)
```

### UiEvent设计原则
```kotlin
sealed class ScreenUiEvent {
    data class OnDataChanged(val data: T) : ScreenUiEvent()
    object OnRefresh : ScreenUiEvent()
    object OnErrorDismissed : ScreenUiEvent()
    // 其他事件...
}
```

## 导航集成

### 参数传递
- 使用NavController处理页面跳转
- 通过导航参数传递数据
- 支持深度链接

### 导航回调
- 每个屏幕接收必要的导航回调
- 保持UI层的状态无关性
- 导航逻辑在ViewModel中决策

## 性能优化

### 懒加载
- 使用LazyColumn处理大列表
- remember缓存计算结果
- derivedStateOf优化重组

### 状态提升
- 状态尽可能提升到合适层级
- 避免不必要的状态传递
- 使用单向数据流

## 测试策略

### UI测试
- 使用Compose UI Test框架
- 测试关键用户流程
- 验证状态转换

### 组件测试
- 独立测试子组件
- 使用preview预览功能
- 模拟ViewModel状态

## 可访问性

### 语义支持
- 为所有交互元素添加语义描述
- 支持内容描述（contentDescription）
- 正确使用头部和标题层级

### 辅助技术
- 支持TalkBack
- 提供大字体支持
- 确保足够的对比度

## 设计模式

### MVVM模式
- View (Screen Composable)
- ViewModel (业务逻辑)
- Model (Domain Model)

### 单一职责
- 每个屏幕负责单一功能
- 子组件职责明确
- 避免过度复杂

### 组合优于继承
- 使用组合构建UI
- 参数化配置组件
- 提高代码复用性

## 常见问题解决

### 1. 状态管理
- 使用collectAsStateWithLifecycle
- 避免在Composable中直接修改状态
- 保持状态的不可变性

### 2. 生命周期
- 正确处理配置变更
- 使用ViewModel保持状态
- 及时清理资源

### 3. 性能问题
- 避免不必要的重组
- 使用key优化列表项
- 缓存昂贵的计算

## 相关文件清单

### 屏幕组件
- `contact/` - 联系人相关屏幕
- `chat/` - 对话屏幕
- `tag/` - 标签管理屏幕
- `settings/` - 设置屏幕
- `aiconfig/` - AI配置屏幕
- `prompt/` - 提示词编辑器

### 状态定义
- 各屏幕的`UiState.kt`文件
- 各屏幕的`UiEvent.kt`文件

### 子组件
- 各屏幕目录下的子组件和工具类

## 变更记录 (Changelog)

### 2025-12-20 - Claude (模块文档创建)
- **创建presentation/ui/screen模块文档**
- **记录所有屏幕组件的功能和结构**
- **整理状态管理模式和设计原则**