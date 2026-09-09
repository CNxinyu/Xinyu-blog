# DevLab V4：Spring AI + pgvector + RAG

> 目标：让 DevLab 正式具备 AI 能力。重点不是简单调用 Chat API，而是基于自己的技术文章构建可引用来源、可评估、可观察的 RAG 系统。

---

# 1. 阶段目标

V4 新增：

```text
Spring AI
LLM
Embedding
pgvector
RAG
Tool Calling
AI Streaming
AI Summary
AI Tagging
Semantic Search
Evaluation
```

最终用户可以：

```text
Ask DevLab
```

例如：

```text
“我写过哪些关于 Go Context 的文章？”
“总结博客中 Redis 缓存一致性的主要观点。”
“比较我关于 Spring Boot 和 Go 的后端文章。”
```

AI 返回：

```text
答案
+
引用文章
+
引用片段
```

---

# 2. 总体架构

```text
                         Next.js
                            │
                            ▼
                       Spring Boot
                            │
             ┌──────────────┼──────────────┐
             │              │              │
             ▼              ▼              ▼
        PostgreSQL        Redis           Kafka
             │
          pgvector
             │
             ▼
         Spring AI
             │
    ┌────────┼─────────┐
    │        │         │
    ▼        ▼         ▼
  LLM    Embedding   RAG
```

---

# 3. 本阶段新增模块

```text
ai
knowledge
embedding
rag
evaluation
```

建议：

```text
com.devlab.ai
├── chat
├── embedding
├── rag
├── tool
├── evaluation
└── config
```

---

# 4. pgvector

PostgreSQL 增加：

```text
vector extension
```

建立文档 Chunk 表：

```text
article_chunks
```

字段：

```text
id
article_id
chunk_index
content
content_hash
embedding
metadata
created_at
updated_at
```

metadata 可放：

```json
{
  "title": "...",
  "slug": "...",
  "category": "...",
  "tags": ["...", "..."]
}
```

---

# 5. 文档处理 Pipeline

文章发布：

```text
Article Published
      │
      ▼
Load Markdown
      │
      ▼
Clean
      │
      ▼
Chunk
      │
      ▼
Embedding
      │
      ▼
pgvector
```

如果已经有 Kafka：

推荐：

```text
ArticlePublished Event
        ↓
AI Index Worker
        ↓
Embedding
        ↓
pgvector
```

V4 可以先由 Spring Boot 内部 Worker 完成。

---

# 6. Chunk 策略

不要固定盲目：

```text
每 500 字切一次
```

建议先按 Markdown 结构切：

```text
H1
H2
H3
Paragraph
Code Block
```

再控制 Token 大小。

每个 Chunk 保存：

```text
articleId
heading
chunkIndex
content
```

---

# 7. 内容 Hash

加入：

```text
content_hash
```

如果文章未变化：

```text
skip re-embedding
```

避免重复成本。

---

# 8. 文章重新发布

流程：

```text
ArticleUpdated
   ↓
compare hash
   ↓
delete/update old chunks
   ↓
re-chunk
   ↓
re-embed
```

要求：

- 不保留过期向量
- 不重复插入同一 chunk

---

# 9. Semantic Search

新增：

```text
GET /api/search/semantic?q=...
```

流程：

```text
Query
 ↓
Embedding
 ↓
pgvector similarity
 ↓
Top K
 ↓
Articles
```

前端可对比：

```text
Keyword Search
vs
Semantic Search
```

这非常适合技术博客实验。

---

# 10. Ask DevLab

API：

```text
POST /api/ai/chat
```

请求：

```json
{
  "message": "总结我关于 Redis 缓存一致性的文章"
}
```

响应建议 Streaming：

```text
SSE
```

前端：

```text
AI Chat UI
```

---

# 11. RAG Pipeline

```text
User Question
     │
     ▼
Query Rewrite（可选）
     │
     ▼
Embedding
     │
     ▼
Vector Search
     │
     ▼
Top K Chunks
     │
     ▼
Prompt Context
     │
     ▼
LLM
     │
     ▼
Answer
     │
     └── Sources
```

---

# 12. 引用

必须返回：

```text
articleId
title
slug
chunkIndex
snippet
score
```

UI：

