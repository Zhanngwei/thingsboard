# Thingsboard Server Commons 模块调用链分析

> 生成范围：`common`  
> Maven artifact：`common`  
> packaging：`pom`  
> 分析方式：静态扫描 POM、Java 注解、入口方法、关键技术触点和类关系；不是运行时 trace。

## 完整流程图

```mermaid
flowchart TD
    A["① 最先调用<br/>application、transport、dao、rule-engine、monitoring、msa 和测试模块通过依赖最先调用 common"]
    B["② 调用原因<br/>需要共享数据模型、队列接口、缓存接口、Actor API、transport API、脚本 API 或工具类"]
    C["③ 调用之前<br/>上游模块已经处在具体业务流程中，需要复用稳定的公共契约或 DTO"]
    D["模块入口<br/>common"]
    E["⑤ 数据变化<br/>数据主要在 DTO、消息、接口参数、缓存 key、队列 payload 和工具返回值之间保持类型化表达"]
    F{"技术触点判定"}
    G["⑥ 数据库<br/>DB 间接/否"]
    H["⑦ Actor<br/>Actor 间接/否"]
    I["⑧ MQTT<br/>MQTT 间接/否"]
    J["⑨ Kafka<br/>Kafka 间接/否"]
    K["⑩ Rule Engine<br/>Rule Engine 间接/否"]
    L["④ 调用之后<br/>公共模型或接口被上游继续传递到 DAO、Actor、队列、Transport 或 Rule Engine"]
    A --> B --> C --> D --> E --> F
    F --> G
    F --> H
    F --> I
    F --> J
    F --> K
    G --> L
    H --> L
    I --> L
    J --> L
    K --> L
```


## 十项调用链问题

| 问题 | 模块级结论 |
| --- | --- |
| ① 谁最先调用这里？ | application、transport、dao、rule-engine、monitoring、msa 和测试模块通过依赖最先调用 common |
| ② 为什么会调用？ | 需要共享数据模型、队列接口、缓存接口、Actor API、transport API、脚本 API 或工具类 |
| ③ 调用之前发生了什么？ | 上游模块已经处在具体业务流程中，需要复用稳定的公共契约或 DTO |
| ④ 调用之后发生什么？ | 公共模型或接口被上游继续传递到 DAO、Actor、队列、Transport 或 Rule Engine |
| ⑤ 数据如何变化？ | 数据主要在 DTO、消息、接口参数、缓存 key、队列 payload 和工具返回值之间保持类型化表达 |
| ⑥ 对数据库进行了哪些操作？ | 否，未发现直接操作证据；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。 |
| ⑦ 是否发送 Actor 消息？ | 否，未发现直接操作证据；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。 |
| ⑧ 是否发送 MQTT 消息？ | 否，未发现直接操作证据；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。 |
| ⑨ 是否写入 Kafka？ | 否，未发现直接操作证据；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。 |
| ⑩ 是否写入 Rule Engine？ | 否，未发现直接操作证据；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。 |

## 入口证据

- 未发现 main、Spring 注解、Controller、Service、Repository、KafkaListener 或测试入口；本模块可能主要提供模型、接口、聚合或资源。


## 静态关键词统计（不等同于实际发送/写入）

- database: 0 处静态触点；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。
- actor: 0 处静态触点；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。
- mqtt: 0 处静态触点；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。
- kafka: 0 处静态触点；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。
- rule_engine: 0 处静态触点；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。
- cache: 0 处静态触点；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。
- queue: 0 处静态触点；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。
- rest: 0 处静态触点；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。
- websocket: 0 处静态触点；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。
- transport: 0 处静态触点；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。

## 调用前后数据流

1. 调用前：上游模块已经处在具体业务流程中，需要复用稳定的公共契约或 DTO
2. 模块入口：`common` 通过入口类、依赖 API、构建聚合或子模块暴露能力。
3. 模块内转换：数据主要在 DTO、消息、接口参数、缓存 key、队列 payload 和工具返回值之间保持类型化表达
4. 调用后：公共模型或接口被上游继续传递到 DAO、Actor、队列、Transport 或 Rule Engine
5. 数据库/Actor/MQTT/Kafka/Rule Engine 这些动作若没有直接触点，通常发生在依赖的 `application`、`dao`、`common/queue`、`common/transport` 或 `rule-engine` 模块中。

## 子模块与依赖

### 子模块

- `data`
- `util`
- `message`
- `actor`
- `queue`
- `transport`
- `dao-api`
- `cluster-api`
- `stats`
- `cache`
- `coap-server`
- `edge-api`
- `version-control`
- `script`
- `proto`


### 主要依赖

- POM 中未声明直接依赖或依赖由父 POM 管理。


## 关键类型样本

- 本模块没有直接 Java 类型。


## 阅读建议

- 先看“完整流程图”确认模块在全链路中的位置。
- 再看入口证据判断调用来源是 HTTP、MQTT/Transport、Actor、Kafka、Rule Engine、DAO、测试还是构建聚合。
- 最后用触点统计区分直接行为和下游间接行为，避免把依赖模块的数据库写入误认为本模块直接写入。
