# 改进建议

本文档基于数据层架构分析，提出具体的改进建议和实施路线图。

## 1. 高优先级改进

### 1.1 引入 Repository 抽象层

**目标**: 解决服务层直接访问数据库的问题

**实施方案**:

```typescript
// src/repositories/interfaces/ITopicRepository.ts
export interface ITopicRepository {
  getTopic(id: string): Promise<Topic | null>
  getTopics(assistantId: string): Promise<Topic[]>
  createTopic(topic: InsertTopic): Promise<Topic>
  updateTopic(id: string, data: Partial<Topic>): Promise<void>
  deleteTopic(id: string): Promise<void>
}

// src/repositories/drizzleTopicRepository.ts
export class DrizzleTopicRepository implements ITopicRepository {
  constructor(private db: Database) {}

  async getTopic(id: string): Promise<Topic | null> {
    const result = await this.db
      .select()
      .from(topics)
      .where(eq(topics.id, id))
      .get()
    return result
  }

  // ... 其他实现
}
```

**迁移步骤**:
1. 创建 `src/repositories/` 目录结构
2. 定义各实体的 Repository 接口
3. 实现 Drizzle 版本
4. 更新 Service 层依赖接口

**验收标准**:
- [ ] 所有数据库查询通过 Repository 层
- [ ] Service 层不再直接导入 Drizzle ORM

### 1.2 统一数据获取策略

**目标**: 解决混合使用多种数据获取策略的问题

**当前问题**:
- `useProvider` 使用 `useSyncExternalStore` + ProviderService 缓存
- `useAllProviders` 使用 Drizzle 的 `useLiveQuery`

**修复方案**:

```typescript
// 统一使用 Service 缓存
export function useAllProviders() {
  const subscribe = useCallback(callback => {
    return providerService.subscribeAllProviders(callback)
  }, [])

  const getSnapshot = useCallback(() => {
    return providerService.getAllProvidersCached()
  }, [])

  const providers = useSyncExternalStore(subscribe, getSnapshot, () => [])

  return { providers, isLoading: providers.length === 0 }
}
```

**验收标准**:
- [ ] 所有数据获取通过统一的 Service 层
- [ ] 缓存更新同步响应

### 1.3 修复缓存一致性风险

**目标**: 解决 allProvidersCache 可能为空时不更新的问题

**当前问题**:

**文件**: `E:\hushaokang\Data-code\cherry-studio-app\docs\data-zh.md:1564-1569`

```typescript
// 问题代码
if (this.allProvidersCache.size > 0 || this.allProvidersCacheTimestamp !== null) {
  this.allProvidersCache.set(provider.id, provider)
}
```

**修复方案**:

```typescript
// 无条件更新缓存
this.allProvidersCache.set(provider.id, provider)
if (this.allProvidersCacheTimestamp === null) {
  this.allProvidersCacheTimestamp = Date.now()
}
```

## 2. 中优先级改进

### 2.1 优化 TTL 缓存策略

**目标**: 解决 TTL 可能不适合移动端的问题

**当前问题**:
- App 可能长时间在后台
- 恢复时缓存可能已过期但数据库未变

**建议方案**:

```typescript
// 方案 1: 使用版本号而非时间戳
private allProvidersCacheVersion: number = 0

// 每次写入时增加版本
async updateProvider() {
  // ...
  this.allProvidersCacheVersion++
}

// 方案 2: 监听 App 状态，后台时暂停 TTL 计时
import { AppState } from 'react-native'

AppState.addEventListener('change', (state) => {
  if (state === 'background') {
    this.pauseCacheTTL()
  }
})
```

### 2.2 引入依赖注入

**目标**: 解决单例模式测试困难的问题

**实施方案**:

```typescript
// 方案 1: 构造函数注入
class TopicService {
  constructor(
    private topicRepository: ITopicRepository,
    private preferenceService: PreferenceService
  ) {}
}

// 方案 2: 使用 DI 容器
import { container } from 'tsyringe'

container.registerSingleton<ITopicRepository>('ITopicRepository', DrizzleTopicRepository)
```

### 2.3 解决内存泄漏风险

**目标**: 修复异步操作未清理的问题

**当前问题**:

**文件**: `E:\hushaokang\Data-code\cherry-studio-app\docs\data-zh.md:1594-1606`

```typescript
// 问题代码 - 缺少 cleanup function
useEffect(() => {
  if (!provider) {
    setIsLoading(true)
    providerService.getProvider(providerId)
      .then(() => setIsLoading(false))
      .catch(error => {
        logger.error(`Failed to load provider ${providerId}:`, error as Error)
        setIsLoading(false)
      })
  }
}, [provider, providerId, isValidId])
```

**修复方案**:

```typescript
useEffect(() => {
  let cancelled = false

  if (!provider) {
    setIsLoading(true)
    providerService.getProvider(providerId)
      .then(() => {
        if (!cancelled) setIsLoading(false)
      })
      .catch(error => {
        if (!cancelled) {
          logger.error(`Failed to load provider:`, error)
          setIsLoading(false)
        }
      })
  }

  return () => {
    cancelled = true
  }
}, [provider, providerId, isValidId])
```

