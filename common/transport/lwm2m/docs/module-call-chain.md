# Thingsboard LwM2M Transport Common 模块调用链分析

> 生成范围：`common/transport/lwm2m`  
> Maven artifact：`lwm2m`  
> packaging：`jar`  
> 分析方式：静态扫描 POM、Java 注解、入口方法、关键技术触点和类关系；不是运行时 trace。

## 完整流程图

```mermaid
flowchart TD
    A["① 最先调用<br/>设备、网关、协议客户端、Docker/系统服务或 Spring Boot main 最先进入 transport 模块"]
    B["② 调用原因<br/>需要把 MQTT/HTTP/CoAP/LwM2M/SNMP 等协议消息接入 ThingsBoard"]
    C["③ 调用之前<br/>客户端已经建立 TCP/HTTP/UDP/DTLS 等连接并携带设备凭据、主题、payload 或 RPC 响应"]
    D["模块入口<br/>lwm2m"]
    E["⑤ 数据变化<br/>协议 payload 被解码为遥测、属性、RPC、订阅或会话事件，并附加租户/设备/会话上下文"]
    F{"技术触点判定"}
    G["⑥ 数据库<br/>DB 间接/否"]
    H["⑦ Actor<br/>Actor 间接/否"]
    I["⑧ MQTT<br/>MQTT 间接/否"]
    J["⑨ Kafka<br/>Kafka 间接/否"]
    K["⑩ Rule Engine<br/>Rule Engine 间接/否"]
    L["④ 调用之后<br/>协议消息被转换为 common transport 消息，进入队列/Actor/Rule Engine/DAO 后续链路"]
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
| ① 谁最先调用这里？ | 设备、网关、协议客户端、Docker/系统服务或 Spring Boot main 最先进入 transport 模块 |
| ② 为什么会调用？ | 需要把 MQTT/HTTP/CoAP/LwM2M/SNMP 等协议消息接入 ThingsBoard |
| ③ 调用之前发生了什么？ | 客户端已经建立 TCP/HTTP/UDP/DTLS 等连接并携带设备凭据、主题、payload 或 RPC 响应 |
| ④ 调用之后发生什么？ | 协议消息被转换为 common transport 消息，进入队列/Actor/Rule Engine/DAO 后续链路 |
| ⑤ 数据如何变化？ | 协议 payload 被解码为遥测、属性、RPC、订阅或会话事件，并附加租户/设备/会话上下文 |
| ⑥ 对数据库进行了哪些操作？ | 否，未发现直接操作证据；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。 |
| ⑦ 是否发送 Actor 消息？ | 否，未发现直接操作证据；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。 |
| ⑧ 是否发送 MQTT 消息？ | 否，未发现直接操作证据；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。 |
| ⑨ 是否写入 Kafka？ | 否，未发现直接操作证据；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。 |
| ⑩ 是否写入 Rule Engine？ | 否，未发现直接操作证据；未发现直接 Rule Engine 写入，但消息可能在下游规则链中继续处理。 |

## 入口证据

- Spring Component 组件入口: `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/LwM2MTransportBootstrapService.java`
- Spring Component 组件入口: `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/TbLwM2MDtlsBootstrapCertificateVerifier.java`
- Spring Component 组件入口: `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MInMemoryBootstrapConfigStore.java`
- Spring Component 组件入口: `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/config/LwM2MTransportBootstrapConfig.java`
- Spring Component 组件入口: `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/config/LwM2MTransportServerConfig.java`
- Spring Component 组件入口: `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/secure/LwM2mCredentialsSecurityInfoValidator.java`
- Spring Component 组件入口: `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/secure/TbLwM2MAuthorizer.java`
- Spring Component 组件入口: `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/secure/TbLwM2MDtlsCertificateVerifier.java`
- Spring Component 组件入口: 其余 5 处入口省略
- Spring Service 业务服务入口: `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapSecurityStore.java`
- Spring Service 业务服务入口: `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/LwM2mVersionedModelProvider.java`
- Spring Service 业务服务入口: `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/LwM2mVersionedModelProvider.java`
- Spring Service 业务服务入口: `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/attributes/DefaultLwM2MAttributesService.java`
- Spring Service 业务服务入口: `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/client/LwM2mClientContextImpl.java`
- Spring Service 业务服务入口: `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/downlink/DefaultLwM2mDownlinkMsgHandler.java`
- Spring Service 业务服务入口: `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/log/DefaultLwM2MTelemetryLogService.java`
- Spring Service 业务服务入口: `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/model/LwM2MModelConfigServiceImpl.java`
- Spring Service 业务服务入口: 其余 4 处入口省略
- 测试框架入口: `common/transport/lwm2m/src/test/java/org/thingsboard/server/transport/lwm2m/bootstrap/LwM2MTransportBootstrapServiceTest.java`
- 测试框架入口: `common/transport/lwm2m/src/test/java/org/thingsboard/server/transport/lwm2m/config/LwM2MTransportServerConfigTest.java`
- 测试框架入口: `common/transport/lwm2m/src/test/java/org/thingsboard/server/transport/lwm2m/server/DefaultLwM2mTransportServiceTest.java`
- 测试框架入口: `common/transport/lwm2m/src/test/java/org/thingsboard/server/transport/lwm2m/server/client/LwM2mClientTest.java`
- 测试框架入口: `common/transport/lwm2m/src/test/java/org/thingsboard/server/transport/lwm2m/server/model/LwM2MModelConfigServiceImplTest.java`
- 测试框架入口: `common/transport/lwm2m/src/test/java/org/thingsboard/server/transport/lwm2m/server/store/TbLwM2mRedisRegistrationStoreTest.java`
- 测试框架入口: `common/transport/lwm2m/src/test/java/org/thingsboard/server/transport/lwm2m/server/store/util/LwM2MClientSerDesTest.java`
- 测试框架入口: `common/transport/lwm2m/src/test/java/org/thingsboard/server/transport/lwm2m/server/store/util/LwM2MIdentitySerDesTest.java`


## 静态关键词统计（不等同于实际发送/写入）

- database: 70 处静态触点；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。
- actor: 0 处静态触点；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。
- mqtt: 0 处静态触点；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。
- kafka: 0 处静态触点；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。
- rule_engine: 0 处静态触点；未发现直接 Rule Engine 写入，但消息可能在下游规则链中继续处理。
- cache: 259 处静态触点；直接操作证据：发现静态关键词触点。关键词触点 259 处仅作为辅助线索。
- queue: 29 处静态触点；直接操作证据：发现静态关键词触点。关键词触点 29 处仅作为辅助线索。
- rest: 0 处静态触点；未发现直接源码证据；若发生，通常在依赖的下游模块中完成。
- websocket: 602 处静态触点；直接操作证据：发现静态关键词触点。关键词触点 602 处仅作为辅助线索。
- transport: 1774 处静态触点；直接操作证据：发现静态关键词触点。关键词触点 1774 处仅作为辅助线索。

## 调用前后数据流

1. 调用前：客户端已经建立 TCP/HTTP/UDP/DTLS 等连接并携带设备凭据、主题、payload 或 RPC 响应
2. 模块入口：`lwm2m` 通过入口类、依赖 API、构建聚合或子模块暴露能力。
3. 模块内转换：协议 payload 被解码为遥测、属性、RPC、订阅或会话事件，并附加租户/设备/会话上下文
4. 调用后：协议消息被转换为 common transport 消息，进入队列/Actor/Rule Engine/DAO 后续链路
5. 数据库/Actor/MQTT/Kafka/Rule Engine 这些动作若没有直接触点，通常发生在依赖的 `application`、`dao`、`common/queue`、`common/transport` 或 `rule-engine` 模块中。

## 子模块与依赖

### 子模块

- 无子模块。


### 主要依赖

- `org.thingsboard.common.transport:transport-api`
- `org.thingsboard.common:data`
- `org.springframework:spring-context-support`
- `org.springframework:spring-context`
- `org.springframework.integration:spring-integration-redis`
- `org.slf4j:slf4j-api`
- `org.slf4j:log4j-over-slf4j`
- `ch.qos.logback:logback-core`
- `ch.qos.logback:logback-classic`
- `org.eclipse.leshan:leshan-server-cf`
- `org.eclipse.leshan:leshan-client-cf`
- `org.eclipse.leshan:leshan-server-redis`
- `org.springframework.boot:spring-boot-starter-test`
- `org.junit.vintage:junit-vintage-engine`
- `org.mockito:mockito-inline`
- `org.awaitility:awaitility`
- `org.eclipse.californium:californium-core`
- `org.eclipse.californium:californium-core`
- `org.eclipse.californium:element-connector`


## 关键类型样本

- `LwM2MTransportBootstrapService` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/LwM2MTransportBootstrapService.java`)
- `LwM2MBootstrapConfig` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2MBootstrapConfig.java`)
- `LwM2MBootstrapServers` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2MBootstrapServers.java`)
- `LwM2MServerBootstrap` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2MServerBootstrap.java`)
- `LwM2mDefaultBootstrapSessionManager` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/LwM2mDefaultBootstrapSessionManager.java`)
- `TbLwM2MDtlsBootstrapCertificateVerifier` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/secure/TbLwM2MDtlsBootstrapCertificateVerifier.java`)
- `LwM2MBootstrapClientInstanceIds` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapClientInstanceIds.java`)
- `LwM2MBootstrapConfigStoreTaskProvider` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapConfigStoreTaskProvider.java`)
- `LwM2MBootstrapSecurityStore` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapSecurityStore.java`)
- `LwM2MBootstrapTaskProvider` (interface, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MBootstrapTaskProvider.java`)
- `LwM2MConfigurationChecker` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MConfigurationChecker.java`)
- `LwM2MInMemoryBootstrapConfigStore` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/bootstrap/store/LwM2MInMemoryBootstrapConfigStore.java`)
- `LwM2MSecureServerConfig` (interface, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/config/LwM2MSecureServerConfig.java`)
- `LwM2MTransportBootstrapConfig` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/config/LwM2MTransportBootstrapConfig.java`)
- `LwM2MTransportServerConfig` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/config/LwM2MTransportServerConfig.java`)
- `TbLwM2mVersion` (enum, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/config/TbLwM2mVersion.java`)
- `LwM2mCredentialsSecurityInfoValidator` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/secure/LwM2mCredentialsSecurityInfoValidator.java`)
- `LwM2mRPkCredentials` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/secure/LwM2mRPkCredentials.java`)
- `TbLwM2MAuthorizer` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/secure/TbLwM2MAuthorizer.java`)
- `TbLwM2MDtlsCertificateVerifier` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/secure/TbLwM2MDtlsCertificateVerifier.java`)
- `TbLwM2MSecurityInfo` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/secure/TbLwM2MSecurityInfo.java`)
- `TbX509DtlsSessionInfo` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/secure/TbX509DtlsSessionInfo.java`)
- `LwM2MClientCredentials` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/secure/credentials/LwM2MClientCredentials.java`)
- `AbstractLwM2mTransportResource` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/AbstractLwM2mTransportResource.java`)
- `DefaultLwM2mTransportService` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/DefaultLwM2mTransportService.java`)
- `LwM2MNetworkConfig` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/LwM2MNetworkConfig.java`)
- `LwM2MOperationType` (enum, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/LwM2MOperationType.java`)
- `LwM2MTransportService` (interface, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/LwM2MTransportService.java`)
- `LwM2mOtaConvert` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/LwM2mOtaConvert.java`)
- `LwM2mQueuedRequest` (interface, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/LwM2mQueuedRequest.java`)
- `LwM2mServerListener` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/LwM2mServerListener.java`)
- `LwM2mSessionMsgListener` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/LwM2mSessionMsgListener.java`)
- `LwM2mTransportCoapResource` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/LwM2mTransportCoapResource.java`)
- `CoapResourceObserver` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/LwM2mTransportCoapResource.java`)
- `LwM2mTransportContext` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/LwM2mTransportContext.java`)
- `LwM2mTransportServerHelper` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/LwM2mTransportServerHelper.java`)
- `LwM2mVersionedModelProvider` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/LwM2mVersionedModelProvider.java`)
- `DynamicModel` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/LwM2mVersionedModelProvider.java`)
- `LwM2MJsonAdaptor` (class, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/adaptors/LwM2MJsonAdaptor.java`)
- `LwM2MTransportAdaptor` (interface, `common/transport/lwm2m/src/main/java/org/thingsboard/server/transport/lwm2m/server/adaptors/LwM2MTransportAdaptor.java`)
- 其余 20 项已省略；完整源码证据可通过本模块 Java 文件继续追踪。


## 阅读建议

- 先看“完整流程图”确认模块在全链路中的位置。
- 再看入口证据判断调用来源是 HTTP、MQTT/Transport、Actor、Kafka、Rule Engine、DAO、测试还是构建聚合。
- 最后用触点统计区分直接行为和下游间接行为，避免把依赖模块的数据库写入误认为本模块直接写入。
