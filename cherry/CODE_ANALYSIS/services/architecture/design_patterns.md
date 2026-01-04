# 服务层设计模式分析

## 概述

Cherry Studio 服务层大量使用经典设计模式，主要包括 **Singleton 模式**、**Observer 模式**、**LRU 缓存模式**、**策略模式** 和 **建造者模式**。这些模式的应用使代码结构清晰、易于维护，但也存在一些过度使用的情况。

---

## 1. Singleton 模式（单例模式）

### 应用场景

所有核心服务都采用 Singleton 模式确保全局唯一实例：

```typescript
// 标准实现（AssistantService）
export class AssistantService {
  private static instance: AssistantService

  private constructor() { ... }

  public static getInstance(): AssistantService {
    if (!AssistantService.instance) {
      AssistantService.instance = new AssistantService()
    }
    return AssistantService.instance
  }
}

export const assistantService = AssistantService.getInstance()
```

### 服务清单

| 服务 | 实例化方式 | 线程安全 |
|------|-----------|----------|
| `AssistantService` | `getInstance()` | ✅ 懒加载 |
| `TopicService` | `getInstance()` | ✅ 懒加载 |
| `ProviderService` | `getInstance()` | ✅ 懒加载 |
| `PreferenceService` | `getInstance()` | ✅ 懒加载 |
| `McpService` | `getInstance()` | ✅ 懒加载 |
| `LoggerService` | `getInstance()` | ✅ 懒加载 |

### 优点

1. **全局状态一致性** - 确保所有组件访问同一实例
2. **延迟初始化** - 实例在首次使用时创建
3. **资源节约** - 避免重复创建大型对象

### 缺点与风险

1. **测试困难** - 单元测试难以模拟单例状态
2. **隐藏依赖** - 跨模块依赖不显式
3. **全局状态污染** - 状态可能在意外时刻被修改

### 建议改进

```typescript
// 可选：依赖注入友好的设计
interface IAssistantService {
  getAssistant(id: string): Promise<Assistant | null>
  createAssistant(assistant: Assistant): Promise<Assistant>
}

// 生产环境单例
const assistantService: IAssistantService = AssistantService.getInstance()

// 测试环境模拟
const mockAssistantService: IAssistantService = {
  getAssistant: jest.fn(),
  createAssistant: jest.fn()
}
```

---

## 2. Observer 模式（观察者模式）

### 应用场景

所有核心服务都实现了发布-订阅机制，支持 React `useSyncExternalStore` 集成：

```typescript
// TopicService 订阅系统实现
export class TopicService {
  private topicSubscribers = new Map<string, Set<() => void>>()
  private globalSubscribers = new Set<() => void>()
  private allTopicsSubscribers = new Set<() => void>()

  public subscribeTopic(topicId: string, callback: () => void): UnsubscribeFunction {
    if (!this.topicSubscribers.has(topicId)) {
      this.topicSubscribers.set(topicId, new Set())
    }
    const subscribers = this.topicSubscribers.get(topicId)!
    subscribers.add(callback)

    return () => {
      subscribers.delete(callback)
      if (subscribers.size === 0) {
        this.topicSubscribers.delete(topicId)
      }
    }
  }

  private notifyTopicSubscribers(topicId: string): void {
    const subscribers = this.topicSubscribers.get(topicId)
    if (subscribers) {
      subscribers.forEach(callback => {
        try { callback() } catch (error) { logger.error(...) }
      })
    }
  }
}
```

### 订阅类型

| 订阅类型 | 作用域 | 用途 |
|---------|--------|------|
| `subscribeXxx(id)` | 单个实体 | 细粒度 UI 更新 |
| `subscribeAll()` | 全局 | 调试或全局同步 |
| `subscribeAllXxx()` | 列表 | 列表视图刷新 |

### 模式优势

1. **解耦** - 主题与观察者之间不直接依赖
2. **动态订阅** - 可随时添加/移除订阅者
3. **React 集成** - 与 `useSyncExternalStore` 天然适配

### 潜在问题

1. **内存泄漏风险** - 未正确取消订阅
2. **回调顺序不确定** - 可能导致状态不一致

---

## 3. LRU Cache 模式（最近最少使用缓存）

### 应用场景

核心服务实现多级缓存策略：

