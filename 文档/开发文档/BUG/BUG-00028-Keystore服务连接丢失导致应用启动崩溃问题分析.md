# BUG-00028: Keystore服务连接丢失导致应用启动崩溃问题分析

> 创建日期: 2025-12-21
> 修复日期: 2025-12-21
> 状态: ✅ 已修复
> 严重程度: Critical (应用无法启动)

---

## 1. 问题描述

### 1.1 错误现象

应用在启动时崩溃，无法进入主界面。

### 1.2 关键日志

```
W  Looks like we may have lost connection to the Keystore daemon.
W  Retrying after giving Keystore 50ms to recover.

E  FATAL EXCEPTION: main
   java.lang.RuntimeException: Unable to create application com.empathy.ai.app.EmpathyApplication: 
   java.lang.NullPointerException: Attempt to invoke interface method 
   'android.os.IBinder android.system.keystore2.IKeystoreService.asBinder()' on a null object reference
```

### 1.3 崩溃堆栈关键路径

```
ActivityThread.handleBindApplication
  → EmpathyApplication.onCreate
    → Hilt_EmpathyApplication.hiltInternalInject
      → DaggerEmpathyApplication_HiltComponents_SingletonC.injectEmpathyApplication
        → aiProviderRepositoryImpl
          → ApiKeyStorage.<init>
            → MasterKey.Builder.build
              → MasterKeys.getOrCreate
                → KeyStore.containsAlias
                  → AndroidKeyStoreSpi.engineContainsAlias
                    → KeyStore2.getKeyEntry
                      → KeyStore2.getService  ← NPE发生点
```

---

## 2. 机制分析

### 2.1 Android Keystore 运行机制

```
┌─────────────────────────────────────────────────────────────────┐
│                    Android Keystore 架构                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐    Binder IPC    ┌──────────────────────┐     │
│  │   App进程     │ ◄──────────────► │  Keystore Daemon     │     │
│  │              │                   │  (系统服务进程)        │     │
│  │ KeyStore2    │                   │                      │     │
│  │ .getService()│                   │  IKeystoreService    │     │
│  └──────────────┘                   └──────────────────────┘     │
│         │                                    │                   │
│         ▼                                    ▼                   │
│  ┌──────────────┐                   ┌──────────────────────┐     │
│  │ MasterKey    │                   │  Hardware Keymaster  │     │
│  │ .Builder     │                   │  (TEE/StrongBox)     │     │
│  │ .build()     │                   └──────────────────────┘     │
│  └──────────────┘                                                │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 正常启动流程

```
1. ActivityThread.handleBindApplication()
   ↓
2. Application.attachBaseContext()
   ↓
3. Hilt 组件初始化 (DaggerXXX_HiltComponents_SingletonC)
   ↓
4. @Singleton 依赖创建 (按依赖图顺序)
   ↓
5. ApiKeyStorage 构造函数执行
   ↓
6. MasterKey.Builder.build() 调用
   ↓
7. Keystore2.getService() 获取 IKeystoreService Binder
   ↓
8. 检查/创建 MasterKey
   ↓
9. EncryptedSharedPreferences 初始化
   ↓
