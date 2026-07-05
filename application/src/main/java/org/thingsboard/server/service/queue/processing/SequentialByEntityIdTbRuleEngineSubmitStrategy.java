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
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;

/**
 * 中文说明：
 * 1. 类目的：`SequentialByEntityIdTbRuleEngineSubmitStrategy` 是ThingsBoard Application 模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
 */
@Slf4j
public abstract class SequentialByEntityIdTbRuleEngineSubmitStrategy extends AbstractTbRuleEngineSubmitStrategy {

    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private volatile BiConsumer<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgConsumer;
    private volatile ConcurrentMap<UUID, EntityId> msgToEntityIdMap = new ConcurrentHashMap<>();
    private volatile ConcurrentMap<EntityId, Queue<IdMsgPair<TransportProtos.ToRuleEngineMsg>>> entityIdToListMap = new ConcurrentHashMap<>();

    /**
     * 功能：创建 `SequentialByEntityIdTbRuleEngineSubmitStrategy` 实例，并初始化必要字段。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * 返回：新创建的对象实例。
     */
    public SequentialByEntityIdTbRuleEngineSubmitStrategy(String queueName) {
        super(queueName);
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `msgs`：待处理消息。
     * 返回：无。
     */
    @Override
    public void init(List<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgs) {
        super.init(msgs);
        initMaps();
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
        entityIdToListMap.forEach((entityId, queue) -> {
            IdMsgPair<TransportProtos.ToRuleEngineMsg> msg = queue.peek();
            if (msg != null) {
                msgConsumer.accept(msg.uuid, msg.msg);
            }
        });
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
        initMaps();
    }

    /**
     * 功能：执行 `doOnSuccess` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * 返回：无。
     */
    @Override
    protected void doOnSuccess(UUID id) {
        EntityId entityId = msgToEntityIdMap.get(id);
        if (entityId != null) {
            Queue<IdMsgPair<TransportProtos.ToRuleEngineMsg>> queue = entityIdToListMap.get(entityId);
            if (queue != null) {
                IdMsgPair<TransportProtos.ToRuleEngineMsg> next = null;
                synchronized (queue) {
                    IdMsgPair<TransportProtos.ToRuleEngineMsg> expected = queue.peek();
                    if (expected != null && expected.uuid.equals(id)) {
                        queue.poll();
                        next = queue.peek();
                    }
                }
                if (next != null) {
                    msgConsumer.accept(next.uuid, next.msg);
                }
            }
        }
    }

    /**
     * 功能：初始化或启动`Maps`。
     * 参数：无。
     * 返回：无。
     */
    private void initMaps() {
        msgToEntityIdMap.clear();
        entityIdToListMap.clear();
        for (IdMsgPair<TransportProtos.ToRuleEngineMsg> pair : orderedMsgList) {
            EntityId entityId = getEntityId(pair.msg.getValue());
            if (entityId != null) {
                msgToEntityIdMap.put(pair.uuid, entityId);
                entityIdToListMap.computeIfAbsent(entityId, id -> new LinkedList<>()).add(pair);
            }
        }
    }

    /**
     * 功能：获取实体ID。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    protected abstract EntityId getEntityId(TransportProtos.ToRuleEngineMsg msg);

}

/*
 * 本类总结：
 * 1. 核心职责：`SequentialByEntityIdTbRuleEngineSubmitStrategy` 在 ThingsBoard Application 模块 中承担队列服务类型职责，核心目的是封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 核心流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
 * 3. 关键依赖：主要依赖或协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
