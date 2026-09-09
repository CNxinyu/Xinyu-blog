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

更新时间：2026-09-09

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
