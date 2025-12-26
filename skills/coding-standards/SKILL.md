---
name: coding-standards
description: 编码规范检查 - 命名规范、代码风格、最佳实践、代码异味识别。在编写代码、审查代码或制定团队规范时使用。
---

# 编码规范

## 激活时机

当满足以下条件时自动激活此技能：
- 编写新代码时
- 代码审查时
- 重构代码前
- 制定团队编码规范
- 配置 Linter/Formatter

## 通用原则

### 代码可读性优先
```
代码被阅读的次数远多于被编写的次数。
优先考虑人能读懂，其次考虑机器能执行。
```

### KISS 原则
```
Keep It Simple, Stupid
- 简单的解决方案优于复杂的方案
- 避免过度设计
- 清晰优于巧妙
```

### DRY 原则
```
Don't Repeat Yourself
- 重复代码提取为函数
- 重复逻辑抽象为模式
- 重复配置提取为常量
```

## 命名规范

### 变量命名

```typescript
// ✅ 好的命名
const userName = 'Alice';           // 清晰表达内容
const isActive = true;              // 布尔值用 is/has/should
const maxRetryCount = 3;            // 具体描述含义
const userResponses = [];           // 集合用复数

// ❌ 差的命名
const n = 'Alice';                  // 单字母不清晰
const flag = true;                  // flag 不表达含义
const count = 3;                    // 不完整的计数
const data = [];                    // 过于通用
```

### 函数命名

```typescript
// ✅ 动词开头，表达动作
function getUserById(id: string) { }
function validateEmail(email: string) { }
function calculateTotal(items: Item[]) { }
function handleUserClick() { }

// ❌ 不清晰的命名
function user(id: string) { }        // 动词不明确
function email(email: string) { }    // 重复参数名
function process() { }               // 太通用
```

### 类命名

```typescript
// ✅ 名词，帕斯卡命名
class UserService { }
class UserRepository { }
class EmailValidator { }
class DatabaseConnection { }

// ❌ 差的命名
class service { }                    // 小写开头
class Manager { }                    // 太通用
class Stuff { }                      // 不清晰
```

### 常量命名

```typescript
// ✅ 全大写，下划线分隔
const MAX_RETRY_COUNT = 3;
const DEFAULT_TIMEOUT_MS = 5000;
const API_BASE_URL = 'https://api.example.com';

// ❌ 差的命名
const max = 3;                       // 不完整
const timeout = 5000;                // 缺少单位
const url = 'https://...';           // 不够具体
```

### 布尔值命名

```typescript
// ✅ 使用 is/has/should/can 前缀
let isLoading = false;
let hasPermission = true;
let shouldRetry = false;
let canEdit = true;

// ❌ 差的命名
let loading = false;                 // 缺少 is
let permission = true;               // 不表达布尔含义
let retry = false;                   // 动词不明确
```

## 代码风格

### 缩进和空格

```typescript
// ✅ 一致的缩进（2 或 4 空格）
function processUser(user: User) {
  if (user.isActive) {
    const result = await userService.update(user);
    return result;
  }
}

// ❌ 不一致的缩进
function processUser(user: User){
if(user.isActive){                   // 缺少空格和缩进
const result=await userService.update(user);
return result;
}
}
```

### 函数长度

```typescript
// ✅ 短函数，职责单一
function validateEmail(email: string): boolean {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

// ❌ 过长的函数
function process(data: any) {  // 100+ 行
  // ... 大量逻辑
}
```

### 参数数量

```typescript
// ✅ 参数少于 5 个
function createUser(name: string, email: string, age: number) { }

// ✅ 多参数使用对象
function createUser(config: CreateUserConfig) { }
interface CreateUserConfig {
  name: string;
  email: string;
  age: number;
  address?: string;
}

// ❌ 参数过多
function createUser(name: string, email: string, age: number, address: string, phone: string, role: string) { }
```

### 嵌套层级

```typescript
// ✅ 扁平结构，早返回
function processUser(user: User | null) {
  if (!user) return null;
  if (!user.isActive) return null;

  return userService.process(user);
}

// ❌ 深层嵌套
function processUser(user: User | null) {
  if (user) {
    if (user.isActive) {
      if (user.isVerified) {
        return userService.process(user);
      }
    }
  }
}
```

## 代码组织

### 文件结构

