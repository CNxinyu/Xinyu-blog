# Xinyu-blog 项目协作约定

## 终端与范围

- 项目根目录：`C:\Users\87773\Desktop\Xinyu-blog`
- 终端统一使用 PowerShell 7：`C:\Program Files\PowerShell\7\pwsh.exe`
- 阶段性开发默认只修改当前阶段涉及的模块，并保留其他未提交改动。

## 数据库约束

- 数据库相关变更只提交 SQL，不直接执行建表、迁移、插入、更新或删除操作。
- 测试阶段可连接本机 PostgreSQL 做只读连通性检查；禁止对用户数据库执行 Flyway 迁移或其他写操作。
- 本地测试连接参数：主机 `localhost`，端口 `5432`，数据库 `xinyu`，用户 `postgres`。
- 当前处于内部测试阶段，PostgreSQL 测试密码允许直接写入 `application.yml`；进入预发布或生产前必须迁移到外部 Secret，并从仓库删除明文密码。
- 当前处于内部测试阶段，JWT RSA 测试密钥允许暂时写入 `application.yml`；进入预发布或生产前必须替换为外部 Secret，并从仓库删除私钥。
- 结构变更统一放在 `java-backend/src/main/resources/db/migration/`，由用户自行审核和执行。

## 阶段更新同步

每次阶段性工作完成后，必须同步更新本文件，至少包含：

1. 当前阶段与更新时间。
2. 已完成的模块、接口或迁移 SQL。
3. 已执行的验证命令及结果。
4. 未执行项目、环境依赖、风险和下一阶段入口。

## 当前阶段状态

更新时间：2026-09-10

### 第五阶段：Xinyu·Aletheia 后台管理端

#### 2026-09-10 首个管理员初始化与 JWT 签名修复

- 经用户明确授权完成一次性本地管理员初始化：通过注册接口生成后端标准 `{bcrypt}` 密码哈希，再将该账号提升为 `ADMIN + ACTIVE`；未记录明文账号密码，未执行建表、迁移或其他业务数据修改。
- 定位登录 500 的根因为原内部测试 RSA 私钥虽可解析且与公钥模数一致，但其 CRT 参数无法被 Java 21 `SHA256withRSA` 完成签名，异常为 `RSA private key operation failed`。
- 将 `application.yml` 中的内部测试 RSA-2048 密钥轮换为可在 Java 21 正常签发/验签的新密钥对，并同步更新 `key-id`；此密钥仍只允许内部测试使用，预发布或生产必须迁移到外部 Secret。
- `JwtKeyConfig` 新增启动期公私钥签发/验签自检；密钥损坏或不匹配时应用会在启动阶段明确失败，不再延迟到用户登录后返回 500。`JwtTokenServiceTest` 增加不匹配密钥拒绝测试。

验证结果：

- Java 21 下 `mvn.cmd -Dtest=JwtTokenServiceTest test`：3 个通过。
- Java 21 下 `mvn.cmd test`：61 个通过，1 个 Testcontainers PostgreSQL 集成测试按默认开关跳过。
- 使用禁用 Flyway、独立端口的临时实例复现旧密钥异常并验证新构建；随后重启本机 `127.0.0.1:8080` 后端，健康检查为 `UP`，真实 `登录 → /api/v1/auth/me → 退出` 链路通过，管理员角色为 `ADMIN`，验证产生的 Refresh Token 已撤销。

未执行项目、风险与下一阶段入口：

- 未执行 Flyway、数据库结构变更或迁移 SQL；除用户明确授权的首个管理员注册和角色提升外，未修改其他业务数据。当前运行中的本地后端显式禁用了 Flyway。
- RSA 测试私钥仍位于仓库配置，仅符合当前内部测试约定；后续进入预发布前必须轮换为外部 Secret，并同步调整 `key-id`。
- 下一入口：使用 Vue 后台完成真实页面联调；生产化前补充外部 Secret、Secure Cookie、同源反向代理及密钥轮换流程。

#### 2026-09-10 Vite + Vue 管理后台完成

