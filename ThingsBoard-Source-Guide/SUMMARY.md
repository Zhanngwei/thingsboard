# ThingsBoard release-3.6 源码解析目录

[首页](README.md) | [HTML 首页](index.html)

> 状态说明：`已完成` 表示调用链、图表、数据库和异常路径均已按当前源码复核；`待分析` 只是全书规划，不代表结论已经形成。

## 第一篇：接入、实体与设备生命周期

- <a id="chapter-01"></a>**01 MQTT 消息进入系统**（已完成）：[Markdown](chapters/01-mqtt-message-ingress/README.md) · [HTML](chapters/01-mqtt-message-ingress/index.html) · [PlantUML](chapters/01-mqtt-message-ingress/sequence.puml) · [时序图 SVG](chapters/01-mqtt-message-ingress/sequence.svg) · [架构图 SVG](assets/architecture/01-mqtt-ingress.svg)
- <a id="chapter-02"></a>**02 Device 创建流程**（已完成）：[Markdown](chapters/02-device-create/README.md) · [HTML](chapters/02-device-create/index.html) · [PlantUML](chapters/02-device-create/sequence.puml) · [时序图 SVG](chapters/02-device-create/sequence.svg) · [架构图 SVG](assets/architecture/02-device-create.svg)
- <a id="chapter-03"></a>**03 Device 删除流程**（已完成）：[Markdown](chapters/03-device-delete/README.md) · [HTML](chapters/03-device-delete/index.html) · [PlantUML](chapters/03-device-delete/sequence.puml) · [时序图 SVG](chapters/03-device-delete/sequence.svg) · [架构图 SVG](assets/architecture/03-device-delete.svg)
- <a id="chapter-04"></a>**04 Device Profile 流程**（已完成）：[Markdown](chapters/04-device-profile/README.md) · [HTML](chapters/04-device-profile/index.html) · [PlantUML](chapters/04-device-profile/sequence.puml) · [时序图 SVG](chapters/04-device-profile/sequence.svg) · [架构图 SVG](assets/architecture/04-device-profile.svg)
- <a id="chapter-05"></a>**05 Rule Chain 执行流程**（待分析）：Rule Chain/Node Actor 创建、关系路由、成功/失败分支和消息确认。
- <a id="chapter-06"></a>**06 Alarm 创建流程**（待分析）：Alarm Rule、去重、状态机、DAO、事件与通知。
- <a id="chapter-07"></a>**07 Alarm 清除流程**（待分析）：clear/ack 状态变化、传播、Rule Engine 回调与数据库更新。
- <a id="chapter-08"></a>**08 Attributes 保存流程**（待分析）：client/shared/server scope、最新值、通知和设备下行。
- <a id="chapter-09"></a>**09 Telemetry 保存流程**（待分析）：所有入口统一后的 `TsKvEntry`、latest/history 双写与订阅通知。
- <a id="chapter-10"></a>**10 RPC 流程**（待分析）：server-side/client-side RPC、会话路由、超时、持久化与状态回报。
- <a id="chapter-11"></a>**11 OTA 流程**（待分析）：固件元数据、包分块、状态遥测、队列和设备协议。
- <a id="chapter-12"></a>**12 Dashboard 流程**（待分析）：Dashboard CRUD、授权、Widget 数据订阅和 WebSocket 查询。
- <a id="chapter-13"></a>**13 Login 流程**（待分析）：认证入口、用户状态、密码策略、审计与 token 签发。
- <a id="chapter-14"></a>**14 JWT 认证流程**（待分析）：Security Filter、token 解析、权限上下文、刷新和失效。

## 第二篇：Actor、队列与集群

