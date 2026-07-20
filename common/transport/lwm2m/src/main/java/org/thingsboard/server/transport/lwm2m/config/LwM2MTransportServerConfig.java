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

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.TbProperty;
import org.thingsboard.server.common.transport.config.ssl.SslCredentials;
import org.thingsboard.server.common.transport.config.ssl.SslCredentialsConfig;

import java.util.List;

/**
 * 中文说明：
 * 1. `LwM2MTransportServerConfig` 是 ThingsBoard Common Transport 中描述 LwM2M 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `LwM2MSecureServerConfig`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Slf4j
@Component
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' || '${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core')  && '${transport.lwm2m.enabled:false}'=='true'")
@ConfigurationProperties(prefix = "transport.lwm2m")
public class LwM2MTransportServerConfig implements LwM2MSecureServerConfig {

    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @Getter
    @Value("${transport.lwm2m.dtls.retransmission_timeout:9000}")
    private int dtlsRetransmissionTimeout;

    /**
     * `dtlsConnectionIdLength` 字段，保存当前对象的对应属性。
     */
    @Getter
    @Value("${transport.lwm2m.dtls.connection_id_length:6}")
    private Integer dtlsConnectionIdLength;

    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @Getter
    @Value("${transport.lwm2m.timeout:}")
    private Long timeout;

    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    @Getter
    @Value("${transport.sessions.report_timeout}")
    private long sessionReportTimeout;

    /**
     * 是否满足`recommendedCiphers`条件。
     */
    @Getter
    @Value("${transport.lwm2m.security.recommended_ciphers:}")
    private boolean recommendedCiphers;

    /**
     * 是否满足`recommendedSupportedGroups`条件。
     */
    @Getter
    @Value("${transport.lwm2m.security.recommended_supported_groups:}")
    private boolean recommendedSupportedGroups;

    /**
     * 下行线程池大小，用于控制处理规模或位置。
     */
    @Getter
    @Value("${transport.lwm2m.downlink_pool_size:}")
    private int downlinkPoolSize;

    /**
     * 上行线程池大小，用于控制处理规模或位置。
     */
    @Getter
    @Value("${transport.lwm2m.uplink_pool_size:}")
    private int uplinkPoolSize;

    /**
     * OTA 线程池大小，用于控制处理规模或位置。
     */
    @Getter
    @Value("${transport.lwm2m.ota_pool_size:}")
    private int otaPoolSize;

    /**
     * 清理周期，用于控制时间范围或等待时长。
     */
    @Getter
    @Value("${transport.lwm2m.clean_period_in_sec:}")
    private int cleanPeriodInSec;

    /**
     * `id`ID，用于定位对应业务对象。
     */
    @Getter
    @Value("${transport.lwm2m.server.id:}")
    private Integer id;

    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    @Getter
    @Value("${transport.lwm2m.server.bind_address:}")
    private String host;

    /**
     * 端口号，用于描述服务监听或访问地址。
     */
    @Getter
    @Value("${transport.lwm2m.server.bind_port:}")
    private Integer port;

    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    @Getter
    @Value("${transport.lwm2m.server.security.bind_address:}")
    private String secureHost;

    /**
     * 端口号，用于描述服务监听或访问地址。
     */
    @Getter
    @Value("${transport.lwm2m.server.security.bind_port:}")
    private Integer securePort;

    /**
     * 定时器，用于安排延迟任务或周期任务。
     */
    @Getter
    @Value("${transport.lwm2m.psm_activity_timer:10000}")
    private long psmActivityTimer;

    /**
     * `pagingTransmissionWindow` 字段，保存当前对象的对应属性。
     */
    @Getter
    @Value("${transport.lwm2m.paging_transmission_window:10000}")
    private long pagingTransmissionWindow;

    /**
     * 配置列表，用于保存一组待处理对象。
     */
    @Getter
    @Setter
    private List<TbProperty> networkConfig;

    /**
     * 功能：执行 `lwm2mServerCredentials` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Bean
    @ConfigurationProperties(prefix = "transport.lwm2m.server.security.credentials")
    public SslCredentialsConfig lwm2mServerCredentials() {
        return new SslCredentialsConfig("LWM2M Server DTLS Credentials", false);
    }

    /**
     * 凭据，保存当前对象的配置选项。
     */
    @Autowired
    @Qualifier("lwm2mServerCredentials")
    private SslCredentialsConfig credentialsConfig;

    /**
     * 功能：执行 `lwm2mTrustCredentials` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Bean
    @ConfigurationProperties(prefix = "transport.lwm2m.security.trust-credentials")
    public SslCredentialsConfig lwm2mTrustCredentials() {
        return new SslCredentialsConfig("LWM2M Trust Credentials", true);
    }

    /**
     * 凭据，保存当前对象的配置选项。
     */
    @Autowired
    @Qualifier("lwm2mTrustCredentials")
    private SslCredentialsConfig trustCredentialsConfig;

    /**
     * 功能：获取凭据。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public SslCredentials getSslCredentials() {
        return this.credentialsConfig.getCredentials();
    }

    /**
     * 功能：获取凭据。
     * 参数：无。
     * 返回：处理结果。
     */
    public SslCredentials getTrustSslCredentials() {
        return this.trustCredentialsConfig.getCredentials();
    }
}
