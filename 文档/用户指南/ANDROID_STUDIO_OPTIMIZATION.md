# Android Studio 编译优化指南

## 🚀 快速开始

### 1. 选择开发变体
在 Android Studio 中：
1. 点击右下角的 **Build Variants** 标签
2. 将 `app` 模块的 Build Variant 改为 `dev`
3. 现在 Shift+F10 将使用优化后的 `dev` 变体

### 2. 使用专用运行配置
1. 在 AS 的运行配置下拉菜单中
2. 选择 "App (Dev Mode)"
3. 点击绿色运行按钮或使用快捷键

## ⚙️ AS 设置优化

### Gradle 设置路径
`File` → `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Gradle`

**推荐配置**：
- ✅ **Build and run using**: Gradle
- ✅ **Run tests using**: Gradle
- ✅ **Gradle JVM**: Use project JDK (17)
- ✅ **Offline mode**: 根据网络情况决定

### Compiler 设置路径
`File` → `Settings` → `Build, Execution, Deployment` → `Compiler`

**推荐配置**：
- ✅ **Build project automatically**: 取消勾选（手动控制编译）
- ✅ **Compile independent modules in parallel**: 勾选

### Memory 设置
如果需要优化 AS 本身的内存：
`Help` → `Edit Custom VM Options**

```
-Xmx8g
-Xms2g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100
-XX:+UseStringDeduplication
```

## 🔧 不同编译模式的区别

| 模式 | 构建命令 | 特点 | 适用场景 |
|------|----------|------|----------|
| **Release** | `assembleRelease` | 完整优化、混淆、资源压缩 | 正式发布 |
| **Debug** | `assembleDebug` | 基础调试信息、无优化 | 日常测试 |
| **Dev** | `assembleDev` | 最大化编译速度 | 日常开发 |

## 📊 性能对比（预期）

| 编译类型 | Debug | Dev | 提升幅度 |
|----------|-------|-----|----------|
| **首次构建** | 2-3分钟 | 1-1.5分钟 | 50% |
| **增量构建** | 30-45秒 | 15-20秒 | 60% |
| **资源处理** | 包含PNG压缩 | 跳过PNG压缩 | 30% |
| **符号信息** | 完整调试符号 | 最小符号集 | 20% |

## 🛠️ 实用技巧

### 1. 增量编译优化
- 使用 `dev` 变体进行日常开发
- 保持代码结构稳定，避免大量重构
- 优先修改单个文件而非批量修改

### 2. 缓存管理
```bash
# 清理构建缓存（如果遇到编译问题）
./gradlew cleanBuildCache

# 重新同步项目
File → Sync Project with Gradle Files
```

### 3. 构建问题排查
如果 `dev` 变体遇到问题：
1. 切换回 `debug` 变体验证代码
2. 检查 Build Variants 配置
3. 清理并重建项目

### 4. 内存监控
在 AS 底部状态栏可以看到内存使用情况：
- 监控 Gradle Daemon 内存
- 如果内存不足，适当调低 `gradle.properties` 中的内存设置

## 🎯 开发工作流建议

### 日常开发（推荐）
1. 使用 `dev` 变体进行功能开发
2. 使用 `debug` 变体进行功能测试
3. 使用 `release` 变体进行发布构建

### 性能测试
```bash
# 测试不同变体的编译时间
./gradlew assembleDev --profile
./gradlew assembleDebug --profile
```

## ⚠️ 注意事项

1. **`dev` 变体仅用于开发**，不能用于正式发布
2. **应用ID不同**：`dev` 变体会添加 `.dev` 后缀
3. **调试信息有限**：某些调试功能可能受限
4. **定期清理缓存**：避免缓存过大影响性能

---

**最后更新**: 2025-12-22
**适用版本**: Android Studio Giraffe | Gradle 8.13 | Kotlin 2.0.21