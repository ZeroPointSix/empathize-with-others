# BUG-00026: LazyColumn Key 重复导致应用崩溃问题分析

> 创建日期: 2025-12-20
> 状态: ✅ 已修复
> 优先级: P0 - 严重
> 影响范围: 联系人详情页 - 概览标签页

---

## 1. 问题描述

### 1.1 现象

用户在联系人详情页浏览时，应用突然崩溃，日志显示：

```
java.lang.IllegalArgumentException: Key "1766224493675" was already used. 
If you are using LazyColumn/Row please make sure you provide a unique key for each item.
    at androidx.compose.ui.layout.LayoutNodeSubcompositionsState.subcompose(SubcomposeLayout.kt:453)
```

### 1.2 崩溃堆栈关键信息

```
at androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScopeImpl.measure-0kLqBqw
at androidx.compose.foundation.lazy.LazyListMeasuredItemProvider.getAndMeasure-0kLqBqw
at androidx.compose.foundation.lazy.LazyListMeasureKt.measureLazyList-x0Ok8Vo
at androidx.compose.foundation.lazy.LazyListState.onScroll$foundation_release
```

### 1.3 复现条件

1. 进入联系人详情页
2. 该联系人有多个在同一毫秒创建的标签（Fact）
3. 滚动页面触发 LazyColumn/LazyRow 重新测量

---

## 2. 根因分析

### 2.1 崩溃 Key 分析

```
Key = "1766224493675"
    = 1766224493675 毫秒
    ≈ 2025-12-20 02:14:53 UTC
```

这个 key 是一个**纯时间戳**，说明问题出在直接使用 `timestamp` 作为 key 的代码。

### 2.2 问题代码定位

**文件**: `app/src/main/java/com/empathy/ai/presentation/ui/screen/contact/overview/TopTagsSection.kt`

```kotlin
// 第 68-71 行 - 问题代码
LazyRow {
    items(
        items = tags.take(5),
        key = { it.timestamp }  // ⚠️ 直接使用 timestamp 作为 key
    ) { tag ->
        SolidTagChip(...)
    }
}
```

### 2.3 Compose LazyColumn/LazyRow Key 机制

```
LazyColumn/LazyRow 渲染流程：
┌─────────────────────────────────────────────────────────────┐
│  1. items() 提供数据列表和 key lambda                        │
│  2. LazyLayoutMeasureScope 根据可见区域计算需要渲染的项目      │
│  3. SubcomposeLayout 使用 key 作为唯一标识进行子组合          │
│  4. 滚动时，框架通过 key 判断哪些项目可以复用                  │
│  5. key 必须在整个列表中唯一，否则抛出 IllegalArgumentException │
└─────────────────────────────────────────────────────────────┘
```

### 2.4 根因总结

| 层级 | 问题 | 说明 |
|------|------|------|
| **框架层** | SubcomposeLayout 强制要求 key 唯一 | 这是 Compose 的设计约束 |
| **代码层** | `TopTagsSection` 使用 `timestamp` 作为 key | 同一毫秒创建的多个 Fact 会产生重复 key |
| **数据层** | `Fact` 模型缺少唯一标识符 | 只有 `timestamp` 可用，但不保证唯一 |

---

## 3. 影响范围

### 3.1 直接影响

- **TopTagsSection.kt**: 核心标签速览组件，使用 `LazyRow` 展示标签
- **OverviewTab.kt**: 概览标签页，包含 `TopTagsSection`

### 3.2 潜在风险点

经代码审查，以下位置也使用了类似的 key 策略，需要一并检查：

| 文件 | 当前 key 策略 | 风险等级 |
|------|--------------|----------|
| `TopTagsSection.kt` | `key = { it.timestamp }` | ⚠️ **高风险** |
| `TimelineView.kt` | `key = { it.id }` | ✅ 安全（使用组合 ID） |
| `ListView.kt` | `key = { it.id }` | ✅ 安全（使用组合 ID） |
| `PersonaTab.kt` | `key = "risk_section"` 等静态 key | ✅ 安全 |

---

## 4. 修复方案

### 4.1 方案 A：TopTagsSection 使用组合 Key（立即修复）

**修改文件**: `TopTagsSection.kt`

```kotlin
// 修复前
items(
    items = tags.take(5),
    key = { it.timestamp }
)

// 修复后
items(
    items = tags.take(5),
    key = { fact -> 
        "tag_${fact.timestamp}_${fact.key.hashCode().toUInt()}_${fact.value.hashCode().toUInt()}"
    }
)
```

**原理**:
- 即使 `timestamp` 相同，`key + value` 的组合也不同
- 使用 `toUInt()` 避免负数 hashCode 导致的字符串问题
- 添加 `tag_` 前缀避免与其他列表的 key 命名空间冲突

### 4.2 方案 C：为 Fact 模型添加 UUID（架构级修复）

**修改文件**: `app/src/main/java/com/empathy/ai/domain/model/Fact.kt`

```kotlin
// 修复前
data class Fact(
    val key: String,
    val value: String,
    val timestamp: Long,
    val source: FactSource
)

// 修复后
data class Fact(
    val id: String = java.util.UUID.randomUUID().toString(),
    val key: String,
    val value: String,
    val timestamp: Long,
    val source: FactSource
)
```

**原理**:
- UUID 保证全局唯一，无论何时创建
- 所有使用 Fact 的地方都可以直接使用 `fact.id` 作为 key
- 符合领域驱动设计的实体标识原则

**配套修改**:

