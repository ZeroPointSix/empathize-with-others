---
name: security
description: 安全最佳实践 - 输入验证、认证授权、数据加密、XSS/SQL注入防护、安全配置。在开发需要处理安全相关功能时使用。
---

# 安全最佳实践

## 激活时机

当满足以下条件时自动激活此技能：
- 处理用户输入
- 实现认证授权
- 存储敏感数据
- 调用外部 API
- 配置安全策略
- 审查代码安全性

## 输入验证

### 白名单验证

```typescript
// ✅ 白名单验证
function validateFileType(filename: string): boolean {
  const allowedTypes = ['.jpg', '.jpeg', '.png', '.gif'];
  const ext = path.extname(filename).toLowerCase();
  return allowedTypes.includes(ext);
}

// ❌ 黑名单验证（不安全）
function validateFileType(filename: string): boolean {
  const dangerousTypes = ['.exe', '.bat', '.sh'];
  const ext = path.extname(filename).toLowerCase();
  return !dangerousTypes.includes(ext);  // 可能遗漏新的危险类型
}
```

### 使用验证库

```typescript
import { z } from 'zod';

// 定义验证模式
const CreateUserSchema = z.object({
  email: z.string().email('Invalid email format'),
  username: z.string()
    .min(3, 'Username must be at least 3 characters')
    .max(20, 'Username must not exceed 20 characters')
    .regex(/^[a-zA-Z0-9_]+$/, 'Only letters, numbers, and underscores'),
  password: z.string()
    .min(8, 'Password must be at least 8 characters')
    .regex(/[A-Z]/, 'Must contain uppercase letter')
    .regex(/[a-z]/, 'Must contain lowercase letter')
    .regex(/[0-9]/, 'Must contain number'),
  age: z.number()
    .min(18, 'Must be 18 or older')
    .max(120, 'Invalid age'),
});

// 使用验证
function createUser(data: unknown) {
  const validated = CreateUserSchema.parse(data);
  // validated 数据类型安全
  return userService.create(validated);
}
```

### SQL 注入防护

```typescript
// ❌ 字符串拼接 - SQL 注入风险
const query = `SELECT * FROM users WHERE email = '${email}'`;

// ✅ 参数化查询
const query = 'SELECT * FROM users WHERE email = $1';
await db.query(query, [email]);

// ✅ ORM 自动防护
await prisma.user.findUnique({
  where: { email }  // Prisma 自动参数化
});
```

### XSS 防护

```typescript
// ❌ 直接渲染用户输入 - XSS 风险
<div>{userInput}</div>

// ✅ React 自动转义
<div>{userInput}</div>  // React 默认转义

// ✅ 手动转义（必要时）
import DOMPurify from 'dompurify';

function renderUserContent(content: string) {
  const clean = DOMPurify.sanitize(content);
  return <div dangerouslySetInnerHTML={{ __html: clean }} />;
}

// ✅ CSP 头
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'
```

## 认证授权

### 密码存储

```typescript
import bcrypt from 'bcrypt';
import crypto from 'crypto';

// ✅ 使用 bcrypt 哈希密码
async function hashPassword(password: string): Promise<string> {
  const salt = await bcrypt.genSalt(10);
  return bcrypt.hash(password, salt);
}

// ✅ 验证密码
async function verifyPassword(
  password: string,
  hashedPassword: string
): Promise<boolean> {
  return bcrypt.compare(password, hashedPassword);
}

// ❌ 不要使用弱哈希
// md5, sha1, sha256 都不适合密码哈希
```

### JWT 认证

```typescript
import jwt from 'jsonwebtoken';

// 生成 Token
function generateToken(userId: string): string {
  return jwt.sign(
    { userId },
    process.env.JWT_SECRET,
    { expiresIn: '1h' }
  );
}

// 验证 Token
function verifyToken(token: string): { userId: string } | null {
  try {
    return jwt.verify(token, process.env.JWT_SECRET) as { userId: string };
  } catch {
    return null;
  }
}

// 刷新 Token
function generateRefreshToken(userId: string): string {
  return jwt.sign(
    { userId, type: 'refresh' },
    process.env.JWT_REFRESH_SECRET,
    { expiresIn: '7d' }
  );
}
```

