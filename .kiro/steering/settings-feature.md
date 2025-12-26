# 设置功能开发规范

> 最后更新: 2025-12-25 | 更新者: Claude

## 🔴 必读文档

**开发设置功能相关代码前，必须先阅读：**

1. **[PRD-00002-设置功能需求](../../文档/开发文档/PRD/PRD-00002-设置功能需求.md)** - 完整需求文档
2. **[WORKSPACE.md](../../WORKSPACE.md)** - 当前工作状态

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

5. **历史对话计数设置** - ✅ 已完成
   - 显示历史对话总数统计
   - 支持清除历史对话记录
   - 确认对话框防止误操作
   - 集成HistoryConversationCountSection组件

6. **提示词设置优化** - ✅ 已完成（TD-00015）
   - 显示4个核心场景（分析、润色、回复、总结）
   - 隐藏废弃场景（安全检查、信息提取）
   - 点击场景项跳转到提示词编辑器
   - 完成数据迁移（CHECK合并到POLISH）
   - 集成PromptSettingsSection组件

### ❌ 明确不包含（后续版本）

- 主题设置（深色/浅色模式）
- 字体大小调节
- 通知设置（使用系统默认）
- 数据导入导出
- 多语言支持

---

## 架构要求

### 文件位置（多模块架构）

```
:domain/src/main/kotlin/com/empathy/ai/domain/
└── repository/
    └── SettingsRepository.kt      ✅ 已存在

:data/src/main/kotlin/com/empathy/ai/data/
├── repository/settings/
│   └── SettingsRepositoryImpl.kt  ✅ 已存在
└── local/
    ├── FloatingWindowPreferences.kt  ✅ 已存在
    ├── PrivacyPreferences.kt         ✅ 已存在
    ├── MemoryPreferences.kt          ✅ 已存在
    ├── ConversationPreferences.kt    ✅ 已存在
    └── UserProfilePreferences.kt     ✅ 已存在

:presentation/src/main/kotlin/com/empathy/ai/presentation/
├── ui/screen/settings/
│   ├── SettingsScreen.kt          ✅ 已存在
│   ├── SettingsUiState.kt         ✅ 已存在
│   ├── SettingsUiEvent.kt         ✅ 已存在
│   └── component/
│       ├── HistoryConversationCountSection.kt  ✅ 已存在
│       └── PromptSettingsSection.kt            ✅ 已存在
└── viewmodel/
    └── SettingsViewModel.kt       ✅ 已存在
```

### 关键实现点

#### 1. 隐私设置持久化

**位置**: `data/src/main/kotlin/com/empathy/ai/data/local/PrivacyPreferences.kt`

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

