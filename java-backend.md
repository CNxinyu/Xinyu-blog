# 项目概览

## 技术版本

springboot4

nasoc 3.1.1

# 搭建阶段

## 1.创建maven父模块

删除src，app

pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>com.xinyu</groupId>
    <artifactId>java-backend</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>java-backend-parent</name>
    <description>xinyu博客后端项目 - 父模块</description>
    <modules>
        <module>common</module>
        <module>article</module>
    </modules>

    <!-- Spring Boot 父依赖 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.1</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>

    <!-- 属性定义 -->
    <properties>
        <java.version>25</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>

        <!-- Spring Cloud 系列版本 -->
        <spring-cloud.version>2025.0.0</spring-cloud.version>
        <spring-cloud-alibaba.version>2025.0.0.0</spring-cloud-alibaba.version>

        <!-- 常用第三方依赖版本（2026年1月最新稳定版） -->
        <mybatis-plus.version>3.5.15</mybatis-plus.version>
        <hutool.version>5.8.40</hutool.version>
        <druid-spring-boot-starter.version>1.2.23</druid-spring-boot-starter.version>
        <knife4j.version>4.5.0</knife4j.version>
        <fastjson2.version>2.0.52</fastjson2.version>
        <sa-token.version>1.39.0</sa-token.version>
        <dynamic-datasource.version>4.3.1</dynamic-datasource.version>
    </properties>

    <!-- 依赖版本管理 -->
    <dependencyManagement>
        <dependencies>
            <!-- Spring Cloud -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- Spring Cloud Alibaba -->
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- MyBatis-Plus -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-generator</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>

            <!-- Hutool 工具包 -->
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>

            <!-- Druid 数据源（可选） -->
            <dependency>
                <groupId>com.alibaba</groupId>
                <artifactId>druid-spring-boot-starter</artifactId>
                <version>${druid-spring-boot-starter.version}</version>
            </dependency>

            <!-- Knife4j 接口文档（Swagger 增强） -->
            <dependency>
                <groupId>com.github.xiaoymin</groupId>
                <artifactId>knife4j-spring-boot-starter</artifactId>
                <version>${knife4j.version}</version>
            </dependency>

            <!-- FastJSON2（可选，高性能 JSON 库） -->
            <dependency>
                <groupId>com.alibaba.fastjson2</groupId>
                <artifactId>fastjson2</artifactId>
                <version>${fastjson2.version}</version>
            </dependency>

            <!-- Sa-Token 权限认证（国产轻量级） -->
            <dependency>
                <groupId>cn.dev33</groupId>
                <artifactId>sa-token-spring-boot-starter</artifactId>
                <version>${sa-token.version}</version>
            </dependency>

            <!-- Dynamic Datasource 多数据源（可选） -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>dynamic-datasource-spring-boot-starter</artifactId>
                <version>${dynamic-datasource.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <!-- 全局构建配置 -->
    <build>
        <plugins>
            <!-- Spring Boot Maven 插件（子模块继承） -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>

            <!-- Maven Compiler 插件 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <encoding>UTF-8</encoding>
                </configuration>
                <executions>
                    <execution>
                        <id>default-testCompile</id>
                        <phase>test-compile</phase>
                        <goals>
                            <goal>testCompile</goal>
                        </goals>
                        <configuration>
                            <skip>true</skip>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

            <!-- Maven Surefire Plugin - 跳过测试执行 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <skipTests>true</skipTests>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

## 2.nacos下载

bin目录启动命令行：startup.cmd -m standalone

配置文件：conf  - application.properties

nacos.core.auth.plugin.nacos.token.secret.key=VGhpc0lzTXlDdXN0b21TZWNyZXRLZXkwMTIzNDU2Nzg=

密码设置：mySecureKey2026

访问页面：http://127.0.0.1:8080/index.html

(新版端口8080，以前版本8848)

common包下pom.xml添加依赖

```xml
        <!-- nacos 服务注册与发现 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>

        <!-- nacos 配置中心 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>
```

主程序添加注解

```java
@EnableDiscoveryClient //开启服务发现
```

yaml中配置

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8080
      config:
        server-addr: 127.0.0.1:8080
        file-extension: yaml
```

```
left join sys_dept d on o.dept_id = d.dept_id
```

```
d.name as deptName
```

# 功能实现

## 1.验证码

### 服务

| 功能       | 服务           | 请求参数              | 响应参数            |
| ---------- | -------------- | --------------------- | ------------------- |
| 发送验证码 | SendVerifyCode | SendVerifyCodeRequest | SendVerifyCodeReply |
| 校验验证码 | VerifyCode     | VerifyCodeRequest     | VerifyCodeReply     |
|            |                |                       |                     |

### 参数

SendVerifyCode

| 含义        | 类型                                                         |             |
| ----------- | ------------------------------------------------------------ | ----------- |
| 接收类型    | 枚举(1:SMS手机短信，2:EMAIL邮件)                             | channel     |
| 业务场景    | 枚举(1:REGISTER注册，2:LOGIN登录,3:RESET_PASSWORD重置密码，4:BIND绑定号码,5:CHANGE变更号码) | Scene       |
| 手机号/邮箱 | string                                                       | receiver    |
| 验证码长度  | int32                                                        | code_length |
| 客户端信息  | 枚举                                                         | ClientInfo  |

SendVerifyCodeReply

| 含义             | 类型                                                         |                 |
| ---------------- | ------------------------------------------------------------ | --------------- |
| 错误代码         | 枚举(1:OK,1006:验证码错误，1005:EXPIRED过期)                 | code            |
|                  |                                                              | message         |
| 校验用的短期凭证 | string                                                       | verification_id |
| 发送时间         | google.protobuf.Timestamp                                    | sent_at         |
| 验证码有效期     | google.protobuf.Duration                                     | ttl             |
| 冷却时间         | google.protobuf.Duration                                     | cooldown        |
| 风控             | 枚举（1:ALLOW允许，2:需要验证码REQUIRE_CAPTCHA,3:BLOCK拒绝） | risk_action     |

VerifyCodeRequest

| 含义                     | 类型   |                 |
| ------------------------ | ------ | --------------- |
| 来自 SendVerifyCodeReply | string | verification_id |
| 用户输入的验证码         | string | code            |
| 用于风控与审计           | 枚举   | client          |

VerifyCodeReply

|          | 类型          |          |
| -------- | ------------- | -------- |
|          | string        | message  |
|          | ErrorCode枚举 | code     |
| 是否通过 | bool          | verified |

proto

```protobuf
syntax = "proto3";

option java_multiple_files = true;
option java_package = "com.xinyu.grpc";
option java_outer_classname = "VerifyCodeProto";

package verify.v1;

import "google/protobuf/timestamp.proto";
import "google/protobuf/duration.proto";

/// 验证码通道/接收类型
enum Channel {
  CHANNEL_UNSPECIFIED = 0;
  SMS = 1;     // 手机短信
  EMAIL = 2;   // 邮件
}

/// 验证码业务场景（强烈建议按业务细分）
enum Scene {
  SCENE_UNSPECIFIED = 0;
  REGISTER = 1;        // 注册
  LOGIN = 2;           // 登录
  RESET_PASSWORD = 3;  // 重置密码
  BIND = 4;            // 绑定手机号/邮箱
  CHANGE = 5;          // 变更手机号/邮箱
}

/// 风控动作：用于告诉调用方“该怎么做”
enum RiskAction {
  RISK_ACTION_UNSPECIFIED = 0;
  ALLOW = 1;        // 允许发送/允许校验
  REQUIRE_CAPTCHA = 2; // 需要验证码/图形验证码/人机校验（由你们业务实现）
  BLOCK = 3;        // 拒绝（高风险/黑名单）
}

/// 统一错误码（业务码）
/// 建议：code=0 表示成功；非 0 表示失败；message 给用户可读信息（谨慎暴露细节）
enum ErrorCode {
  ERROR_CODE_UNSPECIFIED = 0;
  OK = 1;

  INVALID_ARGUMENT = 1001;   // 参数非法
  TOO_MANY_REQUESTS = 1002;  // 触发限流/冷却
  RISK_BLOCKED = 1003;       // 风控拦截
  NOT_FOUND = 1004;          // verification_id 不存在/无效
  EXPIRED = 1005;            // 过期
  CODE_MISMATCH = 1006;      // 验证码错误
  TOO_MANY_ATTEMPTS = 1007;  // 校验失败次数过多
  INTERNAL_ERROR = 2000;     // 服务器内部错误
}

/// 客户端信息：用于风控、审计、限流（不要完全信任，建议结合网关/服务端注入）
message ClientInfo {
  string request_id = 1;   // 幂等键：同一个 request_id 在冷却期内可返回同一结果（可选）
  string ip = 2;           // 客户端 IP（建议由网关填充）
  string user_agent = 3;   // UA（可选）
  string device_id = 4;    // 设备标识（可选）
  string app_id = 5;       // 调用方应用标识（多应用/多租户时有用）
  string tenant_id = 6;    // 租户/组织（可选）
}

/// 发送验证码请求
message SendVerifyCodeRequest {
  Channel channel = 1;     // SMS/EMAIL
  Scene scene = 2;         // REGISTER/LOGIN/...
  string receiver = 3;     // 手机号(建议 E.164) 或 邮箱（由 channel 决定）
  int32 code_length = 4;   // 例如 6（服务端可忽略并强制固定，避免被滥用）
  ClientInfo client = 5;   // 用于风控、审计、限流
}

/// 发送验证码响应（生产环境不返回 verify_code）
message SendVerifyCodeReply {
  ErrorCode code = 1;
  string message = 2;

  string verification_id = 3;                 // 校验用的短期凭证（重要）
  google.protobuf.Timestamp sent_at = 4;      // 发送时间
  google.protobuf.Duration ttl = 5;           // 验证码有效期
  google.protobuf.Duration cooldown = 6;      // 冷却时间（还需等多久才能再次发送）

  RiskAction risk_action = 7;                 // ALLOW / REQUIRE_CAPTCHA / BLOCK
  bool exists = 8;                            // 可选：是否该 receiver 已存在（⚠️注意枚举/用户枚举风险，默认不建议开启）
}

/// 校验验证码请求
message VerifyCodeRequest {
  string verification_id = 1; // 来自 SendVerifyCodeReply
  string code = 2;            // 用户输入的验证码
  ClientInfo client = 3;      // 用于风控与审计（建议由网关补全）
}

/// 校验验证码响应
message VerifyCodeReply {
  ErrorCode code = 1;
  string message = 2;

  bool verified = 3;                         // 是否通过（成功时 true）
  google.protobuf.Timestamp verified_at = 4; // 校验时间

  // 校验失败时可返回剩余尝试次数/锁定时间（避免暴露过多细节，可按需开启）
  int32 remaining_attempts = 5;
  google.protobuf.Duration lock_duration = 6; // 如果触发锁定，锁定多久

  RiskAction risk_action = 7;                // 校验阶段也可能触发风控
}

/// 可选：消费/确认使用验证码（防重放）
/// 如果你们希望“验证码校验通过后必须被消费一次”，可以加这个接口；否则 VerifyCode 成功即视为消费也行。
message ConsumeCodeRequest {
  string verification_id = 1;
  ClientInfo client = 2;
}

message ConsumeCodeReply {
  ErrorCode code = 1;
  string message = 2;
  bool consumed = 3;
  google.protobuf.Timestamp consumed_at = 4;
}

service VerifyCodeService {
  /// 发送验证码（生产级：不返回验证码内容）
  rpc SendVerifyCode(SendVerifyCodeRequest) returns (SendVerifyCodeReply);

  /// 校验验证码（建议：校验成功后立即失效或单次有效）
  rpc VerifyCode(VerifyCodeRequest) returns (VerifyCodeReply);

  /// 可选：消费验证码（如需要二次确认/防重放更严格）
  rpc ConsumeCode(ConsumeCodeRequest) returns (ConsumeCodeReply);
}

```

117，171，118

上海浦西光大店一组，上海浦东八佰伴店，上海浦西光大店2组。

```sql
{
    "code": "OK",
    "message": "OK",
    "verification_id": "niW0Fd6sQXosKkeGxX7rOCVzLvh2Wrdi",
    "sent_at": {
        "seconds": "1769416398",
        "nanos": 872874800
    },
    "ttl": {
        "seconds": "300",
        "nanos": 0
    },
    "cooldown": {
        "seconds": "60",
        "nanos": 0
    },
    "risk_action": "ALLOW",
    "exists": false
}
```

派工推荐类型的派工单

