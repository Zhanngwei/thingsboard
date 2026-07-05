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
package org.thingsboard.server.utils;

import com.fasterxml.jackson.databind.node.NullNode;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNode;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNodeConfiguration;
import org.thingsboard.rule.engine.metadata.TbGetCustomerAttributeNode;
import org.thingsboard.rule.engine.metadata.TbGetEntityDataNodeConfiguration;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.service.component.RuleNodeClassInfo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 中文说明：
 * 1. 类目的：`TbNodeUpgradeUtilsTest` 是ThingsBoard Application 测试模块中的测试支撑类型，用于验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 生命周期：由测试框架按测试类和测试方法管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Test Fixture。
 */
public class TbNodeUpgradeUtilsTest {

    /**
     * 功能：验证规则节点相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpgradeRuleNodeConfigurationWithNullConfig() throws Exception {
        // GIVEN
        var node = new RuleNode();
        var nodeInfo = mock(RuleNodeClassInfo.class);
        var nodeConfigClazz = TbGetAttributesNodeConfiguration.class;
        var annotation = mock(org.thingsboard.rule.engine.api.RuleNode.class);
        var defaultConfig = JacksonUtil.valueToTree(nodeConfigClazz.getDeclaredConstructor().newInstance().defaultConfiguration());

        when(nodeInfo.getClazz()).thenReturn((Class) TbGetAttributesNode.class);
        when(nodeInfo.getCurrentVersion()).thenReturn(1);
        when(nodeInfo.getAnnotation()).thenReturn(annotation);
        when(annotation.configClazz()).thenReturn((Class) nodeConfigClazz);

        // WHEN
        TbNodeUpgradeUtils.upgradeConfigurationAndVersion(node, nodeInfo);
        // THEN
        Assertions.assertThat(node.getConfiguration()).isEqualTo(defaultConfig);
        Assertions.assertThat(node.getConfigurationVersion()).isEqualTo(1);
    }

    /**
     * 功能：验证规则节点相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpgradeRuleNodeConfigurationWithNullNodeConfig() throws Exception {
        // GIVEN
        var node = new RuleNode();
        node.setConfiguration(NullNode.instance);
        var nodeInfo = mock(RuleNodeClassInfo.class);
        var nodeConfigClazz = TbGetAttributesNodeConfiguration.class;
        var annotation = mock(org.thingsboard.rule.engine.api.RuleNode.class);
        var defaultConfig = JacksonUtil.valueToTree(nodeConfigClazz.getDeclaredConstructor().newInstance().defaultConfiguration());

        when(nodeInfo.getClazz()).thenReturn((Class) TbGetAttributesNode.class);
        when(nodeInfo.getCurrentVersion()).thenReturn(1);
        when(nodeInfo.getAnnotation()).thenReturn(annotation);
        when(annotation.configClazz()).thenReturn((Class) nodeConfigClazz);

        // WHEN
        TbNodeUpgradeUtils.upgradeConfigurationAndVersion(node, nodeInfo);
        // THEN
        Assertions.assertThat(node.getConfiguration()).isEqualTo(defaultConfig);
        Assertions.assertThat(node.getConfigurationVersion()).isEqualTo(1);
    }

    /**
     * 功能：验证规则节点相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpgradeRuleNodeConfigurationWithNonNullConfig() throws Exception {
        // GIVEN
        var node = new RuleNode();
        var nodeInfo = mock(RuleNodeClassInfo.class);
        var nodeConfigClazz = TbGetAttributesNodeConfiguration.class;
        var annotation = mock(org.thingsboard.rule.engine.api.RuleNode.class);
        var defaultConfig = JacksonUtil.valueToTree(nodeConfigClazz.getDeclaredConstructor().newInstance().defaultConfiguration());

        when(nodeInfo.getClazz()).thenReturn((Class) TbGetAttributesNode.class);
        when(nodeInfo.getCurrentVersion()).thenReturn(1);
        when(nodeInfo.getAnnotation()).thenReturn(annotation);
        when(annotation.configClazz()).thenReturn((Class) nodeConfigClazz);

        String versionZeroDefaultConfigStr = "{\"fetchToData\":false," +
                "\"clientAttributeNames\":[]," +
                "\"sharedAttributeNames\":[]," +
                "\"serverAttributeNames\":[]," +
                "\"latestTsKeyNames\":[]," +
                "\"tellFailureIfAbsent\":true," +
                "\"getLatestValueWithTs\":false}";
        node.setConfiguration(JacksonUtil.toJsonNode(versionZeroDefaultConfigStr));
        // WHEN
        TbNodeUpgradeUtils.upgradeConfigurationAndVersion(node, nodeInfo);
        // THEN
        Assertions.assertThat(node.getConfiguration()).isEqualTo(defaultConfig);
        Assertions.assertThat(node.getConfigurationVersion()).isEqualTo(1);

    }

    /**
     * 功能：验证规则节点相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpgradeRuleNodeConfigurationWithNewConfigAndOldConfigVersion() throws Exception {
        // GIVEN
        var node = new RuleNode();
        var nodeInfo = mock(RuleNodeClassInfo.class);
        var nodeConfigClazz = TbGetEntityDataNodeConfiguration.class;
        var annotation = mock(org.thingsboard.rule.engine.api.RuleNode.class);
        var defaultConfig = JacksonUtil.valueToTree(nodeConfigClazz.getDeclaredConstructor().newInstance().defaultConfiguration());

        when(nodeInfo.getClazz()).thenReturn((Class) TbGetCustomerAttributeNode.class);
        when(nodeInfo.getCurrentVersion()).thenReturn(1);
        when(nodeInfo.getAnnotation()).thenReturn(annotation);
        when(annotation.configClazz()).thenReturn((Class) nodeConfigClazz);

        String versionOneDefaultConfig = "{\"fetchTo\":\"METADATA\"," +
                "\"dataMapping\":{\"alarmThreshold\":\"threshold\"}," +
                "\"dataToFetch\":\"ATTRIBUTES\"}";
        node.setConfiguration(JacksonUtil.toJsonNode(versionOneDefaultConfig));
        // WHEN
        TbNodeUpgradeUtils.upgradeConfigurationAndVersion(node, nodeInfo);
        // THEN
        Assertions.assertThat(node.getConfiguration()).isEqualTo(defaultConfig);
        Assertions.assertThat(node.getConfigurationVersion()).isEqualTo(1);

    }

    /**
     * 功能：验证规则节点相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpgradeRuleNodeConfigurationWithInvalidConfigAndOldConfigVersion() throws Exception {
        // GIVEN
        var node = new RuleNode();
        var nodeInfo = mock(RuleNodeClassInfo.class);
        var nodeConfigClazz = TbGetEntityDataNodeConfiguration.class;
        var annotation = mock(org.thingsboard.rule.engine.api.RuleNode.class);
        var defaultConfig = JacksonUtil.valueToTree(nodeConfigClazz.getDeclaredConstructor().newInstance().defaultConfiguration());

        when(nodeInfo.getClazz()).thenReturn((Class) TbGetCustomerAttributeNode.class);
        when(nodeInfo.getCurrentVersion()).thenReturn(1);
        when(nodeInfo.getAnnotation()).thenReturn(annotation);
        when(annotation.configClazz()).thenReturn((Class) nodeConfigClazz);

        // missing telemetry field
        String oldConfig = "{\"attrMapping\":{\"alarmThreshold\":\"threshold\"}}";;
        node.setConfiguration(JacksonUtil.toJsonNode(oldConfig));
        // WHEN
        TbNodeUpgradeUtils.upgradeConfigurationAndVersion(node, nodeInfo);
        // THEN
        Assertions.assertThat(node.getConfiguration()).isEqualTo(defaultConfig);
        Assertions.assertThat(node.getConfigurationVersion()).isEqualTo(1);

    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TbNodeUpgradeUtilsTest` 在 ThingsBoard Application 测试模块 中承担测试支撑类型职责，核心目的是验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 核心流程：准备输入和依赖，调用被测对象并断言结果。
 * 3. 关键依赖：主要依赖或协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
