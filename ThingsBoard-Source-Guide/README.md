# ThingsBoard release-3.6 源码解析指南

> 源码基线：ThingsBoard `3.6.4`，工作区提交 `0cb411fc90`。本文档只记录能够从当前工作区源码、配置和建表脚本中核实的行为。

[浏览 HTML 知识库](index.html) | [全书目录](SUMMARY.md) | [第 01 章：MQTT 消息进入系统](chapters/01-mqtt-message-ingress/README.md) | [第 02 章：Device 创建流程](chapters/02-device-create/README.md) | [第 03 章：Device 删除流程](chapters/03-device-delete/README.md) | [第 04 章：Device Profile 流程](chapters/04-device-profile/README.md) | [第 05 章：Rule Chain 执行流程](chapters/05-rule-chain-execution/README.md) | [第 06 章：Alarm 创建流程](chapters/06-alarm-create/README.md) | [第 07 章：Alarm 清除流程](chapters/07-alarm-clear/README.md)

## 阅读定位

这套文档面向需要系统阅读 ThingsBoard 服务端源码的 Java/Spring Boot 开发者。每章只分析一个可闭环的核心流程，从入口追踪到队列、Actor、Rule Engine、DAO 和数据库，并明确异步边界、确认语义与失败行为。

文档不以“类逐个翻译”为目标。重点是回答：请求从哪里进入、消息在何处改变形态、线程和服务边界在哪里、什么时候算成功、最终修改了什么状态。

## 固定章节结构

每个流程固定包含以下 13 部分：

1. 流程目标
2. 入口
3. 完整调用链
4. 消息流（Mermaid）
5. 时序图（PlantUML）
6. 数据变化
7. 源码分析
8. Actor 分析
9. Kafka 分析
10. 数据库分析
11. 异常处理
12. 源码阅读路线
13. 常见面试题

## 当前进度

| 编号 | 流程 | 状态 | 文档 |
|---|---|---|---|
| 01 | MQTT 消息进入系统 | 已完成 | [Markdown](chapters/01-mqtt-message-ingress/README.md) / [HTML](chapters/01-mqtt-message-ingress/index.html) / [PlantUML](chapters/01-mqtt-message-ingress/sequence.puml) / [时序图 SVG](chapters/01-mqtt-message-ingress/sequence.svg) / [架构图 SVG](assets/architecture/01-mqtt-ingress.svg) |
| 02 | Device 创建流程 | 已完成 | [Markdown](chapters/02-device-create/README.md) / [HTML](chapters/02-device-create/index.html) / [PlantUML](chapters/02-device-create/sequence.puml) / [时序图 SVG](chapters/02-device-create/sequence.svg) / [架构图 SVG](assets/architecture/02-device-create.svg) |
| 03 | Device 删除流程 | 已完成 | [Markdown](chapters/03-device-delete/README.md) / [HTML](chapters/03-device-delete/index.html) / [PlantUML](chapters/03-device-delete/sequence.puml) / [时序图 SVG](chapters/03-device-delete/sequence.svg) / [架构图 SVG](assets/architecture/03-device-delete.svg) |
| 04 | Device Profile 流程 | 已完成 | [Markdown](chapters/04-device-profile/README.md) / [HTML](chapters/04-device-profile/index.html) / [PlantUML](chapters/04-device-profile/sequence.puml) / [时序图 SVG](chapters/04-device-profile/sequence.svg) / [架构图 SVG](assets/architecture/04-device-profile.svg) |
| 05 | Rule Chain 执行流程 | 已完成 | [Markdown](chapters/05-rule-chain-execution/README.md) / [HTML](chapters/05-rule-chain-execution/index.html) / [PlantUML](chapters/05-rule-chain-execution/sequence.puml) / [时序图 SVG](chapters/05-rule-chain-execution/sequence.svg) / [架构图 SVG](assets/architecture/05-rule-chain-execution.svg) |
| 06 | Alarm 创建流程 | 已完成 | [Markdown](chapters/06-alarm-create/README.md) / [HTML](chapters/06-alarm-create/index.html) / [PlantUML](chapters/06-alarm-create/sequence.puml) / [时序图 SVG](chapters/06-alarm-create/sequence.svg) / [架构图 SVG](assets/architecture/06-alarm-create.svg) |
| 07 | Alarm 清除流程 | 已完成 | [Markdown](chapters/07-alarm-clear/README.md) / [HTML](chapters/07-alarm-clear/index.html) / [PlantUML](chapters/07-alarm-clear/sequence.puml) / [时序图 SVG](chapters/07-alarm-clear/sequence.svg) / [架构图 SVG](assets/architecture/07-alarm-clear.svg) |