- 将 `vue-frontend` 的 Vite + Vue 3 + TypeScript Demo 完整替换为 Aletheia 管理后台；保留用户此前将旧 Vue 游戏示例升级为 TypeScript Demo、删除旧游戏素材和切换 npm 锁文件的未提交改动，并在其上继续开发。
- 新增 Vue Router、Pinia、Axios、Element Plus、Element Plus Icons 与 DOMPurify；完成 `/login`、`/dashboard`、`/articles`、`/articles/new`、`/articles/:id/edit`、`/categories`、`/tags`、`/comments`、`/users` 和后台 404 路由。
- 登录流程接入现有 CSRF、Bearer Access Token、HttpOnly Refresh Cookie 和 Token 轮换契约：CSRF Token 与 Access Token 只保存在内存；刷新页面自动恢复管理员会话；并发 401 合并为一次 Refresh；失败返回登录页并保留目标路由；非 `ADMIN + ACTIVE` 用户会立即退出并拒绝进入后台。
- Dashboard 复用现有接口并行聚合文章总数、发布数、草稿数、待审核评论、用户、分类和标签数量，展示最近更新文章；单项失败不会阻断其他指标。未新增 Java 聚合接口。
- 文章管理支持关键词、状态、分类、标签筛选及分页，Markdown 双栏编辑、450ms 防抖服务端预览、DOMPurify 二次净化、未保存离开提醒、草稿/发布/归档和永久删除；分类/标签、评论审核与用户角色/状态管理均接入第三阶段和第二阶段已有接口及保护规则。
- 后台使用深黑褐、暗紫、古铜和象牙色的克制魔法视觉；桌面侧栏支持折叠，窄屏改为抽屉导航和编辑/预览切换；未新增人物或场景图片。开发环境通过 `VITE_API_PROXY_TARGET` 将同源 `/api/**` 代理到默认 `http://127.0.0.1:8080`。
- 新增 ESLint、Vitest 和 4 组测试文件，覆盖管理员/普通用户登录、会话恢复失败、并发 Refresh、路由守卫、安全重定向、Dashboard 聚合和部分失败、错误码/TraceId，以及文章生命周期、分类标签、评论和用户管理接口契约。

验证结果：

- 当前本机 Node 24.9.0、npm 11.11.0 下 `npm run lint`：通过，无警告。
- `npm run test`：4 个测试文件、12 个测试通过；使用 Mock API，未启动 Spring Boot、Flyway 或数据库。
- `npm run build`：通过；`vue-tsc` 类型检查和 Vite 8.2.2 production build 成功。Element Plus 全量引入使主入口压缩前约 874 kB，Vite 给出大 chunk 提示但不影响构建。
- 本地 Vite 仅监听 `127.0.0.1:4175`；非浏览器 HTTP 检查 `/login` 与 `/dashboard` 均返回 200，检查后服务器已关闭。
- Sites 通用 `build-site.mjs` 仍因当前运行时找不到项目内 `node_modules/npm/bin/npm-cli.js` 与 `npm-prefix.js` 而无法启动；项目自身 npm lint/test/build 均成功，不影响本地交付。

未执行项目、风险与下一阶段入口：

- 未修改或启动 Java 后端，未连接、迁移或写入 PostgreSQL，未新增 SQL，未部署，也未执行截图或浏览器 DOM 自动化。
- 当前本机 Node 24.9.0 在安装时对 npm 间接依赖给出要求 Node 24.15+ 的 `EBADENGINE` 提示；实际验证已通过，后续应切换到项目约定的 Node 24.19.0 消除环境告警。
- 生产环境必须由反向代理同源提供静态文件和 `/api/**`，并按后端约定启用 Secure Cookie、迁移数据库/JWT/邮件 Secret；本阶段不提供跨域 Cookie 方案。
- 下一入口：使用真实管理员账号和已执行 V3/V4 迁移的本地后端完成联调；后续可按实际体积需求改为 Element Plus 按需导入，并继续图片上传、文章版本历史、批量操作和审计日志。

### 第四阶段：Xinyu·Aletheia 前台

#### 2026-09-10 桌面对话框名字标签修复

- 修复桌面端 Aletheia 名字标签被对话框滚动区域裁掉的问题：取消标签向上越界的负偏移，将其完整放置在文本框内，并增加正文顶部留白，避免名字与台词重叠。
- 窄屏头像展开面板仍沿用原有流式名字样式，未改变手机端关闭、恢复和滚动行为；未修改人物素材、路由、API、Java 后端或数据库。
- Node 24.19.0 下 `npm run lint`、`npm run build` 通过；Next.js 16.3.4 完成 TypeScript 检查并识别 9 个路由入口。`git diff --check` 通过（仅有现有 LF/CRLF 提示），本地开发服务器首页返回 HTTP 200。

