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
package org.thingsboard.server.transport.lwm2m.server.store.util;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.google.protobuf.util.JsonFormat;
import lombok.SneakyThrows;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mNodeException;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.util.datatype.ULong;
import org.eclipse.leshan.server.redis.serialization.RegistrationSerDes;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MClientState;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.ResourceValue;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 中文说明：
 * 1. 类目的：`LwM2MClientSerDes` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class LwM2MClientSerDes {
    /**
     * 字段说明：
     * 1. 保存 `VALUE` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String VALUE = "value";

    @SneakyThrows
    /**
     * 方法说明：
     * 1. 职责：执行 `serialize` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static byte[] serialize(LwM2mClient client) {
        JsonObject o = Json.object();
        o.add("nodeId", client.getNodeId());
        o.add("endpoint", client.getEndpoint());

        JsonObject resources = Json.object();
        client.getResources().forEach((k, v) -> {
            JsonObject resourceValue = Json.object();
            resourceValue.add("lwM2mResource", serialize(v.getLwM2mResource()));
            resourceValue.add("resourceModel", serialize(v.getResourceModel()));
            resources.add(k, resourceValue);
        });
        o.add("resources", resources);
        JsonObject sharedAttributes = Json.object();

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        for (Map.Entry<String, TransportProtos.TsKvProto> entry : client.getSharedAttributes().entrySet()) {
            sharedAttributes.add(entry.getKey(), JsonFormat.printer().print(entry.getValue()));
        }

        o.add("sharedAttributes", sharedAttributes);
        JsonObject keyTsLatestMap = Json.object();
        client.getKeyTsLatestMap().forEach((k, v) -> {
            keyTsLatestMap.add(k, v.get());
        });
        o.add("keyTsLatestMap", keyTsLatestMap);

        o.add("state", client.getState().toString());

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (client.getSession() != null) {
            o.add("session", JsonFormat.printer().print(client.getSession()));
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (client.getTenantId() != null) {
            o.add("tenantId", client.getTenantId().toString());
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (client.getDeviceId() != null) {
            o.add("deviceId", client.getDeviceId().toString());
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (client.getProfileId() != null) {
            o.add("profileId", client.getProfileId().toString());
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (client.getPowerMode() != null) {
            o.add("powerMode", client.getPowerMode().toString());
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (client.getEdrxCycle() != null) {
            o.add("edrxCycle", client.getEdrxCycle());
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (client.getPsmActivityTimer() != null) {
            o.add("psmActivityTimer", client.getPsmActivityTimer());
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (client.getPagingTransmissionWindow() != null) {
            o.add("pagingTransmissionWindow", client.getPagingTransmissionWindow());
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (client.getRegistration() != null) {
            o.add("registration", RegistrationSerDes.jSerialize(client.getRegistration()));
        }
        o.add("asleep", client.isAsleep());
        o.add("lastUplinkTime", client.getLastUplinkTime());

        Field firstEdrxDownlink = LwM2mClient.class.getDeclaredField("firstEdrxDownlink");
        firstEdrxDownlink.setAccessible(true);
        o.add("firstEdrxDownlink", (boolean) firstEdrxDownlink.get(client));
        o.add("retryAttempts", client.getRetryAttempts().get());

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (client.getLastSentRpcId() != null) {
            o.add("lastSentRpcId", client.getLastSentRpcId().toString());
        }

        return o.toString().getBytes();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `serialize` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static JsonObject serialize(LwM2mResource resource) {
        JsonObject o = Json.object();
        o.add("id", resource.getId());
        o.add("type", resource.getType().toString());
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (resource.isMultiInstances()) {
            o.add("multiInstances", true);
            JsonObject instances = Json.object();
            resource.getInstances().forEach((id, in) -> {
                JsonObject instance = Json.object();
                instance.add("id", in.getId());
                addValue(instance, in.getType(), in.getValue());
                instances.add(id.toString(), instance);
            });
            o.add("instances", instances);
        } else {
            o.add("multiInstances", false);
            addValue(o, resource.getType(), resource.getValue());
        }

        return o;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `parseLwM2mResource` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static LwM2mResource parseLwM2mResource(JsonObject o) {
        boolean multiInstances = o.get("multiInstances").asBoolean();
        int id = o.get("id").asInt();
        ResourceModel.Type type = ResourceModel.Type.valueOf(o.get("type").asString());
        if (multiInstances) {
            Map<Integer, Object> instances = new HashMap<>();
            o.get("instances").asObject().forEach(entry -> {
                instances.put(Integer.valueOf(entry.getName()), parseValue(type, entry.getValue().asObject().get("value")));
            });
            return LwM2mMultipleResource.newResource(id, instances, type);
        } else {
            return LwM2mSingleResource.newResource(id, parseValue(type, o.get(VALUE)));
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `parseValue` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static Object parseValue(ResourceModel.Type type, JsonValue value) {
        switch (type) {
            case INTEGER:
                return value.asLong();
            case FLOAT:
                return value.asDouble();
            case BOOLEAN:
                return value.asBoolean();
            case OPAQUE:
                return Base64.getDecoder().decode(value.asString());
            case STRING:
                return value.asString();
            case TIME:
                return new Date(value.asLong());
            case OBJLNK:
                return ObjectLink.decodeFromString(value.asString());
            case UNSIGNED_INTEGER:
                return ULong.valueOf(value.asString());
            default:
                throw new LwM2mNodeException(String.format("Type %s is not supported", type.name()));
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `serialize` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static JsonObject serialize(ResourceModel resourceModel) {
        JsonObject o = Json.object();
        o.add("id", resourceModel.id);
        o.add("name", resourceModel.name);
        o.add("operations", resourceModel.operations.toString());
        o.add("multiple", resourceModel.multiple);
        o.add("mandatory", resourceModel.mandatory);
        o.add("type", resourceModel.type.toString());
        o.add("rangeEnumeration", resourceModel.rangeEnumeration);
        o.add("units", resourceModel.units);
        o.add("description", resourceModel.description);
        return o;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `parseResourceModel` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static ResourceModel parseResourceModel(JsonObject o) {
        Integer id = o.get("id").asInt();
        String name = o.get("name").asString();
        ResourceModel.Operations operations = ResourceModel.Operations.valueOf(o.get("operations").asString());
        Boolean multiple = o.get("multiple").asBoolean();
        Boolean mandatory = o.get("mandatory").asBoolean();
        ResourceModel.Type type = ResourceModel.Type.valueOf(o.get("type").asString());
        String rangeEnumeration = o.get("rangeEnumeration").asString();
        String units = o.get("units").asString();
        String description = o.get("description").asString();
        return new ResourceModel(id, name, operations, multiple, mandatory, type, rangeEnumeration, units, description);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `addValue` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static void addValue(JsonObject o, ResourceModel.Type type, Object value) {
        switch (type) {
            case INTEGER:
                o.add(VALUE, (Long) value);
                break;
            case FLOAT:
                o.add(VALUE, (Double) value);
                break;
            case BOOLEAN:
                o.add(VALUE, (Boolean) value);
                break;
            case OPAQUE:
                o.add(VALUE, Base64.getEncoder().encodeToString((byte[]) value));
                break;
            case STRING:
                o.add(VALUE, (String) value);
                break;
            case TIME:
                o.add(VALUE, ((Date) value).getTime());
                break;
            case OBJLNK:
                o.add(VALUE, ((ObjectLink) value).encodeToString());
                break;
            case UNSIGNED_INTEGER:
                o.add(VALUE, value.toString());
                break;
            default:
                throw new LwM2mNodeException(String.format("Type %s is not supported", type.name()));
        }
    }

    @SneakyThrows
    /**
     * 方法说明：
     * 1. 职责：执行 `deserialize` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static LwM2mClient deserialize(byte[] data) {
        JsonObject o = Json.parse(new String(data)).asObject();
        LwM2mClient lwM2mClient = new LwM2mClient(o.get("nodeId").asString(), o.get("endpoint").asString());

        o.get("resources").asObject().forEach(entry -> {
            JsonObject resource = entry.getValue().asObject();
            LwM2mResource lwM2mResource = parseLwM2mResource(resource.get("lwM2mResource").asObject());
            ResourceModel resourceModel = parseResourceModel(resource.get("resourceModel").asObject());
            ResourceValue resourceValue = new ResourceValue(lwM2mResource, resourceModel);
            lwM2mClient.getResources().put(entry.getName(), resourceValue);
        });

        for (JsonObject.Member entry : o.get("sharedAttributes").asObject()) {
            TransportProtos.TsKvProto.Builder builder = TransportProtos.TsKvProto.newBuilder();
            JsonFormat.parser().merge(entry.getValue().asString(), builder);
            lwM2mClient.getSharedAttributes().put(entry.getName(), builder.build());
        }

        o.get("keyTsLatestMap").asObject().forEach(entry -> {
            lwM2mClient.getKeyTsLatestMap().put(entry.getName(), new AtomicLong(entry.getValue().asLong()));
        });

        lwM2mClient.setState(LwM2MClientState.valueOf(o.get("state").asString()));

        Class<LwM2mClient> lwM2mClientClass = LwM2mClient.class;

        JsonValue session = o.get("session");
        if (session != null) {
            TransportProtos.SessionInfoProto.Builder builder = TransportProtos.SessionInfoProto.newBuilder();
            JsonFormat.parser().merge(session.asString(), builder);

            Field sessionField = lwM2mClientClass.getDeclaredField("session");
            sessionField.setAccessible(true);
            sessionField.set(lwM2mClient, builder.build());
        }

        JsonValue tenantId = o.get("tenantId");
        if (tenantId != null) {
            Field tenantIdField = lwM2mClientClass.getDeclaredField("tenantId");
            tenantIdField.setAccessible(true);
            tenantIdField.set(lwM2mClient, new TenantId(UUID.fromString(tenantId.asString())));
        }

        JsonValue deviceId = o.get("deviceId");
        if (tenantId != null) {
            Field deviceIdField = lwM2mClientClass.getDeclaredField("deviceId");
            deviceIdField.setAccessible(true);
            deviceIdField.set(lwM2mClient, UUID.fromString(deviceId.asString()));
        }

        JsonValue profileId = o.get("profileId");
        if (tenantId != null) {
            Field profileIdField = lwM2mClientClass.getDeclaredField("profileId");
            profileIdField.setAccessible(true);
            profileIdField.set(lwM2mClient, UUID.fromString(profileId.asString()));
        }

        JsonValue powerMode = o.get("powerMode");
        if (powerMode != null) {
            Field powerModeField = lwM2mClientClass.getDeclaredField("powerMode");
            powerModeField.setAccessible(true);
            powerModeField.set(lwM2mClient, PowerMode.valueOf(powerMode.asString()));
        }

        JsonValue edrxCycle = o.get("edrxCycle");
        if (edrxCycle != null) {
            Field edrxCycleField = lwM2mClientClass.getDeclaredField("edrxCycle");
            edrxCycleField.setAccessible(true);
            edrxCycleField.set(lwM2mClient, edrxCycle.asLong());
        }

        JsonValue psmActivityTimer = o.get("psmActivityTimer");
        if (psmActivityTimer != null) {
            Field psmActivityTimerField = lwM2mClientClass.getDeclaredField("psmActivityTimer");
            psmActivityTimerField.setAccessible(true);
            psmActivityTimerField.set(lwM2mClient, psmActivityTimer.asLong());
        }

        JsonValue pagingTransmissionWindow = o.get("pagingTransmissionWindow");
        if (pagingTransmissionWindow != null) {
            Field pagingTransmissionWindowField = lwM2mClientClass.getDeclaredField("pagingTransmissionWindow");
            pagingTransmissionWindowField.setAccessible(true);
            pagingTransmissionWindowField.set(lwM2mClient, pagingTransmissionWindow.asLong());
        }

        JsonValue registration = o.get("registration");
        if (registration != null) {
            lwM2mClient.setRegistration(RegistrationSerDes.deserialize(registration.asObject()));
        }

        lwM2mClient.setAsleep(o.get("asleep").asBoolean());

        Field lastUplinkTimeField = lwM2mClientClass.getDeclaredField("lastUplinkTime");
        lastUplinkTimeField.setAccessible(true);
        lastUplinkTimeField.setLong(lwM2mClient, o.get("lastUplinkTime").asLong());

        Field firstEdrxDownlinkField = lwM2mClientClass.getDeclaredField("firstEdrxDownlink");
        firstEdrxDownlinkField.setAccessible(true);
        firstEdrxDownlinkField.setBoolean(lwM2mClient, o.get("firstEdrxDownlink").asBoolean());

        lwM2mClient.getRetryAttempts().set(o.get("retryAttempts").asInt());

        JsonValue lastSentRpcId = o.get("lastSentRpcId");
        if (lastSentRpcId != null) {
            lwM2mClient.setLastSentRpcId(UUID.fromString(lastSentRpcId.asString()));
        }

        return lwM2mClient;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2MClientSerDes` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
