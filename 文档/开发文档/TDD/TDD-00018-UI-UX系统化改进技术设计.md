# TDD-00018: UI/UX系统化改进技术设计

> **文档类型**: 技术设计文档 (TDD)
> **版本**: 1.1
> **创建日期**: 2025-12-24
> **更新日期**: 2025-12-24
> **负责人**: Kiro
> **状态**: ✅ 已审查通过
> **优先级**: 🔴 高
> **关联PRD**: PRD-00017
> **关联FD**: FD-00017
> **关联调研**: RESEARCH-00036
> **审查报告**: DR-00030

---

## 📋 文档概述

### 设计目标
基于PRD-00017和FD-00017的需求，提供UI/UX系统化改进的详细技术实现方案，包括统一间距系统、交互动效系统、友好错误提示系统、空状态设计系统的技术架构和实现细节。

### 技术原则
- **向后兼容** - 扩展现有组件，不破坏已有功能
- **渐进增强** - 分阶段实施，优先核心功能
- **性能优先** - 动画使用硬件加速，避免掉帧
- **可测试性** - 所有组件支持Preview和单元测试

### 关联文档
- [PRD-00017-UI-UX系统化改进需求](../PRD/PRD-00017-UI-UX系统化改进需求.md)
- [FD-00017-UI-UX系统化改进功能设计](../FD/FD-00017-UI-UX系统化改进功能设计.md)
- [RESEARCH-00036-UI-UX系统化改进调研报告](../RE/RESEARCH-00036-UI-UX系统化改进调研报告.md)

---

## 🏗️ 整体架构设计

### 1. 架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           :presentation 模块                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                         theme/ (主题层)                                │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │  │
│  │  │ Dimensions  │  │AnimationSpec│  │  Spacing    │  │   Color     │  │  │
│  │  │   (现有)    │  │   (现有)    │  │   (新增)    │  │   (现有)    │  │  │
│  │  │ +12dp间距   │  │ +转场动画   │  │ AppSpacing  │  │             │  │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                      │                                       │
│                                      ▼                                       │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                      ui/component/ (组件层)                            │  │
│  │  ┌─────────────────────────────┐  ┌─────────────────────────────────┐ │  │
│  │  │      animation/ (新增)       │  │         state/ (增强)           │ │  │
│  │  │  ┌─────────────────────┐    │  │  ┌─────────────────────────┐   │ │  │
│  │  │  │  AnimatedListItem   │    │  │  │  EmptyView (增强)       │   │ │  │
│  │  │  │  ClickableScale     │    │  │  │  +呼吸动画 +description │   │ │  │
│  │  │  │  AnimatedViewSwitch │    │  │  │  +actionText            │   │ │  │
│  │  │  └─────────────────────┘    │  │  └─────────────────────────┘   │ │  │
│  │  │  ┌─────────────────────┐    │  │  ┌─────────────────────────┐   │ │  │
│  │  │  │  LoadingSkeleton    │    │  │  │  FriendlyErrorCard      │   │ │  │
│  │  │  └─────────────────────┘    │  │  │  (新增)                 │   │ │  │
│  │  └─────────────────────────────┘  │  └─────────────────────────┘   │ │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                      │                                       │
│                                      ▼                                       │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                         util/ (工具层)                                 │  │
│  │  ┌─────────────────────────────────────────────────────────────────┐  │  │
│  │  │                   ErrorMessageMapper (新增)                      │  │  │
│  │  │  mapError(Throwable) → FriendlyErrorMessage                     │  │  │
│  │  └─────────────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                      │                                       │
│                                      ▼                                       │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                      navigation/ (导航层)                              │  │
│  │  ┌─────────────────────────────────────────────────────────────────┐  │  │
│  │  │                    NavGraph.kt (修改)                            │  │  │
│  │  │  +enterTransition +exitTransition +popEnterTransition           │  │  │
│  │  └─────────────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2. 模块依赖关系

```
┌─────────────────────────────────────────────────────────────────┐
│                        依赖关系图                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   Screen层                                                       │
│   ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│   │ContactList   │  │ContactDetail │  │  Settings    │          │
│   │   Screen     │  │   Screen     │  │   Screen     │          │
│   └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│          │                 │                 │                   │
│          └─────────────────┼─────────────────┘                   │
│                            ▼                                     │
│   组件层                                                         │
│   ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│   │AnimatedList  │  │ EmptyView    │  │FriendlyError │          │
│   │    Item      │  │  (增强)      │  │    Card      │          │
│   └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│          │                 │                 │                   │
│          └─────────────────┼─────────────────┘                   │
│                            ▼                                     │
│   主题层                                                         │
│   ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│   │  AppSpacing  │  │AnimationSpec │  │  Dimensions  │          │
│   │   (新增)     │  │   (扩展)     │  │   (扩展)     │          │
│   └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📦 模块1：统一间距系统技术实现

### 1.1 设计目标
扩展现有`Dimensions`对象，添加缺失的12dp间距，并创建`AppSpacing`类型别名，统一所有页面的间距使用。

### 1.2 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `theme/Dimensions.kt` | 修改 | 添加`SpacingMediumSmall = 12.dp` |
| `theme/Spacing.kt` | 新增 | 创建`AppSpacing`类型别名对象 |

### 1.3 Dimensions.kt 扩展实现

**文件路径**: `presentation/src/main/kotlin/com/empathy/ai/presentation/theme/Dimensions.kt`

**修改内容**:

```kotlin
object Dimensions {
    // ========== 现有间距（保持不变） ==========
    
    /** 极小间距 (4dp) */
    val SpacingXSmall = 4.dp
    
    /** 小间距 (8dp) */
    val SpacingSmall = 8.dp
    
    /** 中等间距 (16dp) */
    val SpacingMedium = 16.dp
    
    /** 大间距 (24dp) */
    val SpacingLarge = 24.dp
    
    /** 极大间距 (32dp) */
    val SpacingXLarge = 32.dp
    
    // ========== 新增间距 ==========
    
    /**
     * 🆕 中小间距 (12dp)
     * 用于列表项间距、表单字段间距、按钮内边距
     * 填补8dp和16dp之间的间隙
     */
    val SpacingMediumSmall = 12.dp
    
    // ... 其他现有代码保持不变 ...
}
```

### 1.4 Spacing.kt 新增实现

**文件路径**: `presentation/src/main/kotlin/com/empathy/ai/presentation/theme/Spacing.kt`

**完整代码**:

```kotlin
package com.empathy.ai.presentation.theme

/**
 * 间距规范类型别名
 * 
 * 提供更简洁的API，同时保持与Dimensions的兼容性
 * 基于8dp网格系统 (8dp Grid System)
 * 
 * 使用示例:
 * ```kotlin
 * // 页面边距
 * Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)
 * 
 * // 列表项间距
 * LazyColumn(verticalArrangement = Arrangement.spacedBy(AppSpacing.md))
 * 
 * // 卡片内边距
 * Card { Column(Modifier.padding(AppSpacing.lg)) { ... } }
 * ```
 */
