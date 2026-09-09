# DevLab V1：可上线技术博客

> 技术栈：Next.js + React + TypeScript + Spring Boot + PostgreSQL + Docker + Nginx + HTTPS + GitHub Actions

---

## 1. 阶段目标

V1 的目标不是“做一个 CRUD Demo”，而是完成一条完整工程链路：

```text
需求设计
  ↓
数据库设计
  ↓
Spring Boot 后端
  ↓
Next.js 前端
  ↓
鉴权与权限
  ↓
Markdown 内容系统
  ↓
文件上传
  ↓
测试
  ↓
Docker
  ↓
Linux VPS
  ↓
域名
  ↓
Nginx
  ↓
HTTPS
  ↓
CI/CD
  ↓
备份恢复
```

完成后，必须可以通过真实域名访问，例如：

```text
https://devlab.example.com
```

---

## 2. 本阶段范围

### 2.1 必做

- 用户注册、登录、退出
- Access Token + Refresh Token
- ADMIN / USER 两级权限
- 文章 CRUD
- 草稿 / 发布 / 归档
- Markdown 编辑与预览
- 分类
- 标签
- 评论与审核
- 图片上传
- 文章搜索
- SEO
- PostgreSQL
- Flyway
- Docker Compose
- Linux 部署
- Nginx
- HTTPS
- GitHub Actions
- 数据库与上传文件备份
- 恢复演练

### 2.2 本阶段明确不做

- Redis
- Kafka / RabbitMQ
- Go 微服务
- Python 服务
- Spring AI
- pgvector
- Elasticsearch
- Kubernetes
- Prometheus / Grafana
- 分布式事务
- 服务注册发现

原则：

> V1 先把“一个完整系统从代码到公网运行”做通。

---

# 3. 技术栈

## 3.1 前端

```text
TypeScript
React
Next.js
Tailwind CSS
shadcn/ui（可选）
Markdown Renderer
Syntax Highlight
```

推荐：

- 包管理：pnpm
- HTTP：fetch / 自定义 API Client
- 表单：React Hook Form
- 校验：Zod
- E2E：Playwright

---

## 3.2 后端

```text
Java 21+
Spring Boot
Spring MVC
Spring Security
Spring Data JPA
Bean Validation
Flyway
Spring Boot Actuator
OpenAPI / Swagger
JUnit
Testcontainers
```

---

## 3.3 数据

```text
PostgreSQL
```

V1 不引入 Redis。

---

## 3.4 部署

```text
Ubuntu Server
Docker
Docker Compose
Nginx
Let's Encrypt
Certbot
GitHub Actions
GHCR / Docker Registry
```

---

# 4. 总体架构

```text
                        Internet
                           │
                           ▼
                   devlab.example.com
                           │
                         HTTPS
                           │
                           ▼
                         Nginx
                 ┌─────────┴─────────┐
                 │                   │
                 ▼                   ▼
            Next.js :3000      Spring Boot :8080
                 │                   │
                 │                   ▼
                 │              PostgreSQL :5432
                 │
                 └──────── API ──────┘
```

建议路由：

```text
/              → Next.js
/api/*         → Spring Boot
/uploads/*     → 文件资源
```

公网只开放：

```text
22
80
443
```

不要公网暴露：

```text
3000
8080
5432
```

---

# 5. 仓库结构

建议 Monorepo：

```text
devlab/
├── frontend/
│   ├── src/
│   ├── public/
│   ├── package.json
│   ├── pnpm-lock.yaml
│   └── Dockerfile
│
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
│
├── deploy/
│   ├── nginx/
│   ├── scripts/
│   ├── compose.yaml
│   └── compose.prod.yaml
│
├── docs/
│   ├── architecture.md
│   ├── database.md
│   ├── api.md
│   └── deploy.md
│
├── .github/
│   └── workflows/
│       ├── ci.yml
│       └── deploy.yml
│
├── .env.example
├── .gitignore
└── README.md
```

---

# 6. 领域模块

后端采用 Package by Feature：

```text
com.devlab
├── auth
├── user
├── article
├── taxonomy
├── comment
├── media
├── search
├── admin
└── common
```

每个模块内部可以包含：

```text
controller
service
repository
entity
dto
mapper
```

V1 不强制完整 DDD。

---

# 7. 数据库设计

核心表：

```text
users
refresh_tokens
articles
categories
tags
article_tags
comments
media_files
```

---

## 7.1 users

建议字段：

```text
id
username
email
password_hash
nickname
avatar_url
bio
role
status
created_at
updated_at
```

约束：

```text
username UNIQUE
email UNIQUE
```