### 权限检查

```typescript
// 基于角色的访问控制（RBAC）
enum Role {
  ADMIN = 'admin',
  USER = 'user',
  GUEST = 'guest',
}

function checkPermission(userRole: Role, requiredRole: Role): boolean {
  const roleHierarchy = {
    [Role.ADMIN]: 3,
    [Role.USER]: 2,
    [Role.GUEST]: 1,
  };

  return roleHierarchy[userRole] >= roleHierarchy[requiredRole];
}

// 中间件
function requireRole(role: Role) {
  return (req, res, next) => {
    if (!checkPermission(req.user.role, role)) {
      return res.status(403).json({ error: 'Forbidden' });
    }
    next();
  };
}

// 使用
app.delete('/users/:id',
  authenticate,
  requireRole(Role.ADMIN),
  deleteUserHandler
);
```

## 数据加密

### 敏感数据加密

```typescript
import crypto from 'crypto';

const ALGORITHM = 'aes-256-gcm';
const KEY = crypto.scryptSync(process.env.ENCRYPTION_KEY, 'salt', 32);

// 加密
function encrypt(text: string): { encrypted: string; iv: string; tag: string } {
  const iv = crypto.randomBytes(16);
  const cipher = crypto.createCipheriv(ALGORITHM, KEY, iv);

  let encrypted = cipher.update(text, 'utf8', 'hex');
  encrypted += cipher.final('hex');

  const tag = cipher.getAuthTag();

  return {
    encrypted,
    iv: iv.toString('hex'),
    tag: tag.toString('hex'),
  };
}

// 解密
function decrypt(
  encrypted: string,
  ivHex: string,
  tagHex: string
): string {
  const iv = Buffer.from(ivHex, 'hex');
  const tag = Buffer.from(tagHex, 'hex');

  const decipher = crypto.createDecipheriv(ALGORITHM, KEY, iv);
  decipher.setAuthTag(tag);

  let decrypted = decipher.update(encrypted, 'hex', 'utf8');
  decrypted += decipher.final('utf8');

  return decrypted;
}
```

### HTTPS 配置

```typescript
import https from 'https';
import fs from 'fs';

const options = {
  key: fs.readFileSync('private-key.pem'),
  cert: fs.readFileSync('certificate.pem'),
  ca: fs.readFileSync('ca-bundle.crt'),

  // 安全配置
  minVersion: 'TLSv1.2',
  ciphers: [
    'ECDHE-ECDSA-AES128-GCM-SHA256',
    'ECDHE-RSA-AES128-GCM-SHA256',
    'ECDHE-ECDSA-AES256-GCM-SHA384',
    'ECDHE-RSA-AES256-GCM-SHA384',
  ].join(':'),
  honorCipherOrder: true,
};

https.createServer(options, app).listen(443);
```

## 安全头

### HTTP 安全头

```typescript
import helmet from 'helmet';

app.use(helmet());

// 或手动配置
app.use((req, res, next) => {
  // 防止点击劫持
  res.setHeader('X-Frame-Options', 'DENY');

  // 防止 MIME 类型嗅探
  res.setHeader('X-Content-Type-Options', 'nosniff');

  // XSS 保护
  res.setHeader('X-XSS-Protection', '1; mode=block');

  // 严格传输安全
  res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');

  // 内容安全策略
  res.setHeader('Content-Security-Policy', "default-src 'self'");

  // Referrer 策略
  res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');

  next();
});
```

## 安全配置

### 环境变量管理

