---
name: middleware
description: 中间件开发 - 认证中间件、验证中间件、日志中间件、错误处理、自定义中间件。在开发 Express 中间件时使用。
---

# 中间件开发

## 激活时机

当满足以下条件时自动激活此技能：
- 开发认证授权中间件
- 实现请求验证
- 添加日志记录
- 处理错误响应
- 实现自定义中间件
- 配置 CORS、限流等

## 中间件基础

### 中间件结构

```typescript
// middleware/type.ts
import { Request, Response, NextFunction } from 'express';

// 基本中间件签名
export function basicMiddleware(
  req: Request,
  res: Response,
  next: NextFunction
): void {
  // 处理逻辑
  next(); // 调用下一个中间件
}

// 异步中间件
export async function asyncMiddleware(
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    // 异步操作
    await someAsyncOperation();
    next();
  } catch (error) {
    next(error);
  }
}

// 带配置的中间件工厂
export function configurableMiddleware(options: Options) {
  return (req: Request, res: Response, next: NextFunction) => {
    // 使用 options
    next();
  };
}
```

## 认证中间件

### JWT 认证

```typescript
// middleware/auth.ts
import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';

// 扩展 Express Request 类型
declare global {
  namespace Express {
    interface Request {
      user?: {
        id: string;
        email: string;
        role: string;
      };
    }
  }
}

export function authenticate(req: Request, res: Response, next: NextFunction) {
  // 从 header 获取 token
  const authHeader = req.headers.authorization;

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({
      success: false,
      error: 'No token provided'
    });
  }

  const token = authHeader.substring(7);

  try {
    // 验证 token
    const decoded = jwt.verify(token, process.env.JWT_SECRET!) as {
      id: string;
      email: string;
      role: string;
    };

    // 将用户信息附加到 request
    req.user = decoded;
    next();
  } catch (error) {
    return res.status(401).json({
      success: false,
      error: 'Invalid or expired token'
    });
  }
}

// 可选认证（不强制要求）
export function optionalAuth(
  req: Request,
  res: Response,
  next: NextFunction
) {
  const authHeader = req.headers.authorization;

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return next();
  }

  const token = authHeader.substring(7);

  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET!);
    req.user = decoded;
  } catch (error) {
    // 忽略错误，继续处理
  }

  next();
}
```

### 角色授权

```typescript
// middleware/authorization.ts
import { Request, Response, NextFunction } from 'express';

export function requireRole(...roles: string[]) {
  return (req: Request, res: Response, next: NextFunction) => {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        error: 'Authentication required'
      });
    }

    if (!roles.includes(req.user.role)) {
      return res.status(403).json({
        success: false,
        error: 'Insufficient permissions'
      });
    }

    next();
  };
}

// 使用示例
app.get('/admin/dashboard',
  authenticate,
  requireRole('admin', 'superadmin'),
  adminController.dashboard
);
```

## 验证中间件

### Zod 验证

```typescript
// middleware/validation.ts
import { Request, Response, NextFunction } from 'express';
import { ZodSchema, ZodError } from 'zod';

export function validate(schema: ZodSchema) {
  return (req: Request, res: Response, next: NextFunction) => {
    try {
      // 验证请求体
      req.body = schema.parse(req.body);
      next();
    } catch (error) {
      if (error instanceof ZodError) {
        return res.status(400).json({
          success: false,
          error: 'Validation failed',
          details: error.errors.map(err => ({
            field: err.path.join('.'),
            message: err.message
          }))
        });
      }
      next(error);
    }
  };
}

// 使用示例
import { z } from 'zod';

const createUserSchema = z.object({
  email: z.string().email('Invalid email format'),
  username: z.string().min(3).max(20),
  password: z.string().min(8),
  age: z.number().min(18).optional()
});

app.post('/users',
  validate(createUserSchema),
  userController.create
);
```

### 参数验证

