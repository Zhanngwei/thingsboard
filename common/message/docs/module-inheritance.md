# Thingsboard Server Common Messages 模块继承体系分析

> 生成范围：`common/message`  
> Maven artifact：`message`  
> Java 类型数量：61  
> 分析方式：静态扫描 `class/interface/enum/record` 的 `extends`、`implements`、方法签名和源码触点。

## 完整继承树

```text
AbstractRateLimitException
├── TbRateLimitsException
Exception
├── ProcessingTimeoutException
├── RuleEngineException
├── ├── RuleNodeException
├── SessionException
├── ├── SessionAuthException
Object/外部框架
├── ComponentLifecycleMsg
├── ├── RuleNodeUpdatedMsg
├── DeviceAttributes
├── DeviceAttributesEventNotificationMsg
├── DeviceCredentialsUpdateNotificationMsg
├── DeviceDeleteMsg
├── DeviceEdgeUpdateMsg
├── DeviceMetaData
├── DeviceNameOrTypeUpdateMsg
├── EdgeEventUpdateMsg
├── EncryptionUtil
├── FromDeviceRpcResponse
├── FromDeviceRpcResponseActorMsg
├── FromEdgeSyncResponse
├── PartitionChangeMsg
├── RateLimitsTest
├── RemoveRpcActorMsg
├── RuleNodeInfo
├── SchedulerUtils
├── TbMsg
├── TbMsgMetaData
├── TbMsgMetaDataTest
├── TbMsgProcessingCtx
├── TbMsgProcessingStackItem
├── TbMsgProcessingStackItemTest
├── TbRateLimits
├── TbRuleEngineActorMsg
├── ├── QueueToRuleEngineMsg
├── TimeoutMsg
├── ├── DeviceActorServerSideRpcTimeoutMsg
├── ToDeviceRpcRequest
├── ToDeviceRpcRequestActorMsg
├── ToEdgeSyncRequest
├── TopicPartitionInfo
├── TopicPartitionInfoTest
Serializable
├── «implements» FromDeviceRpcResponse
├── «implements» TbMsg
├── «implements» TbMsgMetaData
├── «implements» TbMsgProcessingCtx
├── «implements» TbMsgProcessingStackItem
├── ToAllNodesMsg
├── ├── «implements» ComponentLifecycleMsg
├── ├── «implements» ├── RuleNodeUpdatedMsg
├── ├── EdgeSessionMsg
├── ├── ├── «implements» EdgeEventUpdateMsg
├── ├── ├── «implements» FromEdgeSyncResponse
├── ├── ├── «implements» ToEdgeSyncRequest
├── ToDeviceActorNotificationMsg
├── ├── «implements» DeviceAttributesEventNotificationMsg
├── ├── «implements» DeviceCredentialsUpdateNotificationMsg
├── ├── «implements» DeviceDeleteMsg
├── ├── «implements» DeviceEdgeUpdateMsg
├── ├── «implements» DeviceNameOrTypeUpdateMsg
├── ├── «implements» FromDeviceRpcResponseActorMsg
├── ├── «implements» RemoveRpcActorMsg
├── ├── «implements» ToDeviceRpcRequestActorMsg
├── «implements» ToDeviceRpcRequest
TbActorMsg
├── DeviceAwareMsg
├── ├── ToDeviceActorNotificationMsg
├── ├── ├── «implements» DeviceAttributesEventNotificationMsg
├── ├── ├── «implements» DeviceCredentialsUpdateNotificationMsg
├── ├── ├── «implements» DeviceDeleteMsg
├── ├── ├── «implements» DeviceEdgeUpdateMsg
├── ├── ├── «implements» DeviceNameOrTypeUpdateMsg
├── ├── ├── «implements» FromDeviceRpcResponseActorMsg
├── ├── ├── «implements» RemoveRpcActorMsg
├── ├── ├── «implements» ToDeviceRpcRequestActorMsg
├── «implements» PartitionChangeMsg
├── RuleChainAwareMsg
├── «implements» TbRuleEngineActorMsg
├── «implements» ├── QueueToRuleEngineMsg
├── TenantAwareMsg
├── ├── «implements» ComponentLifecycleMsg
├── ├── «implements» ├── RuleNodeUpdatedMsg
├── ├── EdgeSessionMsg
├── ├── ├── «implements» EdgeEventUpdateMsg
├── ├── ├── «implements» FromEdgeSyncResponse
├── ├── ├── «implements» ToEdgeSyncRequest
├── ├── ToDeviceActorNotificationMsg
├── ├── ├── «implements» DeviceAttributesEventNotificationMsg
├── ├── ├── «implements» DeviceCredentialsUpdateNotificationMsg
├── ├── ├── «implements» DeviceDeleteMsg
├── ├── ├── «implements» DeviceEdgeUpdateMsg
├── ├── ├── «implements» DeviceNameOrTypeUpdateMsg
├── ├── ├── «implements» FromDeviceRpcResponseActorMsg
├── ├── ├── «implements» RemoveRpcActorMsg
├── ├── ├── «implements» ToDeviceRpcRequestActorMsg
├── «implements» TimeoutMsg
├── «implements» ├── DeviceActorServerSideRpcTimeoutMsg
├── ToAllNodesMsg
├── ├── «implements» ComponentLifecycleMsg
├── ├── «implements» ├── RuleNodeUpdatedMsg
├── ├── EdgeSessionMsg
├── ├── ├── «implements» EdgeEventUpdateMsg
├── ├── ├── «implements» FromEdgeSyncResponse
├── ├── ├── «implements» ToEdgeSyncRequest
├── ToDeviceActorNotificationMsg
├── ├── «implements» DeviceAttributesEventNotificationMsg
├── ├── «implements» DeviceCredentialsUpdateNotificationMsg
├── ├── «implements» DeviceDeleteMsg
├── ├── «implements» DeviceEdgeUpdateMsg
├── ├── «implements» DeviceNameOrTypeUpdateMsg
├── ├── «implements» FromDeviceRpcResponseActorMsg
├── ├── «implements» RemoveRpcActorMsg
├── ├── «implements» ToDeviceRpcRequestActorMsg
```

