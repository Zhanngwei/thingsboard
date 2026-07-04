# Thingsboard Server Common Transport components 模块继承体系分析

> 生成范围：`common/transport/transport-api`  
> Maven artifact：`transport-api`  
> Java 类型数量：75  
> 分析方式：静态扫描 `class/interface/enum/record` 的 `extends`、`implements`、方法签名和源码触点。

## 完整继承树

```text
ActivityStrategy
├── «implements» AllEventsActivityStrategy
├── «implements» FirstAndLastEventActivityStrategy
├── «implements» FirstEventActivityStrategy
├── «implements» LastEventActivityStrategy
DeviceProfileAware
├── «implements» GetOrCreateDeviceFromGatewayResponse
├── «implements» ValidateDeviceCredentialsResponse
EntityLimitsCache
├── «implements» DefaultEntityLimitsCache
Object/外部框架
├── AbstractActivityManager
├── ├── TransportActivityManager
├── ├── ├── DefaultTransportService
├── AbstractSslCredentials
├── ├── KeystoreSslCredentials
├── ├── PemSslCredentials
├── ActivityState
├── ActivityStateWrapper
├── ActivityStrategyTypeTest
├── AllEventsActivityStrategy
├── AllEventsActivityStrategyTest
├── ApiStatsProxyCallback
├── DefaultEntityLimitsCache
├── DefaultTransportDeviceProfileCache
├── DefaultTransportRateLimitService
├── DefaultTransportResourceCache
├── DefaultTransportTenantProfileCache
├── DeviceAuthResult
├── DeviceAwareSessionContext
├── DeviceUpdatedEvent
├── DummyTransportRateLimit
├── EntityLimitKey
├── EntityTransportRateLimits
├── FirstAndLastEventActivityStrategy
├── FirstAndLastEventActivityStrategyTest
├── FirstEventActivityStrategy
├── FirstEventActivityStrategyTest
├── GetOrCreateDeviceFromGatewayResponse
├── InetAddressRateLimitStats
├── JsonUtils
├── LastEventActivityStrategy
├── LastEventActivityStrategyTest
├── MsgPackCallback
├── ResourceCompositeKey
├── RpcRequestMetadata
├── SessionInfoCreator
├── SessionMetaData
├── SimpleTransportRateLimit
├── SslCredentialsConfig
├── SslCredentialsWebServerCustomizer
├── SslUtil
├── StatsCallback
├── TenantProfileUpdateResult
├── ToRuleEngineMsgEncoder
├── ToTransportMsgResponseDecoder
├── TransportActivityManagerTest
├── TransportApiRequestEncoder
├── TransportApiResponseDecoder
├── TransportContext
├── TransportDeviceInfo
├── TransportQueueRoutingInfoService
├── TransportTbQueueCallback
├── TransportTenantRoutingInfoService
├── ValidateDeviceCredentialsResponse
QueueRoutingInfoService
├── «implements» TransportQueueRoutingInfoService
Serializable
├── «implements» TransportDeviceInfo
├── «implements» ValidateDeviceCredentialsResponse
SessionContext
├── «implements» DeviceAwareSessionContext
SslCredentials
├── «implements» AbstractSslCredentials
├── «implements» ├── KeystoreSslCredentials
├── «implements» ├── PemSslCredentials
TbApplicationEvent
├── DeviceDeletedEvent
├── DeviceProfileUpdatedEvent
TbKafkaDecoder
├── «implements» ToTransportMsgResponseDecoder
├── «implements» TransportApiResponseDecoder
TbKafkaEncoder
├── «implements» ToRuleEngineMsgEncoder
├── «implements» TransportApiRequestEncoder
TbQueueCallback
├── «implements» MsgPackCallback
├── «implements» StatsCallback
├── «implements» TransportTbQueueCallback
TenantRoutingInfoService
├── «implements» TransportTenantRoutingInfoService
TransportDeviceProfileCache
├── «implements» DefaultTransportDeviceProfileCache
TransportRateLimit
├── «implements» DummyTransportRateLimit
├── «implements» SimpleTransportRateLimit
TransportRateLimitService
├── «implements» DefaultTransportRateLimitService
TransportResourceCache
├── «implements» DefaultTransportResourceCache
TransportService
├── «implements» DefaultTransportService
├── «implements» TransportActivityManager
├── «implements» ├── DefaultTransportService
TransportTenantProfileCache
├── «implements» DefaultTransportTenantProfileCache
WebServerFactoryCustomizer
├── «implements» SslCredentialsWebServerCustomizer
```

