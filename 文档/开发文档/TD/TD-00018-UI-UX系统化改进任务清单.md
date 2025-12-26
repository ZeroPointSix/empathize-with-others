# TD-00018-UI/UX系统化改进任务清单

## 文档信息

| 项目 | 内容 |
|------|------|
| 文档编号 | TD-00018 |
| 版本 | v1.2 |
| 创建日期 | 2025-12-24 |
| 最后更新 | 2025-12-24 |
| 需求文档 | `文档/开发文档/PRD/PRD-00017-UI-UX系统化改进需求.md` |
| 功能设计 | `文档/开发文档/FD/FD-00017-UI-UX系统化改进功能设计.md` |
| 技术设计 | `文档/开发文档/TDD/TDD-00018-UI-UX系统化改进技术设计.md` |
| 调研报告 | `文档/开发文档/RE/RESEARCH-00036-UI-UX系统化改进调研报告.md` |
| 审查报告 | `文档/开发文档/DR/DR-00031-TD00018任务清单文档审查报告.md` |
| 状态 | ✅ 已完成 |
| 负责人 | Kiro |

---

## 当前进度

| 阶段 | 状态 | 完成任务 | 总任务 | 完成率 |
|------|------|----------|--------|--------|
| Phase 1: 统一间距系统 | ✅ 已完成 | 8/8 | 8 | 100% |
| Phase 2: 交互动效系统 | ✅ 已完成 | 12/12 | 12 | 100% |
| Phase 3: 友好错误提示 | ✅ 已完成 | 7/7 | 7 | 100% |
| Phase 4: 空状态设计 | ✅ 已完成 | 8/8 | 8 | 100% |
| **总计** | **✅ 已完成** | **35/35** | **35** | **100%** |

---

## 任务格式说明

- **[P]**: 可并行执行（不同文件，无依赖）
- **[Phase?]**: 所属阶段（Phase1-4）
- **[DEP:Txxx]**: 依赖其他任务完成
- 描述中包含确切的文件路径
- 每个任务包含预估工作量和需求追溯

---

## 路径约定

### 目标文件结构

```
presentation/src/main/kotlin/com/empathy/ai/presentation/
├── theme/
│   ├── Dimensions.kt          # 修改：添加SpacingMediumSmall
│   ├── AnimationSpec.kt       # 修改：添加转场动画规范
│   └── Spacing.kt             # 🆕 新增：AppSpacing类型别名
├── navigation/
│   └── NavGraph.kt            # 修改：添加转场动画配置
├── util/
│   └── ErrorMessageMapper.kt  # 🆕 新增：错误映射器
└── ui/component/
    ├── animation/             # 🆕 新增目录
    │   ├── AnimatedListItem.kt
    │   ├── ClickableScale.kt
    │   └── AnimatedViewSwitch.kt
    └── state/
        ├── EmptyView.kt       # 修改：增强EmptyType
        ├── LoadingSkeleton.kt # 🆕 新增：骨架屏
        └── FriendlyErrorCard.kt # 🆕 新增：错误卡片
```

---

## 任务总览

| 阶段 | 主任务数 | 预估时间 | 风险等级 | 依赖 |
|------|----------|----------|----------|------|
| Phase 1: 统一间距系统 | 8 | 2-3天 | 低 | 无 |
| Phase 2: 交互动效系统 | 12 | 3-4天 | 中 | Phase 1 |
| Phase 3: 友好错误提示 | 7 | 1-2天 | 低 | 无 |
| Phase 4: 空状态设计 | 8 | 1-2天 | 低 | Phase 1 |
| **总计** | **35** | **7-11天** | - | - |

---

## Phase 1: 统一间距系统（2-3天）

**目标**: 扩展现有Dimensions对象，添加缺失的12dp间距，创建AppSpacing类型别名，统一所有页面的间距使用

**关键**: 此阶段完成前不能开始Phase 2和Phase 4

### 1.1 主题层扩展