## 继承图

```mermaid
flowchart TD
    Object______p0["Object/外部框架"] -->|extends| EncryptionUtil_c0["EncryptionUtil"]
    Object______p1["Object/外部框架"] -->|extends| TbMsg_c1["TbMsg"]
    Serializable_p2["Serializable"] -->|implements| TbMsg_c2["TbMsg"]
    Object______p3["Object/外部框架"] -->|extends| TbMsgMetaData_c3["TbMsgMetaData"]
    Serializable_p4["Serializable"] -->|implements| TbMsgMetaData_c4["TbMsgMetaData"]
    Object______p5["Object/外部框架"] -->|extends| TbMsgProcessingCtx_c5["TbMsgProcessingCtx"]
    Serializable_p6["Serializable"] -->|implements| TbMsgProcessingCtx_c6["TbMsgProcessingCtx"]
    Object______p7["Object/外部框架"] -->|extends| TbMsgProcessingStackItem_c7["TbMsgProcessingStackItem"]
    Serializable_p8["Serializable"] -->|implements| TbMsgProcessingStackItem_c8["TbMsgProcessingStackItem"]
    Object______p9["Object/外部框架"] -->|extends| TbRuleEngineActorMsg_c9["TbRuleEngineActorMsg"]
    TbActorMsg_p10["TbActorMsg"] -->|implements| TbRuleEngineActorMsg_c10["TbRuleEngineActorMsg"]
    TbActorMsg_p11["TbActorMsg"] -->|extends| ToDeviceActorNotificationMsg_c11["ToDeviceActorNotificationMsg"]
    TenantAwareMsg_p12["TenantAwareMsg"] -->|extends| ToDeviceActorNotificationMsg_c12["ToDeviceActorNotificationMsg"]
    DeviceAwareMsg_p13["DeviceAwareMsg"] -->|extends| ToDeviceActorNotificationMsg_c13["ToDeviceActorNotificationMsg"]
    Serializable_p14["Serializable"] -->|extends| ToDeviceActorNotificationMsg_c14["ToDeviceActorNotificationMsg"]
    TbActorMsg_p15["TbActorMsg"] -->|extends| DeviceAwareMsg_c15["DeviceAwareMsg"]
    TbActorMsg_p16["TbActorMsg"] -->|extends| RuleChainAwareMsg_c16["RuleChainAwareMsg"]
    TbActorMsg_p17["TbActorMsg"] -->|extends| TenantAwareMsg_c17["TenantAwareMsg"]
    Serializable_p18["Serializable"] -->|extends| ToAllNodesMsg_c18["ToAllNodesMsg"]
    TbActorMsg_p19["TbActorMsg"] -->|extends| ToAllNodesMsg_c19["ToAllNodesMsg"]
    Object______p20["Object/外部框架"] -->|extends| EdgeEventUpdateMsg_c20["EdgeEventUpdateMsg"]
    EdgeSessionMsg_p21["EdgeSessionMsg"] -->|implements| EdgeEventUpdateMsg_c21["EdgeEventUpdateMsg"]
    TenantAwareMsg_p22["TenantAwareMsg"] -->|extends| EdgeSessionMsg_c22["EdgeSessionMsg"]
    ToAllNodesMsg_p23["ToAllNodesMsg"] -->|extends| EdgeSessionMsg_c23["EdgeSessionMsg"]
    Object______p24["Object/外部框架"] -->|extends| FromEdgeSyncResponse_c24["FromEdgeSyncResponse"]
    EdgeSessionMsg_p25["EdgeSessionMsg"] -->|implements| FromEdgeSyncResponse_c25["FromEdgeSyncResponse"]
    Object______p26["Object/外部框架"] -->|extends| ToEdgeSyncRequest_c26["ToEdgeSyncRequest"]
    EdgeSessionMsg_p27["EdgeSessionMsg"] -->|implements| ToEdgeSyncRequest_c27["ToEdgeSyncRequest"]
    Object______p28["Object/外部框架"] -->|extends| ComponentLifecycleMsg_c28["ComponentLifecycleMsg"]
    TenantAwareMsg_p29["TenantAwareMsg"] -->|implements| ComponentLifecycleMsg_c29["ComponentLifecycleMsg"]
    ToAllNodesMsg_p30["ToAllNodesMsg"] -->|implements| ComponentLifecycleMsg_c30["ComponentLifecycleMsg"]
    ComponentLifecycleMsg_p31["ComponentLifecycleMsg"] -->|extends| RuleNodeUpdatedMsg_c31["RuleNodeUpdatedMsg"]
    Object______p32["Object/外部框架"] -->|extends| PartitionChangeMsg_c32["PartitionChangeMsg"]
    TbActorMsg_p33["TbActorMsg"] -->|implements| PartitionChangeMsg_c33["PartitionChangeMsg"]
    TbRuleEngineActorMsg_p34["TbRuleEngineActorMsg"] -->|extends| QueueToRuleEngineMsg_c34["QueueToRuleEngineMsg"]
    Exception_p35["Exception"] -->|extends| RuleEngineException_c35["RuleEngineException"]
    RuleEngineException_p36["RuleEngineException"] -->|extends| RuleNodeException_c36["RuleNodeException"]
    Object______p37["Object/外部框架"] -->|extends| RuleNodeInfo_c37["RuleNodeInfo"]
    Object______p38["Object/外部框架"] -->|extends| TopicPartitionInfo_c38["TopicPartitionInfo"]
    Object______p39["Object/外部框架"] -->|extends| FromDeviceRpcResponse_c39["FromDeviceRpcResponse"]
    Serializable_p40["Serializable"] -->|implements| FromDeviceRpcResponse_c40["FromDeviceRpcResponse"]
    Object______p41["Object/外部框架"] -->|extends| FromDeviceRpcResponseActorMsg_c41["FromDeviceRpcResponseActorMsg"]
    ToDeviceActorNotificationMsg_p42["ToDeviceActorNotificationMsg"] -->|implements| FromDeviceRpcResponseActorMsg_c42["FromDeviceRpcResponseActorMsg"]
    Object______p43["Object/外部框架"] -->|extends| RemoveRpcActorMsg_c43["RemoveRpcActorMsg"]
    ToDeviceActorNotificationMsg_p44["ToDeviceActorNotificationMsg"] -->|implements| RemoveRpcActorMsg_c44["RemoveRpcActorMsg"]
    Object______p45["Object/外部框架"] -->|extends| ToDeviceRpcRequest_c45["ToDeviceRpcRequest"]
    Serializable_p46["Serializable"] -->|implements| ToDeviceRpcRequest_c46["ToDeviceRpcRequest"]
    Object______p47["Object/外部框架"] -->|extends| ToDeviceRpcRequestActorMsg_c47["ToDeviceRpcRequestActorMsg"]
    ToDeviceActorNotificationMsg_p48["ToDeviceActorNotificationMsg"] -->|implements| ToDeviceRpcRequestActorMsg_c48["ToDeviceRpcRequestActorMsg"]
    Object______p49["Object/外部框架"] -->|extends| DeviceAttributes_c49["DeviceAttributes"]
    Object______p50["Object/外部框架"] -->|extends| DeviceAttributesEventNotificationMsg_c50["DeviceAttributesEventNotificationMsg"]
    ToDeviceActorNotificationMsg_p51["ToDeviceActorNotificationMsg"] -->|implements| DeviceAttributesEventNotificationMsg_c51["DeviceAttributesEventNotificationMsg"]
    Object______p52["Object/外部框架"] -->|extends| DeviceCredentialsUpdateNotificationMsg_c52["DeviceCredentialsUpdateNotificationMsg"]
    ToDeviceActorNotificationMsg_p53["ToDeviceActorNotificationMsg"] -->|implements| DeviceCredentialsUpdateNotificationMsg_c53["DeviceCredentialsUpdateNotificationMsg"]
    Object______p54["Object/外部框架"] -->|extends| DeviceDeleteMsg_c54["DeviceDeleteMsg"]
    ToDeviceActorNotificationMsg_p55["ToDeviceActorNotificationMsg"] -->|implements| DeviceDeleteMsg_c55["DeviceDeleteMsg"]
    Object______p56["Object/外部框架"] -->|extends| DeviceEdgeUpdateMsg_c56["DeviceEdgeUpdateMsg"]
    ToDeviceActorNotificationMsg_p57["ToDeviceActorNotificationMsg"] -->|implements| DeviceEdgeUpdateMsg_c57["DeviceEdgeUpdateMsg"]
    Object______p58["Object/外部框架"] -->|extends| DeviceMetaData_c58["DeviceMetaData"]
    Object______p59["Object/外部框架"] -->|extends| DeviceNameOrTypeUpdateMsg_c59["DeviceNameOrTypeUpdateMsg"]
    ToDeviceActorNotificationMsg_p60["ToDeviceActorNotificationMsg"] -->|implements| DeviceNameOrTypeUpdateMsg_c60["DeviceNameOrTypeUpdateMsg"]
    Exception_p61["Exception"] -->|extends| ProcessingTimeoutException_c61["ProcessingTimeoutException"]
    SessionException_p62["SessionException"] -->|extends| SessionAuthException_c62["SessionAuthException"]
    Exception_p63["Exception"] -->|extends| SessionException_c63["SessionException"]
    TimeoutMsg_p64["TimeoutMsg"] -->|extends| DeviceActorServerSideRpcTimeoutMsg_c64["DeviceActorServerSideRpcTimeoutMsg"]
    Object______p65["Object/外部框架"] -->|extends| TimeoutMsg_c65["TimeoutMsg"]
    TbActorMsg_p66["TbActorMsg"] -->|implements| TimeoutMsg_c66["TimeoutMsg"]
    Object______p67["Object/外部框架"] -->|extends| SchedulerUtils_c67["SchedulerUtils"]
    Object______p68["Object/外部框架"] -->|extends| TbRateLimits_c68["TbRateLimits"]
    AbstractRateLimitException_p69["AbstractRateLimitException"] -->|extends| TbRateLimitsException_c69["TbRateLimitsException"]
    Object______p70["Object/外部框架"] -->|extends| TbMsgMetaDataTest_c70["TbMsgMetaDataTest"]
    Object______p71["Object/外部框架"] -->|extends| TbMsgProcessingStackItemTest_c71["TbMsgProcessingStackItemTest"]
    Object______p72["Object/外部框架"] -->|extends| TopicPartitionInfoTest_c72["TopicPartitionInfoTest"]
    Object______p73["Object/外部框架"] -->|extends| RateLimitsTest_c73["RateLimitsTest"]
```


