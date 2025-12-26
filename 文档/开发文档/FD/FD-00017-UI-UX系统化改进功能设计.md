# FD-00017: UI/UX系统化改进功能设计

> **文档类型**: 功能设计文档 (FD)
> **版本**: 1.2
> **创建日期**: 2025-12-24
> **更新日期**: 2025-12-24
> **负责人**: Kiro
> **状态**: ✅ 已审查通过
> **优先级**: 🔴 高
> **关联PRD**: PRD-00017

---

## 📋 文档概述

### 设计目标
建立统一、现代、情感化的UI/UX设计体系，解决当前界面存在的间距不统一、动画缺失、错误提示不友好、空状态设计简陋等问题。

### 设计原则
- **统一性** - 建立全局设计规范，消除不一致性
- **流畅性** - 所有交互都有平滑的动画过渡
- **友好性** - 错误提示和空状态更具人情味
- **专业性** - 遵循Material Design 3最佳实践

### 关联文档
- [PRD-00017-UI-UX系统化改进需求](../PRD/PRD-00017-UI-UX系统化改进需求.md)
- [RESEARCH-00036-UI-UX系统化改进调研报告](../RE/RESEARCH-00036-UI-UX系统化改进调研报告.md)

---

## 🎯 功能模块设计


### 模块1：统一间距系统

#### 1.1 功能描述
扩展现有`Dimensions`对象，添加缺失的12dp间距，并创建`AppSpacing`类型别名，统一所有页面的间距使用。

#### 1.2 与现有代码的兼容性

**现有实现**: `presentation/src/main/kotlin/com/empathy/ai/presentation/theme/Dimensions.kt`

项目已存在`Dimensions`对象，包含以下间距定义：
- `SpacingXSmall = 4.dp`
- `SpacingSmall = 8.dp`
- `SpacingMedium = 16.dp`
- `SpacingLarge = 24.dp`
- `SpacingXLarge = 32.dp`

**问题**: 缺少12dp间距，实际代码中大量使用硬编码`12.dp`

#### 1.3 设计规范

**方案**: 扩展现有`Dimensions`对象，添加缺失间距

**修改文件**: `presentation/src/main/kotlin/com/empathy/ai/presentation/theme/Dimensions.kt`

```kotlin
object Dimensions {
    // ... 现有代码保持不变 ...
    
    // ========== 间距（扩展） ==========
    
    /** 极小间距 (4dp) */
    val SpacingXSmall = 4.dp
    
    /** 小间距 (8dp) */
    val SpacingSmall = 8.dp
    
    /** 🆕 中小间距 (12dp) - 用于列表项间距、表单字段间距 */
    val SpacingMediumSmall = 12.dp
    
    /** 中等间距 (16dp) */
    val SpacingMedium = 16.dp
    
    /** 大间距 (24dp) */
    val SpacingLarge = 24.dp
    
    /** 极大间距 (32dp) */
    val SpacingXLarge = 32.dp
}
```

**新增文件**: `presentation/src/main/kotlin/com/empathy/ai/presentation/theme/Spacing.kt`

```kotlin
/**
 * 间距规范类型别名
 * 提供更简洁的API，同时保持与Dimensions的兼容性
 * 基于8dp网格系统 (8dp Grid System)
 */
object AppSpacing {
    val xs = Dimensions.SpacingXSmall    // 4dp - 图标与文字间距、标签内边距
    val sm = Dimensions.SpacingSmall     // 8dp - 相关元素间距、卡片内边距
    val md = Dimensions.SpacingMediumSmall // 12dp - 列表项间距、表单字段间距
    val lg = Dimensions.SpacingMedium    // 16dp - 页面边距、section间距
    val xl = Dimensions.SpacingLarge     // 24dp - 主要区域分隔
    val xxl = Dimensions.SpacingXLarge   // 32dp - 特殊强调
}
```

#### 1.4 使用规范