```
文件顶部（按顺序）：
1. 版权声明（如有）
2. 文件注释（说明文件用途）
3. 导入语句（分组：第三方 -> 项目内 -> 类型）
4. 常量定义
5. 类型定义
6. 主要代码
7. 导出语句

示例：
/**
 * 用户服务
 * 负责用户相关业务逻辑
 */

import express from 'express';
import { UserService } from '../services/UserService';
import type { Request, Response } from 'express';

const MAX_RETRY = 3;

interface CreateUserRequest {
  name: string;
  email: string;
}

export class UserController { }
```

### 导入顺序

```typescript
// ✅ 标准导入顺序

// 1. Node.js 内置模块
import path from 'path';
import fs from 'fs';

// 2. 第三方库
import express from 'express';
import _ from 'lodash';

// 3. 项目内模块
import { UserService } from '../services/UserService';
import { UserRepository } from '../repositories/UserRepository';

// 4. 类型导入
import type { Request, Response } from 'express';
import type { User } from '../types';

// ❌ 混乱的导入
import { Request } from 'express';
import express from 'express';
import { UserService } from '../services/UserService';
import path from 'path';
```

### 注释规范

```typescript
// ✅ JSDoc 风格的函数注释
/**
 * 根据用户ID获取用户信息
 * @param id - 用户ID
 * @returns 用户对象，未找到返回 null
 * @throws {ValidationError} 当ID格式无效时抛出
 */
async function getUserById(id: string): Promise<User | null> {
  // ...
}

// ✅ 行内注释说明"为什么"
// 使用缓存避免频繁查询数据库
const cached = await cache.get(key);
if (cached) return JSON.parse(cached);

// ❌ 无意义的注释
// 获取用户
const user = getUser();

// 设置变量
let x = 1;
```

## 代码异味识别

### 长函数
```
问题：函数超过 50 行
影响：难以理解和测试
解决：拆分为多个小函数
```

### 上帝类
```
问题：一个类超过 300 行
影响：职责过多，难以维护
解决：拆分为多个类
```

### 重复代码
```
问题：相同逻辑出现多次
影响：维护成本高，易出错
解决：提取为公共函数
```

### 魔法数字
```
问题：代码中出现未命名常量
影响：含义不明确，难以修改

// ❌
if (retryCount > 3) { }

// ✅
if (retryCount > MAX_RETRY_COUNT) { }
```

### 过长的参数列表
```
问题：函数参数超过 5 个
影响：难以使用和理解
解决：使用对象参数
```

### 特性依恋
```
问题：类过度使用其他类的功能
影响：高耦合
解决：重新设计职责划分
```

## 不同语言规范

### TypeScript/JavaScript

```typescript
// 类型注解
const name: string = 'Alice';
const count: number = 0;
const active: boolean = true;

// 可选链
const email = user?.contact?.email;

// 空值合并
const timeout = config?.timeout ?? 3000;

// const 断言
const config = {
  port: 3000,
} as const;
```

### Python

```python
# 类型注解
def get_user(user_id: str) -> User | None:
    ...

# 列表推导式
names = [user.name for user in users if user.active]

# 上下文管理器
with open('file.txt') as f:
    content = f.read()
```

### Go

```go
// 错误处理
result, err := someFunction()
if err != nil {
    return nil, err
}

// 接口实现
type UserService struct {
    repo UserRepository
}

func (s *UserService) GetUser(id string) (*User, error) {
    return s.repo.FindByID(id)
}
```

## 配置工具

### ESLint (JavaScript/TypeScript)
```json
{
  "extends": [
    "eslint:recommended",
    "typescript-eslint:recommended"
  ],
  "rules": {
    "no-console": "warn",
    "no-unused-vars": "error",
    "prefer-const": "error"
  }
}
```

### Prettier
```json
{
  "semi": true,
  "singleQuote": true,
  "tabWidth": 2,
  "trailingComma": "es5"
}
```

### Black (Python)
```toml
[tool.black]
line-length = 88
target-version = ['py39']
```

## 相关资源

- `resources/naming-conventions.md` - 详细命名规范
- `resources/code-smells.md` - 代码异味完整列表
- `resources/linter-configs.md` - Linter 配置参考

---

**技能状态**: 完成 ✅
**覆盖语言**: TypeScript, JavaScript, Python, Go, Java 等
