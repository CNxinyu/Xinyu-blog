# DevLab V5：Python Agent + 完整 AI 全栈架构

> 目标：在 Spring Boot 企业业务后端、Go 事件服务、Kafka、Redis、pgvector、Spring AI 的基础上，引入 Python/FastAPI/LangGraph，将 DevLab 升级为完整 AI Agent 实验平台。

---

# 1. 阶段目标

V5 的目标不是：

```text
把 Spring AI 重写成 Python
```

而是明确分工：

```text
Spring Boot
→ 核心业务 / 权限 / 事务 / 企业服务

Go
→ 高并发 / WebSocket / Worker / Event Consumer

Python
→ Agent / RAG Experiment / Evaluation / Model Pipeline
```

最终：

```text
                    Next.js
                       │
                       ▼
                  API Gateway
                       │
          ┌────────────┼────────────┐
          │            │            │
          ▼            ▼            ▼
     Spring Boot    Python AI      Go Services
     Spring AI      FastAPI        Kratos
          │         LangGraph          │
          │            │               │
          └────────────┼───────────────┘
                       │
            ┌──────────┼──────────┐
            ▼          ▼          ▼
        PostgreSQL    Redis      Kafka
          pgvector
```

---

# 2. 新增技术

```text
Python
FastAPI
Pydantic
LangGraph
MCP
OpenAI/Anthropic/Gemini SDK
Evaluation Tools
Optional Reranker
```

可选：

```text
LlamaIndex
DSPy
Transformers
PyTorch
```

但不要一次全上。

---

# 3. Python 服务定位

创建：

```text
services/ai-python
```

职责：

```text
Agent Runtime
复杂 RAG
Query Rewrite
Multi-step Retrieval
Rerank
Evaluation
Document Processing
OCR（后续）
Model Experiment
```

---

# 4. 目录结构

```text
services/ai-python/
├── app/
│   ├── api/
│   ├── agents/
│   ├── tools/
│   ├── rag/
│   ├── models/
│   ├── evaluation/
│   ├── integrations/
│   ├── config/
│   └── main.py
│
├── tests/
├── pyproject.toml
├── Dockerfile
└── README.md
```

---

# 5. FastAPI API

示例：

```text
POST /ai/agent/run
POST /ai/agent/stream
POST /ai/evaluate
POST /ai/rag/query
GET  /health
```

内部接口建议独立：

```text
/internal/*
```

---

# 6. Agent 与普通 Chat 的区别

普通：

```text
Question
  ↓
LLM
  ↓
Answer
```

Agent：

```text
Goal
 ↓
Reason
 ↓
Select Tool
 ↓
Execute
 ↓
Observe
 ↓
Decide
 ↓
Next Tool
 ↓
Answer
```

必须设置：

```text
max_steps
timeout
token_budget
tool_allowlist
```

禁止无限循环。

---

# 7. 第一个真正的 Agent

推荐：

> DevLab Content Analyst Agent

用户：

```text
找出过去半年访问量低于 100 的 Go 文章，
分析原因，并给出 SEO 和内容优化建议。
```

Agent：

```text
1. 查询文章
2. 查询 Analytics
3. 读取文章正文
4. 检索相关旧文章
5. 分析
6. 生成建议
```

---

# 8. Tools

建议建立：

```text
SearchArticlesTool
GetArticleTool
GetArticleAnalyticsTool
GetTagStatsTool
GetCategoryStatsTool
GetPopularArticlesTool
SuggestArticleOptimizationTool
```

写操作 Tools：

```text
CreateDraftTool
UpdateDraftTool
```

必须经过 Human Approval。

---

# 9. Tool Ownership

重要原则：

```text
Python Agent
不能直接绕过核心业务数据库规则
```

推荐：

```text
Python
 ↓
Spring Boot Internal API / MCP
 ↓
Business Service
 ↓
Database
```

而不是：

```text
Python
 ↓
直接 UPDATE core tables
```

---

# 10. MCP

DevLab 可以建立：

```text
DevLab MCP Server
```

暴露：

```text
search_articles
get_article
get_article_stats
get_popular_articles
create_draft
```

