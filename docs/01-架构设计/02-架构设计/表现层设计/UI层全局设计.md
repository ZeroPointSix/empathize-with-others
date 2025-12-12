# UI层全局设计

## 一、概述

### 1.1 UI层在Clean Architecture中的定位

UI层(Presentation Layer)是Clean Architecture的**最外层**，负责与用户直接交互。它将业务逻辑的执行结果以可视化的形式呈现给用户，并将用户的操作转化为业务指令传递给服务层。

```
┌─────────────────────────────────────────────┐
│        Presentation Layer (UI层)            │
│  ┌──────────────┐      ┌────────────────┐  │
│  │  ViewModel   │ ───→ │  Composable    │  │
│  │  (状态管理)   │ ←─── │  (UI展示)       │  │
│  └──────┬───────┘      └────────────────┘  │
└─────────┼─────────────────────────────────┘
          │ 调用 UseCase
          ↓
┌─────────────────────────────────────────────┐
│        Domain Layer (服务层)                │
│  ┌──────────┐      ┌─────────────┐         │
│  │ UseCase  │ ───→ │   Service   │         │
│  └────┬─────┘      └─────────────┘         │
└───────┼─────────────────────────────────────┘
        │ 调用 Repository
        ↓
┌─────────────────────────────────────────────┐
│        Data Layer (数据层)                  │
│  ┌──────────┐      ┌─────────────┐         │
│  │Repository│ ───→ │ DataSource  │         │
│  └──────────┘      └─────────────┘         │
└─────────────────────────────────────────────┘
```

### 1.2 UI层的职责

**UI层只做三件事：**

1. **展示状态 (Display State)**：将 ViewModel 的 UiState 渲染为可视化界面
2. **捕获事件 (Capture Events)**：将用户操作(点击、输入等)转化为 UiEvent
3. **导航跳转 (Navigate)**：响应业务结果，跳转到相应页面

**UI层禁止做的事：**

- ❌ **不能**包含业务逻辑(如数据验证、规则判断)
- ❌ **不能**直接访问 Repository 或数据库
- ❌ **不能**直接调用网络请求
- ❌ **不能**处理复杂的数据转换

### 1.3 核心技术栈

| 技术 | 用途 | 版本要求 |
|------|------|---------|
| **Jetpack Compose** | 声明式UI框架 | BOM 2024.02.00+ |
| **Material Design 3** | 设计系统 | 跟随 Compose BOM |
| **ViewModel** | 状态管理 | lifecycle 2.7.0+ |
| **StateFlow** | 响应式数据流 | Kotlin Coroutines |
| **Navigation Compose** | 页面导航 | 2.7.6+ |
| **Hilt** | 依赖注入 | 2.50+ |

---




## 二、板块一：核心界面系统 (Core Screens)

**负责：** 实现应用的主要业务界面，每个界面包含 Screen + ViewModel + UiState + UiEvent。

### 任务1：聊天分析界面 (Chat Analysis Screen)

* **目的**：为用户提供基于 AI 的聊天记录分析功能
* **方法**：
    1. 创建 ChatScreen.kt - 显示消息列表、输入框、分析结果
    2. 创建 ChatViewModel.kt - 注入 AnalyzeChatUseCase 和 CheckDraftUseCase
    3. 定义 ChatUiState - 管理消息、草稿、分析结果状态
    4. 定义 ChatUiEvent - 发送消息、加载聊天、检查草稿等事件
* **AI指令**：
    > 构建聊天分析界面时：Screen 必须是无状态的，通过 collectAsState() 获取状态；用户操作通过 onEvent() 向上传递；ViewModel 只注入 UseCase；使用 viewModelScope.launch 执行异步操作；分析结果以可展开卡片形式展示。

---

### 任务2：联系人管理界面 (Contact Management Screen)

* **目的**：管理联系人档案，包括列表展示和详情编辑
* **方法**：
    1. ContactListScreen.kt - 使用 LazyColumn 展示联系人列表
    2. ContactDetailScreen.kt - 显示档案信息，支持编辑模式
    3. ContactListViewModel.kt - 管理列表状态，处理搜索
    4. ContactDetailViewModel.kt - 处理编辑和保存逻辑