- [x] T1-01 [Phase1] 扩展Dimensions对象，添加SpacingMediumSmall = 12.dp
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/theme/Dimensions.kt`
  - 在现有间距常量后添加 `val SpacingMediumSmall = 12.dp`
  - 添加KDoc注释说明用途
  - 预估: 0.25天
  - _需求: TDD-00018 1.3节_

- [x] T1-02 [Phase1] 创建Spacing.kt，定义AppSpacing类型别名对象
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/theme/Spacing.kt`
  - 创建AppSpacing对象，包含xs/sm/md/lg/xl/xxl六个间距别名
  - 添加完整的KDoc注释和使用示例
  - 预估: 0.25天
  - _需求: TDD-00018 1.4节_

- [x] T1-03 [Phase1] 编写AppSpacing单元测试
  - 文件: `presentation/src/test/kotlin/com/empathy/ai/presentation/theme/AppSpacingTest.kt`
  - 验证间距值与Dimensions的对应关系
  - 验证各间距值的正确性（4dp/8dp/12dp/16dp/24dp/32dp）
  - 预估: 0.25天
  - _需求: TDD-00018 7.1节_

### 1.2 核心界面迁移

- [x] T1-04 [P] [Phase1] [DEP:T1-02] 更新ContactListScreen，替换硬编码间距
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/contact/ContactListScreen.kt`
  - 将所有硬编码的dp值替换为AppSpacing引用
  - 页面边距使用AppSpacing.lg，列表项间距使用AppSpacing.md
  - 验证: 编译通过，Preview正常显示，无布局错位
  - 预估: 0.5天
  - _需求: TDD-00018 1.6节_

- [x] T1-05 [P] [Phase1] [DEP:T1-02] 更新ContactDetailScreen，替换硬编码间距
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/contact/ContactDetailScreen.kt`
  - 将所有硬编码的dp值替换为AppSpacing引用
  - Section间距使用AppSpacing.xl，卡片内边距使用AppSpacing.lg
  - 验证: 编译通过，Preview正常显示，无布局错位
  - 预估: 0.5天
  - _需求: TDD-00018 1.6节_

- [x] T1-06 [P] [Phase1] [DEP:T1-02] 更新SettingsScreen，替换硬编码间距
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/settings/SettingsScreen.kt`
  - 将所有硬编码的dp值替换为AppSpacing引用
  - 设置项间距使用AppSpacing.md
  - 验证: 编译通过，Preview正常显示，无布局错位
  - 预估: 0.5天
  - _需求: TDD-00018 1.6节_

- [x] T1-07 [P] [Phase1] [DEP:T1-02] 更新ChatScreen，替换硬编码间距
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/chat/ChatScreen.kt`
  - 将所有硬编码的dp值替换为AppSpacing引用
  - 消息间距使用AppSpacing.sm，输入区域边距使用AppSpacing.md
  - 验证: 编译通过，Preview正常显示，无布局错位
  - 预估: 0.5天
  - _需求: TDD-00018 1.6节_

### 1.3 验证

- [x] T1-08 [Phase1] [DEP:T1-04,T1-05,T1-06,T1-07] 视觉验证，检查所有页面间距一致性
  - 在模拟器/真机上检查四个核心页面的间距效果
  - 确保无布局错位或间距异常
  - 记录验证结果
  - 预估: 0.25天
  - _需求: TDD-00018 11.1节_

---

## Phase 2: 交互动效系统（3-4天）

**目标**: 建立全局动画规范，为页面转场、列表操作、按钮点击等交互添加流畅的动画效果

**关键**: 依赖Phase 1完成

### 2.1 动画规范扩展

- [x] T2-01 [Phase2] 扩展AnimationSpec，添加转场动画规范常量
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/theme/AnimationSpec.kt`
  - 添加DurationPageEnter/DurationPageExit/SpringDampingRatio/SpringStiffness常量
  - 添加PageEnterTransition/PageExitTransition/FadeInTransition/FadeOutTransition规范
  - 预估: 0.25天
  - _需求: TDD-00018 2.3节_

- [x] T2-02 [Phase2] [DEP:T2-01] 配置NavHost转场动画
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/navigation/NavGraph.kt`
  - 添加enterTransition/exitTransition/popEnterTransition/popExitTransition配置
  - enterTransition: slideInHorizontally(从右) + fadeIn
  - exitTransition: slideOutHorizontally(向左1/3) + fadeOut
  - popEnterTransition: slideInHorizontally(从左1/3) + fadeIn
  - popExitTransition: slideOutHorizontally(向右) + fadeOut
  - 预估: 0.5天
  - _需求: TDD-00018 2.4节_