这样：

```text
Python Agent
Claude
ChatGPT
其他 Agent
```

都可以使用统一工具协议。

---

# 11. MCP 权限

必须区分：

```text
read tools
write tools
admin tools
```

例如：

```text
search_articles
→ READ

create_draft
→ WRITE + ADMIN + APPROVAL
```

---

# 12. Human in the Loop

高风险动作：

```text
发布文章
删除文章
批量修改
修改用户权限
```

Agent 不能直接执行。

流程：

```text
Agent Proposed Action
      ↓
Pending Approval
      ↓
Admin Confirm
      ↓
Execute
```

---

# 13. Agent State

LangGraph 中保存：

```text
conversation_id
user_id
goal
messages
tool_calls
tool_results
current_step
status
created_at
updated_at
```

状态：

```text
RUNNING
WAITING_APPROVAL
COMPLETED
FAILED
CANCELLED
```

---

# 14. 长任务

例如：

```text
分析全部 500 篇文章
```

不能同步 HTTP 一直等。

流程：

```text
POST /agent/jobs
   ↓
job_id
   ↓
Kafka / Task Queue
   ↓
Python Worker
   ↓
Progress Event
   ↓
Go WebSocket
   ↓
Frontend
```

这时前面所有阶段开始真正组合。

---

# 15. Agent Event

事件：

```text
AgentStarted
AgentStepStarted
AgentToolCalled
AgentToolCompleted
AgentWaitingApproval
AgentCompleted
AgentFailed
```

通过 Kafka：

```text
Python
 ↓
Kafka
 ↓
Go Notification
 ↓
WebSocket
 ↓
Next.js
```

---

# 16. 前端 Agent UI

新增：

```text
/ai/agent
```

UI 显示：

```text
Goal
Status
Steps
Tool Calls
Sources
Token Usage
Elapsed Time
Approval Request
Final Answer
```

不要只显示一个“正在思考”。

---

# 17. Streaming

Python：

```text
FastAPI SSE
```

或者事件：

```text
Kafka + WebSocket
```

短任务：

```text
SSE
```

长任务：

```text
Kafka + WebSocket
```

---

# 18. RAG 升级

V4：

```text
Query
→ Vector Search
→ LLM
```

V5 可以实验：

```text
Query Rewrite
   ↓
Hybrid Search
   ↓
Vector + Keyword
   ↓
Rerank
   ↓
Context Compression
   ↓
LLM
```

---

# 19. Hybrid Search

结合：

```text
PostgreSQL Full Text
+
pgvector
```

评分：

```text
keyword_score
vector_score
```

然后融合。

---

# 20. Rerank

加入 reranker 后：

```text
retrieve 20
  ↓
rerank
  ↓
top 5
```

对比：

```text
Recall
Precision
Latency
Cost
```

---

# 21. Evaluation 平台

V5 不只是写测试脚本。

建立：

```text
/admin/ai/evaluation
```

支持：

```text
Dataset
Experiment
Prompt Version
Model
Retriever Config
Result
```

---

# 22. Experiment

例如：

```text
Experiment A
embedding=model-A
topK=5

Experiment B
embedding=model-A
topK=20
rerank=true
```

比较：

```text
Source Recall
Answer Score
Latency
Token
Cost
```

---

# 23. Prompt Registry

建立：

```text
prompt_templates
prompt_versions
```

字段：

```text
name
version
template
created_at
created_by
status
```

Agent/RAG 使用指定版本。

---

# 24. Model Gateway

随着模型越来越多，建议抽象：

```text
ModelGateway
```

统一：

```text
chat
embedding
stream
usage
retry
fallback
```

provider：

```text
OpenAI
Anthropic
Gemini
Local
```

---

# 25. Fallback

例如：

```text
Primary Model
   ↓ fail
Secondary Model
```

但要控制：

```text
retry storm
cost explosion
```

---

# 26. AI Gateway 限流

Redis：

```text
per user
per model
per endpoint
```

统计：

```text
requests/min
tokens/day
cost/day
```

可以后台配置配额。

---

# 27. AI 成本 Dashboard

展示：

