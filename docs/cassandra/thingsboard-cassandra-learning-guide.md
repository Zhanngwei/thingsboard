# 通过 Cassandra 学习 ThingsBoard 架构设计

> 适用对象：熟悉 Java、Spring Boot、MySQL、PostgreSQL，但没有 Cassandra 经验，希望读懂 ThingsBoard 3.6-release 中 Cassandra 时序存储源码的后端开发者。

> 本文档基于当前本地工程 `D:\IoT\thingsboard` 的源码编写。当前工程版本线索来自 `pom.xml`，为 ThingsBoard `3.6.4` / `3.6-release` 系列。本文同时参考了当前 ThingsBoard 官方数据库架构文档：最新官方架构仍然强调 PostgreSQL 始终负责实体、属性、关系、告警和事件，Cassandra 只是可选的高吞吐时序后端。

## 0. 阅读前先建立正确边界

学习 Cassandra 时最容易犯的错误，是把它当成“没有 SQL 的 MySQL”。对 ThingsBoard 来说，Cassandra 也不是替代 PostgreSQL 的万能数据库，而是专门承担一个非常窄但吞吐极高的职责：存储历史遥测时序数据，以及可选地存储最新遥测值。

当前 ThingsBoard 数据层可以先理解成这样：

```text
                         ThingsBoard 应用层
   ┌──────────────────────────────────────────────────────────────┐
   │  Transport / Rule Engine / REST / WebSocket / Dashboard       │
   └──────────────────────────────┬───────────────────────────────┘
                                  │
                ┌─────────────────┴─────────────────┐
                │                                   │
        实体、关系、属性、告警、事件              遥测时序数据
                │                                   │
                ▼                                   ▼
          PostgreSQL 必选                 PostgreSQL / TimescaleDB / Cassandra 可选
```

在本地 3.6.4 源码中：

- `DATABASE_TS_TYPE=sql`：历史时序写入 PostgreSQL `ts_kv`。
- `DATABASE_TS_TYPE=cassandra`：历史时序写入 Cassandra `ts_kv_cf`。
- `DATABASE_TS_LATEST_TYPE=sql`：最新值写入 PostgreSQL `ts_kv_latest`。
- `DATABASE_TS_LATEST_TYPE=cassandra`：最新值写入 Cassandra `ts_kv_latest_cf`。
- `alarm`、`audit_log`、`entity_view`、各类 `*_event` 表仍在 PostgreSQL，不在 Cassandra。

这也是全文的主线：Cassandra 的每个概念都要回答它在 ThingsBoard 时序场景里解决了什么问题，如果换成 PostgreSQL/MySQL 会遇到什么瓶颈，以及源码中哪些类依赖了这个设计。

## 1. Cassandra 基础概念

### 1.1 Cassandra 是什么

Cassandra 是分布式、去中心化、宽列模型的 NoSQL 数据库。它的核心目标不是提供关系模型、Join、复杂事务，而是提供：

- 超高写入吞吐。
- 水平扩展。
- 多副本高可用。
- 跨节点自动分片。
- 面向查询路径的数据建模。

用 MySQL/PostgreSQL 的背景类比：

| 维度 | MySQL/PostgreSQL | Cassandra |
|---|---|---|
| 数据模型 | 表、行、列、外键、Join | Keyspace、Table、Partition、Clustering Row |
| 查询思想 | 先设计规范化模型，再通过索引和 Join 支持多种查询 | 先确定查询，再为查询设计表 |
| 扩展方式 | 单机增强、读写分离、分库分表、分区表 | 原生分布式 Token Ring |
| 写入路径 | WAL + Buffer + Heap/Index 修改 | CommitLog + MemTable + SSTable 顺序写 |
| 一致性 | 单主强一致事务为主 | 可调一致性，常用最终一致性 |
| 适合场景 | OLTP、复杂查询、强事务 | 高写入、按 key/range 查询、海量时序 |
| 不适合场景 | 不适用 | 任意条件查询、Join、跨分区事务、临时分析 |

### 1.2 Cassandra 解决了什么问题

Cassandra 解决的是“单库关系型数据库在写入量、数据量、可用性上同时被打爆”的问题。

ThingsBoard 的时序数据有几个典型特征：

- 写多读少，设备不断上报。
- 单条数据很小，但是数据点数量巨大。
- 查询通常是按实体、遥测 key、时间范围读取。
- 历史数据多数只追加，不需要复杂更新。
- 很多场景允许短暂读不到最新写入，最终能读到即可。
- TTL/保留期天然存在，例如保留 7 天、30 天、1 年。

例如：

```text
50,000 台设备
每台设备每秒上报 5 个 telemetry key

写入量 = 50,000 * 5 = 250,000 data points/s
一天数据点 = 21,600,000,000
```

这个量级如果用单 PostgreSQL 表承载，即使分区、批量写、索引优化都做了，也会逐步遇到：

- WAL 写入压力。
- B-Tree 索引维护压力。
- autovacuum/膨胀问题。
- 历史分区管理复杂度。
- 单机 I/O、CPU、连接数、锁竞争上限。
- 水平扩展需要应用层分片，源码复杂度明显上升。

Cassandra 的选择是牺牲关系型能力，换取“追加写 + 按分区键定位 + 时间范围扫描”的极致吞吐。

### 1.3 Cassandra 与 MySQL/PostgreSQL 的根本区别

关系型数据库的核心是“数据独立性”：你可以先按业务实体做相对规范化的表，再通过 SQL、索引、Join 组合出很多查询。

Cassandra 的核心是“查询路径固定化”：你必须提前知道查询条件，查询条件必须命中 partition key，范围查询通常只能发生在 clustering key 上。

对 ThingsBoard `ts_kv_cf` 来说，它的查询路径非常固定：

```text
给定 entity_type + entity_id + key + time range
读取某段时间内的 telemetry points
```

所以 Cassandra 表直接按这个路径建模：

```sql
PRIMARY KEY ((entity_type, entity_id, key, partition), ts)
```

这里的含义是：

- Partition Key：`entity_type, entity_id, key, partition`
- Clustering Key：`ts`
- 一次查询先定位到“某个设备、某个 key、某个时间桶”的分区。
- 然后在分区内部按 `ts` 做有序范围扫描。

如果用 PostgreSQL，可能是：

```sql
CREATE TABLE ts_kv (
  entity_id uuid NOT NULL,
  key int NOT NULL,
  ts bigint NOT NULL,
  long_v bigint,
  dbl_v double precision,
  PRIMARY KEY (entity_id, key, ts)
) PARTITION BY RANGE (ts);
```

PostgreSQL 能做，但它仍然依赖 B-Tree、分区表、批量写和后台清理去追上时序写入。Cassandra 则从存储引擎开始就把这种访问方式当成主路径。

### 1.4 适用场景与不适用场景

适用场景：

- IoT 遥测历史数据。
- 日志、事件流水、指标监控。
- 用户行为轨迹。
- 按业务 key + 时间范围查询的数据。
- 写入吞吐远大于复杂查询需求的系统。
- 需要多副本高可用、节点故障不中断写入的系统。

不适用场景：

- 订单支付、库存扣减这类强事务。
- 多表 Join 查询。
- 临时分析型 SQL。
- 频繁按任意字段过滤。
- 小数据量系统。
- 开发团队无法接受反规范化和多表冗余的系统。

ThingsBoard 的取舍是混合数据库：

```text
PostgreSQL:
  tenant, customer, user, device, asset, dashboard, relation, alarm, audit_log, event

Cassandra:
  telemetry history: ts_kv_cf
  telemetry latest:  ts_kv_latest_cf 可选
  partition index:   ts_kv_partitions_cf
```

这不是“偏爱 NoSQL”，而是把不同数据形态放到不同数据库。

### 1.5 CAP 理论与 Cassandra 取舍

CAP 不是让你在 C、A、P 里随便选两个，而是在网络分区发生时必须在一致性和可用性之间取舍。

```text
网络正常:
  C / A / P 看起来都能工作

网络分区:
  选择 C: 拒绝部分请求，保证一致
  选择 A: 接收请求，之后修复副本差异
```

Cassandra 更偏 AP：

- 节点故障、网络抖动时仍尽量服务读写。
- 通过多副本、hint、repair、read repair 等机制最终收敛。
- 通过 Consistency Level 在“快”和“准”之间调节。

ThingsBoard 时序数据天然适合这个取舍：

- 设备遥测是事实流，短暂读不到最新数据通常可接受。
- Dashboard 曲线可以稍后刷新。
- 规则链实时处理通常不只依赖数据库读回，而是在消息流中处理。
- 历史数据最终写入并可读，比每次写入都强一致更重要。

如果把 ThingsBoard 的每个 telemetry 写入都强制成 PostgreSQL 强事务：

- 写入延迟会上升。
- 单点数据库压力集中。
- 故障时更容易整体不可写。
- 横向扩展要在应用层做复杂分片。

### 1.6 最终一致性是什么

最终一致性不是“数据可能永远不一致”，而是：

```text
如果没有新的写入继续发生，副本之间最终会通过后台机制收敛到同一个值。
```

在 Cassandra 中，可能出现：

```text
RF=3, CL=ONE

写入 temp=36.5:
  replica A 写成功，客户端收到成功
  replica B/C 暂时没写到或稍后写到

随后读取:
  读到 A: 得到 36.5
  读到 B: 可能暂时读不到
  后台 hint/read repair/repair 后 B/C 收敛
```

ThingsBoard 默认 Cassandra read/write consistency 是 `ONE`。这在 `application/src/main/resources/thingsboard.yml` 中可以看到：

```yaml
cassandra:
  query:
    read_consistency_level: "${CASSANDRA_READ_CONSISTENCY_LEVEL:ONE}"
    write_consistency_level: "${CASSANDRA_WRITE_CONSISTENCY_LEVEL:ONE}"
```

这背后的含义是：系统优先吞吐和可用性，允许遥测历史数据短暂最终一致。

### 1.7 为什么 Cassandra 能支持超大规模时序数据

原因可以从四层理解：

```text
应用建模层:
  按 entity + key + time bucket 查询，避免 Join 和全局二级索引

分布式层:
  partition key hash 到 token ring，数据自动分布到多个节点

存储引擎层:
  写入先追加 commit log + memtable，之后 flush 成不可变 SSTable

一致性层:
  CL=ONE/LOCAL_QUORUM 等可调，避免所有写入都等待全部副本
```

ThingsBoard 的 Cassandra 设计基本就是这四层的组合：

- `ts_kv_cf` 把设备 key 的某个时间桶作为一个 partition。
- `ts` 作为 clustering key，天然适合时间范围扫描。
- `TS_KV_PARTITIONING=MONTHS` 默认按月切桶，避免单个 partition 无限变宽。
- `ts_kv_partitions_cf` 记录哪些时间桶存在，减少读取时查询空桶。
- 写入使用异步 DAO 和 Cassandra query buffer，避免业务线程直接被 I/O 拖死。

## 2. 架构原理

### 2.1 Cluster、Data Center、Rack、Node

Cassandra 的拓扑层级：

```text
Cluster
└── Data Center: dc1
    ├── Rack: rack1
    │   ├── Node A
    │   └── Node B
    └── Rack: rack2
        ├── Node C
        └── Node D
```

概念说明：

- Cluster：一个 Cassandra 集群，所有节点共同服务一组 keyspace。
- Data Center：数据中心，通常对应一个机房、一个云可用区组合、或一个逻辑地域。
- Rack：机架或故障域，用来避免副本都落在同一物理风险点。
- Node：实际 Cassandra 进程，保存部分 token range 和副本。

ThingsBoard 本地配置里默认：

```yaml
cassandra:
  url: "${CASSANDRA_URL:127.0.0.1:9042}"
  local_datacenter: "${CASSANDRA_LOCAL_DATACENTER:datacenter1}"
```