```text
答案……

Sources:
1. Spring Boot JWT Guide
2. Redis Cache Aside
```

点击跳转原文。

原则：

> RAG 没有引用来源，学习价值会大幅下降。

---

# 13. Prompt 规则

系统 Prompt 建议明确：

```text
只根据提供的 DevLab Context 回答。
如果 Context 不足，明确说明无法从博客内容确定。
回答时给出来源。
不要伪造文章。
```

---

# 14. Hallucination 防护

必须做：

```text
Similarity Threshold
Source Required
No Context → Decline / General Mode
```

可以设计两种模式：

```text
Blog Only
General AI
```

默认：

```text
Blog Only
```

---

# 15. AI 摘要

管理员发布文章时：

```text
Generate Summary
```

输入：

```text
Markdown
```

输出：

```text
summary
seoDescription
```

管理员必须确认后保存。

不要让 AI 自动覆盖正文。

---

# 16. AI 标签

文章编辑页面：

```text
Suggest Tags
```

AI 返回：

```text
Spring Boot
Spring Security
JWT
```

管理员选择。

---

# 17. AI 标题/SEO

可选：

```text
Generate SEO Title
Generate SEO Description
```

保持 Human in the Loop。

---

# 18. Tool Calling

V4 可以做第一个 Tool：

```text
SearchArticlesTool
```

例如：

```text
AI
 ↓
Tool
 ↓
ArticleService
 ↓
PostgreSQL
```

后续：

```text
GetArticleTool
GetTagStatsTool
GetPopularArticlesTool
```

---

# 19. Spring AI 的职责

Spring AI 位于：

```text
企业业务层
+
AI 模型调用层
```

让 AI 可以访问现有：

```text
ArticleService
SearchService
AnalyticsService
UserService（受权限约束）
```

---

# 20. 安全边界

AI Tool 不能绕过权限。

例如：

```text
普通用户
→ AI
→ AdminTool
```

必须被拒绝。

Tool 调用要走：

```text
当前用户身份
+
Spring Security Authorization
```

---

# 21. Streaming

Chat 使用：

```text
SSE
```

流程：

```text
Next.js
   │
   ▼
Spring Boot
   │
   ▼
LLM Streaming
   │
   ▼
SSE
   │
   ▼
UI token-by-token
```

---

# 22. AI Chat 表

建议：

```text
ai_conversations
ai_messages
```

字段：

```text
conversation_id
user_id
title
created_at

message_id
conversation_id
role
content
model
token_usage
created_at
```

---

# 23. 成本记录

每次模型调用记录：

```text
provider
model
input_tokens
output_tokens
latency_ms
estimated_cost
success
```

后续 Dashboard：

```text
今日 AI 请求
Token 使用
平均延迟
失败率
费用
```

---

# 24. Rate Limit

V2 已有 Redis，可以给 AI API：

```text
per-user rate limit
```

例如：

```text
每分钟 10 次
每日 100 次
```

避免成本失控。

---

# 25. AI Cache

对于某些固定摘要可以：

```text
contentHash
→ generated summary
```

文章没变时不重复生成。

---

# 26. Model Provider 抽象

配置不要散落。

建立：

```text
AiModelProperties
```

支持切换：

```text
OpenAI
Anthropic
Gemini
Ollama
其他兼容 API
```

通过环境变量选择。

---

# 27. 本地模型

为了实验：

```text
Ollama
```

可以加入开发环境。

比较：

```text
Cloud LLM
vs
Local LLM
```

测试：

```text
质量
延迟
成本
硬件占用
```

---

# 28. Rerank（可选增强）

第一版：

```text
Vector Search
```

后续：

```text
Vector Search
   ↓
Rerank
   ↓
Top N
```

V4 不要求一开始就上。

---

# 29. Evaluation

必须建立最小评估集：

```text
ai_eval_cases
```

例如 20~50 个问题：

```text
question
expected_sources
expected_keywords
notes
```

运行评估：

```text
Recall
Source Accuracy
Answer Quality
Latency
Cost
```

不要只靠“感觉回答还不错”。

---

# 30. Observability

记录：

```text
AI latency
embedding latency
retrieval latency
LLM latency
retrieved chunks
similarity score
token usage
error
```

为 V5 Agent 调试打基础。

---

# 31. Prompt Version

