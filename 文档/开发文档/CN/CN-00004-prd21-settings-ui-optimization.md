# PRD-00021 设置界面UI优化 - 工作交接文档

> **文档编号**: CN-00004
> **创建日期**: 2025-12-26
> **Slug**: prd21-settings-ui-optimization
> **状态**: PRD文档和设计稿已完成，待开发

---

<plan>

## 1. 主要请求和意图

用户要求创建PRD-00021，优化设置界面中三个关键页面的UI设计：

1. **AI配置页面** - 当前使用Material Design 3默认样式，需重构为iOS风格
2. **添加服务商页面** - 表单布局简陋，需优化为iOS原生精致感
3. **个人画像页面** - 界面不够美观，标签管理交互需优化

用户提供了原始HTML设计稿（`HTML/设置/`目录），要求：
- 基于设计稿创建优化版UI原型
- 编写完整的PRD文档
- 后续代码实现必须严格参考设计稿
- 高级选项（Temperature、最大Token数等）作为后续优化方向预留

---

## 2. 关键技术概念

- **iOS Inset Grouped风格** - 分组卡片样式，卡片有边距和圆角
- **iOS系统色彩规范**：
  - `#F2F2F7` - 页面背景
  - `#007AFF` - iOS蓝（主操作）
  - `#34C759` - iOS绿（成功/开关）
  - `#FF3B30` - iOS红（删除/警告）
  - `#5856D6` - iOS紫（次要功能）
  - `#8E8E93` - 次要文字
- **Large Title导航栏** - 34sp粗体大标题
- **Material Symbols Outlined** - 图标库
- **Jetpack Compose** - Android UI框架
- **Clean Architecture** - 项目架构模式

---

## 3. 文件和代码部分

### PRD-00021-设置界面UI优化.md
- **位置**: `文档/开发文档/PRD/PRD-00021-设置界面UI优化.md`
- **重要性**: 核心PRD文档，定义了三个页面的UI优化需求
- **内容**: 
  - 设计规范（色彩、图标、间距）
  - 三个界面的设计要点和交互设计
  - 实施计划（Phase 1-3，共3.5天）
  - 后续优化方向（高级设置、高级选项、画像扩展）
  - 技术实现指南（组件架构、颜色常量、尺寸常量）
  - 验收标准和开发检查清单

### AI配置-iOS风格.html
- **位置**: `文档/开发文档/UI-原型/PRD21/AI配置-iOS风格.html`
- **重要性**: AI配置页面的设计稿，代码实现必须参考
- **关键设计**:
  - 大标题导航栏（34sp粗体）
  - iOS风格搜索栏（圆角10dp，灰色背景#E3E3E8）
  - 推理引擎列表（Inset Grouped卡片，40x40图标）
  - 通用选项区域（网络代理、用量统计）
  - 高级设置区域（请求超时、最大Token数）

### 添加服务商-iOS风格.html
- **位置**: `文档/开发文档/UI-原型/PRD21/添加服务商-iOS风格.html`
- **重要性**: 添加服务商页面的设计稿
- **关键设计**:
  - 标准iOS导航栏（取消/完成按钮）
  - 基础信息表单（名称、端点、密钥）
  - 密钥显示/隐藏切换
  - 测试连接按钮（带动画效果）
  - 模型列表（自动获取、手动添加、默认标记、拖拽手柄）
  - 高级选项（Temperature、最大Token数）

### 个人画像-iOS风格.html
- **位置**: `文档/开发文档/UI-原型/PRD21/个人画像-iOS风格.html`
- **重要性**: 个人画像页面的设计稿
- **关键设计**:
  - 画像完整度卡片（进度条+百分比）
  - 标签页切换（基础信息/自定义维度，下划线指示器）
  - 维度卡片（性格特点、价值观、兴趣爱好、沟通风格、社交偏好）
  - 可展开/收起动画
  - 已添加标签（可编辑）
  - 快速选择预设标签

### index.html
- **位置**: `文档/开发文档/UI-原型/PRD21/index.html`
- **重要性**: 设计稿索引页，方便预览所有设计