这说明 ThingsBoard Java Driver 会使用本地数据中心感知路由。单机开发时不明显，生产多 DC 时它决定优先访问哪个 DC。

### 2.2 Token Ring

Cassandra 使用 token ring 把数据分散到节点。默认 partitioner 是 Murmur3Partitioner，分区键会被 hash 成 token。

```text
                 token ring

             (-2^63)
                ▲
                │
        Node A  │  Node B
          ┌─────┴─────┐
          │           │
          │           │
          └─────┬─────┘
        Node C  │  Node D
                │
                ▼
             (2^63-1)
```

写入一条 ThingsBoard telemetry：

```text
partition key = (DEVICE, device_uuid, temperature, 2026-06-01 00:00:00)
token = hash(partition key)
token 落在哪个 range，就由哪个节点作为主副本范围负责
```

如果 RF=3，则这条数据不只在一个节点上保存，而是保存在 3 个副本节点上。

### 2.3 Gossip 协议

Gossip 是 Cassandra 节点之间传播集群状态的机制。节点会持续交换：

- 谁在线。
- 谁离线。
- 节点地址。
- schema 版本。
- token 信息。
- 节点负载状态。

你可以把它理解成 Cassandra 集群自己的“服务发现 + 心跳 + 元数据传播”。ThingsBoard 不直接操作 Gossip，但它依赖 driver 发现集群拓扑和节点状态。

生产中如果 Gossip 状态异常，ThingsBoard 可能表现为：

- 某些 Cassandra query 超时。
- driver 认为节点不可用。
- token metadata 不更新。
- `nodetool status` 状态不稳定。

### 2.4 Partitioner

Partitioner 决定如何把 partition key 转成 token。现代 Cassandra 默认使用 Murmur3Partitioner。

对 ThingsBoard 最关键的是：partition key 中每个字段都会影响 hash。

`ts_kv_cf` 的 partition key：

```text
(entity_type, entity_id, key, partition)
```

这意味着：

- 同一个设备同一个 telemetry key 的同一个时间桶在一个 Cassandra partition 中。
- 不同 key 分散到不同 partition。
- 不同月份/天/小时分散到不同 partition。
- 不同设备天然分散。

如果 partition key 设计成 `(tenant_id, partition)`，大量设备同一月份都会挤到同一个超宽分区，热点风险极高。ThingsBoard 没有这样做。

### 2.5 Replication Factor

Replication Factor，简称 RF，表示每份数据保存几个副本。

```text
RF=1:
  一份数据只有一个副本，节点坏了就不可读/不可写对应范围

RF=3:
  一份数据保存到 3 个节点，常见生产配置
```

本地安装脚本的 keyspace 是开发默认：

```sql
CREATE KEYSPACE IF NOT EXISTS thingsboard
WITH replication = {
  'class' : 'SimpleStrategy',
  'replication_factor' : 1
};
```

生产更推荐：

```sql
CREATE KEYSPACE thingsboard
WITH replication = {
  'class': 'NetworkTopologyStrategy',
  'datacenter1': 3
};
```

为什么不建议生产使用 `SimpleStrategy`：

- 它不了解 DC/Rack 拓扑。
- 容易把副本放在不理想的故障域。
- 多 DC 场景无法独立设置每个 DC 的副本数。

### 2.6 Consistency Level

Consistency Level，简称 CL，决定一次读写需要多少副本响应才算成功。

常见 CL：

| CL | 含义 | ThingsBoard 遥测场景 |
|---|---|---|
| ONE | 任意 1 个副本成功 | 最高吞吐，默认配置 |
| TWO | 任意 2 个副本成功 | 比 ONE 稳，但延迟更高 |
| THREE | 任意 3 个副本成功 | RF 至少 3 时可用 |
| QUORUM | 多数副本成功 | RF=3 时为 2 |
| LOCAL_QUORUM | 本 DC 多数副本成功 | 多 DC 常用 |
| ALL | 全部副本成功 | 最强但最慢，可用性最低 |

RF=3 时：

```text
QUORUM = floor(3 / 2) + 1 = 2
```

如果读写都用 QUORUM，那么：

```text
write quorum 2 + read quorum 2 > RF 3
```

理论上能避免读到旧值，但代价是每次读写至少等待多数副本。

ThingsBoard 默认用 ONE，是因为 telemetry 数据更关心吞吐和可用性。告警状态、租户、设备、用户等强业务数据仍在 PostgreSQL，不交给 Cassandra。

### 2.7 3 节点集群如何工作

假设：

- Node A、B、C。
- RF=3。
- CL=ONE。

```text
ThingsBoard
   │
   │ INSERT telemetry
   ▼
Coordinator Node A
   ├── Replica A 写入成功
   ├── Replica B 异步/并行写入
   └── Replica C 异步/并行写入

CL=ONE:
  任意一个副本确认后，客户端即可收到成功
```

如果 Node C 宕机：

```text
写入:
  A/B 仍可成功，C 的写入可由 hint 记录，C 恢复后补写

读取:
  CL=ONE 时只要读到 A 或 B 即可
  CL=QUORUM 时 A+B 可满足
  CL=ALL 时失败
```

这就是 ThingsBoard 高吞吐 telemetry 的可用性来源。

### 2.8 5 节点集群如何工作

假设：

- Node A/B/C/D/E。
- RF=3。
- 每条 partition 按 token 落到其中连续或策略选择的 3 个副本。

```text
token range:

  A       B       C       D       E
  |-------|-------|-------|-------|

partition p1 -> replicas: A, B, C
partition p2 -> replicas: C, D, E
partition p3 -> replicas: E, A, B
```

增加节点后，集群通过 token range 重新分布数据，吞吐能力随节点增加而提升。ThingsBoard 应用层不需要知道某个设备在哪个节点，driver 和 Cassandra 负责路由。

这比应用层分库分表更简单：

```text
PostgreSQL/MySQL 分片:
  应用需要知道 device_id -> 哪个库/表
  跨分片查询和迁移复杂

Cassandra:
  partitioner + token ring 管理分布
  应用只提供 partition key
```

代价是 Cassandra 查询能力受限：你不能随便按 `tenant_id`、`type`、`severity` 组合查 telemetry，必须按表设计好的 key 查询。

## 3. 数据模型

### 3.1 Keyspace

Keyspace 类似 MySQL/PostgreSQL 中的 database/schema，但它更重要，因为它定义 replication 策略。

本地 ThingsBoard Cassandra keyspace：

```sql
CREATE KEYSPACE IF NOT EXISTS thingsboard
WITH replication = {
  'class' : 'SimpleStrategy',
  'replication_factor' : 1
};
```

生产建议：

```sql
CREATE KEYSPACE IF NOT EXISTS thingsboard
WITH replication = {
  'class': 'NetworkTopologyStrategy',
  'datacenter1': 3
}
AND durable_writes = true;
```

ThingsBoard 使用：

- `cassandra.keyspace_name` 默认 `thingsboard`。
- `CassandraCluster` 从配置读取 keyspace 并初始化 session。
- DAO 查询表时直接使用 `ModelConstants.TS_KV_CF` 等常量。

如果用 PostgreSQL：

- 副本和高可用不在 schema 层定义。
- 需要 streaming replication、Patroni、主从、分片中间件等额外体系。

Cassandra 的收益是复制和分片是数据库原生能力；代价是 schema 设计必须服从分布式查询约束。

### 3.2 Table

Cassandra table 不是关系表。它更像“按某个查询模式组织好的有序 KV 集合”。

ThingsBoard 历史时序表：

```sql
CREATE TABLE IF NOT EXISTS thingsboard.ts_kv_cf (
    entity_type text,
    entity_id timeuuid,
    key text,
    partition bigint,
    ts bigint,
    bool_v boolean,
    str_v text,
    long_v bigint,
    dbl_v double,
    json_v text,
    PRIMARY KEY (( entity_type, entity_id, key, partition ), ts)
);
```

这个表只服务一个核心查询：

```sql
SELECT key, ts, bool_v, str_v, long_v, dbl_v, json_v
FROM ts_kv_cf
WHERE entity_type = ?
  AND entity_id = ?
  AND key = ?
  AND partition = ?
  AND ts >= ?
  AND ts < ?
ORDER BY ts DESC
LIMIT ?;
```

它不适合：

- 查某个 tenant 下所有设备最新温度。
- 查所有温度大于 80 的设备。
- 按 `long_v` 排序。
- Join device 表过滤设备类型。

这类需求要么通过 ThingsBoard 上层服务和 PostgreSQL 实体查询组合，要么通过规则链、告警、外部分析系统完成。

### 3.3 Partition Key

Partition Key 决定数据分布和查询入口。

`ts_kv_cf`：

```text
Partition Key = (entity_type, entity_id, key, partition)
Clustering Key = ts
```

ASCII 视图：

```text
Partition: (DEVICE, dev-1, temperature, 2026-06)

  ts=2026-06-01 00:00:00 -> 22.1
  ts=2026-06-01 00:00:10 -> 22.2
  ts=2026-06-01 00:00:20 -> 22.4
  ...
```

为什么包含 `partition`：

- 不包含 `partition` 时，一个设备一个 key 的所有历史都落在一个 Cassandra partition。
- 设备运行几年后，这个 partition 会变成极宽分区。
- 极宽分区会导致读放大、compaction 压力、内存压力、修复慢。

为什么包含 `key`：

- 一个设备可能有很多 telemetry key。
- 温度、湿度、电压、电流各自查询频率不同。
- 按 key 拆分后，读取 temperature 不会扫描 humidity。

如果用 PostgreSQL B-Tree：

```sql
PRIMARY KEY (entity_id, key, ts)
```

它也能按 `entity_id + key + ts range` 查，但所有写入都要维护 B-Tree，且数据分布主要受单库/分区策略限制。Cassandra 则通过 partition key 自动散列到集群节点。

最终 ThingsBoard 选择 Cassandra partition key 的原因：

- 查询路径非常稳定。
- 避免跨设备、跨 key、跨时间桶扫描。
- 让写入天然水平分布。
- 通过时间桶控制分区宽度。

### 3.4 Clustering Key

Clustering Key 决定 partition 内部排序。

`ts_kv_cf` 的 clustering key 是 `ts`，这意味着同一个 partition 内部按时间排序。

```text
Partition Key:
  (DEVICE, dev-1, temperature, 2026-06)

Clustering Rows:
  ts asc/desc ordered rows
```

范围查询只需要：

```sql
AND ts >= ?
AND ts < ?
ORDER BY ts ASC|DESC
```

这正好对应 dashboard 曲线：

- 查询某个设备的某个 key。
- 指定时间窗口。
- 指定最大返回数量。
- 可选聚合。

如果 clustering key 不是 `ts`，ThingsBoard 读历史曲线就会变成扫描后过滤，完全不符合 Cassandra 设计。

### 3.5 Composite Key

Cassandra primary key 由 partition key 和 clustering key 组成：

```sql
PRIMARY KEY ((a, b, c), d, e)
```

含义：

- `(a, b, c)` 是复合 partition key。
- `d, e` 是 clustering key。

ThingsBoard：

```sql
PRIMARY KEY (( entity_type, entity_id, key, partition ), ts)
```

这不是联合唯一索引的简单类比，而是同时决定：

- 数据在哪些节点。
- 分区边界在哪里。
- 分区内如何排序。
- 查询必须提供哪些条件。

### 3.6 Static Column

Static Column 是 Cassandra 中属于整个 partition 的列，而不是属于某一行 clustering row 的列。

示例：

```sql
CREATE TABLE device_metric_by_day (
  device_id uuid,
  day text,
  ts bigint,
  firmware text static,
  temperature double,
  PRIMARY KEY ((device_id, day), ts)
);
```

同一个 `(device_id, day)` partition 下所有 ts 行共享 `firmware`。