* **AI指令**：
    > 构建联系人界面时：列表使用 LazyColumn 优化性能；搜索功能使用 debounce(300ms) 防抖；详情页支持查看/编辑两种模式；编辑时用 remember 暂存草稿；保存成功后导航回列表并显示 Snackbar。

---

### 任务3：草稿检查界面 (Draft Check Screen)

* **目的**：在发送消息前，检查草稿内容的安全性和合适性
* **方法**：
    1. DraftCheckScreen.kt - 多行文本输入 + 实时检查结果展示
    2. DraftCheckViewModel.kt - 注入 CheckDraftUseCase，实现防抖检查
    3. DraftCheckUiState - 管理草稿文本、检查结果、建议列表
* **AI指令**：
    > 草稿检查界面：使用 LaunchedEffect + delay 实现防抖；检查结果按风险等级显示不同颜色(绿/黄/红)；建议列表可点击快速替换；支持"忽略"和"采纳"操作；检查过程显示 Loading。

---

## 三、板块二：组件系统 (Component System)

**负责：** 构建可复用的 UI 组件库，遵循原子设计原则。

### 任务1：卡片组件 (Card Components)

**MessageCard - 消息卡片**
* **目的**：展示单条聊天消息，区分用户/对方样式
* **方法**：使用 Surface + 条件样式，根据 isUser 参数切换颜色和圆角方向
* **AI指令**：使用 MaterialTheme.colorScheme，支持深色模式；用户消息右下角小圆角，对方消息左下角小圆角。

**ContactCard - 联系人卡片**
* **目的**：在列表中展示联系人概要
* **方法**：Card + Row 布局(头像 + 信息 + 箭头)，支持点击事件
* **AI指令**：头像使用首字母占位；标签以 Chip 展示，最多3个；提供骨架屏状态。

**AnalysisResultCard - 分析结果卡片**
* **目的**：展示 AI 分析结果，支持展开/收起
* **方法**：AnimatedVisibility + 折叠状态管理
* **AI指令**：使用 remember+mutableStateOf 管理展开状态；展开动画用 fadeIn + expandVertically；内容区域显示分析文本和建议列表。

---

### 任务2：输入组件 (Input Components)

**SearchBar - 搜索栏**
* **目的**：提供搜索功能，支持防抖
* **方法**：TextField + 搜索图标 + 清除按钮
* **AI指令**：使用 LaunchedEffect 实现 300ms 防抖；输入时显示清除按钮；支持键盘搜索键触发。

**MessageInput - 消息输入框**
* **目的**：多行文本输入，支持发送按钮
* **方法**：TextField(multiline) + IconButton
* **AI指令**：空内容时禁用发送按钮；支持 Enter 键发送，Shift+Enter 换行；输入时显示字数统计。

**TagInput - 标签输入**
* **目的**：输入和管理标签
* **方法**：FlowRow + Chip + 输入框
* **AI指令**：已添加标签以 Chip 展示，可点击删除；输入框支持自动补全；按 Enter 或逗号添加标签。

---

### 任务3：反馈组件 (Feedback Components)

**LoadingIndicator - 加载指示器**
* **目的**：显示加载状态
* **方法**：CircularProgressIndicator + 可选文本提示
* **AI指令**：支持全屏遮罩和内联两种模式；使用 MaterialTheme 颜色；可自定义提示文本。

**ErrorDialog - 错误对话框**
* **目的**：展示错误信息并提供操作
* **方法**：AlertDialog + 错误消息 + 重试/取消按钮
* **AI指令**：根据错误类型显示不同图标；支持"重试"和"取消"操作；自动记录错误日志。

**SuccessSnackbar - 成功提示**
* **目的**：显示操作成功的轻量级提示
* **方法**：Snackbar + 成功图标 + 消息文本
* **AI指令**：自动 3 秒后消失；支持撤销操作(可选)；使用 Material3 Snackbar 样式。

