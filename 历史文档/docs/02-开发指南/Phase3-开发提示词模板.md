# Phase 3 开发提示词模板

**文档版本**: v1.0  
**创建日期**: 2025-12-05  
**用途**: 为AI助手提供清晰的Phase 3开发指令

---

## 📋 给AI助手的提示词

### 基础版提示词 (推荐)

```
你好！我需要继续开发共情AI助手项目的UI层Phase 3阶段。

## 当前状态
- ✅ Phase 1 (基础设施) 已完成
- ✅ Phase 2 (可复用组件) 已完成
- 主题系统、导航系统、10个可复用组件都已就绪
- 现在需要开始 Phase 3: 核心Screen阶段

## 任务要求
请根据以下文档开发Phase 3的核心Screen：
1. 阅读 `docs/01-架构设计/UI层/Phase3-核心Screen阶段.md`
2. 阅读 `docs/02-开发指南/UI层开发总体协调计划.md`
3. 阅读 `docs/02-开发指南/Phase2-可复用组件完成总结.md` (了解可用组件)

## 开发规范
- 所有文档和回答使用中文
- 代码注释、变量名、类名使用英文
- 遵循项目的Clean Architecture + MVVM架构
- 使用Jetpack Compose和Material Design 3
- 每个Screen必须有@Preview
- 支持深色模式
- 使用已完成的可复用组件

## 优先级
按以下顺序开发Screen：
1. ContactListScreen (联系人列表页) - P0
2. ContactDetailScreen (联系人详情页) - P0
3. ChatScreen (聊天分析页) - P0
4. BrainTagScreen (标签管理页) - P1

## 可用资源
### 已完成的可复用组件
- LoadingIndicator (加载指示器)
- ErrorView (错误视图)
- EmptyView (空状态视图)
- CustomTextField (输入框)
- PrimaryButton (主按钮)
- SecondaryButton (次要按钮)
- ContactListItem (联系人列表项)
- AnalysisCard (分析结果卡片)
- ProfileCard (档案卡片)
- TagChip (标签芯片)

### 已完成的ViewModel
- ContactListViewModel (联系人列表状态管理)
- ContactDetailViewModel (联系人详情状态管理)
- ChatViewModel (聊天分析状态管理)

### 导航系统
- NavRoutes.kt (路由定义)
- NavGraph.kt (导航图)

## 开发流程
1. 创建Screen目录结构
2. 逐个开发Screen，每个Screen完成后使用getDiagnostics检查
3. 每完成一个Screen，更新每日检查清单
4. 完成Phase 3后，更新CLAUDE.md和OVERVIEW.md

请开始Phase 3的开发工作，从ContactListScreen开始。
```

---

### 详细版提示词 (包含更多上下文)

