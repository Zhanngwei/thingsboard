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
package org.thingsboard.server.common.transport.activity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.transport.activity.strategy.ActivityStrategy;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 中文说明：
 * 1. 类目的：`AbstractActivityManager` 是ThingsBoard Common 模块中的传输协议契约或适配类型，用于抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 生命周期：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Adapter / Strategy / Command。
 */
@Slf4j
public abstract class AbstractActivityManager<Key, Metadata> implements ActivityManager<Key, Metadata> {

    private final ConcurrentMap<Key, ActivityStateWrapper> states = new ConcurrentHashMap<>();

    /**
     * 调度器，用于安排延迟任务或周期任务。
     */
    @Autowired
    protected SchedulerComponent scheduler;

    /**
     * 中文说明：
     * 1. 类目的：`ActivityStateWrapper` 是ThingsBoard Common 模块中的传输协议契约或适配类型，用于抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
     * 4. 生命周期：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Adapter / Strategy / Command。
     */
    @Data
    private class ActivityStateWrapper {

        /**
         * 状态，表示当前对象所处状态。
         */
        private volatile ActivityState<Metadata> state;
        private volatile long lastReportedTime;
        /**
         * 策略对象，封装可复用的处理规则。
         */
        private volatile ActivityStrategy strategy;

    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    protected void init() {
        var reportingPeriodMillis = getReportingPeriodMillis();
        scheduler.scheduleAtFixedRate(this::onReportingPeriodEnd, new Random().nextInt((int) reportingPeriodMillis), reportingPeriodMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 功能：获取上报。
     * 参数：无。
     * 返回：数值结果。
     */
    protected abstract long getReportingPeriodMillis();

    /**
     * 功能：获取策略对象。
     * 参数：无。
     * 返回：处理结果。
     */
    protected abstract ActivityStrategy getStrategy();

    /**
     * 功能：更新状态。
     * 参数：
     * - `key`：键。
     * - `state`：`state` 参数。
     * 返回：处理结果。
     */
    protected abstract ActivityState<Metadata> updateState(Key key, ActivityState<Metadata> state);

    /**
     * 功能：判断`Expired`。
     * 参数：
     * - `lastRecordedTime`：`lastRecordedTime` 参数。
     * 返回：判断结果。
     */
    protected abstract boolean hasExpired(long lastRecordedTime);

    /**
     * 功能：处理状态。
     * 参数：
     * - `key`：键。
     * - `metadata`：待处理数据。
     * 返回：无。
     */
    protected abstract void onStateExpiry(Key key, Metadata metadata);

    /**
     * 功能：上报`Activity`。
     * 参数：
     * - `key`：键。
     * - `metadata`：待处理数据。
     * - `timeToReport`：`timeToReport` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    protected abstract void reportActivity(Key key, Metadata metadata, long timeToReport, ActivityReportCallback<Key> callback);

    /**
     * 功能：处理`on Activity`。
     * 参数：
     * - `key`：键。
     * - `metadata`：待处理数据。
     * - `newLastRecordedTime`：`newLastRecordedTime` 参数。
     * 返回：无。
     */
    @Override
    public void onActivity(Key key, Metadata metadata, long newLastRecordedTime) {
        if (key == null) {
            log.error("Failed to process activity event: provided activity key is null.");
            return;
        }
        log.debug("Received activity event for key: [{}]", key);

        var shouldReport = new AtomicBoolean(false);
        var lastRecordedTime = new AtomicLong();
        var lastReportedTime = new AtomicLong();

        states.compute(key, (__, stateWrapper) -> {
            if (stateWrapper == null) {
                ActivityState<Metadata> newState = new ActivityState<>();
                stateWrapper = new ActivityStateWrapper();
                stateWrapper.setState(newState);
                stateWrapper.setStrategy(getStrategy());
            }
            var state = stateWrapper.getState();
            state.setMetadata(metadata);
            if (state.getLastRecordedTime() < newLastRecordedTime) {
                state.setLastRecordedTime(newLastRecordedTime);
            }
            shouldReport.set(stateWrapper.getStrategy().onActivity());
            lastRecordedTime.set(state.getLastRecordedTime());
            lastReportedTime.set(stateWrapper.getLastReportedTime());
            return stateWrapper;
        });

        if (shouldReport.get() && lastReportedTime.get() < lastRecordedTime.get()) {
            log.debug("Going to report first activity event for key: [{}].", key);
            reportActivity(key, metadata, lastRecordedTime.get(), new ActivityReportCallback<>() {
                @Override
                public void onSuccess(Key key, long reportedTime) {
                    updateLastReportedTime(key, reportedTime);
                }

                @Override
                public void onFailure(Key key, Throwable t) {
                    log.debug("Failed to report first activity event for key: [{}].", key, t);
                }
            });
        }
    }

    /**
     * 功能：处理上报。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void onReportingPeriodEnd() {
        log.debug("Going to end reporting period.");
        for (Map.Entry<Key, ActivityStateWrapper> entry : states.entrySet()) {
            var key = entry.getKey();
            var stateWrapper = entry.getValue();
            var currentState = stateWrapper.getState();

            long lastRecordedTime = currentState.getLastRecordedTime();
            long lastReportedTime = stateWrapper.getLastReportedTime();
            var metadata = currentState.getMetadata();

            boolean hasExpired;
            boolean shouldReport;

            var updatedState = updateState(key, currentState);
            if (updatedState != null) {
                stateWrapper.setState(updatedState);
                lastRecordedTime = updatedState.getLastRecordedTime();
                metadata = updatedState.getMetadata();
                hasExpired = hasExpired(lastRecordedTime);
                shouldReport = stateWrapper.getStrategy().onReportingPeriodEnd();
            } else {
                states.remove(key);
                hasExpired = false;
                shouldReport = true;
            }

            if (hasExpired) {
                states.remove(key);
                onStateExpiry(key, metadata);
                shouldReport = true;
            }

            if (shouldReport && lastReportedTime < lastRecordedTime) {
                log.debug("Going to report last activity event for key: [{}].", key);
                reportActivity(key, metadata, lastRecordedTime, new ActivityReportCallback<>() {
                    @Override
                    public void onSuccess(Key key, long reportedTime) {
                        updateLastReportedTime(key, reportedTime);
                    }

                    @Override
                    public void onFailure(Key key, Throwable t) {
                        log.debug("Failed to report last activity event for key: [{}].", key, t);
                    }
                });
            }
        }
    }

    /**
     * 功能：获取时间。
     * 参数：
     * - `key`：键。
     * 返回：数值结果。
     */
    @Override
    public long getLastRecordedTime(Key key) {
        ActivityStateWrapper stateWrapper = states.get(key);
        return stateWrapper == null ? 0L : stateWrapper.getState().getLastRecordedTime();
    }

    /**
     * 功能：更新时间。
     * 参数：
     * - `key`：键。
     * - `newLastReportedTime`：`newLastReportedTime` 参数。
     * 返回：无。
     */
    private void updateLastReportedTime(Key key, long newLastReportedTime) {
        states.computeIfPresent(key, (__, stateWrapper) -> {
            stateWrapper.setLastReportedTime(Math.max(stateWrapper.getLastReportedTime(), newLastReportedTime));
            return stateWrapper;
        });
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractActivityManager` 在 ThingsBoard Common 模块 中承担传输协议契约或适配类型职责，核心目的是抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 核心流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