object AppSpacing {
    /**
     * 超小间距 (4dp)
     * 用于: 图标与文字间距、标签内边距、紧凑元素间距
     */
    val xs = Dimensions.SpacingXSmall    // 4dp
    
    /**
     * 小间距 (8dp)
     * 用于: 相关元素间距、卡片内边距、按钮组间距
     */
    val sm = Dimensions.SpacingSmall     // 8dp
    
    /**
     * 中间距 (12dp)
     * 用于: 列表项间距、表单字段间距、按钮内边距
     */
    val md = Dimensions.SpacingMediumSmall // 12dp
    
    /**
     * 大间距 (16dp)
     * 用于: 页面边距、Section间距、卡片外边距
     */
    val lg = Dimensions.SpacingMedium    // 16dp
    
    /**
     * 超大间距 (24dp)
     * 用于: 主要区域分隔、模块间距
     */
    val xl = Dimensions.SpacingLarge     // 24dp
    
    /**
     * 特大间距 (32dp)
     * 用于: 特殊强调、空状态页面边距
     */
    val xxl = Dimensions.SpacingXLarge   // 32dp
}
```

### 1.5 间距使用规范

| 场景 | 推荐间距 | 数值 | 代码示例 |
|------|----------|------|----------|
| 页面水平边距 | `AppSpacing.lg` | 16dp | `Modifier.padding(horizontal = AppSpacing.lg)` |
| 页面垂直边距 | `AppSpacing.md` | 12dp | `Modifier.padding(vertical = AppSpacing.md)` |
| 列表项间距 | `AppSpacing.md` | 12dp | `Arrangement.spacedBy(AppSpacing.md)` |
| 卡片内边距 | `AppSpacing.lg` | 16dp | `Modifier.padding(AppSpacing.lg)` |
| Section间距 | `AppSpacing.xl` | 24dp | `Spacer(Modifier.height(AppSpacing.xl))` |
| 图标与文字 | `AppSpacing.sm` | 8dp | `Arrangement.spacedBy(AppSpacing.sm)` |
| 按钮内边距 | `AppSpacing.md` | 12dp | `Modifier.padding(AppSpacing.md)` |
| 空状态页面边距 | `AppSpacing.xxl` | 32dp | `Modifier.padding(AppSpacing.xxl)` |

### 1.6 迁移策略

**第一阶段（核心界面）**:
1. ContactListScreen - 替换所有硬编码间距
2. ContactDetailScreen - 替换所有硬编码间距
3. SettingsScreen - 替换所有硬编码间距
4. ChatScreen - 替换所有硬编码间距

**第二阶段（其他界面）**:
1. ContactDetailTabScreen
2. PromptEditorScreen
3. AiConfigScreen
4. UserProfileScreen

---

## 📦 模块2：交互动效系统技术实现

### 2.1 设计目标
建立全局动画规范，为页面转场、列表操作、按钮点击等交互添加流畅的动画效果。

### 2.2 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `theme/AnimationSpec.kt` | 修改 | 添加转场动画规范 |
| `navigation/NavGraph.kt` | 修改 | 添加页面转场动画配置 |
| `ui/component/animation/AnimatedListItem.kt` | 新增 | 列表项动画组件 |
| `ui/component/animation/ClickableScale.kt` | 新增 | 点击缩放组件 |
| `ui/component/animation/AnimatedViewSwitch.kt` | 新增 | 视图切换动画组件 |
| `ui/component/state/LoadingSkeleton.kt` | 新增 | 加载骨架屏组件 |

### 2.3 AnimationSpec.kt 扩展实现

**文件路径**: `presentation/src/main/kotlin/com/empathy/ai/presentation/theme/AnimationSpec.kt`

**新增内容**:

```kotlin
object AnimationSpec {
    // ========== 现有代码保持不变 ==========
    
    // ========== 新增：页面转场动画规范 ==========
    
    /**
     * 页面进入动画时长 (300ms)
     * 用于NavHost的enterTransition
     */
    const val DurationPageEnter = 300
    
    /**
     * 页面退出动画时长 (300ms)
     * 用于NavHost的exitTransition
     */
    const val DurationPageExit = 300
    
    /**
     * 弹簧动画阻尼比
     * 0.8f表示轻微弹跳效果
     */
    const val SpringDampingRatio = 0.8f
    
    /**
     * 弹簧动画刚度
     * 400f表示适中的弹性
     */
    const val SpringStiffness = 400f
    
    /**
     * 页面进入动画规范
     * 从右侧滑入 + 淡入
     */
    val PageEnterTransition = tween<IntOffset>(
        durationMillis = DurationPageEnter,
        easing = EasingStandard
    )
    
    /**
     * 页面退出动画规范
     * 向左侧滑出 + 淡出
     */
    val PageExitTransition = tween<IntOffset>(
        durationMillis = DurationPageExit,
        easing = EasingStandard
    )
    
    /**
     * 淡入动画规范
     */
    val FadeInTransition = tween<Float>(
        durationMillis = DurationNormal,
        easing = EasingStandard
    )
    
    /**
     * 淡出动画规范
     */
    val FadeOutTransition = tween<Float>(
        durationMillis = DurationNormal,
        easing = EasingStandard
    )
}
```

### 2.4 NavGraph.kt 转场动画配置

**文件路径**: `presentation/src/main/kotlin/com/empathy/ai/presentation/navigation/NavGraph.kt`

**修改内容**:

```kotlin
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.empathy.ai.presentation.theme.AnimationSpec

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.CONTACT_LIST,
        modifier = modifier,
        // 🆕 全局页面转场动画配置
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(
                    durationMillis = AnimationSpec.DurationNormal,
                    easing = AnimationSpec.EasingStandard
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = AnimationSpec.DurationNormal,
                    easing = AnimationSpec.EasingStandard
                )
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(
                    durationMillis = AnimationSpec.DurationNormal,
                    easing = AnimationSpec.EasingStandard
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = AnimationSpec.DurationNormal,
                    easing = AnimationSpec.EasingStandard
                )
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(
                    durationMillis = AnimationSpec.DurationNormal,
                    easing = AnimationSpec.EasingStandard
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = AnimationSpec.DurationNormal,
                    easing = AnimationSpec.EasingStandard
                )
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(
                    durationMillis = AnimationSpec.DurationNormal,
                    easing = AnimationSpec.EasingStandard
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = AnimationSpec.DurationNormal,
                    easing = AnimationSpec.EasingStandard
                )
            )
        }
    ) {
        // ... 现有路由配置保持不变 ...
    }
}
```

### 2.5 AnimatedListItem.kt 实现

**文件路径**: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/animation/AnimatedListItem.kt`

**完整代码**:

