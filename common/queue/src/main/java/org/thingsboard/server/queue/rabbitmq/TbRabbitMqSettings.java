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
package org.thingsboard.server.queue.rabbitmq;

import com.rabbitmq.client.ConnectionFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 中文说明：
 * 1. `TbRabbitMqSettings` 是 ThingsBoard Common Queue 中描述 `Tb Rabbit Mq` 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 它直接协作于配置加载组件和使用这些配置的运行类型。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Slf4j
@ConditionalOnExpression("'${queue.type:null}'=='rabbitmq'")
@Component
@Data
public class TbRabbitMqSettings {
    /**
     * 名称，用于标识或展示当前对象。
     */
    @Value("${queue.rabbitmq.exchange_name:}")
    private String exchangeName;
    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    @Value("${queue.rabbitmq.host:}")
    private String host;
    /**
     * 端口号，用于描述服务监听或访问地址。
     */
    @Value("${queue.rabbitmq.port:}")
    private int port;
    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    @Value("${queue.rabbitmq.virtual_host:}")
    private String virtualHost;
    /**
     * 用户名，用于认证或安全校验。
     */
    @Value("${queue.rabbitmq.username:}")
    private String username;
    /**
     * 密码，用于认证或安全校验。
     */
    @Value("${queue.rabbitmq.password:}")
    private String password;
    /**
     * 是否启用`automatic recovery`。
     */
    @Value("${queue.rabbitmq.automatic_recovery_enabled:}")
    private boolean automaticRecoveryEnabled;
    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @Value("${queue.rabbitmq.connection_timeout:}")
    private int connectionTimeout;
    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @Value("${queue.rabbitmq.handshake_timeout:}")
    private int handshakeTimeout;

    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    private ConnectionFactory connectionFactory;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    private void init() {
        connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setVirtualHost(virtualHost);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setAutomaticRecoveryEnabled(automaticRecoveryEnabled);
        connectionFactory.setConnectionTimeout(connectionTimeout);
        connectionFactory.setHandshakeTimeout(handshakeTimeout);
    }
}
