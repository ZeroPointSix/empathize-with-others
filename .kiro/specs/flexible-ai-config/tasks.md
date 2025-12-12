# 实施计划 - 灵活的 AI 模型配置（优化版）

## 第一阶段：基础功能（1周）

- [x] 1. Domain 层实现





  - 创建核心业务实体和仓库接口
  - 实现业务逻辑用例
  - _Requirements: 1.1, 2.1, 2.2, 2.3, 4.1, 4.2_
  - _依赖: 无_


- [x] 1.1 创建领域模型

  - 创建 `domain/model/AiProvider.kt` 数据类
  - 创建 `domain/model/AiModel.kt` 数据类
  - _Requirements: 1.1, 4.1_
  - _依赖: 无_


- [x] 1.2 定义 Repository 接口

  - 创建 `domain/repository/AiProviderRepository.kt` 接口
  - 定义 getAllProviders、getProvider、saveProvider、deleteProvider 等方法
  - _Requirements: 1.1, 1.3, 1.4_
  - _依赖: 1.1_


- [x] 1.3 实现 SaveProviderUseCase

  - 创建 `domain/usecase/SaveProviderUseCase.kt`
  - 实现基础配置验证逻辑（名称、URL、API Key、模型非空验证）
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 4.1_
  - _依赖: 1.1, 1.2_

- [x] 1.4 实现其他 UseCase


  - 创建 `domain/usecase/DeleteProviderUseCase.kt`
  - 创建 `domain/usecase/GetProvidersUseCase.kt`
  - 创建 `domain/usecase/TestConnectionUseCase.kt`（简单版本，仅测试连通性）
  - _Requirements: 1.4, 1.1, 5.1_
  - _依赖: 1.2_


- [x] 1.5 编写核心属性测试

  - **Property 1: 配置验证完整性**（合并 Property 1 和 2）
  - 测试空字段验证和 URL 格式验证
  - **Validates: Requirements 2.1, 2.2, 2.3, 2.4**
  - _依赖: 1.3_

- [x] 2. Data 层实现





  - 创建数据库实体和 DAO
  - 实现 API Key 安全存储
  - 实现 Repository
  - _Requirements: 2.5, 12.2_
  - _依赖: 1.2_

- [x] 2.1 创建数据库实体和 DAO


  - 创建 `data/local/entity/AiProviderEntity.kt`
  - 创建 `data/local/dao/AiProviderDao.kt`
  - 定义表结构、索引和 CRUD 查询方法
  - _Requirements: 1.1, 1.3, 1.4_
  - _依赖: 1.1_


- [x] 2.2 实现 ApiKeyStorage

  - 创建 `data/local/ApiKeyStorage.kt`
  - 使用 EncryptedSharedPreferences 实现加密存储
  - 实现 save、get、delete、mask 方法
  - _Requirements: 2.5, 12.1, 12.2, 12.3_
  - _依赖: 无_

- [x] 2.3 编写 ApiKeyStorage 单元测试


  - 测试加密存储和读取
  - 测试 API Key 脱敏显示
  - _Requirements: 12.2, 12.3_
  - _依赖: 2.2_

- [x] 2.4 实现 AiProviderRepositoryImpl


  - 创建 `data/repository/AiProviderRepositoryImpl.kt`
  - 实现所有 Repository 接口方法
  - 实现 Entity <-> Domain 映射
  - 使用 Moshi 序列化模型列表
  - _Requirements: 1.1, 1.3, 1.4, 4.1_
  - _依赖: 2.1, 2.2_

- [x] 2.5 编写 Repository 核心属性测试


  - **Property 3: 服务商保存往返一致性**
  - **Property 4: 模型列表完整性**（合并测试）
  - **Validates: Requirements 1.3, 4.1, 4.2, 4.3**
  - _依赖: 2.4_

- [x] 2.6 数据库迁移和 Hilt 配置


  - 在 `AppDatabase` 中添加 `AiProviderEntity`
  - 创建 MIGRATION_1_2 迁移脚本
  - 在 `RepositoryModule` 中绑定 AiProviderRepository
  - _Requirements: 1.1_
  - _依赖: 2.1, 2.4_

