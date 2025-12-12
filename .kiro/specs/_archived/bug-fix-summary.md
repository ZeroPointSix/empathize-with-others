# Bug 修复总结

## 修复日期
2025-12-10

## 问题描述

用户在手动测试时发现两个严重问题：

### 问题 1：数据库迁移错误 ❌

**错误信息**：
```
Room cannot verify the data integrity. Looks like you've changed schema but forgot to update the version number.
Expected identity hash: 55c85d17e0446a76e2e99b42993cb07f
Found: 620c70ef2718320b7504da287c4dbdbf
```

**根本原因**：
- 添加了 `AiProviderEntity` 到数据库
- 但忘记更新数据库版本号（仍然是 version = 1）
- Room 检测到 schema 变化但版本号未更新，抛出错误

**影响**：
- 应用无法启动
- 用户无法使用任何功能

### 问题 2：服务商选择界面缺失 ❌

**问题描述**：
- 用户添加 AI 服务商后，设置页面中没有显示已添加的服务商
- 服务商选择对话框显示的是硬编码的列表（OpenAI、DeepSeek、Google Gemini）
- 无法选择用户实际添加的服务商

**根本原因**：
- `SettingsUiState.availableProviders` 使用硬编码列表
- `SettingsViewModel` 没有从数据库加载实际的服务商列表
- 缺少 `AiProviderRepository` 依赖注入

**影响**：
- 用户添加的服务商无法使用
- 功能不完整

---

## 修复方案

### 修复 1：数据库迁移错误 ✅

**方案选择**：方案 A（简单方案，MVP 阶段推荐）

**修改内容**：

1. **更新数据库版本号**
   - 文件：`app/src/main/java/com/empathy/ai/data/local/AppDatabase.kt`
   - 修改：`version = 1` → `version = 2`
   - 更新文档注释，记录版本变更历史

2. **迁移策略**
   - 使用 `fallbackToDestructiveMigration()`（已在 DatabaseModule 中配置）
   - 升级时会清空所有数据（MVP 阶段可接受）

**代码变更**：

```kotlin
@Database(
    entities = [
        ContactProfileEntity::class,
        BrainTagEntity::class,
        com.empathy.ai.data.local.entity.AiProviderEntity::class
    ],
    version = 2, // 添加 AiProviderEntity 后升级到版本 2
    exportSchema = false
)
```

**验证结果**：
- ✅ 编译成功
- ✅ 构建成功
- ⏳ 等待用户测试

### 修复 2：服务商选择界面 ✅

**修改内容**：

1. **SettingsViewModel 注入 AiProviderRepository**
   - 文件：`app/src/main/java/com/empathy/ai/presentation/viewmodel/SettingsViewModel.kt`
   - 添加依赖注入：`private val aiProviderRepository: AiProviderRepository`

2. **加载服务商列表**
   - 添加 `loadProviders()` 方法
   - 在 `init` 中调用
   - 使用 Flow 监听数据库变化，自动更新列表

3. **更新选择服务商逻辑**
   - 修改 `selectProvider()` 方法
   - 不仅保存到 SettingsRepository，还要更新数据库中的默认服务商
   - 调用 `aiProviderRepository.setDefaultProvider()`

4. **修改 SettingsUiState**
   - 文件：`app/src/main/java/com/empathy/ai/presentation/ui/screen/settings/SettingsUiState.kt`
   - 修改：`availableProviders = listOf("OpenAI", "DeepSeek", "Google Gemini")` 
   - 改为：`availableProviders = emptyList()` // 从数据库动态加载

5. **优化 UI 显示**
   - 文件：`app/src/main/java/com/empathy/ai/presentation/ui/screen/settings/SettingsScreen.kt`
   - 添加空状态提示：当没有服务商时，显示友好提示卡片
   - 只有在有服务商时才显示选择卡片

**代码变更**：