```kotlin
package com.empathy.ai.presentation.ui.component.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.empathy.ai.presentation.theme.AnimationSpec

/**
 * 列表项动画包装组件
 * 
 * 为列表项提供淡入淡出 + 高度变化的动画效果
 * 适用于LazyColumn中的项目增删
 * 
 * @param visible 是否可见
 * @param modifier 修饰符
 * @param content 内容
 * 
 * 使用示例:
 * ```kotlin
 * LazyColumn {
 *     items(contacts, key = { it.id }) { contact ->
 *         AnimatedListItem(visible = true) {
 *             ContactCard(contact)
 *         }
 *     }
 * }
 * ```
 */
@Composable
fun AnimatedListItem(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically(
            animationSpec = tween(
                durationMillis = AnimationSpec.DurationNormal,
                easing = AnimationSpec.EasingStandard
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = AnimationSpec.DurationNormal,
                easing = AnimationSpec.EasingStandard
            )
        ),
        exit = shrinkVertically(
            animationSpec = tween(
                durationMillis = AnimationSpec.DurationNormal,
                easing = AnimationSpec.EasingStandard
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = AnimationSpec.DurationFast,
                easing = AnimationSpec.EasingStandard
            )
        )
    ) {
        content()
    }
}
```

### 2.6 ClickableScale.kt 实现

**文件路径**: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/animation/ClickableScale.kt`

**完整代码**:

```kotlin
package com.empathy.ai.presentation.ui.component.animation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.empathy.ai.presentation.theme.AnimationSpec

/**
 * 点击缩放动画包装组件
 * 
 * 为可点击元素提供按下时的缩放反馈效果
 * 使用弹簧动画实现自然的弹性效果
 * 
 * @param onClick 点击回调
 * @param modifier 修饰符
 * @param scaleOnPress 按下时的缩放比例，默认0.95f
 * @param enabled 是否启用点击
 * @param content 内容
 * 
 * 使用示例:
 * ```kotlin
 * ClickableScale(onClick = { /* 处理点击 */ }) {
 *     Card { Text("点击我") }
 * }
 * ```
 */
@Composable
fun ClickableScale(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    scaleOnPress: Float = 0.95f,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) scaleOnPress else 1f,
        animationSpec = spring(
            dampingRatio = AnimationSpec.SpringDampingRatio,
            stiffness = AnimationSpec.SpringStiffness
        ),
        label = "clickableScale"
    )
    
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            tryAwaitRelease()
                            pressed = false
                        },
                        onTap = { onClick() }
                    )
                }
            }
    ) {
        content()
    }
}
```

### 2.7 AnimatedViewSwitch.kt 实现

**文件路径**: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/animation/AnimatedViewSwitch.kt`

**完整代码**:

```kotlin
package com.empathy.ai.presentation.ui.component.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.empathy.ai.presentation.theme.AnimationSpec

/**
 * 视图切换动画组件
 * 
 * 为视图模式切换提供流畅的过渡动画
 * 使用淡入淡出 + 缩放效果
 * 
 * @param targetState 目标状态
 * @param modifier 修饰符
 * @param content 内容，接收当前状态作为参数
 * 
 * 使用示例:
 * ```kotlin
 * var viewMode by remember { mutableStateOf(ViewMode.Timeline) }
 * 
 * AnimatedViewSwitch(targetState = viewMode) { mode ->
 *     when (mode) {
 *         ViewMode.Timeline -> TimelineView()
 *         ViewMode.List -> ListView()
 *     }
 * }
 * ```
 */
@Composable
fun <T> AnimatedViewSwitch(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(
                animationSpec = tween(
                    durationMillis = AnimationSpec.DurationNormal,
                    easing = AnimationSpec.EasingStandard
                ),
                initialAlpha = 0.3f
            ) + scaleIn(
                initialScale = 0.95f,
                animationSpec = tween(
                    durationMillis = AnimationSpec.DurationNormal,
                    easing = AnimationSpec.EasingStandard
                )
            )).togetherWith(
                fadeOut(
                    animationSpec = tween(
                        durationMillis = AnimationSpec.DurationNormal,
                        easing = AnimationSpec.EasingStandard
                    ),
                    targetAlpha = 0.3f
                ) + scaleOut(
                    targetScale = 0.95f,
                    animationSpec = tween(
                        durationMillis = AnimationSpec.DurationNormal,
                        easing = AnimationSpec.EasingStandard
                    )
                )
            )
        },
        label = "viewSwitch"
    ) { state ->
        content(state)
    }
}
```

### 2.8 LoadingSkeleton.kt 实现

**文件路径**: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/state/LoadingSkeleton.kt`

**完整代码**:

```kotlin
package com.empathy.ai.presentation.ui.component.state

import android.content.res.Configuration
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.AppSpacing
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 加载骨架屏组件
 * 
 * 显示脉冲动画效果的占位符，用于数据加载中的状态展示
 * 比纯LoadingIndicator提供更好的用户体验
 * 
 * @param modifier 修饰符
 * @param shape 形状，默认使用小圆角
 * 
 * 使用示例:
 * ```kotlin
 * if (isLoading) {
 *     LoadingSkeleton(
 *         modifier = Modifier.fillMaxWidth().height(48.dp)
 *     )
 * } else {
 *     ActualContent()
 * }
 * ```
 */
@Composable
fun LoadingSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
    )
}

/**
 * 联系人列表项骨架屏
 * 
 * 模拟联系人列表项的加载状态
 */
@Composable
fun ContactListItemSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像骨架
        LoadingSkeleton(
            modifier = Modifier.size(56.dp),
            shape = CircleShape
        )
        
        Spacer(modifier = Modifier.width(AppSpacing.md))
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            // 名称骨架
            LoadingSkeleton(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(20.dp)
            )
            // 描述骨架
            LoadingSkeleton(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(16.dp)
            )
        }
    }
}

/**
 * 联系人列表骨架屏
 * 
 * 显示多个联系人列表项骨架
 * 
 * @param itemCount 骨架项数量，默认5个
 */
@Composable
fun ContactListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 5
) {
    Column(modifier = modifier) {
        repeat(itemCount) {
            ContactListItemSkeleton()
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "基础骨架屏", showBackground = true)
@Composable
private fun LoadingSkeletonPreview() {
    EmpathyTheme {
        LoadingSkeleton(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(48.dp)
        )
    }
}

@Preview(name = "联系人列表项骨架屏", showBackground = true)
@Composable
private fun ContactListItemSkeletonPreview() {
    EmpathyTheme {
        ContactListItemSkeleton()
    }
}

@Preview(name = "联系人列表骨架屏", showBackground = true)
@Composable
private fun ContactListSkeletonPreview() {
    EmpathyTheme {
        ContactListSkeleton(itemCount = 3)
    }
}

@Preview(name = "深色模式", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoadingSkeletonDarkPreview() {
    EmpathyTheme {
        ContactListSkeleton(itemCount = 3)
    }
}
```

---

## 📦 模块3：友好错误提示系统技术实现

### 3.1 设计目标
将技术错误转换为用户友好的提示，提供解决方案和操作按钮。