---

## 4. 问题解决

### 已解决
1. **设计风格统一** - 确定采用iOS原生Inset Grouped风格
2. **色彩规范** - 定义了完整的iOS系统色彩常量
3. **高级功能预留** - 将Temperature、最大Token数等作为后续优化方向
4. **文档完整性** - PRD文档包含技术实现指南、验收标准、开发检查清单

### 待解决（开发阶段）
1. 创建iOS风格Compose组件（IOSTopAppBar、IOSSearchBar等）
2. 重构现有AiConfigScreen、ProviderFormDialog、UserProfileScreen
3. 实现左滑编辑/删除功能
4. 实现标签页切换动画

---

## 5. 待处理任务

### Phase 1: AI配置页面优化（1天）
- T1-01: 创建IOSTopAppBar组件
- T1-02: 创建IOSSearchBar组件
- T1-03: 重构ProviderCard为iOS风格
- T1-04: 更新AiConfigScreen布局
- T1-05: 添加左滑编辑/删除功能
- T1-06: 添加高级设置区域（可选）

### Phase 2: 添加服务商页面优化（1天）
- T2-01: 创建IOSFormField组件
- T2-02: 重构ProviderFormDialog为全屏页面
- T2-03: 优化模型列表UI
- T2-04: 添加测试连接动画效果
- T2-05: 添加高级选项区域（可选）
- T2-06: 添加自动获取模型列表功能（可选）

### Phase 3: 个人画像页面优化（1.5天）
- T3-01: 创建ProfileCompletionCard组件
- T3-02: 创建DimensionCard组件
- T3-03: 创建EditableTag组件
- T3-04: 创建QuickSelectTags组件
- T3-05: 重构UserProfileScreen
- T3-06: 添加标签页切换动画

---

## 6. 当前工作

本次会话完成了PRD-00021的全部文档工作：

1. **创建了4个HTML设计稿**（位于`文档/开发文档/UI-原型/PRD21/`）
2. **编写了完整的PRD文档**（v1.2版本），包含：
   - 设计规范
   - 三个界面的详细设计要点
   - 实施计划
   - 后续优化方向
   - 技术实现指南（组件架构、颜色/尺寸常量、组件示例代码）
   - 验收标准（视觉、交互、功能、技术、测试）
   - 开发检查清单
   - 完整附录

**文档状态**: ✅ 设计确认完成，待开发

---

## 7. 下一步

开始Phase 1的开发工作：

1. 在`presentation/ui/component/ios/`目录下创建iOS风格组件
2. 首先创建基础组件：
   - `IOSColors.kt` - 颜色常量
   - `IOSDimensions.kt` - 尺寸常量
   - `IOSInsetGroupedCard.kt` - 卡片容器
   - `IOSSettingsItem.kt` - 设置项组件
3. 然后重构`AiConfigScreen.kt`

**注意**: 开发前请先在浏览器中打开`文档/开发文档/UI-原型/PRD21/index.html`预览设计稿效果。

</plan>

---

## 快速参考

### 设计稿位置
```
文档/开发文档/UI-原型/PRD21/
├── index.html                    # 设计稿索引
├── AI配置-iOS风格.html           # AI配置页面
├── 添加服务商-iOS风格.html       # 添加服务商页面
└── 个人画像-iOS风格.html         # 个人画像页面
```

### PRD文档位置
```
文档/开发文档/PRD/PRD-00021-设置界面UI优化.md
```

### 需要重构的现有文件
```
presentation/src/main/kotlin/com/empathy/ai/presentation/
├── ui/screen/aiconfig/AiConfigScreen.kt
├── ui/component/dialog/ProviderFormDialog.kt
├── ui/component/card/ProviderCard.kt
└── ui/screen/userprofile/UserProfileScreen.kt
```

### 需要新建的组件目录
```
presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/ios/
```

---

**使用方法**: 在新会话中使用 `/pickup 文档/开发文档/CN/CN-00004-prd21-settings-ui-optimization.md` 继续工作。