- [x] 3. Presentation 层实现




  - 创建 ViewModel 和 UI 组件
  - 实现用户交互逻辑
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_
  - _依赖: 1.4, 2.4_

- [x] 3.1 创建 UI State 和 Event


  - 创建 `presentation/ui/screen/aiconfig/AiConfigUiState.kt`
  - 创建 `presentation/ui/screen/aiconfig/AiConfigUiEvent.kt`
  - 定义所有 UI 状态和事件
  - _Requirements: 1.1_
  - _依赖: 1.1_

- [x] 3.2 实现 AiConfigViewModel


  - 创建 `presentation/viewmodel/AiConfigViewModel.kt`
  - 实现状态管理和事件处理
  - 集成所有 UseCase
  - 实现基础错误处理（简单的错误消息显示）
  - _Requirements: 1.1, 1.2, 1.3, 1.4_
  - _依赖: 3.1, 1.4_

- [x] 3.3 实现 AiConfigScreen


  - 创建 `presentation/ui/screen/aiconfig/AiConfigScreen.kt`
  - 实现服务商列表展示
  - 实现空状态视图
  - _Requirements: 1.1_
  - _依赖: 3.2_

- [x] 3.4 实现 ProviderCard 组件


  - 创建服务商卡片组件
  - 显示服务商名称、URL、模型数量
  - 实现编辑和删除操作
  - _Requirements: 1.1, 1.4_
  - _依赖: 3.3_


- [x] 3.5 实现 ProviderFormDialog

  - 创建配置表单对话框
  - 实现名称、URL、API Key 输入框
  - 实现基础的模型列表管理
  - _Requirements: 1.2, 1.3, 2.1, 2.2, 2.3, 4.1, 4.2_
  - _依赖: 3.3_


- [x] 4. 集成和基础测试






  - 集成到设置页面
  - 手动测试核心功能
  - _Requirements: 所有_
  - _依赖: 3.5_

- [x] 4.1 集成到设置页面和 AiRepository




  - 在 SettingsScreen 添加 AI 配置入口
  - 配置导航路由
  - 修改 AiRepositoryImpl 使用 AiProviderRepository
  - _Requirements: 1.1, 4.5_
  - _依赖: 3.5, 2.4_

- [x] 4.2 手动测试核心功能




  - 测试添加服务商
  - 测试编辑服务商
  - 测试删除服务商
  - 测试设置默认服务商
  - _Requirements: 所有_
  - _依赖: 4.1_

- [x] 5. Checkpoint - 确保核心功能可用







  - 确保所有核心功能正常工作，如有问题请询问用户
  - _依赖: 4.2_

## 第二阶段：增强功能（1周）

- [ ] 6. 连接测试功能增强
  - 实现完整的连接测试逻辑
  - 添加详细的错误提示
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_
  - _依赖: 第一阶段完成_

- [ ] 6.1 完善 TestConnectionUseCase
  - 实现详细的错误类型识别（API Key 无效、端点不可达、模型不存在、超时、配额用尽）
  - 添加延迟测量
  - _Requirements: 5.2, 5.5, 6.1, 6.2, 6.3, 6.4, 6.5_
  - _依赖: 1.4_

- [ ] 6.2 实现连接测试 UI
  - 在表单中添加测试连接按钮
  - 显示测试结果（成功/失败/延迟）
  - 显示加载状态
  - _Requirements: 5.1, 5.3, 5.4, 5.5_
  - _依赖: 6.1, 3.5_

- [ ] 6.3 编写连接测试单元测试
  - 测试成功场景
  - 测试各种错误场景
  - _Requirements: 5.2, 5.3, 5.4_
  - _依赖: 6.1_

- [ ] 7. 错误处理和 UI 优化
  - 实现友好的错误提示
  - 优化用户体验
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 1.2, 1.3, 1.4, 1.5_
  - _依赖: 6.1_

