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
 * 1. 类目的：`LwM2MTransportServerConfig` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
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

/*
 * 本类总结：
 * 1. 核心职责：`LwM2MTransportServerConfig` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
