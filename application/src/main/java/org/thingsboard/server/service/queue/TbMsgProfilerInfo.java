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
package org.thingsboard.server.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.RuleNodeId;

import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 中文说明：
 * 1. 类目的：`TbMsgProfilerInfo` 是ThingsBoard Application 模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
 */
@Slf4j
public class TbMsgProfilerInfo {
    /**
     * 消息ID，用于定位对应业务对象。
     */
    private final UUID msgId;
    private AtomicLong totalProcessingTime = new AtomicLong();
    private Lock stateLock = new ReentrantLock();
    /**
     * 规则节点ID，用于定位对应业务对象。
     */
    private RuleNodeId currentRuleNodeId;
    private long stateChangeTime;

    /**
     * 功能：创建 `TbMsgProfilerInfo` 实例，并初始化必要字段。
     * 参数：
     * - `msgId`：消息ID。
     * 返回：新创建的对象实例。
     */
    public TbMsgProfilerInfo(UUID msgId) {
        this.msgId = msgId;
    }

    /**
     * 功能：处理`on Start`。
     * 参数：
     * - `ruleNodeId`：规则节点ID。
     * 返回：无。
     */
    public void onStart(RuleNodeId ruleNodeId) {
        long currentTime = System.currentTimeMillis();
        stateLock.lock();
        try {
            currentRuleNodeId = ruleNodeId;
            stateChangeTime = currentTime;
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * 功能：处理`on End`。
     * 参数：
     * - `ruleNodeId`：规则节点ID。
     * 返回：数值结果。
     */
    public long onEnd(RuleNodeId ruleNodeId) {
        long currentTime = System.currentTimeMillis();
        stateLock.lock();
        try {
            if (ruleNodeId.equals(currentRuleNodeId)) {
                long processingTime = currentTime - stateChangeTime;
                stateChangeTime = currentTime;
                totalProcessingTime.addAndGet(processingTime);
                currentRuleNodeId = null;
                return processingTime;
            } else {
                log.trace("[{}] Invalid sequence of rule node processing detected. Expected [{}] but was [{}]", msgId, currentRuleNodeId, ruleNodeId);
                return 0;
            }
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * 功能：处理超时时间。
     * 参数：无。
     * 返回：处理结果。
     */
    public Map.Entry<UUID, Long> onTimeout() {
        long currentTime = System.currentTimeMillis();
        stateLock.lock();
        try {
            if (currentRuleNodeId != null && stateChangeTime > 0) {
                long timeoutTime = currentTime - stateChangeTime;
                totalProcessingTime.addAndGet(timeoutTime);
                return new AbstractMap.SimpleEntry<>(currentRuleNodeId.getId(), timeoutTime);
            }
        } finally {
            stateLock.unlock();
        }
        return null;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbMsgProfilerInfo` 在 ThingsBoard Application 模块 中承担队列服务类型职责，核心目的是封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 核心流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
 * 3. 关键依赖：主要依赖或协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
