# RE-00020-HTML本地化处理指南

> 本地化专项指南 | 创建日期: 2025-12-25 | 版本: 1.0

## 文档目的

本文档为PRD-00020联系人详情页HTML原型的本地化处理提供详细的功能映射和实现指南。

---

## 1. 页面总览

### 1.1 五个页面与Kotlin代码映射

| 序号 | HTML原型文件 | Kotlin主组件 | 数据来源 |
|------|-------------|-------------|----------|
| 1 | 概览页.html | OverviewTab.kt | ContactDetailUiState |
| 2 | 事实流页.html | FactStreamTab.kt | timelineItems, filteredTimelineItems |
| 3 | 画像库页.html | PersonaTabV2.kt | factCategories, brainTags |
| 4 | 资料库页.html | DataVaultTab.kt | conversationCount, summaryCount |
| 5 | 新建联系人页.html | ContactDetailScreen.kt | name, targetGoal, facts |

### 1.2 共享组件

| 组件 | 文件位置 | 功能 |
|------|----------|------|
| 标签页导航 | ContactDetailTabScreen.kt | DetailTab枚举切换 |
| 顶部导航栏 | DynamicEmotionalHeader.kt | 返回、编辑、主题按钮 |
| 底部导航 | 无（使用系统导航） | - |

---

## 2. 概览页 (Overview) 本地化指南

### 2.1 功能点映射表

| HTML元素 | CSS类/ID | Kotlin组件 | 数据字段 | 事件处理 |
|----------|----------|-----------|----------|----------|
| 返回按钮 | `#btn-back` | DynamicEmotionalHeader | - | onBackClick |
| 主题按钮 | `#btn-topic` | DynamicEmotionalHeader | - | ShowTopicDialog |
| 编辑按钮 | `#btn-edit` | DynamicEmotionalHeader | - | StartEditContactInfo |
| 头像区域 | `#btn-avatar` | DynamicEmotionalHeader | contact.avatarUrl | - |
| 姓名 | `.text-[34px]` | DynamicEmotionalHeader | contact.name | - |
| 相识天数 | `.text-blue-600` | DynamicEmotionalHeader | daysSinceFirstMet | - |
| 关系分数卡片 | `#card-health` | RelationshipScoreCard | relationshipScore | - |
| 核心标签卡片 | `#card-tags` | TopTagsSection | topTags: List<Fact> | - |
| 最新动态卡片 | `#card-discovery` | LatestFactHookCard | latestFact | SwitchTab(FactStream) |
| 专属指令卡片 | `#card-instruction` | CustomPromptCard | contact.name | 导航到提示词编辑器 |
| 攻略目标卡片 | `#card-goal` | OverviewTab | contact.targetGoal | - |

### 2.2 本地化字符串

```kotlin
// strings.xml 需要添加的字符串
<string name="overview_relationship_score">关系分数</string>
<string name="overview_score_excellent">关系非常亲密，继续保持！</string>
<string name="overview_score_good">关系良好，有进一步发展的空间</string>
<string name="overview_score_normal">关系一般，需要更多互动</string>
<string name="overview_score_cold">关系较冷淡，建议主动联系</string>
<string name="overview_core_tags">核心标签</string>
<string name="overview_latest_discovery">最新动态</string>
<string name="overview_custom_prompt">专属指令</string>
<string name="overview_target_goal">攻略目标</string>
<string name="overview_days_since_met">已相识 %d 天</string>
```

### 2.3 关键计算逻辑

```kotlin
// 分数描述计算 - RelationshipScoreCard.kt
fun getScoreDescription(score: Int): String = when {
    score >= 81 -> "关系非常亲密，继续保持！"
    score >= 61 -> "关系良好，有进一步发展的空间"
    score >= 31 -> "关系一般，需要更多互动"
    else -> "关系较冷淡，建议主动联系"
}

// 相识天数计算 - 需要在ContactProfile中添加createdAt字段
val daysSinceFirstMet: Int = ChronoUnit.DAYS.between(contact.createdAt, LocalDate.now()).toInt()
```

