---
description: 快速构建 - 智能编译并报告结果
---

# 快速构建命令

执行快速构建并提供智能错误分析。

## 使用方式

```
/QuickBuild              # 快速 Debug 构建
/QuickBuild --release    # Release 构建
/QuickBuild --clean      # 清理后构建
/QuickBuild --install    # 构建并安装到设备
```

## 执行流程

### 1. 预检查

在构建前执行快速检查：
- 检查 Gradle Daemon 状态
- 检查是否有语法错误（使用 getDiagnostics）
- 检查依赖是否有更新

### 2. 执行构建

根据参数选择构建命令：

**默认 (Debug):**
```cmd
scripts\quick-build.bat
```

**Release:**
```cmd
gradlew.bat assembleRelease --build-cache -x lint --parallel
```

**Clean Build:**
```cmd
gradlew.bat clean assembleDebug --build-cache --parallel
```

### 3. 结果分析

**成功时：**
```
✅ 构建成功！

📦 APK 信息:
   - 路径: app\build\outputs\apk\debug\app-debug.apk
   - 大小: 12.5 MB
   - 版本: 1.0.0 (build 42)

⏱️ 构建耗时: 45.2s

💡 下一步:
   - [安装到设备] adb install -r app-debug.apk
   - [运行测试] scripts\quick-test.bat
```

**失败时：**
```
❌ 构建失败

🔍 错误分析:
   文件: ContactDetailViewModel.kt:156
   错误: Unresolved reference: editFact
   
💡 可能原因:
   1. EditFactUseCase 未注入到 ViewModel
   2. 方法名拼写错误
   
🔧 建议修复:
   在 ContactDetailViewModel 构造函数中添加:
   private val editFactUseCase: EditFactUseCase

[查看完整日志] [重新构建] [清理后重试]
```

## 构建优化提示

如果构建时间过长，会提供优化建议：
```
⚠️ 构建耗时较长 (2m 30s)

优化建议:
1. 运行 `gradlew --stop` 重启 Daemon
2. 增加 Gradle 内存: gradle.properties 中设置 org.gradle.jvmargs=-Xmx4g
3. 启用配置缓存: org.gradle.configuration-cache=true
```
