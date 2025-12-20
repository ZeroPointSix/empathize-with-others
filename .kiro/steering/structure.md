# 项目结构

## 🔴 必读文档（开始工作前必须阅读）

**在开始任何工作之前，请务必先阅读以下文档：**

1. **[Rules/RulesReadMe.md](../../Rules/RulesReadMe.md)** - 项目通用规则和文档规范
2. **[WORKSPACE.md](../../WORKSPACE.md)** - 当前工作状态和任务协调

---

## 语言规范

**所有文档和回答必须使用中文。** 代码注释、变量名、类名等保持英文，但所有说明文档、开发指南和与开发者的沟通必须使用中文。

## 架构模式

**Clean Architecture + MVVM** with strict layer separation and dependency rules.

## 包组织结构

```
com.empathy.ai/
├── app/                                    # ✅ 应用入口
│   └── EmpathyApplication.kt           # Hilt 应用类
│
├── domain/                                 # ✅ 领域层（纯 Kotlin，无 Android 依赖）
│   ├── model/                            # ✅ 业务实体
│   │   ├── AnalysisResult.kt
│   │   ├── BrainTag.kt
│   │   ├── ChatMessage.kt
│   │   ├── ContactProfile.kt
│   │   ├── SafetyCheckResult.kt
│   │   ├── AiProvider.kt
│   │   ├── AiModel.kt
│   │   ├── ActionType.kt
│   │   ├── FloatingWindowError.kt
│   │   ├── MinimizedRequestInfo.kt
│   │   ├── MinimizeError.kt
│   │   ├── ExtractedData.kt
│   │   ├── PromptContext.kt              # 🆕 提示词上下文模型
│   │   ├── PromptError.kt                # 🆕 提示词错误模型
│   │   ├── PromptScene.kt                 # 🆕 提示词场景模型
│   │   ├── GlobalPromptConfig.kt           # 🆕 全局提示词配置
│   │   ├── TimelineItem.kt                # 🆕 时间线项目密封类
│   │   ├── EmotionType.kt                 # 🆕 情绪类型枚举
│   │   ├── RelationshipLevel.kt             # 🆕 关系级别枚举
│   │   ├── RelationshipTrend.kt            # 🆕 关系趋势枚举
│   │   ├── Fact.kt                       # 🆕 事实模型
│   │   ├── FactKeys.kt                    # 🆕 事实键常量
│   │   ├── FactSource.kt                  # 🆕 事实来源枚举
│   │   ├── FilterType.kt                  # 🆕 过滤类型枚举
│   │   ├── FloatingBubblePosition.kt        # 🆕 悬浮球位置模型
│   │   ├── FloatingBubbleState.kt          # 🆕 悬浮球状态模型
│   │   ├── FloatingWindowState.kt          # 🆕 悬浮窗状态模型
│   │   ├── FloatingWindowUiState.kt        # 🆕 悬浮窗UI状态模型
│   │   ├── ConversationContextConfig.kt     # 🆕 对话上下文配置
│   │   ├── ConversationLog.kt              # 🆕 对话日志模型
│   │   ├── DailySummary.kt                # 🆕 每日总结模型
│   │   ├── DataStatus.kt                  # 🆕 数据状态枚举
│   │   ├── TimeFlowMarker.kt              # 🆕 时间流标记
│   │   ├── TimestampedMessage.kt           # 🆕 时间戳消息模型
│   │   ├── ViewMode.kt                   # 🆕 视图模式枚举
│   │   ├── KeyEvent.kt                   # 🆕 键事件模型
│   │   ├── AiResult.kt                   # 🆕 AI结果模型
│   │   ├── AppError.kt                   # 🆕 应用错误模型
│   │   ├── ConnectionTestResult.kt          # 🆕 连接测试结果模型
│   │   ├── PolishResult.kt                # 🆕 润色结果模型
│   │   ├── ReplyResult.kt                 # 🆕 回复结果模型
│   │   ├── RefinementRequest.kt            # 🆕 优化请求模型
│   │   ├── TagUpdate.kt                  # 🆕 标签更新模型
│   │   ├── PromptValidationResult.kt        # 🆕 提示词验证结果模型
│   │   ├── ScenePromptConfig.kt            # 🆕 场景提示词配置
│   │   ├── ProviderPresets.kt              # 🆕 提供商预设模型
│   │   ├── PromptHistoryItem.kt           # 🆕 提示词历史项模型
│   │   ├── MessageSender.kt               # 🆕 消息发送者枚举
│   │   ├── CleanupConfig.kt              # 🆕 清理配置模型
│   │   ├── ConflictResult.kt              # 🆕 冲突结果模型
│   │   ├── DateRange.kt                  # 🆕 日期范围模型
│   │   ├── GenerationSource.kt            # 🆕 生成来源枚举
│   │   ├── SummaryError.kt                # 🆕 总结错误模型
│   │   ├── SummaryTask.kt                 # 🆕 总结任务模型
│   │   ├── SummaryTaskStatus.kt            # 🆕 总结任务状态枚举
│   │   ├── SummaryType.kt                 # 🆕 总结类型枚举
│   │   └── ViewMode.kt                   # 🆕 视图模式枚举
│   ├── repository/                        # ✅ 仓库接口
│   │   ├── AiRepository.kt
│   │   ├── BrainTagRepository.kt
│   │   ├── ContactRepository.kt
│   │   ├── PrivacyRepository.kt
│   │   ├── SettingsRepository.kt
│   │   ├── PromptRepository.kt             # 🆕 提示词仓库接口
│   │   ├── ConversationRepository.kt        # 🆕 对话仓库接口
│   │   ├── DailySummaryRepository.kt        # 🆕 每日总结仓库接口
│   │   ├── FailedTaskRepository.kt          # 🆕 失败任务仓库接口
│   │   └── AiProviderRepository.kt
│   ├── usecase/                          # ✅ 业务逻辑用例
│   │   ├── AnalyzeChatUseCase.kt
│   │   ├── CheckDraftUseCase.kt
│   │   ├── FeedTextUseCase.kt
│   │   ├── SaveProfileUseCase.kt
│   │   ├── GetAllContactsUseCase.kt
│   │   ├── GetContactUseCase.kt
│   │   ├── DeleteContactUseCase.kt
│   │   ├── DeleteBrainTagUseCase.kt
│   │   ├── SaveBrainTagUseCase.kt
│   │   ├── SaveProviderUseCase.kt
│   │   ├── DeleteProviderUseCase.kt
│   │   ├── GetProvidersUseCase.kt
│   │   ├── TestConnectionUseCase.kt
│   │   ├── PolishDraftUseCase.kt           # 🆕 润色草稿用例
│   │   ├── GenerateReplyUseCase.kt         # 🆕 生成回复用例
│   │   ├── RefinementUseCase.kt            # 🆕 优化用例
│   │   ├── GetBrainTagsUseCase.kt          # 🆕 获取标签用例
│   │   ├── SummarizeDailyConversationsUseCase.kt  # 🆕 每日对话总结用例
│   │   └── ManualSummaryUseCase.kt         # 🆕 手动总结用例
│   ├── service/                          # ✅ 领域服务
│   │   ├── PrivacyEngine.kt
│   │   ├── RuleEngine.kt
│   │   ├── FloatingWindowService.kt
│   │   └── SessionContextService.kt        # 🆕 会话上下文服务
│   └── util/                            # ✅ 领域工具类
│       ├── ErrorHandler.kt
│       ├── ErrorMapper.kt
│       ├── FallbackStrategy.kt
│       ├── FloatingView.kt
│       ├── FloatingWindowManager.kt
│       ├── OperationExecutor.kt
│       ├── PerformanceMonitor.kt
│       ├── PerformanceTracker.kt
│       ├── RetryConfig.kt
│       ├── WeChatDetector.kt
│       ├── PromptBuilder.kt                # 🆕 提示词构建器
│       ├── PromptSanitizer.kt              # 🆕 提示词清理器
│       ├── PromptValidator.kt               # 🆕 提示词验证器
│       ├── PromptVariableResolver.kt         # 🆕 提示词变量解析器
│       ├── SystemPrompts.kt                # 🆕 系统提示词
│       ├── PromptTemplates.kt               # 🆕 提示词模板
│       ├── ConversationContextBuilder.kt      # 🆕 对话上下文构建器
│       ├── IdentityPrefixHelper.kt           # 🆕 身份前缀助手
│       ├── AiResponseCleaner.kt            # 🆕 AI响应清理器
│       ├── AiSummaryProcessor.kt            # 🆕 AI总结处理器
│       ├── LocalSummaryProcessor.kt          # 🆕 本地总结处理器
│       ├── DataCleanupManager.kt            # 🆕 数据清理管理器
│       ├── DataEncryption.kt                # 🆕 数据加密工具
│       ├── DebugLogger.kt                   # 🆕 调试日志器
│       ├── FloatingViewDebugLogger.kt        # 🆕 悬浮窗调试日志器
│       ├── PerformanceMetrics.kt             # 🆕 性能指标
│       ├── PermissionManager.kt             # 🆕 权限管理器
│       ├── PrivacyConfig.kt                 # 🆕 隐私配置
│       ├── PrivacyDataHandler.kt            # 🆕 隐私数据处理器
│       ├── SecurityConfig.kt                # 🆕 安全配置
│       ├── MemoryConstants.kt               # 🆕 内存常量
│       ├── MemoryLogger.kt                 # 🆕 内存日志器
│       ├── FailedTaskRecovery.kt           # 🆕 失败任务恢复
│       ├── ContactDetailError.kt            # 🆕 联系人详情错误
│       ├── DateUtils.kt                   # 🆕 日期工具类
│       ├── ContextBuilder.kt               # 🆕 上下文构建器
│       ├── DateRangeValidator.kt           # 🆕 日期范围验证器
│       └── SummaryConflictChecker.kt       # 🆕 总结冲突检查器
│
├── data/                                   # ✅ 数据层（实现）
│   ├── local/                          # ✅ 本地存储
│   │   ├── AppDatabase.kt              # Room 数据库配置
│   │   ├── ApiKeyStorage.kt
│   │   ├── FloatingWindowPreferences.kt
│   │   ├── PrivacyPreferences.kt         # 🆕 隐私偏好设置
│   │   ├── MemoryPreferences.kt         # 🆕 记忆偏好设置
│   │   ├── ConversationPreferences.kt    # 🆕 对话偏好设置
│   │   ├── DefaultPrompts.kt           # 🆕 默认提示词
│   │   ├── PromptFileStorage.kt         # 🆕 提示词文件存储
│   │   ├── PromptFileBackup.kt         # 🆕 提示词文件备份
│   │   ├── converter/                # ✅ Room 类型转换器
│   │   │   └── RoomTypeConverters.kt
│   │   │   └── FactListConverter.kt    # 🆕 事实列表转换器
│   │   ├── dao/                    # ✅ 数据访问对象
│   │   │   ├── AiProviderDao.kt
│   │   │   ├── BrainTagDao.kt
│   │   │   ├── ContactDao.kt
│   │   │   ├── ConversationLogDao.kt    # 🆕 对话日志DAO
│   │   │   ├── DailySummaryDao.kt       # 🆕 每日总结DAO
│   │   │   └── FailedSummaryTaskDao.kt  # 🆕 失败总结任务DAO
│   │   └── entity/                 # ✅ 数据库实体
│   │       ├── AiProviderEntity.kt
│   │       ├── BrainTagEntity.kt
│   │       ├── ContactProfileEntity.kt
│   │       ├── ConversationLogEntity.kt    # 🆕 对话日志实体
│   │       ├── DailySummaryEntity.kt       # 🆕 每日总结实体
│   │       └── FailedSummaryTaskEntity.kt  # 🆕 失败总结任务实体
│   ├── remote/                         # ✅ 网络层
│   │   ├── api/                    # ✅ Retrofit API 接口
│   │   │   └── OpenAiApi.kt
│   │   └── model/                  # ✅ DTO（数据传输对象）
│   │       ├── ChatRequestDto.kt
│   │       ├── ChatResponseDto.kt
│   │       ├── MessageDto.kt
│   │       ├── ModelsResponseDto.kt       # 🆕 模型响应DTO
│   │       └── AiSummaryResponse.kt      # 🆕 AI总结响应DTO
│   ├── repository/                     # ✅ 仓库实现
│   │   ├── AiRepositoryImpl.kt
│   │   ├── BrainTagRepositoryImpl.kt
│   │   ├── ContactRepositoryImpl.kt
│   │   ├── PrivacyRepositoryImpl.kt
│   │   ├── AiProviderRepositoryImpl.kt
│   │   ├── settings/
│   │   │   └── SettingsRepositoryImpl.kt
│   │   ├── PromptRepositoryImpl.kt       # 🆕 提示词仓库实现
│   │   ├── ConversationRepositoryImpl.kt  # 🆕 对话仓库实现
│   │   ├── DailySummaryRepositoryImpl.kt  # 🆕 每日总结仓库实现
│   │   ├── FailedTaskRepositoryImpl.kt    # 🆕 失败任务仓库实现
│   │   └── ProviderCompatibility.kt       # 🆕 提供商兼容性
│   └── parser/                         # ✅ AI响应解析器
│       ├── AiResponseParser.kt
│       ├── EnhancedJsonCleaner.kt
│       ├── FallbackHandler.kt
│       ├── FieldMapper.kt
│       └── JsonCleaner.kt
│
├── presentation/                            # ✅ 表现层
│   ├── navigation/                     # ✅ 导航系统
│   │   ├── NavGraph.kt
│   │   ├── NavRoutes.kt
│   │   └── PromptEditorNavigation.kt    # 🆕 提示词编辑器导航
│   ├── theme/                          # ✅ Compose 主题
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   ├── Type.kt
│   │   ├── AnimationSpec.kt              # 🆕 动画规格
│   │   ├── Dimensions.kt                # 🆕 尺寸定义
│   │   ├── RelationshipColors.kt          # 🆕 关系颜色
│   │   └── SemanticColors.kt             # 🆕 语义颜色
│   ├── ui/                             # ✅ UI 组件
│   │   ├── MainActivity.kt
│   │   ├── component/               # ✅ 可复用组件
│   │   │   ├── MaxHeightScrollView.kt    # 🆕 最大高度滚动视图
│   │   │   ├── button/
│   │   │   │   ├── PrimaryButton.kt
│   │   │   │   └── SecondaryButton.kt
│   │   │   ├── card/
│   │   │   │   ├── AnalysisCard.kt
│   │   │   │   ├── ProfileCard.kt
│   │   │   │   ├── ProviderCard.kt
│   │   │   │   ├── AiSummaryCard.kt      # 🆕 AI总结卡片
│   │   │   │   ├── ConversationCard.kt    # 🆕 对话卡片
│   │   │   │   ├── MilestoneCard.kt      # 🆕 里程碑卡片
│   │   │   │   └── PhotoMomentCard.kt    # 🆕 照片时刻卡片
│   │   │   ├── chip/
│   │   │   │   ├── TagChip.kt
│   │   │   │   ├── SolidTagChip.kt       # 🆕 实心标签芯片
│   │   │   │   ├── GuessedTag.kt         # 🆕 推测标签
│   │   │   │   └── ConfirmedTag.kt       # 🆕 确认标签
│   │   │   ├── dialog/
│   │   │   │   ├── AddContactDialog.kt
│   │   │   │   ├── AddTagDialog.kt
│   │   │   │   ├── DeleteTagConfirmDialog.kt
│   │   │   │   ├── PermissionRequestDialog.kt
│   │   │   │   ├── ProviderFormDialog.kt
│   │   │   │   ├── AddFactToStreamDialog.kt      # 🆕 添加事实到流对话框
│   │   │   │   ├── EditConversationDialog.kt      # 🆕 编辑对话对话框
│   │   │   │   └── TagConfirmationDialog.kt       # 🆕 标签确认对话框
│   │   │   ├── input/
│   │   │   │   ├── ContactSearchBar.kt
│   │   │   │   ├── CustomTextField.kt
│   │   │   │   └── TagSearchBar.kt
│   │   │   ├── list/
│   │   │   │   └── ContactListItem.kt
│   │   │   ├── message/
│   │   │   │   ├── MessageBubble.kt
│   │   │   │   └── ConversationBubble.kt       # 🆕 对话气泡
│   │   │   ├── state/
│   │   │   │   ├── EmptyView.kt
│   │   │   │   ├── ErrorView.kt
│   │   │   │   ├── LoadingIndicator.kt
│   │   │   │   └── StatusBadge.kt          # 🆕 状态徽章
│   │   │   ├── control/                 # 🆕 控制组件
│   │   │   │   ├── QuickFilterChips.kt
│   │   │   │   └── SegmentedControl.kt
│   │   │   ├── emotion/                 # 🆕 情感化组件
│   │   │   │   ├── EmotionalBackground.kt
│   │   │   │   ├── EmotionalTimelineNode.kt
│   │   │   │   └── GlassmorphicCard.kt
│   │   │   ├── relationship/             # 🆕 关系进展组件
│   │   │   │   ├── FactItem.kt
│   │   │   │   ├── RelationshipScoreSection.kt
│   │   │   │   └── TrendIcon.kt
│   │   │   └── floating/               # 🆕 悬浮窗组件
│   │   │       ├── FloatingViewV2.kt
│   │   │       ├── FloatingBubbleView.kt
│   │   │       ├── ResultCard.kt
│   │   │       ├── TabSwitcher.kt
│   │   │       └── RefinementOverlay.kt
│   │   └── screen/               # ✅ 功能屏幕
│   │       ├── aiconfig/
│   │       │   ├── AiConfigScreen.kt
│   │       │   ├── AiConfigUiState.kt
│   │       │   └── AiConfigUiEvent.kt
│   │       ├── chat/
│   │       │   ├── ChatScreen.kt
│   │       │   ├── ChatUiState.kt
│   │       │   └── ChatUiEvent.kt
│   │       ├── contact/
│   │       │   ├── ContactListScreen.kt
│   │       │   ├── ContactListUiState.kt
│   │       │   ├── ContactListUiEvent.kt
│   │       │   ├── ContactDetailScreen.kt
│   │       │   ├── ContactDetailUiState.kt
│   │       │   ├── ContactDetailUiEvent.kt
│   │       │   ├── ContactDetailTabScreen.kt          # ✅ 四标签页UI
│   │       │   ├── DetailTab.kt                      # ✅ 标签页枚举
│   │       │   ├── overview/                         # ✅ 概览标签页
│   │       │   │   ├── OverviewTab.kt
│   │       │   │   ├── DynamicEmotionalHeader.kt
│   │       │   │   ├── LatestFactHookCard.kt
│   │       │   │   └── TopTagsSection.kt
│   │       │   ├── factstream/                       # ✅ 事实流标签页
│   │       │   │   ├── FactStreamTab.kt
│   │       │   │   ├── ListView.kt
│   │       │   │   ├── ListViewRow.kt
│   │       │   │   ├── TimelineView.kt
│   │       │   │   └── FactStreamTopBar.kt
│   │       │   ├── persona/                          # ✅ 标签画像标签页
│   │       │   │   ├── PersonaTab.kt
│   │       │   │   ├── CategorySection.kt
│   │       │   │   ├── GuessedTag.kt
│   │       │   │   └── ConfirmedTag.kt
│   │       │   ├── vault/                           # ✅ 资料库标签页
│   │       │   │   ├── DataVaultTab.kt
│   │       │   │   └── DataSourceCard.kt
│   │       │   └── summary/                         # 🆕 总结标签页
│   │       │       ├── SummaryProgressDialog.kt        # 🆕 总结进度对话框
│   │       │       ├── SummaryResultDialog.kt         # 🆕 总结结果对话框
│   │       │       ├── SummaryDetailDialog.kt         # 🆕 总结详情对话框
│   │       │       ├── SummaryErrorDialog.kt          # 🆕 总结错误对话框
│   │       │       ├── QuickDateOptions.kt            # 🆕 快速日期选项
│   │       │       ├── DateRangePickerDialog.kt       # 🆕 日期范围选择对话框
│   │       │       ├── RangeWarningDialog.kt          # 🆕 范围警告对话框
│   │       │       ├── MissingSummaryCard.kt          # 🆕 缺失总结卡片
│   │       │       ├── ManualSummaryFab.kt            # 🆕 手动总结浮动按钮
│   │       │       ├── ConflictResolutionDialog.kt     # 🆕 冲突解决对话框
│   │       │       └── SummarySourceBadge.kt         # 🆕 总结来源徽章
│   │       ├── settings/
│   │       │   ├── SettingsScreen.kt
│   │       │   ├── SettingsUiState.kt
│   │       │   ├── SettingsUiEvent.kt
│   │       │   └── component/                        # 🆕 设置组件
│   │       │       └── HistoryConversationCountSection.kt
│   │       ├── tag/
│   │       │   ├── BrainTagScreen.kt
│   │       │   ├── BrainTagUiState.kt
│   │       │   └── BrainTagUiEvent.kt
│   │       └── prompt/                        # 🆕 提示词编辑器
│   │           ├── PromptEditorScreen.kt
│   │           ├── PromptEditorUiState.kt
│   │           ├── PromptEditorUiEvent.kt
│   │           ├── PromptEditMode.kt
│   │           ├── PromptEditorResult.kt
│   │           └── component/                     # 🆕 提示词编辑器组件
│   │               ├── CharacterCounter.kt
│   │               ├── DiscardConfirmDialog.kt
│   │               ├── InlineErrorBanner.kt
│   │               ├── PromptEditorTopBar.kt
│   │               └── PromptInputField.kt
│   ├── viewmodel/                    # ✅ ViewModel
│   │   ├── BaseViewModel.kt
│   │   ├── AiConfigViewModel.kt
│   │   ├── BrainTagViewModel.kt
│   │   ├── ChatViewModel.kt
│   │   ├── ContactDetailViewModel.kt
│   │   ├── ContactDetailTabViewModel.kt     # ✅ 四标签页ViewModel
│   │   ├── ContactListViewModel.kt
│   │   ├── SettingsViewModel.kt
│   │   └── PromptEditorViewModel.kt          # 🆕 提示词编辑器ViewModel
│   └── util/                             # 🆕 表现层工具类
│       └── ImageLoaderConfig.kt              # 🆕 图片加载配置
│
├── notification/                            # 🆕 通知模块
│   └── AiResultNotificationManager.kt       # AI结果通知管理器
│
└── di/                              # ✅ 依赖注入
    ├── DatabaseModule.kt
    ├── NetworkModule.kt
    ├── RepositoryModule.kt
    ├── ServiceModule.kt
    ├── MemoryModule.kt               # 🆕 记忆系统依赖注入
    ├── PromptModule.kt               # 🆕 提示词系统依赖注入
    ├── DispatcherModule.kt           # 🆕 协程调度器管理
    ├── FloatingWindowModule.kt       # 🆕 悬浮窗依赖注入
    ├── NotificationModule.kt          # 🆕 通知模块依赖注入
    └── SummaryModule.kt              # 🆕 总结系统依赖注入
```

