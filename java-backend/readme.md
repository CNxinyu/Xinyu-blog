config/：Spring 配置类、@ConfigurationProperties、必要的 Bean（Redis/Mail 额外定制）

grpc/：只做协议层（把 proto request 转成业务入参；把业务结果映射成 proto reply）

service/：核心业务逻辑（限流、冷却、锁、验证码生成、发送、校验）

repo/：Redis 读写细节（Key 规则、TTL、incr、json 序列化）

model/：业务内部 DTO（不要直接用 proto 对象穿透到 service/repo，方便未来换协议/HTTP）

util/：纯工具方法（无状态）