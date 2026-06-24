# Rule Engine 模块学习文档

## 模块概述

规则引擎是 ThingsBoard 的核心功能之一，提供可视化的消息处理流水线。用户可以通过拖拽式UI构建复杂的数据处理逻辑，无需编写代码即可实现数据过滤、转换、持久化、告警和外部系统集成。

## 子模块结构

```
rule-engine/
├── rule-engine-api/          # 规则引擎API接口
│   └── src/main/java/org/thingsboard/rule/engine/api/
│       ├── RuleNode.java              # @RuleNode 注解定义
│       ├── TbNode.java                # 规则节点接口
│       ├── TbContext.java             # 规则节点执行上下文
│       ├── TbNodeConfiguration.java   # 节点配置接口
│       └── ...
└── rule-engine-components/   # 规则引擎内置组件
    └── src/main/java/org/thingsboard/rule/engine/
        ├── filter/           # 过滤节点 (19个)
        ├── transform/        # 转换节点 (18个)
        ├── action/           # 动作节点 (28个)
        ├── telemetry/        # 遥测节点 (12个)
        ├── metadata/         # 元数据节点 (31个)
        ├── profile/          # 设备配置节点 (14个)
        ├── flow/             # 流程控制节点 (5个)
        ├── external/         # 外部集成节点
        ├── rest/             # REST调用节点 (6个)
        ├── mail/             # 邮件节点 (4个)
        ├── mqtt/             # MQTT节点 (3个)
        ├── kafka/            # Kafka节点 (2个)
        ├── rabbitmq/         # RabbitMQ节点 (2个)
        ├── aws/              # AWS集成节点 (3个)
        ├── gcp/              # GCP集成节点 (1个)
        ├── rpc/              # RPC节点 (4个)
        ├── notification/     # 通知节点 (4个)
        ├── geo/              # 地理围栏节点 (6个)
        ├── math/             # 数学计算节点 (7个)
        ├── ai/               # AI节点 (4个)
        ├── edge/             # 边缘节点 (6个)
        ├── deduplication/    # 去重节点 (5个)
        ├── delay/            # 延迟节点 (2个)
        └── credentials/      # 凭证节点 (5个)
```

## 核心概念

### Rule Chain (规则链)
规则链是一个有向图，由多个规则节点通过关系（relation）连接。每条消息从根节点（Root Rule Chain）开始处理。

### Rule Node (规则节点)
规则节点是消息处理的基本单元，每个节点实现 `TbNode` 接口：

```java
public interface TbNode {
    void init(TbContext ctx, TbNodeConfiguration configuration);
    void onMsg(TbContext ctx, TbMsg msg);
    void destroy();
}
```

### TbMsg (规则引擎消息)
```java
public class TbMsg {
    UUID id;              // 消息唯一ID
    String type;          // 消息类型(POST_TELEMETRY, POST_ATTRIBUTES等)
    EntityId originator;  // 消息来源实体
    TbMsgMetaData metaData;  // 元数据(key-value)
    String data;          // 消息体(JSON)
    RuleChainId ruleChainId; // 当前规则链ID
    RuleNodeId ruleNodeId;   // 当前规则节点ID
}
```

### TbContext (执行上下文)
提供规则节点执行所需的所有服务：
- `tellSuccess(msg)` - 消息处理成功，传递给下一节点
- `tellFailure(msg, error)` - 消息处理失败
- `tellNext(msg, relations)` - 按指定关系传递消息
- `getDbCallbackExecutor()` - 获取异步执行器
- `getTelemetryService()` - 获取遥测服务
- `getAlarmService()` - 获取告警服务

## 内置规则节点分类

### 1. Filter Nodes (过滤节点)
| 节点 | 功能 |
|------|------|
| `TbMsgTypeFilterNode` | 按消息类型过滤 |
| `TbJsFilterNode` | JavaScript 脚本过滤 |
| `TbMsgTypeSwitchNode` | 消息类型路由 |
| `TbOriginatorTypeSwitchNode` | 来源实体类型路由 |
| `TbCheckRelationFilterNode` | 关系检查过滤 |
| `TbCheckAlarmStatusNode` | 告警状态检查 |

### 2. Transform Nodes (转换节点)
| 节点 | 功能 |
|------|------|
| `TbTransformMsgNode` | JavaScript 脚本转换 |
| `TbChangeOriginatorNode` | 更改消息来源 |
| `TbCopyKeysNode` | 复制消息Key |
| `TbRenameKeysNode` | 重命名Key |
| `TbDeleteKeysNode` | 删除Key |
| `TbToEmailNode` | 转换为邮件格式 |

### 3. Action Nodes (动作节点)
| 节点 | 功能 |
|------|------|
| `TbSaveToCustomCassandraTableNode` | 保存到Cassandra |
| `TbCreateAlarmNode` | 创建告警 |
| `TbClearAlarmNode` | 清除告警 |
| `TbLogNode` | 日志记录 |
| `TbMsgDelayNode` | 消息延迟 |
| `TbRpcCallRequestNode` | RPC调用 |

### 4. Enrichment Nodes (数据增强节点)
| 节点 | 功能 |
|------|------|
| `TbGetAttributesNode` | 获取属性数据 |
| `TbGetTelemetryNode` | 获取遥测数据 |
| `TbGetRelatedAttributeNode` | 获取关联实体属性 |
| `TbGetOriginatorFieldsNode` | 获取来源实体字段 |
| `TbGetCustomerDetailsNode` | 获取客户详情 |

### 5. External Nodes (外部集成节点)
| 节点 | 功能 |
|------|------|
| `TbRestApiCallNode` | REST API调用 |
| `TbSendEmailNode` | 发送邮件 |
| `TbMqttNode` | MQTT发布 |
| `TbKafkaNode` | Kafka发布 |
| `TbRabbitMqNode` | RabbitMQ发布 |
| `TbAwsSnsNode` | AWS SNS |
| `TbAwsSqsNode` | AWS SQS |

## 规则链执行流程

```
1. TbMsg 进入 Root Rule Chain
2. RuleChainActor 将消息发送给第一个 RuleNodeActor
3. RuleNodeActor 调用 TbNode.onMsg(ctx, msg)
4. 节点处理完毕后调用 ctx.tellNext(msg, "Success"/"Failure"/自定义关系)
5. RuleChainActor 根据关系找到下一个节点
6. 重复步骤 3-5 直到消息被所有节点处理完毕
7. 如果需要转发到其他规则链，通过 Rule Chain Input 节点实现
```

## 自定义规则节点开发

```java
@RuleNode(
    type = ComponentType.FILTER,
    name = "my custom filter",
    configClazz = MyFilterConfiguration.class
)
public class MyFilterNode implements TbNode {
    
    private MyFilterConfiguration config;
    
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) {
        this.config = TbNodeUtils.convert(configuration, MyFilterConfiguration.class);
    }
    
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        // 自定义过滤逻辑
        if (shouldPass(msg)) {
            ctx.tellSuccess(msg);
        } else {
            ctx.tellNext(msg, "False");
        }
    }
    
    @Override
    public void destroy() {
        // 清理资源
    }
}
```
