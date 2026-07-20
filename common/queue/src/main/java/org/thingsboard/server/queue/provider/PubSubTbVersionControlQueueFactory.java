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
package org.thingsboard.server.queue.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.pubsub.TbPubSubAdmin;
import org.thingsboard.server.queue.pubsub.TbPubSubConsumerTemplate;
import org.thingsboard.server.queue.pubsub.TbPubSubProducerTemplate;
import org.thingsboard.server.queue.pubsub.TbPubSubSettings;
import org.thingsboard.server.queue.pubsub.TbPubSubSubscriptionSettings;
import org.thingsboard.server.queue.settings.TbQueueCoreSettings;
import org.thingsboard.server.queue.settings.TbQueueVersionControlSettings;

import javax.annotation.PreDestroy;

/**
 * 中文说明：
 * 1. `PubSubTbVersionControlQueueFactory` 是 ThingsBoard Common Queue 中创建或提供队列对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `TbVersionControlQueueFactory`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Component
@ConditionalOnExpression("'${queue.type:null}'=='pubsub' && '${service.type:null}'=='tb-vc-executor'")
public class PubSubTbVersionControlQueueFactory implements TbVersionControlQueueFactory {

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final TbPubSubSettings pubSubSettings;
    private final TbQueueCoreSettings coreSettings;
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final TbQueueVersionControlSettings vcSettings;
    private final TopicService topicService;

    /**
     * `coreAdmin` 字段，保存当前对象的对应属性。
     */
    private final TbQueueAdmin coreAdmin;
    private final TbQueueAdmin notificationAdmin;
    /**
     * `vcAdmin` 字段，保存当前对象的对应属性。
     */
    private final TbQueueAdmin vcAdmin;

    /**
     * 功能：创建 `PubSubTbVersionControlQueueFactory` 实例，并初始化必要字段。
     * 参数：
     * - `pubSubSettings`：配置对象。
     * - `coreSettings`：配置对象。
     * - `vcSettings`：配置对象。
     * - `pubSubSubscriptionSettings`：配置对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public PubSubTbVersionControlQueueFactory(TbPubSubSettings pubSubSettings,
                                              TbQueueCoreSettings coreSettings,
                                              TbQueueVersionControlSettings vcSettings,
                                              TbPubSubSubscriptionSettings pubSubSubscriptionSettings,
                                              TopicService topicService
    ) {
        this.pubSubSettings = pubSubSettings;
        this.coreSettings = coreSettings;
        this.vcSettings = vcSettings;
        this.topicService = topicService;

        this.coreAdmin = new TbPubSubAdmin(pubSubSettings, pubSubSubscriptionSettings.getCoreSettings());
        this.notificationAdmin = new TbPubSubAdmin(pubSubSettings, pubSubSubscriptionSettings.getNotificationsSettings());
        this.vcAdmin = new TbPubSubAdmin(pubSubSettings, pubSubSubscriptionSettings.getVcSettings());
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer() {
        return new TbPubSubProducerTemplate<>(coreAdmin, pubSubSettings, topicService.buildTopicName(coreSettings.getUsageStatsTopic()));
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer() {
        return new TbPubSubProducerTemplate<>(notificationAdmin, pubSubSettings, topicService.buildTopicName(coreSettings.getTopic()));
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToVersionControlServiceMsg>> createToVersionControlMsgConsumer() {
        return new TbPubSubConsumerTemplate<>(vcAdmin, pubSubSettings, topicService.buildTopicName(vcSettings.getTopic()),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportProtos.ToVersionControlServiceMsg.parseFrom(msg.getData()), msg.getHeaders())
        );
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    private void destroy() {
        if (coreAdmin != null) {
            coreAdmin.destroy();
        }
        if (notificationAdmin != null) {
            notificationAdmin.destroy();
        }
        if (vcAdmin != null) {
            vcAdmin.destroy();
        }
    }
}