角色：

```text
ADMIN
USER
```

状态：

```text
ACTIVE
DISABLED
```

---

## 7.2 refresh_tokens

字段：

```text
id
user_id
token_hash
device_info
expires_at
revoked_at
created_at
```

目标：

- 支持刷新
- 支持注销
- 支持 Token 撤销
- 支持多设备会话

不要把明文 Refresh Token 直接存数据库。

---

## 7.3 articles

字段：

```text
id
author_id
category_id
title
slug
summary
content_markdown
cover_image
status
seo_title
seo_description
view_count
created_at
updated_at
published_at
```

状态：

```text
DRAFT
PUBLISHED
ARCHIVED
```

约束：

```text
slug UNIQUE
```

推荐索引：

```text
status
published_at
category_id
author_id
```

---

## 7.4 categories

```text
id
name
slug
description
created_at
updated_at
```

约束：

```text
name UNIQUE
slug UNIQUE
```

---

## 7.5 tags

```text
id
name
slug
created_at
```

约束：

```text
name UNIQUE
slug UNIQUE
```

---

## 7.6 article_tags

```text
article_id
tag_id
```

联合主键或联合唯一索引：

```text
(article_id, tag_id)
```

---

## 7.7 comments

```text
id
article_id
user_id
parent_id
content
status
created_at
updated_at
```

状态：

```text
PENDING
APPROVED
REJECTED
```

V1 最多支持一层回复或两层 UI 展示。

---

## 7.8 media_files

```text
id
owner_id
original_name
storage_key
url
content_type
size
created_at
```

---

# 8. Flyway

不要依赖：

```text
ddl-auto=update
```

推荐：

```text
spring.jpa.hibernate.ddl-auto=validate
```

数据库结构由 Flyway 管理：

```text
V1__init.sql
V2__create_auth_tables.sql
V3__create_article_tables.sql
V4__create_comment_table.sql
V5__create_media_table.sql
```

要求：

- 所有结构变化必须提交 Migration
- 禁止线上手工改表后不补 SQL
- Migration 进入 Git 管理

---

# 9. 统一 API 规范

成功：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

错误：

```json
{
  "code": 40001,
  "message": "username already exists",
  "data": null
}
```

分页：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [],
    "page": 1,
    "size": 20,
    "total": 100,
    "totalPages": 5
  }
}
```

后端实现：

```text
ApiResponse<T>
PageResponse<T>
ErrorCode
BusinessException
GlobalExceptionHandler
```

---

# 10. 认证与权限

## 10.1 注册

```text
POST /api/auth/register
```

字段：

```text
username
email
password
confirmPassword
```

校验：

```text
username: 3~30
email: 合法邮箱格式
password: >= 8
```

---

## 10.2 登录

```text
POST /api/auth/login
```

支持：

```text
username / email
+
password
```

---

## 10.3 Token

采用：

```text
Access Token
+
Refresh Token
```

推荐：

- Access Token 短期有效
- Refresh Token 长期有效
- Refresh Token 使用 HttpOnly Cookie
- 生产环境启用 Secure
- 设置合理 SameSite
- 退出时撤销 Refresh Token

---

## 10.4 API

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
GET  /api/auth/me
```

---

## 10.5 权限矩阵

| 功能 | 游客 | USER | ADMIN |
|---|---:|---:|---:|
| 查看文章 | ✅ | ✅ | ✅ |
| 搜索文章 | ✅ | ✅ | ✅ |
| 注册登录 | ✅ | ✅ | ✅ |
| 评论 | ❌ | ✅ | ✅ |
| 编辑资料 | ❌ | ✅ | ✅ |
| 后台管理 | ❌ | ❌ | ✅ |
| 文章发布 | ❌ | ❌ | ✅ |
| 评论审核 | ❌ | ❌ | ✅ |

---

# 11. 文章系统

## 11.1 管理功能

```text
创建文章
编辑文章
删除文章
保存草稿
预览
发布
归档
分页
按状态筛选
按分类筛选
按标签筛选
```

API 示例：

```text
GET    /api/articles
GET    /api/articles/{slug}

POST   /api/admin/articles
PUT    /api/admin/articles/{id}
DELETE /api/admin/articles/{id}

POST   /api/admin/articles/{id}/publish
POST   /api/admin/articles/{id}/archive
```

---

## 11.2 slug

文章 URL：

```text
/articles/spring-security-jwt-guide
```

而不是：

```text
/articles/123
```

要求：

- slug 唯一
- 支持后台编辑
- 修改 slug 前考虑旧地址兼容，V1 可先不实现 Redirect History

