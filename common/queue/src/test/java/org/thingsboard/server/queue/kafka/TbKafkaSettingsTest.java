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
package org.thingsboard.server.queue.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;

/**
 * 中文说明：
 * 1. `TbKafkaSettingsTest` 是 ThingsBoard Common Queue 中验证 `TbKafkaSettings` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@SpringBootTest(classes = TbKafkaSettings.class)
@TestPropertySource(properties = {
        "queue.type=kafka",
        "queue.kafka.bootstrap.servers=localhost:9092",
        "queue.kafka.other-inline=metrics.recording.level:INFO;metrics.sample.window.ms:30000",
})
class TbKafkaSettingsTest {

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    TbKafkaSettings settings;

    /**
     * 功能：执行 `beforeEach` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    void beforeEach() {
        settings = spy(settings); //SpyBean is not aware on @ConditionalOnProperty, that is why the traditional spy in use
    }

    /**
     * 功能：验证 `givenToProps_whenConfigureSSL_thenVerifyOnce` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenToProps_whenConfigureSSL_thenVerifyOnce() {
        Properties props = settings.toProps();

        assertThat(props).as("TB_QUEUE_KAFKA_REQUEST_TIMEOUT_MS").containsEntry("request.timeout.ms", 30000);
        assertThat(props).as("TB_QUEUE_KAFKA_SESSION_TIMEOUT_MS").containsEntry("session.timeout.ms", 10000);

        //other-inline
        assertThat(props).as("metrics.recording.level").containsEntry("metrics.recording.level", "INFO");
        assertThat(props).as("TB_QUEUE_KAFKA_SESSION_TIMEOUT_MS").containsEntry("metrics.sample.window.ms", "30000");

        Mockito.verify(settings).toProps();
        Mockito.verify(settings).configureSSL(any());
    }

    /**
     * 功能：验证 `givenToAdminProps_whenConfigureSSL_thenVerifyOnce` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenToAdminProps_whenConfigureSSL_thenVerifyOnce() {
        settings.toAdminProps();
        Mockito.verify(settings).toProps();
        Mockito.verify(settings).configureSSL(any());
    }

    /**
     * 功能：验证 `givenToConsumerProps_whenConfigureSSL_thenVerifyOnce` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenToConsumerProps_whenConfigureSSL_thenVerifyOnce() {
        settings.toConsumerProps("main");
        Mockito.verify(settings).toProps();
        Mockito.verify(settings).configureSSL(any());
    }

    /**
     * 功能：验证 `givenTotoProducerProps_whenConfigureSSL_thenVerifyOnce` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenTotoProducerProps_whenConfigureSSL_thenVerifyOnce() {
        settings.toProducerProps();
        Mockito.verify(settings).toProps();
        Mockito.verify(settings).configureSSL(any());
    }
}