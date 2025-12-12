# Task 7: 状态持久化实现总结

## 概述

本文档总结了任务 7（状态持久化）的实现细节和验证结果。

## 实现内容

### 1. FloatingWindowPreferences 类

**位置**: `app/src/main/java/com/empathy/ai/data/local/FloatingWindowPreferences.kt`

**职责**:
- 使用 SharedPreferences 持久化悬浮窗状态
- 提供线程安全的读写操作
- 支持单独保存和加载各个状态属性

**核心方法**:
```kotlin
// 保存完整状态
fun saveState(state: FloatingWindowState)

// 加载完整状态
fun loadState(): FloatingWindowState

// 保存启用状态
fun saveEnabled(isEnabled: Boolean)

// 获取启用状态
fun isEnabled(): Boolean

// 保存按钮位置
fun saveButtonPosition(x: Int, y: Int)

// 获取按钮坐标
fun getButtonX(): Int
fun getButtonY(): Int

// 清除所有状态
fun clear()
```

**特点**:
- 使用 `@Singleton` 注解确保单例
- 使用 Hilt 依赖注入
- 使用 `SharedPreferences.edit { }` 扩展函数简化代码
- 提供默认值处理

### 2. FloatingWindowService 集成

**更新内容**:
1. 注入 `FloatingWindowPreferences` 依赖
2. 在 `showFloatingView()` 中恢复上次保存的按钮位置
3. 监听位置变化并自动保存

**关键代码**:
```kotlin
@Inject
lateinit var floatingWindowPreferences: FloatingWindowPreferences

private fun showFloatingView() {
    // ... 创建 FloatingView
    
    // 恢复上次保存的位置
    val savedX = floatingWindowPreferences.getButtonX()
    val savedY = floatingWindowPreferences.getButtonY()
    params.x = savedX
    params.y = savedY
    
    // 监听位置变化
    floatingView?.onPositionChanged = { x, y ->
        floatingWindowPreferences.saveButtonPosition(x, y)
    }
}
```

### 3. FloatingView 位置变化回调

**更新内容**:
1. 添加 `onPositionChanged` 回调函数
2. 在 `snapToEdge()` 方法中触发回调

**关键代码**:
```kotlin
var onPositionChanged: ((Int, Int) -> Unit)? = null

private fun snapToEdge() {
    // ... 吸附逻辑
    
    // 通知位置变化
    onPositionChanged?.invoke(params.x, params.y)
}
```

### 4. SettingsViewModel 集成

**更新内容**:
1. 注入 `FloatingWindowPreferences` 依赖
2. 在 `init` 中加载悬浮窗状态
3. 在启动/停止服务时保存状态

**关键代码**:
```kotlin
@Inject
lateinit var floatingWindowPreferences: FloatingWindowPreferences

init {
    loadFloatingWindowState()
}

private fun loadFloatingWindowState() {
    val state = floatingWindowPreferences.loadState()
    _uiState.update {
        it.copy(floatingWindowEnabled = state.isEnabled)
    }
}

private fun startFloatingWindowService() {
    FloatingWindowManager.startService(getApplication())
    floatingWindowPreferences.saveEnabled(true)
    // ...
}

private fun stopFloatingWindowService() {
    FloatingWindowManager.stopService(getApplication())
    floatingWindowPreferences.saveEnabled(false)
    // ...
}
```

### 5. EmpathyApplication 自动启动

**更新内容**:
1. 注入 `FloatingWindowPreferences` 依赖
2. 在 `onCreate()` 中根据保存的状态决定是否自动启动服务

**关键代码**:
```kotlin
@Inject
lateinit var floatingWindowPreferences: FloatingWindowPreferences

override fun onCreate() {
    super.onCreate()
    initializeFloatingWindowService()
}

private fun initializeFloatingWindowService() {
    try {
        val state = floatingWindowPreferences.loadState()
        
        // 如果上次启用了悬浮窗，且有权限，则自动启动服务
        if (state.isEnabled && FloatingWindowManager.hasPermission(this)) {
            FloatingWindowManager.startService(this)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
```

## 测试覆盖

### 1. FloatingWindowPreferencesTest

**测试用例**:
- ✅ `saveState should save all state properties`
- ✅ `loadState should return saved state`
- ✅ `loadState should return default state when no data saved`
- ✅ `saveEnabled should save enabled state`
- ✅ `isEnabled should return saved enabled state`
- ✅ `saveButtonPosition should save position`
- ✅ `getButtonX should return saved X coordinate`
- ✅ `getButtonY should return saved Y coordinate`
- ✅ `clear should clear all preferences`

### 2. FloatingWindowServicePersistenceTest

