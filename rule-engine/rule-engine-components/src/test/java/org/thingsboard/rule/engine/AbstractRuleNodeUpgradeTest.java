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
package org.thingsboard.rule.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.util.TbPair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.willCallRealMethod;

/**
 * `AbstractRuleNodeUpgradeTest` 测试类，用于验证 `AbstractRuleNodeUpgrade` 相关行为。
 */
public abstract class AbstractRuleNodeUpgradeTest {

    /**
     * 功能：获取节点实例。
     * 参数：无。
     * 返回：处理结果。
     */
    protected abstract TbNode getTestNode();

    /**
     * 功能：验证 `givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig` 描述的测试场景。
     * 参数：
     * - `givenVersion`：`givenVersion` 参数。
     * - `givenConfigStr`：配置对象。
     * - `hasChanges`：`hasChanges` 参数。
     * - `expectedConfigStr`：配置对象。
     * 返回：无。
     */
    @ParameterizedTest
    @MethodSource
    public void givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig(int givenVersion, String givenConfigStr, boolean hasChanges, String expectedConfigStr) throws TbNodeException {
        // GIVEN
        willCallRealMethod().given(getTestNode()).upgrade(anyInt(), any());
        JsonNode givenConfig = JacksonUtil.toJsonNode(givenConfigStr);
        JsonNode expectedConfig = JacksonUtil.toJsonNode(expectedConfigStr);

        // WHEN
        TbPair<Boolean, JsonNode> upgradeResult = getTestNode().upgrade(givenVersion, givenConfig);

        // THEN
        assertThat(upgradeResult.getFirst()).isEqualTo(hasChanges);
        ObjectNode upgradedConfig = (ObjectNode) upgradeResult.getSecond();
        assertThat(upgradedConfig).isEqualTo(expectedConfig);
    }
}
/*
 * 本类总结：{@code AbstractRuleNodeUpgradeTest} 为 {@code AbstractRuleNodeUpgrade} 的 规则引擎组件 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
