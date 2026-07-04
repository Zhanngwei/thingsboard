# Thingsboard MQTT Transport Common 模块继承体系分析

> 生成范围：`common/transport/mqtt`  
> Maven artifact：`mqtt`  
> Java 类型数量：43  
> 分析方式：静态扫描 `class/interface/enum/record` 的 `extends`、`implements`、方法签名和源码触点。

## 完整继承树

```text
AbstractRemoteAddressFilter
├── IpFilter
ChannelInboundHandlerAdapter
├── MqttTransportHandler
├── ProxyIpFilter
ChannelInitializer
├── MqttTransportServerInitializer
DeviceAwareSessionContext
├── MqttDeviceAwareSessionContext
├── ├── AbstractGatewayDeviceSessionContext
├── ├── ├── GatewayDeviceSessionContext
├── ├── ├── SparkplugDeviceSessionContext
├── ├── DeviceSessionCtx
GenericFutureListener
├── «implements» MqttTransportHandler
MqttTopicFilter
├── «implements» AlwaysTrueTopicFilter
├── «implements» EqualsTopicFilter
├── «implements» RegexTopicFilter
MqttTransportAdaptor
├── «implements» BackwardCompatibilityAdaptor
├── «implements» JsonMqttAdaptor
├── «implements» ProtoMqttAdaptor
Object/外部框架
├── AbstractGatewaySessionHandler
├── ├── GatewaySessionHandler
├── ├── SparkplugNodeSessionHandler
├── AlwaysTrueTopicFilter
├── BackwardCompatibilityAdaptor
├── DeviceProvisionCallback
├── EqualsTopicFilter
├── File
├── GatewaySessionHandlerTest
├── JsonMqttAdaptor
├── MqttSslHandlerProvider
├── MqttTopicFilterFactory
├── MqttTopicFilterFactoryTest
├── MqttTopicMatcher
├── MqttTransportHandlerTest
├── MqttTransportService
├── OtaPackageCallback
├── ProtoMqttAdaptor
├── RegexTopicFilter
├── ReturnCodeResolver
├── SparkplugMetricUtil
├── SparkplugRpcRequestHeader
├── SparkplugRpcResponseBody
├── SparkplugTopic
├── SparkplugTopicUtil
├── ThingsboardMqttX509TrustManager
SessionMsgListener
├── «implements» AbstractGatewayDeviceSessionContext
├── «implements» ├── GatewayDeviceSessionContext
├── «implements» ├── SparkplugDeviceSessionContext
├── «implements» MqttTransportHandler
TbTransportService
├── «implements» MqttTransportService
TransportContext
├── MqttTransportContext
TransportServiceCallback
├── «implements» DeviceProvisionCallback
├── «implements» OtaPackageCallback
X509TrustManager
├── «implements» ThingsboardMqttX509TrustManager
```

## 继承图

