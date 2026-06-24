# Transport 模块学习文档

## 模块概述

Transport 模块负责设备与 ThingsBoard 平台之间的通信。它支持多种 IoT 协议，将设备发送的原始数据转换为平台内部格式，并通过消息队列传递给核心服务。

## 架构设计

```
设备
 │
 ├── MQTT (tcp:1883 / ssl:8883)
 ├── CoAP (udp:5683 / dtls:5684)
 ├── HTTP (http:8080 / https:443)
 ├── LwM2M (udp:5685)
 └── SNMP (udp:161)
 │
 ▼
Transport Layer (协议适配)
 │
 ▼
TransportService (统一处理接口)
 │
 ▼
Message Queue (Kafka)
 │
 ▼
Core Service / Rule Engine
```

## 模块结构

### common/transport/transport-api — 传输层API
核心接口和默认实现:
- `TransportService` - 传输服务主接口
- `DefaultTransportService` - 默认实现(核心!)
- `SessionMsgListener` - 设备会话消息监听
- `TransportContext` - 传输上下文

### common/transport/mqtt — MQTT传输
- 基于 Netty 的高性能 MQTT Broker
- 支持 MQTT 3.1.1 和 5.0
- QoS 0 和 QoS 1
- Topic 格式: `v1/devices/me/telemetry`
- 支持 JSON 和 Protobuf 数据格式

### common/transport/coap — CoAP传输
- 基于 Eclipse Californium
- 支持 DTLS 安全传输
- 资源路径: `/api/v1/{token}/telemetry`

### common/transport/http — HTTP传输
- RESTful API 风格
- 端点: `POST /api/v1/{token}/telemetry`
- 支持 JSON 数据格式

### common/transport/lwm2m — LwM2M传输
- 基于 Eclipse Leshan
- 支持设备管理操作(Read/Write/Execute/Observe)
- OMA LwM2M 对象模型

### common/transport/snmp — SNMP传输
- 基于 SNMP4J
- 支持 SNMP v1/v2c/v3
- GET/SET/TRAP 操作

## 独立传输微服务 (transport/)

在微服务架构下，每种传输协议可以独立部署:
```
transport/
├── mqtt/     # MQTT 独立微服务
├── coap/     # CoAP 独立微服务
├── http/     # HTTP 独立微服务
├── lwm2m/    # LwM2M 独立微服务
└── snmp/     # SNMP 独立微服务
```

## TransportService 核心流程

### 设备认证
```
1. 设备连接并提供凭证(Token/X.509/Basic)
2. TransportService.process(ValidateDeviceTokenRequestMsg)
3. 通过 Transport API Queue 向 Core 服务验证
4. Core 返回设备信息和配置
5. 创建设备会话 SessionMetaData
```

### 遥测上报
```
1. 设备发送遥测数据(JSON/Protobuf)
2. 协议层解析为 PostTelemetryMsg
3. TransportService.process(sessionInfo, PostTelemetryMsg)
4. DefaultTransportService 将消息编码为 TbProtoQueueMsg
5. 通过 TbQueueProducer 发送到规则引擎队列
6. 规则引擎消费并处理
```

### 属性上报
```
1. 设备发送属性数据
2. 协议层解析为 PostAttributeMsg
3. TransportService.process(sessionInfo, PostAttributeMsg)
4. 发送到核心队列
5. Core 服务保存属性
```

### RPC 处理
```
1. 平台通过队列发送 RPC 请求
2. Transport 接收到 ToDeviceRpcRequestMsg
3. 通过 SessionMsgListener 回调通知设备会话
4. 协议层将 RPC 消息发送给设备
5. 设备响应后通过 TransportService 上报响应
```

## MQTT 协议详解

### Topic 结构
```
上行(设备→平台):
  v1/devices/me/telemetry        - 上报遥测
  v1/devices/me/attributes       - 上报属性
  v1/devices/me/rpc/response/{id} - RPC响应

下行(平台→设备):
  v1/devices/me/attributes/response/{id}  - 属性响应
  v1/devices/me/rpc/request/{id}          - RPC请求

网关设备:
  v1/gateway/telemetry           - 网关代理上报遥测
  v1/gateway/connect             - 子设备连接
  v1/gateway/disconnect          - 子设备断开
```

### 认证方式
1. **Access Token**: MQTT username 填写设备 Token
2. **Basic MQTT**: username + password
3. **X.509 Certificate**: TLS 客户端证书

## 限流机制

Transport 层实现了多级限流:
- **设备级**: 每个设备的消息速率限制
- **租户级**: 租户总消息速率限制
- **传输级**: 传输层总连接数限制
- **数据点限制**: 每条消息的数据点数量限制
