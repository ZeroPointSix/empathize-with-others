# UI层开发总体协调计划 - 附录

> 这是完整版文档的附录部分,包含性能指标和参考资料索引

---

## 5.3 性能基准 (续)

| 指标 | 目标值 | 测试方法 | 优先级 |
|------|--------|---------|--------|
| **冷启动时间** | < 3秒 | 物理设备测试 | P0 |
| **页面切换** | < 300ms | 手动操作感知 | P0 |
| **列表滚动** | FPS > 50 | 使用Profiler | P1 |
| **内存占用** | < 200MB | Android Studio Memory Profiler | P1 |
| **首屏渲染** | < 1秒 | 手动计时 | P0 |
| **API响应处理** | < 500ms | 网络调试 | P1 |

### 5.4 用户体验验收标准

#### 交互体验
- [ ] 所有按钮点击有视觉反馈(ripple效果)
- [ ] 所有输入框有焦点提示
- [ ] 页面切换有流畅的转场动画
- [ ] 下拉刷新有Loading动画
- [ ] 长按操作有上下文菜单

#### 反馈机制
- [ ] 操作成功有Toast或Snackbar提示
- [ ] 操作失败有明确的错误信息
- [ ] 网络请求中有Loading指示器
- [ ] 空状态有友好的提示文案
- [ ] 错误状态有重试按钮

#### 可访问性
- [ ] 所有图标有contentDescription
- [ ] 文字大小适中(至少16sp)
- [ ] 颜色对比度符合WCAG标准
- [ ] 支持TalkBack屏幕阅读器

---

## 9. 参考资料索引

### 9.1 架构设计文档

#### UI层设计文档
| 文档名称 | 路径 | 用途 |
|---------|------|------|
| **UI层全局设计** | `docs/01-架构设计/UI层/UI层全局设计.md` | 了解UI层整体架构 |
| **UI层开发规范** | `docs/01-架构设计/UI层/UI层开发规范.md` | 编码规范和最佳实践 |
| **第一模块-UI组件开发** | `docs/01-架构设计/UI层/第一模块-UI组件开发.md` | 组件开发详细指南 |

#### 4个阶段详细文档
| 阶段 | 文档路径 | 关键内容 |
|------|---------|---------|
| **Phase 1** | `docs/01-架构设计/UI层/Phase1-基础设施阶段.md` | 导航、主题、Hilt配置 |
| **Phase 1续** | `docs/01-架构设计/UI层/Phase1-基础设施阶段-续.md` | 补充实现细节 |
| **Phase 2** | `docs/01-架构设计/UI层/Phase2-可复用组件阶段.md` | 组件设计和实现 |
| **Phase 2续** | `docs/01-架构设计/UI层/Phase2-可复用组件阶段-续.md` | 组件变体和优化 |
| **Phase 3** | `docs/01-架构设计/UI层/Phase3-核心Screen阶段.md` | Screen实现详细步骤 |
| **Phase 4** | `docs/01-架构设计/UI层/Phase4-测试和优化阶段.md` | 测试策略和优化方法 |

#### 其他层文档
| 层 | 文档路径 | 参考价值 |
|---|---------|---------|
| **业务层** | `docs/01-架构设计/业务层/` | 了解Domain模型和UseCase |
| **数据层** | `docs/01-架构设计/数据层/` | 了解Repository实现 |
| **服务层** | `docs/01-架构设计/服务层/` | 了解业务规则和服务编排 |

### 9.2 开发指南文档

| 文档名称 | 路径 | 用途 |
|---------|------|------|
| **UI层开发路线图** | `docs/02-开发指南/UI层开发路线图.md` | 整体开发路径规划 |
| **依赖配置说明** | `docs/02-开发指南/依赖配置说明.md` | Gradle依赖配置参考 |
| **依赖快速参考** | `docs/02-开发指南/依赖快速参考.md` | 常用依赖版本查询 |
| **项目进度评估报告** | `docs/02-开发指南/项目进度评估报告-2025-12-05.md` | 当前进度和就绪度 |