```
你好！我需要继续开发共情AI助手Android项目的UI层Phase 3阶段。

## 📊 项目背景

### 项目信息
- **项目名称**: 共情AI助手 (Empathy AI Assistant)
- **技术栈**: Kotlin + Jetpack Compose + Room + Retrofit + Hilt
- **架构**: Clean Architecture + MVVM
- **当前版本**: v1.0.2-dev
- **整体完成度**: 85%

### 已完成工作
✅ Domain Layer (100%) - 业务逻辑层
✅ Data Layer (100%) - 数据访问层
✅ Presentation - ViewModel (100%) - 状态管理
✅ Presentation - Phase 1 (100%) - 基础设施
✅ Presentation - Phase 2 (100%) - 可复用组件（10个组件）

### 当前任务
🔄 Presentation - Phase 3 (0%) - 核心Screen阶段

---

## 🎯 Phase 3 开发目标

使用Phase 2的可复用组件，组装完整的业务页面，实现3个核心功能模块。

### 核心交付物 (按优先级排序)

#### 1️⃣ ContactListScreen - 联系人列表页 (P0)

**功能需求**:
- 显示所有联系人列表
- 支持下拉刷新
- 支持搜索功能
- 点击列表项跳转到详情页
- 显示空状态（无联系人时）
- 显示加载状态
- 显示错误状态

**使用的组件**:
- ContactListItem (列表项)
- EmptyView (空状态)
- LoadingIndicator (加载状态)
- ErrorView (错误状态)
- CustomTextField (搜索框)

**ViewModel**: ContactListViewModel
- uiState: ContactListUiState
- onEvent: ContactListUiEvent

**预计耗时**: 4小时

---

#### 2️⃣ ContactDetailScreen - 联系人详情页 (P0)

**功能需求**:
- 显示联系人完整档案
- 支持编辑模式切换
- 支持保存修改
- 支持删除联系人
- 显示关联的标签列表
- 支持添加/删除标签
- 显示加载/错误状态

**使用的组件**:
- ProfileCard (档案卡片)
- TagChip (标签芯片)
- CustomTextField (编辑输入)
- PrimaryButton (保存按钮)
- SecondaryButton (取消/删除按钮)
- LoadingIndicator (加载状态)
- ErrorView (错误状态)

**ViewModel**: ContactDetailViewModel
- uiState: ContactDetailUiState
- onEvent: ContactDetailUiEvent

**导航参数**: contactId (String)

**预计耗时**: 6小时

---

#### 3️⃣ ChatScreen - 聊天分析页 (P0)

**功能需求**:
- 选择要分析的联系人
- 输入待发送的消息
- 显示AI分析结果
- 支持复制话术建议
- 显示风险等级
- 显示加载/错误状态

**使用的组件**:
- CustomTextField (消息输入)
- AnalysisCard (分析结果)
- PrimaryButton (分析按钮)
- LoadingIndicator (分析中)
- ErrorView (分析失败)

**ViewModel**: ChatViewModel
- uiState: ChatUiState
- onEvent: ChatUiEvent

**预计耗时**: 8小时

---

#### 4️⃣ BrainTagScreen - 标签管理页 (P1)

**功能需求**:
- 显示所有标签列表
- 支持添加新标签
- 支持编辑标签
- 支持删除标签
- 按类型分组显示（雷区/策略）
- 支持搜索过滤

**使用的组件**:
- TagChip (标签芯片)
- CustomTextField (输入/搜索)
- PrimaryButton (添加按钮)
- EmptyView (空状态)
- LoadingIndicator (加载状态)

**ViewModel**: 需要创建 BrainTagViewModel

**预计耗时**: 4小时

---

## 📁 目录结构

请创建以下目录结构：

```
app/src/main/java/com/empathy/ai/presentation/ui/screen/
├── contact/
│   ├── ContactListScreen.kt
│   └── ContactDetailScreen.kt
├── chat/
│   └── ChatScreen.kt
└── tag/
    └── BrainTagScreen.kt
```

---

## 📖 必读文档

### 核心文档 (必须阅读)
1. **Phase 3 设计文档**
   - `docs/01-架构设计/UI层/Phase3-核心Screen阶段.md`

2. **总体协调计划**
   - `docs/02-开发指南/UI层开发总体协调计划.md`

3. **Phase 2 完成总结**
   - `docs/02-开发指南/Phase2-可复用组件完成总结.md`

4. **开发规范**
   - `.kiro/steering/structure.md` - 项目结构规范
   - `.kiro/steering/tech.md` - 技术栈说明
   - `.kiro/steering/product.md` - 产品概览

### 参考文档
5. **ViewModel实现**
   - `app/src/main/java/com/empathy/ai/presentation/viewmodel/ContactListViewModel.kt`
   - `app/src/main/java/com/empathy/ai/presentation/viewmodel/ContactDetailViewModel.kt`
   - `app/src/main/java/com/empathy/ai/presentation/viewmodel/ChatViewModel.kt`

6. **可复用组件**
   - `app/src/main/java/com/empathy/ai/presentation/ui/component/`

7. **导航系统**
   - `app/src/main/java/com/empathy/ai/presentation/navigation/NavRoutes.kt`
   - `app/src/main/java/com/empathy/ai/presentation/navigation/NavGraph.kt`

---

## 🎨 Screen开发规范

### 每个Screen必须包含：

1. **KDoc注释**
```kotlin
/**
 * 联系人列表页面
 * 
 * 显示所有联系人列表，支持搜索和下拉刷新
 * 
 * @param navController 导航控制器
 * @param viewModel 联系人列表ViewModel
 */
@Composable
fun ContactListScreen(...)
```

2. **参数设计**
- navController: NavController (用于页面跳转)
- viewModel: ViewModel (通过Hilt注入)
- modifier: Modifier = Modifier

3. **状态收集**
```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

4. **事件处理**
```kotlin
fun onEvent(event: UiEvent) {
    viewModel.onEvent(event)
}
```

5. **Preview示例**
```kotlin
@Preview(name = "默认状态", showBackground = true)
@Preview(name = "深色模式", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContactListScreenPreview() {
    EmpathyTheme {
        // Preview内容
    }
}
```