## 每一层为什么存在

- 外部/上层父类或接口层：提供框架生命周期、Java 标准契约、Spring/Netty/DAO/Rule Engine 等扩展点。
- 接口层：定义跨模块契约，让调用方依赖稳定 API，而不是具体实现。
- 抽象类层：沉淀公共状态、校验、模板流程和默认实现，把变化点留给子类。
- 具体类层：完成协议、DAO、Controller、Rule Node、工具或测试场景中的最终业务动作。

## 抽象了什么

- 父类/接口抽象公共生命周期、输入输出契约、错误处理、协议适配、DAO 查询形态、消息处理模板或测试夹具。
- 子类保留具体协议、实体类型、规则节点行为、数据库实现、页面/测试步骤或命令参数差异。
- 对聚合或无 Java 模块，抽象体现在 Maven 子模块划分和构建生命周期，而不是 Java 继承。

## 父类负责什么

- 提供稳定方法签名、共享字段、默认流程、通用校验、资源释放和框架回调入口。
- 在 Spring、Netty、DAO、Rule Engine、Transport 等框架中，父类还负责让运行时可以通过统一类型调度不同实现。

## 子类负责什么

- 实现具体业务差异，例如协议解析、实体 DAO、规则节点处理、Controller API、客户端命令或测试用例。
- 覆盖父类预留的扩展点，把模块特有数据转换、数据库查询、消息发送或外部调用补进去。