---

## 四、板块三：状态管理系统 (State Management)

**负责：** 定义和管理 UI 状态，实现单向数据流。

### 任务1：状态定义 (State Definition)

**UiState 数据类设计原则**
1. **必须是 data class**：支持 copy() 方法进行不可变更新
2. **提供默认值**：简化初始化，避免空指针
3. **包含所有 UI 信息**：数据 + 加载状态 + 错误信息
4. **使用 val 而非 var**：保证不可变性

**标准 UiState 模板：**
```kotlin
data class FeatureUiState(
    val data: List<Item> = emptyList(),
    val selectedItem: Item? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val errorType: ErrorType? = null
) {
    val hasData: Boolean get() = data.isNotEmpty()
}
```

**AI指令**：
> 定义 UiState 时：所有属性用 val；提供合理默认值；错误用可空 String；可添加计算属性；避免嵌套过深的数据结构。

---

### 任务2：事件处理 (Event Handling)

**UiEvent 密封类设计原则**
1. **使用 sealed class**：编译时穷举检查
2. **数据事件用 data class**：携带参数
3. **简单事件用 object**：不携带参数
4. **命名清晰**：动词 + 名词，如 SendMessage, LoadChat

**标准 UiEvent 模板：**
```kotlin
sealed class FeatureUiEvent {
    data class Load(val id: String) : FeatureUiEvent()
    data class Update(val item: Item) : FeatureUiEvent()
    data class Delete(val id: String) : FeatureUiEvent()
    object Refresh : FeatureUiEvent()
    object ClearError : FeatureUiEvent()
    object NavigateBack : FeatureUiEvent()
}
```

**AI指令**：
> 定义 UiEvent 时：用 sealed class 而非 interface；数据事件携带必要参数；简单事件用 object；在 ViewModel 用 when 穷举处理。

---

### 任务3：ViewModel 集成 (ViewModel Integration)

**ViewModel + StateFlow 标准模式**

核心要点：
1. 使用 @HiltViewModel 注解
2. 构造函数注入 UseCase
3. 私有 MutableStateFlow + 公开 StateFlow
4. 统一的 onEvent() 方法
5. viewModelScope 执行异步操作

**AI指令**：
> 创建 ViewModel 时：使用 @HiltViewModel 注解；构造函数注入 UseCase；用 MutableStateFlow + asStateFlow() 模式；所有异步操作在 viewModelScope 中；用 update 方法更新状态。

---

## 五、板块四：导航系统 (Navigation System)

**负责：** 管理应用的页面导航和路由。

### 任务1：导航图定义 (NavGraph Definition)

* **目的**：集中管理所有路由和页面关系
* **方法**：
    1. 创建 NavGraph.kt - 定义 NavHost 和所有 composable 路由
    2. 定义 startDestination - 应用启动页
    3. 配置页面转场动画（可选）
* **AI指令**：
    > 创建 NavGraph 时：使用 NavHost 包裹所有路由；每个 composable 传递必要的回调函数；使用 navController.navigate() 进行跳转；用 navController.popBackStack() 返回。

---

### 任务2：路由管理 (Route Management)

* **目的**：类型安全地管理所有路由字符串
* **方法**：
    1. 创建 NavRoutes.kt - 使用 sealed class 定义所有路由
    2. 无参路由直接使用 route 字符串
    3. 带参路由提供 createRoute() 函数
    4. 定义参数名常量
* **AI指令**：
    > 定义路由时：使用 sealed class 而非常量；带参路由用花括号包裹参数名；提供 createRoute 辅助函数；参数名用常量而非硬编码字符串。

---

### 
任务3：参数传递 (Parameter Passing)

* **目的**：在页面间安全地传递数据
* **方法**：
    1. 简单参数（ID、标志位）通过路由参数传递
    2. 复杂对象通过 ViewModel 或 Repository 加载
    3. 使用 SavedStateHandle 保存状态