10. Application.onCreate() 执行
```

### 2.3 问题发生点

在步骤 7，`KeyStore2.getService()` 返回 `null`，导致后续调用 `asBinder()` 时抛出 NPE。

---

## 3. 潜在根因树 (Root Cause Tree)

### 3.1 框架机制层

| 根因 | 可能性 | 说明 |
|------|--------|------|
| Keystore Daemon 未启动 | 中 | 系统启动过程中 Keystore 服务可能尚未就绪 |
| Keystore Daemon 崩溃 | 中 | 日志显示 "lost connection"，表明服务可能崩溃 |
| Binder 连接超时 | 低 | 系统只重试了 50ms，可能不够 |
| SELinux 策略阻止 | 低 | 安全策略可能阻止 Binder 通信 |

### 3.2 模块行为层

| 根因 | 可能性 | 说明 |
|------|--------|------|
| Hilt 急切初始化 | **高** | `@Singleton` + `@Inject` 导致在 Application 创建时立即初始化 |
| 初始化时机过早 | **高** | 在 `attachBaseContext` 阶段就触发 Keystore 访问 |
| 无重试机制 | **高** | `MasterKey.Builder.build()` 内部只有 50ms 重试 |
| 无降级策略 | **高** | 失败直接抛异常，无 fallback |

### 3.3 使用方式层

| 根因 | 可能性 | 说明 |
|------|--------|------|
| 同步阻塞调用 | **高** | 在主线程同步调用 Keystore 操作 |
| 依赖链过深 | 中 | `EmpathyApplication` → `AiProviderRepository` → `ApiKeyStorage` |
| 缺少延迟初始化 | **高** | 没有使用真正的延迟初始化 |

### 3.4 环境层

| 根因 | 可能性 | 说明 |
|------|--------|------|
| 设备重启后首次启动 | **高** | Keystore 服务可能尚未完全初始化 |
| 低端设备/模拟器 | 中 | 资源竞争导致服务启动慢 |
| 系统更新后 | 低 | Keystore 数据可能损坏 |

---

## 4. 最可能的根因（基于机制推理）

### 4.1 根因 #1：Hilt 急切初始化 + Keystore 服务未就绪

**推理过程：**

1. `EmpathyApplication` 使用 `@Inject` 注入多个依赖
2. Hilt 在 `attachBaseContext()` 后立即创建依赖图
3. `ApiKeyStorage` 是 `@Singleton`，被急切创建
4. 此时 Keystore Daemon 可能尚未完全启动
5. `MasterKey.Builder.build()` 只重试 50ms 后失败
6. 抛出 NPE，应用崩溃

### 4.2 根因 #2：多个类使用 `by lazy` 但可能被 Hilt 触发

项目中有三个类使用 `EncryptedSharedPreferences`：
- `ApiKeyStorage` - 已修复为自定义 getter
- `UserProfilePreferences` - 使用 `by lazy`（有风险）
- `SettingsRepositoryImpl` - 使用 `by lazy`（有风险）

Kotlin 的 `by lazy` 在某些情况下可能被 Hilt 的依赖图验证触发。

---

## 5. 稳定修复方案

### 5.1 修复原则

1. **完全延迟初始化**：构造函数不访问 Keystore
2. **重试机制**：3次重试，递增延迟（200ms, 400ms, 600ms）
3. **降级策略**：加密存储不可用时降级到普通 SharedPreferences
4. **线程安全**：使用 synchronized 确保并发安全
5. **统一模式**：所有使用 `EncryptedSharedPreferences` 的类采用相同模式

### 5.2 修复的类

| 文件 | 修复前 | 修复后 |
|------|--------|--------|
| `ApiKeyStorage.kt` | 自定义 getter | ✅ 已修复 |
| `UserProfilePreferences.kt` | `by lazy` | ✅ 已修复 |
| `SettingsRepositoryImpl.kt` | `by lazy` | ✅ 已修复 |
| `EmpathyApplication.kt` | `Provider<T>` | ✅ 已修复 |

### 5.3 核心修复代码模式

```kotlin
@Singleton
class XxxStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY_MS = 200L
    }

    @Volatile
    private var isEncryptionAvailable = true
    
    private val initLock = Any()
    private var _prefs: SharedPreferences? = null

    // 自定义 getter 替代 by lazy
    private val prefs: SharedPreferences
        get() {
            if (_prefs != null) return _prefs!!
            synchronized(initLock) {
                if (_prefs == null) {
                    _prefs = initializePrefs()
                }
                return _prefs!!
            }
        }
    
    private fun initializePrefs(): SharedPreferences {
        val masterKey = createMasterKeyWithRetry()
        if (masterKey != null) {
            try {
                return EncryptedSharedPreferences.create(...)
            } catch (e: Exception) { }
        }
        
        // 降级到普通 SharedPreferences
        isEncryptionAvailable = false
        return context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private fun createMasterKeyWithRetry(): MasterKey? {
        for (attempt in 0 until MAX_RETRY_COUNT) {
            try {
                return MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
            } catch (e: Exception) {
                if (attempt < MAX_RETRY_COUNT - 1) {
                    Thread.sleep(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }
        return null
    }
}
```

### 5.4 为何这样修能从机制上避免问题

1. **自定义 getter 替代 `by lazy`**：不会被 Hilt 依赖图验证触发
2. **重试机制**：给 Keystore 服务更多恢复时间
3. **降级策略**：确保应用在任何情况下都能启动
4. **Application 级别的 Provider 注入**：延迟到 `onCreate()` 之后再获取实例

---

## 6. 修复实施记录

### 6.1 第一次修复

- `ApiKeyStorage.kt`：自定义 getter + synchronized + 重试 + 降级
- `EmpathyApplication.kt`：Provider<T> 延迟注入 + 1秒启动延迟

### 6.2 第二次修复（全面修复）

- `UserProfilePreferences.kt`：移除 `by lazy`，统一使用自定义 getter 模式
- `SettingsRepositoryImpl.kt`：移除 `by lazy`，统一使用自定义 getter 模式

---

## 7. 变更记录

| 日期 | 版本 | 变更内容 |
|------|------|----------|
| 2025-12-21 | 1.0 | 初始分析文档 |
| 2025-12-21 | 2.0 | 完成 ApiKeyStorage 和 EmpathyApplication 修复 |
| 2025-12-21 | 3.0 | 完成 UserProfilePreferences 和 SettingsRepositoryImpl 修复 |
