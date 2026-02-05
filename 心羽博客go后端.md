# 问题与解决

## 1.**环境和依赖**

**国内代理**

设置里修改Go Modules->Environment

```
GOPROXY=https://goproxy.cn,direct
```

或者命令行输入

```bash
go env -w GOPROXY=https://goproxy.cn,direct
```

**模块初始化**

```bash
go mod init github.com/CNxinyu/Xinyu-blog/go-backend
```

**引入hertz**

```bash
go get -u github.com/cloudwego/hertz
```

如果不成功进入bash重试

```bash
touch main.go
```

main加入Hertz案例

```
go mod tidy
```

**cwgo**

```bash
go install github.com/cloudwego/cwgo@latest
```

**thriftgo**

```
go install github.com/cloudwego/thriftgo@latest
```

**依赖**

**ymal**

```
go get gopkg.in/yaml.v3
```

**git**

```
git init
git add .
```

**logrus**

```
go get github.com/sirupsen/logrus
```

**数据库**

```powershell
go get gorm.io/driver/mysql
go get gorm.io/gorm
```

**gin**

```
go get -u github.com/gin-gonic/gin
```

**redis**

```
go get github.com/go-redis/redis/v8
```

**swag**

```
go get github.com/swaggo/gin-swagger
go get github.com/swaggo/files
```

**kitex**

```
go install github.com/cloudwego/kitex/tool/cmd/kitex@latest
```

**hz**

```
go install github.com/cloudwego/hertz/cmd/hz@latest
```

**thirftgo**

```bash
go install github.com/cloudwego/thriftgo@latest
```

go run main.go -e prod

go run main.go -e dev

## 2.yaml方案

方案二（⭐⭐ 真正大厂做法）：命令行参数 + 绝对路径

main.go

```
configPath := flag.String("config", "config/config.yaml", "config file path")
flag.Parse()

if err := app.NewApp(*configPath).Run(); err != nil {
	log.Fatal(err)
}
```

启动：

```
go run cmd/server/main.go -config=config/config.yaml
```

## 3.端口占用问题

**1先确认是谁占用**了 8000

在 PowerShell / CMD 执行：

```
netstat -ano | findstr :8000
```

你会看到类似：

```
TCP    0.0.0.0:8000   0.0.0.0:0   LISTENING   12345
```

最后一列 `12345` 就是 **PID**。

**2再查进程是谁**：

```
tasklist /FI "PID eq 12345"
```

------

**3释放端口**（结束占用进程）

如果确认这个进程可以杀掉：

```
taskkill /PID 12345 /F
```

然后再 `kratos run`。

# 1.框架搭建

## 1.1设计概念DDD(了解)

以业务领域为中心，通过清晰的模型、边界和语言来控制复杂度

1. 通用语言
2. 限界上下文
3. 领域模型
4. 聚合

分层结构

```
┌────────────────────────────────────┐
│           Interface 层              │
│  Controller / API / MQ Consumer     │
└────────────────────────────────────┘
                ↓
┌────────────────────────────────────┐
│         Application 层               │
│  用例编排 / 事务 / 权限 / 防重       │
└────────────────────────────────────┘
                ↓
┌────────────────────────────────────┐
│           Domain 层 ⭐⭐⭐              │
│  Entity / VO / Aggregate / DomainSvc│
└────────────────────────────────────┘
                ↓
┌────────────────────────────────────┐
│       Infrastructure 层              │
│  JPA / MyBatis / Redis / MQ / RPC    │
└────────────────────────────────────┘
```



​	这是一个基于 Gin 的单体博客项目，项目按 Clean Architecture + DDD 思路设计， 核心业务与基础设施解耦，
 初期复杂度可控，但在业务增长、微服务拆分、团队协作时成本最低。



## 1.1模块初始化

推荐github.com/GitHub的用户名/项目名称

```bash
go mod init github.com/CNxinyu/Xinyu-blog/go-backend
```

## 1.2安装核心依赖

| 依赖                     | 类型 / 层级         | 主要作用                       | 在项目中的职责                          | 大厂面试标准说法                             |
| ------------------------ | ------------------- | ------------------------------ | --------------------------------------- | -------------------------------------------- |
| **gin-gonic/gin**        | Web 框架（HTTP 层） | 提供 HTTP Server、路由、中间件 | 路由注册、参数绑定、请求处理、JSON 返回 | Gin 只负责 HTTP 协议与中间件，不承载业务逻辑 |
| **gorm.io/gorm**         | ORM（数据访问层）   | 对象关系映射、CRUD、事务       | DAO 层操作数据库                        | ORM 仅存在于 DAO 层，实现业务与存储解耦      |
| **gorm.io/driver/mysql** | 数据库驱动          | GORM 连接 MySQL                | 提供 MySQL 适配                         | 使用 Driver 解耦数据库，方便后续切换         |
| **spf13/viper**          | 配置管理            | 读取 YAML / ENV 配置           | 统一管理服务配置                        | 配置集中管理，避免硬编码，支持多环境         |
| **uber-go/zap**          | 日志组件            | 高性能结构化日志               | 记录访问、错误、业务日志                | 结构化日志方便生产环境日志采集               |
| **go-redis/redis/v8**    | 缓存 / 中间件       | Redis 操作客户端               | 缓存热点数据、计数                      | Redis 减轻数据库压力，支持高并发             |
| **golang-jwt/jwt/v5**    | 鉴权组件            | JWT 生成与校验                 | 用户登录鉴权                            | JWT 无状态，适合分布式系统                   |