```kotlin
// SettingsViewModel.kt
@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val floatingWindowPreferences: FloatingWindowPreferences,
    private val aiProviderRepository: AiProviderRepository // 新增
) : AndroidViewModel(application) {

    init {
        loadSettings()
        checkFloatingWindowPermission()
        loadFloatingWindowState()
        loadProviders() // 新增
    }

    private fun loadProviders() {
        viewModelScope.launch {
            try {
                aiProviderRepository.getAllProviders().collect { providers ->
                    val providerNames = providers.map { it.name }
                    _uiState.update {
                        it.copy(availableProviders = providerNames)
                    }
                    
                    // 如果当前选中的服务商不在列表中，选择默认服务商
                    val currentProvider = _uiState.value.selectedProvider
                    if (providerNames.isNotEmpty() && currentProvider !in providerNames) {
                        val defaultProvider = providers.find { it.isDefault }?.name 
                            ?: providerNames.firstOrNull() 
                            ?: "OpenAI"
                        _uiState.update { it.copy(selectedProvider = defaultProvider) }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "加载服务商列表失败", e)
            }
        }
    }

    private fun selectProvider(provider: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // 1. 获取选中的服务商
                val providerResult = aiProviderRepository.getProvider(provider)
                val selectedProvider = providerResult.getOrNull()
                
                if (selectedProvider == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "服务商 $provider 不存在"
                        )
                    }
                    return@launch
                }

                // 2. 设置为默认服务商
                val setDefaultResult = aiProviderRepository.setDefaultProvider(selectedProvider.id)
                
                setDefaultResult.onSuccess {
                    // 3. 同时保存到 SettingsRepository（向后兼容）
                    settingsRepository.saveAiProvider(provider)
                    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedProvider = provider,
                            showProviderDialog = false,
                            successMessage = "AI 服务商已切换为 $provider"
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "切换服务商失败"
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "切换服务商失败", e)
            }
        }
    }
}
```

**验证结果**：
- ✅ 编译成功
- ✅ 构建成功
- ⏳ 等待用户测试

---

## 测试计划

### 测试步骤

1. **卸载旧版本**（推荐）
   ```bash
   adb uninstall com.empathy.ai
   ```

2. **安装新版本**
   - APK 位置：`app\build\outputs\apk\debug\app-debug.apk`

3. **测试数据库迁移**
   - ✅ 应用能正常启动
   - ✅ 没有 Room 错误

4. **测试服务商管理**
   - 进入设置页面
   - 点击"管理 AI 服务商"
   - 添加一个服务商（例如：OpenAI）
   - 返回设置页面
   - ✅ 应该看到"当前服务商"卡片显示 OpenAI
   - 点击"当前服务商"卡片
   - ✅ 应该看到服务商选择对话框，列表中包含刚添加的 OpenAI

5. **测试服务商切换**
   - 添加第二个服务商（例如：DeepSeek）
   - 在服务商选择对话框中选择 DeepSeek
   - ✅ 应该看到"当前服务商"更新为 DeepSeek
   - ✅ 应该看到成功提示："AI 服务商已切换为 DeepSeek"

6. **测试空状态**
   - 删除所有服务商
   - 返回设置页面
   - ✅ 应该看到提示卡片："尚未配置 AI 服务商"

### 预期结果

- ✅ 应用正常启动，无崩溃
- ✅ 数据库迁移成功
- ✅ 可以添加服务商
- ✅ 服务商列表动态更新
- ✅ 可以切换服务商
- ✅ 空状态显示友好提示

---

## 修改文件清单

### 修复 1：数据库迁移
1. `app/src/main/java/com/empathy/ai/data/local/AppDatabase.kt`

### 修复 2：服务商选择
1. `app/src/main/java/com/empathy/ai/presentation/viewmodel/SettingsViewModel.kt`
2. `app/src/main/java/com/empathy/ai/presentation/ui/screen/settings/SettingsUiState.kt`
3. `app/src/main/java/com/empathy/ai/presentation/ui/screen/settings/SettingsScreen.kt`

**总计**：4 个文件

---

## 构建状态

✅ **编译成功**：`BUILD SUCCESSFUL in 2m 51s`
✅ **无编译错误**：只有一些弃用警告（不影响功能）
✅ **APK 已生成**：`app-debug.apk`

---

## 下一步

1. **用户测试**：按照测试计划进行手动测试
2. **反馈收集**：记录任何发现的问题
3. **决策**：
   - 如果测试通过 → 继续 Phase 2 开发或应用简化方案
   - 如果发现问题 → 优先修复问题

---

**修复人**：Kiro AI Assistant  
**修复日期**：2025-12-10  
**状态**：✅ 修复完成，等待用户测试确认
