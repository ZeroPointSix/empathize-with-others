# Presentation UI Component - 可复用组件模块

[根目录](../../../../../../CLAUDE.md) > [app](../../../../) > [presentation](../../) > [ui](../) > [component](../) > **component**

## 模块职责

Presentation UI Component模块包含所有可复用的UI组件，这些组件遵循Material Design 3设计规范，可在多个屏幕中重复使用。该模块的目的是提高代码复用性、保持UI一致性，并简化复杂UI的构建。

## 组件分类

### 1. 按钮组件 (button)
负责处理用户点击操作的按钮类组件。

#### PrimaryButton
- **文件**: `button/PrimaryButton.kt`
- **职责**: 主要操作按钮
- **功能**:
  - 支持文本和图标
  - 加载状态显示
  - 多种尺寸配置
  - 禁用状态处理

#### SecondaryButton
- **文件**: `button/SecondaryButton.kt`
- **职责**: 次要操作按钮
- **功能**:
  - 样式与主按钮区分
  - 支持相同的功能特性
  - 边框样式设计

### 2. 输入组件 (input)
负责数据输入的表单组件。

#### CustomTextField
- **文件**: `input/CustomTextField.kt`
- **职责**: 自定义文本输入框
- **功能**:
  - 错误状态显示
  - 清空按钮
  - 输入验证提示
  - 多种输入类型支持

#### ContactSearchBar
- **文件**: `input/ContactSearchBar.kt`
- **职责**: 联系人搜索栏
- **功能**:
  - 实时搜索
  - 搜索历史
  - 清空功能
  - 搜索建议

#### TagSearchBar
- **文件**: `input/TagSearchBar.kt`
- **职责**: 标签搜索栏
- **功能**:
  - 标签过滤搜索
  - 标签分类显示
  - 快速选择

### 3. 卡片组件 (card)
用于展示信息块的自定义卡片组件。

#### AnalysisCard
- **文件**: `card/AnalysisCard.kt`
- **职责**: AI分析结果展示
- **功能**:
  - 风险等级指示
  - 分析结果摘要
  - 操作按钮区域
  - 可展开详情

#### ProfileCard
- **文件**: `card/ProfileCard.kt`
- **职责**: 联系人画像卡片
- **功能**:
  - 关系分数显示
  - 核心标签展示
  - 互动统计
  - 最近事实预览

#### ProviderCard
- **文件**: `card/ProviderCard.kt`
- **职责**: AI服务商配置卡片
- **功能**:
  - 服务商信息展示
  - 连接状态指示
  - 配置选项入口
  - 测试按钮

#### PhotoMomentCard
- **文件**: `card/PhotoMomentCard.kt`
- **职责**: 照片时刻卡片
- **功能**:
  - 照片展示
  - 时间信息
  - 相关标签

#### MilestoneCard
- **文件**: `card/MilestoneCard.kt`
- **职责**: 里程碑事件卡片
- **功能**:
  - 事件描述
  - 重要性标记
  - 时间线定位

#### AiSummaryCard
- **文件**: `card/AiSummaryCard.kt`
- **职责**: AI总结卡片
- **功能**:
  - 总结内容展示
  - 来源标识
  - 编辑入口

#### ConversationCard
- **文件**: `card/ConversationCard.kt`
- **职责**: 对话记录卡片
- **功能**:
  - 对话预览
  - 参与者信息
  - 时间戳

### 4. 列表组件 (list)
用于展示列表项的组件。

#### ContactListItem
- **文件**: `list/ContactListItem.kt`
- **职责**: 联系人列表项
- **功能**:
  - 联系人基本信息
  - 标签预览
  - 最后互动时间
  - 点击/长按处理

### 5. 对话框组件 (dialog)
模态对话框类组件。

#### AddContactDialog
- **文件**: `dialog/AddContactDialog.kt`
- **职责**: 添加联系人对话框
- **功能**:
  - 姓名输入
  - 初始标签设置
  - 表单验证

