# 分层架构详解

## 为什么需要分层？

### 解决的问题
```
❌ 无分层的问题：
- 业务逻辑散落在各处
- 代码难以测试
- 修改一处影响多处
- 无法复用业务逻辑
- 团队协作混乱

✅ 分层的好处：
- 职责清晰，易于理解
- 各层独立测试
- 变更隔离
- 业务逻辑可复用
- 团队分工明确
```

## 四层架构详解

### 第1层：表现层 (Presentation)

**职责**：
- 接收用户请求
- 解析请求参数
- 调用业务层
- 格式化响应
- 处理 HTTP 相关

**代码示例**：
```typescript
// routes/userRoutes.ts
router.get('/users/:id',
  (req, res) => userController.getUser(req, res)
);

// controllers/UserController.ts
class UserController {
  async getUser(req: Request, res: Response) {
    try {
      const validated = userIdSchema.parse(req.params);
      const user = await this.userService.findById(validated.id);
      this.handleSuccess(res, user);
    } catch (error) {
      this.handleError(error, res, 'getUser');
    }
  }
}
```

**不应包含**：
❌ 业务规则判断
❌ 数据库操作
❌ 复杂逻辑处理

---

### 第2层：业务层 (Business)

**职责**：
- 实现业务规则
- 编排多个仓库
- 事务管理
- 业务验证

**代码示例**：
```typescript
// services/UserService.ts
class UserService {
  async createUser(data: CreateUserDTO) {
    // 业务规则：邮箱唯一性
    const existing = await this.userRepo.findByEmail(data.email);
    if (existing) {
      throw new ConflictError('Email already exists');
    }

    // 业务规则：年龄限制
    if (data.age < 18) {
      throw new ValidationError('Must be 18+');
    }

    // 创建用户
    const user = await this.userRepo.create(data);

    // 业务规则：发送欢迎邮件
    await this.emailService.sendWelcome(user.email);

    return user;
  }
}
```

**不应包含**：
❌ HTTP 请求/响应对象
❌ 数据库实现细节
❌ UI 相关逻辑

---

### 第3层：数据访问层 (Data Access)

**职责**：
- 封装数据操作
- 隐藏存储细节
- 查询优化
- 缓存管理

**代码示例**：
```typescript
// repositories/UserRepository.ts
class UserRepository {
  async findById(id: string): Promise<User | null> {
    // 从缓存获取
    const cached = await this.cache.get(`user:${id}`);
    if (cached) return JSON.parse(cached);

    // 从数据库查询
    const user = await prisma.user.findUnique({ where: { id } });

    // 写入缓存
    if (user) {
      await this.cache.set(`user:${id}`, JSON.stringify(user), 3600);
    }

    return user;
  }

  async create(data: CreateUserDTO): Promise<User> {
    return prisma.user.create({ data });
  }
}
```

**不应包含**：
❌ 业务规则
❌ HTTP 相关
❌ 响应格式化

---

### 第4层：基础设施层 (Infrastructure)

**职责**：
- 数据库连接
- 外部服务调用
- 缓存服务
- 消息队列

**代码示例**：
```typescript
// config/database.ts
export class PrismaService {
  static main = new PrismaClient();

  static async connect() {
    await this.main.$connect();
  }
}

// config/redis.ts
export class RedisService {
  static client = createClient();

  static async connect() {
    await this.client.connect();
  }
}
```

## 层间调用规则

```
┌─────────────┐
│ 表现层      │  ← 可调用：业务层
└──────┬──────┘
       ↓
┌─────────────┐
│ 业务层      │  ← 可调用：数据访问层
└──────┬──────┘
       ↓
┌─────────────┐
│ 数据访问层  │  ← 可调用：基础设施层
└──────┬──────┘
       ↓
┌─────────────┐
│ 基础设施层  │
└─────────────┘

⚠️ 禁止：下层调用上层
⚠️ 禁止：跨层调用（表现层直接调用数据访问层）
```

## 请求生命周期示例

```
1. 用户请求 GET /api/users/123
   ↓
2. 路由匹配 userRoutes.ts
   ↓
3. 中间件执行（认证、日志等）
   ↓
4. 路由委托给控制器
   userController.getUser(req, res)
   ↓
5. 控制器验证参数
   userIdSchema.parse({ id: '123' })
   ↓
6. 控制器调用服务
   userService.findById('123')
   ↓
7. 服务调用仓库
   userRepository.findById('123')
   ↓
8. 仓库查询数据库/缓存
   prisma.user.findUnique({ where: { id: '123' } })
   ↓
9. 数据逐层返回
   Repository → Service → Controller → Express
   ↓
10. 响应返回给用户
    { "id": "123", "name": "Alice" }
```

## 常见错误

### ❌ 在路由中写业务逻辑
```typescript
// 错误示例
router.post('/users', async (req, res) => {
  // 200 行业务逻辑...
  const existing = await db.user.findUnique(...);
  if (existing) return res.status(400).json(...);
  const user = await db.user.create(...);
  await sendEmail(user.email);
  res.json(user);
});

// 正确做法
router.post('/users', (req, res) =>
  userController.create(req, res)
);
```

### ❌ 服务层直接操作数据库
```typescript
// 错误示例
class UserService {
  async getUser(id: string) {
    return prisma.user.findUnique({ where: { id } });
  }
}

// 正确做法
class UserService {
  async getUser(id: string) {
    return this.userRepository.findById(id);
  }
}
```

### ❌ 业务层包含 HTTP 细节
```typescript
// 错误示例
class UserService {
  async createUser(data: CreateUserDTO, res: Response) {
    const user = await this.repo.create(data);
    res.status(201).json(user); // ❌
  }
}

// 正确做法
class UserService {
  async createUser(data: CreateUserDTO) {
    return this.userRepository.create(data);
  }
}
```