```typescript
// AssistantService 缓存层次
export class AssistantService {
  // 第1层：系统助手永久缓存
  private systemAssistantsCache = new Map<string, Assistant>()

  // 第2层：LRU 缓存（最多10个）
  private assistantCache = new Map<string, Assistant>()
  private readonly MAX_CACHE_SIZE = 10
  private accessOrder: string[] = []

  // 第3层：全量缓存（带 TTL）
  private allAssistantsCache = new Map<string, Assistant>()
  private readonly CACHE_TTL = 5 * 60 * 1000 // 5分钟
}
```

### 缓存策略对比

| 服务 | 永久缓存 | LRU 大小 | TTL | 全量缓存 |
|------|----------|----------|-----|----------|
| AssistantService | ✅ (系统助手) | 10 | 5分钟 | ✅ |
| TopicService | ✅ (当前话题) | 5 | 5分钟 | ✅ |
| ProviderService | ✅ (默认) | 10 | 5分钟 | ✅ |
| McpService | ❌ | 20 | 5分钟 | ✅ |

### LRU 实现

```typescript
private addToCache(assistantId: string, assistant: Assistant): void {
  // 缓存满时驱逐最旧项
  if (!this.assistantCache.has(assistantId) &&
      this.assistantCache.size >= this.MAX_CACHE_SIZE) {
    this.evictOldestFromCache()
  }

  this.assistantCache.set(assistantId, assistant)
  this.updateAccessOrder(assistantId)
}

private evictOldestFromCache(): void {
  const oldestAssistantId = this.accessOrder.shift()!
  this.assistantCache.delete(oldestAssistantId)
}
```

### 缓存优势

1. **减少数据库查询** - 内存访问 vs 磁盘 I/O
2. **降低延迟** - 同步读取路径
3. **自动失效** - TTL 机制确保数据新鲜度

---

## 4. Strategy 模式（策略模式）

### 应用场景

`StreamProcessingService` 使用策略模式处理不同类型的消息块：

```typescript
// 策略接口
interface StreamProcessorCallbacks {
  onTextStart?: () => void
  onTextChunk?: (text: string) => void
  onTextComplete?: (text: string) => void
  onThinkingStart?: () => void
  onThinkingChunk?: (text: string, time?: number) => void
  onImageCreated?: () => void
  // ... 更多策略
}

// 工厂函数创建策略处理器
export function createStreamProcessor(callbacks: StreamProcessorCallbacks = {}) {
  return (chunk: Chunk) => {
    switch (chunk.type) {
      case ChunkType.TEXT_START:
        callbacks.onTextStart?.()
        break
      case ChunkType.TEXT_DELTA:
        callbacks.onTextChunk?.(chunk.text)
        break
      case ChunkType.IMAGE_DELTA:
        callbacks.onImageDelta?.(chunk.image)
        break
      // ... 更多分支
    }
  }
}
```

### BlockManager 智能更新策略

```typescript
export class BlockManager {
  async smartBlockUpdate(
    blockId: string,
    changes: Partial<MessageBlock>,
    blockType: MessageBlockType,
    isComplete: boolean = false
  ) {
    const isBlockTypeChanged = this._lastBlockType !== null &&
                               this._lastBlockType !== blockType

    if (isBlockTypeChanged || isComplete) {
      // 策略1：类型变化或完成 → 立即更新
      if (isBlockTypeChanged) {
        await this.deps.cancelThrottledBlockUpdate(this._activeBlockInfo!.id)
      }
      await messageBlockDatabase.updateOneBlock({ id: blockId, changes })
    } else {
      // 策略2：同类型继续 → 节流更新
      await this.deps.throttledBlockUpdate(blockId, changes)
    }
  }
}
```

---

## 5. Optimistic Update 模式（乐观更新）

### 应用场景

所有 CRUD 服务都采用乐观更新策略：

```typescript
// TopicService 乐观更新
public async updateTopic(topicId: string, updates: Partial<Topic>): Promise<void> {
  // 1. 先更新缓存
  this.updateTopicInCache(topicId, { ...updates, updatedAt: Date.now() })
  this.notifyCurrentTopicSubscribers()

  // 2. 后台持久化
  try {
    await topicDatabase.upsertTopics([updatedTopic])
  } catch (error) {
    // 3. 失败回滚
    this.rollbackCache(topicId, oldTopic)
    this.notifyCurrentTopicSubscribers()
    throw error
  }
}
```

### 流程图