```bash
# .env.example（提交到代码库）
DATABASE_URL=
JWT_SECRET=
ENCRYPTION_KEY=
API_KEY=

# .env.local（不提交，本地使用）
DATABASE_URL=postgresql://...
JWT_SECRET=your-secret-key
ENCRYPTION_KEY=your-encryption-key
API_KEY=your-api-key

# .env.production（生产环境）
DATABASE_URL=postgresql://...
JWT_SECRET=strong-random-production-secret
ENCRYPTION_KEY=strong-random-encryption-key
API_KEY=production-api-key
```

### CORS 配置

```typescript
import cors from 'cors';

// ✅ 严格配置
app.use(cors({
  origin: (origin, callback) => {
    const allowedOrigins = [
      'https://example.com',
      'https://www.example.com',
    ];

    if (!origin || allowedOrigins.includes(origin)) {
      callback(null, true);
    } else {
      callback(new Error('Not allowed by CORS'));
    }
  },
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization'],
  maxAge: 600,
}));
```

### 速率限制

```typescript
import rateLimit from 'express-rate-limit';

// 通用速率限制
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15分钟
  max: 100, // 最多100次请求
  message: 'Too many requests',
  standardHeaders: true,
  legacyHeaders: false,
});

app.use(limiter);

// 严格限制登录端点
const loginLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 5, // 15分钟内最多5次登录尝试
  skipSuccessfulRequests: true,
});

app.post('/api/auth/login', loginLimiter, loginHandler);
```

## 敏感数据处理

### 不记录敏感信息

```typescript
// ❌ 不要记录敏感信息
logger.info({ user });  // 可能包含密码

// ✅ 过滤敏感字段
function sanitizeUser(user: User) {
  const { password, ...safeUser } = user;
  return safeUser;
}

logger.info({ user: sanitizeUser(user) });

// ✅ 使用 JSON.stringify 的 replacer
logger.info(JSON.stringify(user, (key, value) => {
  if (key === 'password' || key === 'token') {
    return undefined;
  }
  return value;
}));
```

### 安全的密码重置

```typescript
// 生成安全的重置令牌
function generateResetToken(): string {
  return crypto.randomBytes(32).toString('hex');
}

// 设置短期有效期（15-30分钟）
const resetTokenExpiry = Date.now() + 30 * 60 * 1000;

// 发送重置链接
const resetLink = `https://example.com/reset?token=${resetToken}`;

// 验证令牌
async function verifyResetToken(token: string): Promise<boolean> {
  const reset = await db.passwordReset.findUnique({ where: { token } });

  if (!reset) return false;
  if (reset.expiresAt < new Date()) return false;
  if (reset.used) return false;

  return true;
}
```

## 安全检查清单

```markdown
### 输入验证
- [ ] 所有用户输入都验证
- [ ] 使用白名单而非黑名单
- [ ] 验证数据类型和长度
- [ ] 参数化数据库查询

### 认证授权
- [ ] 密码使用 bcrypt 哈希
- [ ] JWT 有过期时间
- [ ] 实现了适当的权限检查
- [ ] 速率限制登录尝试

### 数据保护
- [ ] 敏感数据加密存储
- [ ] 使用 HTTPS
- [ ] 设置安全 HTTP 头
- [ ] 不记录敏感信息

### 配置安全
- [ ] 环境变量不提交到代码库
- [ ] CORS 配置严格
- [ ] CSP 策略启用
- [ ] 依赖定期更新
```

## 常见漏洞防御

| 漏洞类型 | 防御方法 |
|---------|---------|
| SQL 注入 | 参数化查询、ORM |
| XSS | 输入转义、CSP |
| CSRF | CSRF Token、同源检查 |
| 点击劫持 | X-Frame-Options |
| 中间人攻击 | HTTPS、HSTS |
| 暴力破解 | 速率限制、账户锁定 |

## 相关资源

- `resources/owasp-top10.md` - OWASP Top 10 漏洞
- `resources/auth-patterns.md` - 认证模式详解
- `resources/data-encryption.md` - 数据加密指南

---

**技能状态**: 完成 ✅
**核心原则**: 永不信任用户输入
**关键资源**: OWASP、CWE、CVE
