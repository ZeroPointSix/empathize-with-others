# BUG-00043: 重新生成弹窗iOS风格回退

## 基本信息

| 属性 | 值 |
|------|-----|
| BUG编号 | BUG-00043 |
| 标题 | 重新生成弹窗iOS风格回退 |
| 优先级 | 中 |
| 状态 | ✅ 已修复 |
| 发现日期 | 2026-01-03 |
| 修复日期 | 2026-01-03 |
| 发现者 | 用户 |
| 影响范围 | 悬浮窗功能 |

## 问题描述

用户报告悬浮窗的"重新生成"弹窗UI需要匹配iOS风格设计。

### 期望效果（用户提供的截图）

- 白色卡片，圆角（16dp）
- 标题 "🔄 重新生成" 带蓝色图标
- 灰色说明文字
- iOS风格输入框，带占位符 "调整方向（可选）" 和计数器 "0/200"
- 底部两个按钮：
  - "取消" - iOS蓝色文字按钮
  - "生成" - iOS蓝色填充按钮（白色文字）

## 修复记录

### 1. 回退错误修改

在之前的修复尝试中，错误地修改了悬浮窗主界面的按钮样式。已回退以下文件：

**`floating_result_card.xml`（presentation和app模块）**
- ❌ 错误修改：将复制/重新生成按钮从 `MaterialButton` 改为 `TextView`
- ✅ 已回退：恢复为原始的 `MaterialButton` + Material Design 3 风格

**`ResultCard.kt`**
- ❌ 错误修改：将按钮类型从 `MaterialButton` 改为 `TextView`
- ✅ 已回退：恢复为 `com.google.android.material.button.MaterialButton`

### 2. 确认iOS风格弹窗

**`overlay_refinement.xml`** 已经是正确的iOS风格实现：
- ✅ 白色卡片 (`@color/dialog_background`)
- ✅ 圆角 16dp (`app:cardCornerRadius="16dp"`)
- ✅ 标题 "🔄 重新生成"
- ✅ 灰色说明文字 (`@color/text_secondary`)
- ✅ 输入框带占位符和计数器
- ✅ "取消" 按钮 - iOS蓝色 `#007AFF` 文字
- ✅ "生成" 按钮 - iOS蓝色填充背景 (`@drawable/bg_ios_button_primary`)

## 修改的文件

| 文件路径 | 修改类型 | 说明 |
|---------|---------|------|
| `presentation/src/main/res/layout/floating_result_card.xml` | 回退 | 恢复MaterialButton |
| `app/src/main/res/layout/floating_result_card.xml` | 回退 | 恢复MaterialButton |
| `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/floating/ResultCard.kt` | 回退 | 恢复MaterialButton类型 |

## 设计决策

### 悬浮窗主界面（ResultCard）
- 保持 **Material Design 3** 风格
- 使用 `MaterialButton` 组件
- 按钮高度 36dp，带图标

### 重新生成弹窗（RefinementOverlay）
- 使用 **iOS风格** 设计
- 使用 `TextView` 作为按钮
- 按钮高度 44dp，iOS蓝色 `#007AFF`

## 参考资料

- iOS风格对话框参考：`IOSAlertDialog.kt`
- iOS蓝色：`#007AFF`
- iOS按钮高度：44dp
- iOS圆角：14dp（对话框）/ 10dp（按钮）