```mermaid
flowchart TD
    Object______p0["Object/外部框架"] -->|extends| MqttSslHandlerProvider_c0["MqttSslHandlerProvider"]
    Object______p1["Object/外部框架"] -->|extends| ThingsboardMqttX509TrustManager_c1["ThingsboardMqttX509TrustManager"]
    X509TrustManager_p2["X509TrustManager"] -->|implements| ThingsboardMqttX509TrustManager_c2["ThingsboardMqttX509TrustManager"]
    TransportContext_p3["TransportContext"] -->|extends| MqttTransportContext_c3["MqttTransportContext"]
    ChannelInboundHandlerAdapter_p4["ChannelInboundHandlerAdapter"] -->|extends| MqttTransportHandler_c4["MqttTransportHandler"]
    GenericFutureListener_p5["GenericFutureListener"] -->|implements| MqttTransportHandler_c5["MqttTransportHandler"]
    SessionMsgListener_p6["SessionMsgListener"] -->|implements| MqttTransportHandler_c6["MqttTransportHandler"]
    Object______p7["Object/外部框架"] -->|extends| DeviceProvisionCallback_c7["DeviceProvisionCallback"]
    TransportServiceCallback_p8["TransportServiceCallback"] -->|implements| DeviceProvisionCallback_c8["DeviceProvisionCallback"]
    Object______p9["Object/外部框架"] -->|extends| OtaPackageCallback_c9["OtaPackageCallback"]
    TransportServiceCallback_p10["TransportServiceCallback"] -->|implements| OtaPackageCallback_c10["OtaPackageCallback"]
    ChannelInitializer_p11["ChannelInitializer"] -->|extends| MqttTransportServerInitializer_c11["MqttTransportServerInitializer"]
    Object______p12["Object/外部框架"] -->|extends| MqttTransportService_c12["MqttTransportService"]
    TbTransportService_p13["TbTransportService"] -->|implements| MqttTransportService_c13["MqttTransportService"]
    Object______p14["Object/外部框架"] -->|extends| BackwardCompatibilityAdaptor_c14["BackwardCompatibilityAdaptor"]
    MqttTransportAdaptor_p15["MqttTransportAdaptor"] -->|implements| BackwardCompatibilityAdaptor_c15["BackwardCompatibilityAdaptor"]
    Object______p16["Object/外部框架"] -->|extends| JsonMqttAdaptor_c16["JsonMqttAdaptor"]
    MqttTransportAdaptor_p17["MqttTransportAdaptor"] -->|implements| JsonMqttAdaptor_c17["JsonMqttAdaptor"]
    Object______p18["Object/外部框架"] -->|extends| ProtoMqttAdaptor_c18["ProtoMqttAdaptor"]
    MqttTransportAdaptor_p19["MqttTransportAdaptor"] -->|implements| ProtoMqttAdaptor_c19["ProtoMqttAdaptor"]
    AbstractRemoteAddressFilter_p20["AbstractRemoteAddressFilter"] -->|extends| IpFilter_c20["IpFilter"]
    ChannelInboundHandlerAdapter_p21["ChannelInboundHandlerAdapter"] -->|extends| ProxyIpFilter_c21["ProxyIpFilter"]
    MqttDeviceAwareSessionContext_p22["MqttDeviceAwareSessionContext"] -->|extends| AbstractGatewayDeviceSessionContext_c22["AbstractGatewayDeviceSessionContext"]
    SessionMsgListener_p23["SessionMsgListener"] -->|implements| AbstractGatewayDeviceSessionContext_c23["AbstractGatewayDeviceSessionContext"]
    Object______p24["Object/外部框架"] -->|extends| AbstractGatewaySessionHandler_c24["AbstractGatewaySessionHandler"]
    MqttDeviceAwareSessionContext_p25["MqttDeviceAwareSessionContext"] -->|extends| DeviceSessionCtx_c25["DeviceSessionCtx"]
    AbstractGatewayDeviceSessionContext_p26["AbstractGatewayDeviceSessionContext"] -->|extends| GatewayDeviceSessionContext_c26["GatewayDeviceSessionContext"]
    AbstractGatewaySessionHandler_p27["AbstractGatewaySessionHandler"] -->|extends| GatewaySessionHandler_c27["GatewaySessionHandler"]
    DeviceAwareSessionContext_p28["DeviceAwareSessionContext"] -->|extends| MqttDeviceAwareSessionContext_c28["MqttDeviceAwareSessionContext"]
    Object______p29["Object/外部框架"] -->|extends| MqttTopicMatcher_c29["MqttTopicMatcher"]
    AbstractGatewayDeviceSessionContext_p30["AbstractGatewayDeviceSessionContext"] -->|extends| SparkplugDeviceSessionContext_c30["SparkplugDeviceSessionContext"]
    AbstractGatewaySessionHandler_p31["AbstractGatewaySessionHandler"] -->|extends| SparkplugNodeSessionHandler_c31["SparkplugNodeSessionHandler"]
    Object______p32["Object/外部框架"] -->|extends| AlwaysTrueTopicFilter_c32["AlwaysTrueTopicFilter"]
    MqttTopicFilter_p33["MqttTopicFilter"] -->|implements| AlwaysTrueTopicFilter_c33["AlwaysTrueTopicFilter"]
    Object______p34["Object/外部框架"] -->|extends| EqualsTopicFilter_c34["EqualsTopicFilter"]
    MqttTopicFilter_p35["MqttTopicFilter"] -->|implements| EqualsTopicFilter_c35["EqualsTopicFilter"]
    Object______p36["Object/外部框架"] -->|extends| MqttTopicFilterFactory_c36["MqttTopicFilterFactory"]
    Object______p37["Object/外部框架"] -->|extends| RegexTopicFilter_c37["RegexTopicFilter"]
    MqttTopicFilter_p38["MqttTopicFilter"] -->|implements| RegexTopicFilter_c38["RegexTopicFilter"]
    Object______p39["Object/外部框架"] -->|extends| ReturnCodeResolver_c39["ReturnCodeResolver"]
    Object______p40["Object/外部框架"] -->|extends| SparkplugMetricUtil_c40["SparkplugMetricUtil"]
    Object______p41["Object/外部框架"] -->|extends| File_c41["File"]
    Object______p42["Object/外部框架"] -->|extends| SparkplugRpcRequestHeader_c42["SparkplugRpcRequestHeader"]
    Object______p43["Object/外部框架"] -->|extends| SparkplugRpcResponseBody_c43["SparkplugRpcResponseBody"]
    Object______p44["Object/外部框架"] -->|extends| SparkplugTopic_c44["SparkplugTopic"]
    Object______p45["Object/外部框架"] -->|extends| SparkplugTopicUtil_c45["SparkplugTopicUtil"]
    Object______p46["Object/外部框架"] -->|extends| MqttTransportHandlerTest_c46["MqttTransportHandlerTest"]
    Object______p47["Object/外部框架"] -->|extends| GatewaySessionHandlerTest_c47["GatewaySessionHandlerTest"]
    Object______p48["Object/外部框架"] -->|extends| MqttTopicFilterFactoryTest_c48["MqttTopicFilterFactoryTest"]
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

- `MqttTransportAdaptor.UnpooledByteBufAllocator()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/adaptors/MqttTransportAdaptor.java`)
- `MqttTransportAdaptor.createMqttPublishMsg()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/adaptors/MqttTransportAdaptor.java`)
- `MqttTransportAdaptor.MqttFixedHeader()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/adaptors/MqttTransportAdaptor.java`)
- `MqttTransportAdaptor.MqttPublishVariableHeader()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/adaptors/MqttTransportAdaptor.java`)
- `MqttTransportAdaptor.MqttPublishMessage()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/adaptors/MqttTransportAdaptor.java`)
- `AbstractGatewayDeviceSessionContext.getSessionId()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewayDeviceSessionContext.java`)
- `AbstractGatewayDeviceSessionContext.nextMsgId()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewayDeviceSessionContext.java`)
- `AbstractGatewayDeviceSessionContext.onGetAttributesResponse()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewayDeviceSessionContext.java`)
- `AbstractGatewayDeviceSessionContext.getDeviceInfo()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewayDeviceSessionContext.java`)
- `AbstractGatewayDeviceSessionContext.onAttributeUpdate()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewayDeviceSessionContext.java`)
- `AbstractGatewayDeviceSessionContext.getDeviceInfo()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewayDeviceSessionContext.java`)
- `AbstractGatewayDeviceSessionContext.onToDeviceRpcRequest()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewayDeviceSessionContext.java`)
- `AbstractGatewayDeviceSessionContext.onRemoteSessionCloseCommand()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewayDeviceSessionContext.java`)
- `AbstractGatewayDeviceSessionContext.onToServerRpcResponse()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewayDeviceSessionContext.java`)
- `AbstractGatewayDeviceSessionContext.onDeviceDeleted()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewayDeviceSessionContext.java`)
- `AbstractGatewaySessionHandler.createWeakMap()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.onDevicesDisconnect()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.onDeviceDeleted()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.getNodeId()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.getSessionId()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.getPayloadAdaptor()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.deregisterSession()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.writeAndFlush()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.nextMsgId()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.isJsonPayloadType()` (protected, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.processOnConnect()` (protected, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.onSuccess()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.onFailure()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.onDeviceConnect()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.ReentrantLock()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.getDeviceCreationFuture()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.onSuccess()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.onError()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.newDeviceSessionCtx()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.getMsgId()` (protected, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.AdaptorException()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.AdaptorException()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.processOnDisconnect()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.onSuccess()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)
- `AbstractGatewaySessionHandler.JsonSyntaxException()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/session/AbstractGatewaySessionHandler.java`)


## 哪些方法可以重写

- `MqttSslHandlerProvider.mqttSslCredentials()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `MqttSslHandlerProvider.SslCredentialsConfig()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `MqttSslHandlerProvider.getSslHandler()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `MqttSslHandlerProvider.SslHandler()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `MqttSslHandlerProvider.RuntimeException()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `MqttSslHandlerProvider.ThingsboardMqttX509TrustManager()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `MqttSslHandlerProvider.getAcceptedIssuers()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `MqttSslHandlerProvider.CertificateException()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `MqttSslHandlerProvider.CountDownLatch()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `MqttSslHandlerProvider.onSuccess()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `MqttSslHandlerProvider.onError()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `MqttSslHandlerProvider.CertificateException()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `MqttSslHandlerProvider.CertificateException()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `ThingsboardMqttX509TrustManager.mqttSslCredentials()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `ThingsboardMqttX509TrustManager.SslCredentialsConfig()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `ThingsboardMqttX509TrustManager.getSslHandler()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `ThingsboardMqttX509TrustManager.SslHandler()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `ThingsboardMqttX509TrustManager.RuntimeException()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `ThingsboardMqttX509TrustManager.getAcceptedIssuers()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `ThingsboardMqttX509TrustManager.CertificateException()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `ThingsboardMqttX509TrustManager.CountDownLatch()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `ThingsboardMqttX509TrustManager.onSuccess()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `ThingsboardMqttX509TrustManager.onError()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `ThingsboardMqttX509TrustManager.CertificateException()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `ThingsboardMqttX509TrustManager.CertificateException()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `MqttTransportContext.AtomicInteger()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportContext.java`)
- `MqttTransportContext.init()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportContext.java`)
- `MqttTransportContext.channelRegistered()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportContext.java`)
- `MqttTransportContext.channelUnregistered()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportContext.java`)
- `MqttTransportContext.checkAddress()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportContext.java`)
- `MqttTransportContext.onAuthSuccess()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportContext.java`)
- `MqttTransportContext.onAuthFailure()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportContext.java`)
- `MqttTransportHandler.DeviceSessionCtx()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.channelRead()` (public, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.getAddress()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.processMqttMsg()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.DeviceProvisionCallback()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.DeviceProvisionCallback()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.MqttMessage()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.enqueueRegularSessionMsg()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)