### 3.2 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `util/ErrorMessageMapper.kt` | 新增 | 错误消息映射器 |
| `util/FriendlyErrorMessage.kt` | 新增 | 友好错误消息数据类 |
| `ui/component/state/FriendlyErrorCard.kt` | 新增 | 友好错误卡片组件 |

### 3.3 ErrorMessageMapper.kt 实现

**文件路径**: `presentation/src/main/kotlin/com/empathy/ai/presentation/util/ErrorMessageMapper.kt`

**完整代码**:

```kotlin
package com.empathy.ai.presentation.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.graphics.vector.ImageVector
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 错误消息映射器
 * 
 * 将技术错误转换为用户友好的提示信息
 * 支持网络错误、HTTP错误、超时错误等常见错误类型
 */
object ErrorMessageMapper {
    
    /**
     * 将异常映射为友好错误消息
     * 
     * @param error 原始异常
     * @return 友好错误消息
     */
    fun mapError(error: Throwable): FriendlyErrorMessage {
        return when (error) {
            is UnknownHostException -> FriendlyErrorMessage(
                title = "网络连接失败",
                message = "请检查网络连接后重试",
                icon = Icons.Default.CloudOff,
                actions = listOf(
                    ErrorAction("重试", ErrorActionType.Retry)
                )
            )
            
            is SocketTimeoutException -> FriendlyErrorMessage(
                title = "请求超时",
                message = "网络响应时间过长，请稍后重试",
                icon = Icons.Default.HourglassEmpty,
                actions = listOf(
                    ErrorAction("重试", ErrorActionType.Retry)
                )
            )
            
            is HttpException -> mapHttpError(error)
            
            else -> FriendlyErrorMessage(
                title = "发生未知错误",
                message = error.message ?: "请稍后重试",
                icon = Icons.Default.Error,
                actions = listOf(
                    ErrorAction("重试", ErrorActionType.Retry)
                )
            )
        }
    }
    
    /**
     * 映射HTTP错误
     */
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
                actions = listOf(
                    ErrorAction("稍后重试", ErrorActionType.Retry)
                )
            )
            
            in 500..599 -> FriendlyErrorMessage(
                title = "服务器错误",
                message = "服务暂时不可用，请稍后重试",
                icon = Icons.Default.CloudOff,
                actions = listOf(
                    ErrorAction("重试", ErrorActionType.Retry)
                )
            )
            
            else -> FriendlyErrorMessage(
                title = "请求失败",
                message = "错误代码: ${error.code()}",
                icon = Icons.Default.Error,
                actions = listOf(
                    ErrorAction("重试", ErrorActionType.Retry)
                )
            )
        }
    }
}

/**
 * 友好错误消息数据类
 * 
 * @param title 错误标题
 * @param message 错误描述
 * @param icon 错误图标
 * @param actions 可执行的操作列表
 */
data class FriendlyErrorMessage(
    val title: String,
    val message: String,
    val icon: ImageVector,
    val actions: List<ErrorAction>
)

/**
 * 错误操作数据类
 * 
 * @param text 操作按钮文字
 * @param type 操作类型
 */
data class ErrorAction(
    val text: String,
    val type: ErrorActionType
)

/**
 * 错误操作类型枚举
 */
enum class ErrorActionType {
    /** 重试操作 */
    Retry,
    /** 关闭/取消 */
    Dismiss,
    /** 反馈问题 */
    Report,
    /** 导航到其他页面 */
    Navigate
}
```

### 3.4 FriendlyErrorCard.kt 实现

**文件路径**: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/state/FriendlyErrorCard.kt`

**完整代码**:

```kotlin
package com.empathy.ai.presentation.ui.component.state

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.AppSpacing
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.util.ErrorAction
import com.empathy.ai.presentation.util.ErrorActionType
import com.empathy.ai.presentation.util.FriendlyErrorMessage

/**
 * 友好错误卡片组件
 * 
 * 显示用户友好的错误信息，包含图标、标题、描述和操作按钮
 * 使用errorContainer颜色方案，视觉上突出错误状态
 * 
 * @param error 友好错误消息
 * @param onAction 操作回调，接收操作类型
 * @param modifier 修饰符
 * 
 * 使用示例:
 * ```kotlin
 * val error = ErrorMessageMapper.mapError(exception)
 * FriendlyErrorCard(
 *     error = error,
 *     onAction = { actionType ->
 *         when (actionType) {
 *             ErrorActionType.Retry -> viewModel.retry()
 *             ErrorActionType.Navigate -> navController.navigate("settings")
 *             else -> { /* 关闭 */ }
 *         }
 *     }
 * )
 * ```
 */
@Composable
fun FriendlyErrorCard(
    error: FriendlyErrorMessage,
    onAction: (ErrorActionType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            // 标题行：图标 + 标题
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = error.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = error.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            // 错误描述
            Text(
                text = error.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            // 操作按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm, Alignment.End)
            ) {
                error.actions.forEach { action ->
                    TextButton(
                        onClick = { onAction(action.type) }
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

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "网络错误", showBackground = true)
@Composable
private fun FriendlyErrorCardNetworkPreview() {
    EmpathyTheme {
        FriendlyErrorCard(
            error = FriendlyErrorMessage(
                title = "网络连接失败",
                message = "请检查网络连接后重试",
                icon = Icons.Default.CloudOff,
                actions = listOf(
                    ErrorAction("重试", ErrorActionType.Retry)
                )
            ),
            onAction = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "认证错误", showBackground = true)
@Composable
private fun FriendlyErrorCardAuthPreview() {
    EmpathyTheme {
        FriendlyErrorCard(
            error = FriendlyErrorMessage(
                title = "认证失败",
                message = "API密钥无效或已过期，请检查设置",
                icon = Icons.Default.CloudOff,
                actions = listOf(
                    ErrorAction("去设置", ErrorActionType.Navigate),
                    ErrorAction("取消", ErrorActionType.Dismiss)
                )
            ),
            onAction = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "深色模式", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FriendlyErrorCardDarkPreview() {
    EmpathyTheme {
        FriendlyErrorCard(
            error = FriendlyErrorMessage(
                title = "服务器错误",
                message = "服务暂时不可用，请稍后重试",
                icon = Icons.Default.CloudOff,
                actions = listOf(
                    ErrorAction("重试", ErrorActionType.Retry)
                )
            ),
            onAction = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
```

---

## 📦 模块4：空状态设计系统技术实现

### 4.1 设计目标
增强现有`EmptyView`组件和`EmptyType`密封类，添加情感化设计元素，提升用户体验。

### 4.2 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/component/state/EmptyView.kt` | 修改 | 增强EmptyType，添加呼吸动画 |

### 4.3 EmptyView.kt 增强实现

**文件路径**: `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/state/EmptyView.kt`

**完整代码**:

