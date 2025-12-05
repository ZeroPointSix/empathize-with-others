# UI层开发实施计划

## 项目概述

基于用户确认的决策，制定详细的UI层开发实施计划，确保在3周内完成MVP版本的UI层开发。

## 当前状态评估

### ✅ 已完成部分
- **ViewModel架构**: 100%完成 (ChatViewModel, ContactListViewModel, ContactDetailViewModel)
- **UiState/UiEvent**: 100%完成 (符合UI层开发规范)
- **主题系统**: 60%完成 (Theme.kt, Type.kt已完成，Color.kt需完善深色模式)

### ❌ 待完成部分
- **UI组件**: 0%完成 (Screen组件、通用组件)
- **导航系统**: 0%完成 (NavRoutes, NavGraph)
- **测试覆盖**: 0%完成 (UI层ViewModel测试)

### 🚨 需修复问题
- **P0问题**: BrainTagRepositoryImpl异常处理缺失
- **P1问题**: ViewModel直接依赖Repository (违反Clean Architecture)

## 详细实施计划

### Week 1: 问题修复与基础准备 (5天)

#### Day 1: P0问题修复
**目标**: 确保系统稳定性
- [ ] 修复BrainTagRepositoryImpl异常处理 (2小时)
  - 添加try-catch块处理数据库操作异常
  - 实现优雅降级机制
- [ ] FeedTextUseCase功能决策确认 (1小时)
  - MVP阶段暂不实现文本喂养功能
  - 创建占位符实现，返回空结果

#### Day 2: P1问题修复
**目标**: 修复架构违规问题
- [ ] 创建GetContactUseCase (2小时)
  - 实现联系人查询用例
  - 遵循单一职责原则
- [ ] 创建GetContactListUseCase (1小时)
  - 实现联系人列表查询用例
- [ ] 修改ChatViewModel依赖注入 (1小时)
  - 替换ContactRepository为GetContactUseCase
  - 更新相关方法调用

#### Day 3: 导航系统配置
**目标**: 建立页面导航框架
- [ ] 创建NavRoutes.kt (2小时)
  - 定义所有路由常量
  - 实现类型安全的路由参数传递
- [ ] 创建NavGraph.kt (3小时)
  - 配置Compose Navigation
  - 实现页面间导航逻辑
  - 添加嵌套导航支持

#### Day 4: 主题系统完善
**目标**: 完成UI主题配置
- [ ] 完善Color.kt深色模式 (2小时)
  - 添加深色主题颜色配置
  - 实现动态主题切换
- [ ] 创建Shape.kt (1小时)
  - 定义应用形状样式
  - 统一圆角、切割等设计

#### Day 5: 依赖配置优化
**目标**: 优化项目依赖配置
- [ ] 添加Coil图片加载库 (1小时)
  - 配置Coil Compose扩展
  - 实现图片缓存策略
- [ ] 更新gradle配置 (2小时)
  - 确保所有依赖版本兼容
  - 优化编译配置

### Week 2: 核心UI开发 (7天)

#### Day 6-7: ContactListScreen开发
**目标**: 实现联系人列表界面
- [ ] 创建ContactListScreen.kt (6小时)
  - 实现联系人列表展示
  - 添加搜索功能
  - 实现下拉刷新
- [ ] 创建ContactCard组件 (4小时)
  - 设计联系人卡片样式
  - 实现点击交互
  - 添加头像显示

#### Day 8-9: ContactDetailScreen开发
**目标**: 实现联系人详情编辑界面
- [ ] 创建ContactDetailScreen.kt (6小时)
  - 实现联系人信息展示
  - 添加编辑功能
  - 实现表单验证
- [ ] 创建FormField组件 (4小时)
  - 设计通用表单字段
  - 实现输入验证
  - 添加错误提示

#### Day 10-12: ChatScreen开发
**目标**: 实现聊天分析界面
- [ ] 创建ChatScreen.kt (8小时)
  - 实现聊天消息展示
  - 添加消息输入功能
  - 实现AI分析结果展示
- [ ] 创建MessageBubble组件 (4小时)
  - 设计消息气泡样式
  - 区分用户消息和AI回复
  - 添加时间戳显示

