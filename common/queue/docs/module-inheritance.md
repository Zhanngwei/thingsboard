# Thingsboard Server Queue components 模块继承体系分析

> 生成范围：`common/queue`  
> Maven artifact：`queue`  
> Java 类型数量：145  
> 分析方式：静态扫描 `class/interface/enum/record` 的 `extends`、`implements`、方法签名和源码触点。

## 完整继承树

```text
ApplicationEvent
├── TbApplicationEvent
├── ├── ClusterTopologyChangeEvent
├── ├── OtherServiceShutdownEvent
├── ├── PartitionChangeEvent
├── ├── ServiceListChangedEvent
DataDecodingEncodingService
├── «implements» ProtoWithFSTService
DiscoveryService
├── «implements» DummyDiscoveryService
├── «implements» ZkDiscoveryService
InMemoryStorage
├── «implements» DefaultInMemoryStorage
NotificationDeduplicationService
├── «implements» DefaultNotificationDeduplicationService
NotificationRuleProcessor
├── «implements» RemoteNotificationRuleProcessor
Object/外部框架
├── AbstractParallelTbQueueConsumerTemplate
├── AbstractTbQueueConsumerTemplate
├── AbstractTbQueueTemplate
├── AsyncCallbackTemplate
├── AwsSqsMonolithQueueFactory
├── AwsSqsMsgWrapper
├── AwsSqsTbCoreQueueFactory
├── AwsSqsTbQueueMsgMetadata
├── AwsSqsTbRuleEngineQueueFactory
├── AwsSqsTbVersionControlQueueFactory
├── AwsSqsTransportQueueFactory
├── ConsistentHashCircle
├── DefaultInMemoryStorage
├── DefaultInMemoryStorageTest
├── DefaultNotificationDeduplicationService
├── DefaultSchedulerComponent
├── DefaultTbApiUsageReportClient
├── DefaultTbQueueMsg
├── DefaultTbQueueMsgHeaders
├── DefaultTbQueueRequestTemplate
├── DefaultTbQueueRequestTemplateTest
├── DefaultTbQueueResponseTemplate
├── DefaultTbServiceInfoProvider
├── DummyDiscoveryService
├── EnvironmentLogService
├── GroupTopicStats
├── HashPartitionService
├── InMemoryMonolithQueueFactory
├── InMemoryTbQueueConsumer
├── InMemoryTbQueueProducer
├── InMemoryTbTransportQueueFactory
├── KafkaMonolithQueueFactory
├── KafkaTbCoreQueueFactory
├── KafkaTbQueueMsg
├── KafkaTbQueueMsgMetadata
├── KafkaTbRuleEngineQueueFactory
├── KafkaTbTransportQueueFactory
├── KafkaTbVersionControlQueueFactory
├── MultipleTbQueueCallbackWrapper
├── MultipleTbQueueTbMsgCallbackWrapper
├── ParentEntity
├── PropertyUtils
├── PropertyUtilsTest
├── ProtoWithFSTService
├── PubSubMonolithQueueFactory
├── PubSubTbCoreQueueFactory
├── PubSubTbRuleEngineQueueFactory
├── PubSubTbVersionControlQueueFactory
├── PubSubTransportQueueFactory
├── QueueKey
├── QueueKeyTest
├── QueueRoutingInfo
├── RabbitMqMonolithQueueFactory
├── RabbitMqTbCoreQueueFactory
├── RabbitMqTbRuleEngineQueueFactory
├── RabbitMqTbVersionControlQueueFactory
├── RabbitMqTransportQueueFactory
├── RemoteNotificationRuleProcessor
├── ReportLevel
├── ResponseMetaData
├── RuleEngineTbQueueAdminFactory
├── ServiceBusMonolithQueueFactory
├── ServiceBusTbCoreQueueFactory
├── ServiceBusTbRuleEngineQueueFactory
├── ServiceBusTbVersionControlQueueFactory
├── ServiceBusTransportQueueFactory
├── SimpleTbQueueCallback
├── TbApplicationEventListener
├── TbAwsSqsAdmin
├── TbAwsSqsConsumerTemplate
├── TbAwsSqsProducerTemplate
├── TbAwsSqsQueueAttributes
├── TbAwsSqsSettings
├── TbCoreQueueProducerProvider
├── TbKafkaAdmin
├── TbKafkaConsumerStatisticConfig
├── TbKafkaConsumerStatsService
├── TbKafkaConsumerTemplate
├── TbKafkaProducerTemplate
├── TbKafkaProducerTemplateTest
├── TbKafkaSettings
├── TbKafkaSettingsTest
├── TbKafkaTopicConfigs
├── TbProtoJsQueueMsg
├── TbProtoQueueMsg
├── TbPubSubAdmin
├── TbPubSubConsumerTemplate
├── TbPubSubProducerTemplate
├── TbPubSubSettings
├── TbPubSubSubscriptionSettings
├── TbQueueCoreSettings
├── TbQueueRemoteJsInvokeSettings
├── TbQueueRuleEngineSettings
├── TbQueueTbMsgCallbackWrapper
├── TbQueueTransportApiSettings
├── TbQueueTransportNotificationSettings
├── TbQueueVersionControlSettings
├── TbRabbitMqAdmin
├── TbRabbitMqConsumerTemplate
├── TbRabbitMqProducerTemplate
├── TbRabbitMqQueueArguments
├── TbRabbitMqSettings
├── TbRuleEngineProducerProvider
├── TbServiceBusAdmin
├── TbServiceBusConsumerTemplate
├── TbServiceBusProducerTemplate
├── TbServiceBusQueueConfigs
├── TbServiceBusSettings
├── TbTransportQueueProducerProvider
├── TbVersionControlProducerProvider
├── TenantRoutingInfo
├── TopicService
├── ZkDiscoveryService
├── ZkDiscoveryServiceTest
PartitionService
├── «implements» HashPartitionService
PathChildrenCacheListener
├── «implements» ZkDiscoveryService
SchedulerComponent
├── «implements» DefaultSchedulerComponent
TbApiUsageReportClient
├── «implements» DefaultTbApiUsageReportClient
TbQueueAdmin
├── «implements» TbAwsSqsAdmin
├── «implements» TbKafkaAdmin
├── «implements» TbPubSubAdmin
├── «implements» TbRabbitMqAdmin
├── «implements» TbServiceBusAdmin
TbQueueCallback
├── «implements» MultipleTbQueueCallbackWrapper
├── «implements» MultipleTbQueueTbMsgCallbackWrapper
├── «implements» SimpleTbQueueCallback
├── «implements» TbQueueTbMsgCallbackWrapper
TbQueueMsg
├── «implements» DefaultTbQueueMsg
├── «implements» KafkaTbQueueMsg
├── «implements» TbProtoQueueMsg
TbQueueMsgHeaders
├── «implements» DefaultTbQueueMsgHeaders
TbQueueMsgMetadata
├── «implements» AwsSqsTbQueueMsgMetadata
├── «implements» KafkaTbQueueMsgMetadata
TbQueueProducerProvider
├── «implements» TbCoreQueueProducerProvider
├── «implements» TbRuleEngineProducerProvider
├── «implements» TbTransportQueueProducerProvider
├── «implements» TbVersionControlProducerProvider
TbServiceInfoProvider
├── «implements» DefaultTbServiceInfoProvider
TbUsageStatsClientQueueFactory
├── TbCoreQueueFactory
├── ├── «implements» AwsSqsMonolithQueueFactory
├── ├── «implements» AwsSqsTbCoreQueueFactory
├── ├── «implements» InMemoryMonolithQueueFactory
├── ├── «implements» KafkaMonolithQueueFactory
├── ├── «implements» KafkaTbCoreQueueFactory
├── ├── «implements» PubSubMonolithQueueFactory
├── ├── «implements» PubSubTbCoreQueueFactory
├── ├── «implements» RabbitMqMonolithQueueFactory
├── ├── «implements» RabbitMqTbCoreQueueFactory
├── ├── «implements» ServiceBusMonolithQueueFactory
├── ├── «implements» ServiceBusTbCoreQueueFactory
├── TbRuleEngineQueueFactory
├── ├── «implements» AwsSqsMonolithQueueFactory
├── ├── «implements» AwsSqsTbRuleEngineQueueFactory
├── ├── «implements» InMemoryMonolithQueueFactory
├── ├── «implements» KafkaMonolithQueueFactory
├── ├── «implements» KafkaTbRuleEngineQueueFactory
├── ├── «implements» PubSubMonolithQueueFactory
├── ├── «implements» PubSubTbRuleEngineQueueFactory
├── ├── «implements» RabbitMqMonolithQueueFactory
├── ├── «implements» RabbitMqTbRuleEngineQueueFactory
├── ├── «implements» ServiceBusMonolithQueueFactory
├── ├── «implements» ServiceBusTbRuleEngineQueueFactory
├── TbTransportQueueFactory
├── ├── «implements» AwsSqsTransportQueueFactory
├── ├── «implements» InMemoryTbTransportQueueFactory
├── ├── «implements» KafkaTbTransportQueueFactory
├── ├── «implements» PubSubTransportQueueFactory
├── ├── «implements» RabbitMqTransportQueueFactory
├── ├── «implements» ServiceBusTransportQueueFactory
├── TbVersionControlQueueFactory
├── ├── «implements» AwsSqsMonolithQueueFactory
├── ├── «implements» AwsSqsTbVersionControlQueueFactory
├── ├── «implements» InMemoryMonolithQueueFactory
├── ├── «implements» KafkaMonolithQueueFactory
├── ├── «implements» KafkaTbVersionControlQueueFactory
├── ├── «implements» PubSubMonolithQueueFactory
├── ├── «implements» PubSubTbVersionControlQueueFactory
├── ├── «implements» RabbitMqMonolithQueueFactory
├── ├── «implements» RabbitMqTbVersionControlQueueFactory
├── ├── «implements» ServiceBusMonolithQueueFactory
├── ├── «implements» ServiceBusTbVersionControlQueueFactory
```

