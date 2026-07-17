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
package org.thingsboard.server.queue.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 中文说明：
 * 1. `PropertyUtilsTest` 是 ThingsBoard Common Queue 中验证 `PropertyUtils` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
class PropertyUtilsTest {

    /**
     * 功能：验证 `givenNullOrEmpty_whenGetConfig_thenEmptyMap` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenNullOrEmpty_whenGetConfig_thenEmptyMap() {
        assertThat(PropertyUtils.getProps(null)).as("null property").isEmpty();
        assertThat(PropertyUtils.getProps("")).as("empty property").isEmpty();
        assertThat(PropertyUtils.getProps(";")).as("ends with ;").isEmpty();
    }

    /**
     * 功能：验证 `givenKafkaOtherProperties_whenGetConfig_thenReturnMappedValues` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenKafkaOtherProperties_whenGetConfig_thenReturnMappedValues() {
        assertThat(PropertyUtils.getProps("metrics.recording.level:INFO;metrics.sample.window.ms:30000"))
                .as("two pairs")
                .isEqualTo(Map.of(
                        "metrics.recording.level", "INFO",
                        "metrics.sample.window.ms", "30000"
                ));

        assertThat(PropertyUtils.getProps("metrics.recording.level:INFO;metrics.sample.window.ms:30000" + ";"))
                .as("two pairs ends with ;")
                .isEqualTo(Map.of(
                        "metrics.recording.level", "INFO",
                        "metrics.sample.window.ms", "30000"
                ));
    }

    /**
     * 功能：验证 `givenKafkaTopicProperties_whenGetConfig_thenReturnMappedValues` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenKafkaTopicProperties_whenGetConfig_thenReturnMappedValues() {
        assertThat(PropertyUtils.getProps("retention.ms:604800000;segment.bytes:26214400;retention.bytes:1048576000;partitions:1;min.insync.replicas:1"))
                .isEqualTo(Map.of(
                        "retention.ms", "604800000",
                        "segment.bytes", "26214400",
                        "retention.bytes", "1048576000",
                        "partitions", "1",
                        "min.insync.replicas", "1"
                ));
    }

}
