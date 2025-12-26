---
name: database-operations
description: 数据库操作 - Prisma ORM、SQL 查询优化、事务处理、连接池管理、数据迁移。在进行数据库相关开发时使用。
---

# 数据库操作

## 激活时机

当满足以下条件时自动激活此技能：
- 设计数据库表结构
- 编写数据库查询
- 优化查询性能
- 处理数据库事务
- 执行数据迁移
- 连接数据库

## Prisma ORM

### Schema 定义

```prisma
// schema.prisma

generator client {
  provider = "prisma-client-js"
}

datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}

model User {
  id        String   @id @default(uuid())
  email     String   @unique
  username  String   @unique
  password  String
  profile   Profile?
  posts     Post[]
  createdAt DateTime @default(now())
  updatedAt DateTime @updatedAt

  @@index([email])
  @@index([username])
}

model Profile {
  id        String   @id @default(uuid())
  userId    String   @unique
  user      User     @relation(fields: [userId], references: [id])
  firstName String?
  lastName  String?
  bio       String?
  avatar    String?

  @@index([userId])
}

model Post {
  id        String   @id @default(uuid())
  title     String
  content   String
  published Boolean  @default(false)
  authorId  String
  author    User     @relation(fields: [authorId], references: [id])
  tags      Tag[]
  createdAt DateTime @default(now())
  updatedAt DateTime @updatedAt

  @@index([authorId])
  @@index([published])
}

model Tag {
  id    String @id @default(uuid())
  name  String @unique
  posts Post[]
}
```

### 基础 CRUD 操作

```typescript
import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

// Create
const user = await prisma.user.create({
  data: {
    email: 'alice@example.com',
    username: 'alice',
    password: hashedPassword,
    profile: {
      create: {
        firstName: 'Alice',
        lastName: 'Smith',
      }
    }
  }
});

// Read - Find unique
const user = await prisma.user.findUnique({
  where: { email: 'alice@example.com' },
  include: { profile: true }
});

// Read - Find many
const users = await prisma.user.findMany({
  where: { profile: { firstName: 'Alice' } },
  select: { id: true, email: true, username: true },
  orderBy: { createdAt: 'desc' },
  take: 10,
  skip: 0,
});

// Update
const user = await prisma.user.update({
  where: { id: userId },
  data: { email: 'newemail@example.com' }
});

// Delete
await prisma.user.delete({
  where: { id: userId }
});

// Upsert
const user = await prisma.user.upsert({
  where: { email: 'alice@example.com' },
  update: { username: 'alice_updated' },
  create: {
    email: 'alice@example.com',
    username: 'alice',
    password: hashedPassword,
  }
});
```

### 复杂查询

```typescript
// 关联查询
const usersWithPosts = await prisma.user.findMany({
  include: {
    posts: {
      where: { published: true },
      include: { tags: true },
      orderBy: { createdAt: 'desc' },
    },
    profile: true,
  }
});

// 条件查询
const posts = await prisma.post.findMany({
  where: {
    AND: [
      { published: true },
      { author: { email: { contains: '@example.com' } } },
      { createdAt: { gte: new Date('2024-01-01') } },
    ],
    OR: [
      { title: { contains: 'TypeScript' } },
      { content: { contains: 'TypeScript' } },
    ],
  },
});

// 聚合查询
const stats = await prisma.post.aggregate({
  where: { published: true },
  _count: { id: true },
  _avg: { id: true },
  _sum: { id: true },
  _min: { createdAt: true },
  _max: { createdAt: true },
});

// 分组查询
const postCounts = await prisma.post.groupBy({
  by: ['authorId'],
  _count: { id: true },
  having: { id: { _count: { gt: 5 } } },
});

// 事务查询
const users = await prisma.user.findMany({
  cursor: { id: lastUserId },
  take: 20,
});
```

## 事务处理

### 顺序事务

```typescript
const result = await prisma.$transaction([
  prisma.user.create({
    data: { email: 'alice@example.com', username: 'alice', password: '...' }
  }),
  prisma.post.create({
    data: {
      title: 'Hello World',
      content: 'My first post',
      authorId: 'user-id'
    }
  }),
]);
```

### 嵌套事务

```typescript
const result = await prisma.$transaction(async (tx) => {
  // 创建用户
  const user = await tx.user.create({
    data: { email: 'alice@example.com', username: 'alice', password: '...' }
  });

  // 创建帖子
  const post = await tx.post.create({
    data: {
      title: 'Hello World',
      content: 'My first post',
      authorId: user.id
    }
  });

  // 如果这里抛出错误，整个事务会回滚
  if (someCondition) {
    throw new Error('Transaction failed');
  }

  return { user, post };
});
```

### 隔离级别

```typescript
await prisma.$transaction(
  async (tx) => {
    // 事务操作
  },
  {
    maxWait: 5000,      // 最大等待时间
    timeout: 10000,     // 事务超时
    isolationLevel: Prisma.TransactionIsolationLevel.Serializable,
  }
);
```

## 查询优化

### 避免 N+1 问题

```typescript
// ❌ N+1 问题
const users = await prisma.user.findMany();
for (const user of users) {
  const posts = await prisma.post.findMany({  // N 次查询
    where: { authorId: user.id }
  });
  user.posts = posts;
}

// ✅ 使用 include 一次查询
const users = await prisma.user.findMany({
  include: {
    posts: true
  }
});
```

### 选择性查询字段

```typescript
// ✅ 只选择需要的字段
const users = await prisma.user.findMany({
  select: {
    id: true,
    email: true,
    username: true,
    // 不查询 password, createdAt 等字段
  }
});
```

### 索引优化