* **AI指令**：
    > 参数传递规则：只传递 ID 等简单数据；复杂对象在目标页 ViewModel 中加载；使用 navArgument 定义参数类型；可选参数提供 defaultValue。

---

## 六、板块五：主题系统 (Theme System)

**负责：** 管理应用的视觉设计和样式。

### 任务1：Material Design 3 集成

* **目的**：使用 Material Design 3 设计系统
* **方法**：
    1. 创建 Theme.kt - 定义主题函数
    2. 创建 Color.kt - 定义浅色/深色配色方案
    3. 创建 Type.kt - 定义排版系统
    4. 创建 Shape.kt - 定义形状系统（可选）
* **AI指令**：
    > 集成 Material 3 时：使用 lightColorScheme 和 darkColorScheme；支持动态颜色（Android 12+）；在 MainActivity 中用主题包裹整个应用；所有组件使用 MaterialTheme.colorScheme 获取颜色。

---

### 任务2：深色模式支持

* **目的**：完整支持浅色和深色两种模式
* **方法**：
    1. 定义两套配色方案（LightColorScheme 和 DarkColorScheme）
    2. 根据系统设置自动切换
    3. 支持用户手动切换（可选）
* **AI指令**：
    > 实现深色模式：所有颜色使用 MaterialTheme.colorScheme 而非硬编码；提供两套完整配色；使用 isSystemInDarkTheme() 检测系统设置；所有预览函数添加深色模式变体。

---

### 任务3：自定义主题

* **目的**：定义应用的品牌色和语义化颜色
* **方法**：
    1. 定义品牌主色（Primary）和辅助色（Secondary）
    2. 定义语义化颜色（成功、警告、错误）
    3. 确保颜色对比度符合无障碍标准
* **AI指令**：
    > 自定义主题时：品牌色要有浅色和深色两个版本；每个颜色定义对应的 onXxx 颜色（文本颜色）；使用 Material Theme Builder 生成配色；确保对比度符合 WCAG AA 标准。

---

## 七、核心设计原则

### 1. 单向数据流 (Unidirectional Data Flow)

**流程：**
```
ViewModel (UiState) → UI 组件展示
        ↑                  ↓
   调用 UseCase      用户操作 (UiEvent)
```

**原则：**
- UI 只能读取状态，不能直接修改
- 所有修改通过 ViewModel 的 onEvent() 方法
- ViewModel 是唯一的状态修改者

---

### 2. 状态提升 (State Hoisting)

**原则：**
- 可复用组件应该是无状态的
- 状态由父组件或 ViewModel 管理
- 组件通过参数接收状态，通过回调向上传递事件

**好处：**
- 组件可复用性更高
- 便于测试
- 数据流向清晰

---

### 3. 组件可复用性

**原则：**
- 组件应该小而专注，只做一件事
- 使用 Modifier 参数支持外部定制
- 提供合理的默认值
- 添加预览函数

---

### 4. 关注点分离 (Separation of Concerns)

**UI层职责：**
- ✅ 展示数据
- ✅ 捕获用户输入
- ✅ 导航跳转

**不属于UI层：**
- ❌ 业务逻辑（在 UseCase 中）
- ❌ 数据持久化（在 Repository 中）
- ❌ 网络请求（在 Repository 中）

---

### 5. 响应式设计

**原则：**
- 使用 StateFlow 实现响应式更新
- UI 自动响应状态变化（Recomposition）
- 避免手动刷新 UI

---

## 八、典型业务流程

### 流程1：聊天分析流程

```
用户点击"分析聊天"按钮
        ↓
ChatScreen 触发 UiEvent.AnalyzeChat
        ↓
ChatViewModel.onEvent() 接收事件
        ↓
更新状态：isAnalyzing = true
        ↓
调用 analyzeChatUseCase()
        ↓
UseCase 执行业务逻辑
  - 加载联系人档案
  - 脱敏处理
  - 调用 AI API
        ↓
返回 Result<AnalysisResult>
        ↓
ViewModel 更新状态
  - isAnalyzing = false
  - analysisResult = 结果
        ↓
UI 自动重组，显示分析结果
```

