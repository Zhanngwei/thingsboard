# Thingsboard LwM2M Transport Common 模块继承体系分析

> 生成范围：`common/transport/lwm2m`  
> Maven artifact：`lwm2m`  
> Java 类型数量：170  
> 分析方式：静态扫描 `class/interface/enum/record` 的 `extends`、`implements`、方法签名和源码触点。

## 完整继承树

```text
Authorizer
├── «implements» TbLwM2MAuthorizer
BootstrapSecurityStore
├── «implements» LwM2MBootstrapSecurityStore
BootstrapTaskProvider
├── LwM2MBootstrapTaskProvider
├── ├── «implements» LwM2MBootstrapConfigStoreTaskProvider
CaliforniumRegistrationStore
├── «implements» TbLwM2mRedisRegistrationStore
Cloneable
├── «implements» ModelObject
ConfigurationChecker
├── LwM2MConfigurationChecker
DefaultBootstrapSessionManager
├── LwM2mDefaultBootstrapSessionManager
Destroyable
├── «implements» TbLwM2mRedisRegistrationStore
Exception
├── LwM2MClientStateException
GenericFutureListener
├── «implements» LwM2mSessionMsgListener
HasContentFormat
├── «implements» TbLwM2MObserveRequest
├── «implements» TbLwM2MReadCompositeRequest
├── «implements» TbLwM2MReadRequest
InMemoryBootstrapConfigStore
├── LwM2MInMemoryBootstrapConfigStore
LwM2MAttributesService
├── «implements» DefaultLwM2MAttributesService
LwM2MModelConfigService
├── «implements» LwM2MModelConfigServiceImpl
LwM2MOtaUpdateService
├── «implements» DefaultLwM2MOtaUpdateService
LwM2MRpcRequestHandler
├── «implements» DefaultLwM2MRpcRequestHandler
LwM2MSecureServerConfig
├── «implements» LwM2MTransportBootstrapConfig
├── «implements» LwM2MTransportServerConfig
LwM2MSessionManager
├── «implements» DefaultLwM2MSessionManager
LwM2MTelemetryLogService
├── «implements» DefaultLwM2MTelemetryLogService
LwM2MTransportAdaptor
├── «implements» LwM2MJsonAdaptor
LwM2mClientContext
├── «implements» LwM2mClientContextImpl
LwM2mCoapResource
├── AbstractLwM2mTransportResource
├── ├── LwM2mTransportCoapResource
LwM2mDownlinkMsgHandler
├── «implements» DefaultLwM2mDownlinkMsgHandler
LwM2mModel
├── «implements» DynamicModel
LwM2mModelProvider
├── «implements» LwM2mVersionedModelProvider
LwM2mUplinkMsgHandler
├── «implements» DefaultLwM2mUplinkMsgHandler
LwM2mValueConverter
├── «implements» LwM2mValueConverterImpl
NewAdvancedCertificateVerifier
├── «implements» TbLwM2MDtlsBootstrapCertificateVerifier
├── «implements» TbLwM2MDtlsCertificateVerifier
Object/外部框架
├── AbstractTbLwM2MRequestCallback
├── ├── TbLwM2MCancelAllObserveCallback
├── ├── TbLwM2MCancelObserveCallback
├── AbstractTbLwM2MTargetedDownlinkCompositeRequest
├── ├── TbLwM2MReadCompositeRequest
├── AbstractTbLwM2MTargetedDownlinkRequest
├── ├── TbLwM2MCancelObserveRequest
├── ├── TbLwM2MCreateRequest
├── ├── TbLwM2MDeleteRequest
├── ├── TbLwM2MDiscoverRequest
├── ├── TbLwM2MExecuteRequest
├── ├── TbLwM2MObserveRequest
├── ├── TbLwM2MReadRequest
├── ├── TbLwM2MWriteAttributesRequest
├── ├── TbLwM2MWriteCompositeRequest
├── ├── TbLwM2MWriteReplaceRequest
├── ├── TbLwM2MWriteUpdateRequest
├── Cleaner
├── CoapResourceObserver
├── DefaultLwM2MAttributesService
├── DefaultLwM2MRpcRequestHandler
├── DefaultLwM2MSessionManager
├── DefaultLwM2MTelemetryLogService
├── DefaultLwM2mTransportService
├── DefaultLwM2mTransportServiceTest
├── DynamicModel
├── LwM2MBootstrapClientInstanceIds
├── LwM2MBootstrapConfig
├── LwM2MBootstrapConfigStoreTaskProvider
├── LwM2MBootstrapSecurityStore
├── LwM2MBootstrapServers
├── LwM2MClientCredentials
├── LwM2MClientOtaInfo
├── ├── LwM2MClientFwOtaInfo
├── ├── LwM2MClientSwOtaInfo
├── LwM2MClientSerDes
├── LwM2MClientSerDesTest
├── LwM2MExecutorAwareService
├── ├── DefaultLwM2MOtaUpdateService
├── ├── DefaultLwM2mDownlinkMsgHandler
├── ├── DefaultLwM2mUplinkMsgHandler
├── LwM2MIdentitySerDes
├── LwM2MIdentitySerDesTest
├── LwM2MJsonAdaptor
├── LwM2MModelConfig
├── LwM2MModelConfigServiceImpl
├── LwM2MModelConfigServiceImplTest
├── LwM2MNetworkConfig
├── LwM2MRpcRequestHeader
├── ├── RpcCreateRequest
├── ├── RpcWriteAttributesRequest
├── ├── RpcWriteReplaceRequest
├── ├── RpcWriteUpdateRequest
├── LwM2MRpcResponseBody
├── LwM2MServerBootstrap
├── LwM2MTransportBootstrapConfig
├── LwM2MTransportBootstrapService
├── LwM2MTransportBootstrapServiceTest
├── LwM2MTransportServerConfig
├── LwM2MTransportServerConfigTest
├── LwM2MTransportUtil
├── LwM2mClient
├── LwM2mClientContextImpl
├── LwM2mClientTest
├── LwM2mCredentialsSecurityInfoValidator
├── LwM2mOtaConvert
├── LwM2mRPkCredentials
├── LwM2mServerListener
├── LwM2mSessionMsgListener
├── LwM2mTransportServerHelper
├── LwM2mValueConverterImpl
├── LwM2mVersionedModelProvider
├── ModelObject
├── ParametersAnalyzeResult
├── ResourceValue
├── ResultsAddKeyValueProto
├── RpcCreateResponseCallback
├── RpcDownlinkRequestCallbackProxy
├── ├── RpcCancelAllObserveCallback
├── ├── RpcCancelObserveCallback
├── RpcEmptyResponseCallback
├── RpcLinkSetCallback
├── RpcLwM2MDownlinkCallback
├── ├── RpcDiscoverCallback
├── RpcReadCompositeRequest
├── RpcReadResponseCallback
├── RpcReadResponseCompositeCallback
├── RpcWriteCompositeRequest
├── TbDummyLwM2MClientOtaInfoStore
├── TbDummyLwM2MClientStore
├── TbDummyLwM2MModelConfigStore
├── TbInMemorySecurityStore
├── TbL2M2MDtlsSessionInMemoryStore
├── TbLwM2MAuthorizer
├── TbLwM2MCancelAllRequest
├── TbLwM2MDiscoverAllRequest
├── TbLwM2MDtlsBootstrapCertificateVerifier
├── TbLwM2MDtlsCertificateVerifier
├── TbLwM2MDtlsSessionRedisStore
├── TbLwM2MLatchCallback
├── TbLwM2MObserveAllRequest
├── TbLwM2MSecurityInfo
├── TbLwM2MTargetedCallback
├── ├── TbLwM2MDeleteCallback
├── ├── TbLwM2MDiscoverCallback
├── ├── TbLwM2MExecuteCallback
├── ├── TbLwM2MWriteAttributesCallback
├── TbLwM2MUplinkTargetedCallback
├── ├── TbLwM2MCreateResponseCallback
├── ├── TbLwM2MObserveCallback
├── ├── TbLwM2MReadCallback
├── ├── TbLwM2MReadCompositeCallback
├── ├── TbLwM2MWriteResponseCallback
├── ├── TbLwM2MWriteResponseCompositeCallback
├── TbLwM2mRedisClientOtaInfoStore
├── TbLwM2mRedisRegistrationStore
├── TbLwM2mRedisRegistrationStoreTest
├── TbLwM2mRedisSecurityStore
├── TbLwM2mSecurityStore
├── TbLwM2mStoreFactory
├── TbRedisLwM2MClientStore
├── TbRedisLwM2MModelConfigStore
├── TbX509DtlsSessionInfo
ResourceObserver
├── «implements» CoapResourceObserver
Runnable
├── «implements» Cleaner
RuntimeException
├── LwM2MAuthException
SecurityStore
├── TbSecurityStore
├── ├── TbEditableSecurityStore
├── ├── ├── «implements» TbInMemorySecurityStore
├── ├── ├── «implements» TbLwM2mRedisSecurityStore
├── ├── TbMainSecurityStore
├── ├── ├── «implements» TbLwM2mSecurityStore
Serializable
├── «implements» LwM2MBootstrapConfig
├── «implements» TbLwM2MSecurityInfo
├── «implements» TbX509DtlsSessionInfo
SessionMsgListener
├── «implements» LwM2mSessionMsgListener
Startable
├── «implements» TbLwM2mRedisRegistrationStore
Stoppable
├── «implements» TbLwM2mRedisRegistrationStore
TbLwM2MClientOtaInfoStore
├── «implements» TbDummyLwM2MClientOtaInfoStore
├── «implements» TbLwM2mRedisClientOtaInfoStore
TbLwM2MClientStore
├── «implements» TbDummyLwM2MClientStore
├── «implements» TbRedisLwM2MClientStore
TbLwM2MDownlinkRequest
├── «implements» TbLwM2MCancelAllRequest
├── «implements» TbLwM2MDiscoverAllRequest
├── «implements» TbLwM2MObserveAllRequest
TbLwM2MDtlsSessionStore
├── «implements» TbL2M2MDtlsSessionInMemoryStore
├── «implements» TbLwM2MDtlsSessionRedisStore
TbLwM2MModelConfigStore
├── «implements» TbDummyLwM2MModelConfigStore
├── «implements» TbRedisLwM2MModelConfigStore
TbTransportService
├── LwM2MTransportService
├── ├── «implements» DefaultLwM2mTransportService
TransportContext
├── LwM2mTransportContext
```