```kotlin
package com.empathy.ai.presentation.ui.component.state

import android.content.res.Configuration
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.AppSpacing
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 空状态类型枚举（增强版）
 * 
 * 添加description和actionText属性，支持情感化设计
 * 
 * @param icon 空状态图标
 * @param title 空状态标题
 * @param description 空状态描述
 * @param actionText 操作按钮文字，为null时不显示按钮
 */
sealed class EmptyType(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val actionText: String? = null
) {
    /** 暂无数据 */
    data object NoData : EmptyType(
        icon = Icons.Outlined.SearchOff,
        title = "暂无数据",
        description = "这里还没有任何内容",
        actionText = null
    )
    
    /** 还没有联系人 */
    data object NoContacts : EmptyType(
        icon = Icons.Outlined.PersonAdd,
        title = "还没有联系人",
        description = "添加第一个联系人，开始记录你们的故事",
        actionText = "添加联系人"
    )
    
    /** 还没有标签 */
    data object NoTags : EmptyType(
        icon = Icons.Outlined.LocalOffer,
        title = "还没有标签",
        description = "标签可以帮助你更好地了解联系人",
        actionText = "添加标签"
    )
    
    /** 没有找到结果 */
    data object NoResults : EmptyType(
        icon = Icons.Outlined.SearchOff,
        title = "没有找到结果",
        description = "试试其他关键词",
        actionText = null
    )
    
    /** 🆕 网络错误 */
    data object NetworkError : EmptyType(
        icon = Icons.Outlined.CloudOff,
        title = "网络连接失败",
        description = "请检查网络连接后重试",
        actionText = "重试"
    )
}
```

```kotlin
/**
 * 空状态视图组件（增强版）
 * 
 * 添加呼吸动画效果，提升情感化体验
 * 支持自定义操作按钮
 * 
 * @param emptyType 空状态类型
 * @param onAction 操作按钮点击回调，为null时不显示按钮
 * @param modifier 修饰符
 * 
 * 使用示例:
 * ```kotlin
 * EmptyView(
 *     emptyType = EmptyType.NoContacts,
 *     onAction = { navController.navigate("add_contact") }
 * )
 * ```
 */
@Composable
fun EmptyView(
    emptyType: EmptyType,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 带呼吸动画的图标
        val transition = rememberInfiniteTransition(label = "emptyIcon")
        val scale by transition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "iconScale"
        )
        
        Icon(
            imageVector = emptyType.icon,
            contentDescription = emptyType.title,
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.lg))
        
        // 标题
        Text(
            text = emptyType.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        // 描述
        Text(
            text = emptyType.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        // 操作按钮
        if (emptyType.actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(AppSpacing.xl))
            Button(onClick = onAction) {
                Text(emptyType.actionText)
            }
        }
    }
}

// 保留旧版API的兼容性重载
@Composable
fun EmptyView(
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    emptyType: EmptyType = EmptyType.NoData
) {
    // 使用新版实现，但保持旧版参数兼容
    EmptyView(
        emptyType = emptyType,
        onAction = if (actionText != null) onAction else null,
        modifier = modifier
    )
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "无联系人", showBackground = true)
@Composable
private fun EmptyViewNoContactsPreview() {
    EmpathyTheme {
        EmptyView(
            emptyType = EmptyType.NoContacts,
            onAction = {}
        )
    }
}

@Preview(name = "无标签", showBackground = true)
@Composable
private fun EmptyViewNoTagsPreview() {
    EmpathyTheme {
        EmptyView(
            emptyType = EmptyType.NoTags,
            onAction = {}
        )
    }
}

@Preview(name = "无搜索结果", showBackground = true)
@Composable
private fun EmptyViewNoResultsPreview() {
    EmpathyTheme {
        EmptyView(
            emptyType = EmptyType.NoResults,
            onAction = null
        )
    }
}

@Preview(name = "网络错误", showBackground = true)
@Composable
private fun EmptyViewNetworkErrorPreview() {
    EmpathyTheme {
        EmptyView(
            emptyType = EmptyType.NetworkError,
            onAction = {}
        )
    }
}

@Preview(name = "深色模式", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptyViewDarkPreview() {
    EmpathyTheme {
        EmptyView(
            emptyType = EmptyType.NoContacts,
            onAction = {}
        )
    }
}
```

---

## 🔄 数据流设计

### 5.1 间距系统数据流

```
┌─────────────────────────────────────────────────────────────┐
│                      间距系统数据流                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   Dimensions.kt                                              │
│   ┌─────────────────────────────────────────────────────┐   │
│   │  SpacingXSmall = 4.dp                               │   │
│   │  SpacingSmall = 8.dp                                │   │
│   │  SpacingMediumSmall = 12.dp  ← 🆕 新增              │   │
│   │  SpacingMedium = 16.dp                              │   │
│   │  SpacingLarge = 24.dp                               │   │
│   │  SpacingXLarge = 32.dp                              │   │
│   └─────────────────────────────────────────────────────┘   │
│                            │                                 │
│                            ▼                                 │
│   Spacing.kt (类型别名)                                      │
│   ┌─────────────────────────────────────────────────────┐   │
│   │  AppSpacing.xs = Dimensions.SpacingXSmall           │   │
│   │  AppSpacing.sm = Dimensions.SpacingSmall            │   │
│   │  AppSpacing.md = Dimensions.SpacingMediumSmall      │   │
│   │  AppSpacing.lg = Dimensions.SpacingMedium           │   │
│   │  AppSpacing.xl = Dimensions.SpacingLarge            │   │
│   │  AppSpacing.xxl = Dimensions.SpacingXLarge          │   │
│   └─────────────────────────────────────────────────────┘   │
│                            │                                 │
│                            ▼                                 │
│   Screen层使用                                               │
│   ┌─────────────────────────────────────────────────────┐   │
│   │  Modifier.padding(AppSpacing.lg)                    │   │
│   │  Arrangement.spacedBy(AppSpacing.md)                │   │
│   └─────────────────────────────────────────────────────┘   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 错误处理数据流

```
┌─────────────────────────────────────────────────────────────┐
│                      错误处理数据流                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   ViewModel层                                                │
│   ┌─────────────────────────────────────────────────────┐   │
│   │  try {                                              │   │
│   │      val result = useCase()                         │   │
│   │  } catch (e: Exception) {                           │   │
│   │      _uiState.update {                              │   │
│   │          it.copy(error = e)  ← 原始异常             │   │
│   │      }                                              │   │
│   │  }                                                  │   │
│   └─────────────────────────────────────────────────────┘   │
│                            │                                 │
│                            ▼                                 │
│   Screen层                                                   │
│   ┌─────────────────────────────────────────────────────┐   │
│   │  uiState.error?.let { error ->                      │   │
│   │      val friendlyError = ErrorMessageMapper         │   │
│   │          .mapError(error)  ← 转换为友好消息         │   │
│   │      FriendlyErrorCard(                             │   │
│   │          error = friendlyError,                     │   │
│   │          onAction = { actionType ->                 │   │
│   │              when (actionType) {                    │   │
│   │                  Retry -> viewModel.retry()         │   │
│   │                  Navigate -> navController.navigate │   │
│   │                  Dismiss -> viewModel.clearError()  │   │
│   │              }                                      │   │
│   │          }                                          │   │
│   │      )                                              │   │
│   │  }                                                  │   │
│   └─────────────────────────────────────────────────────┘   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔌 接口设计

