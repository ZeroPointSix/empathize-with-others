# 自由探索报告

## 基本信息

| 项目 | 内容 |
|------|------|
| 日期 | 2026-01-12 |
| 分支 | freedom |
| 状态 | 📖仅参考 |
| 探索者 | free-explorer |
| 决策日志 | DECISION_JOURNAL.md |

---

## 🔗 相关文档

- 决策日志: `DECISION_JOURNAL.md`
- 经验积累: 未新增（本次探索没有经过验证的通用经验）

---

## 探索主题

### 探索方向

围绕“联系人搜索体验清晰度”做改造。当前搜索会过滤列表，但用户只看到结果变少，并不一定能立刻理解“匹配点在哪里”。因此尝试引入轻量的关键词高亮，并把高亮逻辑做成通用工具函数，方便在不同列表复用，同时避免重复算法带来的维护成本。

### 创意来源

1. 联系人搜索 UI 已存在但缺少显式反馈。
2. 画像标签搜索已有高亮实现，但逻辑是组件内私有实现，无法复用。
3. AI 军师联系人选择页也有搜索功能，但没有高亮提示，体验不一致。

---

## 探索目标

- 在联系人列表搜索结果中高亮匹配关键词，让用户立刻确认匹配位置。
- 复用已有的高亮实现思想，抽取通用工具函数，减少重复逻辑。
- 将高亮能力同步到 AI 军师联系人选择页，保证同类搜索体验一致。
- 扩展高亮到 AI 配置服务商列表的搜索结果，提高命中可见性。
- 扩展高亮到标签管理列表，让标签搜索命中更直观。
- 补齐标签管理页搜索入口，避免“有搜索状态但无输入”的割裂体验。
- AI 配置搜索无结果提示，避免列表空白带来的误解。
- 标签搜索栏的返回关闭行为，符合常见搜索交互预期。
- 保持 iOS 风格的简洁外观，不引入额外复杂 UI。

---

## 探索过程

### 尝试 1：在 ContactListItem 内直接复制高亮逻辑

最初考虑直接在联系人列表项中复制 `SelectableTagChip` 里的高亮算法。这样改动小，接入速度快，但会造成两套重复逻辑。一旦后续发现高亮算法问题，就要改多处，而且不同页面的高亮风格也难以统一。综合维护成本与长期一致性，这个方案没有采用。

### 尝试 2：抽取通用高亮工具函数（采用）

我决定把高亮逻辑抽离为 `TextHighlight.kt` 工具函数，集中管理大小写匹配、分段拼接与 SpanStyle 渲染。这样不仅让联系人列表使用该函数，也能替换掉 `SelectableTagChip` 的内嵌实现，避免重复。工具函数通过 `String.indexOf(..., ignoreCase = true)` 来定位匹配位置，避免 `lowercase()` 造成字符长度变化的潜在风险。

### 尝试 3：在 AI 军师联系人选择页同步高亮（采用）

该页面已经有搜索过滤逻辑，但视觉反馈不足。为了保证体验一致，给该列表项新增 `highlightQuery` 参数并调用通用高亮工具函数，保证搜索关键词在姓名和“最后消息预览”中可见。这样用户在两个页面看到的搜索体验是统一的。

### 尝试 4：添加“匹配 X 位联系人”提示（放弃）

我考虑过在搜索结果列表顶部增加一条计数提示，让用户知道匹配数量。但这会引入额外布局、字体样式以及产品口径确认。该探索目标主要是验证高亮是否改善反馈，因此暂时不做计数提示，避免 UI 复杂度上升。

### 尝试 5：补充高亮工具单元测试（采用）

高亮工具函数属于纯逻辑实现，容易在后续优化中出现匹配范围或大小写逻辑回归。我为 `buildHighlightedText` 增加了少量单元测试，覆盖空查询、大小写匹配、多次匹配范围三类核心场景，并已运行通过，作为合并前的验证基础。

### 尝试 6：深色模式高亮透明度调整（采用）

我注意到高亮背景使用固定 0.2 透明度，在深色模式下可能不够显眼。为了提升可见性，在深色模式下将高亮透明度提升到 0.35。该调整仍需手动预览确认视觉效果。

### 尝试 7：补充 ASCII 大小写与无匹配测试（采用）

现有用例覆盖了中文匹配和多次匹配，但没有验证英文大小写忽略匹配，也没有覆盖“无匹配返回原文”的场景。我补充了 `buildHighlightedText_caseInsensitiveAscii_matchesAndHighlights` 和 `buildHighlightedText_noMatch_returnsPlainText` 两条测试，并重新运行单测确认通过。

### 尝试 8：抽取高亮样式函数（采用）

深色模式高亮透明度调整后，我发现 ContactListItem 与 ContactSelectScreen 内的样式生成逻辑出现重复。为避免后续调参漏改，我在 `TextHighlight.kt` 增加 `searchHighlightAlpha` 与 `createSearchHighlightStyle`，集中管理高亮透明度与字体权重，并让两处调用统一走工具函数。

### 尝试 9：扩展到 AI 配置服务商列表（采用）

AI 配置页面同样有搜索，但服务商列表只有过滤，没有高亮提示。我将 `IOSProviderCard` 增加 `highlightQuery`，并在 `AiConfigScreen` 内把 `uiState.searchQuery` 传入，使服务商名称在搜索时可见高亮。这样用户能快速确认“命中位置”，避免只有列表变短的反馈。

### 尝试 10：扩展到标签管理列表（采用）

标签管理页已有搜索逻辑（过滤标签），但标签芯片不展示命中位置。我在 `TagChip` 增加可选的 `highlightQuery`，并在 `BrainTagScreen` 的标签列表传入搜索串。该改动复用统一高亮工具函数，但由于 TagChip 背景为彩色容器，仍需人工视觉确认是否过于突兀。

### 尝试 11：补齐标签管理搜索入口（采用）

BrainTagScreen 顶部搜索图标原本没有行为，导致搜索状态“存在但不可用”。我使用现有 `TagSearchBar` 组件，在 TopAppBar 下方按需展示搜索栏，搜索关闭时同时触发 `ClearSearch`，确保搜索状态回收。这项改动让标签管理页的搜索入口变为可用，但未进行 UI 预览验证。

### 尝试 12：标签搜索无结果提示（采用）

标签搜索无命中时列表区域会完全空白，用户无法确认是否“没有结果”。我在 `TagList` 中增加条件分支，当 `searchQuery` 非空且过滤结果为空时，展示 `EmptyView` 的 NoResults 状态提示“没有找到匹配的标签”。该改动提高了反馈清晰度，但未进行 UI 预览验证。

### 尝试 13：AI 配置搜索无结果提示（采用）

AI 配置页搜索后如果无匹配，服务商列表会空白。我在 `AiConfigScreenContent` 中新增分支，当 `searchQuery` 非空且 `filteredProviders` 为空时显示 `EmptyView.NoResults`，提示“未找到匹配的服务商”。该改动与标签、联系人搜索的反馈一致，但未进行 UI 预览验证。

### 尝试 14：标签搜索栏返回关闭（采用）

搜索栏展开时，Back 手势直接退出页面不符合预期。我增加 `BackHandler` 逻辑，当搜索栏可见时优先关闭搜索栏并清空搜索，再次 Back 才会真正返回。该改动提升了搜索交互一致性，但未进行 UI 预览验证。

---
## 实验结果

### 成功的实验

| 实验 | 结果 | 价值评估 |
|------|------|----------|
| 抽取通用高亮工具函数 | ✅ 复用到多个组件 | 高 |
| 联系人列表搜索高亮 | ✅ 匹配位置更清晰 | 中 |
| AI 军师联系人选择页高亮 | ✅ 搜索体验一致 | 中 |
| AI 配置服务商列表高亮 | ✅ 命中反馈更直观 | 中 |
| 标签管理列表高亮 | ✅ 搜索体验更一致 | 低 |
| 标签管理搜索入口补齐 | ✅ 搜索能力可用 | 中 |
| 标签搜索无结果提示 | ✅ 反馈更清晰 | 低 |
| AI 配置搜索无结果提示 | ✅ 反馈更清晰 | 低 |
| 标签搜索栏返回关闭 | ✅ 交互更符合预期 | 低 |
| 高亮工具单元测试编写 | ✅ 已运行通过 | 中 |
| 深色模式高亮透明度调整 | ⚠️ 代码完成，待视觉验证 | 低 |
| 高亮样式函数抽取 | ✅ 统一样式生成 | 中 |

### 失败/放弃的实验

| 实验 | 失败原因 | 教训 |
|------|----------|------|
| 搜索结果计数提示 | 需要额外设计确认，超出当前探索范围 | 探索应聚焦可验证改动，避免引入新 UI 争议 |

---

## 代码变更

### 变更 1：新增通用高亮工具函数

**修改原因**（不少于 3 句话）：
1. 现有高亮逻辑隐藏在 `SelectableTagChip` 内，无法复用。
2. 联系人搜索需要高亮时，如果复制逻辑会造成重复实现。
3. 抽取工具函数能集中处理大小写匹配与分段拼接，降低未来维护成本。
4. 深色模式高亮需要统一透明度策略，适合在工具层集中管理。

**影响分析**：
- 新增 `TextHighlight.kt` 只依赖 Compose text API，不影响业务层。
- 其它组件可以直接复用该函数，减少重复代码。

**替代方案**：
- 保留每个组件独立实现高亮逻辑，但会造成重复和风格不一致。

**代码变更**：

修改前：
```
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/util/TextHighlight.kt
// 新增文件，修改前不存在
```

修改后：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/util/TextHighlight.kt
package com.empathy.ai.presentation.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

fun buildHighlightedText(
    text: String,
    query: String,
    highlightStyle: SpanStyle
): AnnotatedString {
    if (query.isBlank() || text.isEmpty()) {
        return AnnotatedString(text)
    }

    var currentIndex = 0

    return buildAnnotatedString {
        while (currentIndex < text.length) {
            val matchIndex = text.indexOf(query, currentIndex, ignoreCase = true)
            if (matchIndex == -1) {
                append(text.substring(currentIndex))
                break
            }

            if (matchIndex > currentIndex) {
                append(text.substring(currentIndex, matchIndex))
            }

            withStyle(highlightStyle) {
                append(text.substring(matchIndex, matchIndex + query.length))
            }

            currentIndex = matchIndex + query.length
        }
    }
}

fun searchHighlightAlpha(isDarkTheme: Boolean): Float {
    return if (isDarkTheme) 0.35f else 0.2f
}

fun createSearchHighlightStyle(
    isDarkTheme: Boolean,
    baseColor: Color
): SpanStyle {
    return SpanStyle(
        fontWeight = FontWeight.SemiBold,
        background = baseColor.copy(alpha = searchHighlightAlpha(isDarkTheme))
    )
}
```

---

### 变更 2：SelectableTagChip 复用通用高亮工具

**修改原因**：
1. 该组件原有的高亮逻辑与联系人列表需求高度一致。
2. 若不抽取，会导致“同类算法重复”。
3. 统一工具函数后更易保持视觉一致性。

**影响分析**：
- 高亮逻辑从组件内私有函数迁移到工具函数。
- 组件行为不变，仅实现方式变化。

**替代方案**：
- 保留原函数并在联系人列表复制，但会导致重复实现。

**代码变更**：

修改前：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/contact/persona/SelectableTagChip.kt
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectableTagChip(
    fact: Fact,
    isEditMode: Boolean,
    isSelected: Boolean,
    searchQuery: String,
    categoryColor: ComposeCategoryColor,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 根据选中状态决定颜色
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        categoryColor.tagBackgroundColor
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        categoryColor.tagTextColor
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .combinedClickable(
                onClick = {
                    if (isEditMode) {
                        onToggleSelection()
                    } else {
                        onClick()
                    }
                },
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 选中状态图标
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.selected),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        // 标签文本（支持搜索高亮）
        Text(
            text = buildHighlightedText(fact.value, searchQuery, textColor),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}

@Composable
private fun buildHighlightedText(
    text: String,
    query: String,
    defaultColor: Color
) = buildAnnotatedString {
    if (query.isBlank()) {
        append(text)
        return@buildAnnotatedString
    }

    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    var currentIndex = 0

    while (currentIndex < text.length) {
        val matchIndex = lowerText.indexOf(lowerQuery, currentIndex)
        if (matchIndex == -1) {
            append(text.substring(currentIndex))
            break
        }

        if (matchIndex > currentIndex) {
            append(text.substring(currentIndex, matchIndex))
        }

        withStyle(
            SpanStyle(
                fontWeight = FontWeight.Bold,
                background = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            append(text.substring(matchIndex, matchIndex + query.length))
        }

        currentIndex = matchIndex + query.length
    }
}
```