---

## ✅ 开发流程

### Step 1: 创建目录结构
```bash
mkdir -p app/src/main/java/com/empathy/ai/presentation/ui/screen/{contact,chat,tag}
```

### Step 2: 按优先级开发Screen

对于每个Screen：

1. **创建文件**
   - 在对应目录下创建Kotlin文件

2. **实现Screen**
   - 编写Composable函数
   - 添加完整的KDoc注释
   - 实现所有必需功能
   - 使用可复用组件

3. **状态管理**
   - 收集ViewModel的uiState
   - 处理各种UI状态（加载/成功/错误/空）
   - 实现事件处理

4. **导航集成**
   - 处理页面跳转
   - 传递导航参数
   - 处理返回逻辑

5. **添加Preview**
   - 至少2个Preview (浅色/深色)
   - 展示不同状态

6. **代码检查**
   - 使用 `getDiagnostics` 检查代码
   - 确保无错误和警告

7. **更新文档**
   - 更新每日检查清单
   - 记录完成情况

### Step 3: Phase 3完成后

1. **更新进度文档**
   - 更新 `CLAUDE.md`
   - 更新 `docs/00-项目概述/OVERVIEW.md`
   - 更新 `docs/02-开发指南/UI层开发每日检查清单.md`

2. **创建完成总结**
   - 创建 `docs/02-开发指南/Phase3-核心Screen完成总结.md`

3. **验收检查**
   - 所有P0 Screen完成
   - 所有Screen有Preview
   - 支持深色模式
   - 代码通过诊断
   - 导航流程正常

---

## 🚨 注意事项

### 必须遵守的规则

1. **语言规范**
   - ✅ 所有文档和回答使用中文
   - ✅ 代码注释、变量名、类名使用英文
   - ✅ KDoc注释使用中文

2. **代码规范**
   - ✅ 使用 `collectAsStateWithLifecycle()` 收集Flow
   - ✅ Composable函数使用PascalCase命名
   - ✅ 参数使用camelCase命名
   - ✅ 使用Hilt注入ViewModel

3. **架构规范**
   - ✅ Screen只负责UI展示
   - ✅ 业务逻辑在ViewModel中
   - ✅ 使用UiState管理状态
   - ✅ 使用UiEvent处理事件

4. **组件使用**
   - ✅ 优先使用Phase 2的可复用组件
   - ✅ 不要重复造轮子
   - ✅ 保持UI一致性

5. **质量规范**
   - ✅ 每个Screen必须有@Preview
   - ✅ 每个Screen必须有KDoc注释
   - ✅ 使用getDiagnostics检查代码
   - ✅ 无编译错误和警告

### 常见错误避免

❌ **错误示例**:
```kotlin
// 直接在Screen中调用Repository
val contacts = contactRepository.getAllProfiles()

// 没有处理加载状态
LazyColumn {
    items(contacts) { contact ->
        ContactListItem(contact)
    }
}

// 缺少Preview
@Composable
fun ContactListScreen() { ... }
```

✅ **正确示例**:
```kotlin
/**
 * 联系人列表页面
 */
@Composable
fun ContactListScreen(
    navController: NavController,
    viewModel: ContactListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    when {
        uiState.isLoading -> LoadingIndicator()
        uiState.error != null -> ErrorView(
            message = uiState.error!!,
            onRetry = { viewModel.onEvent(ContactListUiEvent.Refresh) }
        )
        uiState.contacts.isEmpty() -> EmptyView(
            message = "还没有联系人",
            actionText = "添加联系人",
            onAction = { /* 添加逻辑 */ }
        )
        else -> ContactList(
            contacts = uiState.contacts,
            onContactClick = { contact ->
                navController.navigate(NavRoutes.ContactDetail.createRoute(contact.id))
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ContactListScreenPreview() {
    EmpathyTheme {
        // Preview内容
    }
}
```

---

## 📊 进度跟踪

### 每完成一个Screen，更新以下内容：

1. **每日检查清单**
   - 文件: `docs/02-开发指南/UI层开发每日检查清单.md`
   - 勾选对应的完成项

2. **开发日志**
   - 在检查清单中记录完成时间
   - 记录遇到的问题和解决方案

3. **进度统计**
   - 更新完成百分比
   - 评估是否符合预期

---

## 🎯 验收标准

Phase 3完成的标准：

