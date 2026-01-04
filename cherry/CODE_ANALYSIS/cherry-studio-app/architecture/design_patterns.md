# 设计模式识别

本文档详细分析 Cherry Studio App 数据层架构中使用的设计模式。

## 1. 创建型模式

### 1.1 Singleton (单例模式)

**应用场景**: 所有 Service 类都采用单例模式

**文件**: `E:\hushaokang\Data-code\cherry-studio-app\src\services\TopicService.ts`

```typescript
class TopicService {
  private static instance: TopicService | null = null

  // 私有构造函数
  private constructor() {
    this.db = database
    this.init()
  }

  // 获取单例实例
  static getInstance(): TopicService {
    if (!TopicService.instance) {
      TopicService.instance = new TopicService()
    }
    return TopicService.instance
  }

  // 提供全局访问点
  static readonly topicService = TopicService.topicService
}

export const topicService = TopicService.topicService
```

**应用实例**:
- `TopicService` - `E:\hushaokang\Data-code\cherry-studio-app\src\services\TopicService.ts`
- `AssistantService` - `E:\hushaokang\Data-code\cherry-studio-app\src\services\AssistantService.ts`
- `ProviderService` - `E:\hushaokang\Data-code\cherry-studio-app\src\services\ProviderService.ts`
- `PreferenceService` - `E:\hushaokang\Data-code\cherry-studio-app\src\services\PreferenceService.ts`
- `McpService` - `E:\hushaokang\Data-code\cherry-studio-app\src\services\McpService.ts`

**优点**:
- 全局唯一实例，避免资源浪费
- 统一的数据访问入口
- 便于缓存状态管理

**缺点**:
- 难以进行单元测试（难以 Mock）
- 单例生命周期与应用绑定，难以控制

### 1.2 Builder (建造者模式)

**应用场景**: Drizzle 查询构建器

**文件**: `E:\hushaokang\Data-code\cherry-studio-app\db\schema\index.ts`

```typescript
import { relations } from 'drizzle-orm'

// 使用 Drizzle 的查询构建器
const query = db
  .select()
  .from(topics)
  .where(eq(topics.assistant_id, assistantId))
  .orderBy(desc(topics.created_at))
  .limit(10)
  .offset(0)
```

**应用场景**: AI Core 中间件构建器

**文件**: `E:\hushaokang\Data-code\cherry-studio-app\src\aiCore\legacy\middleware\builder.ts`

```typescript
export const createMiddlewarePipeline = (config: MiddlewareConfig) => {
  return middlewareBuilder
    .use(errorHandler)
    .use(logging)
    .use(streamAdapter)
    .build()
}
```

## 2. 结构型模式

### 2.1 Repository (仓储模式)

**应用场景**: Service 层封装数据访问

虽然没有显式的 Repository 接口，但 Service 层实际上实现了仓储模式：

```typescript
// 文件: E:\hushaokang\Data-code\cherry-studio-app\src\services\TopicService.ts
class TopicService {
  // 仓储操作
  async getTopic(id: string): Promise<Topic | null>
  async getTopics(assistantId: string): Promise<Topic[]>
  async createTopic(topic: InsertTopic): Promise<Topic>
  async updateTopic(id: string, data: Partial<Topic>): Promise<void>
  async deleteTopic(id: string): Promise<void>

  // 缓存操作
  getCurrentTopic(): Topic | undefined
  getTopicCached(id: string): Topic | undefined
  invalidateCache(): void
}
```

**优点**:
- 封装数据访问逻辑
- 提供统一的 API
- 便于添加缓存、审计等功能

**问题**: 缺少抽象接口，难以替换实现

### 2.2 Facade (外观模式)

**应用场景**: 服务层提供统一的数据访问接口

```typescript
// 文件: E:\hushaokang\Data-code\cherry-studio-app\src\services\TopicService.ts
class TopicService {
  // 外观接口 - 隐藏复杂的缓存逻辑
  async switchToTopic(topicId: string): Promise<void> {
    // 内部处理:
    // 1. 获取新主题 (检查多层缓存)
    // 2. 更新当前主题缓存
    // 3. 将旧主题移入 LRU
    // 4. 同步到 preference
    // 5. 通知订阅者
    // 6. 异步写入数据库
  }
}
```

### 2.3 Proxy (代理模式)

**应用场景**: 缓存代理

