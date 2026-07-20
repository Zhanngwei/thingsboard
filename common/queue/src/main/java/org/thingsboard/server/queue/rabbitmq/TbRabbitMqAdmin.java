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

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.queue.TbQueueAdmin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * 中文说明：
 * 1. `TbRabbitMqAdmin` 是 ThingsBoard Common Queue 中围绕 `Tb Rabbit Mq` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `TbQueueAdmin`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Slf4j
public class TbRabbitMqAdmin implements TbQueueAdmin {

    /**
     * 网络通道，表示当前网络连接使用的通道。
     */
    private final Channel channel;
    private final Connection connection;
    /**
     * 参数映射关系，用于按键查找对应值。
     */
    private final Map<String, Object> arguments;

    /**
     * 功能：创建 `TbRabbitMqAdmin` 实例，并初始化必要字段。
     * 参数：
     * - `rabbitMqSettings`：配置对象。
     * - `arguments`：键值映射。
     * 返回：新创建的对象实例。
     */
    public TbRabbitMqAdmin(TbRabbitMqSettings rabbitMqSettings, Map<String, Object> arguments) {
        this.arguments = arguments;

        try {
            connection = rabbitMqSettings.getConnectionFactory().newConnection();
        } catch (IOException | TimeoutException e) {
            log.error("Failed to create connection.", e);
            throw new RuntimeException("Failed to create connection.", e);
        }

        try {
            channel = connection.createChannel();
        } catch (IOException e) {
            log.error("Failed to create chanel.", e);
            throw new RuntimeException("Failed to create chanel.", e);
        }
    }

    /**
     * 功能：保存或创建主题。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `properties`：`properties` 参数。
     * 返回：无。
     */
    @Override
    public void createTopicIfNotExists(String topic, String properties) {
        Map<String, Object> arguments = this.arguments;
        if (StringUtils.isNotBlank(properties)) {
            arguments = new HashMap<>(arguments);
            arguments.putAll(TbRabbitMqQueueArguments.getArgs(properties));
        }
        try {
            channel.queueDeclare(topic, false, false, false, arguments);
        } catch (IOException e) {
            log.error("Failed to bind queue: [{}]", topic, e);
        }
    }

    /**
     * 功能：删除或清理主题。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：无。
     */
    @Override
    public void deleteTopic(String topic) {
        try {
            channel.queueDelete(topic);
        } catch (IOException e) {
            log.error("Failed to delete RabbitMq queue [{}].", topic);
        }
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void destroy() {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException | TimeoutException e) {
                log.error("Failed to close Chanel.", e);
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                log.error("Failed to close Connection.", e);
            }
        }
    }
}