### 6.1 AppSpacing 接口

```kotlin
/**
 * 间距规范对象
 * 提供统一的间距值访问接口
 */
object AppSpacing {
    val xs: Dp    // 4dp - 超小间距
    val sm: Dp    // 8dp - 小间距
    val md: Dp    // 12dp - 中间距
    val lg: Dp    // 16dp - 大间距
    val xl: Dp    // 24dp - 超大间距
    val xxl: Dp   // 32dp - 特大间距
}
```

### 6.2 ErrorMessageMapper 接口

```kotlin
/**
 * 错误消息映射器
 */
object ErrorMessageMapper {
    /**
     * 将异常映射为友好错误消息
     * @param error 原始异常
     * @return 友好错误消息
     */
    fun mapError(error: Throwable): FriendlyErrorMessage
}
```

### 6.3 动画组件接口

```kotlin
/**
 * 列表项动画组件
 */
@Composable
fun AnimatedListItem(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
)

/**
 * 点击缩放组件
 */
@Composable
fun ClickableScale(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    scaleOnPress: Float = 0.95f,
    enabled: Boolean = true,
    content: @Composable () -> Unit
)

/**
 * 视图切换动画组件
 */
@Composable
fun <T> AnimatedViewSwitch(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
)

/**
 * 加载骨架屏组件
 */
@Composable
fun LoadingSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small
)
```

---

## 🧪 测试策略

### 7.1 单元测试

#### AppSpacing测试

**文件路径**: `presentation/src/test/kotlin/com/empathy/ai/presentation/theme/AppSpacingTest.kt`

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
    
    @Test
    fun `AppSpacing xs should be 4dp`() {
        assertEquals(4.dp, AppSpacing.xs)
    }
    
    @Test
    fun `AppSpacing md should be 12dp`() {
        assertEquals(12.dp, AppSpacing.md)
    }
}
```

#### ErrorMessageMapper测试

**文件路径**: `presentation/src/test/kotlin/com/empathy/ai/presentation/util/ErrorMessageMapperTest.kt`

```kotlin
class ErrorMessageMapperTest {
    
    @Test
    fun `mapError should return network error for UnknownHostException`() {
        val error = UnknownHostException()
        val result = ErrorMessageMapper.mapError(error)
        
        assertEquals("网络连接失败", result.title)
        assertEquals("请检查网络连接后重试", result.message)
        assertTrue(result.actions.any { it.type == ErrorActionType.Retry })
    }
    
    @Test
    fun `mapError should return timeout error for SocketTimeoutException`() {
        val error = SocketTimeoutException()
        val result = ErrorMessageMapper.mapError(error)
        
        assertEquals("请求超时", result.title)
        assertTrue(result.actions.any { it.type == ErrorActionType.Retry })
    }
    
    @Test
    fun `mapError should return auth error for 401 HttpException`() {
        val error = HttpException(Response.error<Any>(401, "".toResponseBody()))
        val result = ErrorMessageMapper.mapError(error)
        
        assertEquals("认证失败", result.title)
        assertTrue(result.actions.any { it.type == ErrorActionType.Navigate })
    }
    
    @Test
    fun `mapError should return rate limit error for 429 HttpException`() {
        val error = HttpException(Response.error<Any>(429, "".toResponseBody()))
        val result = ErrorMessageMapper.mapError(error)
        
        assertEquals("请求过于频繁", result.title)
    }
    
    @Test
    fun `mapError should return server error for 5xx HttpException`() {
        val error = HttpException(Response.error<Any>(500, "".toResponseBody()))
        val result = ErrorMessageMapper.mapError(error)
        
        assertEquals("服务器错误", result.title)
    }
    
    @Test
    fun `mapError should return unknown error for other exceptions`() {
        val error = RuntimeException("Test error")
        val result = ErrorMessageMapper.mapError(error)
        
        assertEquals("发生未知错误", result.title)
    }
}
```

### 7.2 UI测试

#### EmptyView测试

**文件路径**: `presentation/src/androidTest/kotlin/com/empathy/ai/presentation/ui/component/state/EmptyViewTest.kt`

```kotlin
class EmptyViewTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `EmptyView should display title and description`() {
        composeTestRule.setContent {
            EmpathyTheme {
                EmptyView(emptyType = EmptyType.NoContacts)
            }
        }
        
        composeTestRule.onNodeWithText("还没有联系人").assertIsDisplayed()
        composeTestRule.onNodeWithText("添加第一个联系人，开始记录你们的故事").assertIsDisplayed()
    }
    
    @Test
    fun `EmptyView should show action button when actionText is not null`() {
        composeTestRule.setContent {
            EmpathyTheme {
                EmptyView(emptyType = EmptyType.NoContacts, onAction = {})
            }
        }
        
        composeTestRule.onNodeWithText("添加联系人").assertIsDisplayed()
    }
    
    @Test
    fun `EmptyView should not show action button when onAction is null`() {
        composeTestRule.setContent {
            EmpathyTheme {
                EmptyView(emptyType = EmptyType.NoResults, onAction = null)
            }
        }
        
        composeTestRule.onNodeWithText("添加联系人").assertDoesNotExist()
    }
    
    @Test
    fun `EmptyView action button should trigger callback`() {
        var clicked = false
        
        composeTestRule.setContent {
            EmpathyTheme {
                EmptyView(
                    emptyType = EmptyType.NoContacts,
                    onAction = { clicked = true }
                )
            }
        }
        
        composeTestRule.onNodeWithText("添加联系人").performClick()
        assertTrue(clicked)
    }
}
```

#### FriendlyErrorCard测试

**文件路径**: `presentation/src/androidTest/kotlin/com/empathy/ai/presentation/ui/component/state/FriendlyErrorCardTest.kt`

```kotlin
class FriendlyErrorCardTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `FriendlyErrorCard should display error title and message`() {
        val error = FriendlyErrorMessage(
            title = "网络连接失败",
            message = "请检查网络连接后重试",
            icon = Icons.Default.CloudOff,
            actions = listOf(ErrorAction("重试", ErrorActionType.Retry))
        )
        
        composeTestRule.setContent {
            EmpathyTheme {
                FriendlyErrorCard(error = error, onAction = {})
            }
        }
        
