# ThingsBoard release-3.6 源码解析目录

[首页](README.md) | [HTML 首页](index.html)

> 状态说明：`已完成` 表示调用链、图表、数据库和异常路径均已按当前源码复核；`待分析` 只是全书规划，不代表结论已经形成。

## 第一篇：接入、实体与设备生命周期

- <a id="chapter-01"></a>**01 MQTT 消息进入系统**（已完成）：[Markdown](chapters/01-mqtt-message-ingress/README.md) · [HTML](chapters/01-mqtt-message-ingress/index.html) · [PlantUML](chapters/01-mqtt-message-ingress/sequence.puml) · [时序图 SVG](chapters/01-mqtt-message-ingress/sequence.svg) · [架构图 SVG](assets/architecture/01-mqtt-ingress.svg)
- <a id="chapter-02"></a>**02 Device 创建流程**（已完成）：[Markdown](chapters/02-device-create/README.md) · [HTML](chapters/02-device-create/index.html) · [PlantUML](chapters/02-device-create/sequence.puml) · [时序图 SVG](chapters/02-device-create/sequence.svg) · [架构图 SVG](assets/architecture/02-device-create.svg)
- <a id="chapter-03"></a>**03 Device 删除流程**（已完成）：[Markdown](chapters/03-device-delete/README.md) · [HTML](chapters/03-device-delete/index.html) · [PlantUML](chapters/03-device-delete/sequence.puml) · [时序图 SVG](chapters/03-device-delete/sequence.svg) · [架构图 SVG](assets/architecture/03-device-delete.svg)
- <a id="chapter-04"></a>**04 Device Profile 流程**（已完成）：[Markdown](chapters/04-device-profile/README.md) · [HTML](chapters/04-device-profile/index.html) · [PlantUML](chapters/04-device-profile/sequence.puml) · [时序图 SVG](chapters/04-device-profile/sequence.svg) · [架构图 SVG](assets/architecture/04-device-profile.svg)
- <a id="chapter-05"></a>**05 Rule Chain 执行流程**（已完成）：[Markdown](chapters/05-rule-chain-execution/README.md) · [HTML](chapters/05-rule-chain-execution/index.html) · [PlantUML](chapters/05-rule-chain-execution/sequence.puml) · [时序图 SVG](chapters/05-rule-chain-execution/sequence.svg) · [架构图 SVG](assets/architecture/05-rule-chain-execution.svg)
- <a id="chapter-06"></a>**06 Alarm 创建流程**（已完成）：[Markdown](chapters/06-alarm-create/README.md) · [HTML](chapters/06-alarm-create/index.html) · [PlantUML](chapters/06-alarm-create/sequence.puml) · [时序图 SVG](chapters/06-alarm-create/sequence.svg) · [架构图 SVG](assets/architecture/06-alarm-create.svg)
- <a id="chapter-07"></a>**07 Alarm 清除流程**（已完成）：[Markdown](chapters/07-alarm-clear/README.md) · [HTML](chapters/07-alarm-clear/index.html) · [PlantUML](chapters/07-alarm-clear/sequence.puml) · [时序图 SVG](chapters/07-alarm-clear/sequence.svg) · [架构图 SVG](assets/architecture/07-alarm-clear.svg)
- <a id="chapter-08"></a>**08 Attributes 保存流程**（已完成）：[Markdown](chapters/08-attributes-save/README.md) · [HTML](chapters/08-attributes-save/index.html) · [时序图 SVG](chapters/08-attributes-save/sequence.svg) · [架构图 SVG](assets/architecture/08-attributes-save.svg)
- <a id="chapter-09"></a>**09 Telemetry 保存流程**（已完成）：[Markdown](chapters/09-telemetry-save/README.md) · [HTML](chapters/09-telemetry-save/index.html) · [时序图 SVG](chapters/09-telemetry-save/sequence.svg) · [架构图 SVG](assets/architecture/09-telemetry-save.svg)
- <a id="chapter-10"></a>**10 RPC 流程**（已完成）：[Markdown](chapters/10-rpc/README.md) · [HTML](chapters/10-rpc/index.html) · [时序图 SVG](chapters/10-rpc/sequence.svg) · [架构图 SVG](assets/architecture/10-rpc.svg)
- <a id="chapter-11"></a>**11 OTA 流程**（已完成）：[Markdown](chapters/11-ota/README.md) · [HTML](chapters/11-ota/index.html) · [PlantUML](chapters/11-ota/sequence.puml) · [时序图 SVG](chapters/11-ota/sequence.svg) · [架构图 SVG](assets/architecture/11-ota.svg)
- <a id="chapter-12"></a>**12 Dashboard 流程**（已完成）：[Markdown](chapters/12-dashboard/README.md) · [HTML](chapters/12-dashboard/index.html) · [PlantUML](chapters/12-dashboard/sequence.puml) · [时序图 SVG](chapters/12-dashboard/sequence.svg) · [架构图 SVG](assets/architecture/12-dashboard.svg)
- <a id="chapter-13"></a>**13 Login 流程**（已完成）：[Markdown](chapters/13-login/README.md) · [HTML](chapters/13-login/index.html) · [PlantUML](chapters/13-login/sequence.puml) · [时序图 SVG](chapters/13-login/sequence.svg) · [架构图 SVG](assets/architecture/13-login.svg)
- <a id="chapter-14"></a>**14 JWT 认证流程**（已完成）：[Markdown](chapters/14-jwt-authentication/README.md) · [HTML](chapters/14-jwt-authentication/index.html) · [PlantUML](chapters/14-jwt-authentication/sequence.puml) · [时序图 SVG](chapters/14-jwt-authentication/sequence.svg) · [架构图 SVG](assets/architecture/14-jwt-authentication.svg)