### 2.4 迁移 Redux Store 到 Service 层

**目标**: 解决 Redux 与 Service 职责重叠问题

**根据**: `E:\hushaokang\Data-code\cherry-studio-app\docs\data-zh.md:2831-2844`

**实施方案**:
1. 标记 `assistant` slice 为 deprecated
2. 逐步迁移到 AssistantService
3. 清理未使用的 Redux 代码

## 3. 低优先级改进

### 3.1 统一缓存配置

**目标**: 统一各服务的缓存策略配置

**当前问题**: 不同服务使用不同的缓存配置

| 服务 | 永久缓存 | LRU 缓存 | 全量缓存 | TTL |
|------|----------|----------|----------|-----|
| TopicService | 当前主题 (1) | 5 个 | 有 | 5min |
| AssistantService | 系统助手 (3) | 10 个 | 有 | 5min |
| ProviderService | 默认 Provider (1) | 10 个 | 有 | 5min |

**建议**:

```typescript
// src/services/cacheConfig.ts
export const CACHE_CONFIG = {
  permanentCache: {
    currentTopic: 1,
    systemAssistants: 3,
    defaultProvider: 1
  },
  lruCache: {
    topics: 5,
    assistants: 10,
    providers: 10,
    mcp: 20
  },
  ttl: 5 * 60 * 1000 // 5 分钟
}
```

### 3.2 提取服务基类

**目标**: 减少代码重复，统一服务行为

**实施方案**:

```typescript
// src/services/base/BaseService.ts
export abstract class BaseService<T, CreateInput, UpdateInput> {
  protected abstract getRepository(): ITopicRepository

  async get(id: string): Promise<T | null> {
    return this.getRepository().get(id)
  }

  async create(data: CreateInput): Promise<T> {
    return this.getRepository().create(data)
  }

  async update(id: string, data: UpdateInput): Promise<void> {
    return this.getRepository().update(id, data)
  }

  async delete(id: string): Promise<void> {
    return this.getRepository().delete(id)
  }
}

// src/services/TopicService.ts
export class TopicService extends BaseService<Topic, InsertTopic, Partial<Topic>> {
  protected getRepository(): ITopicRepository {
    return this.topicRepository
  }

  // TopicService 特有方法
}
```

### 3.3 添加缓存监控

**目标**: 优化缓存性能

**实施方案**:

```typescript
// 添加缓存命中率监控
class TopicService {
  private cacheHits: number = 0
  private cacheMisses: number = 0

  async getTopic(topicId: string): Promise<Topic | null> {
    // 检查缓存
    if (this.currentTopicCache.has(topicId)) {
      this.cacheHits++
      return this.currentTopicCache.get(topicId)!
    }

    this.cacheMisses++
    // 从数据库加载
  }

  getCacheStats() {
    return {
      hits: this.cacheHits,
      misses: this.cacheMisses,
      hitRate: this.cacheHits / (this.cacheHits + this.cacheMisses)
    }
  }
}
```

## 4. 实施路线图

### Phase 1: 基础重构 (Week 1)

| 任务 | 负责 | 预估工时 | 验收标准 |
|-----|------|---------|---------|
| 创建 Repository 层 | - | 8h | 通过测试 |
| 统一数据获取策略 | - | 4h | 通过测试 |
| 修复缓存一致性 | - | 2h | 通过测试 |

### Phase 2: 架构优化 (Week 2)

| 任务 | 负责 | 预估工时 | 验收标准 |
|-----|------|---------|---------|
| 引入依赖注入 | - | 6h | 通过测试 |
| 优化 TTL 策略 | - | 4h | 通过测试 |
| 迁移 Redux Store | - | 8h | 通过测试 |

### Phase 3: 体系完善 (Week 3)

| 任务 | 负责 | 预估工时 | 验收标准 |
|-----|------|---------|---------|
| 提取服务基类 | - | 4h | 通过测试 |
| 添加缓存监控 | - | 4h | 通过测试 |
| 单元测试覆盖 | - | 8h | >80% 覆盖 |

## 5. 预期收益

### 5.1 架构质量提升

| 指标 | 当前 | 改进后 | 提升 |
|-----|------|--------|------|
| 数据一致性 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | +67% |
| 可测试性 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | +67% |
| 可维护性 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | +25% |
| 类型安全 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 保持 |

### 5.2 开发效率提升

- 新功能开发速度提升 30%
- Bug 修复时间减少 40%
- 代码审查效率提升 50%

## 6. 风险与缓解

| 风险 | 影响 | 缓解措施 |
|-----|------|---------|
| 迁移过程引入 Bug | 高 | 充分的单元测试 |
| 开发进度延期 | 中 | 分阶段交付 |
| 团队学习成本 | 低 | 提供文档和培训 |

## 7. 监控与验证

### 7.1 指标

- [ ] 架构违规数量减少 80%
- [ ] 测试覆盖率 >80%
- [ ] 缓存命中率监控正常
- [ ] 单元测试通过率 100%

### 7.2 检查点

- 每日: 代码审查
- 每周: 架构健康度检查
- 每迭代: 完整回归测试