| 场景 | 推荐间距 | 数值 |
|------|----------|------|
| 页面边距 | `AppSpacing.lg` | 16dp |
| 列表项间距 | `AppSpacing.md` | 12dp |
| 卡片内边距 | `AppSpacing.lg` | 16dp |
| Section间距 | `AppSpacing.xl` | 24dp |
| 图标与文字 | `AppSpacing.sm` | 8dp |
| 按钮内边距 | `AppSpacing.md` | 12dp |

#### 1.5 代码示例

```kotlin
// ✅ 正确用法
LazyColumn(
    contentPadding = PaddingValues(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
) { ... }

// ❌ 错误用法
LazyColumn(
    contentPadding = PaddingValues(16.dp, 12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) { ... }
```

#### 1.6 实施范围

**第一阶段（核心界面）**：ContactListScreen、ContactDetailScreen、SettingsScreen、ChatScreen

**第二阶段（其他界面）**：ContactDetailTabScreen、PromptEditorScreen、AiConfigScreen


---

### 模块2：交互动效系统

#### 2.1 功能描述
建立全局动画规范，为页面转场、列表操作、按钮点击等交互添加流畅的动画效果。

#### 2.2 设计原则
- **流畅自然** - 动画时长200-400ms，使用缓动曲线
- **有意义的反馈** - 每个操作都有明确的视觉反馈
- **不干扰用户** - 动画不能影响用户操作效率
- **性能优先** - 使用硬件加速，避免掉帧

#### 2.3 与现有AnimationSpec的整合

**现有实现**: `presentation/src/main/kotlin/com/empathy/ai/presentation/theme/AnimationSpec.kt`

项目已存在`AnimationSpec`对象，包含以下动画规范：
- `DurationFast = 150ms` - 快速动画
- `DurationNormal = 300ms` - 标准动画
- `DurationSlow = 500ms` - 慢速动画
- `EasingStandard = FastOutSlowInEasing` - 标准缓动曲线

**设计原则**: 新增动画组件应复用现有`AnimationSpec`常量，保持一致性。

#### 2.4 页面转场动画

**应用场景**: NavHost中的所有页面导航

```kotlin
// NavGraph.kt 中配置（复用AnimationSpec常量）
NavHost(
    navController = navController,
    startDestination = NavRoutes.CONTACT_LIST,
    enterTransition = {
        slideInHorizontally(
            initialOffsetX = { it }, 
            animationSpec = tween(AnimationSpec.DurationNormal, easing = AnimationSpec.EasingStandard)
        ) + fadeIn(tween(AnimationSpec.DurationNormal))
    },
    exitTransition = {
        slideOutHorizontally(
            targetOffsetX = { -it / 3 }, 
            animationSpec = tween(AnimationSpec.DurationNormal, easing = AnimationSpec.EasingStandard)
        ) + fadeOut(tween(AnimationSpec.DurationNormal))
    }
) { ... }
```

#### 2.5 列表项动画组件

**文件**: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/animation/AnimatedListItem.kt`

```kotlin
@Composable
fun AnimatedListItem(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(AnimationSpec.DurationNormal)) + fadeIn(tween(AnimationSpec.DurationNormal)),
        exit = shrinkVertically(tween(AnimationSpec.DurationNormal)) + fadeOut(tween(AnimationSpec.DurationFast)),
        modifier = modifier
    ) { content() }
}
```

#### 2.6 按钮点击反馈组件

**文件**: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/animation/ClickableScale.kt`

```kotlin
@Composable
fun ClickableScale(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    scaleOnPress: Float = 0.95f,
    content: @Composable () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleOnPress else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
    )
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { onClick() }
                )
            }
    ) { content() }
}
```

#### 2.7 加载骨架屏组件

**文件**: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/state/LoadingSkeleton.kt`

```kotlin
@Composable
fun LoadingSkeleton(modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.small) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
    )
    Box(modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), shape))
}
```


---

### 模块3：友好错误提示系统

#### 3.1 功能描述
将技术错误转换为用户友好的提示，提供解决方案和操作按钮。

#### 3.2 错误映射器

**文件**: `presentation/src/main/kotlin/com/empathy/ai/presentation/util/ErrorMessageMapper.kt`

```kotlin
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 错误消息映射器
 * 将技术错误转换为用户友好的提示
 */