        composeTestRule.onNodeWithText("网络连接失败").assertIsDisplayed()
        composeTestRule.onNodeWithText("请检查网络连接后重试").assertIsDisplayed()
    }
    
    @Test
    fun `FriendlyErrorCard should display all action buttons`() {
        val error = FriendlyErrorMessage(
            title = "认证失败",
            message = "API密钥无效",
            icon = Icons.Default.Lock,
            actions = listOf(
                ErrorAction("去设置", ErrorActionType.Navigate),
                ErrorAction("取消", ErrorActionType.Dismiss)
            )
        )
        
        composeTestRule.setContent {
            EmpathyTheme {
                FriendlyErrorCard(error = error, onAction = {})
            }
        }
        
        composeTestRule.onNodeWithText("去设置").assertIsDisplayed()
        composeTestRule.onNodeWithText("取消").assertIsDisplayed()
    }
    
    @Test
    fun `FriendlyErrorCard action button should trigger callback with correct type`() {
        var actionType: ErrorActionType? = null
        val error = FriendlyErrorMessage(
            title = "错误",
            message = "消息",
            icon = Icons.Default.Error,
            actions = listOf(ErrorAction("重试", ErrorActionType.Retry))
        )
        
        composeTestRule.setContent {
            EmpathyTheme {
                FriendlyErrorCard(error = error, onAction = { actionType = it })
            }
        }
        
        composeTestRule.onNodeWithText("重试").performClick()
        assertEquals(ErrorActionType.Retry, actionType)
    }
}
```

### 7.3 测试覆盖率目标

| 模块 | 目标覆盖率 | 测试类型 |
|------|-----------|----------|
| AppSpacing | 100% | 单元测试 |
| ErrorMessageMapper | 90%+ | 单元测试 |
| AnimatedListItem | 80%+ | UI测试 |
| ClickableScale | 80%+ | UI测试 |
| LoadingSkeleton | 80%+ | UI测试 |
| EmptyView | 90%+ | UI测试 |
| FriendlyErrorCard | 90%+ | UI测试 |

### 7.4 测试策略详细说明

#### 单元测试策略

| 测试层级 | 测试框架 | 测试重点 | 运行环境 |
|----------|----------|----------|----------|
| Theme层 | JUnit 4 + Truth | 间距值正确性、动画参数验证 | JVM |
| Util层 | JUnit 4 + MockK | 错误映射逻辑、边界条件 | JVM |
| 组件层 | Compose Test | 组件渲染、交互响应 | Android |

#### UI测试策略

```kotlin
// 动画组件测试示例
@Test
fun `AnimatedListItem should animate visibility change`() {
    var visible by mutableStateOf(false)
    
    composeTestRule.setContent {
        AnimatedListItem(visible = visible) {
            Text("Test Content")
        }
    }
    
    // 初始状态不可见
    composeTestRule.onNodeWithText("Test Content").assertDoesNotExist()
    
    // 切换为可见
    visible = true
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Test Content").assertIsDisplayed()
}
```

#### 性能测试策略

| 测试项 | 测试方法 | 通过标准 |
|--------|----------|----------|
| 动画帧率 | Macrobenchmark | 稳定60fps，P95 > 55fps |
| 页面渲染 | Compose Metrics | 首帧渲染 < 500ms |
| 内存占用 | Memory Profiler | 无内存泄漏，增量 < 5MB |
| 列表滚动 | Scroll Benchmark | 无掉帧，流畅度 > 90% |

---

## 📈 性能影响评估

### 8.1 构建时间影响

| 构建类型 | 预期影响 | 说明 |
|----------|----------|------|
| 全量构建 | +5-10% | 新增组件和测试文件 |
| 增量编译 | 无明显影响 | 仅修改文件重编译 |
| 测试构建 | +10-15% | 新增UI测试用例 |

### 8.2 运行时性能影响

| 性能指标 | 预期影响 | 优化措施 |
|----------|----------|----------|
| 动画性能 | 可能影响低端设备 | 使用`graphicsLayer`硬件加速 |
| 内存占用 | +2-5MB | 动画使用`remember`缓存 |
| 启动时间 | 无明显影响 | 延迟加载动画组件 |
| 列表滚动 | 可能轻微影响 | 使用`key`优化重组 |

### 8.3 性能优化策略

```kotlin
// 1. 使用graphicsLayer进行硬件加速
Modifier.graphicsLayer {
    scaleX = scale
    scaleY = scale
}

// 2. 使用remember缓存动画状态
val transition = remember { MutableTransitionState(false) }

// 3. 使用derivedStateOf减少重组
val isAnimating by remember {
    derivedStateOf { transition.isRunning }
}

// 4. 列表项使用key优化
LazyColumn {
    items(items, key = { it.id }) { item ->
        AnimatedListItem(visible = true) {
            ItemContent(item)
        }
    }
}
```

### 8.4 性能基准测试

**测试设备基准**：
- 高端设备：Pixel 7 Pro (Android 14)
- 中端设备：Pixel 4a (Android 13)
- 低端设备：Redmi Note 8 (Android 11)

**性能目标**：

| 设备类型 | 动画帧率 | 页面渲染 | 列表滚动 |
|----------|----------|----------|----------|
| 高端设备 | 60fps | < 300ms | 流畅 |
| 中端设备 | 60fps | < 500ms | 流畅 |
| 低端设备 | 45fps+ | < 800ms | 基本流畅 |

---

## 🔄 CI/CD集成建议

### 9.1 构建流水线调整

```yaml
# GitHub Actions 示例配置
jobs:
  ui-ux-tests:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4
      
      - name: Setup JDK
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run Unit Tests
        run: ./gradlew :presentation:testDebugUnitTest
      
      - name: Run UI Tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 31
          script: ./gradlew :presentation:connectedDebugAndroidTest
      
      - name: Upload Test Results
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: presentation/build/reports/
```

### 9.2 自动化检查清单

| 检查项 | 工具 | 触发条件 |
|--------|------|----------|
| Lint检查 | Android Lint | 每次PR |
| 单元测试 | JUnit | 每次PR |
| UI测试 | Compose Test | 每次PR |
| 性能测试 | Macrobenchmark | 每周/Release前 |
| 代码覆盖率 | JaCoCo | 每次PR |

### 9.3 质量门禁

| 指标 | 阈值 | 说明 |
|------|------|------|
| 单元测试通过率 | 100% | 所有测试必须通过 |
| UI测试通过率 | 95%+ | 允许少量Flaky测试 |
| 代码覆盖率 | 80%+ | 新增代码覆盖率要求 |
| Lint警告 | 0 | 不允许新增警告 |

### 9.4 增量测试策略

```bash
# 基于变更文件选择性运行测试
# 如果修改了theme/目录，运行主题相关测试
if git diff --name-only | grep -q "theme/"; then
  ./gradlew :presentation:testDebugUnitTest --tests "*SpacingTest*"
  ./gradlew :presentation:testDebugUnitTest --tests "*AnimationSpecTest*"
fi

# 如果修改了component/animation/目录，运行动画组件测试
if git diff --name-only | grep -q "component/animation/"; then
  ./gradlew :presentation:connectedDebugAndroidTest --tests "*AnimatedListItemTest*"
  ./gradlew :presentation:connectedDebugAndroidTest --tests "*ClickableScaleTest*"