## 继承图

```mermaid
flowchart TD
    Object______p0["Object/外部框架"] -->|extends| LwM2MTransportBootstrapService_c0["LwM2MTransportBootstrapService"]
    Object______p1["Object/外部框架"] -->|extends| LwM2MBootstrapConfig_c1["LwM2MBootstrapConfig"]
    Serializable_p2["Serializable"] -->|implements| LwM2MBootstrapConfig_c2["LwM2MBootstrapConfig"]
    Object______p3["Object/外部框架"] -->|extends| LwM2MBootstrapServers_c3["LwM2MBootstrapServers"]
    Object______p4["Object/外部框架"] -->|extends| LwM2MServerBootstrap_c4["LwM2MServerBootstrap"]
    DefaultBootstrapSessionManager_p5["DefaultBootstrapSessionManager"] -->|extends| LwM2mDefaultBootstrapSessionManager_c5["LwM2mDefaultBootstrapSessionManager"]
    Object______p6["Object/外部框架"] -->|extends| TbLwM2MDtlsBootstrapCertificateVerifier_c6["TbLwM2MDtlsBootstrapCertificateVerifier"]
    NewAdvancedCertificateVerifier_p7["NewAdvancedCertificateVerifier"] -->|implements| TbLwM2MDtlsBootstrapCertificateVerifier_c7["TbLwM2MDtlsBootstrapCertificateVerifier"]
    Object______p8["Object/外部框架"] -->|extends| LwM2MBootstrapClientInstanceIds_c8["LwM2MBootstrapClientInstanceIds"]
    Object______p9["Object/外部框架"] -->|extends| LwM2MBootstrapConfigStoreTaskProvider_c9["LwM2MBootstrapConfigStoreTaskProvider"]
    LwM2MBootstrapTaskProvider_p10["LwM2MBootstrapTaskProvider"] -->|implements| LwM2MBootstrapConfigStoreTaskProvider_c10["LwM2MBootstrapConfigStoreTaskProvider"]
    Object______p11["Object/外部框架"] -->|extends| LwM2MBootstrapSecurityStore_c11["LwM2MBootstrapSecurityStore"]
    BootstrapSecurityStore_p12["BootstrapSecurityStore"] -->|implements| LwM2MBootstrapSecurityStore_c12["LwM2MBootstrapSecurityStore"]
    BootstrapTaskProvider_p13["BootstrapTaskProvider"] -->|extends| LwM2MBootstrapTaskProvider_c13["LwM2MBootstrapTaskProvider"]
    ConfigurationChecker_p14["ConfigurationChecker"] -->|extends| LwM2MConfigurationChecker_c14["LwM2MConfigurationChecker"]
    InMemoryBootstrapConfigStore_p15["InMemoryBootstrapConfigStore"] -->|extends| LwM2MInMemoryBootstrapConfigStore_c15["LwM2MInMemoryBootstrapConfigStore"]
    Object______p16["Object/外部框架"] -->|extends| LwM2MTransportBootstrapConfig_c16["LwM2MTransportBootstrapConfig"]
    LwM2MSecureServerConfig_p17["LwM2MSecureServerConfig"] -->|implements| LwM2MTransportBootstrapConfig_c17["LwM2MTransportBootstrapConfig"]
    Object______p18["Object/外部框架"] -->|extends| LwM2MTransportServerConfig_c18["LwM2MTransportServerConfig"]
    LwM2MSecureServerConfig_p19["LwM2MSecureServerConfig"] -->|implements| LwM2MTransportServerConfig_c19["LwM2MTransportServerConfig"]
    Object______p20["Object/外部框架"] -->|extends| LwM2mCredentialsSecurityInfoValidator_c20["LwM2mCredentialsSecurityInfoValidator"]
    Object______p21["Object/外部框架"] -->|extends| LwM2mRPkCredentials_c21["LwM2mRPkCredentials"]
    Object______p22["Object/外部框架"] -->|extends| TbLwM2MAuthorizer_c22["TbLwM2MAuthorizer"]
    Authorizer_p23["Authorizer"] -->|implements| TbLwM2MAuthorizer_c23["TbLwM2MAuthorizer"]
    Object______p24["Object/外部框架"] -->|extends| TbLwM2MDtlsCertificateVerifier_c24["TbLwM2MDtlsCertificateVerifier"]
    NewAdvancedCertificateVerifier_p25["NewAdvancedCertificateVerifier"] -->|implements| TbLwM2MDtlsCertificateVerifier_c25["TbLwM2MDtlsCertificateVerifier"]
    Object______p26["Object/外部框架"] -->|extends| TbLwM2MSecurityInfo_c26["TbLwM2MSecurityInfo"]
    Serializable_p27["Serializable"] -->|implements| TbLwM2MSecurityInfo_c27["TbLwM2MSecurityInfo"]
    Object______p28["Object/外部框架"] -->|extends| TbX509DtlsSessionInfo_c28["TbX509DtlsSessionInfo"]
    Serializable_p29["Serializable"] -->|implements| TbX509DtlsSessionInfo_c29["TbX509DtlsSessionInfo"]
    Object______p30["Object/外部框架"] -->|extends| LwM2MClientCredentials_c30["LwM2MClientCredentials"]
    LwM2mCoapResource_p31["LwM2mCoapResource"] -->|extends| AbstractLwM2mTransportResource_c31["AbstractLwM2mTransportResource"]
    Object______p32["Object/外部框架"] -->|extends| DefaultLwM2mTransportService_c32["DefaultLwM2mTransportService"]
    LwM2MTransportService_p33["LwM2MTransportService"] -->|implements| DefaultLwM2mTransportService_c33["DefaultLwM2mTransportService"]
    Object______p34["Object/外部框架"] -->|extends| LwM2MNetworkConfig_c34["LwM2MNetworkConfig"]
    TbTransportService_p35["TbTransportService"] -->|extends| LwM2MTransportService_c35["LwM2MTransportService"]
    Object______p36["Object/外部框架"] -->|extends| LwM2mOtaConvert_c36["LwM2mOtaConvert"]
    Object______p37["Object/外部框架"] -->|extends| LwM2mServerListener_c37["LwM2mServerListener"]
    Object______p38["Object/外部框架"] -->|extends| LwM2mSessionMsgListener_c38["LwM2mSessionMsgListener"]
    GenericFutureListener_p39["GenericFutureListener"] -->|implements| LwM2mSessionMsgListener_c39["LwM2mSessionMsgListener"]
    SessionMsgListener_p40["SessionMsgListener"] -->|implements| LwM2mSessionMsgListener_c40["LwM2mSessionMsgListener"]
    AbstractLwM2mTransportResource_p41["AbstractLwM2mTransportResource"] -->|extends| LwM2mTransportCoapResource_c41["LwM2mTransportCoapResource"]
    Object______p42["Object/外部框架"] -->|extends| CoapResourceObserver_c42["CoapResourceObserver"]
    ResourceObserver_p43["ResourceObserver"] -->|implements| CoapResourceObserver_c43["CoapResourceObserver"]
    TransportContext_p44["TransportContext"] -->|extends| LwM2mTransportContext_c44["LwM2mTransportContext"]
    Object______p45["Object/外部框架"] -->|extends| LwM2mTransportServerHelper_c45["LwM2mTransportServerHelper"]
    Object______p46["Object/外部框架"] -->|extends| LwM2mVersionedModelProvider_c46["LwM2mVersionedModelProvider"]
    LwM2mModelProvider_p47["LwM2mModelProvider"] -->|implements| LwM2mVersionedModelProvider_c47["LwM2mVersionedModelProvider"]
    Object______p48["Object/外部框架"] -->|extends| DynamicModel_c48["DynamicModel"]
    LwM2mModel_p49["LwM2mModel"] -->|implements| DynamicModel_c49["DynamicModel"]
    Object______p50["Object/外部框架"] -->|extends| LwM2MJsonAdaptor_c50["LwM2MJsonAdaptor"]
    LwM2MTransportAdaptor_p51["LwM2MTransportAdaptor"] -->|implements| LwM2MJsonAdaptor_c51["LwM2MJsonAdaptor"]
    Object______p52["Object/外部框架"] -->|extends| DefaultLwM2MAttributesService_c52["DefaultLwM2MAttributesService"]
    LwM2MAttributesService_p53["LwM2MAttributesService"] -->|implements| DefaultLwM2MAttributesService_c53["DefaultLwM2MAttributesService"]
    RuntimeException_p54["RuntimeException"] -->|extends| LwM2MAuthException_c54["LwM2MAuthException"]
    Exception_p55["Exception"] -->|extends| LwM2MClientStateException_c55["LwM2MClientStateException"]
    Object______p56["Object/外部框架"] -->|extends| LwM2mClient_c56["LwM2mClient"]
    Object______p57["Object/外部框架"] -->|extends| LwM2mClientContextImpl_c57["LwM2mClientContextImpl"]
    LwM2mClientContext_p58["LwM2mClientContext"] -->|implements| LwM2mClientContextImpl_c58["LwM2mClientContextImpl"]
    Object______p59["Object/外部框架"] -->|extends| ModelObject_c59["ModelObject"]
    Cloneable_p60["Cloneable"] -->|implements| ModelObject_c60["ModelObject"]
    Object______p61["Object/外部框架"] -->|extends| ParametersAnalyzeResult_c61["ParametersAnalyzeResult"]
    Object______p62["Object/外部框架"] -->|extends| ResourceValue_c62["ResourceValue"]
    Object______p63["Object/外部框架"] -->|extends| ResultsAddKeyValueProto_c63["ResultsAddKeyValueProto"]
    Object______p64["Object/外部框架"] -->|extends| LwM2MExecutorAwareService_c64["LwM2MExecutorAwareService"]
    Object______p65["Object/外部框架"] -->|extends| AbstractTbLwM2MRequestCallback_c65["AbstractTbLwM2MRequestCallback"]
    Object______p66["Object/外部框架"] -->|extends| AbstractTbLwM2MTargetedDownlinkRequest_c66["AbstractTbLwM2MTargetedDownlinkRequest"]
    LwM2MExecutorAwareService_p67["LwM2MExecutorAwareService"] -->|extends| DefaultLwM2mDownlinkMsgHandler_c67["DefaultLwM2mDownlinkMsgHandler"]
    LwM2mDownlinkMsgHandler_p68["LwM2mDownlinkMsgHandler"] -->|implements| DefaultLwM2mDownlinkMsgHandler_c68["DefaultLwM2mDownlinkMsgHandler"]
    AbstractTbLwM2MRequestCallback_p69["AbstractTbLwM2MRequestCallback"] -->|extends| TbLwM2MCancelAllObserveCallback_c69["TbLwM2MCancelAllObserveCallback"]
    Object______p70["Object/外部框架"] -->|extends| TbLwM2MCancelAllRequest_c70["TbLwM2MCancelAllRequest"]
    TbLwM2MDownlinkRequest_p71["TbLwM2MDownlinkRequest"] -->|implements| TbLwM2MCancelAllRequest_c71["TbLwM2MCancelAllRequest"]
    AbstractTbLwM2MRequestCallback_p72["AbstractTbLwM2MRequestCallback"] -->|extends| TbLwM2MCancelObserveCallback_c72["TbLwM2MCancelObserveCallback"]
    AbstractTbLwM2MTargetedDownlinkRequest_p73["AbstractTbLwM2MTargetedDownlinkRequest"] -->|extends| TbLwM2MCancelObserveRequest_c73["TbLwM2MCancelObserveRequest"]
    AbstractTbLwM2MTargetedDownlinkRequest_p74["AbstractTbLwM2MTargetedDownlinkRequest"] -->|extends| TbLwM2MCreateRequest_c74["TbLwM2MCreateRequest"]
    TbLwM2MUplinkTargetedCallback_p75["TbLwM2MUplinkTargetedCallback"] -->|extends| TbLwM2MCreateResponseCallback_c75["TbLwM2MCreateResponseCallback"]
    TbLwM2MTargetedCallback_p76["TbLwM2MTargetedCallback"] -->|extends| TbLwM2MDeleteCallback_c76["TbLwM2MDeleteCallback"]
    AbstractTbLwM2MTargetedDownlinkRequest_p77["AbstractTbLwM2MTargetedDownlinkRequest"] -->|extends| TbLwM2MDeleteRequest_c77["TbLwM2MDeleteRequest"]
    Object______p78["Object/外部框架"] -->|extends| TbLwM2MDiscoverAllRequest_c78["TbLwM2MDiscoverAllRequest"]
    TbLwM2MDownlinkRequest_p79["TbLwM2MDownlinkRequest"] -->|implements| TbLwM2MDiscoverAllRequest_c79["TbLwM2MDiscoverAllRequest"]
    TbLwM2MTargetedCallback_p80["TbLwM2MTargetedCallback"] -->|extends| TbLwM2MDiscoverCallback_c80["TbLwM2MDiscoverCallback"]
    AbstractTbLwM2MTargetedDownlinkRequest_p81["AbstractTbLwM2MTargetedDownlinkRequest"] -->|extends| TbLwM2MDiscoverRequest_c81["TbLwM2MDiscoverRequest"]
    TbLwM2MTargetedCallback_p82["TbLwM2MTargetedCallback"] -->|extends| TbLwM2MExecuteCallback_c82["TbLwM2MExecuteCallback"]
    AbstractTbLwM2MTargetedDownlinkRequest_p83["AbstractTbLwM2MTargetedDownlinkRequest"] -->|extends| TbLwM2MExecuteRequest_c83["TbLwM2MExecuteRequest"]
    Object______p84["Object/外部框架"] -->|extends| TbLwM2MLatchCallback_c84["TbLwM2MLatchCallback"]
    Object______p85["Object/外部框架"] -->|extends| TbLwM2MObserveAllRequest_c85["TbLwM2MObserveAllRequest"]
    TbLwM2MDownlinkRequest_p86["TbLwM2MDownlinkRequest"] -->|implements| TbLwM2MObserveAllRequest_c86["TbLwM2MObserveAllRequest"]
    TbLwM2MUplinkTargetedCallback_p87["TbLwM2MUplinkTargetedCallback"] -->|extends| TbLwM2MObserveCallback_c87["TbLwM2MObserveCallback"]
    AbstractTbLwM2MTargetedDownlinkRequest_p88["AbstractTbLwM2MTargetedDownlinkRequest"] -->|extends| TbLwM2MObserveRequest_c88["TbLwM2MObserveRequest"]
    HasContentFormat_p89["HasContentFormat"] -->|implements| TbLwM2MObserveRequest_c89["TbLwM2MObserveRequest"]
    TbLwM2MUplinkTargetedCallback_p90["TbLwM2MUplinkTargetedCallback"] -->|extends| TbLwM2MReadCallback_c90["TbLwM2MReadCallback"]
    AbstractTbLwM2MTargetedDownlinkRequest_p91["AbstractTbLwM2MTargetedDownlinkRequest"] -->|extends| TbLwM2MReadRequest_c91["TbLwM2MReadRequest"]
    HasContentFormat_p92["HasContentFormat"] -->|implements| TbLwM2MReadRequest_c92["TbLwM2MReadRequest"]
    Object______p93["Object/外部框架"] -->|extends| TbLwM2MTargetedCallback_c93["TbLwM2MTargetedCallback"]
    Object______p94["Object/外部框架"] -->|extends| TbLwM2MUplinkTargetedCallback_c94["TbLwM2MUplinkTargetedCallback"]
    TbLwM2MTargetedCallback_p95["TbLwM2MTargetedCallback"] -->|extends| TbLwM2MWriteAttributesCallback_c95["TbLwM2MWriteAttributesCallback"]
    AbstractTbLwM2MTargetedDownlinkRequest_p96["AbstractTbLwM2MTargetedDownlinkRequest"] -->|extends| TbLwM2MWriteAttributesRequest_c96["TbLwM2MWriteAttributesRequest"]
    AbstractTbLwM2MTargetedDownlinkRequest_p97["AbstractTbLwM2MTargetedDownlinkRequest"] -->|extends| TbLwM2MWriteReplaceRequest_c97["TbLwM2MWriteReplaceRequest"]
    TbLwM2MUplinkTargetedCallback_p98["TbLwM2MUplinkTargetedCallback"] -->|extends| TbLwM2MWriteResponseCallback_c98["TbLwM2MWriteResponseCallback"]
    AbstractTbLwM2MTargetedDownlinkRequest_p99["AbstractTbLwM2MTargetedDownlinkRequest"] -->|extends| TbLwM2MWriteUpdateRequest_c99["TbLwM2MWriteUpdateRequest"]
    Object______p100["Object/外部框架"] -->|extends| AbstractTbLwM2MTargetedDownlinkCompositeRequest_c100["AbstractTbLwM2MTargetedDownlinkCompositeRequest"]
    TbLwM2MUplinkTargetedCallback_p101["TbLwM2MUplinkTargetedCallback"] -->|extends| TbLwM2MReadCompositeCallback_c101["TbLwM2MReadCompositeCallback"]
    AbstractTbLwM2MTargetedDownlinkCompositeRequest_p102["AbstractTbLwM2MTargetedDownlinkCompositeRequest"] -->|extends| TbLwM2MReadCompositeRequest_c102["TbLwM2MReadCompositeRequest"]
    HasContentFormat_p103["HasContentFormat"] -->|implements| TbLwM2MReadCompositeRequest_c103["TbLwM2MReadCompositeRequest"]
    AbstractTbLwM2MTargetedDownlinkRequest_p104["AbstractTbLwM2MTargetedDownlinkRequest"] -->|extends| TbLwM2MWriteCompositeRequest_c104["TbLwM2MWriteCompositeRequest"]
    TbLwM2MUplinkTargetedCallback_p105["TbLwM2MUplinkTargetedCallback"] -->|extends| TbLwM2MWriteResponseCompositeCallback_c105["TbLwM2MWriteResponseCompositeCallback"]
    Object______p106["Object/外部框架"] -->|extends| DefaultLwM2MTelemetryLogService_c106["DefaultLwM2MTelemetryLogService"]
    LwM2MTelemetryLogService_p107["LwM2MTelemetryLogService"] -->|implements| DefaultLwM2MTelemetryLogService_c107["DefaultLwM2MTelemetryLogService"]
    Object______p108["Object/外部框架"] -->|extends| LwM2MModelConfig_c108["LwM2MModelConfig"]
    Object______p109["Object/外部框架"] -->|extends| LwM2MModelConfigServiceImpl_c109["LwM2MModelConfigServiceImpl"]
    LwM2MModelConfigService_p110["LwM2MModelConfigService"] -->|implements| LwM2MModelConfigServiceImpl_c110["LwM2MModelConfigServiceImpl"]
    LwM2MExecutorAwareService_p111["LwM2MExecutorAwareService"] -->|extends| DefaultLwM2MOtaUpdateService_c111["DefaultLwM2MOtaUpdateService"]
    LwM2MOtaUpdateService_p112["LwM2MOtaUpdateService"] -->|implements| DefaultLwM2MOtaUpdateService_c112["DefaultLwM2MOtaUpdateService"]
    Object______p113["Object/外部框架"] -->|extends| LwM2MClientOtaInfo_c113["LwM2MClientOtaInfo"]
    LwM2MClientOtaInfo_p114["LwM2MClientOtaInfo"] -->|extends| LwM2MClientFwOtaInfo_c114["LwM2MClientFwOtaInfo"]
    LwM2MClientOtaInfo_p115["LwM2MClientOtaInfo"] -->|extends| LwM2MClientSwOtaInfo_c115["LwM2MClientSwOtaInfo"]
    Object______p116["Object/外部框架"] -->|extends| DefaultLwM2MRpcRequestHandler_c116["DefaultLwM2MRpcRequestHandler"]
    LwM2MRpcRequestHandler_p117["LwM2MRpcRequestHandler"] -->|implements| DefaultLwM2MRpcRequestHandler_c117["DefaultLwM2MRpcRequestHandler"]
    Object______p118["Object/外部框架"] -->|extends| LwM2MRpcRequestHeader_c118["LwM2MRpcRequestHeader"]
    Object______p119["Object/外部框架"] -->|extends| LwM2MRpcResponseBody_c119["LwM2MRpcResponseBody"]
    RpcDownlinkRequestCallbackProxy_p120["RpcDownlinkRequestCallbackProxy"] -->|extends| RpcCancelAllObserveCallback_c120["RpcCancelAllObserveCallback"]
    RpcDownlinkRequestCallbackProxy_p121["RpcDownlinkRequestCallbackProxy"] -->|extends| RpcCancelObserveCallback_c121["RpcCancelObserveCallback"]
    LwM2MRpcRequestHeader_p122["LwM2MRpcRequestHeader"] -->|extends| RpcCreateRequest_c122["RpcCreateRequest"]
    Object______p123["Object/外部框架"] -->|extends| RpcCreateResponseCallback_c123["RpcCreateResponseCallback"]
    RpcLwM2MDownlinkCallback_p124["RpcLwM2MDownlinkCallback"] -->|extends| RpcDiscoverCallback_c124["RpcDiscoverCallback"]
    Object______p125["Object/外部框架"] -->|extends| RpcDownlinkRequestCallbackProxy_c125["RpcDownlinkRequestCallbackProxy"]
    Object______p126["Object/外部框架"] -->|extends| RpcEmptyResponseCallback_c126["RpcEmptyResponseCallback"]
    Object______p127["Object/外部框架"] -->|extends| RpcLinkSetCallback_c127["RpcLinkSetCallback"]
    Object______p128["Object/外部框架"] -->|extends| RpcLwM2MDownlinkCallback_c128["RpcLwM2MDownlinkCallback"]
    Object______p129["Object/外部框架"] -->|extends| RpcReadResponseCallback_c129["RpcReadResponseCallback"]
    LwM2MRpcRequestHeader_p130["LwM2MRpcRequestHeader"] -->|extends| RpcWriteAttributesRequest_c130["RpcWriteAttributesRequest"]
    LwM2MRpcRequestHeader_p131["LwM2MRpcRequestHeader"] -->|extends| RpcWriteReplaceRequest_c131["RpcWriteReplaceRequest"]
    LwM2MRpcRequestHeader_p132["LwM2MRpcRequestHeader"] -->|extends| RpcWriteUpdateRequest_c132["RpcWriteUpdateRequest"]
    Object______p133["Object/外部框架"] -->|extends| RpcReadCompositeRequest_c133["RpcReadCompositeRequest"]
    Object______p134["Object/外部框架"] -->|extends| RpcReadResponseCompositeCallback_c134["RpcReadResponseCompositeCallback"]
    Object______p135["Object/外部框架"] -->|extends| RpcWriteCompositeRequest_c135["RpcWriteCompositeRequest"]
    Object______p136["Object/外部框架"] -->|extends| DefaultLwM2MSessionManager_c136["DefaultLwM2MSessionManager"]
    LwM2MSessionManager_p137["LwM2MSessionManager"] -->|implements| DefaultLwM2MSessionManager_c137["DefaultLwM2MSessionManager"]
    Object______p138["Object/外部框架"] -->|extends| TbDummyLwM2MClientOtaInfoStore_c138["TbDummyLwM2MClientOtaInfoStore"]
    TbLwM2MClientOtaInfoStore_p139["TbLwM2MClientOtaInfoStore"] -->|implements| TbDummyLwM2MClientOtaInfoStore_c139["TbDummyLwM2MClientOtaInfoStore"]
    Object______p140["Object/外部框架"] -->|extends| TbDummyLwM2MClientStore_c140["TbDummyLwM2MClientStore"]
    TbLwM2MClientStore_p141["TbLwM2MClientStore"] -->|implements| TbDummyLwM2MClientStore_c141["TbDummyLwM2MClientStore"]
    Object______p142["Object/外部框架"] -->|extends| TbDummyLwM2MModelConfigStore_c142["TbDummyLwM2MModelConfigStore"]
    TbLwM2MModelConfigStore_p143["TbLwM2MModelConfigStore"] -->|implements| TbDummyLwM2MModelConfigStore_c143["TbDummyLwM2MModelConfigStore"]
    TbSecurityStore_p144["TbSecurityStore"] -->|extends| TbEditableSecurityStore_c144["TbEditableSecurityStore"]
    Object______p145["Object/外部框架"] -->|extends| TbInMemorySecurityStore_c145["TbInMemorySecurityStore"]
    TbEditableSecurityStore_p146["TbEditableSecurityStore"] -->|implements| TbInMemorySecurityStore_c146["TbInMemorySecurityStore"]
    Object______p147["Object/外部框架"] -->|extends| TbL2M2MDtlsSessionInMemoryStore_c147["TbL2M2MDtlsSessionInMemoryStore"]
    TbLwM2MDtlsSessionStore_p148["TbLwM2MDtlsSessionStore"] -->|implements| TbL2M2MDtlsSessionInMemoryStore_c148["TbL2M2MDtlsSessionInMemoryStore"]
    Object______p149["Object/外部框架"] -->|extends| TbLwM2MDtlsSessionRedisStore_c149["TbLwM2MDtlsSessionRedisStore"]
    TbLwM2MDtlsSessionStore_p150["TbLwM2MDtlsSessionStore"] -->|implements| TbLwM2MDtlsSessionRedisStore_c150["TbLwM2MDtlsSessionRedisStore"]
    Object______p151["Object/外部框架"] -->|extends| TbLwM2mRedisClientOtaInfoStore_c151["TbLwM2mRedisClientOtaInfoStore"]
    TbLwM2MClientOtaInfoStore_p152["TbLwM2MClientOtaInfoStore"] -->|implements| TbLwM2mRedisClientOtaInfoStore_c152["TbLwM2mRedisClientOtaInfoStore"]
    Object______p153["Object/外部框架"] -->|extends| TbLwM2mRedisRegistrationStore_c153["TbLwM2mRedisRegistrationStore"]
    CaliforniumRegistrationStore_p154["CaliforniumRegistrationStore"] -->|implements| TbLwM2mRedisRegistrationStore_c154["TbLwM2mRedisRegistrationStore"]
    Startable_p155["Startable"] -->|implements| TbLwM2mRedisRegistrationStore_c155["TbLwM2mRedisRegistrationStore"]
    Stoppable_p156["Stoppable"] -->|implements| TbLwM2mRedisRegistrationStore_c156["TbLwM2mRedisRegistrationStore"]
    Destroyable_p157["Destroyable"] -->|implements| TbLwM2mRedisRegistrationStore_c157["TbLwM2mRedisRegistrationStore"]
    Object______p158["Object/外部框架"] -->|extends| Cleaner_c158["Cleaner"]
    Runnable_p159["Runnable"] -->|implements| Cleaner_c159["Cleaner"]
    Object______p160["Object/外部框架"] -->|extends| TbLwM2mRedisSecurityStore_c160["TbLwM2mRedisSecurityStore"]
    TbEditableSecurityStore_p161["TbEditableSecurityStore"] -->|implements| TbLwM2mRedisSecurityStore_c161["TbLwM2mRedisSecurityStore"]
    Object______p162["Object/外部框架"] -->|extends| TbLwM2mSecurityStore_c162["TbLwM2mSecurityStore"]
    TbMainSecurityStore_p163["TbMainSecurityStore"] -->|implements| TbLwM2mSecurityStore_c163["TbLwM2mSecurityStore"]
    Object______p164["Object/外部框架"] -->|extends| TbLwM2mStoreFactory_c164["TbLwM2mStoreFactory"]
    TbSecurityStore_p165["TbSecurityStore"] -->|extends| TbMainSecurityStore_c165["TbMainSecurityStore"]
    Object______p166["Object/外部框架"] -->|extends| TbRedisLwM2MClientStore_c166["TbRedisLwM2MClientStore"]
    TbLwM2MClientStore_p167["TbLwM2MClientStore"] -->|implements| TbRedisLwM2MClientStore_c167["TbRedisLwM2MClientStore"]
    Object______p168["Object/外部框架"] -->|extends| TbRedisLwM2MModelConfigStore_c168["TbRedisLwM2MModelConfigStore"]
    TbLwM2MModelConfigStore_p169["TbLwM2MModelConfigStore"] -->|implements| TbRedisLwM2MModelConfigStore_c169["TbRedisLwM2MModelConfigStore"]
    SecurityStore_p170["SecurityStore"] -->|extends| TbSecurityStore_c170["TbSecurityStore"]
    Object______p171["Object/外部框架"] -->|extends| LwM2MClientSerDes_c171["LwM2MClientSerDes"]
    Object______p172["Object/外部框架"] -->|extends| LwM2MIdentitySerDes_c172["LwM2MIdentitySerDes"]
    LwM2MExecutorAwareService_p173["LwM2MExecutorAwareService"] -->|extends| DefaultLwM2mUplinkMsgHandler_c173["DefaultLwM2mUplinkMsgHandler"]
    LwM2mUplinkMsgHandler_p174["LwM2mUplinkMsgHandler"] -->|implements| DefaultLwM2mUplinkMsgHandler_c174["DefaultLwM2mUplinkMsgHandler"]
    Object______p175["Object/外部框架"] -->|extends| LwM2MTransportUtil_c175["LwM2MTransportUtil"]
    Object______p176["Object/外部框架"] -->|extends| LwM2mValueConverterImpl_c176["LwM2mValueConverterImpl"]
    LwM2mValueConverter_p177["LwM2mValueConverter"] -->|implements| LwM2mValueConverterImpl_c177["LwM2mValueConverterImpl"]
    Object______p178["Object/外部框架"] -->|extends| LwM2MTransportBootstrapServiceTest_c178["LwM2MTransportBootstrapServiceTest"]
    Object______p179["Object/外部框架"] -->|extends| LwM2MTransportServerConfigTest_c179["LwM2MTransportServerConfigTest"]
    Object______p180["Object/外部框架"] -->|extends| DefaultLwM2mTransportServiceTest_c180["DefaultLwM2mTransportServiceTest"]
    Object______p181["Object/外部框架"] -->|extends| LwM2mClientTest_c181["LwM2mClientTest"]
    Object______p182["Object/外部框架"] -->|extends| LwM2MModelConfigServiceImplTest_c182["LwM2MModelConfigServiceImplTest"]
    Object______p183["Object/外部框架"] -->|extends| TbLwM2mRedisRegistrationStoreTest_c183["TbLwM2mRedisRegistrationStoreTest"]
    Object______p184["Object/外部框架"] -->|extends| LwM2MClientSerDesTest_c184["LwM2MClientSerDesTest"]
    Object______p185["Object/外部框架"] -->|extends| LwM2MIdentitySerDesTest_c185["LwM2MIdentitySerDesTest"]
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

- `LwM2MBootstrapTaskProvider.remove()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapTaskProvider.java`)
- `LwM2MSecureServerConfig.getId()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/config/LwM2MSecureServerConfig.java`)
- `LwM2MSecureServerConfig.getHost()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/config/LwM2MSecureServerConfig.java`)
- `LwM2MSecureServerConfig.getPort()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/config/LwM2MSecureServerConfig.java`)
- `LwM2MSecureServerConfig.getSecureHost()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/config/LwM2MSecureServerConfig.java`)
- `LwM2MSecureServerConfig.getSecurePort()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/config/LwM2MSecureServerConfig.java`)
- `LwM2MSecureServerConfig.getSslCredentials()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/config/LwM2MSecureServerConfig.java`)
- `AbstractLwM2mTransportResource.handleGET()` (public, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/AbstractLwM2mTransportResource.java`)
- `AbstractLwM2mTransportResource.handlePOST()` (public, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/AbstractLwM2mTransportResource.java`)
- `AbstractLwM2mTransportResource.processHandleGet()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/AbstractLwM2mTransportResource.java`)
- `AbstractLwM2mTransportResource.processHandlePost()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/AbstractLwM2mTransportResource.java`)
- `LwM2mQueuedRequest.send()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/LwM2mQueuedRequest.java`)
- `LwM2MAttributesService.getSharedAttributes()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/attributes/LwM2MAttributesService.java`)
- `LwM2MAttributesService.onGetAttributesResponse()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/attributes/LwM2MAttributesService.java`)
- `LwM2MAttributesService.onAttributesUpdate()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/attributes/LwM2MAttributesService.java`)
- `LwM2MAttributesService.onAttributesUpdate()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/attributes/LwM2MAttributesService.java`)
- `LwM2mClientContext.getClientByEndpoint()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/client/LwM2mClientContext.java`)
- `LwM2mClientContext.getClientBySessionInfo()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/client/LwM2mClientContext.java`)
- `LwM2mClientContext.getLwM2mClients()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/client/LwM2mClientContext.java`)
- `LwM2mClientContext.getProfile()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/client/LwM2mClientContext.java`)
- `LwM2mClientContext.getProfile()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/client/LwM2mClientContext.java`)
- `LwM2mClientContext.profileUpdate()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/client/LwM2mClientContext.java`)
- `LwM2mClientContext.getSupportedIdVerInClient()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/client/LwM2mClientContext.java`)
- `LwM2mClientContext.getClientByDeviceId()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/client/LwM2mClientContext.java`)
- `LwM2mClientContext.getObjectIdByKeyNameFromProfile()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/client/LwM2mClientContext.java`)
- `LwM2mClientContext.registerClient()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/client/LwM2mClientContext.java`)
- `LwM2mClientContext.update()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/client/LwM2mClientContext.java`)
- `LwM2mClientContext.sendMsgsAfterSleeping()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/client/LwM2mClientContext.java`)
- `LwM2mClientContext.onUplink()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/client/LwM2mClientContext.java`)
- `LwM2mClientContext.getRequestTimeout()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/client/LwM2mClientContext.java`)
- `LwM2mClientContext.asleep()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/client/LwM2mClientContext.java`)
- `LwM2mClientContext.awake()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/client/LwM2mClientContext.java`)
- `LwM2mClientContext.isDownlinkAllowed()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/client/LwM2mClientContext.java`)
- `LwM2MExecutorAwareService.getExecutorSize()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/common/LwM2MExecutorAwareService.java`)
- `LwM2MExecutorAwareService.getExecutorName()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/common/LwM2MExecutorAwareService.java`)
- `LwM2MExecutorAwareService.init()` (protected, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/common/LwM2MExecutorAwareService.java`)
- `LwM2MExecutorAwareService.destroy()` (public, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/common/LwM2MExecutorAwareService.java`)
- `AbstractTbLwM2MRequestCallback.onValidationError()` (public, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/downlink/AbstractTbLwM2MRequestCallback.java`)
- `AbstractTbLwM2MRequestCallback.onError()` (public, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/downlink/AbstractTbLwM2MRequestCallback.java`)
- `DownlinkRequestCallback.onSent()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/downlink/DownlinkRequestCallback.java`)


## 哪些方法可以重写

- `LwM2MTransportBootstrapService.init()` (public, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/LwM2MTransportBootstrapService.java`)
- `LwM2MTransportBootstrapService.shutdown()` (public, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/LwM2MTransportBootstrapService.java`)
- `LwM2MTransportBootstrapService.getLhBootstrapServer()` (public, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/LwM2MTransportBootstrapService.java`)
- `LwM2MTransportBootstrapService.LeshanBootstrapServerBuilder()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/LwM2MTransportBootstrapService.java`)
- `LwM2MTransportBootstrapService.LwM2mDefaultBootstrapSessionManager()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/LwM2MTransportBootstrapService.java`)
- `LwM2MBootstrapConfig.getLwM2MBootstrapConfig()` (public, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2MBootstrapConfig.java`)
- `LwM2MBootstrapConfig.BootstrapConfig()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2MBootstrapConfig.java`)
- `LwM2mDefaultBootstrapSessionManager.SecurityChecker()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java`)
- `LwM2mDefaultBootstrapSessionManager.begin()` (public, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java`)
- `LwM2mDefaultBootstrapSessionManager.DefaultBootstrapSession()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java`)
- `LwM2mDefaultBootstrapSessionManager.hasConfigFor()` (public, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java`)
- `LwM2mDefaultBootstrapSessionManager.initTasks()` (protected, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java`)
- `LwM2mDefaultBootstrapSessionManager.getFirstRequest()` (public, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java`)
- `LwM2mDefaultBootstrapSessionManager.nextRequest()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java`)
- `LwM2mDefaultBootstrapSessionManager.nextRequest()` (protected, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java`)
- `LwM2mDefaultBootstrapSessionManager.BootstrapFinishRequest()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java`)
- `LwM2mDefaultBootstrapSessionManager.nextRequest()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java`)
- `LwM2mDefaultBootstrapSessionManager.BootstrapFinishRequest()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java`)
- `LwM2mDefaultBootstrapSessionManager.onResponseSuccess()` (public, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java`)
- `LwM2mDefaultBootstrapSessionManager.onResponseError()` (public, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java`)
- `LwM2mDefaultBootstrapSessionManager.onRequestFailure()` (public, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java`)
- `LwM2mDefaultBootstrapSessionManager.end()` (public, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java`)
- `LwM2mDefaultBootstrapSessionManager.failed()` (public, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java`)
- `TbLwM2MDtlsBootstrapCertificateVerifier.getSupportedCertificateTypes()` (public, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/TbLwM2MDtlsBootstrapCertificateVerifier.java`)
- `TbLwM2MDtlsBootstrapCertificateVerifier.init()` (public, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/TbLwM2MDtlsBootstrapCertificateVerifier.java`)
- `TbLwM2MDtlsBootstrapCertificateVerifier.StaticNewAdvancedCertificateVerifier()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/TbLwM2MDtlsBootstrapCertificateVerifier.java`)
- `TbLwM2MDtlsBootstrapCertificateVerifier.verifyCertificate()` (public, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/TbLwM2MDtlsBootstrapCertificateVerifier.java`)
- `TbLwM2MDtlsBootstrapCertificateVerifier.CertificateVerificationResult()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/TbLwM2MDtlsBootstrapCertificateVerifier.java`)
- `TbLwM2MDtlsBootstrapCertificateVerifier.AlertMessage()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/TbLwM2MDtlsBootstrapCertificateVerifier.java`)
- `TbLwM2MDtlsBootstrapCertificateVerifier.HandshakeException()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/TbLwM2MDtlsBootstrapCertificateVerifier.java`)
- `TbLwM2MDtlsBootstrapCertificateVerifier.CertificateVerificationResult()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/TbLwM2MDtlsBootstrapCertificateVerifier.java`)
- `TbLwM2MDtlsBootstrapCertificateVerifier.CertificateVerificationResult()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/TbLwM2MDtlsBootstrapCertificateVerifier.java`)
- `TbLwM2MDtlsBootstrapCertificateVerifier.getAcceptedIssuers()` (public, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/TbLwM2MDtlsBootstrapCertificateVerifier.java`)
- `TbLwM2MDtlsBootstrapCertificateVerifier.setResultHandler()` (public, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/TbLwM2MDtlsBootstrapCertificateVerifier.java`)
- `LwM2MBootstrapConfigStoreTaskProvider.ReentrantReadWriteLock()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapConfigStoreTaskProvider.java`)
- `LwM2MBootstrapConfigStoreTaskProvider.getTasks()` (public, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapConfigStoreTaskProvider.java`)
- `LwM2MBootstrapConfigStoreTaskProvider.Tasks()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapConfigStoreTaskProvider.java`)
- `LwM2MBootstrapConfigStoreTaskProvider.BootstrapDiscoverRequest()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapConfigStoreTaskProvider.java`)
- `LwM2MBootstrapConfigStoreTaskProvider.Tasks()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapConfigStoreTaskProvider.java`)
- `LwM2MBootstrapConfigStoreTaskProvider.BootstrapReadRequest()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapConfigStoreTaskProvider.java`)


## 哪些方法必须重写

- `LwM2MTransportBootstrapService.LeshanBootstrapServerBuilder()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/LwM2MTransportBootstrapService.java`)
- `LwM2MTransportBootstrapService.LwM2mDefaultBootstrapSessionManager()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/LwM2MTransportBootstrapService.java`)
- `LwM2MBootstrapConfig.BootstrapConfig()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2MBootstrapConfig.java`)
- `LwM2mDefaultBootstrapSessionManager.SecurityChecker()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java`)
- `LwM2mDefaultBootstrapSessionManager.DefaultBootstrapSession()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java`)
- `LwM2mDefaultBootstrapSessionManager.nextRequest()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java`)
- `LwM2mDefaultBootstrapSessionManager.BootstrapFinishRequest()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java`)
- `LwM2mDefaultBootstrapSessionManager.nextRequest()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java`)
- `LwM2mDefaultBootstrapSessionManager.BootstrapFinishRequest()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java`)
- `TbLwM2MDtlsBootstrapCertificateVerifier.StaticNewAdvancedCertificateVerifier()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/TbLwM2MDtlsBootstrapCertificateVerifier.java`)
- `TbLwM2MDtlsBootstrapCertificateVerifier.CertificateVerificationResult()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/TbLwM2MDtlsBootstrapCertificateVerifier.java`)
- `TbLwM2MDtlsBootstrapCertificateVerifier.AlertMessage()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/TbLwM2MDtlsBootstrapCertificateVerifier.java`)
- `TbLwM2MDtlsBootstrapCertificateVerifier.HandshakeException()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/TbLwM2MDtlsBootstrapCertificateVerifier.java`)
- `TbLwM2MDtlsBootstrapCertificateVerifier.CertificateVerificationResult()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/TbLwM2MDtlsBootstrapCertificateVerifier.java`)
- `TbLwM2MDtlsBootstrapCertificateVerifier.CertificateVerificationResult()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/TbLwM2MDtlsBootstrapCertificateVerifier.java`)
- `LwM2MBootstrapConfigStoreTaskProvider.ReentrantReadWriteLock()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapConfigStoreTaskProvider.java`)
- `LwM2MBootstrapConfigStoreTaskProvider.Tasks()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapConfigStoreTaskProvider.java`)
- `LwM2MBootstrapConfigStoreTaskProvider.BootstrapDiscoverRequest()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapConfigStoreTaskProvider.java`)
- `LwM2MBootstrapConfigStoreTaskProvider.Tasks()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapConfigStoreTaskProvider.java`)
- `LwM2MBootstrapConfigStoreTaskProvider.BootstrapReadRequest()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapConfigStoreTaskProvider.java`)
- `LwM2MBootstrapConfigStoreTaskProvider.LwM2mPath()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapConfigStoreTaskProvider.java`)
- `LwM2MBootstrapConfigStoreTaskProvider.BigInteger()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapConfigStoreTaskProvider.java`)
- `LwM2MBootstrapConfigStoreTaskProvider.LwM2mPath()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapConfigStoreTaskProvider.java`)
- `LwM2MBootstrapConfigStoreTaskProvider.BootstrapDeleteRequest()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapConfigStoreTaskProvider.java`)
- `LwM2MBootstrapConfigStoreTaskProvider.BootstrapDeleteRequest()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapConfigStoreTaskProvider.java`)
- `LwM2MBootstrapConfigStoreTaskProvider.BootstrapDeleteRequest()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapConfigStoreTaskProvider.java`)
- `LwM2MBootstrapConfigStoreTaskProvider.LwM2MBootstrapClientInstanceIds()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapConfigStoreTaskProvider.java`)
- `LwM2MBootstrapSecurityStore.LwM2mSessionMsgListener()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapSecurityStore.java`)
- `LwM2MBootstrapSecurityStore.AtomicBoolean()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapSecurityStore.java`)
- `LwM2MBootstrapSecurityStore.AtomicBoolean()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapSecurityStore.java`)
- `LwM2MBootstrapTaskProvider.remove()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapTaskProvider.java`)
- `LwM2MConfigurationChecker.InvalidConfigurationException()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MConfigurationChecker.java`)
- `LwM2MConfigurationChecker.InvalidConfigurationException()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MConfigurationChecker.java`)
- `LwM2MConfigurationChecker.InvalidConfigurationException()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MConfigurationChecker.java`)
- `LwM2MInMemoryBootstrapConfigStore.ReentrantReadWriteLock()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MInMemoryBootstrapConfigStore.java`)
- `LwM2MInMemoryBootstrapConfigStore.LwM2MConfigurationChecker()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MInMemoryBootstrapConfigStore.java`)
- `LwM2MInMemoryBootstrapConfigStore.InvalidConfigurationException()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MInMemoryBootstrapConfigStore.java`)
- `LwM2MSecureServerConfig.getId()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/config/LwM2MSecureServerConfig.java`)
- `LwM2MSecureServerConfig.getHost()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/config/LwM2MSecureServerConfig.java`)
- `LwM2MSecureServerConfig.getPort()` (package, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/config/LwM2MSecureServerConfig.java`)