---

## 3. 事实流页 (FactStream) 本地化指南

### 3.1 功能点映射表

| HTML元素 | CSS类/ID | Kotlin组件 | 数据字段 | 事件处理 |
|----------|----------|-----------|----------|----------|
| 视图切换 | `.segment-control` | FactStreamTopBar | viewMode | SwitchViewMode |
| 筛选标签 | 筛选区域 | FactStreamTopBar | selectedFilters | ToggleFilter |
| 添加按钮 | 顶部add按钮 | FactStreamTopBar | - | ShowAddFactToStreamDialog |
| 时光轴视图 | `<main>` | TimelineView | filteredTimelineItems | - |
| 对话卡片 | `.emotion-sweet` | TimelineItem.Conversation | ConversationLog | EditConversation |
| AI总结卡片 | `.border-l-purple-400` | TimelineItem.AiSummary | DailySummary | StartEditSummary |
| 用户事实卡片 | `.bg-teal-50` | TimelineItem.UserFact | Fact | StartEditFact |
| 手动总结FAB | 底部悬浮按钮 | ManualSummaryFab | - | ShowDatePicker |

### 3.2 本地化字符串

```kotlin
// strings.xml 需要添加的字符串
<string name="factstream_timeline">时光轴</string>
<string name="factstream_list">清单</string>
<string name="factstream_filter_all">全部</string>
<string name="factstream_filter_sweet">💕 甜蜜</string>
<string name="factstream_filter_conflict">⚡ 冲突</string>
<string name="factstream_filter_date">📅 约会</string>
<string name="factstream_filter_gift">🎁 礼物</string>
<string name="factstream_filter_ai_summary">🤖 AI总结</string>
<string name="factstream_ai_suggestion">AI建议：</string>
<string name="factstream_ai_reminder">AI提醒：</string>
<string name="factstream_manual_add">手动添加</string>
<string name="factstream_load_more">下拉加载更多...</string>
```

### 3.3 情绪类型枚举映射

```kotlin
// EmotionType.kt
enum class EmotionType {
    SWEET,      // 甜蜜 - emotion-sweet (粉色渐变)
    CONFLICT,   // 冲突 - emotion-conflict (橙色渐变)
    NEUTRAL,    // 平淡 - emotion-neutral (蓝灰渐变)
    GIFT,       // 礼物 - emotion-gift (金色渐变)
    DATE,       // 约会 - emotion-date (紫色渐变)
    DEEP_TALK   // 深谈 - emotion-deep-talk (青色渐变)
}
```

---

## 4. 画像库页 (Persona) 本地化指南

### 4.1 功能点映射表

| HTML元素 | CSS类/ID | Kotlin组件 | 数据字段 | 事件处理 |
|----------|----------|-----------|----------|----------|
| 搜索栏 | 搜索input | CategorySearchBar | personaSearchState.query | UpdatePersonaSearch |
| 编辑按钮 | 顶部"编辑" | EditModeTopBar | editModeState.isActive | EnterEditMode |
| 分类卡片 | `<section>` | DynamicCategoryCard | factCategories | ToggleCategoryExpand |
| 标签项 | `.px-3.py-1.5` | SelectableTagChip | FactCategory.facts | ToggleFactSelection |
| 雷区标签 | `.border-l-red-500` | RiskTagCard | BrainTag(RISK_RED) | - |
| 策略标签 | `.border-l-emerald-500` | StrategyTagCard | BrainTag(STRATEGY_GREEN) | - |
| AI推测区域 | AI推测section | PendingTagSection | 未确认的Fact | ConfirmTag/RejectTag |
| 批量操作栏 | 底部操作栏 | BatchActionBar | editModeState.selectedCount | ShowBatchDeleteConfirm |
| 添加标签FAB | 底部悬浮按钮 | - | - | ShowAddTagDialog |

### 4.2 本地化字符串

