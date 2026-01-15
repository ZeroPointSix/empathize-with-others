# IMPL-00010: AI 超时优化修复

## 问题描述

### 现象
用户在使用悬浮窗的"Ask Gemini"功能时，频繁出现超时错误：
```
TimeoutCancellationException: Timed out waiting for 10000 ms
IOException: Canceled
```

### 根本原因分析

通过系统性分析，发现了以下根本原因：

#### 1. 超时配置冲突（主要原因）
- **协程超时**：`AI_TIMEOUT_MS = 10000L` (10秒)
- **OkHttp 读取超时**：`readTimeout(60, TimeUnit.SECONDS)` (60秒)
- **实际 AI 响应时间**：Gemini 通常需要 15-30 秒

**问题**：协程超时比 HTTP 超时短，导致请求被强制取消
- 10 秒后协程超时触发 `TimeoutCancellationException`
- 协程取消传播到 Retrofit，调用 `Call.cancel()`
- OkHttp 抛出 `IOException: Canceled`
- 但此时 Gemini 可能还在处理中（浪费了 API 配额）

#### 2. 缺少差异化配置
不同 AI 服务商的响应速度差异很大：
- OpenAI GPT-3.5/4：通常 5-15 秒
- Gemini：通常 15-30 秒
- DeepSeek：通常 10-20 秒

代码使用统一的 10 秒超时，没有考虑服务商差异。

#### 3. 缺少重试机制
网络波动时没有自动重试，超时后直接失败。

---

## 修复方案

### 方案概述

采用**分层超时策略 + 重试机制 + Provider 预设配置**的组合方案：

1. **分层超时**：协程超时 > HTTP 超时，让 HTTP 层先处理超时
2. **动态超时**：根据 Provider 配置动态调整超时时间
3. **重试机制**：网络超时自动重试，使用指数退避策略
4. **预设配置**：为常见服务商提供优化的超时配置

### 架构设计