---

# 12. Markdown

后台编辑器：

```text
┌─────────────────────┬──────────────────────┐
│ Markdown Editor     │ Preview              │
├─────────────────────┼──────────────────────┤
│ # Title             │ Rendered Title       │
│                     │                      │
│ ```java             │ highlighted code     │
│ ...                 │                      │
│ ```                 │                      │
└─────────────────────┴──────────────────────┘
```

至少支持：

- Heading
- Table
- Blockquote
- List
- Code Block
- Link
- Image
- GFM
- Syntax Highlight
- Copy Code

---

# 13. 分类与标签

## Category

一篇文章一个主要分类。

例如：

```text
Java
Go
AI
Database
DevOps
Frontend
```

---

## Tag

一篇文章多个标签。

例如：

```text
Spring Boot
Spring Security
JWT
PostgreSQL
Docker
```

---

# 14. 评论

功能：

```text
发表评论
回复评论
后台查看待审核
通过
拒绝
删除
```

API：

```text
GET    /api/articles/{id}/comments
POST   /api/articles/{id}/comments

GET    /api/admin/comments
PUT    /api/admin/comments/{id}/status
DELETE /api/admin/comments/{id}
```

---

# 15. 搜索

V1 使用 PostgreSQL。

入口：

```text
GET /api/search?q=spring
```

搜索字段：

```text
title
summary
category
tags
```

V1 先不引 Elasticsearch。

---

# 16. 文件上传

抽象接口：

```text
StorageService
├── LocalStorageService
└── FutureStorageService
```

V1 存储：

```text
/data/devlab/uploads
```

Docker Volume 持久化。

后续可替换：

```text
S3
Cloudflare R2
MinIO
OSS
COS
```

---

# 17. 前端页面

## 前台

```text
/
├── /articles
├── /articles/[slug]
├── /categories
├── /categories/[slug]
├── /tags
├── /tags/[slug]
├── /search
├── /login
├── /register
└── /profile
```

---

## 后台

```text
/admin
├── /admin/articles
├── /admin/articles/new
├── /admin/articles/[id]/edit
├── /admin/categories
├── /admin/tags
├── /admin/comments
├── /admin/users
└── /admin/media
```

---

# 18. SEO

每篇文章支持：

```text
title
description
canonical
OpenGraph
Twitter Card
```

站点：

```text
robots.txt
sitemap.xml
```

文章详情优先服务端渲染或预渲染。

---

# 19. 测试

## 后端

```text
JUnit
Spring Boot Test
Testcontainers
```

至少测试：

- 注册成功
- 重复用户名
- 登录成功
- 密码错误
- Refresh Token
- USER 无权发布文章
- ADMIN 可以发布文章
- slug 唯一
- 文章分页
- 评论创建
- 评论审核

数据库集成测试使用真实 PostgreSQL Container。

---

## 前端

使用 Playwright 做至少一个关键 E2E：

```text
ADMIN 登录
  ↓
创建文章
  ↓
发布
  ↓
访问首页
  ↓
进入文章详情
```

---

# 20. Actuator

启用：

```text
/actuator/health
```

用于：

- Docker healthcheck
- 部署后健康检查
- CI/CD 验证

生产环境不要暴露所有 Actuator 端点。

---

# 21. Docker

至少：

```text
devlab-frontend
devlab-backend
devlab-postgres
```

Volume：

```text
postgres_data
uploads_data
```

开发：

```bash
docker compose up -d
```

生产：

```bash
docker compose \
  -f compose.yaml \
  -f compose.prod.yaml \
  up -d
```

---

# 22. Linux 部署

推荐目录：

```text
/opt/devlab
├── compose.yaml
├── compose.prod.yaml
├── .env
├── nginx/
├── data/
└── backup/
```

服务器安装：

```text
Docker
Docker Compose
Nginx
Certbot
```

---

# 23. DNS

示例：

```text
A
devlab.example.com
→ VPS_PUBLIC_IP
```

可选：

```text
www.devlab.example.com
```

---

# 24. Nginx

逻辑：

```text
/
→ 127.0.0.1:3000

/api/
→ 127.0.0.1:8080

/uploads/
→ 文件目录
```

建议 Host 端口只绑定：

```text
127.0.0.1:3000
127.0.0.1:8080
```

PostgreSQL 不映射公网端口。

---

# 25. HTTPS

使用：

```text
Let's Encrypt
Certbot
```

要求：

```text
HTTP → 301 → HTTPS
```

验证自动续期。

---

# 26. 环境变量

`.env.example`：