## 层级职责

### 领域层（纯业务逻辑）
- **✅ 无 Android 依赖** - 可以在不依赖 Android 框架的情况下进行测试
- **✅ 包含业务模型、仓库接口、用例和领域服务**
- **✅ 所有用例返回 `Result<T>` 以实现一致的错误处理**
- **✅ 所有 IO 操作都是 `suspend` 函数**

### 数据层（数据访问）
- **✅ 实现领域层的仓库接口**
- **✅ Room 数据库用于本地存储，支持 Flow**
- **✅ Retrofit 用于网络调用，使用 Moshi JSON 解析**
- **✅ EncryptedSharedPreferences 用于敏感数据（API 密钥）**
- **✅ 实体使用 `snake_case` 作为数据库列名，`camelCase` 作为 Kotlin 属性名**

### 表现层（UI 和交互）
- **✅ Jetpack Compose 用于声明式 UI**
- **✅ 使用 Hilt 注入的 ViewModel**
- **✅ StateFlow 用于 UI 状态管理**
- **✅ UiState 和 UiEvent 密封类用于类型安全的状态/事件处理**

### DI 层（依赖注入）
- **✅ Hilt 模块用于提供依赖**
- **✅ 为数据库、网络和仓库分别创建模块**

## 命名规范

### 文件
- **PascalCase** 用于所有 Kotlin 文件：`ContactProfile.kt`
- **Entity 后缀** 用于数据库实体：`ContactProfileEntity.kt`
- **ViewModel 后缀**：`ChatViewModel.kt`
- **UiState 后缀**：`ChatUiState.kt`
- **UiEvent 后缀**：`ChatUiEvent.kt`
- **UseCase 后缀**：`AnalyzeChatUseCase.kt`