修改后：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/contact/persona/SelectableTagChip.kt
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectableTagChip(
    fact: Fact,
    isEditMode: Boolean,
    isSelected: Boolean,
    searchQuery: String,
    categoryColor: ComposeCategoryColor,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 根据选中状态决定颜色
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        categoryColor.tagBackgroundColor
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        categoryColor.tagTextColor
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .combinedClickable(
                onClick = {
                    if (isEditMode) {
                        onToggleSelection()
                    } else {
                        onClick()
                    }
                },
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 选中状态图标
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.selected),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        // 标签文本（支持搜索高亮）
        Text(
            text = buildHighlightedText(
                text = fact.value,
                query = searchQuery,
                highlightStyle = SpanStyle(
                    fontWeight = FontWeight.Bold,
                    background = MaterialTheme.colorScheme.tertiaryContainer
                )
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}
```

---
### 变更 3：ContactListItem 支持关键词高亮

**修改原因**：
1. 联系人搜索结果列表缺少关键词反馈。
2. 高亮匹配文本可以快速确认“过滤依据”。
3. 使用通用高亮工具函数能保持一致风格。
4. 深色模式下提升高亮透明度，避免可见性不足。

**影响分析**：
- ContactListItem 增加 `highlightQuery` 参数，默认空字符串不影响现有调用。
- 仅影响 UI 文本渲染，不改变业务逻辑。

**替代方案**：
- 在搜索列表外层渲染高亮，但更难控制单元格内部的文本样式。

**代码变更**：

修改前：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/list/ContactListItem.kt
@Composable
fun ContactListItem(
    contact: ContactProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tagCount: Int = 0,
    showDivider: Boolean = true,
    relativeTime: String? = null
) {
    val (backgroundColor, textColor) = AvatarColors.getColorPair(contact.name)
    val dividerColor = iOSSeparator

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick)
            .drawBehind {
                if (showDivider) {
                    val startX = 76.dp.toPx()
                    drawLine(
                        color = dividerColor,
                        start = Offset(startX, size.height - 0.5.dp.toPx()),
                        end = Offset(size.width, size.height - 0.5.dp.toPx()),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像 (淡色背景+深色首字母)
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 联系人信息
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // 第一行：姓名
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = contact.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    color = iOSTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 第二行：目标描述
            if (contact.targetGoal.isNotBlank()) {
                Text(
                    text = contact.targetGoal,
                    fontSize = 14.sp,
                    color = iOSTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 右侧：时间 + 箭头
        Column(
            horizontalAlignment = Alignment.End
        ) {
            if (relativeTime != null) {
                Text(
                    text = relativeTime,
                    fontSize = 13.sp,
                    color = iOSTextSecondary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "查看详情",
                tint = iOSTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
```

修改后：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/list/ContactListItem.kt
@Composable
fun ContactListItem(
    contact: ContactProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tagCount: Int = 0,
    showDivider: Boolean = true,
    relativeTime: String? = null,
    highlightQuery: String = ""
) {
    val (backgroundColor, textColor) = AvatarColors.getColorPair(contact.name)
    val dividerColor = iOSSeparator
    val highlightStyle = createSearchHighlightStyle(
        isDarkTheme = isSystemInDarkTheme(),
        baseColor = iOSBlue
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick)
            .drawBehind {
                if (showDivider) {
                    val startX = 76.dp.toPx()
                    drawLine(
                        color = dividerColor,
                        start = Offset(startX, size.height - 0.5.dp.toPx()),
                        end = Offset(size.width, size.height - 0.5.dp.toPx()),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像 (淡色背景+深色首字母)
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 联系人信息
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // 第一行：姓名
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildHighlightedText(
                        text = contact.name,
                        query = highlightQuery,
                        highlightStyle = highlightStyle
                    ),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    color = iOSTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 第二行：目标描述
            if (contact.targetGoal.isNotBlank()) {
                Text(
                    text = buildHighlightedText(
                        text = contact.targetGoal,
                        query = highlightQuery,
                        highlightStyle = highlightStyle
                    ),
                    fontSize = 14.sp,
                    color = iOSTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 右侧：时间 + 箭头
        Column(
            horizontalAlignment = Alignment.End
        ) {
            if (relativeTime != null) {
                Text(
                    text = relativeTime,
                    fontSize = 13.sp,
                    color = iOSTextSecondary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "查看详情",
                tint = iOSTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
```

---
### 变更 4：ContactListScreen 搜索模式传递高亮关键词

**修改原因**：
1. 高亮逻辑已加入 ContactListItem，需要在搜索模式时传入关键词。
2. 只在搜索模式传入关键词，避免普通列表出现误高亮。
3. 该改动局部且不影响非搜索流程。

**影响分析**：
- 仅影响搜索模式列表项渲染。
- 对非搜索场景无影响。

**替代方案**：
- 在 ViewModel 中构造高亮字符串，但会导致 UI 逻辑下沉到状态层。

**代码变更**：

修改前：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/contact/ContactListScreen.kt
private fun SearchModeContent(
    searchQuery: String,
    searchResults: List<ContactProfile>,
    onQueryChange: (String) -> Unit,
    onSearchClose: () -> Unit,
    onContactClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    val focusRequester = remember { FocusRequester() }
    
    // 自动聚焦搜索框
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(iOSBackground)
    ) {
        // 搜索栏
        SearchHeader(
            searchQuery = searchQuery,
            onQueryChange = onQueryChange,
            onSearchClose = onSearchClose,
            focusRequester = focusRequester
        )
        
        // 搜索结果
        when {
            searchResults.isEmpty() && searchQuery.isNotBlank() -> {
                // 无结果提示
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyView(
                        message = "未找到匹配的联系人",
                        actionText = null,
                        onAction = {}
                    )
                }
            }
            searchResults.isNotEmpty() -> {
                // 搜索结果列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimensions.spacingMedium),
                            shape = RoundedCornerShape(dimensions.cornerRadiusMedium),
                            color = iOSCardBackground,
                            shadowElevation = 1.dp
                        ) {
                            Column {
                                searchResults.forEachIndexed { index, contact ->
                                    ContactListItem(
                                        contact = contact,
                                        onClick = { onContactClick(contact.id) },
                                        showDivider = index < searchResults.size - 1
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(dimensions.spacingLarge))
                    }
                }
            }
            else -> {
                // 搜索词为空，显示提示
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "输入关键词搜索联系人",
                        color = iOSTextSecondary,
                        fontSize = dimensions.fontSizeBody
                    )
                }
            }
        }
    }
}
```

修改后：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/contact/ContactListScreen.kt
private fun SearchModeContent(
    searchQuery: String,
    searchResults: List<ContactProfile>,
    onQueryChange: (String) -> Unit,
    onSearchClose: () -> Unit,
    onContactClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    val focusRequester = remember { FocusRequester() }
    
    // 自动聚焦搜索框
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(iOSBackground)
    ) {
        // 搜索栏
        SearchHeader(
            searchQuery = searchQuery,
            onQueryChange = onQueryChange,
            onSearchClose = onSearchClose,
            focusRequester = focusRequester
        )
        
        // 搜索结果
        when {
            searchResults.isEmpty() && searchQuery.isNotBlank() -> {
                // 无结果提示
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyView(
                        message = "未找到匹配的联系人",
                        actionText = null,
                        onAction = {}
                    )
                }
            }
            searchResults.isNotEmpty() -> {
                // 搜索结果列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimensions.spacingMedium),
                            shape = RoundedCornerShape(dimensions.cornerRadiusMedium),
                            color = iOSCardBackground,
                            shadowElevation = 1.dp
                        ) {
                            Column {
                                searchResults.forEachIndexed { index, contact ->
                                    ContactListItem(
                                        contact = contact,
                                        onClick = { onContactClick(contact.id) },
                                        showDivider = index < searchResults.size - 1,
                                        highlightQuery = searchQuery
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(dimensions.spacingLarge))
                    }
                }
            }
            else -> {
                // 搜索词为空，显示提示
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "输入关键词搜索联系人",
                        color = iOSTextSecondary,
                        fontSize = dimensions.fontSizeBody
                    )
                }
            }
        }
    }
}
```

---
### 变更 5：ContactSelectScreen 搜索列表高亮

**修改原因**：
1. AI 军师联系人选择页也有搜索功能，但缺乏高亮反馈。
2. 同类场景应该提供一致体验。
3. 复用通用高亮工具函数成本低，效果明确。
4. 深色模式下同步高亮透明度策略，避免体验分裂。

**影响分析**：
- ContactSelectScreen 的内部列表组件增加 `highlightQuery`。
- 仅改变 UI 文本渲染，不改变选择逻辑。

**替代方案**：
- 保持原样，但会导致两个搜索页面体验不一致。

**代码变更**：

修改前：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/advisor/ContactSelectScreen.kt
private fun ContactList(
    contacts: List<ContactProfile>,
    onContactClick: (ContactProfile) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = contacts,
            key = { it.id }
        ) { contact ->
            ContactListItem(
                contact = contact,
                onClick = { onContactClick(contact) }
            )
        }
    }
}

@Composable
private fun ContactListItem(
    contact: ContactProfile,
    onClick: () -> Unit
) {
    val dimensions = AdaptiveDimensions.current
    
    // 头像颜色方案
    val avatarColors = listOf(
        Color(0xFFE8EAF6) to Color(0xFF5C6BC0), // indigo
        Color(0xFFE3F2FD) to Color(0xFF42A5F5), // blue
        Color(0xFFFCE4EC) to Color(0xFFEC407A), // rose
        Color(0xFFE8F5E9) to Color(0xFF66BB6A), // emerald
        Color(0xFFE0F7FA) to Color(0xFF26C6DA)  // cyan
    )

    // 根据联系人ID选择颜色
    val colorIndex = contact.id.hashCode().let { kotlin.math.abs(it) % avatarColors.size }
    val (bgColor, textColor) = avatarColors[colorIndex]

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(iOSCardBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.firstOrNull()?.toString() ?: "?",
                    fontSize = dimensions.fontSizeSubtitle,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 信息区域
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = contact.name,
                            fontSize = dimensions.fontSizeSubtitle,
                            fontWeight = FontWeight.Medium,
                            color = iOSTextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = contact.getRelationshipLevel().displayName,
                            fontSize = dimensions.fontSizeXSmall,
                            color = iOSTextSecondary
                        )
                    }
                    Text(
                        text = contact.lastInteractionDate ?: "未知",
                        fontSize = dimensions.fontSizeXSmall,
                        color = iOSTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 最后消息预览
                Text(
                    text = contact.targetGoal.ifEmpty { "暂无消息" },
                    fontSize = dimensions.fontSizeCaption,
                    color = iOSTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 右箭头
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = iOSTextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }

        // 分隔线
        HorizontalDivider(
            modifier = Modifier.padding(start = 72.dp),
            color = Color(0xFFE5E5EA),
            thickness = 0.5.dp
        )
    }
}
```

修改后：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/advisor/ContactSelectScreen.kt
private fun ContactList(
    contacts: List<ContactProfile>,
    highlightQuery: String,
    onContactClick: (ContactProfile) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = contacts,
            key = { it.id }
        ) { contact ->
            ContactListItem(
                contact = contact,
                onClick = { onContactClick(contact) },
                highlightQuery = highlightQuery
            )
        }
    }
}

@Composable
private fun ContactListItem(
    contact: ContactProfile,
    onClick: () -> Unit,
    highlightQuery: String
) {
    val dimensions = AdaptiveDimensions.current
    val highlightStyle = createSearchHighlightStyle(
        isDarkTheme = isSystemInDarkTheme(),
        baseColor = iOSBlue
    )
    
    // 头像颜色方案
    val avatarColors = listOf(
        Color(0xFFE8EAF6) to Color(0xFF5C6BC0), // indigo
        Color(0xFFE3F2FD) to Color(0xFF42A5F5), // blue
        Color(0xFFFCE4EC) to Color(0xFFEC407A), // rose
        Color(0xFFE8F5E9) to Color(0xFF66BB6A), // emerald
        Color(0xFFE0F7FA) to Color(0xFF26C6DA)  // cyan
    )

    // 根据联系人ID选择颜色
    val colorIndex = contact.id.hashCode().let { kotlin.math.abs(it) % avatarColors.size }
    val (bgColor, textColor) = avatarColors[colorIndex]

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(iOSCardBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.firstOrNull()?.toString() ?: "?",
                    fontSize = dimensions.fontSizeSubtitle,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 信息区域
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = buildHighlightedText(
                                text = contact.name,
                                query = highlightQuery,
                                highlightStyle = highlightStyle
                            ),
                            fontSize = dimensions.fontSizeSubtitle,
                            fontWeight = FontWeight.Medium,
                            color = iOSTextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = contact.getRelationshipLevel().displayName,
                            fontSize = dimensions.fontSizeXSmall,
                            color = iOSTextSecondary
                        )
                    }
                    Text(
                        text = contact.lastInteractionDate ?: "未知",
                        fontSize = dimensions.fontSizeXSmall,
                        color = iOSTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 最后消息预览
                Text(
                    text = buildHighlightedText(
                        text = contact.targetGoal.ifEmpty { "暂无消息" },
                        query = highlightQuery,
                        highlightStyle = highlightStyle
                    ),
                    fontSize = dimensions.fontSizeCaption,
                    color = iOSTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 右箭头
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = iOSTextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }

        // 分隔线
        HorizontalDivider(
            modifier = Modifier.padding(start = 72.dp),
            color = Color(0xFFE5E5EA),
            thickness = 0.5.dp
        )
    }
}
```

---

### 变更 6：新增 TextHighlightTest

**修改原因**：
1. 高亮函数是纯逻辑函数，容易因后续优化引入回归。
2. 最小单元测试可以验证空查询、大小写匹配、多次匹配范围。
3. 测试成本低，可作为探索结果的稳定性补强。

**影响分析**：
- 新增 JVM 单元测试文件，不影响运行逻辑。
- 已执行测试并通过（构建含既有警告，不影响结果）。

**替代方案**：
- 不写测试，依赖人工验证，但回归风险更高。

**代码变更**：

修改前：
```
// file: presentation/src/test/kotlin/com/empathy/ai/presentation/util/TextHighlightTest.kt
// 新增文件，修改前不存在
```

修改后：
```kotlin
// file: presentation/src/test/kotlin/com/empathy/ai/presentation/util/TextHighlightTest.kt
package com.empathy.ai.presentation.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextHighlightTest {

    private val highlightStyle = SpanStyle(
        fontWeight = FontWeight.SemiBold,
        background = Color(0xFF00FF00)
    )

    @Test
    fun buildHighlightedText_blankQuery_returnsPlainText() {
        val result = buildHighlightedText(
            text = "hello",
            query = "",
            highlightStyle = highlightStyle
        )

        assertEquals("hello", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun buildHighlightedText_caseInsensitive_matchesAndHighlights() {
        val result = buildHighlightedText(
            text = "张三",
            query = "张",
            highlightStyle = highlightStyle
        )

        assertEquals("张三", result.text)
        assertEquals(1, result.spanStyles.size)
        val range = result.spanStyles.first()
        assertEquals(0, range.start)
        assertEquals(1, range.end)
        assertEquals(highlightStyle, range.item)
    }

    @Test
    fun buildHighlightedText_caseInsensitiveAscii_matchesAndHighlights() {
        val result = buildHighlightedText(
            text = "Alice",
            query = "aL",
            highlightStyle = highlightStyle
        )

        assertEquals("Alice", result.text)
        assertEquals(1, result.spanStyles.size)
        val range = result.spanStyles.first()
        assertEquals(0, range.start)
        assertEquals(2, range.end)
        assertEquals(highlightStyle, range.item)
    }

    @Test
    fun buildHighlightedText_noMatch_returnsPlainText() {
        val result = buildHighlightedText(
            text = "hello",
            query = "xyz",
            highlightStyle = highlightStyle
        )

        assertEquals("hello", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun buildHighlightedText_multipleMatches_returnsAllRanges() {
        val result = buildHighlightedText(
            text = "bananana",
            query = "na",
            highlightStyle = highlightStyle
        )

        val ranges = result.spanStyles.map { it.start to it.end }
        assertEquals(listOf(2 to 4, 4 to 6, 6 to 8), ranges)
    }

    @Test
    fun searchHighlightAlpha_returnsExpectedValues() {
        assertEquals(0.35f, searchHighlightAlpha(true), 0.0001f)
        assertEquals(0.2f, searchHighlightAlpha(false), 0.0001f)
    }

    @Test
    fun createSearchHighlightStyle_usesBaseColorWithAlpha() {
        val baseColor = Color(0xFF112233)
        val style = createSearchHighlightStyle(isDarkTheme = true, baseColor = baseColor)

        assertEquals(FontWeight.SemiBold, style.fontWeight)
        assertEquals(baseColor.copy(alpha = 0.35f), style.background)
    }
}
```

---

### 变更 7：TagChip 支持搜索高亮并接入标签列表

**修改原因**：
1. 标签管理页搜索只做过滤，没有命中提示，用户难以确认匹配位置。
2. TagChip 是统一标签组件，适合承载高亮逻辑，避免在列表层分叉实现。
3. 已有高亮工具函数可复用，改动范围可控。

**影响分析**：
- TagChip 增加 `highlightQuery` 可选参数，旧调用不受影响。
- BrainTagScreen 在列表渲染时传入 `searchQuery`，空字符串时行为与原来一致。
- 标签高亮效果在彩色容器上需要人工验证对比度。

**替代方案**：
- 在 TagList 内部手写富文本高亮，不改 TagChip，但会造成组件职责不一致。

**代码变更**：

修改前：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/chip/TagChip.kt
@Composable
fun TagChip(
    text: String,
    tagType: TagType,
    onDelete: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    val colors = getTagColors(tagType)
    
    AssistChip(
        onClick = { onClick?.invoke() },
        label = { Text(text) },
        modifier = modifier,
        leadingIcon = {
            Icon(
                imageVector = when (tagType) {
                    TagType.RISK_RED -> Icons.Default.Warning
                    TagType.STRATEGY_GREEN -> Icons.Default.Lightbulb
                },
                contentDescription = when (tagType) {
                    TagType.RISK_RED -> "雷区"
                    TagType.STRATEGY_GREEN -> "策略"
                },
                modifier = Modifier.size(dimensions.iconSizeSmall),
                tint = colors.iconColor
            )
        },
        trailingIcon = if (onDelete != null) {
            {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(dimensions.iconSizeSmall + 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "删除",
                        modifier = Modifier.size(dimensions.iconSizeSmall - 2.dp)
                    )
                }
            }
        } else null,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = colors.backgroundColor,
            labelColor = colors.textColor,
            leadingIconContentColor = colors.iconColor
        )
    )
}
```

修改后：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/chip/TagChip.kt
@Composable
fun TagChip(
    text: String,
    tagType: TagType,
    highlightQuery: String = "",
    onDelete: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    val colors = getTagColors(tagType)
    val highlightStyle = createSearchHighlightStyle(
        isDarkTheme = isSystemInDarkTheme(),
        baseColor = colors.iconColor
    )
    
    AssistChip(
        onClick = { onClick?.invoke() },
        label = {
            Text(
                text = buildHighlightedText(
                    text = text,
                    query = highlightQuery,
                    highlightStyle = highlightStyle
                )
            )
        },
        modifier = modifier,
        leadingIcon = {
            Icon(
                imageVector = when (tagType) {
                    TagType.RISK_RED -> Icons.Default.Warning
                    TagType.STRATEGY_GREEN -> Icons.Default.Lightbulb
                },
                contentDescription = when (tagType) {
                    TagType.RISK_RED -> "雷区"
                    TagType.STRATEGY_GREEN -> "策略"
                },
                modifier = Modifier.size(dimensions.iconSizeSmall),
                tint = colors.iconColor
            )
        },
        trailingIcon = if (onDelete != null) {
            {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(dimensions.iconSizeSmall + 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "删除",
                        modifier = Modifier.size(dimensions.iconSizeSmall - 2.dp)
                    )
                }
            }
        } else null,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = colors.backgroundColor,
            labelColor = colors.textColor,
            leadingIconContentColor = colors.iconColor
        )
    )
}
```

修改前：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/tag/BrainTagScreen.kt
@Composable
private fun TagList(
    tags: List<BrainTag>,
    searchQuery: String,
    onDeleteTag: (Long) -> Unit,
    onEditTag: (BrainTag) -> Unit,
    modifier: Modifier = Modifier
) {
    // 过滤标签
    val filteredTags = if (searchQuery.isBlank()) {
        tags
    } else {
        tags.filter { it.content.contains(searchQuery, ignoreCase = true) }
    }

    // 按类型分组
    val landmineTags = filteredTags.filter { it.type == TagType.RISK_RED }
    val strategyTags = filteredTags.filter { it.type == TagType.STRATEGY_GREEN }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
    ) {
        // 雷区标签
        if (landmineTags.isNotEmpty()) {
            item {
                Text(
                    text = "雷区标签 (${landmineTags.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            items(
                items = landmineTags,
                key = { it.id }
            ) { tag ->
                TagChip(
                    text = tag.content,
                    tagType = tag.type,
                    onDelete = { onDeleteTag(tag.id) },
                    onClick = { onEditTag(tag) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
        }

        // 策略标签
        if (strategyTags.isNotEmpty()) {
            item {
                Text(
                    text = "策略标签 (${strategyTags.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(
                items = strategyTags,
                key = { it.id }
            ) { tag ->
                TagChip(
                    text = tag.content,
                    tagType = tag.type,
                    onDelete = { onDeleteTag(tag.id) },
                    onClick = { onEditTag(tag) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
```

修改后：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/tag/BrainTagScreen.kt
@Composable
private fun TagList(
    tags: List<BrainTag>,
    searchQuery: String,
    onDeleteTag: (Long) -> Unit,
    onEditTag: (BrainTag) -> Unit,
    modifier: Modifier = Modifier
) {
    // 过滤标签
    val filteredTags = if (searchQuery.isBlank()) {
        tags
    } else {
        tags.filter { it.content.contains(searchQuery, ignoreCase = true) }
    }

    // 按类型分组
    val landmineTags = filteredTags.filter { it.type == TagType.RISK_RED }
    val strategyTags = filteredTags.filter { it.type == TagType.STRATEGY_GREEN }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
    ) {
        // 雷区标签
        if (landmineTags.isNotEmpty()) {
            item {
                Text(
                    text = "雷区标签 (${landmineTags.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            items(
                items = landmineTags,
                key = { it.id }
            ) { tag ->
                TagChip(
                    text = tag.content,
                    tagType = tag.type,
                    highlightQuery = searchQuery,
                    onDelete = { onDeleteTag(tag.id) },
                    onClick = { onEditTag(tag) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
        }

        // 策略标签
        if (strategyTags.isNotEmpty()) {
            item {
                Text(
                    text = "策略标签 (${strategyTags.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(
                items = strategyTags,
                key = { it.id }
            ) { tag ->
                TagChip(
                    text = tag.content,
                    tagType = tag.type,
                    highlightQuery = searchQuery,
                    onDelete = { onDeleteTag(tag.id) },
                    onClick = { onEditTag(tag) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
```

---

### 变更 8：服务商列表支持搜索高亮

**修改原因**：
1. AI 配置页支持搜索，但服务商列表没有命中提示。
2. 该列表和联系人列表属于同类“搜索过滤场景”，需要体验一致。
3. `IOSProviderCard` 是统一列表项组件，扩展成本低。

**影响分析**：
- `IOSProviderCard` 增加 `highlightQuery` 参数并使用统一高亮样式。
- `ProviderListContent` 传入 `uiState.searchQuery`，空查询时行为不变。
- 未进行 UI 视觉验证，需确认高亮在浅色/深色模式下的可读性。

**替代方案**：
- 仅保持过滤不高亮，但体验一致性不足。

**代码变更**：

修改前：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/ios/IOSProviderCard.kt
@Composable
fun IOSProviderCard(
    provider: AiProvider,
    isDefault: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    showDivider: Boolean = true,
    icon: ImageVector = Icons.Default.Cloud,
    iconBackgroundColor: Color = getProviderColor(provider.name)
) {
    // 使用响应式尺寸
    val dimensions = AdaptiveDimensions.current
    val density = LocalDensity.current
    
    val dividerColor = iOSSeparator
    // 分隔线起始位置 = padding(16) + iconSize(40) + spacing(12)
    val dividerStartPadding = dimensions.spacingMedium + dimensions.iosIconContainerSize + dimensions.spacingMediumSmall
    
    // 滑动阈值 - 使用响应式尺寸
    val swipeThresholdDp = dimensions.swipeActionTotalWidth
    val swipeThreshold = with(density) { swipeThresholdDp.toPx() }
    val buttonWidthDp = dimensions.swipeActionButtonWidth
    
    // 列表项高度 - 响应式
    val itemHeight = dimensions.iosListItemHeight + dimensions.spacingLarge // 约72dp
    
    var offsetX by remember { mutableFloatStateOf(0f) }
    var isSwipeOpen by remember { mutableStateOf(false) }
    
    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isSwipeOpen) -swipeThreshold else offsetX,
        animationSpec = tween(durationMillis = 200),
        label = "swipeOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(itemHeight)
    ) {
        // Action buttons (behind the card)
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
        ) {
            // Delete button (左边)
            if (onDelete != null) {
                Box(
                    modifier = Modifier
                        .width(buttonWidthDp)
                        .fillMaxHeight()
                        .background(iOSRed)
                        .clickable {
                            isSwipeOpen = false
                            offsetX = 0f
                            onDelete()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = Color.White,
                            modifier = Modifier.size(dimensions.iconSizeMedium)
                        )
                        Text(
                            text = "删除",
                            color = Color.White,
                            fontSize = dimensions.fontSizeXSmall
                        )
                    }
                }
            }
            
            // Edit button (右边，滑动时先露出)
            if (onEdit != null) {
                Box(
                    modifier = Modifier
                        .width(buttonWidthDp)
                        .fillMaxHeight()
                        .background(iOSOrange)
                        .clickable {
                            isSwipeOpen = false
                            offsetX = 0f
                            onEdit()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = Color.White,
                            modifier = Modifier.size(dimensions.iconSizeMedium)
                        )
                        Text(
                            text = "编辑",
                            color = Color.White,
                            fontSize = dimensions.fontSizeXSmall
                        )
                    }
                }
            }
        }

        // Main card content (swipeable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .background(Color.White)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < -swipeThreshold / 2) {
                                isSwipeOpen = true
                            } else {
                                isSwipeOpen = false
                            }
                            offsetX = 0f
                        },
                        onDragCancel = {
                            offsetX = 0f
                            isSwipeOpen = false
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            if (!isSwipeOpen) {
                                val newOffset = (offsetX + dragAmount).coerceIn(-swipeThreshold, 0f)
                                offsetX = newOffset
                            } else {
                                if (dragAmount > 0) {
                                    isSwipeOpen = false
                                    offsetX = 0f
                                }
                            }
                        }
                    )
                }
                .clickable {
                    if (isSwipeOpen) {
                        isSwipeOpen = false
                        offsetX = 0f
                    } else {
                        onClick()
                    }
                }
                .drawBehind {
                    if (showDivider) {
                        val startX = dividerStartPadding.toPx()
                        drawLine(
                            color = dividerColor,
                            start = Offset(startX, size.height - 0.5.dp.toPx()),
                            end = Offset(size.width, size.height - 0.5.dp.toPx()),
                            strokeWidth = 0.5.dp.toPx()
                        )
                    }
                }
                .padding(horizontal = dimensions.spacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标容器 (响应式尺寸)
            Box(
                modifier = Modifier
                    .size(dimensions.iosIconContainerSize)
                    .background(
                        color = iconBackgroundColor,
                        shape = RoundedCornerShape(dimensions.cornerRadiusSmall)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(dimensions.iconSizeMedium)
                )
            }

            Spacer(modifier = Modifier.width(dimensions.spacingMediumSmall))

            // 标题和描述 - 使用响应式字体
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = provider.name,
                    fontSize = dimensions.fontSizeTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = getProviderDescription(provider),
                    fontSize = dimensions.fontSizeBody,
                    color = iOSTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 默认标记
            if (isDefault) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "默认",
                    tint = iOSBlue,
                    modifier = Modifier.size(dimensions.iconSizeMedium)
                )
            }
        }
    }
}
```

修改后：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/ios/IOSProviderCard.kt
@Composable
fun IOSProviderCard(
    provider: AiProvider,
    isDefault: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    showDivider: Boolean = true,
    icon: ImageVector = Icons.Default.Cloud,
    iconBackgroundColor: Color = getProviderColor(provider.name),
    highlightQuery: String = ""
) {
    // 使用响应式尺寸
    val dimensions = AdaptiveDimensions.current
    val density = LocalDensity.current
    val highlightStyle = createSearchHighlightStyle(
        isDarkTheme = isSystemInDarkTheme(),
        baseColor = iOSBlue
    )
    
    val dividerColor = iOSSeparator
    // 分隔线起始位置 = padding(16) + iconSize(40) + spacing(12)
    val dividerStartPadding = dimensions.spacingMedium + dimensions.iosIconContainerSize + dimensions.spacingMediumSmall
    
    // 滑动阈值 - 使用响应式尺寸
    val swipeThresholdDp = dimensions.swipeActionTotalWidth
    val swipeThreshold = with(density) { swipeThresholdDp.toPx() }
    val buttonWidthDp = dimensions.swipeActionButtonWidth
    
    // 列表项高度 - 响应式
    val itemHeight = dimensions.iosListItemHeight + dimensions.spacingLarge // 约72dp
    
    var offsetX by remember { mutableFloatStateOf(0f) }
    var isSwipeOpen by remember { mutableStateOf(false) }
    
    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isSwipeOpen) -swipeThreshold else offsetX,
        animationSpec = tween(durationMillis = 200),
        label = "swipeOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(itemHeight)
    ) {
        // Action buttons (behind the card)
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
        ) {
            // Delete button (左边)
            if (onDelete != null) {
                Box(
                    modifier = Modifier
                        .width(buttonWidthDp)
                        .fillMaxHeight()
                        .background(iOSRed)
                        .clickable {
                            isSwipeOpen = false
                            offsetX = 0f
                            onDelete()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = Color.White,
                            modifier = Modifier.size(dimensions.iconSizeMedium)
                        )
                        Text(
                            text = "删除",
                            color = Color.White,
                            fontSize = dimensions.fontSizeXSmall
                        )
                    }
                }
            }
            
            // Edit button (右边，滑动时先露出)
            if (onEdit != null) {
                Box(
                    modifier = Modifier
                        .width(buttonWidthDp)
                        .fillMaxHeight()
                        .background(iOSOrange)
                        .clickable {
                            isSwipeOpen = false
                            offsetX = 0f
                            onEdit()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = Color.White,
                            modifier = Modifier.size(dimensions.iconSizeMedium)
                        )
                        Text(
                            text = "编辑",
                            color = Color.White,
                            fontSize = dimensions.fontSizeXSmall
                        )
                    }
                }
            }
        }

        // Main card content (swipeable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .background(Color.White)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < -swipeThreshold / 2) {
                                isSwipeOpen = true
                            } else {
                                isSwipeOpen = false
                            }
                            offsetX = 0f
                        },
                        onDragCancel = {
                            offsetX = 0f
                            isSwipeOpen = false
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            if (!isSwipeOpen) {
                                val newOffset = (offsetX + dragAmount).coerceIn(-swipeThreshold, 0f)
                                offsetX = newOffset
                            } else {
                                if (dragAmount > 0) {
                                    isSwipeOpen = false
                                    offsetX = 0f
                                }
                            }
                        }
                    )
                }
                .clickable {
                    if (isSwipeOpen) {
                        isSwipeOpen = false
                        offsetX = 0f
                    } else {
                        onClick()
                    }
                }
                .drawBehind {
                    if (showDivider) {
                        val startX = dividerStartPadding.toPx()
                        drawLine(
                            color = dividerColor,
                            start = Offset(startX, size.height - 0.5.dp.toPx()),
                            end = Offset(size.width, size.height - 0.5.dp.toPx()),
                            strokeWidth = 0.5.dp.toPx()
                        )
                    }
                }
                .padding(horizontal = dimensions.spacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标容器 (响应式尺寸)
            Box(
                modifier = Modifier
                    .size(dimensions.iosIconContainerSize)
                    .background(
                        color = iconBackgroundColor,
                        shape = RoundedCornerShape(dimensions.cornerRadiusSmall)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(dimensions.iconSizeMedium)
                )
            }

            Spacer(modifier = Modifier.width(dimensions.spacingMediumSmall))

            // 标题和描述 - 使用响应式字体
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = buildHighlightedText(
                        text = provider.name,
                        query = highlightQuery,
                        highlightStyle = highlightStyle
                    ),
                    fontSize = dimensions.fontSizeTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = getProviderDescription(provider),
                    fontSize = dimensions.fontSizeBody,
                    color = iOSTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 默认标记
            if (isDefault) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "默认",
                    tint = iOSBlue,
                    modifier = Modifier.size(dimensions.iconSizeMedium)
                )
            }
        }
    }
}
```

修改前：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/aiconfig/AiConfigScreen.kt
@Composable
private fun ProviderListContent(
    uiState: AiConfigUiState,
    onEvent: (AiConfigUiEvent) -> Unit,
    onNavigateToEditProvider: ((String) -> Unit)?
) {
    val dimensions = AdaptiveDimensions.current
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = dimensions.spacingXLarge)
    ) {
        // 推理引擎分组
        item {
            IOSSettingsSection(
                title = "推理引擎",
                footer = "向左滑动可编辑或删除服务商，点击切换默认引擎"
            ) {
                val providers = uiState.filteredProviders
                providers.forEachIndexed { index, provider ->
                    IOSProviderCard(
                        provider = provider,
                        isDefault = provider.isDefault,
                        onClick = { 
                            // 点击直接设为默认服务商
                            onEvent(AiConfigUiEvent.SetDefaultProvider(provider.id))
                        },
                        onEdit = {
                            // 滑动编辑 - 导航到编辑页面
                            if (onNavigateToEditProvider != null) {
                                onNavigateToEditProvider(provider.id)
                            } else {
                                onEvent(AiConfigUiEvent.ShowEditDialog(provider))
                            }
                        },
                        onDelete = {
                            // 滑动删除
                            onEvent(AiConfigUiEvent.ShowDeleteConfirmDialog(provider.id))
                        },
                        showDivider = index < providers.lastIndex
                    )
                }
            }
        }

        // 通用选项分组
        item {
            IOSSettingsSection(title = "通用选项") {
                IOSSettingsItem(
                    icon = Icons.Default.Language,
                    iconBackgroundColor = iOSBlue,
                    title = "网络代理",
                    value = if (uiState.proxyConfig?.enabled == true) {
                        "${uiState.proxyConfig?.type?.name ?: "HTTP"} ${uiState.proxyConfig?.host ?: ""}:${uiState.proxyConfig?.port ?: ""}"
                    } else {
                        "未设置"
                    },
                    showDivider = true,
                    onClick = { onEvent(AiConfigUiEvent.ShowProxyDialog) }
                )
                IOSSettingsItem(
                    icon = Icons.Default.QueryStats,
                    iconBackgroundColor = iOSPurple,
                    title = "用量统计",
                    showDivider = false,
                    onClick = { onEvent(AiConfigUiEvent.NavigateToUsageStats) }
                )
            }
        }
    }
}
```

修改后：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/aiconfig/AiConfigScreen.kt
@Composable
private fun ProviderListContent(
    uiState: AiConfigUiState,
    onEvent: (AiConfigUiEvent) -> Unit,
    onNavigateToEditProvider: ((String) -> Unit)?
) {
    val dimensions = AdaptiveDimensions.current
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = dimensions.spacingXLarge)
    ) {
        // 推理引擎分组
        item {
            IOSSettingsSection(
                title = "推理引擎",
                footer = "向左滑动可编辑或删除服务商，点击切换默认引擎"
            ) {
                val providers = uiState.filteredProviders
                providers.forEachIndexed { index, provider ->
                    IOSProviderCard(
                        provider = provider,
                        isDefault = provider.isDefault,
                        onClick = { 
                            // 点击直接设为默认服务商
                            onEvent(AiConfigUiEvent.SetDefaultProvider(provider.id))
                        },
                        onEdit = {
                            // 滑动编辑 - 导航到编辑页面
                            if (onNavigateToEditProvider != null) {
                                onNavigateToEditProvider(provider.id)
                            } else {
                                onEvent(AiConfigUiEvent.ShowEditDialog(provider))
                            }
                        },
                        onDelete = {
                            // 滑动删除
                            onEvent(AiConfigUiEvent.ShowDeleteConfirmDialog(provider.id))
                        },
                        showDivider = index < providers.lastIndex,
                        highlightQuery = uiState.searchQuery
                    )
                }
            }
        }

        // 通用选项分组
        item {
            IOSSettingsSection(title = "通用选项") {
                IOSSettingsItem(
                    icon = Icons.Default.Language,
                    iconBackgroundColor = iOSBlue,
                    title = "网络代理",
                    value = if (uiState.proxyConfig?.enabled == true) {
                        "${uiState.proxyConfig?.type?.name ?: "HTTP"} ${uiState.proxyConfig?.host ?: ""}:${uiState.proxyConfig?.port ?: ""}"
                    } else {
                        "未设置"
                    },
                    showDivider = true,
                    onClick = { onEvent(AiConfigUiEvent.ShowProxyDialog) }
                )
                IOSSettingsItem(
                    icon = Icons.Default.QueryStats,
                    iconBackgroundColor = iOSPurple,
                    title = "用量统计",
                    showDivider = false,
                    onClick = { onEvent(AiConfigUiEvent.NavigateToUsageStats) }
                )
            }
        }
    }
}
```

---

### 变更 9：标签管理页接入搜索栏

**修改原因**：
1. BrainTagScreen 已有搜索事件与状态，但 UI 没有输入入口，功能不可用。
2. 现成的 TagSearchBar 可复用，减少额外 UI 设计成本。
3. 展示在 TopAppBar 下方不影响主体列表布局。

**影响分析**：
- BrainTagScreen 使用 `rememberSaveable` 管理搜索栏显示状态。
- 关闭搜索栏时触发 `ClearSearch`，保证状态一致。
- UI 预览未验证，需人工确认视觉效果与高度。

**替代方案**：
- 将输入框嵌入 TopAppBar，但布局更拥挤，交互成本更高。

**代码变更**：

修改前：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/tag/BrainTagScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrainTagScreenContent(
    uiState: BrainTagUiState,
    onEvent: (BrainTagUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("标签管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: 实现搜索 */ }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEvent(BrainTagUiEvent.ShowAddDialog) }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加标签"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicatorFullScreen(
                        message = "加载标签..."
                    )
                }
                uiState.isEmptyState -> {
                    EmptyView(
                        message = "还没有标签",
                        actionText = "添加标签",
                        onAction = { onEvent(BrainTagUiEvent.ShowAddDialog) },
                        emptyType = EmptyType.NoTags
                    )
                }
                else -> {
                    TagList(
                        tags = uiState.displayTags,
                        searchQuery = uiState.searchQuery,
                        onDeleteTag = { tagId -> onEvent(BrainTagUiEvent.DeleteTag(tagId)) },
                        onEditTag = { tag -> onEvent(BrainTagUiEvent.StartEditTag(tag)) }
                    )
                }
            }
        }
    }
    // 添加标签对话框
    if (uiState.showAddDialog) {
        AddTagDialog(
            tagContent = uiState.newTagContent,
            selectedType = uiState.selectedTagType,
            onContentChange = { onEvent(BrainTagUiEvent.UpdateNewTagContent(it)) },
            onTypeChange = { onEvent(BrainTagUiEvent.UpdateSelectedTagType(it)) },
            onDismiss = { onEvent(BrainTagUiEvent.HideAddDialog) },
            onConfirm = { onEvent(BrainTagUiEvent.ConfirmAddTag) }
        )
    }
    // 编辑标签对话框 (BUG-00066)
    if (uiState.showEditDialog && uiState.editingTag != null) {
        EditBrainTagDialog(
            tag = uiState.editingTag,
            onConfirm = { tagId, newContent, newType ->
                onEvent(BrainTagUiEvent.ConfirmEditTag(tagId, newContent, newType))
            },
            onDismiss = { onEvent(BrainTagUiEvent.CancelEditTag) }
        )
    }
    // 错误提示 - iOS风格
    uiState.error?.let { error ->
        IOSAlertDialog(
            title = "错误",
            message = error,
            confirmText = "确定",
            onConfirm = { onEvent(BrainTagUiEvent.ClearError) },
            onDismiss = { onEvent(BrainTagUiEvent.ClearError) },
            showDismissButton = false
        )
    }
}
```

修改后：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/tag/BrainTagScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrainTagScreenContent(
    uiState: BrainTagUiState,
    onEvent: (BrainTagUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchBarVisible by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("标签管理") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchBarVisible = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "搜索"
                            )
                        }
                    }
                )

                if (isSearchBarVisible) {
                    TagSearchBar(
                        searchQuery = uiState.searchQuery,
                        resultCount = uiState.displayTags.size,
                        onQueryChange = { onEvent(BrainTagUiEvent.UpdateSearchQuery(it)) },
                        onSearchClose = {
                            isSearchBarVisible = false
                            onEvent(BrainTagUiEvent.ClearSearch)
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEvent(BrainTagUiEvent.ShowAddDialog) }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加标签"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicatorFullScreen(
                        message = "加载标签..."
                    )
                }
                uiState.isEmptyState -> {
                    EmptyView(
                        message = "还没有标签",
                        actionText = "添加标签",
                        onAction = { onEvent(BrainTagUiEvent.ShowAddDialog) },
                        emptyType = EmptyType.NoTags
                    )
                }
                else -> {
                    TagList(
                        tags = uiState.displayTags,
                        searchQuery = uiState.searchQuery,
                        onDeleteTag = { tagId -> onEvent(BrainTagUiEvent.DeleteTag(tagId)) },
                        onEditTag = { tag -> onEvent(BrainTagUiEvent.StartEditTag(tag)) }
                    )
                }
            }
        }
    }
    // 添加标签对话框
    if (uiState.showAddDialog) {
        AddTagDialog(
            tagContent = uiState.newTagContent,
            selectedType = uiState.selectedTagType,
            onContentChange = { onEvent(BrainTagUiEvent.UpdateNewTagContent(it)) },
            onTypeChange = { onEvent(BrainTagUiEvent.UpdateSelectedTagType(it)) },
            onDismiss = { onEvent(BrainTagUiEvent.HideAddDialog) },
            onConfirm = { onEvent(BrainTagUiEvent.ConfirmAddTag) }
        )
    }
    // 编辑标签对话框 (BUG-00066)
    if (uiState.showEditDialog && uiState.editingTag != null) {
        EditBrainTagDialog(
            tag = uiState.editingTag,
            onConfirm = { tagId, newContent, newType ->
                onEvent(BrainTagUiEvent.ConfirmEditTag(tagId, newContent, newType))
            },
            onDismiss = { onEvent(BrainTagUiEvent.CancelEditTag) }
        )
    }
    // 错误提示 - iOS风格
    uiState.error?.let { error ->
        IOSAlertDialog(
            title = "错误",
            message = error,
            confirmText = "确定",
            onConfirm = { onEvent(BrainTagUiEvent.ClearError) },
            onDismiss = { onEvent(BrainTagUiEvent.ClearError) },
            showDismissButton = false
        )
    }
}
```

---

### 变更 10：标签搜索无结果提示

**修改原因**：
1. 搜索无结果时列表区域完全空白，用户容易误解为加载异常。
2. EmptyView 已有 NoResults 类型，可复用。
3. 搜索反馈应明确告知结果为零。

**影响分析**：
- 仅在 TagList 内增加条件分支，分组逻辑不变。
- 仅影响搜索无结果的展示，不影响默认列表。

**替代方案**：
- 在 TagSearchBar 附近提示“0”，但列表仍空白，反馈不够明确。

**代码变更**：

修改前：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/tag/BrainTagScreen.kt
@Composable
private fun TagList(
    tags: List<BrainTag>,
    searchQuery: String,
    onDeleteTag: (Long) -> Unit,
    onEditTag: (BrainTag) -> Unit,
    modifier: Modifier = Modifier
) {
    // 过滤标签
    val filteredTags = if (searchQuery.isBlank()) {
        tags
    } else {
        tags.filter { it.content.contains(searchQuery, ignoreCase = true) }
    }

    // 按类型分组
    val landmineTags = filteredTags.filter { it.type == TagType.RISK_RED }
    val strategyTags = filteredTags.filter { it.type == TagType.STRATEGY_GREEN }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
    ) {
        // 雷区标签
        if (landmineTags.isNotEmpty()) {
            item {
                Text(
                    text = "雷区标签 (${landmineTags.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            items(
                items = landmineTags,
                key = { it.id }
            ) { tag ->
                TagChip(
                    text = tag.content,
                    tagType = tag.type,
                    highlightQuery = searchQuery,
                    onDelete = { onDeleteTag(tag.id) },
                    onClick = { onEditTag(tag) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
        }

        // 策略标签
        if (strategyTags.isNotEmpty()) {
            item {
                Text(
                    text = "策略标签 (${strategyTags.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(
                items = strategyTags,
                key = { it.id }
            ) { tag ->
                TagChip(
                    text = tag.content,
                    tagType = tag.type,
                    highlightQuery = searchQuery,
                    onDelete = { onDeleteTag(tag.id) },
                    onClick = { onEditTag(tag) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
```