- [ ] 7.1 实现错误映射和 ErrorSnackbar
  - 创建错误类型到用户消息的映射
  - 创建统一的错误提示组件
  - 支持自动消失和手动关闭
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_
  - _依赖: 6.1_

- [ ] 7.2 添加加载状态和确认对话框
  - 在保存、删除、测试连接时显示加载指示器
  - 禁用相关按钮防止重复操作
  - 删除服务商前显示确认对话框
  - _Requirements: 1.2, 1.3, 1.5, 5.5_
  - _依赖: 3.5_

- [ ] 7.3 优化表单验证
  - 实时验证输入字段
  - 显示验证错误提示
  - _Requirements: 2.1, 2.2, 2.3, 2.4_
  - _依赖: 3.5_

- [ ] 8. 补充属性测试
  - 补充剩余的关键属性测试
  - _Requirements: 4.5, 12.2, 12.3_
  - _依赖: 6.1, 7.1_

- [ ] 8.1 编写 API Key 脱敏属性测试
  - **Property 6: API Key 脱敏显示**
  - **Validates: Requirements 12.3**
  - _依赖: 2.2_

- [ ] 8.2 编写默认服务商唯一性属性测试
  - **Property 7: 默认服务商唯一性**
  - **Validates: Requirements 4.5**
  - _依赖: 2.4_

- [ ] 9. 集成测试
  - 测试完整的用户流程
  - _Requirements: 1.2, 1.3, 1.4_
  - _依赖: 7.3_

- [ ] 9.1 编写端到端集成测试
  - 测试完整的添加服务商流程
  - 测试编辑和删除流程
  - 测试连接测试流程
  - _Requirements: 1.2, 1.3, 1.4, 5.1_
  - _依赖: 7.3_

- [ ] 10. Checkpoint - 确保所有功能完整
  - 确保所有增强功能正常工作，如有问题请询问用户
  - _依赖: 9.1_

## 第三阶段：完善功能（1周）

- [ ] 11. 边界情况处理
  - 处理各种边界情况
  - 确保应用稳定性
  - _Requirements: 所有_
  - _依赖: 第二阶段完成_

- [ ] 11.1 处理空数据和网络异常
  - 优化空状态视图，提供友好的引导
  - 优化网络错误处理，提供重试机制
  - _Requirements: 1.1, 5.2, 6.2_
  - _依赖: 7.1_

- [ ] 11.2 处理数据迁移
  - 测试从旧配置迁移到新系统
  - 确保向后兼容
  - _Requirements: 1.1_
  - _依赖: 2.6_

- [ ] 12. 文档完善
  - 更新相关文档
  - 添加使用说明
  - _Requirements: 所有_
  - _依赖: 11.2_

- [ ] 12.1 更新 README 和代码注释
  - 添加功能说明和使用示例
  - 为关键类和方法添加 KDoc
  - 解释复杂逻辑
  - _Requirements: 所有_
  - _依赖: 11.2_

- [ ] 13. 用户反馈和后续规划
  - 收集用户反馈
  - 规划后续迭代
  - _Requirements: 所有_
  - _依赖: 12.1_

- [ ] 13.1 内部测试和反馈收集
  - 邀请团队成员测试
  - 收集反馈意见
  - _Requirements: 所有_
  - _依赖: 12.1_

- [ ] 13.2 规划后续功能
  - 根据反馈规划配置模板功能
  - 规划配置导入导出功能
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 9.1, 9.2, 9.3, 9.4, 9.5_
  - _依赖: 13.1_

- [ ] 14. Final Checkpoint - 确保所有测试通过
  - 确保所有测试通过，功能稳定，如有问题请询问用户
  - _依赖: 13.2_

---

## 可选任务（推迟到后续迭代）

以下任务为性能优化相关，可在 MVP 稳定后根据实际需求决定是否实施：

- [ ]* 性能优化 - 数据库查询优化
  - 确认索引正确创建
  - 使用 Flow 的 distinctUntilChanged
  - _Requirements: 1.1_

- [ ]* 性能优化 - UI 渲染优化
  - 使用 remember 缓存计算结果
  - 优化 LazyColumn 性能
  - _Requirements: 1.1_
