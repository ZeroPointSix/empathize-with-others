---
name: api-design
description: API 设计 - RESTful 规范、接口设计、版本管理、错误处理、API 文档。在设计和开发 API 接口时使用。
---

# API 设计

## 激活时机

当满足以下条件时自动激活此技能：
- 设计新的 API 接口
- 重构现有 API
- 制定 API 规范
- 编写 API 文档
- API 版本升级

## RESTful 设计原则

### 资源命名

```
✅ 使用名词复数
GET    /users          # 获取用户列表
GET    /users/123      # 获取特定用户
POST   /users          # 创建用户
PUT    /users/123      # 更新用户
DELETE /users/123      # 删除用户

❌ 避免的模式
GET    /getUser        # 动词
GET    /user           # 单数
GET    /users/get/123  # 路径包含动作
```

### URL 层级设计

```
✅ 层级清晰，不超过3层
/users/123/posts/456/comments

❌ 层级过深
/users/123/posts/456/comments/789/replies/999

✅ 使用查询参数替代过深路径
/posts/456/comments?replyTo=789
```

### HTTP 方法语义

| 方法 | 安全 | 幂等 | 用途 |
|------|------|------|------|
| GET | ✅ | ✅ | 获取资源 |
| POST | ❌ | ❌ | 创建资源 |
| PUT | ❌ | ✅ | 完整更新资源 |
| PATCH | ❌ | ❌ | 部分更新资源 |
| DELETE | ❌ | ✅ | 删除资源 |

## 接口设计规范

### 请求设计

#### URL 设计

```
# 基础资源
GET    /api/v1/users
POST   /api/v1/users
GET    /api/v1/users/{id}
PUT    /api/v1/users/{id}
PATCH  /api/v1/users/{id}
DELETE /api/v1/users/{id}

# 子资源
GET    /api/v1/users/{id}/posts
POST   /api/v1/users/{id}/posts
GET    /api/v1/posts/{id}/comments

# 过滤和搜索
GET    /api/v1/users?status=active
GET    /api/v1/users?name=Alice&age=25
GET    /api/v1/posts?q=TypeScript

# 分页
GET    /api/v1/users?page=1&limit=20
GET    /api/v1/posts?offset=0&limit=20

# 排序
GET    /api/v1/users?sort=createdAt:desc
GET    /api/v1/users?sort=-createdAt  # 降序
GET    /api/v1/users?sort=+createdAt  # 升序

# 字段选择
GET    /api/v1/users?fields=id,name,email
```

#### 请求头

```
# 通用请求头
Content-Type: application/json
Accept: application/json
Authorization: Bearer {token}
User-Agent: MyApp/1.0
X-Request-ID: {uuid}

# 条件请求
If-None-Match: "etag-value"
If-Modified-Since: Wed, 21 Oct 2024 07:28:00 GMT

# 内容协商
Accept-Language: zh-CN
Accept-Encoding: gzip, deflate
```

#### 请求体

```json
// POST /api/v1/users
{
  "email": "alice@example.com",
  "username": "alice",
  "password": "SecurePass123",
  "profile": {
    "firstName": "Alice",
    "lastName": "Smith"
  }
}

// PATCH /api/v1/users/123
{
  "email": "newemail@example.com"
}
```

### 响应设计

#### 成功响应

```json
// 200 OK - 获取资源
{
  "data": {
    "id": "123",
    "email": "alice@example.com",
    "username": "alice",
    "createdAt": "2024-01-01T00:00:00Z"
  },
  "meta": {
    "timestamp": "2024-01-01T12:00:00Z",
    "requestId": "abc-123"
  }
}

// 201 Created - 创建资源
{
  "data": {
    "id": "123",
    "email": "alice@example.com"
  },
  "meta": {
    "location": "/api/v1/users/123"
  }
}

// 200 OK - 列表响应
{
  "data": [...],
  "meta": {
    "total": 100,
    "page": 1,
    "limit": 20,
    "totalPages": 5
  },
  "links": {
    "self": "/api/v1/users?page=1",
    "next": "/api/v1/users?page=2",
    "prev": null,
    "first": "/api/v1/users?page=1",
    "last": "/api/v1/users?page=5"
  }
}
```