## 继承图

```mermaid
flowchart TD
    Object______p0["Object/外部框架"] -->|extends| RuleEngineTbQueueAdminFactory_c0["RuleEngineTbQueueAdminFactory"]
    Object______p1["Object/外部框架"] -->|extends| TbServiceBusAdmin_c1["TbServiceBusAdmin"]
    TbQueueAdmin_p2["TbQueueAdmin"] -->|implements| TbServiceBusAdmin_c2["TbServiceBusAdmin"]
    Object______p3["Object/外部框架"] -->|extends| TbServiceBusConsumerTemplate_c3["TbServiceBusConsumerTemplate"]
    Object______p4["Object/外部框架"] -->|extends| TbServiceBusProducerTemplate_c4["TbServiceBusProducerTemplate"]
    Object______p5["Object/外部框架"] -->|extends| TbServiceBusQueueConfigs_c5["TbServiceBusQueueConfigs"]
    Object______p6["Object/外部框架"] -->|extends| TbServiceBusSettings_c6["TbServiceBusSettings"]
    Object______p7["Object/外部框架"] -->|extends| AbstractParallelTbQueueConsumerTemplate_c7["AbstractParallelTbQueueConsumerTemplate"]
    Object______p8["Object/外部框架"] -->|extends| AbstractTbQueueConsumerTemplate_c8["AbstractTbQueueConsumerTemplate"]
    Object______p9["Object/外部框架"] -->|extends| AbstractTbQueueTemplate_c9["AbstractTbQueueTemplate"]
    Object______p10["Object/外部框架"] -->|extends| AsyncCallbackTemplate_c10["AsyncCallbackTemplate"]
    Object______p11["Object/外部框架"] -->|extends| DefaultTbQueueMsg_c11["DefaultTbQueueMsg"]
    TbQueueMsg_p12["TbQueueMsg"] -->|implements| DefaultTbQueueMsg_c12["DefaultTbQueueMsg"]
    Object______p13["Object/外部框架"] -->|extends| DefaultTbQueueMsgHeaders_c13["DefaultTbQueueMsgHeaders"]
    TbQueueMsgHeaders_p14["TbQueueMsgHeaders"] -->|implements| DefaultTbQueueMsgHeaders_c14["DefaultTbQueueMsgHeaders"]
    Object______p15["Object/外部框架"] -->|extends| DefaultTbQueueRequestTemplate_c15["DefaultTbQueueRequestTemplate"]
    Object______p16["Object/外部框架"] -->|extends| ResponseMetaData_c16["ResponseMetaData"]
    Object______p17["Object/外部框架"] -->|extends| DefaultTbQueueResponseTemplate_c17["DefaultTbQueueResponseTemplate"]
    Object______p18["Object/外部框架"] -->|extends| MultipleTbQueueCallbackWrapper_c18["MultipleTbQueueCallbackWrapper"]
    TbQueueCallback_p19["TbQueueCallback"] -->|implements| MultipleTbQueueCallbackWrapper_c19["MultipleTbQueueCallbackWrapper"]
    Object______p20["Object/外部框架"] -->|extends| MultipleTbQueueTbMsgCallbackWrapper_c20["MultipleTbQueueTbMsgCallbackWrapper"]
    TbQueueCallback_p21["TbQueueCallback"] -->|implements| MultipleTbQueueTbMsgCallbackWrapper_c21["MultipleTbQueueTbMsgCallbackWrapper"]
    Object______p22["Object/外部框架"] -->|extends| SimpleTbQueueCallback_c22["SimpleTbQueueCallback"]
    TbQueueCallback_p23["TbQueueCallback"] -->|implements| SimpleTbQueueCallback_c23["SimpleTbQueueCallback"]
    Object______p24["Object/外部框架"] -->|extends| TbProtoJsQueueMsg_c24["TbProtoJsQueueMsg"]
    Object______p25["Object/外部框架"] -->|extends| TbProtoQueueMsg_c25["TbProtoQueueMsg"]
    TbQueueMsg_p26["TbQueueMsg"] -->|implements| TbProtoQueueMsg_c26["TbProtoQueueMsg"]
    Object______p27["Object/外部框架"] -->|extends| TbQueueTbMsgCallbackWrapper_c27["TbQueueTbMsgCallbackWrapper"]
    TbQueueCallback_p28["TbQueueCallback"] -->|implements| TbQueueTbMsgCallbackWrapper_c28["TbQueueTbMsgCallbackWrapper"]
    Object______p29["Object/外部框架"] -->|extends| ConsistentHashCircle_c29["ConsistentHashCircle"]
    Object______p30["Object/外部框架"] -->|extends| DefaultTbServiceInfoProvider_c30["DefaultTbServiceInfoProvider"]
    TbServiceInfoProvider_p31["TbServiceInfoProvider"] -->|implements| DefaultTbServiceInfoProvider_c31["DefaultTbServiceInfoProvider"]
    Object______p32["Object/外部框架"] -->|extends| DummyDiscoveryService_c32["DummyDiscoveryService"]
    DiscoveryService_p33["DiscoveryService"] -->|implements| DummyDiscoveryService_c33["DummyDiscoveryService"]
    Object______p34["Object/外部框架"] -->|extends| HashPartitionService_c34["HashPartitionService"]
    PartitionService_p35["PartitionService"] -->|implements| HashPartitionService_c35["HashPartitionService"]
    Object______p36["Object/外部框架"] -->|extends| QueueKey_c36["QueueKey"]
    Object______p37["Object/外部框架"] -->|extends| QueueRoutingInfo_c37["QueueRoutingInfo"]
    Object______p38["Object/外部框架"] -->|extends| TbApplicationEventListener_c38["TbApplicationEventListener"]
    Object______p39["Object/外部框架"] -->|extends| TenantRoutingInfo_c39["TenantRoutingInfo"]
    Object______p40["Object/外部框架"] -->|extends| TopicService_c40["TopicService"]
    Object______p41["Object/外部框架"] -->|extends| ZkDiscoveryService_c41["ZkDiscoveryService"]
    DiscoveryService_p42["DiscoveryService"] -->|implements| ZkDiscoveryService_c42["ZkDiscoveryService"]
    PathChildrenCacheListener_p43["PathChildrenCacheListener"] -->|implements| ZkDiscoveryService_c43["ZkDiscoveryService"]
    TbApplicationEvent_p44["TbApplicationEvent"] -->|extends| ClusterTopologyChangeEvent_c44["ClusterTopologyChangeEvent"]
    TbApplicationEvent_p45["TbApplicationEvent"] -->|extends| OtherServiceShutdownEvent_c45["OtherServiceShutdownEvent"]
    TbApplicationEvent_p46["TbApplicationEvent"] -->|extends| PartitionChangeEvent_c46["PartitionChangeEvent"]
    TbApplicationEvent_p47["TbApplicationEvent"] -->|extends| ServiceListChangedEvent_c47["ServiceListChangedEvent"]
    ApplicationEvent_p48["ApplicationEvent"] -->|extends| TbApplicationEvent_c48["TbApplicationEvent"]
    Object______p49["Object/外部框架"] -->|extends| EnvironmentLogService_c49["EnvironmentLogService"]
    Object______p50["Object/外部框架"] -->|extends| KafkaTbQueueMsg_c50["KafkaTbQueueMsg"]
    TbQueueMsg_p51["TbQueueMsg"] -->|implements| KafkaTbQueueMsg_c51["KafkaTbQueueMsg"]
    Object______p52["Object/外部框架"] -->|extends| KafkaTbQueueMsgMetadata_c52["KafkaTbQueueMsgMetadata"]
    TbQueueMsgMetadata_p53["TbQueueMsgMetadata"] -->|implements| KafkaTbQueueMsgMetadata_c53["KafkaTbQueueMsgMetadata"]
    Object______p54["Object/外部框架"] -->|extends| TbKafkaAdmin_c54["TbKafkaAdmin"]
    TbQueueAdmin_p55["TbQueueAdmin"] -->|implements| TbKafkaAdmin_c55["TbKafkaAdmin"]
    Object______p56["Object/外部框架"] -->|extends| TbKafkaConsumerStatisticConfig_c56["TbKafkaConsumerStatisticConfig"]
    Object______p57["Object/外部框架"] -->|extends| TbKafkaConsumerStatsService_c57["TbKafkaConsumerStatsService"]
    Object______p58["Object/外部框架"] -->|extends| GroupTopicStats_c58["GroupTopicStats"]
    Object______p59["Object/外部框架"] -->|extends| TbKafkaConsumerTemplate_c59["TbKafkaConsumerTemplate"]
    Object______p60["Object/外部框架"] -->|extends| TbKafkaProducerTemplate_c60["TbKafkaProducerTemplate"]
    Object______p61["Object/外部框架"] -->|extends| TbKafkaSettings_c61["TbKafkaSettings"]
    Object______p62["Object/外部框架"] -->|extends| TbKafkaTopicConfigs_c62["TbKafkaTopicConfigs"]
    Object______p63["Object/外部框架"] -->|extends| DefaultInMemoryStorage_c63["DefaultInMemoryStorage"]
    InMemoryStorage_p64["InMemoryStorage"] -->|implements| DefaultInMemoryStorage_c64["DefaultInMemoryStorage"]
    Object______p65["Object/外部框架"] -->|extends| InMemoryTbQueueConsumer_c65["InMemoryTbQueueConsumer"]
    Object______p66["Object/外部框架"] -->|extends| InMemoryTbQueueProducer_c66["InMemoryTbQueueProducer"]
    Object______p67["Object/外部框架"] -->|extends| DefaultNotificationDeduplicationService_c67["DefaultNotificationDeduplicationService"]
    NotificationDeduplicationService_p68["NotificationDeduplicationService"] -->|implements| DefaultNotificationDeduplicationService_c68["DefaultNotificationDeduplicationService"]
    Object______p69["Object/外部框架"] -->|extends| RemoteNotificationRuleProcessor_c69["RemoteNotificationRuleProcessor"]
    NotificationRuleProcessor_p70["NotificationRuleProcessor"] -->|implements| RemoteNotificationRuleProcessor_c70["RemoteNotificationRuleProcessor"]
    Object______p71["Object/外部框架"] -->|extends| AwsSqsMonolithQueueFactory_c71["AwsSqsMonolithQueueFactory"]
    TbCoreQueueFactory_p72["TbCoreQueueFactory"] -->|implements| AwsSqsMonolithQueueFactory_c72["AwsSqsMonolithQueueFactory"]
    TbRuleEngineQueueFactory_p73["TbRuleEngineQueueFactory"] -->|implements| AwsSqsMonolithQueueFactory_c73["AwsSqsMonolithQueueFactory"]
    TbVersionControlQueueFactory_p74["TbVersionControlQueueFactory"] -->|implements| AwsSqsMonolithQueueFactory_c74["AwsSqsMonolithQueueFactory"]
    Object______p75["Object/外部框架"] -->|extends| AwsSqsTbCoreQueueFactory_c75["AwsSqsTbCoreQueueFactory"]
    TbCoreQueueFactory_p76["TbCoreQueueFactory"] -->|implements| AwsSqsTbCoreQueueFactory_c76["AwsSqsTbCoreQueueFactory"]
    Object______p77["Object/外部框架"] -->|extends| AwsSqsTbRuleEngineQueueFactory_c77["AwsSqsTbRuleEngineQueueFactory"]
    TbRuleEngineQueueFactory_p78["TbRuleEngineQueueFactory"] -->|implements| AwsSqsTbRuleEngineQueueFactory_c78["AwsSqsTbRuleEngineQueueFactory"]
    Object______p79["Object/外部框架"] -->|extends| AwsSqsTbVersionControlQueueFactory_c79["AwsSqsTbVersionControlQueueFactory"]
    TbVersionControlQueueFactory_p80["TbVersionControlQueueFactory"] -->|implements| AwsSqsTbVersionControlQueueFactory_c80["AwsSqsTbVersionControlQueueFactory"]
    Object______p81["Object/外部框架"] -->|extends| AwsSqsTransportQueueFactory_c81["AwsSqsTransportQueueFactory"]
    TbTransportQueueFactory_p82["TbTransportQueueFactory"] -->|implements| AwsSqsTransportQueueFactory_c82["AwsSqsTransportQueueFactory"]
    Object______p83["Object/外部框架"] -->|extends| InMemoryMonolithQueueFactory_c83["InMemoryMonolithQueueFactory"]
    TbCoreQueueFactory_p84["TbCoreQueueFactory"] -->|implements| InMemoryMonolithQueueFactory_c84["InMemoryMonolithQueueFactory"]
    TbRuleEngineQueueFactory_p85["TbRuleEngineQueueFactory"] -->|implements| InMemoryMonolithQueueFactory_c85["InMemoryMonolithQueueFactory"]
    TbVersionControlQueueFactory_p86["TbVersionControlQueueFactory"] -->|implements| InMemoryMonolithQueueFactory_c86["InMemoryMonolithQueueFactory"]
    Object______p87["Object/外部框架"] -->|extends| InMemoryTbTransportQueueFactory_c87["InMemoryTbTransportQueueFactory"]
    TbTransportQueueFactory_p88["TbTransportQueueFactory"] -->|implements| InMemoryTbTransportQueueFactory_c88["InMemoryTbTransportQueueFactory"]
    Object______p89["Object/外部框架"] -->|extends| KafkaMonolithQueueFactory_c89["KafkaMonolithQueueFactory"]
    TbCoreQueueFactory_p90["TbCoreQueueFactory"] -->|implements| KafkaMonolithQueueFactory_c90["KafkaMonolithQueueFactory"]
    TbRuleEngineQueueFactory_p91["TbRuleEngineQueueFactory"] -->|implements| KafkaMonolithQueueFactory_c91["KafkaMonolithQueueFactory"]
    TbVersionControlQueueFactory_p92["TbVersionControlQueueFactory"] -->|implements| KafkaMonolithQueueFactory_c92["KafkaMonolithQueueFactory"]
    Object______p93["Object/外部框架"] -->|extends| KafkaTbCoreQueueFactory_c93["KafkaTbCoreQueueFactory"]
    TbCoreQueueFactory_p94["TbCoreQueueFactory"] -->|implements| KafkaTbCoreQueueFactory_c94["KafkaTbCoreQueueFactory"]
    Object______p95["Object/外部框架"] -->|extends| KafkaTbRuleEngineQueueFactory_c95["KafkaTbRuleEngineQueueFactory"]
    TbRuleEngineQueueFactory_p96["TbRuleEngineQueueFactory"] -->|implements| KafkaTbRuleEngineQueueFactory_c96["KafkaTbRuleEngineQueueFactory"]
    Object______p97["Object/外部框架"] -->|extends| KafkaTbTransportQueueFactory_c97["KafkaTbTransportQueueFactory"]
    TbTransportQueueFactory_p98["TbTransportQueueFactory"] -->|implements| KafkaTbTransportQueueFactory_c98["KafkaTbTransportQueueFactory"]
    Object______p99["Object/外部框架"] -->|extends| KafkaTbVersionControlQueueFactory_c99["KafkaTbVersionControlQueueFactory"]
    TbVersionControlQueueFactory_p100["TbVersionControlQueueFactory"] -->|implements| KafkaTbVersionControlQueueFactory_c100["KafkaTbVersionControlQueueFactory"]
    Object______p101["Object/外部框架"] -->|extends| PubSubMonolithQueueFactory_c101["PubSubMonolithQueueFactory"]
    TbCoreQueueFactory_p102["TbCoreQueueFactory"] -->|implements| PubSubMonolithQueueFactory_c102["PubSubMonolithQueueFactory"]
    TbRuleEngineQueueFactory_p103["TbRuleEngineQueueFactory"] -->|implements| PubSubMonolithQueueFactory_c103["PubSubMonolithQueueFactory"]
    TbVersionControlQueueFactory_p104["TbVersionControlQueueFactory"] -->|implements| PubSubMonolithQueueFactory_c104["PubSubMonolithQueueFactory"]
    Object______p105["Object/外部框架"] -->|extends| PubSubTbCoreQueueFactory_c105["PubSubTbCoreQueueFactory"]
    TbCoreQueueFactory_p106["TbCoreQueueFactory"] -->|implements| PubSubTbCoreQueueFactory_c106["PubSubTbCoreQueueFactory"]
    Object______p107["Object/外部框架"] -->|extends| PubSubTbRuleEngineQueueFactory_c107["PubSubTbRuleEngineQueueFactory"]
    TbRuleEngineQueueFactory_p108["TbRuleEngineQueueFactory"] -->|implements| PubSubTbRuleEngineQueueFactory_c108["PubSubTbRuleEngineQueueFactory"]
    Object______p109["Object/外部框架"] -->|extends| PubSubTbVersionControlQueueFactory_c109["PubSubTbVersionControlQueueFactory"]
    TbVersionControlQueueFactory_p110["TbVersionControlQueueFactory"] -->|implements| PubSubTbVersionControlQueueFactory_c110["PubSubTbVersionControlQueueFactory"]
    Object______p111["Object/外部框架"] -->|extends| PubSubTransportQueueFactory_c111["PubSubTransportQueueFactory"]
    TbTransportQueueFactory_p112["TbTransportQueueFactory"] -->|implements| PubSubTransportQueueFactory_c112["PubSubTransportQueueFactory"]
    Object______p113["Object/外部框架"] -->|extends| RabbitMqMonolithQueueFactory_c113["RabbitMqMonolithQueueFactory"]
    TbCoreQueueFactory_p114["TbCoreQueueFactory"] -->|implements| RabbitMqMonolithQueueFactory_c114["RabbitMqMonolithQueueFactory"]
    TbRuleEngineQueueFactory_p115["TbRuleEngineQueueFactory"] -->|implements| RabbitMqMonolithQueueFactory_c115["RabbitMqMonolithQueueFactory"]
    TbVersionControlQueueFactory_p116["TbVersionControlQueueFactory"] -->|implements| RabbitMqMonolithQueueFactory_c116["RabbitMqMonolithQueueFactory"]
    Object______p117["Object/外部框架"] -->|extends| RabbitMqTbCoreQueueFactory_c117["RabbitMqTbCoreQueueFactory"]
    TbCoreQueueFactory_p118["TbCoreQueueFactory"] -->|implements| RabbitMqTbCoreQueueFactory_c118["RabbitMqTbCoreQueueFactory"]
    Object______p119["Object/外部框架"] -->|extends| RabbitMqTbRuleEngineQueueFactory_c119["RabbitMqTbRuleEngineQueueFactory"]
    TbRuleEngineQueueFactory_p120["TbRuleEngineQueueFactory"] -->|implements| RabbitMqTbRuleEngineQueueFactory_c120["RabbitMqTbRuleEngineQueueFactory"]
    Object______p121["Object/外部框架"] -->|extends| RabbitMqTbVersionControlQueueFactory_c121["RabbitMqTbVersionControlQueueFactory"]
    TbVersionControlQueueFactory_p122["TbVersionControlQueueFactory"] -->|implements| RabbitMqTbVersionControlQueueFactory_c122["RabbitMqTbVersionControlQueueFactory"]
    Object______p123["Object/外部框架"] -->|extends| RabbitMqTransportQueueFactory_c123["RabbitMqTransportQueueFactory"]
    TbTransportQueueFactory_p124["TbTransportQueueFactory"] -->|implements| RabbitMqTransportQueueFactory_c124["RabbitMqTransportQueueFactory"]
    Object______p125["Object/外部框架"] -->|extends| ServiceBusMonolithQueueFactory_c125["ServiceBusMonolithQueueFactory"]
    TbCoreQueueFactory_p126["TbCoreQueueFactory"] -->|implements| ServiceBusMonolithQueueFactory_c126["ServiceBusMonolithQueueFactory"]
    TbRuleEngineQueueFactory_p127["TbRuleEngineQueueFactory"] -->|implements| ServiceBusMonolithQueueFactory_c127["ServiceBusMonolithQueueFactory"]
    TbVersionControlQueueFactory_p128["TbVersionControlQueueFactory"] -->|implements| ServiceBusMonolithQueueFactory_c128["ServiceBusMonolithQueueFactory"]
    Object______p129["Object/外部框架"] -->|extends| ServiceBusTbCoreQueueFactory_c129["ServiceBusTbCoreQueueFactory"]
    TbCoreQueueFactory_p130["TbCoreQueueFactory"] -->|implements| ServiceBusTbCoreQueueFactory_c130["ServiceBusTbCoreQueueFactory"]
    Object______p131["Object/外部框架"] -->|extends| ServiceBusTbRuleEngineQueueFactory_c131["ServiceBusTbRuleEngineQueueFactory"]
    TbRuleEngineQueueFactory_p132["TbRuleEngineQueueFactory"] -->|implements| ServiceBusTbRuleEngineQueueFactory_c132["ServiceBusTbRuleEngineQueueFactory"]
    Object______p133["Object/外部框架"] -->|extends| ServiceBusTbVersionControlQueueFactory_c133["ServiceBusTbVersionControlQueueFactory"]
    TbVersionControlQueueFactory_p134["TbVersionControlQueueFactory"] -->|implements| ServiceBusTbVersionControlQueueFactory_c134["ServiceBusTbVersionControlQueueFactory"]
    Object______p135["Object/外部框架"] -->|extends| ServiceBusTransportQueueFactory_c135["ServiceBusTransportQueueFactory"]
    TbTransportQueueFactory_p136["TbTransportQueueFactory"] -->|implements| ServiceBusTransportQueueFactory_c136["ServiceBusTransportQueueFactory"]
    TbUsageStatsClientQueueFactory_p137["TbUsageStatsClientQueueFactory"] -->|extends| TbCoreQueueFactory_c137["TbCoreQueueFactory"]
    Object______p138["Object/外部框架"] -->|extends| TbCoreQueueProducerProvider_c138["TbCoreQueueProducerProvider"]
    TbQueueProducerProvider_p139["TbQueueProducerProvider"] -->|implements| TbCoreQueueProducerProvider_c139["TbCoreQueueProducerProvider"]
    Object______p140["Object/外部框架"] -->|extends| TbRuleEngineProducerProvider_c140["TbRuleEngineProducerProvider"]
    TbQueueProducerProvider_p141["TbQueueProducerProvider"] -->|implements| TbRuleEngineProducerProvider_c141["TbRuleEngineProducerProvider"]
    TbUsageStatsClientQueueFactory_p142["TbUsageStatsClientQueueFactory"] -->|extends| TbRuleEngineQueueFactory_c142["TbRuleEngineQueueFactory"]
    TbUsageStatsClientQueueFactory_p143["TbUsageStatsClientQueueFactory"] -->|extends| TbTransportQueueFactory_c143["TbTransportQueueFactory"]
    Object______p144["Object/外部框架"] -->|extends| TbTransportQueueProducerProvider_c144["TbTransportQueueProducerProvider"]
    TbQueueProducerProvider_p145["TbQueueProducerProvider"] -->|implements| TbTransportQueueProducerProvider_c145["TbTransportQueueProducerProvider"]
    Object______p146["Object/外部框架"] -->|extends| TbVersionControlProducerProvider_c146["TbVersionControlProducerProvider"]
    TbQueueProducerProvider_p147["TbQueueProducerProvider"] -->|implements| TbVersionControlProducerProvider_c147["TbVersionControlProducerProvider"]
    TbUsageStatsClientQueueFactory_p148["TbUsageStatsClientQueueFactory"] -->|extends| TbVersionControlQueueFactory_c148["TbVersionControlQueueFactory"]
    Object______p149["Object/外部框架"] -->|extends| TbPubSubAdmin_c149["TbPubSubAdmin"]
    TbQueueAdmin_p150["TbQueueAdmin"] -->|implements| TbPubSubAdmin_c150["TbPubSubAdmin"]
    Object______p151["Object/外部框架"] -->|extends| TbPubSubConsumerTemplate_c151["TbPubSubConsumerTemplate"]
    Object______p152["Object/外部框架"] -->|extends| TbPubSubProducerTemplate_c152["TbPubSubProducerTemplate"]
    Object______p153["Object/外部框架"] -->|extends| TbPubSubSettings_c153["TbPubSubSettings"]
    Object______p154["Object/外部框架"] -->|extends| TbPubSubSubscriptionSettings_c154["TbPubSubSubscriptionSettings"]
    Object______p155["Object/外部框架"] -->|extends| TbRabbitMqAdmin_c155["TbRabbitMqAdmin"]
    TbQueueAdmin_p156["TbQueueAdmin"] -->|implements| TbRabbitMqAdmin_c156["TbRabbitMqAdmin"]
    Object______p157["Object/外部框架"] -->|extends| TbRabbitMqConsumerTemplate_c157["TbRabbitMqConsumerTemplate"]
    Object______p158["Object/外部框架"] -->|extends| TbRabbitMqProducerTemplate_c158["TbRabbitMqProducerTemplate"]
    Object______p159["Object/外部框架"] -->|extends| TbRabbitMqQueueArguments_c159["TbRabbitMqQueueArguments"]
    Object______p160["Object/外部框架"] -->|extends| TbRabbitMqSettings_c160["TbRabbitMqSettings"]
    Object______p161["Object/外部框架"] -->|extends| DefaultSchedulerComponent_c161["DefaultSchedulerComponent"]
    SchedulerComponent_p162["SchedulerComponent"] -->|implements| DefaultSchedulerComponent_c162["DefaultSchedulerComponent"]
    Object______p163["Object/外部框架"] -->|extends| TbQueueCoreSettings_c163["TbQueueCoreSettings"]
    Object______p164["Object/外部框架"] -->|extends| TbQueueRemoteJsInvokeSettings_c164["TbQueueRemoteJsInvokeSettings"]
    Object______p165["Object/外部框架"] -->|extends| TbQueueRuleEngineSettings_c165["TbQueueRuleEngineSettings"]
    Object______p166["Object/外部框架"] -->|extends| TbQueueTransportApiSettings_c166["TbQueueTransportApiSettings"]
    Object______p167["Object/外部框架"] -->|extends| TbQueueTransportNotificationSettings_c167["TbQueueTransportNotificationSettings"]
    Object______p168["Object/外部框架"] -->|extends| TbQueueVersionControlSettings_c168["TbQueueVersionControlSettings"]
    Object______p169["Object/外部框架"] -->|extends| AwsSqsTbQueueMsgMetadata_c169["AwsSqsTbQueueMsgMetadata"]
    TbQueueMsgMetadata_p170["TbQueueMsgMetadata"] -->|implements| AwsSqsTbQueueMsgMetadata_c170["AwsSqsTbQueueMsgMetadata"]
    Object______p171["Object/外部框架"] -->|extends| TbAwsSqsAdmin_c171["TbAwsSqsAdmin"]
    TbQueueAdmin_p172["TbQueueAdmin"] -->|implements| TbAwsSqsAdmin_c172["TbAwsSqsAdmin"]
    Object______p173["Object/外部框架"] -->|extends| TbAwsSqsConsumerTemplate_c173["TbAwsSqsConsumerTemplate"]
    Object______p174["Object/外部框架"] -->|extends| AwsSqsMsgWrapper_c174["AwsSqsMsgWrapper"]
    Object______p175["Object/外部框架"] -->|extends| TbAwsSqsProducerTemplate_c175["TbAwsSqsProducerTemplate"]
    Object______p176["Object/外部框架"] -->|extends| TbAwsSqsQueueAttributes_c176["TbAwsSqsQueueAttributes"]
    Object______p177["Object/外部框架"] -->|extends| TbAwsSqsSettings_c177["TbAwsSqsSettings"]
    Object______p178["Object/外部框架"] -->|extends| DefaultTbApiUsageReportClient_c178["DefaultTbApiUsageReportClient"]
    TbApiUsageReportClient_p179["TbApiUsageReportClient"] -->|implements| DefaultTbApiUsageReportClient_c179["DefaultTbApiUsageReportClient"]
    Object______p180["Object/外部框架"] -->|extends| ReportLevel_c180["ReportLevel"]
    Object______p181["Object/外部框架"] -->|extends| ParentEntity_c181["ParentEntity"]
    Object______p182["Object/外部框架"] -->|extends| PropertyUtils_c182["PropertyUtils"]
    Object______p183["Object/外部框架"] -->|extends| ProtoWithFSTService_c183["ProtoWithFSTService"]
    DataDecodingEncodingService_p184["DataDecodingEncodingService"] -->|implements| ProtoWithFSTService_c184["ProtoWithFSTService"]
    Object______p185["Object/外部框架"] -->|extends| DefaultTbQueueRequestTemplateTest_c185["DefaultTbQueueRequestTemplateTest"]
    Object______p186["Object/外部框架"] -->|extends| QueueKeyTest_c186["QueueKeyTest"]
    Object______p187["Object/外部框架"] -->|extends| ZkDiscoveryServiceTest_c187["ZkDiscoveryServiceTest"]
    Object______p188["Object/外部框架"] -->|extends| TbKafkaProducerTemplateTest_c188["TbKafkaProducerTemplateTest"]
    Object______p189["Object/外部框架"] -->|extends| TbKafkaSettingsTest_c189["TbKafkaSettingsTest"]
    Object______p190["Object/外部框架"] -->|extends| DefaultInMemoryStorageTest_c190["DefaultInMemoryStorageTest"]
    Object______p191["Object/外部框架"] -->|extends| PropertyUtilsTest_c191["PropertyUtilsTest"]
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

- `AbstractParallelTbQueueConsumerTemplate.initNewExecutor()` (protected, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractParallelTbQueueConsumerTemplate.java`)
- `AbstractParallelTbQueueConsumerTemplate.shutdownExecutor()` (protected, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractParallelTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.ReentrantLock()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.subscribe()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.TopicPartitionInfo()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.subscribe()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.poll()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.RuntimeException()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.emptyList()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.sleepAndReturnEmpty()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.RuntimeException()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.sleepAndReturnEmpty()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.decodeRecords()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.decodeRecords()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.RuntimeException()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.sleepAndReturnEmpty()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.emptyList()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.commit()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.RuntimeException()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.stop()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.unsubscribe()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.isStopped()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.doPoll()` (protected, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.doSubscribe()` (protected, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.doCommit()` (protected, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.doUnsubscribe()` (protected, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.getFullTopicNames()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.isLongPollingSupported()` (protected, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `DiscoveryService.getOtherServers()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/discovery/DiscoveryService.java`)
- `DiscoveryService.isMonolith()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/discovery/DiscoveryService.java`)
- `PartitionService.resolve()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/discovery/PartitionService.java`)
- `PartitionService.resolve()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/discovery/PartitionService.java`)
- `PartitionService.isMyPartition()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/discovery/PartitionService.java`)
- `PartitionService.getMyPartitions()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/discovery/PartitionService.java`)
- `PartitionService.recalculatePartitions()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/discovery/PartitionService.java`)
- `PartitionService.getAllServiceIds()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/discovery/PartitionService.java`)
- `PartitionService.getAllServices()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/discovery/PartitionService.java`)
- `PartitionService.getOtherServices()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/discovery/PartitionService.java`)
- `PartitionService.resolvePartitionIndex()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/discovery/PartitionService.java`)
- `PartitionService.evictTenantInfo()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/discovery/PartitionService.java`)


## 哪些方法可以重写

- `RuleEngineTbQueueAdminFactory.createKafkaAdmin()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/RuleEngineTbQueueAdminFactory.java`)
- `RuleEngineTbQueueAdminFactory.TbKafkaAdmin()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/RuleEngineTbQueueAdminFactory.java`)
- `RuleEngineTbQueueAdminFactory.createAwsSqsAdmin()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/RuleEngineTbQueueAdminFactory.java`)
- `RuleEngineTbQueueAdminFactory.TbAwsSqsAdmin()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/RuleEngineTbQueueAdminFactory.java`)
- `RuleEngineTbQueueAdminFactory.createPubSubAdmin()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/RuleEngineTbQueueAdminFactory.java`)
- `RuleEngineTbQueueAdminFactory.TbPubSubAdmin()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/RuleEngineTbQueueAdminFactory.java`)
- `RuleEngineTbQueueAdminFactory.createRabbitMqAdmin()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/RuleEngineTbQueueAdminFactory.java`)
- `RuleEngineTbQueueAdminFactory.TbRabbitMqAdmin()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/RuleEngineTbQueueAdminFactory.java`)
- `RuleEngineTbQueueAdminFactory.createServiceBusAdmin()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/RuleEngineTbQueueAdminFactory.java`)
- `RuleEngineTbQueueAdminFactory.TbServiceBusAdmin()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/RuleEngineTbQueueAdminFactory.java`)
- `RuleEngineTbQueueAdminFactory.createInMemoryAdmin()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/RuleEngineTbQueueAdminFactory.java`)
- `RuleEngineTbQueueAdminFactory.TbQueueAdmin()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/RuleEngineTbQueueAdminFactory.java`)
- `RuleEngineTbQueueAdminFactory.createTopicIfNotExists()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/RuleEngineTbQueueAdminFactory.java`)
- `RuleEngineTbQueueAdminFactory.deleteTopic()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/RuleEngineTbQueueAdminFactory.java`)
- `RuleEngineTbQueueAdminFactory.destroy()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/RuleEngineTbQueueAdminFactory.java`)
- `TbServiceBusAdmin.ConnectionStringBuilder()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusAdmin.java`)
- `TbServiceBusAdmin.ManagementClient()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusAdmin.java`)
- `TbServiceBusAdmin.RuntimeException()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusAdmin.java`)
- `TbServiceBusAdmin.createTopicIfNotExists()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusAdmin.java`)
- `TbServiceBusAdmin.QueueDescription()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusAdmin.java`)
- `TbServiceBusAdmin.deleteTopic()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusAdmin.java`)
- `TbServiceBusAdmin.destroy()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusAdmin.java`)
- `TbServiceBusConsumerTemplate.Gson()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusConsumerTemplate.java`)
- `TbServiceBusConsumerTemplate.doPoll()` (protected, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusConsumerTemplate.java`)
- `TbServiceBusConsumerTemplate.fromList()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusConsumerTemplate.java`)
- `TbServiceBusConsumerTemplate.doSubscribe()` (protected, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusConsumerTemplate.java`)
- `TbServiceBusConsumerTemplate.doCommit()` (protected, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusConsumerTemplate.java`)
- `TbServiceBusConsumerTemplate.doUnsubscribe()` (protected, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusConsumerTemplate.java`)
- `TbServiceBusConsumerTemplate.RuntimeException()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusConsumerTemplate.java`)
- `TbServiceBusConsumerTemplate.SettleModePair()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusConsumerTemplate.java`)
- `TbServiceBusConsumerTemplate.ConnectionStringBuilder()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusConsumerTemplate.java`)
- `TbServiceBusConsumerTemplate.String()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusConsumerTemplate.java`)
- `TbServiceBusProducerTemplate.Gson()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusProducerTemplate.java`)
- `TbServiceBusProducerTemplate.init()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusProducerTemplate.java`)
- `TbServiceBusProducerTemplate.getDefaultTopic()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusProducerTemplate.java`)
- `TbServiceBusProducerTemplate.send()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusProducerTemplate.java`)
- `TbServiceBusProducerTemplate.Message()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusProducerTemplate.java`)
- `TbServiceBusProducerTemplate.stop()` (public, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusProducerTemplate.java`)
- `TbServiceBusProducerTemplate.ConnectionStringBuilder()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusProducerTemplate.java`)
- `TbServiceBusProducerTemplate.QueueClient()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusProducerTemplate.java`)


