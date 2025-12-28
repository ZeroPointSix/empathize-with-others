---
name: performance-optimization
description: 性能优化 - 代码性能优化、数据库优化、缓存策略、并发处理、资源管理。在系统性能不佳或需要优化时使用。
---

# 性能优化

## 激活时机

当满足以下条件时自动激活此技能：
- 响应时间过长
- 内存占用过高
- CPU 使用率高
- 数据库查询慢
- 页面加载慢
- 需要处理大量数据

## 优化策略

### 测量优先

```
1. 测量 → 2. 识别瓶颈 → 3. 优化 → 4. 验证
```

```typescript
// 测量执行时间
console.time('operation');
// ... 操作 ...
console.timeEnd('operation');

// 使用性能 API
const start = performance.now();
await expensiveOperation();
const duration = performance.now() - start;
console.log(`Operation took ${duration}ms`);
```

## 前端优化

### 代码分割

```typescript
// 路由级别代码分割
const Dashboard = lazy(() => import('./pages/Dashboard'));
const Settings = lazy(() => import('./pages/Settings'));

// 组件级别代码分割
const HeavyChart = lazy(() => import('./components/HeavyChart'));

// 动态导入
const loadModule = async () => {
  const module = await import('./utils/heavyModule');
  module.doSomething();
};
```

### 图片优化

```html
<!-- 响应式图片 -->
<img
  src="image-800.jpg"
  srcset="image-400.jpg 400w,
          image-800.jpg 800w,
          image-1200.jpg 1200w"
  sizes="(max-width: 600px) 400px,
         (max-width: 1200px) 800px,
         1200px"
  loading="lazy"
/>

<!-- Next.js Image 组件 -->
<Image
  src="/photo.jpg"
  alt="Photo"
  width={800}
  height={600}
  loading="lazy"
  placeholder="blur"
/>
```

### 虚拟化长列表

```typescript
import { useVirtualizer } from '@tanstack/react-virtual';

function VirtualList({ items }: { items: Item[] }) {
  const parentRef = useRef<HTMLDivElement>(null);

  const rowVirtualizer = useVirtualizer({
    count: items.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 50,
    overscan: 5,
  });

  return (
    <div ref={parentRef} style={{ height: '400px', overflow: 'auto' }}>
      <div style={{ height: `${rowVirtualizer.getTotalSize()}px` }}>
        {rowVirtualizer.getVirtualItems().map((virtualRow) => (
          <div
            key={virtualRow.key}
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              width: '100%',
              height: `${virtualRow.size}px`,
              transform: `translateY(${virtualRow.start}px)`,
            }}
          >
            {items[virtualRow.index].content}
          </div>
        ))}
      </div>
    </div>
  );
}
```

### 防抖与节流

```typescript
// 防抖 - 延迟执行
function debounce<T extends (...args: any[]) => any>(
  fn: T,
  delay: number
): (...args: Parameters<T>) => void {
  let timeoutId: NodeJS.Timeout;
  return (...args: Parameters<T>) => {
    clearTimeout(timeoutId);
    timeoutId = setTimeout(() => fn(...args), delay);
  };
}

// 使用
const debouncedSearch = debounce((query: string) => {
  searchAPI(query);
}, 300);

<input onChange={(e) => debouncedSearch(e.target.value)} />

// 节流 - 限制频率
function throttle<T extends (...args: any[]) => any>(
  fn: T,
  limit: number
): (...args: Parameters<T>) => void {
  let inThrottle: boolean;
  return (...args: Parameters<T>) => {
    if (!inThrottle) {
      fn(...args);
      inThrottle = true;
      setTimeout(() => (inThrottle = false), limit);
    }
  };
}

// 使用
const throttledScroll = throttle(() => {
  handleScroll();
}, 100);
```

## 后端优化

### 数据库查询优化

```typescript
// ❌ N+1 问题
const users = await prisma.user.findMany();
for (const user of users) {
  user.posts = await prisma.post.findMany({  // N 次查询
    where: { authorId: user.id }
  });
}

// ✅ 使用 include
const users = await prisma.user.findMany({
  include: { posts: true }  // 1 次查询
});

// ✅ 选择性查询字段
const users = await prisma.user.findMany({
  select: {
    id: true,
    name: true,
    email: true,
  }
});

// ✅ 分页查询
const users = await prisma.user.findMany({
  take: 20,
  skip: (page - 1) * 20,
  orderBy: { createdAt: 'desc' }
});
```

### 索引优化

```prisma
model User {
  id        String   @id @default(uuid())
  email     String   @unique
  username  String   @unique

  // 单列索引
  @@index([email])
  @@index([username])

  // 复合索引
  @@index([status, createdAt])
}

// 查询优化
// ✅ 使用索引
await prisma.user.findMany({
  where: {
    email: 'test@example.com'  // 使用 email 索引
  }
});

// ❌ 无法使用索引
await prisma.user.findMany({
  where: {
    email: { contains: '@example.com' }  // 模糊查询无法使用索引
  }
});
```

### 批量操作

```typescript
// ❌ 逐个插入
for (const user of users) {
  await prisma.user.create({ data: user });  // N 次查询
}

// ✅ 批量插入
await prisma.user.createMany({
  data: users  // 1 次查询
});

// ❌ 逐个更新
for (const user of users) {
  await prisma.user.update({
    where: { id: user.id },
    data: { status: 'active' }
  });
}

// ✅ 批量更新
await prisma.user.updateMany({
  where: { id: { in: user_ids } },
  data: { status: 'active' }
});
```

