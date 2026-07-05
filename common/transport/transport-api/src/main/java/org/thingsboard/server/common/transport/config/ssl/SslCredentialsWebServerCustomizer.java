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
package org.thingsboard.server.common.transport.config.ssl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.SslStoreProvider;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.security.KeyStore;

/**
 * 中文说明：
 * 1. 类目的：`SslCredentialsWebServerCustomizer` 是ThingsBoard Common 模块中的传输协议契约或适配类型，用于抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 生命周期：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Adapter / Strategy / Command。
 */
@Component
@ConditionalOnExpression("'${spring.main.web-environment:true}'=='true' && '${server.ssl.enabled:false}'=='true'")
public class SslCredentialsWebServerCustomizer implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    /**
     * 功能：执行 `httpServerSslCredentials` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Bean
    @ConfigurationProperties(prefix = "server.ssl.credentials")
    public SslCredentialsConfig httpServerSslCredentials() {
        return new SslCredentialsConfig("HTTP Server SSL Credentials", false);
    }

    /**
     * 凭据，保存当前对象的配置选项。
     */
    @Autowired
    @Qualifier("httpServerSslCredentials")
    private SslCredentialsConfig httpServerSslCredentialsConfig;

    /**
     * 服务端，用于支撑当前网络或外部服务交互。
     */
    private final ServerProperties serverProperties;

    /**
     * 功能：创建 `SslCredentialsWebServerCustomizer` 实例，并初始化必要字段。
     * 参数：
     * - `serverProperties`：`serverProperties` 参数。
     * 返回：新创建的对象实例。
     */
    public SslCredentialsWebServerCustomizer(ServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }

    /**
     * 功能：执行 `customize` 对应的处理。
     * 参数：
     * - `factory`：`factory` 参数。
     * 返回：无。
     */
    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        SslCredentials sslCredentials = this.httpServerSslCredentialsConfig.getCredentials();
        Ssl ssl = serverProperties.getSsl();
        ssl.setKeyAlias(sslCredentials.getKeyAlias());
        ssl.setKeyPassword(sslCredentials.getKeyPassword());
        factory.setSsl(ssl);
        factory.setSslStoreProvider(new SslStoreProvider() {
            @Override
            public KeyStore getKeyStore() {
                return sslCredentials.getKeyStore();
            }

            @Override
            public KeyStore getTrustStore() {
                return null;
            }
        });
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`SslCredentialsWebServerCustomizer` 在 ThingsBoard Common 模块 中承担传输协议契约或适配类型职责，核心目的是抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 核心流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
