# ThingsBoard 项目完整学习指南

## 项目概述

ThingsBoard 是一个开源的物联网(IoT)平台，用于数据收集、处理、可视化和设备管理。它支持通过行业标准的 IoT 协议（MQTT、CoAP、HTTP）实现设备连接，并提供强大的规则引擎进行数据处理和告警触发。

- **版本**: 4.4.0-SNAPSHOT
- **技术栈**: Java 25 + Spring Boot 3.5.x + Angular 前端
- **许可证**: Apache License 2.0
- **官网**: https://thingsboard.io

---

## 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        UI Layer (Angular)                         │
│                         ui-ngx 模块                               │
└────────────────────────────────┬────────────────────────────────┘
                                 │ REST API / WebSocket
┌────────────────────────────────┴────────────────────────────────┐
│                    Application Layer                              │
│    ┌──────────┐  ┌──────────────┐  ┌──────────────────────┐     │
│    │Controller│  │  Service层   │  │    Actor System       │     │
│    │  (REST)  │  │(业务逻辑)    │  │(消息处理/设备管理)    │     │
│    └──────────┘  └──────────────┘  └──────────────────────┘     │
└────────────────────────────────┬────────────────────────────────┘
                                 │
┌────────────────────────────────┴────────────────────────────────┐
│                    Rule Engine Layer                              │
│    ┌──────────────┐  ┌───────────────┐  ┌─────────────────┐    │
│    │ Rule Chains  │  │  Rule Nodes   │  │  Message Queue  │    │
│    │ (规则链)     │  │  (规则节点)   │  │  (消息队列)     │    │
│    └──────────────┘  └───────────────┘  └─────────────────┘    │
└────────────────────────────────┬────────────────────────────────┘
                                 │
┌────────────────────────────────┴────────────────────────────────┐
│                    Transport Layer                                │
│    ┌──────┐  ┌──────┐  ┌──────┐  ┌───────┐  ┌──────┐          │
│    │ MQTT │  │ CoAP │  │ HTTP │  │ LwM2M │  │ SNMP │          │
│    └──────┘  └──────┘  └──────┘  └───────┘  └──────┘          │
└────────────────────────────────┬────────────────────────────────┘
                                 │
┌────────────────────────────────┴────────────────────────────────┐
│                    Data Access Layer (DAO)                        │
│    ┌──────────────┐  ┌───────────────┐  ┌──────────────────┐   │
│    │  PostgreSQL  │  │  Cassandra    │  │     Cache        │   │
│    │  (关系数据)  │  │  (时序数据)   │  │  (Valkey/Redis)  │   │
│    └──────────────┘  └───────────────┘  └──────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 模块结构总览

| 模块 | 路径 | 职责 |
|------|------|------|
| **application** | `/application` | 主应用入口，REST API控制器，业务服务，Actor系统 |
| **common/actor** | `/common/actor` | Actor模型框架，消息驱动的并发处理 |
| **common/cache** | `/common/cache` | 缓存抽象层(Valkey/Redis) |
| **common/cluster-api** | `/common/cluster-api` | 集群通信API |
| **common/dao-api** | `/common/dao-api` | 数据访问对象接口定义 |
| **common/data** | `/common/data` | 核心数据模型(Entity, ID, DTO) |
| **common/message** | `/common/message` | 消息模型定义(TbMsg, MsgType) |
| **common/queue** | `/common/queue` | 消息队列抽象(Kafka, Memory) |
| **common/transport** | `/common/transport` | 传输层协议实现 |
| **common/edqs** | `/common/edqs` | 实体数据查询服务 |
| **common/edge-api** | `/common/edge-api` | 边缘计算API |
| **dao** | `/dao` | 数据持久化实现(SQL/NoSQL) |
| **edqs** | `/edqs` | EDQS独立服务模块 |
| **rule-engine** | `/rule-engine` | 规则引擎(API + 组件) |
| **transport** | `/transport` | 独立传输微服务 |
| **ui-ngx** | `/ui-ngx` | Angular前端应用 |
| **msa** | `/msa` | 微服务架构打包与部署 |
| **rest-client** | `/rest-client` | Java REST客户端SDK |
| **monitoring** | `/monitoring` | 监控服务 |
| **tools** | `/tools` | 工具集 |
| **netty-mqtt** | `/netty-mqtt` | MQTT协议Netty实现 |

---

## 核心概念

### 1. 多租户架构
- **系统管理员(SysAdmin)**: 管理整个平台
- **租户(Tenant)**: 独立的业务单元，拥有自己的设备、用户和规则
- **客户(Customer)**: 租户下的子单元，可管理部分设备
- **用户(User)**: 具体的操作人员

### 2. 设备管理
- **Device**: 物理设备的数字化表示
- **Device Profile**: 设备配置模板(传输类型、告警规则等)
- **Asset**: 逻辑资产(如建筑物、区域)
- **Entity View**: 设备数据的只读视图

### 3. 规则引擎
- **Rule Chain**: 由多个规则节点组成的处理流水线
- **Rule Node**: 独立的消息处理单元(过滤、转换、动作)
- **TbMsg**: 规则引擎内部传递的消息对象

### 4. Actor模型
- **AppActor**: 应用级顶层Actor
- **TenantActor**: 租户级Actor
- **RuleChainActor**: 规则链Actor
- **RuleNodeActor**: 规则节点Actor
- **DeviceActor**: 设备Actor(管理设备状态和RPC)

### 5. 传输协议
- **MQTT**: 最常用的IoT协议，支持QoS 0/1
- **CoAP**: 受限设备的轻量协议
- **HTTP**: 简单的请求-响应模式
- **LwM2M**: 设备管理协议
- **SNMP**: 网络管理协议

---

## 数据流转

### 设备遥测数据上报流程
```
设备 → 传输层(MQTT/CoAP/HTTP) → TransportService → 消息队列(Kafka)
→ Rule Engine Consumer → Actor System → Rule Chain处理 → 数据存储/告警/通知
```

### RPC命令下发流程
```
用户/规则引擎 → REST API/Rule Node → DeviceActor → TransportService
→ 消息队列 → 传输层 → 设备
```

---

## 快速开始

### 编译项目
```bash
mvn clean install -DskipTests
```

### 启动单体应用
```bash
cd application
mvn spring-boot:run
```

### Docker部署
```bash
cd docker
docker-compose up -d
```

---

## 学习路径建议

1. **入门阶段**: 阅读 `common/data` 了解核心数据模型
2. **传输层**: 阅读 `common/transport` 了解设备连接机制
3. **规则引擎**: 阅读 `rule-engine` 了解消息处理逻辑
4. **Actor系统**: 阅读 `common/actor` + `application/actors` 了解并发模型
5. **数据层**: 阅读 `dao` 了解数据持久化
6. **API层**: 阅读 `application/controller` 了解REST接口
7. **前端**: 阅读 `ui-ngx` 了解Angular UI

---

## 配套文档

- [各模块详细文档](tb-modules/) - 每个模块的深入分析
- [架构流程图](tb-diagrams/architecture-flow.html) - 可视化系统架构
- [数据流转图](tb-diagrams/canvas-dataflow.html) - 数据流转可视化
- [学习指南](tb-diagrams/learning-guide.html) - 交互式学习指南
- [Mermaid图表](tb-diagrams/mermaid-diagrams.html) - UML/时序图/流程图