## 为什么不用组合

- 继承用于框架生命周期和模板方法：运行时需要把子类当作父类处理，例如 Spring Bean、Netty Handler、DAO Repository、Rule Node 或测试基类。
- 组合适合注入协作者，本模块中仍然通过字段依赖使用组合；但当需要统一回调签名、共享模板流程或多态派发时，单纯组合不能替代继承。

## 为什么不用接口

- 接口只能表达契约，不能集中保存公共状态、默认校验、资源关闭和模板流程。
- 当模块只需要契约时会使用接口；当多个实现还需要共享代码、默认行为或受保护扩展点时使用抽象类。

## 模板方法

- `TbActorError.isUnrecoverable()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbActorError.java`)
- `TbActorMsg.getMsgType()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbActorMsg.java`)
- `TbActorMsg.onTbActorStopped()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbActorMsg.java`)
- `CustomerAwareMsg.getCustomerId()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/aware/CustomerAwareMsg.java`)
- `DeviceAwareMsg.getDeviceId()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/aware/DeviceAwareMsg.java`)
- `NodeAwareMsg.getNodeId()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/aware/NodeAwareMsg.java`)
- `RuleChainAwareMsg.getRuleChainId()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/aware/RuleChainAwareMsg.java`)
- `RuleChainAwareMsg.getMsg()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/aware/RuleChainAwareMsg.java`)
- `TenantAwareMsg.getTenantId()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/aware/TenantAwareMsg.java`)
- `NotificationRuleProcessor.process()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/notification/NotificationRuleProcessor.java`)
- `ComponentLifecycleListener.onComponentLifecycleMsg()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/plugin/ComponentLifecycleListener.java`)
- `TbCallback.onSuccess()` (public, `common/message/src/main/java/org/thingsboard/server/common/msg/queue/TbCallback.java`)
- `TbCallback.onFailure()` (public, `common/message/src/main/java/org/thingsboard/server/common/msg/queue/TbCallback.java`)
- `TbCallback.onSuccess()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/queue/TbCallback.java`)
- `TbCallback.onFailure()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/queue/TbCallback.java`)
- `TbMsgCallback.onSuccess()` (public, `common/message/src/main/java/org/thingsboard/server/common/msg/queue/TbMsgCallback.java`)
- `TbMsgCallback.onFailure()` (public, `common/message/src/main/java/org/thingsboard/server/common/msg/queue/TbMsgCallback.java`)
- `TbMsgCallback.onSuccess()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/queue/TbMsgCallback.java`)
- `TbMsgCallback.onFailure()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/queue/TbMsgCallback.java`)
- `TbMsgCallback.onRateLimit()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/queue/TbMsgCallback.java`)
- `TbMsgCallback.isMsgValid()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/queue/TbMsgCallback.java`)
- `TbMsgCallback.onProcessingStart()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/queue/TbMsgCallback.java`)
- `TbMsgCallback.onProcessingEnd()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/queue/TbMsgCallback.java`)