### Week 3: 完善与测试 (3天)

#### Day 13: 通用组件开发
**目标**: 创建可复用UI组件
- [ ] 创建LoadingIndicator组件 (1小时)
- [ ] 创建ErrorDialog组件 (1小时)
- [ ] 创建EmptyState组件 (1小时)
- [ ] 创建ConfirmationDialog组件 (1小时)

#### Day 14: ViewModel测试
**目标**: 确保ViewModel逻辑正确性
- [ ] 编写ChatViewModelTest (2小时)
- [ ] 编写ContactListViewModelTest (2小时)
- [ ] 编写ContactDetailViewModelTest (2小时)

#### Day 15: 集成测试与MVP交付
**目标**: 确保整体功能正常
- [ ] 端到端测试 (2小时)
- [ ] 性能优化 (1小时)
- [ ] MVP版本交付准备 (1小时)

## 技术实施细节

### 1. P0问题修复方案

```kotlin
// BrainTagRepositoryImpl.kt 修复示例
override suspend fun insertBrainTag(brainTag: BrainTag): Result<BrainTag> {
    return try {
        val entity = brainTag.toEntity()
        val id = brainTagDao.insertBrainTag(entity)
        Result.success(brainTag.copy(id = id))
    } catch (e: Exception) {
        Result.failure(Exception("插入脑标签失败", e))
    }
}
```

### 2. UseCase创建示例

```kotlin
// GetContactUseCase.kt
@Singleton
class GetContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(id: Long): Result<ContactProfile> {
        return contactRepository.getContactById(id)
    }
}
```

### 3. 导航配置示例

```kotlin
// NavRoutes.kt
sealed class NavRoutes(val route: String) {
    object ContactList : NavRoutes("contact_list")
    object ContactDetail : NavRoutes("contact_detail/{contactId}") {
        fun createRoute(contactId: Long) = "contact_detail/$contactId"
    }
    object Chat : NavRoutes("chat/{contactId}") {
        fun createRoute(contactId: Long) = "chat/$contactId"
    }
}
```

## 风险评估与应对

### 高风险项
1. **导航复杂性**: 嵌套导航可能增加复杂度
   - 应对: 采用渐进式开发，先实现基础导航

2. **性能问题**: Compose重组可能影响性能
   - 应对: 使用remember和derivedStateOf优化重组

### 中风险项
1. **UI一致性**: 多个Screen可能风格不统一
   - 应对: 严格遵循设计系统和主题配置

2. **测试覆盖**: 时间紧张可能导致测试不足
   - 应对: 优先测试核心ViewModel逻辑

## 质量保证措施

### 代码质量
- 遵循UI层开发规范
- 使用Kotlin编码规范
- 实施Code Review流程

### 测试策略
- ViewModel单元测试覆盖率 > 80%
- UI组件测试覆盖率 > 60%
- 端到端测试覆盖核心用户流程

### 性能监控
- 使用Compose编译器报告优化重组
- 监控应用启动时间
- 检查内存使用情况

## 交付标准

### MVP功能清单
- [ ] 联系人列表展示与搜索
- [ ] 联系人详情查看与编辑
- [ ] 聊天分析功能
- [ ] 基础导航功能
- [ ] 深色模式支持

### 质量标准
- [ ] 所有P0/P1问题修复
- [ ] ViewModel测试覆盖率 > 80%
- [ ] 无明显性能问题
- [ ] 遵循UI层开发规范

## 后续规划

### MVP+ 功能 (v1.1)
- 联系人分组功能
- 聊天历史记录
- 导出分析报告
- 多语言支持

### 长期规划 (v2.0)
- FeedTextUseCase文本喂养功能
- 高级分析算法
- 云端同步
- 团队协作功能

## 总结

本实施计划基于3周(15个工作日)的时间框架，采用渐进式开发策略，确保每个阶段都有可交付的成果。通过优先修复关键问题，然后逐步构建UI组件，最后进行测试优化，确保MVP版本的按时高质量交付。

**预计交付日期**: 2025-12-18
**项目成功率**: 85%
**关键成功因素**: 严格按照计划执行，及时解决技术问题