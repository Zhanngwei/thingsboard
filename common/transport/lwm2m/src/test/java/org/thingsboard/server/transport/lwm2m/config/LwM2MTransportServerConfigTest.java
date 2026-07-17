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
package org.thingsboard.server.transport.lwm2m.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.thingsboard.server.common.transport.config.ssl.SslCredentialsConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 中文说明：
 * 1. `LwM2MTransportServerConfigTest` 是 ThingsBoard Common Transport 中验证 `LwM2MTransportServerConfig` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = LwM2MTransportServerConfig.class)
@ContextConfiguration(classes = {LwM2MTransportServerConfig.class}, loader = SpringBootContextLoader.class)
@TestPropertySource(properties = {
        "transport.sessions.report_timeout=10",
        "transport.lwm2m.security.recommended_ciphers=true",
        "transport.lwm2m.security.recommended_supported_groups=true",
        "transport.lwm2m.downlink_pool_size=10",
        "transport.lwm2m.uplink_pool_size=10",
        "transport.lwm2m.ota_pool_size=10",
        "transport.lwm2m.clean_period_in_sec=2",
        "transport.lwm2m.dtls.connection_id_length="

})
class LwM2MTransportServerConfigTest {

    /**
     * 凭据，保存当前对象的配置选项。
     */
    @MockBean(name = "lwm2mServerCredentials")
    private SslCredentialsConfig credentialsConfig;

    /**
     * 凭据，保存当前对象的配置选项。
     */
    @MockBean(name = "lwm2mTrustCredentials")
    private SslCredentialsConfig trustCredentialsConfig;

    /**
     * 配置，保存当前对象的配置选项。
     */
    @Autowired
    private LwM2MTransportServerConfig serverConfig;

    /**
     * 功能：获取`Dtls Connection Id Length return null is property is empty`。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void getDtlsConnectionIdLength_return_null_is_property_is_empty() {
        // note: transport.lwm2m.dtls.connect_id_length is set in TestPropertySource
        assertThat(serverConfig.getDtlsConnectionIdLength()).isNull();
    }
}