### 数据库
- **表名**：`snake_case` 复数形式：`contact_profiles`、`brain_tags`、`ai_providers`
- **列名**：`snake_case`：`contact_id`、`tag_type`
- **始终使用 `@ColumnInfo(name = "...")` 来解耦 Kotlin 名称和 SQL**

### Kotlin
- **属性**：`camelCase`：`contactId`、`tagType`
- **常量**：`UPPER_SNAKE_CASE`：`MAX_RETRY_COUNT`
- **Composable**：`PascalCase`：`ChatScreen`、`MessageBubble`

## 关键模式

### 仓库模式
```kotlin
// 领域层定义接口
interface ContactRepository {
    fun getAllProfiles(): Flow<List<ContactProfile>>
    suspend fun insertProfile(profile: ContactProfile): Result<Unit>
}

// 数据层实现
class ContactRepositoryImpl @Inject constructor(
    private val dao: ContactDao
) : ContactRepository {
    // 实现包含 Entity <-> Domain 映射
}
```

### 用例模式
```kotlin
class AnalyzeChatUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(...): Result<AnalysisResult> {
        // 业务逻辑，使用 Result 包装
    }
}
```

### ViewModel 模式
```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val analyzeChatUseCase: AnalyzeChatUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    fun onEvent(event: ChatUiEvent) { /* 处理事件 */ }
}
```