object ErrorMessageMapper {
    fun mapError(error: Throwable): FriendlyErrorMessage = when (error) {
        is UnknownHostException -> FriendlyErrorMessage(
            title = "网络连接失败", 
            message = "请检查网络连接后重试",
            icon = Icons.Default.CloudOff, 
            actions = listOf(ErrorAction("重试", ErrorActionType.Retry))
        )
        is SocketTimeoutException -> FriendlyErrorMessage(
            title = "请求超时", 
            message = "网络响应时间过长，请稍后重试",
            icon = Icons.Default.HourglassEmpty, 
            actions = listOf(ErrorAction("重试", ErrorActionType.Retry))
        )
        is HttpException -> mapHttpError(error)
        else -> FriendlyErrorMessage(
            title = "发生未知错误", 
            message = error.message ?: "请稍后重试",
            icon = Icons.Default.Error, 
            actions = listOf(ErrorAction("重试", ErrorActionType.Retry))
        )
    }
    
    private fun mapHttpError(error: HttpException): FriendlyErrorMessage {
        return when (error.code()) {
            401 -> FriendlyErrorMessage(
                title = "认证失败",
                message = "API密钥无效或已过期，请检查设置",
                icon = Icons.Default.Lock,
                actions = listOf(
                    ErrorAction("去设置", ErrorActionType.Navigate),
                    ErrorAction("取消", ErrorActionType.Dismiss)
                )
            )
            429 -> FriendlyErrorMessage(
                title = "请求过于频繁",
                message = "请稍后再试，或升级API配额",
                icon = Icons.Default.Speed,
                actions = listOf(ErrorAction("稍后重试", ErrorActionType.Retry))
            )
            in 500..599 -> FriendlyErrorMessage(
                title = "服务器错误",
                message = "服务暂时不可用，请稍后重试",
                icon = Icons.Default.CloudOff,
                actions = listOf(ErrorAction("重试", ErrorActionType.Retry))
            )
            else -> FriendlyErrorMessage(
                title = "请求失败",
                message = "错误代码: ${error.code()}",
                icon = Icons.Default.Error,
                actions = listOf(ErrorAction("重试", ErrorActionType.Retry))
            )
        }
    }
}

data class FriendlyErrorMessage(
    val title: String,
    val message: String,
    val icon: ImageVector,
    val actions: List<ErrorAction>
)

data class ErrorAction(val text: String, val type: ErrorActionType)

enum class ErrorActionType { Retry, Dismiss, Report, Navigate }
```

#### 3.3 友好错误卡片组件

**文件**: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/state/FriendlyErrorCard.kt`

```kotlin
@Composable
fun FriendlyErrorCard(
    error: FriendlyErrorMessage,
    onAction: (ErrorActionType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(Modifier.padding(AppSpacing.lg), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Icon(error.icon, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                Text(error.title, style = MaterialTheme.typography.titleMedium)
            }
            Text(error.message, style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm, Alignment.End)) {
                error.actions.forEach { action ->
                    TextButton(onClick = { onAction(action.type) }) { Text(action.text) }
                }
            }
        }
    }
}
```


---

### 模块4：空状态设计系统

#### 4.1 功能描述
增强现有`EmptyView`组件和`EmptyType`密封类，添加情感化设计元素，提升用户体验。

#### 4.2 与现有代码的兼容性

**现有实现**: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/state/EmptyView.kt`

项目已存在`EmptyType`密封类：
- `NoData` - 暂无数据
- `NoContacts` - 还没有联系人
- `NoTags` - 还没有标签
- `NoResults` - 没有找到结果

**设计方案**: 增强现有`EmptyType`，添加description和actionText属性，而非创建新类。

#### 4.3 增强EmptyType定义

**修改文件**: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/state/EmptyView.kt`

