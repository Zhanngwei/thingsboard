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
package org.thingsboard.server.queue.azure.servicebus;

import com.google.gson.Gson;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 中文说明：
 * 1. 类目的：`TbServiceBusProducerTemplate` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public class TbServiceBusProducerTemplate<T extends TbQueueMsg> implements TbQueueProducer<T> {
    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    private final String defaultTopic;
    private final Gson gson = new Gson();
    /**
     * `admin` 字段，保存当前对象的对应属性。
     */
    private final TbQueueAdmin admin;
    private final TbServiceBusSettings serviceBusSettings;
    private final Map<String, QueueClient> clients = new ConcurrentHashMap<>();
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final ExecutorService executorService;

    /**
     * 功能：创建 `TbServiceBusProducerTemplate` 实例，并初始化必要字段。
     * 参数：
     * - `admin`：`admin` 参数。
     * - `serviceBusSettings`：服务对象。
     * - `defaultTopic`：主题名称或主题对象。
     * 返回：新创建的对象实例。
     */
    public TbServiceBusProducerTemplate(TbQueueAdmin admin, TbServiceBusSettings serviceBusSettings, String defaultTopic) {
        this.admin = admin;
        this.defaultTopic = defaultTopic;
        this.serviceBusSettings = serviceBusSettings;
        executorService = Executors.newCachedThreadPool();
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void init() {

    }

    /**
     * 功能：获取主题。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getDefaultTopic() {
        return defaultTopic;
    }

    /**
     * 功能：执行 `send` 对应的处理。
     * 参数：
     * - `tpi`：`tpi` 参数。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void send(TopicPartitionInfo tpi, T msg, TbQueueCallback callback) {
        IMessage message = new Message(gson.toJson(new DefaultTbQueueMsg(msg)));
        CompletableFuture<Void> future = getClient(tpi.getFullTopicName()).sendAsync(message);
        future.whenCompleteAsync((success, err) -> {
            if (err != null) {
                callback.onFailure(err);
            } else {
                callback.onSuccess(null);
            }
        }, executorService);
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void stop() {
        clients.forEach((t, client) -> {
            try {
                client.close();
            } catch (ServiceBusException e) {
                log.error("Failed to close QueueClient.", e);
            }
        });

        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    /**
     * 功能：获取客户端。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：处理结果。
     */
    private QueueClient getClient(String topic) {
        return clients.computeIfAbsent(topic, k -> {
            admin.createTopicIfNotExists(topic);
            ConnectionStringBuilder builder =
                    new ConnectionStringBuilder(
                            serviceBusSettings.getNamespaceName(),
                            topic,
                            serviceBusSettings.getSasKeyName(),
                            serviceBusSettings.getSasKey());
            try {
                return new QueueClient(builder, ReceiveMode.PEEKLOCK);
            } catch (InterruptedException | ServiceBusException e) {
                log.error("Failed to create new client for the Queue: [{}]", topic, e);
                throw new RuntimeException("Failed to create new client for the Queue", e);
            }
        });
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbServiceBusProducerTemplate` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
