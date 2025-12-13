# 设置功能代码审查规范

## 🔴 必读文档

**审查设置功能相关代码前，必须先阅读：**

1. **[PRD-00002-设置功能需求](../../文档/开发文档/PRD/PRD-00002-设置功能需求.md)** - 需求文档
2. **[FD-00002-设置功能设计](../../文档/开发文档/FD/FD-00002-设置功能设计.md)** - 功能设计（待创建）
3. **[TDD-00002-设置架构设计](../../文档/开发文档/TDD/TDD-00002-设置架构设计.md)** - 架构设计（待创建）
4. **[Rules/WORKSPACE.md](../../Rules/WORKSPACE.md)** - 工作状态

---

## Roo的审查职责

### 代码审查重点

1. **架构合规性**
   - 是否遵循Clean Architecture
   - 是否正确使用MVVM模式
   - 是否正确使用Hilt依赖注入

2. **代码质量**
   - 是否有充分的注释
   - 是否有错误处理
   - 是否有单元测试

3. **功能完整性**
   - 是否实现了PRD中的所有需求
   - 是否符合FD中的设计
   - 是否符合TDD中的架构

4. **性能和安全**
   - 是否有性能问题
   - 是否有安全隐患
   - 是否正确使用加密存储

---

## MVP范围检查

### ✅ 应该实现的功能

1. **AI服务商配置**
   - [ ] 显示当前默认服务商
   - [ ] 切换默认服务商
   - [ ] 跳转到服务商管理
   - [ ] 未配置时的提示

2. **隐私保护设置**
   - [ ] 数据掩码开关
   - [ ] 本地优先模式开关
   - [ ] 设置持久化到SharedPreferences
   - [ ] 在业务逻辑中正确读取设置

3. **悬浮窗设置**
   - [ ] 权限检测
   - [ ] 启用/禁用服务
   - [ ] 权限引导对话框
   - [ ] 状态持久化

4. **关于信息**
   - [ ] 显示版本号
   - [ ] 显示应用简介

### ❌ 不应该实现的功能

- 主题设置
- 字体大小调节
- 通知设置
- 数据导入导出
- 多语言支持

**如果发现这些功能被实现，应标记为"超出MVP范围"。**

---

## 关键审查点

### 1. PrivacyPreferences实现

**必须检查**：

```kotlin
// ✅ 正确：使用SharedPreferences
class PrivacyPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("privacy_settings", Context.MODE_PRIVATE)
    
    fun isDataMaskingEnabled(): Boolean = prefs.getBoolean("data_masking_enabled", true)
    fun setDataMaskingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("data_masking_enabled", enabled).apply()
    }
}

// ❌ 错误：使用Room数据库（过度设计）
@Entity
data class PrivacySettings(...)
```

**审查要点**：
- [ ] 使用SharedPreferences而非Room
- [ ] 默认值正确（数据掩码=true，本地优先=true）
- [ ] 键名使用常量定义
- [ ] 使用`@Singleton`注解

### 2. ViewModel中的持久化

**必须检查**：

```kotlin
// ✅ 正确：调用Preferences保存
private fun toggleDataMasking() {
    val newValue = !_uiState.value.dataMaskingEnabled
    privacyPreferences.setDataMaskingEnabled(newValue)  // 保存
    _uiState.update { it.copy(dataMaskingEnabled = newValue) }
}

// ❌ 错误：只更新UI状态，未保存
private fun toggleDataMasking() {
    _uiState.update { it.copy(dataMaskingEnabled = !it.dataMaskingEnabled) }
    // TODO: 保存到 SharedPreferences  ← 这是错误的
}
```

**审查要点**：
- [ ] 所有开关切换都调用Preferences保存
- [ ] init()中加载保存的设置
- [ ] 没有TODO注释

### 3. 业务逻辑中读取设置

**必须检查**：

```kotlin
// ✅ 正确：在业务逻辑中读取设置
class PrivacyEngine @Inject constructor(
    private val privacyPreferences: PrivacyPreferences
) {
    suspend fun maskSensitiveData(text: String): String {
        if (!privacyPreferences.isDataMaskingEnabled()) {
            return text
        }
        // 执行掩码
    }
}

// ❌ 错误：硬编码或忽略设置
suspend fun maskSensitiveData(text: String): String {
    // 总是执行掩码，忽略用户设置
}
```

**审查要点**：
- [ ] PrivacyEngine读取数据掩码设置
- [ ] CheckDraftUseCase读取本地优先设置
- [ ] 设置关闭时跳过相应逻辑

### 4. 依赖注入配置

**必须检查**：

