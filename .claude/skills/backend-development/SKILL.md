---
name: backend-development
description: 后端开发 - Node.js/Express、控制器服务仓库模式、中间件、异步处理。在开发后端 API 和服务时使用。
---

# 后端开发

## 激活时机

当满足以下条件时自动激活此技能：
- 开发 Node.js/Express API
- 实现后端业务逻辑
- 设计服务层架构
- 编写数据库操作
- 实现中间件
- 处理异步请求

## 分层架构

### 四层架构模式

```
HTTP Request
    ↓
Routes (路由层)
    ↓
Controllers (控制器层)
    ↓
Services (服务层)
    ↓
Repositories (仓库层)
    ↓
Database (数据库)
```

### 各层职责

| 层级 | 职责 | 不应包含 |
|------|------|----------|
| **Routes** | 路由定义、中间件注册 | 业务逻辑、数据操作 |
| **Controllers** | 请求处理、响应格式化 | 业务规则、数据库操作 |
| **Services** | 业务逻辑、流程编排 | HTTP、数据库细节 |
| **Repositories** | 数据访问抽象 | 业务规则、HTTP 响应 |

## 路由层

### 路由定义

```typescript
// routes/userRoutes.ts
import express from 'express';
import { UserController } from '../controllers/UserController';
import { authenticate } from '../middleware/auth';
import { validate } from '../middleware/validation';
import { createUserSchema, updateUserSchema } from '../validators/userSchema';

const router = express.Router();
const userController = new UserController();

// 公开路由
router.post('/',
  validate(createUserSchema),
  (req, res) => userController.create(req, res)
);

// 认证路由
router.get('/',
  authenticate,
  (req, res) => userController.findAll(req, res)
);

router.get('/:id',
  authenticate,
  (req, res) => userController.findById(req, res)
);

router.patch('/:id',
  authenticate,
  validate(updateUserSchema),
  (req, res) => userController.update(req, res)
);

router.delete('/:id',
  authenticate,
  (req, res) => userController.delete(req, res)
);

export { router as userRoutes };
```

### 路由注册

```typescript
// app.ts
import express from 'express';
import { userRoutes } from './routes/userRoutes';
import { postRoutes } from './routes/postRoutes';

const app = express();

// 中间件
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// 路由
app.use('/api/users', userRoutes);
app.use('/api/posts', postRoutes);

// 错误处理
app.use(errorHandler);

export { app };
```

## 控制器层

### BaseController 模式

```typescript
// controllers/BaseController.ts
import { Response } from 'express';
import * as Sentry from '@sentry/node';

export abstract class BaseController {
  // 成功响应
  protected handleSuccess<T>(
    res: Response,
    data: T,
    message?: string
  ): void {
    res.status(200).json({
      success: true,
      data,
      message: message || 'Success'
    });
  }

  // 创建响应
  protected handleCreated<T>(
    res: Response,
    data: T,
    message?: string
  ): void {
    res.status(201).json({
      success: true,
      data,
      message: message || 'Created'
    });
  }

  // 错误处理
  protected handleError(
    error: Error,
    res: Response,
    context: string
  ): void {
    Sentry.captureException(error, {
      tags: { context }
    });

    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        success: false,
        error: error.message
      });
    } else {
      res.status(500).json({
        success: false,
        error: 'Internal server error'
      });
    }
  }

  // 分页响应
  protected handlePaginated<T>(
    res: Response,
    data: T[],
    total: number,
    page: number,
    limit: number
  ): void {
    res.status(200).json({
      success: true,
      data,
      meta: {
        total,
        page,
        limit,
        totalPages: Math.ceil(total / limit)
      }
    });
  }
}
```

### 具体控制器