## 哪些方法必须重写

- `MqttSslHandlerProvider.SslCredentialsConfig()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `MqttSslHandlerProvider.SslHandler()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `MqttSslHandlerProvider.RuntimeException()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `MqttSslHandlerProvider.ThingsboardMqttX509TrustManager()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `MqttSslHandlerProvider.CertificateException()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `MqttSslHandlerProvider.CountDownLatch()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `MqttSslHandlerProvider.CertificateException()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `MqttSslHandlerProvider.CertificateException()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `ThingsboardMqttX509TrustManager.SslCredentialsConfig()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `ThingsboardMqttX509TrustManager.SslHandler()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `ThingsboardMqttX509TrustManager.RuntimeException()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `ThingsboardMqttX509TrustManager.CertificateException()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `ThingsboardMqttX509TrustManager.CountDownLatch()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `ThingsboardMqttX509TrustManager.CertificateException()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `ThingsboardMqttX509TrustManager.CertificateException()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttSslHandlerProvider.java`)
- `MqttTransportContext.AtomicInteger()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportContext.java`)
- `MqttTransportHandler.DeviceSessionCtx()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.DeviceProvisionCallback()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.DeviceProvisionCallback()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.MqttMessage()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.MqttMessage()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.getMetadata()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.getMetadata()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.getPubAckCallback()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.getPubAckCallback()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.getPubAckCallback()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.getPubAckCallback()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.getMetadata()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.getMetadata()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.getMetadata()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.getMetadata()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.getMetadata()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.getMetadata()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.getPubAckCallback()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.getPubAckCallback()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.getPubAckCallback()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.getPubAckCallback()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.getPubAckCallback()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.getPubAckCallback()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)
- `MqttTransportHandler.getPubAckCallback()` (package, `common/transport/mqtt/src/main/java/org/thingsboard/server/transport/mqtt/MqttTransportHandler.java`)


## 哪些地方使用了多态

- `Object/外部框架` -> `MqttSslHandlerProvider` (extends)
- `Object/外部框架` -> `ThingsboardMqttX509TrustManager` (extends)
- `X509TrustManager` -> `ThingsboardMqttX509TrustManager` (implements)
- `TransportContext` -> `MqttTransportContext` (extends)
- `ChannelInboundHandlerAdapter` -> `MqttTransportHandler` (extends)
- `GenericFutureListener` -> `MqttTransportHandler` (implements)
- `SessionMsgListener` -> `MqttTransportHandler` (implements)
- `Object/外部框架` -> `DeviceProvisionCallback` (extends)
- `TransportServiceCallback` -> `DeviceProvisionCallback` (implements)
- `Object/外部框架` -> `OtaPackageCallback` (extends)
- `TransportServiceCallback` -> `OtaPackageCallback` (implements)
- `ChannelInitializer` -> `MqttTransportServerInitializer` (extends)
- `Object/外部框架` -> `MqttTransportService` (extends)
- `TbTransportService` -> `MqttTransportService` (implements)
- `Object/外部框架` -> `BackwardCompatibilityAdaptor` (extends)
- `MqttTransportAdaptor` -> `BackwardCompatibilityAdaptor` (implements)
- `Object/外部框架` -> `JsonMqttAdaptor` (extends)
- `MqttTransportAdaptor` -> `JsonMqttAdaptor` (implements)
- `Object/外部框架` -> `ProtoMqttAdaptor` (extends)
- `MqttTransportAdaptor` -> `ProtoMqttAdaptor` (implements)
- `AbstractRemoteAddressFilter` -> `IpFilter` (extends)
- `ChannelInboundHandlerAdapter` -> `ProxyIpFilter` (extends)
- `MqttDeviceAwareSessionContext` -> `AbstractGatewayDeviceSessionContext` (extends)
- `SessionMsgListener` -> `AbstractGatewayDeviceSessionContext` (implements)
- `Object/外部框架` -> `AbstractGatewaySessionHandler` (extends)
- `MqttDeviceAwareSessionContext` -> `DeviceSessionCtx` (extends)
- `AbstractGatewayDeviceSessionContext` -> `GatewayDeviceSessionContext` (extends)
- `AbstractGatewaySessionHandler` -> `GatewaySessionHandler` (extends)
- `DeviceAwareSessionContext` -> `MqttDeviceAwareSessionContext` (extends)
- `Object/外部框架` -> `MqttTopicMatcher` (extends)
- `AbstractGatewayDeviceSessionContext` -> `SparkplugDeviceSessionContext` (extends)
- `AbstractGatewaySessionHandler` -> `SparkplugNodeSessionHandler` (extends)
- `Object/外部框架` -> `AlwaysTrueTopicFilter` (extends)
- `MqttTopicFilter` -> `AlwaysTrueTopicFilter` (implements)
- `Object/外部框架` -> `EqualsTopicFilter` (extends)
- `MqttTopicFilter` -> `EqualsTopicFilter` (implements)
- `Object/外部框架` -> `MqttTopicFilterFactory` (extends)
- `Object/外部框架` -> `RegexTopicFilter` (extends)
- `MqttTopicFilter` -> `RegexTopicFilter` (implements)
- `Object/外部框架` -> `ReturnCodeResolver` (extends)
- 其余 9 项已省略；完整源码证据可通过本模块 Java 文件继续追踪。


## 外部父类/接口

- `AbstractRemoteAddressFilter`
- `ChannelInboundHandlerAdapter`
- `ChannelInitializer`
- `DeviceAwareSessionContext`
- `GenericFutureListener`
- `Object/外部框架`
- `SessionMsgListener`
- `TbTransportService`
- `TransportContext`
- `TransportServiceCallback`
- `X509TrustManager`


## 整个继承体系解决了什么问题

该继承体系把“稳定生命周期/契约”和“模块特定行为”分开：父类或接口负责统一入口、共享流程和多态调度，子类负责差异化协议、实体、规则、DAO、测试或工具行为。这样可以让上游模块通过父类型调用下游实现，同时保持每个实现只关注自己的变化点。