```bash
go get -u github.com/gin-gonic/gin
go get -u gorm.io/gorm
go get -u gorm.io/driver/mysql
go get -u github.com/spf13/viper
go get -u go.uber.org/zap
go get -u github.com/go-redis/redis/v8
go get -u github.com/golang-jwt/jwt/v5
go get github.com/redis/go-redis/v9
```

## 1.3项目初始目录

```
backend/
├── cmd/
│   └── server/
│       └── main.go                # 只做启动：加载配置、调用 bootstrap 装配并 run
│
├── bootstrap/                     # ⭐ 依赖装配（wire 或手写）
│   ├── wire.go
│   ├── http.go                    # router/handler/middleware 装配
│   ├── persistence.go             # db/tx/repo 装配
│   ├── cache.go                   # redis
│   ├── mq.go                      # mq
│   └── app.go                     # 应用服务装配
│
├── interfaces/                    # ⭐ 接口层（Interface / Adapter）
│   ├── http/
│   │   ├── router.go
│   │   ├── middleware/
│   │   └── v1/
│   │       ├── admin/
│   │       │   └── user_handler.go
│   │       └── customer/
│   │           └── schedule_handler.go
│   ├── grpc/                      # 可选
│   └── dto/                       # Request/Response DTO（只属于接口层）
│
├── application/                   # ⭐ 应用层（Use Case / App Service）
│   ├── user/
│   │   ├── command/
│   │   │   ├── login.go
│   │   │   ├── register.go
│   │   │   └── change_password.go
│   │   ├── query/
│   │   │   └── user_profile.go
│   │   ├── service.go             # UserAppService：编排领域对象、事务、repo、事件
│   │   └── ports.go               # 应用层依赖的端口(接口)：Tx、Repo、Cache、MQ...
│   ├── schedule/
│   └── auth/
│
├── domain/                        # ⭐ 领域层（纯业务核心）
│   ├── user/
│   │   ├── aggregate.go           # User 聚合根
│   │   ├── behavior.go            # 领域行为（方法/策略）
│   │   ├── repository.go          # 仓储接口（domain 视角）
│   │   ├── service.go             # 领域服务（少用，放跨实体规则）
│   │   ├── event.go               # 领域事件
│   │   └── vo.go                  # 值对象
│   ├── order/
│   ├── schedule/
│   └── shared/                    # ⭐ 跨域共享：VO、ID、Money、TimeRange、领域错误等
│
├── infrastructure/                # ⭐ 基础设施层（技术细节实现）
│   ├── persistence/
│   │   ├── db/                    # db连接、迁移、orm初始化
│   │   ├── tx/                    # 事务实现：TxManager
│   │   └── user/
│   │       ├── repo.go            # 实现 domain.user.Repository
│   │       ├── mapper.go          # DO/PO <-> Domain 转换
│   │       └── model.go           # 持久化模型（表结构）
│   ├── cache/
│   │   └── redis/
│   ├── mq/
│   ├── auth/                      # jwt/oidc/casbin 等实现
│   ├── crypto/                    # encrypt/hash
│   ├── captcha/
│   └── verify/
│
├── pkg/                           # ⭐ 可复用库（可被别的服务引用，不要放业务）
│   ├── logger/
│   ├── errors/
│   ├── config/
│   ├── tracing/
│   └── httpx/
│
├── config/                        # 配置文件（yaml/toml）
├── docs/                          # 接口文档/架构文档
└── go.mod

```

## 1.2项目初始化（修）