```typescript
class TopicService {
  // 缓存代理 - 透明地添加缓存层
  async getTopic(topicId: string): Promise<Topic | null> {
    // 1. 检查当前主题缓存
    if (this.currentTopicCache.has(topicId)) {
      return this.currentTopicCache.get(topicId)!
    }

    // 2. 检查 LRU 缓存
    if (this.topicCache.has(topicId)) {
      return this.topicCache.get(topicId)!
    }

    // 3. 从数据库加载
    const topic = await this.loadTopicFromDB(topicId)

    // 4. 加入缓存
    this.topicCache.set(topicId, topic)

    return topic
  }
}
```

## 3. 行为型模式

### 3.1 Strategy (策略模式)

**应用场景**: 缓存策略

```typescript
// 文件: E:\hushaokang\Data-code\cherry-studio-app\src\services\TopicService.ts
class TopicService {
  // 策略接口
  private readonly CACHE_STRATEGIES = {
    current: 'current',   // 当前主题 - 永久缓存
    lru: 'lru',           // 最近访问 - LRU 策略
    ttl: 'ttl'            // 全量列表 - TTL 策略
  } as const

  // 根据不同策略获取数据
  private async getTopicWithStrategy(
    topicId: string,
    strategy: keyof typeof this.CACHE_STRATEGIES
  ): Promise<Topic | null> {
    switch (strategy) {
      case 'current':
        return this.currentTopicCache.get(topicId) ?? null
      case 'lru':
        return this.topicCache.get(topicId) ?? null
      case 'ttl':
        const allTopics = this.allTopicsCache.get(assistantId)
        return allTopics?.find(t => t.id === topicId) ?? null
    }
  }
}
```

### 3.2 Observer (观察者模式)

**应用场景**: 缓存变更订阅

```typescript
// 文件: E:\hushaokang\Data-code\cherry-studio-app\src\services\TopicService.ts
class TopicService {
  // 订阅者存储
  private subscribers: Map<string, Set<() => void>> = new Map()

  // 订阅当前主题变化
  subscribeCurrentTopic(callback: () => void): () => void {
    const topicId = this.getCurrentTopicId()
    if (!this.subscribers.has(topicId)) {
      this.subscribers.set(topicId, new Set())
    }
    this.subscribers.get(topicId)!.add(callback)

    // 返回取消订阅函数
    return () => {
      this.subscribers.get(topicId)?.delete(callback)
    }
  }

  // 通知所有订阅者
  private notifySubscribers(topicId: string): void {
    this.subscribers.get(topicId)?.forEach(callback => callback())
  }
}
```

**React 集成**:

```typescript
// 文件: src/hooks/useTopic.ts
export function useTopic(topicId: string) {
  const subscribe = useCallback((callback: () => void) => {
    return topicService.subscribeTopic(topicId, callback)
  }, [topicId])

  const topic = useSyncExternalStore(subscribe, () =>
    topicService.getTopicCached(topicId)
  )

  return { topic }
}
```

### 3.3 Memento (备忘录模式)

**应用场景**: 乐观更新回滚

```typescript
// 文件: E:\hushaokang\Data-code\cherry-studio-app\src\services\TopicService.ts
class TopicService {
  // 保存状态用于回滚
  private saveStateForRollback(topicId: string, topic: Topic): void {
    this.rollbackCache.set(topicId, {
      timestamp: Date.now(),
      data: topic
    })
  }

  // 回滚到之前状态
  private async rollbackToPrevious(topicId: string): Promise<void> {
    const savedState = this.rollbackCache.get(topicId)
    if (savedState) {
      // 恢复缓存
      this.currentTopicCache.set(topicId, savedState.data)
      // 通知订阅者
      this.notifySubscribers(topicId)
      // 清除回滚记录
      this.rollbackCache.delete(topicId)
    }
  }

  async updateTopic(id: string, data: Partial<Topic>): Promise<void> {
    // 1. 保存当前状态
    const current = this.getTopicCached(id)
    if (current) {
      this.saveStateForRollback(id, current)
    }

    // 2. 乐观更新
    this.optimisticUpdate(id, data)

    // 3. 异步写入数据库
    try {
      await this.db.update(topics).set(data).where(eq(topics.id, id))
    } catch (error) {
      // 4. 失败回滚
      await this.rollbackToPrevious(id)
      throw error
    }
  }
}
```

### 3.4 Chain of Responsibility (责任链模式)

**应用场景**: 中间件处理链

**文件**: `E:\hushaokang\Data-code\cherry-studio-app\src\aiCore\legacy\middleware\composer.ts`

