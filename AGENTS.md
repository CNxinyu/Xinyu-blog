# Xinyu-blog 项目协作约定

## 终端与范围

- 项目根目录：`C:\Users\87773\Desktop\Xinyu-blog`
- 终端统一使用 PowerShell 7：`C:\Program Files\PowerShell\7\pwsh.exe`
- 阶段性开发默认只修改当前阶段涉及的模块，并保留其他未提交改动。

## 数据库约束

- 数据库相关变更只提交 SQL，不直接执行建表、迁移、插入、更新或删除操作。
- 测试阶段可连接本机 PostgreSQL 做只读连通性检查；禁止对用户数据库执行 Flyway 迁移或其他写操作。
- 本地测试连接参数：主机 `localhost`，端口 `5432`，数据库 `xinyu`，用户 `postgres`。
- 密码由用户在测试环境临时提供，不写入仓库、脚本、日志或提交记录。
- 结构变更统一放在 `java-backend/src/main/resources/db/migration/`，由用户自行审核和执行。

## 阶段更新同步

每次阶段性工作完成后，必须同步更新本文件，至少包含：

1. 当前阶段与更新时间。
2. 已完成的模块、接口或迁移 SQL。
3. 已执行的验证命令及结果。
4. 未执行项目、环境依赖、风险和下一阶段入口。

## 当前阶段状态

更新时间：2026-09-09

### 第一阶段：Java Backend 工程化基础

已完成：

- Spring Boot 4.1.1、Java 21、MyBatis-Plus Boot 4、PostgreSQL、Flyway、OpenAPI、Actuator 依赖与基础配置。
- 统一响应、数字错误码、TraceId、全局异常处理和 Validation。
- RSA RS256 JWT、Access Token、Refresh Token Cookie 轮换、重放检测和 Token Family 撤销。
- 注册、登录、刷新、登出、当前用户和管理员用户管理接口。
- `users` 与 `refresh_tokens` 的 V1/V2 Flyway SQL。
- 保留验证码 gRPC/protobuf 契约，并将 Redis 仓储迁移到 Jackson 3。
- 单元测试与可选 Testcontainers PostgreSQL 集成测试。

验证结果：

- Java 21 下 `mvn.cmd verify`：通过。
- 测试结果：15 个通过，1 个数据库集成测试按约束跳过。
- 本地 PostgreSQL 只读连通性检查：通过，数据库为 `xinyu`，服务端为 PostgreSQL 18.4。
- 未执行数据库写操作或迁移；未提交 JWT、数据库、Redis 或邮件凭据。

后续阶段开始前，先更新“当前阶段状态”，并在完成后同步本节内容。