```kotlin
// ✅ 正确：在Module中提供
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun providePrivacyPreferences(
        @ApplicationContext context: Context
    ): PrivacyPreferences = PrivacyPreferences(context)
}

// ❌ 错误：直接new实例
class SettingsViewModel(...) {
    private val privacyPreferences = PrivacyPreferences(context)  // 错误
}
```

**审查要点**：
- [ ] PrivacyPreferences在Module中提供
- [ ] 使用@Singleton注解
- [ ] ViewModel通过构造函数注入

---

## 测试审查

### 单元测试要求

**必须有的测试**：

```kotlin
// PrivacyPreferencesTest.kt
@Test
fun `数据掩码默认开启`()

@Test
fun `能正确保存和读取数据掩码设置`()

@Test
fun `本地优先模式默认开启`()

@Test
fun `能正确保存和读取本地优先设置`()

// SettingsViewModelTest.kt
@Test
fun `切换数据掩码能正确保存`()

@Test
fun `init时能正确加载保存的设置`()

@Test
fun `切换服务商能正确更新默认服务商`()
```

**审查要点**：
- [ ] 测试覆盖率 > 80%
- [ ] 所有关键路径都有测试
- [ ] 使用MockK进行mock
- [ ] 测试命名清晰

---

## 性能审查

### 性能要求

- [ ] 设置页面加载时间 < 500ms
- [ ] 开关切换响应时间 < 100ms
- [ ] 悬浮窗服务启动时间 < 1s
- [ ] 内存占用 < 50MB

### 常见性能问题

**❌ 错误示例**：

```kotlin
// 每次都读取SharedPreferences（性能差）
fun isDataMaskingEnabled(): Boolean {
    val prefs = context.getSharedPreferences(...)
    return prefs.getBoolean(...)
}
```

**✅ 正确示例**：

```kotlin
// 缓存SharedPreferences实例
private val prefs by lazy {
    context.getSharedPreferences(...)
}

fun isDataMaskingEnabled(): Boolean {
    return prefs.getBoolean(...)
}
```

---

## 安全审查

### 安全要求

- [ ] 敏感数据使用EncryptedSharedPreferences
- [ ] 普通设置使用SharedPreferences
- [ ] 权限申请有明确说明
- [ ] 没有硬编码的密钥或密码

### 常见安全问题

**❌ 错误示例**：

```kotlin
// 使用普通SharedPreferences存储API Key（不安全）
prefs.putString("api_key", apiKey)
```

**✅ 正确示例**：

```kotlin
// 使用EncryptedSharedPreferences存储API Key
encryptedPrefs.putString("api_key", apiKey)
```

---

## 审查流程

### 1. 审查前准备

- [ ] 读取PRD、FD、TDD文档
- [ ] 读取WORKSPACE，了解开发状态
- [ ] 在WORKSPACE中记录审查开始

### 2. 执行审查

- [ ] 检查架构合规性
- [ ] 检查代码质量
- [ ] 检查功能完整性
- [ ] 检查测试覆盖率
- [ ] 检查性能和安全

### 3. 输出审查报告

**报告格式**：

```markdown
# CR-00002-设置功能代码审查

## 审查概况
- 审查时间: YYYY-MM-DD
- 审查人: Roo
- 审查范围: 设置功能相关代码

## 审查结果
- 架构合规性: ✅ 通过 / ⚠️ 有问题 / ❌ 不通过
- 代码质量: ✅ 通过 / ⚠️ 有问题 / ❌ 不通过
- 功能完整性: ✅ 通过 / ⚠️ 有问题 / ❌ 不通过
- 测试覆盖率: XX%

## 发现的问题
1. [严重] 问题描述
2. [一般] 问题描述
3. [建议] 问题描述

## 改进建议
1. 建议1
2. 建议2

## 总体评价
通过 / 有条件通过 / 不通过
```

### 4. 完成后更新

- [ ] 更新WORKSPACE审查状态
- [ ] 添加变更日志
- [ ] 如有问题，通知Kiro修复

---

## 与其他AI的协作

### 与Kiro的协作

**发现问题时**：
1. 在WORKSPACE中标记问题
2. 在代码审查报告中详细说明
3. 等待Kiro修复后重新审查

**审查通过后**：
1. 在WORKSPACE中标记"审查通过"
2. 通知用户可以合并代码

### 与Claude的协作

**发现文档问题时**：
1. 在WORKSPACE中标记"文档需要更新"
2. 说明文档与代码的不一致之处
3. 等待Claude更新文档

---

## 相关文档

- [PRD-00002-设置功能需求](../../文档/开发文档/PRD/PRD-00002-设置功能需求.md)
- [Rules/WORKSPACE.md](../../Rules/WORKSPACE.md)