ThingsBoard `ts_kv_cf` 没有使用 static column，原因是 telemetry row 本身已经足够小，且实体属性、设备元数据在 PostgreSQL/attribute_kv 中管理。把设备静态属性塞进 Cassandra telemetry partition 会带来冗余和一致性问题。

### 3.7 Collection

Cassandra 支持 `list`、`set`、`map` 等 collection。

示例：

```sql
CREATE TABLE device_tags (
  device_id uuid PRIMARY KEY,
  tags set<text>,
  thresholds map<text, double>
);
```

ThingsBoard telemetry 表没有使用 collection，原因是：

- telemetry 是一条条时间点数据，适合 append。
- collection 更新会让单行变复杂。
- 大 collection 会形成隐藏的宽行问题。
- 查询和 TTL 管理不如单点 row 清晰。

### 3.8 为什么 Cassandra 没有传统 Join

Cassandra 不支持分布式 Join，不是功能没做完，而是设计目标决定的。

Join 通常意味着：

```text
查表 A -> 得到一批 key -> 去表 B 查 -> 合并 -> 过滤 -> 排序
```

在分布式数据库中，如果 A/B 的数据分布在不同节点，这会导致：

- 跨节点大量网络传输。
- 查询协调节点压力过大。
- 延迟不可预测。
- 分区故障时语义复杂。

ThingsBoard 的做法：

- 设备、资产、客户、关系等复杂实体查询放在 PostgreSQL。
- telemetry 只按 entity id 和 key 查询。
- 上层服务先用 PostgreSQL 确定实体集合，再对 telemetry 发起确定的 key/range 查询。

这就是混合架构的意义：让 PostgreSQL 做关系查询，让 Cassandra 做海量时序读写。

### 3.9 为什么 Cassandra 强调根据查询设计表

在 MySQL/PostgreSQL 中，你可能这样设计：

```text
device(id, tenant_id, type)
telemetry(id, device_id, key, ts, value)

查询时 join device + telemetry
```

在 Cassandra 中，要先问：

```text
我需要支持哪些查询？
查询条件是否能完整给出 partition key？
范围过滤是否只落在 clustering key 上？
返回数据是否来自一个或少数几个 partition？
```

ThingsBoard `ts_kv_cf` 的答案是：

```text
查询：按 entity + key + time range 查询 telemetry
Partition Key：entity_type + entity_id + key + partition
Clustering Key：ts
```

这就是“查询即模型”。

代价：

- 如果新需求要按 value 查，需要新表或外部搜索/分析系统。
- 如果要按 tenant 聚合所有设备，不能直接扫 Cassandra 全表。
- 数据可能冗余，例如 latest 单独一张表。

收益：

- 热路径极快。
- 写入路径稳定。
- 水平扩展简单。

## 4. CQL 语法

这一章不是完整 CQL 手册，而是覆盖阅读 ThingsBoard Cassandra schema 和 DAO 所必需的语法。

### 4.1 CREATE KEYSPACE

开发环境：

```sql
CREATE KEYSPACE IF NOT EXISTS thingsboard
WITH replication = {
  'class': 'SimpleStrategy',
  'replication_factor': 1
};
```

生产单 DC：

```sql
CREATE KEYSPACE IF NOT EXISTS thingsboard
WITH replication = {
  'class': 'NetworkTopologyStrategy',
  'datacenter1': 3
}
AND durable_writes = true;
```

生产多 DC：

```sql
CREATE KEYSPACE IF NOT EXISTS thingsboard
WITH replication = {
  'class': 'NetworkTopologyStrategy',
  'dc_cn': 3,
  'dc_us': 3
}
AND durable_writes = true;
```

注意点：

- `durable_writes=true` 表示使用 commit log，生产不要关闭。
- RF 和 CL 是一起设计的。
- 修改 keyspace RF 后需要 repair，确保新副本有数据。

### 4.2 CREATE TABLE

ThingsBoard 历史时序表：

```sql
CREATE TABLE IF NOT EXISTS thingsboard.ts_kv_cf (
    entity_type text,
    entity_id timeuuid,
    key text,
    partition bigint,
    ts bigint,
    bool_v boolean,
    str_v text,
    long_v bigint,
    dbl_v double,
    json_v text,
    PRIMARY KEY (( entity_type, entity_id, key, partition ), ts)
);
```

ThingsBoard latest 表：

```sql
CREATE TABLE IF NOT EXISTS thingsboard.ts_kv_latest_cf (
    entity_type text,
    entity_id timeuuid,
    key text,
    ts bigint,
    bool_v boolean,
    str_v text,
    long_v bigint,
    dbl_v double,
    json_v text,
    PRIMARY KEY (( entity_type, entity_id ), key)
) WITH compaction = { 'class' : 'LeveledCompactionStrategy' };
```

ThingsBoard partition 目录表：

```sql
CREATE TABLE IF NOT EXISTS thingsboard.ts_kv_partitions_cf (
    entity_type text,
    entity_id timeuuid,
    key text,
    partition bigint,
    PRIMARY KEY (( entity_type, entity_id, key ), partition)
) WITH CLUSTERING ORDER BY ( partition ASC )
  AND compaction = { 'class' : 'LeveledCompactionStrategy' };
```

设计解读：

- `ts_kv_cf` 存实际历史值。
- `ts_kv_latest_cf` 存最新值，避免 dashboard 最新值查询每次扫历史。
- `ts_kv_partitions_cf` 存某个 entity/key 有哪些时间桶，避免读时间范围时对不存在的桶发查询。

### 4.3 ALTER TABLE

添加列：

```sql
ALTER TABLE thingsboard.ts_kv_cf ADD quality text;
```

修改 compaction：

```sql
ALTER TABLE thingsboard.ts_kv_cf
WITH compaction = {
  'class': 'TimeWindowCompactionStrategy',
  'compaction_window_unit': 'DAYS',
  'compaction_window_size': '1'
};
```

注意：

- Cassandra 添加普通列通常很轻量。
- 修改 primary key 不支持；需要新建表并迁移数据。
- 对 ThingsBoard 核心表不要随意改 compaction，除非你明确知道读写模式和版本兼容性。

### 4.4 INSERT

插入历史 telemetry：

```sql
INSERT INTO thingsboard.ts_kv_cf
(entity_type, entity_id, key, partition, ts, dbl_v)
VALUES (
  'DEVICE',
  11111111-1111-1111-1111-111111111111,
  'temperature',
  1780272000000,
  1780315200000,
  36.5
);
```

ThingsBoard DAO 中对应：

```java
INSERT INTO ts_kv_cf(entity_type,entity_id,key,partition,ts,dbl_v)
VALUES(?, ?, ?, ?, ?, ?)
```

如果 `set_null_values_enabled=true`，DAO 会写入所有 value 列，把非目标类型设为 null：

```sql
INSERT INTO ts_kv_cf
(entity_type, entity_id, key, partition, ts, bool_v, str_v, long_v, dbl_v, json_v)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
```

这样做的原因是同一个 `(entity,key,partition,ts)` 如果以前写过 long，后来同 ts 写 double，可以清掉旧类型列，避免一行多值类型混乱。

### 4.5 UPDATE

Cassandra 的 `UPDATE` 与 `INSERT` 都是 upsert。不存在的行会被创建。

```sql
UPDATE thingsboard.ts_kv_latest_cf
SET ts = 1780315200000,
    dbl_v = 36.5,
    str_v = null,
    long_v = null,
    bool_v = null,
    json_v = null
WHERE entity_type = 'DEVICE'
  AND entity_id = 11111111-1111-1111-1111-111111111111
  AND key = 'temperature';
```

ThingsBoard 实际用 `INSERT` 保存 latest，语义仍然是 upsert。

注意：

- Cassandra 更新不是原地修改，会写新版本，旧版本靠 compaction 清理。
- 高频覆盖同一行可能产生更多 compaction 压力。
- `ts_kv_latest_cf` 是覆盖写，量比历史写少，但仍要关注写热点。

### 4.6 DELETE

删除某个时间范围：

```sql
DELETE FROM thingsboard.ts_kv_cf
WHERE entity_type = 'DEVICE'
  AND entity_id = 11111111-1111-1111-1111-111111111111
  AND key = 'temperature'
  AND partition = 1780272000000
  AND ts >= 1780310000000
  AND ts < 1780315200000;
```

Cassandra delete 会产生 tombstone，不是立即物理删除。大量删除加范围查询容易产生 tombstone scan。

ThingsBoard 源码中 `CassandraBaseTimeseriesDao.remove()` 会：

1. 根据 start/end 算出分区范围。
2. 从 `ts_kv_partitions_cf` 取已有分区。
3. 对每个分区发 `DELETE FROM ts_kv_cf ... ts >= ? AND ts < ?`。
4. 如需删除 latest，`CassandraBaseTimeseriesLatestDao.removeLatest()` 会判断 latest ts 是否落在删除区间。

### 4.7 SELECT

正确查询：

```sql
SELECT key, ts, bool_v, str_v, long_v, dbl_v, json_v
FROM thingsboard.ts_kv_cf
WHERE entity_type = 'DEVICE'
  AND entity_id = 11111111-1111-1111-1111-111111111111
  AND key = 'temperature'
  AND partition = 1780272000000
  AND ts >= 1780310000000
  AND ts < 1780315200000
ORDER BY ts DESC
LIMIT 1000;
```

错误或危险查询：

```sql
-- 缺少 partition key，不允许或性能极差
SELECT * FROM thingsboard.ts_kv_cf WHERE key = 'temperature';

-- 按 value 查，不符合表设计
SELECT * FROM thingsboard.ts_kv_cf WHERE dbl_v > 80;

-- ALLOW FILTERING 会把问题推给集群，不适合生产热路径
SELECT * FROM thingsboard.ts_kv_cf WHERE key = 'temperature' ALLOW FILTERING;
```

### 4.8 TTL

插入时设置 TTL：

```sql
INSERT INTO thingsboard.ts_kv_cf
(entity_type, entity_id, key, partition, ts, dbl_v)
VALUES ('DEVICE', 11111111-1111-1111-1111-111111111111, 'temperature', 1780272000000, 1780315200000, 36.5)
USING TTL 2592000;
```

`2592000` 秒约等于 30 天。

ThingsBoard 配置：

```yaml
cassandra:
  query:
    ts_key_value_ttl: "${TS_KV_TTL:0}"
```

源码逻辑：

- `CassandraBaseTimeseriesDao.computeTtl()` 会把系统 TTL 作为上限。
- 如果 rule node 或消息 metadata 设置了更短 TTL，取更短者。
- `cleanup(long systemTtl)` 对 Cassandra 是空实现，因为清理由 Cassandra TTL 原生完成。

PostgreSQL 对比：

- PostgreSQL 需要后台任务删除过期数据或 drop partition。
- 删除产生 WAL、索引更新、vacuum 压力。
- Cassandra TTL 写入时进入存储引擎，过期后通过 tombstone/compaction 清理。

代价：

- TTL 过短且写入量大时会产生大量 tombstone。
- 查询时间范围覆盖大量过期但未 compact 的数据时，读延迟可能升高。

### 4.9 BATCH

Cassandra BATCH 示例：

```sql
BEGIN BATCH
  INSERT INTO thingsboard.ts_kv_cf
  (entity_type, entity_id, key, partition, ts, dbl_v)
  VALUES ('DEVICE', 11111111-1111-1111-1111-111111111111, 'temperature', 1780272000000, 1780315200000, 36.5);

  INSERT INTO thingsboard.ts_kv_latest_cf
  (entity_type, entity_id, key, ts, dbl_v)
  VALUES ('DEVICE', 11111111-1111-1111-1111-111111111111, 'temperature', 1780315200000, 36.5);
APPLY BATCH;
```

注意：Cassandra BATCH 不是批量导入优化工具。跨 partition 的大 batch 会给 coordinator 带来额外压力。

ThingsBoard 没有用一个 Cassandra BATCH 同时写 `ts_kv_cf`、`ts_kv_latest_cf`、`ts_kv_partitions_cf`，而是发异步写：

