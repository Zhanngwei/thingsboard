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

import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`AbstractTbRuleEngineSubmitStrategy` 是ThingsBoard Application 模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
 */
public abstract class AbstractTbRuleEngineSubmitStrategy implements TbRuleEngineSubmitStrategy {

    /**
     * 队列名称，用于标识或展示当前对象。
     */
    protected final String queueName;
    protected List<IdMsgPair<TransportProtos.ToRuleEngineMsg>> orderedMsgList;
    /**
     * 当前处理是否已经停止。
     */
    private volatile boolean stopped;

    /**
     * 功能：创建 `AbstractTbRuleEngineSubmitStrategy` 实例，并初始化必要字段。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * 返回：新创建的对象实例。
     */
    public AbstractTbRuleEngineSubmitStrategy(String queueName) {
        this.queueName = queueName;
    }

    /**
     * 功能：执行 `doOnSuccess` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * 返回：无。
     */
    protected abstract void doOnSuccess(UUID id);

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `msgs`：待处理消息。
     * 返回：无。
     */
    @Override
    public void init(List<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgs) {
        orderedMsgList = msgs.stream().map(msg -> new IdMsgPair<>(UUID.randomUUID(), msg)).collect(Collectors.toList());
    }

    /**
     * 功能：获取`Pending Map`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> getPendingMap() {
        return orderedMsgList.stream().collect(Collectors.toConcurrentMap(pair -> pair.uuid, pair -> pair.msg));
    }

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `reprocessMap`：键值映射。
     * 返回：无。
     */
    @Override
    public void update(ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> reprocessMap) {
        List<IdMsgPair<TransportProtos.ToRuleEngineMsg>> newOrderedMsgList = new ArrayList<>(reprocessMap.size());
        for (IdMsgPair<TransportProtos.ToRuleEngineMsg> pair : orderedMsgList) {
            if (reprocessMap.containsKey(pair.uuid)) {
                if (StringUtils.isNotEmpty(pair.getMsg().getValue().getFailureMessage())) {
                    var toRuleEngineMsg = TransportProtos.ToRuleEngineMsg.newBuilder(pair.getMsg().getValue())
                            .clearFailureMessage()
                            .clearRelationTypes()
                            .build();
                    var newMsg = new TbProtoQueueMsg<>(pair.getMsg().getKey(), toRuleEngineMsg, pair.getMsg().getHeaders());
                    newOrderedMsgList.add(new IdMsgPair<>(pair.getUuid(), newMsg));
                } else {
                    newOrderedMsgList.add(pair);
                }
            }
        }
        orderedMsgList = newOrderedMsgList;
    }

    /**
     * 功能：处理`on Success`。
     * 参数：
     * - `id`：`id`ID。
     * 返回：无。
     */
    @Override
    public void onSuccess(UUID id) {
        if (!stopped) {
            doOnSuccess(id);
        }
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void stop() {
        stopped = true;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractTbRuleEngineSubmitStrategy` 在 ThingsBoard Application 模块 中承担队列服务类型职责，核心目的是封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 核心流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
 * 3. 关键依赖：主要依赖或协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