```kotlin
/**
 * 空状态类型枚举（增强版）
 * 添加description和actionText属性，支持情感化设计
 */
sealed class EmptyType(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val actionText: String? = null
) {
    data object NoData : EmptyType(
        Icons.Default.Search, "暂无数据", "这里还没有任何内容", null
    )
    data object NoContacts : EmptyType(
        Icons.Outlined.PersonAdd, "还没有联系人", 
        "添加第一个联系人，开始记录你们的故事", "添加联系人"
    )
    data object NoTags : EmptyType(
        Icons.Outlined.LocalOffer, "还没有标签", 
        "标签可以帮助你更好地了解联系人", "添加标签"
    )
    data object NoResults : EmptyType(
        Icons.Outlined.SearchOff, "没有找到结果", "试试其他关键词", null
    )
    data object NetworkError : EmptyType(
        Icons.Outlined.CloudOff, "网络连接失败", "请检查网络连接后重试", "重试"
    )
}
```

#### 4.4 增强EmptyView组件

```kotlin
/**
 * 空状态视图组件（增强版）
 * 添加呼吸动画效果，提升情感化体验
 */
@Composable
fun EmptyView(
    emptyType: EmptyType,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(AppSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 带呼吸动画的图标
        val transition = rememberInfiniteTransition(label = "emptyIcon")
        val scale by transition.animateFloat(
            initialValue = 0.95f, targetValue = 1.05f,
            animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
            label = "scale"
        )

        Icon(
            imageVector = emptyType.icon,
            contentDescription = emptyType.title,
            modifier = Modifier.size(80.dp).graphicsLayer { scaleX = scale; scaleY = scale },
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.lg))
        Text(emptyType.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        Text(emptyType.description, style = MaterialTheme.typography.bodyMedium, 
             color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        
        if (emptyType.actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(AppSpacing.xl))
            Button(onClick = onAction) { Text(emptyType.actionText) }
        }
    }
}
```

#### 4.5 各页面空状态应用

| 页面 | 空状态类型 | 操作按钮 |
|------|-----------|----------|
| 联系人列表 | `NoContacts` | 添加联系人 |
| 联系人详情-标签 | `NoTags` | 添加标签 |
| 搜索结果 | `NoResults` | 无 |


---

### 模块5：悬浮窗Compose迁移（长期优化）

#### 5.1 功能描述
将传统View实现的悬浮窗迁移到Compose，统一UI框架，复用主题系统。

#### 5.2 迁移范围

| 组件 | 当前实现 | 目标实现 | 优先级 |
|------|----------|----------|--------|
| FloatingViewV2 | LinearLayout | Compose | 🟢 低 |
| TabSwitcher | MaterialButton | Compose Tab | 🟢 低 |
| ResultCard | CardView | Compose Card | 🟢 低 |

#### 5.3 迁移收益
- 统一主题系统 - 复用Material 3主题
- 统一代码风格 - 所有UI使用Compose
- 更好的动画支持 - 利用Compose动画API

#### 5.4 实施计划
- **阶段1**: 技术验证（1-2天）
- **阶段2**: 功能迁移（3-4天）
- **阶段3**: 集成测试（1-2天）

---

## 📁 文件结构设计

### 新增文件

```
presentation/src/main/kotlin/com/empathy/ai/presentation/
├── theme/
│   └── Spacing.kt                    # 🆕 间距规范（AppSpacing别名）
├── util/
│   └── ErrorMessageMapper.kt         # 🆕 错误映射器
└── ui/component/
    ├── animation/                    # 🆕 动画组件目录
    │   ├── AnimatedListItem.kt       # 🆕 列表项动画
    │   ├── ClickableScale.kt         # 🆕 点击缩放
    │   └── AnimatedViewSwitch.kt     # 🆕 视图切换动画
    └── state/
        ├── LoadingSkeleton.kt        # 🆕 加载骨架屏
        └── FriendlyErrorCard.kt      # 🆕 友好错误卡片
```

### 修改文件

