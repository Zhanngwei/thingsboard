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
 * `TbCheckpointNodeTest` 测试类，用于验证 `TbCheckpointNode` 相关行为。
 */
@Slf4j
public class TbCheckpointNodeTest extends AbstractRuleNodeUpgradeTest {

    // Rule nodes upgrade
    /**
     * 功能：验证 `givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig` 描述的测试场景。
     * 参数：无。
     * 返回：处理结果。
     */
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

    /**
     * 功能：获取节点实例。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected TbNode getTestNode() {
        return spy(TbCheckpointNode.class);
    }
}
/*
 * 本类总结：{@code TbCheckpointNodeTest} 为 {@code TbCheckpointNode} 的 流程控制节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