## 测试结构

```
test/
└── com/empathy/ai/
    ├── data/
    │   ├── local/converter/
    │   └── repository/
    ├── domain/
    │   ├── service/
    │   ├── usecase/
    │   └── util/
    ├── integration/
    ├── presentation/
    │   ├── integration/
    ├── viewmodel/
    └── performance/
```

测试文件镜像源代码结构，使用 `Test` 后缀：`AnalyzeChatUseCaseTest.kt`

## 文档

所有架构和开发文档位于 `docs/`：
- `00-项目概述/` - 项目概览
- `01-架构设计/` - 架构设计（数据层、服务层、UI 层）
- `02-开发指南/` - 开发指南
- `03-测试文档/` - 测试文档

## 当前实现状态

### ✅ 完全实现的模块
- **领域层**: 100%完成，所有模型、接口、用例、服务完整实现
  - 新增TimelineItem密封类，统一管理时间线内容
  - 新增EmotionType枚举，支持情感化展示
  - 新增RelationshipLevel、RelationshipTrend等关系追踪模型
  - 提示词管理系统：新增PromptContext、PromptError、PromptScene、GlobalPromptConfig等模型
  - 新增PromptBuilder、PromptSanitizer、PromptValidator、PromptVariableResolver、SystemPrompts等工具类
  - 新增SessionContextService，统一管理历史对话上下文
  - 新增SummaryTask、SummaryTaskStatus、SummaryType等总结相关模型
