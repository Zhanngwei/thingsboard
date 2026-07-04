# Thingsboard Rule Engine Components 模块继承体系分析

> 生成范围：`rule-engine/rule-engine-components`  
> Maven artifact：`rule-engine-components`  
> Java 类型数量：293  
> 分析方式：静态扫描 `class/interface/enum/record` 的 `extends`、`implements`、方法签名和源码触点。

## 完整继承树

```text
AbstractListeningExecutor
├── DBCallbackExecutor
├── RuleDispatcherExecutor
CacheLoader
├── CustomerCacheLoader
├── EntityCacheLoader
ClientCredentials
├── «implements» AnonymousCredentials
├── «implements» BasicCredentials
├── «implements» CertPemCredentials
├── «implements» ├── AzureIotHubSasCredentials
DynamicPredicateValueCtx
├── «implements» DynamicPredicateValueCtxImpl
FutureCallback
├── «implements» TelemetryNodeCallback
├── «implements» ├── AttributesDeleteNodeCallback
├── «implements» ├── AttributesUpdateNodeCallback
ListeningExecutor
├── «implements» TestDbCallbackExecutor
NodeConfiguration
├── «implements» BaseTbMsgPushNodeConfiguration
├── «implements» ├── TbMsgPushToCloudNodeConfiguration
├── «implements» ├── TbMsgPushToEdgeNodeConfiguration
├── «implements» CalculateDeltaNodeConfiguration
├── «implements» TbAssignToCustomerNodeConfiguration
├── «implements» TbChangeOriginatorNodeConfiguration
├── «implements» TbCheckAlarmStatusNodeConfig
├── «implements» TbCheckMessageNodeConfiguration
├── «implements» TbCheckRelationNodeConfiguration
├── «implements» TbClearAlarmNodeConfiguration
├── «implements» TbCopyKeysNodeConfiguration
├── «implements» TbCreateAlarmNodeConfiguration
├── «implements» TbCreateRelationNodeConfiguration
├── «implements» TbDeleteKeysNodeConfiguration
├── «implements» TbDeleteRelationNodeConfiguration
├── «implements» TbDeviceProfileNodeConfiguration
├── «implements» TbDeviceStateNodeConfiguration
├── «implements» TbFetchDeviceCredentialsNodeConfiguration
├── «implements» TbGetAttributesNodeConfiguration
├── «implements» ├── TbGetDeviceAttrNodeConfiguration
├── «implements» TbGetCustomerDetailsNodeConfiguration
├── «implements» TbGetEntityDataNodeConfiguration
├── «implements» ├── TbGetRelatedDataNodeConfiguration
├── «implements» TbGetOriginatorFieldsConfiguration
├── «implements» TbGetTelemetryNodeConfiguration
├── «implements» TbGetTenantDetailsNodeConfiguration
├── «implements» TbGpsGeofencingFilterNodeConfiguration
├── «implements» ├── TbGpsGeofencingActionNodeConfiguration
├── «implements» TbJsFilterNodeConfiguration
├── «implements» TbJsSwitchNodeConfiguration
├── «implements» TbJsonPathNodeConfiguration
├── «implements» TbKafkaNodeConfiguration
├── «implements» TbLogNodeConfiguration
├── «implements» TbMathNodeConfiguration
├── «implements» TbMqttNodeConfiguration
├── «implements» ├── TbAzureIotHubNodeConfiguration
├── «implements» TbMsgAttributesNodeConfiguration
├── «implements» TbMsgCountNodeConfiguration
├── «implements» TbMsgDeduplicationNodeConfiguration
├── «implements» TbMsgDelayNodeConfiguration
├── «implements» TbMsgDeleteAttributesNodeConfiguration
├── «implements» TbMsgGeneratorNodeConfiguration
├── «implements» TbMsgTimeseriesNodeConfiguration
├── «implements» TbMsgToEmailNodeConfiguration
├── «implements» TbMsgTypeFilterNodeConfiguration
├── «implements» TbNotificationNodeConfiguration
├── «implements» TbOriginatorTypeFilterNodeConfiguration
├── «implements» TbPubSubNodeConfiguration
├── «implements» TbRabbitMqNodeConfiguration
├── «implements» TbRenameKeysNodeConfiguration
├── «implements» TbRestApiCallNodeConfiguration
├── «implements» TbRuleChainInputNodeConfiguration
├── «implements» TbSaveToCustomCassandraTableNodeConfiguration
├── «implements» TbSendEmailNodeConfiguration
├── «implements» TbSendRpcReplyNodeConfiguration
├── «implements» TbSendRpcRequestNodeConfiguration
├── «implements» TbSendSmsNodeConfiguration
├── «implements» TbSlackNodeConfiguration
├── «implements» TbSnsNodeConfiguration
├── «implements» TbSqsNodeConfiguration
├── «implements» TbTransformMsgNodeConfiguration
├── «implements» TbUnassignFromCustomerNodeConfiguration
Object/外部框架
├── AbstractGeofencingNode
├── ├── TbGpsGeofencingActionNode
├── ├── TbGpsGeofencingFilterNode
├── AbstractRuleNodeUpgradeTest
├── ├── TbCheckpointNodeTest
├── ├── TbGpsGeofencingActionNodeTest
├── ├── TbMsgAttributesNodeTest
├── ├── TbMsgDeduplicationNodeTest
├── ├── TbMsgGeneratorNodeTest
├── AbstractTbMsgPushNode
├── ├── TbMsgPushToCloudNode
├── ├── TbMsgPushToEdgeNode
├── AlarmRuleState
├── AlarmRuleStateTest
├── AlarmState
├── AlarmStateTest
├── AnonymousCredentials
├── BaseTbMsgPushNodeConfiguration
├── ├── TbMsgPushToCloudNodeConfiguration
├── ├── TbMsgPushToEdgeNodeConfiguration
├── BasicCredentials
├── CalculateDeltaNode
├── CalculateDeltaNodeConfiguration
├── CalculateDeltaNodeTest
├── CertPemCredentials
├── ├── AzureIotHubSasCredentials
├── CertPemCredentialsTest
├── Coordinates
├── CustomerKey
├── DataSnapshot
├── DeduplicationData
├── DeviceRelationsQuery
├── DeviceState
├── DeviceStateTest
├── DynamicPredicateValueCtxImpl
├── EntitiesAlarmOriginatorIdAsyncLoader
├── EntitiesByNameAndTypeLoader
├── EntitiesCustomerIdAsyncLoader
├── EntitiesCustomerIdAsyncLoaderTest
├── EntitiesFieldsAsyncLoader
├── EntitiesFieldsAsyncLoaderTest
├── EntitiesRelatedDeviceIdAsyncLoader
├── EntitiesRelatedDeviceIdAsyncLoaderTest
├── EntitiesRelatedEntityIdAsyncLoader
├── EntitiesRelatedEntityIdAsyncLoaderTest
├── EntityContainer
├── EntityGeofencingState
├── EntityKey
├── EntityKeyValue
├── GeoUtil
├── GeoUtilTest
├── GpsGeofencingActionTestCase
├── GpsGeofencingEvents
├── Interval
├── ListMatcher
├── MailBodyTypeTestConfig
├── MultipleTbMsgsCallbackWrapper
├── Perimeter
├── PersistedAlarmRuleState
├── PersistedAlarmState
├── PersistedDeviceState
├── ProfileState
├── RelationContainer
├── RelationsQuery
├── SearchDirectionIds
├── SemaphoreWithQueue
├── SnapshotUpdate
├── TbAbstractAlarmNode
├── ├── TbClearAlarmNode
├── ├── TbCreateAlarmNode
├── TbAbstractAlarmNodeConfiguration
├── ├── TbClearAlarmNodeConfiguration
├── ├── TbCreateAlarmNodeConfiguration
├── TbAbstractCustomerActionNode
├── ├── TbAssignToCustomerNode
├── ├── TbUnassignFromCustomerNode
├── TbAbstractCustomerActionNodeConfiguration
├── ├── TbAssignToCustomerNodeConfiguration
├── ├── TbUnassignFromCustomerNodeConfiguration
├── TbAbstractExternalNode
├── ├── TbKafkaNode
├── ├── TbMqttNode
├── ├── ├── TbAzureIotHubNode
├── ├── TbNotificationNode
├── ├── TbPubSubNode
├── ├── TbRabbitMqNode
├── ├── TbRestApiCallNode
├── ├── TbSendEmailNode
├── ├── TbSendSmsNode
├── ├── TbSlackNode
├── ├── TbSnsNode
├── ├── TbSqsNode
├── TbAbstractFetchToNodeConfiguration
├── ├── TbAbstractGetEntityDetailsNodeConfiguration
├── ├── ├── TbGetCustomerDetailsNodeConfiguration
├── ├── ├── TbGetTenantDetailsNodeConfiguration
├── ├── TbFetchDeviceCredentialsNodeConfiguration
├── ├── TbGetAttributesNodeConfiguration
├── ├── ├── TbGetDeviceAttrNodeConfiguration
├── ├── TbGetMappedDataNodeConfiguration
├── ├── ├── TbGetEntityDataNodeConfiguration
├── ├── ├── ├── TbGetRelatedDataNodeConfiguration
├── ├── ├── TbGetOriginatorFieldsConfiguration
├── TbAbstractGetAttributesNode
├── ├── TbGetAttributesNode
├── ├── TbGetDeviceAttrNode
├── TbAbstractGetEntityDataNode
├── ├── TbGetCustomerAttributeNode
├── ├── TbGetRelatedAttributeNode
├── ├── TbGetTenantAttributeNode
├── TbAbstractGetEntityDetailsNode
├── ├── TbGetCustomerDetailsNode
├── ├── TbGetTenantDetailsNode
├── TbAbstractGetMappedDataNode
├── ├── TbGetOriginatorFieldsNode
├── TbAbstractNodeWithFetchTo
├── ├── TbFetchDeviceCredentialsNode
├── TbAbstractRelationActionNode
├── ├── TbCreateRelationNode
├── ├── TbDeleteRelationNode
├── TbAbstractRelationActionNodeConfiguration
├── ├── TbCreateRelationNodeConfiguration
├── ├── TbDeleteRelationNodeConfiguration
├── TbAbstractTransformNode
├── ├── TbChangeOriginatorNode
├── ├── TbTransformMsgNode
├── TbAbstractTransformNodeWithTbMsgSource
├── ├── TbCopyKeysNode
├── ├── TbDeleteKeysNode
├── ├── TbRenameKeysNode
├── TbAbstractTypeSwitchNode
├── ├── TbAssetTypeSwitchNode
├── ├── TbDeviceTypeSwitchNode
├── ├── TbOriginatorTypeSwitchNode
├── TbAckNode
├── TbAlarmNodeTest
├── TbAlarmResult
├── TbAssetTypeSwitchNodeTest
├── TbChangeOriginatorNodeConfiguration
├── TbChangeOriginatorNodeTest
├── TbCheckAlarmStatusNode
├── TbCheckAlarmStatusNodeConfig
├── TbCheckAlarmStatusNodeTest
├── TbCheckMessageNode
├── TbCheckMessageNodeConfiguration
├── TbCheckMessageNodeTest
├── TbCheckRelationNode
├── TbCheckRelationNodeConfiguration
├── TbCheckRelationNodeTest
├── TbCheckpointNode
├── TbCopyAttributesToEntityViewNode
├── TbCopyKeysNodeConfiguration
├── TbCopyKeysNodeTest
├── TbCreateRelationNodeTest
├── TbDeleteKeysNodeConfiguration
├── TbDeleteKeysNodeTest
├── TbDeviceProfileNode
├── TbDeviceProfileNodeConfiguration
├── TbDeviceProfileNodeTest
├── TbDeviceStateNode
├── TbDeviceStateNodeConfiguration
├── TbDeviceStateNodeTest
├── TbDeviceTypeSwitchNodeTest
├── TbFetchDeviceCredentialsNodeTest
├── TbGetAttributesNodeTest
├── TbGetCustomerAttributeNodeTest
├── TbGetCustomerDetailsNodeTest
├── TbGetDeviceAttrNodeTest
├── TbGetOriginatorFieldsNodeTest
├── TbGetRelatedAttributeNodeTest
├── TbGetTelemetryNode
├── TbGetTelemetryNodeConfiguration
├── TbGetTelemetryNodeTest
├── TbGetTenantAttributeNodeTest
├── TbGetTenantDetailsNodeTest
├── TbGpsGeofencingFilterNodeConfiguration
├── ├── TbGpsGeofencingActionNodeConfiguration
├── TbGpsGeofencingFilterNodeTest
├── TbHttpClient
├── TbHttpClientTest
├── TbJsFilterNode
├── TbJsFilterNodeConfiguration
├── TbJsFilterNodeTest
├── TbJsSwitchNode
├── TbJsSwitchNodeConfiguration
├── TbJsSwitchNodeTest
├── TbJsonPathNode
├── TbJsonPathNodeConfiguration
├── TbJsonPathNodeTest
├── TbKafkaNodeConfiguration
├── TbLogNode
├── TbLogNodeConfiguration
├── TbLogNodeTest
├── TbMathArgument
├── TbMathArgumentValue
├── TbMathArgumentValueTest
├── TbMathNode
├── TbMathNodeConfiguration
├── TbMathNodeTest
├── TbMathResult
├── TbMqttNodeConfiguration
├── ├── TbAzureIotHubNodeConfiguration
├── TbMsgAttributesNode
├── TbMsgAttributesNodeConfiguration
├── TbMsgAttributesNodeConfigurationTest
├── TbMsgCountNode
├── TbMsgCountNodeConfiguration
├── TbMsgDeduplicationNode
├── TbMsgDeduplicationNodeConfiguration
├── TbMsgDelayNode
├── TbMsgDelayNodeConfiguration
├── TbMsgDeleteAttributesNode
├── TbMsgDeleteAttributesNodeConfiguration
├── TbMsgDeleteAttributesNodeTest
├── TbMsgGeneratorNode
├── TbMsgGeneratorNodeConfiguration
├── TbMsgPushToEdgeNodeTest
├── TbMsgTbContextBiFunction
├── TbMsgTimeseriesNode
├── TbMsgTimeseriesNodeConfiguration
├── TbMsgToEmailNode
├── TbMsgToEmailNodeConfiguration
├── TbMsgToEmailNodeTest
├── TbMsgTypeFilterNode
├── TbMsgTypeFilterNodeConfiguration
├── TbMsgTypeFilterNodeTest
├── TbMsgTypeSwitchNode
├── TbMsgTypeSwitchNodeTest
├── TbNotificationNodeConfiguration
├── TbOriginatorTypeFilterNode
├── TbOriginatorTypeFilterNodeConfiguration
├── TbOriginatorTypeFilterNodeTest
├── TbOriginatorTypeSwitchNodeTest
├── TbPubSubNodeConfiguration
├── TbRabbitMqNodeConfiguration
├── TbRenameKeysNodeConfiguration
├── TbRenameKeysNodeTest
├── TbRestApiCallNodeConfiguration
├── TbRestApiCallNodeTest
├── TbRuleChainInputNode
├── TbRuleChainInputNodeConfiguration
├── TbRuleChainOutputNode
├── TbSaveToCustomCassandraTableNode
├── TbSaveToCustomCassandraTableNodeConfiguration
├── TbSendEmailNodeConfiguration
├── TbSendRPCReplyNode
├── TbSendRPCReplyNodeTest
├── TbSendRPCRequestNode
├── TbSendRpcReplyNodeConfiguration
├── TbSendRpcRequestNodeConfiguration
├── TbSendSmsNodeConfiguration
├── TbSlackNodeConfiguration
├── TbSnsNodeConfiguration
├── TbSplitArrayMsgNode
├── TbSplitArrayMsgNodeTest
├── TbSqsNodeConfiguration
├── TbSynchronizationBeginNode
├── TbSynchronizationEndNode
├── TbTransformMsgNodeConfiguration
├── TbTransformMsgNodeTest
├── TelemetryNodeCallback
├── ├── AttributesDeleteNodeCallback
├── ├── AttributesUpdateNodeCallback
├── TenantIdLoader
├── TenantIdLoaderTest
├── TestDbCallbackExecutor
├── ValueWithTs
RuntimeException
├── NumericParseException
TbMsgCallbackWrapper
├── «implements» MultipleTbMsgsCallbackWrapper
TbNode
├── «implements» AbstractGeofencingNode
├── «implements» ├── TbGpsGeofencingActionNode
├── «implements» ├── TbGpsGeofencingFilterNode
├── «implements» AbstractTbMsgPushNode
├── «implements» ├── TbMsgPushToCloudNode
├── «implements» ├── TbMsgPushToEdgeNode
├── «implements» CalculateDeltaNode
├── «implements» TbAbstractAlarmNode
├── «implements» ├── TbClearAlarmNode
├── «implements» ├── TbCreateAlarmNode
├── «implements» TbAbstractCustomerActionNode
├── «implements» ├── TbAssignToCustomerNode
├── «implements» ├── TbUnassignFromCustomerNode
├── «implements» TbAbstractExternalNode
├── «implements» ├── TbKafkaNode
├── «implements» ├── TbMqttNode
├── «implements» ├── ├── TbAzureIotHubNode
├── «implements» ├── TbNotificationNode
├── «implements» ├── TbPubSubNode
├── «implements» ├── TbRabbitMqNode
├── «implements» ├── TbRestApiCallNode
├── «implements» ├── TbSendEmailNode
├── «implements» ├── TbSendSmsNode
├── «implements» ├── TbSlackNode
├── «implements» ├── TbSnsNode
├── «implements» ├── TbSqsNode
├── «implements» TbAbstractNodeWithFetchTo
├── «implements» ├── TbFetchDeviceCredentialsNode
├── «implements» TbAbstractRelationActionNode
├── «implements» ├── TbCreateRelationNode
├── «implements» ├── TbDeleteRelationNode
├── «implements» TbAbstractTransformNode
├── «implements» ├── TbChangeOriginatorNode
├── «implements» ├── TbTransformMsgNode
├── «implements» TbAbstractTransformNodeWithTbMsgSource
├── «implements» ├── TbCopyKeysNode
├── «implements» ├── TbDeleteKeysNode
├── «implements» ├── TbRenameKeysNode
├── «implements» TbAbstractTypeSwitchNode
├── «implements» ├── TbAssetTypeSwitchNode
├── «implements» ├── TbDeviceTypeSwitchNode
├── «implements» ├── TbOriginatorTypeSwitchNode
├── «implements» TbAckNode
├── «implements» TbCheckAlarmStatusNode
├── «implements» TbCheckMessageNode
├── «implements» TbCheckRelationNode
├── «implements» TbCheckpointNode
├── «implements» TbCopyAttributesToEntityViewNode
├── «implements» TbDeviceProfileNode
├── «implements» TbDeviceStateNode
├── «implements» TbGetTelemetryNode
├── «implements» TbJsFilterNode
├── «implements» TbJsSwitchNode
├── «implements» TbJsonPathNode
├── «implements» TbLogNode
├── «implements» TbMathNode
├── «implements» TbMsgAttributesNode
├── «implements» TbMsgCountNode
├── «implements» TbMsgDeduplicationNode
├── «implements» TbMsgDelayNode
├── «implements» TbMsgDeleteAttributesNode
├── «implements» TbMsgGeneratorNode
├── «implements» TbMsgTimeseriesNode
├── «implements» TbMsgToEmailNode
├── «implements» TbMsgTypeFilterNode
├── «implements» TbMsgTypeSwitchNode
├── «implements» TbOriginatorTypeFilterNode
├── «implements» TbRuleChainInputNode
├── «implements» TbRuleChainOutputNode
├── «implements» TbSaveToCustomCassandraTableNode
├── «implements» TbSendRPCReplyNode
├── «implements» TbSendRPCRequestNode
├── «implements» TbSplitArrayMsgNode
├── «implements» TbSynchronizationBeginNode
├── «implements» TbSynchronizationEndNode
```

