# PRD-00018: UI/UX系统化改进需求

> **文档类型**: 产品需求文档 (PRD)
> **版本**: 1.0
> **创建日期**: 2025-12-24
> **更新日期**: 2025-12-24
> **负责人**: Claude
> **状态**: 📋 待评审
> **优先级**: 🔴 高
> **目标受众**: UI/UX生成AI、前端代码生成AI、自动化测试AI

---

## 🎯 核心目标

建立**统一、现代、情感化**的UI/UX设计体系，为MVP验证阶段的用户体验打下坚实基础。

**设计哲学**：
- **统一性** - 建立全局设计规范，消除不一致性
- **流畅性** - 所有交互都有平滑的动画过渡
- **友好性** - 错误提示和空状态更具人情味
- **专业性** - 遵循Material Design 3最佳实践

---

## 📋 需求背景

### 当前状况分析

**✅ 已有优势**：
1. **完整的主题系统** - Material 3配色、字体规范、深色模式支持
2. **丰富的组件库** - 60+可复用组件，覆盖大部分场景
3. **良好的代码组织** - 清晰的模块划分、完整的Preview、详细的注释

**❌ 存在问题**：

#### 问题1：间距不统一 🔴
```
现状：不同组件使用不同的间距值
- ContactListScreen: 16.dp, 12.dp
- ContactDetailScreen: 16.dp, 12.dp, 8.dp
- SettingsScreen: 16.dp, 12.dp, 24.dp

影响：视觉节奏不统一，界面显得混乱
```

#### 问题2：动画缺失 🔴
```
现状：大部分交互无动画过渡
- 页面切换：硬切
- 列表项操作：无反馈动画
- 按钮点击：无缩放反馈
- 状态变化：无过渡动画

影响：体验生硬，缺乏精致感
```

#### 问题3：错误提示不够友好 🟡
```
现状：直接显示技术错误信息
- 示例："网络连接失败，请检查网络设置"
- 问题：没有解释原因，没有提供解决方案

影响：用户不知道如何处理，体验受挫
```

#### 问题4：空状态设计简陋 🟡
```
现状：纯文本提示
- 示例："还没有联系人"
- 问题：没有视觉吸引力，缺乏引导

影响：页面显得空洞，用户不知道下一步做什么
```

#### 问题5：Compose与View混用 🟡
```
现状：悬浮窗使用传统View，其他页面使用Compose
- FloatingViewV2: LinearLayout + MaterialButton
- 其他页面: Compose UI

影响：代码风格不一致，无法复用主题系统
```

### 用户痛点

**痛点1：视觉混乱**
> "不同页面的间距好像不一样，有时候16dp，有时候12dp，感觉不够统一"

**痛点2：体验生硬**
> "页面切换太快了，感觉有点突兀，如果能有过渡动画就好了"

**痛点3：遇到错误不知道怎么办**
> "出现错误提示时，只是显示一个错误信息，我不知道该怎么解决"

**痛点4：空页面很冷清**
> "第一次打开应用，看到'还没有联系人'这几个字，感觉有点空，不知道该做什么"

---

## 🎨 设计方案

### 方案1：统一间距系统 🔴 高优先级

#### 设计规范

建立全局间距规范，统一所有页面的间距使用：

```kotlin
/**
 * 全局间距规范
 *
 * 基于8dp网格系统 (8dp Grid System)
 * 所有间距应该是8dp的倍数
 */
object AppSpacing {
    // 超小间距 - 用于图标与文字间距、标签内边距
    val xs = 4.dp   // 0.5x

    // 小间距 - 用于相关元素间距、卡片内边距
    val sm = 8.dp   // 1x

    // 中间距 - 用于列表项间距、表单字段间距
    val md = 12.dp  // 1.5x

    // 大间距 - 用于页面边距、section间距
    val lg = 16.dp  // 2x

    // 超大间距 - 用于主要区域分隔
    val xl = 24.dp  // 3x

    // 特大间距 - 用于特殊强调
    val xxl = 32.dp // 4x
}
```

#### 使用规范

**页面边距**：
```kotlin
// ✅ 正确
PaddingValues(horizontal = AppSpacing.lg, vertical = AppSpacing.md)

// ❌ 错误
PaddingValues(16.dp, 12.dp)
```

**列表项间距**：
```kotlin
// ✅ 正确
LazyColumn(
    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
) { ... }

// ❌ 错误
LazyColumn(
    verticalArrangement = Arrangement.spacedBy(12.dp)
) { ... }
```