**测试用例**:
- ✅ `should save enabled state when service starts`
- ✅ `should save disabled state when service stops`
- ✅ `should save button position when dragged`
- ✅ `should load saved state on startup`
- ✅ `should load default state on first launch`
- ✅ `should restore button position when service starts`
- ✅ `should maintain state consistency across save and load`
- ✅ `should clear all saved state`

## 验证需求

### 需求 4.5: 状态持久化

**验收标准**:
- ✅ 创建 FloatingWindowPreferences 类
- ✅ 实现状态保存方法
- ✅ 实现状态加载方法
- ✅ 在服务启动时恢复上次的按钮位置
- ✅ 在应用重启时根据状态决定是否启动服务

## 数据存储

### SharedPreferences 键值

| 键名 | 类型 | 说明 | 默认值 |
|------|------|------|--------|
| `is_enabled` | Boolean | 悬浮窗是否启用 | false |
| `button_x` | Int | 悬浮按钮 X 坐标 | 0 |
| `button_y` | Int | 悬浮按钮 Y 坐标 | 0 |

### 文件位置

SharedPreferences 文件名: `floating_window_prefs`

存储路径: `/data/data/com.empathy.ai/shared_prefs/floating_window_prefs.xml`

## 使用流程

### 1. 首次启动

```
用户启动应用
  ↓
EmpathyApplication.onCreate()
  ↓
加载状态: isEnabled = false
  ↓
不启动服务
```

### 2. 用户启用悬浮窗

```
用户在设置页面启用悬浮窗
  ↓
SettingsViewModel.startFloatingWindowService()
  ↓
保存状态: isEnabled = true
  ↓
启动 FloatingWindowService
  ↓
恢复按钮位置: (x, y) = (0, 0)
```

### 3. 用户拖动悬浮按钮

```
用户拖动悬浮按钮
  ↓
FloatingView.snapToEdge()
  ↓
触发 onPositionChanged 回调
  ↓
保存位置: (x, y) = (新位置)
```

### 4. 应用重启

```
用户重启应用
  ↓
EmpathyApplication.onCreate()
  ↓
加载状态: isEnabled = true, (x, y) = (保存的位置)
  ↓
检查权限: 有权限
  ↓
自动启动 FloatingWindowService
  ↓
恢复按钮位置: (x, y) = (保存的位置)
```

### 5. 用户禁用悬浮窗

```
用户在设置页面禁用悬浮窗
  ↓
SettingsViewModel.stopFloatingWindowService()
  ↓
保存状态: isEnabled = false
  ↓
停止 FloatingWindowService
```

## 注意事项

### 1. 权限检查

应用重启时自动启动服务需要满足两个条件：
1. 上次启用了悬浮窗（`isEnabled = true`）
2. 有悬浮窗权限（`FloatingWindowManager.hasPermission() = true`）

如果没有权限，即使 `isEnabled = true`，也不会自动启动服务。

### 2. 异常处理

`EmpathyApplication.initializeFloatingWindowService()` 使用 try-catch 包裹，确保启动失败不会影响应用正常运行。

### 3. 线程安全

`SharedPreferences.edit { }` 扩展函数使用 `apply()` 方法，异步写入数据，不会阻塞主线程。

### 4. 数据一致性

所有状态更新都通过 `FloatingWindowPreferences` 统一管理，确保数据一致性。

## 后续优化

### 1. 数据迁移

如果未来需要更改数据结构，可以添加版本号进行数据迁移：

```kotlin
companion object {
    private const val KEY_VERSION = "version"
    private const val CURRENT_VERSION = 1
}

private fun migrateIfNeeded() {
    val version = prefs.getInt(KEY_VERSION, 0)
    if (version < CURRENT_VERSION) {
        // 执行数据迁移
        prefs.edit {
            putInt(KEY_VERSION, CURRENT_VERSION)
        }
    }
}
```

### 2. 加密存储

如果需要存储敏感信息，可以使用 `EncryptedSharedPreferences`：

```kotlin
private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
    context,
    PREFS_NAME,
    MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build(),
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

### 3. 数据备份

可以添加导出/导入功能，允许用户备份和恢复设置：

```kotlin
fun exportState(): String {
    val state = loadState()
    return Json.encodeToString(state)
}

fun importState(json: String) {
    val state = Json.decodeFromString<FloatingWindowState>(json)
    saveState(state)
}
```

## 总结

任务 7（状态持久化）已完成所有子任务：

1. ✅ 创建 FloatingWindowPreferences 类
2. ✅ 实现状态保存方法
3. ✅ 实现状态加载方法
4. ✅ 在服务启动时恢复上次的按钮位置
5. ✅ 在应用重启时根据状态决定是否启动服务

所有功能已通过单元测试验证，符合需求 4.5 的验收标准。

---

**完成日期**: 2025-12-07  
**开发者**: Kiro AI Assistant  
**文档版本**: v1.0