Prompt 进入版本管理：

```text
rag-answer-v1
summary-v1
tag-suggest-v1
```

可以放：

```text
resources/prompts
```

或者数据库。

---

# 32. Kafka 与 AI

已有 V3 Kafka。

推荐事件：

```text
ArticlePublished
ArticleUpdated
ArticleArchived
```

消费者：

```text
AI Indexing Worker
```

处理：

```text
chunk
embedding
vector update
```

避免用户发布文章时同步等待 Embedding。

---

# 33. 失败重试

Embedding 失败：

```text
retry
```

最终：

```text
DLQ
```

后台增加：

```text
AI Index Status
```

状态：

```text
PENDING
INDEXED
FAILED
```

---

# 34. 数据一致性

文章归档/删除：

```text
ArticleArchived
   ↓
Remove / Disable Chunks
```

不能让 AI 继续引用已经删除的文章。

---

# 35. 前端新增页面

```text
/ai
/ai/chat
/search/semantic
/admin/ai
```

管理后台：

```text
AI Request Stats
Embedding Status
Failed Index Jobs
Model Config（只展示非 Secret）
```

---

# 36. 测试

## Chunk

测试：

```text
Markdown
→ expected chunks
```

## Embedding

测试：

```text
Article Published
→ vectors created
```

## Update

```text
Article Updated
→ old chunks replaced
```

## RAG

```text
known question
→ expected source in Top K
```

## Security

```text
AI Tool
→ cannot bypass role permission
```

---

# 37. 生产部署

新增环境变量：

```text
AI_PROVIDER
AI_MODEL
AI_API_KEY
EMBEDDING_MODEL
```

Secret：

```text
绝不进入 Git
```

pgvector：

```text
PostgreSQL extension
```

生产部署前测试 Migration。

---

# 38. 成本保护

必须设置：

```text
请求限制
Token 上限
Timeout
Retry Limit
模型降级
```

不要：

```text
无限上下文
无限重试
无限 Agent Loop
```

---

# 39. 开发顺序

```text
1. pgvector
2. article_chunks
3. Chunk Pipeline
4. Embedding
5. Article Index
6. Semantic Search
7. Spring AI Chat
8. RAG
9. Sources/Citations
10. Streaming
11. AI Summary
12. AI Tag Suggest
13. Tool Calling
14. Security
15. Cost Metrics
16. Evaluation Dataset
17. Kafka Async Index
18. Production Deploy
```

---

# 40. Definition of Done

- [ ] pgvector 可用
- [ ] 文章可以 Chunk
- [ ] 文章 Embedding 可生成
- [ ] 向量写入 PostgreSQL
- [ ] 文章更新会重新索引
- [ ] 文章删除/归档会清理索引
- [ ] Semantic Search 可用
- [ ] Ask DevLab 可用
- [ ] Chat 支持 Streaming
- [ ] RAG 返回引用来源
- [ ] AI 不足信息时会明确说明
- [ ] AI Summary 可用
- [ ] AI Tag Suggest 可用
- [ ] 至少一个 Tool Calling 功能
- [ ] Tool 权限不会绕过 Spring Security
- [ ] AI API 有限流
- [ ] Token/延迟/错误可统计
- [ ] 至少 20 个评估问题
- [ ] Kafka 异步索引链路可用
- [ ] 生产环境成功部署

---

# 41. 常见风险

## 只做 Chat API

解决：

> V4 必须包含 Embedding + Retrieval + Citation + Evaluation。

## RAG 没有来源

解决：

> 每次返回 source metadata。

## AI 能调用所有 Service

解决：

> Tool 必须权限隔离。

## 没有成本指标

解决：

> Token、Latency、Cost 从第一天记录。

## 文章更新后向量过期

解决：

> Article Event 驱动索引更新。

---

# 42. 进入 V5 的条件

你需要能够解释：

```text
RAG 和 Fine-tuning 的区别
Chunk 为什么影响检索
Embedding 是什么
Similarity Search 如何工作
为什么需要 Citation
为什么要做 Evaluation
Tool Calling 和普通 Function Call 有何区别
为什么 Agent 不应该无限循环
```

然后进入 V5：

```text
Python + FastAPI + LangGraph
Agent Runtime
MCP
多服务 AI 全栈
```
