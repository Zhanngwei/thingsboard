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
package org.thingsboard.server.service.transport;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.UUID;
import java.util.function.Consumer;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * 中文说明：
 * 1. 类目的：`DefaultTbCoreToTransportService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Slf4j
@Service
@TbCoreComponent
public class DefaultTbCoreToTransportService implements TbCoreToTransportService {

    /**
     * 主题，提供当前类调用的业务操作。
     */
    private final TopicService topicService;
    private final TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> tbTransportProducer;

    /**
     * 功能：创建 `DefaultTbCoreToTransportService` 实例，并初始化必要字段。
     * 参数：
     * - `topicService`：服务对象。
     * - `tbQueueProducerProvider`：队列名称或队列对象。
     * 返回：新创建的对象实例。
     */
    public DefaultTbCoreToTransportService(TopicService topicService, TbQueueProducerProvider tbQueueProducerProvider) {
        this.topicService = topicService;
        this.tbTransportProducer = tbQueueProducerProvider.getTransportNotificationsMsgProducer();
    }

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `nodeId`：节点实例ID。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void process(String nodeId, ToTransportMsg msg) {
        process(nodeId, msg, null, null);
    }

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `nodeId`：节点实例ID。
     * - `msg`：待处理消息。
     * - `onSuccess`：`onSuccess` 参数。
     * - `onFailure`：`onFailure` 参数。
     * 返回：无。
     */
    @Override
    public void process(String nodeId, ToTransportMsg msg, Runnable onSuccess, Consumer<Throwable> onFailure) {
        if (nodeId == null || nodeId.isEmpty()) {
            log.trace("process: skipping message without nodeId [{}], (ToTransportMsg) msg [{}]", nodeId, msg);
            if (onSuccess != null) {
                onSuccess.run();
            }
            return;
        }
        TopicPartitionInfo tpi = topicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, nodeId);
        UUID sessionId = new UUID(msg.getSessionIdMSB(), msg.getSessionIdLSB());
        log.trace("[{}][{}] Pushing session data to topic: {}", tpi.getFullTopicName(), sessionId, msg);
        TbProtoQueueMsg<ToTransportMsg> queueMsg = new TbProtoQueueMsg<>(NULL_UUID, msg);
        tbTransportProducer.send(tpi, queueMsg, new QueueCallbackAdaptor(onSuccess, onFailure));
    }

    /**
     * 中文说明：
     * 1. 类目的：`QueueCallbackAdaptor` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
     * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Service / Facade。
     */
    private static class QueueCallbackAdaptor implements TbQueueCallback {
        /**
         * `onSuccess` 字段，保存当前对象的对应属性。
         */
        private final Runnable onSuccess;
        private final Consumer<Throwable> onFailure;

        QueueCallbackAdaptor(Runnable onSuccess, Consumer<Throwable> onFailure) {
            this.onSuccess = onSuccess;
            this.onFailure = onFailure;
        }

        /**
         * 功能：处理`on Success`。
         * 参数：
         * - `metadata`：待处理数据。
         * 返回：无。
         */
        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
            if (onSuccess != null) {
                onSuccess.run();
            }
        }

        /**
         * 功能：处理失败信息。
         * 参数：
         * - `t`：`t` 参数。
         * 返回：无。
         */
        @Override
        public void onFailure(Throwable t) {
            if (onFailure != null) {
                onFailure.accept(t);
            }
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbCoreToTransportService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
