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

import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToVersionControlServiceMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

/**
 * Responsible for providing various Producers to other services.
 */
/**
 * 中文说明：
 * 1. `TbQueueProducerProvider` 是 ThingsBoard Common Queue 中定义队列能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface TbQueueProducerProvider {

    /**
     * Used to push messages to instances of TB Transport Service
     *
     * @return
     */
    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：处理结果。
     */
    TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> getTransportNotificationsMsgProducer();

    /**
     * Used to push messages to instances of TB RuleEngine Service
     *
     * @return
     */
    /**
     * 功能：获取规则引擎。
     * 参数：无。
     * 返回：处理结果。
     */
    TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> getRuleEngineMsgProducer();

    /**
     * Used to push notifications to instances of TB RuleEngine Service
     *
     * @return
     */
    /**
     * 功能：获取规则引擎。
     * 参数：无。
     * 返回：处理结果。
     */
    TbQueueProducer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> getRuleEngineNotificationsMsgProducer();

    /**
     * Used to push messages to other instances of TB Core Service
     *
     * @return
     */
    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：处理结果。
     */
    TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> getTbCoreMsgProducer();

    /**
     * Used to push messages to other instances of TB Core Service
     *
     * @return
     */
    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：处理结果。
     */
    TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> getTbCoreNotificationsMsgProducer();

    /**
     * Used to push messages to other instances of TB Core Service
     *
     * @return
     */
    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：处理结果。
     */
    TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> getTbUsageStatsMsgProducer();

        /**
     * Used to push messages to other instances of TB Core Service
     *
     * @return
     */
    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：处理结果。
     */
    TbQueueProducer<TbProtoQueueMsg<ToVersionControlServiceMsg>> getTbVersionControlMsgProducer();
}
