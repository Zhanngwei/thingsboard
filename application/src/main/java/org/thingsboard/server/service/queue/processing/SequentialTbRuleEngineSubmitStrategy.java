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
package org.thingsboard.server.service.queue.processing;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * 中文说明：
 * 1. 类目的：`SequentialTbRuleEngineSubmitStrategy` 是ThingsBoard Application 模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
 */
@Slf4j
public class SequentialTbRuleEngineSubmitStrategy extends AbstractTbRuleEngineSubmitStrategy {

    private final AtomicInteger msgIdx = new AtomicInteger(0);
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private volatile BiConsumer<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgConsumer;
    private volatile UUID expectedMsgId;

    /**
     * 功能：创建 `SequentialTbRuleEngineSubmitStrategy` 实例，并初始化必要字段。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * 返回：新创建的对象实例。
     */
    public SequentialTbRuleEngineSubmitStrategy(String queueName) {
        super(queueName);
    }

    /**
     * 功能：发送或提交`Attempt`。
     * 参数：
     * - `msgConsumer`：待处理消息。
     * 返回：无。
     */
    @Override
    public void submitAttempt(BiConsumer<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgConsumer) {
        this.msgConsumer = msgConsumer;
        msgIdx.set(0);
        submitNext();
    }

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `reprocessMap`：键值映射。
     * 返回：无。
     */
    @Override
    public void update(ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> reprocessMap) {
        super.update(reprocessMap);
    }

    /**
     * 功能：执行 `doOnSuccess` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * 返回：无。
     */
    @Override
    protected void doOnSuccess(UUID id) {
        if (expectedMsgId.equals(id)) {
            msgIdx.incrementAndGet();
            submitNext();
        }
    }

    /**
     * 功能：发送或提交`Next`。
     * 参数：无。
     * 返回：无。
     */
    private void submitNext() {
        int listSize = orderedMsgList.size();
        int idx = msgIdx.get();
        if (idx < listSize) {
            IdMsgPair<TransportProtos.ToRuleEngineMsg> pair = orderedMsgList.get(idx);
            expectedMsgId = pair.uuid;
            if (log.isDebugEnabled()) {
                log.debug("[{}] submitting [{}] message to rule engine", queueName, pair.msg);
            }
            msgConsumer.accept(pair.uuid, pair.msg);
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`SequentialTbRuleEngineSubmitStrategy` 在 ThingsBoard Application 模块 中承担队列服务类型职责，核心目的是封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 核心流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
 * 3. 关键依赖：主要依赖或协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
