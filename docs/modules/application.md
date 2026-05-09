# Application 模块学习文档

## 模块概述

`application` 模块是 ThingsBoard 的核心启动模块，包含了主应用入口、REST API 控制器、业务服务实现和 Actor 系统。这是整个平台的"大脑"，协调所有其他模块的工作。

## 目录结构

```
application/src/main/java/org/thingsboard/server/
├── ThingsboardServerApplication.java    # Spring Boot 主入口
├── ThingsboardInstallApplication.java   # 安装/升级入口
├── actors/                              # Actor 系统实现
│   ├── ActorSystemContext.java          # Actor 系统上下文(核心依赖注入)
│   ├── app/                             # 应用级 Actor
│   ├── tenant/                          # 租户级 Actor
│   ├── ruleChain/                       # 规则链 Actor
│   ├── device/                          # 设备 Actor
│   ├── calculatedField/                 # 计算字段 Actor
│   └── service/                         # Actor 服务层
├── controller/                          # REST API 控制器 (65+个)
│   ├── BaseController.java             # 所有控制器基类
│   ├── DeviceController.java           # 设备管理API
│   ├── TelemetryController.java        # 遥测数据API
│   ├── RuleChainController.java        # 规则链管理API
│   ├── AlarmController.java            # 告警管理API
│   └── ...
├── service/                            # 业务服务实现 (43个子包)
│   ├── queue/                          # 队列消费者服务
│   ├── subscription/                   # WebSocket订阅服务
│   ├── telemetry/                      # 遥测处理服务
│   ├── transport/                      # 传输层服务
│   ├── rpc/                           # RPC服务
│   ├── security/                      # 安全认证服务
│   ├── install/                       # 安装/升级服务
│   └── ...
├── config/                            # Spring 配置类
├── exception/                         # 异常处理
└── utils/                            # 工具类
```

## 核心组件分析

### 1. Actor 系统 (`actors/`)

ThingsBoard 使用自定义的 Actor 模型来处理高并发的设备消息。

**Actor 层级结构:**
```
AppActor (应用级)
  └── TenantActor (租户级，每个租户一个)
        ├── RuleChainActor (规则链级)
        │     └── RuleNodeActor (规则节点级)
        └── DeviceActor (设备级，每个设备一个)
```

**关键类:**
- `ActorSystemContext`: 注入所有Actor需要的服务依赖
- `AppActor`: 顶层Actor，负责创建和管理TenantActor
- `TenantActor`: 管理租户下的规则链和设备Actor
- `RuleChainActor`: 管理规则链内的规则节点执行流程
- `DeviceActor`: 管理设备状态、会话和RPC调用

### 2. REST API 控制器 (`controller/`)

提供完整的 RESTful API，包括：

| 控制器 | 功能 |
|--------|------|
| `DeviceController` | 设备CRUD、凭证管理、分配 |
| `TelemetryController` | 遥测数据读写、属性管理 |
| `AlarmController` | 告警创建、确认、清除 |
| `RuleChainController` | 规则链CRUD、导入导出 |
| `DashboardController` | 仪表盘管理 |
| `AuthController` | 登录、刷新Token、密码重置 |
| `UserController` | 用户管理 |
| `AssetController` | 资产管理 |
| `EntityRelationController` | 实体关系管理 |

### 3. 业务服务 (`service/`)

**队列消费服务 (`queue/`):**
- `DefaultTbCoreConsumerService`: 核心消息消费者
- `DefaultTbRuleEngineConsumerService`: 规则引擎消息消费者
- 负责从 Kafka 消费消息并分发到 Actor 系统

**订阅服务 (`subscription/`):**
- 管理 WebSocket 连接
- 实时推送遥测数据变更、告警事件

**传输服务 (`transport/`):**
- 处理设备与平台之间的会话管理
- 消息路由和转发

## 消息处理流程

```
1. 设备通过Transport发送消息
2. TransportService 将消息放入 Kafka Queue
3. TbRuleEngineConsumerService 消费消息
4. 消息被路由到对应的 TenantActor
5. TenantActor 将消息转发到 RuleChainActor
6. RuleChainActor 按规则链配置依次执行 RuleNodeActor
7. RuleNodeActor 执行具体逻辑(保存数据、创建告警、外部调用等)
```

## 配置文件

- `application/src/main/resources/thingsboard.yml` - 主配置文件
- `application/src/main/conf/` - 部署配置模板

## 关键依赖

- Spring Boot 3.5.x (Web, Security, Actuator)
- 所有 common/* 模块
- rule-engine 模块
- dao 模块