```typescript
// 中间件链
class MiddlewareChain {
  private middlewares: Middleware[] = []

  use(middleware: Middleware): this {
    this.middlewares.push(middleware)
    return this
  }

  async execute(request: Request): Promise<Response> {
    let index = 0

    const next = async (req: Request): Promise<Response> => {
      if (index >= this.middlewares.length) {
        return this.handleRequest(req)
      }
      const middleware = this.middlewares[index++]
      return middleware.process(req, next)
    }

    return next(request)
  }
}

// 使用
const chain = new MiddlewareChain()
  .use(errorHandler)
  .use(loggingMiddleware)
  .use(authMiddleware)
  .use(streamAdapter)

const response = await chain.execute(request)
```

## 4. 并发控制模式

### 4.1 Request Queue (请求队列)

```typescript
// 文件: E:\hushaokang\Data-code\cherry-studio-app\src\services\TopicService.ts
class TopicService {
  // 请求队列 - 序列化同一 key 的更新
  private requestQueue: Map<string, Promise<void>> = new Map()

  async updateTopic(id: string, data: Partial<Topic>): Promise<void> {
    // 如果有相同 ID 的请求在执行，等待它完成
    const existingRequest = this.requestQueue.get(id)
    if (existingRequest) {
      await existingRequest
    }

    // 创建新请求
    const newRequest = this.performUpdate(id, data)
    this.requestQueue.set(id, newRequest)

    try {
      await newRequest
    } finally {
      // 请求完成后清理
      this.requestQueue.delete(id)
    }
  }
}
```

### 4.2 Load Deduplication (加载去重)

```typescript
class TopicService {
  // 加载去重 - 防止重复加载同一主题
  private loadPromises: Map<string, Promise<Topic>> = new Map()

  async getTopic(topicId: string): Promise<Topic | null> {
    // 检查是否正在加载
    if (this.loadPromises.has(topicId)) {
      return this.loadPromises.get(topicId)!
    }

    // 创建加载 Promise
    const loadPromise = this.loadFromDatabase(topicId)
    this.loadPromises.set(topicId, loadPromise)

    try {
      const topic = await loadPromise
      // 加载完成后加入缓存
      this.topicCache.set(topicId, topic)
      return topic
    } finally {
      this.loadPromises.delete(topicId)
    }
  }
}
```

## 5. 缓存模式

### 5.1 Three-Level Cache (三级缓存)

```typescript
// 文件: E:\hushaokang\Data-code\cherry-studio-app\src\services\TopicService.ts
class TopicService {
  // Level 1: 当前主题缓存 (永久)
  private currentTopicCache: Map<string, Topic> = new Map()

  // Level 2: LRU 缓存 (固定容量)
  private topicCache: LRUCache<string, Topic> = new LRUCache({
    max: 5
  })

  // Level 3: 全量缓存 (TTL)
  private allTopicsCache: Map<string, { data: Topic[]; timestamp: number }> = new Map()
  private readonly CACHE_TTL = 5 * 60 * 1000 // 5 分钟

  async getTopic(topicId: string): Promise<Topic | null> {
    // 1. 检查当前主题 (最快)
    const currentId = await this.preferenceService.get('topic.current_id')
    if (topicId === currentId && this.currentTopicCache.has(topicId)) {
      return this.currentTopicCache.get(topicId)!
    }

    // 2. 检查 LRU 缓存 (快)
    if (this.topicCache.has(topicId)) {
      return this.topicCache.get(topicId)!
    }

    // 3. 从数据库加载 (慢)
    const topic = await this.loadFromDatabase(topicId)
    if (topic) {
      this.topicCache.set(topicId, topic)
    }

    return topic
  }
}
```

## 6. 架构模式总结

### 6.1 使用频率统计

| 模式 | 使用频率 | 应用场景 |
|------|----------|----------|
| Singleton | 高 | 所有 Service 类 |
| Observer | 高 | 缓存订阅系统 |
| Strategy | 中 | 缓存策略、WebSearch |
| Proxy | 中 | 缓存代理 |
| Builder | 中 | Drizzle 查询构建器 |
| Facade | 中 | Service 统一接口 |
| Chain of Responsibility | 低 | 中间件处理链 |
| Memento | 中 | 乐观更新回滚 |

### 6.2 模式使用评估

| 方面 | 评估 |
|------|------|
| 模式应用 | 良好 - 合理使用了多种设计模式 |
| 一致性 | 中等 - 部分服务实现存在差异 |
| 文档 | 良好 - 模式使用有注释说明 |
| 测试 | 需改进 - 单例模式增加测试难度 |

### 6.3 改进建议

1. **引入 Repository 接口**: 定义数据访问抽象层
2. **统一服务基类**: 提取公共逻辑到基类
3. **依赖注入**: 使用 DI 容器替代单例
4. **策略模式扩展**: 将缓存策略配置化