完整规划见 [SUMMARY.md](SUMMARY.md)。待分析章节只链接到目录锚点，避免产生指向不存在文件的失效链接。

## 第 01 章关键结论

ThingsBoard 3.6.4 的 MQTT 遥测路径不能简单理解为“MQTT -> Device Actor -> Rule Engine”。连接打开、关闭、订阅和设备 RPC 等会话类消息会走 Core Queue 和 Device Actor；普通设备遥测 `PUBLISH` 则由 `DefaultTransportService` 直接转换为 `TbMsg` 并投递 Rule Engine Queue。队列消费者随后才把 `QueueToRuleEngineMsg` 送入 `AppActor -> TenantActor -> RuleChainActor -> RuleNodeActor`。

另一个必须区分的边界是 MQTT `PUBACK`。在此版本源码中，Transport 回调在 Rule Engine Queue producer 写入成功时返回；它不等待 Save Timeseries 节点完成，更不等价于 `ts_kv`/`ts_kv_latest` 已经持久化。

## 第 02 章关键结论

标准 REST 创建在 `DeviceServiceImpl` 的 Spring 事务中原子保存 `device` 与 `device_credentials`。事务提交后才执行缓存失效、Edge 事件、Transport/Core/Rule Engine Queue 传播和异步审计；这些动作与 PostgreSQL 没有共同事务，也不被 HTTP 请求逐项等待。

Device `CREATED` 生命周期不会主动创建 Device Actor。它经 Rule Engine notification consumer 到达 `AppActor -> TenantActor` 后只投递给已经存在的实体 Actor；首次 session、RPC 或其他 device-aware 消息才触发 `getOrCreateDeviceActor(...)`。

## 第 03 章关键结论

标准 REST 删除在 `DefaultTbDeviceService.delete(Device, User)` 的外层事务中原子删除 originator Alarm、Credentials、Relations、entity-alarm 和 Device 元数据。但 Gateway、Transport、Core State、lifecycle 与 `ENTITY_DELETED` 消息在事务提交前发送，没有 transactional outbox；数据库回滚不能撤销已消费的运行时删除消息。

Device 删除不会自动清理 `attribute_kv`、`ts_kv`、`ts_kv_latest`、Cassandra telemetry 或设备 RPC。3.6.4 的 SQL/Timescale TTL 过程依赖当前 `device` 表反查 tenant/customer 归属，Device 行删除后的 orphan history 可能无法再被该 TTL 路径命中。Lifecycle DELETED 还会 `getOrCreateDeviceActor(...)`，即使 Actor 原先不存在也可能临时创建后立即停止。

## 第 04 章关键结论

标准 Device Profile 保存没有应用层外事务。Profile 行由 `JpaDeviceProfileDao.saveAndFlush(...)` 先提交，随后才同步兼容字段 `device.type`，并向 Transport、lifecycle、OTA、Rule Engine 和 Audit 传播；重命名失败可能留下已提交的 Profile 与部分 Device 更新。

默认 Profile 切换把旧 Profile 的 `is_default` 改为 `false`、再把目标 Profile 改为 `true`，两次 DAO save 属于独立事务，建表脚本也没有“每租户唯一默认”的约束。Transport 收到完整 Profile 后会热更新匹配的在线 Session；Rule Engine 侧通过 Profile cache listener 和 Rule Node self message 刷新 Alarm Rule 状态，系统中不存在独立的 Device Profile Actor。