#### AddTagDialog
- **文件**: `dialog/AddTagDialog.kt`
- **职责**: 添加标签对话框
- **功能**:
  - 标签内容输入
  - 类型选择
  - 来源标记

#### DeleteTagConfirmDialog
- **文件**: `dialog/DeleteTagConfirmDialog.kt`
- **职责**: 删除标签确认对话框
- **功能**:
  - 删除警告
  - 影响说明
  - 确认操作

#### PermissionRequestDialog
- **文件**: `dialog/PermissionRequestDialog.kt`
- **职责**: 权限请求对话框
- **功能**:
  - 权限说明
  - 授权引导

#### AddFactToStreamDialog
- **文件**: `dialog/AddFactToStreamDialog.kt`
- **职责**: 添加事实到流对话框
- **功能**:
  - 事实内容输入
  - 时间选择
  - 标签关联

#### EditConversationDialog
- **文件**: `dialog/EditConversationDialog.kt`
- **职责**: 编辑对话对话框
- **功能**:
  - 对话内容编辑
  - 参与者修改
  - 时间调整

#### ProviderFormDialog
- **文件**: `dialog/ProviderFormDialog.kt`
- **职责**: AI服务商表单对话框
- **功能**:
  - 服务商信息填写
  - API配置
  - 测试连接

#### TagConfirmationDialog
- **文件**: `dialog/TagConfirmationDialog.kt`
- **职责**: 标签确认对话框
- **功能**:
  - 标签内容展示
  - 确认/驳回选择

### 6. 消息组件 (message)
聊天消息展示组件。

#### MessageBubble
- **文件**: `message/MessageBubble.kt`
- **职责**: 基础消息气泡
- **功能**:
  - 发送者区分
  - 消息内容展示
  - 时间戳显示

#### ConversationBubble
- **文件**: `message/ConversationBubble.kt`
- **职责**: 对话气泡
- **功能**:
  - 多轮对话展示
  - 上下文关联
  - 折叠/展开

### 7. 芯片组件 (chip)
标签和过滤用的芯片组件。

#### TagChip
- **文件**: `chip/TagChip.kt`
- **职责**: 通用标签芯片
- **功能**:
  - 标签文本展示
  - 可选样式
  - 关闭功能

#### SolidTagChip
- **文件**: `chip/SolidTagChip.kt`
- **职责**: 实心标签芯片
- **功能**:
  - 背景色填充
  - 高对比度显示

#### ConfirmedTag
- **文件**: `chip/ConfirmedTag.kt`
- **职责**: 已确认标签
- **功能**:
  - 确认状态指示
  - 不可编辑状态

#### GuessedTag
- **文件**: `chip/GuessedTag.kt`
- **职责**: AI推测标签
- **功能**:
  - 推测状态标识
  - 确认操作入口

### 8. 状态组件 (state)
展示不同状态的特殊组件。

#### LoadingIndicator
- **文件**: `state/LoadingIndicator.kt`
- **职责**: 加载指示器
- **功能**:
  - 多种加载样式
  - 全屏/内联模式
  - 自定义颜色

#### EmptyView
- **文件**: `state/EmptyView.kt`
- **职责**: 空状态视图
- **功能**:
  - 友好的空状态提示
  - 操作引导
  - 图标展示

#### ErrorView
- **文件**: `state/ErrorView.kt`
- **职责**: 错误状态视图
- **功能**:
  - 错误信息展示
  - 重试操作
  - 错误分类

#### StatusBadge
- **文件**: `state/StatusBadge.kt`
- **职责**: 状态徽章
- **功能**:
  - 状态类型展示
  - 颜色编码
  - 小巧设计

### 9. 关系组件 (relationship)
展示关系相关的特殊组件。

#### TrendIcon
- **文件**: `relationship/TrendIcon.kt`
- **职责**: 趋势图标
- **功能**:
  - 上升/下降/持平
  - 动画效果
  - 颜色编码