## 缓存策略

### 多级缓存

```typescript
class CacheService {
  private memoryCache = new Map<string, any>();
  private redis: Redis;

  async get(key: string): Promise<any> {
    // L1: 内存缓存
    if (this.memoryCache.has(key)) {
      return this.memoryCache.get(key);
    }

    // L2: Redis 缓存
    const redisValue = await this.redis.get(key);
    if (redisValue) {
      this.memoryCache.set(key, JSON.parse(redisValue));
      return JSON.parse(redisValue);
    }

    return null;
  }

  async set(key: string, value: any, ttl: number = 3600) {
    // 写入内存缓存
    this.memoryCache.set(key, value);

    // 写入 Redis
    await this.redis.setex(key, ttl, JSON.stringify(value));
  }
}
```

### 缓存失效策略

```typescript
// TTL 过期
await cache.set('user:123', user, 3600);  // 1小时后过期

// 主动失效
async updateUser(userId: string, data: any) {
  const user = await prisma.user.update({
    where: { id: userId },
    data
  });

  // 更新后清除缓存
  await cache.del(`user:${userId}`);
  await cache.del(`user:posts:${userId}`);

  return user;
}

// 标签失效
await cache.setTag('user:123', ['user', 'profile']);
await cache.invalidateTag('user');  // 清除所有 user 相关缓存
```

### 缓存预热

```typescript
async function warmupCache() {
  const popularUsers = await prisma.user.findMany({
    where: { followers: { gt: 10000 } },
    take: 100
  });

  for (const user of popularUsers) {
    await cache.set(`user:${user.id}`, user, 3600);
  }
}
```

## 并发处理

### 并行处理

```typescript
// ❌ 串行执行
const user = await fetchUser(userId);
const posts = await fetchUserPosts(userId);
const comments = await fetchUserComments(userId);

// ✅ 并行执行
const [user, posts, comments] = await Promise.all([
  fetchUser(userId),
  fetchUserPosts(userId),
  fetchUserComments(userId),
]);
```

### 并发控制

```typescript
// 限制并发数
import pLimit from 'p-limit';

const limit = pLimit(5);  // 最多5个并发

const tasks = urls.map(url =>
  limit(() => fetch(url))
);

const results = await Promise.all(tasks);
```

### Worker 线程

```typescript
// 主线程
const worker = new Worker('./heavy-worker.js');

worker.postMessage({ data: largeData });

worker.onmessage = (e) => {
  const result = e.data;
  // 处理结果
};

// heavy-worker.js
self.onmessage = (e) => {
  const result = heavyComputation(e.data);
  self.postMessage(result);
};
```

## 内存优化

### 避免内存泄漏

```typescript
// ❌ 事件监听器未清理
useEffect(() => {
  window.addEventListener('resize', handleResize);
}, []);

// ✅ 清理事件监听器
useEffect(() => {
  window.addEventListener('resize', handleResize);
  return () => {
    window.removeEventListener('resize', handleResize);
  };
}, []);

// ❌ 定时器未清理
useEffect(() => {
  setInterval(() => {
    updateData();
  }, 1000);
}, []);

// ✅ 清理定时器
useEffect(() => {
  const interval = setInterval(() => {
    updateData();
  }, 1000);

  return () => clearInterval(interval);
}, []);
```

### 流式处理大文件

```typescript
// ❌ 一次性加载大文件
const content = await fs.readFile('large-file.txt');

// ✅ 流式读取
const stream = fs.createReadStream('large-file.txt');
stream.on('data', (chunk) => {
  processChunk(chunk);
});
```

## 监控与分析

### 性能监控

```typescript
// 前端性能监控
export function reportWebVitals(metric) {
  const { name, value, id } = metric;

  // 发送到分析服务
  analytics.track('performance', {
    metric: name,
    value: Math.round(name === 'CLS' ? value * 1000 : value),
    id,
  });
}

// 后端性能监控
app.use((req, res, next) => {
  const start = Date.now();

  res.on('finish', () => {
    const duration = Date.now() - start;
    logger.info({
      method: req.method,
      path: req.path,
      status: res.statusCode,
      duration,
    });
  });

  next();
});
```

### 性能分析工具

```
前端：
- Lighthouse - 综合性能分析
- Chrome DevTools Performance - 运行时性能
- React DevTools Profiler - React 组件性能

后端：
- Node.js Profiler - CPU/内存分析
- pprof - Go 性能分析
- py-spy - Python 性能分析
```

## 优化检查清单

```markdown
### 前端
- [ ] 代码分割和懒加载
- [ ] 图片压缩和优化
- [ ] 长列表虚拟化
- [ ] 防抖节流处理
- [ ] 缓存策略
- [ ] Bundle 大小优化

### 后端
- [ ] 数据库查询优化
- [ ] 索引使用
- [ ] 批量操作
- [ ] 连接池配置
- [ ] 缓存使用
- [ ] 并发处理

### 监控
- [ ] 性能指标收集
- [ ] 错误监控
- [ ] 日志分析
- [ ] 告警设置
```

## 相关资源

- `resources/frontend-performance.md` - 前端性能详解
- `resources/backend-performance.md` - 后端性能详解
- `resources/caching-strategies.md` - 缓存策略详解

---

**技能状态**: 完成 ✅
**核心原则**: 测量 → 识别 → 优化 → 验证
**关键指标**: 响应时间、吞吐量、资源使用率