下一入口：在实际浏览器中刷新首页确认标签与台词间距；若希望恢复标签压住边框的视觉，可后续把滚动层拆成外框与内部正文两层，而不再让标签处于裁切容器之外。

#### 2026-09-10 人物动态与桌面对话框优化

- 桌面端（宽度 >= 1024px 且高度 >= 560px）恢复第一版 Galgame 式底部对话框：文本框固定横跨页面下侧，并与右侧人物分离；人物仍固定在视口右侧，内容区继续预留人物宽度，页面尾部增加对话框安全空间，避免文章和页脚被遮挡。
- 人物新增缓慢呼吸／漂浮、紫色魔力光晕、月光扫过和精细指针视差；指针视差最大约 5px × 3px，不与页面滚动绑定。`prefers-reduced-motion: reduce` 下关闭所有新增位移、光扫与过渡，保留静态人物。
- 窄屏继续沿用 56px 可收起头像与展开面板；桌面样式通过媒体查询隔离，不改变手机端展开、Escape 关闭、焦点返回及首页流内台词行为。
- 使用 Sites 构建规范完成现有站点迭代，保留 Next.js、现有依赖、路由、API 和本地交付方式；项目不存在 `.openai/hosting.json`，未注册、部署或新增托管配置。
- Node 24.19.0 下 `npm run lint`、`npm run build` 通过，Next.js 16.3.4 完成 TypeScript 检查并识别 9 个路由入口；`git diff --check` 通过（仅有现有 LF/CRLF 转换提示）。本轮未执行截图或浏览器 DOM 自动化；现有 `companion-layout.mjs` 已同步更新桌面断言，后续显式视觉回归时会检查底部文本框宽度、人物位于其上方及内容避让。
- Sites 通用 `build-site.mjs` 在当前独立 Node 24 运行时中因找不到其预期的项目内 npm CLI 而未能启动；项目自身 npm CLI 的 lint/build 已成功完成，不影响本地 Next.js 构建结果。

下一入口：在用户实际笔记本分辨率下确认动态强度与底部文本框高度；若长台词变多，可进一步增加内部滚动或台词逐字显示，但本轮未扩展交互范围。

#### 2026-09-10 人物定位与响应式修复

- 布局修复已实施：首页与内容页复用 `AletheiaCompanion`，人物与台词位于同一个视口固定容器；正文、页脚预留统一右栏，导航高度通过 ResizeObserver 实测，取消人物 sticky／台词 fixed 的混合定位和外壳 overflow:hidden。
- 宽度 >= 1024px 且高度 >= 560px 时展示右侧人物；帽子、脸部与上半身优先，允许下半身裁切。其余视口默认使用 56px 小头像，支持展开半身与台词、关闭、Escape、焦点返回、收起头像和恢复入口。
- 首页改用网格与正常文档流，短屏允许滚动；窄屏保留正文流内的菜单台词和进入链接，头像收起时仍可进入分区。开场期间工坊内容 inert，避免键盘误入遮罩后的菜单。
- 新增 `react_frontend/tests/companion-layout.mjs` 和测试说明；隔离 Mock API 与 Next production preview 仅监听本机随机端口，结束自动关闭，不影响用户开发服务器。
- Node 24.19.0：`npm run lint`、`npm run build` 通过；`git diff --check` 通过（现有 LF/CRLF 提示）。浏览器回归使用本机 Edge headless：12 个视口 × 4 页面共 48 组布局检查，以及移动菜单、空列表、3 个缩放等效场景、两种动画设置的开场／返回／跳过，共 55 项通过。补充修复窄屏展开面板被导航遮挡的问题，并使用 elementFromPoint 验证脸部未被其他层覆盖。缩放采用 CSS 视口与 deviceScaleFactor 等效测试，未操作浏览器菜单缩放。
- 已查看桌面与移动端真实渲染截图。截图与机器结果在 `react_frontend/.next/companion-validation/`（构建缓存产物，后续构建可能清理），几何检查通过不等于素材边缘通过。
- **素材阻塞已解除（用户后续授权非生成式精细抠图）**：从未硬抠过的第一次正面原图建立图像专用空间遮罩、连通区域与窄边缘反混色，保护脸部、肩部、白羽和象牙衣料；最终接入 `aletheia-guide-front-matted-v4.png`，不再引用旧 v3。包含 836,842 个完全透明、41,606 个半透明、694,416 个完全不透明像素；688,458 个内部像素通过原始 RGB 与 Alpha=255 不变断言。黑色／暗紫／工坊背景及发丝、羽饰特写检查已通过。未使用生成式重绘、外部 API 或 CSS 模糊／发光掩盖边缘。
- 原始正面图副本、源图 SHA-256、可复现脚本、区域遮罩和前后对照保留在 `output/character-design/Aletheia/matting/`；旧 v3 保留供对照，未修改原始人物设定真源。此前 ImageGen 两次无 Alpha 的失败输出仍作为历史记录保留在测试说明中。
- 未改 Java 后端、数据库、路由和 API 契约，未部署。下一入口：真实文章 API 联调与实际设备视觉反馈；当前边缘 Alpha 是从烘焙背景估计重建，不宣称恢复原始分层。

