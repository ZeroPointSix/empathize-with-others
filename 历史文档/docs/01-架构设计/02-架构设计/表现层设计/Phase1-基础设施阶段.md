# Phase1: 基础设施阶段

## 📋 阶段概览

**目标**: 建立UI层的基础框架,为后续组件和Screen开发做好准备

**预计工期**: 1-2天

**优先级**: P0 (必须完成)

**前置条件**:
- ✅ Domain Layer (业务层) 100%完成
- ✅ ViewModel层 100%完成
- ✅ UiState/UiEvent定义完成

**交付物**:
1. 完善的深色模式配色方案 (`Color.kt`)
2. 导航系统框架 (`NavRoutes.kt`, `NavGraph.kt`)
3. MainActivity的Compose集成
4. 依赖注入配置 (`HiltModule`)

---

## 一、深色模式配色方案 (Color.kt)

### 1.1 任务目标

完善主题系统中的Color.kt,提供完整的Material Design 3配色方案,支持浅色和深色两种模式。

### 1.2 实现规范

#### 文件结构
```
presentation/
└── theme/
    ├── Color.kt     ← 需要创建/完善
    ├── Theme.kt     ← 已存在,需要更新引用
    └── Type.kt      ← 已存在
```

#### Color.kt 完整实现

**文件路径**: `app/src/main/java/com/empathy/ai/presentation/theme/Color.kt`

```kotlin
package com.empathy.ai.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * 品牌主色定义
 * 
 * 设计原则:
 * - 浅色模式使用较深的颜色作为Primary
 * - 深色模式使用较浅的颜色作为Primary
 * - 确保对比度符合WCAG AA标准 (4.5:1)
 */

// ============================================================
// 浅色模式颜色 (Light Mode Colors)
// ============================================================

// Primary - 主色调 (紫色系)
val PrimaryLight = Color(0xFF6750A4)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFEADDFF)
val OnPrimaryContainerLight = Color(0xFF21005D)

// Secondary - 辅助色 (紫灰色系)
val SecondaryLight = Color(0xFF625B71)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFE8DEF8)
val OnSecondaryContainerLight = Color(0xFF1D192B)

// Tertiary - 第三色 (粉色系)
val TertiaryLight = Color(0xFF7D5260)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFD8E4)
val OnTertiaryContainerLight = Color(0xFF31111D)

// Error - 错误色 (红色系)
val ErrorLight = Color(0xFFB3261E)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)

// Background & Surface - 背景和表面
val BackgroundLight = Color(0xFFFFFBFE)
val OnBackgroundLight = Color(0xFF1C1B1F)
val SurfaceLight = Color(0xFFFFFBFE)
val OnSurfaceLight = Color(0xFF1C1B1F)
val SurfaceVariantLight = Color(0xFFE7E0EC)
val OnSurfaceVariantLight = Color(0xFF49454F)

// Outline - 边框
val OutlineLight = Color(0xFF79747E)
val OutlineVariantLight = Color(0xFFCAC4D0)

// ============================================================
// 深色模式颜色 (Dark Mode Colors)
// ============================================================

// Primary - 主色调 (紫色系,更亮)
val PrimaryDark = Color(0xFFD0BCFF)
val OnPrimaryDark = Color(0xFF381E72)
val PrimaryContainerDark = Color(0xFF4F378B)
val OnPrimaryContainerDark = Color(0xFFEADDFF)

// Secondary - 辅助色 (紫灰色系,更亮)
val SecondaryDark = Color(0xFFCCC2DC)
val OnSecondaryDark = Color(0xFF332D41)
val SecondaryContainerDark = Color(0xFF4A4458)
val OnSecondaryContainerDark = Color(0xFFE8DEF8)

// Tertiary - 第三色 (粉色系,更亮)
val TertiaryDark = Color(0xFFEFB8C8)
val OnTertiaryDark = Color(0xFF492532)
val TertiaryContainerDark = Color(0xFF633B48)
val OnTertiaryContainerDark = Color(0xFFFFD8E4)

// Error - 错误色 (红色系,更亮)
val ErrorDark = Color(0xFFF2B8B5)
val OnErrorDark = Color(0xFF601410)
val ErrorContainerDark = Color(0xFF8C1D18)
val OnErrorContainerDark = Color(0xFFF9DEDC)

// Background & Surface - 背景和表面 (深色)
val BackgroundDark = Color(0xFF1C1B1F)
val OnBackgroundDark = Color(0xFFE6E1E5)
val SurfaceDark = Color(0xFF1C1B1F)
val OnSurfaceDark = Color(0xFFE6E1E5)
val SurfaceVariantDark = Color(0xFF49454F)
val OnSurfaceVariantDark = Color(0xFFCAC4D0)

// Outline - 边框
val OutlineDark = Color(0xFF938F99)
val OutlineVariantDark = Color(0xFF49454F)

// ============================================================
// 语义化颜色 (Semantic Colors)
// ============================================================

// Success - 成功色 (绿色系)
val SuccessLight = Color(0xFF4CAF50)
val SuccessDark = Color(0xFF81C784)

// Warning - 警告色 (橙色系)
val WarningLight = Color(0xFFFF9800)
val WarningDark = Color(0xFFFFB74D)

// Info - 信息色 (蓝色系)
val InfoLight = Color(0xFF2196F3)
val InfoDark = Color(0xFF64B5F6)
```

#### Theme.kt 更新

**文件路径**: `app/src/main/java/com/empathy/ai/presentation/theme/Theme.kt`