- <a id="chapter-15"></a>**15 Actor 模型**（待分析）：ActorSystem、Mailbox、Dispatcher、App/Tenant/Device/Rule Actor 层次。
- <a id="chapter-16"></a>**16 Kafka 发送流程**（待分析）：Producer Provider、TopicPartitionInfo、序列化、回调与生产者配置。
- <a id="chapter-17"></a>**17 Kafka 消费流程**（待分析）：订阅、poll、pack、提交、重试、rebalance 和幂等风险。
- <a id="chapter-18"></a>**18 Queue 管理**（待分析）：Queue/Profile 配置、submit/processing strategy、隔离队列和动态更新。
- <a id="chapter-19"></a>**19 Cluster 通信**（待分析）：服务发现、分区分配、Core/Rule Engine/Transport 通知和远程路由。
- <a id="chapter-20"></a>**20 Cassandra 写入流程**（待分析）：分区键、TTL、异步 driver、latest/history 和 partition registry。
- <a id="chapter-21"></a>**21 TimescaleDB 写入流程**（待分析）：Hypertable、chunk、批量队列、聚合查询和 TTL。
- <a id="chapter-22"></a>**22 PostgreSQL 写入流程**（待分析）：JPA/自定义 JDBC、事务边界、分区表、批处理和连接池。

## 第三篇：协议与会话

- <a id="chapter-23"></a>**23 HTTP 设备 API 流程**（待分析）：token 认证、telemetry/attributes/RPC 与 TransportService。
- <a id="chapter-24"></a>**24 CoAP 消息流程**（待分析）：资源匹配、DTLS、会话上下文、适配与响应。
- <a id="chapter-25"></a>**25 LwM2M 注册与观测流程**（待分析）：Bootstrap、Registration、Observe、对象模型和遥测转换。
- <a id="chapter-26"></a>**26 MQTT Gateway 流程**（待分析）：虚拟设备连接、遥测/属性代理、RPC 路由和会话维护。
- <a id="chapter-27"></a>**27 Device Provision 流程**（待分析）：Provision Profile、密钥/X.509、设备创建和凭据返回。
- <a id="chapter-28"></a>**28 Device Claim 流程**（待分析）：claim secret、客户归属、有效期与并发控制。
- <a id="chapter-29"></a>**29 Session 与 Device State 流程**（待分析）：open/close/activity、在线状态、超时调度和 active 遥测。

## 第四篇：查询、存储与实时推送

- <a id="chapter-30"></a>**30 Telemetry 查询流程**（待分析）：latest/range/aggregation、分页、降采样和后端差异。
- <a id="chapter-31"></a>**31 Timeseries TTL 清理流程**（待分析）：SQL partition、Timescale delete、Cassandra TTL 与租户 TTL。
- <a id="chapter-32"></a>**32 Redis 与本地缓存流程**（待分析）：Cache-Aside、Caffeine/Redis、跨节点失效和一致性窗口。
- <a id="chapter-33"></a>**33 WebSocket 订阅流程**（待分析）：session、subscription command、实时遥测通知和背压。
- <a id="chapter-34"></a>**34 Entity Query 流程**（待分析）：EntityDataQuery、动态 SQL、关系过滤、latest join 和分页。
- <a id="chapter-35"></a>**35 Relation 流程**（待分析）：关系创建、递归查询、方向语义与删除联动。
- <a id="chapter-36"></a>**36 Event 与 Audit Log 流程**（待分析）：调试事件、审计上下文、TTL 与分区。

## 第五篇：平台协作与运行时

- <a id="chapter-37"></a>**37 Rule Node 外部集成流程**（待分析）：HTTP/MQTT/Kafka/邮件节点、异步回调与错误关系。
- <a id="chapter-38"></a>**38 Notification 流程**（待分析）：请求、模板、规则、队列、发送器与状态。
- <a id="chapter-39"></a>**39 Edge 同步流程**（待分析）：Edge Event、gRPC、顺序、冲突和重试。
- <a id="chapter-40"></a>**40 服务启动与关闭流程**（待分析）：Spring 生命周期、schema 安装、Actor/Queue/Transport 初始化顺序。
- <a id="chapter-41"></a>**41 限流与 API Usage 流程**（待分析）：Transport 限流、租户配额、Rule Engine/DB 开关和统计。
- <a id="chapter-42"></a>**42 失败恢复与可观测性**（待分析）：消息重放、死信缺口、指标、日志、debug event 和排障路径。
