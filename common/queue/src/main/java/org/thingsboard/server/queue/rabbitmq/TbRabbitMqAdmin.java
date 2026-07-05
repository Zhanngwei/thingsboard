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
 * 1. 类目的：`TbRabbitMqAdmin` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
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

/*
 * 本类总结：
 * 1. 核心职责：`TbRabbitMqAdmin` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
