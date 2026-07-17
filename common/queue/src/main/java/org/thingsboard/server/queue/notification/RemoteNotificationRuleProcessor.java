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
package org.thingsboard.server.queue.notification;

import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTrigger;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `RemoteNotificationRuleProcessor` 是 ThingsBoard Common Queue 中处理通知的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `NotificationRuleProcessor`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Service
@ConditionalOnMissingBean(value = NotificationRuleProcessor.class, ignored = RemoteNotificationRuleProcessor.class)
@RequiredArgsConstructor
@Slf4j
public class RemoteNotificationRuleProcessor implements NotificationRuleProcessor {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final NotificationDeduplicationService deduplicationService;
    private final TbQueueProducerProvider producerProvider;
    /**
     * 主题，提供当前类调用的业务操作。
     */
    private final TopicService topicService;
    private final PartitionService partitionService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final DataDecodingEncodingService encodingService;

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `trigger`：`trigger` 参数。
     * 返回：无。
     */
    @Override
    public void process(NotificationRuleTrigger trigger) {
        try {
            if (trigger.deduplicate() && deduplicationService.alreadyProcessed(trigger)) {
                return;
            }

            log.debug("Submitting notification rule trigger: {}", trigger);
            TransportProtos.NotificationRuleProcessorMsg.Builder msg = TransportProtos.NotificationRuleProcessorMsg.newBuilder()
                    .setTrigger(ByteString.copyFrom(encodingService.encode(trigger)));

            partitionService.getAllServiceIds(ServiceType.TB_CORE).stream().findAny().ifPresent(serviceId -> {
                TopicPartitionInfo tpi = topicService.getNotificationsTopic(ServiceType.TB_CORE, serviceId);
                producerProvider.getTbCoreNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(),
                        TransportProtos.ToCoreNotificationMsg.newBuilder()
                                .setNotificationRuleProcessorMsg(msg)
                                .build()), null);
            });
        } catch (Throwable e) {
            log.error("Failed to submit notification rule trigger: {}", trigger, e);
        }
    }

}