以下为此前第四阶段交付记录；人物素材与小屏验收结论以上述本轮记录为准。

已完成：

- 将 `react_frontend` 的 Next.js Demo 替换为“Xinyu·Aletheia”沉浸式个人工坊前台；首页实现 `checking → cover → flying → revealing → ready` 状态机、每标签会话一次的夜空开场、骑扫帚退场、“开始旅程”入口、跳过入口、减少动画模式和 Galgame 式工坊菜单对话。
- 首页信息架构调整为大分区与子入口两级：藏书阁作为已开放的知识分区，技能书、分类卷轴、符文标签和星盘检索归入其下；炼金坊用于未来陈列自研项目，百宝箱用于未来收纳实用网站、工具与下载地址，两者当前显示为可选择的“筹备中”分区且不创建空路由。
- 新增正面视角立绘 `aletheia-guide-front-cutout-v3.png`，替换侧面构图；后续视觉反馈确认存在白边与定位缺陷，其修复状态见本轮记录，不能仅凭新资源路径判定素材合格。
- 基于 Aletheia 三视图、服装细节与标准色板生成并接入骑扫帚透明立绘、站立透明立绘和无人物魔法工坊背景；正式素材位于 `react_frontend/public/images/aletheia/`，未保留占位人物或场景。
- 新增统一内容外壳、顶部导航、原生搜索入口、右侧 Aletheia 引导区、移动端低透明度构图、羊皮纸文章卡片/正文、分页、空态、错误态、404 和搜索加载页；全站使用深黑褐、暗紫、古铜与象牙白主题，移除 `next/font/google` 网络依赖。
- 完成 `/articles`、`/articles/[slug]`、`/categories`、`/categories/[slug]`、`/tags`、`/tags/[slug]`、`/search` 路由；支持每页 9 条、非法页码归一、超页重定向、动态元数据、未知资源真实 HTTP 404、空搜索与 100 字符限制。
- 新增服务端专用 API 客户端和第三阶段 DTO 对应类型；读取 `API_BASE_URL`（默认 `http://127.0.0.1:8080`），使用 `cache: "no-store"` 校验 HTTP 状态与业务码，前端浏览器不直接跨端口请求，API 故障显示可重试提示和 TraceId，不使用模拟回退。
- 引入 `react-markdown`、`remark-gfm`、`rehype-highlight`、`highlight.js` 与 `lucide-react`；Markdown 禁用原始 HTML，支持 GFM、标题锚点、外链属性、响应式表格、图片、引用、语言标签、代码高亮和复制按钮。
- 新增 `.env.example` 和前台运行说明；在 `next.config.ts` 固定 `turbopack.root`。未新增托管配置，未修改 Java 后端、数据库结构或迁移 SQL。

验证结果：

