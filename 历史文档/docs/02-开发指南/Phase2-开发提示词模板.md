# Phase 2 开发提示词模板

**文档版本**: v1.0  
**创建日期**: 2025-12-05  
**用途**: 为AI助手提供清晰的Phase 2开发指令

---

## 📋 给AI助手的提示词

### 基础版提示词 (推荐)

```
你好！我需要继续开发共情AI助手项目的UI层。

## 当前状态
- ✅ Phase 1 (基础设施) 已完成
- 主题系统、导航系统、MainActivity集成都已就绪
- 现在需要开始 Phase 2: 可复用组件阶段

## 任务要求
请根据以下文档开发Phase 2的可复用组件：
1. 阅读 `docs/01-架构设计/UI层/Phase2-可复用组件阶段.md`
2. 阅读 `docs/01-架构设计/UI层/Phase2-可复用组件阶段-续.md`
3. 阅读 `docs/02-开发指南/UI层开发总体协调计划.md`

## 开发规范
- 所有文档和回答使用中文
- 代码注释、变量名、类名使用英文
- 遵循项目的Clean Architecture + MVVM架构
- 使用Jetpack Compose和Material Design 3
- 每个组件必须有@Preview
- 支持深色模式

## 优先级
按以下顺序开发P0组件：
1. LoadingIndicator (加载指示器)
2. ErrorView (错误视图)
3. EmptyView (空状态视图)
4. CustomTextField (输入框)
5. PrimaryButton (主按钮)
6. ContactListItem (联系人列表项)
7. AnalysisCard (分析结果卡片)

## 开发流程
1. 创建组件目录结构
2. 逐个开发组件，每个组件完成后使用getDiagnostics检查
3. 每完成一个组件，更新每日检查清单
4. 完成Phase 2后，更新CLAUDE.md和OVERVIEW.md

请开始Phase 2的开发工作。
```

---

### 详细版提示词 (包含更多上下文)

```
你好！我需要继续开发共情AI助手Android项目的UI层Phase 2阶段。

## 📊 项目背景

### 项目信息
- **项目名称**: 共情AI助手 (Empathy AI Assistant)
- **技术栈**: Kotlin + Jetpack Compose + Room + Retrofit + Hilt
- **架构**: Clean Architecture + MVVM
- **当前版本**: v1.0.2-dev
- **整体完成度**: 82%

### 已完成工作
✅ Domain Layer (100%) - 业务逻辑层
✅ Data Layer (100%) - 数据访问层
✅ Presentation - ViewModel (100%) - 状态管理
✅ Presentation - Phase 1 (100%) - 基础设施
  - Material Design 3 主题系统
  - 类型安全的导航系统
  - MainActivity集成

### 当前任务
🔄 Presentation - Phase 2 (0%) - 可复用组件阶段

---

## 🎯 Phase 2 开发目标

构建一套完整的可复用UI组件库，为Phase 3的页面开发提供"积木"。

### 核心交付物 (按优先级排序)

#### P0组件 (必须完成)
1. **LoadingIndicator** - 加载指示器
   - 支持不同大小 (Small/Medium/Large)
   - 支持自定义颜色
   - 有加载文本提示

2. **ErrorView** - 错误视图
   - 显示错误图标和消息
   - 支持重试按钮
   - 支持自定义错误类型

3. **EmptyView** - 空状态视图
   - 显示空状态图标和提示
   - 支持自定义文案
   - 支持操作按钮

4. **CustomTextField** - 自定义输入框
   - 支持标签和占位符
   - 支持错误状态和提示
   - 支持前缀/后缀图标
   - 支持多行输入

5. **PrimaryButton** - 主按钮
   - 支持加载状态
   - 支持禁用状态
   - 支持图标
   - 支持不同尺寸

6. **ContactListItem** - 联系人列表项
   - 显示头像、姓名、关系
   - 支持点击和长按
   - 显示标签数量

7. **AnalysisCard** - AI分析结果卡片
   - 显示分析标题和内容
   - 支持展开/收起
   - 支持复制功能

#### P1组件 (建议完成)
8. **SecondaryButton** - 次要按钮
9. **TagChip** - 标签芯片
10. **ProfileCard** - 档案卡片

---

## 📁 目录结构

请创建以下目录结构：

```
app/src/main/java/com/empathy/ai/presentation/ui/component/
├── button/
│   ├── PrimaryButton.kt
│   └── SecondaryButton.kt
├── input/
│   └── CustomTextField.kt
├── state/
│   ├── LoadingIndicator.kt
│   ├── ErrorView.kt
│   └── EmptyView.kt
├── list/
│   └── ContactListItem.kt
├── card/
│   ├── AnalysisCard.kt
│   └── ProfileCard.kt
└── chip/
    └── TagChip.kt