## 第 05 章关键结论

Rule Chain 本地单目标路由沿用原始 Queue record 的 `TbMsgCallback`；单目标跨分区和多目标 fan-out 会生成新 UUID 的 Queue 消息，父 callback 只等待 producer handoff，不等待子 Rule Chain 执行完成。Singleton Node 跨服务重路由甚至以 `callback=null` 发送后立即 ack 源消息。

默认 Main Queue 使用 `BURST + SKIP_ALL_FAILURES`：一个 poll pack 并发进入 Actor，失败或超时最终 commit。超时 cleanup 后旧 callback 仍然 valid，业务 Node 可能在 offset commit 后继续完成；Retry Strategy 也无法回滚已经提交的数据库或外部副作用。Rule Chain metadata 的 Nodes、relations 和 first node 在一个 PostgreSQL transaction 中更新，但 lifecycle notification 与 Actor reload 在提交后异步发生。

## 第 06 章关键结论

`createAlarm(...)` 实际由 PostgreSQL `create_or_update_active_alarm(...)` 按 `originator_id + type + cleared=false` 创建或更新 active Alarm。已有行通过 `FOR UPDATE` 串行更新，但 active partial index 不是 UNIQUE；首次并发创建时不存在可锁 tuple，因此 3.6 数据库不能强保证只有一条 active Alarm。

Alarm 主体 `alarm`、可见性索引 `entity_alarm`、WebSocket、Alarm notification、Rule Engine action、audit 和 severity comment 没有共同事务。`entity_alarm` 保存失败只记录 warning，传播配置收缩也不会删除旧记录。REST、显式 Create Alarm Node 和 Device Profile Alarm Rule 共用持久化主线，但权限、审计、Actor callback 和下游消息语义不同。

## 第 07 章关键结论

`clear_alarm(...)` 按 `(tenant_id,id) FOR UPDATE` 串行化状态转换：首次调用写 `cleared/clear_ts`，重复调用成功但不修改时间或 details。`JpaAlarmDao` 只把首次清除映射为 `modified=true`，所以 WebSocket 和 `AlarmTrigger` 不会因重复清除再次触发；但 `BaseAlarmService` 仍会为重复调用发布 Edge `ActionEntityEvent`，REST 随后才返回 already-cleared 错误。

REST、显式 Clear Alarm Node、Device Profile 自动清除和 Edge 上行的输出并不等价。只有 REST 生成用户 SYSTEM comment 与 audit；显式节点同时产生 root `ALARM_CLEAR` 和局部 `Cleared`；Device Profile 只产生 `Alarm Cleared` relation；Edge 上行绕过 Subscription，因此没有本地 WS、AlarmTrigger 或 Rule Engine clear 消息。

## 图表约定

- Markdown 中的 `mermaid` 代码块可由支持 Mermaid 的编辑器直接渲染。
- HTML 使用 Mermaid 10 和 markdown-it 的 CDN 版本；直接打开 HTML 即可浏览，首次渲染需要能够访问 CDN。
- 所有章节统一引用 `assets/guide.js`：Mermaid 使用白底深色文字的高对比主题，页面只显示可点击缩略图；点击后以独立 SVG 新窗口打开，不继承正文的 `max-width` 限制。
- PlantUML 时序图保留独立 `.puml` 源文件，便于 JetBrains PlantUML 插件、PlantUML CLI 或 CI 渲染。
- PlantUML 和架构 SVG 在 HTML 中也显示为缩略图，链接必须使用新窗口打开原始 `.svg` 文件，保留浏览器原生缩放和矢量清晰度。

## 版本边界

本教材基于当前 `release-3.6` 工作区，而不是最新版 ThingsBoard。队列类型、时序后端、Device Profile 默认 Rule Chain 和 Rule Engine 节点都可配置；文档会分别标明“源码固定行为”“默认配置”和“部署选择”，不把可选实现写成唯一架构。