## 哪些方法可以重写

- `EncryptionUtil.SHA3Digest()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/EncryptionUtil.java`)
- `EncryptionUtil.StringBuilder()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/EncryptionUtil.java`)
- `EncryptionUtil.getSha3Hash()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/EncryptionUtil.java`)
- `TbActorError.isUnrecoverable()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbActorError.java`)
- `TbActorMsg.getMsgType()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbActorMsg.java`)
- `TbActorMsg.onTbActorStopped()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbActorMsg.java`)
- `TbMsg.getAndIncrementRuleNodeCounter()` (public, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.newMsg()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.newMsg()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.newMsg()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.newMsg()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.newMsg()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.newMsg()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.newMsg()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.newMsg()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.CustomerId()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.TbMsgProcessingCtx()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.TbMsgMetaData()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.UUID()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.CustomerId()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.RuleChainId()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.RuleNodeId()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.TbMsgProcessingCtx()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.IllegalStateException()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.copyWithRuleChainId()` (public, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.copyWithRuleChainId()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.copyWithRuleChainId()` (public, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.copyWithRuleNodeId()` (public, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.copyWithNewCtx()` (public, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.getCallback()` (public, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.pushToStack()` (public, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.popFormStack()` (public, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.isValid()` (public, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.getCallback()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.getMetaDataTs()` (public, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.isTypeOf()` (public, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.isTypeOneOf()` (public, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsgMetaData.getValue()` (public, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsgMetaData.java`)
- `TbMsgMetaData.putValue()` (public, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsgMetaData.java`)
- `TbMsgMetaData.values()` (public, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsgMetaData.java`)


## 哪些方法必须重写

- `EncryptionUtil.SHA3Digest()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/EncryptionUtil.java`)
- `EncryptionUtil.StringBuilder()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/EncryptionUtil.java`)
- `EncryptionUtil.getSha3Hash()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/EncryptionUtil.java`)
- `TbActorError.isUnrecoverable()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbActorError.java`)
- `TbActorMsg.getMsgType()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbActorMsg.java`)
- `TbActorMsg.onTbActorStopped()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbActorMsg.java`)
- `TbMsg.newMsg()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.newMsg()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.newMsg()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.newMsg()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.newMsg()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.newMsg()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.newMsg()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.newMsg()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.CustomerId()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.TbMsgProcessingCtx()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.TbMsgMetaData()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.UUID()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.CustomerId()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.RuleChainId()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.RuleNodeId()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.TbMsgProcessingCtx()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.IllegalStateException()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.copyWithRuleChainId()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsg.getCallback()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsg.java`)
- `TbMsgProcessingCtx.AtomicInteger()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsgProcessingCtx.java`)
- `TbMsgProcessingCtx.TbMsgProcessingStackItem()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/TbMsgProcessingCtx.java`)
- `CustomerAwareMsg.getCustomerId()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/aware/CustomerAwareMsg.java`)
- `DeviceAwareMsg.getDeviceId()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/aware/DeviceAwareMsg.java`)
- `NodeAwareMsg.getNodeId()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/aware/NodeAwareMsg.java`)
- `RuleChainAwareMsg.getRuleChainId()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/aware/RuleChainAwareMsg.java`)
- `RuleChainAwareMsg.getMsg()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/aware/RuleChainAwareMsg.java`)
- `TenantAwareMsg.getTenantId()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/aware/TenantAwareMsg.java`)
- `NotificationRuleProcessor.process()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/notification/NotificationRuleProcessor.java`)
- `ComponentLifecycleListener.onComponentLifecycleMsg()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/plugin/ComponentLifecycleListener.java`)
- `QueueToRuleEngineMsg.RuleEngineException()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/queue/QueueToRuleEngineMsg.java`)
- `RuleEngineException.ObjectMapper()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/queue/RuleEngineException.java`)
- `RuleEngineException.RuntimeException()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/queue/RuleEngineException.java`)
- `RuleNodeException.RuleChainId()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/queue/RuleNodeException.java`)
- `RuleNodeException.RuleNodeId()` (package, `common/message/src/main/java/org/thingsboard/server/common/msg/queue/RuleNodeException.java`)


## 哪些地方使用了多态

- `Object/外部框架` -> `EncryptionUtil` (extends)
- `Object/外部框架` -> `TbMsg` (extends)
- `Serializable` -> `TbMsg` (implements)
- `Object/外部框架` -> `TbMsgMetaData` (extends)
- `Serializable` -> `TbMsgMetaData` (implements)
- `Object/外部框架` -> `TbMsgProcessingCtx` (extends)
- `Serializable` -> `TbMsgProcessingCtx` (implements)
- `Object/外部框架` -> `TbMsgProcessingStackItem` (extends)
- `Serializable` -> `TbMsgProcessingStackItem` (implements)
- `Object/外部框架` -> `TbRuleEngineActorMsg` (extends)
- `TbActorMsg` -> `TbRuleEngineActorMsg` (implements)
- `TbActorMsg` -> `ToDeviceActorNotificationMsg` (extends)
- `TenantAwareMsg` -> `ToDeviceActorNotificationMsg` (extends)
- `DeviceAwareMsg` -> `ToDeviceActorNotificationMsg` (extends)
- `Serializable` -> `ToDeviceActorNotificationMsg` (extends)
- `TbActorMsg` -> `DeviceAwareMsg` (extends)
- `TbActorMsg` -> `RuleChainAwareMsg` (extends)
- `TbActorMsg` -> `TenantAwareMsg` (extends)
- `Serializable` -> `ToAllNodesMsg` (extends)
- `TbActorMsg` -> `ToAllNodesMsg` (extends)
- `Object/外部框架` -> `EdgeEventUpdateMsg` (extends)
- `EdgeSessionMsg` -> `EdgeEventUpdateMsg` (implements)
- `TenantAwareMsg` -> `EdgeSessionMsg` (extends)
- `ToAllNodesMsg` -> `EdgeSessionMsg` (extends)
- `Object/外部框架` -> `FromEdgeSyncResponse` (extends)
- `EdgeSessionMsg` -> `FromEdgeSyncResponse` (implements)
- `Object/外部框架` -> `ToEdgeSyncRequest` (extends)
- `EdgeSessionMsg` -> `ToEdgeSyncRequest` (implements)
- `Object/外部框架` -> `ComponentLifecycleMsg` (extends)
- `TenantAwareMsg` -> `ComponentLifecycleMsg` (implements)
- `ToAllNodesMsg` -> `ComponentLifecycleMsg` (implements)
- `ComponentLifecycleMsg` -> `RuleNodeUpdatedMsg` (extends)
- `Object/外部框架` -> `PartitionChangeMsg` (extends)
- `TbActorMsg` -> `PartitionChangeMsg` (implements)
- `TbRuleEngineActorMsg` -> `QueueToRuleEngineMsg` (extends)
- `Exception` -> `RuleEngineException` (extends)
- `RuleEngineException` -> `RuleNodeException` (extends)
- `Object/外部框架` -> `RuleNodeInfo` (extends)
- `Object/外部框架` -> `TopicPartitionInfo` (extends)
- `Object/外部框架` -> `FromDeviceRpcResponse` (extends)
- 其余 34 项已省略；完整源码证据可通过本模块 Java 文件继续追踪。


## 外部父类/接口

- `AbstractRateLimitException`
- `Exception`
- `Object/外部框架`
- `Serializable`


## 整个继承体系解决了什么问题

该继承体系把“稳定生命周期/契约”和“模块特定行为”分开：父类或接口负责统一入口、共享流程和多态调度，子类负责差异化协议、实体、规则、DAO、测试或工具行为。这样可以让上游模块通过父类型调用下游实现，同时保持每个实现只关注自己的变化点。
