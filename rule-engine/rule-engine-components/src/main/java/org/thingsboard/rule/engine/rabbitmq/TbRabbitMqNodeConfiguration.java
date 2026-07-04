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

@Data
/**
 * RabbitMQ 节点配置模型，保存连接参数、目标 exchange/routingKey 模板和消息属性。
 * 配置类本身不直接建立 RabbitMQ 连接、不执行发布，也不涉及异步回调、数据库或缓存。
 */
public class TbRabbitMqNodeConfiguration implements NodeConfiguration<TbRabbitMqNodeConfiguration> {

    /**
     * RabbitMQ exchange 名称模板，空值表示默认 exchange。
     */
    private String exchangeNamePattern;
    /**
     * RabbitMQ routing key 模板，空值表示空 routing key。
     */
    private String routingKeyPattern;
    /**
     * RabbitMQ MessageProperties 常量名称。
     */
    private String messageProperties;
    /**
     * RabbitMQ Broker 主机。
     */
    private String host;
    /**
     * RabbitMQ Broker 端口。
     */
    private int port;
    /**
     * RabbitMQ virtual host。
     */
    private String virtualHost;
    /**
     * RabbitMQ 用户名。
     */
    private String username;
    /**
     * RabbitMQ 密码。
     */
    private String password;
    /**
     * 是否启用 RabbitMQ 客户端自动恢复。
     */
    private boolean automaticRecoveryEnabled;
    /**
     * RabbitMQ 连接超时时间。
     */
    private int connectionTimeout;
    /**
     * RabbitMQ 握手超时时间。
     */
    private int handshakeTimeout;
    /**
     * 透传到 RabbitMQ ConnectionFactory 的客户端属性。
     */
    private Map<String, String> clientProperties;

    /**
     * 构造 RabbitMQ 节点默认配置。
     * 本方法只设置默认值，不直接连接 Broker，也不处理 Rule Engine 消息确认。
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

/*
 * 本类总结：
 * 本类提供 RabbitMQ 外部节点的连接和发布目标配置，实际连接与 basicPublish 由 TbRabbitMqNode 执行。
 * 配置类本身没有外部调用、异步回调、数据库或缓存逻辑。
 */
