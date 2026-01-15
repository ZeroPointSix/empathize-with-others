# Presentation Theme 模块文档

> [根目录](../../../../CLAUDE.md) > [项目文档](../../README.md) > [presentation](../README.md) > **theme**

## 模块职责

Presentation Theme模块负责应用主题配置：
- **颜色系统**: 定义应用颜色规范
- **字体排版**: 定义字体样式
- **形状定义**: 定义组件形状
- **动画规范**: 定义动画参数

## 核心组件

### 颜色系统

#### Color.kt
- **职责**: 颜色定义
- **内容**:
  - 主色调（Primary）
  - 次色调（Secondary）
  - 错误色（Error）
  - 背景色（Background）
  - 表面色（Surface）
  - 自定义颜色

#### AvatarColors.kt
- **职责**: 头像颜色分配
- **功能**: 基于姓名自动生成头像颜色

### 字体排版

#### Type.kt
- **职责**: 字体样式定义
- **内容**:
  - 显示样式（Display）
  - 标题样式（Headline）
  - 标题（Title）
  - 正文（Body）
  - 标签（Label）

### 尺寸规范

#### Dimensions.kt
- **职责**: 尺寸常量定义
- **内容**:
  - 间距（Spacing）
  - 圆角（Radius）
  - 图标大小（IconSize）
  - 组件高度（ComponentHeight）

#### Spacing.kt
- **职责**: 间距规范
- **内容**:
  - xs, sm, md, lg, xl 间距级别

### 动画规范

#### AnimationSpec.kt
- **职责**: 动画参数定义
- **内容**:
  - 淡入淡出动画
  - 滑动动画
  - 缩放动画
  - 弹性动画

## 主题配置

### 主题定义

```kotlin
@Composable
fun EmpathyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

### 颜色方案

#### 浅色主题
```kotlin
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2196F3),
    secondary = Color(0xFF03A9F4),
    error = Color(0xFFEF5350),
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
    // ...
)
```

#### 深色主题
```kotlin
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    secondary = Color(0xFF81D4FA),
    error = Color(0xFFEF5350),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    // ...
)
```

## 使用示例

### 应用主题

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EmpathyTheme {
                MainScreen()
            }
        }
    }
}
```

### 使用颜色

```kotlin
@Composable
fun ExampleComponent() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Text(
            text = "Hello",
            color = MaterialTheme.colorScheme.primary
        )
    }
}
```

### 使用字体样式

```kotlin
@Composable
fun TitleText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge
    )
}
```

### 使用尺寸

```kotlin
@Composable
fun SpacerExample() {
    Spacer(modifier = Modifier.height(Dimensions.Spacing.md))
}
```

### 使用动画

```kotlin
@Composable
fun AnimatedVisibilityExample() {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = AnimationSpec.FadeIn)
    ) {
        Content()
    }
}
```

## 设计规范

### 颜色规范
- 遵循Material 3设计规范
- 支持浅色/深色主题
- 动态颜色适配（Android 12+）

### 字体规范
- 使用系统默认字体
- 响应式字体大小
- 支持无障碍设置

### 间距规范
- 使用8dp基准网格
- 一致的间距级别
- 预定义的间距常量

### 动画规范
- 流畅的过渡动画
- 符合Material Motion
- 可自定义动画参数

## 相关文件清单

### 主题配置
- `Color.kt` - 颜色定义
- `Type.kt` - 字体样式
- `Theme.kt` - 主题配置

### 尺寸规范
- `Dimensions.kt` - 尺寸常量
- `Spacing.kt` - 间距规范

### 动画规范
- `AnimationSpec.kt` - 动画参数

### 辅助文件
- `AvatarColors.kt` - 头像颜色

## 变更记录

### 2025-12-25 - 初始创建
- 创建presentation/theme模块文档
