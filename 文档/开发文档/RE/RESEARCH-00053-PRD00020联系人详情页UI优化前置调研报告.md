# RESEARCH-00053-PRD00020联系人详情页UI优化前置调研报告

## 文档信息

| 项目 | 内容 |
|------|------|
| 文档编号 | RESEARCH-00053 |
| 创建日期 | 2025-12-25 |
| 调研人 | Kiro |
| 状态 | 调研完成 |
| 调研目的 | 为PRD-00020联系人详情页UI优化提供前置技术调研 |
| 关联任务 | PRD-00020、FD-00020（待创建） |

---

## 1. 调研范围

### 1.1 调研主题
联系人详情页UI优化，包括5个页面的完全重写：
1. 概览页（OverviewTab）
2. 事实流页（FactStreamTab）
3. 画像库页（PersonaTab）
4. 资料库页（DataVaultTab）
5. 新建联系人页（CreateContactScreen）

### 1.2 关注重点
- 现有代码架构和组件结构
- 设计稿与现有实现的差距分析
- 需要新增/修改/删除的组件
- 技术实现难点和风险

### 1.3 关联文档

| 文档类型 | 文档编号 | 文档名称 |
|----------|----------|----------|
| PRD | PRD-00020 | 联系人详情页优化 |
| 设计稿 | - | 文档/开发文档/UI-原型/PRD20/联系人详情/*.html |
| 参考FD | FD-00019 | UI视觉美观化改造功能设计 |

---

## 2. 代码现状分析

### 2.1 相关文件清单

| 文件路径 | 类型 | 行数 | 说明 |
|----------|------|------|------|
| `presentation/.../contact/ContactDetailTabScreen.kt` | 主容器 | ~870 | 四标签页主屏幕 |
| `presentation/.../contact/DetailTab.kt` | 枚举 | ~35 | 标签页枚举定义 |
| `presentation/.../contact/ContactDetailUiState.kt` | 状态 | ~100 | UI状态定义 |
| `presentation/.../contact/ContactDetailUiEvent.kt` | 事件 | ~80 | UI事件定义 |
| `presentation/.../contact/overview/OverviewTab.kt` | 组件 | ~250 | 概览标签页 |
| `presentation/.../contact/overview/DynamicEmotionalHeader.kt` | 组件 | ~200 | 动态情感头部 |
| `presentation/.../contact/overview/TopTagsSection.kt` | 组件 | ~100 | 核心标签区域 |
| `presentation/.../contact/overview/LatestFactHookCard.kt` | 组件 | ~80 | 最新动态卡片 |
| `presentation/.../contact/factstream/FactStreamTab.kt` | 组件 | ~300 | 事实流标签页 |
| `presentation/.../contact/factstream/TimelineView.kt` | 组件 | ~200 | 时光轴视图 |
| `presentation/.../contact/factstream/ListView.kt` | 组件 | ~150 | 清单视图 |
| `presentation/.../contact/factstream/ListViewRow.kt` | 组件 | ~100 | 清单行组件 |
| `presentation/.../contact/factstream/FactStreamTopBar.kt` | 组件 | ~80 | 事实流顶栏 |
| `presentation/.../contact/persona/PersonaTab.kt` | 组件 | ~250 | 画像标签页 |
| `presentation/.../contact/persona/PersonaTabV2.kt` | 组件 | ~300 | 画像标签页V2 |
| `presentation/.../contact/persona/CategorySection.kt` | 组件 | ~150 | 分类区域 |
| `presentation/.../contact/persona/DynamicCategoryCard.kt` | 组件 | ~200 | 动态分类卡片 |
| `presentation/.../contact/persona/SelectableTagChip.kt` | 组件 | ~100 | 可选标签胶囊 |
| `presentation/.../contact/vault/DataVaultTab.kt` | 组件 | ~200 | 资料库标签页 |
| `presentation/.../contact/vault/DataSourceCard.kt` | 组件 | ~150 | 数据来源卡片 |
| `presentation/.../contact/ContactListScreen.kt` | 屏幕 | ~400 | 联系人列表（含新建入口） |

### 2.2 核心类/接口分析

#### ContactDetailTabScreen
- **文件位置**: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/contact/ContactDetailTabScreen.kt`
- **职责**: 四标签页主容器，管理标签切换和状态
- **关键方法**: 
  - `ContactDetailTabScreen()` - 有状态版本
  - `ContactDetailTabScreenContent()` - 无状态内容
- **依赖关系**: 
  - 依赖: ContactDetailTabViewModel, ManualSummaryViewModel
  - 被依赖: NavGraph

#### DetailTab枚举
- **文件位置**: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/contact/DetailTab.kt`
- **职责**: 定义四个标签页类型
- **枚举值**: Overview, FactStream, Persona, DataVault

#### OverviewTab
- **文件位置**: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/contact/overview/OverviewTab.kt`
- **职责**: 概览标签页，展示关系概况
- **子组件**: DynamicEmotionalHeader, TopTagsSection, LatestFactHookCard, RelationshipScoreCard, CustomPromptCard

#### FactStreamTab
- **文件位置**: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/contact/factstream/FactStreamTab.kt`
- **职责**: 事实流标签页，展示时间线和对话历史
- **子组件**: FactStreamTopBar, TimelineView, ListView

#### PersonaTab / PersonaTabV2
- **文件位置**: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/contact/persona/`
- **职责**: 画像标签页，管理联系人标签
- **子组件**: CategorySection, DynamicCategoryCard, SelectableTagChip, BatchActionBar

#### DataVaultTab
- **文件位置**: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/contact/vault/DataVaultTab.kt`
- **职责**: 资料库标签页，展示数据源和处理状态
- **子组件**: DataSourceCard

### 2.3 数据流分析

```
用户操作 → ContactDetailUiEvent → ContactDetailTabViewModel → ContactDetailUiState → UI更新
                                          ↓
                                    Repository层
                                          ↓
                                    Room数据库
```

### 2.4 当前实现状态

| 功能点 | 状态 | 说明 |
|--------|------|------|
| 四标签页切换 | ✅ 已实现 | 使用PrimaryTabRow |
| 概览页基础布局 | ✅ 已实现 | 但样式与设计稿差距大 |
| 事实流时光轴 | ✅ 已实现 | 但样式与设计稿差距大 |
| 事实流清单视图 | ✅ 已实现 | 但样式与设计稿差距大 |
| 画像标签管理 | ✅ 已实现 | 但样式与设计稿差距大 |
| 资料库数据展示 | ✅ 已实现 | 但样式与设计稿差距大 |
| iOS分段控制器 | ❌ 未实现 | 设计稿要求使用iOS风格 |
| Apple Health风格圆环 | ❌ 未实现 | 设计稿要求双层圆环+Sparkline |
| 情绪节点时光轴 | ⚠️ 部分实现 | 需要重新设计样式 |
| 分类卡片左侧色条 | ❌ 未实现 | 设计稿要求的新样式 |
| AI推测区域 | ⚠️ 部分实现 | 需要重新设计交互 |

---

## 3. 设计稿与现有实现差距分析

### 3.1 概览页差距

| 设计稿要求 | 现有实现 | 差距程度 | 改造方案 |
|------------|----------|----------|----------|
| iOS分段控制器 | PrimaryTabRow | 🔴 高 | 新建IOSSegmentedControl组件 |
| 微信名片式个人信息 | 简单头像+文字 | 🔴 高 | 重写个人信息卡片 |
| Apple Health双层圆环 | 简单数字展示 | 🔴 高 | 新建HealthRingChart组件 |
| Sparkline趋势图 | 无 | 🔴 高 | 新建SparklineChart组件 |
| Emoji+马卡龙色标签 | 普通标签 | 🟡 中 | 修改TopTagsSection |
| 微信朋友圈式动态卡片 | 简单卡片 | 🟡 中 | 修改LatestFactHookCard |
| iOS设置项式专属指令 | 简单按钮 | 🟡 中 | 修改CustomPromptCard |

### 3.2 事实流页差距

| 设计稿要求 | 现有实现 | 差距程度 | 改造方案 |
|------------|----------|----------|----------|
| 情绪节点+连接线时光轴 | 简单列表 | 🔴 高 | 重写TimelineView |
| 6种情绪类型颜色 | 部分实现 | 🟡 中 | 完善EmotionType颜色映射 |
| 筛选胶囊 | 简单筛选 | 🟡 中 | 新建FilterChip组件 |
| AI日总结卡片 | 部分实现 | 🟡 中 | 优化样式 |
| 手动总结悬浮按钮 | ✅ 已实现 | 🟢 低 | 微调样式 |
| 清单视图日期分组 | 部分实现 | 🟡 中 | 优化分组样式 |

### 3.3 画像库页差距

| 设计稿要求 | 现有实现 | 差距程度 | 改造方案 |
|------------|----------|----------|----------|
| 分类卡片左侧色条 | 无 | 🔴 高 | 新建CategoryCard组件 |
| 莫兰迪色系标签胶囊 | 普通标签 | 🟡 中 | 修改TagChip样式 |
| 雷区标签带图标描述 | 简单列表 | 🔴 高 | 新建RiskTagItem组件 |
| 策略标签带图标描述 | 简单列表 | 🔴 高 | 新建StrategyTagItem组件 |
| AI推测区域列表样式 | 部分实现 | 🟡 中 | 优化AiInferenceSection |
| 折叠/展开动画 | 无 | 🟡 中 | 添加动画效果 |

### 3.4 资料库页差距

| 设计稿要求 | 现有实现 | 差距程度 | 改造方案 |
|------------|----------|----------|----------|
| 数据统计卡片装饰背景 | 简单卡片 | 🟡 中 | 优化StatisticsCard |
| 2列数据网格 | 部分实现 | 🟢 低 | 微调布局 |
| 状态角标 | 部分实现 | 🟢 低 | 完善StatusBadge |
| 功能说明区域 | 无 | 🟡 中 | 新增提示区域 |

### 3.5 新建联系人页差距

| 设计稿要求 | 现有实现 | 差距程度 | 改造方案 |
|------------|----------|----------|----------|
| iOS导航栏（取消/完成） | 标准导航栏 | 🟡 中 | 修改TopAppBar |
| 圆形头像占位符 | 部分实现 | 🟢 低 | 微调样式 |
| iOS风格表单 | 普通表单 | 🟡 中 | 使用IOSSettingsSection |
| 添加画像事实按钮 | 部分实现 | 🟢 低 | 微调样式 |

---

## 4. 架构合规性分析

### 4.1 层级划分

| 文件 | 所属层级 | 合规性 | 说明 |
|------|----------|--------|------|
| ContactDetailTabScreen.kt | Presentation | ✅ 合规 | UI组件 |
| ContactDetailTabViewModel.kt | Presentation | ✅ 合规 | ViewModel |
| ContactDetailUiState.kt | Presentation | ✅ 合规 | UI状态 |
| ContactDetailUiEvent.kt | Presentation | ✅ 合规 | UI事件 |
| ContactProfile.kt | Domain | ✅ 合规 | 领域模型 |
| ContactRepository.kt | Domain | ✅ 合规 | 仓库接口 |

### 4.2 依赖方向检查

| 源文件 | 依赖目标 | 合规性 | 说明 |
|--------|----------|--------|------|
| ContactDetailTabScreen | Domain模型 | ✅ 合规 | Presentation依赖Domain |
| ContactDetailTabViewModel | UseCase | ✅ 合规 | 通过UseCase访问数据 |
| OverviewTab | Domain模型 | ✅ 合规 | 只依赖领域模型 |

---

## 5. 技术栈分析

### 5.1 使用的依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| Jetpack Compose | BOM 2024.12.01 | UI框架 |
| Material 3 | 1.3.1 | 设计系统 |
| Hilt | 2.52 | 依赖注入 |
| Navigation Compose | 2.8.5 | 导航 |
| Coil | 2.5.0 | 图片加载 |

### 5.2 需要新增的技术实现

| 技术点 | 说明 | 复杂度 |
|--------|------|--------|
| iOS分段控制器 | 自定义Compose组件 | 中 |
| Apple Health圆环图 | Canvas绑制 | 高 |
| Sparkline趋势图 | Canvas绘制 | 中 |
| 情绪节点时光轴 | 自定义布局 | 高 |
| 分类卡片折叠动画 | AnimatedVisibility | 低 |

---

## 6. 问题与风险

### 6.1 🔴 阻塞问题 (P0)

无阻塞问题，所有技术方案均可实现。

### 6.2 🟡 风险问题 (P1)

#### P1-001: Apple Health圆环图实现复杂度
- **问题描述**: 设计稿要求的双层圆环+渐变+圆头末端需要Canvas自定义绑制
- **潜在影响**: 可能需要较长开发时间
- **建议措施**: 参考FD-00019中的实现方案，或使用第三方图表库

#### P1-002: 情绪节点时光轴布局
- **问题描述**: 左侧节点+连接线+右侧卡片的布局需要自定义
- **潜在影响**: 布局复杂，需要处理动态高度
- **建议措施**: 使用Row+Column组合，连接线使用Canvas绘制

#### P1-003: 现有代码重构范围大
- **问题描述**: 需要完全重写5个页面的UI代码
- **潜在影响**: 工作量大，可能引入回归问题
- **建议措施**: 分阶段实施，每个页面独立测试

### 6.3 🟢 优化建议 (P2)

#### P2-001: 组件复用
- **当前状态**: 部分组件可以跨页面复用
- **优化建议**: 提取通用组件到component目录
- **预期收益**: 减少代码重复，提高维护性

#### P2-002: 动画性能
- **当前状态**: 新增大量动画效果
- **优化建议**: 使用remember缓存动画状态
- **预期收益**: 提高动画流畅度

### 6.4 ⚪ 待确认问题

| 编号 | 问题 | 需要确认的内容 |
|------|------|----------------|
| Q-001 | 新建联系人页是否需要单独的Screen | 目前在ContactListScreen中，是否需要独立 |
| Q-002 | 是否需要保留PersonaTabV2 | 存在两个版本，需要确认使用哪个 |

---

## 7. 关键发现总结

### 7.1 核心结论

1. **现有代码架构良好**：Clean Architecture + MVVM模式，层级清晰，依赖方向正确
2. **UI样式差距大**：现有实现与设计稿差距较大，需要完全重写UI组件
3. **功能逻辑可复用**：ViewModel、UseCase、Repository层代码可以复用
4. **新增组件较多**：需要新增约15个UI组件

### 7.2 技术要点

| 要点 | 说明 | 重要程度 |
|------|------|----------|
| iOS分段控制器 | 替代PrimaryTabRow | 高 |
| Apple Health圆环 | Canvas自定义绑制 | 高 |
| 情绪节点时光轴 | 自定义布局+Canvas | 高 |
| 分类卡片色条 | 左侧4px色条 | 中 |
| 莫兰迪色系标签 | 淡色背景+深色文字 | 中 |

### 7.3 注意事项

- ⚠️ 保持现有ViewModel和数据层代码不变
- ⚠️ 新增组件需要添加Preview和单元测试
- ⚠️ 动画效果需要考虑性能影响
- ⚠️ 严格按照设计稿实现，不要自行发挥

---

## 8. 后续任务建议

### 8.1 推荐的任务顺序

1. **FD-00020编写** - 基于本调研报告编写功能设计文档
2. **TDD-00020编写** - 技术设计文档
3. **TD-00020编写** - 任务清单
4. **概览页实现** - 第一阶段
5. **事实流页实现** - 第二阶段
6. **画像库页实现** - 第三阶段
7. **资料库页实现** - 第四阶段
8. **新建联系人页实现** - 第五阶段

### 8.2 预估工作量

| 任务 | 预估时间 | 复杂度 | 依赖 |
|------|----------|--------|------|
| FD-00020 | 2小时 | 中 | 本调研报告 |
| TDD-00020 | 3小时 | 高 | FD-00020 |
| TD-00020 | 1小时 | 低 | TDD-00020 |
| 概览页实现 | 8小时 | 高 | TD-00020 |
| 事实流页实现 | 6小时 | 高 | 概览页 |
| 画像库页实现 | 6小时 | 高 | 事实流页 |
| 资料库页实现 | 4小时 | 中 | 画像库页 |
| 新建联系人页实现 | 3小时 | 中 | 资料库页 |

### 8.3 风险预警

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|----------|
| Canvas绑制复杂度超预期 | 中 | 中 | 预留额外时间 |
| 动画性能问题 | 低 | 中 | 性能测试 |
| 回归问题 | 中 | 高 | 分阶段测试 |

---

## 9. 附录

### 9.1 参考资料

- [PRD-00020-联系人详情页优化](../PRD/PRD-00020-联系人详情页优化.md)
- [FD-00019-UI视觉美观化改造功能设计](../FD/FD-00019-UI视觉美观化改造功能设计.md)
- [设计稿README](../UI-原型/PRD20/联系人详情/README.md)

### 9.2 设计稿文件

| 文件 | 路径 |
|------|------|
| 概览页 | `文档/开发文档/UI-原型/PRD20/联系人详情/概览页.html` |
| 事实流页 | `文档/开发文档/UI-原型/PRD20/联系人详情/事实流页.html` |
| 画像库页 | `文档/开发文档/UI-原型/PRD20/联系人详情/画像库页.html` |
| 资料库页 | `文档/开发文档/UI-原型/PRD20/联系人详情/资料库页.html` |
| 新建联系人页 | `文档/开发文档/UI-原型/PRD20/联系人详情/新建联系人页.html` |

### 9.3 术语表

| 术语 | 解释 |
|------|------|
| iOS分段控制器 | iOS风格的Tab切换组件，带滑块动画 |
| Apple Health圆环 | 苹果健康App风格的双层进度环 |
| Sparkline | 迷你趋势图，用于展示数据变化趋势 |
| 莫兰迪色系 | 低饱和度的柔和色彩体系 |
| 情绪节点 | 时光轴上表示情绪类型的圆形节点 |

---

**文档版本**: 1.0  
**最后更新**: 2025-12-25