## 哪些方法必须重写

- `RuleEngineTbQueueAdminFactory.TbKafkaAdmin()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/RuleEngineTbQueueAdminFactory.java`)
- `RuleEngineTbQueueAdminFactory.TbAwsSqsAdmin()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/RuleEngineTbQueueAdminFactory.java`)
- `RuleEngineTbQueueAdminFactory.TbPubSubAdmin()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/RuleEngineTbQueueAdminFactory.java`)
- `RuleEngineTbQueueAdminFactory.TbRabbitMqAdmin()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/RuleEngineTbQueueAdminFactory.java`)
- `RuleEngineTbQueueAdminFactory.TbServiceBusAdmin()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/RuleEngineTbQueueAdminFactory.java`)
- `TbServiceBusAdmin.ConnectionStringBuilder()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusAdmin.java`)
- `TbServiceBusAdmin.ManagementClient()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusAdmin.java`)
- `TbServiceBusAdmin.RuntimeException()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusAdmin.java`)
- `TbServiceBusAdmin.QueueDescription()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusAdmin.java`)
- `TbServiceBusConsumerTemplate.Gson()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusConsumerTemplate.java`)
- `TbServiceBusConsumerTemplate.fromList()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusConsumerTemplate.java`)
- `TbServiceBusConsumerTemplate.RuntimeException()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusConsumerTemplate.java`)
- `TbServiceBusConsumerTemplate.SettleModePair()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusConsumerTemplate.java`)
- `TbServiceBusConsumerTemplate.ConnectionStringBuilder()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusConsumerTemplate.java`)
- `TbServiceBusConsumerTemplate.String()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusConsumerTemplate.java`)
- `TbServiceBusProducerTemplate.Gson()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusProducerTemplate.java`)
- `TbServiceBusProducerTemplate.Message()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusProducerTemplate.java`)
- `TbServiceBusProducerTemplate.ConnectionStringBuilder()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusProducerTemplate.java`)
- `TbServiceBusProducerTemplate.QueueClient()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusProducerTemplate.java`)
- `TbServiceBusProducerTemplate.RuntimeException()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/azure/servicebus/TbServiceBusProducerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.ReentrantLock()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.TopicPartitionInfo()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.RuntimeException()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.emptyList()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.sleepAndReturnEmpty()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.RuntimeException()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.sleepAndReturnEmpty()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.decodeRecords()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.RuntimeException()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.emptyList()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.RuntimeException()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.doPoll()` (protected, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.doSubscribe()` (protected, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.doCommit()` (protected, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueConsumerTemplate.doUnsubscribe()` (protected, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueConsumerTemplate.java`)
- `AbstractTbQueueTemplate.UUID()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueTemplate.java`)
- `AbstractTbQueueTemplate.String()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/AbstractTbQueueTemplate.java`)
- `DefaultTbQueueMsg.DefaultTbQueueMsgHeaders()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/DefaultTbQueueMsg.java`)
- `DefaultTbQueueRequestTemplate.ReentrantLock()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/DefaultTbQueueRequestTemplate.java`)
- `DefaultTbQueueRequestTemplate.TimeoutException()` (package, `common/queue/src/main/java/org/thingsboard/server/queue/common/DefaultTbQueueRequestTemplate.java`)


