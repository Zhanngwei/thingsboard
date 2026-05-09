# Common 模块学习文档

## 模块概述

`common` 是 ThingsBoard 的基础框架模块集合，提供了整个平台所需的核心抽象、数据模型、消息定义和基础设施。所有上层模块都依赖于 common 中的组件。

---

## 子模块详解

### 1. common/actor — Actor 模型框架

**职责**: 提供轻量级的 Actor 并发模型框架

**核心类:**
- `TbActorSystem` - Actor系统接口，管理所有Actor的创建和通信
- `DefaultTbActorSystem` - 默认实现，基于 Java Executor
- `TbActor` - Actor 接口，定义消息处理方法
- `TbActorMailbox` - Actor 邮箱，缓存待处理消息
- `TbActorRef` - Actor 引用，用于发送消息
- `TbActorId` - Actor 唯一标识

**设计特点:**
- 非阻塞消息传递
- 支持父子层级关系
- 支持高优先级消息
- 支持广播消息

---

### 2. common/data — 核心数据模型

**职责**: 定义平台所有实体的数据模型

**核心实体:**
| 类 | 说明 |
|----|------|
| `Device` | 设备实体 |
| `DeviceProfile` | 设备配置文件 |
| `Asset` | 资产实体 |
| `Tenant` | 租户实体 |
| `Customer` | 客户实体 |
| `User` | 用户实体 |
| `Dashboard` | 仪表盘 |
| `Alarm` | 告警 |
| `EntityView` | 实体视图 |
| `OtaPackage` | OTA升级包 |
| `TbResource` | 平台资源 |

**ID系统 (`id/`):**
- `EntityId` - 实体ID基类(UUID)
- `DeviceId`, `AssetId`, `TenantId` 等具体ID类
- `EntityIdFactory` - ID工厂类

**数据类型 (`kv/`):**
- `KvEntry` - 键值对接口
- `BasicTsKvEntry` - 时序数据条目
- `AttributeKvEntry` - 属性键值对

**查询模型 (`query/`):**
- `EntityDataQuery` - 实体数据查询
- `EntityFilter` - 实体过滤器
- `EntityCountQuery` - 实体计数查询

---

### 3. common/message — 消息模型

**职责**: 定义系统内部所有消息类型

**核心类:**
- `TbMsg` - 规则引擎消息(最核心的消息对象)
- `TbActorMsg` - Actor 消息接口
- `MsgType` - 内部消息类型枚举
- `TbMsgMetaData` - 消息元数据

**消息类型 (MsgType):**
```
PARTITION_CHANGE_MSG        - 分区变更消息
COMPONENT_LIFE_CYCLE_MSG   - 组件生命周期消息
QUEUE_TO_RULE_ENGINE_MSG   - 队列到规则引擎消息
RULE_CHAIN_TO_RULE_MSG     - 规则链到规则节点消息
TRANSPORT_TO_DEVICE_ACTOR_MSG - 传输层到设备Actor消息
DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG - RPC请求消息
```

---

### 4. common/queue — 消息队列抽象

**职责**: 提供消息队列的统一抽象层

**支持的队列实现:**
- Apache Kafka (生产推荐)
- In-Memory Queue (开发测试)

**核心接口:**
- `TbQueueProducer` - 消息生产者
- `TbQueueConsumer` - 消息消费者
- `TbQueueAdmin` - 队列管理
- `TopicPartitionInfo` - 主题分区信息

**队列类型:**
- `TB_CORE` - 核心队列(设备状态、RPC)
- `TB_RULE_ENGINE` - 规则引擎队列(遥测、属性)
- `TB_TRANSPORT_API` - 传输API队列

**Kafka实现 (`kafka/`):**
- `TbKafkaProducerTemplate` - Kafka生产者
- `TbKafkaConsumerTemplate` - Kafka消费者
- `TbKafkaSettings` - Kafka配置

---

### 5. common/transport — 传输层框架

**职责**: 提供设备连接的传输层抽象和默认实现

**核心接口:**
- `TransportService` - 传输服务接口(处理设备认证、遥测上报、属性等)
- `SessionMsgListener` - 会话消息监听器
- `TransportContext` - 传输上下文

**默认实现:**
- `DefaultTransportService` - 传输服务默认实现(68KB大文件!)
  - 设备认证和会话管理
  - 遥测/属性消息编码和发送到队列
  - RPC请求/响应处理

**协议实现:**
- `transport/mqtt/` - MQTT 传输层
- `transport/coap/` - CoAP 传输层
- `transport/http/` - HTTP 传输层
- `transport/lwm2m/` - LwM2M 传输层
- `transport/snmp/` - SNMP 传输层

**限流 (`limits/`):**
- 设备级别限流
- 租户级别限流
- 传输层速率限制

---

### 6. common/cache — 缓存层

**职责**: 提供分布式缓存抽象

**支持:**
- Valkey/Redis 缓存
- 本地缓存(Caffeine)
- 设备会话缓存
- 属性缓存
- 设备配置缓存

---

### 7. common/cluster-api — 集群通信

**职责**: 定义集群节点间的通信接口

**功能:**
- 节点发现
- 服务分区
- 集群消息路由
- 分区再平衡

---

### 8. common/dao-api — 数据访问接口

**职责**: 定义数据访问层的接口(不含实现)

**核心服务接口:**
- `DeviceService` - 设备数据服务
- `TelemetryService` - 遥测数据服务
- `AlarmService` - 告警数据服务
- `RuleChainService` - 规则链数据服务
- `DashboardService` - 仪表盘数据服务
- `AttributesService` - 属性数据服务

---

### 9. common/edqs — 实体数据查询服务

**职责**: 提供高效的实体数据查询能力

**功能:**
- 复杂实体查询
- 数据聚合
- 分页和排序
- 实时查询更新

---

### 10. common/edge-api — 边缘计算API

**职责**: 定义边缘节点与云端的通信协议

**功能:**
- 边缘节点同步
- 事件推送
- 配置下发

---

### 11. common/proto — Protocol Buffers 定义

**职责**: 定义系统内部 gRPC/Protobuf 通信协议

**用途:**
- 传输层与核心服务间通信
- 集群节点间通信
- 消息序列化

---

### 12. common/script — 脚本引擎

**职责**: 提供 JavaScript/TBEL 脚本执行能力

**用途:**
- 规则节点中的脚本执行
- 数据转换脚本
- 过滤脚本

---

### 13. common/stats — 统计框架

**职责**: 提供性能统计和指标收集

**功能:**
- 消息处理计数
- 队列统计
- 性能指标

---

### 14. common/version-control — 版本控制

**职责**: 提供实体配置的Git版本控制

**功能:**
- 规则链版本管理
- 仪表盘版本管理
- 配置导入导出

---

### 15. common/util — 工具集

**职责**: 通用工具类

**包含:**
- JSON处理工具
- 并发工具
- 字符串工具
- 加密工具