### 2.2 动画组件创建

- [x] T2-03 [P] [Phase2] [DEP:T2-01] 创建AnimatedListItem组件
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/animation/AnimatedListItem.kt`
  - 实现列表项淡入淡出 + 高度变化动画
  - 添加Preview函数和KDoc注释
  - 预估: 0.5天
  - _需求: TDD-00018 2.5节_

- [x] T2-04 [P] [Phase2] [DEP:T2-01] 创建ClickableScale组件
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/animation/ClickableScale.kt`
  - 实现按下时的缩放反馈效果（默认0.95f）
  - 使用弹簧动画实现自然弹性
  - 添加Preview函数和KDoc注释
  - 预估: 0.5天
  - _需求: TDD-00018 2.6节_

- [x] T2-05 [P] [Phase2] [DEP:T2-01] 创建AnimatedViewSwitch组件
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/animation/AnimatedViewSwitch.kt`
  - 实现视图模式切换的淡入淡出 + 缩放动画
  - 支持泛型状态参数
  - 添加Preview函数和KDoc注释
  - 预估: 0.5天
  - _需求: TDD-00018 2.7节_

- [x] T2-06 [P] [Phase2] [DEP:T2-01] 创建LoadingSkeleton组件
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/state/LoadingSkeleton.kt`
  - 实现脉冲动画效果的占位符
  - 创建ContactListItemSkeleton和ContactListSkeleton变体
  - 添加Preview函数（包含深色模式）和KDoc注释
  - 预估: 0.5天
  - _需求: TDD-00018 2.8节_

### 2.3 动画组件集成

- [x] T2-07 [Phase2] [DEP:T2-03,T2-04,T2-06] 将动画组件应用到ContactListScreen
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/contact/ContactListScreen.kt`
  - 使用AnimatedListItem包装列表项
  - 使用ClickableScale包装可点击卡片
  - 加载状态使用ContactListSkeleton
  - 预估: 0.5天
  - _需求: TDD-00018 10.2节_

- [x] T2-08 [Phase2] [DEP:T2-03,T2-04] 将动画组件应用到ContactDetailScreen
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/contact/ContactDetailScreen.kt`
  - 使用ClickableScale包装操作按钮
  - 使用AnimatedViewSwitch处理Tab切换
  - 预估: 0.5天
  - _需求: TDD-00018 10.2节_

- [x] T2-09 [Phase2] [DEP:T2-04] 将动画组件应用到SettingsScreen
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/settings/SettingsScreen.kt`
  - 使用ClickableScale包装设置项
  - 预估: 0.25天
  - _需求: TDD-00018 10.2节_

- [x] T2-10 [Phase2] [DEP:T2-04] 将动画组件应用到ChatScreen
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/chat/ChatScreen.kt`
  - 使用ClickableScale包装发送按钮
  - 预估: 0.25天
  - _需求: TDD-00018 10.2节_

### 2.4 测试与验证

- [x] T2-11 [Phase2] [DEP:T2-03,T2-04,T2-05,T2-06] 编写动画组件UI测试
  - 文件: `presentation/src/androidTest/kotlin/com/empathy/ai/presentation/ui/component/animation/`
  - 创建AnimatedListItemTest.kt、ClickableScaleTest.kt、AnimatedViewSwitchTest.kt测试文件
  - 验证动画触发和状态变化
  - 测试异步动画完成回调
  - 预估: 0.75天
  - _需求: TDD-00018 7.2节_

- [x] T2-12 [Phase2] [DEP:T2-07,T2-08,T2-09,T2-10] 性能测试，确保60fps
  - 在中端设备上测试页面转场和列表滚动
  - 使用Android Profiler检查帧率
  - 确保无明显掉帧
  - 预估: 0.25天
  - _需求: TDD-00018 8.4节_

---

## Phase 3: 友好错误提示（1-2天）