## 继承图

```mermaid
flowchart TD
    TbApplicationEvent_p0["TbApplicationEvent"] -->|extends| DeviceDeletedEvent_c0["DeviceDeletedEvent"]
    TbApplicationEvent_p1["TbApplicationEvent"] -->|extends| DeviceProfileUpdatedEvent_c1["DeviceProfileUpdatedEvent"]
    Object______p2["Object/外部框架"] -->|extends| DeviceUpdatedEvent_c2["DeviceUpdatedEvent"]
    Object______p3["Object/外部框架"] -->|extends| TransportContext_c3["TransportContext"]
    Object______p4["Object/外部框架"] -->|extends| AbstractActivityManager_c4["AbstractActivityManager"]
    Object______p5["Object/外部框架"] -->|extends| ActivityStateWrapper_c5["ActivityStateWrapper"]
    Object______p6["Object/外部框架"] -->|extends| ActivityState_c6["ActivityState"]
    Object______p7["Object/外部框架"] -->|extends| AllEventsActivityStrategy_c7["AllEventsActivityStrategy"]
    ActivityStrategy_p8["ActivityStrategy"] -->|implements| AllEventsActivityStrategy_c8["AllEventsActivityStrategy"]
    Object______p9["Object/外部框架"] -->|extends| FirstAndLastEventActivityStrategy_c9["FirstAndLastEventActivityStrategy"]
    ActivityStrategy_p10["ActivityStrategy"] -->|implements| FirstAndLastEventActivityStrategy_c10["FirstAndLastEventActivityStrategy"]
    Object______p11["Object/外部框架"] -->|extends| FirstEventActivityStrategy_c11["FirstEventActivityStrategy"]
    ActivityStrategy_p12["ActivityStrategy"] -->|implements| FirstEventActivityStrategy_c12["FirstEventActivityStrategy"]
    Object______p13["Object/外部框架"] -->|extends| LastEventActivityStrategy_c13["LastEventActivityStrategy"]
    ActivityStrategy_p14["ActivityStrategy"] -->|implements| LastEventActivityStrategy_c14["LastEventActivityStrategy"]
    Object______p15["Object/外部框架"] -->|extends| DeviceAuthResult_c15["DeviceAuthResult"]
    Object______p16["Object/外部框架"] -->|extends| GetOrCreateDeviceFromGatewayResponse_c16["GetOrCreateDeviceFromGatewayResponse"]
    DeviceProfileAware_p17["DeviceProfileAware"] -->|implements| GetOrCreateDeviceFromGatewayResponse_c17["GetOrCreateDeviceFromGatewayResponse"]
    Object______p18["Object/外部框架"] -->|extends| SessionInfoCreator_c18["SessionInfoCreator"]
    Object______p19["Object/外部框架"] -->|extends| TransportDeviceInfo_c19["TransportDeviceInfo"]
    Serializable_p20["Serializable"] -->|implements| TransportDeviceInfo_c20["TransportDeviceInfo"]
    Object______p21["Object/外部框架"] -->|extends| ValidateDeviceCredentialsResponse_c21["ValidateDeviceCredentialsResponse"]
    DeviceProfileAware_p22["DeviceProfileAware"] -->|implements| ValidateDeviceCredentialsResponse_c22["ValidateDeviceCredentialsResponse"]
    Serializable_p23["Serializable"] -->|implements| ValidateDeviceCredentialsResponse_c23["ValidateDeviceCredentialsResponse"]
    Object______p24["Object/外部框架"] -->|extends| AbstractSslCredentials_c24["AbstractSslCredentials"]
    SslCredentials_p25["SslCredentials"] -->|implements| AbstractSslCredentials_c25["AbstractSslCredentials"]
    AbstractSslCredentials_p26["AbstractSslCredentials"] -->|extends| KeystoreSslCredentials_c26["KeystoreSslCredentials"]
    AbstractSslCredentials_p27["AbstractSslCredentials"] -->|extends| PemSslCredentials_c27["PemSslCredentials"]
    Object______p28["Object/外部框架"] -->|extends| SslCredentialsConfig_c28["SslCredentialsConfig"]
    Object______p29["Object/外部框架"] -->|extends| SslCredentialsWebServerCustomizer_c29["SslCredentialsWebServerCustomizer"]
    WebServerFactoryCustomizer_p30["WebServerFactoryCustomizer"] -->|implements| SslCredentialsWebServerCustomizer_c30["SslCredentialsWebServerCustomizer"]
    Object______p31["Object/外部框架"] -->|extends| DefaultEntityLimitsCache_c31["DefaultEntityLimitsCache"]
    EntityLimitsCache_p32["EntityLimitsCache"] -->|implements| DefaultEntityLimitsCache_c32["DefaultEntityLimitsCache"]
    Object______p33["Object/外部框架"] -->|extends| DefaultTransportRateLimitService_c33["DefaultTransportRateLimitService"]
    TransportRateLimitService_p34["TransportRateLimitService"] -->|implements| DefaultTransportRateLimitService_c34["DefaultTransportRateLimitService"]
    Object______p35["Object/外部框架"] -->|extends| DummyTransportRateLimit_c35["DummyTransportRateLimit"]
    TransportRateLimit_p36["TransportRateLimit"] -->|implements| DummyTransportRateLimit_c36["DummyTransportRateLimit"]
    Object______p37["Object/外部框架"] -->|extends| EntityLimitKey_c37["EntityLimitKey"]
    Object______p38["Object/外部框架"] -->|extends| EntityTransportRateLimits_c38["EntityTransportRateLimits"]
    Object______p39["Object/外部框架"] -->|extends| InetAddressRateLimitStats_c39["InetAddressRateLimitStats"]
    Object______p40["Object/外部框架"] -->|extends| SimpleTransportRateLimit_c40["SimpleTransportRateLimit"]
    TransportRateLimit_p41["TransportRateLimit"] -->|implements| SimpleTransportRateLimit_c41["SimpleTransportRateLimit"]
    Object______p42["Object/外部框架"] -->|extends| TenantProfileUpdateResult_c42["TenantProfileUpdateResult"]
    Object______p43["Object/外部框架"] -->|extends| DefaultTransportDeviceProfileCache_c43["DefaultTransportDeviceProfileCache"]
    TransportDeviceProfileCache_p44["TransportDeviceProfileCache"] -->|implements| DefaultTransportDeviceProfileCache_c44["DefaultTransportDeviceProfileCache"]
    Object______p45["Object/外部框架"] -->|extends| DefaultTransportResourceCache_c45["DefaultTransportResourceCache"]
    TransportResourceCache_p46["TransportResourceCache"] -->|implements| DefaultTransportResourceCache_c46["DefaultTransportResourceCache"]
    Object______p47["Object/外部框架"] -->|extends| ResourceCompositeKey_c47["ResourceCompositeKey"]
    TransportActivityManager_p48["TransportActivityManager"] -->|extends| DefaultTransportService_c48["DefaultTransportService"]
    TransportService_p49["TransportService"] -->|implements| DefaultTransportService_c49["DefaultTransportService"]
    Object______p50["Object/外部框架"] -->|extends| TransportTbQueueCallback_c50["TransportTbQueueCallback"]
    TbQueueCallback_p51["TbQueueCallback"] -->|implements| TransportTbQueueCallback_c51["TransportTbQueueCallback"]
    Object______p52["Object/外部框架"] -->|extends| StatsCallback_c52["StatsCallback"]
    TbQueueCallback_p53["TbQueueCallback"] -->|implements| StatsCallback_c53["StatsCallback"]
    Object______p54["Object/外部框架"] -->|extends| MsgPackCallback_c54["MsgPackCallback"]
    TbQueueCallback_p55["TbQueueCallback"] -->|implements| MsgPackCallback_c55["MsgPackCallback"]
    Object______p56["Object/外部框架"] -->|extends| ApiStatsProxyCallback_c56["ApiStatsProxyCallback"]
    Object______p57["Object/外部框架"] -->|extends| DefaultTransportTenantProfileCache_c57["DefaultTransportTenantProfileCache"]
    TransportTenantProfileCache_p58["TransportTenantProfileCache"] -->|implements| DefaultTransportTenantProfileCache_c58["DefaultTransportTenantProfileCache"]
    Object______p59["Object/外部框架"] -->|extends| RpcRequestMetadata_c59["RpcRequestMetadata"]
    Object______p60["Object/外部框架"] -->|extends| SessionMetaData_c60["SessionMetaData"]
    Object______p61["Object/外部框架"] -->|extends| ToRuleEngineMsgEncoder_c61["ToRuleEngineMsgEncoder"]
    TbKafkaEncoder_p62["TbKafkaEncoder"] -->|implements| ToRuleEngineMsgEncoder_c62["ToRuleEngineMsgEncoder"]
    Object______p63["Object/外部框架"] -->|extends| ToTransportMsgResponseDecoder_c63["ToTransportMsgResponseDecoder"]
    TbKafkaDecoder_p64["TbKafkaDecoder"] -->|implements| ToTransportMsgResponseDecoder_c64["ToTransportMsgResponseDecoder"]
    AbstractActivityManager_p65["AbstractActivityManager"] -->|extends| TransportActivityManager_c65["TransportActivityManager"]
    TransportService_p66["TransportService"] -->|implements| TransportActivityManager_c66["TransportActivityManager"]
    Object______p67["Object/外部框架"] -->|extends| TransportApiRequestEncoder_c67["TransportApiRequestEncoder"]
    TbKafkaEncoder_p68["TbKafkaEncoder"] -->|implements| TransportApiRequestEncoder_c68["TransportApiRequestEncoder"]
    Object______p69["Object/外部框架"] -->|extends| TransportApiResponseDecoder_c69["TransportApiResponseDecoder"]
    TbKafkaDecoder_p70["TbKafkaDecoder"] -->|implements| TransportApiResponseDecoder_c70["TransportApiResponseDecoder"]
    Object______p71["Object/外部框架"] -->|extends| TransportQueueRoutingInfoService_c71["TransportQueueRoutingInfoService"]
    QueueRoutingInfoService_p72["QueueRoutingInfoService"] -->|implements| TransportQueueRoutingInfoService_c72["TransportQueueRoutingInfoService"]
    Object______p73["Object/外部框架"] -->|extends| TransportTenantRoutingInfoService_c73["TransportTenantRoutingInfoService"]
    TenantRoutingInfoService_p74["TenantRoutingInfoService"] -->|implements| TransportTenantRoutingInfoService_c74["TransportTenantRoutingInfoService"]
    Object______p75["Object/外部框架"] -->|extends| DeviceAwareSessionContext_c75["DeviceAwareSessionContext"]
    SessionContext_p76["SessionContext"] -->|implements| DeviceAwareSessionContext_c76["DeviceAwareSessionContext"]
    Object______p77["Object/外部框架"] -->|extends| JsonUtils_c77["JsonUtils"]
    Object______p78["Object/外部框架"] -->|extends| SslUtil_c78["SslUtil"]
    Object______p79["Object/外部框架"] -->|extends| ActivityStrategyTypeTest_c79["ActivityStrategyTypeTest"]
    Object______p80["Object/外部框架"] -->|extends| AllEventsActivityStrategyTest_c80["AllEventsActivityStrategyTest"]
    Object______p81["Object/外部框架"] -->|extends| FirstAndLastEventActivityStrategyTest_c81["FirstAndLastEventActivityStrategyTest"]
    Object______p82["Object/外部框架"] -->|extends| FirstEventActivityStrategyTest_c82["FirstEventActivityStrategyTest"]
    Object______p83["Object/外部框架"] -->|extends| LastEventActivityStrategyTest_c83["LastEventActivityStrategyTest"]
    Object______p84["Object/外部框架"] -->|extends| TransportActivityManagerTest_c84["TransportActivityManagerTest"]
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

- `SessionMsgListener.onGetAttributesResponse()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onAttributeUpdate()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onRemoteSessionCloseCommand()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onToDeviceRpcRequest()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onToServerRpcResponse()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onDeviceDeleted()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onUplinkNotification()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onToTransportUpdateCredentials()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onDeviceProfileUpdate()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onDeviceUpdate()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onResourceUpdate()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onResourceDelete()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `TransportContext.ObjectMapper()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportContext.java`)
- `TransportContext.init()` (public, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportContext.java`)
- `TransportContext.stop()` (public, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportContext.java`)
- `TransportContext.getNodeId()` (public, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportContext.java`)
- `TransportDeviceProfileCache.getOrCreate()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportDeviceProfileCache.java`)
- `TransportDeviceProfileCache.get()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportDeviceProfileCache.java`)
- `TransportDeviceProfileCache.put()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportDeviceProfileCache.java`)
- `TransportDeviceProfileCache.put()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportDeviceProfileCache.java`)
- `TransportDeviceProfileCache.evict()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportDeviceProfileCache.java`)
- `TransportResourceCache.get()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportResourceCache.java`)
- `TransportResourceCache.update()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportResourceCache.java`)
- `TransportResourceCache.evict()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportResourceCache.java`)
- `TransportService.getEntityProfile()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.getQueueRoutingInfo()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.getResource()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.getSnmpDevicesIds()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.getDevice()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.getDeviceCredentials()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.onProfileUpdate()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)