fi
```

---

## 📊 实施计划

### 10.1 阶段划分

| 阶段 | 内容 | 工作量 | 优先级 | 依赖 |
|------|------|--------|--------|------|
| 第一阶段 | 统一间距系统 | 2-3天 | 🔴 高 | 无 |
| 第二阶段 | 交互动效系统 | 3-4天 | 🔴 高 | 第一阶段 |
| 第三阶段 | 友好错误提示 | 1-2天 | 🟡 中 | 无 |
| 第四阶段 | 空状态设计 | 1-2天 | 🟡 中 | 第一阶段 |
| 第五阶段 | 悬浮窗迁移 | 5-7天 | 🟢 低 | 全部 |

### 10.2 详细任务分解

#### 第一阶段：统一间距系统（2-3天）

| 任务ID | 任务 | 说明 | 预估 |
|--------|------|------|------|
| T1-01 | 扩展Dimensions对象 | 添加SpacingMediumSmall = 12.dp | 0.25天 |
| T1-02 | 创建Spacing.kt | 创建AppSpacing类型别名对象 | 0.25天 |
| T1-03 | 编写AppSpacing单元测试 | 验证间距值正确性 | 0.25天 |
| T1-04 | 更新ContactListScreen | 替换硬编码间距 | 0.5天 |
| T1-05 | 更新ContactDetailScreen | 替换硬编码间距 | 0.5天 |
| T1-06 | 更新SettingsScreen | 替换硬编码间距 | 0.5天 |
| T1-07 | 更新ChatScreen | 替换硬编码间距 | 0.5天 |
| T1-08 | 视觉验证 | 检查所有页面间距一致性 | 0.25天 |

#### 第二阶段：交互动效系统（3-4天）

| 任务ID | 任务 | 说明 | 预估 |
|--------|------|------|------|
| T2-01 | 扩展AnimationSpec | 添加转场动画规范常量 | 0.25天 |
| T2-02 | 配置NavHost转场动画 | 在NavGraph中添加动画配置 | 0.5天 |
| T2-03 | 创建AnimatedListItem | 列表项动画包装器 | 0.5天 |
| T2-04 | 创建ClickableScale | 按钮点击反馈组件 | 0.5天 |
| T2-05 | 创建AnimatedViewSwitch | 视图切换动画组件 | 0.5天 |
| T2-06 | 创建LoadingSkeleton | 加载骨架屏组件 | 0.5天 |
| T2-07 | 应用到ContactListScreen | 集成动画组件 | 0.5天 |
| T2-08 | 应用到其他页面 | 集成动画组件 | 0.5天 |
| T2-09 | 性能测试 | 确保60fps | 0.25天 |

#### 第三阶段：友好错误提示（1-2天）

| 任务ID | 任务 | 说明 | 预估 |
|--------|------|------|------|
| T3-01 | 创建ErrorMessageMapper | 错误消息映射器 | 0.5天 |
| T3-02 | 创建FriendlyErrorCard | 友好错误卡片组件 | 0.5天 |
| T3-03 | 编写单元测试 | ErrorMessageMapper测试 | 0.25天 |
| T3-04 | 编写UI测试 | FriendlyErrorCard测试 | 0.25天 |
| T3-05 | 集成到核心页面 | 替换现有错误处理 | 0.5天 |

#### 第四阶段：空状态设计（1-2天）

| 任务ID | 任务 | 说明 | 预估 |
|--------|------|------|------|
| T4-01 | 增强EmptyType | 添加description和actionText | 0.25天 |
| T4-02 | 增强EmptyView | 添加呼吸动画效果 | 0.5天 |
| T4-03 | 编写UI测试 | EmptyView测试 | 0.25天 |
| T4-04 | 更新ContactListScreen | 使用增强空状态 | 0.25天 |
| T4-05 | 更新其他页面 | 使用增强空状态 | 0.5天 |
| T4-06 | 视觉验证 | 检查所有空状态一致性 | 0.25天 |

---

## ✅ 验收标准

### 11.1 功能验收

| 验收项 | 标准 | 优先级 |
|--------|------|--------|
| 间距统一 | 所有页面使用AppSpacing，无硬编码 | 🔴 高 |
| 页面转场 | 所有导航有平滑的滑入滑出动画 | 🔴 高 |
| 列表动画 | 列表项增删有淡入淡出动画 | 🔴 高 |
| 按钮反馈 | 按钮点击有缩放反馈 | 🔴 高 |
| 错误提示 | 所有错误显示友好提示 | 🟡 中 |
| 空状态 | 所有空状态有图标、标题、描述、操作 | 🟡 中 |

### 11.2 性能验收

| 验收项 | 标准 |
|--------|------|
| 动画帧率 | 稳定60fps，无掉帧 |
| 页面渲染 | 首屏渲染 < 500ms |
| 列表滚动 | 流畅无卡顿 |
| 内存占用 | 无明显增加 |

### 11.3 代码质量验收

| 验收项 | 标准 |
|--------|------|
| Preview | 所有新增组件有完整Preview |
| 注释 | 所有新增代码有KDoc注释 |
| Lint | 通过Lint检查，无警告 |
| 测试 | 关键组件有单元测试，覆盖率>80% |

---

## ⚠️ 风险与缓解

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|----------|
| 间距修改导致布局问题 | 中 | 中 | 逐页面验证，充分测试 |
| 动画导致性能问题 | 低 | 高 | 使用硬件加速，监控帧率 |
| 旧版API兼容性问题 | 低 | 中 | 保留旧版重载函数 |
| 工作量超出预期 | 中 | 中 | 分阶段实施，优先核心功能 |

---

## 📝 附录

### A. 文件结构总览

```
presentation/src/main/kotlin/com/empathy/ai/presentation/
├── theme/
│   ├── Dimensions.kt          # 修改：添加SpacingMediumSmall
│   ├── AnimationSpec.kt       # 修改：添加转场动画规范
│   ├── Spacing.kt             # 🆕 新增：AppSpacing类型别名
│   └── ...
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

### B. 参考资料

- [Material Design 3 - Motion](https://m3.material.io/styles/motion/overview)
- [Jetpack Compose Animation](https://developer.android.com/jetpack/compose/animation)
- [Android App Quality Guidelines](https://developer.android.com/quality)
- [Compose Navigation Animation](https://developer.android.com/jetpack/compose/navigation#animated-nav-host)

### C. 术语表

| 术语 | 解释 |
|------|------|
| AppSpacing | 应用间距规范对象，提供统一的间距值 |
| AnimationSpec | 动画规范对象，定义动画时长和缓动曲线 |
| ErrorMessageMapper | 错误信息映射器，将技术错误转换为友好提示 |
| EmptyType | 空状态类型枚举，定义不同场景的空状态 |
| LoadingSkeleton | 加载骨架屏组件，显示脉冲动画占位符 |
| ClickableScale | 点击缩放组件，提供按下时的缩放反馈 |

---

**文档版本**: 1.1  
**最后更新**: 2025-12-24  
**更新内容**: 根据DR-00030审查建议，补充性能影响评估、增强测试策略、添加CI/CD集成建议  
**下一步**: 创建TD任务清单文档
