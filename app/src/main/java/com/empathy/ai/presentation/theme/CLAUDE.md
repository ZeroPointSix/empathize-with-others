# Presentation Theme - Compose主题模块

[根目录](../../../../CLAUDE.md) > [app](../../../) > [presentation](../../) > [theme](../) > **theme**

## 模块职责

Presentation Theme模块负责定义和管理应用的视觉主题系统，包括颜色、字体、尺寸、动画和语义化颜色等。该模块基于Material Design 3设计规范，支持深色模式、动态颜色和自定义主题配置。

## 核心组件

### 1. Theme - 主题配置
- **文件**: `Theme.kt`
- **职责**: 定义应用的主主题和配置
- **功能**:
  - 配置浅色/深色配色方案
  - 支持Android 12+的动态颜色
  - 设置状态栏样式
  - 提供语义化颜色

**主要组件**:
```kotlin
@Composable
fun EmpathyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // 主题配置逻辑
}
```

### 2. Color - 颜色定义
- **文件**: `Color.kt`
- **职责**: 定义所有颜色资源
- **功能**:
  - 定义Material Design 3颜色系统
  - 支持浅色/深色模式颜色
  - 提供品牌色彩

**颜色类别**:
- Primary: 主色调
- Secondary: 次要色
- Tertiary: 第三色
- Surface: 表面色
- Background: 背景色
- Error: 错误色

### 3. SemanticColors - 语义化颜色
- **文件**: `SemanticColors.kt`
- **职责**: 定义具有语义的颜色
- **功能**:
  - 提供业务相关的颜色定义
  - 支持关系类型颜色
  - 便于主题维护和修改

**语义颜色示例**:
```kotlin
data class SemanticColors(
    val positive: Color,      // 积极/成功
    val negative: Color,      // 消极/危险
    val neutral: Color,       // 中性
    val relationship: Map<RelationshipType, Color>  // 关系类型颜色
)
```

### 4. RelationshipColors - 关系颜色
- **文件**: `RelationshipColors.kt`
- **职责**: 定义联系人关系的颜色映射
- **功能**:
  - 为不同关系类型分配颜色
  - 支持自定义关系颜色
  - 提供颜色一致性

### 5. Type - 字体排版
- **文件**: `Type.kt`
- **职责**: 定义应用的字体系统
- **功能**:
  - 配置Material Design 3字体规范
  - 定义不同级别的字体样式
  - 支持动态字体缩放

**字体级别**:
- Display: 大标题
- Headline: 标题
- Title: 子标题
- Body: 正文
- Label: 标签

### 6. Dimensions - 尺寸规范
- **文件**: `Dimensions.kt`
- **职责**: 定义应用的尺寸系统
- **功能**:
  - 统一边距、圆角、高度等尺寸
  - 响应式设计支持
  - 保持设计一致性

### 7. AnimationSpec - 动画规范
- **文件**: `AnimationSpec.kt`
- **职责**: 定义动画参数和时长
- **功能**:
  - 统一动画时长和缓动函数
  - 提供预设动画组合
  - 确保动画体验一致性

## 主题系统架构

### 颜色系统层次
```mermaid
graph TD
    A[Material 3 ColorScheme] --> B[Light/Dark Scheme]
    A --> C[Dynamic Color (Android 12+)]

    B --> D[品牌色 Primary/Secondary/Tertiary]
    B --> E[系统色 Surface/Background/Error]

    D --> F[Semantic Colors]
    E --> F

    F --> G[Business Colors]
    F --> H[Relationship Colors]
```

### 主题应用流程
1. **检测系统设置**: 深色模式、动态颜色支持
2. **选择配色方案**: 根据设置选择合适的颜色方案
3. **应用语义化颜色**: 将系统颜色映射到业务语义
4. **配置其他属性**: 字体、尺寸、动画等
5. **提供CompositionLocal**: 通过CompositionLocalProvider全局提供

## 使用示例

### 基本主题应用
```kotlin
@Composable
fun MyApp() {
    EmpathyTheme(
        darkTheme = false,  // 强制浅色模式
        dynamicColor = true // 启用动态颜色
    ) {
        // 应用内容
        MyAppContent()
    }
}
```

### 使用主题颜色
```kotlin
@Composable
fun ThemedButton() {
    Button(
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text("按钮")
    }
}
```