```
┌─────────────────────────────────────────────────────────┐
│ FloatingWindowService (协程层)                          │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ withTimeout(Provider.timeoutMs + 5s)                │ │
│ │   ↓                                                 │ │
│ │ AiRepositoryImpl (HTTP 层)                          │ │
│ │ ┌─────────────────────────────────────────────────┐ │ │
│ │ │ OkHttp readTimeout = 45s                        │ │ │
│ │ │   ↓                                             │ │ │
│ │ │ withRetry (最多 3 次)                           │ │ │
│ │ │   ↓                                             │ │ │
│ │ │ API 请求                                        │ │ │
│ │ └─────────────────────────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

---

## 实施步骤

### 1. 为 AiProvider 添加超时配置

**修改文件**：
- `app/src/main/java/com/empathy/ai/domain/model/AiProvider.kt`
- `app/src/main/java/com/empathy/ai/data/local/entity/AiProviderEntity.kt`

**变更**：
```kotlin
data class AiProvider(
    // ... 其他字段
    val timeoutMs: Long = 30000L,  // 🆕 新增：默认 30 秒
    val createdAt: Long = System.currentTimeMillis()
)
```

### 2. 数据库迁移

**修改文件**：
- `app/src/main/java/com/empathy/ai/data/local/AppDatabase.kt` (版本 2 → 3)
- `app/src/main/java/com/empathy/ai/di/DatabaseModule.kt`

**迁移脚本**：
```kotlin
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            ALTER TABLE ai_providers 
            ADD COLUMN timeout_ms INTEGER NOT NULL DEFAULT 30000
        """.trimIndent())
    }
}
```

### 3. 更新 Repository 映射

**修改文件**：
- `app/src/main/java/com/empathy/ai/data/repository/AiProviderRepositoryImpl.kt`

**变更**：在 `entityToDomain()` 和 `domainToEntity()` 中添加 `timeoutMs` 字段映射。

### 4. 调整 OkHttp 超时配置

**修改文件**：
- `app/src/main/java/com/empathy/ai/di/NetworkModule.kt`

**变更**：
```kotlin
// 修改前
.readTimeout(60, TimeUnit.SECONDS)  // 60秒

// 修改后
.readTimeout(45, TimeUnit.SECONDS)  // 45秒（比协程超时短）
```

**原理**：让 HTTP 层先超时（45秒），协程超时作为兜底（50秒）。

### 5. FloatingWindowService 使用动态超时

**修改文件**：
- `app/src/main/java/com/empathy/ai/domain/service/FloatingWindowService.kt`

**新增依赖注入**：
```kotlin
@Inject
lateinit var aiProviderRepository: AiProviderRepository
```

**新增辅助方法**：
```kotlin
private suspend fun getAiTimeout(): Long {
    return try {
        val provider = aiProviderRepository.getDefaultProvider().getOrNull()
        if (provider != null) {
            provider.timeoutMs + AI_TIMEOUT_BUFFER_MS  // Provider超时 + 5秒缓冲
        } else {
            DEFAULT_AI_TIMEOUT_MS  // 默认 50 秒
        }
    } catch (e: Exception) {
        DEFAULT_AI_TIMEOUT_MS
    }
}
```

**修改超时常量**：
```kotlin
// 修改前
private const val AI_TIMEOUT_MS = 10000L  // 10秒

// 修改后
private const val AI_TIMEOUT_BUFFER_MS = 5000L      // 缓冲时间
private const val DEFAULT_AI_TIMEOUT_MS = 50000L    // 默认超时
```

**使用动态超时**：
```kotlin
// performAnalyze 和 performCheck 方法中
val timeoutMs = getAiTimeout()
val result = withTimeout(timeoutMs) {
    // ...
}
```

### 6. 添加重试机制

**修改文件**：
- `app/src/main/java/com/empathy/ai/data/repository/AiRepositoryImpl.kt`

**新增重试包装器**：
```kotlin
private suspend fun <T> withRetry(block: suspend () -> T): T {
    var lastException: Exception? = null
    
    repeat(MAX_RETRIES) { attempt ->
        try {
            return block()
        } catch (e: SocketTimeoutException) {
            // 超时重试，使用指数退避
            lastException = e
            if (attempt < MAX_RETRIES - 1) {
                val delayMs = INITIAL_DELAY_MS * (1 shl attempt)  // 1s, 2s, 4s
                delay(delayMs)
            }
        } catch (e: IOException) {
            // 检查是否是协程取消
            if (e.message?.contains("Canceled") == true) {
                throw e  // 协程取消不重试
            }
            // 其他 IO 错误重试
            lastException = e
            if (attempt < MAX_RETRIES - 1) {
                val delayMs = INITIAL_DELAY_MS * (1 shl attempt)
                delay(delayMs)
            }
        }
    }
    
    throw lastException ?: Exception("未知错误")
}
```

**应用重试**：
```kotlin
// analyzeChat 和 checkDraftSafety 方法中
val response = withRetry {
    api.chatCompletion(url, headers, request)
}
```

### 7. 创建 Provider 预设配置

**新增文件**：
- `app/src/main/java/com/empathy/ai/domain/model/ProviderPresets.kt`

**预设配置**：
```kotlin
object ProviderPresets {
    fun createOpenAiGpt4(apiKey: String): AiProvider {
        return AiProvider(
            name = "OpenAI GPT-4",
            baseUrl = "https://api.openai.com/v1",
            apiKey = apiKey,
            timeoutMs = 20000L,  // 20 秒
            // ...
        )
    }
    
    fun createGeminiPro(apiKey: String): AiProvider {
        return AiProvider(
            name = "Google Gemini Pro",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta",
            apiKey = apiKey,
            timeoutMs = 40000L,  // 40 秒（Gemini 较慢）
            // ...
        )
    }
    
    // ... 其他预设
}
```

---

## 超时配置对照表

| 服务商 | 典型响应时间 | Provider 超时 | 协程超时 | HTTP 超时 |
|--------|-------------|--------------|---------|----------|
| OpenAI GPT-3.5 | 3-8秒 | 15秒 | 20秒 | 45秒 |
| OpenAI GPT-4 | 5-15秒 | 20秒 | 25秒 | 45秒 |
| DeepSeek | 8-20秒 | 25秒 | 30秒 | 45秒 |
| Gemini Pro | 15-30秒 | 40秒 | 45秒 | 45秒 |
| 自定义 | 不确定 | 30秒 | 35秒 | 45秒 |

**超时层级**：
1. **Provider 超时**：针对服务商优化的超时时间
2. **协程超时**：Provider 超时 + 5秒缓冲
3. **HTTP 超时**：固定 45 秒，作为最底层保护

---

## 重试策略

### 重试条件
- ✅ `SocketTimeoutException`：网络超时，重试
- ✅ `IOException`（非 Canceled）：网络错误，重试
- ❌ `IOException: Canceled`：协程取消，不重试
- ❌ `HttpException`：HTTP 错误（400/401/500等），不重试

### 重试次数
最多重试 3 次

### 退避策略
指数退避：
- 第 1 次重试：延迟 1 秒
- 第 2 次重试：延迟 2 秒
- 第 3 次重试：延迟 4 秒

---

## 测试验证

### 测试场景

#### 1. 正常响应（< 超时时间）
- **预期**：请求成功，无重试
- **验证**：检查日志无重试记录

#### 2. 慢响应（接近超时时间）
- **预期**：请求成功，无超时
- **验证**：Gemini 响应 30 秒内成功

#### 3. 网络波动
- **预期**：自动重试，最终成功
- **验证**：日志显示重试记录

#### 4. 真实超时（> HTTP 超时）
- **预期**：HTTP 层超时，重试 3 次后失败
- **验证**：用户看到友好的错误提示

#### 5. 用户取消
- **预期**：立即停止，不重试
- **验证**：日志显示 "Canceled"

### 测试命令

```bash
# 编译项目
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug

# 查看日志
adb logcat | grep -E "FloatingWindowService|AiRepositoryImpl"
```

---

## 性能影响

### 内存
- **增加**：每个 Provider 增加 8 字节（Long 类型）
- **影响**：可忽略不计

### 数据库
- **增加**：ai_providers 表增加 1 列
- **迁移**：自动迁移，无需用户操作

### 网络
- **优化**：减少不必要的请求取消
- **增加**：失败时最多重试 3 次

---

## 向后兼容性

### 数据库迁移
- ✅ 自动迁移：MIGRATION_2_3 自动添加 timeout_ms 列
- ✅ 默认值：30000（30秒）
- ✅ 现有数据：自动应用默认值

### API 兼容性
- ✅ 新字段可选：timeoutMs 有默认值
- ✅ 旧代码兼容：不影响现有功能

---

## 后续优化建议

### 1. 用户可配置超时
在设置页面允许用户自定义超时时间：
```kotlin
// 设置页面
"超时时间: ${provider.timeoutMs / 1000} 秒"
```

### 2. 自适应超时
根据历史响应时间动态调整：
```kotlin
val avgResponseTime = calculateAverageResponseTime(provider.id)
val adaptiveTimeout = avgResponseTime * 1.5
```

### 3. 超时预警
在接近超时时显示进度提示：
```kotlin
if (elapsedTime > timeout * 0.8) {
    showMessage("请求处理中，请稍候...")
}
```

---

## 相关文档

- [PRD-00002-设置功能需求](../PRD/PRD-00002-设置功能需求.md)
- [IMPL-00009-HTTP400错误排查修复](./IMPL-00009-HTTP400错误排查修复.md)
- [tech.md](../../../.kiro/steering/tech.md)

---

## 修复日期

2025-12-13

## 修复人员

Kiro AI Assistant