| 文件 | 修改内容 |
|------|----------|
| `Dimensions.kt` | 添加`SpacingMediumSmall = 12.dp` |
| `EmptyView.kt` | 增强EmptyType，添加呼吸动画 |
| `ErrorView.kt` | 集成ErrorMessageMapper |
| `NavGraph.kt` | 添加页面转场动画配置 |
| `ContactListScreen.kt` | 使用AppSpacing、动画组件、增强空状态 |
| `ContactDetailScreen.kt` | 使用AppSpacing、动画组件 |
| `SettingsScreen.kt` | 使用AppSpacing |
| `ChatScreen.kt` | 使用AppSpacing、增强错误处理 |


---

## 🧪 测试用例设计

### 单元测试

#### AppSpacing测试
```kotlin
class AppSpacingTest {
    @Test
    fun `AppSpacing values should match Dimensions`() {
        assertEquals(Dimensions.SpacingXSmall, AppSpacing.xs)
        assertEquals(Dimensions.SpacingSmall, AppSpacing.sm)
        assertEquals(Dimensions.SpacingMediumSmall, AppSpacing.md)
        assertEquals(Dimensions.SpacingMedium, AppSpacing.lg)
        assertEquals(Dimensions.SpacingLarge, AppSpacing.xl)
        assertEquals(Dimensions.SpacingXLarge, AppSpacing.xxl)
    }
}
```

#### ErrorMessageMapper测试
```kotlin
class ErrorMessageMapperTest {
    @Test
    fun `mapError should return network error for UnknownHostException`() {
        val error = UnknownHostException()
        val result = ErrorMessageMapper.mapError(error)
        assertEquals("网络连接失败", result.title)
        assertTrue(result.actions.any { it.type == ErrorActionType.Retry })
    }
    
    @Test
    fun `mapError should return timeout error for SocketTimeoutException`() {
        val error = SocketTimeoutException()
        val result = ErrorMessageMapper.mapError(error)
        assertEquals("请求超时", result.title)
    }
}
```

### UI测试

#### EmptyView测试
```kotlin
class EmptyViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `EmptyView should display title and description`() {
        composeTestRule.setContent {
            EmptyView(emptyType = EmptyType.NoContacts)
        }
        composeTestRule.onNodeWithText("还没有联系人").assertIsDisplayed()
        composeTestRule.onNodeWithText("添加第一个联系人，开始记录你们的故事").assertIsDisplayed()
    }
    
    @Test
    fun `EmptyView should show action button when actionText is not null`() {
        composeTestRule.setContent {
            EmptyView(emptyType = EmptyType.NoContacts, onAction = {})
        }
        composeTestRule.onNodeWithText("添加联系人").assertIsDisplayed()
    }
}
```

---

## 📊 实施计划

### 阶段划分

| 阶段 | 内容 | 工作量 | 优先级 |
|------|------|--------|--------|
| 第一阶段 | 统一间距系统 | 2-3天 | 🔴 高 |
| 第二阶段 | 交互动效系统 | 3-4天 | 🔴 高 |
| 第三阶段 | 友好错误提示 | 1-2天 | 🟡 中 |
| 第四阶段 | 空状态设计 | 1-2天 | 🟡 中 |
| 第五阶段 | 悬浮窗迁移 | 5-7天 | 🟢 低 |

### 详细任务分解

#### 第一阶段：统一间距系统（2-3天）

| 任务 | 说明 | 预估 |
|------|------|------|
| 扩展Dimensions对象 | 添加SpacingMediumSmall = 12.dp | 0.25天 |
| 创建AppSpacing别名 | 在theme目录创建Spacing.kt | 0.25天 |
| 更新ContactListScreen | 替换硬编码间距 | 0.5天 |
| 更新ContactDetailScreen | 替换硬编码间距 | 0.5天 |
| 更新SettingsScreen | 替换硬编码间距 | 0.5天 |
| 更新ChatScreen | 替换硬编码间距 | 0.5天 |
| 视觉验证 | 检查所有页面间距一致性 | 0.5天 |