```kotlin
// strings.xml 需要添加的字符串
<string name="persona_search_hint">搜索标签或分类...</string>
<string name="persona_edit">编辑</string>
<string name="persona_done">完成</string>
<string name="persona_tag_count">%d个</string>
<string name="persona_risk_high">高风险</string>
<string name="persona_risk_medium">中风险</string>
<string name="persona_risk_low">低风险</string>
<string name="persona_strategy_recommend">推荐</string>
<string name="persona_ai_infer">AI 推测</string>
<string name="persona_accept_all">全部采纳</string>
<string name="persona_infer_reason">基于最近 %d 次对话推测</string>
<string name="persona_empty_hint">暂无标签，AI会根据对话自动推测</string>
<string name="persona_selected_count">已选择 %d 项</string>
<string name="persona_delete_confirm">确定要删除选中的 %d 个标签吗？此操作不可撤销。</string>
```

### 4.3 分类颜色映射

```kotlin
// 分类颜色 - SolidTagColors.kt
val categoryColors = mapOf(
    "性格特点" to Color(0xFFEC4899),  // pink-500
    "兴趣爱好" to Color(0xFF3B82F6),  // blue-500
    "工作信息" to Color(0xFFF97316),  // orange-500
    "禁忌话题" to Color(0xFFEF4444),  // red-500
    "家庭背景" to Color(0xFF8B5CF6),  // violet-500
    "其他" to Color(0xFF6B7280)       // gray-500
)
```

---

## 5. 资料库页 (DataVault) 本地化指南

### 5.1 功能点映射表

| HTML元素 | CSS类/ID | Kotlin组件 | 数据字段 | 事件处理 |
|----------|----------|-----------|----------|----------|
| 数据统计卡片 | 统计区域 | DataVaultTab | conversationCount + summaryCount | - |
| 聊天记录卡片 | `.text-blue-600` | DataSourceCard | conversationCount | onDataSourceClick |
| AI总结卡片 | `.text-violet-600` | DataSourceCard | summaryCount | onDataSourceClick |
| 图片卡片 | `.text-pink-600` | DataSourceCard | 0 (未实现) | onDataSourceClick |
| 语音卡片 | `.text-cyan-600` | DataSourceCard | 0 (未实现) | onDataSourceClick |
| 视频卡片 | `.text-orange-600` | DataSourceCard | 0 (未实现) | onDataSourceClick |
| 文件卡片 | `.text-emerald-600` | DataSourceCard | 0 (未实现) | onDataSourceClick |
| 状态角标 | `.w-2.h-2` | StatusBadge | DataStatus | - |

### 5.2 本地化字符串

```kotlin
// strings.xml 需要添加的字符串
<string name="vault_title">数据来源</string>
<string name="vault_subtitle">管理与该联系人相关的所有数据</string>
<string name="vault_total_count">数据总量</string>
<string name="vault_count_unit">条</string>
<string name="vault_chat">聊天记录</string>
<string name="vault_chat_desc">对话历史</string>
<string name="vault_summary">AI 总结</string>
<string name="vault_summary_desc">智能分析</string>
<string name="vault_image">图片</string>
<string name="vault_image_desc">媒体文件</string>
<string name="vault_voice">语音消息</string>
<string name="vault_voice_desc">音频记录</string>
<string name="vault_video">视频</string>
<string name="vault_video_desc">视频动态</string>
<string name="vault_file">文件</string>
<string name="vault_file_desc">其他文档</string>
<string name="vault_status_completed">已完成</string>
<string name="vault_status_processing">处理中</string>
<string name="vault_status_not_available">不可用</string>
<string name="vault_status_failed">失败</string>
<string name="vault_media_coming_soon">媒体功能开发中</string>
<string name="vault_media_coming_soon_desc">图片、语音、视频和文件功能正在开发中，敬请期待。目前仅支持聊天记录和AI总结的管理。</string>
```

### 5.3 数据状态枚举

```kotlin
// DataStatus.kt
enum class DataStatus {
    COMPLETED,      // 已完成 - 绿色 (#34C759)
    PROCESSING,     // 处理中 - 蓝色 (#007AFF)
    NOT_AVAILABLE,  // 不可用 - 灰色 (#8E8E93)
    FAILED          // 失败 - 红色 (#FF3B30)
}
```