#### 错误响应

```json
// 标准错误格式
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      {
        "field": "email",
        "message": "Invalid email format"
      },
      {
        "field": "password",
        "message": "Password must be at least 8 characters"
      }
    ],
    "requestId": "abc-123",
    "timestamp": "2024-01-01T12:00:00Z"
  }
}

// HTTP 状态码
400 Bad Request           # 请求参数错误
401 Unauthorized          # 未认证
403 Forbidden             # 无权限
404 Not Found             # 资源不存在
409 Conflict              # 资源冲突
422 Unprocessable Entity  # 验证失败
429 Too Many Requests     # 请求频率限制
500 Internal Server Error # 服务器错误
503 Service Unavailable   # 服务不可用
```

## API 版本管理

### 版本策略

```
# URL 版本（推荐）
/api/v1/users
/api/v2/users

# 请求头版本
Accept: application/vnd.myapi.v1+json
Accept: application/vnd.myapi.v2+json

# 查询参数版本（不推荐）
/api/users?version=1
```

### 版本升级

```
# v1 → v2 变更类型

# 破坏性变更（需要新版本）
- 删除字段
- 修改字段类型
- 修改响应结构
- 修改请求参数

# 非破坏性变更（保持版本）
- 添加新字段
- 添加新端点
- 添加可选参数
```

## 认证授权

### Bearer Token 认证

```http
GET /api/v1/users/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### API Key 认证

```http
GET /api/v1/users
X-API-Key: your-api-key-here
```

### OAuth 2.0

```http
# 获取 Token
POST /oauth/token
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials&client_id=xxx&client_secret=xxx

# 使用 Token
GET /api/v1/users
Authorization: Bearer {access_token}
```

## 速率限制

```
# 响应头
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1609459200

# 超限响应
429 Too Many Requests
{
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Rate limit exceeded. Try again in 60 seconds.",
    "retryAfter": 60
  }
}
```

## API 文档

### OpenAPI 规范

```yaml
openapi: 3.0.0
info:
  title: User API
  version: 1.0.0
  description: User management API

paths:
  /users:
    get:
      summary: List users
      parameters:
        - name: page
          in: query
          schema:
            type: integer
            default: 1
        - name: limit
          in: query
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: Successful response
          content:
            application/json:
              schema:
                type: object
                properties:
                  data:
                    type: array
                    items:
                      $ref: '#/components/schemas/User'

    post:
      summary: Create user
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateUser'
      responses:
        '201':
          description: User created

components:
  schemas:
    User:
      type: object
      properties:
        id:
          type: string
        email:
          type: string
        username:
          type: string

    CreateUser:
      type: object
      required:
        - email
        - password
      properties:
        email:
          type: string
          format: email
        password:
          type: string
          minLength: 8
```

## 最佳实践

### ✅ 应该做的

```http
# 1. 使用名词复数
GET /users

# 2. 使用标准 HTTP 方法
GET /users/123
POST /users
PUT /users/123
DELETE /users/123

# 3. 使用查询参数过滤
GET /users?status=active&age=25

# 4. 分页使用 page/limit
GET /users?page=1&limit=20

# 5. 返回合适的 HTTP 状态码
200 OK          # 成功
201 Created     # 创建成功
400 Bad Request # 请求错误
404 Not Found   # 未找到
```

### ❌ 不应该做的

```http
# 1. 不要在 URL 中使用动词
GET /getUsers
POST /createUser
DELETE /deleteUser/123

# 2. 不要返回不必要的数据
GET /users?fields=*  # 返回所有字段

# 3. 不要忽略错误处理
# 总是返回适当的错误响应

# 4. 不要在 URL 中传递复杂参数
GET /users?filter=age%3E25%20and%20status%3Dactive
# 应该使用 POST 或简化参数
GET /users?ageMin=25&status=active
```

## 相关资源

- `resources/restful-design.md` - RESTful 设计详解
- `resources/api-versions.md` - API 版本管理策略
- `resources/openapi-template.yaml` - OpenAPI 模板

---

**技能状态**: 完成 ✅
**推荐规范**: RESTful, GraphQL, gRPC
**文档工具**: OpenAPI/Swagger, Postman, Insomnia