## 继承图

```mermaid
flowchart TD
    Object______p0["Object/外部框架"] -->|extends| TbAbstractAlarmNode_c0["TbAbstractAlarmNode"]
    TbNode_p1["TbNode"] -->|implements| TbAbstractAlarmNode_c1["TbAbstractAlarmNode"]
    Object______p2["Object/外部框架"] -->|extends| TbAbstractAlarmNodeConfiguration_c2["TbAbstractAlarmNodeConfiguration"]
    Object______p3["Object/外部框架"] -->|extends| TbAbstractCustomerActionNode_c3["TbAbstractCustomerActionNode"]
    TbNode_p4["TbNode"] -->|implements| TbAbstractCustomerActionNode_c4["TbAbstractCustomerActionNode"]
    Object______p5["Object/外部框架"] -->|extends| CustomerKey_c5["CustomerKey"]
    CacheLoader_p6["CacheLoader"] -->|extends| CustomerCacheLoader_c6["CustomerCacheLoader"]
    Object______p7["Object/外部框架"] -->|extends| TbAbstractCustomerActionNodeConfiguration_c7["TbAbstractCustomerActionNodeConfiguration"]
    Object______p8["Object/外部框架"] -->|extends| TbAbstractRelationActionNode_c8["TbAbstractRelationActionNode"]
    TbNode_p9["TbNode"] -->|implements| TbAbstractRelationActionNode_c9["TbAbstractRelationActionNode"]
    Object______p10["Object/外部框架"] -->|extends| EntityKey_c10["EntityKey"]
    Object______p11["Object/外部框架"] -->|extends| SearchDirectionIds_c11["SearchDirectionIds"]
    CacheLoader_p12["CacheLoader"] -->|extends| EntityCacheLoader_c12["EntityCacheLoader"]
    Object______p13["Object/外部框架"] -->|extends| RelationContainer_c13["RelationContainer"]
    Object______p14["Object/外部框架"] -->|extends| TbAbstractRelationActionNodeConfiguration_c14["TbAbstractRelationActionNodeConfiguration"]
    Object______p15["Object/外部框架"] -->|extends| TbAlarmResult_c15["TbAlarmResult"]
    TbAbstractCustomerActionNode_p16["TbAbstractCustomerActionNode"] -->|extends| TbAssignToCustomerNode_c16["TbAssignToCustomerNode"]
    TbAbstractCustomerActionNodeConfiguration_p17["TbAbstractCustomerActionNodeConfiguration"] -->|extends| TbAssignToCustomerNodeConfiguration_c17["TbAssignToCustomerNodeConfiguration"]
    NodeConfiguration_p18["NodeConfiguration"] -->|implements| TbAssignToCustomerNodeConfiguration_c18["TbAssignToCustomerNodeConfiguration"]
    TbAbstractAlarmNode_p19["TbAbstractAlarmNode"] -->|extends| TbClearAlarmNode_c19["TbClearAlarmNode"]
    TbAbstractAlarmNodeConfiguration_p20["TbAbstractAlarmNodeConfiguration"] -->|extends| TbClearAlarmNodeConfiguration_c20["TbClearAlarmNodeConfiguration"]
    NodeConfiguration_p21["NodeConfiguration"] -->|implements| TbClearAlarmNodeConfiguration_c21["TbClearAlarmNodeConfiguration"]
    Object______p22["Object/外部框架"] -->|extends| TbCopyAttributesToEntityViewNode_c22["TbCopyAttributesToEntityViewNode"]
    TbNode_p23["TbNode"] -->|implements| TbCopyAttributesToEntityViewNode_c23["TbCopyAttributesToEntityViewNode"]
    TbAbstractAlarmNode_p24["TbAbstractAlarmNode"] -->|extends| TbCreateAlarmNode_c24["TbCreateAlarmNode"]
    TbAbstractAlarmNodeConfiguration_p25["TbAbstractAlarmNodeConfiguration"] -->|extends| TbCreateAlarmNodeConfiguration_c25["TbCreateAlarmNodeConfiguration"]
    NodeConfiguration_p26["NodeConfiguration"] -->|implements| TbCreateAlarmNodeConfiguration_c26["TbCreateAlarmNodeConfiguration"]
    TbAbstractRelationActionNode_p27["TbAbstractRelationActionNode"] -->|extends| TbCreateRelationNode_c27["TbCreateRelationNode"]
    TbAbstractRelationActionNodeConfiguration_p28["TbAbstractRelationActionNodeConfiguration"] -->|extends| TbCreateRelationNodeConfiguration_c28["TbCreateRelationNodeConfiguration"]
    NodeConfiguration_p29["NodeConfiguration"] -->|implements| TbCreateRelationNodeConfiguration_c29["TbCreateRelationNodeConfiguration"]
    TbAbstractRelationActionNode_p30["TbAbstractRelationActionNode"] -->|extends| TbDeleteRelationNode_c30["TbDeleteRelationNode"]
    TbAbstractRelationActionNodeConfiguration_p31["TbAbstractRelationActionNodeConfiguration"] -->|extends| TbDeleteRelationNodeConfiguration_c31["TbDeleteRelationNodeConfiguration"]
    NodeConfiguration_p32["NodeConfiguration"] -->|implements| TbDeleteRelationNodeConfiguration_c32["TbDeleteRelationNodeConfiguration"]
    Object______p33["Object/外部框架"] -->|extends| TbDeviceStateNode_c33["TbDeviceStateNode"]
    TbNode_p34["TbNode"] -->|implements| TbDeviceStateNode_c34["TbDeviceStateNode"]
    Object______p35["Object/外部框架"] -->|extends| TbDeviceStateNodeConfiguration_c35["TbDeviceStateNodeConfiguration"]
    NodeConfiguration_p36["NodeConfiguration"] -->|implements| TbDeviceStateNodeConfiguration_c36["TbDeviceStateNodeConfiguration"]
    Object______p37["Object/外部框架"] -->|extends| TbLogNode_c37["TbLogNode"]
    TbNode_p38["TbNode"] -->|implements| TbLogNode_c38["TbLogNode"]
    Object______p39["Object/外部框架"] -->|extends| TbLogNodeConfiguration_c39["TbLogNodeConfiguration"]
    NodeConfiguration_p40["NodeConfiguration"] -->|implements| TbLogNodeConfiguration_c40["TbLogNodeConfiguration"]
    Object______p41["Object/外部框架"] -->|extends| TbMsgCountNode_c41["TbMsgCountNode"]
    TbNode_p42["TbNode"] -->|implements| TbMsgCountNode_c42["TbMsgCountNode"]
    Object______p43["Object/外部框架"] -->|extends| TbMsgCountNodeConfiguration_c43["TbMsgCountNodeConfiguration"]
    NodeConfiguration_p44["NodeConfiguration"] -->|implements| TbMsgCountNodeConfiguration_c44["TbMsgCountNodeConfiguration"]
    Object______p45["Object/外部框架"] -->|extends| TbSaveToCustomCassandraTableNode_c45["TbSaveToCustomCassandraTableNode"]
    TbNode_p46["TbNode"] -->|implements| TbSaveToCustomCassandraTableNode_c46["TbSaveToCustomCassandraTableNode"]
    Object______p47["Object/外部框架"] -->|extends| TbSaveToCustomCassandraTableNodeConfiguration_c47["TbSaveToCustomCassandraTableNodeConfiguration"]
    NodeConfiguration_p48["NodeConfiguration"] -->|implements| TbSaveToCustomCassandraTableNodeConfiguration_c48["TbSaveToCustomCassandraTableNodeConfiguration"]
    TbAbstractCustomerActionNode_p49["TbAbstractCustomerActionNode"] -->|extends| TbUnassignFromCustomerNode_c49["TbUnassignFromCustomerNode"]
    TbAbstractCustomerActionNodeConfiguration_p50["TbAbstractCustomerActionNodeConfiguration"] -->|extends| TbUnassignFromCustomerNodeConfiguration_c50["TbUnassignFromCustomerNodeConfiguration"]
    NodeConfiguration_p51["NodeConfiguration"] -->|implements| TbUnassignFromCustomerNodeConfiguration_c51["TbUnassignFromCustomerNodeConfiguration"]
    TbAbstractExternalNode_p52["TbAbstractExternalNode"] -->|extends| TbSnsNode_c52["TbSnsNode"]
    Object______p53["Object/外部框架"] -->|extends| TbSnsNodeConfiguration_c53["TbSnsNodeConfiguration"]
    NodeConfiguration_p54["NodeConfiguration"] -->|implements| TbSnsNodeConfiguration_c54["TbSnsNodeConfiguration"]
    TbAbstractExternalNode_p55["TbAbstractExternalNode"] -->|extends| TbSqsNode_c55["TbSqsNode"]
    Object______p56["Object/外部框架"] -->|extends| TbSqsNodeConfiguration_c56["TbSqsNodeConfiguration"]
    NodeConfiguration_p57["NodeConfiguration"] -->|implements| TbSqsNodeConfiguration_c57["TbSqsNodeConfiguration"]
    Object______p58["Object/外部框架"] -->|extends| AnonymousCredentials_c58["AnonymousCredentials"]
    ClientCredentials_p59["ClientCredentials"] -->|implements| AnonymousCredentials_c59["AnonymousCredentials"]
    Object______p60["Object/外部框架"] -->|extends| BasicCredentials_c60["BasicCredentials"]
    ClientCredentials_p61["ClientCredentials"] -->|implements| BasicCredentials_c61["BasicCredentials"]
    Object______p62["Object/外部框架"] -->|extends| CertPemCredentials_c62["CertPemCredentials"]
    ClientCredentials_p63["ClientCredentials"] -->|implements| CertPemCredentials_c63["CertPemCredentials"]
    Object______p64["Object/外部框架"] -->|extends| DeviceRelationsQuery_c64["DeviceRelationsQuery"]
    Object______p65["Object/外部框架"] -->|extends| RelationsQuery_c65["RelationsQuery"]
    Object______p66["Object/外部框架"] -->|extends| TbMsgGeneratorNode_c66["TbMsgGeneratorNode"]
    TbNode_p67["TbNode"] -->|implements| TbMsgGeneratorNode_c67["TbMsgGeneratorNode"]
    Object______p68["Object/外部框架"] -->|extends| TbMsgGeneratorNodeConfiguration_c68["TbMsgGeneratorNodeConfiguration"]
    NodeConfiguration_p69["NodeConfiguration"] -->|implements| TbMsgGeneratorNodeConfiguration_c69["TbMsgGeneratorNodeConfiguration"]
    Object______p70["Object/外部框架"] -->|extends| DeduplicationData_c70["DeduplicationData"]
    Object______p71["Object/外部框架"] -->|extends| TbMsgDeduplicationNode_c71["TbMsgDeduplicationNode"]
    TbNode_p72["TbNode"] -->|implements| TbMsgDeduplicationNode_c72["TbMsgDeduplicationNode"]
    Object______p73["Object/外部框架"] -->|extends| TbMsgDeduplicationNodeConfiguration_c73["TbMsgDeduplicationNodeConfiguration"]
    NodeConfiguration_p74["NodeConfiguration"] -->|implements| TbMsgDeduplicationNodeConfiguration_c74["TbMsgDeduplicationNodeConfiguration"]
    Object______p75["Object/外部框架"] -->|extends| TbMsgDelayNode_c75["TbMsgDelayNode"]
    TbNode_p76["TbNode"] -->|implements| TbMsgDelayNode_c76["TbMsgDelayNode"]
    Object______p77["Object/外部框架"] -->|extends| TbMsgDelayNodeConfiguration_c77["TbMsgDelayNodeConfiguration"]
    NodeConfiguration_p78["NodeConfiguration"] -->|implements| TbMsgDelayNodeConfiguration_c78["TbMsgDelayNodeConfiguration"]
    Object______p79["Object/外部框架"] -->|extends| AbstractTbMsgPushNode_c79["AbstractTbMsgPushNode"]
    TbNode_p80["TbNode"] -->|implements| AbstractTbMsgPushNode_c80["AbstractTbMsgPushNode"]
    Object______p81["Object/外部框架"] -->|extends| BaseTbMsgPushNodeConfiguration_c81["BaseTbMsgPushNodeConfiguration"]
    NodeConfiguration_p82["NodeConfiguration"] -->|implements| BaseTbMsgPushNodeConfiguration_c82["BaseTbMsgPushNodeConfiguration"]
    AbstractTbMsgPushNode_p83["AbstractTbMsgPushNode"] -->|extends| TbMsgPushToCloudNode_c83["TbMsgPushToCloudNode"]
    BaseTbMsgPushNodeConfiguration_p84["BaseTbMsgPushNodeConfiguration"] -->|extends| TbMsgPushToCloudNodeConfiguration_c84["TbMsgPushToCloudNodeConfiguration"]
    AbstractTbMsgPushNode_p85["AbstractTbMsgPushNode"] -->|extends| TbMsgPushToEdgeNode_c85["TbMsgPushToEdgeNode"]
    BaseTbMsgPushNodeConfiguration_p86["BaseTbMsgPushNodeConfiguration"] -->|extends| TbMsgPushToEdgeNodeConfiguration_c86["TbMsgPushToEdgeNodeConfiguration"]
    Object______p87["Object/外部框架"] -->|extends| TbAbstractExternalNode_c87["TbAbstractExternalNode"]
    TbNode_p88["TbNode"] -->|implements| TbAbstractExternalNode_c88["TbAbstractExternalNode"]
    Object______p89["Object/外部框架"] -->|extends| TbAbstractTypeSwitchNode_c89["TbAbstractTypeSwitchNode"]
    TbNode_p90["TbNode"] -->|implements| TbAbstractTypeSwitchNode_c90["TbAbstractTypeSwitchNode"]
    TbAbstractTypeSwitchNode_p91["TbAbstractTypeSwitchNode"] -->|extends| TbAssetTypeSwitchNode_c91["TbAssetTypeSwitchNode"]
    Object______p92["Object/外部框架"] -->|extends| TbCheckAlarmStatusNode_c92["TbCheckAlarmStatusNode"]
    TbNode_p93["TbNode"] -->|implements| TbCheckAlarmStatusNode_c93["TbCheckAlarmStatusNode"]
    Object______p94["Object/外部框架"] -->|extends| TbCheckAlarmStatusNodeConfig_c94["TbCheckAlarmStatusNodeConfig"]
    NodeConfiguration_p95["NodeConfiguration"] -->|implements| TbCheckAlarmStatusNodeConfig_c95["TbCheckAlarmStatusNodeConfig"]
    Object______p96["Object/外部框架"] -->|extends| TbCheckMessageNode_c96["TbCheckMessageNode"]
    TbNode_p97["TbNode"] -->|implements| TbCheckMessageNode_c97["TbCheckMessageNode"]
    Object______p98["Object/外部框架"] -->|extends| TbCheckMessageNodeConfiguration_c98["TbCheckMessageNodeConfiguration"]
    NodeConfiguration_p99["NodeConfiguration"] -->|implements| TbCheckMessageNodeConfiguration_c99["TbCheckMessageNodeConfiguration"]
    Object______p100["Object/外部框架"] -->|extends| TbCheckRelationNode_c100["TbCheckRelationNode"]
    TbNode_p101["TbNode"] -->|implements| TbCheckRelationNode_c101["TbCheckRelationNode"]
    Object______p102["Object/外部框架"] -->|extends| TbCheckRelationNodeConfiguration_c102["TbCheckRelationNodeConfiguration"]
    NodeConfiguration_p103["NodeConfiguration"] -->|implements| TbCheckRelationNodeConfiguration_c103["TbCheckRelationNodeConfiguration"]
    TbAbstractTypeSwitchNode_p104["TbAbstractTypeSwitchNode"] -->|extends| TbDeviceTypeSwitchNode_c104["TbDeviceTypeSwitchNode"]
    Object______p105["Object/外部框架"] -->|extends| TbJsFilterNode_c105["TbJsFilterNode"]
    TbNode_p106["TbNode"] -->|implements| TbJsFilterNode_c106["TbJsFilterNode"]
    Object______p107["Object/外部框架"] -->|extends| TbJsFilterNodeConfiguration_c107["TbJsFilterNodeConfiguration"]
    NodeConfiguration_p108["NodeConfiguration"] -->|implements| TbJsFilterNodeConfiguration_c108["TbJsFilterNodeConfiguration"]
    Object______p109["Object/外部框架"] -->|extends| TbJsSwitchNode_c109["TbJsSwitchNode"]
    TbNode_p110["TbNode"] -->|implements| TbJsSwitchNode_c110["TbJsSwitchNode"]
    Object______p111["Object/外部框架"] -->|extends| TbJsSwitchNodeConfiguration_c111["TbJsSwitchNodeConfiguration"]
    NodeConfiguration_p112["NodeConfiguration"] -->|implements| TbJsSwitchNodeConfiguration_c112["TbJsSwitchNodeConfiguration"]
    Object______p113["Object/外部框架"] -->|extends| TbMsgTypeFilterNode_c113["TbMsgTypeFilterNode"]
    TbNode_p114["TbNode"] -->|implements| TbMsgTypeFilterNode_c114["TbMsgTypeFilterNode"]
    Object______p115["Object/外部框架"] -->|extends| TbMsgTypeFilterNodeConfiguration_c115["TbMsgTypeFilterNodeConfiguration"]
    NodeConfiguration_p116["NodeConfiguration"] -->|implements| TbMsgTypeFilterNodeConfiguration_c116["TbMsgTypeFilterNodeConfiguration"]
    Object______p117["Object/外部框架"] -->|extends| TbMsgTypeSwitchNode_c117["TbMsgTypeSwitchNode"]
    TbNode_p118["TbNode"] -->|implements| TbMsgTypeSwitchNode_c118["TbMsgTypeSwitchNode"]
    Object______p119["Object/外部框架"] -->|extends| TbOriginatorTypeFilterNode_c119["TbOriginatorTypeFilterNode"]
    TbNode_p120["TbNode"] -->|implements| TbOriginatorTypeFilterNode_c120["TbOriginatorTypeFilterNode"]
    Object______p121["Object/外部框架"] -->|extends| TbOriginatorTypeFilterNodeConfiguration_c121["TbOriginatorTypeFilterNodeConfiguration"]
    NodeConfiguration_p122["NodeConfiguration"] -->|implements| TbOriginatorTypeFilterNodeConfiguration_c122["TbOriginatorTypeFilterNodeConfiguration"]
    TbAbstractTypeSwitchNode_p123["TbAbstractTypeSwitchNode"] -->|extends| TbOriginatorTypeSwitchNode_c123["TbOriginatorTypeSwitchNode"]
    Object______p124["Object/外部框架"] -->|extends| TbAckNode_c124["TbAckNode"]
    TbNode_p125["TbNode"] -->|implements| TbAckNode_c125["TbAckNode"]
    Object______p126["Object/外部框架"] -->|extends| TbCheckpointNode_c126["TbCheckpointNode"]
    TbNode_p127["TbNode"] -->|implements| TbCheckpointNode_c127["TbCheckpointNode"]
    Object______p128["Object/外部框架"] -->|extends| TbRuleChainInputNode_c128["TbRuleChainInputNode"]
    TbNode_p129["TbNode"] -->|implements| TbRuleChainInputNode_c129["TbRuleChainInputNode"]
    Object______p130["Object/外部框架"] -->|extends| TbRuleChainInputNodeConfiguration_c130["TbRuleChainInputNodeConfiguration"]
    NodeConfiguration_p131["NodeConfiguration"] -->|implements| TbRuleChainInputNodeConfiguration_c131["TbRuleChainInputNodeConfiguration"]
    Object______p132["Object/外部框架"] -->|extends| TbRuleChainOutputNode_c132["TbRuleChainOutputNode"]
    TbNode_p133["TbNode"] -->|implements| TbRuleChainOutputNode_c133["TbRuleChainOutputNode"]
    TbAbstractExternalNode_p134["TbAbstractExternalNode"] -->|extends| TbPubSubNode_c134["TbPubSubNode"]
    Object______p135["Object/外部框架"] -->|extends| TbPubSubNodeConfiguration_c135["TbPubSubNodeConfiguration"]
    NodeConfiguration_p136["NodeConfiguration"] -->|implements| TbPubSubNodeConfiguration_c136["TbPubSubNodeConfiguration"]
    Object______p137["Object/外部框架"] -->|extends| AbstractGeofencingNode_c137["AbstractGeofencingNode"]
    TbNode_p138["TbNode"] -->|implements| AbstractGeofencingNode_c138["AbstractGeofencingNode"]
    Object______p139["Object/外部框架"] -->|extends| Coordinates_c139["Coordinates"]
    Object______p140["Object/外部框架"] -->|extends| EntityGeofencingState_c140["EntityGeofencingState"]
    Object______p141["Object/外部框架"] -->|extends| GeoUtil_c141["GeoUtil"]
    Object______p142["Object/外部框架"] -->|extends| Perimeter_c142["Perimeter"]
    AbstractGeofencingNode_p143["AbstractGeofencingNode"] -->|extends| TbGpsGeofencingActionNode_c143["TbGpsGeofencingActionNode"]
    TbGpsGeofencingFilterNodeConfiguration_p144["TbGpsGeofencingFilterNodeConfiguration"] -->|extends| TbGpsGeofencingActionNodeConfiguration_c144["TbGpsGeofencingActionNodeConfiguration"]
    AbstractGeofencingNode_p145["AbstractGeofencingNode"] -->|extends| TbGpsGeofencingFilterNode_c145["TbGpsGeofencingFilterNode"]
    Object______p146["Object/外部框架"] -->|extends| TbGpsGeofencingFilterNodeConfiguration_c146["TbGpsGeofencingFilterNodeConfiguration"]
    NodeConfiguration_p147["NodeConfiguration"] -->|implements| TbGpsGeofencingFilterNodeConfiguration_c147["TbGpsGeofencingFilterNodeConfiguration"]
    TbAbstractExternalNode_p148["TbAbstractExternalNode"] -->|extends| TbKafkaNode_c148["TbKafkaNode"]
    Object______p149["Object/外部框架"] -->|extends| TbKafkaNodeConfiguration_c149["TbKafkaNodeConfiguration"]
    NodeConfiguration_p150["NodeConfiguration"] -->|implements| TbKafkaNodeConfiguration_c150["TbKafkaNodeConfiguration"]
    Object______p151["Object/外部框架"] -->|extends| TbMsgToEmailNode_c151["TbMsgToEmailNode"]
    TbNode_p152["TbNode"] -->|implements| TbMsgToEmailNode_c152["TbMsgToEmailNode"]
    Object______p153["Object/外部框架"] -->|extends| TbMsgToEmailNodeConfiguration_c153["TbMsgToEmailNodeConfiguration"]
    NodeConfiguration_p154["NodeConfiguration"] -->|implements| TbMsgToEmailNodeConfiguration_c154["TbMsgToEmailNodeConfiguration"]
    TbAbstractExternalNode_p155["TbAbstractExternalNode"] -->|extends| TbSendEmailNode_c155["TbSendEmailNode"]
    Object______p156["Object/外部框架"] -->|extends| TbSendEmailNodeConfiguration_c156["TbSendEmailNodeConfiguration"]
    NodeConfiguration_p157["NodeConfiguration"] -->|implements| TbSendEmailNodeConfiguration_c157["TbSendEmailNodeConfiguration"]
    Object______p158["Object/外部框架"] -->|extends| TbMathArgument_c158["TbMathArgument"]
    Object______p159["Object/外部框架"] -->|extends| TbMathArgumentValue_c159["TbMathArgumentValue"]
    Object______p160["Object/外部框架"] -->|extends| TbMathNode_c160["TbMathNode"]
    TbNode_p161["TbNode"] -->|implements| TbMathNode_c161["TbMathNode"]
    Object______p162["Object/外部框架"] -->|extends| SemaphoreWithQueue_c162["SemaphoreWithQueue"]
    Object______p163["Object/外部框架"] -->|extends| TbMsgTbContextBiFunction_c163["TbMsgTbContextBiFunction"]
    Object______p164["Object/外部框架"] -->|extends| TbMathNodeConfiguration_c164["TbMathNodeConfiguration"]
    NodeConfiguration_p165["NodeConfiguration"] -->|implements| TbMathNodeConfiguration_c165["TbMathNodeConfiguration"]
    Object______p166["Object/外部框架"] -->|extends| TbMathResult_c166["TbMathResult"]
    Object______p167["Object/外部框架"] -->|extends| CalculateDeltaNode_c167["CalculateDeltaNode"]
    TbNode_p168["TbNode"] -->|implements| CalculateDeltaNode_c168["CalculateDeltaNode"]
    Object______p169["Object/外部框架"] -->|extends| ValueWithTs_c169["ValueWithTs"]
    Object______p170["Object/外部框架"] -->|extends| CalculateDeltaNodeConfiguration_c170["CalculateDeltaNodeConfiguration"]
    NodeConfiguration_p171["NodeConfiguration"] -->|implements| CalculateDeltaNodeConfiguration_c171["CalculateDeltaNodeConfiguration"]
    Object______p172["Object/外部框架"] -->|extends| TbAbstractFetchToNodeConfiguration_c172["TbAbstractFetchToNodeConfiguration"]
    Object______p173["Object/外部框架"] -->|extends| TbAbstractGetAttributesNode_c173["TbAbstractGetAttributesNode"]
    Object______p174["Object/外部框架"] -->|extends| TbAbstractGetEntityDataNode_c174["TbAbstractGetEntityDataNode"]
    Object______p175["Object/外部框架"] -->|extends| TbAbstractGetEntityDetailsNode_c175["TbAbstractGetEntityDetailsNode"]
    TbAbstractFetchToNodeConfiguration_p176["TbAbstractFetchToNodeConfiguration"] -->|extends| TbAbstractGetEntityDetailsNodeConfiguration_c176["TbAbstractGetEntityDetailsNodeConfiguration"]
    Object______p177["Object/外部框架"] -->|extends| TbAbstractGetMappedDataNode_c177["TbAbstractGetMappedDataNode"]
    Object______p178["Object/外部框架"] -->|extends| TbAbstractNodeWithFetchTo_c178["TbAbstractNodeWithFetchTo"]
    TbNode_p179["TbNode"] -->|implements| TbAbstractNodeWithFetchTo_c179["TbAbstractNodeWithFetchTo"]
    TbAbstractNodeWithFetchTo_p180["TbAbstractNodeWithFetchTo"] -->|extends| TbFetchDeviceCredentialsNode_c180["TbFetchDeviceCredentialsNode"]
    TbAbstractFetchToNodeConfiguration_p181["TbAbstractFetchToNodeConfiguration"] -->|extends| TbFetchDeviceCredentialsNodeConfiguration_c181["TbFetchDeviceCredentialsNodeConfiguration"]
    NodeConfiguration_p182["NodeConfiguration"] -->|implements| TbFetchDeviceCredentialsNodeConfiguration_c182["TbFetchDeviceCredentialsNodeConfiguration"]
    TbAbstractGetAttributesNode_p183["TbAbstractGetAttributesNode"] -->|extends| TbGetAttributesNode_c183["TbGetAttributesNode"]
    TbAbstractFetchToNodeConfiguration_p184["TbAbstractFetchToNodeConfiguration"] -->|extends| TbGetAttributesNodeConfiguration_c184["TbGetAttributesNodeConfiguration"]
    NodeConfiguration_p185["NodeConfiguration"] -->|implements| TbGetAttributesNodeConfiguration_c185["TbGetAttributesNodeConfiguration"]
    TbAbstractGetEntityDataNode_p186["TbAbstractGetEntityDataNode"] -->|extends| TbGetCustomerAttributeNode_c186["TbGetCustomerAttributeNode"]
    TbAbstractGetEntityDetailsNode_p187["TbAbstractGetEntityDetailsNode"] -->|extends| TbGetCustomerDetailsNode_c187["TbGetCustomerDetailsNode"]
    TbAbstractGetEntityDetailsNodeConfiguration_p188["TbAbstractGetEntityDetailsNodeConfiguration"] -->|extends| TbGetCustomerDetailsNodeConfiguration_c188["TbGetCustomerDetailsNodeConfiguration"]
    NodeConfiguration_p189["NodeConfiguration"] -->|implements| TbGetCustomerDetailsNodeConfiguration_c189["TbGetCustomerDetailsNodeConfiguration"]
    TbAbstractGetAttributesNode_p190["TbAbstractGetAttributesNode"] -->|extends| TbGetDeviceAttrNode_c190["TbGetDeviceAttrNode"]
    TbGetAttributesNodeConfiguration_p191["TbGetAttributesNodeConfiguration"] -->|extends| TbGetDeviceAttrNodeConfiguration_c191["TbGetDeviceAttrNodeConfiguration"]
    TbGetMappedDataNodeConfiguration_p192["TbGetMappedDataNodeConfiguration"] -->|extends| TbGetEntityDataNodeConfiguration_c192["TbGetEntityDataNodeConfiguration"]
    NodeConfiguration_p193["NodeConfiguration"] -->|implements| TbGetEntityDataNodeConfiguration_c193["TbGetEntityDataNodeConfiguration"]
    TbAbstractFetchToNodeConfiguration_p194["TbAbstractFetchToNodeConfiguration"] -->|extends| TbGetMappedDataNodeConfiguration_c194["TbGetMappedDataNodeConfiguration"]
    TbGetMappedDataNodeConfiguration_p195["TbGetMappedDataNodeConfiguration"] -->|extends| TbGetOriginatorFieldsConfiguration_c195["TbGetOriginatorFieldsConfiguration"]
    NodeConfiguration_p196["NodeConfiguration"] -->|implements| TbGetOriginatorFieldsConfiguration_c196["TbGetOriginatorFieldsConfiguration"]
    TbAbstractGetMappedDataNode_p197["TbAbstractGetMappedDataNode"] -->|extends| TbGetOriginatorFieldsNode_c197["TbGetOriginatorFieldsNode"]
    TbAbstractGetEntityDataNode_p198["TbAbstractGetEntityDataNode"] -->|extends| TbGetRelatedAttributeNode_c198["TbGetRelatedAttributeNode"]
    TbGetEntityDataNodeConfiguration_p199["TbGetEntityDataNodeConfiguration"] -->|extends| TbGetRelatedDataNodeConfiguration_c199["TbGetRelatedDataNodeConfiguration"]
    Object______p200["Object/外部框架"] -->|extends| TbGetTelemetryNode_c200["TbGetTelemetryNode"]
    TbNode_p201["TbNode"] -->|implements| TbGetTelemetryNode_c201["TbGetTelemetryNode"]
    Object______p202["Object/外部框架"] -->|extends| Interval_c202["Interval"]
    Object______p203["Object/外部框架"] -->|extends| TbGetTelemetryNodeConfiguration_c203["TbGetTelemetryNodeConfiguration"]
    NodeConfiguration_p204["NodeConfiguration"] -->|implements| TbGetTelemetryNodeConfiguration_c204["TbGetTelemetryNodeConfiguration"]
    TbAbstractGetEntityDataNode_p205["TbAbstractGetEntityDataNode"] -->|extends| TbGetTenantAttributeNode_c205["TbGetTenantAttributeNode"]
    TbAbstractGetEntityDetailsNode_p206["TbAbstractGetEntityDetailsNode"] -->|extends| TbGetTenantDetailsNode_c206["TbGetTenantDetailsNode"]
    TbAbstractGetEntityDetailsNodeConfiguration_p207["TbAbstractGetEntityDetailsNodeConfiguration"] -->|extends| TbGetTenantDetailsNodeConfiguration_c207["TbGetTenantDetailsNodeConfiguration"]
    NodeConfiguration_p208["NodeConfiguration"] -->|implements| TbGetTenantDetailsNodeConfiguration_c208["TbGetTenantDetailsNodeConfiguration"]
    TbAbstractExternalNode_p209["TbAbstractExternalNode"] -->|extends| TbMqttNode_c209["TbMqttNode"]
    Object______p210["Object/外部框架"] -->|extends| TbMqttNodeConfiguration_c210["TbMqttNodeConfiguration"]
    NodeConfiguration_p211["NodeConfiguration"] -->|implements| TbMqttNodeConfiguration_c211["TbMqttNodeConfiguration"]
    CertPemCredentials_p212["CertPemCredentials"] -->|extends| AzureIotHubSasCredentials_c212["AzureIotHubSasCredentials"]
    TbMqttNode_p213["TbMqttNode"] -->|extends| TbAzureIotHubNode_c213["TbAzureIotHubNode"]
    TbMqttNodeConfiguration_p214["TbMqttNodeConfiguration"] -->|extends| TbAzureIotHubNodeConfiguration_c214["TbAzureIotHubNodeConfiguration"]
    TbAbstractExternalNode_p215["TbAbstractExternalNode"] -->|extends| TbNotificationNode_c215["TbNotificationNode"]
    Object______p216["Object/外部框架"] -->|extends| TbNotificationNodeConfiguration_c216["TbNotificationNodeConfiguration"]
    NodeConfiguration_p217["NodeConfiguration"] -->|implements| TbNotificationNodeConfiguration_c217["TbNotificationNodeConfiguration"]
    TbAbstractExternalNode_p218["TbAbstractExternalNode"] -->|extends| TbSlackNode_c218["TbSlackNode"]
    Object______p219["Object/外部框架"] -->|extends| TbSlackNodeConfiguration_c219["TbSlackNodeConfiguration"]
    NodeConfiguration_p220["NodeConfiguration"] -->|implements| TbSlackNodeConfiguration_c220["TbSlackNodeConfiguration"]
    Object______p221["Object/外部框架"] -->|extends| AlarmRuleState_c221["AlarmRuleState"]
    Object______p222["Object/外部框架"] -->|extends| AlarmState_c222["AlarmState"]
    Object______p223["Object/外部框架"] -->|extends| DataSnapshot_c223["DataSnapshot"]
    Object______p224["Object/外部框架"] -->|extends| DeviceState_c224["DeviceState"]
    Object______p225["Object/外部框架"] -->|extends| DynamicPredicateValueCtxImpl_c225["DynamicPredicateValueCtxImpl"]
    DynamicPredicateValueCtx_p226["DynamicPredicateValueCtx"] -->|implements| DynamicPredicateValueCtxImpl_c226["DynamicPredicateValueCtxImpl"]
    Object______p227["Object/外部框架"] -->|extends| EntityKeyValue_c227["EntityKeyValue"]
    RuntimeException_p228["RuntimeException"] -->|extends| NumericParseException_c228["NumericParseException"]
    Object______p229["Object/外部框架"] -->|extends| ProfileState_c229["ProfileState"]
    Object______p230["Object/外部框架"] -->|extends| SnapshotUpdate_c230["SnapshotUpdate"]
    Object______p231["Object/外部框架"] -->|extends| TbDeviceProfileNode_c231["TbDeviceProfileNode"]
    TbNode_p232["TbNode"] -->|implements| TbDeviceProfileNode_c232["TbDeviceProfileNode"]
    Object______p233["Object/外部框架"] -->|extends| TbDeviceProfileNodeConfiguration_c233["TbDeviceProfileNodeConfiguration"]
    NodeConfiguration_p234["NodeConfiguration"] -->|implements| TbDeviceProfileNodeConfiguration_c234["TbDeviceProfileNodeConfiguration"]
    Object______p235["Object/外部框架"] -->|extends| PersistedAlarmRuleState_c235["PersistedAlarmRuleState"]
    Object______p236["Object/外部框架"] -->|extends| PersistedAlarmState_c236["PersistedAlarmState"]
    Object______p237["Object/外部框架"] -->|extends| PersistedDeviceState_c237["PersistedDeviceState"]
    TbAbstractExternalNode_p238["TbAbstractExternalNode"] -->|extends| TbRabbitMqNode_c238["TbRabbitMqNode"]
    Object______p239["Object/外部框架"] -->|extends| TbRabbitMqNodeConfiguration_c239["TbRabbitMqNodeConfiguration"]
    NodeConfiguration_p240["NodeConfiguration"] -->|implements| TbRabbitMqNodeConfiguration_c240["TbRabbitMqNodeConfiguration"]
    Object______p241["Object/外部框架"] -->|extends| TbHttpClient_c241["TbHttpClient"]
    TbAbstractExternalNode_p242["TbAbstractExternalNode"] -->|extends| TbRestApiCallNode_c242["TbRestApiCallNode"]
    Object______p243["Object/外部框架"] -->|extends| TbRestApiCallNodeConfiguration_c243["TbRestApiCallNodeConfiguration"]
    NodeConfiguration_p244["NodeConfiguration"] -->|implements| TbRestApiCallNodeConfiguration_c244["TbRestApiCallNodeConfiguration"]
    Object______p245["Object/外部框架"] -->|extends| TbSendRPCReplyNode_c245["TbSendRPCReplyNode"]
    TbNode_p246["TbNode"] -->|implements| TbSendRPCReplyNode_c246["TbSendRPCReplyNode"]
    Object______p247["Object/外部框架"] -->|extends| TbSendRPCRequestNode_c247["TbSendRPCRequestNode"]
    TbNode_p248["TbNode"] -->|implements| TbSendRPCRequestNode_c248["TbSendRPCRequestNode"]
    Object______p249["Object/外部框架"] -->|extends| TbSendRpcReplyNodeConfiguration_c249["TbSendRpcReplyNodeConfiguration"]
    NodeConfiguration_p250["NodeConfiguration"] -->|implements| TbSendRpcReplyNodeConfiguration_c250["TbSendRpcReplyNodeConfiguration"]
    Object______p251["Object/外部框架"] -->|extends| TbSendRpcRequestNodeConfiguration_c251["TbSendRpcRequestNodeConfiguration"]
    NodeConfiguration_p252["NodeConfiguration"] -->|implements| TbSendRpcRequestNodeConfiguration_c252["TbSendRpcRequestNodeConfiguration"]
    TbAbstractExternalNode_p253["TbAbstractExternalNode"] -->|extends| TbSendSmsNode_c253["TbSendSmsNode"]
    Object______p254["Object/外部框架"] -->|extends| TbSendSmsNodeConfiguration_c254["TbSendSmsNodeConfiguration"]
    NodeConfiguration_p255["NodeConfiguration"] -->|implements| TbSendSmsNodeConfiguration_c255["TbSendSmsNodeConfiguration"]
    TelemetryNodeCallback_p256["TelemetryNodeCallback"] -->|extends| AttributesDeleteNodeCallback_c256["AttributesDeleteNodeCallback"]
    TelemetryNodeCallback_p257["TelemetryNodeCallback"] -->|extends| AttributesUpdateNodeCallback_c257["AttributesUpdateNodeCallback"]
    Object______p258["Object/外部框架"] -->|extends| TbMsgAttributesNode_c258["TbMsgAttributesNode"]
    TbNode_p259["TbNode"] -->|implements| TbMsgAttributesNode_c259["TbMsgAttributesNode"]
    Object______p260["Object/外部框架"] -->|extends| TbMsgAttributesNodeConfiguration_c260["TbMsgAttributesNodeConfiguration"]
    NodeConfiguration_p261["NodeConfiguration"] -->|implements| TbMsgAttributesNodeConfiguration_c261["TbMsgAttributesNodeConfiguration"]
    Object______p262["Object/外部框架"] -->|extends| TbMsgDeleteAttributesNode_c262["TbMsgDeleteAttributesNode"]
    TbNode_p263["TbNode"] -->|implements| TbMsgDeleteAttributesNode_c263["TbMsgDeleteAttributesNode"]
    Object______p264["Object/外部框架"] -->|extends| TbMsgDeleteAttributesNodeConfiguration_c264["TbMsgDeleteAttributesNodeConfiguration"]
    NodeConfiguration_p265["NodeConfiguration"] -->|implements| TbMsgDeleteAttributesNodeConfiguration_c265["TbMsgDeleteAttributesNodeConfiguration"]
    Object______p266["Object/外部框架"] -->|extends| TbMsgTimeseriesNode_c266["TbMsgTimeseriesNode"]
    TbNode_p267["TbNode"] -->|implements| TbMsgTimeseriesNode_c267["TbMsgTimeseriesNode"]
    Object______p268["Object/外部框架"] -->|extends| TbMsgTimeseriesNodeConfiguration_c268["TbMsgTimeseriesNodeConfiguration"]
    NodeConfiguration_p269["NodeConfiguration"] -->|implements| TbMsgTimeseriesNodeConfiguration_c269["TbMsgTimeseriesNodeConfiguration"]
    Object______p270["Object/外部框架"] -->|extends| TelemetryNodeCallback_c270["TelemetryNodeCallback"]
    FutureCallback_p271["FutureCallback"] -->|implements| TelemetryNodeCallback_c271["TelemetryNodeCallback"]
    Object______p272["Object/外部框架"] -->|extends| TbSynchronizationBeginNode_c272["TbSynchronizationBeginNode"]
    TbNode_p273["TbNode"] -->|implements| TbSynchronizationBeginNode_c273["TbSynchronizationBeginNode"]
    Object______p274["Object/外部框架"] -->|extends| TbSynchronizationEndNode_c274["TbSynchronizationEndNode"]
    TbNode_p275["TbNode"] -->|implements| TbSynchronizationEndNode_c275["TbSynchronizationEndNode"]
    Object______p276["Object/外部框架"] -->|extends| MultipleTbMsgsCallbackWrapper_c276["MultipleTbMsgsCallbackWrapper"]
    TbMsgCallbackWrapper_p277["TbMsgCallbackWrapper"] -->|implements| MultipleTbMsgsCallbackWrapper_c277["MultipleTbMsgsCallbackWrapper"]
    Object______p278["Object/外部框架"] -->|extends| TbAbstractTransformNode_c278["TbAbstractTransformNode"]
    TbNode_p279["TbNode"] -->|implements| TbAbstractTransformNode_c279["TbAbstractTransformNode"]
    Object______p280["Object/外部框架"] -->|extends| TbAbstractTransformNodeWithTbMsgSource_c280["TbAbstractTransformNodeWithTbMsgSource"]
    TbNode_p281["TbNode"] -->|implements| TbAbstractTransformNodeWithTbMsgSource_c281["TbAbstractTransformNodeWithTbMsgSource"]
    TbAbstractTransformNode_p282["TbAbstractTransformNode"] -->|extends| TbChangeOriginatorNode_c282["TbChangeOriginatorNode"]
    Object______p283["Object/外部框架"] -->|extends| TbChangeOriginatorNodeConfiguration_c283["TbChangeOriginatorNodeConfiguration"]
    NodeConfiguration_p284["NodeConfiguration"] -->|implements| TbChangeOriginatorNodeConfiguration_c284["TbChangeOriginatorNodeConfiguration"]
    TbAbstractTransformNodeWithTbMsgSource_p285["TbAbstractTransformNodeWithTbMsgSource"] -->|extends| TbCopyKeysNode_c285["TbCopyKeysNode"]
    Object______p286["Object/外部框架"] -->|extends| TbCopyKeysNodeConfiguration_c286["TbCopyKeysNodeConfiguration"]
    NodeConfiguration_p287["NodeConfiguration"] -->|implements| TbCopyKeysNodeConfiguration_c287["TbCopyKeysNodeConfiguration"]
    TbAbstractTransformNodeWithTbMsgSource_p288["TbAbstractTransformNodeWithTbMsgSource"] -->|extends| TbDeleteKeysNode_c288["TbDeleteKeysNode"]
    Object______p289["Object/外部框架"] -->|extends| TbDeleteKeysNodeConfiguration_c289["TbDeleteKeysNodeConfiguration"]
    NodeConfiguration_p290["NodeConfiguration"] -->|implements| TbDeleteKeysNodeConfiguration_c290["TbDeleteKeysNodeConfiguration"]
    Object______p291["Object/外部框架"] -->|extends| TbJsonPathNode_c291["TbJsonPathNode"]
    TbNode_p292["TbNode"] -->|implements| TbJsonPathNode_c292["TbJsonPathNode"]
    Object______p293["Object/外部框架"] -->|extends| TbJsonPathNodeConfiguration_c293["TbJsonPathNodeConfiguration"]
    NodeConfiguration_p294["NodeConfiguration"] -->|implements| TbJsonPathNodeConfiguration_c294["TbJsonPathNodeConfiguration"]
    TbAbstractTransformNodeWithTbMsgSource_p295["TbAbstractTransformNodeWithTbMsgSource"] -->|extends| TbRenameKeysNode_c295["TbRenameKeysNode"]
    Object______p296["Object/外部框架"] -->|extends| TbRenameKeysNodeConfiguration_c296["TbRenameKeysNodeConfiguration"]
    NodeConfiguration_p297["NodeConfiguration"] -->|implements| TbRenameKeysNodeConfiguration_c297["TbRenameKeysNodeConfiguration"]
    Object______p298["Object/外部框架"] -->|extends| TbSplitArrayMsgNode_c298["TbSplitArrayMsgNode"]
    TbNode_p299["TbNode"] -->|implements| TbSplitArrayMsgNode_c299["TbSplitArrayMsgNode"]
    TbAbstractTransformNode_p300["TbAbstractTransformNode"] -->|extends| TbTransformMsgNode_c300["TbTransformMsgNode"]
    Object______p301["Object/外部框架"] -->|extends| TbTransformMsgNodeConfiguration_c301["TbTransformMsgNodeConfiguration"]
    NodeConfiguration_p302["NodeConfiguration"] -->|implements| TbTransformMsgNodeConfiguration_c302["TbTransformMsgNodeConfiguration"]
    Object______p303["Object/外部框架"] -->|extends| EntitiesAlarmOriginatorIdAsyncLoader_c303["EntitiesAlarmOriginatorIdAsyncLoader"]
    Object______p304["Object/外部框架"] -->|extends| EntitiesByNameAndTypeLoader_c304["EntitiesByNameAndTypeLoader"]
    Object______p305["Object/外部框架"] -->|extends| EntitiesCustomerIdAsyncLoader_c305["EntitiesCustomerIdAsyncLoader"]
    Object______p306["Object/外部框架"] -->|extends| EntitiesFieldsAsyncLoader_c306["EntitiesFieldsAsyncLoader"]
    Object______p307["Object/外部框架"] -->|extends| EntitiesRelatedDeviceIdAsyncLoader_c307["EntitiesRelatedDeviceIdAsyncLoader"]
    Object______p308["Object/外部框架"] -->|extends| EntitiesRelatedEntityIdAsyncLoader_c308["EntitiesRelatedEntityIdAsyncLoader"]
    Object______p309["Object/外部框架"] -->|extends| EntityContainer_c309["EntityContainer"]
    Object______p310["Object/外部框架"] -->|extends| GpsGeofencingEvents_c310["GpsGeofencingEvents"]
    Object______p311["Object/外部框架"] -->|extends| TenantIdLoader_c311["TenantIdLoader"]
    Object______p312["Object/外部框架"] -->|extends| AbstractRuleNodeUpgradeTest_c312["AbstractRuleNodeUpgradeTest"]
    Object______p313["Object/外部框架"] -->|extends| TestDbCallbackExecutor_c313["TestDbCallbackExecutor"]
    ListeningExecutor_p314["ListeningExecutor"] -->|implements| TestDbCallbackExecutor_c314["TestDbCallbackExecutor"]
    Object______p315["Object/外部框架"] -->|extends| TbAlarmNodeTest_c315["TbAlarmNodeTest"]
    Object______p316["Object/外部框架"] -->|extends| TbCreateRelationNodeTest_c316["TbCreateRelationNodeTest"]
    Object______p317["Object/外部框架"] -->|extends| TbDeviceStateNodeTest_c317["TbDeviceStateNodeTest"]
    Object______p318["Object/外部框架"] -->|extends| TbLogNodeTest_c318["TbLogNodeTest"]
    Object______p319["Object/外部框架"] -->|extends| CertPemCredentialsTest_c319["CertPemCredentialsTest"]
    AbstractRuleNodeUpgradeTest_p320["AbstractRuleNodeUpgradeTest"] -->|extends| TbMsgGeneratorNodeTest_c320["TbMsgGeneratorNodeTest"]
    Object______p321["Object/外部框架"] -->|extends| TbMsgPushToEdgeNodeTest_c321["TbMsgPushToEdgeNodeTest"]
    Object______p322["Object/外部框架"] -->|extends| TbAssetTypeSwitchNodeTest_c322["TbAssetTypeSwitchNodeTest"]
    Object______p323["Object/外部框架"] -->|extends| TbCheckAlarmStatusNodeTest_c323["TbCheckAlarmStatusNodeTest"]
    Object______p324["Object/外部框架"] -->|extends| TbCheckMessageNodeTest_c324["TbCheckMessageNodeTest"]
    Object______p325["Object/外部框架"] -->|extends| TbCheckRelationNodeTest_c325["TbCheckRelationNodeTest"]
    Object______p326["Object/外部框架"] -->|extends| TbDeviceTypeSwitchNodeTest_c326["TbDeviceTypeSwitchNodeTest"]
    Object______p327["Object/外部框架"] -->|extends| TbJsFilterNodeTest_c327["TbJsFilterNodeTest"]
    Object______p328["Object/外部框架"] -->|extends| TbJsSwitchNodeTest_c328["TbJsSwitchNodeTest"]
    Object______p329["Object/外部框架"] -->|extends| TbMsgTypeFilterNodeTest_c329["TbMsgTypeFilterNodeTest"]
    Object______p330["Object/外部框架"] -->|extends| TbMsgTypeSwitchNodeTest_c330["TbMsgTypeSwitchNodeTest"]
    Object______p331["Object/外部框架"] -->|extends| TbOriginatorTypeFilterNodeTest_c331["TbOriginatorTypeFilterNodeTest"]
    Object______p332["Object/外部框架"] -->|extends| TbOriginatorTypeSwitchNodeTest_c332["TbOriginatorTypeSwitchNodeTest"]
    AbstractRuleNodeUpgradeTest_p333["AbstractRuleNodeUpgradeTest"] -->|extends| TbCheckpointNodeTest_c333["TbCheckpointNodeTest"]
    Object______p334["Object/外部框架"] -->|extends| GeoUtilTest_c334["GeoUtilTest"]
    Object______p335["Object/外部框架"] -->|extends| GpsGeofencingActionTestCase_c335["GpsGeofencingActionTestCase"]
    AbstractRuleNodeUpgradeTest_p336["AbstractRuleNodeUpgradeTest"] -->|extends| TbGpsGeofencingActionNodeTest_c336["TbGpsGeofencingActionNodeTest"]
    Object______p337["Object/外部框架"] -->|extends| TbGpsGeofencingFilterNodeTest_c337["TbGpsGeofencingFilterNodeTest"]
    Object______p338["Object/外部框架"] -->|extends| TbMsgToEmailNodeTest_c338["TbMsgToEmailNodeTest"]
    Object______p339["Object/外部框架"] -->|extends| MailBodyTypeTestConfig_c339["MailBodyTypeTestConfig"]
    Object______p340["Object/外部框架"] -->|extends| TbMathArgumentValueTest_c340["TbMathArgumentValueTest"]
    Object______p341["Object/外部框架"] -->|extends| TbMathNodeTest_c341["TbMathNodeTest"]
    AbstractListeningExecutor_p342["AbstractListeningExecutor"] -->|extends| RuleDispatcherExecutor_c342["RuleDispatcherExecutor"]
    AbstractListeningExecutor_p343["AbstractListeningExecutor"] -->|extends| DBCallbackExecutor_c343["DBCallbackExecutor"]
    Object______p344["Object/外部框架"] -->|extends| CalculateDeltaNodeTest_c344["CalculateDeltaNodeTest"]
    Object______p345["Object/外部框架"] -->|extends| ListMatcher_c345["ListMatcher"]
    Object______p346["Object/外部框架"] -->|extends| TbFetchDeviceCredentialsNodeTest_c346["TbFetchDeviceCredentialsNodeTest"]
    Object______p347["Object/外部框架"] -->|extends| TbGetAttributesNodeTest_c347["TbGetAttributesNodeTest"]
    Object______p348["Object/外部框架"] -->|extends| TbGetCustomerAttributeNodeTest_c348["TbGetCustomerAttributeNodeTest"]
    Object______p349["Object/外部框架"] -->|extends| TbGetCustomerDetailsNodeTest_c349["TbGetCustomerDetailsNodeTest"]
    Object______p350["Object/外部框架"] -->|extends| TbGetDeviceAttrNodeTest_c350["TbGetDeviceAttrNodeTest"]
    Object______p351["Object/外部框架"] -->|extends| TbGetOriginatorFieldsNodeTest_c351["TbGetOriginatorFieldsNodeTest"]
    Object______p352["Object/外部框架"] -->|extends| TbGetRelatedAttributeNodeTest_c352["TbGetRelatedAttributeNodeTest"]
    Object______p353["Object/外部框架"] -->|extends| TbGetTelemetryNodeTest_c353["TbGetTelemetryNodeTest"]
    Object______p354["Object/外部框架"] -->|extends| TbGetTenantAttributeNodeTest_c354["TbGetTenantAttributeNodeTest"]
    Object______p355["Object/外部框架"] -->|extends| TbGetTenantDetailsNodeTest_c355["TbGetTenantDetailsNodeTest"]
    Object______p356["Object/外部框架"] -->|extends| AlarmRuleStateTest_c356["AlarmRuleStateTest"]
    Object______p357["Object/外部框架"] -->|extends| AlarmStateTest_c357["AlarmStateTest"]
    Object______p358["Object/外部框架"] -->|extends| DeviceStateTest_c358["DeviceStateTest"]
    Object______p359["Object/外部框架"] -->|extends| TbDeviceProfileNodeTest_c359["TbDeviceProfileNodeTest"]
    Object______p360["Object/外部框架"] -->|extends| TbHttpClientTest_c360["TbHttpClientTest"]
    Object______p361["Object/外部框架"] -->|extends| TbRestApiCallNodeTest_c361["TbRestApiCallNodeTest"]
    Object______p362["Object/外部框架"] -->|extends| TbSendRPCReplyNodeTest_c362["TbSendRPCReplyNodeTest"]
    Object______p363["Object/外部框架"] -->|extends| TbMsgAttributesNodeConfigurationTest_c363["TbMsgAttributesNodeConfigurationTest"]
    AbstractRuleNodeUpgradeTest_p364["AbstractRuleNodeUpgradeTest"] -->|extends| TbMsgAttributesNodeTest_c364["TbMsgAttributesNodeTest"]
    Object______p365["Object/外部框架"] -->|extends| TbMsgDeleteAttributesNodeTest_c365["TbMsgDeleteAttributesNodeTest"]
    Object______p366["Object/外部框架"] -->|extends| TbChangeOriginatorNodeTest_c366["TbChangeOriginatorNodeTest"]
    Object______p367["Object/外部框架"] -->|extends| TbCopyKeysNodeTest_c367["TbCopyKeysNodeTest"]
    Object______p368["Object/外部框架"] -->|extends| TbDeleteKeysNodeTest_c368["TbDeleteKeysNodeTest"]
    Object______p369["Object/外部框架"] -->|extends| TbJsonPathNodeTest_c369["TbJsonPathNodeTest"]
    AbstractRuleNodeUpgradeTest_p370["AbstractRuleNodeUpgradeTest"] -->|extends| TbMsgDeduplicationNodeTest_c370["TbMsgDeduplicationNodeTest"]
    Object______p371["Object/外部框架"] -->|extends| TbRenameKeysNodeTest_c371["TbRenameKeysNodeTest"]
    Object______p372["Object/外部框架"] -->|extends| TbSplitArrayMsgNodeTest_c372["TbSplitArrayMsgNodeTest"]
    Object______p373["Object/外部框架"] -->|extends| TbTransformMsgNodeTest_c373["TbTransformMsgNodeTest"]
    Object______p374["Object/外部框架"] -->|extends| EntitiesCustomerIdAsyncLoaderTest_c374["EntitiesCustomerIdAsyncLoaderTest"]
    Object______p375["Object/外部框架"] -->|extends| EntitiesFieldsAsyncLoaderTest_c375["EntitiesFieldsAsyncLoaderTest"]
    Object______p376["Object/外部框架"] -->|extends| EntitiesRelatedDeviceIdAsyncLoaderTest_c376["EntitiesRelatedDeviceIdAsyncLoaderTest"]
    Object______p377["Object/外部框架"] -->|extends| EntitiesRelatedEntityIdAsyncLoaderTest_c377["EntitiesRelatedEntityIdAsyncLoaderTest"]
    Object______p378["Object/外部框架"] -->|extends| TenantIdLoaderTest_c378["TenantIdLoaderTest"]
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

- `TbAbstractAlarmNode.onMsg()` (public, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractAlarmNode.java`)
- `TbAbstractAlarmNode.processAlarm()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractAlarmNode.java`)
- `TbAbstractAlarmNode.buildAlarmDetails()` (protected, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractAlarmNode.java`)
- `TbAbstractAlarmNode.destroy()` (public, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractAlarmNode.java`)
- `TbAbstractCustomerActionNode.CustomerCacheLoader()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.createCustomerIfNotExists()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.onMsg()` (public, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.doProcessCustomerAction()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.getCustomer()` (protected, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.CustomerKey()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.RuntimeException()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.destroy()` (public, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.CustomerCacheLoader()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.load()` (public, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.Customer()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractRelationActionNode.EntityCacheLoader()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.onMsg()` (public, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.destroy()` (public, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.processEntityRelationAction()` (protected, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.createEntityIfNotExists()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.doProcessEntityRelationAction()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.getEntity()` (protected, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.EntityKey()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.RuntimeException()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.processSingleSearchDirection()` (protected, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.SearchDirectionIds()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.processListSearchDirection()` (protected, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.processPattern()` (protected, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.EntityCacheLoader()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.load()` (public, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.loadEntity()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.EntityContainer()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.Device()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.Asset()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.Customer()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `ClientCredentials.getType()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/credentials/ClientCredentials.java`)
- `AbstractTbMsgPushNode.getConfigClazz()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/edge/AbstractTbMsgPushNode.java`)
- `AbstractTbMsgPushNode.onMsg()` (public, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/edge/AbstractTbMsgPushNode.java`)
- `AbstractTbMsgPushNode.RuntimeException()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/edge/AbstractTbMsgPushNode.java`)
- `AbstractTbMsgPushNode.buildEvent()` (protected, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/edge/AbstractTbMsgPushNode.java`)


## 哪些方法可以重写

- `TbAbstractAlarmNode.onMsg()` (public, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractAlarmNode.java`)
- `TbAbstractAlarmNode.processAlarm()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractAlarmNode.java`)
- `TbAbstractAlarmNode.buildAlarmDetails()` (protected, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractAlarmNode.java`)
- `TbAbstractAlarmNode.destroy()` (public, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractAlarmNode.java`)
- `TbAbstractCustomerActionNode.CustomerCacheLoader()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.createCustomerIfNotExists()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.onMsg()` (public, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.doProcessCustomerAction()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.getCustomer()` (protected, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.CustomerKey()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.RuntimeException()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.destroy()` (public, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.CustomerCacheLoader()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.load()` (public, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.Customer()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerKey.CustomerCacheLoader()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerKey.createCustomerIfNotExists()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerKey.onMsg()` (public, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerKey.doProcessCustomerAction()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerKey.getCustomer()` (protected, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerKey.RuntimeException()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerKey.destroy()` (public, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerKey.CustomerCacheLoader()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerKey.load()` (public, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerKey.Customer()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerCacheLoader.createCustomerIfNotExists()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerCacheLoader.onMsg()` (public, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerCacheLoader.doProcessCustomerAction()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerCacheLoader.getCustomer()` (protected, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerCacheLoader.CustomerKey()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerCacheLoader.RuntimeException()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerCacheLoader.destroy()` (public, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerCacheLoader.load()` (public, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerCacheLoader.Customer()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractRelationActionNode.EntityCacheLoader()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.onMsg()` (public, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.destroy()` (public, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.processEntityRelationAction()` (protected, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.createEntityIfNotExists()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.doProcessEntityRelationAction()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)


## 哪些方法必须重写

- `TbAbstractAlarmNode.processAlarm()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractAlarmNode.java`)
- `TbAbstractCustomerActionNode.CustomerCacheLoader()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.createCustomerIfNotExists()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.doProcessCustomerAction()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.CustomerKey()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.RuntimeException()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractCustomerActionNode.Customer()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerKey.CustomerCacheLoader()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerKey.createCustomerIfNotExists()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerKey.doProcessCustomerAction()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerKey.RuntimeException()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerKey.Customer()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerCacheLoader.createCustomerIfNotExists()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerCacheLoader.doProcessCustomerAction()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerCacheLoader.CustomerKey()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerCacheLoader.RuntimeException()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `CustomerCacheLoader.Customer()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractCustomerActionNode.java`)
- `TbAbstractRelationActionNode.EntityCacheLoader()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.createEntityIfNotExists()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.doProcessEntityRelationAction()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.EntityKey()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.RuntimeException()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.SearchDirectionIds()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.loadEntity()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.EntityContainer()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.Device()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.Asset()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `TbAbstractRelationActionNode.Customer()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `EntityKey.EntityCacheLoader()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `EntityKey.createEntityIfNotExists()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `EntityKey.doProcessEntityRelationAction()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `EntityKey.RuntimeException()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `EntityKey.SearchDirectionIds()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `EntityKey.loadEntity()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `EntityKey.EntityContainer()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `EntityKey.Device()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `EntityKey.Asset()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `EntityKey.Customer()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `SearchDirectionIds.EntityCacheLoader()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)
- `SearchDirectionIds.createEntityIfNotExists()` (package, `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/TbAbstractRelationActionNode.java`)


## 哪些地方使用了多态

- `Object/外部框架` -> `TbAbstractAlarmNode` (extends)
- `TbNode` -> `TbAbstractAlarmNode` (implements)
- `Object/外部框架` -> `TbAbstractAlarmNodeConfiguration` (extends)
- `Object/外部框架` -> `TbAbstractCustomerActionNode` (extends)
- `TbNode` -> `TbAbstractCustomerActionNode` (implements)
- `Object/外部框架` -> `CustomerKey` (extends)
- `CacheLoader` -> `CustomerCacheLoader` (extends)
- `Object/外部框架` -> `TbAbstractCustomerActionNodeConfiguration` (extends)
- `Object/外部框架` -> `TbAbstractRelationActionNode` (extends)
- `TbNode` -> `TbAbstractRelationActionNode` (implements)
- `Object/外部框架` -> `EntityKey` (extends)
- `Object/外部框架` -> `SearchDirectionIds` (extends)
- `CacheLoader` -> `EntityCacheLoader` (extends)
- `Object/外部框架` -> `RelationContainer` (extends)
- `Object/外部框架` -> `TbAbstractRelationActionNodeConfiguration` (extends)
- `Object/外部框架` -> `TbAlarmResult` (extends)
- `TbAbstractCustomerActionNode` -> `TbAssignToCustomerNode` (extends)
- `TbAbstractCustomerActionNodeConfiguration` -> `TbAssignToCustomerNodeConfiguration` (extends)
- `NodeConfiguration` -> `TbAssignToCustomerNodeConfiguration` (implements)
- `TbAbstractAlarmNode` -> `TbClearAlarmNode` (extends)
- `TbAbstractAlarmNodeConfiguration` -> `TbClearAlarmNodeConfiguration` (extends)
- `NodeConfiguration` -> `TbClearAlarmNodeConfiguration` (implements)
- `Object/外部框架` -> `TbCopyAttributesToEntityViewNode` (extends)
- `TbNode` -> `TbCopyAttributesToEntityViewNode` (implements)
- `TbAbstractAlarmNode` -> `TbCreateAlarmNode` (extends)
- `TbAbstractAlarmNodeConfiguration` -> `TbCreateAlarmNodeConfiguration` (extends)
- `NodeConfiguration` -> `TbCreateAlarmNodeConfiguration` (implements)
- `TbAbstractRelationActionNode` -> `TbCreateRelationNode` (extends)
- `TbAbstractRelationActionNodeConfiguration` -> `TbCreateRelationNodeConfiguration` (extends)
- `NodeConfiguration` -> `TbCreateRelationNodeConfiguration` (implements)
- `TbAbstractRelationActionNode` -> `TbDeleteRelationNode` (extends)
- `TbAbstractRelationActionNodeConfiguration` -> `TbDeleteRelationNodeConfiguration` (extends)
- `NodeConfiguration` -> `TbDeleteRelationNodeConfiguration` (implements)
- `Object/外部框架` -> `TbDeviceStateNode` (extends)
- `TbNode` -> `TbDeviceStateNode` (implements)
- `Object/外部框架` -> `TbDeviceStateNodeConfiguration` (extends)
- `NodeConfiguration` -> `TbDeviceStateNodeConfiguration` (implements)
- `Object/外部框架` -> `TbLogNode` (extends)
- `TbNode` -> `TbLogNode` (implements)
- `Object/外部框架` -> `TbLogNodeConfiguration` (extends)
- 其余 40 项已省略；完整源码证据可通过本模块 Java 文件继续追踪。


## 外部父类/接口

- `AbstractListeningExecutor`
- `CacheLoader`
- `FutureCallback`
- `ListeningExecutor`
- `NodeConfiguration`
- `Object/外部框架`
- `RuntimeException`
- `TbNode`


## 整个继承体系解决了什么问题

该继承体系把“稳定生命周期/契约”和“模块特定行为”分开：父类或接口负责统一入口、共享流程和多态调度，子类负责差异化协议、实体、规则、DAO、测试或工具行为。这样可以让上游模块通过父类型调用下游实现，同时保持每个实现只关注自己的变化点。