## 第二篇：Actor、队列与集群

- <a id="chapter-15"></a>**15 Actor 模型**（已完成）：[Markdown](chapters/15-actor-model/README.md) · [HTML](chapters/15-actor-model/index.html) · [PlantUML](chapters/15-actor-model/sequence.puml) · [时序图 SVG](chapters/15-actor-model/sequence.svg) · [架构图 SVG](assets/architecture/15-actor-model.svg)
- <a id="chapter-16"></a>**16 Kafka 发送流程**（已完成）：[Markdown](chapters/16-kafka-producer/README.md) · [HTML](chapters/16-kafka-producer/index.html) · [PlantUML](chapters/16-kafka-producer/sequence.puml) · [时序图 SVG](chapters/16-kafka-producer/sequence.svg) · [架构图 SVG](assets/architecture/16-kafka-producer.svg)
- <a id="chapter-17"></a>**17 Kafka 消费流程**（已完成）：[Markdown](chapters/17-kafka-consumer/README.md) · [HTML](chapters/17-kafka-consumer/index.html) · [PlantUML](chapters/17-kafka-consumer/sequence.puml) · [时序图 SVG](chapters/17-kafka-consumer/sequence.svg) · [架构图 SVG](assets/architecture/17-kafka-consumer.svg)
- <a id="chapter-18"></a>**18 Queue 管理**（已完成）：[Markdown](chapters/18-queue-management/README.md) · [HTML](chapters/18-queue-management/index.html) · [PlantUML](chapters/18-queue-management/sequence.puml) · [时序图 SVG](chapters/18-queue-management/sequence.svg) · [架构图 SVG](assets/architecture/18-queue-management.svg)
- <a id="chapter-19"></a>**19 Cluster 通信**（已完成）：[Markdown](chapters/19-cluster-communication/README.md) · [HTML](chapters/19-cluster-communication/index.html) · [PlantUML](chapters/19-cluster-communication/sequence.puml) · [时序图 SVG](chapters/19-cluster-communication/sequence.svg) · [架构图 SVG](assets/architecture/19-cluster-communication.svg)
- <a id="chapter-20"></a>**20 Cassandra 写入流程**（已完成）：[Markdown](chapters/20-cassandra-write/README.md) · [HTML](chapters/20-cassandra-write/index.html) · [PlantUML](chapters/20-cassandra-write/sequence.puml) · [时序图 SVG](chapters/20-cassandra-write/sequence.svg) · [架构图 SVG](assets/architecture/20-cassandra-write.svg)
- <a id="chapter-21"></a>**21 TimescaleDB 写入流程**（已完成）：[Markdown](chapters/21-timescale-write/README.md) · [HTML](chapters/21-timescale-write/index.html) · [PlantUML](chapters/21-timescale-write/sequence.puml) · [时序图 SVG](chapters/21-timescale-write/sequence.svg) · [架构图 SVG](assets/architecture/21-timescale-write.svg)
- <a id="chapter-22"></a>**22 PostgreSQL 写入流程**（已完成）：[Markdown](chapters/22-postgresql-write/README.md) · [HTML](chapters/22-postgresql-write/index.html) · [PlantUML](chapters/22-postgresql-write/sequence.puml) · [时序图 SVG](chapters/22-postgresql-write/sequence.svg) · [架构图 SVG](assets/architecture/22-postgresql-write.svg)

## 第三篇：协议与会话

- <a id="chapter-23"></a>**23 HTTP 设备 API 流程**（已完成）：[Markdown](chapters/23-http-device-api/README.md) · [HTML](chapters/23-http-device-api/index.html) · [PlantUML](chapters/23-http-device-api/sequence.puml) · [时序图 SVG](chapters/23-http-device-api/sequence.svg) · [架构图 SVG](assets/architecture/23-http-device-api.svg)
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