**位置**: `presentation/src/main/kotlin/com/empathy/ai/presentation/viewmodel/SettingsViewModel.kt`

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val application: Application,
    private val settingsRepository: SettingsRepository,
    private val floatingWindowPreferences: FloatingWindowPreferences,
    private val privacyPreferences: PrivacyPreferences,  // 🆕 注入
    private val aiProviderRepository: AiProviderRepository
) : AndroidViewModel(getApplication()) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

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

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.ToggleDataMasking -> toggleDataMasking()
            is SettingsUiEvent.ToggleLocalFirstMode -> toggleLocalFirstMode()
            // 其他事件处理...
        }
    }
}
```

#### 3. 在业务逻辑中读取设置

**在 `PrivacyEngine` 中**：

```kotlin
@Singleton
class PrivacyEngine @Inject constructor(
    private val privacyPreferences: PrivacyPreferences,
    private val privacyRepository: PrivacyRepository
) {
    suspend fun maskSensitiveData(text: String): String {
        // 检查是否启用数据掩码
        if (!privacyPreferences.isDataMaskingEnabled()) {
            return text  // 未启用，直接返回原文
        }

        // 执行掩码逻辑
        val mappings = privacyRepository.getAllMappingRules()
        // 应用掩码规则...
        return maskedText
    }
}
```

**在 `CheckDraftUseCase` 中**：

```kotlin
@Singleton
class CheckDraftUseCase @Inject constructor(
    private val privacyPreferences: PrivacyPreferences,
    private val aiRepository: AiRepository,
    private val ruleEngine: RuleEngine
) {
    suspend operator fun invoke(draft: String): Result<SafetyCheckResult> {
        // 检查是否启用本地优先模式
        if (privacyPreferences.isLocalFirstModeEnabled()) {
            // 先使用本地规则检查
            val localResult = ruleEngine.checkSafety(draft)
            if (localResult.isSafe) {
                return Result.success(localResult)
            }
        }

        // 使用AI检查
        return aiRepository.checkSafety(draft)
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
    val context = ApplicationProvider.getApplicationContext<Context>()
    val preferences = PrivacyPreferences(context)

    // When
    preferences.setDataMaskingEnabled(false)

    // Then
    assertFalse(preferences.isDataMaskingEnabled())
}

@Test
fun `本地优先模式开关能正确保存和读取`() {
    // Given
    val context = ApplicationProvider.getApplicationContext<Context>()
    val preferences = PrivacyPreferences(context)

    // When
    preferences.setLocalFirstModeEnabled(false)

    // Then
    assertFalse(preferences.isLocalFirstModeEnabled())
}
```

### UI测试

```kotlin
@Test
fun `点击数据掩码开关能正确切换状态`() {
    composeTestRule.setContent {
        SettingsScreen(
            uiState = SettingsUiState(),
            onEvent = {}
        )
    }

    // 点击开关
    composeTestRule.onNodeWithText("数据掩码").performClick()

    // 验证状态已改变
    composeTestRule.onNodeWithText("已关闭").assertIsDisplayed()
}

@Test
fun `点击本地优先模式开关能正确切换状态`() {
    composeTestRule.setContent {
        SettingsScreen(
            uiState = SettingsUiState(),
            onEvent = {}
        )
    }

    // 点击开关
    composeTestRule.onNodeWithText("本地优先模式").performClick()

    // 验证状态已改变
    composeTestRule.onNodeWithText("已关闭").assertIsDisplayed()
}
```

---

## 待解决问题

### ✅ 已解决问题

1. **PrivacyPreferences.kt创建状态**: 已完成
   - ✅ PrivacyPreferences.kt已创建并实现
   - ✅ 包含数据掩码和本地优先模式的持久化功能
   - ✅ 与SettingsViewModel正确集成

2. **示例代码一致性**: 已验证
   - ✅ SettingsViewModel中的注入和调用方式与当前实现一致
   - ✅ PrivacyEngine中的设置读取方式与当前实现一致
   - ✅ CheckDraftUseCase中的设置检查逻辑与当前实现一致

3. **悬浮球状态指示与启动模式问题**: 已完成BUG-00014修复
   - ✅ 添加显示模式持久化（FloatingWindowPreferences）
   - ✅ 修复启动时直接显示对话框问题
   - ✅ 在AI调用流程中集成状态回调
   - ✅ 实现悬浮球加载状态和完成通知

4. **三种模式上下文不共通问题**: 已完成BUG-00015修复
   - ✅ 新增SessionContextService统一管理历史上下文
   - ✅ 修改PolishDraftUseCase和GenerateReplyUseCase添加历史上下文支持
   - ✅ 更新FloatingWindowModule添加依赖注入
   - ✅ 新增相关测试用例验证修复效果

5. **悬浮窗结果页内容过长导致按钮不可见问题**: 已完成BUG-00021修复
   - ✅ 采用动态高度计算策略，将结果区域最大高度限制为屏幕高度的40%
   - ✅ 确保底部操作按钮（复制、重新生成）始终在屏幕可见范围内
   - ✅ 在ResultCard中暴露setMaxHeight接口，支持动态调整
   - ✅ 新增MaxHeightScrollView组件，支持内容超出时的滚动

6. **AI响应JSON解析失败问题**: 已完成BUG-00025修复
   - ✅ 增强EnhancedJsonCleaner的清理能力
   - ✅ 改进AiResponseCleaner的错误处理机制
   - ✅ 优化FallbackHandler的错误恢复策略
   - ✅ 提升AI响应解析的稳定性和容错性

7. **历史对话计数设置**: 已完成
   - ✅ 新增HistoryConversationCountSection组件
   - ✅ 支持显示历史对话总数统计
   - ✅ 支持清除历史对话记录功能
   - ✅ 添加确认对话框防止误操作
   - ✅ 与SettingsViewModel正确集成

8. **提示词设置优化**: 已完成（TD-00015）
   - ✅ 简化提示词场景从6个到4个核心场景（分析、润色、回复、总结）
   - ✅ 废弃CHECK和EXTRACT场景（保留代码兼容性，隐藏UI）
   - ✅ 实现CHECK到POLISH的数据迁移逻辑
   - ✅ GlobalPromptConfig版本升级到v3
   - ✅ 新增PromptSettingsSection组件，集成到设置界面
   - ✅ 完整测试覆盖：7个测试文件，61+个测试用例
   - ✅ 状态：22/25任务完成（88%，核心功能100%）

9. **Clean Architecture多模块改造**: 已完成（TD-00017）
   - ✅ 创建:domain模块（纯Kotlin，无Android依赖）
   - ✅ 创建:data模块（Android Library，Room、Retrofit、Repository实现）
   - ✅ 创建:presentation模块（Android Library，Compose UI、ViewModel）
   - ✅ 重构:app模块（应用入口、Android服务、DI聚合）
   - ✅ 完成65/65任务，100%完成率
   - ✅ Release APK构建成功（4.2MB）

### ⚠️ 待解决问题

### 1. 清除数据范围
**问题**: 当前"清除所有数据"只清除AI服务商，是否需要清除联系人和标签？

**建议**: MVP阶段只清除设置数据，不清除用户数据（联系人、标签）

### 2. 关于信息内容
**问题**: 是否需要添加开源许可、隐私政策等链接？

**建议**: MVP阶段只显示版本号和简介，后续版本再添加

### 3. 输入内容身份识别与双向对话历史功能
**问题**: TD-00008技术设计已完成，但尚未实现

**建议**: 根据TDD-00008技术设计文档实现IdentityPrefixHelper和相关功能

### 4. 手动触发AI总结功能
**问题**: TD-00011技术设计已完成，但尚未实现

**建议**: 根据TDD-00011技术设计文档实现ManualSummaryUseCase和相关UI组件

---

## 相关文档

- [PRD-00002-设置功能需求](../../文档/开发文档/PRD/PRD-00002-设置功能需求.md)
- [product.md](./product.md) - 产品概览
- [structure.md](./structure.md) - 项目结构
- [tech.md](./tech.md) - 技术栈