- **数据层**: 100%完成，Room数据库、网络层、仓库实现完整
  - 数据库版本升级至v8，新增prompt_templates、prompt_backups表
  - 完整的Migration脚本和测试（1→2→3→4→5→6→7→8）
  - 新增PromptFileStorage、PromptFileBackup、PromptRepositoryImpl等文件管理组件
  - 新增ConversationLogEntity、DailySummaryEntity、FailedSummaryTaskEntity等记忆系统实体
- **表现层**: 100%完成，UI组件、屏幕、ViewModel完整实现
  - 联系人画像记忆系统UI：100%完成，四标签页架构完整实现
  - ✅ 概览标签页：DynamicEmotionalHeader、LatestFactHookCard、TopTagsSection
  - ✅ 事实流标签页：TimelineView、ListView、FactStreamTopBar
  - ✅ 标签画像标签页：PersonaTab、CategorySection、GuessedTag/ConfirmedTag
  - ✅ 资料库标签页：DataVaultTab、DataSourceCard
  - 提示词编辑器UI：100%完成，完整的提示词工程界面
  - ✅ PromptEditorScreen：支持创建、编辑、验证提示词
  - ✅ PromptEditorViewModel：状态管理和业务逻辑
  - ✅ 提示词组件：CharacterCounter、DiscardConfirmDialog、InlineErrorBanner等
  - 悬浮窗功能重构UI：100%完成，Tab系统和状态管理完整实现
  - ✅ FloatingViewV2：支持分析/润色/回复三个功能Tab
  - ✅ TabSwitcher：Tab切换器和状态指示器
  - ✅ FloatingBubbleView：悬浮球状态指示与拖动
  - ✅ MaxHeightScrollView：自适应高度滚动视图
  - 新增AddFactToStreamDialog、EditConversationDialog、TagConfirmationDialog
  - 新增总结相关UI组件：SummaryProgressDialog、SummaryResultDialog等