### 9.3 测试文档

| 文档名称 | 路径 | 用途 |
|---------|------|------|
| **测试逻辑** | `docs/03-测试文档/测试逻辑.md` | 测试策略和方法 |
| **快速测试清单** | `docs/03-测试文档/快速测试清单.md` | 快速验证检查点 |
| **黑盒测试指南** | `docs/03-测试文档/黑盒测试指南.md` | 功能测试指南 |
| **UI层开发就绪度评估** | `docs/03-测试文档/UI层开发就绪度评估报告.md` | 开发前置条件检查 |

### 9.4 ViewModel和UseCase实现

#### ViewModel实现
| ViewModel | 文件路径 | 功能 |
|-----------|---------|------|
| **ContactListViewModel** | `app/src/main/java/com/empathy/ai/presentation/viewmodel/ContactListViewModel.kt` | 联系人列表状态管理 |
| **ContactDetailViewModel** | `app/src/main/java/com/empathy/ai/presentation/viewmodel/ContactDetailViewModel.kt` | 联系人详情状态管理 |
| **ChatViewModel** | `app/src/main/java/com/empathy/ai/presentation/viewmodel/ChatViewModel.kt` | 聊天分析状态管理 |

#### UiState定义
| UiState | 文件路径 | 说明 |
|---------|---------|------|
| **ContactListUiState** | `app/src/main/java/com/empathy/ai/presentation/ui/screen/contact/ContactListUiState.kt` | 列表页状态 |
| **ContactDetailUiState** | `app/src/main/java/com/empathy/ai/presentation/ui/screen/contact/ContactDetailUiState.kt` | 详情页状态 |
| **ChatUiState** | `app/src/main/java/com/empathy/ai/presentation/ui/screen/chat/ChatUiState.kt` | 聊天页状态 |

#### UiEvent定义
| UiEvent | 文件路径 | 说明 |
|---------|---------|------|
| **ContactListUiEvent** | `app/src/main/java/com/empathy/ai/presentation/ui/screen/contact/ContactListUiEvent.kt` | 列表页事件 |
| **ContactDetailUiEvent** | `app/src/main/java/com/empathy/ai/presentation/ui/screen/contact/ContactDetailUiEvent.kt` | 详情页事件 |
| **ChatUiEvent** | `app/src/main/java/com/empathy/ai/presentation/ui/screen/chat/ChatUiEvent.kt` | 聊天页事件 |

#### UseCase实现
| UseCase | 文件路径 | 功能 |
|---------|---------|------|
| **GetAllContactsUseCase** | `app/src/main/java/com/empathy/ai/domain/usecase/GetAllContactsUseCase.kt` | 获取所有联系人 |
| **GetContactUseCase** | `app/src/main/java/com/empathy/ai/domain/usecase/GetContactUseCase.kt` | 获取单个联系人 |
| **SaveProfileUseCase** | `app/src/main/java/com/empathy/ai/domain/usecase/SaveProfileUseCase.kt` | 保存档案 |
| **DeleteContactUseCase** | `app/src/main/java/com/empathy/ai/domain/usecase/DeleteContactUseCase.kt` | 删除联系人 |
| **AnalyzeChatUseCase** | `app/src/main/java/com/empathy/ai/domain/usecase/AnalyzeChatUseCase.kt` | AI聊天分析 |
| **CheckDraftUseCase** | `app/src/main/java/com/empathy/ai/domain/usecase/CheckDraftUseCase.kt` | 安全检查 |
| **GetBrainTagsUseCase** | `app/src/main/java/com/empathy/ai/domain/usecase/GetBrainTagsUseCase.kt` | 获取标签 |
| **SaveBrainTagUseCase** | `app/src/main/java/com/empathy/ai/domain/usecase/SaveBrainTagUseCase.kt` | 保存标签 |
| **DeleteBrainTagUseCase** | `app/src/main/java/com/empathy/ai/domain/usecase/DeleteBrainTagUseCase.kt` | 删除标签 |

### 9.5 现有主题和资源