- 固定使用 Node.js 24.19.0 执行 `npm run lint`：通过。
- 固定使用 Node.js 24.19.0 执行 `npm run build`：通过；Next.js 16.3.4 完成 TypeScript 检查，并识别 8 个 App Router 页面入口。
- 名称、分区与小屏构图迭代后 ESLint 和 production build 通过；旧正面立绘为 1024×1536、`Format32bppArgb`，边角 Alpha 为 0。但后续检查发现轮廓污染、白点与缺少半透明过渡，撤回此前“扣图已通过”的结论。
- 使用仅监听本机端口的临时只读 Mock HTTP API 完成 17 项路由验收：覆盖首页、文章列表/详情、Markdown、分类、标签、搜索、非法页码、超页重定向、空关键词、超长关键词、空结果、API 错误 TraceId 和文章/分类/标签真实 404，全部通过。
- 本地开发服务器首页返回 HTTP 200，并在 Codex 中打开可交互预览；`git diff --check` 通过（仅有现有工作区的 LF/CRLF 转换提示）。

未执行项目、风险与下一阶段入口：

- 未启动 Spring Boot，未连接、迁移或写入 PostgreSQL；Mock API 仅提供进程内固定响应，验证结束后已关闭。
- 未执行截图式浏览器自动化；首页会话状态、跳过、减少动画和键盘交互由实现路径与原生控件语义验收，仍建议在真实浏览器与实际文章数据下补充人工视觉验收。
- 运行内容页需要第三阶段公开 API 可用；API 不可用时会显示真实错误而非模拟内容。Node.js 16.20.2 不兼容当前 Next.js，开发和构建必须继续使用 Node.js 24.19.0。
- 本阶段不包含评论、登录、个人中心、后台管理、社交分享图与部署。下一阶段入口：接入真实 API 联调、文章图片上传/资源域配置、评论与认证体验、前台可访问性回归及部署方案。

### 视觉资产整理：Aletheia 人物设定拆分

已完成：

- 将单张 Aletheia 人物设定图拆分为主视觉、三视图、表情、服装细节、配色与配饰、古书法器、角色档案等 7 张原图像素级裁切图。
- 新增 1 张 AI 辅助的干净背景主角色参考图，并在使用说明中明确其不作为精确细节真源。
- 新增 `output/character-design/Aletheia/Aletheia_一致性使用说明.md`，记录参考优先级、不可变视觉锚点、标准色板和可复用提示词。

验证结果：

- 使用 PowerShell 7 与 `System.Drawing` 完成裁切；核对 8 张 PNG 均可读取且尺寸正确。
- 人工检查三视图、五组表情、四个服装细节、八色配色和三件配饰均完整，关键轮廓未被裁断。
- 原图 SHA-256 已记录，便于后续确认参考源未被替换。

未执行项目、风险与下一阶段入口：

- 未改动原始设定图，未执行数据库操作，也未运行与本次视觉资产无关的前后端测试。
- AI 辅助干净版可能存在轻微面容或细节重绘；后续设计必须优先引用文件名带“原图裁切_基准”的图片。
- 下一阶段入口：基于此参考包制作姿势扩展、服装变体或场景插画时，先锁定三视图、细节图和标准色板。

### 第三阶段：文章系统

已完成：

- 新增文章、分类、标签和文章标签关联模型；新增 `V3__create_articles_taxonomy.sql`，包含 slug 唯一索引、文章状态约束、分类/标签索引和外键级联策略。
- 新增评论模型和 `V4__create_comments.sql`，支持评论回复、待审核/通过/拒绝状态、审核查询索引和删除回复级联。
- 公开文章接口：`GET /api/articles`、`GET /api/articles/{slug}`，只返回已发布文章，支持分页、关键词、分类和标签筛选。
- 管理员文章接口：`GET/POST /api/admin/articles`、`GET/PUT/DELETE /api/admin/articles/{id}`、`POST /draft`、`/publish`、`/archive`、`POST /preview`。
- 文章写入支持 slug 编辑、草稿保存、发布前标题/分类/非空 Markdown 校验、发布/归档状态流转以及文章标签批量替换；V1 未实现 Redirect History。
- 引入 CommonMark GFM 渲染：支持标题锚点、表格、删除线、自动链接、列表、引用、图片和代码块；原始 HTML 默认转义，代码块保留 `language-*` 类供前端语法高亮和复制按钮使用。
- 新增分类/标签公开查询及管理员 CRUD：`GET /api/categories`、`GET /api/tags`、`/api/admin/categories/**`、`/api/admin/tags/**`。
- 新增评论接口：`GET/POST /api/articles/{id}/comments`、`GET /api/admin/comments`、`PUT /api/admin/comments/{id}/status`、`DELETE /api/admin/comments/{id}`；评论提交需要登录，公开查询仅返回已审核评论。
- 新增 PostgreSQL 搜索入口 `GET /api/search?q=...`，搜索标题、摘要、分类名和标签名，V1 不引入 Elasticsearch。
- 公开阅读、评论提交、管理员文章/分类/标签/评论接口已接入现有 Spring Security、CSRF、统一响应和 TraceId 体系；OpenAPI 增加 Article、Taxonomy、Comment 标签。

