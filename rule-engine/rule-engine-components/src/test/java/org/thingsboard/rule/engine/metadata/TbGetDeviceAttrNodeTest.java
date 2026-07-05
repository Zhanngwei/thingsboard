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
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.util.TbPair;

/**
 * `TbGetDeviceAttrNodeTest` 测试类，用于验证 `TbGetDeviceAttrNode` 相关行为。
 */
public class TbGetDeviceAttrNodeTest {

    /**
     * 功能：验证 `givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig() throws Exception {
        var defaultConfig = new TbGetDeviceAttrNodeConfiguration().defaultConfiguration();
        var node = new TbGetDeviceAttrNode();
        String oldConfig = "{\"fetchToData\":false," +
                "\"clientAttributeNames\":[]," +
                "\"sharedAttributeNames\":[]," +
                "\"serverAttributeNames\":[]," +
                "\"latestTsKeyNames\":[]," +
                "\"tellFailureIfAbsent\":true," +
                "\"getLatestValueWithTs\":false," +
                "\"deviceRelationsQuery\":{\"direction\":\"FROM\",\"maxLevel\":1,\"relationType\":\"Contains\",\"deviceTypes\":[\"default\"]," +
                "\"fetchLastLevelOnly\":false}}";
        JsonNode configJson = JacksonUtil.toJsonNode(oldConfig);
        TbPair<Boolean, JsonNode> upgrade = node.upgrade(0, configJson);
        Assertions.assertTrue(upgrade.getFirst());
        Assertions.assertEquals(defaultConfig, JacksonUtil.treeToValue(upgrade.getSecond(), defaultConfig.getClass()));
    }

    /**
     * 功能：验证 `givenOldConfigWithNoFetchToDataProperty_whenUpgrade_thenShouldReturnTrueResultWithNewConfig` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenOldConfigWithNoFetchToDataProperty_whenUpgrade_thenShouldReturnTrueResultWithNewConfig() throws Exception {
        var defaultConfig = new TbGetDeviceAttrNodeConfiguration().defaultConfiguration();
        var node = new TbGetDeviceAttrNode();
        String oldConfig = "{\"clientAttributeNames\":[]," +
                "\"sharedAttributeNames\":[]," +
                "\"serverAttributeNames\":[]," +
                "\"latestTsKeyNames\":[]," +
                "\"tellFailureIfAbsent\":true," +
                "\"getLatestValueWithTs\":false," +
                "\"deviceRelationsQuery\":{\"direction\":\"FROM\",\"maxLevel\":1,\"relationType\":\"Contains\",\"deviceTypes\":[\"default\"]," +
                "\"fetchLastLevelOnly\":false}}";
        JsonNode configJson = JacksonUtil.toJsonNode(oldConfig);
        TbPair<Boolean, JsonNode> upgrade = node.upgrade(0, configJson);
        Assertions.assertTrue(upgrade.getFirst());
        Assertions.assertEquals(defaultConfig, JacksonUtil.treeToValue(upgrade.getSecond(), defaultConfig.getClass()));
    }

    /**
     * 功能：验证 `givenOldConfigWithNullFetchToDataProperty_whenUpgrade_thenShouldReturnTrueResultWithNewConfig` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenOldConfigWithNullFetchToDataProperty_whenUpgrade_thenShouldReturnTrueResultWithNewConfig() throws Exception {
        var defaultConfig = new TbGetDeviceAttrNodeConfiguration().defaultConfiguration();
        var node = new TbGetDeviceAttrNode();
        String oldConfig = "{\"fetchToData\":null," +
                "\"clientAttributeNames\":[]," +
                "\"sharedAttributeNames\":[]," +
                "\"serverAttributeNames\":[]," +
                "\"latestTsKeyNames\":[]," +
                "\"tellFailureIfAbsent\":true," +
                "\"getLatestValueWithTs\":false," +
                "\"deviceRelationsQuery\":{\"direction\":\"FROM\",\"maxLevel\":1,\"relationType\":\"Contains\",\"deviceTypes\":[\"default\"]," +
                "\"fetchLastLevelOnly\":false}}";
        JsonNode configJson = JacksonUtil.toJsonNode(oldConfig);
        TbPair<Boolean, JsonNode> upgrade = node.upgrade(0, configJson);
        Assertions.assertTrue(upgrade.getFirst());
        Assertions.assertEquals(defaultConfig, JacksonUtil.treeToValue(upgrade.getSecond(), defaultConfig.getClass()));
    }

}
/*
 * 本类总结：{@code TbGetDeviceAttrNodeTest} 为 {@code TbGetDeviceAttrNode} 的 元数据增强节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
