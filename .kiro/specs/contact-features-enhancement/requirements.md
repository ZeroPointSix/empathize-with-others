# 需求文档 - 联系人功能增强

## 简介

本文档定义了共情 AI 助手应用中三个关键功能的需求：联系人详情页的标签添加功能、联系人列表的搜索功能，以及脑标签管理页面的完整实现。这些功能是应用核心用户体验的重要组成部分，需要确保用户能够高效地管理联系人信息和标签。

## 术语表

- **System**: 共情 AI 助手应用
- **User**: 使用应用的最终用户
- **Contact**: 联系人，存储在本地数据库中的联系人资料
- **BrainTag**: 脑标签，用于标记联系人的雷区（RISK_RED）或策略（STRATEGY_GREEN）
- **ContactDetailScreen**: 联系人详情页面
- **ContactListScreen**: 联系人列表页面
- **BrainTagScreen**: 脑标签管理页面
- **TagType**: 标签类型枚举，包含 RISK_RED（雷区）和 STRATEGY_GREEN（策略）
- **ViewModel**: 视图模型，负责管理 UI 状态和业务逻辑
- **Repository**: 仓库，负责数据访问
- **UI State**: UI 状态，使用 StateFlow 管理的不可变状态对象

## 需求

### 需求 1：联系人详情页标签添加功能

**用户故事**：作为用户，我想在联系人详情页添加脑标签，以便记录该联系人的雷区和沟通策略。

#### 验收标准

1. WHEN 用户在联系人详情页点击"添加标签"按钮 THEN THE System SHALL 显示标签添加对话框
2. WHEN 用户在标签添加对话框中输入标签内容并选择标签类型 THEN THE System SHALL 验证标签内容不为空
3. WHEN 用户确认添加标签且标签内容有效 THEN THE System SHALL 将标签保存到数据库并更新 UI 显示
4. WHEN 标签保存失败 THEN THE System SHALL 显示错误提示信息
5. WHEN 用户取消添加标签 THEN THE System SHALL 关闭对话框并清空输入内容
6. WHEN 标签添加成功 THEN THE System SHALL 在标签列表中立即显示新添加的标签
7. WHEN 用户在编辑模式下添加标签 THEN THE System SHALL 将标签添加到临时状态，直到用户保存联系人信息

### 需求 2：联系人列表搜索功能

**用户故事**：作为用户，我想在联系人列表中搜索联系人，以便快速找到特定的联系人。

#### 验收标准

1. WHEN 用户点击联系人列表页的搜索图标 THEN THE System SHALL 显示搜索输入框
2. WHEN 用户在搜索框中输入文本 THEN THE System SHALL 实时过滤联系人列表
3. WHEN 搜索文本匹配联系人姓名 THEN THE System SHALL 在过滤结果中显示该联系人
4. WHEN 搜索文本匹配联系人目标 THEN THE System SHALL 在过滤结果中显示该联系人
5. WHEN 搜索文本匹配联系人事实信息 THEN THE System SHALL 在过滤结果中显示该联系人
6. WHEN 搜索结果为空 THEN THE System SHALL 显示"未找到匹配的联系人"提示
7. WHEN 用户清空搜索文本 THEN THE System SHALL 显示所有联系人
8. WHEN 用户关闭搜索模式 THEN THE System SHALL 隐藏搜索框并显示所有联系人
9. WHEN 搜索过程中 THEN THE System SHALL 保持搜索操作的响应性，不阻塞 UI

### 需求 3：脑标签管理页面功能

**用户故事**：作为用户，我想在脑标签管理页面查看和管理所有标签，以便统一管理跨联系人的标签信息。

#### 验收标准

1. WHEN 用户进入脑标签管理页面 THEN THE System SHALL 加载并显示所有标签
2. WHEN 标签列表为空 THEN THE System SHALL 显示空状态提示和添加标签按钮
3. WHEN 用户点击添加标签按钮 THEN THE System SHALL 显示标签添加对话框
4. WHEN 用户在对话框中输入标签内容并选择类型 THEN THE System SHALL 验证输入有效性
5. WHEN 用户确认添加标签 THEN THE System SHALL 保存标签到数据库并更新列表显示
6. WHEN 用户点击标签的删除按钮 THEN THE System SHALL 显示删除确认对话框
7. WHEN 用户确认删除标签 THEN THE System SHALL 从数据库删除标签并更新列表显示
8. WHEN 标签按类型分组显示 THEN THE System SHALL 将雷区标签和策略标签分别显示
9. WHEN 用户点击搜索图标 THEN THE System SHALL 显示搜索输入框
10. WHEN 用户在搜索框中输入文本 THEN THE System SHALL 实时过滤标签列表
11. WHEN 搜索文本匹配标签内容 THEN THE System SHALL 在过滤结果中显示该标签
12. WHEN 标签操作失败 THEN THE System SHALL 显示错误提示信息

### 需求 4：数据一致性和状态管理

**用户故事**：作为开发者，我想确保标签数据在不同页面间保持一致，以便用户获得可靠的体验。

#### 验收标准

1. WHEN 用户在联系人详情页添加标签 THEN THE System SHALL 确保标签与联系人正确关联
2. WHEN 用户在脑标签管理页面删除标签 THEN THE System SHALL 从所有关联的联系人中移除该标签
3. WHEN 标签数据发生变化 THEN THE System SHALL 使用 Flow 自动更新所有订阅该数据的 UI
4. WHEN 用户在编辑模式下修改联系人信息 THEN THE System SHALL 在用户保存前不持久化标签更改
5. WHEN 用户取消编辑 THEN THE System SHALL 恢复标签到原始状态
6. WHEN 多个操作同时进行 THEN THE System SHALL 使用协程确保操作的线程安全性

### 需求 5：用户体验和性能

**用户故事**：作为用户，我想要流畅的交互体验，以便高效地使用应用功能。

#### 验收标准

1. WHEN 用户执行任何标签操作 THEN THE System SHALL 在 100 毫秒内响应用户输入
2. WHEN 数据加载时 THEN THE System SHALL 显示加载指示器
3. WHEN 操作成功完成 THEN THE System SHALL 提供视觉反馈（如动画或提示）
4. WHEN 操作失败 THEN THE System SHALL 显示清晰的错误信息和恢复建议
5. WHEN 用户输入无效数据 THEN THE System SHALL 实时显示验证错误信息
6. WHEN 搜索大量联系人或标签 THEN THE System SHALL 保持 UI 流畅性，不出现卡顿
7. WHEN 用户在不同页面间导航 THEN THE System SHALL 保持导航的流畅性和一致性