```typescript
// controllers/UserController.ts
import { Request, Response } from 'express';
import { BaseController } from './BaseController';
import { UserService } from '../services/UserService';

export class UserController extends BaseController {
  private userService: UserService;

  constructor() {
    super();
    this.userService = new UserService();
  }

  async create(req: Request, res: Response): Promise<void> {
    try {
      const user = await this.userService.create(req.body);
      this.handleCreated(res, user, 'User created successfully');
    } catch (error) {
      this.handleError(error as Error, res, 'UserController.create');
    }
  }

  async findById(req: Request, res: Response): Promise<void> {
    try {
      const user = await this.userService.findById(req.params.id);
      this.handleSuccess(res, user);
    } catch (error) {
      this.handleError(error as Error, res, 'UserController.findById');
    }
  }

  async findAll(req: Request, res: Response): Promise<void> {
    try {
      const { page = '1', limit = '20' } = req.query;
      const result = await this.userService.findAll({
        page: Number(page),
        limit: Number(limit)
      });
      this.handlePaginated(
        res,
        result.data,
        result.total,
        Number(page),
        Number(limit)
      );
    } catch (error) {
      this.handleError(error as Error, res, 'UserController.findAll');
    }
  }

  async update(req: Request, res: Response): Promise<void> {
    try {
      const user = await this.userService.update(req.params.id, req.body);
      this.handleSuccess(res, user, 'User updated successfully');
    } catch (error) {
      this.handleError(error as Error, res, 'UserController.update');
    }
  }

  async delete(req: Request, res: Response): Promise<void> {
    try {
      await this.userService.delete(req.params.id);
      res.status(204).send();
    } catch (error) {
      this.handleError(error as Error, res, 'UserController.delete');
    }
  }
}
```

## 服务层

### 服务基类

```typescript
// services/BaseService.ts
export abstract class BaseService<T> {
  abstract repository: any;

  async findAll(options: PaginationOptions): Promise<PaginatedResult<T>> {
    const [data, total] = await Promise.all([
      this.repository.findMany(options),
      this.repository.count(options.where)
    ]);

    return { data, total };
  }

  async findById(id: string): Promise<T> {
    const entity = await this.repository.findById(id);
    if (!entity) {
      throw new NotFoundError(this.getEntityName());
    }
    return entity;
  }

  protected abstract getEntityName(): string;
}
```

### 具体服务

```typescript
// services/UserService.ts
import { UserRepository } from '../repositories/UserRepository';
import { ConflictError, ValidationError } from '../errors/AppError';

export class UserService {
  private userRepository: UserRepository;

  constructor() {
    this.userRepository = new UserRepository();
  }

  async create(data: CreateUserDTO): Promise<User> {
    // 业务规则：检查邮箱唯一性
    const existing = await this.userRepository.findByEmail(data.email);
    if (existing) {
      throw new ConflictError('Email already exists');
    }

    // 业务规则：年龄限制
    if (data.age && data.age < 18) {
      throw new ValidationError('Must be 18 or older');
    }

    // 哈希密码
    const hashedPassword = await hashPassword(data.password);

    // 创建用户
    const user = await this.userRepository.create({
      ...data,
      password: hashedPassword
    });

    // 发送欢迎邮件
    await this.emailService.sendWelcome(user.email);

    return user;
  }

  async update(id: string, data: UpdateUserDTO): Promise<User> {
    const user = await this.findById(id);

    // 业务规则：更新邮箱时检查唯一性
    if (data.email && data.email !== user.email) {
      const existing = await this.userRepository.findByEmail(data.email);
      if (existing) {
        throw new ConflictError('Email already exists');
      }
    }

    return this.userRepository.update(id, data);
  }

  async delete(id: string): Promise<void> {
    await this.findById(id);
    await this.userRepository.delete(id);
  }
}
```

## 仓库层

### 仓库基类

```typescript
// repositories/BaseRepository.ts
import { PrismaClient } from '@prisma/client';

export abstract class BaseRepository<T> {
  protected prisma: PrismaClient;

  constructor() {
    this.prisma = new PrismaClient();
  }

  async findById(id: string): Promise<T | null> {
    return this.getModel().findUnique({
      where: { id }
    });
  }

  async findMany(options: any): Promise<T[]> {
    return this.getModel().findMany(options);
  }

  async create(data: any): Promise<T> {
    return this.getModel().create({ data });
  }

  async update(id: string, data: any): Promise<T> {
    return this.getModel().update({
      where: { id },
      data
    });
  }

  async delete(id: string): Promise<T> {
    return this.getModel().delete({
      where: { id }
    });
  }

  async count(where?: any): Promise<number> {
    return this.getModel().count({ where });
  }

  protected abstract getModel(): any;
}
```

### 具体仓库

