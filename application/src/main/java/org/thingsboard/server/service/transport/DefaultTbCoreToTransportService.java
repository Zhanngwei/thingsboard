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
 * 1. `DefaultTbCoreToTransportService` 是 ThingsBoard Application 中负责传输层的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `TbCoreToTransportService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
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
     * 1. `QueueCallbackAdaptor` 是 ThingsBoard Application 中负责队列接入或传输适配的类型。
     * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
     * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
     * 4. 直接依赖的类型边界包括 `TbQueueCallback`。
     * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
     * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
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
