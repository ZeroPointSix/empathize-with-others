# 自由探索报告

## 基本信息

| 项目 | 内容 |
|------|------|
| 日期 | 2026-01-18 |
| 分支 | explore/free-20260118 |
| 状态 | 📖仅参考 |
| 探索者 | free-explorer |
| 决策日志 | DECISION_JOURNAL.md |

---

## 🔗 相关文档

- 决策日志: `DECISION_JOURNAL.md`
- 测试用例: `文档/开发文档/TE/TE-00076-联系人搜索历史测试用例.md`

---

## 探索主题

### 探索方向

联系人搜索目前只提供“过滤列表”的结果，缺少对搜索行为的延展。用户需要重复输入关键词，且清空搜索后没有可复用入口。本次探索尝试为联系人搜索补充“最近搜索”能力，同时尽量保持现有 UI 风格与 Clean Architecture 分层。

### 创意来源

1. 搜索体验只在当次输入中有效，缺少历史回溯。
2. 搜索模式空输入时只有提示文案，空间利用率低。
3. SharedPreferences 已用于其它偏好设置，适合作为轻量存储方案。

---

## 探索目标

- 新增最近搜索列表，支持快速回填。
- 搜索历史持久化，应用重启仍可使用。
- 限制历史条数并去重，保持列表清晰。
- 搜索历史可手动清空。
- 补齐单元测试与测试用例文档。

---

## 探索过程

### 尝试 1：UI 层静态展示（放弃）

仅做 UI 静态展示不具备真实价值，无法在用户再次进入搜索时复用关键词，且会误导用户以为有历史功能。因此放弃。

### 尝试 2：SharedPreferences 持久化（采用）

实现成本可控，无需数据库迁移，符合 Clean Architecture 的“偏好设置”场景。通过 Repository + UseCase 让 ViewModel 只处理业务事件。

### 尝试 3：搜索历史保存策略（采用）

使用去重 + 上限 8 条，只保存“命中结果”的关键词，避免无效搜索污染历史列表。

---

## 实验结果

| 实验 | 结果 | 价值评估 |
|------|------|----------|
| 搜索历史持久化 | ✅ 完成 | 高 |
| 搜索历史 UI 展示 | ✅ 完成 | 中 |
| 历史回填搜索 | ✅ 完成 | 中 |
| 清空历史入口 | ✅ 完成 | 中 |
| 单元测试补充 | ✅ 完成 | 中 |

---

## 代码变更

### 变更 1：新增搜索历史仓库与用例

**修改原因**：
1. 搜索历史属于偏好设置，需要独立的 Repository 管理。
2. UseCase 便于复用与测试，避免 ViewModel 直接依赖存储细节。
3. 与现有 Clean Architecture 分层保持一致。

**修改后核心代码**：

```kotlin
// domain/src/main/kotlin/.../ContactSearchHistoryRepository.kt
interface ContactSearchHistoryRepository {
    suspend fun getHistory(): Result<List<String>>
    suspend fun saveQuery(query: String): Result<List<String>>
    suspend fun clearHistory(): Result<Unit>
}
```

```kotlin
// domain/src/main/kotlin/.../SaveContactSearchQueryUseCase.kt
class SaveContactSearchQueryUseCase @Inject constructor(
    private val repository: ContactSearchHistoryRepository
) {
    suspend operator fun invoke(query: String): Result<List<String>> {
        return repository.saveQuery(query)
    }
}
```

### 变更 2：SharedPreferences 持久化实现

**修改原因**：
1. 轻量持久化，无需数据库迁移。
2. 支持去重与条数限制。
3. 便于在 data 层集中管理。

**修改后核心代码**：

```kotlin
// data/src/main/kotlin/.../ContactSearchHistoryPreferences.kt
override suspend fun saveQuery(query: String): Result<List<String>> {
    val normalized = query.trim()
    if (normalized.isBlank()) return Result.success(readHistory())
    val existing = readHistory()
    val updated = buildList {
        add(normalized)
        addAll(existing.filterNot { it.equals(normalized, ignoreCase = true) })
    }.take(MAX_HISTORY_SIZE)
    writeHistory(updated)
    return Result.success(updated)
}
```

### 变更 3：搜索历史 UI 与交互

**修改原因**：
1. 搜索模式空输入时提供可复用入口。
2. 点击历史关键词自动回填并触发搜索。
3. 提供“清空历史”入口。

**修改后核心代码**：

```kotlin
// presentation/.../ContactListScreen.kt
SearchModeContent(
    searchQuery = uiState.searchQuery,
    searchResults = uiState.searchResults,
    searchHistory = uiState.searchHistory,
    onQueryChange = { onEvent(ContactListUiEvent.UpdateSearchQuery(it)) },
    onSearchClose = { onEvent(ContactListUiEvent.CancelSearch) },
    onClearHistory = { onEvent(ContactListUiEvent.ClearSearchHistory) },
    onContactClick = { contactId ->
        onEvent(ContactListUiEvent.SaveSearchHistory)
        onNavigateToDetail(contactId)
    }
)
```

---

## 测试补充

### 单元测试

- `domain/src/test/.../GetContactSearchHistoryUseCaseTest.kt`
- `domain/src/test/.../SaveContactSearchQueryUseCaseTest.kt`
- `domain/src/test/.../ClearContactSearchHistoryUseCaseTest.kt`
- `presentation/src/test/.../ContactSearchHistoryFeatureTest.kt`

### 测试用例文档

- `文档/开发文档/TE/TE-00076-联系人搜索历史测试用例.md`

---

## 验证与结果

| 项目 | 结果 |
|------|------|
| `./gradlew.bat :domain:test --tests "*ContactSearchHistory*"` | ✅ 通过（有既有弃用/提示性告警） |
| `./gradlew.bat :presentation:test --tests "*ContactSearchHistoryFeatureTest*"` | ❌ 不支持 `--tests` 参数 |
| `./gradlew.bat :presentation:testDebugUnitTest --tests "*ContactSearchHistoryFeatureTest*"` | ✅ 通过（有既有弃用/提示性告警） |
| `./gradlew.bat installDebug "-Pandroid.injected.serial=127.0.0.1:7555"` | ✅ 安装成功，但实际安装到 `emulator-5556` 与 `V2324HA` 两台设备 |
| `adb -s 127.0.0.1:7555 install -r app\\build\\outputs\\apk\\debug\\app-debug.apk` | ✅ 安装成功（仅目标设备） |

### 版本更新说明

- 执行 `./gradlew.bat updateVersionAndIcon` 更新至 1.12.1（versionCode 11201）。
- 图标切换失败：缺少 `软件图标.png`，仅版本号与历史已更新。

---

## 风险与注意事项

1. 搜索历史仅保存“有结果”的关键词，若用户希望保留无结果关键词，需要产品确认。
2. 当前 UI 未对长关键词做省略处理，极长关键词可能影响列表布局。

---

## 结论

本次自由探索实现了联系人搜索历史能力，提升了搜索效率与可复用性。已按 Clean Architecture 分层完成 Repository/UseCase/UI 接入，并补充测试与测试用例文档。建议后续根据产品反馈决定是否保存“无结果”的搜索词。
