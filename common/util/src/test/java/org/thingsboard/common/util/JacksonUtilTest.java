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
package org.thingsboard.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;

import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`JacksonUtilTest` 是ThingsBoard Common 测试模块中的公共工具类型，用于提供跨模块复用的纯函数、解析、转换或辅助逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Application、DAO、Transport、Rule Engine、测试工具和第三方库。
 * 4. 生命周期：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Utility / Helper。
 */
public class JacksonUtilTest {

    /**
     * 功能：执行 `allowUnquotedFieldMapperTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void allowUnquotedFieldMapperTest() {
        String data = "{data: 123}";
        JsonNode actualResult = JacksonUtil.toJsonNode(data, JacksonUtil.ALLOW_UNQUOTED_FIELD_NAMES_MAPPER); // should be: {"data": 123}
        ObjectNode expectedResult = JacksonUtil.newObjectNode();
        expectedResult.put("data", 123); // {"data": 123}
        Assert.assertEquals(expectedResult, actualResult);
        Assert.assertThrows(IllegalArgumentException.class, () -> JacksonUtil.toJsonNode(data)); // syntax exception due to missing quotes in the field name!
    }

    /**
     * 功能：执行 `failOnUnknownPropertiesMapperTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void failOnUnknownPropertiesMapperTest() {
        Asset asset = new Asset();
        asset.setId(new AssetId(UUID.randomUUID()));
        asset.setName("Test");
        asset.setType("type");
        String serializedAsset = JacksonUtil.toString(asset);
        JsonNode jsonNode = JacksonUtil.toJsonNode(serializedAsset);
        // case: add new field to serialized Asset string and check for backward compatibility with original Asset object
        Assert.assertNotNull(jsonNode);
        ((ObjectNode) jsonNode).put("test", (String) null);
        serializedAsset = JacksonUtil.toString(jsonNode);
        // deserialize with FAIL_ON_UNKNOWN_PROPERTIES = false
        Asset result = JacksonUtil.fromString(serializedAsset, Asset.class, true);
        Assert.assertNotNull(result);
        Assert.assertEquals(asset.getId(), result.getId());
        Assert.assertEquals(asset.getName(), result.getName());
        Assert.assertEquals(asset.getType(), result.getType());
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`JacksonUtilTest` 在 ThingsBoard Common 测试模块 中承担公共工具类型职责，核心目的是提供跨模块复用的纯函数、解析、转换或辅助逻辑。
 * 2. 核心流程：接收输入参数后执行本地转换、校验或解析并返回结果。
 * 3. 关键依赖：主要依赖或协作对象包括Application、DAO、Transport、Rule Engine、测试工具和第三方库。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
