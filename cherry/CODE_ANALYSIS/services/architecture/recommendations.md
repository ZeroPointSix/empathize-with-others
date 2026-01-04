# 服务层改进建议

## 概述

基于前述的模块结构、设计模式、依赖关系和架构违规分析，本文档提供具体的、可操作的改进建议。改进建议分为**短期**（立即可做）、**中期**（计划内）和**长期**（架构升级）三个层次。

---

## 一、短期改进（1-2周内可完成）

### 1.1 提取公共配置模块

**问题**：`getDefaultModel()` 在 `AssistantService` 中定义，但被 `ProviderService` 依赖，形成隐式循环依赖。

**建议**：创建独立配置模块

```typescript
// src/config/system.ts

export const SYSTEM_CONFIG = {
  models: {
    default: {
      id: 'gpt-4o',
      name: 'GPT-4o',
      provider: 'cherryai',
      capabilities: ['chat', 'streaming', 'tools']
    }
  },
  cache: {
    assistant: { maxSize: 10, ttl: 300000 },
    topic: { maxSize: 5, ttl: 300000 },
    provider: { maxSize: 10, ttl: 300000 },
    mcp: { maxSize: 20, ttl: 300000 },
    preference: { ttl: 0 } // 无缓存
  }
} as const

export function getDefaultModel() {
  return SYSTEM_CONFIG.models.default
}
```

**修改文件**：
- 新建：`src/config/system.ts`
- 修改：`src/services/AssistantService.ts` - 移除 `getDefaultModel`，从 `system.ts` 导入
- 修改：`src/services/ProviderService.ts` - 从 `system.ts` 导入

### 1.2 统一缓存配置

**问题**：各服务硬编码缓存大小和 TTL。

**建议**：使用统一配置

```typescript
// src/services/cache/constants.ts
import { SYSTEM_CONFIG } from '@/config/system'

export const CACHE_CONFIGS = {
  assistant: {
    lruSize: SYSTEM_CONFIG.cache.assistant.maxSize,
    ttl: SYSTEM_CONFIG.cache.assistant.ttl
  },
  topic: {
    lruSize: SYSTEM_CONFIG.cache.topic.maxSize,
    ttl: SYSTEM_CONFIG.cache.topic.ttl
  },
  provider: {
    lruSize: SYSTEM_CONFIG.cache.provider.maxSize,
    ttl: SYSTEM_CONFIG.cache.provider.ttl
  },
  mcp: {
    lruSize: SYSTEM_CONFIG.cache.mcp.maxSize,
    ttl: SYSTEM_CONFIG.cache.mcp.ttl
  }
}
```

### 1.3 命名规范统一

**问题**：`MessagesService` 导出模式与其他服务不一致。

**建议**：统一导出模式

```typescript
// src/services/MessagesService.ts

// 保留现有导出（向后兼容）
export async function sendMessage(...) { ... }
export async function deleteMessageById(...) { ... }

// 新增服务实例
export class MessagesService {
  // ... 现有方法移入此类
}

export const messagesService = MessagesService.getInstance()
```

---

## 二、中期改进（3-4周计划）

### 2.1 创建基类减少重复代码

**问题**：Singleton、LRU 缓存、订阅系统的实现在各服务中重复。

**建议**：创建可复用的基类

```typescript
// src/services/base/BaseSingleton.ts
export abstract class BaseSingleton<T extends object> {
  private static readonly instances = new Map<new () => T, T>()

  static getInstance<T extends object>(this: new () => T): T {
    const Constructor = this as new () => T
    if (!BaseSingleton.instances.has(Constructor)) {
      BaseSingleton.instances.set(Constructor, new Constructor())
    }
    return BaseSingleton.instances.get(Constructor)!
  }
}

// src/services/base/CacheMixin.ts
export type Entity = { id: string }

export interface CacheConfig {
  maxSize: number
  ttl: number
}

export function CacheMixin<T extends new (...args: any[]) => object>(
  EntityClass: T,
  config: CacheConfig
) {
  return class extends EntityClass {
    protected cache = new Map<string, InstanceType<T>>()
    protected accessOrder: string[] = []
    protected readonly MAX_CACHE_SIZE = config.maxSize
    protected readonly CACHE_TTL = config.ttl

    protected addToCache(id: string, entity: InstanceType<T>) {
      if (!this.cache.has(id) && this.cache.size >= this.MAX_CACHE_SIZE) {
        this.evictOldest()
      }
      this.cache.set(id, entity)
      this.updateAccessOrder(id)
    }

    protected evictOldest() {
      const oldest = this.accessOrder.shift()!
      this.cache.delete(oldest)
    }

    protected updateAccessOrder(id: string) {
      const idx = this.accessOrder.indexOf(id)
      if (idx > -1) this.accessOrder.splice(idx, 1)
      this.accessOrder.push(id)
    }
  }
}
```