```text
BaseTimeseriesService.save()
  -> timeseriesDao.savePartition()
  -> timeseriesDao.save()
  -> timeseriesLatestDao.saveLatest()
```

这说明 ThingsBoard 接受三者之间短暂不一致，换取更好的吞吐和更简单的失败隔离。

### 4.10 INDEX

创建二级索引：

```sql
CREATE INDEX idx_ts_kv_dbl_v ON thingsboard.ts_kv_cf (dbl_v);
```

但对 ThingsBoard telemetry 大表通常不建议这样做。

原因：

- `dbl_v` 高基数，写入量巨大。
- 二级索引也要分布式维护。
- 查询 `dbl_v > 80` 仍然可能跨大量节点。
- 时序热写场景下索引会拖慢写入。

如果你需要“查所有温度大于 80 的设备”，更合适的方案通常是：

- 在规则链中实时判断并生成 alarm。
- 把数据同步到 Elasticsearch/ClickHouse/Druid 等分析系统。
- 为特定查询新建 Cassandra 反向表，但要承担冗余写和一致性成本。

## 5. 存储引擎原理

### 5.1 从一次写入开始

Cassandra 写入路径：

```text
Client / ThingsBoard
    │
    │ INSERT
    ▼
Coordinator Node
    │
    ├── 根据 partition key 计算 token
    ├── 找到 replica 节点
    └── 向副本发送 mutation
             │
             ▼
       Replica Node
             │
             ├── append CommitLog
             ├── write MemTable
             └── 按 CL 返回 ack
                         │
                         ▼
                  MemTable flush
                         │
                         ▼
                    immutable SSTable
                         │
                         ▼
                     Compaction 合并
```

ThingsBoard telemetry 写入路径：

```text
Transport 接收遥测
  -> Rule Engine / Save Timeseries
  -> BaseTimeseriesService.save()
     -> CassandraBaseTimeseriesDao.savePartition()
     -> CassandraBaseTimeseriesDao.save()
     -> CassandraBaseTimeseriesLatestDao.saveLatest()
  -> CassandraAbstractDao.executeAsyncWrite()
  -> CassandraBufferedRateWriteExecutor
  -> DataStax Java Driver
  -> Cassandra
```

### 5.2 Commit Log

Commit Log 是 Cassandra 的 WAL。每次写入先追加到 commit log，用于宕机恢复。

与 PostgreSQL WAL 类似点：

- 都提供崩溃恢复能力。
- 都是顺序追加。

不同点：

- PostgreSQL 还要维护 heap page、B-Tree index、MVCC visibility。
- Cassandra 写入主要是 commit log append + memtable，磁盘 SSTable 后续顺序生成。

ThingsBoard 大量 telemetry 写入非常依赖这个 append-friendly 模型。它不需要每次写入都更新多个复杂索引，也不需要 Join 约束检查。

### 5.3 MemTable

MemTable 是内存中的写缓冲，按 key 有序组织。

写入成功后，数据可能还没落到 SSTable，但已经在：

- CommitLog：保证恢复。
- MemTable：支持读到最新数据。

MemTable 达到阈值后 flush 成 SSTable。

对 ThingsBoard 的意义：

- 高频写入可以先进入内存结构。
- 磁盘写入被转换成批量顺序 flush。
- 适合“海量小点位”的遥测写入。

### 5.4 SSTable

SSTable 是不可变的磁盘文件。MemTable flush 后生成 SSTable，之后不会原地修改。

```text
SSTable
├── Data.db             数据
├── Partitions.db       partition index
├── Rows.db             wide partition row index
├── Summary.db          partition index sample
├── Filter.db           Bloom filter
├── Statistics.db       tombstone/TTL/compaction 等统计
└── CompressionInfo.db  压缩块信息
```

不可变的好处：

- 写入不随机覆盖旧页。
- 适合顺序 I/O。
- 并发读写冲突少。
- compaction 后批量清理旧版本和 tombstone。

代价：

- 更新和删除会产生新版本/tombstone。
- 读可能要合并多个 SSTable。
- compaction 是必须管理的后台成本。

### 5.5 Bloom Filter

Bloom Filter 用来判断某个 partition key “可能在这个 SSTable 中”或“一定不在”。

查询 ThingsBoard 某个 partition：

```text
(DEVICE, dev-1, temperature, 2026-06)
```

Cassandra 会对多个 SSTable 判断：

```text
SSTable A Filter: 可能存在 -> 继续查 index/data
SSTable B Filter: 一定不存在 -> 跳过
SSTable C Filter: 可能存在 -> 继续查
```

这对 telemetry 很重要，因为同一个表会产生大量 SSTable。Bloom Filter 可以减少不必要磁盘读取。

### 5.6 Partition Index 与 Partition Summary

Partition Index 记录 partition key 到磁盘位置的映射。Partition Summary 是 index 的采样，用来更快定位 index 区间。

读取流程简化：

```text
partition key
  -> Bloom Filter 判断 SSTable 是否可能包含
  -> Partition Summary 定位 Index 范围
  -> Partition Index 找到 Data.db offset
  -> 读取目标 partition 和 clustering range
```

ThingsBoard 的建模让 partition key 非常精确，所以这些结构能发挥作用。如果查询不带完整 partition key，就无法走这条高效路径。

### 5.7 Compaction

Compaction 把多个 SSTable 合并，清理旧版本和 tombstone。

```text
SSTable 1 + SSTable 2 + SSTable 3
             │
             ▼
        Compaction
             │
             ▼
        SSTable 4
```

ThingsBoard 表里：

- `ts_kv_partitions_cf` 和 `ts_kv_latest_cf` 使用 `LeveledCompactionStrategy`。
- `ts_kv_cf` 在本地 schema 中未显式指定 compaction，使用 Cassandra 默认策略。

生产中可选方案：

- 历史时序按时间写入，很多团队会评估 `TimeWindowCompactionStrategy`。
- latest 表是覆盖更新，`LeveledCompactionStrategy` 有利于读最新值。

但不要机械套用。compaction 选择要结合：

- 读写比例。
- TTL。
- 分区粒度。
- Cassandra 版本。
- 磁盘空间余量。

### 5.8 Tombstone

Tombstone 是删除标记。Cassandra 删除数据或 TTL 过期时，不会立即从所有 SSTable 物理移除，而是写入 tombstone，等 compaction 清理。

```text
ts=100 value=36.5  in SSTable A
ts=100 tombstone   in SSTable B

读取时合并:
  tombstone 覆盖旧值

compaction 后:
  旧值和 tombstone 在安全条件满足后一起清理
```

ThingsBoard 中 tombstone 来源：

- telemetry TTL 过期。
- 用户删除 telemetry 时间范围。
- latest 被删除。
- 同一行不同类型值覆盖时旧列变 null。

常见问题：

- TTL 很短，但查询范围很大。
- 删除大量历史区间。
- compaction 跟不上写入和过期速度。
- 宽分区中有大量 tombstone，查询延迟急剧上升。

### 5.9 为什么 Cassandra 写入性能极高

核心原因：

```text
写入路径短:
  append commit log + write memtable

磁盘友好:
  flush 生成顺序 SSTable，不频繁随机更新旧页

索引克制:
  主要按 partition key + clustering key 组织，不维护大量二级索引

分布式:
  partition key hash 到多个节点，写入随节点增加而分散

一致性可调:
  CL=ONE 不等待全部副本
```

与 PostgreSQL 对比：

```text
PostgreSQL telemetry insert:
  WAL
  heap page insert
  primary key index insert
  secondary index insert
  partition routing
  autovacuum/visibility map 后续维护

Cassandra telemetry insert:
  commit log append
  memtable write
  later flush SSTable
```

所以在 ThingsBoard 百万级 telemetry 写入场景下，Cassandra 的存储模型与业务形态更匹配。

## 6. 分区与热点问题

### 6.1 Token 如何计算

简化过程：

```text
partition key columns
  -> 序列化成 bytes
  -> Murmur3 hash
  -> token
  -> token range
  -> replica nodes
```

ThingsBoard 示例：

```text
partition key = ('DEVICE', device_uuid, 'temperature', 1780272000000)
token = murmur3(partition key bytes)
```

`partition` 字段是时间桶起始时间戳。默认 `MONTHS` 时，2026 年 6 月内的 timestamp 会被截断到 2026-06-01 00:00:00 UTC 对应毫秒。

源码：

```java
long toPartitionTs(long ts) {
    LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC);
    return tsFormat.truncatedTo(time).toInstant(ZoneOffset.UTC).toEpochMilli();
}
```

### 6.2 数据如何分布到节点

假设：

```text
dev-1 temperature 2026-06 -> token 100 -> Node A/B/C
dev-1 humidity    2026-06 -> token 250 -> Node B/C/D
dev-2 temperature 2026-06 -> token 780 -> Node D/E/A
```

同一个设备不同 key 会分散，同一个 key 不同时间桶也会分散。

这就是 ThingsBoard schema 中同时包含 `key` 和 `partition` 的原因。

### 6.3 什么是热点分区

热点分区是某个 partition 被远高于其他 partition 的读写集中访问。

ThingsBoard 例子：

```text
一个网关设备代表 100,000 个子设备上报
所有数据都写成 entity_id = gateway-1, key = telemetry, partition = current_month

结果:
  (DEVICE, gateway-1, telemetry, 2026-06) 成为超级热点
```

症状：

- 少数节点 CPU 高。
- p99 写延迟高。
- `nodetool tablestats` 某表 partition size 异常。
- read latency/write latency 在少数节点上升。
- compaction backlog 集中。

避免方式：

- 不要把大量物理设备汇总成一个 Cassandra entity。
- 对超高频 key 调整 partitioning 为 `DAYS` 或 `HOURS`。
- 将不同指标拆成不同 telemetry key。
- 在业务上拆分虚拟设备。
- 对极端高频流使用消息队列/流处理先聚合降采样。

### 6.4 什么是宽分区

宽分区是一个 partition 中 clustering rows 过多。

`ts_kv_cf` 中：

```text
Partition = (entity_type, entity_id, key, partition)
Rows = 这个时间桶内的每个 ts
```

如果一个设备每秒写 100 个 `temperature` 点，默认按月：

```text
100 points/s * 86400 * 30 = 259,200,000 rows/month
```

这个 partition 过宽，读取、repair、compaction 都会痛苦。

解决：

```yaml
TS_KV_PARTITIONING=DAYS
```

甚至：

```yaml
TS_KV_PARTITIONING=HOURS
```

取舍：

- 时间桶越小，单 partition 越健康。
- 但一次查询跨越的 partition 数越多。
- `ts_kv_partitions_cf` 可以降低查询空桶成本，但不能消除跨桶查询成本。

### 6.5 实际案例：默认 MONTHS 什么时候不够

案例：

```text
设备 A 是高频电表网关
key = raw_current
频率 = 200 points/s
默认 partition = MONTHS

单月 rows = 200 * 86400 * 30 = 518,400,000 rows
```

问题：

- 单 partition 极宽。
- 查询最近 1 小时仍可能需要在巨大 partition 内定位。
- compaction 和 repair 成本高。

优化方案：

```text
方案 A: TS_KV_PARTITIONING=DAYS
  单 partition rows = 17,280,000

方案 B: TS_KV_PARTITIONING=HOURS
  单 partition rows = 720,000

方案 C: 上游聚合，raw_current 高频原始数据只保留 7 天，分钟聚合保留 1 年
```

如果我是 ThingsBoard 架构师：

- 默认 `MONTHS` 是为了兼顾普通设备和查询 fan-out。
- 极高频设备必须允许用户通过配置调小时间桶。
- 再极端的场景，应引入流处理和冷/热分层，不把所有原始点都长期压在 Cassandra。

## 7. 一致性与故障恢复

### 7.1 写一致性