---

## 6. 新建联系人页 本地化指南

### 6.1 功能点映射表

| HTML元素 | CSS类/ID | Kotlin组件 | 数据字段 | 事件处理 |
|----------|----------|-----------|----------|----------|
| 取消按钮 | 左上角 | ContactDetailScreen | - | NavigateBack |
| 完成按钮 | 右上角 | ContactDetailScreen | canSave | SaveContact |
| 头像区域 | 头像占位符 | ContactDetailScreen | - | 选择照片 |
| 姓名输入 | 姓名input | ContactDetailScreen | name | UpdateName |
| 沟通目标输入 | 目标input | ContactDetailScreen | targetGoal | UpdateTargetGoal |
| 添加画像事实 | 绿色按钮 | ContactDetailScreen | - | ShowAddFactDialog |
| 删除联系人 | 红色按钮 | ContactDetailScreen | - | ShowDeleteConfirmDialog |

### 6.2 本地化字符串

```kotlin
// strings.xml 需要添加的字符串
<string name="create_contact_title">新建联系人</string>
<string name="create_contact_cancel">取消</string>
<string name="create_contact_done">完成</string>
<string name="create_contact_add_photo">添加照片</string>
<string name="create_contact_name">姓名</string>
<string name="create_contact_name_hint">输入姓名</string>
<string name="create_contact_goal">沟通目标</string>
<string name="create_contact_goal_hint">例如：建立初步信任</string>
<string name="create_contact_goal_desc">设定明确的沟通目标有助于建立更深层的联系。</string>
<string name="create_contact_add_fact">添加画像事实</string>
<string name="create_contact_delete">删除联系人</string>
<string name="create_contact_name_error">姓名不能为空</string>
```

---

## 7. 通用本地化要点

### 7.1 颜色规范

```kotlin
// Colors.kt - iOS风格颜色
object iOSColors {
    val systemBackground = Color(0xFFF2F2F7)  // 统一背景色
    val cardBackground = Color.White
    val primaryText = Color(0xFF1C1C1E)
    val secondaryText = Color(0xFF8E8E93)
    val separator = Color(0xFFC6C6C8)
    val systemBlue = Color(0xFF007AFF)
    val systemGreen = Color(0xFF34C759)
    val systemOrange = Color(0xFFFF9500)
    val systemRed = Color(0xFFFF3B30)
}
```

### 7.2 间距规范

```kotlin
// Dimensions.kt
object Spacing {
    val pageMargin = 16.dp
    val cardSpacing = 16.dp
    val cardPadding = 16.dp
    val elementSpacing = 8.dp
    val buttonHeight = 44.dp  // iOS触摸标准
}
```

### 7.3 字体规范

```kotlin
// Typography.kt
val iOSTypography = Typography(
    displayLarge = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal),
    labelSmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal)
)
```

---

## 8. 本地化实施检查清单

### 8.1 概览页
- [ ] 关系分数环形图表实现
- [ ] 分数描述本地化
- [ ] 相识天数计算逻辑
- [ ] 核心标签颜色映射
- [ ] 最新动态图标映射

### 8.2 事实流页
- [ ] 视图切换动画
- [ ] 筛选标签本地化
- [ ] 情绪类型颜色映射
- [ ] 时间线卡片样式
- [ ] 手动总结FAB

### 8.3 画像库页
- [ ] 搜索防抖实现
- [ ] 分类颜色指示条
- [ ] 编辑模式切换
- [ ] 批量操作栏
- [ ] AI推测确认/驳回

### 8.4 资料库页
- [ ] 数据统计卡片装饰
- [ ] 数据源网格布局
- [ ] 状态角标颜色
- [ ] 媒体功能提示

### 8.5 新建联系人页
- [ ] iOS风格表单
- [ ] 头像占位符
- [ ] 表单验证提示
- [ ] 删除确认对话框

---

**文档版本**: 1.0  
**创建日期**: 2025-12-25  
**作者**: AI Assistant
