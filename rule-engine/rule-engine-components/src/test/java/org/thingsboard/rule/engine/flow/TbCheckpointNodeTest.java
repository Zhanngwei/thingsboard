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
package org.thingsboard.rule.engine.flow;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.provider.Arguments;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.TbNode;

import java.util.stream.Stream;

import static org.mockito.Mockito.spy;

/**
 * 测试目标：验证 {@code TbCheckpointNodeTest} 覆盖的 流程控制节点 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbCheckpointNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
@Slf4j
public class TbCheckpointNodeTest extends AbstractRuleNodeUpgradeTest {

    // Rule nodes upgrade
    /** 参数源方法：{@code givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig} 生成参数化测试输入组合，期望由消费它的测试方法断言。 */
    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 0
                Arguments.of(0,
                        "{\"queueName\":null}",
                        true,
                        "{}"),
                // default config for version 0 with queueName
                Arguments.of(0,
                        "{\"queueName\":\"Main\"}",
                        true,
                        "{}"),
                // default config for version 1 with upgrade from version 0
                Arguments.of(0,
                        "{}",
                        false,
                        "{}")
        );
    }

    /** 实现方法：{@code getTestNode} 为测试替身或抽象基类提供最小行为，输入来自调用方，生命周期随 enclosing fixture。 */
    @Override
    protected TbNode getTestNode() {
        return spy(TbCheckpointNode.class);
    }
}
/*
 * 本类总结：{@code TbCheckpointNodeTest} 为 {@code TbCheckpointNode} 的 流程控制节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