- **依赖注入**: 100%完成，Hilt模块完整配置
  - 新增MemoryModule，管理记忆系统相关依赖
  - 新增PromptModule，管理提示词系统相关依赖
  - 新增DispatcherModule，统一管理协程调度器
  - 新增FloatingWindowModule，管理悬浮窗相关依赖
  - 新增NotificationModule，管理通知相关依赖
  - 新增SummaryModule，管理总结系统相关依赖
- **通知系统**: 100%完成，AI结果通知管理器完整实现
  - AiResultNotificationManager：统一管理AI完成后的系统通知
- **测试覆盖**: 99.1% (测试代码行数 / 源代码行数)
  - 单元测试：88个文件，24,470行
  - Android测试：4个文件
  - 新增提示词系统相关完整测试套件
  - 新增悬浮窗功能重构相关测试套件
  - 代码统计：219个Kotlin文件 (131个主代码 + 88个单元测试)

### ⚠️ 部分实现/待验证模块
- **输入内容身份识别与双向对话历史**: TD-00008技术设计完成
  - 任务状态：技术设计完成，待实现
  - 需要实现：IdentityPrefixHelper、UseCase层集成、系统提示词增强、UI渲染优化
  - 相关文档：TDD-00008-输入内容身份识别与双向对话历史技术设计.md