写一致性决定写入多少副本成功后返回客户端。

RF=3：

```text
CL=ONE:
  1 个副本成功即可，吞吐最高

CL=QUORUM:
  2 个副本成功，读写一致性更强

CL=ALL:
  3 个副本都成功，任何副本不可用都会导致失败
```

ThingsBoard 默认 `write_consistency_level=ONE`。

原因：

- telemetry 写入量巨大。
- 数据可最终一致。
- 数据库不可用比短暂副本不一致更影响业务。
- 强业务状态不放 Cassandra。

### 7.2 读一致性

读一致性决定读多少副本后返回。

ThingsBoard 默认 `read_consistency_level=ONE`。

对 dashboard 来说：

- 读到稍旧数据通常可以接受。
- 刷新后可能读到更新结果。
- 聚合曲线更关注总体趋势。

如果你把读写都调成 `QUORUM`：

收益：

- 更不容易读到旧值。

代价：

- 读写延迟上升。
- 节点抖动时超时更多。
- 集群可用性下降。
- 同样硬件下吞吐下降。

### 7.3 ONE、TWO、THREE、QUORUM、LOCAL_QUORUM、ALL

举例 RF=3：

```text
ONE          1/3
TWO          2/3
THREE        3/3
QUORUM       2/3
ALL          3/3
```

多 DC：

```text
dc1 RF=3, dc2 RF=3

LOCAL_QUORUM:
  只等本地 DC 的 2 个副本

QUORUM:
  等全局 6 个副本中的 4 个，跨 DC 延迟更高
```

ThingsBoard 多 DC 如果部署本地读写，通常考虑 `LOCAL_QUORUM` 而不是全局 `QUORUM`。但 telemetry 默认仍可能使用 `ONE` 以优先吞吐。

### 7.4 Hint Handoff

Hint Handoff 用于节点短暂不可用时的补写。

```text
写入 p1，replicas A/B/C
C 宕机
A 作为 coordinator:
  A/B 写成功
  为 C 记录 hint
C 恢复:
  A 将 hint replay 给 C
```

ThingsBoard 场景：

- 某 Cassandra 节点滚动重启。
- CL=ONE 写入仍成功。
- 节点恢复后通过 hint 补齐短暂缺失。

注意：

- hint 不等于长期备份。
- 节点离线太久仍需要 repair。

### 7.5 Read Repair

Read Repair 在读取多个副本时发现不一致，会修复旧副本。

```text
read QUORUM:
  Replica A: ts=100 value=36.5
  Replica B: ts=100 missing
  返回新值，同时修复 B
```

ThingsBoard 默认 CL=ONE 时，读修复触发机会有限。生产一致性要求更高的场景需要配合 repair 策略。

### 7.6 Anti Entropy Repair

Anti Entropy Repair 是主动比较副本数据并修复差异的运维动作。

常用命令：

```bash
nodetool repair thingsboard ts_kv_cf
```

生产注意：

- repair 消耗 I/O、网络、CPU。
- 大表 repair 要规划窗口。
- 多节点分批 repair。
- 长期不 repair 会增加副本分歧风险。

### 7.7 生产案例：节点故障

场景：

```text
5 节点 Cassandra
RF=3
ThingsBoard CL=ONE
Node C 磁盘故障下线 20 分钟
```

表现：

- telemetry 写入继续成功。
- 少数读取可能缺最新值。
- Node C 恢复后 hint replay。
- 如果离线超过 hint 保留窗口，执行 repair。

如果使用 PostgreSQL 单主：

- 主库故障需要 failover。
- failover 期间写入中断或丢连接。
- 分区/索引压力仍集中在主库。

Cassandra 收益：

- 单节点故障不影响整体写入。
- 副本机制原生支持。

代价：

- 需要理解副本修复。
- 最终一致性需要业务接受。

## 8. 运维与排障

### 8.1 cqlsh 常用命令

连接：

```bash
cqlsh 127.0.0.1 9042
```

基础查看：

```sql
DESCRIBE KEYSPACES;
USE thingsboard;
DESCRIBE TABLE ts_kv_cf;
DESCRIBE TABLE ts_kv_latest_cf;
DESCRIBE TABLE ts_kv_partitions_cf;
```

设置一致性：

```sql
CONSISTENCY ONE;
CONSISTENCY QUORUM;
```

查询 latest：

```sql
SELECT *
FROM ts_kv_latest_cf
WHERE entity_type = 'DEVICE'
  AND entity_id = 11111111-1111-1111-1111-111111111111
LIMIT 20;
```

查询历史：

```sql
SELECT key, ts, dbl_v
FROM ts_kv_cf
WHERE entity_type = 'DEVICE'
  AND entity_id = 11111111-1111-1111-1111-111111111111
  AND key = 'temperature'
  AND partition = 1780272000000
  AND ts >= 1780310000000
  AND ts < 1780315200000
ORDER BY ts DESC
LIMIT 100;
```

开启 tracing：

```sql
TRACING ON;
SELECT key, ts, dbl_v
FROM ts_kv_cf
WHERE entity_type = 'DEVICE'
  AND entity_id = 11111111-1111-1111-1111-111111111111
  AND key = 'temperature'
  AND partition = 1780272000000
  AND ts >= 1780310000000
  AND ts < 1780315200000
LIMIT 10;
TRACING OFF;
```

### 8.2 nodetool 常用命令

集群状态：

```bash
nodetool status
nodetool info
nodetool describecluster
```

线程池和积压：

```bash
nodetool tpstats
```

表统计：

```bash
nodetool tablestats thingsboard.ts_kv_cf
nodetool tablestats thingsboard.ts_kv_latest_cf
nodetool tablestats thingsboard.ts_kv_partitions_cf
```

compaction：

```bash
nodetool compactionstats
nodetool compactionhistory
```

网络流：

```bash
nodetool netstats
```

flush：

```bash
nodetool flush thingsboard ts_kv_cf
```

repair：

```bash
nodetool repair thingsboard ts_kv_cf
```

清理不再属于本节点的 token 数据：

```bash
nodetool cleanup thingsboard ts_kv_cf
```

### 8.3 查看集群状态

`nodetool status` 示例：

```text
Datacenter: datacenter1
=======================
Status=Up/Down
|/ State=Normal/Leaving/Joining/Moving
--  Address     Load       Tokens  Owns  Host ID                               Rack
UN  10.0.0.11   800 GB     256     ?     aaaa                                  rack1
UN  10.0.0.12   790 GB     256     ?     bbbb                                  rack1
UN  10.0.0.13   805 GB     256     ?     cccc                                  rack2
```

关注：

- `UN` 是正常。
- `DN` 表示 Down。
- Load 是否明显倾斜。
- 节点是否处于 Joining/Leaving 太久。

### 8.4 查看副本状态

复合 partition key 的 token 查询不如单 key 直观。实际排障时更常用：

```sql
SELECT token(entity_type, entity_id, key, partition)
FROM ts_kv_cf
WHERE entity_type = 'DEVICE'
  AND entity_id = 11111111-1111-1111-1111-111111111111
  AND key = 'temperature'
  AND partition = 1780272000000
LIMIT 1;
```

然后结合：

```bash
nodetool ring
```

或用应用日志、driver tracing 判断 coordinator 和 replica。

### 8.5 查看表统计

重点看：

```bash
nodetool tablestats thingsboard.ts_kv_cf
```

关注指标：

- `Read Latency`
- `Write Latency`
- `SSTable count`
- `Space used`
- `Number of partitions`
- `Average partition size`
- `Maximum partition size`
- `Tombstone drop time`
- `Dropped Mutations`

如果 `Maximum partition size` 特别大，优先怀疑宽分区。对 ThingsBoard 来说，检查是否有高频设备/key，是否需要调小 `TS_KV_PARTITIONING`。

### 8.6 查看 Compaction

```bash
nodetool compactionstats
```

如果 pending compactions 持续很高：

- 写入超过磁盘 compaction 能力。
- TTL/tombstone 太多。
- 磁盘 I/O 不足。
- compaction 策略不适合当前读写模式。

ThingsBoard 表现：

- 写入延迟升高。
- 查询 p99 抖动。
- Cassandra 日志出现 timeout。
- ThingsBoard `CassandraBufferedRateWriteExecutor` 队列积压。

### 8.7 查看 Tombstone

手段：

- Cassandra 日志中的 tombstone warning。
- `nodetool tablestats` 中 tombstone 相关统计。
- cqlsh tracing 查看扫描行数。
- 分析 TTL 和删除模式。

ThingsBoard 常见来源：

```text
短 TTL + 高频写入
大范围 DELETE telemetry
latest 表频繁覆盖/删除
```

处理：

- 避免短 TTL 查询跨大范围历史。
- 对短保留高频数据使用更小时间桶。
- 避免批量删除大量历史，优先 TTL 或按分区清理。
- 确保 compaction 有足够 I/O。

### 8.8 性能分析流程

线上 telemetry 查询慢时：

```text
1. 确认 ThingsBoard 查询条件
   entity_id? key? 时间范围? aggregation? limit?

2. 算 Cassandra partition 范围
   startTs/endTs -> MONTHS/DAYS/HOURS buckets

3. 查 ts_kv_partitions_cf
   是否分区数量异常多?

4. 对单个 partition 用 cqlsh tracing
   是否 tombstone 多? SSTable 多? replica 慢?

5. 看 nodetool tablestats
   partition size、SSTable count、latency

6. 看 compactionstats/tpstats
   是否 compaction backlog 或 read/write executor 积压?

7. 回到业务
   是否高频设备? 是否查询时间窗口太大? 是否需要降采样?
```

写入慢时：

```text
1. 看 ThingsBoard 日志中的 Cassandra query queue 统计
2. 看 CASSANDRA_QUERY_CONCURRENT_LIMIT / BUFFER_SIZE
3. 看 nodetool tpstats 是否 MutationStage 积压
4. 看磁盘 I/O、commitlog 盘、data 盘
5. 看 compactionstats 是否持续积压
6. 看是否出现少数热点 partition
```

### 8.9 常见故障案例

案例一：热点设备导致少数节点高负载

```text
症状:
  Node B/C CPU 高，写延迟高

定位:
  tablestats 显示 maximum partition size 异常
  业务发现某网关把所有子设备数据写到同一 entity/key

解决:
  拆分 entity
  调小 TS_KV_PARTITIONING
  上游聚合
```

案例二：TTL 导致 tombstone storm

```text
症状:
  查询最近 7 天慢，日志 tombstone warning

原因:
  TTL=1天，但 dashboard 查询 30 天历史，扫描大量已过期未 compact 数据

解决:
  限制 dashboard 时间窗口
  调整 TTL 与查询窗口一致
  确保 compaction 跟上
```

案例三：compaction backlog

```text
症状:
  写入吞吐下降，p99 延迟升高
  nodetool compactionstats pending 很高

解决:
  提升磁盘 I/O
  检查 compaction throughput
  分析表 compaction 策略
  降低写入峰值或增加节点
```

案例四：节点离线后数据不一致

```text
症状:
  节点恢复后部分历史查询偶尔缺点

原因:
  节点离线超过 hint 窗口

解决:
  对 keyspace/table 执行 repair
  后续缩短节点离线时间，规划滚动维护
```

## 9. ThingsBoard 专题

### 9.1 ThingsBoard 为什么支持 Cassandra

官方数据库架构把 ThingsBoard 存储分成两层：

```text
PostgreSQL:
  entities, attributes, relations, alarms, events

Time-series backend:
  PostgreSQL or Cassandra
```

ThingsBoard 支持 Cassandra 的原因不是“Cassandra 比 PostgreSQL 更高级”，而是 telemetry 的数据形态适合 Cassandra：

- 高写入。
- 小行。
- append。
- 按 entity/key/time 查询。
- TTL。
- 可最终一致。
- 需要横向扩展。

如果只使用 PostgreSQL：

