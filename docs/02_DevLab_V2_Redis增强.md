# DevLab V2：Redis 增强阶段

> 目标：在已经上线的 V1 技术博客基础上，引入 Redis，并用真实业务功能学习缓存、计数、限流、排行榜、分布式锁与数据结构。

---

# 1. 阶段目标

V2 不做“Redis Demo”。

目标是把 Redis 放入真实业务链路：

```text
Next.js
   │
Spring Boot
   │
   ├──────── PostgreSQL
   │
   └──────── Redis
```

重点掌握：

```text
Cache
TTL
Cache Aside
热点数据
排行榜
Rate Limit
Session / Token
Bitmap
HyperLogLog
ZSet
Stream
分布式锁
缓存一致性
缓存穿透
缓存击穿
缓存雪崩
```

---

# 2. 本阶段新增技术

```text
Redis
Spring Data Redis
Lettuce
Testcontainers Redis
```

可选：

```text
Redisson
```

---

# 3. 本阶段不做

- Kafka
- RabbitMQ
- Go 服务
- Spring AI
- Python
- Kubernetes

---

# 4. Redis 使用原则

不要因为“Redis 快”就把所有数据放 Redis。

数据职责：

```text
PostgreSQL
→ Source of Truth

Redis
→ Cache / Counter / Temporary State
```

业务永久数据必须以 PostgreSQL 为准。

---

# 5. Redis Key 规范

统一格式：

```text
devlab:{module}:{resource}:{id}
```

例如：

```text
devlab:article:detail:1001
devlab:article:view:1001
devlab:rank:article:daily
devlab:auth:ratelimit:192.168.1.1
```

要求：

- 所有 key 有命名规范
- 缓存 key 默认设置 TTL
- 禁止到处硬编码 key
- 建立 RedisKeyBuilder

---

# 6. 功能 1：文章详情缓存

请求：

```text
GET /api/articles/{slug}
```

改造：

```text
Client
  ↓
Spring Boot
  ↓
Redis
  │
  ├─ Hit → 返回
  │
  └─ Miss
        ↓
    PostgreSQL
        ↓
    Write Cache
        ↓
      返回
```

模式：

```text
Cache Aside
```

---

## 6.1 缓存 Key

```text
devlab:article:detail:{articleId}
```

TTL：

```text
10~30 分钟
```

可以加入随机抖动：

```text
baseTTL + random
```

降低缓存雪崩风险。

---

## 6.2 更新策略

更新文章后：

```text
DB UPDATE
   ↓
DELETE CACHE
```

不要在 V2 一开始追求极端复杂强一致。

---

# 7. 功能 2：热门文章排行榜

使用：

```text
Sorted Set
```

Key：

```text
devlab:rank:article:daily
devlab:rank:article:weekly
devlab:rank:article:all
```

写入：

```text
ZINCRBY
```

查询：

```text
ZREVRANGE
```

前端增加：

```text
热门文章
本周热门
今日热门
```

---

# 8. 功能 3：文章访问量

不要每次浏览都直接：

```text
UPDATE articles SET view_count = view_count + 1
```

可以：

```text
浏览文章
  ↓
Redis INCR
  ↓
批量同步 PostgreSQL
```

例如：

```text
devlab:article:view:{id}
```

定时任务：

```text
每 1~5 分钟
```

执行：

```text
Redis Counter
   ↓
Batch Update
   ↓
PostgreSQL
```

---

# 9. 功能 4：UV 统计

## HyperLogLog

用于近似 UV：

```text
devlab:uv:site:2026-09-08
```

操作：

```text
PFADD
PFCOUNT
```

学习：

- HyperLogLog
- 近似统计
- 内存占用优势

---

# 10. 功能 5：每日签到 Playground

虽然博客不一定真正需要签到，但这是 DevLab，可以增加：

```text
/dev/playground/redis/checkin
```

使用：

```text
Bitmap
```

Key：

```text
devlab:checkin:{userId}:2026-09
```

功能：

```text
今日签到
当月签到天数
连续签到
签到日历
```

目的：

> 用真实 UI 学 Redis Bitmap。

---

# 11. 功能 6：限流

适合：

```text
登录
注册
评论
搜索
上传
AI（未来）
```

例如：

```text
POST /api/auth/login
```

规则：

```text
同一 IP
60 秒最多 10 次
```

方案：

```text
Redis Counter + TTL
```

后续可以实现：

```text
Sliding Window
Token Bucket
Lua Script
```

---

# 12. 功能 7：验证码/临时状态

如果后续加入邮箱验证，可以使用：

```text
devlab:verify:email:{email}
```

TTL：

```text
5~10 分钟
```

练习：

```text
Temporary State
TTL
One-time Token
```

---

# 13. 功能 8：分布式锁

V2 可以挑一个适合的场景：

例如：

```text
定时同步访问量
```

部署多个 Backend Instance 时：

```text
Instance A
Instance B
Instance C
```

只允许一个执行：

```text
syncArticleViewCount()
```

使用：

```text
Redis Distributed Lock
```

推荐后期用：

```text
Redisson
```

不要自己随意拼：

```text
SETNX + DEL
```

而忽略 token、安全释放、续约等问题。

---

# 14. 功能 9：Redis Stream Playground

为了学习 Stream，加入一个实验模块：

```text
/dev/playground/redis/stream
```

场景：