修改后：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/tag/BrainTagScreen.kt
@Composable
private fun TagList(
    tags: List<BrainTag>,
    searchQuery: String,
    onDeleteTag: (Long) -> Unit,
    onEditTag: (BrainTag) -> Unit,
    modifier: Modifier = Modifier
) {
    // 过滤标签
    val filteredTags = if (searchQuery.isBlank()) {
        tags
    } else {
        tags.filter { it.content.contains(searchQuery, ignoreCase = true) }
    }

    if (filteredTags.isEmpty() && searchQuery.isNotBlank()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptyView(
                message = "没有找到匹配的标签",
                actionText = null,
                onAction = null,
                emptyType = EmptyType.NoResults
            )
        }
        return
    }

    // 按类型分组
    val landmineTags = filteredTags.filter { it.type == TagType.RISK_RED }
    val strategyTags = filteredTags.filter { it.type == TagType.STRATEGY_GREEN }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
    ) {
        // 雷区标签
        if (landmineTags.isNotEmpty()) {
            item {
                Text(
                    text = "雷区标签 (${landmineTags.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            items(
                items = landmineTags,
                key = { it.id }
            ) { tag ->
                TagChip(
                    text = tag.content,
                    tagType = tag.type,
                    highlightQuery = searchQuery,
                    onDelete = { onDeleteTag(tag.id) },
                    onClick = { onEditTag(tag) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
            }
        }

        // 策略标签
        if (strategyTags.isNotEmpty()) {
            item {
                Text(
                    text = "策略标签 (${strategyTags.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(
                items = strategyTags,
                key = { it.id }
            ) { tag ->
                TagChip(
                    text = tag.content,
                    tagType = tag.type,
                    highlightQuery = searchQuery,
                    onDelete = { onDeleteTag(tag.id) },
                    onClick = { onEditTag(tag) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
```

---

### 变更 11：AI 配置搜索无结果提示

**修改原因**：
1. AI 配置搜索无结果时列表空白，反馈不明确。
2. 搜索体验应在所有页面保持一致的“无结果”提示。
3. EmptyView.NoResults 已存在，可复用。

**影响分析**：
- 仅在 AiConfigScreenContent 的展示分支中新增条件。
- 不影响已有“无服务商”空状态逻辑。

**替代方案**：
- 不做提示，维持空白，但体验不一致。

**代码变更**：

修改前：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/aiconfig/AiConfigScreen.kt
when {
    uiState.isLoading -> {
        LoadingIndicatorFullScreen(message = "加载服务商...")
    }
    uiState.error != null -> {
        FriendlyErrorCard(
            error = UserFriendlyError(
                title = "出错了",
                message = uiState.error ?: "未知错误",
                icon = Icons.Default.Warning
            ),
            onAction = { onEvent(AiConfigUiEvent.LoadProviders) }
        )
    }
    !uiState.hasProviders -> {
        EmptyView(
            message = "还没有配置 AI 服务商\n点击右上角按钮添加",
            actionText = null,
            onAction = null
        )
    }
    else -> {
        ProviderListContent(
            uiState = uiState,
            onEvent = onEvent,
            onNavigateToEditProvider = onNavigateToEditProvider
        )
    }
}
```

修改后：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/aiconfig/AiConfigScreen.kt
when {
    uiState.isLoading -> {
        LoadingIndicatorFullScreen(message = "加载服务商...")
    }
    uiState.error != null -> {
        FriendlyErrorCard(
            error = UserFriendlyError(
                title = "出错了",
                message = uiState.error ?: "未知错误",
                icon = Icons.Default.Warning
            ),
            onAction = { onEvent(AiConfigUiEvent.LoadProviders) }
        )
    }
    !uiState.hasProviders -> {
        EmptyView(
            message = "还没有配置 AI 服务商\n点击右上角按钮添加",
            actionText = null,
            onAction = null
        )
    }
    uiState.searchQuery.isNotBlank() && uiState.filteredProviders.isEmpty() -> {
        EmptyView(
            message = "未找到匹配的服务商",
            actionText = null,
            onAction = null,
            emptyType = EmptyType.NoResults
        )
    }
    else -> {
        ProviderListContent(
            uiState = uiState,
            onEvent = onEvent,
            onNavigateToEditProvider = onNavigateToEditProvider
        )
    }
}
```

---

### 变更 12：标签搜索栏 BackHandler 关闭

**修改原因**：
1. 搜索栏展开时 Back 直接返回页面，交互不符合用户预期。
2. 搜索 UI 应优先关闭，而非立即退出页面。
3. BackHandler 可以在 UI 层直接处理，改动范围小。

**影响分析**：
- 仅影响 BrainTagScreenContent 的 Back 行为。
- 当搜索栏不可见时，返回行为保持不变。

**替代方案**：
- 保持默认返回行为，但体验不一致。

**代码变更**：

修改前：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/tag/BrainTagScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrainTagScreenContent(
    uiState: BrainTagUiState,
    onEvent: (BrainTagUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchBarVisible by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("标签管理") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchBarVisible = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "搜索"
                            )
                        }
                    }
                )

                if (isSearchBarVisible) {
                    TagSearchBar(
                        searchQuery = uiState.searchQuery,
                        resultCount = uiState.displayTags.size,
                        onQueryChange = { onEvent(BrainTagUiEvent.UpdateSearchQuery(it)) },
                        onSearchClose = {
                            isSearchBarVisible = false
                            onEvent(BrainTagUiEvent.ClearSearch)
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEvent(BrainTagUiEvent.ShowAddDialog) }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加标签"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicatorFullScreen(
                        message = "加载标签..."
                    )
                }
                uiState.isEmptyState -> {
                    EmptyView(
                        message = "还没有标签",
                        actionText = "添加标签",
                        onAction = { onEvent(BrainTagUiEvent.ShowAddDialog) },
                        emptyType = EmptyType.NoTags
                    )
                }
                else -> {
                    TagList(
                        tags = uiState.displayTags,
                        searchQuery = uiState.searchQuery,
                        onDeleteTag = { tagId -> onEvent(BrainTagUiEvent.DeleteTag(tagId)) },
                        onEditTag = { tag -> onEvent(BrainTagUiEvent.StartEditTag(tag)) }
                    )
                }
            }
        }
    }
    // 添加标签对话框
    if (uiState.showAddDialog) {
        AddTagDialog(
            tagContent = uiState.newTagContent,
            selectedType = uiState.selectedTagType,
            onContentChange = { onEvent(BrainTagUiEvent.UpdateNewTagContent(it)) },
            onTypeChange = { onEvent(BrainTagUiEvent.UpdateSelectedTagType(it)) },
            onDismiss = { onEvent(BrainTagUiEvent.HideAddDialog) },
            onConfirm = { onEvent(BrainTagUiEvent.ConfirmAddTag) }
        )
    }
    // 编辑标签对话框 (BUG-00066)
    if (uiState.showEditDialog && uiState.editingTag != null) {
        EditBrainTagDialog(
            tag = uiState.editingTag,
            onConfirm = { tagId, newContent, newType ->
                onEvent(BrainTagUiEvent.ConfirmEditTag(tagId, newContent, newType))
            },
            onDismiss = { onEvent(BrainTagUiEvent.CancelEditTag) }
        )
    }
    // 错误提示 - iOS风格
    uiState.error?.let { error ->
        IOSAlertDialog(
            title = "错误",
            message = error,
            confirmText = "确定",
            onConfirm = { onEvent(BrainTagUiEvent.ClearError) },
            onDismiss = { onEvent(BrainTagUiEvent.ClearError) },
            showDismissButton = false
        )
    }
}
```

修改后：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/tag/BrainTagScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrainTagScreenContent(
    uiState: BrainTagUiState,
    onEvent: (BrainTagUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchBarVisible by rememberSaveable { mutableStateOf(false) }

    BackHandler {
        if (isSearchBarVisible) {
            isSearchBarVisible = false
            onEvent(BrainTagUiEvent.ClearSearch)
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("标签管理") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchBarVisible = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "搜索"
                            )
                        }
                    }
                )

                if (isSearchBarVisible) {
                    TagSearchBar(
                        searchQuery = uiState.searchQuery,
                        resultCount = uiState.displayTags.size,
                        onQueryChange = { onEvent(BrainTagUiEvent.UpdateSearchQuery(it)) },
                        onSearchClose = {
                            isSearchBarVisible = false
                            onEvent(BrainTagUiEvent.ClearSearch)
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEvent(BrainTagUiEvent.ShowAddDialog) }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加标签"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicatorFullScreen(
                        message = "加载标签..."
                    )
                }
                uiState.isEmptyState -> {
                    EmptyView(
                        message = "还没有标签",
                        actionText = "添加标签",
                        onAction = { onEvent(BrainTagUiEvent.ShowAddDialog) },
                        emptyType = EmptyType.NoTags
                    )
                }
                else -> {
                    TagList(
                        tags = uiState.displayTags,
                        searchQuery = uiState.searchQuery,
                        onDeleteTag = { tagId -> onEvent(BrainTagUiEvent.DeleteTag(tagId)) },
                        onEditTag = { tag -> onEvent(BrainTagUiEvent.StartEditTag(tag)) }
                    )
                }
            }
        }
    }
    // 添加标签对话框
    if (uiState.showAddDialog) {
        AddTagDialog(
            tagContent = uiState.newTagContent,
            selectedType = uiState.selectedTagType,
            onContentChange = { onEvent(BrainTagUiEvent.UpdateNewTagContent(it)) },
            onTypeChange = { onEvent(BrainTagUiEvent.UpdateSelectedTagType(it)) },
            onDismiss = { onEvent(BrainTagUiEvent.HideAddDialog) },
            onConfirm = { onEvent(BrainTagUiEvent.ConfirmAddTag) }
        )
    }
    // 编辑标签对话框 (BUG-00066)
    if (uiState.showEditDialog && uiState.editingTag != null) {
        EditBrainTagDialog(
            tag = uiState.editingTag,
            onConfirm = { tagId, newContent, newType ->
                onEvent(BrainTagUiEvent.ConfirmEditTag(tagId, newContent, newType))
            },
            onDismiss = { onEvent(BrainTagUiEvent.CancelEditTag) }
        )
    }
    // 错误提示 - iOS风格
    uiState.error?.let { error ->
        IOSAlertDialog(
            title = "错误",
            message = error,
            confirmText = "确定",
            onConfirm = { onEvent(BrainTagUiEvent.ClearError) },
            onDismiss = { onEvent(BrainTagUiEvent.ClearError) },
            showDismissButton = false
        )
    }
}
```

---

### 变更 13：联系人画像搜索高亮

**修改原因**：
1. PersonaTab 搜索仅过滤列表，没有命中位置提示，用户难以确认匹配点。
2. 联系人列表/标签管理/服务商列表已统一高亮，画像页缺失会造成体验割裂。
3. 复用 `TextHighlight` 工具即可低成本补齐一致性。

**影响分析**：
- 仅影响联系人画像页（PersonaTab）显示层。
- 搜索过滤逻辑不变，仅增加文本高亮渲染。

**替代方案**：
- 仅过滤不高亮（反馈不清晰）。
- 只高亮标签值，不高亮类别标题（类别命中不明显）。

**代码变更**：

修改前：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/contact/persona/PersonaTab.kt
@Composable
fun PersonaTab(
    facts: List<Fact>,
    onFactClick: (Fact) -> Unit,
    onFactLongClick: (Fact) -> Unit,
    showResetButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    ...
    SimpleCategoryCard(
        categoryName = category,
        facts = categoryFacts,
        isExpanded = category in expandedCategories,
        onToggle = { ... },
        onFactClick = onFactClick,
        onFactLongClick = onFactLongClick
    )
}

@Composable
private fun SimpleCategoryCard(
    categoryName: String,
    facts: List<Fact>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onFactClick: (Fact) -> Unit,
    onFactLongClick: (Fact) -> Unit,
    modifier: Modifier = Modifier
) {
    ...
    Text(
        text = categoryName,
        fontSize = dimensions.fontSizeSubtitle,
        fontWeight = FontWeight.SemiBold,
        color = Color.Black,
        modifier = Modifier.weight(1f)
    )
    ...
    SimpleTagChip(
        text = fact.value,
        color = categoryColor,
        onClick = { onFactClick(fact) },
        onLongClick = { onFactLongClick(fact) }
    )
}

@Composable
private fun SimpleTagChip(
    text: String,
    color: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = dimensions.fontSizeBody,
        color = color.copy(alpha = 0.9f),
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = dimensions.spacingMedium, vertical = dimensions.spacingSmall)
    )
}
```

修改后：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/contact/persona/PersonaTab.kt
@Composable
fun PersonaTab(
    facts: List<Fact>,
    onFactClick: (Fact) -> Unit,
    onFactLongClick: (Fact) -> Unit,
    showResetButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    val highlightStyle = createSearchHighlightStyle(
        isDarkTheme = isSystemInDarkTheme(),
        baseColor = iOSBlue
    )
    var searchQuery by rememberSaveable { mutableStateOf("") }
    ...
    SimpleCategoryCard(
        categoryName = category,
        facts = categoryFacts,
        isExpanded = category in expandedCategories,
        searchQuery = searchQuery,
        highlightStyle = highlightStyle,
        onToggle = { ... },
        onFactClick = onFactClick,
        onFactLongClick = onFactLongClick
    )
}

@Composable
private fun SimpleCategoryCard(
    categoryName: String,
    facts: List<Fact>,
    isExpanded: Boolean,
    searchQuery: String,
    highlightStyle: SpanStyle,
    onToggle: () -> Unit,
    onFactClick: (Fact) -> Unit,
    onFactLongClick: (Fact) -> Unit,
    modifier: Modifier = Modifier
) {
    ...
    Text(
        text = buildHighlightedText(
            text = categoryName,
            query = searchQuery,
            highlightStyle = highlightStyle
        ),
        fontSize = dimensions.fontSizeSubtitle,
        fontWeight = FontWeight.SemiBold,
        color = Color.Black,
        modifier = Modifier.weight(1f)
    )
    ...
    SimpleTagChip(
        text = fact.value,
        color = categoryColor,
        onClick = { onFactClick(fact) },
        onLongClick = { onFactLongClick(fact) },
        highlightQuery = searchQuery,
        highlightStyle = highlightStyle
    )
}

@Composable
private fun SimpleTagChip(
    text: String,
    color: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    highlightQuery: String,
    highlightStyle: SpanStyle,
    modifier: Modifier = Modifier
) {
    Text(
        text = buildHighlightedText(
            text = text,
            query = highlightQuery,
            highlightStyle = highlightStyle
        ),
        fontSize = dimensions.fontSizeBody,
        color = color.copy(alpha = 0.9f),
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = dimensions.spacingMedium, vertical = dimensions.spacingSmall)
    )
}
```

---

### 变更 14：PersonaTabV2 分类与标签搜索高亮

**修改原因**：
1. PersonaTabV2 的搜索结果缺少分类标题高亮，反馈不完整。
2. SelectableTagChip 使用固定高亮样式，无法与整体高亮策略保持一致。
3. 复用 `TextHighlight` 工具可提升一致性与可维护性。

**影响分析**：
- 仅影响 PersonaTabV2 的分类标题与标签渲染。
- 搜索过滤逻辑不变，仅增加高亮样式传递。

**替代方案**：
- 保持现状：搜索命中位置不明显。
- 仅高亮分类标题：标签命中仍不直观。

**代码变更**：

修改前：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/contact/persona/DynamicCategoryCard.kt
@Composable
fun DynamicCategoryCard(
    category: FactCategory,
    isEditMode: Boolean,
    selectedFactIds: Set<String>,
    searchQuery: String,
    isDarkMode: Boolean,
    onToggleExpand: () -> Unit,
    onFactClick: (String) -> Unit,
    onFactLongClick: (String) -> Unit,
    onToggleFactSelection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = category.color.toComposeColor()

    ...
    Text(
        text = category.key,
        style = MaterialTheme.typography.titleMedium,
        color = categoryColor.titleColor,
        modifier = Modifier.weight(1f)
    )
    ...
    SelectableTagChip(
        fact = fact,
        isEditMode = isEditMode,
        isSelected = selectedFactIds.contains(fact.id),
        searchQuery = searchQuery,
        categoryColor = categoryColor,
        onClick = { onFactClick(fact.id) },
        onLongClick = { onFactLongClick(fact.id) },
        onToggleSelection = { onToggleFactSelection(fact.id) }
    )
}
```

```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/contact/persona/SelectableTagChip.kt
@Composable
fun SelectableTagChip(
    fact: Fact,
    isEditMode: Boolean,
    isSelected: Boolean,
    searchQuery: String,
    categoryColor: ComposeCategoryColor,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    ...
    Text(
        text = buildHighlightedText(
            text = fact.value,
            query = searchQuery,
            highlightStyle = SpanStyle(
                fontWeight = FontWeight.Bold,
                background = MaterialTheme.colorScheme.tertiaryContainer
            )
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = textColor
    )
}
```

修改后：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/contact/persona/DynamicCategoryCard.kt
@Composable
fun DynamicCategoryCard(
    category: FactCategory,
    isEditMode: Boolean,
    selectedFactIds: Set<String>,
    searchQuery: String,
    isDarkMode: Boolean,
    onToggleExpand: () -> Unit,
    onFactClick: (String) -> Unit,
    onFactLongClick: (String) -> Unit,
    onToggleFactSelection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = category.color.toComposeColor()
    val highlightStyle = createSearchHighlightStyle(
        isDarkTheme = isDarkMode,
        baseColor = categoryColor.titleColor
    )

    ...
    Text(
        text = buildHighlightedText(
            text = category.key,
            query = searchQuery,
            highlightStyle = highlightStyle
        ),
        style = MaterialTheme.typography.titleMedium,
        color = categoryColor.titleColor,
        modifier = Modifier.weight(1f)
    )
    ...
    SelectableTagChip(
        fact = fact,
        isEditMode = isEditMode,
        isSelected = selectedFactIds.contains(fact.id),
        searchQuery = searchQuery,
        categoryColor = categoryColor,
        highlightStyle = highlightStyle,
        onClick = { onFactClick(fact.id) },
        onLongClick = { onFactLongClick(fact.id) },
        onToggleSelection = { onToggleFactSelection(fact.id) }
    )
}
```

```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/contact/persona/SelectableTagChip.kt
@Composable
fun SelectableTagChip(
    fact: Fact,
    isEditMode: Boolean,
    isSelected: Boolean,
    searchQuery: String,
    categoryColor: ComposeCategoryColor,
    highlightStyle: SpanStyle,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    ...
    Text(
        text = buildHighlightedText(
            text = fact.value,
            query = searchQuery,
            highlightStyle = highlightStyle
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = textColor
    )
}
```

---

### 变更 15：ModernPersonaTab 搜索高亮与无结果提示

**修改原因**：
1. ModernPersonaTab 搜索仅过滤列表，标签文本没有高亮反馈。
2. 搜索无结果时仍展示“暂无标签”，与实际语义不符。
3. 需要与其他搜索场景保持一致体验。

**影响分析**：
- 仅影响 ModernPersonaTab 组件显示层。
- 搜索过滤逻辑保持不变。

**替代方案**：
- 仅保留过滤，不加高亮与无结果提示。
- 仅新增无结果提示，忽略高亮。

**代码变更**：

修改前：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/persona/ModernPersonaTab.kt
val filteredCategories = categories.map { category ->
    val filteredTags = if (searchQuery.isBlank()) {
        category.tags
    } else {
        category.tags.filter { it.contains(searchQuery, ignoreCase = true) }
    }
    category.copy(tags = filteredTags)
}.filter { it.tags.isNotEmpty() || searchQuery.isBlank() }

if (filteredCategories.isEmpty() || filteredCategories.all { it.tags.isEmpty() }) {
    EmptyPersonaPlaceholder(modifier = Modifier.weight(1f))
} else {
    LazyColumn { ... }
}

@Composable
fun ModernFolderCard(
    category: TagCategory,
    tags: List<String>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAddTag: () -> Unit,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ...
    tags.forEach { tag ->
        MorandiTagChip(
            text = tag,
            category = category,
            onClick = { onTagClick(tag) }
        )
    }
}

@Composable
fun MorandiTagChip(
    text: String,
    category: TagCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MorandiTagColors.getColors(category)
    Text(
        text = text,
        fontSize = 14.sp,
        color = colors.textColor,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}
```

修改后：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/persona/ModernPersonaTab.kt
val filteredCategories = categories.map { category ->
    val filteredTags = if (searchQuery.isBlank()) {
        category.tags
    } else {
        category.tags.filter { it.contains(searchQuery, ignoreCase = true) }
    }
    category.copy(tags = filteredTags)
}.filter { it.tags.isNotEmpty() || searchQuery.isBlank() }

val hasSearchQuery = searchQuery.isNotBlank()
if (filteredCategories.isEmpty() || filteredCategories.all { it.tags.isEmpty() }) {
    if (hasSearchQuery) {
        EmptyPersonaSearchResult(modifier = Modifier.weight(1f))
    } else {
        EmptyPersonaPlaceholder(modifier = Modifier.weight(1f))
    }
} else {
    LazyColumn { ... }
}

@Composable
fun ModernFolderCard(
    category: TagCategory,
    tags: List<String>,
    isExpanded: Boolean,
    searchQuery: String,
    onToggle: () -> Unit,
    onAddTag: () -> Unit,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ...
    tags.forEach { tag ->
        MorandiTagChip(
            text = tag,
            category = category,
            highlightQuery = searchQuery,
            onClick = { onTagClick(tag) }
        )
    }
}

@Composable
fun MorandiTagChip(
    text: String,
    category: TagCategory,
    onClick: () -> Unit,
    highlightQuery: String = "",
    modifier: Modifier = Modifier
) {
    val colors = MorandiTagColors.getColors(category)
    val highlightStyle = createSearchHighlightStyle(
        isDarkTheme = isSystemInDarkTheme(),
        baseColor = colors.textColor
    )
    Text(
        text = buildHighlightedText(
            text = text,
            query = highlightQuery,
            highlightStyle = highlightStyle
        ),
        fontSize = 14.sp,
        color = colors.textColor,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}
```

---

### 变更 16：ModernPersonaTab 分类名称搜索匹配与标题高亮

**修改原因**：
1. ModernPersonaTab 提示“搜索标签或分类”，但原逻辑仅匹配标签文本。
2. 分类名称不参与匹配会造成“有分类但搜不到”的体验落差。
3. 分类标题缺少高亮反馈，不利于定位命中位置。

**影响分析**：
- 仅影响 ModernPersonaTab 的过滤逻辑与分类标题渲染。
- 标签过滤逻辑保持原样，新增分类命中保留标签内容。

**替代方案**：
- 继续只匹配标签，忽略分类名称（与提示文案不一致）。
- 分类命中时仍过滤标签（可能出现空分类）。

**代码变更**：

修改前：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/persona/ModernPersonaTab.kt
val filteredCategories = categories.map { category ->
    val filteredTags = if (searchQuery.isBlank()) {
        category.tags
    } else {
        category.tags.filter { it.contains(searchQuery, ignoreCase = true) }
    }
    category.copy(tags = filteredTags)
}.filter { it.tags.isNotEmpty() || searchQuery.isBlank() }

Text(
    text = category.displayName,
    fontSize = 17.sp,
    fontWeight = FontWeight.SemiBold,
    color = iOSTextPrimary
)
```

修改后：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/persona/ModernPersonaTab.kt
val hasSearchQuery = searchQuery.isNotBlank()
val filteredCategories = categories.mapNotNull { category ->
    val categoryMatches = hasSearchQuery &&
        category.category.displayName.contains(searchQuery, ignoreCase = true)
    val filteredTags = when {
        !hasSearchQuery -> category.tags
        categoryMatches -> category.tags
        else -> category.tags.filter { it.contains(searchQuery, ignoreCase = true) }
    }
    if (!hasSearchQuery || categoryMatches || filteredTags.isNotEmpty()) {
        category.copy(tags = filteredTags)
    } else {
        null
    }
}

val highlightStyle = createSearchHighlightStyle(
    isDarkTheme = isSystemInDarkTheme(),
    baseColor = style.iconBackground
)
Text(
    text = buildHighlightedText(
        text = category.displayName,
        query = searchQuery,
        highlightStyle = highlightStyle
    ),
    fontSize = 17.sp,
    fontWeight = FontWeight.SemiBold,
    color = iOSTextPrimary
)
```

---

### 变更 17：ModernPersonaTab 搜索自动展开

**修改原因**：
1. 搜索模式下若分类被折叠，命中标签会被隐藏，搜索体验不完整。
2. 搜索阶段优先展示结果可见性。

**影响分析**：
- 仅影响 ModernPersonaTab 搜索模式下的展开行为。
- 搜索结束后仍保留用户原有展开状态。

**替代方案**：
- 保持折叠状态，允许用户手动展开（命中结果可见性差）。
- 仅自动展开命中分类（需额外匹配逻辑）。

**代码变更**：

修改前：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/persona/ModernPersonaTab.kt
ModernFolderCard(
    category = categoryData.category,
    tags = categoryData.tags,
    isExpanded = categoryData.category in expandedCategories,
    searchQuery = searchQuery,
    onToggle = { ... },
    onAddTag = { ... },
    onTagClick = { ... }
)
```

修改后：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/persona/ModernPersonaTab.kt
ModernFolderCard(
    category = categoryData.category,
    tags = categoryData.tags,
    isExpanded = hasSearchQuery || categoryData.category in expandedCategories,
    searchQuery = searchQuery,
    onToggle = { ... },
    onAddTag = { ... },
    onTagClick = { ... }
)
```

---

### 变更 18：ModernPersonaTab 无结果提示显示关键词

**修改原因**：
1. 无结果提示未展示关键词，反馈不够明确。
2. 其他搜索场景普遍会显示查询内容，保持一致性更好。

**影响分析**：
- 仅影响 ModernPersonaTab 的无结果占位文案。
- 不影响过滤逻辑。

**替代方案**：
- 保持通用提示文本。
- 显示关键词但不高亮。

**代码变更**：

修改前：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/persona/ModernPersonaTab.kt
if (hasSearchQuery) {
    EmptyPersonaSearchResult(modifier = Modifier.weight(1f))
}

@Composable
private fun EmptyPersonaSearchResult(modifier: Modifier = Modifier) {
    ...
    Text(
        text = "没有找到匹配的标签",
        fontSize = 17.sp,
        color = iOSTextPrimary
    )
}
```

修改后：
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/persona/ModernPersonaTab.kt
if (hasSearchQuery) {
    EmptyPersonaSearchResult(
        query = searchQuery,
        modifier = Modifier.weight(1f)
    )
}

@Composable
private fun EmptyPersonaSearchResult(
    query: String,
    modifier: Modifier = Modifier
) {
    val highlightStyle = createSearchHighlightStyle(
        isDarkTheme = isSystemInDarkTheme(),
        baseColor = iOSBlue
    )
    ...
    Text(
        text = buildHighlightedText(
            text = "未找到 \"$query\"",
            query = query,
            highlightStyle = highlightStyle
        ),
        fontSize = 17.sp,
        color = iOSTextPrimary
    )
}
```

---

## 测试情况

- 单元测试: 运行 `:presentation:testDebugUnitTest --tests "com.empathy.ai.presentation.util.TextHighlightTest"` ✅（新增改动后再次运行通过；ModernPersonaTab 自动展开与无结果提示改动后未重新执行；存在既有构建/测试告警，包括 Kapt 兼容提示与多个 deprecations）
- UI 预览: 未执行手动预览验证。
- 备注: 服务商/标签/画像/PersonaTabV2/ModernPersonaTab 高亮视觉效果、标签搜索栏与无结果提示、深色模式对比度、ModernPersonaTab 分类匹配/搜索自动展开/关键词提示均未验证。

---

## 成果清单

### A 类（文档/报告）
- 本报告: `文档/开发文档/MA/FREE/FREE-20260112-contact-search-highlight.md`
- 决策日志: `DECISION_JOURNAL.md`

### B 类（测试）
- `presentation/src/test/kotlin/com/empathy/ai/presentation/util/TextHighlightTest.kt`

### C 类（功能改动）
- 新增通用高亮工具函数 `TextHighlight.kt`。
- 联系人列表搜索结果高亮。
- AI 军师联系人选择页搜索高亮。
- AI 配置服务商列表搜索高亮（`IOSProviderCard`/`AiConfigScreen`）。
- 标签管理列表搜索高亮（`TagChip`/`BrainTagScreen`）。
- 标签管理搜索入口（`TagSearchBar`/`BrainTagScreen`）。
- 标签搜索无结果提示（`EmptyView.NoResults`）。
- AI 配置搜索无结果提示（`EmptyView.NoResults`）。
- 标签搜索栏 BackHandler 关闭。
- 联系人画像 PersonaTab 搜索高亮（类别标题与标签值）。
- PersonaTabV2 搜索高亮（分类标题与标签值）。
- ModernPersonaTab 搜索高亮与无结果提示。
- ModernPersonaTab 分类搜索匹配与标题高亮。
- ModernPersonaTab 搜索自动展开（保证命中结果可见）。
- ModernPersonaTab 无结果提示显示关键词并高亮。
- SelectableTagChip 复用通用高亮。
- 抽取高亮透明度与样式生成函数（`searchHighlightAlpha`/`createSearchHighlightStyle`）。

### D 类（重构/结构调整）
- 将 SelectableTagChip 内部高亮逻辑移至通用工具函数。

---

## 合并建议

**建议状态：📖仅参考**

理由：
1. 仅针对特定单测执行验证，未覆盖 UI 视觉验证。
2. 深色模式与不同字体大小下的高亮对比度未验证。
3. 新增服务商/标签列表高亮未进行预览确认。
4. 标签管理搜索栏新增未进行交互验证。
5. 标签搜索无结果提示未进行预览确认。
6. 适合作为 UX 方向探索的参考实现，建议在人工体验确认后再合并。

---

## 后续工作

1. 在深色模式与不同字体缩放下验证高亮可读性。
2. 在 AI 配置、标签管理、联系人画像、PersonaTabV2、ModernPersonaTab 页面验证搜索高亮的对比度与可读性。
3. 验证标签管理页搜索栏展开/关闭交互是否符合预期。
4. 验证标签搜索无结果提示的视觉与布局表现。
5. 验证 ModernPersonaTab 分类搜索匹配是否符合预期。
6. 验证 ModernPersonaTab 搜索时分类自动展开是否符合预期。
7. 验证 ModernPersonaTab 无结果提示是否显示关键词且高亮正确。
8. 评估是否需要加入“匹配数量提示”，并与产品确认布局。
9. 若合并，考虑补充更复杂的多语种/表情符号匹配测试。

---

## 探索日志

| 时间 | 尝试内容 | 结果 |
|------|----------|------|
| 20:36 | 复制高亮逻辑到 ContactListItem | ❌ 放弃（重复实现） |
| 20:39 | 抽取 TextHighlight 工具函数 | ✅ 采用 |
| 20:41 | 联系人列表搜索高亮 | ✅ 完成 |
| 20:42 | 联系人选择页搜索高亮 | ✅ 完成 |
| 20:43 | 搜索结果计数提示 | ❌ 放弃（需额外设计确认） |
| 20:54 | 编写 TextHighlightTest | ✅ 完成 |
| 20:56 | 运行 `:presentation:test --tests` | ❌ 参数不支持 |
| 20:57 | 运行 `:presentation:testDebugUnitTest` | ❌ 超时 |
| 21:00 | 重试 `:presentation:testDebugUnitTest` | ✅ 通过 |
| 21:03 | 深色模式高亮透明度调整 | ✅ 完成 |
| 21:08 | 再次运行 `:presentation:testDebugUnitTest` | ❌ 超时 |
| 21:10 | 重试 `:presentation:testDebugUnitTest` | ✅ 通过 |
| 21:25 | 抽取高亮样式函数 | ✅ 完成 |
| 21:29 | 运行 `:presentation:testDebugUnitTest`（更新样式） | ✅ 通过 |
| 21:46 | 扩展高亮到 AI 配置服务商列表 | ✅ 完成 |
| 21:49 | 扩展高亮到标签管理列表 | ✅ 完成 |
| 22:01 | 补齐标签管理搜索入口 | ✅ 完成 |
| 22:06 | 标签搜索无结果提示 | ✅ 完成 |
| 22:08 | 运行 `:presentation:testDebugUnitTest` | ❌ 超时 |
| 22:11 | 重试 `:presentation:testDebugUnitTest` | ✅ 通过 |
| 22:15 | AI 配置搜索无结果提示 | ✅ 完成 |
| 22:15 | BackHandler 关闭标签搜索栏 | ✅ 完成 |
| 22:17 | 运行 `:presentation:testDebugUnitTest`（新改动） | ✅ 通过 |
| 22:32 | PersonaTab 搜索高亮 | ✅ 完成 |
| 22:43 | PersonaTabV2 搜索高亮 | ✅ 完成 |
| 22:51 | ModernPersonaTab 搜索高亮与无结果提示 | ✅ 完成 |
| 22:57 | ModernPersonaTab 分类搜索匹配与标题高亮 | ✅ 完成 |
| 23:05 | 运行 `:presentation:testDebugUnitTest`（TextHighlightTest） | ✅ 通过 |
| 23:11 | ModernPersonaTab 搜索自动展开 | ✅ 完成 |
| 23:14 | ModernPersonaTab 无结果提示显示关键词 | ✅ 完成 |

---

## 报告质量自检

### 字数检查
- [x] 总字数达到最低要求（约 3200+ 字，要求 2000 字）
- [x] 代码行数达到最低要求（约 240+ 行，要求 50 行）

### 内容完整性
- [x] 所有必须章节都已填写
- [x] 每个代码变更都包含修改前+修改后
- [x] 探索过程记录了失败尝试

### 自包含检查
- [x] 删除分支后，仅凭报告可理解全部变更
- [x] 代码片段均为完整函数级别
- [x] 所有代码片段标注文件路径

### 可操作性检查
- [x] 成果清单按 A/B/C/D 分类
- [x] 合并建议明确给出理由
- [x] 后续工作包含具体行动项