#### 第二阶段：交互动效系统（3-4天）

| 任务 | 说明 | 预估 |
|------|------|------|
| 配置NavHost转场动画 | 在NavGraph中添加动画配置 | 0.5天 |
| 创建AnimatedListItem组件 | 列表项动画包装器 | 0.5天 |
| 创建ClickableScale组件 | 按钮点击反馈 | 0.5天 |
| 创建LoadingSkeleton组件 | 加载骨架屏 | 0.5天 |
| 应用到联系人列表 | 集成动画组件 | 0.5天 |
| 应用到其他页面 | 集成动画组件 | 1天 |
| 性能测试 | 确保60fps | 0.5天 |


---

## ✅ 验收标准

### 功能验收

| 验收项 | 标准 | 优先级 |
|--------|------|--------|
| 间距统一 | 所有页面使用AppSpacing，无硬编码 | 🔴 高 |
| 页面转场 | 所有导航有平滑的滑入滑出动画 | 🔴 高 |
| 列表动画 | 列表项增删有淡入淡出动画 | 🔴 高 |
| 按钮反馈 | 按钮点击有缩放反馈 | 🔴 高 |
| 错误提示 | 所有错误显示友好提示 | 🟡 中 |
| 空状态 | 所有空状态有图标、标题、描述、操作 | 🟡 中 |

### 性能验收

| 验收项 | 标准 |
|--------|------|
| 动画帧率 | 稳定60fps，无掉帧 |
| 页面渲染 | 首屏渲染 < 500ms |
| 列表滚动 | 流畅无卡顿 |
| 内存占用 | 无明显增加 |

### 代码质量验收

| 验收项 | 标准 |
|--------|------|
| Preview | 所有新增组件有完整Preview |
| 注释 | 所有新增代码有KDoc注释 |
| Lint | 通过Lint检查，无警告 |
| 测试 | 关键组件有单元测试 |

---

## 🔗 依赖关系

### 模块依赖

```
模块1: 统一间距系统
    ↓
模块2: 交互动效系统 (依赖间距规范)
    ↓
模块3: 友好错误提示 (可独立)
    ↓
模块4: 空状态设计 (可独立)
    ↓
模块5: 悬浮窗迁移 (可独立，长期)
```

### 技术依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| Jetpack Compose | BOM 2024.12.01 | UI框架 |
| Material 3 | 1.3.1 | 设计系统 |
| Navigation Compose | 2.8.5 | 导航动画 |

---

## ⚠️ 风险与缓解

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|----------|
| 间距修改导致布局问题 | 中 | 中 | 逐页面验证，充分测试 |
| 动画导致性能问题 | 低 | 高 | 使用硬件加速，监控帧率 |
| 悬浮窗迁移兼容性问题 | 中 | 高 | 充分测试，保留回退方案 |
| 工作量超出预期 | 中 | 中 | 分阶段实施，优先核心功能 |

---

## 📝 附录

### A. 参考资料
- [Material Design 3](https://m3.material.io/)
- [Jetpack Compose Animation](https://developer.android.com/jetpack/compose/animation)
- [Android App Quality Guidelines](https://developer.android.com/quality)

### B. 相关文档
- [PRD-00017-UI-UX系统化改进需求](../PRD/PRD-00017-UI-UX系统化改进需求.md)
- [RESEARCH-00036-UI-UX系统化改进调研报告](../RE/RESEARCH-00036-UI-UX系统化改进调研报告.md)

### C. 术语表

| 术语 | 解释 |
|------|------|
| AppSpacing | 应用间距规范对象 |
| AnimationSpec | 动画规范对象 |
| ErrorMessageMapper | 错误信息映射器 |
| EmptyType | 空状态类型枚举 |
| LoadingSkeleton | 加载骨架屏组件 |

---

**文档版本**: 1.2  
**最后更新**: 2025-12-24  
**审查报告**: [DR-00029-FD00017文档审查报告](../DR/DR-00029-FD00017文档审查报告.md)  
**下一步**: 创建TD任务清单文档
