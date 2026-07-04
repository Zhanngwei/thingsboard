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
package org.thingsboard.script.api.tbel;

import com.google.common.primitives.Bytes;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mvel2.ExecutionContext;
import org.mvel2.ParserContext;
import org.mvel2.SandboxedParserConfiguration;
import org.mvel2.execution.ExecutionArrayList;
import org.mvel2.execution.ExecutionHashMap;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

@Slf4j

/**
 * 中文说明：
 * 1. 类目的：`TbUtilsTest` 是ThingsBoard Common 测试模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class TbUtilsTest {

    /**
     * 字段说明：
     * 1. 保存 `ctx` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private ExecutionContext ctx;

    /**
     * 字段说明：
     * 1. 保存 `intValHex` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final String intValHex = "41EA62CC";
    private final float floatVal = 29.29824f;
    /**
     * 字段说明：
     * 1. 保存 `floatValStr` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final String floatValStr = "29.29824";


    /**
     * 字段说明：
     * 1. 保存 `floatValHexRev` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final String floatValHexRev = "CC62EA41";
    private final float floatValRev = -5.948442E7f;

    /**
     * 字段说明：
     * 1. 保存 `longVal` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final long longVal = 0x409B04B10CB295EAL;
    private final String longValHex = "409B04B10CB295EA";
    /**
     * 字段说明：
     * 1. 保存 `longValRev` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final long longValRev = 0xEA95B20CB1049B40L;
    private final String longValHexRev = "EA95B20CB1049B40";
    /**
     * 字段说明：
     * 1. 保存 `doubleValStr` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final String doubleValStr = "1729.1729";
    private final double doubleVal = 1729.1729;
    /**
     * 字段说明：
     * 1. 保存 `doubleValRev` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final double doubleValRev = -2.7208640774822924E205;


    @Before
    /**
     * 方法说明：
     * 1. 职责：执行 `before` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void before() {
        SandboxedParserConfiguration parserConfig = ParserContext.enableSandboxedMode();
        parserConfig.addImport("JSON", TbJson.class);
        parserConfig.registerDataType("Date", TbDate.class, date -> 8L);
        parserConfig.registerDataType("Random", Random.class, date -> 8L);
        parserConfig.registerDataType("Calendar", Calendar.class, date -> 8L);
        try {
            TbUtils.register(parserConfig);
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Exception e) {
            log.error("Cannot register functions", e);
        }
        ctx = new ExecutionContext(parserConfig);
        Assert.assertNotNull(ctx);
    }

    @After
    /**
     * 方法说明：
     * 1. 职责：执行 `after` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void after() {
        ctx.stop();
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `parseHexToInt` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void parseHexToInt() {
        Assert.assertEquals(0xAB, TbUtils.parseHexToInt("AB"));

        Assert.assertEquals(0xABBA, TbUtils.parseHexToInt("ABBA", true));
        Assert.assertEquals(0xBAAB, TbUtils.parseHexToInt("ABBA", false));
        Assert.assertEquals(0xAABBCC, TbUtils.parseHexToInt("AABBCC", true));
        Assert.assertEquals(0xAABBCC, TbUtils.parseHexToInt("CCBBAA", false));
        Assert.assertEquals(0xAABBCCDD, TbUtils.parseHexToInt("AABBCCDD", true));
        Assert.assertEquals(0xAABBCCDD, TbUtils.parseHexToInt("DDCCBBAA", false));
        Assert.assertEquals(0xDDCCBBAA, TbUtils.parseHexToInt("DDCCBBAA", true));
        Assert.assertEquals(0xDDCCBBAA, TbUtils.parseHexToInt("AABBCCDD", false));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `parseBytesToInt_checkPrimitives` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void parseBytesToInt_checkPrimitives() {
        int expected = 257;
        byte[] data = ByteBuffer.allocate(4).putInt(expected).array();
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4));
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 2, 2, true));
        Assert.assertEquals(1, TbUtils.parseBytesToInt(data, 3, 1, true));

        expected = Integer.MAX_VALUE;
        data = ByteBuffer.allocate(4).putInt(expected).array();
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, true));

        expected = 0xAABBCCDD;
        data = new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD};
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, true));
        data = new byte[]{(byte) 0xDD, (byte) 0xCC, (byte) 0xBB, (byte) 0xAA};
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, false));

        expected = 0xAABBCC;
        data = new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC};
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 3, true));
        data = new byte[]{(byte) 0xCC, (byte) 0xBB, (byte) 0xAA};
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 3, false));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `parseBytesToInt_checkLists` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void parseBytesToInt_checkLists() {
        int expected = 257;
        List<Byte> data = toList(ByteBuffer.allocate(4).putInt(expected).array());
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4));
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 2, 2, true));
        Assert.assertEquals(1, TbUtils.parseBytesToInt(data, 3, 1, true));

        expected = Integer.MAX_VALUE;
        data = toList(ByteBuffer.allocate(4).putInt(expected).array());
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, true));

        expected = 0xAABBCCDD;
        data = toList(new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD});
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, true));
        data = toList(new byte[]{(byte) 0xDD, (byte) 0xCC, (byte) 0xBB, (byte) 0xAA});
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, false));

        expected = 0xAABBCC;
        data = toList(new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC});
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 3, true));
        data = toList(new byte[]{(byte) 0xCC, (byte) 0xBB, (byte) 0xAA});
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 3, false));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `toFlatMap` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void toFlatMap() {
        ExecutionHashMap<String, Object> inputMap = new ExecutionHashMap<>(16, ctx);
        inputMap.put("name", "Alice");
        inputMap.put("age", 30);
        inputMap.put("devices", new ExecutionArrayList<>(List.of(
                new ExecutionHashMap<>(16, ctx) {{
                    put("id", "dev001");
                    put("type", "sensor");
                }},
                new ExecutionHashMap<>(16, ctx) {{
                    put("id", "dev002");
                    put("type", "actuator");
                }}
        ), ctx));
        inputMap.put("settings", new ExecutionHashMap<>(16, ctx) {{
            put("notifications", true);
            put("timezone", "UTC-5");
            put("params", new ExecutionHashMap<>(16, ctx) {{
                put("param1", "value1");
                put("param2", "value2");
                put("param3", new ExecutionHashMap<>(16, ctx) {{
                    put("subParam1", "value1");
                    put("subParam2", "value2");
                }});
            }});
        }});
        ExecutionArrayList<String> excludeList = new ExecutionArrayList<>(ctx);
        excludeList.addAll(List.of("age", "id", "param1", "subParam2"));

        ExecutionHashMap<String, Object> expectedMapWithPath = new ExecutionHashMap<>(16, ctx);
        expectedMapWithPath.put("name", "Alice");
        expectedMapWithPath.put("devices.0.type", "sensor");
        expectedMapWithPath.put("devices.1.type", "actuator");
        expectedMapWithPath.put("settings.notifications", true);
        expectedMapWithPath.put("settings.timezone", "UTC-5");
        expectedMapWithPath.put("settings.params.param2", "value2");
        expectedMapWithPath.put("settings.params.param3.subParam1", "value1");

        ExecutionHashMap<String, Object> actualMapWithPaths = TbUtils.toFlatMap(ctx, inputMap, excludeList, true);

        Assert.assertEquals(expectedMapWithPath, actualMapWithPaths);

        ExecutionHashMap<String, Object> expectedMapWithoutPaths = new ExecutionHashMap<>(16, ctx);
        expectedMapWithoutPaths.put("timezone", "UTC-5");
        expectedMapWithoutPaths.put("name", "Alice");
        expectedMapWithoutPaths.put("id", "dev002");
        expectedMapWithoutPaths.put("subParam2", "value2");
        expectedMapWithoutPaths.put("type", "actuator");
        expectedMapWithoutPaths.put("subParam1", "value1");
        expectedMapWithoutPaths.put("param1", "value1");
        expectedMapWithoutPaths.put("notifications", true);
        expectedMapWithoutPaths.put("age", 30);
        expectedMapWithoutPaths.put("param2", "value2");

        ExecutionHashMap<String, Object> actualMapWithoutPaths = TbUtils.toFlatMap(ctx, inputMap, false);

        Assert.assertEquals(expectedMapWithoutPaths, actualMapWithoutPaths);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `parseInt` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void parseInt() {
        Assert.assertNull(TbUtils.parseInt(null));
        Assert.assertNull(TbUtils.parseInt(""));
        Assert.assertNull(TbUtils.parseInt(" "));

        Assert.assertEquals(java.util.Optional.of(0).get(), TbUtils.parseInt("0"));
        Assert.assertEquals(java.util.Optional.of(0).get(), TbUtils.parseInt("-0"));
        Assert.assertEquals(java.util.Optional.of(473).get(), TbUtils.parseInt("473"));
        Assert.assertEquals(java.util.Optional.of(-255).get(), TbUtils.parseInt("-0xFF"));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt("FF"));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt("0xFG"));

        Assert.assertEquals(java.util.Optional.of(102).get(), TbUtils.parseInt("1100110", 2));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt("1100210", 2));

        Assert.assertEquals(java.util.Optional.of(63).get(), TbUtils.parseInt("77", 8));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt("18", 8));

        Assert.assertEquals(java.util.Optional.of(-255).get(), TbUtils.parseInt("-FF", 16));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt("FG", 16));


        Assert.assertEquals(java.util.Optional.of(Integer.MAX_VALUE).get(), TbUtils.parseInt(Integer.toString(Integer.MAX_VALUE), 10));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt(BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.valueOf(1)).toString(10), 10));
        Assert.assertEquals(java.util.Optional.of(Integer.MIN_VALUE).get(), TbUtils.parseInt(Integer.toString(Integer.MIN_VALUE), 10));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt(BigInteger.valueOf(Integer.MIN_VALUE).subtract(BigInteger.valueOf(1)).toString(10), 10));

        Assert.assertEquals(java.util.Optional.of(506070563).get(), TbUtils.parseInt("KonaIn", 30));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt("KonaIn", 10));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `parseFloat` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void parseFloat() {
        Assert.assertEquals(java.util.Optional.of(floatVal).get(), TbUtils.parseFloat(floatValStr));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `toFixedFloat` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void toFixedFloat() {
        float actualF = TbUtils.toFixed(floatVal, 3);
        Assert.assertEquals(1, Float.compare(floatVal, actualF));
        Assert.assertEquals(0, Float.compare(29.298f, actualF));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `parseHexToFloat` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void parseHexToFloat() {
        Assert.assertEquals(0, Float.compare(floatVal, TbUtils.parseHexToFloat(intValHex)));
        Assert.assertEquals(0, Float.compare(floatValRev, TbUtils.parseHexToFloat(intValHex, false)));
        Assert.assertEquals(0, Float.compare(floatVal, TbUtils.parseBigEndianHexToFloat(intValHex)));
        Assert.assertEquals(0, Float.compare(floatVal, TbUtils.parseLittleEndianHexToFloat(floatValHexRev)));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `arseBytesToFloat` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void arseBytesToFloat() {
        byte[] floatValByte = {65, -22, 98, -52};
        Assert.assertEquals(0, Float.compare(floatVal, TbUtils.parseBytesToFloat(floatValByte, 0)));
        Assert.assertEquals(0, Float.compare(floatValRev, TbUtils.parseBytesToFloat(floatValByte, 0, false)));

        List <Byte> floatVaList = Bytes.asList(floatValByte);
        Assert.assertEquals(0, Float.compare(floatVal, TbUtils.parseBytesToFloat(floatVaList, 0)));
        Assert.assertEquals(0, Float.compare(floatValRev, TbUtils.parseBytesToFloat(floatVaList, 0, false)));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `parseLong` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void parseLong() {
        Assert.assertNull(TbUtils.parseLong(null));
        Assert.assertNull(TbUtils.parseLong(""));
        Assert.assertNull(TbUtils.parseLong(" "));

        Assert.assertEquals(java.util.Optional.of(0L).get(), TbUtils.parseLong("0"));
        Assert.assertEquals(java.util.Optional.of(0L).get(), TbUtils.parseLong("-0"));
        Assert.assertEquals(java.util.Optional.of(473L).get(), TbUtils.parseLong("473"));
        Assert.assertEquals(java.util.Optional.of(-65535L).get(), TbUtils.parseLong("-0xFFFF"));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseLong("FFFFFFFF"));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseLong("0xFGFFFFFF"));

        Assert.assertEquals(java.util.Optional.of(13158L).get(), TbUtils.parseLong("11001101100110", 2));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseLong("11001101100210", 2));

        Assert.assertEquals(java.util.Optional.of(9223372036854775807L).get(), TbUtils.parseLong("777777777777777777777", 8));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseLong("1787", 8));

        Assert.assertEquals(java.util.Optional.of(-255L).get(), TbUtils.parseLong("-FF", 16));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseLong("FG", 16));


        Assert.assertEquals(java.util.Optional.of(Long.MAX_VALUE).get(), TbUtils.parseLong(Long.toString(Long.MAX_VALUE), 10));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseLong(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(1)).toString(10), 10));
        Assert.assertEquals(java.util.Optional.of(Long.MIN_VALUE).get(), TbUtils.parseLong(Long.toString(Long.MIN_VALUE), 10));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseLong(BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.valueOf(1)).toString(10), 10));

        Assert.assertEquals(java.util.Optional.of(218840926543L).get(), TbUtils.parseLong("KonaLong", 27));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseLong("KonaLong", 10));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `parseHexToLong` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void parseHexToLong() {
        Assert.assertEquals(longVal, TbUtils.parseHexToLong(longValHex));
        Assert.assertEquals(longVal, TbUtils.parseHexToLong(longValHexRev, false));
        Assert.assertEquals(longVal, TbUtils.parseBigEndianHexToLong(longValHex));
        Assert.assertEquals(longVal, TbUtils.parseLittleEndianHexToLong(longValHexRev));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `parseBytesToLong` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void parseBytesToLong() {
        byte[] longValByte = {64, -101, 4, -79, 12, -78, -107, -22};
        Assert.assertEquals(longVal, TbUtils.parseBytesToLong(longValByte, 0, 8));
        Bytes.reverse(longValByte);
        Assert.assertEquals(longVal, TbUtils.parseBytesToLong(longValByte, 0, 8, false));

        List <Byte> longVaList = Bytes.asList(longValByte);
        Assert.assertEquals(longVal, TbUtils.parseBytesToLong(longVaList, 0, 8, false));
        Assert.assertEquals(longValRev, TbUtils.parseBytesToLong(longVaList, 0, 8));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `parsDouble` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void parsDouble() {
        Assert.assertEquals(java.util.Optional.of(doubleVal).get(), TbUtils.parseDouble(doubleValStr));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `toFixedDouble` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void toFixedDouble() {
        double actualD = TbUtils.toFixed(doubleVal, 3);
        Assert.assertEquals(-1, Double.compare(doubleVal, actualD));
        Assert.assertEquals(0, Double.compare(1729.173, actualD));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `parseHexToDouble` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void parseHexToDouble() {
        Assert.assertEquals(0, Double.compare(doubleVal, TbUtils.parseHexToDouble(longValHex)));
        Assert.assertEquals(0, Double.compare(doubleValRev, TbUtils.parseHexToDouble(longValHex, false)));
        Assert.assertEquals(0, Double.compare(doubleVal, TbUtils.parseBigEndianHexToDouble(longValHex)));
        Assert.assertEquals(0, Double.compare(doubleVal, TbUtils.parseLittleEndianHexToDouble(longValHexRev)));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `parseBytesToDouble` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void parseBytesToDouble() {
        byte[] doubleValByte = {64, -101, 4, -79, 12, -78, -107, -22};
        Assert.assertEquals(0, Double.compare(doubleVal, TbUtils.parseBytesToDouble(doubleValByte, 0)));
        Assert.assertEquals(0, Double.compare(doubleValRev, TbUtils.parseBytesToDouble(doubleValByte, 0, false)));

        List <Byte> doubleVaList = Bytes.asList(doubleValByte);
        Assert.assertEquals(0, Double.compare(doubleVal, TbUtils.parseBytesToDouble(doubleVaList, 0)));
        Assert.assertEquals(0, Double.compare(doubleValRev, TbUtils.parseBytesToDouble(doubleVaList, 0, false)));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `parseBytesDecodeToJson` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void parseBytesDecodeToJson() throws IOException, IllegalAccessException {
        String expectedStr = "{\"hello\": \"world\"}";
        ExecutionHashMap<String, Object> expectedJson = new ExecutionHashMap<>(1, ctx);
        expectedJson.put("hello", "world");
        List<Byte> expectedBytes = TbUtils.stringToBytes(ctx, expectedStr);
        Object actualJson =  TbUtils.decodeToJson(ctx, expectedBytes);
        Assert.assertEquals(expectedJson,actualJson);
    }
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `parseStringDecodeToJson` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void parseStringDecodeToJson() throws IOException {
        String expectedStr = "{\"hello\": \"world\"}";
        ExecutionHashMap<String, Object> expectedJson = new ExecutionHashMap<>(1, ctx);
        expectedJson.put("hello", "world");
        Object actualJson =  TbUtils.decodeToJson(ctx, expectedStr);
        Assert.assertEquals(expectedJson,actualJson);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `stringToBytesInputObjectTest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void stringToBytesInputObjectTest() throws IOException, IllegalAccessException {
        String expectedStr = "{\"hello\": \"world\"}";
        Object inputJson = TbUtils.decodeToJson(ctx, expectedStr);
        byte[] arrayBytes = {119, 111, 114, 108, 100};
        List<Byte> expectedBytes = Bytes.asList(arrayBytes);
        List<Byte> actualBytes = TbUtils.stringToBytes(ctx, ((ExecutionHashMap) inputJson).get("hello"));
        Assert.assertEquals(expectedBytes, actualBytes);
        actualBytes = TbUtils.stringToBytes(ctx, ((ExecutionHashMap) inputJson).get("hello"), "UTF-8");
        Assert.assertEquals(expectedBytes, actualBytes);

        expectedStr = "{\"hello\": 1234}";
        inputJson = TbUtils.decodeToJson(ctx, expectedStr);
        Object finalInputJson = inputJson;
        Assert.assertThrows(IllegalAccessException.class, () -> TbUtils.stringToBytes(ctx, ((ExecutionHashMap) finalInputJson).get("hello")));
        Assert.assertThrows(IllegalAccessException.class, () -> TbUtils.stringToBytes(ctx, ((ExecutionHashMap) finalInputJson).get("hello"), "UTF-8"));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `bytesFromList` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void bytesFromList() {
        byte[] arrayBytes =      {(byte)0x00, (byte)0x08, (byte)0x10, (byte)0x1C, (byte)0xFF, (byte)0xFC, (byte)0xAD, (byte)0x88, (byte)0x75, (byte)0x74, (byte)0x8A, (byte)0x82};
        Object[] arrayMix      = {    "0x00",         8,        "16",     "0x1C",        255, (byte)0xFC,        173,        136,        117,        116,       -118,     "-126"};

        String expected = new String(arrayBytes);
        ArrayList<Byte> listBytes = new ArrayList<>(arrayBytes.length);
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (Byte element : arrayBytes) {
            listBytes.add(element);
        }
        Assert.assertEquals(expected, TbUtils.bytesToString(listBytes));

        ArrayList<Object> listMix = new ArrayList<>(arrayMix.length);
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (Object element : arrayMix) {
            listMix.add(element);
        }
        Assert.assertEquals(expected, TbUtils.bytesToString(listMix));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `bytesFromList_Error` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void bytesFromList_Error() {
        List<String> listHex = new ArrayList<>();
        listHex.add("0xFG");
        try {
            TbUtils.bytesToString(listHex);
            Assert.fail("Should throw NumberFormatException");
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (NumberFormatException e) {
            Assert.assertTrue(e.getMessage().contains("Failed radix: [16] for value: \"FG\"!"));
        }
        listHex.add(0, "1F");
        try {
            TbUtils.bytesToString(listHex);
            Assert.fail("Should throw NumberFormatException");
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (NumberFormatException e) {
            Assert.assertTrue(e.getMessage().contains("Failed radix: [10] for value: \"1F\"!"));
        }

        List<String> listIntString = new ArrayList<>();
        listIntString.add("-129");
        try {
            TbUtils.bytesToString(listIntString);
            Assert.fail("Should throw NumberFormatException");
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (NumberFormatException e) {
            Assert.assertTrue(e.getMessage().contains("The value '-129' could not be correctly converted to a byte. " +
                    "Integer to byte conversion requires the use of only 8 bits (with a range of min/max = -128/255)!"));
        }

        listIntString.add(0, "256");
        try {
            TbUtils.bytesToString(listIntString);
            Assert.fail("Should throw NumberFormatException");
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (NumberFormatException e) {
            Assert.assertTrue(e.getMessage().contains("The value '256' could not be correctly converted to a byte. " +
                    "Integer to byte conversion requires the use of only 8 bits (with a range of min/max = -128/255)!"));
        }

        ArrayList<Integer> listIntBytes = new ArrayList<>();
        listIntBytes.add(-129);
        try {
            TbUtils.bytesToString(listIntBytes);
            Assert.fail("Should throw NumberFormatException");
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (NumberFormatException e) {
            Assert.assertTrue(e.getMessage().contains("The value '-129' could not be correctly converted to a byte. " +
                    "Integer to byte conversion requires the use of only 8 bits (with a range of min/max = -128/255)!"));
        }

        listIntBytes.add(0, 256);
        try {
            TbUtils.bytesToString(listIntBytes);
            Assert.fail("Should throw NumberFormatException");
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (NumberFormatException e) {
            Assert.assertTrue(e.getMessage().contains("The value '256' could not be correctly converted to a byte. " +
                    "Integer to byte conversion requires the use of only 8 bits (with a range of min/max = -128/255)!"));
        }

        ArrayList<Object> listObjects = new ArrayList<>();
        ArrayList<String> listStringObjects = new ArrayList<>();
        listStringObjects.add("0xFD");
        listObjects.add(listStringObjects);
        try {
            TbUtils.bytesToString(listObjects);
            Assert.fail("Should throw NumberFormatException");
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (NumberFormatException e) {
            Assert.assertTrue(e.getMessage().contains("The value '[0xFD]' could not be correctly converted to a byte. " +
                    "Must be a HexDecimal/String/Integer/Byte format !"));
        }
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `toList` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static List<Byte> toList(byte[] data) {
        List<Byte> result = new ArrayList<>(data.length);
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (Byte b : data) {
            result.add(b);
        }
        return result;
    }
}


/*
 * 本类总结：
 * 1. 核心职责：`TbUtilsTest` 在 ThingsBoard Common 测试模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