**目标**: 将技术错误转换为用户友好的提示，提供解决方案和操作按钮

**关键**: 可与Phase 1并行执行（Phase级别并行），内部任务按依赖顺序执行

### 3.1 错误映射器

- [x] T3-01 [P] [Phase3] 创建ErrorMessageMapper错误消息映射器
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/util/ErrorMessageMapper.kt`
  - 实现mapError方法，支持UnknownHostException/SocketTimeoutException/HttpException
  - 创建FriendlyErrorMessage/ErrorAction/ErrorActionType数据类和枚举
  - 添加KDoc注释
  - 预估: 0.5天
  - _需求: TDD-00018 3.3节_

- [x] T3-02 [Phase3] [DEP:T3-01] 编写ErrorMessageMapper单元测试
  - 文件: `presentation/src/test/kotlin/com/empathy/ai/presentation/util/ErrorMessageMapperTest.kt`
  - 测试各种异常类型的映射结果
  - 测试HTTP错误码（401/429/5xx）的处理
  - 预估: 0.25天
  - _需求: TDD-00018 7.1节_

### 3.2 错误卡片组件

- [x] T3-03 [Phase3] [DEP:T3-01] 创建FriendlyErrorCard组件
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/state/FriendlyErrorCard.kt`
  - 显示图标、标题、描述和操作按钮
  - 使用errorContainer颜色方案
  - 添加Preview函数（包含深色模式）和KDoc注释
  - 预估: 0.5天
  - _需求: TDD-00018 3.4节_

- [x] T3-04 [Phase3] [DEP:T3-03] 编写FriendlyErrorCard UI测试
  - 文件: `presentation/src/androidTest/kotlin/com/empathy/ai/presentation/ui/component/state/FriendlyErrorCardTest.kt`
  - 测试错误信息显示
  - 测试操作按钮点击回调
  - 预估: 0.25天
  - _需求: TDD-00018 7.2节_

### 3.3 错误处理集成

- [x] T3-05 [Phase3] [DEP:T3-03] 将FriendlyErrorCard集成到ContactListScreen
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/contact/ContactListScreen.kt`
  - 替换现有的错误处理逻辑
  - 使用ErrorMessageMapper转换异常
  - 预估: 0.25天
  - _需求: TDD-00018 10.2节_

- [x] T3-06 [Phase3] [DEP:T3-03] 将FriendlyErrorCard集成到ChatScreen
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/chat/ChatScreen.kt`
  - 替换现有的错误处理逻辑
  - 处理AI调用失败的错误提示
  - 预估: 0.25天
  - _需求: TDD-00018 10.2节_

- [x] T3-07 [Phase3] [DEP:T3-03] 将FriendlyErrorCard集成到AiConfigScreen
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/aiconfig/AiConfigScreen.kt`
  - 处理API连接测试失败的错误提示
  - 预估: 0.25天
  - _需求: TDD-00018 10.2节_


---

## Phase 4: 空状态设计（1-2天）

**目标**: 增强现有EmptyView组件和EmptyType密封类，添加情感化设计元素

**关键**: 依赖Phase 1完成（使用AppSpacing）

### 4.1 EmptyType增强

- [x] T4-01 [Phase4] [DEP:T1-02] 增强EmptyType密封类
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/state/EmptyView.kt`
  - 为EmptyType添加description和actionText属性
  - 新增NetworkError类型
  - 保持向后兼容
  - 预估: 0.25天
  - _需求: TDD-00018 4.3节_

- [x] T4-02 [Phase4] [DEP:T4-01] 增强EmptyView组件，添加呼吸动画
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/state/EmptyView.kt`
  - 为图标添加呼吸动画效果（scale 0.95f-1.05f）
  - 使用AppSpacing统一间距
  - 添加操作按钮支持
  - 保留旧版API兼容性重载
  - 预估: 0.5天
  - _需求: TDD-00018 4.3节_

- [x] T4-03 [Phase4] [DEP:T4-02] 更新EmptyView Preview函数
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/state/EmptyView.kt`
  - 添加NoContacts/NoTags/NoResults/NetworkError四种预览
  - 添加深色模式预览
  - 预估: 0.25天
  - _需求: TDD-00018 4.3节_

