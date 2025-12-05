# Phase 4 开发提示词模板

**文档版本**: v1.0  
**创建日期**: 2025-12-05  
**用途**: 为AI助手提供清晰的Phase 4开发指令

---

## 📋 给AI助手的提示词

### 基础版提示词 (推荐)

```
你好！我需要继续开发共情AI助手项目的UI层Phase 4阶段。

## 当前状态
- ✅ Phase 1 (基础设施) 已完成
- ✅ Phase 2 (可复用组件) 已完成
- ✅ Phase 3 (核心Screen) 已完成
- ✅ Phase 3 Bug修复已完成
- 主题系统、导航系统、10个可复用组件、4个核心Screen都已就绪
- 现在需要开始 Phase 4: MainActivity集成与测试优化阶段

## 任务要求
请根据以下文档完成Phase 4的开发任务：
1. 阅读 `docs/02-开发指南/Phase3-核心Screen完成总结.md`
2. 阅读 `docs/02-开发指南/Phase3-Bug修复完成报告.md`
3. 阅读 `docs/02-开发指南/UI层开发总体协调计划.md`

## 开发规范
- 所有文档和回答使用中文
- 代码注释、变量名、类名使用英文
- 遵循项目的Clean Architecture + MVVM架构
- 使用Jetpack Compose和Material Design 3
- 使用Hilt依赖注入
- 所有修改后使用getDiagnostics检查代码

## Phase 4 核心任务

### 任务1: MainActivity集成 (P0)

**目标**: 将NavGraph集成到MainActivity，实现完整的应用导航

**文件**: `app/src/main/java/com/empathy/ai/presentation/ui/MainActivity.kt`

**要求**:
1. 使用Jetpack Compose的setContent
2. 应用EmpathyTheme主题
3. 创建NavController
4. 集成NavGraph
5. 设置起始页面为ContactListScreen
6. 支持系统返回键

**参考代码结构**:
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            EmpathyTheme {
                val navController = rememberNavController()
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph(navController = navController)
                }
            }
        }
    }
}
```

**验证步骤**:
1. 运行getDiagnostics检查无错误
2. 确认可以正常导航到各个Screen
3. 确认返回键功能正常
4. 确认主题正确应用

---

### 任务2: 导航流程测试 (P0)

**目标**: 验证所有导航路径正常工作

**测试场景**:

1. **联系人列表 → 联系人详情**
   - 点击联系人列表项
   - 验证跳转到详情页
   - 验证contactId正确传递
   - 验证返回按钮正常

2. **联系人列表 → 新建联系人**
   - 点击FAB按钮
   - 验证跳转到详情页（新建模式）
   - 验证contactId为空字符串
   - 验证返回按钮正常

3. **联系人详情 → 编辑 → 保存**
   - 进入详情页
   - 点击编辑按钮
   - 修改信息
   - 点击保存
   - 验证自动返回列表页

4. **联系人列表 → 聊天分析**
   - 点击联系人（如果有聊天入口）
   - 验证跳转到聊天页
   - 验证contactId正确传递
   - 验证返回按钮正常

5. **标签管理页面**
   - 导航到标签管理页
   - 验证标签列表显示
   - 验证添加/删除功能
   - 验证返回按钮正常

**验证清单**:
- [ ] 所有导航路径正常
- [ ] 参数传递正确
- [ ] 返回按钮功能正常
- [ ] 无导航错误或崩溃

---

### 任务3: 状态管理验证 (P1)

**目标**: 验证所有Screen的状态管理正确

**验证内容**:

1. **加载状态**
   - 验证LoadingIndicator正确显示
   - 验证加载完成后正确隐藏

2. **错误状态**
   - 验证ErrorView正确显示
   - 验证错误消息正确
   - 验证重试按钮功能

3. **空状态**
   - 验证EmptyView正确显示
   - 验证空状态提示正确
   - 验证操作按钮功能

4. **成功状态**
   - 验证数据正确显示
   - 验证交互功能正常

**验证清单**:
- [ ] ContactListScreen状态管理正确
- [ ] ContactDetailScreen状态管理正确
- [ ] ChatScreen状态管理正确
- [ ] BrainTagScreen状态管理正确

---

### 任务4: UI/UX优化 (P1)

**目标**: 优化用户体验和界面细节

**优化项**:

1. **动画效果**
   - 页面切换动画
   - 列表项动画
   - 对话框动画

2. **交互反馈**
   - 按钮点击反馈
   - 加载状态提示
   - 操作成功提示

3. **错误处理**
   - 友好的错误提示
   - 网络错误处理
   - 表单验证提示

4. **性能优化**
   - 列表滚动性能
   - 图片加载优化
   - 内存使用优化