### 功能完整性
- [ ] ContactListScreen全部功能完成
- [ ] ContactDetailScreen全部功能完成
- [ ] ChatScreen全部功能完成
- [ ] BrainTagScreen基本功能完成
- [ ] 所有导航流程正常

### 代码质量
- [ ] 所有Screen有完整的KDoc注释
- [ ] 所有Screen有至少2个Preview
- [ ] 所有Screen支持深色模式
- [ ] 所有文件通过getDiagnostics检查
- [ ] 无编译错误和警告

### 状态处理
- [ ] 正确处理加载状态
- [ ] 正确处理错误状态
- [ ] 正确处理空状态
- [ ] 正确处理成功状态

### 文档完整性
- [ ] 更新了CLAUDE.md
- [ ] 更新了OVERVIEW.md
- [ ] 更新了每日检查清单
- [ ] 创建了Phase3完成总结

---

## 🚀 开始开发

请按照以下步骤开始：

1. **确认理解**
   - 阅读上述所有内容
   - 确认已理解开发要求

2. **阅读文档**
   - 阅读Phase3设计文档
   - 阅读总体协调计划
   - 了解可用组件

3. **创建目录**
   - 创建screen目录结构

4. **开始开发**
   - 从ContactListScreen开始
   - 按优先级逐个完成

5. **持续更新**
   - 每完成一个Screen更新文档
   - 遇到问题及时记录

---

## 💡 提示

- 如果遇到技术问题，可以参考已完成的ViewModel代码
- 如果不确定组件用法，可以查看组件的Preview示例
- 保持代码简洁，遵循KISS原则
- 优先完成功能，后续可以优化
- 充分利用已完成的可复用组件

---

**准备好了吗？让我们开始Phase 3的开发吧！**
```

---

## 🔄 快速启动版提示词 (极简版)

如果AI助手已经熟悉项目，可以使用这个极简版：

```
继续开发共情AI助手项目。

当前状态: Phase 1和Phase 2完成，需要开发Phase 3核心Screen。

任务:
1. 阅读 docs/01-架构设计/UI层/Phase3-核心Screen阶段.md
2. 创建 presentation/ui/screen/ 目录结构
3. 按优先级开发Screen: ContactListScreen, ContactDetailScreen, ChatScreen, BrainTagScreen
4. 使用Phase 2的可复用组件
5. 每个Screen必须有@Preview和KDoc
6. 完成后更新CLAUDE.md和每日检查清单

可用组件: LoadingIndicator, ErrorView, EmptyView, CustomTextField, PrimaryButton, SecondaryButton, ContactListItem, AnalysisCard, ProfileCard, TagChip

开始吧！
```

---

## 📝 使用说明

### 选择合适的提示词版本

1. **基础版** - 适合对项目有一定了解的AI
2. **详细版** - 适合首次接手Phase 3的AI，包含完整上下文
3. **快速启动版** - 适合已经熟悉项目的AI，快速继续开发

### 提示词使用技巧

1. **复制粘贴**: 直接复制对应版本的提示词
2. **适当调整**: 根据实际情况调整优先级或要求
3. **分步执行**: 如果任务量大，可以分多次对话完成
4. **持续反馈**: 在开发过程中提供反馈和调整

### 常见问题

**Q: AI没有按照要求开发怎么办？**
A: 重新强调关键要求，特别是"必须遵守的规则"部分

**Q: AI跳过了某些步骤怎么办？**
A: 明确指出缺失的步骤，要求补充完成

**Q: 如何确保代码质量？**
A: 每完成一个Screen就要求使用getDiagnostics检查

**Q: 如何确保使用可复用组件？**
A: 在提示词中明确列出可用组件，要求优先使用

---

## 🎯 Phase 3 vs Phase 2 的区别

### Phase 2 (可复用组件)
- 目标: 创建独立的UI组件
- 特点: 无状态、可复用、通用
- 示例: Button、TextField、Card

### Phase 3 (核心Screen)
- 目标: 组装完整的业务页面
- 特点: 有状态、业务相关、特定功能
- 示例: ContactListScreen、ChatScreen

### 关键差异
- Phase 2组件是"积木"，Phase 3是用积木搭建"房子"
- Phase 2关注UI，Phase 3关注业务流程
- Phase 2无ViewModel，Phase 3需要ViewModel
- Phase 2组件独立，Phase 3需要导航

---

**文档版本**: v1.0  
**创建日期**: 2025-12-05  
**维护者**: AI Assistant  
**适用场景**: Phase 3开发交接