- 中小规模部署更简单。
- 强一致和 SQL 能力更好。
- 但写入吞吐到 5K-10K data points/s 以上时需要非常谨慎的批量写、分区、硬件和运维。

如果使用 Cassandra：

- 大规模 telemetry 写入能力更强。
- 存储更适合时序。
- 但查询模型受限，运维复杂度更高。

### 9.2 PostgreSQL 与 Cassandra 职责划分

当前本地源码真实表：

| 数据 | PostgreSQL | Cassandra |
|---|---|---|
| tenant/customer/user | 是 | 否 |
| device/asset/entity relation | 是 | 否 |
| attributes | 是 | 否 |
| alarm | 是 | 否 |
| audit_log | 是 | 否 |
| rule/debug/lifecycle/error event | 是 | 否 |
| edge_event | 是 | 否 |
| historical telemetry | `ts_kv` 可选 | `ts_kv_cf` 可选 |
| latest telemetry | `ts_kv_latest` 可选 | `ts_kv_latest_cf` 可选 |
| telemetry partition directory | SQL 分区元数据/表结构 | `ts_kv_partitions_cf` |

`application/src/main/resources/thingsboard.yml`：

```yaml
database:
  ts:
    type: "${DATABASE_TS_TYPE:sql}"
  ts_latest:
    type: "${DATABASE_TS_LATEST_TYPE:sql}"
```

这说明 latest 和 history 可以独立配置。实践中也可以出现：

```text
history: Cassandra
latest: PostgreSQL
```

或者：

```text
history: Cassandra
latest: Cassandra
```

### 9.3 时序数据如何存储

一条 telemetry：

```json
{
  "temperature": 36.5,
  "humidity": 61
}
```

进入 ThingsBoard 后，按 key 拆成多个 `TsKvEntry`：

```text
entity = DEVICE/dev-1
ts = 1780315200000
key = temperature, dbl_v = 36.5
key = humidity, long_v = 61
```

保存 historical telemetry：

```text
ts_kv_cf:
  entity_type = DEVICE
  entity_id   = dev-1 uuid
  key         = temperature
  partition   = month/day/hour bucket start
  ts          = original timestamp
  dbl_v       = 36.5
```

保存 latest：

```text
ts_kv_latest_cf:
  entity_type = DEVICE
  entity_id   = dev-1 uuid
  key         = temperature
  ts          = latest timestamp
  dbl_v       = 36.5
```

保存 partition directory：

```text
ts_kv_partitions_cf:
  entity_type = DEVICE
  entity_id   = dev-1 uuid
  key         = temperature
  partition   = 2026-06 bucket
```

### 9.4 ts_kv / ts_kv_cf 表结构设计

用户常说 `ts_kv`，但在 Cassandra 中真实表名是 `ts_kv_cf`。二者是不同后端下的同一概念。

PostgreSQL：

```sql
CREATE TABLE IF NOT EXISTS ts_kv
(
    entity_id uuid   NOT NULL,
    key       int    NOT NULL,
    ts        bigint NOT NULL,
    bool_v    boolean,
    str_v     varchar(10000000),
    long_v    bigint,
    dbl_v     double precision,
    json_v    json,
    CONSTRAINT ts_kv_pkey PRIMARY KEY (entity_id, key, ts)
) PARTITION BY RANGE (ts);
```

Cassandra：

```sql
CREATE TABLE IF NOT EXISTS thingsboard.ts_kv_cf (
    entity_type text,
    entity_id timeuuid,
    key text,
    partition bigint,
    ts bigint,
    bool_v boolean,
    str_v text,
    long_v bigint,
    dbl_v double,
    json_v text,
    PRIMARY KEY (( entity_type, entity_id, key, partition ), ts)
);
```

关键差异：

| 设计点 | PostgreSQL `ts_kv` | Cassandra `ts_kv_cf` |
|---|---|---|
| key 类型 | int，来自 `ts_kv_dictionary` | text，直接存 telemetry key |
| 时间分区 | PostgreSQL table partition by `ts` | Cassandra partition key 包含时间桶 |
| 查询入口 | B-Tree `(entity_id,key,ts)` | Cassandra partition key + clustering `ts` |
| 扩展方式 | 单库分区/Timescale/外部分片 | token ring 原生分布 |
| TTL | 后台清理/drop partition | Cassandra TTL |

为什么 Cassandra 里 `key` 用 text：

- 避免在 Cassandra 热写路径上查 SQL dictionary。
- 每条 telemetry 可以独立写入。
- 代价是 key 字符串重复占用空间。

为什么 PostgreSQL 里 `key` 用 int：

- B-Tree 索引更小。
- SQL 查询和存储更节省。
- 代价是需要 `ts_kv_dictionary` 维护映射。

### 9.5 ts_kv_latest / ts_kv_latest_cf 表结构设计

PostgreSQL：

```sql
CREATE TABLE IF NOT EXISTS ts_kv_latest
(
    entity_id uuid   NOT NULL,
    key       int    NOT NULL,
    ts        bigint NOT NULL,
    bool_v    boolean,
    str_v     varchar(10000000),
    long_v    bigint,
    dbl_v     double precision,
    json_v    json,
    CONSTRAINT ts_kv_latest_pkey PRIMARY KEY (entity_id, key)
);
```

Cassandra：

```sql
CREATE TABLE IF NOT EXISTS thingsboard.ts_kv_latest_cf (
    entity_type text,
    entity_id timeuuid,
    key text,
    ts bigint,
    bool_v boolean,
    str_v text,
    long_v bigint,
    dbl_v double,
    json_v text,
    PRIMARY KEY (( entity_type, entity_id ), key)
) WITH compaction = { 'class' :  'LeveledCompactionStrategy'  };
```

设计含义：

```text
Partition Key = (entity_type, entity_id)
Clustering Key = key
```

读取某个设备全部 latest：

```sql
SELECT key, ts, str_v, bool_v, long_v, dbl_v, json_v
FROM ts_kv_latest_cf
WHERE entity_type = ?
  AND entity_id = ?;
```

读取某个 key latest：

```sql
SELECT key, ts, str_v, bool_v, long_v, dbl_v, json_v
FROM ts_kv_latest_cf
WHERE entity_type = ?
  AND entity_id = ?
  AND key = ?;
```

为什么 latest 单独成表：

- dashboard、规则和 API 经常查最新值。
- 如果每次查 latest 都扫 `ts_kv_cf ORDER BY ts DESC LIMIT 1`，需要知道最近 partition，还要处理空桶。
- latest 表把查询变成一次精确 partition lookup。

代价：

- 每个 telemetry 多一次写。
- history 和 latest 可能短暂不一致。
- 删除历史时要考虑 latest 是否需要重写。

源码中 `CassandraBaseTimeseriesLatestDao.removeLatest()` 如果删除区间覆盖当前 latest 且 `rewriteLatestIfDeleted=true`，会回查历史找新的 latest。

### 9.6 ts_kv_partitions_cf 设计

真实结构：

```sql
CREATE TABLE IF NOT EXISTS thingsboard.ts_kv_partitions_cf (
    entity_type text,
    entity_id timeuuid,
    key text,
    partition bigint,
    PRIMARY KEY (( entity_type, entity_id, key ), partition)
) WITH CLUSTERING ORDER BY ( partition ASC )
  AND compaction = { 'class' :  'LeveledCompactionStrategy'  };
```

用途：

```text
给定 entity + key + time range
先查这个 key 真实存在的 partitions
再只查询存在的 ts_kv_cf partition
```

没有这张表会怎样：

```text
查询 1 年数据，partitioning=DAYS
需要构造 365 个 day bucket
其中很多 bucket 可能没有数据
仍然会对 Cassandra 发空查询
```

有 `ts_kv_partitions_cf`：

```text
先查 directory:
  dev-1 temperature 有数据的 day bucket = [day1, day2, day9]

再查 ts_kv_cf:
  只查这 3 个 partition
```

代价：

- 每个 telemetry 至少多一次 partition 记录写入。
- ThingsBoard 用 `CassandraTsPartitionsCache` 避免同一个 partition 重复写太多。
- directory 表与实际数据之间可能短暂不一致，但可接受。

### 9.7 entity_view

用户提到 `entity_views`，本地真实表名是 `entity_view`，在 PostgreSQL 中：

```sql
CREATE TABLE IF NOT EXISTS entity_view (
    id uuid NOT NULL CONSTRAINT entity_view_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    entity_id uuid,
    entity_type varchar(255),
    tenant_id uuid,
    customer_id uuid,
    type varchar(255),
    name varchar(255),
    keys varchar(10000000),
    start_ts bigint,
    end_ts bigint,
    additional_info varchar,
    external_id uuid,
    CONSTRAINT entity_view_external_id_unq_key UNIQUE (tenant_id, external_id)
);
```

它不是 Cassandra 表。Entity View 是对真实 entity 的视图约束：

- 允许暴露部分 telemetry keys。
- 限制 start/end 时间范围。
- 用于客户/用户访问控制和数据窗口裁剪。

源码：

```text
BaseTimeseriesService.findAllByQueries()
  if entityId.getEntityType() == ENTITY_VIEW:
    entityViewService.findEntityViewById()
    filter keys
    update start/end time
    timeseriesDao.findAllAsync(real entity id, updated queries)
```

也就是说：

```text
entity_view 元数据在 PostgreSQL
entity_view 指向的 telemetry 仍在 ts_kv_cf 或 ts_kv
```

为什么不放 Cassandra：

- Entity View 是强业务元数据。
- 需要租户、客户、权限、唯一约束、管理界面查询。
- PostgreSQL 更适合。

### 9.8 event

ThingsBoard 3.6.4 没有一个单独叫 `event` 的 Cassandra 表。事件相关表在 PostgreSQL，并按时间分区。

规则节点调试事件：

```sql
CREATE TABLE IF NOT EXISTS rule_node_debug_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL ,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar,
    e_type varchar,
    e_entity_id uuid,
    e_entity_type varchar,
    e_msg_id uuid,
    e_msg_type varchar,
    e_data_type varchar,
    e_relation_type varchar,
    e_data varchar,
    e_metadata varchar,
    e_error varchar
) PARTITION BY RANGE (ts);
```

规则链调试事件：

```sql
CREATE TABLE IF NOT EXISTS rule_chain_debug_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar NOT NULL,
    e_message varchar,
    e_error varchar
) PARTITION BY RANGE (ts);
```

统计事件：

```sql
CREATE TABLE IF NOT EXISTS stats_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar NOT NULL,
    e_messages_processed bigint NOT NULL,
    e_errors_occurred bigint NOT NULL
) PARTITION BY RANGE (ts);
```

生命周期事件：

```sql
CREATE TABLE IF NOT EXISTS lc_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar NOT NULL,
    e_type varchar NOT NULL,
    e_success boolean NOT NULL,
    e_error varchar
) PARTITION BY RANGE (ts);
```

错误事件：

```sql
CREATE TABLE IF NOT EXISTS error_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar NOT NULL,
    e_method varchar NOT NULL,
    e_error varchar
) PARTITION BY RANGE (ts);
```

索引：

```sql
CREATE INDEX IF NOT EXISTS idx_rule_node_debug_event_main
    ON rule_node_debug_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST) WITH (FILLFACTOR=95);
```

为什么事件不用 Cassandra：

- 事件查询通常按 tenant/entity/service/time 组合过滤。
- 调试事件需要管理端分页、过滤、清理。
- 事件吞吐通常低于 telemetry。
- 官方架构也把 events 放在 PostgreSQL，可选独立 PostgreSQL 实例承载事件库。

### 9.9 alarm

真实结构：