```text
ArticleViewedEvent
```

生产：

```text
XADD
```

消费：

```text
Consumer Group
XREADGROUP
```

重点学习：

```text
Stream
Consumer Group
Pending Entries
ACK
Retry
```

注意：

> V2 只把 Redis Stream 当学习与小规模事件实验，不代替下一阶段 Kafka。

---

# 15. 缓存异常专题

V2 必须做实验。

---

## 15.1 缓存穿透

场景：

```text
持续请求不存在的 articleId
```

解决：

- 缓存空值
- 参数校验
- 可选 Bloom Filter

---

## 15.2 缓存击穿

场景：

```text
超级热门文章 Cache 刚过期
大量并发同时查询数据库
```

实验方案：

- Mutex
- Logical Expire
- 热点永不过期 + 异步刷新

---

## 15.3 缓存雪崩

大量 key：

```text
同一时间过期
```

解决：

```text
随机 TTL
分批预热
降级
```

---

# 16. Redis 配置

开发：

```text
localhost Redis
```

测试：

```text
Testcontainers Redis
```

生产：

```text
Redis Container
```

生产 Compose：

```text
frontend
backend
postgres
redis
```

---

# 17. Spring Boot 结构

增加：

```text
common/redis
├── RedisKeyBuilder
├── RedisCacheService
├── RedisLockService
└── RedisRateLimitService
```

但领域相关逻辑仍放：

```text
article
auth
analytics
```

---

# 18. 新增模块

推荐新增：

```text
analytics
ranking
playground
```

---

# 19. 新增 API

示例：

```text
GET /api/articles/popular
GET /api/analytics/site
GET /api/analytics/articles/{id}

POST /api/playground/checkin
GET  /api/playground/checkin
```

---

# 20. 测试

必须覆盖：

### Cache

```text
第一次查询 → DB
第二次查询 → Redis
修改文章 → Cache Evict
```

### Rate Limit

```text
允许范围内 → 200
超过阈值 → 429
```

### Ranking

```text
文章访问增加
排名正确
```

### Distributed Lock

模拟多个实例竞争。

---

# 21. 监控与可观察性

V2 先记录：

```text
cache hit
cache miss
cache evict
redis error
rate limit rejected
```

可以通过 Actuator/Micrometer 暴露自定义指标，为后续 Grafana 做准备。

---

# 22. Redis 故障降级

必须考虑：

```text
Redis Down
```

文章详情：

```text
Redis Failure
   ↓
Fallback PostgreSQL
```

不能因为 Redis 挂了导致整个博客不可用。

对于：

```text
排行榜
UV
```

可以暂时降级。

---

# 23. 部署步骤

## Step 1

Docker Compose 加入：

```text
redis
```

## Step 2

增加环境变量：

```text
REDIS_HOST
REDIS_PORT
REDIS_PASSWORD
```

## Step 3

后端接入 Redis。

## Step 4

部署测试环境。

## Step 5

压测文章详情。

## Step 6

启用生产缓存。

## Step 7

观察 Redis 内存和命中率。

---

# 24. 数据安全

Redis 不作为永久数据源。

生产建议：

- 不开放公网端口
- Docker 内部网络
- 设置密码
- 根据需要启用 AOF
- 对临时缓存与重要 Redis 数据做不同持久化决策

---

# 25. 推荐开发顺序

```text
1. Redis Container
2. Spring Data Redis
3. Key Convention
4. Article Cache
5. Cache Eviction
6. Hot Article Ranking
7. View Counter
8. HyperLogLog UV
9. Rate Limit
10. Bitmap Playground
11. Distributed Lock
12. Redis Stream Playground
13. Failure Fallback
14. Test
15. Deploy
16. Load Test
```

---

# 26. Definition of Done

- [ ] Redis 已加入 Docker Compose
- [ ] 文章详情实现 Cache Aside
- [ ] 修改文章会正确失效缓存
- [ ] 缓存 TTL 有随机抖动
- [ ] 热门文章使用 ZSet
- [ ] 浏览量使用 Redis Counter
- [ ] 浏览量可同步回 PostgreSQL
- [ ] UV 使用 HyperLogLog
- [ ] 至少一个 Bitmap 实验功能
- [ ] 登录或评论实现 Redis 限流
- [ ] 至少一个分布式锁场景
- [ ] Redis Stream Playground 可运行
- [ ] Redis 宕机时核心文章服务可降级
- [ ] 集成测试覆盖主要 Redis 行为
- [ ] 生产部署成功
- [ ] 完成简单缓存性能对比

---

# 27. 推荐实验报告

建议在博客里自己写一篇：

```text
《DevLab Redis 实战：从 PostgreSQL 到 Cache Aside》
```

记录：

```text
无缓存延迟
有缓存延迟
缓存命中率
QPS
Redis 内存
PostgreSQL 压力
```

这样项目本身又反过来产生博客内容。

---

# 28. 进入 V3 的条件

当你已经能回答：

```text
Redis 为什么存在？
什么数据适合缓存？
什么数据不适合？
Cache Aside 的一致性风险是什么？
排行榜为什么用 ZSet？
UV 为什么用 HyperLogLog？
Redis 挂了系统怎么办？
```

再进入：

```text
Kafka + Go
```

V3 的重点将从“缓存与临时状态”升级到：

```text
事件驱动
异步解耦
服务拆分
Go 并发
WebSocket
```