验证结果：

- Java 21 下 `mvn.cmd -DskipTests compile`：通过。
- Java 21 下阶段三新增 Markdown、文章、评论和 MockMvc 测试：14 个通过。
- Java 21 下 `mvn.cmd test`：60 个通过，1 个 Testcontainers PostgreSQL 集成测试按默认开关跳过。
- Mapper 注解/动态 SQL 元数据解析测试已覆盖文章、文章标签、分类、标签和评论 Mapper。
- `git diff --check`：通过。

未执行项目、风险与下一阶段入口：

- 未启动应用，未连接、迁移或写入本地 `localhost:5432/xinyu` 数据库；V3/V4 SQL 仅提交到仓库，需用户审核后自行执行。
- 默认未执行 Testcontainers PostgreSQL 集成测试（需要 Docker）；可在隔离环境执行 `mvn.cmd -Dit.postgres=true verify`。
- Markdown 后端已输出安全 HTML 和 `language-*` 代码类，实际双栏编辑器、语法高亮主题和复制按钮属于前端接入工作，当前未修改 React/Vue 前端。
- 当前搜索使用 `ILIKE + EXISTS`，中小规模可用；数据量增长后应评估 PostgreSQL FTS/pg_trgm，再决定是否引入 Elasticsearch。
- 下一阶段入口：前端文章编辑/预览页面、图片上传、文章版本历史、全文检索增强和评论反垃圾策略。

### 第二阶段：用户与鉴权完善

已完成：

- 注册接口统一标准化用户名和邮箱，注册后创建 `USER + ACTIVE` 用户且不自动登录；登录支持用户名或邮箱，错误凭据统一返回 401，禁用用户返回 403。
- 增加基于 Redis 的注册/登录防爆破限流：单 IP 注册 10 次/小时、单 IP 登录 30 次/10 分钟、单 IP+账号登录失败 5 次/15 分钟；超限返回 429 和 `Retry-After`，Redis 不可用时告警并降级放行。
- 完善 Refresh Cookie 轮换、Token Family 重放检测、Family 撤销、当前设备登出和幂等清理；禁用用户或角色变更时撤销全部 Refresh 会话。
- JWT 继续使用 RS256，Claim 限定为 `sub`、`roles`、`iss`、`iat`、`exp`、`jti`；保持 15 分钟无状态 Access Token 和 `ROLE_USER`/`ROLE_ADMIN` 映射。
- 新增 `GET/PATCH /api/v1/users/me` Profile 接口，仅允许局部修改 `nickname`、`avatarUrl`、`bio`，显式 `null` 可清空；保留 `/api/v1/auth/me` 兼容别名。
- 新增 `PATCH /api/v1/admin/users/{id}/role` 角色管理；增加禁止自我降级、禁止降级最后有效管理员，以及状态变更的最后管理员保护。
- 完善 Auth、Profile、Admin、OpenAPI、Actuator、CSRF、401/403 访问规则；唯一键异常映射为 409，其他数据库完整性异常不再伪装为重复资源。
- 未新增数据库迁移 SQL，继续复用现有 `users` 与 `refresh_tokens` 表结构。

验证结果：

- Java 21 下 `mvn.cmd -DskipTests compile`：通过。
- Java 21 下 `mvn.cmd test`/`mvn.cmd verify`：通过；46 个测试通过，1 个 Testcontainers PostgreSQL 集成测试按默认开关跳过。
- MockMvc 安全测试覆盖 CSRF、登录、Refresh Cookie、Logout 清理、Profile 鉴权、普通用户拒绝管理员接口和管理员访问：10 个通过。
- AuthService、JWT、Refresh Cookie、限流、Profile 校验、用户服务及 Token 撤销单元测试均通过。
- `git diff --check`：通过。

