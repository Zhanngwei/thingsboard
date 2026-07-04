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
package org.thingsboard.server.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKey;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.edge.EdgeEventUpdateMsg;
import org.thingsboard.server.common.msg.edge.FromEdgeSyncResponse;
import org.thingsboard.server.common.msg.edge.ToEdgeSyncRequest;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponseActorMsg;
import org.thingsboard.server.common.msg.rpc.RemoveRpcActorMsg;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequestActorMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceCredentialsUpdateNotificationMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceEdgeUpdateMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceNameOrTypeUpdateMsg;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 中文说明：
 * 1. 类目的：`ProtoUtilsTest` 是ThingsBoard Common 测试模块中的公共工具类型，用于提供跨模块复用的纯函数、解析、转换或辅助逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Application、DAO、Transport、Rule Engine、测试工具和第三方库。
 * 4. 生命周期：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Utility / Helper。
 */
class ProtoUtilsTest {

    TenantId tenantId = TenantId.fromUUID(UUID.fromString("35e10f77-16e7-424d-ae46-ee780f87ac4f"));
    EntityId entityId = new RuleChainId(UUID.fromString("c640b635-4f0f-41e6-b10b-25a86003094e"));
    DeviceId deviceId = new DeviceId(UUID.fromString("ceebb9e5-4239-437c-a507-dc5f71f1232d"));
    EdgeId edgeId = new EdgeId(UUID.fromString("364be452-2183-459b-af93-1ddb325feac1"));
    UUID id = UUID.fromString("31a07d85-6ed5-46f8-83c0-6715cb0a8782");

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `protoComponentLifecycleSerialization` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void protoComponentLifecycleSerialization() {
        ComponentLifecycleMsg msg = new ComponentLifecycleMsg(tenantId, entityId, ComponentLifecycleEvent.UPDATED);
        assertThat(ProtoUtils.fromProto(ProtoUtils.toProto(msg))).as("deserialized").isEqualTo(msg);
        msg = new ComponentLifecycleMsg(tenantId, entityId, ComponentLifecycleEvent.STARTED);
        assertThat(ProtoUtils.fromProto(ProtoUtils.toProto(msg))).as("deserialized").isEqualTo(msg);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `protoEntityTypeSerialization` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void protoEntityTypeSerialization() {
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (EntityType entityType : EntityType.values()) {
            assertThat(ProtoUtils.fromProto(ProtoUtils.toProto(entityType))).as(entityType.getNormalName()).isEqualTo(entityType);
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `protoEdgeEventUpdateSerialization` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void protoEdgeEventUpdateSerialization() {
        EdgeEventUpdateMsg msg = new EdgeEventUpdateMsg(tenantId, edgeId);
        assertThat(ProtoUtils.fromProto(ProtoUtils.toProto(msg))).as("deserialized").isEqualTo(msg);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `protoToEdgeSyncRequestSerialization` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void protoToEdgeSyncRequestSerialization() {
        ToEdgeSyncRequest msg = new ToEdgeSyncRequest(id, tenantId, edgeId);
        assertThat(ProtoUtils.fromProto(ProtoUtils.toProto(msg))).as("deserialized").isEqualTo(msg);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `protoFromEdgeSyncResponseSerialization` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void protoFromEdgeSyncResponseSerialization() {
        FromEdgeSyncResponse msg = new FromEdgeSyncResponse(id, tenantId, edgeId, true);
        assertThat(ProtoUtils.fromProto(ProtoUtils.toProto(msg))).as("deserialized").isEqualTo(msg);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `protoDeviceEdgeUpdateSerialization` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void protoDeviceEdgeUpdateSerialization() {
        DeviceEdgeUpdateMsg msg = new DeviceEdgeUpdateMsg(tenantId, deviceId, edgeId);
        // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
        TransportProtos.ToDeviceActorNotificationMsgProto serializedMsg = ProtoUtils.toProto(msg);
        Assertions.assertNotNull(serializedMsg);
        assertThat(ProtoUtils.fromProto(serializedMsg)).as("deserialized").isEqualTo(msg);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `protoDeviceNameOrTypeSerialization` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void protoDeviceNameOrTypeSerialization() {
        String deviceName = "test", deviceType = "test";
        DeviceNameOrTypeUpdateMsg msg = new DeviceNameOrTypeUpdateMsg(tenantId, deviceId, deviceName, deviceType);
        // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
        TransportProtos.ToDeviceActorNotificationMsgProto serializedMsg = ProtoUtils.toProto(msg);
        Assertions.assertNotNull(serializedMsg);
        assertThat(ProtoUtils.fromProto(serializedMsg)).as("deserialized").isEqualTo(msg);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `protoDeviceAttributesEventSerialization` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void protoDeviceAttributesEventSerialization() {
        DeviceAttributesEventNotificationMsg msg = new DeviceAttributesEventNotificationMsg(tenantId, deviceId, null, "CLIENT_SCOPE",
                List.of(new BaseAttributeKvEntry(System.currentTimeMillis(), new StringDataEntry("key", "value"))), false);
        // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
        TransportProtos.ToDeviceActorNotificationMsgProto serializedMsg = ProtoUtils.toProto(msg);
        Assertions.assertNotNull(serializedMsg);
        assertThat(ProtoUtils.fromProto(serializedMsg)).as("deserialized").isEqualTo(msg);

        msg = new DeviceAttributesEventNotificationMsg(tenantId, deviceId, null, "SERVER_SCOPE",
                List.of(new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("doubleEntry", 231.5)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new JsonDataEntry("jsonEntry", "jsonValue"))), false);
        serializedMsg = ProtoUtils.toProto(msg);
        Assertions.assertNotNull(serializedMsg);
        assertThat(ProtoUtils.fromProto(serializedMsg)).as("deserialized").isEqualTo(msg);

        msg = new DeviceAttributesEventNotificationMsg(tenantId, deviceId, null, "SERVER_SCOPE",
                List.of(new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("entry", 11.3)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new BooleanDataEntry("jsonEntry", true))), false);
        serializedMsg = ProtoUtils.toProto(msg);
        Assertions.assertNotNull(serializedMsg);
        assertThat(ProtoUtils.fromProto(serializedMsg)).as("deserialized").isEqualTo(msg);

        msg = new DeviceAttributesEventNotificationMsg(tenantId, deviceId, Set.of(new AttributeKey("SHARED_SCOPE", "attributeKey")), null, null, true);
        serializedMsg = ProtoUtils.toProto(msg);
        Assertions.assertNotNull(serializedMsg);
        assertThat(ProtoUtils.fromProto(serializedMsg)).as("deserialized").isEqualTo(msg);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `protoDeviceCredentialsUpdateSerialization` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void protoDeviceCredentialsUpdateSerialization() {
        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setDeviceId(deviceId);
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsValue("test");
        deviceCredentials.setCredentialsId("test");
        DeviceCredentialsUpdateNotificationMsg msg = new DeviceCredentialsUpdateNotificationMsg(tenantId, deviceId, deviceCredentials);
        // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
        TransportProtos.ToDeviceActorNotificationMsgProto serializedMsg = ProtoUtils.toProto(msg);
        Assertions.assertNotNull(serializedMsg);
        assertThat(ProtoUtils.fromProto(serializedMsg)).as("deserialized").isEqualTo(msg);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `protoToDeviceRpcRequestSerialization` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void protoToDeviceRpcRequestSerialization() {
        String serviceId = "cadcaac6-85c3-4211-9756-f074dcd1e7f7";
        ToDeviceRpcRequest request = new ToDeviceRpcRequest(id, tenantId, deviceId, true, 0, new ToDeviceRpcRequestBody("method", "params"), false, 0, "");
        // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
        ToDeviceRpcRequestActorMsg msg = new ToDeviceRpcRequestActorMsg(serviceId, request);
        // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
        TransportProtos.ToDeviceActorNotificationMsgProto serializedMsg = ProtoUtils.toProto(msg);
        Assertions.assertNotNull(serializedMsg);
        assertThat(ProtoUtils.fromProto(serializedMsg)).as("deserialized").isEqualTo(msg);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `protoFromDeviceRpcResponseSerialization` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void protoFromDeviceRpcResponseSerialization() {
        // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
        FromDeviceRpcResponseActorMsg msg = new FromDeviceRpcResponseActorMsg(23, tenantId, deviceId, new FromDeviceRpcResponse(id, "response", RpcError.NOT_FOUND));
        // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
        TransportProtos.ToDeviceActorNotificationMsgProto serializedMsg = ProtoUtils.toProto(msg);
        Assertions.assertNotNull(serializedMsg);
        assertThat(ProtoUtils.fromProto(serializedMsg)).as("deserialized").isEqualTo(msg);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `protoRemoveRpcActorSerialization` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void protoRemoveRpcActorSerialization() {
        // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
        RemoveRpcActorMsg msg = new RemoveRpcActorMsg(tenantId, deviceId, id);
        // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
        TransportProtos.ToDeviceActorNotificationMsgProto serializedMsg = ProtoUtils.toProto(msg);
        Assertions.assertNotNull(serializedMsg);
        assertThat(ProtoUtils.fromProto(serializedMsg)).as("deserialized").isEqualTo(msg);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`ProtoUtilsTest` 在 ThingsBoard Common 测试模块 中承担公共工具类型职责，核心目的是提供跨模块复用的纯函数、解析、转换或辅助逻辑。
 * 2. 核心流程：接收输入参数后执行本地转换、校验或解析并返回结果。
 * 3. 关键依赖：主要依赖或协作对象包括Application、DAO、Transport、Rule Engine、测试工具和第三方库。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