```text
Today Requests
Input Tokens
Output Tokens
Estimated Cost
Avg Latency
Error Rate
Top Users
Top Models
```

---

# 28. Agent Trace

每次 Agent Run：

```text
trace_id
run_id
conversation_id
```

贯穿：

```text
Next.js
→ Spring Boot
→ Python
→ MCP
→ Kafka
→ Go
```

为 OpenTelemetry 做准备。

---

# 29. OpenTelemetry

V5 可以正式加入：

```text
OpenTelemetry
```

收集：

```text
Trace
Metric
Log Correlation
```

后续可接：

```text
Prometheus
Grafana
Tempo
Loki
```

---

# 30. 可观察性架构

```text
Spring Boot ─┐
Go           ├── OpenTelemetry
Python       ┘
                │
                ▼
        Collector / Backend
                │
        ┌───────┼───────┐
        ▼       ▼       ▼
     Metrics  Traces   Logs
```

---

# 31. Python 与 Kafka

Python 服务也成为 Kafka Consumer/Producer。

消费：

```text
ArticlePublished
ArticleUpdated
AgentJobCreated
```

生产：

```text
AgentStarted
AgentProgress
AgentCompleted
AIIndexCompleted
```

---

# 32. Job 模型

数据库：

```text
ai_jobs
```

字段：

```text
id
user_id
type
status
progress
input
output
error
created_at
started_at
completed_at
```

---

# 33. Retry Strategy

AI Job：

```text
transient error
→ retry

business error
→ fail

approval required
→ waiting
```

不要统一所有错误都无限重试。

---

# 34. Timeout

每层设置：

```text
HTTP timeout
LLM timeout
Tool timeout
Agent max duration
Kafka processing timeout
```

---

# 35. Security

Python 服务原则上不公开公网。

架构：

```text
Internet
  ↓
Nginx / Gateway
  ↓
Spring Boot
  ↓
Python Internal Service
```

或只公开受控 Agent API。

---

# 36. Service-to-Service Auth

内部服务不能完全裸奔。

可选：

```text
Internal JWT
mTLS（后期）
API Key（早期）
```

个人项目推荐：

```text
Internal JWT / Shared Secret
```

后续再实验 mTLS。

---

# 37. Secrets

统一从环境变量或 Secret 管理系统读取：

```text
AI_API_KEY
DB_PASSWORD
JWT_SECRET
INTERNAL_SERVICE_SECRET
```

不要写入：

```text
Dockerfile
Git
application.yml
```

---

# 38. 容器架构

V5 Compose：

```text
frontend
spring-backend
postgres
redis
kafka
notification-go
ai-python
nginx
```

可选：

```text
otel-collector
prometheus
grafana
```

---

# 39. Kubernetes 是否现在引入

只有当：

- Compose 已经很难管理
- 想系统学习 K8s
- 需要多节点/滚动发布/自动恢复实验

再进入：

```text
V5.5 / V6
```

不要因为“最终架构很大”就自动上 Kubernetes。

---

# 40. API Gateway

V5 可以评估：

```text
Nginx
vs
Spring Cloud Gateway
vs
Go Gateway
```

如果目标是巩固 Go：

可以进一步实现：

```text
Go API Gateway
```

负责：

```text
Auth Verify
Rate Limit
Routing
Tracing
Request ID
Streaming Proxy
```

但这是增强项，不是 V5 必做。

---

# 41. DevLab Playground

到 V5 后，Playground 可以扩展：

```text
/dev/playground
├── sql
├── redis
├── kafka
├── websocket
├── vector-search
├── llm
├── rag
├── agent
└── mcp
```

DevLab 正式成为长期技术实验平台。

---

# 42. 开发顺序

## Step 1：Python 服务

完成：

```text
FastAPI
Pydantic
Config
Health
Docker
```

## Step 2：Spring Boot ↔ Python

建立内部 API 与认证。

## Step 3：LangGraph

做最小 Agent。

## Step 4：Tools

先实现只读 Tools。

## Step 5：MCP

将核心查询能力暴露为 MCP。

## Step 6：Agent Trace

保存 run/step/tool 数据。

