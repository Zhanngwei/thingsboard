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
package org.thingsboard.server.common.msg;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * 中文说明：
 * 1. 类目的：`TbMsgMetaDataTest` 是ThingsBoard Common 测试模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class TbMsgMetaDataTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    /**
     * JSON，表示当前对象的对应属性。
     */
    private final String metadataJsonStr = "{\"deviceName\":\"Test Device\",\"deviceType\":\"default\",\"ts\":\"1645112691407\"}";
    private JsonNode metadataJson;
    /**
     * `metadataExpected`映射关系，用于按键查找对应值。
     */
    private Map<String, String> metadataExpected;

    /**
     * 功能：初始化或启动`Init`。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void startInit() throws Exception {
        metadataJson = objectMapper.readValue(metadataJsonStr, JsonNode.class);
        metadataExpected = objectMapper.convertValue(metadataJson, new TypeReference<>() {
        });
    }

    /**
     * 功能：验证值相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testScript_whenMetadataWithoutPropertiesValueNull_returnMetadataWithAllValue() {
        TbMsgMetaData tbMsgMetaData = new TbMsgMetaData(metadataExpected);
        Map<String, String> dataActual = tbMsgMetaData.values();
        assertEquals(metadataExpected.size(), dataActual.size());
    }

    /**
     * 功能：验证值相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testScript_whenMetadataWithPropertiesValueNull_returnMetadataWithoutPropertiesValueEqualsNull() {
        metadataExpected.put("deviceName", null);
        TbMsgMetaData tbMsgMetaData = new TbMsgMetaData(metadataExpected);
        Map<String, String> dataActual = tbMsgMetaData.copy().getData();
        assertEquals(metadataExpected.size() - 1, dataActual.size());
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbMsgMetaDataTest` 在 ThingsBoard Common 测试模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
