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
 * 1. `AbstractActivityManager` 是 ThingsBoard Common Transport 中负责 `Activity` 的协调管理组件。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `ActivityManager`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
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
     * 1. `ActivityStateWrapper` 是 ThingsBoard Common Transport 中负责 `Activity State Wrapper` 接入或传输适配的类型。
     * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
     * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
     * 4. 它直接协作于传输服务、会话对象、编解码器或网络处理器。
     * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
     * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
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