```

---

## 📖 必读文档

### 核心文档 (必须阅读)
1. **Phase 2 设计文档**
   - `docs/01-架构设计/UI层/Phase2-可复用组件阶段.md`
   - `docs/01-架构设计/UI层/Phase2-可复用组件阶段-续.md`

2. **总体协调计划**
   - `docs/02-开发指南/UI层开发总体协调计划.md`
   - `docs/02-开发指南/UI层开发总体协调计划-续.md`

3. **开发规范**
   - `.kiro/steering/structure.md` - 项目结构规范
   - `.kiro/steering/tech.md` - 技术栈说明
   - `.kiro/steering/product.md` - 产品概览

### 参考文档
4. **已完成工作**
   - `docs/02-开发指南/Phase1-基础设施完成总结.md`
   - `CLAUDE.md` - 项目整体状态
   - `docs/00-项目概述/OVERVIEW.md` - 项目概览

5. **主题系统参考**
   - `app/src/main/java/com/empathy/ai/presentation/theme/Color.kt`
   - `app/src/main/java/com/empathy/ai/presentation/theme/Theme.kt`

---

## 🎨 设计规范

### Compose组件规范

每个组件必须包含：

1. **KDoc注释**
```kotlin
/**
 * 主按钮组件
 * 
 * 用于主要操作，如提交、确认等
 * 
 * @param text 按钮文本
 * @param onClick 点击回调
 * @param modifier 修饰符
 * @param enabled 是否启用
 * @param loading 是否显示加载状态
 */
@Composable
fun PrimaryButton(...)
```

2. **参数设计**
- 必需参数在前
- 可选参数在后，提供合理默认值
- modifier参数放在可选参数第一位

3. **Preview示例**
```kotlin
@Preview(name = "默认状态", showBackground = true)
@Preview(name = "深色模式", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrimaryButtonPreview() {
    EmpathyTheme {
        PrimaryButton(
            text = "确认",
            onClick = {}
        )
    }
}
```

4. **主题使用**
- 使用 `MaterialTheme.colorScheme` 获取颜色
- 使用 `MaterialTheme.typography` 获取字体
- 使用 `MaterialTheme.shapes` 获取形状

---

## ✅ 开发流程

### Step 1: 创建目录结构
```bash
mkdir -p app/src/main/java/com/empathy/ai/presentation/ui/component/{button,input,state,list,card,chip}
```

### Step 2: 按优先级开发组件

对于每个组件：

1. **创建文件**
   - 在对应目录下创建Kotlin文件

2. **实现组件**
   - 编写Composable函数
   - 添加完整的KDoc注释
   - 实现所有必需功能

3. **添加Preview**
   - 至少2个Preview (浅色/深色)
   - 展示不同状态

4. **代码检查**
   - 使用 `getDiagnostics` 检查代码
   - 确保无错误和警告

5. **更新文档**
   - 更新每日检查清单
   - 记录完成情况

### Step 3: Phase 2完成后

1. **更新进度文档**
   - 更新 `CLAUDE.md`
   - 更新 `docs/00-项目概述/OVERVIEW.md`
   - 更新 `docs/02-开发指南/UI层开发每日检查清单.md`

2. **创建完成总结**
   - 创建 `docs/02-开发指南/Phase2-可复用组件完成总结.md`

3. **验收检查**
   - 所有P0组件完成
   - 所有组件有Preview
   - 支持深色模式
   - 代码通过诊断

---

## 🚨 注意事项

### 必须遵守的规则

1. **语言规范**
   - ✅ 所有文档和回答使用中文
   - ✅ 代码注释、变量名、类名使用英文
   - ✅ KDoc注释使用中文

2. **代码规范**
   - ✅ 使用 `data object` 而不是 `object`
   - ✅ Composable函数使用PascalCase命名
   - ✅ 参数使用camelCase命名
   - ✅ 常量使用UPPER_SNAKE_CASE命名

3. **主题规范**
   - ✅ 使用 `MaterialTheme.colorScheme` 而不是硬编码颜色
   - ✅ 支持浅色和深色模式
   - ✅ 使用 `EmpathyTheme` 包裹Preview

4. **质量规范**
   - ✅ 每个组件必须有@Preview
   - ✅ 每个组件必须有KDoc注释
   - ✅ 使用getDiagnostics检查代码
   - ✅ 无编译错误和警告

### 常见错误避免

❌ **错误示例**:
```kotlin
// 硬编码颜色
Text(text = "Hello", color = Color(0xFF6750A4))

// 缺少Preview
@Composable
fun MyButton() { ... }

// 缺少KDoc
@Composable
fun MyButton() { ... }
```

✅ **正确示例**:
```kotlin
/**
 * 自定义按钮
 */
@Composable
fun MyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(text = text)
    }
}