**卡片内边距**：
```kotlin
// ✅ 正确
Card(
    modifier = Modifier.padding(AppSpacing.lg)
) {
    Column(
        modifier = Modifier.padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) { ... }
}

// ❌ 错误
Card(
    modifier = Modifier.padding(16.dp)
) {
    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) { ... }
}
```

#### 实施范围

**核心界面优先**（第一阶段）：
- ✅ ContactListScreen - 联系人列表
- ✅ ContactDetailScreen - 联系人详情
- ✅ SettingsScreen - 设置页面
- ✅ ChatScreen - 聊天界面
- ✅ FloatingViewV2 - 悬浮窗

**其他界面**（第二阶段）：
- ContactDetailTabScreen - 联系人详情Tab
- PromptEditorScreen - 提示词编辑器
- AiConfigScreen - AI配置页面

#### 验收标准

- [ ] 所有页面使用 `AppSpacing` 替代硬编码间距值
- [ ] 同类元素使用相同间距（如所有列表项间距为 `AppSpacing.md`）
- [ ] 视觉节奏统一，界面显得整洁有序
- [ ] 通过视觉审查，无明显的间距不一致

---

### 方案2：交互动效系统 🔴 高优先级

#### 设计原则

1. **流畅自然** - 动画时长200-400ms，使用缓动曲线
2. **有意义的反馈** - 每个操作都有明确的视觉反馈
3. **不干扰用户** - 动画不能影响用户操作效率
4. **性能优先** - 使用硬件加速，避免掉帧

#### 动画规范

**缓动曲线**：
```kotlin
/**
 * 全局动画规范
 */
object AppAnimation {
    // 缓动曲线
    val easingStandard = FastOutSlowInEasing     // 标准：进入快，退出慢
    val easingDecelerate = DecelerateEasing      // 减速：快速进入，平滑停止
    val easingAccelerate = AccelerateEasing      // 加速：慢速进入，快速退出

    // 动画时长
    val durationFast = 200.ms    // 快速：微交互
    val durationNormal = 300.ms  // 标准：大多数过渡
    val durationSlow = 400.ms    // 慢速：复杂转场

    // 弹簧参数
    val spring = spring(
        dampingRatio = 0.8f,    // 阻尼比：0.8（轻微弹跳）
        stiffness = 400f        // 刚度：400（适中）
    )
}
```

**动画类型**：

##### 1. 页面转场动画

**场景**：页面导航时

**效果**：新页面从右侧滑入，旧页面向左侧滑出（淡出）

```kotlin
/**
 * 页面转场动画
 *
 * 使用场景：导航到新页面时
 */
fun NavHost fadeInSlide() {
    enterTransition = {
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(AppAnimation.durationNormal)
        ) + fadeIn(
            animationSpec = tween(AppAnimation.durationNormal)
        )
    }
    exitTransition = {
        slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(AppAnimation.durationNormal)
        ) + fadeOut(
            animationSpec = tween(AppAnimation.durationNormal)
        )
    }
}
```

##### 2. 列表项动画

**场景**：列表项插入、删除、更新

**效果**：新淡入淡出 + 高度变化

```kotlin
/**
 * 列表项动画
 *
 * 使用场景：LazyColumn项目增删改
 */
@Composable
fun AnimatedListItem(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(
            animationSpec = tween(AppAnimation.durationNormal)
        ) + fadeIn(
            animationSpec = tween(AppAnimation.durationNormal)
        ),
        exit = shrinkVertically(
            animationSpec = tween(AppAnimation.durationNormal)
        ) + fadeOut(
            animationSpec = tween(AppAnimation.durationFast)
        )
    ) {
        content()
    }
}
```

##### 3. 按钮点击反馈

**场景**：按钮点击时

**效果**：轻微缩放（0.95 → 1.0）

```kotlin
/**
 * 按钮点击反馈动画
 *
 * 使用场景：所有可点击元素
 */
@Composable
fun ClickableScale(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(AppAnimation.spring),
        label = "scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInteropFilter {
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> pressed = true
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> pressed = false
                }
                false
            }
            .clickable(onClick = onClick)
    ) {
        content()
    }
}
```

##### 4. 加载状态动画

**场景**：数据加载中

**效果**：脉冲动画 + 骨架屏

