---
name: error-tracking
description: 错误跟踪 - 集成 Sentry、错误捕获、日志记录、性能监控、异常分析。在生产环境需要错误监控时使用。
---

# 错误跟踪

## 激活时机

当满足以下条件时自动激活此技能：
- 新项目需要配置错误监控
- 生产环境出现未捕获异常
- 需要分析和调试错误
- 需要性能监控和分析
- 需要用户错误反馈追踪

## Sentry 集成

### 初始化配置

```typescript
// instrument.ts - 确保最先导入
import * as Sentry from "@sentry/node";
import { ProfilingIntegration } from "@sentry/profiling-node";

Sentry.init({
  dsn: process.env.SENTRY_DSN,
  environment: process.env.NODE_ENV || 'development',
  tracesSampleRate: process.env.NODE_ENV === 'production' ? 0.1 : 1.0,
  profilesSampleRate: process.env.NODE_ENV === 'production' ? 0.1 : 1.0,
  integrations: [
    new ProfilingIntegration(),
    new Sentry.Integrations.Http({ tracing: true }),
    new Sentry.Integrations.Express({ app }),
  ],
  beforeSend(event, hint) {
    // 过滤敏感信息
    if (event.request) {
      delete event.request.cookies;
      delete event.request.headers?.authorization;
    }
    return event;
  },
});
```

### Express 中间件集成

```typescript
import express from "express";
import * as Sentry from "@sentry/node";

const app = express();

// 请求处理中间件（必须在路由之前）
app.use(Sentry.Handlers.requestHandler());

// 性能监控中间件
app.use(Sentry.Handlers.tracingHandler());

// ... 你的路由 ...

// 错误处理中间件（必须在所有其他错误处理之后）
app.use(Sentry.Handlers.errorHandler());

// 自定义错误处理器
app.use((err: Error, req: express.Request, res: express.Response, next: express.NextFunction) => {
  // Sentry 已经自动捕获错误
  res.status(500).json({
    error: process.env.NODE_ENV === 'production'
      ? 'Internal Server Error'
      : err.message
  });
});
```

## 错误捕获模式

### 同步错误捕获

```typescript
try {
  const result = dangerousOperation();
  return result;
} catch (error) {
  // 自动发送到 Sentry
  Sentry.captureException(error);
  throw error;
}
```

### 异步错误捕获

```typescript
// 方法1: async/await
async function processUser(userId: string) {
  try {
    const user = await fetchUser(userId);
    return user;
  } catch (error) {
    Sentry.captureException(error);
    throw error;
  }
}

// 方法2: Promise catch
fetchUser(userId)
  .then(user => processUser(user))
  .catch(error => {
    Sentry.captureException(error);
  });
```

### 错误包装器

```typescript
// 统一错误处理包装器
export function asyncErrorWrapper(
  fn: (...args: any[]) => Promise<any>
) {
  return async (req: express.Request, res: express.Response, next: express.NextFunction) => {
    try {
      await fn(req, res, next);
    } catch (error) {
      Sentry.captureException(error, {
        tags: {
          route: req.path,
          method: req.method,
        },
        user: {
          id: req.user?.id,
          ip: req.ip,
        },
      });
      next(error);
    }
  };
}

// 使用
app.post('/users',
  asyncErrorWrapper(async (req, res) => {
    const user = await userService.create(req.body);
    res.json(user);
  })
);
```

## 错误上下文增强

### 添加自定义标签

```typescript
try {
  processPayment(order);
} catch (error) {
  Sentry.captureException(error, {
    tags: {
      module: 'payment',
      operation: 'process',
      payment_method: order.paymentMethod,
    },
  });
}
```

### 添加用户信息

```typescript
Sentry.setUser({
  id: user.id,
  email: user.email,
  username: user.name,
  segment: user.subscriptionType,
});

// 清除用户信息
Sentry.setUser(null);
```

### 添加面包屑导航

```typescript
// 记录用户操作路径
Sentry.addBreadcrumb({
  message: 'User clicked checkout',
  category: 'ui',
  level: 'info',
  data: {
    cartItems: cart.items.length,
    total: cart.total,
  },
});

Sentry.addBreadcrumb({
  message: 'Payment processing started',
  category: 'payment',
  level: 'info',
});
```

### 添加自定义上下文

```typescript
Sentry.setContext('order_context', {
  orderId: order.id,
  userId: order.userId,
  total: order.total,
  items: order.items.length,
  paymentMethod: order.paymentMethod,
});

try {
  processOrder(order);
} catch (error) {
  // 上下文会自动包含在错误报告中
  Sentry.captureException(error);
}
```

## 性能监控

### 事务追踪

```typescript
// 开始事务
const transaction = Sentry.startTransaction({
  op: 'process-order',
  name: 'Order Processing',
});

try {
  // 创建子操作
  const validateSpan = transaction.startChild({
    op: 'validation',
    description: 'Validate order data',
  });
  validateOrder(order);
  validateSpan.finish();

  const paymentSpan = transaction.startChild({
    op: 'payment',
    description: 'Process payment',
  });
  await processPayment(order);
  paymentSpan.finish();

} finally {
  transaction.finish();
}
```

