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
package org.thingsboard.rule.engine.rabbitmq;

import com.rabbitmq.client.ConnectionFactory;
import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.Collections;
import java.util.Map;

/**
 * `TbRabbitMqNodeConfiguration` 类，封装当前模块中的一组相关职责。
 */
@Data
public class TbRabbitMqNodeConfiguration implements NodeConfiguration<TbRabbitMqNodeConfiguration> {

    /**
     * 名称，用于展示或标识当前对象。
     */
    private String exchangeNamePattern;
    /**
     * 键，用于定位映射、配置或数据项。
     */
    private String routingKeyPattern;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private String messageProperties;
    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    private String host;
    /**
     * 端口号，用于描述服务监听或访问地址。
     */
    private int port;
    /**
     * RabbitMQ virtual host。
     */
    private String virtualHost;
    /**
     * 用户名，用于认证或安全校验。
     */
    private String username;
    /**
     * 密码，用于认证或安全校验。
     */
    private String password;
    /**
     * 是否启用`automatic recovery`。
     */
    private boolean automaticRecoveryEnabled;
    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    private int connectionTimeout;
    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    private int handshakeTimeout;
    /**
     * 客户端映射关系，用于按键查找对应值。
     */
    private Map<String, String> clientProperties;

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbRabbitMqNodeConfiguration defaultConfiguration() {
        TbRabbitMqNodeConfiguration configuration = new TbRabbitMqNodeConfiguration();
        configuration.setExchangeNamePattern("");
        configuration.setRoutingKeyPattern("");
        configuration.setMessageProperties(null);
        configuration.setHost(ConnectionFactory.DEFAULT_HOST);
        configuration.setPort(ConnectionFactory.DEFAULT_AMQP_PORT);
        configuration.setVirtualHost(ConnectionFactory.DEFAULT_VHOST);
        configuration.setUsername(ConnectionFactory.DEFAULT_USER);
        configuration.setPassword(ConnectionFactory.DEFAULT_PASS);
        configuration.setAutomaticRecoveryEnabled(false);
        configuration.setConnectionTimeout(ConnectionFactory.DEFAULT_CONNECTION_TIMEOUT);
        configuration.setHandshakeTimeout(ConnectionFactory.DEFAULT_HANDSHAKE_TIMEOUT);
        configuration.setClientProperties(Collections.emptyMap());
        return configuration;
    }
}