| 资源类型 | 文件路径 | 说明 |
|---------|---------|------|
| **主题定义** | `app/src/main/java/com/empathy/ai/presentation/theme/Theme.kt` | Material3主题配置 |
| **字体定义** | `app/src/main/java/com/empathy/ai/presentation/theme/Type.kt` | 字体样式定义 |
| **字符串资源** | `app/src/main/res/values/strings.xml` | 应用内文本 |
| **颜色资源** | `app/src/main/res/values/colors.xml` | 颜色定义 |
| **MainActivity** | `app/src/main/java/com/empathy/ai/presentation/ui/MainActivity.kt` | 应用入口Activity |

### 9.6 外部参考资料

#### Jetpack Compose官方文档
- **Compose基础**: https://developer.android.com/jetpack/compose/tutorial
- **状态管理**: https://developer.android.com/jetpack/compose/state
- **导航**: https://developer.android.com/jetpack/compose/navigation
- **Material Design 3**: https://developer.android.com/jetpack/compose/designsystems/material3

#### 最佳实践
- **Compose性能优化**: https://developer.android.com/jetpack/compose/performance
- **Compose可访问性**: https://developer.android.com/jetpack/compose/accessibility
- **Compose测试**: https://developer.android.com/jetpack/compose/testing

#### Hilt依赖注入
- **Hilt官方文档**: https://developer.android.com/training/dependency-injection/hilt-android
- **Hilt ViewModel**: https://developer.android.com/training/dependency-injection/hilt-jetpack

---

## 10. 快速启动指南

### 10.1 第一天应该做什么?

**Day 1 推荐任务** (优先级排序):

1. **环境准备** (30分钟)
   - [ ] 确保Android Studio是最新版本
   - [ ] 同步Gradle依赖
   - [ ] 运行现有代码确认无错误

2. **创建Navigation图** (1-2小时)
   - [ ] 创建`res/navigation/nav_graph.xml`
   - [ ] 定义3个destination
   - [ ] 配置简单的导航动作

3. **设置主题系统** (2-3小时)
   - [ ] 检查现有的`Theme.kt`
   - [ ] 根据需要调整颜色
   - [ ] 确保深色模式支持

4. **配置MainActivity** (1小时)
   - [ ] 集成NavHost
   - [ ] 设置主题
   - [ ] 测试应用启动

5. **结束验证** (30分钟)
   - [ ] 应用可以启动
   - [ ] 可以看到导航结构
   - [ ] 主题颜色正确

### 10.2 遇到问题怎么办?

#### 常见问题和解决方案

| 问题 | 可能原因 | 解决方案 |
|------|---------|---------|
| **Hilt编译错误** | Kapt配置问题 | 检查`build.gradle.kts`中的kapt配置 |
| **Navigation找不到destination** | XML配置错误 | 检查destination的id和name |
| **ViewModel注入失败** | Module未配置 | 确认ViewModelModule已创建并添加@InstallIn |
| **主题不生效** | Theme未应用到Activity | 检查MainActivity的setContent包裹 |
| **编译速度慢** | Gradle缓存问题 | 执行`./gradlew clean` |

#### 寻求帮助的渠道
1. **项目文档**: 优先查阅相关Phase文档
2. **代码示例**: 参考现有的ViewModel实现
3. **官方文档**: Jetpack Compose官方文档
4. **社区**: Stack Overflow, GitHub Issues

### 10.3 关键提醒

⚠️ **严格遵循阶段顺序**:
- 不要跳过Phase 1直接做组件
- 不要跳过Phase 2直接做Screen
- 每个阶段完成后做验收检查

⚠️ **时间管理**:
- 设置每日提醒检查进度
- 如果某任务超过预估时间2倍,立即评估风险
- Day 10是关键节点,必须完成Phase 3

⚠️ **质量vs速度**:
- Phase 1-3优先速度(先能跑起来)
- Phase 4重点质量(优化和测试)
- 遇到阻塞问题超过4小时,考虑简化方案

---

## 11. 文档使用说明

### 11.1 