```sql
CREATE TABLE IF NOT EXISTS alarm (
    id uuid NOT NULL CONSTRAINT alarm_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    ack_ts bigint,
    clear_ts bigint,
    additional_info varchar,
    end_ts bigint,
    originator_id uuid,
    originator_type integer,
    propagate boolean,
    severity varchar(255),
    start_ts bigint,
    assign_ts bigint DEFAULT 0,
    assignee_id uuid,
    tenant_id uuid,
    customer_id uuid,
    propagate_relation_types varchar,
    type varchar(255),
    propagate_to_owner boolean,
    propagate_to_tenant boolean,
    acknowledged boolean,
    cleared boolean
);
```

相关映射表：

```sql
CREATE TABLE IF NOT EXISTS entity_alarm (
    tenant_id uuid NOT NULL,
    entity_type varchar(32),
    entity_id uuid NOT NULL,
    created_time bigint NOT NULL,
    alarm_type varchar(255) NOT NULL,
    customer_id uuid,
    alarm_id uuid,
    CONSTRAINT entity_alarm_pkey PRIMARY KEY (entity_id, alarm_id),
    CONSTRAINT fk_entity_alarm_id FOREIGN KEY (alarm_id) REFERENCES alarm(id) ON DELETE CASCADE
);
```

索引：

```sql
CREATE INDEX IF NOT EXISTS idx_alarm_originator_alarm_type ON alarm(originator_id, type, start_ts DESC);
CREATE INDEX IF NOT EXISTS idx_alarm_tenant_created_time ON alarm(tenant_id, created_time DESC);
CREATE INDEX IF NOT EXISTS idx_alarm_originator_alarm_type_active
    ON alarm USING btree (originator_id, type) WHERE cleared = false;
```

为什么 alarm 不放 Cassandra：

- 告警有 ack/clear/assign 状态变更。
- 需要按 tenant、originator、type、severity、active 状态过滤。
- 需要外键关联和删除级联。
- 管理端分页查询复杂。

PostgreSQL 更适合这类业务状态表。Cassandra 更适合不可变或少更新的海量时序事实。

### 9.10 audit_log

真实结构：

```sql
CREATE TABLE IF NOT EXISTS audit_log (
    id uuid NOT NULL,
    created_time bigint NOT NULL,
    tenant_id uuid,
    customer_id uuid,
    entity_id uuid,
    entity_type varchar(255),
    entity_name varchar(255),
    user_id uuid,
    user_name varchar(255),
    action_type varchar(255),
    action_data varchar(1000000),
    action_status varchar(255),
    action_failure_details varchar(1000000)
) PARTITION BY RANGE (created_time);
```

索引：

```sql
CREATE INDEX IF NOT EXISTS idx_audit_log_tenant_id_and_created_time
ON audit_log(tenant_id, created_time DESC);
```

为什么 audit log 仍在 PostgreSQL：

- 查询维度围绕 tenant/user/entity/action/time。
- 数据量比 telemetry 小。
- 管理端分页和筛选更像业务查询。
- 官方架构支持把 events/audit log 放到独立 PostgreSQL，而不是 Cassandra。

### 9.11 Cassandra DAO 层真实实现思路

关键接口和实现：

```text
common/dao-api/.../TimeseriesService.java
dao/.../timeseries/BaseTimeseriesService.java
dao/.../timeseries/TimeseriesDao.java
dao/.../timeseries/TimeseriesLatestDao.java
dao/.../timeseries/CassandraBaseTimeseriesDao.java
dao/.../timeseries/CassandraBaseTimeseriesLatestDao.java
dao/.../timeseries/AbstractCassandraBaseTimeseriesDao.java
dao/.../nosql/CassandraAbstractDao.java
dao/.../nosql/CassandraAbstractAsyncDao.java
dao/.../nosql/CassandraBufferedRateReadExecutor.java
dao/.../nosql/CassandraBufferedRateWriteExecutor.java
common/dao-api/.../cassandra/guava/GuavaSession.java
```

`CassandraAbstractDao` 做了几件关键事：

- 缓存 prepared statement。
- 如果 statement 未指定 CL，则设置默认 read/write CL。
- 把请求提交给 read/write rate executor。
- 使用 Guava future 封装异步结果。

核心逻辑：

```text
executeAsyncWrite(tenantId, statement)
  -> if statement.consistencyLevel == null:
       statement = statement.setConsistencyLevel(defaultWriteLevel)
  -> rateWriteLimiter.submit(new CassandraStatementTask(...))
```

这说明 ThingsBoard 不直接在业务线程同步等待 Cassandra，而是通过异步和限流保护 Cassandra 集群。

## 10. 源码阅读指南

### 10.1 先从配置读起

第一步看：

```text
application/src/main/resources/thingsboard.yml
```

关键配置：

```yaml
database:
  ts:
    type: "${DATABASE_TS_TYPE:sql}"
  ts_latest:
    type: "${DATABASE_TS_LATEST_TYPE:sql}"

cassandra:
  url: "${CASSANDRA_URL:127.0.0.1:9042}"
  local_datacenter: "${CASSANDRA_LOCAL_DATACENTER:datacenter1}"
  query:
    read_consistency_level: "${CASSANDRA_READ_CONSISTENCY_LEVEL:ONE}"
    write_consistency_level: "${CASSANDRA_WRITE_CONSISTENCY_LEVEL:ONE}"
    default_fetch_size: "${CASSANDRA_DEFAULT_FETCH_SIZE:2000}"
    ts_key_value_partitioning: "${TS_KV_PARTITIONING:MONTHS}"
    use_ts_key_value_partitioning_on_read: "${USE_TS_KV_PARTITIONING_ON_READ:true}"
    ts_key_value_partitions_max_cache_size: "${TS_KV_PARTITIONS_MAX_CACHE_SIZE:100000}"
    ts_key_value_ttl: "${TS_KV_TTL:0}"
    buffer_size: "${CASSANDRA_QUERY_BUFFER_SIZE:200000}"
    concurrent_limit: "${CASSANDRA_QUERY_CONCURRENT_LIMIT:1000}"
    result_processing_threads: "${CASSANDRA_QUERY_RESULT_PROCESSING_THREADS:50}"
```

阅读重点：

- `DATABASE_TS_TYPE` 决定使用 SQL DAO 还是 Cassandra DAO。
- `TS_KV_PARTITIONING` 决定 `partition` 字段粒度。
- consistency level 决定读写副本确认策略。
- query buffer/concurrent limit 决定应用层限流。

### 10.2 Cassandra DAO 层结构

```text
TimeseriesService 接口
        ▲
        │
BaseTimeseriesService
        │
        ├── TimeseriesDao
        │       └── CassandraBaseTimeseriesDao
        │
        └── TimeseriesLatestDao
                └── CassandraBaseTimeseriesLatestDao
```

底层通用能力：

```text
CassandraBaseTimeseriesDao
  extends AbstractCassandraBaseTimeseriesDao
    extends CassandraAbstractAsyncDao
      extends CassandraAbstractDao
```

职责：

- `BaseTimeseriesService`：业务编排、校验、Entity View 处理、history/latest 双写。
- `CassandraBaseTimeseriesDao`：历史时序读写、分区计算、聚合查询。
- `CassandraBaseTimeseriesLatestDao`：latest 读写、latest 删除和重写。
- `CassandraAbstractDao`：statement、consistency、executor、driver session。
- `CassandraAbstractAsyncDao`：Guava Future transform、结果处理线程池。

### 10.3 查询请求如何构建

非聚合查询入口：

```text
BaseTimeseriesService.findAllByQueries()
  -> timeseriesDao.findAllAsync()
  -> CassandraBaseTimeseriesDao.findAllAsync()
  -> findAllAsyncWithLimit()
```

`findAllAsyncWithLimit()` 做：

```text
1. minPartition = toPartitionTs(startTs)
2. maxPartition = toPartitionTs(endTs)
3. getPartitionsFuture()
4. 创建 TsKvQueryCursor
5. findAllAsyncSequentiallyWithLimit()
```

读取单个 partition 的 prepared statement：

```sql
SELECT key, ts, bool_v, str_v, long_v, dbl_v, json_v
FROM ts_kv_cf
WHERE entity_type = ?
  AND entity_id = ?
  AND key = ?
  AND partition = ?
  AND ts >= ?
  AND ts < ?
ORDER BY ts DESC
LIMIT ?;
```

为什么 sequentially：

- 查询多个时间桶时，需要按 order 和 limit 控制返回数量。
- 如果先并发查所有桶，再排序截断，可能浪费大量查询。
- cursor 可以在结果够 limit 时停止。

### 10.4 异步查询实现方式

ThingsBoard 使用 Guava `ListenableFuture`。

```text
executeAsyncRead()
  -> CassandraBufferedRateReadExecutor.submit()
  -> CassandraStatementTask
  -> GuavaSession.executeAsync()
  -> TbResultSetFuture
  -> Futures.transform / transformAsync
```

`CassandraAbstractAsyncDao`：

```java
protected <T> ListenableFuture<T> getFuture(
    TbResultSetFuture future,
    java.util.function.Function<TbResultSet, T> transformer
) {
    return Futures.transform(future, input -> transformer.apply(input), readResultsProcessingExecutor);
}
```

这说明：

- Cassandra I/O 异步。
- 结果转换也放到 `cassandra-callback` 线程池。
- 不把结果处理压在 driver I/O 线程上。

### 10.5 分页查询实现方式

Cassandra driver 有 page size：

```yaml
cassandra:
  query:
    default_fetch_size: 2000
```

ThingsBoard 对 telemetry API 的分页/limit 不只是 driver page，而是业务 limit：

```text
ReadTsKvQuery.limit
  -> TsKvQueryCursor.currentLimit
  -> SELECT ... LIMIT ?
  -> cursor.addData()
  -> 如果数据够了就停止查后续 partition
```

同时，`TbResultSet.allRows(readResultsProcessingExecutor)` 会处理 driver 多页结果。

理解区别：

- driver fetch size：一次从 Cassandra 拉多少行。
- API limit：业务最多返回多少 telemetry points。
- partition cursor：跨多个时间桶时如何停止。

### 10.6 时间序列写入流程

时序图：

```text
Device
  │ telemetry
  ▼
Transport
  │
  ▼
Rule Engine / Save Timeseries Node
  │
  ▼
BaseTimeseriesService.save()
  │
  ├── validate entity
  │
  ├── timeseriesDao.savePartition()
  │     └── INSERT ts_kv_partitions_cf
  │
  ├── timeseriesDao.save()
  │     └── INSERT ts_kv_cf USING TTL ?
  │
  └── timeseriesLatestDao.saveLatest()
        └── INSERT ts_kv_latest_cf
```

源码重点：

```text
BaseTimeseriesService.saveAndRegisterFutures()
BaseTimeseriesService.doSaveAndRegisterFuturesFor()
CassandraBaseTimeseriesDao.save()
CassandraBaseTimeseriesDao.savePartition()
CassandraBaseTimeseriesLatestDao.saveLatest()
```

为什么是 2 或 3 次写：

- history 是完整历史。
- latest 是高频查询优化。
- partitions 是读路径优化。

代价：

- 写放大。
- 三张表最终一致。
- 某次部分写失败需要上层容忍或重试。

收益：

- 查询 latest 不扫历史。
- 查询历史不扫空时间桶。
- 写入仍保持 Cassandra 友好的 append/upsert。

### 10.7 时间序列读取流程

时序图：

```text
Dashboard / REST API
  │
  ▼
TimeseriesController / Subscription
  │
  ▼
TimeseriesService.findAllByQueries()
  │
  ├── 如果 EntityView:
  │     ├── 读取 entity_view 元数据
  │     ├── 过滤 keys
  │     └── 裁剪 start/end
  │
  ▼
CassandraBaseTimeseriesDao.findAllAsync()
  │
  ├── Aggregation.NONE?
  │     └── findAllAsyncWithLimit()
  │
  ├── toPartitionTs(start/end)
  ├── fetchPartitions() from ts_kv_partitions_cf
  ├── TsKvQueryCursor
  ├── SELECT ts_kv_cf partition by partition
  └── convert rows to TsKvEntry
```

