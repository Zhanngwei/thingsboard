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
package org.thingsboard.rule.engine.telemetry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * `TbMsgAttributesNodeConfigurationTest` 测试类，用于验证 `TbMsgAttributesNodeConfiguration` 相关行为。
 */
class TbMsgAttributesNodeConfigurationTest {

    /**
     * 功能：验证配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void testDefaultConfig_givenUpdateAttributesOnlyOnValueChange_thenTrue_sinceVersion1() {
        assertThat(new TbMsgAttributesNodeConfiguration().defaultConfiguration().isUpdateAttributesOnlyOnValueChange()).isTrue();
    }

}
/*
 * 本类总结：{@code TbMsgAttributesNodeConfigurationTest} 为 {@code TbMsgAttributesNodeConfiguration} 的 遥测与属性节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