## 哪些地方使用了多态

- `Object/外部框架` -> `RuleEngineTbQueueAdminFactory` (extends)
- `Object/外部框架` -> `TbServiceBusAdmin` (extends)
- `TbQueueAdmin` -> `TbServiceBusAdmin` (implements)
- `Object/外部框架` -> `TbServiceBusConsumerTemplate` (extends)
- `Object/外部框架` -> `TbServiceBusProducerTemplate` (extends)
- `Object/外部框架` -> `TbServiceBusQueueConfigs` (extends)
- `Object/外部框架` -> `TbServiceBusSettings` (extends)
- `Object/外部框架` -> `AbstractParallelTbQueueConsumerTemplate` (extends)
- `Object/外部框架` -> `AbstractTbQueueConsumerTemplate` (extends)
- `Object/外部框架` -> `AbstractTbQueueTemplate` (extends)
- `Object/外部框架` -> `AsyncCallbackTemplate` (extends)
- `Object/外部框架` -> `DefaultTbQueueMsg` (extends)
- `TbQueueMsg` -> `DefaultTbQueueMsg` (implements)
- `Object/外部框架` -> `DefaultTbQueueMsgHeaders` (extends)
- `TbQueueMsgHeaders` -> `DefaultTbQueueMsgHeaders` (implements)
- `Object/外部框架` -> `DefaultTbQueueRequestTemplate` (extends)
- `Object/外部框架` -> `ResponseMetaData` (extends)
- `Object/外部框架` -> `DefaultTbQueueResponseTemplate` (extends)
- `Object/外部框架` -> `MultipleTbQueueCallbackWrapper` (extends)
- `TbQueueCallback` -> `MultipleTbQueueCallbackWrapper` (implements)
- `Object/外部框架` -> `MultipleTbQueueTbMsgCallbackWrapper` (extends)
- `TbQueueCallback` -> `MultipleTbQueueTbMsgCallbackWrapper` (implements)
- `Object/外部框架` -> `SimpleTbQueueCallback` (extends)
- `TbQueueCallback` -> `SimpleTbQueueCallback` (implements)
- `Object/外部框架` -> `TbProtoJsQueueMsg` (extends)
- `Object/外部框架` -> `TbProtoQueueMsg` (extends)
- `TbQueueMsg` -> `TbProtoQueueMsg` (implements)
- `Object/外部框架` -> `TbQueueTbMsgCallbackWrapper` (extends)
- `TbQueueCallback` -> `TbQueueTbMsgCallbackWrapper` (implements)
- `Object/外部框架` -> `ConsistentHashCircle` (extends)
- `Object/外部框架` -> `DefaultTbServiceInfoProvider` (extends)
- `TbServiceInfoProvider` -> `DefaultTbServiceInfoProvider` (implements)
- `Object/外部框架` -> `DummyDiscoveryService` (extends)
- `DiscoveryService` -> `DummyDiscoveryService` (implements)
- `Object/外部框架` -> `HashPartitionService` (extends)
- `PartitionService` -> `HashPartitionService` (implements)
- `Object/外部框架` -> `QueueKey` (extends)
- `Object/外部框架` -> `QueueRoutingInfo` (extends)
- `Object/外部框架` -> `TbApplicationEventListener` (extends)
- `Object/外部框架` -> `TenantRoutingInfo` (extends)
- 其余 40 项已省略；完整源码证据可通过本模块 Java 文件继续追踪。


## 外部父类/接口

- `ApplicationEvent`
- `NotificationRuleProcessor`
- `Object/外部框架`
- `PathChildrenCacheListener`
- `TbApiUsageReportClient`
- `TbQueueAdmin`
- `TbQueueCallback`
- `TbQueueMsg`
- `TbQueueMsgHeaders`
- `TbQueueMsgMetadata`


## 整个继承体系解决了什么问题

该继承体系把“稳定生命周期/契约”和“模块特定行为”分开：父类或接口负责统一入口、共享流程和多态调度，子类负责差异化协议、实体、规则、DAO、测试或工具行为。这样可以让上游模块通过父类型调用下游实现，同时保持每个实现只关注自己的变化点。
