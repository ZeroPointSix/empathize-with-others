# BUG-00035 多模块Hilt运行时类找不到问题

## 问题概述

| 属性 | 值 |
|------|-----|
| **BUG编号** | BUG-00035 |
| **发现日期** | 2025-12-25 |
| **严重程度** | 🔴 严重（应用启动崩溃） |
| **状态** | ✅ 已解决 |
| **影响范围** | 应用启动、所有功能 |
| **相关任务** | TD-00017 Clean Architecture多模块改造 |

---

## 问题描述

### 现象
应用启动时立即崩溃，抛出 `NoClassDefFoundError` 异常，提示找不到 `ContactRepositoryImpl` 类。

### 错误日志
```
FATAL EXCEPTION: main
Process: com.empathy.ai, PID: 9001
java.lang.NoClassDefFoundError: Failed resolution of: Lcom/empathy/ai/data/repository/ContactRepositoryImpl;
    at com.empathy.ai.app.DaggerEmpathyApplication_HiltComponents_SingletonC$SingletonCImpl.contactRepositoryImpl(...)
    at com.empathy.ai.app.DaggerEmpathyApplication_HiltComponents_SingletonC$ViewModelCImpl.getAllContactsUseCase(...)
    ...
Caused by: java.lang.ClassNotFoundException: Didn't find class "com.empathy.ai.data.repository.ContactRepositoryImpl" 
on path: DexPathList[[zip file "/data/app/.../base.apk"],nativeLibraryDirectories=[...]]
```

### 触发条件
- 多模块架构（`:domain`、`:data`、`:presentation`、`:app`）
- 使用Gradle构建缓存
- 增量构建后安装APK

---

## 根本原因分析

### 1. 构建缓存污染
Gradle的增量构建和构建缓存机制在多模块项目中可能导致：
- 模块间依赖关系未正确更新
- 旧的DEX文件被复用
- Hilt生成的代码与实际类不匹配

### 2. 多模块Hilt依赖传递
在Clean Architecture多模块架构中：
```
:app → :presentation → :domain
:app → :data → :domain
```

Hilt需要在编译时扫描所有模块的 `@Module` 和 `@Inject` 注解，构建缓存可能导致：
- `:data` 模块的类未被正确打包到APK
- Hilt生成的 `Dagger*` 类引用了不存在的实现类

### 3. DEX合并问题
多模块项目的DEX合并过程中，如果缓存不一致，可能导致：
- 某些模块的类被遗漏
- 类引用指向错误的DEX文件

---

## 解决方案

### 立即修复（推荐）
使用以下命令强制完全重新构建：

```bash
# 停止所有Gradle进程
./gradlew --stop

# 终止所有Java进程（Windows）
taskkill /F /IM java.exe

# 等待进程完全终止
Start-Sleep -Seconds 3

# 完全重新构建，禁用缓存
./gradlew clean assembleDebug --rerun-tasks --no-build-cache
```

### 关键参数说明
| 参数 | 作用 |
|------|------|
| `--stop` | 停止Gradle Daemon，释放文件锁 |
| `clean` | 清理所有构建产物 |
| `--rerun-tasks` | 强制重新执行所有任务，忽略UP-TO-DATE检查 |
| `--no-build-cache` | 禁用构建缓存，确保从源码编译 |

### 快捷脚本
可以创建 `scripts/full-rebuild.bat`：
```batch
@echo off
echo 正在停止Gradle进程...
call gradlew --stop
taskkill /F /IM java.exe 2>nul
timeout /t 3 /nobreak >nul
echo 正在完全重新构建...
call gradlew clean assembleDebug --rerun-tasks --no-build-cache
echo 构建完成！
pause
```

---

## 预防措施

### 1. 开发时建议
- 修改模块依赖后，执行完全重新构建
- 遇到奇怪的运行时错误，首先尝试清理缓存
- 定期执行 `./gradlew --stop` 释放Daemon

### 2. CI/CD配置
在CI环境中始终使用：
```yaml
- name: Build APK
  run: ./gradlew clean assembleRelease --no-build-cache
```

### 3. 多模块Hilt最佳实践
- 确保所有模块正确配置 `kapt` 或 `ksp` 处理Hilt
- 使用 `api` 而非 `implementation` 暴露需要跨模块访问的依赖
- 在 `:data` 模块的 `build.gradle.kts` 中：
  ```kotlin
  dependencies {
      // 使用api暴露domain模块
      api(project(":domain"))
      
      // Hilt配置
      implementation(libs.hilt.android)
      kapt(libs.hilt.compiler)
  }
  ```

---

## 相关文件

| 文件 | 说明 |
|------|------|
| `data/src/main/kotlin/.../di/RepositoryModule.kt` | Repository绑定配置 |
| `data/src/main/kotlin/.../repository/ContactRepositoryImpl.kt` | 问题类 |
| `app/build.gradle.kts` | 应用模块依赖配置 |
| `data/build.gradle.kts` | 数据模块构建配置 |

---

## 验证步骤

1. 执行完全重新构建命令
2. 安装APK到设备：`adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. 启动应用：`adb shell am start -n com.empathy.ai/.ui.MainActivity`
4. 确认应用正常启动，无崩溃

---

## 总结

这是多模块Hilt项目中常见的构建缓存问题。当遇到 `NoClassDefFoundError` 或 `ClassNotFoundException` 指向 Repository/UseCase 等DI管理的类时，首先尝试完全重新构建。

**关键命令**：
```bash
./gradlew clean assembleDebug --rerun-tasks --no-build-cache
```

---

**文档版本**: 1.0  
**最后更新**: 2025-12-25  
**作者**: Kiro AI Assistant