```typescript
// repositories/UserRepository.ts
import { PrismaClient, User } from '@prisma/client';
import { BaseRepository } from './BaseRepository';

export class UserRepository extends BaseRepository<User> {
  protected getModel(): any {
    return this.prisma.user;
  }

  async findByEmail(email: string): Promise<User | null> {
    return this.prisma.user.findUnique({
      where: { email }
    });
  }

  async findByUsername(username: string): Promise<User | null> {
    return this.prisma.user.findUnique({
      where: { username }
    });
  }

  async findActive(): Promise<User[]> {
    return this.prisma.user.findMany({
      where: { status: 'ACTIVE' }
    });
  }
}
```

## 错误处理

### 自定义错误

```typescript
// errors/AppError.ts
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

export class UnauthorizedError extends AppError {
  constructor(message: string = 'Unauthorized') {
    super(message, 401);
  }
}

export class ForbiddenError extends AppError {
  constructor(message: string = 'Forbidden') {
    super(message, 403);
  }
}
```

### 错误处理中间件

```typescript
// middleware/errorHandler.ts
import { Request, Response, NextFunction } from 'express';
import { ZodError } from 'zod';
import { AppError } from '../errors/AppError';

export function errorHandler(
  error: Error,
  req: Request,
  res: Response,
  next: NextFunction
) {
  // Zod 验证错误
  if (error instanceof ZodError) {
    return res.status(400).json({
      success: false,
      error: 'Validation failed',
      details: error.errors
    });
  }

  // 自定义应用错误
  if (error instanceof AppError) {
    return res.status(error.statusCode).json({
      success: false,
      error: error.message
    });
  }

  // 未知错误
  console.error('Unexpected error:', error);
  return res.status(500).json({
    success: false,
    error: 'Internal server error'
  });
}
```

## 异步处理

### Promise 错误处理包装器

```typescript
// middleware/asyncHandler.ts
export function asyncHandler(
  fn: (req: Request, res: Response, next: NextFunction) => Promise<any>
) {
  return (req: Request, res: Response, next: NextFunction) => {
    Promise.resolve(fn(req, res, next)).catch(next);
  };
}

// 使用
router.get('/users',
  asyncHandler(async (req, res) => {
    const users = await userService.findAll();
    res.json(users);
  })
);
```

### 并发处理

```typescript
// 并行执行独立操作
async function getUserDashboard(userId: string) {
  const [user, posts, stats] = await Promise.all([
    userService.findById(userId),
    postService.findByUser(userId),
    statsService.getUserStats(userId)
  ]);

  return { user, posts, stats };
}

// 带错误处理的并发
async function getUserData(userId: string) {
  const results = await Promise.allSettled([
    userService.findById(userId),
    notificationService.getUnread(userId)
  ]);

  const user = results[0].status === 'fulfilled' ? results[0].value : null;
  const notifications = results[1].status === 'fulfilled' ? results[1].value : [];

  return { user, notifications };
}
```

## 最佳实践

### ✅ 应该做的

```typescript
// 1. 分层清晰
Routes → Controllers → Services → Repositories

// 2. 使用 BaseController/BaseService
class UserController extends BaseController { }

// 3. 所有错误都捕获
try {
  await operation();
} catch (error) {
  this.handleError(error, res, 'context');
}

// 4. 业务逻辑在服务层
class UserService {
  async create(data: CreateUserDTO) {
    // 业务规则在这里
  }
}
```

### ❌ 不应该做的

```typescript
// 1. 不要在路由中写业务逻辑
router.post('/users', async (req, res) => {
  // 200 行业务逻辑 ❌
});

// 2. 不要在控制器中直接访问数据库
class UserController {
  async create(req, res) {
    const user = await prisma.user.create({ ... }); // ❌
  }
}

// 3. 不要吞掉错误
try {
  await operation();
} catch (error) {
  // 什么都不做 ❌
}
```

## 相关资源

- `resources/controllers.md` - 控制器详解
- `resources/services.md` - 服务层详解
- `resources/repositories.md` - 仓库模式详解
- `resources/async-patterns.md` - 异步模式指南

---

**技能状态**: 完成 ✅
**推荐框架**: Express, Fastify, NestJS
**数据库 ORM**: Prisma, TypeORM, Sequelize