### 4.2 测试

- [x] T4-04 [Phase4] [DEP:T4-02] 编写EmptyView UI测试
  - 文件: `presentation/src/androidTest/kotlin/com/empathy/ai/presentation/ui/component/state/EmptyViewTest.kt`
  - 测试标题和描述显示
  - 测试操作按钮显示和点击
  - 预估: 0.25天
  - _需求: TDD-00018 7.2节_

### 4.3 空状态集成

- [x] T4-05 [Phase4] [DEP:T4-02] 更新ContactListScreen使用增强空状态
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/contact/ContactListScreen.kt`
  - 使用EmptyType.NoContacts替换现有空状态
  - 添加"添加联系人"操作按钮
  - 预估: 0.25天
  - _需求: TDD-00018 10.2节_

- [x] T4-06 [Phase4] [DEP:T4-02] 更新ContactDetailTabScreen使用增强空状态
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/contact/ContactDetailTabScreen.kt`
  - 为事实流、标签等Tab使用对应的EmptyType
  - 预估: 0.25天
  - _需求: TDD-00018 10.2节_

- [x] T4-07 [Phase4] [DEP:T4-02] 更新BrainTagScreen使用增强空状态
  - 文件: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/tag/BrainTagScreen.kt`
  - 使用EmptyType.NoTags替换现有空状态
  - 添加"添加标签"操作按钮
  - 预估: 0.25天
  - _需求: TDD-00018 10.2节_

### 4.4 验证

- [x] T4-08 [Phase4] [DEP:T4-05,T4-06,T4-07] 视觉验证，检查所有空状态一致性
  - 在模拟器/真机上检查各页面的空状态效果
  - 确保呼吸动画流畅
  - 确保操作按钮功能正常
  - 预估: 0.25天
  - _需求: TDD-00018 11.1节_

---

## 验收标准

### 功能验收

| 验收项 | 标准 | 优先级 | 关联任务 |
|--------|------|--------|----------|
| 间距统一 | 所有页面使用AppSpacing，无硬编码 | 高 | T1-04~T1-07 |
| 页面转场 | 所有导航有平滑的滑入滑出动画 | 高 | T2-02 |
| 列表动画 | 列表项增删有淡入淡出动画 | 高 | T2-03,T2-07 |
| 按钮反馈 | 按钮点击有缩放反馈 | 高 | T2-04,T2-07~T2-10 |
| 错误提示 | 所有错误显示友好提示 | 中 | T3-05~T3-07 |
| 空状态 | 所有空状态有图标、标题、描述、操作 | 中 | T4-05~T4-07 |

### 性能验收

| 验收项 | 标准 | 关联任务 |
|--------|------|----------|
| 动画帧率 | 稳定60fps，无掉帧 | T2-12 |
| 页面渲染 | 首屏渲染 < 500ms | T2-12 |
| 列表滚动 | 流畅无卡顿 | T2-12 |

### 代码质量验收

| 验收项 | 标准 | 关联任务 |
|--------|------|----------|
| Preview | 所有新增组件有完整Preview | T2-03~T2-06,T3-03,T4-03 |
| 注释 | 所有新增代码有KDoc注释 | 全部 |
| 测试 | 关键组件有单元测试，覆盖率>80% | T1-03,T2-11,T3-02,T3-04,T4-04 |

---

## 风险与缓解

| 风险 | 概率 | 影响 | 缓解措施 | 关联任务 |
|------|------|------|----------|----------|
| 间距修改导致布局问题 | 中 | 中 | 逐页面验证，充分测试 | T1-08 |
| 动画导致性能问题 | 低 | 高 | 使用硬件加速，监控帧率 | T2-12 |
| 旧版API兼容性问题 | 低 | 中 | 保留旧版重载函数 | T4-02 |
| 工作量超出预期 | 中 | 中 | 分阶段实施，优先核心功能 | - |

---

**文档版本**: 1.2
**最后更新**: 2025-12-24
**更新内容**: 根据DR-00032审查报告优化任务描述，补充验证步骤和动画配置细节