```kotlin
/**
 * 加载状态骨架屏
 *
 * 使用场景：替代纯LoadingIndicator
 */
@Composable
fun LoadingSkeleton(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                shape = MaterialTheme.shapes.small
            )
    )
}
```

##### 5. 状态切换动画

**场景**：视图模式切换（如时间线 ↔ 列表）

**效果**：流体变形 + 淡入淡出

```kotlin
/**
 * 视图切换动画
 *
 * 使用场景：时间线/列表切换、Tab切换
 */
@Composable
fun <T> AnimatedViewSwitch(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        transitionSpec = {
            fadeIn(
                animationSpec = tween(AppAnimation.durationNormal),
                initialAlpha = 0.3f
            ) + scaleIn(
                initialScale = 0.95f,
                animationSpec = tween(AppAnimation.durationNormal)
            ) with fadeOut(
                animationSpec = tween(AppAnimation.durationNormal),
                targetAlpha = 0.3f
            ) + scaleOut(
                targetScale = 0.95f,
                animationSpec = tween(AppAnimation.durationNormal)
            )
        },
        label = "viewSwitch",
        modifier = modifier
    ) { state ->
        content(state)
    }
}
```

#### 实施范围

**第一阶段**（核心交互）：
- ✅ 页面转场动画 - 所有导航
- ✅ 列表项动画 - 联系人列表、标签列表
- ✅ 按钮点击反馈 - 所有Primary/Secondary按钮
- ✅ 加载状态骨架屏 - 联系人列表、聊天界面

**第二阶段**（增强体验）：
- 状态切换动画 - 时间线/列表切换
- 下拉刷新动画 - 所有列表页面
- 悬浮窗展开/收起动画
- 对话框进入/退出动画

#### 验收标准

- [ ] 所有页面导航有平滑的转场动画
- [ ] 列表项增删有淡入淡出动画
- [ ] 按钮点击有缩放反馈
- [ ] 加载状态使用骨架屏（主要列表）
- [ ] 动画流畅，无掉帧（60fps）
- [ ] 动画时长不超过400ms，不影响效率

---

### 方案3：友好错误提示系统 🟡 中优先级

#### 设计原则

1. **用户友好** - 技术错误转换为用户能理解的文案
2. **提供解决方案** - 告诉用户如何解决问题
3. **区分级别** - 错误、警告、提示使用不同样式
4. **易于操作** - 提供直接的操作按钮

#### 错误映射表

```kotlin
/**
 * 错误信息映射表
 *
 * 将技术错误转换为用户友好的提示
 */
object ErrorMessageMapper {

    fun mapError(error: Throwable): FriendlyErrorMessage {
        return when (error) {
            is UnknownHostException -> FriendlyErrorMessage(
                title = "网络连接失败",
                message = "请检查网络连接后重试",
                icon = Icons.Default.CloudOff,
                actions = listOf(
                    ErrorAction("重试", ActionRetry)
                )
            )

            is SocketTimeoutException -> FriendlyErrorMessage(
                title = "请求超时",
                message = "网络响应时间过长，请稍后重试",
                icon = Icons.Default.HourglassEmpty,
                actions = listOf(
                    ErrorAction("重试", ActionRetry)
                )
            )

            is HttpException -> FriendlyErrorMessage(
                title = "服务异常",
                message = "服务器暂时无法响应，请稍后重试",
                icon = Icons.Default.ErrorOutline,
                actions = listOf(
                    ErrorAction("重试", ActionRetry)
                )
            )

            is SerializationException -> FriendlyErrorMessage(
                title = "数据解析失败",
                message = "数据格式异常，请联系技术支持",
                icon = Icons.Default.BrokenImage,
                actions = listOf(
                    ErrorAction("反馈问题", ActionReport)
                )
            )

            else -> FriendlyErrorMessage(
                title = "发生未知错误",
                message = error.message ?: "请稍后重试，如果问题持续请联系技术支持",
                icon = Icons.Default.Error,
                actions = listOf(
                    ErrorAction("重试", ActionRetry),
                    ErrorAction("反馈问题", ActionReport)
                )
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

data class ErrorAction(
    val text: String,
    val action: ErrorActionType
)

enum class ErrorActionType {
    ActionRetry,
    ActionReport,
    ActionCancel,
    ActionDismiss
}
```

#### UI组件设计