@Preview(showBackground = true)
@Composable
private fun MyButtonPreview() {
    EmpathyTheme {
        MyButton(text = "点击", onClick = {})
    }
}
```

---

## 📊 进度跟踪

### 每完成一个组件，更新以下内容：

1. **每日检查清单**
   - 文件: `docs/02-开发指南/UI层开发每日检查清单.md`
   - 勾选对应的完成项

2. **开发日志**
   - 在检查清单中记录完成时间
   - 记录遇到的问题和解决方案

3. **进度统计**
   - 更新完成百分比
   - 评估是否符合预期

---

## 🎯 验收标准

Phase 2完成的标准：

### 功能完整性
- [ ] 所有P0组件(7个)全部完成
- [ ] 至少完成80%的P1组件
- [ ] 每个组件功能正常

### 代码质量
- [ ] 所有组件有完整的KDoc注释
- [ ] 所有组件有至少2个Preview
- [ ] 所有组件支持深色模式
- [ ] 所有文件通过getDiagnostics检查
- [ ] 无编译错误和警告

### 文档完整性
- [ ] 更新了CLAUDE.md
- [ ] 更新了OVERVIEW.md
- [ ] 更新了每日检查清单
- [ ] 创建了Phase2完成总结

---

## 🚀 开始开发

请按照以下步骤开始：

1. **确认理解**
   - 阅读上述所有内容
   - 确认已理解开发要求

2. **阅读文档**
   - 阅读Phase2设计文档
   - 阅读总体协调计划

3. **创建目录**
   - 创建component目录结构

4. **开始开发**
   - 从LoadingIndicator开始
   - 按优先级逐个完成

5. **持续更新**
   - 每完成一个组件更新文档
   - 遇到问题及时记录

---

## 💡 提示

- 如果遇到技术问题，可以参考已完成的主题系统代码
- 如果不确定设计，可以参考Material Design 3官方文档
- 保持代码简洁，遵循KISS原则
- 优先完成功能，后续可以优化

---

**准备好了吗？让我们开始Phase 2的开发吧！**
```

---

## 🔄 快速启动版提示词 (极简版)

如果AI助手已经熟悉项目，可以使用这个极简版：

```
继续开发共情AI助手项目。

当前状态: Phase 1完成，需要开发Phase 2可复用组件。

任务:
1. 阅读 docs/01-架构设计/UI层/Phase2-可复用组件阶段.md
2. 创建 presentation/ui/component/ 目录结构
3. 按优先级开发P0组件: LoadingIndicator, ErrorView, EmptyView, CustomTextField, PrimaryButton, ContactListItem, AnalysisCard
4. 每个组件必须有@Preview和KDoc
5. 完成后更新CLAUDE.md和每日检查清单

开始吧！
```

---

## 📝 使用说明

### 选择合适的提示词版本

1. **基础版** - 适合对项目有一定了解的AI
2. **详细版** - 适合首次接手项目的AI，包含完整上下文
3. **快速启动版** - 适合已经熟悉项目的AI，快速继续开发

### 提示词使用技巧

1. **复制粘贴**: 直接复制对应版本的提示词
2. **适当调整**: 根据实际情况调整优先级或要求
3. **分步执行**: 如果任务量大，可以分多次对话完成
4. **持续反馈**: 在开发过程中提供反馈和调整

### 常见问题

**Q: AI没有按照要求开发怎么办？**
A: 重新强调关键要求，特别是"必须遵守的规则"部分

**Q: AI跳过了某些步骤怎么办？**
A: 明确指出缺失的步骤，要求补充完成

**Q: 如何确保代码质量？**
A: 每完成一个组件就要求使用getDiagnostics检查

---

**文档版本**: v1.0  
**创建日期**: 2025-12-05  
**维护者**: AI Assistant  
**适用场景**: Phase 2开发交接
