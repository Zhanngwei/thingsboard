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
package org.thingsboard.server.transport.mqtt.util.sparkplug;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;

import java.math.BigInteger;
import java.util.Date;

/**
 * Created by nickAS21 on 10.01.23
 */

/**
 * 中文说明：
 * 1. 类目的：`MetricDataType` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public enum MetricDataType {

    // Basic Types
    Int8(1, Byte.class),
    Int16(2, Short.class),
    Int32(3, Integer.class),
    Int64(4, Long.class),
    UInt8(5, Short.class),
    UInt16(6, Integer.class),
    UInt32(7, Long.class),
    UInt64(8, BigInteger.class),
    Float(9, Float.class),
    Double(10, Double.class),
    Boolean(11, Boolean.class),
    String(12, String.class),
    DateTime(13, Date.class),
    Text(14, String.class),

    // Custom Types for Metrics
    UUID(15, String.class),
    DataSet(16, SparkplugBProto.Payload.DataSet.class),
    Bytes(17, byte[].class),
    File(18, SparkplugMetricUtil.File.class),
    Template(19, SparkplugBProto.Payload.Template.class),

    // PropertyValue Types (20 and 21) are NOT metric datatypes

    // Unknown
    Unknown(0, Object.class);

    /**
     * `clazz` 字段，保存当前对象的对应属性。
     */
    private Class<?> clazz = null;
    private int intValue = 0;

    /**
     * Constructor
     *
     * @param intValue the integer value of this {@link MetricDataType}
     * @param clazz    the {@link Class} type associated with this {@link MetricDataType}
     */
    /**
     * 功能：创建 `MetricDataType` 实例，并初始化必要字段。
     * 参数：
     * - `intValue`：值。
     * - `clazz`：`clazz` 参数。
     * 返回：新创建的对象实例。
     */
    private MetricDataType(int intValue, Class<?> clazz) {
        this.intValue = intValue;
        this.clazz = clazz;
    }

    /**
     * Checks the type of a specified value against the specified {@link MetricDataType}
     *
     * @param value the {@link Object} value to check against the {@link MetricDataType}
     * @throws AdaptorException if the value is not a valid type for the given {@link MetricDataType}
     */
    /**
     * 功能：校验类型。
     * 参数：
     * - `value`：值。
     * 返回：无。
     */
    public void checkType(Object value) throws AdaptorException {
        if (value != null && !clazz.isAssignableFrom(value.getClass())) {
            String msgError = "Failed type check - " + clazz + " != " + ((value != null) ? value.getClass().toString() : "null");
            log.debug(msgError);
            throw new AdaptorException(msgError);
        }
    }

    /**
     * Returns an integer representation of the data type.
     *
     * @return an integer representation of the data type.
     */
    /**
     * 功能：执行 `toIntValue` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    public int toIntValue() {
        return this.intValue;
    }

    /**
     * Converts the integer representation of the data type into a {@link MetricDataType} instance.
     *
     * @param i the integer representation of the data type.
     * @return a {@link MetricDataType} instance.
     */
    /**
     * 功能：执行 `fromInteger` 对应的处理。
     * 参数：
     * - `i`：`i` 参数。
     * 返回：处理结果。
     */
    public static MetricDataType fromInteger(int i) {
        switch (i) {
            case 1:
                return Int8;
            case 2:
                return Int16;
            case 3:
                return Int32;
            case 4:
                return Int64;
            case 5:
                return UInt8;
            case 6:
                return UInt16;
            case 7:
                return UInt32;
            case 8:
                return UInt64;
            case 9:
                return Float;
            case 10:
                return Double;
            case 11:
                return Boolean;
            case 12:
                return String;
            case 13:
                return DateTime;
            case 14:
                return Text;
            case 15:
                return UUID;
            case 16:
                return DataSet;
            case 17:
                return Bytes;
            case 18:
                return File;
            case 19:
                return Template;
            default:
                return Unknown;
        }
    }

    /**
     * @return the class type for this DataType
     */
    /**
     * 功能：获取`Clazz`。
     * 参数：无。
     * 返回：处理结果。
     */
    public Class<?> getClazz() {
        return clazz;
    }



/*
 * 本类总结：
 * 1. 核心职责：`MetricDataType` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
}