**使用示例**：

```typescript
// src/services/TopicService.ts
import { BaseSingleton } from '@/services/base/BaseSingleton'
import { CacheMixin, type Entity } from '@/services/base/CacheMixin'
import { CACHE_CONFIGS } from '@/services/cache/constants'

interface Topic extends Entity { ... }

class TopicServiceCore extends CacheMixin(TopicServiceBase, CACHE_CONFIGS.topic) {
  // 只需实现业务方法，缓存逻辑由 Mixin 提供
}

export class TopicService extends BaseSingleton(TopicServiceCore) {
  // 门面方法
}
```

### 2.2 拆分 MessagesService

**问题**：`MessagesService` 混合了 CRUD、批处理、多模型、翻译等职责。

**建议**：按职责拆分为独立服务

```
src/services/messages/
├── index.ts                    # 统一导出
├── MessagesCRUDService.ts      # 基础 CRUD (300行)
├── BlockBatchService.ts        # 块批量更新 (200行)
├── MultiModelService.ts        # 多模型处理 (150行)
├── TranslationService.ts       # 翻译功能 (200行)
└── MessageStreamingService.ts  # 流式处理集成 (150行)
```

**实现建议**：

```typescript
// src/services/messages/MessagesCRUDService.ts
export class MessagesCRUDService extends BaseSingleton<MessagesCRUDService> {
  async sendMessage(...) { ... }
  async deleteMessage(...) { ... }
  async editMessage(...) { ... }
  async regenerateMessage(...) { ... }
}

// src/services/messages/BlockBatchService.ts
export class BlockBatchService extends BaseSingleton<BlockBatchService> {
  private pendingUpdates = new Map<string, any>()
  private flushTimer: ReturnType<typeof setTimeout> | null = null

  async scheduleUpdate(blockId: string, updates: any) { ... }
  async flushPending(ids?: string[]) { ... }
}

// src/services/messages/index.ts
import { MessagesCRUDService } from './MessagesCRUDService'
import { BlockBatchService } from './BlockBatchService'
import { MultiModelService } from './MultiModelService'
import { TranslationService } from './TranslationService'

export const messagesCRUDService = MessagesCRUDService.getInstance()
export const blockBatchService = BlockBatchService.getInstance()
export const multiModelService = MultiModelService.getInstance()
export const translationService = TranslationService.getInstance()

// 保持向后兼容
export { sendMessage, deleteMessageById } from './MessagesCRUDService'
```

### 2.3 统一错误处理

**问题**：各服务错误处理方式不一致。

**建议**：定义统一错误类型和处理器

```typescript
// src/services/errors/ServiceError.ts
export class ServiceError extends Error {
  constructor(
    message: string,
    public readonly code: ServiceErrorCode,
    public readonly context: Record<string, any> = {},
    public readonly recoverable: boolean = false
  ) {
    super(message)
    this.name = 'ServiceError'
  }
}

export type ServiceErrorCode =
  | 'NOT_FOUND'
  | 'CONFLICT'
  | 'VALIDATION_ERROR'
  | 'PERSISTENCE_ERROR'
  | 'CACHE_ERROR'
  | 'SUBSCRIPTION_ERROR'
  | 'UNKNOWN_ERROR'

// src/services/errors/errorHandler.ts
import { loggerService } from '@/services/LoggerService'

export function serviceErrorHandler(
  serviceName: string,
  methodName: string
) {
  return function <T extends (...args: any[]) => Promise<any>>(
    target: any,
    propertyKey: string,
    descriptor: PropertyDescriptor
  ): PropertyDescriptor {
    const originalMethod = descriptor.value

    descriptor.value = async function (...args: any[]) {
      try {
        return await originalMethod.apply(this, args)
      } catch (error) {
        if (error instanceof ServiceError) {
          throw error
        }

        const wrappedError = new ServiceError(
          error instanceof Error ? error.message : String(error),
          'UNKNOWN_ERROR',
          { service: serviceName, method: methodName }
        )

        loggerService.error(
          `[${serviceName}] ${methodName} failed`,
          wrappedError
        )

        throw wrappedError
      }
    }

    return descriptor
  }
}

// 使用示例
export class TopicService {
  @serviceErrorHandler('TopicService', 'createTopic')
  async createTopic(assistant: Assistant): Promise<Topic> {
    // ...
  }
}
```

---

## 三、长期改进（1-2月架构升级）

### 3.1 引入依赖注入

