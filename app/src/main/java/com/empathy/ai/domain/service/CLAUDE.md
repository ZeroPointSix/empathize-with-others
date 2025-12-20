[根目录](../../../../CLAUDE.md) > [domain](../) > **service**

# domain/service - 领域服务

## 模块职责

领域服务层包含无法自然归属到实体或值对象的复杂业务逻辑，提供跨聚合的业务操作。这些服务是领域层的核心组件，负责处理复杂的业务规则和协调多个领域对象。

## 服务列表

### 1. PrivacyEngine - 隐私脱敏引擎
**文件**: `PrivacyEngine.kt`
**职责**: 在发送数据给AI之前进行脱敏处理，保护用户隐私
**核心功能**:
- 基于映射规则的脱敏替换
- 基于正则表达式的自动检测脱敏
- 混合脱敏模式（映射+自动检测）
- 敏感信息扫描和检测

**关键特性**:
```kotlin
// 自动检测的敏感信息类型
object Patterns {
    val PHONE_NUMBER = "1[3-9]\\d{9}".toRegex()      // 中国手机号
    val ID_CARD = "\\d{17}[\\dXx]".toRegex()          // 身份证号
    val EMAIL = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}".toRegex()  // 邮箱
}

// 混合脱敏
fun maskHybrid(
    rawText: String,
    privacyMapping: Map<String, String> = emptyMap(),
    enabledPatterns: List<String> = emptyList()
): String
```

**设计原则**:
- 无状态设计：使用object单例，便于测试和复用
- 模式优先级：手机号优先于身份证号，避免18位数字误判
- 重叠处理：智能处理重叠的敏感信息，避免重复脱敏

### 2. SessionContextService - 会话上下文服务
**文件**: `SessionContextService.kt`
**职责**: 统一管理历史对话上下文的构建，解决三种模式上下文不共通问题
**解决的问题**: BUG-00015 三种模式（分析/润色/回复）上下文不共通

**核心功能**:
- 获取联系人的历史对话上下文
- 支持自定义历史条数
- 快速检查是否有历史对话
- 构建带时间流逝标记的上下文

**使用场景**:
```kotlin
// 所有UseCase统一调用此服务
val historyContext = sessionContextService.getHistoryContext(contactId)

// 支持自定义条数
val customHistory = sessionContextService.getHistoryContext(contactId, limit = 10)
```

**设计特点**:
- 单例服务：使用@Singleton确保全局唯一
- 配置驱动：通过SettingsRepository读取用户配置
- 容错设计：失败时返回空历史，不影响主流程
- 性能优化：避免重复查询，复用历史上下文

### 3. RuleEngine - 业务规则引擎
**文件**: `RuleEngine.kt`
**职责**: 提供可扩展的规则匹配系统，支持多种匹配策略
**核心功能**:
- 支持精确匹配、子串匹配、正则匹配
- 规则优先级管理
- 避免重复匹配的范围检查
- 策略模式设计，便于扩展

**匹配策略**:
```kotlin
interface RuleMatchStrategy {
    fun matches(text: String, pattern: String): Boolean
}

// 三种内置策略
class ExactMatchStrategy : RuleMatchStrategy      // 精确匹配
class SubstringMatchStrategy : RuleMatchStrategy  // 子串匹配
class RegexMatchStrategy : RuleMatchStrategy      // 正则匹配
```

**使用示例**:
```kotlin
val engine = RuleEngine()

// 添加规则
engine.addRule(BusinessRule(
    id = "no_money",
    name = "禁止提及money",
    pattern = "money",
    matchType = MatchType.SUBSTRING,
    priority = 100
))

// 执行检查
val matches = engine.evaluate("I need money")
```

**特性**:
- 优先级排序：按优先级从高到低执行
- 范围去重：避免同一文本被多个规则重复匹配
- 策略可扩展：支持自定义匹配策略

### 4. FloatingWindowService - 悬浮窗服务
**文件**: `FloatingWindowService.kt`
**职责**: 管理悬浮窗生命周期和用户交互
**核心功能**:
- 悬浮窗的创建、显示、隐藏
- 用户交互事件处理
- AI功能调用（分析、润色、回复）
- 前台服务管理

**注入的UseCase**:
```kotlin
@Inject lateinit var analyzeChatUseCase: AnalyzeChatUseCase
@Inject lateinit var checkDraftUseCase: CheckDraftUseCase
@Inject lateinit var polishDraftUseCase: PolishDraftUseCase
@Inject lateinit var generateReplyUseCase: GenerateReplyUseCase
@Inject lateinit var refinementUseCase: RefinementUseCase
```

