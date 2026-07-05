/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data.device.profile;

/**
 * Created by ashvayka on 19.01.17.
 */
/**
 * 中文说明：
 * 1. 类目的：`MqttTopics` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
public class MqttTopics {

    /**
     * 请求常量，用于统一引用固定值。
     */
    private static final String REQUEST = "/request";
    private static final String RESPONSE = "/response";
    /**
     * RPC常量，用于统一引用固定值。
     */
    private static final String RPC = "/rpc";
    private static final String CONNECT = "/connect";
    /**
     * `DISCONNECT`常量，用于统一引用固定值。
     */
    private static final String DISCONNECT = "/disconnect";
    private static final String TELEMETRY = "/telemetry";
    /**
     * `ATTRIBUTES`常量，用于统一引用固定值。
     */
    private static final String ATTRIBUTES = "/attributes";
    private static final String CLAIM = "/claim";
    /**
     * 主题常量，用于统一引用固定值。
     */
    private static final String SUB_TOPIC = "+";
    private static final String PROVISION = "/provision";
    /**
     * `FIRMWARE`常量，用于统一引用固定值。
     */
    private static final String FIRMWARE = "/fw";
    private static final String SOFTWARE = "/sw";
    /**
     * `CHUNK`常量，用于统一引用固定值。
     */
    private static final String CHUNK = "/chunk/";
    private static final String ERROR = "/error";
    /**
     * 遥测常量，用于统一引用固定值。
     */
    private static final String TELEMETRY_SHORT = "/t";
    private static final String ATTRIBUTES_SHORT = "/a";
    /**
     * RPC常量，用于统一引用固定值。
     */
    private static final String RPC_SHORT = "/r";
    private static final String REQUEST_SHORT = "/req";
    /**
     * 响应常量，用于统一引用固定值。
     */
    private static final String RESPONSE_SHORT = "/res";
    private static final String JSON_SHORT = "j";
    /**
     * Protobuf常量，用于统一引用固定值。
     */
    private static final String PROTO_SHORT = "p";
    private static final String ATTRIBUTES_RESPONSE = ATTRIBUTES + RESPONSE;
    /**
     * 请求常量，用于统一引用固定值。
     */
    private static final String ATTRIBUTES_REQUEST = ATTRIBUTES + REQUEST;
    private static final String ATTRIBUTES_RESPONSE_SHORT = ATTRIBUTES_SHORT + RESPONSE_SHORT + "/";
    /**
     * 请求常量，用于统一引用固定值。
     */
    private static final String ATTRIBUTES_REQUEST_SHORT = ATTRIBUTES_SHORT + REQUEST_SHORT + "/";
    private static final String DEVICE_RPC_RESPONSE = RPC + RESPONSE + "/";
    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final String DEVICE_RPC_REQUEST = RPC + REQUEST + "/";
    private static final String DEVICE_RPC_RESPONSE_SHORT = RPC_SHORT + RESPONSE_SHORT + "/";
    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final String DEVICE_RPC_REQUEST_SHORT = RPC_SHORT + REQUEST_SHORT + "/";
    private static final String DEVICE_ATTRIBUTES_RESPONSE = ATTRIBUTES_RESPONSE + "/";
    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final String DEVICE_ATTRIBUTES_REQUEST = ATTRIBUTES_REQUEST + "/";
    // v1 topics
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String BASE_DEVICE_API_TOPIC = "v1/devices/me";
    public static final String DEVICE_RPC_RESPONSE_TOPIC = BASE_DEVICE_API_TOPIC + DEVICE_RPC_RESPONSE;
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_RPC_RESPONSE_SUB_TOPIC = DEVICE_RPC_RESPONSE_TOPIC + SUB_TOPIC;
    public static final String DEVICE_RPC_REQUESTS_TOPIC = BASE_DEVICE_API_TOPIC + DEVICE_RPC_REQUEST;
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_RPC_REQUESTS_SUB_TOPIC = DEVICE_RPC_REQUESTS_TOPIC + SUB_TOPIC;
    public static final String DEVICE_ATTRIBUTES_RESPONSE_TOPIC_PREFIX = BASE_DEVICE_API_TOPIC + DEVICE_ATTRIBUTES_RESPONSE;
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_ATTRIBUTES_RESPONSES_TOPIC = DEVICE_ATTRIBUTES_RESPONSE_TOPIC_PREFIX + SUB_TOPIC;
    public static final String DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX = BASE_DEVICE_API_TOPIC + DEVICE_ATTRIBUTES_REQUEST;
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_TELEMETRY_TOPIC = BASE_DEVICE_API_TOPIC + TELEMETRY;
    public static final String DEVICE_CLAIM_TOPIC = BASE_DEVICE_API_TOPIC + CLAIM;
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_ATTRIBUTES_TOPIC = BASE_DEVICE_API_TOPIC + ATTRIBUTES;
    public static final String DEVICE_PROVISION_REQUEST_TOPIC = PROVISION + REQUEST;
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_PROVISION_RESPONSE_TOPIC = PROVISION + RESPONSE;
    // v1 gateway topics
    /**
     * 主题常量，用于统一引用固定值。
     */
    public static final String BASE_GATEWAY_API_TOPIC = "v1/gateway";
    public static final String GATEWAY_CONNECT_TOPIC = BASE_GATEWAY_API_TOPIC + CONNECT;
    /**
     * 主题常量，用于统一引用固定值。
     */
    public static final String GATEWAY_DISCONNECT_TOPIC = BASE_GATEWAY_API_TOPIC + DISCONNECT;
    public static final String GATEWAY_ATTRIBUTES_TOPIC = BASE_GATEWAY_API_TOPIC + ATTRIBUTES;
    /**
     * 主题常量，用于统一引用固定值。
     */
    public static final String GATEWAY_TELEMETRY_TOPIC = BASE_GATEWAY_API_TOPIC + TELEMETRY;
    public static final String GATEWAY_CLAIM_TOPIC = BASE_GATEWAY_API_TOPIC + CLAIM;
    /**
     * 主题常量，用于统一引用固定值。
     */
    public static final String GATEWAY_RPC_TOPIC = BASE_GATEWAY_API_TOPIC + RPC;
    public static final String GATEWAY_ATTRIBUTES_REQUEST_TOPIC = BASE_GATEWAY_API_TOPIC + ATTRIBUTES_REQUEST;
    /**
     * 主题常量，用于统一引用固定值。
     */
    public static final String GATEWAY_ATTRIBUTES_RESPONSE_TOPIC = BASE_GATEWAY_API_TOPIC + ATTRIBUTES_RESPONSE;
    // v2 topics
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String BASE_DEVICE_API_TOPIC_V2 = "v2";
    public static final String REQUEST_ID_PATTERN = "(?<requestId>\\d+)";
    /**
     * `CHUNK_PATTERN`常量，用于统一引用固定值。
     */
    public static final String CHUNK_PATTERN = "(?<chunk>\\d+)";
    public static final String DEVICE_FIRMWARE_REQUEST_TOPIC_PATTERN = BASE_DEVICE_API_TOPIC_V2 + FIRMWARE + REQUEST + "/" + REQUEST_ID_PATTERN + CHUNK + CHUNK_PATTERN;
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_FIRMWARE_RESPONSES_TOPIC = BASE_DEVICE_API_TOPIC_V2 + FIRMWARE + RESPONSE + "/" + SUB_TOPIC + CHUNK + SUB_TOPIC;
    public static final String DEVICE_FIRMWARE_ERROR_TOPIC = BASE_DEVICE_API_TOPIC_V2 + FIRMWARE + ERROR;
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_SOFTWARE_FIRMWARE_RESPONSES_TOPIC_FORMAT = BASE_DEVICE_API_TOPIC_V2 + "/%s" + RESPONSE + "/%s" + CHUNK + "%d";
    public static final String DEVICE_SOFTWARE_REQUEST_TOPIC_PATTERN = BASE_DEVICE_API_TOPIC_V2 + SOFTWARE + REQUEST + "/" + REQUEST_ID_PATTERN + CHUNK + CHUNK_PATTERN;
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_SOFTWARE_RESPONSES_TOPIC = BASE_DEVICE_API_TOPIC_V2 + SOFTWARE + RESPONSE + "/" + SUB_TOPIC + CHUNK + SUB_TOPIC;
    public static final String DEVICE_SOFTWARE_ERROR_TOPIC = BASE_DEVICE_API_TOPIC_V2 + SOFTWARE + ERROR;
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_ATTRIBUTES_SHORT_TOPIC = BASE_DEVICE_API_TOPIC_V2 + ATTRIBUTES_SHORT;
    public static final String DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC = BASE_DEVICE_API_TOPIC_V2 + ATTRIBUTES_SHORT + "/" + JSON_SHORT;
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_ATTRIBUTES_SHORT_PROTO_TOPIC = BASE_DEVICE_API_TOPIC_V2 + ATTRIBUTES_SHORT + "/" + PROTO_SHORT;
    public static final String DEVICE_TELEMETRY_SHORT_TOPIC = BASE_DEVICE_API_TOPIC_V2 + TELEMETRY_SHORT;
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_TELEMETRY_SHORT_JSON_TOPIC = BASE_DEVICE_API_TOPIC_V2 + TELEMETRY_SHORT + "/" + JSON_SHORT;
    public static final String DEVICE_TELEMETRY_SHORT_PROTO_TOPIC = BASE_DEVICE_API_TOPIC_V2 + TELEMETRY_SHORT + "/" + PROTO_SHORT;
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_RPC_RESPONSE_SHORT_TOPIC = BASE_DEVICE_API_TOPIC_V2 + DEVICE_RPC_RESPONSE_SHORT;
    public static final String DEVICE_RPC_RESPONSE_SHORT_JSON_TOPIC = BASE_DEVICE_API_TOPIC_V2 + DEVICE_RPC_RESPONSE_SHORT + JSON_SHORT + "/";
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_RPC_RESPONSE_SHORT_PROTO_TOPIC = BASE_DEVICE_API_TOPIC_V2 + DEVICE_RPC_RESPONSE_SHORT + PROTO_SHORT + "/";
    public static final String DEVICE_RPC_RESPONSE_SUB_SHORT_TOPIC = DEVICE_RPC_RESPONSE_SHORT_TOPIC + SUB_TOPIC;
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_RPC_RESPONSE_SUB_SHORT_JSON_TOPIC = DEVICE_RPC_RESPONSE_SHORT_TOPIC  + JSON_SHORT + "/" + SUB_TOPIC;
    public static final String DEVICE_RPC_RESPONSE_SUB_SHORT_PROTO_TOPIC = DEVICE_RPC_RESPONSE_SHORT_TOPIC  + PROTO_SHORT + "/" + SUB_TOPIC;
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_RPC_REQUESTS_SHORT_TOPIC = BASE_DEVICE_API_TOPIC_V2 + DEVICE_RPC_REQUEST_SHORT;
    public static final String DEVICE_RPC_REQUESTS_SHORT_JSON_TOPIC = BASE_DEVICE_API_TOPIC_V2 + DEVICE_RPC_REQUEST_SHORT + JSON_SHORT + "/";
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_RPC_REQUESTS_SHORT_PROTO_TOPIC = BASE_DEVICE_API_TOPIC_V2 + DEVICE_RPC_REQUEST_SHORT + PROTO_SHORT + "/";
    public static final String DEVICE_RPC_REQUESTS_SUB_SHORT_TOPIC = DEVICE_RPC_REQUESTS_SHORT_TOPIC + SUB_TOPIC;
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_RPC_REQUESTS_SUB_SHORT_JSON_TOPIC = DEVICE_RPC_REQUESTS_SHORT_TOPIC + JSON_SHORT + "/" + SUB_TOPIC;
    public static final String DEVICE_RPC_REQUESTS_SUB_SHORT_PROTO_TOPIC = DEVICE_RPC_REQUESTS_SHORT_TOPIC + PROTO_SHORT + "/" + SUB_TOPIC;
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_ATTRIBUTES_RESPONSE_SHORT_TOPIC_PREFIX = BASE_DEVICE_API_TOPIC_V2 + ATTRIBUTES_RESPONSE_SHORT;
    public static final String DEVICE_ATTRIBUTES_RESPONSES_SHORT_TOPIC = DEVICE_ATTRIBUTES_RESPONSE_SHORT_TOPIC_PREFIX + SUB_TOPIC;
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_ATTRIBUTES_RESPONSE_SHORT_JSON_TOPIC_PREFIX = DEVICE_ATTRIBUTES_RESPONSE_SHORT_TOPIC_PREFIX + JSON_SHORT + "/";
    public static final String DEVICE_ATTRIBUTES_RESPONSES_SHORT_JSON_TOPIC = DEVICE_ATTRIBUTES_RESPONSE_SHORT_JSON_TOPIC_PREFIX + SUB_TOPIC;
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_ATTRIBUTES_RESPONSE_SHORT_PROTO_TOPIC_PREFIX = DEVICE_ATTRIBUTES_RESPONSE_SHORT_TOPIC_PREFIX + PROTO_SHORT + "/";
    public static final String DEVICE_ATTRIBUTES_RESPONSES_SHORT_PROTO_TOPIC = DEVICE_ATTRIBUTES_RESPONSE_SHORT_PROTO_TOPIC_PREFIX + SUB_TOPIC;
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_ATTRIBUTES_REQUEST_SHORT_TOPIC_PREFIX = BASE_DEVICE_API_TOPIC_V2 + ATTRIBUTES_REQUEST_SHORT;
    public static final String DEVICE_ATTRIBUTES_REQUEST_SHORT_JSON_TOPIC_PREFIX = DEVICE_ATTRIBUTES_REQUEST_SHORT_TOPIC_PREFIX + JSON_SHORT + "/";
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_ATTRIBUTES_REQUEST_SHORT_PROTO_TOPIC_PREFIX = DEVICE_ATTRIBUTES_REQUEST_SHORT_TOPIC_PREFIX + PROTO_SHORT + "/";

    /**
     * 功能：创建 `MqttTopics` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    private MqttTopics() {
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`MqttTopics` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