```text
POSTGRES_DB=devlab
POSTGRES_USER=devlab
POSTGRES_PASSWORD=
JWT_SECRET=
APP_DOMAIN=
```

严禁提交真实：

```text
DB_PASSWORD
JWT_SECRET
PRIVATE_KEY
```

---

# 27. CI

GitHub Actions：

```text
push / pull_request
      │
      ├── backend test
      ├── backend package
      ├── frontend lint
      ├── frontend test
      └── frontend build
```

所有检查通过后才允许合并。

---

# 28. CD

推荐：

```text
push main
   │
   ▼
CI
   │
   ▼
Build Docker Images
   │
   ▼
Push GHCR
   │
   ▼
Deploy VPS
   │
   ├── docker compose pull
   ├── docker compose up -d
   └── health check
```

---

# 29. 数据备份

数据库：

```text
cron
 ↓
pg_dump
 ↓
gzip
 ↓
/opt/devlab/backup
```

建议保留：

```text
7~14 天
```

上传文件也必须备份。

最重要：

> 必须做一次完整恢复演练。

---

# 30. 开发顺序

## Step 0：初始化仓库

- 创建 Monorepo
- 配置 `.gitignore`
- 创建 README
- 创建 docs
- 初始化 frontend/backend

---

## Step 1：数据库

- 设计 ER
- 创建 Flyway
- 创建核心表
- 添加索引
- 添加唯一约束
- 本地迁移验证

---

## Step 2：Spring Boot 基础设施

完成：

```text
统一响应
异常处理
Validation
JPA
Flyway
OpenAPI
Actuator
```

---

## Step 3：Auth/User

完成：

```text
Register
Login
Refresh
Logout
JWT
Role
Spring Security
Profile
```

---

## Step 4：Article

完成：

```text
CRUD
Draft
Publish
Archive
Pagination
Slug
```

---

## Step 5：Category/Tag

完成：

```text
Category CRUD
Tag CRUD
Article-Tag
```

---

## Step 6：Next.js 前台

完成：

```text
首页
文章列表
文章详情
分类
标签
搜索
```

---

## Step 7：后台

完成：

```text
登录
Dashboard
文章管理
分类管理
标签管理
```

---

## Step 8：Markdown/Media

完成：

```text
Markdown Editor
Preview
代码高亮
上传图片
```

---

## Step 9：评论

完成：

```text
评论
回复
审核
```

---

## Step 10：测试

完成：

```text
Backend Integration Test
Frontend E2E
```

---

## Step 11：Docker

做到：

```bash
docker compose up -d
```

可以完整启动系统。

---

## Step 12：公网部署

完成：

```text
VPS
DNS
Nginx
HTTPS
```

---

## Step 13：CI/CD

做到：

```text
git push
→ 自动测试
→ 自动构建
→ 自动部署
```

---

## Step 14：Backup

- 自动备份数据库
- 自动备份上传文件
- 人工执行一次恢复

---

# 31. Definition of Done

只有以下全部完成，V1 才结束：

- [ ] 用户可注册
- [ ] 用户可登录与退出
- [ ] Refresh Token 可刷新与撤销
- [ ] ADMIN 权限有效
- [ ] 管理员可创建 Markdown 文章
- [ ] 支持草稿/发布/归档
- [ ] Category / Tag 完成
- [ ] 图片上传可用
- [ ] 评论可发布
- [ ] 管理员可审核评论
- [ ] 搜索可用
- [ ] SEO 基础完成
- [ ] Docker Compose 一键启动
- [ ] 公网域名可访问
- [ ] HTTPS 正常
- [ ] GitHub Actions CI 正常
- [ ] 自动部署正常
- [ ] PostgreSQL 可备份
- [ ] 上传文件可备份
- [ ] 完成一次恢复演练

---

# 32. 常见风险

### 风险 1：过早微服务化

解决：

> V1 保持模块化单体。

### 风险 2：为了“先进”引入过多组件

解决：

> 任何新组件必须回答“V1 为什么现在需要它”。

### 风险 3：只开发不部署

解决：

> 域名 + HTTPS + CI/CD 是 V1 必做，不是附加任务。

### 风险 4：JWT 只写 Demo

解决：

> Refresh Token、注销、撤销、HttpOnly Cookie 必须真实实现。

### 风险 5：备份从未恢复过

解决：

> V1 结束前做一次真实恢复。

---

# 33. 进入 V2 的条件

V1 上线并稳定运行后，再进入 Redis 阶段。

V2 不重构整个系统，只增强：

```text
缓存
会话
限流
排行榜
UV
分布式锁
事件流
```

这时 Redis 才有真实业务价值。