1. **FactListConverter.kt** - 更新 JSON 序列化/反序列化
2. **TopTagsSection.kt** - 使用 `fact.id` 作为 key
3. **ContactDetailTabViewModel.kt** - 简化 TimelineItem.UserFact 的 ID 生成

---

## 5. 实施计划

### 5.1 任务清单

| 序号 | 任务 | 文件 | 优先级 |
|------|------|------|--------|
| T1 | 为 Fact 模型添加 id 字段 | `Fact.kt` | P0 |
| T2 | 更新 FactListConverter 支持 id 字段 | `FactListConverter.kt` | P0 |
| T3 | 修复 TopTagsSection 的 key 策略 | `TopTagsSection.kt` | P0 |
| T4 | 简化 buildTimelineItems 的 ID 生成 | `ContactDetailTabViewModel.kt` | P1 |
| T5 | 简化 addFactToStream 的 ID 生成 | `ContactDetailTabViewModel.kt` | P1 |
| T6 | 添加单元测试验证 key 唯一性 | `TopTagsSectionKeyTest.kt` | P1 |
| T7 | 更新 FactTest 测试用例 | `FactTest.kt` | P1 |

### 5.2 数据迁移考虑

由于 `Fact` 是嵌入在 `ContactProfile` 中通过 JSON 存储的，添加 `id` 字段需要考虑：

1. **新字段默认值**: 使用 `UUID.randomUUID().toString()` 作为默认值
2. **旧数据兼容**: Moshi 反序列化时，缺失的 `id` 字段会使用默认值
3. **无需数据库迁移**: `Fact` 不是独立的数据库表

---

## 6. 测试验证

### 6.1 单元测试

```kotlin
@Test
fun `TopTagsSection should generate unique keys for facts with same timestamp`() {
    // Given: 多个具有相同 timestamp 的 facts
    val sameTimestamp = System.currentTimeMillis()
    val facts = listOf(
        Fact(key = "兴趣", value = "音乐", timestamp = sameTimestamp, source = FactSource.MANUAL),
        Fact(key = "兴趣", value = "电影", timestamp = sameTimestamp, source = FactSource.MANUAL),
        Fact(key = "兴趣", value = "游戏", timestamp = sameTimestamp, source = FactSource.MANUAL)
    )

    // When: 生成 keys
    val keys = facts.map { fact -> 
        "tag_${fact.timestamp}_${fact.key.hashCode().toUInt()}_${fact.value.hashCode().toUInt()}"
    }

    // Then: 所有 keys 应该唯一
    assertEquals(keys.size, keys.toSet().size)
}

@Test
fun `Fact should have unique id by default`() {
    // Given & When
    val fact1 = Fact(key = "兴趣", value = "音乐", timestamp = 1L, source = FactSource.MANUAL)
    val fact2 = Fact(key = "兴趣", value = "音乐", timestamp = 1L, source = FactSource.MANUAL)

    // Then
    assertNotEquals(fact1.id, fact2.id)
}
```

### 6.2 手动测试

1. 快速连续添加多个标签（同一毫秒内）
2. 滚动联系人详情页
3. 切换标签页后返回概览页
4. 验证应用不崩溃

---

## 7. 相关文档

- [TDD-00004-联系人画像记忆系统UI架构设计.md](../TDD/TDD-00004-联系人画像记忆系统UI架构设计.md)
- [ContactDetailTabViewModelKeyTest.kt](../../../app/src/test/java/com/empathy/ai/presentation/viewmodel/ContactDetailTabViewModelKeyTest.kt) - 已有的 key 唯一性测试

---

## 8. 修复记录

| 日期 | 操作 | 执行者 |
|------|------|--------|
| 2025-12-20 | 创建问题分析文档 | Kiro |
| 2025-12-20 | 完成修复：为Fact模型添加id字段，更新TopTagsSection和ContactDetailTabViewModel | Kiro |

## 9. 修复详情

### 9.1 修改的文件

| 文件 | 修改内容 |
|------|----------|
| `Fact.kt` | 添加 `id: String = UUID.randomUUID().toString()` 字段 |
| `TopTagsSection.kt` | 将 `key = { it.timestamp }` 改为 `key = { it.id }` |
| `ContactDetailTabViewModel.kt` | `buildTimelineItems()` 使用 `fact.id` 生成唯一ID |
| `ContactDetailTabViewModel.kt` | `addFactToStream()` 使用 `fact.id` 生成唯一ID |
| `FactListConverter.kt` | 旧格式迁移时自动生成UUID |
| `FactTest.kt` | 添加id字段相关测试用例 |
| `FactListConverterTest.kt` | 更新测试用例支持id字段 |
| `ContactDetailTabViewModelKeyTest.kt` | 更新测试逻辑使用fact.id |
| `ProfileCard.kt` | 更新Preview中的Fact构造函数调用 |
| `FactItem.kt` | 更新Preview中的Fact构造函数调用 |
| `ContactListScreen.kt` | 更新Preview中的Fact构造函数调用 |
| `SummaryDetailDialog.kt` | 更新Preview中的Fact构造函数调用 |
| `ContextBuilderTest.kt` | 更新测试中的Fact构造函数调用 |

### 9.2 修复原理

1. **根本解决方案**：为 `Fact` 模型添加唯一 `id` 字段（UUID）
2. **自动生成**：每次创建 Fact 时自动生成唯一 UUID
3. **向后兼容**：旧数据反序列化时自动生成 UUID
4. **统一使用**：所有 LazyColumn/LazyRow 的 key 都使用 `fact.id`