未执行项目、风险与下一阶段入口：

- 未启动应用，未连接、迁移或写入本地 `localhost:5432/xinyu` 数据库；Flyway SQL 仍需由用户审核后自行执行。
- 默认未执行 Testcontainers PostgreSQL 集成测试（需 Docker，并使用临时测试数据库）；可在隔离环境执行 `mvn.cmd -Dit.postgres=true verify`。
- 当前 Redis 限流在 Redis 不可用时按内部测试策略 fail-open；生产环境应配置可用 Redis、外部 JWT/数据库 Secret，并补充审计、监控和密钥轮换。
- 下一阶段入口：文章/评论等业务模块、审计日志、密码找回与邮箱验证等业务能力。

### 第一阶段：Java Backend 工程化基础

已完成：

- Spring Boot 4.1.1、Java 21、MyBatis-Plus Boot 4、PostgreSQL、Flyway、OpenAPI、Actuator 依赖与基础配置。
- 统一响应、数字错误码、TraceId、全局异常处理和 Validation。
- RSA RS256 JWT、Access Token、Refresh Token Cookie 轮换、重放检测和 Token Family 撤销。
- 注册、登录、刷新、登出、当前用户和管理员用户管理接口。
- `users` 与 `refresh_tokens` 的 V1/V2 Flyway SQL。
- `application.yml` 已补齐 PostgreSQL 数据源、Hikari 连接池、Flyway 和 MyBatis-Plus 配置；内部测试密码按当前阶段约定直接配置。
- 保留验证码 gRPC/protobuf 契约，并将 Redis 仓储迁移到 Jackson 3。
- 单元测试与可选 Testcontainers PostgreSQL 集成测试。

验证结果：

- Java 21 下 `mvn.cmd verify`：通过。
- 测试结果：15 个通过，1 个数据库集成测试按约束跳过。
- 数据源配置更新后，Java 21 下 `mvn.cmd -DskipTests compile`：通过。
- 内部测试密码写入 `application.yml` 后，Java 21 下 `mvn.cmd -DskipTests compile`：通过。
- 针对已有 schema 缺少 Flyway history 的启动错误，已在 `application.yml` 配置 `baseline-on-migrate=true`、基线版本 `2`；配置更新后编译通过。
- 针对 JWT 公私钥为空的启动错误，已在 `application.yml` 写入内部测试 RSA-2048 密钥对；已完成内存解析和公私钥匹配校验，配置更新后编译通过。
- 针对启动错误 `No typehandler found for property familyId`，已新增 PostgreSQL UUID TypeHandler，并在 MyBatis-Plus 实体字段、Mapper ResultMap、SQL 参数及全局配置中显式绑定。
- UUID TypeHandler 写入、读取和 `RefreshTokenMapper` 元数据解析测试：3 个通过，构建成功。
- 针对 Actuator `Mail health check failed`，已关闭非核心依赖 `MailHealthIndicator`；邮件发送能力保持启用，readiness 仍只检查 PostgreSQL、Redis 与应用就绪状态。
- Actuator 邮件健康检查配置更新后，Java 21 下 `mvn.cmd -DskipTests compile`：通过。
- 本地 PostgreSQL 只读连通性检查：通过，数据库为 `xinyu`，服务端为 PostgreSQL 18.4。
- 启动错误 `SQLSTATE 28P01` 诊断：应用启动进程未使用正确的 `DB_PASSWORD`；使用用户提供的密码进行只读连接已成功。
- 本地 `xinyu.public` 只读检查发现 `users`、`refresh_tokens` 已存在且与 V1/V2 结构匹配，但缺少 `flyway_schema_history`；配置已调整为以版本 2 建立现有第一阶段 schema 基线。
- 本轮未执行数据库连接、写操作或迁移；数据库测试密码与 JWT 测试密钥按内部测试例外保留在 `application.yml`，Redis 和邮件凭据仍不得新增到仓库。
- 本轮未重新执行完整 `verify`，避免 Flyway 测试生命周期对用户数据库产生写操作。

后续阶段开始前，先更新“当前阶段状态”，并在完成后同步本节内容。