## 哪些地方使用了多态

- `Object/外部框架` -> `LwM2MTransportBootstrapService` (extends)
- `Object/外部框架` -> `LwM2MBootstrapConfig` (extends)
- `Serializable` -> `LwM2MBootstrapConfig` (implements)
- `Object/外部框架` -> `LwM2MBootstrapServers` (extends)
- `Object/外部框架` -> `LwM2MServerBootstrap` (extends)
- `DefaultBootstrapSessionManager` -> `LwM2mDefaultBootstrapSessionManager` (extends)
- `Object/外部框架` -> `TbLwM2MDtlsBootstrapCertificateVerifier` (extends)
- `NewAdvancedCertificateVerifier` -> `TbLwM2MDtlsBootstrapCertificateVerifier` (implements)
- `Object/外部框架` -> `LwM2MBootstrapClientInstanceIds` (extends)
- `Object/外部框架` -> `LwM2MBootstrapConfigStoreTaskProvider` (extends)
- `LwM2MBootstrapTaskProvider` -> `LwM2MBootstrapConfigStoreTaskProvider` (implements)
- `Object/外部框架` -> `LwM2MBootstrapSecurityStore` (extends)
- `BootstrapSecurityStore` -> `LwM2MBootstrapSecurityStore` (implements)
- `BootstrapTaskProvider` -> `LwM2MBootstrapTaskProvider` (extends)
- `ConfigurationChecker` -> `LwM2MConfigurationChecker` (extends)
- `InMemoryBootstrapConfigStore` -> `LwM2MInMemoryBootstrapConfigStore` (extends)
- `Object/外部框架` -> `LwM2MTransportBootstrapConfig` (extends)
- `LwM2MSecureServerConfig` -> `LwM2MTransportBootstrapConfig` (implements)
- `Object/外部框架` -> `LwM2MTransportServerConfig` (extends)
- `LwM2MSecureServerConfig` -> `LwM2MTransportServerConfig` (implements)
- `Object/外部框架` -> `LwM2mCredentialsSecurityInfoValidator` (extends)
- `Object/外部框架` -> `LwM2mRPkCredentials` (extends)
- `Object/外部框架` -> `TbLwM2MAuthorizer` (extends)
- `Authorizer` -> `TbLwM2MAuthorizer` (implements)
- `Object/外部框架` -> `TbLwM2MDtlsCertificateVerifier` (extends)
- `NewAdvancedCertificateVerifier` -> `TbLwM2MDtlsCertificateVerifier` (implements)
- `Object/外部框架` -> `TbLwM2MSecurityInfo` (extends)
- `Serializable` -> `TbLwM2MSecurityInfo` (implements)
- `Object/外部框架` -> `TbX509DtlsSessionInfo` (extends)
- `Serializable` -> `TbX509DtlsSessionInfo` (implements)
- `Object/外部框架` -> `LwM2MClientCredentials` (extends)
- `LwM2mCoapResource` -> `AbstractLwM2mTransportResource` (extends)
- `Object/外部框架` -> `DefaultLwM2mTransportService` (extends)
- `LwM2MTransportService` -> `DefaultLwM2mTransportService` (implements)
- `Object/外部框架` -> `LwM2MNetworkConfig` (extends)
- `TbTransportService` -> `LwM2MTransportService` (extends)
- `Object/外部框架` -> `LwM2mOtaConvert` (extends)
- `Object/外部框架` -> `LwM2mServerListener` (extends)
- `Object/外部框架` -> `LwM2mSessionMsgListener` (extends)
- `GenericFutureListener` -> `LwM2mSessionMsgListener` (implements)
- 其余 40 项已省略；完整源码证据可通过本模块 Java 文件继续追踪。


## 外部父类/接口

- `Authorizer`
- `BootstrapSecurityStore`
- `BootstrapTaskProvider`
- `CaliforniumRegistrationStore`
- `Cloneable`
- `ConfigurationChecker`
- `DefaultBootstrapSessionManager`
- `Destroyable`
- `Exception`
- `GenericFutureListener`
- `InMemoryBootstrapConfigStore`
- `LwM2mCoapResource`
- `LwM2mModel`
- `LwM2mModelProvider`
- `LwM2mValueConverter`
- `NewAdvancedCertificateVerifier`
- `Object/外部框架`
- `ResourceObserver`
- `Runnable`
- `RuntimeException`
- `SecurityStore`
- `Serializable`
- `SessionMsgListener`
- `Startable`
- `Stoppable`
- `TbTransportService`
- `TransportContext`


## 整个继承体系解决了什么问题

该继承体系把“稳定生命周期/契约”和“模块特定行为”分开：父类或接口负责统一入口、共享流程和多态调度，子类负责差异化协议、实体、规则、DAO、测试或工具行为。这样可以让上游模块通过父类型调用下游实现，同时保持每个实现只关注自己的变化点。