## Step 7：Agent UI

显示实时步骤与结果。

## Step 8：Human Approval

为写操作加入审批。

## Step 9：Kafka Job

将长任务异步化。

## Step 10：Go 实时推送

把 Agent Progress 推给前端。

## Step 11：Hybrid Search

升级 RAG。

## Step 12：Rerank

加入可选 reranker。

## Step 13：Evaluation

建立实验与数据集。

## Step 14：Cost Dashboard

记录 Token、费用和延迟。

## Step 15：OpenTelemetry

打通跨服务 Trace。

## Step 16：Production

生产部署和故障演练。

---

# 43. 测试

## Python Unit

测试：

```text
Tool
RAG
State Transition
Prompt Builder
```

## Integration

```text
Python → Spring Boot
Python → pgvector
Python → Kafka
```

## Agent

测试：

```text
max steps
timeout
tool error
approval
retry
```

## Security

测试：

```text
User cannot use admin tool
Agent cannot bypass service authorization
```

---

# 44. 故障演练

必须覆盖：

### LLM Provider Down

```text
fallback / clear error
```

### Python Service Down

Spring Boot 核心博客仍可运行。

### Kafka Down

Agent Job 状态不会丢失。

### Go Down

实时推送暂时不可用，但 Job 不丢。

### Redis Down

AI 限流/缓存降级。

### pgvector 查询慢

观察 timeout 和 fallback。

---

# 45. Definition of Done

- [ ] FastAPI 服务独立运行
- [ ] Spring Boot 与 Python 可安全通信
- [ ] 至少一个 LangGraph Agent
- [ ] 至少 5 个真实 Tools
- [ ] Tools 不绕过核心业务权限
- [ ] MCP Server 可用
- [ ] Agent 有 max_steps
- [ ] Agent 有 timeout
- [ ] Agent Run 状态持久化
- [ ] Agent UI 可展示步骤
- [ ] Agent 支持 Streaming
- [ ] 写操作支持 Human Approval
- [ ] 长任务通过 Kafka 异步执行
- [ ] Go 可实时推送 Agent Progress
- [ ] Hybrid Search 可用
- [ ] Rerank 至少完成一次实验
- [ ] Evaluation Dataset 可运行
- [ ] Prompt 有版本管理
- [ ] Token/成本/延迟有 Dashboard
- [ ] 跨服务 trace_id 打通
- [ ] OpenTelemetry 基础接入
- [ ] LLM Provider 故障有处理策略
- [ ] Python 故障不影响博客核心功能
- [ ] 生产部署成功

---

# 46. 最终能力地图

完成 V1~V5 后，DevLab 覆盖：

```text
Frontend
├── TypeScript
├── React
├── Next.js
├── SSR
├── SEO
├── Streaming
└── WebSocket

Backend
├── Java
├── Spring Boot
├── Security
├── JPA
├── REST
├── Transaction
└── Spring AI

Data
├── PostgreSQL
├── Redis
├── pgvector
└── Flyway

Distributed
├── Kafka
├── Event Driven
├── Outbox
├── Retry
├── DLQ
└── Idempotency

Go
├── Kratos
├── Goroutine
├── Channel
├── WebSocket
├── Worker
└── Consumer

AI
├── LLM
├── Embedding
├── RAG
├── Tool Calling
├── Agent
├── MCP
├── Evaluation
├── Rerank
└── Human in the Loop

DevOps
├── Docker
├── Compose
├── Linux
├── Nginx
├── HTTPS
├── CI/CD
├── Backup
└── OpenTelemetry
```

---

# 47. 最终项目定位

完成 V5 后，它已经不是“博客项目”。

它是：

> DevLab — Developer Knowledge & AI Engineering Platform

可以长期用来实验：

```text
新数据库
新缓存方案
新消息队列
新 AI 模型
新 Agent Framework
新 MCP Tool
新 Go Service
新 Observability Stack
新部署方式
```

未来学习任何新技术，不再新建一个孤立 Demo，而是优先考虑：

```text
这个技术能解决 DevLab 的什么真实问题？
```

这才是整个五阶段路线最重要的价值。