**验证清单**:
- [ ] 动画流畅自然
- [ ] 交互反馈及时
- [ ] 错误提示友好
- [ ] 性能表现良好

---

### 任务5: 深色模式验证 (P1)

**目标**: 验证所有Screen在深色模式下正常显示

**验证内容**:
1. 切换到深色模式
2. 逐个验证所有Screen
3. 检查颜色对比度
4. 检查文字可读性
5. 检查图标清晰度

**验证清单**:
- [ ] ContactListScreen深色模式正常
- [ ] ContactDetailScreen深色模式正常
- [ ] ChatScreen深色模式正常
- [ ] BrainTagScreen深色模式正常
- [ ] 所有组件深色模式正常

---

### 任务6: 代码质量检查 (P0)

**目标**: 确保代码质量达标

**检查项**:

1. **编译检查**
   ```bash
   ./gradlew build
   ```
   - 验证无编译错误
   - 验证无编译警告

2. **Lint检查**
   ```bash
   ./gradlew lint
   ```
   - 修复所有Critical问题
   - 修复所有Error问题
   - 评估Warning问题

3. **代码规范检查**
   - 使用getDiagnostics检查所有文件
   - 验证命名规范
   - 验证注释完整性
   - 验证代码格式

**验证清单**:
- [ ] 编译无错误
- [ ] Lint无Critical/Error
- [ ] 代码规范符合要求
- [ ] 注释完整清晰

---

### 任务7: 文档更新 (P0)

**目标**: 更新项目文档

**更新内容**:

1. **CLAUDE.md**
   - 更新项目状态
   - 更新完成度
   - 更新Phase 4完成情况

2. **OVERVIEW.md**
   - 更新项目进度
   - 更新完成模块
   - 更新下一步计划

3. **Phase4完成总结.md**
   - 创建Phase 4完成总结文档
   - 记录完成的任务
   - 记录遇到的问题和解决方案
   - 记录优化建议

**文档清单**:
- [ ] CLAUDE.md已更新
- [ ] OVERVIEW.md已更新
- [ ] Phase4完成总结.md已创建
- [ ] 所有文档使用中文

---

## 开发流程

### Step 1: MainActivity集成

1. **修改MainActivity.kt**
   - 添加@AndroidEntryPoint注解
   - 使用setContent设置Compose内容
   - 应用EmpathyTheme主题
   - 创建NavController
   - 集成NavGraph

2. **验证**
   - 运行getDiagnostics检查
   - 尝试运行应用
   - 验证导航功能

---

### Step 2: 导航流程测试

1. **测试所有导航路径**
   - 联系人列表 → 详情
   - 联系人列表 → 新建
   - 联系人详情 → 编辑 → 保存
   - 聊天分析页面
   - 标签管理页面

2. **记录问题**
   - 记录发现的问题
   - 记录修复方案
   - 更新文档

---

### Step 3: 状态管理验证

1. **验证每个Screen的状态**
   - 加载状态
   - 错误状态
   - 空状态
   - 成功状态

2. **修复问题**
   - 修复状态显示问题
   - 优化状态切换
   - 验证修复效果

---

### Step 4: UI/UX优化

1. **添加动画效果**
   - 页面切换动画
   - 列表项动画
   - 对话框动画

2. **优化交互反馈**
   - 按钮点击反馈
   - 加载提示
   - 操作反馈

3. **优化错误处理**
   - 友好的错误提示
   - 重试机制
   - 表单验证

---

### Step 5: 质量检查

1. **运行编译检查**
   ```bash
   ./gradlew build
   ```

2. **运行Lint检查**
   ```bash
   ./gradlew lint
   ```

3. **使用getDiagnostics检查所有文件**

4. **修复所有问题**

---

### Step 6: 文档更新

1. **更新CLAUDE.md**
   - 更新项目状态为Phase 4完成
   - 更新完成度为95%+

2. **更新OVERVIEW.md**
   - 更新UI层完成度为100%
   - 更新整体进度

3. **创建Phase4完成总结.md**
   - 记录完成的任务
   - 记录遇到的问题
   - 记录优化建议

---

## 注意事项

### 必须遵守的规则

1. **语言规范**
   - ✅ 所有文档和回答使用中文
   - ✅ 代码注释、变量名、类名使用英文
   - ✅ KDoc注释使用中文

2. **代码规范**
   - ✅ 使用@AndroidEntryPoint注解
   - ✅ 使用rememberNavController()创建NavController
   - ✅ 使用EmpathyTheme包裹内容
   - ✅ 使用Surface设置背景色

3. **架构规范**
   - ✅ MainActivity只负责设置Compose内容
   - ✅ 导航逻辑在NavGraph中
   - ✅ 业务逻辑在ViewModel中
   - ✅ UI展示在Screen中

