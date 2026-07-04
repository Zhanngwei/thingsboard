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

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.server.common.data.kv.Aggregation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * 测试目标：验证 {@code TbGetTelemetryNodeTest} 覆盖的 元数据增强节点 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbGetTelemetryNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
public class TbGetTelemetryNodeTest {

    /** 可变 fixture 字段：{@code node} 保存 {@code TbGetTelemetryNode} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    TbGetTelemetryNode node;
    /** 可变 fixture 字段：{@code config} 保存 {@code TbGetTelemetryNodeConfiguration} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    TbGetTelemetryNodeConfiguration config;
    /** 可变 fixture 字段：{@code nodeConfiguration} 保存 {@code TbNodeConfiguration} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    TbNodeConfiguration nodeConfiguration;
    /** 可变 fixture 字段：{@code ctx} 保存 {@code TbContext} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    TbContext ctx;

    /**
     * 生命周期方法：{@code setUp} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @Before
    public void setUp() throws Exception {
        ctx = mock(TbContext.class);
        node = spy(new TbGetTelemetryNode());
        config = new TbGetTelemetryNodeConfiguration();
        config.setFetchMode("ALL");
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        willCallRealMethod().given(node).parseAggregationConfig(any());
    }

    /**
     * 测试方法：覆盖 {@code givenAggregationAsString_whenParseAggregation_thenReturnEnum} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenAggregationAsString_whenParseAggregation_thenReturnEnum() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        //compatibility with old configs without "aggregation" parameter
        assertThat(node.parseAggregationConfig(null), is(Aggregation.NONE));
        assertThat(node.parseAggregationConfig(""), is(Aggregation.NONE));

        //common values
        assertThat(node.parseAggregationConfig("MIN"), is(Aggregation.MIN));
        assertThat(node.parseAggregationConfig("MAX"), is(Aggregation.MAX));
        assertThat(node.parseAggregationConfig("AVG"), is(Aggregation.AVG));
        assertThat(node.parseAggregationConfig("SUM"), is(Aggregation.SUM));
        assertThat(node.parseAggregationConfig("COUNT"), is(Aggregation.COUNT));
        assertThat(node.parseAggregationConfig("NONE"), is(Aggregation.NONE));

        //all possible values in future
        for (Aggregation aggEnum : Aggregation.values()) {
            assertThat(node.parseAggregationConfig(aggEnum.name()), is(aggEnum));
        }
    }

    /**
     * 测试方法：覆盖 {@code givenAggregationWhiteSpace_whenParseAggregation_thenException} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenAggregationWhiteSpace_whenParseAggregation_thenException() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            node.parseAggregationConfig(" ");
        });
    }

    /**
     * 测试方法：覆盖 {@code givenAggregationIncorrect_whenParseAggregation_thenException} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenAggregationIncorrect_whenParseAggregation_thenException() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            node.parseAggregationConfig("TOP");
        });
    }

}
/*
 * 本类总结：{@code TbGetTelemetryNodeTest} 为 {@code TbGetTelemetryNode} 的 元数据增强节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
