# 模块文档索引

## 核心模块

| 文档 | 内容 |
|------|------|
| [application.md](./application.md) | 主应用模块：REST API、Actor系统、业务服务 |
| [rule-engine.md](./rule-engine.md) | 规则引擎：规则链、规则节点、自定义开发 |
| [common.md](./common.md) | 公共框架：Actor、数据模型、消息、队列、传输 |
| [dao.md](./dao.md) | 数据持久层：PostgreSQL、Cassandra、缓存策略 |
| [transport.md](./transport.md) | 传输协议：MQTT、CoAP、HTTP、LwM2M、SNMP |
| [ui-and-others.md](./ui-and-others.md) | 前端UI、微服务架构、辅助模块 |

## 可视化文档

| 文档 | 内容 |
|------|------|
| [architecture-flow.html](../tb-diagrams/architecture-flow.html) | 交互式系统架构图，点击查看组件详情 |
| [canvas-dataflow.html](../tb-diagrams/canvas-dataflow.html) | 数据流转可视化：遥测/RPC/告警/属性/集群 |
| [learning-guide.html](../tb-diagrams/learning-guide.html) | 交互式学习指南，含进度追踪 |
| [mermaid-diagrams.html](../tb-diagrams/mermaid-diagrams.html) | Mermaid图表集：时序图/类图/ER图/部署图 |
| [design-patterns.html](../tb-diagrams/design-patterns.html) | 设计思想：Actor模型/多租户/队列架构/插件化 |

## 建议阅读顺序

1. 先浏览 [learning-guide.html](../tb-diagrams/learning-guide.html) 了解学习路径
2. 打开 [architecture-flow.html](../tb-diagrams/architecture-flow.html) 建立全局视角
3. 阅读 [common.md](./common.md) 理解基础框架
4. 阅读 [transport.md](./transport.md) 理解设备接入
5. 阅读 [rule-engine.md](./rule-engine.md) 理解消息处理
6. 阅读 [application.md](./application.md) 理解服务编排
7. 阅读 [dao.md](./dao.md) 理解数据持久化
8. 最后看 [design-patterns.html](../tb-diagrams/design-patterns.html) 深入设计哲学