```kotlin
/**
 * 友好错误提示卡片
 *
 * 替代简单的Snackbar，提供更丰富的错误信息和操作
 */
@Composable
fun FriendlyErrorCard(
    error: FriendlyErrorMessage,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            // 标题和图标
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = error.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = error.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            // 错误消息
            Text(
                text = error.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm, Alignment.End)
            ) {
                error.actions.forEach { action ->
                    TextButton(
                        onClick = {
                            when (action.action) {
                                ErrorActionType.ActionRetry -> onRetry()
                                ErrorActionType.ActionReport -> onReport()
                                else -> onDismiss()
                            }
                        }
                    ) {
                        Text(
                            text = action.text,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
```

#### 实施范围

**所有页面**：
- ✅ ContactListScreen - 联系人加载失败
- ✅ ContactDetailScreen - 联系人详情加载失败
- ✅ SettingsScreen - 设置保存失败
- ✅ ChatScreen - AI请求失败
- ✅ FloatingViewV2 - AI分析失败

#### 验收标准

- [ ] 所有错误都有用户友好的标题和描述
- [ ] 技术错误不直接显示给用户
- [ ] 错误提示包含可操作的按钮（如"重试"）
- [ ] 不同严重程度的错误使用不同样式
- [ ] 错误提示可以被用户关闭

---

### 方案4：空状态设计系统 🟡 中优先级

#### 设计原则

1. **情感化** - 使用插画和友好的文案
2. **引导性** - 告诉用户下一步该做什么
3. **一致性** - 同类空状态使用相同的设计

#### 空状态类型

##### 1. 无数据空状态

**场景**：列表为空（联系人、标签、历史记录等）

**设计**：
- 插画：使用Material Icons或自定义SVG插画
- 标题：友好的提示（如"还没有联系人"）
- 描述：简短说明（如"添加第一个联系人开始使用"）
- 操作：主要操作按钮（如"添加联系人"）

```kotlin
/**
 * 无数据空状态组件
 */
@Composable
fun EmptyDataState(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 图标（带动画）
        val infiniteTransition = rememberInfiniteTransition(label = "icon")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "iconScale"
        )

        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale },
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )

        Spacer(modifier = Modifier.height(AppSpacing.lg))

        // 标题
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(AppSpacing.sm))

        // 描述
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(AppSpacing.xl))

        // 操作按钮
        PrimaryButton(
            text = actionText,
            onClick = onAction
        )
    }
}
```

##### 2. 搜索无结果空状态

**场景**：搜索没有匹配结果

**设计**：
- 图标：搜索图标
- 标题："没有找到相关内容"
- 描述："试试其他关键词"
- 操作：清除搜索按钮

##### 3. 网络错误空状态

**场景**：首次加载失败

**设计**：
- 图标：网络错误图标
- 标题："加载失败"
- 描述："请检查网络连接后重试"
- 操作：重试按钮

##### 4. 权限请求空状态

**场景**：缺少必要权限

**设计**：
- 图标：权限图标
- 标题："需要权限"
- 描述："需要XX权限才能使用此功能"
- 操作：授权按钮

#### 实施范围

**所有页面的空状态**：
- ✅ ContactListScreen - 无联系人
- ✅ ContactDetailScreen - 无事实/标签
- ✅ ChatScreen - 无对话记录
- ✅ SettingsScreen - 无服务商配置
- ✅ 所有搜索页面 - 搜索无结果

#### 验收标准

- [ ] 所有空状态都有图标、标题、描述和操作
- [ ] 空状态使用友好的文案，避免冷冰冰的技术术语
- [ ] 空状态提供明确的下一步操作指引
- [ ] 图标有轻微的动画效果（呼吸效果）
- [ ] 同类空状态使用一致的设计风格

---

### 方案5：悬浮窗Compose迁移 🟢 低优先级（长期优化）

#### 迁移目标

将传统View实现的悬浮窗迁移到Compose，统一UI框架。

#### 迁移范围

**迁移组件**：
- FloatingViewV2 → Compose实现
- TabSwitcher → Compose实现
- ResultCard → Compose实现
- RefinementOverlay → Compose实现

#### 迁移收益

1. **统一主题系统** - 复用Material 3主题
2. **统一代码风格** - 所有UI使用Compose
3. **更好的动画支持** - 利用Compose动画API
4. **更少的代码** - Compose代码量更少
5. **更好的预览** - Compose Preview实时预览

#### 实施计划

**阶段1**：技术验证
- 创建Compose悬浮窗原型
- 验证性能和兼容性
- 评估工作量