**生命周期**:
1. **onCreate**: 初始化WindowManager和性能监控
2. **onStartCommand**: 启动前台服务并显示悬浮视图
3. **onDestroy**: 移除悬浮视图并清理资源

**UI版本管理**:
- 旧版View：保持兼容性
- 新版ViewV2：TD-00009实现的三Tab界面
- 动态切换：通过useNewUI标志控制

## 设计模式

### 1. 策略模式 (RuleEngine)
- **策略接口**: RuleMatchStrategy
- **具体策略**: ExactMatchStrategy、SubstringMatchStrategy、RegexMatchStrategy
- **上下文**: RuleEngine管理策略选择和执行

### 2. 单例模式 (PrivacyEngine)
- **实现方式**: Kotlin object
- **优势**: 全局唯一、线程安全、便于测试
- **使用场景**: 无状态的工具类服务

### 3. 服务模式 (SessionContextService)
- **依赖注入**: 使用Hilt管理依赖
- **生命周期**: @Singleton单例
- **职责边界**: 专注于历史上下文管理

### 4. 门面模式 (FloatingWindowService)
- **统一接口**: 为UI层提供简化的接口
- **复杂性封装**: 隐藏UseCase调用的复杂性
- **协调者角色**: 协调多个UseCase的执行

## 关键设计决策

### 1. 无状态 vs 有状态
- **PrivacyEngine**: 无状态设计，便于并发和测试
- **SessionContextService**: 有状态（依赖Repository），但通过依赖注入管理
- **RuleEngine**: 有状态（规则列表），提供规则管理接口

### 2. 同步 vs 异步
- **PrivacyEngine**: 同步设计，纯计算逻辑
- **SessionContextService**: 异步设计，涉及数据库查询
- **RuleEngine**: 同步设计，内存计算

### 3. 可扩展性设计
- **RuleEngine**: 策略模式支持自定义匹配算法
- **PrivacyEngine**: 模式定义可扩展，支持新的敏感信息类型
- **FloatingWindowService**: UI版本可配置，支持新旧切换

## 性能优化

### 1. 避免重复计算
- SessionContextService统一管理历史上下文
- RuleEngine记录已处理范围，避免重复匹配

### 2. 批量处理
- PrivacyEngine提供maskBatch方法支持批量脱敏
- RuleEngine支持批量添加规则

### 3. 懒加载
- FloatingWindowService的性能监控按需启动
- SessionContextService按需查询历史对话

## 错误处理

### 1. 降级策略
- SessionContextService失败时返回空历史
- RuleEngine正则表达式无效时回退到子串匹配
- FloatingWindowService异常时不影响主应用

### 2. 日志记录
- 使用统一的日志标签和级别
- 关键操作失败时记录详细信息
- 性能监控记录关键指标

## 测试策略

### 1. 单元测试
- PrivacyEngine的脱敏逻辑测试
- RuleEngine的规则匹配测试
- SessionContextService的上下文构建测试

### 2. 集成测试
- FloatingWindowService与UseCase的集成测试
- SessionContextService与Repository的集成测试

### 3. Mock策略
- Repository使用Mock进行测试
- UseCase使用Mock验证调用

## 相关文件清单

- `PrivacyEngine.kt` - 隐私脱敏引擎实现
- `SessionContextService.kt` - 会话上下文服务实现
- `RuleEngine.kt` - 业务规则引擎实现
- `FloatingWindowService.kt` - 悬浮窗服务实现

## 变更记录 (Changelog)

### 2025-12-20 - Claude (模块文档创建)
- **创建domain/service模块的CLAUDE.md文档**
- **整理所有服务的职责和核心功能**
- **添加设计模式和使用示例**

### 2025-12-19 - Kiro (BUG修复)
- **修复SessionContextService创建，解决BUG-00015**
- **三种模式（分析/润色/回复）上下文统一管理**

### 2025-12-15 - Kiro (隐私增强)
- **增强PrivacyEngine的自动检测能力**
- **添加重叠检测和优先级处理**

### 2025-12-10 - Kiro (规则引擎)
- **实现RuleEngine基础框架**
- **支持多种匹配策略和优先级**

---

**最后更新**: 2025-12-20 | 更新者: Claude
**维护者**: hushaokang
**文档版本**: v1.0.0
**Git提交**: 阶段C-深度补捞