#### RelationshipScoreSection
- **文件**: `relationship/RelationshipScoreSection.kt`
- **职责**: 关系分数展示区
- **功能**:
  - 分数可视化
  - 历史趋势
  - 分段展示

#### FactItem
- **文件**: `relationship/FactItem.kt`
- **职责**: 事实条目
- **功能**:
  - 事实内容展示
  - 来源信息
  - 时间戳

### 10. 情感化组件 (emotion)
提供情感化体验的特殊组件。

#### EmotionalBackground
- **文件**: `emotion/EmotionalBackground.kt`
- **职责**: 情感化背景
- **功能**:
  - 动态渐变
  - 情感关联颜色
  - 柔和过渡

#### GlassmorphicCard
- **文件**: `emotion/GlassmorphicCard.kt`
- **职责**: 玻璃拟态卡片
- **功能**:
  - 毛玻璃效果
  - 半透明背景
  - 模糊处理

#### EmotionalTimelineNode
- **文件**: `emotion/EmotionalTimelineNode.kt`
- **职责**: 情感时间线节点
- **功能**:
  - 情感值展示
  - 节点连接线
  - 交互效果

### 11. 控制组件 (control)
特殊控制类组件。

#### SegmentedControl
- **文件**: `control/SegmentedControl.kt`
- **职责**: 分段控制器
- **功能**:
  - 多选项切换
  - 平滑动画
  - 自适应宽度

#### QuickFilterChips
- **文件**: `control/QuickFilterChips.kt`
- **职责**: 快速过滤芯片组
- **功能**:
  - 多选支持
  - 清除所有
  - 展开/收起

### 12. 工具组件 (utils)
通用的工具类组件。

#### MaxHeightScrollView
- **文件**: `MaxHeightScrollView.kt`
- **职责**: 最大高度滚动视图
- **功能**:
  - 限制最大高度
  - 超出可滚动
  - 内容自适应

## 组件设计原则

### 1. 可复用性
- 参数化配置
- 避免硬编码
- 灵活的组合

### 2. 一致性
- 统一的视觉风格
- 一致的交互模式
- 标准的状态处理

### 3. 可访问性
- 语义描述支持
- 内容描述
- 键盘导航支持

### 4. 性能优化
- 使用remember缓存
- 避免不必要重组
- 懒加载机制

## 使用示例

### 基础按钮使用
```kotlin
PrimaryButton(
    text = "保存",
    onClick = { onSave() },
    loading = isLoading,
    icon = Icons.Default.Check
)
```

### 自定义输入框
```kotlin
CustomTextField(
    value = text,
    onValueChange = { text = it },
    label = "联系人姓名",
    isError = hasError,
    errorMessage = if (hasError) "姓名不能为空" else null
)
```

### 对话框使用
```kotlin
AddContactDialog(
    visible = showDialog,
    onDismiss = { showDialog = false },
    onConfirm = { name, tags -> addContact(name, tags) }
)
```

## 测试策略

### 组件测试
- 使用ComposeTestRule
- 测试所有交互状态
- 验证可访问性

### 预览支持
- 提供@Preview函数
- 展示不同状态
- 支持暗色模式

## 文档维护

每个组件都应该包含：
1. 清晰的KDoc注释
2. 参数说明
3. 使用示例
4. 预览函数

## 相关文件清单

- `button/` - 按钮组件
- `input/` - 输入组件
- `card/` - 卡片组件
- `list/` - 列表组件
- `dialog/` - 对话框组件
- `message/` - 消息组件
- `chip/` - 芯片组件
- `state/` - 状态组件
- `relationship/` - 关系组件
- `emotion/` - 情感化组件
- `control/` - 控制组件
- `utils/` - 工具组件

## 变更记录 (Changelog)

### 2025-12-20 - Claude (模块文档创建)
- **创建presentation/ui/component模块文档**
- **记录所有可复用组件的功能和分类**
- **提供组件设计原则和使用示例**