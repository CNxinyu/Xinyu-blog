# Xinyu·Aletheia 后台管理端

第五阶段后台前端，使用 Vite、Vue 3、TypeScript、Vue Router、Pinia、Axios 和 Element Plus。当前仅面向本地开发与同源部署，不包含在线托管配置。

## 功能

- 管理员登录、Refresh Cookie 会话恢复和安全退出
- Dashboard 内容指标、最近文章和快速入口
- 文章筛选、分页、Markdown 双栏编辑、预览及草稿/发布/归档生命周期
- 分类和标签增删改查
- 评论筛选、审核、拒绝、退回待审核和删除
- 用户筛选、启用/禁用和管理员角色维护
- 窄屏导航抽屉、编辑/预览切换及统一加载、空态、错误与 TraceId 提示

## 环境要求

- Node.js 24
- npm
- Spring Boot 后端默认运行在 `http://127.0.0.1:8080`

复制环境变量示例：

```powershell
Copy-Item .env.example .env.local
```

如后端地址不同，修改：

```dotenv
VITE_API_PROXY_TARGET=http://127.0.0.1:8080
```

## 本地运行

```powershell
npm install
npm run dev
```

浏览器访问 Vite 输出的本地地址。前端始终请求同源 `/api/**`，开发服务器将其代理到 Spring Boot，因此无需修改后端 CORS。

## 登录机制

1. 页面先读取 `GET /api/v1/auth/csrf`，将 CSRF Token 保存在内存。
2. 登录成功后 Access Token 仅保存在 Pinia 内存，不写入 LocalStorage 或 SessionStorage。
3. HttpOnly Refresh Cookie 由后端管理；刷新页面时通过 `POST /api/v1/auth/refresh` 轮换并恢复会话。
4. 并发 401 合并为一次 Refresh；恢复失败后返回登录页并保留原目标地址。
5. 非 `ADMIN + ACTIVE` 用户即使凭据正确，也会立即退出并拒绝进入后台。

生产环境需要由反向代理同时提供静态文件和 `/api/**`，保持浏览器视角同源。生产部署前还必须按后端约定启用 Secure Cookie 并迁移 Secret；本项目不提供跨域 Cookie 方案。

## 验证

```powershell
npm run lint
npm run test
npm run build
```

测试使用 Mock API，不会启动 Flyway、连接或写入 PostgreSQL。构建产物位于 `dist/`。

## 路由

| 路由 | 用途 |
| --- | --- |
| `/login` | 管理员登录 |
| `/dashboard` | 内容概览 |
| `/articles` | 文章列表 |
| `/articles/new` | 新建文章 |
| `/articles/:id/edit` | 编辑文章 |
| `/categories` | 分类管理 |
| `/tags` | 标签管理 |
| `/comments` | 评论审核 |
| `/users` | 用户管理 |

本阶段不包含自动保存、富文本编辑、图片上传、版本历史、批量操作、审计日志、主题切换或线上发布。