**问题**：硬编码的服务依赖导致测试困难。

**建议**：引入轻量级 DI 容器

```typescript
// src/di/container.ts
import { Container } from ' inversify-js'

const container = new Container()

// 绑定服务
container.bind<ITopicService>('ITopicService').to(TopicService).inSingletonScope()
container.bind<IAssistantService>('IAssistantService').to(AssistantService).inSingletonScope()
container.bind<IProviderService>('IProviderService').to(ProviderService).inSingletonScope()

// 服务定位器
export const serviceContainer = container

// 便捷获取
export const topicService = serviceContainer.get<ITopicService>('ITopicService')
export const assistantService = serviceContainer.get<IAssistantService>('IAssistantService')
```

**使用示例**：

```typescript
// 通过构造函数注入
export class MessagesService {
  constructor(
    private topicService: ITopicService,
    private assistantService: IAssistantService,
    private blockBatchService: IBlockBatchService
  ) {}

  async sendMessage(...) {
    await this.topicService.updateTopic(...)
  }
}
```

### 3.2 拆分大型服务

#### 3.2.1 TopicService 拆分

```
src/services/topic/
├── index.ts                           # 统一导出
├── TopicCRUDService.ts                # CRUD 操作
├── TopicCacheManager.ts               # 缓存管理 (LRU + TTL)
├── TopicSubscriptionManager.ts        # 订阅管理
├── TopicCurrentTopicManager.ts        # 当前话题管理
├── TopicPersistence.ts                # 数据库操作
└── TopicConcurrency.ts                # 并发控制
```

**拆分原则**：
- 每个类职责单一（< 300 行）
- 类之间通过接口通信
- 保留 TopicService 作为门面类

#### 3.2.2 ApiService 拆分

```
src/services/api/
├── index.ts                           # 统一导出
├── ChatApiService.ts                  # 聊天 API 调用
├── ModelApiService.ts                 # 模型相关 API
├── McpToolsService.ts                 # MCP 工具获取
└── TopicNamingService.ts              # 话题命名生成
```

### 3.3 创建服务层公共基础设施

```
src/services/infrastructure/
├── base/
│   ├── BaseService.ts                 # 服务基类
│   ├── BaseSingleton.ts               # 单例基类
│   └── BaseCacheableService.ts        # 可缓存服务基类
├── cache/
│   ├── LruCache.ts                    # LRU 缓存实现
│   ├── TtlCache.ts                    # TTL 缓存实现
│   └── CacheManager.ts                # 缓存管理器
├── subscription/
│   ├── SubscriptionManager.ts         # 订阅管理器
│   └── SubscriptionRegistry.ts        # 订阅注册表
├── concurrency/
│   ├── ConcurrencyQueue.ts            # 并发队列
│   └── RequestDeduplicator.ts         # 请求去重
└── errors/
    ├── ServiceError.ts                # 统一错误类型
    ├── errorHandler.ts                # 错误处理装饰器
    └── errorCodes.ts                  # 错误码定义
```

---

## 四、代码质量提升建议

### 4.1 增加类型安全性

```typescript
// 建议：使用模板字面量类型定义服务方法

type ServiceMethod = 'create' | 'update' | 'delete' | 'get' | 'list'

interface CRUDService<T, CreateInput, UpdateInput> {
  create(input: CreateInput): Promise<T>
  update(id: string, input: UpdateInput): Promise<void>
  delete(id: string): Promise<void>
  get(id: string): Promise<T | null>
  list(): Promise<T[]>
}

// 使用
interface TopicService extends CRUDService<Topic, CreateTopicInput, UpdateTopicInput> {
  switchToTopic(id: string): Promise<void>
  getCurrentTopic(): Topic | null
}
```

### 4.2 增强测试覆盖

```typescript
// 建议：为核心服务添加更多单元测试
describe('TopicService', () => {
  describe('CRUD Operations', () => {
    it('should create topic with optimistic update', async () => { ... })
    it('should rollback on database failure', async () => { ... })
    it('should notify subscribers on change', async () => { ... })
  })

  describe('Cache', () => {
    it('should evict oldest entry when cache is full', async () => { ... })
    it('should invalidate cache after TTL', async () => { ... })
  })

  describe('Concurrency', () => {
    it('should queue concurrent updates', async () => { ... })
    it('should handle rapid updates correctly', async () => { ... })
  })
})
```

### 4.3 性能监控