```typescript
// middleware/paramValidation.ts
export function validateId(paramName: string = 'id') {
  return (req: Request, res: Response, next: NextFunction) => {
    const id = req.params[paramName];

    // UUID 验证
    const uuidRegex =
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

    if (!uuidRegex.test(id)) {
      return res.status(400).json({
        success: false,
        error: 'Invalid ID format'
      });
    }

    next();
  };
}

// 使用
app.get('/users/:id',
  validateId('id'),
  userController.findById
);
```

## 日志中间件

### 请求日志

```typescript
// middleware/logger.ts
import { Request, Response, NextFunction } from 'express';
import pino from 'pino';

const logger = pino({
  level: process.env.LOG_LEVEL || 'info'
});

export function requestLogger(
  req: Request,
  res: Response,
  next: NextFunction
) {
  const startTime = Date.now();

  // 记录请求开始
  logger.info({
    method: req.method,
    path: req.path,
    query: req.query,
    ip: req.ip,
    userAgent: req.get('user-agent')
  });

  // 监听响应完成
  res.on('finish', () => {
    const duration = Date.now() - startTime;

    logger.info({
      method: req.method,
      path: req.path,
      status: res.statusCode,
      duration: `${duration}ms`
    });
  });

  next();
}

// 开发环境详细日志
export function devLogger(
  req: Request,
  res: Response,
  next: NextFunction
) {
  if (process.env.NODE_ENV === 'development') {
    console.log(`📥 ${req.method} ${req.path}`);
    console.log('Headers:', req.headers);
    console.log('Body:', req.body);
  }
  next();
}
```

### 错误日志

```typescript
// middleware/errorLogger.ts
import * as Sentry from '@sentry/node';

export function errorLogger(
  error: Error,
  req: Request,
  res: Response,
  next: NextFunction
) {
  // 记录到 Sentry
  Sentry.captureException(error, {
    tags: {
      method: req.method,
      path: req.path
    },
    user: req.user ? {
      id: req.user.id,
      email: req.user.email
    } : undefined
  });

  // 记录到日志
  logger.error({
    error: error.message,
    stack: error.stack,
    method: req.method,
    path: req.path,
    body: req.body
  });

  next(error);
}
```

## CORS 中间件

```typescript
// middleware/cors.ts
import { Request, Response, NextFunction } from 'express';

export function corsHandler(
  req: Request,
  res: Response,
  next: NextFunction
) {
  const allowedOrigins = [
    'https://example.com',
    'https://www.example.com',
    process.env.FRONTEND_URL
  ];

  const origin = req.headers.origin;

  if (origin && allowedOrigins.includes(origin)) {
    res.setHeader('Access-Control-Allow-Origin', origin);
  }

  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, PATCH');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  res.setHeader('Access-Control-Allow-Credentials', 'true');
  res.setHeader('Access-Control-Max-Age', '86400'); // 24小时

  if (req.method === 'OPTIONS') {
    res.sendStatus(204);
    return;
  }

  next();
}
```

## 速率限制中间件

```typescript
// middleware/rateLimit.ts
import rateLimit from 'express-rate-limit';

// 通用速率限制
export const generalLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15分钟
  max: 100, // 最多100次请求
  message: 'Too many requests',
  standardHeaders: true,
  legacyHeaders: false,
  handler: (req, res) => {
    res.status(429).json({
      success: false,
      error: 'Too many requests, please try again later'
    });
  }
});

// 严格限制登录
export const loginLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 5, // 15分钟内最多5次
  skipSuccessfulRequests: true, // 成功的请求不计数
  message: 'Too many login attempts'
});

// 基于 IP 的限制
export const ipLimiter = rateLimit({
  windowMs: 60 * 1000, // 1分钟
  max: 30,
  keyGenerator: (req) => req.ip || 'unknown'
});

// 使用
app.use('/api', generalLimiter);
app.post('/api/auth/login', loginLimiter, authController.login);
```