### 使用语义化颜色
```kotlin
@Composable
fun StatusIndicator(status: Status) {
    val color = when (status) {
        Status.SUCCESS -> LocalSemanticColors.current.positive
        Status.ERROR -> LocalSemanticColors.current.negative
        else -> LocalSemanticColors.current.neutral
    }

    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, CircleShape)
    )
}
```

### 使用关系颜色
```kotlin
@Composable
fun RelationshipTag(type: RelationshipType) {
    val color = RelationshipColors.getColor(type)

    Text(
        text = type.displayName,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
```

### 使用字体规范
```kotlin
@Composable
fun TypographyExample() {
    Text(
        text = "大标题",
        style = MaterialTheme.typography.displayLarge
    )
    Text(
        text = "正文内容",
        style = MaterialTheme.typography.bodyMedium
    )
}
```

### 使用尺寸规范
```kotlin
@Composable
fun SpacedContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(Dimensions.spacing.medium)
    ) {
        // 内容项
    }
}
```

### 使用动画规范
```kotlin
@Composable
fun AnimatedContent() {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = AnimationSpec.fadeIn),
        exit = fadeOut(animationSpec = AnimationSpec.fadeOut)
    ) {
        // 可见内容
    }
}
```

## 主题定制

### 1. 扩展主题属性
```kotlin
// 添加自定义的CompositionLocal
val CustomColors = staticCompositionLocalOf {
    CustomColorScheme()
}

// 在主题中提供
CompositionLocalProvider(
    LocalSemanticColors provides semanticColors,
    CustomColors provides customColors
) {
    MaterialTheme(...)
}
```

### 2. 创建主题变体
```kotlin
@Composable
fun EmpathyCompactTheme(
    content: @Composable () -> Unit
) {
    EmpathyTheme {
        // 覆盖部分样式
        CompositionLocalProvider(
            LocalSpacing provides CompactSpacing
        ) {
            content()
        }
    }
}
```

## 最佳实践

### 1. 颜色使用
- 优先使用MaterialTheme.colorScheme中的颜色
- 对于有特定含义的场景使用语义化颜色
- 避免硬编码颜色值

### 2. 字体使用
- 使用预定义的Typography样式
- 保持字体层级清晰
- 支持系统字体大小设置

### 3. 尺寸规范
- 使用Dimensions中定义的尺寸值
- 保持间距和尺寸的一致性
- 考虑不同屏幕尺寸的适配

### 4. 动画设计
- 使用统一的动画时长和缓动
- 保持动画的性能优化
- 尊重用户的动画偏好设置

## 性能优化

### 1. 避免重组
- 使用@Stable注解标记主题相关类
- 使用remember缓存主题计算结果
- 避免在主题中进行复杂计算

### 2. 懒加载资源
- 延迟加载字体资源
- 按需加载颜色资源
- 缓存主题相关的对象

## 测试策略

### 1. 主题测试
```kotlin
@Test
fun testThemeColors() {
    composeTestRule.setContent {
        EmpathyTheme {
            Surface(
                color = MaterialTheme.colorScheme.primary
            ) {
                // 验证颜色
            }
        }
    }
}
```

### 2. 深色模式测试
```kotlin
@Test
fun testDarkTheme() {
    composeTestRule.setContent {
        EmpathyTheme(darkTheme = true) {
            // 测试深色模式样式
        }
    }
}
```

## 常见问题

### 1. 动态颜色不生效
- 确保设备是Android 12+
- 检查是否启用了dynamicColor参数
- 验证壁纸是否支持提取颜色

### 2. 主题切换闪烁
- 确保主题状态管理正确
- 使用适当的动画过渡
- 避免频繁的主题切换

### 3. 字体不一致
- 检查是否正确使用了Typography
- 确保字体资源正确加载
- 验证系统字体设置

## 相关文件清单

- Theme.kt - 主题配置和主入口
- Color.kt - Material Design 3颜色定义
- SemanticColors.kt - 语义化颜色
- RelationshipColors.kt - 关系类型颜色
- Type.kt - 字体排版系统
- Dimensions.kt - 尺寸和间距规范
- AnimationSpec.kt - 动画参数定义

## 变更记录 (Changelog)

### 2025-12-20 - Claude (模块文档创建)
- **创建presentation/theme模块文档**
- **记录7个主题组件的功能和用法**
- **提供主题系统架构和使用示例**