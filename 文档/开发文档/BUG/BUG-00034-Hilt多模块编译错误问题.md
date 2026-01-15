# BUG-00034-Hilt多模块编译错误问题

## 文档信息

| 项目 | 内容 |
|------|------|
| 文档编号 | BUG-00034 |
| 创建日期 | 2025-12-25 |
| 修复人 | Kiro |
| 状态 | ✅ 已修复 |
| 严重程度 | P0 - 阻塞编译 |
| 影响范围 | 整个项目无法编译 |

---

## 1. 问题描述

### 1.1 现象
执行`./gradlew :app:assembleDebug`时，编译失败，报错：
```
找不到符号 MainActivity
位置: 接口 MainActivity_GeneratedInjector
```

### 1.2 错误位置
- 任务：`:presentation:compileDebugJavaWithJavac`
- 文件：`presentation/build/generated/source/kapt/debug/.../MainActivity_GeneratedInjector.java`

### 1.3 影响
- 无法编译Debug APK
- 无法编译Release APK
- 阻塞所有开发工作

---

## 2. 根因分析

### 2.1 直接原因
Hilt KAPT在presentation模块（Android Library）中生成的Java代码，在编译时无法找到Kotlin编写的`MainActivity`类。

### 2.2 根本原因
**MainActivity放在了错误的模块**：
- `MainActivity`使用了`@AndroidEntryPoint`注解
- Hilt的`@AndroidEntryPoint`需要AGP的字节码转换支持
- 字节码转换**只在Application模块**（`com.android.application`插件）中生效
- presentation模块使用的是`com.android.library`插件，不支持字节码转换

### 2.3 架构问题
在TD-00017 Clean Architecture多模块改造时，MainActivity被放在了presentation模块，违反了Hilt的多模块最佳实践。

---

## 3. 修复方案

### 3.1 方案选择
将MainActivity从presentation模块移动到app模块。

### 3.2 修复步骤

| 步骤 | 操作 | 文件 |
|------|------|------|
| 1 | 创建新的MainActivity | `app/src/main/java/com/empathy/ai/ui/MainActivity.kt` |
| 2 | 更新AndroidManifest | 修改activity声明为`.ui.MainActivity` |
| 3 | 删除旧文件 | 删除`presentation/.../MainActivity.kt` |
| 4 | 验证编译 | 执行`./gradlew clean :app:assembleDebug` |

### 3.3 代码变更

**新文件位置**：
```
app/src/main/java/com/empathy/ai/ui/MainActivity.kt
```

**包名变更**：
```kotlin
// 旧: package com.empathy.ai.presentation.ui
// 新: package com.empathy.ai.ui
```

**AndroidManifest变更**：
```xml
<!-- 旧 -->
<activity android:name=".presentation.ui.MainActivity" ... />

<!-- 新 -->
<activity android:name=".ui.MainActivity" ... />
```

---

## 4. 验证结果

### 4.1 编译验证
```
BUILD SUCCESSFUL in 9m 25s
97 actionable tasks: 59 executed, 38 from cache
```

### 4.2 APK生成
- 文件：`app/build/outputs/apk/debug/app-debug.apk`
- 状态：✅ 成功生成

---

## 5. 经验总结

### 5.1 Hilt多模块最佳实践
1. **Activity、Service等Android组件**必须放在Application模块（app模块）
2. **ViewModel、Repository等**可以放在Library模块
3. `@AndroidEntryPoint`注解的类必须在Application模块

### 5.2 Clean Architecture模块划分
| 组件类型 | 推荐模块 | 原因 |
|----------|----------|------|
| Activity | app | 需要AGP字节码转换 |
| Service | app | 需要AGP字节码转换 |
| ViewModel | presentation | 不需要字节码转换 |
| UseCase | domain | 纯Kotlin |
| Repository | data | 数据层实现 |

### 5.3 预防措施
- 在进行模块化改造时，注意Hilt的限制
- Activity和Service必须保留在app模块
- 定期执行完整编译验证架构变更

---

## 6. 关联文档

| 文档类型 | 文档编号 | 文档名称 |
|----------|----------|----------|
| RE | RESEARCH-00047 | Hilt多模块编译错误深度调研报告 |
| TD | TD-00017 | Clean Architecture多模块改造任务清单 |

---

**文档版本**: 1.0  
**最后更新**: 2025-12-25
