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
package org.thingsboard.server.common.data;

import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.github.os72.protobuf.dynamic.EnumDefinition;
import com.github.os72.protobuf.dynamic.MessageDefinition;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.squareup.wire.Syntax;
import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.internal.parser.EnumConstantElement;
import com.squareup.wire.schema.internal.parser.EnumElement;
import com.squareup.wire.schema.internal.parser.FieldElement;
import com.squareup.wire.schema.internal.parser.MessageElement;
import com.squareup.wire.schema.internal.parser.OneOfElement;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.ProtoParser;
import com.squareup.wire.schema.internal.parser.TypeElement;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`DynamicProtoUtils` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
public class DynamicProtoUtils {

    public static final Location LOCATION = new Location("", "", -1, -1);
    /**
     * 字段说明：
     * 1. 保存 `PROTO_3_SYNTAX` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String PROTO_3_SYNTAX = "proto3";

    /**
     * 方法说明：
     * 1. 职责：执行 `getDescriptor` 对应的公共数据模型类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static Descriptors.Descriptor getDescriptor(String protoSchema, String schemaName) {
        try {
            DynamicMessage.Builder builder = getDynamicMessageBuilder(protoSchema, schemaName);
            return builder.getDescriptorForType();
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Exception e) {
            log.warn("Failed to get Message Descriptor due to {}", e.getMessage());
            return null;
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getDynamicMessageBuilder` 对应的公共数据模型类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static DynamicMessage.Builder getDynamicMessageBuilder(String protoSchema, String schemaName) {
        ProtoFileElement protoFileElement = getProtoFileElement(protoSchema);
        DynamicSchema dynamicSchema = getDynamicSchema(protoFileElement, schemaName);
        String lastMsgName = getMessageTypes(protoFileElement.getTypes()).stream()
                .map(MessageElement::getName).reduce((previous, last) -> last).get();
        return dynamicSchema.newMessageBuilder(lastMsgName);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getDynamicSchema` 对应的公共数据模型类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static DynamicSchema getDynamicSchema(ProtoFileElement protoFileElement, String schemaName) {
        DynamicSchema.Builder schemaBuilder = DynamicSchema.newBuilder();
        schemaBuilder.setName(schemaName);
        schemaBuilder.setSyntax(PROTO_3_SYNTAX);
        schemaBuilder.setPackage(StringUtils.isNotEmpty(protoFileElement.getPackageName()) ?
                protoFileElement.getPackageName() : schemaName.toLowerCase());
        List<TypeElement> types = protoFileElement.getTypes();
        List<MessageElement> messageTypes = getMessageTypes(types);

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!messageTypes.isEmpty()) {
            List<EnumElement> enumTypes = getEnumElements(types);
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (!enumTypes.isEmpty()) {
                enumTypes.forEach(enumElement -> {
                    EnumDefinition enumDefinition = getEnumDefinition(enumElement);
                    schemaBuilder.addEnumDefinition(enumDefinition);
                });
            }
            List<MessageDefinition> messageDefinitions = getMessageDefinitions(messageTypes);
            messageDefinitions.forEach(schemaBuilder::addMessageDefinition);
            try {
                return schemaBuilder.build();
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (Descriptors.DescriptorValidationException e) {
                throw new RuntimeException("Failed to create dynamic schema due to: " + e.getMessage());
            }
        } else {
            throw new RuntimeException("Failed to get Dynamic Schema! Message types is empty for schema:" + schemaName);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getProtoFileElement` 对应的公共数据模型类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static ProtoFileElement getProtoFileElement(String protoSchema) {
        return new ProtoParser(LOCATION, protoSchema.toCharArray()).readProtoFile();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `dynamicMsgToJson` 对应的公共数据模型类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static String dynamicMsgToJson(Descriptors.Descriptor descriptor, byte[] payload) throws InvalidProtocolBufferException {
        DynamicMessage dynamicMessage = DynamicMessage.parseFrom(descriptor, payload);
        return JsonFormat.printer().includingDefaultValueFields().print(dynamicMessage);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `jsonToDynamicMessage` 对应的公共数据模型类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static DynamicMessage jsonToDynamicMessage(DynamicMessage.Builder builder, String payload) throws InvalidProtocolBufferException {
        JsonFormat.parser().ignoringUnknownFields().merge(payload, builder);
        return builder.build();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getMessageTypes` 对应的公共数据模型类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static List<MessageElement> getMessageTypes(List<TypeElement> types) {
        return types.stream()
                .filter(typeElement -> typeElement instanceof MessageElement)
                .map(typeElement -> (MessageElement) typeElement)
                .collect(Collectors.toList());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getEnumElements` 对应的公共数据模型类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static List<EnumElement> getEnumElements(List<TypeElement> types) {
        return types.stream()
                .filter(typeElement -> typeElement instanceof EnumElement)
                .map(typeElement -> (EnumElement) typeElement)
                .collect(Collectors.toList());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getMessageDefinitions` 对应的公共数据模型类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static List<MessageDefinition> getMessageDefinitions(List<MessageElement> messageElementsList) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!messageElementsList.isEmpty()) {
            List<MessageDefinition> messageDefinitions = new ArrayList<>();
            messageElementsList.forEach(messageElement -> {
                MessageDefinition.Builder messageDefinitionBuilder = MessageDefinition.newBuilder(messageElement.getName());

                List<TypeElement> nestedTypes = messageElement.getNestedTypes();
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (!nestedTypes.isEmpty()) {
                    List<EnumElement> nestedEnumTypes = getEnumElements(nestedTypes);
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (!nestedEnumTypes.isEmpty()) {
                        nestedEnumTypes.forEach(enumElement -> {
                            EnumDefinition nestedEnumDefinition = getEnumDefinition(enumElement);
                            messageDefinitionBuilder.addEnumDefinition(nestedEnumDefinition);
                        });
                    }
                    List<MessageElement> nestedMessageTypes = getMessageTypes(nestedTypes);
                    List<MessageDefinition> nestedMessageDefinitions = getMessageDefinitions(nestedMessageTypes);
                    nestedMessageDefinitions.forEach(messageDefinitionBuilder::addMessageDefinition);
                }
                List<FieldElement> messageElementFields = messageElement.getFields();
                List<OneOfElement> oneOfs = messageElement.getOneOfs();
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (!oneOfs.isEmpty()) {
                    // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
                    for (OneOfElement oneOfelement : oneOfs) {
                        MessageDefinition.OneofBuilder oneofBuilder = messageDefinitionBuilder.addOneof(oneOfelement.getName());
                        addMessageFieldsToTheOneOfDefinition(oneOfelement.getFields(), oneofBuilder);
                    }
                }
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (!messageElementFields.isEmpty()) {
                    addMessageFieldsToTheMessageDefinition(messageElementFields, messageDefinitionBuilder);
                }
                messageDefinitions.add(messageDefinitionBuilder.build());
            });
            return messageDefinitions;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getEnumDefinition` 对应的公共数据模型类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static EnumDefinition getEnumDefinition(EnumElement enumElement) {
        List<EnumConstantElement> enumElementTypeConstants = enumElement.getConstants();
        EnumDefinition.Builder enumDefinitionBuilder = EnumDefinition.newBuilder(enumElement.getName());
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!enumElementTypeConstants.isEmpty()) {
            enumElementTypeConstants.forEach(constantElement -> enumDefinitionBuilder.addValue(constantElement.getName(), constantElement.getTag()));
        }
        return enumDefinitionBuilder.build();
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `addMessageFieldsToTheMessageDefinition` 对应的公共数据模型类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static void addMessageFieldsToTheMessageDefinition(List<FieldElement> messageElementFields, MessageDefinition.Builder messageDefinitionBuilder) {
        messageElementFields.forEach(fieldElement -> {
            String labelStr = null;
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (fieldElement.getLabel() != null) {
                labelStr = fieldElement.getLabel().name().toLowerCase();
            }
            messageDefinitionBuilder.addField(
                    labelStr,
                    fieldElement.getType(),
                    fieldElement.getName(),
                    fieldElement.getTag());
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `addMessageFieldsToTheOneOfDefinition` 对应的公共数据模型类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static void addMessageFieldsToTheOneOfDefinition(List<FieldElement> oneOfsElementFields, MessageDefinition.OneofBuilder oneofBuilder) {
        oneOfsElementFields.forEach(fieldElement -> oneofBuilder.addField(
                fieldElement.getType(),
                fieldElement.getName(),
                fieldElement.getTag()));
        oneofBuilder.msgDefBuilder();
    }

    // validation

    /**
     * 方法说明：
     * 1. 职责：执行 `validateProtoSchema` 对应的公共数据模型类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static void validateProtoSchema(String schema, String schemaName, String exceptionPrefix) throws IllegalArgumentException {
        ProtoParser schemaParser = new ProtoParser(LOCATION, schema.toCharArray());
        ProtoFileElement protoFileElement;
        try {
            protoFileElement = schemaParser.readProtoFile();
        } catch (Exception e) {
            throw new IllegalArgumentException(exceptionPrefix + " failed to parse " + schemaName + " due to: " + e.getMessage());
        }
        checkProtoFileSyntax(schemaName, protoFileElement);
        checkProtoFileCommonSettings(schemaName, protoFileElement.getOptions().isEmpty(), " Schema options don't support!", exceptionPrefix);
        checkProtoFileCommonSettings(schemaName, protoFileElement.getPublicImports().isEmpty(), " Schema public imports don't support!", exceptionPrefix);
        checkProtoFileCommonSettings(schemaName, protoFileElement.getImports().isEmpty(), " Schema imports don't support!", exceptionPrefix);
        checkProtoFileCommonSettings(schemaName, protoFileElement.getExtendDeclarations().isEmpty(), " Schema extend declarations don't support!", exceptionPrefix);
        checkTypeElements(schemaName, protoFileElement, exceptionPrefix);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `checkProtoFileSyntax` 对应的公共数据模型类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static void checkProtoFileSyntax(String schemaName, ProtoFileElement protoFileElement) {
        if (protoFileElement.getSyntax() == null || !protoFileElement.getSyntax().equals(Syntax.PROTO_3)) {
            throw new IllegalArgumentException("[Transport Configuration] invalid schema syntax: " + protoFileElement.getSyntax() +
                    " for " + schemaName + " provided! Only " + Syntax.PROTO_3 + " allowed!");
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `checkProtoFileCommonSettings` 对应的公共数据模型类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static void checkProtoFileCommonSettings(String schemaName, boolean isEmptySettings, String invalidSettingsMessage, String exceptionPrefix) {
        if (!isEmptySettings) {
            throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + invalidSettingsMessage);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `checkTypeElements` 对应的公共数据模型类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static void checkTypeElements(String schemaName, ProtoFileElement protoFileElement, String exceptionPrefix) {
        List<TypeElement> types = protoFileElement.getTypes();
        if (!types.isEmpty()) {
            if (types.stream().noneMatch(typeElement -> typeElement instanceof MessageElement)) {
                throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " At least one Message definition should exists!");
            } else {
                checkEnumElements(schemaName, getEnumElements(types), exceptionPrefix);
                checkMessageElements(schemaName, getMessageTypes(types), exceptionPrefix);
            }
        } else {
            throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " Type elements is empty!");
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `checkFieldElements` 对应的公共数据模型类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static void checkFieldElements(String schemaName, List<FieldElement> fieldElements, String exceptionPrefix) {
        if (!fieldElements.isEmpty()) {
            boolean hasRequiredLabel = fieldElements.stream().anyMatch(fieldElement -> {
                Field.Label label = fieldElement.getLabel();
                return label != null && label.equals(Field.Label.REQUIRED);
            });
            if (hasRequiredLabel) {
                throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " Required labels are not supported!");
            }
            boolean hasDefaultValue = fieldElements.stream().anyMatch(fieldElement -> fieldElement.getDefaultValue() != null);
            if (hasDefaultValue) {
                throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " Default values are not supported!");
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `checkEnumElements` 对应的公共数据模型类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static void checkEnumElements(String schemaName, List<EnumElement> enumTypes, String exceptionPrefix) {
        if (enumTypes.stream().anyMatch(enumElement -> !enumElement.getNestedTypes().isEmpty())) {
            throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " Nested types in Enum definitions are not supported!");
        }
        if (enumTypes.stream().anyMatch(enumElement -> !enumElement.getOptions().isEmpty())) {
            throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " Enum definitions options are not supported!");
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `checkMessageElements` 对应的公共数据模型类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static void checkMessageElements(String schemaName, List<MessageElement> messageElementsList, String exceptionPrefix) {
        if (!messageElementsList.isEmpty()) {
            messageElementsList.forEach(messageElement -> {
                checkProtoFileCommonSettings(schemaName, messageElement.getGroups().isEmpty(),
                        " Message definition groups don't support!", exceptionPrefix);
                checkProtoFileCommonSettings(schemaName, messageElement.getOptions().isEmpty(),
                        " Message definition options don't support!", exceptionPrefix);
                checkProtoFileCommonSettings(schemaName, messageElement.getExtensions().isEmpty(),
                        " Message definition extensions don't support!", exceptionPrefix);
                checkProtoFileCommonSettings(schemaName, messageElement.getReserveds().isEmpty(),
                        " Message definition reserved elements don't support!", exceptionPrefix);
                checkFieldElements(schemaName, messageElement.getFields(), exceptionPrefix);
                List<OneOfElement> oneOfs = messageElement.getOneOfs();
                if (!oneOfs.isEmpty()) {
                    oneOfs.forEach(oneOfElement -> {
                        checkProtoFileCommonSettings(schemaName, oneOfElement.getGroups().isEmpty(),
                                " OneOf definition groups don't support!", exceptionPrefix);
                        checkFieldElements(schemaName, oneOfElement.getFields(), exceptionPrefix);
                    });
                }
                List<TypeElement> nestedTypes = messageElement.getNestedTypes();
                if (!nestedTypes.isEmpty()) {
                    List<EnumElement> nestedEnumTypes = getEnumElements(nestedTypes);
                    if (!nestedEnumTypes.isEmpty()) {
                        checkEnumElements(schemaName, nestedEnumTypes, exceptionPrefix);
                    }
                    List<MessageElement> nestedMessageTypes = getMessageTypes(nestedTypes);
                    checkMessageElements(schemaName, nestedMessageTypes, exceptionPrefix);
                }
            });
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `invalidSchemaProvidedMessage` 对应的公共数据模型类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static String invalidSchemaProvidedMessage(String schemaName, String exceptionPrefix) {
        return exceptionPrefix + " invalid " + schemaName + " provided!";
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DynamicProtoUtils` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