```prisma
// 单列索引
@@index([email])
@@index([username])

// 复合索引
@@index([authorId, published])

// 唯一索引
@@unique([email])
@@unique([username, companyId])
```

### 分页查询

```typescript
// 基于游标的分页（性能更好）
const posts = await prisma.post.findMany({
  take: 20,
  cursor: cursor ? { id: cursor } : undefined,
  skip: cursor ? 1 : 0,
  orderBy: { id: 'asc' },
});

// 基于偏移的分页（简单但性能较差）
const posts = await prisma.post.findMany({
  take: 20,
  skip: (page - 1) * 20,
  orderBy: { createdAt: 'desc' },
});
```

## 仓库模式

```typescript
// repositories/UserRepository.ts
export class UserRepository {
  constructor(private prisma: PrismaClient) {}

  async findById(id: string): Promise<User | null> {
    return this.prisma.user.findUnique({
      where: { id },
      include: { profile: true }
    });
  }

  async findByEmail(email: string): Promise<User | null> {
    return this.prisma.user.findUnique({
      where: { email }
    });
  }

  async create(data: CreateUserDTO): Promise<User> {
    return this.prisma.user.create({
      data,
      include: { profile: true }
    });
  }

  async update(id: string, data: UpdateUserDTO): Promise<User> {
    return this.prisma.user.update({
      where: { id },
      data,
    });
  }

  async delete(id: string): Promise<User> {
    return this.prisma.user.delete({
      where: { id },
    });
  }

  async findMany(params: PaginationParams): Promise<User[]> {
    return this.prisma.user.findMany({
      where: params.where,
      select: params.select,
      orderBy: params.orderBy,
      take: params.take,
      skip: params.skip,
    });
  }

  async count(where?: Prisma.UserWhereInput): Promise<number> {
    return this.prisma.user.count({ where });
  }
}

// 使用
const userRepo = new UserRepository(prisma);
const user = await userRepo.findById('123');
```

## 数据迁移

### 创建迁移

```bash
# 创建迁移
npx prisma migrate dev --name add_user_role

# 应用迁移
npx prisma migrate deploy

# 重置数据库
npx prisma migrate reset

# 查看迁移状态
npx prisma migrate status
```

### 自定义迁移

```typescript
// prisma/migrations/123456_add_user_role/migration.sql
-- AlterTable
ALTER TABLE "User" ADD COLUMN "role" TEXT NOT NULL DEFAULT 'user';

-- Update existing users
UPDATE "User" SET "role" = 'admin' WHERE "email" LIKE '%@admin.com';
```

## 连接管理

### Prisma 单例

```typescript
// lib/prisma.ts
import { PrismaClient } from '@prisma/client';

class PrismaService {
  private static instance: PrismaClient;

  static get main(): PrismaClient {
    if (!this.instance) {
      this.instance = new PrismaClient({
        log: process.env.NODE_ENV === 'development'
          ? ['query', 'info', 'warn', 'error']
          : ['error'],
      });
    }
    return this.instance;
  }

  static async connect() {
    await this.main.$connect();
  }

  static async disconnect() {
    await this.main.$disconnect();
  }
}

export { PrismaService };
```

### 连接池配置

```prisma
datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")

  // 连接池配置（在 DATABASE_URL 中添加）
  // postgresql://user:pass@host:5432/db?connection_limit=10&pool_timeout=20
}
```

## 原始 SQL

```typescript
// 执行原始 SQL
const result = await prisma.$queryRaw`
  SELECT * FROM "User"
  WHERE "email" = ${email}
`;

// 执行原始 SQL 并映射
const users = await prisma.$queryRaw<User[]>`
  SELECT id, email, username
  FROM "User"
  WHERE "createdAt" > ${date}
`;

// 执行不返回结果的 SQL
await prisma.$executeRaw`
  UPDATE "User" SET "role" = 'admin'
  WHERE "email" = ${email}
`;
```

## 最佳实践

### ✅ 应该做的

```typescript
// 1. 使用事务处理多表操作
await prisma.$transaction(async (tx) => {
  const user = await tx.user.create({ ... });
  await tx.profile.create({ ... });
});

// 2. 使用索引优化查询
@@index([authorId, published])

// 3. 使用 include 避免N+1
const users = await prisma.user.findMany({
  include: { posts: true }
});

// 4. 使用仓库模式封装数据访问
class UserRepository {
  async findById(id: string) { ... }
}
```

### ❌ 不应该做的

```typescript
// 1. 不要在循环中查询
for (const id of userIds) {
  await prisma.user.findUnique({ where: { id } });  // ❌
}

// ✅ 批量查询
await prisma.user.findMany({
  where: { id: { in: userIds } }
});

// 2. 不要查询所有字段
await prisma.user.findMany();  // ❌ 包含所有字段

// ✅ 选择需要的字段
await prisma.user.findMany({
  select: { id: true, email: true }
});

// 3. 不要忽略错误
try {
  await prisma.user.create({ ... });
} catch (error) {
  // ❌ 忽略错误
}

// ✅ 处理错误
if (error instanceof Prisma.PrismaClientKnownRequestError) {
  if (error.code === 'P2002') {
    // 唯一约束冲突
    throw new ConflictError('Email already exists');
  }
}
```

## 相关资源

- `resources/prisma-queries.md` - Prisma 查询参考
- `resources/migration-guide.md` - 数据迁移指南
- `resources/performance-tuning.md` - 性能调优

---

**技能状态**: 完成 ✅
**支持数据库**: PostgreSQL, MySQL, SQLite, SQL Server, MongoDB
**推荐 ORM**: Prisma, TypeORM, Sequelize