4. **质量规范**
   - ✅ 所有修改后使用getDiagnostics检查
   - ✅ 无编译错误和警告
   - ✅ 代码格式规范
   - ✅ 注释完整清晰

### 常见错误避免

❌ **错误示例**:
```kotlin
// 没有使用@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // ...
}

// 没有应用主题
setContent {
    NavGraph(navController = navController)
}

// 没有设置背景色
setContent {
    EmpathyTheme {
        NavGraph(navController = navController)
    }
}
```

✅ **正确示例**:
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            EmpathyTheme {
                val navController = rememberNavController()
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph(navController = navController)
                }
            }
        }
    }
}
```

---

## 验收标准

Phase 4完成的标准：

### 功能完整性
- [ ] MainActivity正确集成NavGraph
- [ ] 所有导航路径正常工作
- [ ] 所有Screen状态管理正确
- [ ] 深色模式正常显示
- [ ] 返回键功能正常

### 代码质量
- [ ] 编译无错误和警告
- [ ] Lint检查通过
- [ ] 代码规范符合要求
- [ ] 注释完整清晰

### 用户体验
- [ ] 动画流畅自然
- [ ] 交互反馈及时
- [ ] 错误提示友好
- [ ] 性能表现良好

### 文档完整性
- [ ] CLAUDE.md已更新
- [ ] OVERVIEW.md已更新
- [ ] Phase4完成总结.md已创建
- [ ] 所有文档使用中文

---

## 提示

- MainActivity的实现相对简单，主要是集成工作
- 重点在于测试和验证各个功能
- 发现问题及时记录和修复
- 保持代码简洁，遵循KISS原则
- 充分测试导航流程
- 验证所有状态管理正确

---

**准备好了吗？让我们开始Phase 4的开发吧！**
```

---

## 🔄 快速启动版提示词 (极简版)

如果AI助手已经熟悉项目，可以使用这个极简版：

```
继续开发共情AI助手项目。

当前状态: Phase 1、2、3完成，需要开发Phase 4 MainActivity集成与测试优化。

任务:
1. 修改MainActivity.kt集成NavGraph
2. 测试所有导航路径
3. 验证状态管理
4. 优化UI/UX
5. 运行质量检查
6. 更新项目文档

核心要求:
- 使用@AndroidEntryPoint注解
- 应用EmpathyTheme主题
- 使用rememberNavController()
- 所有修改后使用getDiagnostics检查
- 更新CLAUDE.md和OVERVIEW.md

开始吧！
```

---

## 📝 使用说明

### 选择合适的提示词版本

1. **基础版** - 适合首次接手Phase 4的AI，包含完整指导
2. **快速启动版** - 适合已经熟悉项目的AI，快速继续开发

### 提示词使用技巧

1. **复制粘贴**: 直接复制对应版本的提示词
2. **适当调整**: 根据实际情况调整任务优先级
3. **分步执行**: 可以分多次对话完成不同任务
4. **持续反馈**: 在开发过程中提供反馈和调整

### 常见问题

**Q: MainActivity集成失败怎么办？**
A: 检查是否添加@AndroidEntryPoint注解，是否正确导入NavGraph

**Q: 导航不工作怎么办？**
A: 检查NavRoutes定义是否正确，检查参数传递是否正确

**Q: 如何验证深色模式？**
A: 在设备设置中切换深色模式，或在Preview中使用uiMode参数

**Q: 如何优化性能？**
A: 使用Android Profiler分析性能，优化列表滚动，减少重组

---

## 🎯 Phase 4 vs Phase 3 的区别

### Phase 3 (核心Screen)
- 目标: 开发核心业务页面
- 特点: 创建新Screen，实现业务逻辑
- 示例: ContactListScreen、ChatScreen

### Phase 4 (集成与优化)
- 目标: 集成所有模块，优化体验
- 特点: MainActivity集成，测试验证，优化细节
- 示例: 导航集成、状态验证、UI优化

### 关键差异
- Phase 3是"建造"，Phase 4是"组装和打磨"
- Phase 3关注功能实现，Phase 4关注整体体验
- Phase 3创建新代码，Phase 4优化现有代码
- Phase 3独立开发，Phase 4集成测试

---

## 📊 预期成果

### 完成后的状态

1. **功能完整**: 所有核心功能可以正常使用
2. **导航流畅**: 所有页面可以正常跳转
3. **状态正确**: 所有状态管理正确
4. **体验良好**: UI/UX优化到位
5. **质量达标**: 代码质量符合要求
6. **文档完整**: 所有文档更新完成

### 项目完成度

- **UI层**: 100%
- **整体项目**: 95%+
- **MVP就绪**: ✅

---

**文档版本**: v1.0  
**创建日期**: 2025-12-05  
**维护者**: AI Assistant  
**适用场景**: Phase 4开发交接
