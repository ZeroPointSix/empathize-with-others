# 联系人详情页HTML本地化处理

## 任务概述

根据现有Kotlin代码功能修改HTML原型，使其与代码完全对应。

## 完成状态

| 页面 | 状态 | 更新内容 |
|------|------|----------|
| 概览页.html | ✅ 已完成 | 核心标签显示fact.value、最新动态卡片、专属指令卡片、删除攻略目标卡片 |
| 事实流页.html | ✅ 已完成 | FilterType枚举修正（移除GIFT）、EmotionType emoji修正、代码映射注释完善 |
| 画像库页.html | ✅ 已完成 | 代码映射注释完善，添加FactCategory、EditModeState等模型说明 |
| 资料库页.html | ✅ 已完成 | 代码映射注释完善，添加DataStatus枚举和技术债务说明 |
| 新建联系人页.html | ✅ 已完成 | 添加完整代码映射注释，包含ContactDetailScreen相关事件和状态 |

## 关键技术发现

### 1. FilterType枚举（事实流筛选）
```kotlin
enum class FilterType(val displayName: String) {
    ALL("全部"),
    AI_SUMMARY("只看AI"),
    CONFLICT("只看冲突"),
    DATE("只看约会"),
    SWEET("只看甜蜜")
    // 注意：没有GIFT筛选！
}
```

### 2. EmotionType枚举（情绪类型）
```kotlin
enum class EmotionType(val emoji: String, val displayName: String) {
    SWEET("❤️", "甜蜜"),      // 不是💕
    CONFLICT("⛈️", "冲突"),   // 不是⚡
    GIFT("🎁", "礼物"),
    DATE("🍽️", "约会"),       // 不是📅
    DEEP_TALK("💬", "深聊"),
    NEUTRAL("⭕", "日常")
}
```

### 3. ViewMode枚举（视图模式）
```kotlin
enum class ViewMode(val displayName: String) {
    Timeline("时光轴"),
    List("清单")
}
```

### 4. DataStatus枚举（数据状态）
```kotlin
enum class DataStatus {
    COMPLETED,      // 已完成（绿色）
    PROCESSING,     // 处理中（蓝色）
    FAILED,         // 失败（红色）
    NOT_AVAILABLE   // 不可用（灰色）
}
```

### 5. 新建联系人页面
- 使用ContactDetailScreen.kt，通过contactId判断新建/编辑模式
- contactId为空字符串时为新建模式
- BUG-00017修复：移除脑标签UI，只保留事实录入功能
- 上下文深度(contextDepth)UI已隐藏，使用默认值10

## 代码映射关系

### 事实流页 (FactStreamTab)
| HTML元素 | Kotlin组件 | 说明 |
|----------|-----------|------|
| 视图切换控件 | SegmentedControl | ViewMode.Timeline / ViewMode.List |
| 筛选标签 | QuickFilterChips | FilterType枚举（5种） |
| 时间线卡片 | TimelineView | TimelineItem的5种类型 |
| 添加事实按钮 | onAddFactClick | ShowAddFactToStreamDialog事件 |
| 手动总结FAB | ManualSummaryFab | TD-00011功能 |

### 画像库页 (PersonaTabV2)
| HTML元素 | Kotlin组件 | 说明 |
|----------|-----------|------|
| 搜索栏 | CategorySearchBar | 300ms防抖 |
| 分类卡片 | DynamicCategoryCard | 支持折叠/展开 |
| 编辑模式顶栏 | EditModeTopBar | 选中计数、全选/取消 |
| 批量操作栏 | BatchActionBar | 移动/删除按钮 |
| AI推测标签 | PendingTagCard | 确认/驳回按钮 |

### 资料库页 (DataVaultTab)
| HTML元素 | Kotlin组件 | 说明 |
|----------|-----------|------|
| 数据网格 | LazyVerticalGrid | 2列固定布局 |
| 数据源卡片 | DataSourceCard | 图标+计数+状态 |
| 状态角标 | StatusBadge | DataStatus枚举颜色 |

### 新建联系人页 (ContactDetailScreen)
| HTML元素 | Kotlin组件 | 说明 |
|----------|-----------|------|
| 取消按钮 | NavigateBack | 检查未保存更改 |
| 完成按钮 | SaveContact | canSave验证 |
| 姓名输入 | CustomTextField | UpdateName事件 |
| 沟通目标输入 | CustomTextField | UpdateTargetGoal事件 |
| 添加画像事实 | AddFactToStreamDialog | ShowAddFactDialog事件 |
| 删除联系人 | DeleteConfirmDialog | ShowDeleteConfirmDialog事件 |

## 更新日期

- 2025-12-25: 完成所有5个页面的本地化处理