---

### 流程2：联系人保存流程

```
用户在表单中输入信息
        ↓
ContactDetailScreen 暂存草稿
（使用 remember { mutableStateOf() }）
        ↓
用户点击"保存"按钮
        ↓
触发 UiEvent.SaveContact
        ↓
ContactDetailViewModel.onEvent() 处理
        ↓
更新状态：isSaving = true
        ↓
调用 saveProfileUseCase()
        ↓
UseCase 保存到数据库
        ↓
返回 Result<Unit>
        ↓
ViewModel 处理结果
  - 成功：触发导航返回
  - 失败：更新 error 状态
        ↓
UI 响应
  - 成功：导航回列表，显示 Snackbar
  - 失败：显示错误对话框
```

---

## 九、与其他层的关系

### UI层 → 服务层（Domain Layer）

**依赖方式：**
- ViewModel 通过构造函数注入 UseCase
- 使用 @HiltViewModel 自动依赖注入

**交互模式：**
```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val analyzeChatUseCase: AnalyzeChatUseCase
) : ViewModel() {
    
    fun analyzeChat(contactId: String, messages: List<String>) {
        viewModelScope.launch {
            val result = analyzeChatUseCase(contactId, messages)
            
            result.onSuccess { analysis ->
                _uiState.update { it.copy(analysisResult = analysis) }
            }
            result.onFailure { error ->
                _uiState.update { it.copy(error = error.message) }
            }
        }
    }
}
```

**接收数据：**
- UI层接收 Domain Model（如 ContactProfile, AnalysisResult）
- 不需要进行数据转换（已由 Repository 完成）

---

### UI层与数据层的隔离

**严格禁止：**
- ❌ UI层直接访问 Repository
- ❌ UI层直接访问 DAO 或数据库
- ❌ UI层直接调用网络 API
- ❌ UI层处理 Entity 或 DTO

**正确做法：**
- ✅ 通过 UseCase 间接访问数据层
- ✅ 只处理 Domain Model
- ✅ 所有业务逻辑在服务层

**好处：**
1. **完全的关注点分离**：UI 只关心展示
2. **易于测试**：可以 Mock UseCase
3. **易于维护**：数据层变化不影响 UI
4. **遵循 Clean Architecture**：依赖方向正确

---

### 架构层级图

```
┌─────────────────────────────────────┐
│      Presentation Layer (UI)       │
│  ┌──────────┐      ┌─────────────┐ │
│  │ViewModel │ ───→ │ Composable  │ │
│  │          │ ←─── │   Screen    │ │
│  └────┬─────┘      └─────────────┘ │
└───────┼─────────────────────────────┘
        │ 依赖注入 UseCase
        ↓
┌─────────────────────────────────────┐
│      Domain Layer (服务层)          │
│  ┌──────────┐      ┌─────────────┐ │
│  │ UseCase  │ ───→ │   Service   │ │
│  │          │      │   (Engine)  │ │
│  └────┬─────┘      └─────────────┘ │
└───────┼─────────────────────────────┘
        │ 调用 Repository 接口
        ↓
┌─────────────────────────────────────┐
│       Data Layer (数据层)           │
│  ┌──────────┐      ┌─────────────┐ │
│  │Repository│ ───→ │ DataSource  │ │
│  │  Impl    │      │ (Room/API)  │ │
│  └──────────┘      └─────────────┘ │
└─────────────────────────────────────┘
```

**依赖方向：** UI层 → 服务层 → 数据层

**数据流向：** 数据层 → 服务层 → UI层

---

## 十、总结

### 关键要点

1. **UI层专注于视图展示和用户交互**
   - 不包含业务逻辑
   - 不直接访问数据层
   - 通过 ViewModel 管理状态

2. **使用MVVM架构模式**
   - ViewModel 管理 UI 状态
   - View（Composable）展示状态
   - 单向数据流保证可预测性

3. **Jetpack Compose 声明式UI**
   - 状态驱动 UI
   - 自动重组机制
   - 组件化开发