- **手动触发AI总结功能**: TD-00011技术设计完成
  - 任务状态：技术设计完成，待实现
  - 需要实现：ManualSummaryUseCase、SummaryTask、SummaryProgressDialog等
  - 相关文档：TDD-00011-手动触发AI总结功能技术设计.md
- **AI响应解析**: AiResponseParser接口已定义，但实现可能不完整
  - 代码架构存在：AiResponseParser接口、FallbackHandler等
  - ⚠️ 集成状态不明：需要验证解析器在实际AI调用中的使用情况
- **媒体处理**: FeedTextUseCase已实现，但AiRepositoryImpl中transcribeMedia方法未实现
  - 代码架构已设计：ExtractedData模型、AiRepository接口定义
  - ❌ 实际实现：AiRepositoryImpl.transcribeMedia直接返回未实现异常
  - 需要集成：FFmpeg音视频处理、ASR语音识别、OCR文字识别
- **规则引擎**: RuleEngine功能完整，但与实际业务流程的集成状态不明
  - 代码架构完整：RuleEngine、多种匹配策略
  - ⚠️ 集成状态不明：可能未在实际业务流程中被调用
- **悬浮窗服务**: FloatingWindowService代码庞大，需要验证与UI层的实际集成
  - ✅ BUG-00014悬浮球状态指示与启动模式修复已完成
  - ✅ BUG-00015三种模式上下文不共通问题修复已完成
  - ✅ TD-00009悬浮窗功能重构全部完成（46/46任务）
  - ✅ TD-00010悬浮球状态指示与拖动（23/26任务完成，88.5%）
- **无障碍服务**: WeChatDetector等工具类存在，但实际集成状态不明
  - 代码架构存在：WeChatDetector、FloatingWindowManager等
  - ❌ 实际集成未验证：需要确认与悬浮窗服务的协作