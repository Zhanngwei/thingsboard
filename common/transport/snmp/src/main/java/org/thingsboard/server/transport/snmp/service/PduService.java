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
package org.thingsboard.server.transport.snmp.service;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.transport.snmp.SnmpMapping;
import org.thingsboard.server.common.data.transport.snmp.SnmpMethod;
import org.thingsboard.server.common.data.transport.snmp.SnmpProtocolVersion;
import org.thingsboard.server.common.data.transport.snmp.config.SnmpCommunicationConfig;
import org.thingsboard.server.common.data.util.TypeCastUtil;
import org.thingsboard.server.queue.util.TbSnmpTransportComponent;
import org.thingsboard.server.transport.snmp.session.DeviceSessionContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@TbSnmpTransportComponent
@Service
@Slf4j
@RequiredArgsConstructor
/**
 * 中文说明：
 * 1. 类目的：`PduService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class PduService {

    @Value("${transport.snmp.max_request_oids:100}")
    /**
     * 字段说明：
     * 1. 保存 `maxRequestOids` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int maxRequestOids;

    @Value("${transport.snmp.response.ignore_type_cast_errors:false}")
    /**
     * 字段说明：
     * 1. 保存 `ignoreTypeCastErrors` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private boolean ignoreTypeCastErrors;

    /**
     * 方法说明：
     * 1. 职责：执行 `createPdus` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<PDU> createPdus(DeviceSessionContext sessionContext, SnmpCommunicationConfig communicationConfig, Map<String, String> values) {
        List<PDU> pdus = new ArrayList<>();
        List<SnmpMapping> allMappings = communicationConfig.getAllMappings();

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (List<SnmpMapping> mappings : Lists.partition(allMappings, maxRequestOids)) {
            PDU pdu = setUpPdu(sessionContext);
            pdu.setType(communicationConfig.getMethod().getCode());
            pdu.addAll(mappings.stream()
                    .filter(mapping -> values.isEmpty() || values.containsKey(mapping.getKey()))
                    .map(mapping -> Optional.ofNullable(values.get(mapping.getKey()))
                            .map(value -> {
                                Variable variable = toSnmpVariable(value, mapping.getDataType());
                                return new VariableBinding(new OID(mapping.getOid()), variable);
                            })
                            .orElseGet(() -> new VariableBinding(new OID(mapping.getOid()))))
                    .collect(Collectors.toList()));
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (pdu.size() > 0) {
                pdus.add(pdu);
            }
        }

        return pdus;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createSingleVariablePdu` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PDU createSingleVariablePdu(DeviceSessionContext sessionContext, SnmpMethod snmpMethod, String oid, String value, DataType dataType) {
        PDU pdu = setUpPdu(sessionContext);
        pdu.setType(snmpMethod.getCode());

        Variable variable = value == null ? Null.instance : toSnmpVariable(value, dataType);
        pdu.add(new VariableBinding(new OID(oid), variable));

        return pdu;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `toSnmpVariable` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Variable toSnmpVariable(String value, DataType dataType) {
        dataType = dataType == null ? DataType.STRING : dataType;
        Variable variable;
        // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
        switch (dataType) {
            case LONG:
                try {
                    variable = new Integer32(Integer.parseInt(value));
                    break;
                // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
                } catch (NumberFormatException ignored) {
                }
            case DOUBLE:
            case BOOLEAN:
            case STRING:
            case JSON:
            default:
                variable = new OctetString(value);
        }
        return variable;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `setUpPdu` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private PDU setUpPdu(DeviceSessionContext sessionContext) {
        PDU pdu;
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        SnmpDeviceTransportConfiguration deviceTransportConfiguration = sessionContext.getDeviceTransportConfiguration();
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        SnmpProtocolVersion snmpVersion = deviceTransportConfiguration.getProtocolVersion();
        // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
        switch (snmpVersion) {
            case V1:
            case V2C:
                pdu = new PDU();
                break;
            case V3:
                ScopedPDU scopedPdu = new ScopedPDU();
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                scopedPdu.setContextName(new OctetString(deviceTransportConfiguration.getContextName()));
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                scopedPdu.setContextEngineID(new OctetString(deviceTransportConfiguration.getEngineId()));
                pdu = scopedPdu;
                break;
            default:
                throw new UnsupportedOperationException("SNMP version " + snmpVersion + " is not supported");
        }
        return pdu;
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `processPdus` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public JsonObject processPdus(List<PDU> pdus, List<SnmpMapping> responseMappings) {
        Map<OID, String> values = processPdus(pdus);

        Map<OID, SnmpMapping> mappings = new HashMap<>();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (responseMappings != null) {
            // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
            for (SnmpMapping mapping : responseMappings) {
                OID oid = new OID(mapping.getOid());
                mappings.put(oid, mapping);
            }
        }

        JsonObject data = new JsonObject();
        values.forEach((oid, value) -> {
            log.trace("Processing variable binding: {} - {}", oid, value);

            SnmpMapping mapping = mappings.get(oid);
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (mapping == null) {
                log.debug("No SNMP mapping for oid {}", oid);
                return;
            }

            processValue(mapping.getKey(), mapping.getDataType(), value, data);
        });

        return data;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processPdus` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Map<OID, String> processPdus(List<PDU> pdus) {
        return pdus.stream()
                .flatMap(pdu -> pdu.getVariableBindings().stream())
                .filter(Objects::nonNull)
                .filter(variableBinding -> !(variableBinding.getVariable() instanceof Null))
                .collect(Collectors.toMap(VariableBinding::getOid, VariableBinding::toValueString));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processValue` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void processValue(String key, DataType dataType, String value, JsonObject result) {
        try {
            switch (dataType) {
                case STRING:
                case JSON:
                    result.addProperty(key, value);
                    break;
                case LONG:
                case DOUBLE:
                    result.addProperty(key, TypeCastUtil.castToNumber(value).getValue());
                    break;
                case BOOLEAN:
                    if (StringUtils.equalsAnyIgnoreCase(value, "true", "false")) {
                        result.addProperty(key, Boolean.parseBoolean(value));
                    } else {
                        throw new IllegalArgumentException("Can't parse '" + value + "' as boolean");
                    }
                    break;
            }
        } catch (IllegalArgumentException e) {
            if (ignoreTypeCastErrors) {
                log.debug("Ignoring value '{}' for key '{}' because of data type mismatch ({} required)", value, key, dataType);
            } else {
                throw e;
            }
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`PduService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