聚合查询：

```text
findAllAsync()
  -> 按 interval 切多个 subQuery
  -> findAndAggregateAsync()
  -> getFetchChunksAsyncFunction()
  -> AggregatePartitionsFunction
```

聚合不是 Cassandra 原生 group by 完成，而是 ThingsBoard DAO 拉取指定范围数据后在 Java 侧聚合。

为什么：

- Cassandra 不适合任意 group by。
- ThingsBoard 的聚合窗口由 API 参数决定。
- Java 侧聚合更可控，但会消耗应用 CPU 和网络。

### 10.8 关键类清单

建议按顺序阅读：

1. `common/dao-api/src/main/java/org/thingsboard/server/dao/timeseries/TimeseriesService.java`
2. `dao/src/main/java/org/thingsboard/server/dao/timeseries/BaseTimeseriesService.java`
3. `dao/src/main/java/org/thingsboard/server/dao/timeseries/TimeseriesDao.java`
4. `dao/src/main/java/org/thingsboard/server/dao/timeseries/TimeseriesLatestDao.java`
5. `dao/src/main/java/org/thingsboard/server/dao/timeseries/CassandraBaseTimeseriesDao.java`
6. `dao/src/main/java/org/thingsboard/server/dao/timeseries/CassandraBaseTimeseriesLatestDao.java`
7. `dao/src/main/java/org/thingsboard/server/dao/timeseries/AbstractCassandraBaseTimeseriesDao.java`
8. `dao/src/main/java/org/thingsboard/server/dao/timeseries/CassandraTsPartitionsCache.java`
9. `dao/src/main/java/org/thingsboard/server/dao/timeseries/TsKvQueryCursor.java`
10. `dao/src/main/java/org/thingsboard/server/dao/timeseries/QueryCursor.java`
11. `dao/src/main/java/org/thingsboard/server/dao/timeseries/AggregatePartitionsFunction.java`
12. `dao/src/main/java/org/thingsboard/server/dao/nosql/CassandraAbstractDao.java`
13. `dao/src/main/java/org/thingsboard/server/dao/nosql/CassandraAbstractAsyncDao.java`
14. `dao/src/main/java/org/thingsboard/server/dao/nosql/CassandraBufferedRateReadExecutor.java`
15. `dao/src/main/java/org/thingsboard/server/dao/nosql/CassandraBufferedRateWriteExecutor.java`
16. `common/dao-api/src/main/java/org/thingsboard/server/dao/cassandra/CassandraDriverOptions.java`
17. `common/dao-api/src/main/java/org/thingsboard/server/dao/cassandra/guava/GuavaSession.java`
18. `common/dao-api/src/main/java/org/thingsboard/server/dao/cassandra/guava/GuavaRequestAsyncProcessor.java`

### 10.9 读源码时的检查问题

读每个方法时问：

- 它是否要求完整 partition key？
- 是否跨多个 Cassandra partition？
- 是否会 fan-out 到很多时间桶？
- limit 在哪里生效？
- TTL 在哪里计算？
- CL 在哪里设置？
- prepared statement 是否缓存？
- 回调在哪个线程池执行？
- SQL 后端和 Cassandra 后端是否语义一致？
- latest 和 history 是否可能短暂不一致？

这些问题比记住类名更重要。

## 11. 学习路线

### 第一阶段：能看懂 Cassandra 表和简单查询

预计时间：3 到 5 天。

目标：

- 理解 keyspace、table、partition key、clustering key。
- 能解释 `PRIMARY KEY ((entity_type, entity_id, key, partition), ts)`。
- 能用 cqlsh 查询 `ts_kv_cf`、`ts_kv_latest_cf`。
- 知道 Cassandra 为什么不支持 Join。
- 知道 `ALLOW FILTERING` 为什么危险。

练习：

1. 本地启动 Cassandra。
2. 用 `schema-keyspace.cql`、`schema-ts.cql`、`schema-ts-latest.cql` 创建表。
3. 手动 insert 几条 telemetry。
4. 分别按正确和错误查询条件执行 SELECT。
5. 用 `DESCRIBE TABLE` 对照 schema。

验收：

- 能手画 `ts_kv_cf` 的 partition 和 clustering rows。
- 能说出 `partition` 字段解决什么问题。

### 第二阶段：理解存储引擎和运维现象

预计时间：1 到 2 周。

目标：

- 理解 commit log、memtable、SSTable、Bloom filter。
- 理解 compaction 和 tombstone。
- 理解 RF、CL、hint、repair。
- 能用 `nodetool status/tablestats/compactionstats/tpstats` 做基本判断。
- 能识别热点分区和宽分区。

练习：

1. 写入大量测试 telemetry。
2. 调整 TTL，观察 tombstone 和 compaction。
3. 用 `nodetool tablestats` 看 partition size。
4. 停掉一个 Cassandra 节点，观察 CL=ONE 和 QUORUM 差异。
5. 对单表执行 repair，观察资源消耗。

验收：

- 能解释为什么 Cassandra 写入快。
- 能解释为什么短 TTL 可能导致查询慢。
- 能根据一个慢查询判断是时间桶太多、宽分区、tombstone 还是 compaction。

### 第三阶段：独立分析 ThingsBoard Cassandra 模块

预计时间：2 到 3 周。

目标：

- 能从 API 请求追到 `BaseTimeseriesService`。
- 能读懂 `CassandraBaseTimeseriesDao.findAllAsyncWithLimit()`。
- 能读懂 latest 删除重写逻辑。
- 能解释 `ts_kv_partitions_cf` 的作用和代价。
- 能对比 SQL DAO 和 Cassandra DAO 的语义差异。
- 能根据业务吞吐选择 `TS_KV_PARTITIONING` 和 CL。

练习：

1. 在 IDE 中对 `BaseTimeseriesService.save()` 打断点。
2. 跟踪一次 telemetry 写入三张 Cassandra 表。
3. 跟踪一次 dashboard 历史查询。
4. 修改 `TS_KV_PARTITIONING=DAYS`，观察 partition 值变化。
5. 模拟删除 latest 覆盖区间，跟踪 `removeLatest()` 如何回查历史。

验收：

- 能独立画出 ThingsBoard telemetry 写入/读取时序图。
- 能解释为什么 alarm/event/audit_log 不在 Cassandra。
- 能提出 Cassandra 热点/宽分区/TTL 问题的优化方案。

## 12. 作为架构师还能有哪些可选方案

### 12.1 PostgreSQL-only

适合：

- 中小规模。
- 团队只会 SQL。
- 运维资源有限。
- telemetry 写入量低于 5K-10K data points/s。

优点：

- 架构简单。
- 查询灵活。
- 事务强。
- 备份恢复熟悉。

缺点：

- 高写入下单库压力大。
- 水平扩展复杂。
- TTL/历史清理要靠后台任务和分区。

### 12.2 PostgreSQL + TimescaleDB

适合：

- 想保持 SQL 查询能力。
- 数据主要是时序。
- 写入高于普通 PostgreSQL，但未到 Cassandra 级别。

优点：

- hypertable、chunk、压缩、time_bucket 等能力更贴近时序。
- SQL 生态保留。

缺点：

- 水平扩展和极限写入仍不是 Cassandra 的同类模型。
- ThingsBoard 3.6 中也支持 `timescale` 后端，但本篇重点是 Cassandra。

### 12.3 Cassandra

适合：

- 遥测写入巨大。
- 查询模式固定。
- 能接受最终一致。
- 需要横向扩展和高可用。

优点：

- 写入吞吐强。
- 多副本高可用。
- 数据按 key 自动分布。
- TTL 原生支持。

缺点：

- 查询模型受限。
- 运维复杂。
- tombstone/compaction/repair 要认真管理。
- 不适合业务实体和复杂过滤。

### 12.4 ClickHouse / Druid / Elasticsearch 等分析系统

适合：

- 大范围聚合分析。
- 按 value、tag、维度过滤。
- 报表和 OLAP。

但它们不是 ThingsBoard 3.6 源码中内置的 telemetry 主存储路径。作为架构师，可以把 Cassandra 作为热写主存储，再异步同步到分析系统。

最终 ThingsBoard 选择 Cassandra 的原因：

```text
PostgreSQL:
  负责强业务模型

Cassandra:
  负责海量 telemetry 原始事实

规则链/外部系统:
  负责实时派生、告警、分析、聚合
```

这是按数据形态拆分，而不是按技术偏好拆分。

## 13. 本地版本和源码索引

本地版本线索：

```text
pom.xml:
  com.datastax.oss java-driver-core/query-builder = 4.15.0
  org.apache.cassandra cassandra-all = 3.11.14

docker/docker-compose.hybrid.yml:
  cassandra image = cassandra:4.0.4
```

注意：

- `java-driver-core 4.15.0` 是 Java 客户端驱动版本。
- `cassandra-all 3.11.14` 主要用于工具/迁移相关依赖，不能直接等同于生产 Cassandra 服务端版本。
- 本地 hybrid docker 明确使用 `cassandra:4.0.4`。
- 生产服务端版本要结合 ThingsBoard 官方支持矩阵、JDK、驱动兼容性和运维策略选择。

核心文件：

```text
dao/src/main/resources/cassandra/schema-keyspace.cql
dao/src/main/resources/cassandra/schema-ts.cql
dao/src/main/resources/cassandra/schema-ts-latest.cql
dao/src/main/resources/sql/schema-ts-psql.sql
dao/src/main/resources/sql/schema-ts-latest-psql.sql
dao/src/main/resources/sql/schema-entities.sql
dao/src/main/resources/sql/schema-entities-idx.sql
dao/src/main/resources/sql/schema-entities-idx-psql-addon.sql
application/src/main/resources/thingsboard.yml
```

源码入口：

```text
dao/src/main/java/org/thingsboard/server/dao/timeseries/
dao/src/main/java/org/thingsboard/server/dao/nosql/
common/dao-api/src/main/java/org/thingsboard/server/dao/cassandra/
```

官方参考：

- ThingsBoard Database Layer: https://thingsboard.io/docs/pe/reference/architecture/database/
- Apache Cassandra Architecture Overview: https://cassandra.apache.org/doc/latest/cassandra/architecture/overview.html
- Apache Cassandra Storage Engine: https://cassandra.apache.org/doc/latest/cassandra/architecture/storage-engine.html
- Apache Cassandra CQL Data Definition: https://cassandra.apache.org/doc/latest/cassandra/developing/cql/ddl.html

## 14. 最后总结

要读懂 ThingsBoard Cassandra 源码，不需要先成为 Cassandra DBA。你真正需要抓住的是：

```text
1. ThingsBoard 只把高吞吐 telemetry 交给 Cassandra。
2. Cassandra 表不是关系模型，而是查询路径的物化结果。
3. ts_kv_cf 的核心是 entity + key + time bucket + ts。
4. ts_kv_latest_cf 是为了最新值查询，不是历史表的替代。
5. ts_kv_partitions_cf 是读路径优化，用一次额外写减少空分区读取。
6. CL=ONE 是 ThingsBoard telemetry 对吞吐和可用性的取舍。
7. 写入快来自 commit log + memtable + immutable SSTable + token ring。
8. 生产问题多集中在热点分区、宽分区、tombstone、compaction、repair。
9. alarm/event/audit_log/entity_view 留在 PostgreSQL 是有意设计。
10. 源码阅读要从 BaseTimeseriesService 进入，再下钻 DAO 和 CassandraAbstractDao。
```

如果你带着这些问题去读源码：

- 这个查询是否命中完整 partition key？
- 这个方法是否跨多个时间桶？
- 这个写入为什么要写三张表？
- 这个配置是在换吞吐、延迟、一致性还是运维复杂度？

你就不会把 Cassandra 当成“另一种 SQL 数据库”，而是能看到 ThingsBoard 在海量 IoT 时序数据上的架构设计思想。
