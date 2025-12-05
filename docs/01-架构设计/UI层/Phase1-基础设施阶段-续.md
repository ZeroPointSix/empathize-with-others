# Phase1: 基础设施阶段 (续)

> 本文件是Phase1-基础设施阶段.md的续篇

## 2.4 导航最佳实践 (续)

#### 参数传递原则
```kotlin
// ✅ 正确: 只传递简单数据(ID)
navController.navigate(NavRoutes.Chat.createRoute(contactId = "contact_123"))

// ❌ 错误: 尝试传递复杂对象
// 不要尝试序列化整个ContactProfile对象
```

#### 返回栈管理
```kotlin
// 单例模式: 避免重复压栈
navController.navigate(route) {
    launchSingleTop = true
}

// 清空返回栈: 用于登录后跳转主页
navController.navigate(NavRoutes.ContactList.route) {
    popUpTo(navController.graph.startDestinationId) {
        inclusive = true
    }
}
```

### 2.5 验证清单

- [ ] NavRoutes.kt定义了所有核心路由
- [ ] NavGraph.kt正确配置了所有composable
- [ ] 路由参数使用常量而非硬编码
- [ ] 提供了createRoute()辅助函数

---

## 三、MainActivity集成

### 3.1 任务目标

将Compose与MainActivity集成,设置应用入口。

### 3.2 MainActivity.kt实现

**文件路径**: `app/src/main/java/com/empathy/ai/presentation/ui/MainActivity.kt`

```kotlin
package com.empathy.ai.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.navigation.EmpathyNavGraph
import dagger.hilt.android.AndroidEntryPoint

/**
 * 应用主Activity
 * 
 * 职责:
 * 1. 设置Compose内容
 * 2. 应用主题
 * 3. 初始化导航
 * 4. 依赖注入入口
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            // 应用主题
            EmpathyTheme {
                // Surface容器,提供背景色
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 创建导航控制器
                    val navController = rememberNavController()
                    
                    // 导航图
                    EmpathyNavGraph(
                        navController = navController
                    )
                }
            }
        }
    }
}
```

### 3.3 验证清单

- [ ] MainActivity使用@AndroidEntryPoint注解
- [ ] setContent中正确应用EmpathyTheme
- [ ] NavController正确传递给NavGraph
- [ ] 应用可以成功启动(即使页面为空)

---

## 四、依赖注入配置

### 4.1 Application类验证

**文件路径**: `app/src/main/java/com/empathy/ai/app/EmpathyApplication.kt`

确认Application类已配置Hilt:

```kotlin
package com.empathy.ai.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class EmpathyApplication : Application()
```

### 4.2 build.gradle验证

确保包含Hilt依赖:

```kotlin
// app/build.gradle.kts
dependencies {
    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
}
```

### 4.3 验证清单

- [ ] EmpathyApplication使用@HiltAndroidApp
- [ ] MainActivity使用@AndroidEntryPoint
- [ ] 所有ViewModel使用@HiltViewModel
- [ ] 项目可以成功编译

---

## 五、目录结构规范

### 5.1 最终目录结构

```
presentation/
├── theme/
│   ├── Color.kt          ✅ 新建
│   ├── Theme.kt          ✅ 更新
│   ├── Type.kt           ✅ 已存在
│   └── Shape.kt          🔲 可选
│
├── ui/
│   ├── MainActivity.kt   ✅ 更新
│   ├── navigation/       ✅ 新建目录
│   │   ├── NavRoutes.kt  ✅ 新建
│   │   └── NavGraph.kt   ✅ 新建
│   ├── screen/           🔲 Phase3
│   └── component/        🔲 Phase2
│
├── viewmodel/            ✅ 已存在
│   ├── ChatViewModel.kt
│   ├── ContactListViewModel.kt
│   └── ContactDetailViewModel.kt
│
└── app/
    └── EmpathyApplication.kt  ✅ 验证
```

---

## 六、测试验证

### 6.1 编译测试

```bash
./gradlew clean
./gradlew build
./gradlew installDebug
```

### 6.2 功能测试清单

#### 主题测试
- [ ] 浅色模式正常显示
- [ ] 深色模式正常显示
- [ ] 系统切换时自动跟随
- [ ] 颜色对比度清晰

#### 导航测试  
- [ ] 应用成功启动
- [ ] MainActivity正确设置Compose
- [ ] NavGraph正确初始化
- [ ] 无编译错误

#### 依赖注入测试
- [ ] Hilt代码生成成功
- [ ] Application正确初始化
- [ ] MainActivity依赖注入正常

---

## 七、常见问题

### 7.1 颜色对比度不足

**解决方案**:
```kotlin
// 使用对比度检查工具
// https://webaim.org/resources/contrastchecker/
val OnSurfaceDark = Color(0xFFE6E1E5)  // 确保4.5:1对比度
```

### 7.2 Hilt编译错误

**解决方案**:
```bash
./gradlew clean
./gradlew build

# 确保AndroidManifest.xml中声明Application
<application android:name=".app.EmpathyApplication" ...>
```

---

## 八、验收标准

### 8.1 必须完成(P0)

- [ ] Color.kt包含完整配色方案
- [ ] Theme.kt正确应用配色
- [ ] NavRoutes.kt定义所有核心路由
- [ ] NavGraph.kt创建导航框架
- [ ] MainActivity集成Compose和导航
- [ ] Hilt依赖注入配置正确

### 8.2 应该完成(P1)

- [ ] 添加颜色使用示例
- [ ] 导航添加过渡动画
- [ ] 添加Shape.kt形状系统

---

## 九、下一步

完成Phase1后,进入**Phase2: 可复用组件阶段**

**Phase2核心任务**:
1. LoadingIndicator - 加载指示器
2. ErrorDialog - 错误对话框
3. ContactCard - 联系人卡片
4. BrainTagChip - 标签芯片
5. MessageBubble - 消息气泡

---

## 十、参考资料

### 官方文档
- [Material Design 3](https://m3.material.io/)
- [Jetpack Compose导航](https://developer.android.com/jetpack/compose/navigation)
- [Hilt依赖注入](https://developer.android.com/training/dependency-injection/hilt-android)

### 项目文档
- [`UI层全局设计.md`](UI层全局设计.md)
- [`UI层开发规范.md`](UI层开发规范.md)
- [`UI层开发路线图.md`](../../02-开发指南/UI层开发路线图.md)

---

**文档版本**: v1.0  
**创建日期**: 2025-12-05  
**维护者**: 架构团队