## 审计中间件

```typescript
// middleware/audit.ts
import { AsyncLocalStorage } from 'async_hooks';

interface AuditContext {
  userId?: string;
  requestId: string;
  action: string;
  resource: string;
  metadata?: Record<string, any>;
}

const auditStorage = new AsyncLocalStorage<AuditContext>();

export function auditMiddleware(
  action: string,
  resource: string
) {
  return (req: Request, res: Response, next: NextFunction) => {
    const context: AuditContext = {
      userId: req.user?.id,
      requestId: req.headers['x-request-id'] as string || generateId(),
      action,
      resource,
      metadata: {
        method: req.method,
        path: req.path,
        ip: req.ip
      }
    };

    auditStorage.run(context, () => {
      next();
    });
  };
}

export function getAuditContext(): AuditContext | undefined {
  return auditStorage.getStore();
}

// 使用
app.post('/api/users',
  auditMiddleware('user.create', 'user'),
  userController.create
);
```

## 健康检查中间件

```typescript
// middleware/healthCheck.ts
export function healthCheck(req: Request, res: Response) {
  const health = {
    status: 'ok',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
    checks: {
      database: 'unknown',
      redis: 'unknown',
      external: 'unknown'
    }
  };

  // 并行检查所有依赖
  Promise.allSettled([
    checkDatabase(),
    checkRedis(),
    checkExternalAPI()
  ]).then(results => {
    health.checks.database = results[0].status === 'fulfilled' ? 'ok' : 'error';
    health.checks.redis = results[1].status === 'fulfilled' ? 'ok' : 'error';
    health.checks.external = results[2].status === 'fulfilled' ? 'ok' : 'error';

    const allHealthy = Object.values(health.checks).every(v => v === 'ok');
    res.status(allHealthy ? 200 : 503).json(health);
  });
}
```

## 中间件顺序

### Express 中间件执行顺序

```typescript
// app.ts
const app = express();

// 1. 基础中间件（最先执行）
app.use(helmet());                    // 安全头
app.use(cors());                      // CORS
app.use(express.json());              // 请求体解析
app.use(express.urlencoded({ extended: true }));

// 2. 日志中间件
app.use(requestLogger);

// 3. 自定义中间件
app.use(auditMiddleware);

// 4. 路由
app.use('/api', apiRoutes);

// 5. 错误处理中间件（最后执行）
app.use(errorLogger);
app.use(errorHandler);

// 404 处理（所有路由之后）
app.use((req, res) => {
  res.status(404).json({
    success: false,
    error: 'Not found'
  });
});
```

## 最佳实践

### ✅ 应该做的

```typescript
// 1. 中间件职责单一
export function validate(schema) { }

// 2. 支持配置
export function rateLimit(options) { }

// 3. 正确调用 next()
if (error) {
  next(error);
} else {
  next();
}

// 4. 扩展 Request 类型
declare global {
  namespace Express {
    interface Request {
      user?: User;
    }
  }
}
```

### ❌ 不应该做的

```typescript
// 1. 不要忘记调用 next()
app.use((req, res, next) => {
  validate(req);
  // 忘记 next() ❌
});

// 2. 不要在中间件中发送多个响应
app.use((req, res, next) => {
  res.json({ message: 'first' });
  res.json({ message: 'second' }); // ❌ 错误
});

// 3. 不要阻塞请求太久
app.use(async (req, res, next) => {
  await longRunningOperation(); // 可能超时 ❌
  next();
});
```

## 相关资源

- `resources/auth-middleware.md` - 认证中间件详解
- `resources/validation-middleware.md` - 验证中间件详解
- `resources/custom-middleware.md` - 自定义中间件指南

---

**技能状态**: 完成 ✅
**关键原则**: 职责单一、正确调用 next()、适当扩展类型
**执行顺序**: 安全 → 解析 → 日志 → 路由 → 错误
