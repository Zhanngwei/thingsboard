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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * 中文说明：
 * 1. 类目的：`BatchTbRuleEngineSubmitStrategy` 是ThingsBoard Application 模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
 */
@Slf4j
public class BatchTbRuleEngineSubmitStrategy extends AbstractTbRuleEngineSubmitStrategy {

    /**
     * 批量大小，用于控制处理规模或位置。
     */
    private final int batchSize;
    private final AtomicInteger packIdx = new AtomicInteger(0);
    private final Map<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> pendingPack = new LinkedHashMap<>();
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private volatile BiConsumer<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgConsumer;

    /**
     * 功能：创建 `BatchTbRuleEngineSubmitStrategy` 实例，并初始化必要字段。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * - `batchSize`：`batchSize` 参数。
     * 返回：新创建的对象实例。
     */
    public BatchTbRuleEngineSubmitStrategy(String queueName, int batchSize) {
        super(queueName);
        this.batchSize = batchSize;
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
        packIdx.set(0);
    }

    /**
     * 功能：执行 `doOnSuccess` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * 返回：无。
     */
    @Override
    protected void doOnSuccess(UUID id) {
        boolean endOfPendingPack;
        synchronized (pendingPack) {
            TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg> msg = pendingPack.remove(id);
            endOfPendingPack = msg != null && pendingPack.isEmpty();
        }
        if (endOfPendingPack) {
            packIdx.incrementAndGet();
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
        int startIdx = Math.min(packIdx.get() * batchSize, listSize);
        int endIdx = Math.min(startIdx + batchSize, listSize);
        Map<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> tmpPack;
        synchronized (pendingPack) {
            pendingPack.clear();
            for (int i = startIdx; i < endIdx; i++) {
                IdMsgPair<TransportProtos.ToRuleEngineMsg> pair = orderedMsgList.get(i);
                pendingPack.put(pair.uuid, pair.msg);
            }
            tmpPack = new LinkedHashMap<>(pendingPack);
        }
        int submitSize = pendingPack.size();
        if (log.isDebugEnabled() && submitSize > 0) {
            log.debug("[{}] submitting [{}] messages to rule engine", queueName, submitSize);
        }
        tmpPack.forEach(msgConsumer);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`BatchTbRuleEngineSubmitStrategy` 在 ThingsBoard Application 模块 中承担队列服务类型职责，核心目的是封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 核心流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
 * 3. 关键依赖：主要依赖或协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