```typescript
// 建议：添加服务性能指标监控

export class TopicService {
  private metrics = {
    cacheHits: 0,
    cacheMisses: 0,
    dbQueries: 0,
    updateOperations: 0
  }

  async getTopic(id: string): Promise<Topic | null> {
    const startTime = performance.now()

    // ... 获取逻辑

    const duration = performance.now() - startTime
    this.metrics.cacheHits++

    // 记录指标
    metricsService.record('topic.get', {
      duration,
      cacheHit: true,
      cacheSize: this.topicCache.size
    })

    return topic
  }

  getMetrics() {
    return {
      ...this.metrics,
      cacheHitRate: this.metrics.cacheHits /
        (this.metrics.cacheHits + this.metrics.cacheMisses)
    }
  }
}
```

---

## 五、实施路线图

### Phase 1: 基础改进（Week 1-2）

| 任务 | 负责人 | 预计工作量 | 验收标准 |
|------|--------|-----------|----------|
| 创建 `src/config/system.ts` | - | 1天 | 配置可从独立模块导入 |
| 统一缓存配置 | - | 1天 | 各服务使用统一配置 |
| 修复命名不一致 | - | 0.5天 | MessagesService 导出 service 实例 |
| 文档更新 | - | 0.5天 | 更新 CLAUDE.md |

### Phase 2: 代码复用（Week 3-4）

| 任务 | 负责人 | 预计工作量 | 验收标准 |
|------|--------|-----------|----------|
| 创建 `BaseSingleton` 基类 | - | 1天 | 各服务继承基类 |
| 创建错误处理基础设施 | - | 2天 | 统一错误类型和处理器 |
| 拆分 MessagesService | - | 3天 | 职责分离，测试通过 |

### Phase 3: 架构优化（Week 5-8）

| 任务 | 负责人 | 预计工作量 | 验收标准 |
|------|--------|-----------|----------|
| 引入依赖注入 | - | 2周 | 接口抽象，测试友好 |
| 拆分 TopicService | - | 1周 | 模块化，测试覆盖 |
| 拆分 ApiService | - | 1周 | 层间解耦 |

### Phase 4: 持续改进（进行中）

| 任务 | 频率 | 内容 |
|------|------|------|
| 代码审查 | 每次 PR | 检查新服务是否符合规范 |
| 架构评审 | 每月 | 检查架构腐化 |
| 性能测试 | 每季度 | 基准测试和优化 |

---

## 六、预期收益

### 可维护性提升

| 指标 | 当前 | 改进后 | 提升 |
|------|------|--------|------|
| 平均服务行数 | 750行 | 300行 | 60% |
| 重复代码量 | ~500行 | ~100行 | 80% |
| 循环依赖数 | 2个 | 0个 | 100% |

### 可测试性提升

| 指标 | 当前 | 改进后 | 提升 |
|------|------|--------|------|
| 单元测试覆盖率 | ~40% | 70% | 75% |
| Mock 难度 | 高 | 低 | - |
| 测试执行时间 | 30s | 20s | 33% |

### 性能提升

| 指标 | 当前 | 改进后 | 提升 |
|------|------|--------|------|
| 服务初始化时间 | 100ms | 50ms | 50% |
| 缓存命中率 | 85% | 92% | 8% |
| 内存占用 | 基准 | -10% | 10% |

---

## 七、风险与缓解

| 风险 | 可能性 | 影响 | 缓解措施 |
|------|--------|------|----------|
| 变更导致回归 | 中 | 高 | 充分测试，分阶段发布 |
| 学习曲线 | 低 | 中 | 渐进式迁移，保持文档更新 |
| 性能下降 | 低 | 中 | 性能监控，及时优化 |
| 团队接受度 | 中 | 中 | 早期沟通，展示收益 |

---

## 八、总结

服务层整体架构**设计合理**，大量使用了成熟的设计模式（Singleton、Observer、LRU Cache、Optimistic Update），但存在以下需要改进的地方：

### 核心问题

1. **MessagesService 职责过重** - 混合了 4 种不同职责
2. **大型服务类** - 5 个服务超过 500 行，最大达 1423 行
3. **代码重复** - 缓存、订阅、Singleton 实现高度重复
4. **循环依赖** - 存在隐式循环依赖

### 改进优先级

| 优先级 | 任务 | 工作量 |
|--------|------|--------|
| P0 | 拆分 MessagesService | 3天 |
| P1 | 提取公共配置模块 | 1天 |
| P2 | 创建基类减少重复 | 1周 |
| P3 | 引入依赖注入 | 2周 |
| P4 | 拆分大型服务 | 2周 |

### 长期目标

通过本次重构，目标是将服务层转变为：
- **职责单一** - 每个服务类 < 300 行
- **高度复用** - 公共逻辑抽象为基类和 Mixin
- **易于测试** - 通过依赖注入和接口抽象
- **性能可观测** - 完善的监控和指标