```kotlin
package com.empathy.ai.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 浅色配色方案
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

/**
 * 深色配色方案
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

/**
 * Empathy应用主题
 * 
 * @param darkTheme 是否使用深色模式,默认跟随系统
 * @param dynamicColor 是否使用动态颜色 (Android 12+)
 * @param content 主题内容
 */
@Composable
fun EmpathyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Android 12+ 支持动态颜色
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) 
            else dynamicLightColorScheme(context)
        }
        // 深色模式
        darkTheme -> DarkColorScheme
        // 浅色模式
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

### 1.3 验证清单

- [ ] Color.kt包含完整的浅色/深色配色
- [ ] 所有颜色对比度符合WCAG AA标准
- [ ] Theme.kt正确引用Color.kt中的颜色
- [ ] 在Android Studio预览中测试深色/浅色切换

---

## 二、导航系统设计

### 2.1 任务目标

创建类型安全的导航系统,管理页面跳转和参数传递。

### 2.2 NavRoutes.kt - 路由定义

**文件路径**: `app/src/main/java/com/empathy/ai/presentation/ui/navigation/NavRoutes.kt`

```kotlin
package com.empathy.ai.presentation.ui.navigation

/**
 * 导航路由定义
 * 
 * 设计原则:
 * 1. 使用sealed class确保类型安全
 * 2. 无参路由直接使用route字符串
 * 3. 带参路由提供createRoute()辅助函数
 * 4. 参数名使用常量,避免硬编码
 */
sealed class NavRoutes(val route: String) {
    
    /**
     * 联系人列表页
     * 
     * 路由: contact_list
     * 参数: 无
     */
    object ContactList : NavRoutes("contact_list")
    
    /**
     * 联系人详情页
     * 
     * 路由: contact_detail/{contactId}
     * 参数: 
     * - contactId: String - 联系人ID
     */
    object ContactDetail : NavRoutes("contact_detail/{$ARG_CONTACT_ID}") {
        const val ARG_CONTACT_ID = "contactId"
        
        fun createRoute(contactId: String): String {
            return "contact_detail/$contactId"
        }
    }
    
    /**
     * 聊天分析页
     * 
     * 路由: chat/{contactId}
     * 参数:
     * - contactId: String - 联系人ID
     */
    object Chat : NavRoutes("chat/{$ARG_CONTACT_ID}") {
        const val ARG_CONTACT_ID = "contactId"
        
        fun createRoute(contactId: String): String {
            return "chat/$contactId"
        }
    }
    
    /**
     * 设置页 (预留)
     * 
     * 路由: settings
     * 参数: 无
     */
    object Settings : NavRoutes("settings")
    
    /**
     * 关于页 (预留)
     * 
     * 路由: about
     * 参数: 无
     */
    object About : NavRoutes("about")
}
```

### 2.3 NavGraph.kt - 导航图

**文件路径**: `app/src/main/java/com/empathy/ai/presentation/ui/navigation/NavGraph.kt`

```kotlin
package com.empathy.ai.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/**
 * 应用导航图
 * 
 * 职责:
 * 1. 定义所有页面的路由
 * 2. 管理页面间的跳转
 * 3. 处理路由参数传递
 * 
 * @param navController 导航控制器
 * @param modifier 修饰符
 * @param startDestination 起始页面,默认为联系人列表
 */
@Composable
fun EmpathyNavGraph(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
    startDestination: String = NavRoutes.ContactList.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // ============================================================
        // 联系人列表页
        // ============================================================
        composable(route = NavRoutes.ContactList.route) {
            // TODO: Phase3 - 实现ContactListScreen
            // ContactListScreen(
            //     onNavigateToDetail = { contactId ->
            //         navController.navigate(NavRoutes.ContactDetail.createRoute(contactId))
            //     },
            //     onNavigateToChat = { contactId ->
            //         navController.navigate(NavRoutes.Chat.createRoute(contactId))
            //     }
            // )
        }
        
        // ============================================================
        // 联系人详情页
        // ============================================================
        composable(
            route = NavRoutes.ContactDetail.route,
            arguments = listOf(
                navArgument(NavRoutes.ContactDetail.ARG_CONTACT_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString(
                NavRoutes.ContactDetail.ARG_CONTACT_ID
            ) ?: ""
            
            // TODO: Phase3 - 实现ContactDetailScreen
            // ContactDetailScreen(
            //     contactId = contactId,
            //     onNavigateBack = { navController.popBackStack() }
            // )
        }
        
        // ============================================================
        // 聊天分析页
        // ============================================================
        composable(
            route = NavRoutes.Chat.route,
            arguments = listOf(
                navArgument(NavRoutes.Chat.ARG_CONTACT_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString(
                NavRoutes.Chat.ARG_CONTACT_ID
            ) ?: ""
            
            // TODO: Phase3 - 实现ChatScreen
            // ChatScreen(
            //     contactId = contactId,
            //     onNavigateBack = { navController.popBackStack() }
            // )
        }
        
        // ============================================================
        // 设置页 (预留)
        // ============================================================
        composable(route = NavRoutes.Settings.route) {
            // TODO: 实现SettingsScreen
        }
        
        // ============================================================
        // 关于页 (预留)
        // ============================================================
        composable(route = NavRoutes.About.route) {
            // TODO: 实现AboutScreen
        }
    }
}
```

### 2.4 导航最佳实践

#### 参数传递原则
```kotlin
// ✅ 正确: 只传递简单数据(ID)
navController.navigate(NavRoutes.Chat.createRoute(contactId = 