**阶段2**：功能迁移
- 迁移TabSwitcher组件
- 迁移结果卡片
- 迁移输入框

**阶段3**：集成测试
- 功能回归测试
- 性能测试
- 兼容性测试

#### 验收标准

- [ ] 悬浮窗使用Compose完全重写
- [ ] 功能与原版本保持一致
- [ ] 性能不低于原版本
- [ ] 主题系统与主应用一致
- [ ] 动画更加流畅

---

## 📊 实施计划

### 阶段划分

#### 第一阶段：基础规范（必须完成）

**目标**：建立设计规范，统一视觉语言

**任务**：
1. 创建 `AppSpacing` 间距规范
2. 创建 `AppAnimation` 动画规范
3. 更新核心界面使用新规范
4. 补充缺失的Preview

**工作量**：2-3天

**优先级**：�� 高

---

#### 第二阶段：交互动效（必须完成）

**目标**：添加核心动画，提升体验流畅度

**任务**：
1. 实现页面转场动画
2. 实现列表项动画
3. 实现按钮点击反馈
4. 实现加载骨架屏

**工作量**：3-4天

**优先级**：🔴 高

---

#### 第三阶段：体验优化（建议完成）

**目标**：优化错误提示和空状态，提升友好度

**任务**：
1. 实现友好错误提示系统
2. 实现空状态设计系统
3. 更新所有页面使用新组件

**工作量**：2-3天

**优先级**：🟡 中

---

#### 第四阶段：架构优化（可选）

**目标**：迁移悬浮窗到Compose，统一UI框架

**任务**：
1. 技术验证和原型开发
2. 功能迁移
3. 集成测试

**工作量**：5-7天

**优先级**：🟢 低

---

### 优先级排序

| 优先级 | 方案 | 工作量 | 价值提升 | 建议顺序 |
|--------|------|--------|----------|----------|
| 🔴 高 | 统一间距系统 | 2-3天 | ⭐⭐⭐⭐ | 1 |
| 🔴 高 | 交互动效系统 | 3-4天 | ⭐⭐⭐⭐⭐ | 2 |
| 🟡 中 | 友好错误提示 | 1-2天 | ⭐⭐⭐ | 3 |
| 🟡 中 | 空状态设计 | 1-2天 | ⭐⭐⭐ | 4 |
| 🟢 低 | 悬浮窗迁移 | 5-7天 | ⭐⭐⭐ | 5（长期） |

---

## ✅ 验收标准

### 整体验收

- [ ] 所有核心界面使用统一的间距规范
- [ ] 所有页面导航有平滑的转场动画
- [ ] 所有按钮点击有视觉反馈
- [ ] 所有错误提示都是用户友好的
- [ ] 所有空状态都有引导操作
- [ ] 动画流畅，无掉帧（60fps）
- [ ] 深色模式下所有页面正常显示
- [ ] 通过视觉审查，界面风格统一

### 性能验收

- [ ] 页面首屏渲染时间 < 500ms
- [ ] 动画帧率稳定在 60fps
- [ ] 列表滚动流畅，无卡顿
- [ ] 内存占用无明显增加

### 代码质量验收

- [ ] 所有新增组件有完整的Preview
- [ ] 所有新增代码有KDoc注释
- [ ] 通过Lint检查，无警告
- [ ] 通过单元测试

---

## 📝 附录

### A. 设计规范文件结构

```
presentation/src/main/kotlin/com/empathy/ai/presentation/theme/
├── Color.kt              # 颜色规范（已有）
├── Type.kt               # 字体规范（已有）
├── Theme.kt              # 主题配置（已有）
├── Spacing.kt            # 间距规范（新增）
├── Animation.kt          # 动画规范（新增）
└── DesignSystem.kt       # 设计系统入口（新增）
```

### B. 组件文件结构

```
presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/
├── animation/            # 动画组件（新增）
│   ├── AnimatedListItem.kt
│   ├── ClickableScale.kt
│   └── LoadingSkeleton.kt
├── state/                # 状态组件（已有，需增强）
│   ├── EmptyView.kt
│   ├── ErrorView.kt
│   └── LoadingIndicator.kt
└── ...
```

### C. 参考资料

- [Material Design 3](https://m3.material.io/)
- [Jetpack Compose Animation](https://developer.android.com/jetpack/compose/animation)
- [Android App Quality Guidelines](https://developer.android.com/quality)

---

**文档结束**