```
UI 操作
   │
   ▼
┌─────────────────┐
│ 更新内存缓存     │ ← 乐观更新
└────────┬────────┘
         │ 即时返回
         ▼
┌─────────────────┐
│ 通知订阅者       │ ← UI 立即刷新
└────────┬────────┘
         │
         ▼ (异步)
┌─────────────────┐
│ 持久化到数据库   │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
  成功       失败
    │         │
    ▼         ▼
  完成    ┌─────────────────┐
          │ 回滚缓存         │
          └─────────────────┘
          │ 重新通知订阅者    │
          ▼
        UI 回滚
```

---

## 6. Queue Pattern（队列模式）

### 应用场景

并发控制队列防止竞态条件：

```typescript
export class TopicService {
  private updateQueue = new Map<string, Promise<void>>()

  public async updateTopic(topicId: string, updates: Partial<Topic>): Promise<void> {
    // 等待同一实体的前序更新完成
    const previousUpdate = this.updateQueue.get(topicId)
    if (previousUpdate) {
      await previousUpdate
    }

    // 执行当前更新
    const currentUpdate = this.performTopicUpdate(topicId, updates)
    this.updateQueue.set(topicId, currentUpdate)

    try {
      await currentUpdate
    } finally {
      if (this.updateQueue.get(topicId) === currentUpdate) {
        this.updateQueue.delete(topicId)
      }
    }
  }
}
```

### 优势

1. **防止竞态** - 同一实体的更新顺序执行
2. **自动等待** - 后到的请求自动等待先到的完成
3. **资源友好** - 避免重复查询/更新

---

## 7. Anti-Patterns（反模式）

### 1. God Service（上帝服务）

**问题**：`TopicService` (1423行)、`AssistantService` (903行)、`ProviderService` (1094行) 过于庞大

**表现**：
- 单一类承担过多职责
- 难以测试和维护
- 修改容易引入副作用

**建议**：拆分为多个协作类

```typescript
// 建议：拆分为多个职责单一的服务
class TopicCacheManager { ... }     // 缓存管理
class TopicPersistence { ... }      // 持久化
class TopicNotification { ... }     // 通知
class TopicService {
  constructor(
    private cache: TopicCacheManager,
    private persistence: TopicPersistence,
    private notification: TopicNotification
  ) {}
}
```

### 2. Duplicate Cache Logic（重复缓存逻辑）

**问题**：每个服务的缓存管理代码高度相似

**表现**：
- LRU 实现逻辑在 5 个服务中重复
- 订阅系统代码几乎一致
- TTL 检查机制相同

**建议**：抽象为通用基类或 Mixin

```typescript
// 建议：通用缓存服务
abstract class CachedService<Entity> {
  protected lruCache = new Map<string, Entity>()
  protected ttlCache = new Map<string, { data: Entity; timestamp: number }>()

  protected getWithCache(id: string): Entity | null { ... }
  protected setLru(id: string, entity: Entity): void { ... }
  protected evictOldest(): void { ... }
}
```

### 3. Mixed Concerns（混合关注点）

**问题**：`MessagesService` 混合多种职责

**表现**：
- 消息 CRUD
- 消息块节流更新
- 多模型响应分发
- 翻译功能

**建议**：按职责拆分

```typescript
// 建议拆分为
MessageCRUDService        // 基础 CRUD
BlockBatchUpdateService   // 块批量更新
MultiModelService         // 多模型处理
TranslationService        // 翻译功能
```

---

## 8. 模式使用统计

| 模式 | 使用频率 | 应用成熟度 |
|------|---------|-----------|
| Singleton | ⭐⭐⭐⭐⭐ 普遍 | 成熟 |
| Observer | ⭐⭐⭐⭐⭐ 普遍 | 成熟 |
| LRU Cache | ⭐⭐⭐⭐ 核心服务 | 成熟 |
| Optimistic Update | ⭐⭐⭐⭐ 核心服务 | 成熟 |
| Queue | ⭐⭐⭐ 部分服务 | 良好 |
| Strategy | ⭐⭐ 流处理模块 | 良好 |
| Builder | ⭐⭐ 少用 | 一般 |

---

## 9. 建议与总结

### 正面评价

1. **设计模式应用广泛** - 代码结构清晰、可维护
2. **缓存策略完善** - 多级缓存显著提升性能
3. **乐观更新** - UI 响应性好
4. **订阅机制** - 与 React 生态良好集成

### 改进建议

1. **拆分大型服务** - 将 1000+ 行的服务拆分为协作类
2. **抽象通用逻辑** - 提取缓存、订阅、队列的公共实现
3. **引入依赖注入** - 提高测试可测试性
4. **统一错误处理** - 建立服务层统一的错误边界