4. **单向数据流确保状态可预测**
   - State → UI → Event → ViewModel → State
   - 避免双向绑定的复杂性
   - 易于调试和追踪

5. **完全依赖服务层，不直接访问数据层**
   - 通过 UseCase 执行业务逻辑
   - 接收 Domain Model
   - 返回 Result 类型统一错误处理

---

### 建议执行路径

**阶段1：基础搭建**
1. 先做 **板块五（主题系统）** - 建立视觉基础
   - 定义颜色系统
   - 配置排版系统
   - 设置深色模式
2. 配置 **板块四（导航系统）** - 搭建导航框架
   - 定义路由管理
   - 创建导航图
   - 配置页面跳转

**阶段2：核心功能**
1. 实现 **板块三（状态管理）** - 定义所有 UiState 和 ViewModel
   - 创建 UiState 数据类
   - 定义 UiEvent 密封类
   - 编写 ViewModel 逻辑
2. 开发 **板块二（组件系统）** - 构建可复用组件
   - 卡片组件
   - 输入组件
   - 反馈组件

**阶段3：业务实现**
1. 完成 **板块一（核心界面）** - 组装业务界面
   - 聊天分析界面
   - 联系人管理界面
   - 草稿检查界面
2. 集成服务层 UseCase
   - 注入依赖
   - 处理业务结果
   - 
更新 UI 状态

**阶段4：优化完善**
1. 测试和调试
   - 编写 ViewModel 单元测试
   - UI 预览测试
   - 端到端测试
2. 性能优化
   - LazyColumn 优化
   - 避免不必要的重组
   - 内存泄漏检查
3. 无障碍功能支持
   - contentDescription
   - 语义化标签
   - 键盘导航

---

### 下一步

参考 **UI层开发规范.md** 中的详细规范，按照上述执行路径逐步实现UI层功能。

**关键文档：**
- 详细代码规范：`UI层开发规范.md`
- 服务层接口：`服务层全局设计.md`
- 领域模型定义：`domain/model/` 目录

**开发顺序建议：**
1. 先完成主题系统，确保视觉一致性
2. 然后开发可复用组件，建立组件库
3. 接着实现状态管理，定义所有状态和事件
4. 最后组装业务界面，完成功能闭环

**注意事项：**
- 遵循 Material Design 3 设计规范
- 所有组件必须支持深色模式
- ViewModel 只依赖 UseCase，不依赖 Repository
- 使用 StateFlow 而非 LiveData
- 所有 Composable 提供 @Preview 函数
- 保持单向数据流：State → UI → Event → ViewModel

---

## 附录：快速参考

### ViewModel 模板

```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val xxxUseCase: XxxUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FeatureUiState())
    val uiState: StateFlow<FeatureUiState> = _uiState.asStateFlow()
    
    fun onEvent(event: FeatureUiEvent) {
        when (event) {
            is FeatureUiEvent.Action -> handleAction(event.param)
        }
    }
    
    private fun handleAction(param: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = xxxUseCase(param)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    data = result.getOrNull(),
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }
}
```

### Screen 模板

```kotlin
@Composable
fun FeatureScreen(
    viewModel: FeatureViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    FeatureScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

@Composable
private fun FeatureScreenContent(
    uiState: FeatureUiState,
    onEvent: (FeatureUiEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("标题") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.error != null -> ErrorDialog(uiState.error)
                else -> ContentView(uiState, onEvent)
            }
        }
    }
}

@Preview
@Composable
private fun FeatureScreenPreview() {
    MaterialTheme {
        FeatureScreenContent(
            uiState = FeatureUiState(),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
```

### 路由定义模板

```kotlin
sealed class NavRoutes(val route: String) {
    object FeatureList : NavRoutes("feature_list")
    
    object FeatureDetail : NavRoutes("feature_detail/{id}") {
        fun createRoute(id: String) = "feature_detail/$id"
        const val ARG_ID = "id"
    }
}
```

---

**文档版本：** v1.0  
**最后更新：** 2024-12  
**维护者：** 架构团队