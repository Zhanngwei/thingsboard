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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.config.ssl.SslCredentials;
import org.thingsboard.server.common.transport.config.ssl.SslCredentialsConfig;

/**
 * 中文说明：
 * 1. `LwM2MTransportBootstrapConfig` 是 ThingsBoard Common Transport 中描述 LwM2M 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `LwM2MSecureServerConfig`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Slf4j
@Component
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' || '${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core')  && '${transport.lwm2m.enabled:false}'=='true' && '${transport.lwm2m.bootstrap.enabled:false}'=='true'")
public class LwM2MTransportBootstrapConfig implements LwM2MSecureServerConfig {

    /**
     * `id`ID，用于定位对应业务对象。
     */
    @Getter
    @Value("${transport.lwm2m.bootstrap.id:}")
    private Integer id;

    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    @Getter
    @Value("${transport.lwm2m.bootstrap.bind_address:}")
    private String host;

    /**
     * 端口号，用于描述服务监听或访问地址。
     */
    @Getter
    @Value("${transport.lwm2m.bootstrap.bind_port:}")
    private Integer port;

    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    @Getter
    @Value("${transport.lwm2m.bootstrap.security.bind_address:}")
    private String secureHost;

    /**
     * 端口号，用于描述服务监听或访问地址。
     */
    @Getter
    @Value("${transport.lwm2m.bootstrap.security.bind_port:}")
    private Integer securePort;

    /**
     * 功能：执行 `lwm2mBootstrapCredentials` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Bean
    @ConfigurationProperties(prefix = "transport.lwm2m.bootstrap.security.credentials")
    public SslCredentialsConfig lwm2mBootstrapCredentials() {
        return new SslCredentialsConfig("LWM2M Bootstrap DTLS Credentials", false);
    }

    /**
     * 凭据，保存当前对象的配置选项。
     */
    @Autowired
    @Qualifier("lwm2mBootstrapCredentials")
    private SslCredentialsConfig credentialsConfig;

    /**
     * 功能：获取凭据。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public SslCredentials getSslCredentials() {
        return this.credentialsConfig.getCredentials();
    }
}
