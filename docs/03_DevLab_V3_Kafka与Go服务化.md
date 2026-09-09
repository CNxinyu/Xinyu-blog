# DevLab V3：Kafka + Go 服务化

> 目标：在 V1/V2 的单体系统基础上，引入事件驱动架构，并用 Go 实现一个真正合理的独立服务，而不是为了微服务而微服务。

---

# 1. 阶段目标

V3 开始引入：

```text
Kafka
Go
Kratos
gRPC（可选）
WebSocket / SSE
```

核心学习：

```text
Event Driven Architecture
Producer
Consumer
Consumer Group
Offset
Retry
Dead Letter
Idempotency
At-least-once
Outbox Pattern
服务拆分
Go 并发
WebSocket
可观测性
```

---

# 2. 为什么现在才引入 Go

V1/V2 已经有真实业务和流量路径。

此时 Go 不再是：

```text
再写一套 CRUD
```

而是解决一个明确问题：

```text
实时通知
异步任务
高并发连接
事件消费
```

---

# 3. 总体架构

```text
                         Next.js
                            │
                            ▼
                       Spring Boot
                            │
            ┌───────────────┼───────────────┐
            │               │               │
            ▼               ▼               ▼
       PostgreSQL         Redis            Kafka
                                             │
                                             ▼
                                      Go Notification
                                      Service / Kratos
                                             │
                                      WebSocket / SSE
                                             │
                                             ▼
                                           User
```

---

# 4. 首个 Go 服务

推荐：

```text
notification-service
```

职责：

- 消费 Kafka 事件
- 生成通知
- 推送在线用户
- 提供通知查询接口
- WebSocket / SSE
- 可选存储 notification 数据

---

# 5. 业务事件

第一批事件：

```text
ArticlePublished
CommentCreated
CommentApproved
CommentReplied
UserRegistered
ArticleLiked（未来）
```

事件例子：

```json
{
  "eventId": "uuid",
  "eventType": "CommentReplied",
  "occurredAt": "2026-09-08T12:00:00Z",
  "aggregateId": "comment-1001",
  "data": {
    "articleId": 101,
    "commentId": 1001,
    "recipientUserId": 88
  }
}
```

---

# 6. Event Envelope

统一事件结构：

```text
eventId
eventType
version
occurredAt
producer
aggregateId
traceId
data
```

好处：

- 版本管理
- Debug
- Trace
- 幂等
- 跨语言稳定

---

# 7. Topic 设计

不要每个事件一个 Topic。

可以先：

```text
devlab.article.events
devlab.comment.events
devlab.user.events
```

Key：

```text
aggregateId
```

用于同一个实体的事件顺序。

---

# 8. Spring Boot Producer

流程：

```text
业务事务
   │
   ├── Update PostgreSQL
   │
   └── Publish Kafka
```

这里存在经典问题：

```text
DB 成功
Kafka 失败
```

因此 V3 应该学习：

> Transactional Outbox Pattern

---

# 9. Outbox Pattern

数据库新增：

```text
outbox_events
```

字段：

```text
id
aggregate_type
aggregate_id
event_type
payload
status
created_at
published_at
```

业务事务：

```text
BEGIN
  update article
  insert outbox_event
COMMIT
```

后台 Publisher：

```text
Outbox
  ↓
Kafka
  ↓
Mark Published
```

这样保证：

```text
业务变化
+
事件记录
```

同一数据库事务完成。

---

# 10. Go Notification Service

目录建议：

```text
services/notification-go
├── cmd/
├── internal/
│   ├── biz/
│   ├── data/
│   ├── service/
│   ├── server/
│   └── conf/
├── api/
├── configs/
├── Dockerfile
└── go.mod
```

如果使用 Kratos：

```text
api
biz
data
service
server
```

正好练你已有知识。

---

# 11. 通知领域模型

```text
notification
├── id
├── user_id
├── type
├── title
├── content
├── link
├── read_at
└── created_at
```

类型：

```text
COMMENT_REPLY
COMMENT_APPROVED
SYSTEM
ARTICLE
```

---

# 12. WebSocket

流程：

```text
Browser
  │
  ├── HTTP
  │
  └── WebSocket
        │
        ▼
Go Notification Service
        │
        ▼
Connection Hub
```

重点练：

```text
goroutine
channel
connection registry
heartbeat
disconnect
broadcast
user routing
context cancellation
```

---

# 13. WebSocket 认证

客户端建立连接：

```text
wss://devlab.example.com/ws
```

需要认证。

方案可选：

- 短时 WebSocket Ticket
- Access Token
- Cookie

推荐：

```text
Spring Boot
POST /api/ws-ticket
  ↓
生成短时 ticket
  ↓
WebSocket 使用 ticket 建连
```

避免长期 Token 出现在 URL。

---

# 14. Nginx WebSocket

增加：

```text
/ws
→ Go Notification Service
```

需要支持：

```text
Upgrade
Connection
```

---

# 15. 消费者幂等

Kafka 常见语义：

```text
At Least Once
```

可能重复消费。

因此：

```text
eventId
```

必须用于幂等。

可以建立：

```text
processed_events
```

或者 Redis：

```text
devlab:event:processed:{eventId}
```

但重要通知建议数据库保证。

---

# 16. Retry

消费者失败：

```text
consume
  ↓
retry
  ↓
retry
  ↓
DLQ
```

例如：

```text
devlab.comment.events
devlab.comment.events.retry
devlab.comment.events.dlq
```

V3 要能人工查看 DLQ。

---

# 17. Poison Message

对无法解析的数据：

- 不无限重试
- 记录错误
- 进入 DLQ
- 保留 eventId
- 保留 payload
- 支持人工重放

---

# 18. 消息版本

事件：