### 自动性能监控

```typescript
// Express 路由自动追踪
app.get('/api/users/:id', async (req, res) => {
  // Sentry 自动记录此请求的性能
  const user = await userService.findById(req.params.id);
  res.json(user);
});
```

## 错误分类

### 自定义错误类

```typescript
export class AppError extends Error {
  constructor(
    public message: string,
    public statusCode: number = 500,
    public isOperational: boolean = true
  ) {
    super(message);
    this.name = this.constructor.name;
    Error.captureStackTrace(this, this.constructor);
  }
}

export class ValidationError extends AppError {
  constructor(message: string) {
    super(message, 400);
  }
}

export class NotFoundError extends AppError {
  constructor(resource: string) {
    super(`${resource} not found`, 404);
  }
}

export class ConflictError extends AppError {
  constructor(message: string) {
    super(message, 409);
  }
}

// 使用
if (!user) {
  throw new NotFoundError('User');
}
```

### 错误级别

```typescript
// 致命错误 - 应用无法继续
Sentry.captureException(error, { level: 'fatal' });

// 错误 - 功能失败但应用继续
Sentry.captureException(error, { level: 'error' });

// 警告 - 非预期但可恢复
Sentry.captureMessage('Deprecated API used', { level: 'warning' });

// 信息 - 正常但值得记录
Sentry.captureMessage('User login', { level: 'info' });
```

## 日志记录

### 结构化日志

```typescript
import pino from 'pino';

export const logger = pino({
  level: process.env.LOG_LEVEL || 'info',
  formatters: {
    level: (label) => {
      return { level: label };
    },
  },
  serializers: {
    err: pino.stdErrSerializer,
    req: pino.stdSerializers.req,
    res: pino.stdSerializers.res,
  },
});

// 使用
logger.info({ userId, action }, 'User logged in');
logger.error({ err, userId }, 'Login failed');
logger.warn({ retryCount }, 'Retrying operation');
```

### 日志 + Sentry 集成

```typescript
// 自动发送 error 级别日志到 Sentry
logger.error({ err: error, userId }, 'Operation failed');
// 等同于 Sentry.captureException(error)
```

## 错误分析

### Sentry 查询

```
# 按用户查看错误
isUnhandled:true
user.email:alice@example.com

# 按环境过滤
environment:production
release:1.2.3

# 按标签过滤
tag:module:payment
tag:payment_method:stripe

# 时间范围
event.timestamp:>2024-01-01T00:00:00Z
```

### 常见错误模式

```
1. TypeError: Cannot read property 'X' of undefined
   → 添加空值检查

2. NetworkError: Failed to fetch
   → 检查网络连接，添加重试逻辑

3. DatabaseError: Connection lost
   → 检查连接池配置，添加重连逻辑

4. ValidationError: Invalid input
   → 加强输入验证
```

## 最佳实践

### ✅ 应该做的

```typescript
// 1. 所有错误都记录
try {
  await operation();
} catch (error) {
  logger.error({ err: error }, 'Operation failed');
  Sentry.captureException(error);
  throw error;
}

// 2. 提供有用的上下文
Sentry.captureException(error, {
  tags: { module: 'payment', method: 'stripe' },
  user: { id: user.id },
  extra: { orderId: order.id, amount: order.total },
});

// 3. 区分可操作和不可操作错误
if (error.isOperational) {
  // 可预期的业务错误
  return res.status(error.statusCode).json({ error: error.message });
} else {
  // 未预期的系统错误
  Sentry.captureException(error);
  return res.status(500).json({ error: 'Internal error' });
}
```

### ❌ 不应该做的

```typescript
// 1. 不要吞掉错误
try {
  await operation();
} catch (error) {
  // ❌ 错误被忽略
}

// 2. 不要记录敏感信息
Sentry.captureException(error, {
  extra: {
    password: user.password,     // ❌
    creditCard: card.number,      // ❌
    token: authToken,             // ❌
  },
});

// 3. 不要在循环中重复发送
for (const item of items) {
  try {
    await processItem(item);
  } catch (error) {
    Sentry.captureException(error);  // ❌ 可能产生大量重复
  }
}

// 应该收集后统一发送
const errors = [];
for (const item of items) {
  try {
    await processItem(item);
  } catch (error) {
    errors.push({ item, error });
  }
}
if (errors.length > 0) {
  Sentry.captureException(
    new BatchProcessingError(errors),
    { extra: { errorCount: errors.length } }
  );
}
```

## 相关资源

- `resources/sentry-setup.md` - Sentry 完整配置指南
- `resources/error-handling-patterns.md` - 错误处理模式
- `resources/performance-monitoring.md` - 性能监控详解

---

**技能状态**: 完成 ✅
**推荐工具**: Sentry, LogRocket, Bugsnag
**适用环境**: 生产环境必备