[项目初始化 | Kratos](https://go-kratos.dev/zh-cn/docs/getting-started/start/)

对照Kratos官方文档进行初始化项目

## 1.3简单案例详解

**proto文件**

1.**命令行新建文件**

```powershell
kratos proto add api/test/test.proto
```

2.**修改内容**

```protobuf
message GetTestRequest {
	uint32 length = 1;
	TYPE type = 2;
}
/// 返回参数结构
message GetTestReply {
	string code = 1;
}
```

3.**生成客户端代码**

```
kratos proto client api/test/test.proto
```

4.**生成服务端代码**

```
kratos proto server api/test/test.proto -t internal/service
```

5.**service/service.go中调用函数**

```go
var ProviderSet = wire.NewSet(NewGreeterService,NewTestService)
```

6.**server/grpc.go**

```
// 增加 testService 参数
func NewGRPCServer(c *conf.Server, greeter *service.GreeterService,testService *service.TestService, logger log.Logger) *grpc.Server {
.......
	// 完成 TestService 的注册
	test.RegisterTestServer(srv, testService)
	return srv


```

7.生成

```
go generate ./...
```

8.**写服务** service

/service.go

```go
package service

import (
	"context"
	"crypto/rand"
	"math/big"

	pb "go-backend/api/test"
)

type TestService struct {
	pb.UnimplementedTestServer
}

func NewTestService() *TestService {
	return &TestService{}
}

func (s *TestService) GetTest(ctx context.Context, req *pb.GetTestRequest) (*pb.GetTestReply, error) {
    return &pb.GetTestReply{
		Code: RandCode(int(req.Length), req.Type),
	}, nil
}

func RandCode(length int, codeType pb.TYPE) string {
	if length <= 0 {
		return ""
	}

	var charset string
	switch codeType {
	case pb.TYPE_DIGIT:
		charset = "0123456789"
	case pb.TYPE_LETTER:
		charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
	case pb.TYPE_MIXED:
		charset = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
	default:
		charset = "0123456789"
	}

	bytes := make([]byte, length)
	charsetLen := big.NewInt(int64(len(charset)))

	for i := 0; i < length; i++ {
		n, err := rand.Int(rand.Reader, charsetLen)
		if err != nil {
			return ""
		}
		bytes[i] = charset[n.Int64()]
	}

	return string(bytes)
}

```

**文件**

**解释**

1. **文件头**：syntax / package / option

  syntax = "proto3";

必须写在第一行附近，声明用 proto3 语法。

proto3 特点：字段默认值、optional 行为、enum 默认 0、字段不再需要 required/optional（proto2 才有 required）。

package api.test;

proto 的“逻辑命名空间”，影响生成代码的包名/命名空间（不同语言映射不同）。

建议与目录结构一致，比如 api/test/test.proto 对应 package api.test;

option go_package = "...";

Go 生成代码的 import 路径（非常重要，不然 go module 引用会乱）。

常见写法：

option go_package = "github.com/xxx/yyy/api/test;test";

分号后面的 ;test 是 Go 包名（生成的 package name）

option java_multiple_files = true;

Java 每个 message/service 单独生成一个文件（更好用）。

option java_package = "api.test";

Java 的 package 名，通常跟你 Java 工程包结构一致。

**service：定义 RPC 接口**

```protobuf
service Test {
  rpc CreateTest (CreateTestRequest) returns (CreateTestReply);
  rpc UpdateTest (UpdateTestRequest) returns (UpdateTestReply);
  rpc DeleteTest (DeleteTestRequest) returns (DeleteTestReply);
  rpc GetTest (GetTestRequest) returns (GetTestReply);
  rpc ListTest (ListTestRequest) returns (ListTestReply);
}
```

这块是 gRPC 的接口定义（类似 REST 的 Controller）：

- `rpc 方法名 (请求消息) returns (响应消息);`
- 方法名一般用动词开头（Create/Update/Delete/Get/List）
- 常见增强写法（可选）：
  - 增加注释说明用途、权限、幂等、错误码约定
  - 使用 `google.api.http` 做 REST 映射（如果你们要 grpc-gateway）

 **message：定义请求/响应结构**

你现在每个 message 都是 `{}` 空结构，只是骨架。真实项目通常会放：

**Create**

- Request：创建所需字段（name、type、payload、创建人等）
- Reply：新建后的 id、完整对象、或仅返回 status

**Update**

- Request：id + 可变字段（建议配 `FieldMask` 做部分更新）
- Reply：更新后的对象/成功标记

**Delete**

- Request：id（可选：软删标记、删除原因）
- Reply：成功标记

**Get**

- Request：id
- Reply：对象详情

**List**

- Request：分页 page/page_size、过滤条件 filter、排序 order_by

- Reply：items 数组 + total

- ## proto3 常用语法速记

  - 字段格式：`<类型> <字段名> = <编号>;`
    - 编号从 1 开始递增，**一旦发布不要随便改**（兼容性）
  - 类型常用：
    - 标量：`string, int32, int64, bool, double`
    - 时间：推荐 `google.protobuf.Timestamp`
    - 数组：`repeated string tags = 5;`
    - 枚举：`enum Status { STATUS_UNSPECIFIED = 0; ... }`
    - map：`map<string, string> labels = 10;`
  - 结构复用：把核心对象单独抽成 `message TestEntity { ... }`

# 2.功能实现

## 2.1验证码获取

命令：

```powershell
kratos proto add api/user/v1/user.proto
```

```
kratos proto client api/user/v1/user.proto
```

```
kratos proto server api/user/v1/user.proto -t internal/service
```

server/http.go 

```
userv1 "go-backend/api/user/v1"
...
func NewHTTPServer(c *conf.Server, userService *service.UserService, greeter
...
userv1.RegisterUserHTTPServer(srv,userService)
```

service/service.go

```
set里添加NewUserService
```

命令行

```
go generate ./...
```

 