## 哪些方法可以重写

- `DeviceDeletedEvent.Object()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/DeviceDeletedEvent.java`)
- `DeviceProfileUpdatedEvent.Object()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/DeviceProfileUpdatedEvent.java`)
- `SessionMsgListener.onGetAttributesResponse()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onAttributeUpdate()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onRemoteSessionCloseCommand()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onToDeviceRpcRequest()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onToServerRpcResponse()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onDeviceDeleted()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onUplinkNotification()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onToTransportUpdateCredentials()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onDeviceProfileUpdate()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onDeviceUpdate()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onResourceUpdate()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onResourceDelete()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `TransportContext.ObjectMapper()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportContext.java`)
- `TransportContext.init()` (public, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportContext.java`)
- `TransportContext.stop()` (public, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportContext.java`)
- `TransportContext.getNodeId()` (public, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportContext.java`)
- `TransportDeviceProfileCache.getOrCreate()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportDeviceProfileCache.java`)
- `TransportDeviceProfileCache.get()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportDeviceProfileCache.java`)
- `TransportDeviceProfileCache.put()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportDeviceProfileCache.java`)
- `TransportDeviceProfileCache.put()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportDeviceProfileCache.java`)
- `TransportDeviceProfileCache.evict()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportDeviceProfileCache.java`)
- `TransportResourceCache.get()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportResourceCache.java`)
- `TransportResourceCache.update()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportResourceCache.java`)
- `TransportResourceCache.evict()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportResourceCache.java`)
- `TransportService.getEntityProfile()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.getQueueRoutingInfo()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.getResource()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.getSnmpDevicesIds()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.getDevice()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.getDeviceCredentials()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.onProfileUpdate()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)


## 哪些方法必须重写

- `DeviceDeletedEvent.Object()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/DeviceDeletedEvent.java`)
- `DeviceProfileUpdatedEvent.Object()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/DeviceProfileUpdatedEvent.java`)
- `SessionMsgListener.onGetAttributesResponse()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onAttributeUpdate()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onRemoteSessionCloseCommand()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onToDeviceRpcRequest()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onToServerRpcResponse()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onDeviceDeleted()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onUplinkNotification()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onToTransportUpdateCredentials()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onDeviceProfileUpdate()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onDeviceUpdate()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onResourceUpdate()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `SessionMsgListener.onResourceDelete()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/SessionMsgListener.java`)
- `TransportContext.ObjectMapper()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportContext.java`)
- `TransportDeviceProfileCache.getOrCreate()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportDeviceProfileCache.java`)
- `TransportDeviceProfileCache.get()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportDeviceProfileCache.java`)
- `TransportDeviceProfileCache.put()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportDeviceProfileCache.java`)
- `TransportDeviceProfileCache.put()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportDeviceProfileCache.java`)
- `TransportDeviceProfileCache.evict()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportDeviceProfileCache.java`)
- `TransportResourceCache.get()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportResourceCache.java`)
- `TransportResourceCache.update()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportResourceCache.java`)
- `TransportResourceCache.evict()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportResourceCache.java`)
- `TransportService.getEntityProfile()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.getQueueRoutingInfo()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.getResource()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.getSnmpDevicesIds()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.getDevice()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.getDeviceCredentials()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.onProfileUpdate()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)
- `TransportService.process()` (package, `common/transport/transport-api/src/main/java/org/thingsboard/server/common/transport/TransportService.java`)


## 哪些地方使用了多态

- `TbApplicationEvent` -> `DeviceDeletedEvent` (extends)
- `TbApplicationEvent` -> `DeviceProfileUpdatedEvent` (extends)
- `Object/外部框架` -> `DeviceUpdatedEvent` (extends)
- `Object/外部框架` -> `TransportContext` (extends)
- `Object/外部框架` -> `AbstractActivityManager` (extends)
- `Object/外部框架` -> `ActivityStateWrapper` (extends)
- `Object/外部框架` -> `ActivityState` (extends)
- `Object/外部框架` -> `AllEventsActivityStrategy` (extends)
- `ActivityStrategy` -> `AllEventsActivityStrategy` (implements)
- `Object/外部框架` -> `FirstAndLastEventActivityStrategy` (extends)
- `ActivityStrategy` -> `FirstAndLastEventActivityStrategy` (implements)
- `Object/外部框架` -> `FirstEventActivityStrategy` (extends)
- `ActivityStrategy` -> `FirstEventActivityStrategy` (implements)
- `Object/外部框架` -> `LastEventActivityStrategy` (extends)
- `ActivityStrategy` -> `LastEventActivityStrategy` (implements)
- `Object/外部框架` -> `DeviceAuthResult` (extends)
- `Object/外部框架` -> `GetOrCreateDeviceFromGatewayResponse` (extends)
- `DeviceProfileAware` -> `GetOrCreateDeviceFromGatewayResponse` (implements)
- `Object/外部框架` -> `SessionInfoCreator` (extends)
- `Object/外部框架` -> `TransportDeviceInfo` (extends)
- `Serializable` -> `TransportDeviceInfo` (implements)
- `Object/外部框架` -> `ValidateDeviceCredentialsResponse` (extends)
- `DeviceProfileAware` -> `ValidateDeviceCredentialsResponse` (implements)
- `Serializable` -> `ValidateDeviceCredentialsResponse` (implements)
- `Object/外部框架` -> `AbstractSslCredentials` (extends)
- `SslCredentials` -> `AbstractSslCredentials` (implements)
- `AbstractSslCredentials` -> `KeystoreSslCredentials` (extends)
- `AbstractSslCredentials` -> `PemSslCredentials` (extends)
- `Object/外部框架` -> `SslCredentialsConfig` (extends)
- `Object/外部框架` -> `SslCredentialsWebServerCustomizer` (extends)
- `WebServerFactoryCustomizer` -> `SslCredentialsWebServerCustomizer` (implements)
- `Object/外部框架` -> `DefaultEntityLimitsCache` (extends)
- `EntityLimitsCache` -> `DefaultEntityLimitsCache` (implements)
- `Object/外部框架` -> `DefaultTransportRateLimitService` (extends)
- `TransportRateLimitService` -> `DefaultTransportRateLimitService` (implements)
- `Object/外部框架` -> `DummyTransportRateLimit` (extends)
- `TransportRateLimit` -> `DummyTransportRateLimit` (implements)
- `Object/外部框架` -> `EntityLimitKey` (extends)
- `Object/外部框架` -> `EntityTransportRateLimits` (extends)
- `Object/外部框架` -> `InetAddressRateLimitStats` (extends)
- 其余 40 项已省略；完整源码证据可通过本模块 Java 文件继续追踪。


## 外部父类/接口

- `Object/外部框架`
- `QueueRoutingInfoService`
- `Serializable`
- `TbApplicationEvent`
- `TbKafkaDecoder`
- `TbKafkaEncoder`
- `TbQueueCallback`
- `TenantRoutingInfoService`
- `WebServerFactoryCustomizer`


## 整个继承体系解决了什么问题

该继承体系把“稳定生命周期/契约”和“模块特定行为”分开：父类或接口负责统一入口、共享流程和多态调度，子类负责差异化协议、实体、规则、DAO、测试或工具行为。这样可以让上游模块通过父类型调用下游实现，同时保持每个实现只关注自己的变化点。