```text
CommentReplied v1
```

未来修改结构时：

```text
version: 2
```

消费者要考虑兼容。

不要直接任意修改 payload。

---

# 19. Go Worker Pool

另一个适合 Go 的实验：

```text
media-worker
```

可以作为可选子模块：

```text
ImageUploaded
   ↓
Kafka
   ↓
Go Worker
   ↓
压缩
缩略图
WebP
Metadata
```

重点：

```text
worker pool
bounded concurrency
backpressure
context timeout
retry
```

V3 主线仍然以 notification-service 为准。

---

# 20. API

Go 服务可提供：

```text
GET  /api/notifications
GET  /api/notifications/unread-count
POST /api/notifications/{id}/read
POST /api/notifications/read-all
```

可以由 Nginx：

```text
/api/notifications/*
→ Go Service
```

或者 Spring Boot BFF 转发。

V3 推荐先直接通过 Gateway/Nginx 路由。

---

# 21. 数据库选择

两种方案：

## 方案 A

Go 服务共用 PostgreSQL Server，但独立 Schema：

```text
devlab_core
devlab_notification
```

## 方案 B

独立数据库。

个人项目 V3 推荐：

```text
同一 PostgreSQL 实例
不同 schema/database
```

降低运维复杂度。

---

# 22. gRPC

V3 不要求必须上。

如果你想巩固 Go + Kratos：

可以让：

```text
Spring Boot
→ gRPC
→ Go Notification Service
```

但通知主链路已经通过 Kafka 解耦，所以 gRPC 可用于：

```text
查询未读数
管理命令
内部同步调用
```

不要为了 gRPC 强行制造 RPC。

---

# 23. Docker Compose

新增：

```text
kafka
notification-go
```

最终：

```text
frontend
backend
postgres
redis
kafka
notification-go
```

Kafka 可以使用 KRaft 模式，个人项目无需 ZooKeeper。

---

# 24. 本地开发顺序

## Step 1

Docker 启动 Kafka。

## Step 2

Spring Boot 写 Producer。

## Step 3

设计 Event Envelope。

## Step 4

实现 Outbox。

## Step 5

创建 Go notification-service。

## Step 6

消费 Kafka。

## Step 7

落库 notification。

## Step 8

实现 REST 查询。

## Step 9

实现 WebSocket。

## Step 10

前端通知中心。

## Step 11

消费者幂等。

## Step 12

Retry/DLQ。

## Step 13

测试。

## Step 14

生产部署。

---

# 25. 前端改造

增加：

```text
通知铃铛
未读数
通知列表
实时消息
连接状态
```

示例：

```text
🔔 3
```

点击：

```text
Comment Reply
Article Published
System Message
```

---

# 26. 测试

## Producer

测试：

```text
文章发布
→ Outbox Created
```

---

## Publisher

测试：

```text
Outbox
→ Kafka
→ Published
```

---

## Consumer

测试：

```text
Kafka Event
→ Notification Created
```

---

## Idempotency

同一个 eventId 投递两次：

```text
只生成一条 notification
```

---

## WebSocket

测试：

```text
user online
event generated
message pushed
```

---

## DLQ

模拟非法消息：

```text
retry
→ dlq
```

---

# 27. 事件追踪

Event Envelope 加：

```text
traceId
```

日志：

```text
Spring Boot
traceId=xxx

Kafka
traceId=xxx

Go
traceId=xxx
```

为后续 OpenTelemetry 打基础。

---

# 28. 生产运维

关注：

```text
Kafka Disk
Consumer Lag
Go Goroutine
WebSocket Connections
Consumer Error
DLQ Size
```

V3 可以先通过日志 + 基础指标观察。

---

# 29. 故障演练

必须做：

### Kafka 停止

观察：

```text
业务数据库是否还能正常提交？
Outbox 是否保留？
Kafka 恢复后是否继续发布？
```

### Go Service 停止

观察：

```text
Kafka 是否保留消息？
Consumer 恢复后是否继续消费？
```

### 重复投递

验证幂等。

---

# 30. Definition of Done

- [ ] Kafka 已部署
- [ ] Event Envelope 统一
- [ ] Spring Boot 可发布事件
- [ ] 使用 Outbox Pattern
- [ ] Go Notification Service 可运行
- [ ] Go 消费 Kafka
- [ ] 通知落库
- [ ] REST 查询可用
- [ ] WebSocket/SSE 实时推送可用
- [ ] 前端显示未读通知
- [ ] 消费幂等实现
- [ ] Retry 实现
- [ ] DLQ 实现
- [ ] 至少一次 Kafka 故障恢复演练
- [ ] 至少一次 Go Service 故障恢复演练
- [ ] Docker Compose 一键启动
- [ ] 生产环境部署成功

---

# 31. 常见风险

## 过早拆太多服务

V3 只拆：

```text
notification-service
```

最多再加一个：

```text
media-worker
```

不要一下拆：

```text
user-service
article-service
comment-service
auth-service
```

---

## Kafka 替代所有调用

错误。

Kafka 适合：

```text
事件
异步
解耦
```

不适合所有同步查询。

---

## 没有幂等

Kafka 消费者必须默认考虑重复消息。

---

## 没有 Outbox

如果业务状态和消息必须对应，要考虑 DB/Kafka 双写一致性。

---

# 32. 进入 V4 的条件

你需要能够解释：

```text
Kafka 为什么不是 Redis Stream？
为什么使用 Outbox？
At-least-once 是什么？
为什么消费者必须幂等？
Consumer Group 怎么工作？
为什么 Go 适合 WebSocket/事件消费？
```

然后进入：

```text
Spring AI + pgvector + RAG
```

V4 将把 DevLab 从“工程平台”升级成真正的 AI 技术博客。
