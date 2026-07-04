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
package org.thingsboard.server.transport.lwm2m.utils;

import com.google.api.client.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mValueConverter;
import org.eclipse.leshan.core.util.Hex;
import org.thingsboard.server.common.data.StringUtils;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE;

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`LwM2mValueConverterImpl` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class LwM2mValueConverterImpl implements LwM2mValueConverter {

    private static final LwM2mValueConverterImpl INSTANCE = new LwM2mValueConverterImpl();

    /**
     * 方法说明：
     * 1. 职责：执行 `getInstance` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static LwM2mValueConverterImpl getInstance() {
        return INSTANCE;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `convertValue` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Object convertValue(Object value, Type currentType, Type expectedType, LwM2mPath resourcePath)
            throws CodecException {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (value == null) {
           return null;
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (expectedType == null) {
            /** unknown resource, trusted value */
            return value;
        }

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (currentType == expectedType) {
            /** expected type */
            return value;
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (currentType == null) {
            currentType = OPAQUE;
        }

        // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
        switch (expectedType) {
            case INTEGER:
                // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
                switch (currentType) {
                    case FLOAT:
                        log.debug("Trying to convert float value [{}] to Integer", value);
                        Long longValue = ((Double) value).longValue();
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        if ((double) value == longValue.doubleValue()) {
                            return longValue;
                        }
                    case STRING:
                        log.debug("Trying to convert String value [{}] to Integer", value);
                        return Long.parseLong((String) value);
                    default:
                        break;
                }
                break;
            case FLOAT:
                // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
                switch (currentType) {
                    case INTEGER:
                        log.debug("Trying to convert integer value [{}] to float", value);
                        Double floatValue = ((Long) value).doubleValue();
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        if ((long) value == floatValue.longValue()) {
                            return floatValue;
                        }
                    case STRING:
                        log.debug("Trying to convert String value [{}] to Float", value);
                        return Float.valueOf((String) value);
                    default:
                        break;
                }
                break;
            case BOOLEAN:
                // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
                switch (currentType) {
                    case STRING:
                        log.debug("Trying to convert string value {} to boolean", value);
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        if (StringUtils.equalsIgnoreCase((String) value, "true")) {
                            return true;
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        } else if (StringUtils.equalsIgnoreCase((String) value, "false")) {
                            return false;
                        }
                        break;
                    case INTEGER:
                        log.debug("Trying to convert int value {} to boolean", value);
                        Long val = (Long) value;
                        if (val == 1) {
                            return true;
                        } else if (val == 0) {
                            return false;
                        }
                        break;
                    default:
                        break;
                }
                break;
            case TIME:
                switch (currentType) {
                    case INTEGER:
                        log.debug("Trying to convert long value {} to date", value);
                        /* let's assume we received the millisecond since 1970/1/1 */
                        return new Date(((Number) value).longValue() * 1000L);
                    case STRING:
                        log.debug("Trying to convert string value {} to date", value);
                        /** let's assume we received an ISO 8601 format date */
                        try {
                            return new Date(Long.decode(value.toString()));
                            /**
                            DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
                            XMLGregorianCalendar cal = datatypeFactory.newXMLGregorianCalendar((String) value);
                            return cal.toGregorianCalendar().getTime();
                             **/
                        } catch (IllegalArgumentException e) {
                            log.debug("Unable to convert string to date", e);
                            throw new CodecException("Unable to convert string (%s) to date for resource %s", value,
                                    resourcePath);
                        }
                    default:
                        break;
                }
                break;
            case STRING:
                switch (currentType) {
                    case BOOLEAN:
                    case INTEGER:
                    case FLOAT:
                        return String.valueOf(value);
                    case TIME:
                        String DATE_FORMAT = "MMM d, yyyy HH:mm a";
                        Long timeValue;
                        try {
                            timeValue = ((Date) value).getTime();
                        }
                        catch (Exception e){
                           timeValue = new BigInteger((byte [])value).longValue();
                        }
                        DateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
                        return formatter.format(new Date(timeValue));
                    case OPAQUE:
                        return Hex.encodeHexString((byte[])value);
                    case OBJLNK:
                        return ObjectLink.decodeFromString((String) value);
                    default:
                        break;
                }
                break;
            case OPAQUE:
                if (currentType == Type.STRING) {
                    /** let's assume we received an hexadecimal string */
                    log.debug("Trying to convert hexadecimal/base64 string [{}] to byte array", value);
                    try {
                        return Hex.decodeHex(((String)value).toCharArray());
                    } catch (IllegalArgumentException e) {
                        try {
                            return Base64.decodeBase64(((String) value).getBytes());
                        } catch (IllegalArgumentException ea) {
                            throw new CodecException("Unable to convert hexastring or base64 [%s] to byte array for resource %s",
                                    value, resourcePath);
                        }
                    }
                }
                break;
            case OBJLNK:
                if (currentType == Type.STRING) {
                    return ObjectLink.fromPath(value.toString());
                }
            default:
        }
        throw new CodecException("Invalid value type for resource %s, expected %s, got %s", resourcePath, expectedType,
                currentType);
    }
 }

/*
 * 本类总结：
 * 1. 核心职责：`LwM2mValueConverterImpl` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
