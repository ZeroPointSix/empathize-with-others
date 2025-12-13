# 设置功能开发规范

## 🔴 必读文档

**开发设置功能相关代码前，必须先阅读：**

1. **[PRD-00002-设置功能需求](../../文档/开发文档/PRD/PRD-00002-设置功能需求.md)** - 完整需求文档
2. **[Rules/WORKSPACE.md](../../Rules/WORKSPACE.md)** - 当前工作状态

---

## MVP范围

### ✅ 本次实现（MVP阶段）

1. **AI服务商配置**
   - 显示当前默认服务商
   - 切换默认服务商
   - 跳转到服务商管理页面
   - 未配置时显示友好提示

2. **隐私保护设置**
   - 数据掩码开关（默认开启）
   - 本地优先模式开关（默认开启）
   - 设置持久化保存到SharedPreferences

3. **悬浮窗设置**
   - 权限检测和状态显示
   - 启用/禁用悬浮窗服务
   - 权限引导对话框
   - 状态持久化保存

4. **关于信息**
   - 应用版本号
   - 应用名称和简介

### ❌ 明确不包含（后续版本）

- 主题设置（深色/浅色模式）
- 字体大小调节
- 通知设置（使用系统默认）
- 数据导入导出
- 多语言支持

---

## 架构要求

### 文件位置

```
presentation/
├── ui/screen/settings/
│   ├── SettingsScreen.kt          ✅ 已存在
│   ├── SettingsUiState.kt         ✅ 已存在
│   └── SettingsUiEvent.kt         ✅ 已存在
├── viewmodel/
│   └── SettingsViewModel.kt       ✅ 已存在
domain/
└── repository/
    └── SettingsRepository.kt      ✅ 已存在
data/
├── repository/settings/
│   └── SettingsRepositoryImpl.kt  ✅ 已存在
└── local/
    ├── FloatingWindowPreferences.kt  ✅ 已存在
    └── PrivacyPreferences.kt         🆕 需要创建
```

### 关键实现点

#### 1. 隐私设置持久化

**需要创建 `PrivacyPreferences.kt`**：

```kotlin
@Singleton
class PrivacyPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("privacy_settings", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_DATA_MASKING = "data_masking_enabled"
        private const val KEY_LOCAL_FIRST = "local_first_mode_enabled"
    }
    
    fun isDataMaskingEnabled(): Boolean = prefs.getBoolean(KEY_DATA_MASKING, true)
    fun setDataMaskingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DATA_MASKING, enabled).apply()
    }
    
    fun isLocalFirstModeEnabled(): Boolean = prefs.getBoolean(KEY_LOCAL_FIRST, true)
    fun setLocalFirstModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCAL_FIRST, enabled).apply()
    }
}
```

#### 2. ViewModel中的持久化调用

**修改 `SettingsViewModel.kt`**：

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val floatingWindowPreferences: FloatingWindowPreferences,
    private val privacyPreferences: PrivacyPreferences,  // 🆕 注入
    private val aiProviderRepository: AiProviderRepository
) : AndroidViewModel(application) {
    
    // 加载设置
    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    dataMaskingEnabled = privacyPreferences.isDataMaskingEnabled(),
                    localFirstMode = privacyPreferences.isLocalFirstModeEnabled()
                )
            }
        }
    }
    
    // 切换数据掩码
    private fun toggleDataMasking() {
        val newValue = !_uiState.value.dataMaskingEnabled
        privacyPreferences.setDataMaskingEnabled(newValue)
        _uiState.update { it.copy(dataMaskingEnabled = newValue) }
    }
    
    // 切换本地优先模式
    private fun toggleLocalFirstMode() {
        val newValue = !_uiState.value.localFirstMode
        privacyPreferences.setLocalFirstModeEnabled(newValue)
        _uiState.update { it.copy(localFirstMode = newValue) }
    }
}
```

#### 3. 在业务逻辑中读取设置

**在 `PrivacyEngine` 中**：

```kotlin
class PrivacyEngine @Inject constructor(
    private val privacyPreferences: PrivacyPreferences
) {
    suspend fun maskSensitiveData(text: String): String {
        // 检查是否启用数据掩码
        if (!privacyPreferences.isDataMaskingEnabled()) {
            return text  // 未启用，直接返回原文
        }
        
        // 执行掩码逻辑
        // ...
    }
}
```

**在 `CheckDraftUseCase` 中**：

```kotlin
class CheckDraftUseCase @Inject constructor(
    private val privacyPreferences: PrivacyPreferences,
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(draft: String): Result<SafetyCheckResult> {
        // 检查是否启用本地优先模式
        if (privacyPreferences.isLocalFirstModeEnabled()) {
            // 先使用本地规则检查
            val localResult = checkWithLocalRules(draft)
            if (localResult.isSafe) {
                return Result.success(localResult)
            }
        }
        
        // 使用AI检查
        return checkWithAi(draft)
    }
}
```

---

## 代码规范

### 命名规范

- **Preferences类**: `XxxPreferences.kt`
- **键名常量**: `KEY_XXX_XXX` (大写下划线)
- **方法名**: `isXxxEnabled()`, `setXxxEnabled()`

### 默认值

- **数据掩码**: 默认开启 (`true`)
- **本地优先模式**: 默认开启 (`true`)
- **悬浮窗**: 默认关闭 (`false`)

### 错误处理

- 所有设置读取失败时使用默认值
- 设置保存失败时显示Toast提示
- 不因设置问题导致应用崩溃

---

## 测试要求

### 单元测试

```kotlin
@Test
fun `数据掩码开关能正确保存和读取`() {
    // Given
    val preferences = PrivacyPreferences(context)
    
    // When
    preferences.setDataMaskingEnabled(false)
    
    // Then
    assertFalse(preferences.isDataMaskingEnabled())
}
```

### UI测试

```kotlin
@Test
fun `点击数据掩码开关能正确切换状态`() {
    composeTestRule.setContent {
        SettingsScreen(...)
    }
    
    // 点击开关
    composeTestRule.onNodeWithText("数据掩码").performClick()
    
    // 验证状态已改变
    // ...
}
```

---

## 待解决问题

### 1. 清除数据范围
**问题**: 当前"清除所有数据"只清除AI服务商，是否需要清除联系人和标签？

**建议**: MVP阶段只清除设置数据，不清除用户数据（联系人、标签）

### 2. 关于信息内容
**问题**: 是否需要添加开源许可、隐私政策等链接？

**建议**: MVP阶段只显示版本号和简介，后续版本再添加

---

## 相关文档

- [PRD-00002-设置功能需求](../../文档/开发文档/PRD/PRD-00002-设置功能需求.md)
- [product.md](./product.md) - 产品概览
- [structure.md](./structure.md) - 项目结构
- [tech.md](./tech.md) - 技术栈
